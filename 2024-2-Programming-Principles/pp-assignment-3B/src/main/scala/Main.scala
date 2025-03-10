package pp202402.assign3b

import scala.reflect.ClassTag
import pp202402.assign3b.Data._
import pp202402.assign3b.ViewController._
import scala.annotation.tailrec

object Assignment3B:
  /** Principles of Programming: Assignment 3B.
   * This assignment is about implementing a simple game engine that can be used to play two games: Omok and Sudoku.
   *
   * The game engine is implemented using the State design pattern, where the state of the game is represented by `State`.
   * - `State`: contains the current state of the game board and the current player.
   * The game engine also uses the Rule design pattern, where the rules of the game are implemented in `Rule`.
   * - `Rule`: contains the rules of the game, such as the initial message, initial value, and the rules for making a move.
   * The game engine also uses the ViewController design pattern, where the view of the game is implemented in a `ViewController`.
   * - `ViewController`: contains the methods to display the game board and the values on the board.
   * The game engine also uses the GameController class to control the game.
   * - `GameController`: contains the methods to initialize the game, display the game board, and make a move.
   *
   * The game engine is implemented in a generic way, so it can be used to play any game that
   * can be represented by the `State`, `Rule`, and `ViewController` classes.
   * Check `UI.scala` to understand how the game engine is used to play Omok and Sudoku.
   *
   * Fill in the TODOs
   * - in the `OmokState` and `SudokuState` classes to implement the state of the Omok and Sudoku games.
   * - in the `OmokRule` and `SudokuRule` classes to implement the rules of the Omok and Sudoku games.
   * - in the `GameController` class to implement the game controller.
   *
   * You are free to implement more functions inside the classes if needed.
   *
   * There are no TODOs on other files, but you can check them to understand the game engine.
   * We will only extract your `Main.scala` to grade your assignment.
   * Thus, please do not make changes on other files.
   */

  /** Omok game.
   *
   * Omok is a two-player game where the players take turns to fill the board with Os and Xs.
   * - The game board is a 5x5 board.
   * - A player wins if they have 5 consecutive pieces in a row, column, or diagonal.
   * - Player 1 ALWAYS makes the first move when the game starts.
   * - Also, players will take turns to fill the board until one player wins or the board is full without a winner.
   */

  /** Omok state.
   * Here, you should implement the state of the Omok game using the `OmokState` class that extends the `State` class.
   * There are two states in the Omok game:
   * - a board with "O"s, "X"s, and empty cells (" "). Each cell is represented by a string value.
   * - the current player.
   *
   * Also, there are two methods in the `OmokState` class:
   * - `fillCell`: fills the cell at the given row and column with the given value.
   * - `nextTurn`: returns the state that represents the next turn.
   *    If the current player is Player1, the next player is Player2. If the current player is Player2, the next player is Player1.
   * Here, note that we have splitted the stage of updating the board and the stage of updating the player, but
   * a consistent game state requires the all the updates to be done at one turn.
   */
  class OmokState(board: MyArray[String], currentPlayer: Player) extends State[String](board, currentPlayer, 5, 5):
    // TODOs start here //
    def fillCell(i: Int, j: Int, v: String): OmokState = {
      board.update(i * getNCols() + j, v)
      this
    }

    def nextTurn(): OmokState = {
      currentPlayer match
        case Player.Player1 => new OmokState(board, Player.Player2)
        case Player.Player2 => new OmokState(board, Player.Player1)
    }

  /** Omok rule.
   *
   * Here, you should implement the rules of the Omok game using the `OmokRule` class that extends the `Rule` class.
   * We will introduce rules that we are going to check in public/hidden tests.
   * We will not check the rules that are not introduced here.
   */
  class OmokRule extends Rule[String]:
    def initMessage(): String =
      "Omok: Fill the board with Os and Xs. A player wins if they have 5 consecutive pieces in a row, column, or diagonal.\n" +
      "Player 1 is O and Player 2 is X. Player 1 starts first.\n"

    def initValue(): String = " "
    def player1: String = "O"
    def player2: String = "X"

    // TODOs start here //

    // TODO: Implement the renderInput method for the Omok game.
    // Render a input character into a string value. A valid input should be either 'O' or 'X'.
    // If the input is not valid, throw an InvalidInputException.
    def renderInput(input: Char): String = {
      input match
        case 'O' => player1
        case 'X' => player2
        case _ => throw InvalidInputException(s"Invalid input: $input")
    }

    // TODO: Implement the isAvailableValue method for the Omok game.
    // - check1 if the value is valid. A valid value should be either "O" or "X".
    def isAvailableValue(v: String): Boolean = v == player1 || v == player2

    // TODO: Implement the isStateValid method for the Omok game.
    // Given a current state with a board and the current player,
    // check if the state is consistent
    // - hint: think about how many pieces each player can have on the board when they take turns.
    def isStateValid(state: State[String]): Boolean = {
      def countPieces(row: Int, col: Int, pieceCnt1: Int, pieceCnt2: Int): (Int, Int) = {
        if (col >= state.getNCols()) (pieceCnt1, pieceCnt2)
        else if (row >= state.getNRows()) countPieces(0, col + 1, pieceCnt1, pieceCnt2)
        else state.getCell(row, col) match
          case "O" => countPieces(row + 1, col, pieceCnt1 + 1, pieceCnt2)
          case "X" => countPieces(row + 1, col, pieceCnt1, pieceCnt2 + 1)
          case _ => countPieces(row + 1, col, pieceCnt1, pieceCnt2)
      }
      val (pieceCnt1, pieceCnt2) = countPieces(0, 0, 0, 0)
      if (state.getPlayer() == Player.Player1 && pieceCnt1 != pieceCnt2) false
      else if (state.getPlayer() == Player.Player2 && pieceCnt1 != pieceCnt2 + 1) false
      else true
    }

    // TODO: Implement the isNextMoveValid method for the Omok game.
    // Given a current state with a board, the current player,
    // and the next move (i, j, v) this player wants to make,
    // check if the next move is valid.
    // - check1: you cannot put a piece on a cell that is already filled.
    // - check2: you cannot put a piece on a cell if the game is already done.
    def isNextMoveValid(state: State[String], i: Int, j: Int, v: String): Boolean = {
      def isPlayerValid(player: Player): Boolean = {
        if (player == Player.Player1) v == player1
        else if (player == Player.Player2) v == player2
        else false
      }
      state.getCell(i, j) == " " && isAvailableValue(v) && isPlayerValid(state.getPlayer()) && !isDone(state)
    }

    // TODO: Implement the isDone method for the Omok game.
    // Given a current state with a board, check if the game is done.
    // The game is done if one player wins or the board is full.
    def isDone(state: State[String]): Boolean = {
      // Full Check
      def isBoardFull(row: Int, col: Int): Boolean = {
        if (col >= state.getNCols()) true
        else if (state.getCell(row, col) == initValue()) false
        else if (row >= state.getNRows()) isBoardFull(0, col + 1)
        else isBoardFull(row, col + 1)
      }

      // Win Check
      def isWin(state: State[String]): Boolean = {
        def checkLine(row: Int, col: Int, dRow: Int, dCol: Int, count: Int, value: String): Boolean = {
          if (count == 5) true
          else if (row < 0 || row >= state.getNRows() || col < 0 || col >= state.getNCols()) false
          else if (state.getCell(row, col) != value) false
          else checkLine(row + dRow, col + dCol, dRow, dCol, count + 1, value)
        }

        def checkAllDirections(row: Int, col: Int): Boolean = {
          val value = state.getCell(row, col)
          if (value == initValue()) false
          else {
            checkLine(row, col, 0, 1, 0, value) || // 가로
            checkLine(row, col, 1, 0, 0, value) || // 세로
            checkLine(row, col, 1, 1, 0, value) || // 대각선 (우하향)
            checkLine(row, col, 1, -1, 0, value)   // 대각선 (좌하향)
          }
        }

        def checkBoard(row: Int, col: Int): Boolean = {
          if (col >= state.getNCols()) false
          else if (row >= state.getNRows()) checkBoard(0, col + 1)
          else if (checkAllDirections(row, col)) true
          else checkBoard(row + 1, col)
        }

        checkBoard(0, 0)
      }
      isBoardFull(0, 0) || isWin(state)
    }

  /** Sudoku game.
   *
   * Sudoku is a single-player game where the player fills the board with numbers from 1 to 9.
   * - The game board is a 9x9 board.
   * - No number can be repeated in the same row, column, or 3x3 subgrid.
   * - The game is done when the board is full and the rules are satisfied.
   * - The player do not have to check if the initial board is can be solved or not.
   */

  /** Sudoku state.
   * Here, you should implement the state of the Sudoku game using the `SudokuState` class that extends the `State` class.
   * There are two states in the Sudoku game:
    * - a board with numbers from 1 to 9 and empty cells. Each cell is represented by a `SudokuCell` value.
    * - the current player.
    *
    * Also, there are two methods in the `SudokuState` class:
    * - `fillCell`: fills the cell at the given row and column with the given value.
    * - `nextTurn`: returns the state that represents the next turn.
    */
  class SudokuState(board: MyArray[SudokuCell], currentPlayer: Player) extends State[SudokuCell](board, currentPlayer, 9, 9):
    def fillCell(i: Int, j: Int, v: SudokuCell): SudokuState = {
      board.update(i * getNCols() + j, v)
      this
    }

    def nextTurn(): SudokuState = {
      new SudokuState(board, currentPlayer)
    }

  /** Sudoku rule.
   *
   * Here, you should implement the rules of the Sudoku game using the `SudokuRule` class that extends the `Rule` class.
   * We will introduce rules that we are going to check in public/hidden tests.
   * We will not check the rules that are not introduced here.
   */
  class SudokuRule extends Rule[SudokuCell]:
    def initMessage(): String = "Sudoku: Fill the board with numbers from 1 to 9.\n" +
      "No number can be repeated in the same row, column, or 3x3 subgrid.\n"
    def initValue(): SudokuCell = SudokuCell.Empty

    // TODOs start here

    // TODO: Implement the renderInput method for the Sudoku game.
    // Render a input character into a SudokuCell value. A valid input should be a character from '1' to '9'.
    // If the input is not valid, throw an InvalidInputException.
    // You can assume that only player input will be passed to this method. (No fixed values)
    def renderInput(input: Char): SudokuCell = {
      input match
        case '1' => SudokuCell.UserValue(1)
        case '2' => SudokuCell.UserValue(2)
        case '3' => SudokuCell.UserValue(3)
        case '4' => SudokuCell.UserValue(4)
        case '5' => SudokuCell.UserValue(5)
        case '6' => SudokuCell.UserValue(6)
        case '7' => SudokuCell.UserValue(7)
        case '8' => SudokuCell.UserValue(8)
        case '9' => SudokuCell.UserValue(9)
        case _ => throw InvalidInputException(s"Invalid input: $input")
    }

    // TODO: Implement the isAvailableValue method for the Sudoku game.
    // Determine if the input SudokuCell is available to put on the board.
    // - check1: a player is allowed to put a number from 1 to 9.
    // - check2: a player is allowed to put an empty cell.
    def isAvailableValue(v: SudokuCell): Boolean = {
      def isValidValue(value: Int): Boolean = value >= 1 && value <= 9
      v match
        case SudokuCell.Empty => true
        case SudokuCell.UserValue(value) => isValidValue(value)
        case _ => false
    }

    // TODO: Implement the isStateValid method for the Sudoku game.
    // Given a current state with a board and the current player,
    // check if the state is consistent
    // - check1: No number can be repeated in the same row, column, or 3x3 subgrid.
    def isStateValid(state: State[SudokuCell]): Boolean = {

      def isBoardValid(): Boolean = {
        isRowsValid(0) && isColsValid(0) && isGridsValid(0, 0)
      }

      def isRowsValid(row: Int): Boolean = {
        if (row >= state.getNRows()) true
        else if (!isRowValid(row)) false
        else isRowsValid(row + 1)
      }

      def isColsValid(col: Int): Boolean = {
        if (col >= state.getNCols()) true
        else if (!isColValid(col)) false
        else isColsValid(col + 1)
      }

      def isGridsValid(startRow: Int, startCol: Int): Boolean = {
        if (startRow >= state.getNRows()) true
        else if (startCol >= state.getNCols()) isGridsValid(startRow + 3, 0)
        else if (!isGridValid(startRow, startCol)) false
        else isGridsValid(startRow, startCol + 3)
      }

      def isRowValid(row: Int): Boolean = {
        def checkNumber(num: Int): Boolean = {
          if (num > 9) true
          else {
            val count = countInRow(row, 0, num)
            if (count > 1) false
            else checkNumber(num + 1)
          }
        }

        def countInRow(row: Int, col: Int, target: Int): Int = {
          if (col >= state.getNCols()) 0
          else state.getCell(row, col) match {
            case SudokuCell.Empty => countInRow(row, col + 1, target)
            case SudokuCell.FixedValue(v) =>
              if (v == target) 1 + countInRow(row, col + 1, target)
              else countInRow(row, col + 1, target)
            case SudokuCell.UserValue(v) =>
              if (v == target) 1 + countInRow(row, col + 1, target)
              else countInRow(row, col + 1, target)
          }
        }

        checkNumber(1)
      }

      def isColValid(col: Int): Boolean = {
        def checkNumber(num: Int): Boolean = {
          if (num > 9) true
          else {
            val count = countInCol(0, col, num)
            if (count > 1) false
            else checkNumber(num + 1)
          }
        }

        def countInCol(row: Int, col: Int, target: Int): Int = {
          if (row >= state.getNRows()) 0
          else state.getCell(row, col) match {
            case SudokuCell.Empty => countInCol(row + 1, col, target)
            case SudokuCell.FixedValue(v) =>
              if (v == target) 1 + countInCol(row + 1, col, target)
              else countInCol(row + 1, col, target)
            case SudokuCell.UserValue(v) =>
              if (v == target) 1 + countInCol(row + 1, col, target)
              else countInCol(row + 1, col, target)
          }
        }

        checkNumber(1)
      }

      def isGridValid(startRow: Int, startCol: Int): Boolean = {
        def checkNumber(num: Int): Boolean = {
          if (num > 9) true
          else {
            val count = countInGrid(startRow, startCol, num)
            if (count > 1) false
            else checkNumber(num + 1)
          }
        }

        def countInGrid(startRow: Int, startCol: Int, target: Int): Int = {
          def checkCell(row: Int, col: Int): Int = {
            if (row >= startRow + 3) 0
            else if (col >= startCol + 3) checkCell(row + 1, startCol)
            else state.getCell(row, col) match {
              case SudokuCell.Empty => checkCell(row, col + 1)
              case SudokuCell.FixedValue(v) =>
                if (v == target) 1 + checkCell(row, col + 1)
                else checkCell(row, col + 1)
              case SudokuCell.UserValue(v) =>
                if (v == target) 1 + checkCell(row, col + 1)
                else checkCell(row, col + 1)
            }
          }
          checkCell(startRow, startCol)
        }

        checkNumber(1)
      }

      isBoardValid()
    }

    // TODO: Implement the isNextMoveValid method for the Sudoku game.
    // Given a current state with a board, the current player,
    // and the next move (i, j, v) this player wants to make,
    // check if the next move is valid.
    // - check1: you cannot put a number on a cell that is already fixed.
    // - check2: you cannot put a number on a cell if the game is already done.
    def isNextMoveValid(state: State[SudokuCell], i: Int, j: Int, v: SudokuCell): Boolean = {
      val cell = state.getCell(i, j)
      if (!isStateValid(state.fillCell(i, j, v)))
      {
        state.fillCell(i, j, cell)
        false
      }
      else
      {
        // check1 & check2
        state.getCell(i, j) match
          case SudokuCell.FixedValue(_) => false
          case _ => isAvailableValue(v) && !isDone(state)
      }
    }

    // TODO: Implement the isDone method for the Sudoku game.
    // Given a current state with a board, check if the game is done.
    // - check1: The game is done if the board is full and the rules are satisfied.
    def isDone(state: State[SudokuCell]): Boolean = {
      // check isfull

      def isBoardFull(row: Int, col: Int): Boolean = {
        if (col >= state.getNCols()) true
        else if (row >= state.getNRows()) isBoardFull(0, col + 1)
        else if (state.getCell(row, col) == SudokuCell.Empty) false
        else isBoardFull(row + 1, col)
      }
      isBoardFull(0, 0)
    }

  /** Game controller.
   * Implement the game controller using the `GameController` class.
   */
  class GameController[A: ClassTag](rule: Rule[A], state: State[A], viewController: ViewController[A]):
    def init(): String = rule.initMessage()

    def display(): String = viewController.displayBoardState(state)

    def isDone(): Boolean = rule.isDone(state)

    // TODO starts here //

    // TODO: Implement the makeMove method for the game controller.
    // Given the row, column, and value (Char) the player wants to put on the board,
    // check if the move is valid and update the game state.
    // If the move is not valid, throw an InvalidMoveException.
    def makeMove(i: Int, j: Int, v: Char): GameController[A] = {
      if (!rule.isStateValid(state)) throw InvalidMoveException(s"Invalid move: ($i, $j, $v)")
      else if (!rule.isNextMoveValid(state, i, j, rule.renderInput(v))) throw InvalidMoveException(s"Invalid move: ($i, $j, $v)")
      else
        new GameController(rule, state.fillCell(i, j, rule.renderInput(v)).nextTurn(), viewController)
    }
