package cz.cvut.fel.arimaa.model;

import cz.cvut.fel.arimaa.types.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.logging.Logger;

import static cz.cvut.fel.arimaa.types.Square.getSquare;

public class Board {

    public static final int WIDTH = 8;
    public static final int HEIGHT = 8;

    static final String EMPTY_BOARD
            = """
             +-----------------+
            8|                 |
            7|                 |
            6|     x     x     |
            5|                 |
            4|                 |
            3|     x     x     |
            2|                 |
            1|                 |
             +-----------------+
               a b c d e f g h""";

    static final String DEFAULT_BOARD
            = """
             +-----------------+
            8| r r r r r r r r |
            7| d h c e m c h d |
            6|     x     x     |
            5|                 |
            4|                 |
            3|     x     x     |
            2| D H C E M C H D |
            1| R R R R R R R R |
             +-----------------+
               a b c d e f g h""";

    private static final List<Square> TRAPS = List.of(getSquare("c3"),
            getSquare("f3"), getSquare("c6"), getSquare("f6"));

    private static final Logger logger = Logger.getLogger(Board.class.getName());

    private Piece[][] board;
    private ObservableList<Move> moves;

    Board() {
        board = new Piece[WIDTH][HEIGHT];
        moves = FXCollections.observableArrayList();
    }

    public static boolean isTrap(Square pos) {
        return TRAPS.contains(pos);
    }

    void load() {
        load(DEFAULT_BOARD);
    }

    boolean undoStep() {
        Move lastMove = getLastMove();
        if (moves.size() <= 3 && lastMove.getNumberOfSteps() <= 0) {
            return false;
        }

        if (lastMove.getNumberOfSteps() <= 0) {
            moves.remove(moves.size() - 1);
            lastMove = getLastMove();
        }

        Step lastStep = lastMove.getStep(lastMove.getNumberOfSteps() - 1);

        if (lastStep.removed) {
            Square from = lastStep.from;
            board[from.x][from.y] = lastStep.piece;
            lastMove.removeLastStep();
            lastStep = lastMove.getStep(lastMove.getNumberOfSteps() - 1);
        }

        Square from = lastStep.from;
        Square to = lastStep.getDestination();
        Piece piece = board[to.x][to.y];
        board[to.x][to.y] = null;
        board[from.x][from.y] = piece;
        lastMove.removeLastStep();

        moves.set(moves.size() - 1, lastMove);

        return true;
    }

    boolean load(String positions) {
        reset();
        String[] lines = positions.split("\n");
        if (lines.length != 11
                || !lines[0].equals(" +-----------------+")
                || !lines[9].equals(" +-----------------+")
                || !lines[10].startsWith("   a b c d e f g h")) {
            return false;
        }

        for (int y = 0; y < HEIGHT; ++y) {
            String next = lines[y + 1];
            if (next.length() != 20
                    || !next.startsWith((HEIGHT - y) + "|")
                    || !next.endsWith(" |")) {
                return false;
            }

            for (int x = 0; x < WIDTH; ++x) {
                boolean isTrap = isTrap(getSquare(x, y));
                char space = next.charAt(2 + 2 * x);
                char repr = next.charAt(3 + 2 * x);
                Piece piece = Piece.fromRepr(repr);

                if (space != ' ' || (piece == null && repr != ' '
                        && repr != (isTrap ? 'x' : ' '))) {
                    return false;
                }

                board[x][y] = piece;
            }
        }

        addInitialPhaseSteps();

        return true;
    }

    private void addInitialPhaseSteps() {
        Move goldArrangement = new Move();
        Move silverArrangement = new Move();

        for (int y = HEIGHT - 1; y >= 0; --y) {
            for (int x = 0; x < WIDTH; ++x) {
                Square square = Square.getSquare(x, y);
                Piece piece = getPieceAt(square);
                if (piece == null) {
                    continue;
                }

                Move arrangement = piece.color == Color.GOLD
                        ? goldArrangement : silverArrangement;
                arrangement.addStep(new Step(piece, square, null, false, StepType.SIMPLE));
            }
        }

        moves.add(goldArrangement);
        moves.add(silverArrangement);
        moves.add(new Move());
    }

