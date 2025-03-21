//--------------------------------------------------------------------------------------------------
// System Programming                       Memory Lab                                   Spring 2024
//
/// @file
/// @brief dynamic memory manager
/// @author Jinho Choi
/// @studid 2020-12852
//--------------------------------------------------------------------------------------------------


// Dynamic memory manager
// ======================
// This module implements a custom dynamic memory manager.
//
// Heap organization:
// ------------------
// The data segment for the heap is provided by the dataseg module. A 'word' in the heap is
// eight bytes.
//
// Implicit free list:
// -------------------
// - minimal block size: 32 bytes (header + footer + 2 data words)
// - h,f: header/footer of free block
// - H,F: header/footer of allocated block
//
// - state after initialization
//
//         initial sentinel half-block                  end sentinel half-block
//                   |                                             |
//   ds_heap_start   |   heap_start                         heap_end       ds_heap_brk
//               |   |   |                                         |       |
//               v   v   v                                         v       v
//               +---+---+-----------------------------------------+---+---+
//               |???| F | h :                                 : f | H |???|
//               +---+---+-----------------------------------------+---+---+
//                       ^                                         ^
//                       |                                         |
//               32-byte aligned                           32-byte aligned
//
// - allocation policy: best fit
// - block splitting: always at 32-byte boundaries
// - immediate coalescing upon free
//
// Explicit free list:
// -------------------
// - minimal block size: 32 bytes (header + footer + next + prev)
// - h,f: header/footer of free block
// - n,p: next/previous pointer
// - H,F: header/footer of allocated block
//
// - state after initialization
//
//         initial sentinel half-block                  end sentinel half-block
//                   |                                             |
//   ds_heap_start   |   heap_start                         heap_end       ds_heap_brk
//               |   |   |                                         |       |
//               v   v   v                                         v       v
//               +---+---+-----------------------------------------+---+---+
//               |???| F | h : n : p :                         : f | H |???|
//               +---+---+-----------------------------------------+---+---+
//                       ^                                         ^
//                       |                                         |
//               32-byte aligned                           32-byte aligned
//
// - allocation policy: best fit
// - block splitting: always at 32-byte boundaries
// - immediate coalescing upon free
//

#define _GNU_SOURCE

#include <assert.h>
#include <error.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "dataseg.h"
#include "memmgr.h"


/// @name global variables
/// @{
static void *ds_heap_start = NULL;                     ///< physical start of data segment
static void *ds_heap_brk   = NULL;                     ///< physical end of data segment
static void *heap_start    = NULL;                     ///< logical start of heap
static void *heap_end      = NULL;                     ///< logical end of heap
static int  PAGESIZE       = 0;                        ///< memory system page size
static void *(*get_free_block)(size_t) = NULL;         ///< get free block for selected allocation policy
static size_t CHUNKSIZE    = 1<<16;                    ///< minimal data segment allocation unit
static size_t SHRINKTHLD   = 1<<14;                    ///< threshold to shrink heap
static int  mm_initialized = 0;                        ///< initialized flag (yes: 1, otherwise 0)
static int  mm_loglevel    = 0;                        ///< log level (0: off; 1: info; 2: verbose)

// Freelist
static FreelistPolicy freelist_policy  = 0;            ///< free list management policy


//
// TODO: add more global variables as needed
//

static void *freelist_root = NULL;

/// @}

/// @name Macro definitions
/// @{
#define MAX(a, b)          ((a) > (b) ? (a) : (b))     ///< MAX function

#define TYPE               unsigned long               ///< word type of heap
#define TYPE_SIZE          sizeof(TYPE)                ///< size of word type

#define ALLOC              1                           ///< block allocated flag
#define FREE               0                           ///< block free flag
#define STATUS_MASK        ((TYPE)(0x7))               ///< mask to retrieve flags from header/footer
#define SIZE_MASK          (~STATUS_MASK)              ///< mask to retrieve size from header/footer

