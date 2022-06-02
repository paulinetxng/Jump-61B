package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board work = new Board(getBoard());
        int value;
        assert getSide() == work.whoseMove();
        _foundMove = -1;
        if (getSide() == RED) {
            minMax(work,  4, true, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else {
            minMax(work,  4, true, -1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return _foundMove;
    }


    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        ArrayList<Integer> moves = new ArrayList<Integer>();
        for (int i = 0; i < board.numSquares(); i++) {
            if (board.isLegal(getSide(), i)) {
                moves.add(i);
            }
        }
        int score;
        int bestSoFar;
        if (sense == 1) {
            if (board.getWinner() != null || depth == 0) {
                return staticEval(board, 100);
            }
            bestSoFar = alpha;
            for (int m : moves) {
                board.addSpot(board.whoseMove(), m);
                score = minMax(board, depth - 1, false, -1, alpha, beta);
                if (score > bestSoFar) {
                    bestSoFar = score;
                    alpha = Math.max(alpha, bestSoFar);
                    if (saveMove) {
                        _foundMove = m;
                    }
                    if (alpha >= beta) {
                        return bestSoFar;
                    }
                }
                board.undo();
            }
        } else {
            if (board.getWinner() != null || depth == 0) {
                return staticEval(board, 100);
            }
            bestSoFar = beta;
            for (int m : moves) {
                board.addSpot(board.whoseMove(), m);
                score = minMax(board, depth - 1, false, 1, alpha, beta);
                if (score < bestSoFar) {
                    bestSoFar = score;
                    beta = Math.min(beta, bestSoFar);
                    if (saveMove) {
                        _foundMove = m;
                    }
                    if (alpha >= beta) {
                        return bestSoFar;
                    }
                }
                board.undo();
            }
        }
        return bestSoFar;
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        if (b.getWinner() == RED) {
            return winningValue;
        } else if (b.getWinner() == BLUE) {
            return -winningValue;
        } else {
            int heuristic;
            int corner = 0;
            int side = 0;
            int inside = 0;
            if (getSide() == RED) {
                for (int i = 0; i < b.size(); i++) {
                    if (b.get(i).getSide() == RED) {
                        if (i == 0 || i == b.size() || i == b.numSquares() - 1
                                || i == b.numSquares() - b.size()) {
                            corner += 3;
                        } else if (b.row(i) == 1 || b.row(i) == b.size()
                                || b.col(i) == 1 || b.col(i) == b.size()) {
                            side += 2;
                        } else {
                            inside += 1;
                        }
                    }
                }
                heuristic = b.numPiecesRed() * (corner + side + inside);
            } else {
                for (int i = 0; i < b.size(); i++) {
                    if (b.get(i).getSide() == BLUE) {
                        if (i == 0 || i == b.size() || i == b.numSquares() - 1
                                || i == b.numSquares() - b.size()) {
                            corner -= 3;
                        } else if (b.row(i) == 1 || b.row(i) == b.size()
                                || b.col(i) == 1 || b.col(i) == b.size()) {
                            side -= 2;
                        } else {
                            inside -= 1;
                        }
                    }
                }
                heuristic = b.numPiecesBlue() * (corner + side + inside);
            }
            return heuristic;
        }
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;
}
