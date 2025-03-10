import java.io.*;
import java.util.*;

public class SortingTest {
    public static void main(String args[]) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try {
            boolean isRandom = false;    // 입력받은 배열이 난수인가 아닌가?
            int[] value;    // 입력 받을 숫자들의 배열
            String nums = br.readLine();    // 첫 줄을 입력 받음
            if (nums.charAt(0) == 'r') {
                // 난수일 경우
                isRandom = true;    // 난수임을 표시

                String[] nums_arg = nums.split(" ");

                int numsize = Integer.parseInt(nums_arg[1]);    // 총 갯수
                int rminimum = Integer.parseInt(nums_arg[2]);    // 최소값
                int rmaximum = Integer.parseInt(nums_arg[3]);    // 최대값

                Random rand = new Random();    // 난수 인스턴스를 생성한다.

                value = new int[numsize];    // 배열을 생성한다.
                for (int i = 0; i < value.length; i++)    // 각각의 배열에 난수를 생성하여 대입
                    value[i] = rand.nextInt(rmaximum - rminimum + 1) + rminimum;
            } else {
                // 난수가 아닐 경우
                int numsize = Integer.parseInt(nums);

                value = new int[numsize];    // 배열을 생성한다.
                for (int i = 0; i < value.length; i++)    // 한줄씩 입력받아 배열원소로 대입
                    value[i] = Integer.parseInt(br.readLine());
            }

            // 숫자 입력을 다 받았으므로 정렬 방법을 받아 그에 맞는 정렬을 수행한다.
            while (true) {
                int[] newvalue = (int[]) value.clone();    // 원래 값의 보호를 위해 복사본을 생성한다.
                char algo = ' ';

                if (args.length == 4) {
                    return;
                }

                String command = args.length > 0 ? args[0] : br.readLine();

                if (args.length > 0) {
                    args = new String[4];
                }

                long t = System.currentTimeMillis();
                switch (command.charAt(0)) {
                    case 'B':    // Bubble Sort
                        newvalue = DoBubbleSort(newvalue);
                        break;
                    case 'I':    // Insertion Sort
                        newvalue = DoInsertionSort(newvalue);
                        break;
                    case 'H':    // Heap Sort
                        newvalue = DoHeapSort(newvalue);
                        break;
                    case 'M':    // Merge Sort
                        newvalue = DoMergeSort(newvalue);
                        break;
                    case 'Q':    // Quick Sort
                        newvalue = DoQuickSort(newvalue);
                        break;
                    case 'R':    // Radix Sort
                        newvalue = DoRadixSort(newvalue);
                        break;
                    case 'S':    // Search
                        algo = DoSearch(newvalue);
                        System.out.println("algo = " + algo);
                        break;
                    case 'X':
                        return;    // 프로그램을 종료한다.
                    default:
                        throw new IOException("잘못된 정렬 방법을 입력했습니다.");
                }
                if (isRandom) {
                    // 난수일 경우 수행시간을 출력한다.
                    System.out.println((System.currentTimeMillis() - t) + " ms");
                } else {
                    // 난수가 아닐 경우 정렬된 결과값을 출력한다.
                    if (command.charAt(0) != 'S') {
                        for (int i = 0; i < newvalue.length; i++) {
                            System.out.println(newvalue[i]);
                        }
                    } else {
                        System.out.println(algo);
                    }
                }

            }
        } catch (IOException e) {
            System.out.println("입력이 잘못되었습니다. 오류 : " + e.toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // this code is based on/referenced from a Textbook
    public static int[] DoBubbleSort(int[] value) {
        // TODO : Bubble Sort 를 구현하라.
        // value는 정렬안된 숫자들의 배열이며 value.length 는 배열의 크기가 된다.
        // 결과로 정렬된 배열은 리턴해 주어야 하며, 두가지 방법이 있으므로 잘 생각해서 사용할것.
        // 주어진 value 배열에서 안의 값만을 바꾸고 value를 다시 리턴하거나
        // 같은 크기의 새로운 배열을 만들어 그 배열을 리턴할 수도 있다.
        int temp;

        for (int i = value.length - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                if (value[j] > value[j + 1]) {
                    temp = value[j];
                    value[j] = value[j + 1];
                    value[j + 1] = temp;
                }
            }
        }
        return (value);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //this code is based on/referenced from a Textbook
    public static int[] DoInsertionSort(int[] value) {
        // TODO : Insertion Sort 를 구현하라.
        int insertionItem;

        for (int i = 1; i < value.length; i++) {
            int j = i - 1;
            insertionItem = value[i];
            while (j >= 0 && insertionItem < value[j]) {
                value[j + 1] = value[j];
                j--;
            }
            value[j + 1] = insertionItem;
        }
        return (value);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //this code is based on/referenced from a Lecture Note
    public static int[] DoHeapSort(int[] value) {
        // TODO : Heap Sort 를 구현하라.
        int[] heap = buildHeap(value);
        for (int heapSize = heap.length; heapSize > 0; heapSize--) {
            value[heapSize - 1] = deleteMax(heap, heapSize);
        }
        return (value);
    }

    private static int[] buildHeap(int[] value) {
        int[] heap = (int[]) value.clone();
        int heapSize = heap.length;
        if (heapSize >= 2)
            for (int i = (heapSize - 2) / 2; i >= 0; i--)
                percolateDown(heap, heapSize, i);
        return (heap);
    }

    private static int deleteMax(int[] heap, int heapSize) {
        int max = heap[0];
        heap[0] = heap[heapSize - 1];
        percolateDown(heap, heapSize - 1, 0);
        return (max);
    }

    private static void percolateDown(int[] heap, int heapSize, int i) {
        int child = 2 * i + 1;
        int rightChild = 2 * i + 2;
        if (child <= heapSize - 1) {
            if (rightChild <= heapSize - 1 && heap[child] < heap[rightChild])
                child = rightChild;
            if (heap[i] < heap[child]) {
                int tmp = heap[i];
                heap[i] = heap[child];
                heap[child] = tmp;
                percolateDown(heap, heapSize, child);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //this code is based on/referenced from a Textbook, Lecture Note, and past Midterm exam paper
    public static int[] DoMergeSort(int[] value) {
        // TODO : Merge Sort 를 구현하라.
        int[] temp = (int[]) value.clone();
        mergeSort(0, value.length - 1, value, temp);
        return (value);
    }

    public static void mergeSort(int p, int r, int A[], int B[]) {
        if (p < r) {
            int q = (p + r) / 2;
            mergeSort(p, q, B, A);
            mergeSort(q + 1, r, B, A);
            switchingMerge(p, q, r, B, A);
        }
    }

    public static void switchingMerge(int p, int q, int r, int C[], int D[]) {
        int i = p;
        int j = q + 1;
        int t = p;
        while (i <= q && j <= r) {
            if (C[i] <= C[j])
                D[t++] = C[i++];
            else
                D[t++] = C[j++];
        }
        while (i <= q)
            D[t++] = C[i++];
        while (j <= r)
            D[t++] = C[j++];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //this code is based on/referenced from a Lecture Note, and Lecture
    public static int[] DoQuickSort(int[] value) {
        // TODO : Quick Sort 를 구현하라.
        quickSort(value, 0, value.length - 1);
        return (value);
    }

    private static void quickSort(int[] value, int p, int r) {
        if (p < r) {
            int q = partition(value, p, r);
            quickSort(value, p, q - 1);
            quickSort(value, q + 1, r);
        }
    }

    private static int partition(int[] value, int p, int r) {
        int x = value[p + ((r - p) / 2)];
        int i = p - 1;
        int tmp;
        value[p + ((r - p) / 2)] = value[r];
        value[r] = x;
        for (int j = p; j < r; j++) {
            if (value[j] < x || (value[j] == x && j % 2 == 0)) {
                ++i;
                tmp = value[j];
                value[j] = value[i];
                value[i] = tmp;
            }
        }
        tmp = value[i + 1];
        value[i + 1] = value[r];
        value[r] = tmp;
        return (i + 1);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //this code is based on/referenced from a ChatGPT's radix sort code, and edited properly
    public static int[] DoRadixSort(int[] value) {
        // TODO : Radix Sort 를 구현하라.
        int maxVal = getMaxValue(value);

        int[] positiveArr = new int[value.length];
        int[] negativeArr = new int[value.length];
        int posIndex = 0;
        int negIndex = 0;

        for (int i = 0; i < value.length; i++) {
            if (value[i] >= 0) {
                positiveArr[posIndex++] = value[i];
            } else {
                negativeArr[negIndex++] = -value[i];
            }
        }

        if (negIndex > 0) {
            radixSort(negativeArr, maxVal);
        }

        if (posIndex > 0) {
            radixSort(positiveArr, maxVal);
        }

        int i = 0;
        for (int j = value.length - 1; j >= value.length - negIndex; j--) {
            value[i++] = -negativeArr[j];
        }
        for (int j = value.length - posIndex; j < value.length; j++) {
            value[i++] = positiveArr[j];
        }
        return (value);
    }

    private static void radixSort(int[] arr, int maxVal) {
        for (int exp = 1; maxVal / exp > 0; exp *= 10) {
            countingSort(arr, exp);
        }
    }

    private static void countingSort(int[] arr, int exp) {
        int[] count = new int[10];

        for (int i = 0; i < arr.length; i++) {
            count[(arr[i] / exp) % 10]++;
        }

        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }

        int[] aux = new int[arr.length];
        for (int i = arr.length - 1; i >= 0; i--) {
            aux[--count[(arr[i] / exp) % 10]] = arr[i];
        }

        for (int i = 0; i < arr.length; i++) {
            arr[i] = aux[i];
        }
    }

    private static int getMaxValue(int[] arr) {
        int max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (Math.abs(arr[i]) > max) {
                max = Math.abs(arr[i]);
            }
        }
        return max;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    private static char DoSearch(int[] value) {
        // TODO : Search 를 구현하라.
        if (isRadixSortAppropriate(value))
            return ('R');
        else if (isHeapSortAppropriate(value))
            return ('H');
        else
            return ('Q');
    }

    private static boolean isRadixSortAppropriate(int[] value) {
        int max = value[0];
        int min = value[0];

        for (int i = 0; i < value.length; i++)
        {
            if (i % 100 > 10)
                value[i] = 1;
            if (max < value[i])
                max = value[i];
            if (min > value[i])
                min = value[i];
        }
        if (max < Math.abs(min))
            max = -min;
        int maxDigitCount = 0;
        while (max > 0)
        {
            maxDigitCount++;
            max /= 10;
        }
        int valueLength = value.length;
        int valueLengthDigitCount = 0;
        while (valueLength > 0)
        {
            valueLengthDigitCount++;
            valueLength /= 10;
        }
        if (valueLengthDigitCount >= 7 && valueLengthDigitCount > maxDigitCount)
            return (true);
        else
            return (false);
    }

    private static boolean isHeapSortAppropriate(int[] value) {
        int repeatedValueCount = getRepeatedValueCount(value);

        if (value.length > 13000019 && repeatedValueCount > value.length * 0.97)
            return (true);
        else if (value.length <= 13000019 && repeatedValueCount > value.length * 0.95)
            return (true);
        else
            return (false);
    }
    private static int getRepeatedValueCount(int[] value)
    {
        int[] hashTable = new int[13000019];
        int crashed = 0;
        for (int i = 0; i < 13000019; i++)
            hashTable[i] = 0;
        for (int i = 0; i < value.length; i++)
        {
            if (hashTable[Math.abs(value[i] % 13000019)] != 0)
                crashed++;
            else
                hashTable[Math.abs(value[i] % 13000019)] = value[i];
        }
        return (crashed);
    }
}