#define BS                 32                          ///< minimal block size. Must be a power of 2
#define BS_MASK            (~(BS-1))                   ///< alignment mask

#define WORD(p)            ((TYPE)(p))                 ///< convert pointer to TYPE
#define PTR(w)             ((void*)(w))                ///< convert TYPE to void*

#define PREV_PTR(p)        ((p)-TYPE_SIZE)             ///< get pointer to word preceeding p
#define NEXT_PTR(p)        ((p)+TYPE_SIZE)             ///< get pointer to word succeeding p
#define HDR2FTR(p)         ((p)+GET_SIZE(p)-TYPE_SIZE) ///< get footer for given header
#define FTR2HDR(p)         ((p)-GET_SIZE(p)+TYPE_SIZE) ///< get header for given footer

#define PACK(size,status)  ((size) | (status))         ///< pack size & status into boundary tag
#define SIZE(v)            (v & SIZE_MASK)             ///< extract size from boundary tag
#define STATUS(v)          (v & STATUS_MASK)           ///< extract status from boundary tag

#define PUT(p, v)          (*(TYPE*)(p) = (TYPE)(v))   ///< write word v to *p
#define GET(p)             (*(TYPE*)(p))               ///< read word at *p
#define GET_SIZE(p)        (SIZE(GET(p)))              ///< extract size from header/footer
#define GET_STATUS(p)      (STATUS(GET(p)))            ///< extract status from header/footer

#define PREV_ALIGN(p)      (PTR(WORD(p)&BS_MASK))             ///< round down to multiple of BS
#define NEXT_ALIGN(p)      (PTR((WORD(p)+(BS-1))&BS_MASK))      ///< round up to multiple of BS

#define PREV_BLOCK(p)      (FTR2HDR(PREV_PTR(p)))
#define NEXT_BLOCK(p)      (NEXT_PTR(HDR2FTR(p)))

#define N_PREV_PTR(p, n)   ((p) - (n * TYPE_SIZE))
#define N_NEXT_PTR(p, n)   ((p) + (n * TYPE_SIZE))

#define PREV_LIST_GET(p)   (PTR(GET(NEXT_PTR(NEXT_PTR(p))))) ///< get previous list ptr explicit mode
#define NEXT_LIST_GET(p)   (PTR(GET(NEXT_PTR(p))))           ///< get next list ptr explicit mode

#define PREV_LIST_SET(p, v)   (PUT(NEXT_PTR(NEXT_PTR(p)), v)) ///< set previous list ptr explicit mode
#define NEXT_LIST_SET(p, v)   (PUT(NEXT_PTR(p), v))           ///< set next list ptr explicit mode

//
// TODO: add more macros as needed
//
/// @}


/// @name Logging facilities
/// @{

/// @brief print a log message if level <= mm_loglevel. The variadic argument is a printf format
///        string followed by its parametrs
#ifdef DEBUG
  #define LOG(level, ...) mm_log(level, __VA_ARGS__)

/// @brief print a log message. Do not call directly; use LOG() instead
/// @param level log level of message.
/// @param ... variadic parameters for vprintf function (format string with optional parameters)
static void mm_log(int level, ...)
{
  if (level > mm_loglevel) return;

  va_list va;
  va_start(va, level);
  const char *fmt = va_arg(va, const char*);

  if (fmt != NULL) vfprintf(stdout, fmt, va);

  va_end(va);

  fprintf(stdout, "\n");
}

#else
  #define LOG(level, ...)
#endif

/// @}


/// @name Program termination facilities
/// @{

/// @brief print error message and terminate process. The variadic argument is a printf format
///        string followed by its parameters
#define PANIC(...) mm_panic(__func__, __VA_ARGS__)

/// @brief print error message and terminate process. Do not call directly, Use PANIC() instead.
/// @param func function name
/// @param ... variadic parameters for vprintf function (format string with optional parameters)
static void mm_panic(const char *func, ...)
{
  va_list va;
  va_start(va, func);
  const char *fmt = va_arg(va, const char*);

  fprintf(stderr, "PANIC in %s%s", func, fmt ? ": " : ".");
  if (fmt != NULL) vfprintf(stderr, fmt, va);

  va_end(va);

  fprintf(stderr, "\n");

  exit(EXIT_FAILURE);
}
/// @}


