package pp202402.assign2b

import scala.annotation.tailrec
import scala.util.control.TailCalls._
import scala.compiletime.ops.boolean

/** Principles of Programming: Assignment 2B.
  *
  * Implement given functions, which are currently left blank. (???) **WARNING:
  * Please read the restrictions below carefully.**
  *
  * If you do not follow these, **your submission will not be graded.**
  *
  *   - Do not use the keyword `var`. Use `val` and `def` instead.
  *   - Do not use any library functions or data structures like `List`,
  *     `Range` (`1 to n`, `1 until n` ...), `fold`, `map`, `reduce` or
  *     etc except for `Array`.
  *   - If you want to use a data structure, create new one instead of using the
  *     library ones.
  *   - You can only use tuples, `scala.annotation.tailrec`, and
  *     `scala.util.control.TailCalls._`, `Array` from the library.
  *   - Do not use any looping syntax of Scala (`for`, `while`, `do-while`,
  *     `yield`, ...)
  *
  * Again, your score will be zero if you do not follow these rules.
  *
  * Note that these rules will be gradually relaxed through the next
  * assignments.
  *
  * We do not require tail-recursion explicitly for this assignment.
  *
  * Timeout: 30 sec.
  */
object Assignment2B:
  import IOption.*

  /** Problem 3: Sudoku Solver
   *
   * Solve a given 9x9 Sudoku puzzle. A Sudoku board is a 9x9 grid,
   * where some cells are pre-filled with numbers between 1 and 9, and other cells
   * are empty (represented by 0). The goal is to fill all the empty cells with
   * numbers between 1 and 9 while satisfying the following conditions:
   *
   * 1. Each number from 1 to 9 must appear exactly once in each row.
   * 2. Each number from 1 to 9 must appear exactly once in each column.
   * 3. Each number from 1 to 9 must appear exactly once in each of the nine 3x3 subgrids.
   *
   * This means that for any empty cell, you need to find a valid number such that
   * it does not already exist in its corresponding row, column, or subgrid.
   * Check the link below for more information on Sudoku puzzles:
   * https://en.wikipedia.org/wiki/Sudoku
   *
   * The function `solveSudoku(board: Array[Int]): IOption[Array[Int]]`
   * takes a 9x9 Sudoku board as input and returns an `IOption` of the solved board.
   * If the board is solvable, the function should return `ISome(solvedBoard)`.
   * If the board is not solvable, the function should return `INone`.
   *
   * Note: While you are not allowed to use `Array` in other problems,
   * we have provided a board as a 1D array using `Array` here. You can use indexing to access elements in the board.
   * You can think of this 1D array as a flattened version of a 2D array, where each row is concatenated sequentially.
   * You can access to the value of an array with the following syntax:
   * `A(3)` for the 3rd element of array A.
   */
   def solveSudoku(board: Array[Int]): IOption[Array[Int]] = {
    val N = 9
    def convertIndex(row: Int, col: Int): Int = row * N + col
    def getElement(board: Array[Int], row: Int, col: Int): Int =
      board(convertIndex(row, col))
    def updateElement(board: Array[Int], row: Int, col: Int, num: Int): Unit =
      board(convertIndex(row, col)) = num

    def findEmptyCell(board: Array[Int], index: Int): IOption[(Int, Int)] = {
      if index >= 81 then INone
      else if board(index) == 0 then
        val row = index / 9
        val col = index % 9
        ISome((row, col))
      else findEmptyCell(board, index + 1)
    }

    def isRowValid(board: Array[Int], row: Int, num: Int, col: Int): Boolean = {
      if col >= 9 then true
      else if getElement(board, row, col) == num then false
      else isRowValid(board, row, num, col + 1)
    }

    def isColValid(board: Array[Int], col: Int, num: Int, row: Int): Boolean = {
      if row >= 9 then true
      else if getElement(board, row, col) == num then false
      else isColValid(board, col, num, row + 1)
    }

    def isBoxValid(board: Array[Int], boxRowStart: Int, boxColStart: Int, num: Int, rowOffset: Int, colOffset: Int): Boolean = {
      if rowOffset >= 3 then true
      else if colOffset >= 3 then isBoxValid(board, boxRowStart, boxColStart, num, rowOffset + 1, 0)
      else if getElement(board, boxRowStart + rowOffset, boxColStart + colOffset) == num then false
      else isBoxValid(board, boxRowStart, boxColStart, num, rowOffset, colOffset + 1)
    }

    def isValid(board: Array[Int], row: Int, col: Int, num: Int): Boolean = {
      isRowValid(board, row, num, 0) &&
      isColValid(board, col, num, 0) &&
      isBoxValid(board, (row / 3) * 3, (col / 3) * 3, num, 0, 0)
    }

    def tryNumbers(board: Array[Int], row: Int, col: Int, num: Int): IOption[Array[Int]] = {
      if num > 9 then INone
      else if isValid(board, row, col, num) then
        updateElement(board, row, col, num)
        solveSudoku(board) match
          case ISome(solution) => ISome(solution)
          case INone =>
            updateElement(board, row, col, 0)
            tryNumbers(board, row, col, num + 1)
      else
        tryNumbers(board, row, col, num + 1)
    }

    def isValidBoard(board: Array[Int], row: Int, col: Int): Boolean = {
      if row >= 9 then true
      else if col >= 9 then isValidBoard(board, row + 1, 0)
      else if getElement(board, row, col) == 0 then
        isValidBoard(board, row, col + 1)
      else
        val currentNum = getElement(board, row, col)
        updateElement(board, row, col, 0)
        val valIsValid = isValid(board, row, col, currentNum)
        updateElement(board, row, col, currentNum)
        if (valIsValid) isValidBoard(board, row, col+1)
        else false
    }

    if (!isValidBoard(board, 0, 0))
      INone
    else
      findEmptyCell(board, 0) match {
        case INone => ISome(board)
        case ISome((row, col)) => tryNumbers(board, row, col, 1)
      }
   }
