package tablut;

import java.util.List;

import static java.lang.Math.*;

import static tablut.Piece.*;

/** A Player that automatically generates moves.
 *  @author Ramon Moreno
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;
    /** A big integer increase factor. */
    private static final int BIG = 100000;
    /** A not as big integer increase factor. */
    private static final int LESSBIG = 10000;
    /** The number 50 used for piece weights. */
    private static final int FIFTY = 50;
    /** Required number of moves to search to depth 5. */
    private static final int D5 = 52;
    /** Required number of moves to search to depth 4. */
    private static final int D4 = 42;
    /** Required number of moves to search to depth 3. */
    private static final int D3 = 32;
    /** Required number of moves to search to depth 2. */
    private static final int D2 = 22;


    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        return findMove().toString();
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        _lastFoundMove = null;
        int theSense;
        if (myPiece() == WHITE) {
            theSense = 1;
        } else {
            theSense = -1;
        }
        int largestScore = findMove(b, maxDepth(b), true,
                theSense, -INFTY, INFTY);
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (depth == 0 || board.winner() != null) {
            return staticScore(board);
        }
        int bestScore = (sense == 1) ? -INFTY : INFTY;
        int opponentSense = -sense;
        Piece mySide = (sense == 1) ? WHITE : BLACK;
        Move tempMove = null;
        List<Move> legalMoves = board.legalMoves(mySide);
        for (Move x : legalMoves) {
            board.makeMove(x);
            if (board.repeatedPosition()) {
                board.undo();
                continue;
            }
            int opponentR = findMove(board, depth - 1, false,
                    opponentSense, alpha, beta);
            board.undo();
            if ((mySide == BLACK && opponentR <= bestScore)
                    || (mySide == WHITE && opponentR >= bestScore)) {
                bestScore = opponentR;
                if (mySide == WHITE) {
                    alpha = max(alpha, opponentR);
                } else {
                    beta = min(beta, opponentR);
                }
                if (saveMove) {
                    tempMove = x;
                }
                if (beta <= alpha) {
                    return bestScore;
                }
            }
        }
        _lastFoundMove = tempMove;
        return bestScore;
    }

    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        int moveCount = board.moveCount();
        if (moveCount > D5) {
            return 5;
        } else if (moveCount > D4) {
            return 4;
        } else if (moveCount > D3) {
            return 3;
        } else if (moveCount > D2) {
            return 2;
        } else {
            return 1;
        }
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int score = 0;
        if (kingDistance(board) == 0) {
            score = WINNING_VALUE;
            return score;
        } else {
            score = score + ((4 - kingDistance(board)) * LESSBIG);
        }
        if (board.pieces(BLACK) > board.pieces(WHITE)) {
            score = score - (FIFTY * 1000);
        } else if (board.pieces(BLACK) < board.pieces(WHITE)) {
            score = score + (FIFTY * 1000);
        }
        if (board.kingPosition() == null) {
            return -WINNING_VALUE;
        }
        for (Square x : board.pieceLocations(BLACK)) {
            if (x.adjacent(board.kingPosition())) {
                score = score - BIG;
            }
        }
        int col = board.kingPosition().col();
        int row = board.kingPosition().row();

        if (board.get(col, row - 1) == EMPTY) {
            score = score + BIG;
        }
        if (board.get(col - 1, row) == EMPTY) {
            score = score + BIG;
        }
        if (board.get(col, row + 1) == EMPTY) {
            score = score + BIG;
        }
        if (board.get(col + 1, row) == EMPTY) {
            score = score + BIG;
        }
        return score;
    }

    /** Returns how far away the king is from the
     * edge of the board.
     * @param board A board.*/
    private int kingDistance(Board board) {
        int startingDist = 5;
        Square theKing = board.kingPosition();
        if (theKing != null) {
            int[] distances = {
                    Math.abs(0 - theKing.col()),
                    Math.abs(8 - theKing.col()),
                    Math.abs(0 - theKing.row()),
                    Math.abs(8 - theKing.row())};

            for (int x : distances) {
                startingDist = min(startingDist, x);
            }
        }
        return startingDist;
    }

}