static void* bf_get_free_block_implicit(size_t size);
static void* bf_get_free_block_explicit(size_t size);

void mm_init(FreelistPolicy fp)
{
  LOG(1, "mm_init()");

  //
  // set free list policy
  //
  freelist_policy = fp;
  switch (freelist_policy)
  {
    case fp_Implicit:
      get_free_block = bf_get_free_block_implicit;
      break;

    case fp_Explicit:
      get_free_block = bf_get_free_block_explicit;
      break;

    default:
      PANIC("Non supported freelist policy.");
      break;
  }

  //
  // retrieve heap status and perform a few initial sanity checks
  //
  ds_heap_stat(&ds_heap_start, &ds_heap_brk, NULL);
  PAGESIZE = ds_getpagesize();

  LOG(2, "  ds_heap_start:          %p\n"
         "  ds_heap_brk:            %p\n"
         "  PAGESIZE:               %d\n",
         ds_heap_start, ds_heap_brk, PAGESIZE);

  if (ds_heap_start == NULL) PANIC("Data segment not initialized.");
  if (ds_heap_start != ds_heap_brk) PANIC("Heap not clean.");
  if (PAGESIZE == 0) PANIC("Reported pagesize == 0.");

  //
  // initialize heap
  //
  // TODO

  ds_sbrk(CHUNKSIZE);
  void *ds_heap_end;

  ds_heap_stat(&ds_heap_start, &ds_heap_brk, &ds_heap_end);
  heap_start = NEXT_ALIGN(ds_heap_start + 8);
  heap_end = PREV_ALIGN(ds_heap_brk - 8);

  PUT(PREV_PTR(heap_start), ALLOC);
  PUT(heap_start, heap_end - heap_start);
  PUT(HDR2FTR(heap_start), GET_SIZE(heap_start));
  PUT(heap_end, ALLOC);

  if (freelist_policy == fp_Explicit)
  {
    freelist_root = heap_start;
    NEXT_LIST_SET(heap_start, 0);
    PREV_LIST_SET(heap_start, 0);
  }

  //
  // heap is initialized
  //
  mm_initialized = 1;
}

void push_freelist(void *block)
{
  if (freelist_root != NULL)
  {
    PREV_LIST_SET(freelist_root, block);
    NEXT_LIST_SET(block, freelist_root);
    PREV_LIST_SET(block, 0);
  }
  freelist_root = block;
}

size_t get_chunk_size(size_t size)
{
  size_t last_block_size = GET_STATUS(PREV_PTR(heap_end)) == FREE ? \
                                    GET_SIZE(PREV_PTR(heap_end)) : \
                                    0;
  size_t chunk_size = CHUNKSIZE;
  while (chunk_size + last_block_size < size)
    chunk_size += CHUNKSIZE;
  return chunk_size;
}

void *coalesce_blocks(void *p);

void *expand_heap(size_t size)
{
  PUT(heap_end, FREE);

  size_t chunk_size = get_chunk_size(size);
  if (ds_sbrk(chunk_size) == (void*)-1)
    return NULL; // cannot expand heap

  void *last_hdr = heap_end;
  ds_heap_stat(&ds_heap_start, &ds_heap_brk, NULL);
  heap_end = PREV_ALIGN(ds_heap_brk - 8);

  PUT(last_hdr, PACK(heap_end - last_hdr, FREE));
  PUT(HDR2FTR(last_hdr), PACK(heap_end - last_hdr, FREE));
  PUT(heap_end, ALLOC);

  return coalesce_blocks(last_hdr);
}