    void reset() {
        moves.clear();
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                board[x][y] = null;
            }
        }
    }

    public boolean isSafeAt(Square pos, Color color) {
        if (!isTrap(pos)) {
            return true;
        }

        boolean safe = false;
        for (Direction direction : Direction.values()) {
            Square shifted = direction.shift(pos);
            if (shifted == null || !isPieceAt(shifted)) {
                continue;
            }

            if (getPieceAt(shifted).color == color) {
                safe = true;
                break;
            }
        }

        return safe;
    }

    public Piece getPieceAt(Square pos) {
        return pos != null ? board[pos.x][pos.y] : null;
    }

    public boolean isPieceAt(Square pos) {
        return pos != null && board[pos.x][pos.y] != null;
    }

    public boolean isFrozenAt(Square pos) {
        if (!isPieceAt(pos)) {
            return false;
        }

        Piece piece = getPieceAt(pos);

        boolean frozen = false;
        for (Direction direction : Direction.values()) {
            Square shifted = direction.shift(pos);
            if (shifted == null || !isPieceAt(shifted)) {
                continue;
            }

            Piece adjacentPiece = getPieceAt(shifted);
            if (adjacentPiece.color == piece.color) {
                frozen = false;
                break;
            } else if (adjacentPiece.isStronger(piece)) {
                frozen = true;
            }
        }

        return frozen;
    }

    boolean makeMove(Move move) {
        if (move == null || !move.hasSteps()) {
            return false;
        }

        int numberOfSteps = move.getNumberOfSteps();
        boolean made = true;
        for (int i = 0; i < numberOfSteps; ++i) {
            if (!makeStep(move.getStep(i))) {
                made = false;
                break;
            }
        }

        return made;
    }

    ObservableList<Move> getMoves() {
        return moves;
    }

    boolean makeSteps(Step[] steps) {
        for (Step step : steps) {
            if (!makeStep(step)) {
                return false;
            }
        }

        return true;
    }

    private void makeRemovedSteps() {
        for (Square trap : TRAPS) {
            Piece piece = getPieceAt(trap);
            if (piece == null) {
                continue;
            }

            if (!isSafeAt(trap, piece.color)) {
                Step step = new Step(piece, trap, null, true, StepType.SIMPLE);
                addStepToMoves(step);
                board[trap.x][trap.y] = null;
                logger.info("Made " + step.getDescription());
            }
        }
    }

    private void addStepToMoves(Step step) {
        Move lastMove = getLastMove();
        lastMove.addStep(step);
        moves.set(moves.size() - 1, lastMove);
    }

    boolean makeStep(Step step) {
        if (!isValidStep(step)) {
            return false;
        }

        if (step.removed) {
            board[step.from.x][step.from.y] = null;
        } else if (step.direction == null) {
            board[step.from.x][step.from.y] = step.piece;
        } else {
            Square shifted = step.direction.shift(step.from);
            board[shifted.x][shifted.y] = board[step.from.x][step.from.y];
            board[step.from.x][step.from.y] = null;
        }

        logger.info("Made " + step.getDescription());
        addStepToMoves(step);
        makeRemovedSteps();

        return true;
    }

    boolean isValidStep(Step step) {
        if (step == null || step.piece == null || step.from == null) {
            return false;
        }

        if (step.type == StepType.SIMPLE && step.removed) {
            return true;
        }

        return step.type == StepType.SIMPLE
                ? isValidSimpleStep(step) : isValidNonSimpleStep(step);
    }

    private boolean isValidSimpleStep(Step step) {
        return getValidSteps(step.from).contains(step);
    }

    public Set<Step> getValidSteps(Color color) {
        Set<Step> validSteps = new HashSet<>();
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                if (board[x][y] != null && board[x][y].color == color) {
                    validSteps.addAll(getValidSteps(getSquare(x, y)));
                }
            }
        }

        Step previousStep = getPreviousStep();
        if (previousStep != null && previousStep.type == StepType.PUSH) {
            validSteps.removeIf(step ->
                    !step.getDestination().equals(previousStep.from));
        }

        return validSteps;
    }

    Set<Step> getValidSteps(Square from) {
        if (!isPieceAt(from)) {
            return Collections.emptySet();
        }

        return getPieceAt(from).getValidSteps(this, from);
    }

    private boolean isValidNonSimpleStep(Step step) {
        Set<Step> validSteps = new HashSet<>();
        Square lookAt = step.type == StepType.PULL
                ? step.getDestination() : step.from;

        for (Direction direction : Direction.values()) {
            Square shifted = direction.shift(lookAt);
            Piece enemy = getPieceAt(shifted);
            if (enemy == null
                    || step.piece.color == enemy.color
                    || !enemy.isStronger(step.piece)) {
                continue;
            }

            validSteps.addAll(getValidSteps(shifted));
        }

        return validSteps.contains(step);
    }

    void finishMakingMove() {
        moves.add(new Move());
    }

    Move getLastMove() {
        return moves.isEmpty() ? null : moves.get(moves.size() - 1);
    }

    public Step getPreviousStep() {
        Move lastMove = getLastMove();
        if (lastMove == null) {
            return null;
        }

        for (int i = lastMove.getNumberOfSteps() - 1; i >= 0; --i) {
            Step step = lastMove.getStep(i);
            if (!step.removed) {
                return step;
            }
        }

        return null;
    }

    public Board getCopy() {
        Board copied = new Board();
        copied.board = Arrays.copyOf(board, board.length);
        copied.moves.addAll(moves);

        return copied;
    }

    private boolean rabbitReachedGoal(Color color) {
        int goal = color == Color.GOLD ? 0 : HEIGHT - 1;
        char repr = color == Color.GOLD ? 'R' : 'r';
        for (int x = 0; x < WIDTH; ++x) {
            if (board[x][goal] != null && board[x][goal].getRepr() == repr) {
                return true;
            }
        }

        return false;
    }

    private boolean lostAllRabbits(Color color) {
        char repr = color == Color.GOLD ? 'R' : 'r';
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                if (board[x][y] != null && board[x][y].getRepr() == repr) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean hasPossibleSteps(Color color) {
        return !getValidSteps(color).isEmpty();
    }

    GameResult getGameResult(Color player) {
        Color opponent = Color.getOpposingColor(player);
        GameResult playerWins = GameResult.fromColor(player);
        GameResult opponentWins = GameResult.fromColor(opponent);

        if (rabbitReachedGoal(player)) {
            return playerWins;
        } else if (rabbitReachedGoal(opponent)) {
            return opponentWins;
        } else if (lostAllRabbits(opponent)) {
            return playerWins;
        } else if (lostAllRabbits(player)) {
            return opponentWins;
        } else if (!hasPossibleSteps(opponent)) {
            return playerWins;
        }

        return GameResult.NONE;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(EMPTY_BOARD);
        for (int y = 0; y < HEIGHT; ++y) {
            for (int x = 0; x < WIDTH; ++x) {
                if (board[x][y] == null) {
                    continue;
                }

                int index = 24 + 21 * y + 2 * x;
                builder.setCharAt(index, board[x][y].getRepr());
            }
        }

        return builder.toString();
    }
}
