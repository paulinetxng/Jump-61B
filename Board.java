package jump61;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.ArrayDeque;

import java.util.function.Consumer;

import static jump61.Side.*;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Pauline Tang
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _size = N;
        _Board = new Square[N][N];
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                _Board[r][c] = Square.INITIAL;
            }
        }
        _numSquares = N * N;
        _currPlayer = RED;
        _current = 0;
        _history = new ArrayList<GameState>();
        _history.add(new GameState());
        _history.get(0).saveState();
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        this.copy(board0);
        _readonlyBoard = new ConstantBoard(this);
        _numSquares = _size * _size;
        _currPlayer = RED;
        _current = 0;
    }

    /** Board.
     * @return _board */
    Square[][] board() {
        return _Board;
    }

    /** Num of squares on board.
     * @return _numSquares */
    int numSquares() {
        return _numSquares;
    }

    /** Current move count.
     * @return _current */
    int numMoves() {
        return _current;
    }

    /** Current player.
     * @return _currPlayer*/
    Side currPlayer() {
        return _currPlayer;
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        announce();
        _size = N;
        _Board = new Square[N][N];
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                _Board[r][c] = Square.INITIAL;
            }
        }
        _numSquares = N * N;
        _current = 0;
        _history.clear();
        _history.add(new GameState());
        _history.get(0).saveState();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        internalCopy(board);
        _current = 0;
        _size = board.size();
        _history.clear();
        _history.add(new GameState());
        _history.get(_current).saveState();
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        int i = 0;
        for (int r = 0; r < _size; r++) {
            for (int c = 0; c < _size; c++) {
                _Board[r][c] = board.get(i);
                i += 1;
            }
        }
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        int r = n / _size;
        int c = n % _size;
        return _Board[r][c];
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int numSpots = 0;
        for (int r = 0; r < _size; r++) {
            for (int c = 0; c < _size; c++) {
                numSpots += _Board[r][c].getSpots();
            }
        }
        return numSpots;
    }

    /** Returns the total number of RED spots on the board. */
    int numPiecesRed() {
        int numSpots = 0;
        int i = 0;
        for (int r = 0; r < _size; r++) {
            for (int c = 0; c < _size; c++) {
                if (get(i).getSide() == RED) {
                    numSpots += _Board[r][c].getSpots();
                }
                i += 1;
            }
        }
        return numSpots;
    }

    /** Returns the total number of Blue spots on the board. */
    int numPiecesBlue() {
        int numSpots = 0;
        int i = 0;
        for (int r = 0; r < _size; r++) {
            for (int c = 0; c < _size; c++) {
                if (get(i).getSide() == BLUE) {
                    numSpots += _Board[r][c].getSpots();
                }
                i += 1;
            }
        }
        return numSpots;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        Square sq = get(n);
        return sq.getSide().equals(WHITE) || sq.getSide().equals(player);
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return _currPlayer == player;
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        Side player = get(0).getSide();
        for (int i = 1; i < _numSquares; i++) {
            Square sq = get(i);
            if (!sq.getSide().equals(player) || player == WHITE) {
                return null;
            }
        }
        return player;
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int numSide = 0;
        for (int i = 0; i < _numSquares; i++) {
            Square sq = get(i);
            if (sq.getSide() == side) {
                numSide += 1;
            }
        }
        return numSide;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        _currPlayer = player;
        int r = n / _size;
        int c = n % _size;
        Square sq = get(n);
        int currSpots = sq.getSpots();
        if (sq.getSide() == WHITE) {
            sq = Square.square(player, 2);
            _Board[r][c] = sq;
        } else if (getWinner() == null) {
            internalSet(n, 1 + currSpots, player);
            if (overfull(n)) {
                jump(n);
            }
        }
        GameState gs = new GameState();
        _current += 1;
        _history.add(gs);
        _history.get(_current).saveState();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        Side turn = player;
        Square sq = Square.square(turn, num);
        int r = n / _size;
        int c = n % _size;
        _Board[r][c] = sq;
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (_current > 0) {
            _current -= 1;
            _history.get(_current).restoreState();
        }
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        _history.add(new GameState());
        _history.get(_current).saveState();
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        int r = S / _size;
        int c = S % _size;
        Square sq = get(S);
        int numSpots = sq.getSpots();
        internalSet(S, numSpots - neighbors(S), _currPlayer);
        for (int i = 0; i < neighborsOf(S).size(); i++) {
            addSpotInner(_currPlayer, neighborsOf(S).get(i));
        }
    }

    /** Helper method for adding a spot to neighbors for JUMP.
     * Adds a spot from PLAYER at square #N. */
    private void addSpotInner(Side player, int n) {
        _currPlayer = player;
        int r = n / _size;
        int c = n % _size;
        Square sq = get(n);
        int currSpots = sq.getSpots();
        if (getWinner() == null) {
            internalSet(n, currSpots + 1, player);
            if (overfull(n)) {
                jump(n);
            }
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        String sep;
        out.format("===\n");
        for (int r = 1; r < _size + 1; r++) {
            out.format("    ");
            sep = "";
            for (int c = 1; c < _size + 1; c++) {
                String playr;
                Side turn = get(r, c).getSide();
                if (turn == WHITE) {
                    playr = "-";
                } else if (turn == RED) {
                    playr = "r";
                } else {
                    playr = "b";
                }
                String spots = String.valueOf(get(r, c).getSpots());
                out.format(sep + spots + playr);
                sep = " ";
            }
            if (r != _size) {
                out.format("\n");
            }
        }
        out.format("\n===");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            if (_size != B._size) {
                return false;
            }
            for (int r = 1; r < _size + 1; r++) {
                for (int c = 1; c < _size + 1; c++) {
                    if (get(r, c).getSide() != B.get(r, c).getSide()
                            && get(r, c).getSpots() != B.get(r, c).getSpots()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** Returns an ArrayList of ints of the neighbors of N. */
    public ArrayList<Integer> neighborsOf(int N) {
        int r = N / _size;
        int c = N % _size;
        ArrayList<Integer> neigh = new ArrayList<Integer>();
        if (r > 0) {
            neigh.add(N - _size);
        }
        if (c > 0) {
            neigh.add(N - 1);
        }
        if (r < _size - 1) {
            neigh.add(N + _size);
        }
        if (c < _size - 1) {
            neigh.add(N + 1);
        }
        return neigh;
    }

    /** Next Player.
     * @return side */
    public Side nextPlayer() {
        if (_currPlayer == RED) {
            _currPlayer = BLUE;
            return BLUE;
        } else {
            _currPlayer = RED;
            return RED;
        }
    }

    /** Returns TRUE iff the square at N is overfull. */
    public boolean overfull(int n) {
        Square sq = get(n);
        int spots = sq.getSpots();
        return spots > neighbors(n);
    }

    /** Class that records the current gamestate of the board. */
    private class GameState {
        /** Creates a savestate. */
        GameState() {
            _savedBoard = new Square[_size][_size];
        }

        /** Initialize to the current state of the Model. */
        void saveState() {
            for (int r = 0; r < _size; r++) {
                for (int c = 0; c < _size; c++) {
                    _savedBoard[r][c] = _Board[r][c];
                }
            }
        }

        /** Restore the current Model's state from our saved state. */
        void restoreState() {
            for (int r = 0; r < _size; r++) {
                for (int c = 0; c < _size; c++) {
                    _Board[r][c] = _savedBoard[r][c];
                }
            }
        }

        /** Contents of board. */
        private Square[][] _savedBoard;
    }

    /** Board. */
    private Square[][] _Board;

    /** Size of board. */
    private int _size;

    /** Num of squares on board. */
    private int _numSquares;

    /** History. */
    private ArrayList<GameState> _history;

    /** Current move count. */
    private int _current;

    /** Current player. */
    private Side _currPlayer;
}