/// @brief find and return a free block of at least @a size bytes (best fit)
/// @param size size of block (including header & footer tags), in bytes
/// @retval void* pointer to header of large enough free block
/// @retval NULL if no free block of the requested size is avilable
static void* bf_get_free_block_implicit(size_t size)
{
  LOG(1, "bf_get_free_block_implicit(0x%lx (%lu))", size, size);

  assert(mm_initialized);

  //
  // TODO
  //
  void *p;
  void *best_fit;

  p = heap_start;
  best_fit = NULL;
  while (p < heap_end)
  {
    TYPE hdr = GET(p);
    TYPE block_size = SIZE(hdr);
    TYPE block_status = STATUS(hdr);

    if (best_fit == NULL && block_status == FREE && size <= block_size)
      best_fit = p;
    else if (best_fit != NULL && block_status == FREE && size <= block_size && block_size < GET_SIZE(best_fit))
      best_fit = p;

    // not enough heap
    if (p + block_size >= heap_end && best_fit == NULL)
    {
      if (!(p = expand_heap(size)))
        return NULL;
    }
    else
      p = p + block_size;
  }

  size_t original_size = GET_SIZE(best_fit);
  if (original_size > size)
  {
    PUT(best_fit, PACK(size, FREE));
    PUT(HDR2FTR(best_fit), PACK(size, FREE));
    PUT(best_fit + size, PACK(original_size - size, FREE));
    PUT(HDR2FTR(best_fit + size), PACK(original_size - size, FREE));
  }

  return best_fit;
}

void splice_out(void *block);

/// @brief find and return a free block of at least @a size bytes (best fit)
/// @param size size of block (including header & footer tags), in bytes
/// @retval void* pointer to header of large enough free block
/// @retval NULL if no free block of the requested size is avilable
static void* bf_get_free_block_explicit(size_t size)
{
  LOG(1, "bf_get_free_block_explicit(0x%lx (%lu))", size, size);

  assert(mm_initialized);

  //
  // TODO
  //
  void *p = freelist_root;
  void *best_fit = NULL;

  if (p == NULL)
    if (!(p = expand_heap(size)))
      return NULL;

  while (p != NULL)
  {
    TYPE hdr = GET(p);
    TYPE block_size = SIZE(hdr);
    TYPE block_status = STATUS(hdr);

    if (best_fit == NULL && block_status == FREE && size <= block_size)
      best_fit = p;
    else if (best_fit != NULL && block_status == FREE && size <= block_size && block_size < GET_SIZE(best_fit))
      best_fit = p;

    // not enough heap
    if (NEXT_LIST_GET(p) == NULL && best_fit == NULL)
    {
      if (!(best_fit = expand_heap(size)))
        return NULL;
      break;
    }
    else
      p = NEXT_LIST_GET(p);
  }

  size_t original_size = GET_SIZE(best_fit);
  if (original_size > size)
  {
    void *new_block = best_fit + size;

    PUT(best_fit, PACK(size, FREE));
    PUT(HDR2FTR(best_fit), PACK(size, FREE));
    PUT(new_block, PACK(original_size - size, FREE));
    PUT(HDR2FTR(new_block), PACK(original_size - size, FREE));

    if (best_fit == freelist_root)
    {
      PREV_LIST_SET(new_block, 0);
      NEXT_LIST_SET(new_block, NEXT_LIST_GET(best_fit));
      freelist_root = new_block;
    }
    else
    {
      PREV_LIST_SET(freelist_root, new_block);
      NEXT_LIST_SET(new_block, freelist_root);
      PREV_LIST_SET(new_block, 0);
      freelist_root = new_block;
    }
  }
  else
  {
    if (NEXT_LIST_GET(best_fit) != 0)
      PREV_LIST_SET(NEXT_LIST_GET(best_fit), PREV_LIST_GET(best_fit));
    if (PREV_LIST_GET(best_fit) != 0)
      NEXT_LIST_SET(PREV_LIST_GET(best_fit), NEXT_LIST_GET(best_fit));
    else
      freelist_root = NEXT_LIST_GET(best_fit);
  }
  return best_fit;
}

