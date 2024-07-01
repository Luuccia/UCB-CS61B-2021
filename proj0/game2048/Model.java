package game2048;

import java.util.Formatter;
import java.util.Observable;


/**
 * The state of a game of 2048.
 *
 * @author DaiQL
 */
public class Model extends Observable {
    /**
     * Current contents of the board.
     */
    private Board board;
    /**
     * Current score.
     */
    private int score;
    /**
     * Maximum score so far.  Updated when game ends.
     */
    private int maxScore;
    /**
     * True iff game is ended.
     */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /**
     * Largest piece value.
     */
    public static final int MAX_PIECE = 2048;

    /**
     * A new 2048 game on a board of size SIZE with no pieces
     * and score 0.
     */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /**
     * A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes.
     */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /**
     * Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     * 0 <= COL < size(). Returns null if there is no tile there.
     * Used for testing. Should be deprecated and removed.
     */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /**
     * Return the number of squares on one side of the board.
     * Used for testing. Should be deprecated and removed.
     */
    public int size() {
        return board.size();
    }

    /**
     * Return true iff the game is over (there are no moves, or
     * there is a tile with value 2048 on the board).
     */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /**
     * Return the current score.
     */
    public int score() {
        return score;
    }

    /**
     * Return the current maximum game score (updated at end of game).
     */
    public int maxScore() {
        return maxScore;
    }

    /**
     * Clear the board to empty and reset the score.
     */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /**
     * Add TILE to the board. There must be no Tile currently at the
     * same position.
     */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /**
     * Tilt the board toward SIDE. Return true iff this changes the board.
     * <p>
     * 1. If two Tile objects are adjacent in the direction of motion and have
     * the same value, they are merged into one Tile of twice the original
     * value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     * tilt. So each move, every tile will only ever be part of at most one
     * merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     * value, then the leading two tiles in the direction of motion merge,
     * and the trailing tile does not.
     */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        board.setViewingPerspective(side);

        int size = board.size();
        for (int col = 0; col < size; col++) {
            boolean moveResult = moveColumn(col, size);
            changed = changed || moveResult;
        }

        board.setViewingPerspective(Side.NORTH);

        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    /**
     * 一次操作中某一列的变化
     *
     * @param col  列
     * @param size 正方形的长
     * @return 是否有块移动
     */
    private boolean moveColumn(int col, int size) {
        // 可以直接移动到的空格子
        int firstEmptyTile = -1;
        // 标记是否融合，默认为false
        boolean[] isMerged = new boolean[size];
        boolean hasChanged = false;

        for (int row = size - 1; row >= 0; row--) {

            Tile tile = board.tile(col, row);

            // 当前为空格子，保留最顶端的
            if (tile == null && firstEmptyTile < row) {
                firstEmptyTile = row;
            }
            // 当前为块
            else if (tile != null) {
                // 前面没有空格子，但考虑能否与前一块融合
                if (firstEmptyTile == -1 && row + 1 < size && !isMerged[row + 1]) {

                    Tile preTile = board.tile(col, row + 1);
                    if (preTile != null && preTile.value() == tile.value()) {
                        isMerged[row + 1] = board.move(col, row + 1, tile);
                        score += board.tile(col, row + 1).value();
                        firstEmptyTile = row;
                        hasChanged = true;
                    }
                }
                // 前面有空格子，并考虑能否与再前一块融合
                else if (firstEmptyTile != -1) {

                    // 空格子在最顶端，或
                    // 空格子不在最顶端，且前一块已经融合过
                    if (firstEmptyTile == size - 1 || isMerged[firstEmptyTile + 1]) {
                        board.move(col, firstEmptyTile, tile);
                        firstEmptyTile = firstEmptyTile - 1;
                        hasChanged = true;
                    }
                    // 空格子不在最顶端，且前一块没有被融合过
                    else {
                        Tile preTile = board.tile(col, firstEmptyTile + 1);
                        // 值相等，能与前一块融合
                        if (preTile != null && preTile.value() == tile.value()) {
                            isMerged[firstEmptyTile + 1] = board.move(col, firstEmptyTile + 1, tile);
                            score += board.tile(col, firstEmptyTile + 1).value();
                        }
                        // 值不等，不能与前一块融合
                        else {
                            board.move(col, firstEmptyTile, tile);
                            firstEmptyTile -= 1;
                        }
                        hasChanged = true;
                    }
                }
                // 前面没有空格子，且没有前一块
                else {
                    continue;
                }
            }
        }
        return hasChanged;
    }

    /**
     * Checks if the game is over and sets the gameOver variable
     * appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /**
     * Determine whether game is over.
     */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /**
     * Returns true if at least one space on the Board is empty.
     * Empty spaces are stored as null.
     */
    public static boolean emptySpaceExists(Board b) {
        int size = b.size();
        for (int col = 0; col < size; col += 1) {
            for (int row = 0; row < size; row += 1) {
                if (b.tile(col, row) == null)
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        int size = b.size();
        for (int col = 0; col < size; col += 1) {
            for (int row = 0; row < size; row += 1) {
                Tile tile = b.tile(col, row);
                if (tile != null && tile.value() == MAX_PIECE)
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        boolean hasEmpty = emptySpaceExists(b);
        // 是否有空格子
        if (hasEmpty)
            return true;

        // 是否有相邻且相等的两个块
        int size = b.size();
        for (int col = 0; col < size; col += 1) {
            for (int row = 0; row < size; row += 1) {
                Tile rightTile = null;
                Tile bottomTile = null;
                if (col + 1 < size)
                    rightTile = b.tile(col + 1, row);
                if (row + 1 < size)
                    bottomTile = b.tile(col, row + 1);
                Tile tile = b.tile(col, row);
                if (tile != null && rightTile != null && tile.value() == rightTile.value())
                    return true;
                if (tile != null && bottomTile != null && tile.value() == bottomTile.value())
                    return true;
            }
        }
        return false;
    }


    @Override
    /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
