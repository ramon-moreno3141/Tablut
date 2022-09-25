package tablut;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Formatter;

import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Move.mv;


/** The state of a Tablut Game.
 *  @author Ramon Moreno
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
        NTHRONE = sq(4, 5),
        STHRONE = sq(4, 3),
        WTHRONE = sq(3, 4),
        ETHRONE = sq(5, 4);

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }

        squarePieceHashMap = new HashMap<>(model.squarePieceMap());
        _previousPositions = new HashSet<>(model.previousPositions());
        _undoStack = new Stack<>();
        _undoStack.addAll(model.undoStack());
        _moveLim = model.moveLimit();
        _moveCount = model.moveCount();
        _repeated = model.repeatedPosition();
        _turn = model.turn();
        _winner = model.winner();
    }

    /** Clears the board to the initial position. */
    void init() {
        _moveLim = 0;
        _moveCount = 0;
        _repeated = false;

        _turn = BLACK;
        _winner = EMPTY;

        squarePieceHashMap = new HashMap<>();
        _previousPositions = new HashSet<>();
        _undoStack = new Stack<>();

        _attackers = new ArrayList<Square>(Arrays.asList(INITIAL_ATTACKERS));
        _defenders = new ArrayList<Square>(Arrays.asList(INITIAL_DEFENDERS));

        for (Square x : SQUARE_LIST) {
            if (x == THRONE) {
                squarePieceHashMap.put(x, KING);
            } else if (_defenders.contains(x)) {
                squarePieceHashMap.put(x, WHITE);
            } else if (_attackers.contains(x)) {
                squarePieceHashMap.put(x, BLACK);
            } else {
                squarePieceHashMap.put(x, EMPTY);
            }
        }

        _previousPositions.add(new HashMap<>(squarePieceHashMap));
    }

    /** Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     *  @param n Move limit. */
    void setMoveLimit(int n) {
        if (2 * n <= moveCount()) {
            throw new IllegalArgumentException("(MoveLimit * 2) must"
                    + " be greater than the total move count.");
        } else {
            _moveLim = n;
        }
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        if (_winner == EMPTY) {
            return null;
        }
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (!_previousPositions.add(new HashMap<>(squarePieceHashMap))) {
            _repeated = true;
            _winner = turn().opponent();
        }
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        for (Square sq : squarePieceHashMap.keySet()) {
            if (get(sq) == KING) {
                _theKingSquare = sq;
            }
        }
        return _theKingSquare;
    }

    /** Return the move limit of the current board. */
    int moveLimit() {
        return _moveLim;
    }

    /** Return the HashMap used to represent the board. */
    HashMap<Square, Piece> squarePieceMap() {
        return squarePieceHashMap;
    }

    /** Return the HashSet used to record previous positions
     *  of the board. */
    HashSet<HashMap<Square, Piece>> previousPositions() {
        return _previousPositions;
    }

    /** Return the Stack used in order to undo moves. */
    Stack<HashMap<Square, Piece>> undoStack() {
        return _undoStack;
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        return squarePieceHashMap.get(sq(col, row));
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        squarePieceHashMap.put(s, p);
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        _undoStack.push(new HashMap<>(squarePieceHashMap));
        put(p, s);
    }

    /** Record current state of the board for undoing. */
    final void record() {
        _undoStack.push(new HashMap<>(squarePieceHashMap));
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        int fromRow = from.row();
        int fromCol = from.col();
        int toRow = to.row();
        int toCol = to.col();

        int distance;
        int direction = from.direction(to);
        Square oneSquare;
        Move rookMove = mv(from, to);

        if (ROOK_MOVES[from.index()][direction].contains(rookMove)) {
            if (direction == 0 || direction == 2) {
                distance = Math.abs(toRow - fromRow);
            } else {
                distance = Math.abs(toCol - fromCol);
            }

            for (int i = 1; i <= distance; i++) {
                oneSquare = from.rookMove(direction, i);
                if (squarePieceHashMap.get(oneSquare) != EMPTY) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /** Return true iff FROM-TO is a valid move. */
    boolean isLegal(Square from, Square to) {
        if (get(from) == EMPTY) {
            return false;
        }

        if (to == THRONE) {
            if (squarePieceHashMap.get(from) != KING) {
                return false;
            }
        }

        return from.isRookMove(to) && isUnblockedMove(from, to);
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        if (!isLegal(from, to)) {
            return;
        }
        if (!isLegal(from)) {
            return;
        }
        if (!hasMove(_turn)) {
            _winner = _turn.opponent();
            return;
        }

        Piece fromPiece = get(from);
        Square rookSq;
        record();
        put(EMPTY, from);
        put(fromPiece, to);

        for (int dir = 0; dir < 4; dir++) {
            rookSq = to.rookMove(dir, 2);
            if (rookSq != null) {
                capture(to, rookSq);
            }
        }
        checkRepeated();

        _moveCount++;
        if (_winner == EMPTY && _moveLim != 0 && (_moveCount > _moveLim)) {
            _winner = _turn.opponent();
        }

        if (fromPiece == KING && to.isEdge() && _winner == EMPTY) {
            _winner = _turn;
        }
        _turn = _turn.opponent();
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0, Square sq2) {
        Square sqBetween = sq0.between(sq2);
        Piece pBetween = get(sqBetween);
        Piece sq0Side = get(sq0).side();
        Piece sq2Side = get(sq2).side();

        Square rookSquare;
        Piece rookSquarePiece;
        int blPieceCount = 0;
        int emptyCount = 0;

        if (pBetween == KING) {
            if (sqBetween == THRONE || sqBetween == NTHRONE
                    || sqBetween == ETHRONE || sqBetween == STHRONE
                    || sqBetween == WTHRONE) {
                for (int dir = 0; dir < 4; dir++) {
                    rookSquare = sqBetween.rookMove(dir, 1);
                    rookSquarePiece = get(rookSquare);
                    if (rookSquarePiece == EMPTY) {
                        emptyCount++;
                    } else if (rookSquarePiece == BLACK) {
                        blPieceCount++;
                    }
                }

                if ((blPieceCount == 4 && sqBetween == THRONE)
                        || (blPieceCount == 3 && emptyCount == 1
                                && sqBetween != THRONE)) {
                    put(EMPTY, sqBetween);
                    _winner = BLACK;
                }

            } else if (sq0Side == BLACK && sq2Side == BLACK) {
                put(EMPTY, sqBetween);
                _winner = BLACK;
            }
        } else if (pBetween == BLACK) {
            if ((sq0Side == WHITE && sq2Side == WHITE)
                    || (sq0Side == WHITE && sq2Side == EMPTY
                    && sq2 == THRONE)) {
                put(EMPTY, sqBetween);
            }
        } else if (pBetween == WHITE) {
            if ((sq0Side == BLACK && sq2Side == BLACK)
                    || (sq0Side == BLACK && sq2Side == EMPTY && sq2 == THRONE)
                    || (sq0Side == BLACK && sq2Side == WHITE
                            && sq2 == THRONE && surrounded(sq2))) {
                put(EMPTY, sqBetween);
            }
        }

    }

    /** Returns true if THRONE is surrounded by 3 BLACK pieces. */
    boolean surrounded(Square throne) {
        Square rookSquare;
        Piece rookSquarePiece;
        int blCount = 0;
        for (int dir = 0; dir < 4; dir++) {
            rookSquare = throne.rookMove(dir, 1);
            rookSquarePiece = get(rookSquare);
            if (rookSquarePiece == BLACK) {
                blCount++;
            }
        }
        return blCount == 3;
    }

    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
            squarePieceHashMap = _undoStack.pop();
            _turn = _turn.opponent();
            _moveCount--;
            _winner = EMPTY;
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        if (!_repeated && _moveCount != 0) {
            _previousPositions.remove(squarePieceHashMap);
        }
        _repeated = false;
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        _undoStack.clear();
        _moveCount = 0;
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        ArrayList<Move> legalMoves = new ArrayList<>();

        for (Square x : pieceLocations(side)) {
            for (int d = 0; d <= 3; d++) {
                for (Square rookSquare : ROOK_SQUARES[x.index()][d]) {
                    if (isLegal(x, rookSquare)) {
                        legalMoves.add(mv(x, rookSquare));
                    }
                }
            }
        }

        return legalMoves;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {
        return legalMoves(side).size() > 0;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        HashSet<Square> setOfSquares = new HashSet<>();

        for (Square x : squarePieceHashMap.keySet()) {
            if (get(x).side() == side) {
                setOfSquares.add(x);
            }
        }

        return setOfSquares;
    }

    /** Return the number of pieces on SIDE. */
    int pieces(Piece side) {
        HashSet<Square> locations = pieceLocations(side);
        return locations.size();
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or EMPTY if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;

    /** The square where the king is located. */
    private Square _theKingSquare;

    /** An int representing the move limit for the game. */
    private int _moveLim;

    /** An ArrayList of the attackers of the board. */
    private ArrayList<Square> _attackers;

    /** An ArrayList of the defenders of the board. */
    private ArrayList<Square>  _defenders;

    /** A HashMap that keeps track of the state of the board
     *  through Square and Piece mappings. */
    private HashMap<Square, Piece> squarePieceHashMap;

    /** A HashSet that keeps track of previous board positions. */
    private HashSet<HashMap<Square, Piece>> _previousPositions;

    /** A stack that keeps track of the state of the board. */
    private Stack<HashMap<Square, Piece>> _undoStack;

}