/// @brief get block-aligned memory size over size + padding
/// @param padding space used to save metadata
TYPE get_block_size(size_t size, size_t padding)
{
  size_t block_size = BS;

  while (block_size < size + padding)
    block_size += BS;
  return block_size;
}

void* mm_malloc(size_t size)
{
  LOG(1, "mm_malloc(0x%lx (%lu))", size, size);

  assert(mm_initialized);

  //
  // TODO
  //

  size_t block_size;
  if (freelist_policy == fp_Implicit)
    block_size = get_block_size(size, 2 * TYPE_SIZE);
  else  // fp_Explicit
    block_size = get_block_size(size, BS);
  void *p = get_free_block(block_size);

  if (!p)
    return NULL;

  if (freelist_policy == fp_Explicit)
  {
    if (NEXT_LIST_GET(p) != 0 || PREV_LIST_GET(p) != 0)
      splice_out(p);
  }
  PUT(p, PACK(GET_SIZE(p), ALLOC));
  PUT(HDR2FTR(p), PACK(GET_SIZE(p), ALLOC));

  return NEXT_PTR(p);
}


void* mm_calloc(size_t nmemb, size_t size)
{
  LOG(1, "mm_calloc(0x%lx, 0x%lx (%lu))", nmemb, size, size);

  assert(mm_initialized);

  //
  // calloc is simply malloc() followed by memset()
  //
  void *payload = mm_malloc(nmemb * size);

  if (payload != NULL) memset(payload, 0, nmemb * size);

  return payload;
}


void* mm_realloc(void *ptr, size_t size)
{
  LOG(1, "mm_realloc(%p, 0x%lx (%lu))", ptr, size, size);

  assert(mm_initialized);

  //
  // TODO
  //
  // ptr == NULL?
  if (ptr == NULL)
    return mm_malloc(size);
  // size == 0?
  else if (size == 0)
  {
    mm_free(ptr);
    return NULL;
  }
  void *p = PREV_PTR(ptr);
  size_t size_new = get_block_size(size, 2 * TYPE_SIZE);
  size_t size_now = GET_SIZE(p);
  //size_new < size_now?
  if (size_new < size_now)
  {
    PUT(p, PACK(size_new, ALLOC));
    PUT(HDR2FTR(p), PACK(size_new, ALLOC));
    PUT(NEXT_BLOCK(p), PACK(size_now - size_new, FREE));
    PUT(HDR2FTR(NEXT_BLOCK(p)), PACK(size_now - size_new, FREE));
    coalesce_blocks(NEXT_BLOCK(p));
  }
  // size_new == size_now
  else if (size_new == GET_SIZE(p))
    return ptr;
  else
  {
    size_t succesor_size = GET_SIZE(NEXT_BLOCK(p));
    // successor == free && successor_size + size_now >= size_new
    if (GET_STATUS(NEXT_BLOCK(p)) == FREE && succesor_size + size_now >= size_new)
    {
      if (freelist_policy == fp_Explicit)
        splice_out(NEXT_BLOCK(p));
      PUT(p, PACK(size_new, ALLOC));
      PUT(HDR2FTR(p), PACK(size_new, ALLOC));
      if (succesor_size + size_now != size_new)
      {
        // 여기 링크드리스트
        PUT(NEXT_BLOCK(p), PACK(succesor_size + size_now - size_new, FREE));
        PUT(HDR2FTR(NEXT_BLOCK(p)), PACK(succesor_size + size_now - size_new, FREE));
        if (freelist_policy == fp_Explicit)
          push_freelist(NEXT_BLOCK(p));
      }
      return ptr;
    }
    else
    {
      // copy payloads
      void *new_p = get_free_block(size_new);
      unsigned char *src = (unsigned char *)(NEXT_PTR(p));
      unsigned char *dst = (unsigned char *)(NEXT_PTR(new_p));

      PUT(new_p, PACK(GET_SIZE(new_p), ALLOC));
      PUT(HDR2FTR(new_p), PACK(GET_SIZE(new_p), ALLOC));

      for (int i = 0; i < size_now - 2 * TYPE_SIZE; i++)
        dst[i] = src[i];
      mm_free(ptr);
      return NEXT_PTR(new_p);
    }
  }
  return NULL;
}

void splice_out(void *block)
{
  if (PREV_LIST_GET(block) != 0)
    NEXT_LIST_SET(PREV_LIST_GET(block), NEXT_LIST_GET(block));
  else
    freelist_root = NEXT_LIST_GET(block);
  if (NEXT_LIST_GET(block) != 0)
    PREV_LIST_SET(NEXT_LIST_GET(block), PREV_LIST_GET(block));
}

void *coalesce_blocks(void *p)
{
  void *prev_hdr = PREV_BLOCK(p);
  void *next_hdr = NEXT_BLOCK(p);
  void *coalesced_block = p;

  if (GET_STATUS(prev_hdr) == ALLOC && GET_STATUS(next_hdr) == ALLOC)
  {
    PUT(p, PACK(GET_SIZE(p), FREE));
    PUT(HDR2FTR(p), PACK(GET_SIZE(p), FREE));
    if (freelist_policy == fp_Explicit)
      push_freelist(coalesced_block);
  }
  else if (GET_STATUS(prev_hdr) == FREE && GET_STATUS(next_hdr) == ALLOC)
  {
    PUT(prev_hdr, PACK(GET_SIZE(prev_hdr) + GET_SIZE(p), FREE));
    PUT(HDR2FTR(prev_hdr), PACK(GET_SIZE(prev_hdr), FREE));
    coalesced_block = prev_hdr;
  }
  else if (GET_STATUS(prev_hdr) == ALLOC && GET_STATUS(next_hdr) == FREE)
  {
    PUT(p, PACK(GET_SIZE(next_hdr) + GET_SIZE(p), FREE));
    PUT(HDR2FTR(p), PACK(GET_SIZE(p), FREE));
    NEXT_LIST_SET(p, 0);
    PREV_LIST_SET(p, 0);
    if (freelist_policy == fp_Explicit)
    {
      splice_out(next_hdr);
      push_freelist(coalesced_block);
    }
  }
  else if (GET_STATUS(prev_hdr) == FREE && GET_STATUS(next_hdr) == FREE)
  {
    PUT(prev_hdr, PACK(GET_SIZE(prev_hdr) + GET_SIZE(p) + GET_SIZE(next_hdr), FREE));
    PUT(HDR2FTR(prev_hdr), PACK(GET_SIZE(prev_hdr), FREE));
    NEXT_LIST_SET(p, 0);
    PREV_LIST_SET(p, 0);
    coalesced_block = prev_hdr;
    if (freelist_policy == fp_Explicit)
    {
      splice_out(prev_hdr);
      splice_out(next_hdr);
      push_freelist(coalesced_block);
    }
  }
  return coalesced_block;
}

void shrink_heap(void *coalesced_block)
{
  if (NEXT_BLOCK(coalesced_block) == heap_end)
  {
    size_t shrink_space = 0;
    while (GET_SIZE(coalesced_block) >= CHUNKSIZE + shrink_space)
      shrink_space += SHRINKTHLD;
    if (shrink_space > 0)
    {
      if (ds_sbrk(-shrink_space) != (void *)-1)
      {
        ds_heap_stat(&ds_heap_start, &ds_heap_brk, NULL);
        heap_end = PREV_ALIGN(ds_heap_brk - TYPE_SIZE);
        PUT(coalesced_block, heap_end - coalesced_block);
        PUT(HDR2FTR(coalesced_block), GET_SIZE(coalesced_block));
        PUT(heap_end, ALLOC);
      }
    }
  }
}

void mm_free(void *ptr)
{
  LOG(1, "mm_free(%p)", ptr);

  assert(mm_initialized);

  //
  // TODO
  //
  if (heap_start > ptr || heap_end < ptr)
  {
    fprintf(stderr, "attempt to free invalid ptr\n");
    return;
  }
  void *p = PREV_PTR(ptr);
  if (GET_STATUS(p) == FREE)
  {
    fprintf(stderr, "double free\n");
    return;
  }
  p = coalesce_blocks(p);
  shrink_heap(p);
}


void mm_setloglevel(int level)
{
  mm_loglevel = level;
}


void mm_check(void)
{
  assert(mm_initialized);

  void *p;

  char *fpstr;
  if (freelist_policy == fp_Implicit) fpstr = "Implicit";
  else if (freelist_policy == fp_Explicit) fpstr = "Explicit";
  else fpstr = "invalid";

  printf("----------------------------------------- mm_check ----------------------------------------------\n");
  printf("  ds_heap_start:          %p\n", ds_heap_start);
  printf("  ds_heap_brk:            %p\n", ds_heap_brk);
  printf("  heap_start:             %p\n", heap_start);
  printf("  heap_end:               %p\n", heap_end);
  printf("  free list policy:       %s\n", fpstr);

  printf("\n");
  p = PREV_PTR(heap_start);
  printf("  initial sentinel:       %p: size: %6lx (%7ld), status: %s\n",
         p, GET_SIZE(p), GET_SIZE(p), GET_STATUS(p) == ALLOC ? "allocated" : "free");
  p = heap_end;
  printf("  end sentinel:           %p: size: %6lx (%7ld), status: %s\n",
         p, GET_SIZE(p), GET_SIZE(p), GET_STATUS(p) == ALLOC ? "allocated" : "free");
  printf("\n");

  if(freelist_policy == fp_Implicit){
    printf("    %-14s  %8s  %10s  %10s  %8s  %s\n", "address", "offset", "size (hex)", "size (dec)", "payload", "status");
  }
  else if(freelist_policy == fp_Explicit){
    printf("    %-14s  %8s  %10s  %10s  %8s  %-14s  %-14s  %s\n", "address", "offset", "size (hex)", "size (dec)", "payload", "next", "prev", "status");
  }

  long errors = 0;
  p = heap_start;
  while (p < heap_end) {
    char *ofs_str, *size_str;

    TYPE hdr = GET(p);
    TYPE size = SIZE(hdr);
    TYPE status = STATUS(hdr);

    void *next = NEXT_LIST_GET(p);
    void *prev = PREV_LIST_GET(p);

    if (asprintf(&ofs_str, "0x%lx", p-heap_start) < 0) ofs_str = NULL;
    if (asprintf(&size_str, "0x%lx", size) < 0) size_str = NULL;

    if(freelist_policy == fp_Implicit){
      printf("    %p  %8s  %10s  %10ld  %8ld  %s\n",
                p, ofs_str, size_str, size, size-2*TYPE_SIZE, status == ALLOC ? "allocated" : "free");
    }
    else if(freelist_policy == fp_Explicit){
      printf("    %p  %8s  %10s  %10ld  %8ld  %-14p  %-14p  %s\n",
                p, ofs_str, size_str, size, size-2*TYPE_SIZE,
                status == ALLOC ? NULL : next, status == ALLOC ? NULL : prev,
                status == ALLOC ? "allocated" : "free");
    }

    free(ofs_str);
    free(size_str);

    void *fp = p + size - TYPE_SIZE;
    TYPE ftr = GET(fp);
    TYPE fsize = SIZE(ftr);
    TYPE fstatus = STATUS(ftr);

    if ((size != fsize) || (status != fstatus)) {
      errors++;
      printf("    --> ERROR: footer at %p with different properties: size: %lx, status: %lx\n",
             fp, fsize, fstatus);
      mm_panic("mm_check");
    }

    p = p + size;
    if (size == 0) {
      printf("    WARNING: size 0 detected, aborting traversal.\n");
      break;
    }
  }

  printf("\n");
  if ((p == heap_end) && (errors == 0)) printf("  Block structure coherent.\n");
  printf("-------------------------------------------------------------------------------------------------\n");
}


