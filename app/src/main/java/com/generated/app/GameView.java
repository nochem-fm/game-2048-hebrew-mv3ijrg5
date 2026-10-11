package com.generated.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    public interface OnGameEventListener {
        void onScoreChanged(int currentScore, int bestScore);
        void onGameOver();
        void onGameWon();
    }

    private static final int GRID_SIZE = 4;
    private final int[][] grid = new int[GRID_SIZE][GRID_SIZE];

    // Undo state
    private final int[][] undoGrid = new int[GRID_SIZE][GRID_SIZE];
    private int undoScore = 0;
    private boolean canUndo = false;

    private int score = 0;
    private int bestScore = 0;
    private boolean won = false;
    private boolean keepPlaying = false;
    private boolean gameOver = false;

    private OnGameEventListener listener;
    private GestureDetector gestureDetector;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final RectF rectF = new RectF();

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50;
            private static final int SWIPE_VELOCITY_THRESHOLD = 50;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (gameOver || (won && !keepPlaying)) {
                    return false;
                }
                if (e1 == null || e2 == null) {
                    return false;
                }

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            move(Direction.RIGHT);
                        } else {
                            move(Direction.LEFT);
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            move(Direction.DOWN);
                        } else {
                            move(Direction.UP);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void setOnGameEventListener(OnGameEventListener listener) {
        this.listener = listener;
    }

    public void setBestScore(int best) {
        this.bestScore = best;
    }

    public int getScore() {
        return score;
    }

    public int getBestScore() {
        return bestScore;
    }

    public boolean canUndo() {
        return canUndo;
    }

    public void undo() {
        if (!canUndo) return;
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(undoGrid[r], 0, grid[r], 0, GRID_SIZE);
        }
        score = undoScore;
        canUndo = false;
        gameOver = false;
        if (listener != null) {
            listener.onScoreChanged(score, bestScore);
        }
        invalidate();
    }

    public void startNewGame() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = 0;
            }
        }
        score = 0;
        canUndo = false;
        won = false;
        keepPlaying = false;
        gameOver = false;
        spawnRandomTile();
        spawnRandomTile();
        if (listener != null) {
            listener.onScoreChanged(score, bestScore);
        }
        invalidate();
    }

    public void continuePlaying() {
        keepPlaying = true;
        invalidate();
    }

    private void saveUndoState() {
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(grid[r], 0, undoGrid[r], 0, GRID_SIZE);
        }
        undoScore = score;
        canUndo = true;
    }

    private enum Direction { UP, DOWN, LEFT, RIGHT }

    private void move(Direction direction) {
        int[][] temp = new int[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            System.arraycopy(grid[r], 0, temp[r], 0, GRID_SIZE);
        }

        saveUndoState();
        boolean moved = false;
        int pointsEarned = 0;

        switch (direction) {
            case LEFT:
                for (int r = 0; r < GRID_SIZE; r++) {
                    pointsEarned += slideAndMergeRow(grid[r]);
                }
                break;
            case RIGHT:
                for (int r = 0; r < GRID_SIZE; r++) {
                    reverse(grid[r]);
                    pointsEarned += slideAndMergeRow(grid[r]);
                    reverse(grid[r]);
                }
                break;
            case UP:
                for (int c = 0; c < GRID_SIZE; c++) {
                    int[] col = new int[GRID_SIZE];
                    for (int r = 0; r < GRID_SIZE; r++) col[r] = grid[r][c];
                    pointsEarned += slideAndMergeRow(col);
                    for (int r = 0; r < GRID_SIZE; r++) grid[r][c] = col[r];
                }
                break;
            case DOWN:
                for (int c = 0; c < GRID_SIZE; c++) {
                    int[] col = new int[GRID_SIZE];
                    for (int r = 0; r < GRID_SIZE; r++) col[r] = grid[r][c];
                    reverse(col);
                    pointsEarned += slideAndMergeRow(col);
                    reverse(col);
                    for (int r = 0; r < GRID_SIZE; r++) grid[r][c] = col[r];
                }
                break;
        }

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (temp[r][c] != grid[r][c]) {
                    moved = true;
                    break;
                }
            }
        }

        if (moved) {
            score += pointsEarned;
            if (score > bestScore) {
                bestScore = score;
            }
            if (listener != null) {
                listener.onScoreChanged(score, bestScore);
            }

            spawnRandomTile();

            if (!won && !keepPlaying && hasTile(2048)) {
                won = true;
                if (listener != null) {
                    listener.onGameWon();
                }
            } else if (checkGameOver()) {
                gameOver = true;
                if (listener != null) {
                    listener.onGameOver();
                }
            }
            invalidate();
        } else {
            // Did not move, revert canUndo
            canUndo = false;
        }
    }

    private int slideAndMergeRow(int[] row) {
        int points = 0;
        int[] result = new int[GRID_SIZE];
        int targetIdx = 0;

        for (int i = 0; i < GRID_SIZE; i++) {
            if (row[i] != 0) {
                result[targetIdx++] = row[i];
            }
        }

        int[] merged = new int[GRID_SIZE];
        int mIdx = 0;
        for (int i = 0; i < GRID_SIZE; i++) {
            if (result[i] == 0) break;
            if (i < GRID_SIZE - 1 && result[i] == result[i + 1]) {
                int val = result[i] * 2;
                merged[mIdx++] = val;
                points += val;
                i++; // skip merged neighbor
            } else {
                merged[mIdx++] = result[i];
            }
        }

        System.arraycopy(merged, 0, row, 0, GRID_SIZE);
        return points;
    }

    private void reverse(int[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            int tmp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = tmp;
        }
    }

    private void spawnRandomTile() {
        int emptyCount = 0;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == 0) emptyCount++;
            }
        }
        if (emptyCount == 0) return;

        int target = (int) (Math.random() * emptyCount);
        int current = 0;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == 0) {
                    if (current == target) {
                        grid[r][c] = (Math.random() < 0.9) ? 2 : 4;
                        return;
                    }
                    current++;
                }
            }
        }
    }

    private boolean hasTile(int value) {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == value) return true;
            }
        }
        return false;
    }

    private boolean checkGameOver() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c] == 0) return false;
                if (c < GRID_SIZE - 1 && grid[r][c] == grid[r][c + 1]) return false;
                if (r < GRID_SIZE - 1 && grid[r][c] == grid[r + 1][c]) return false;
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewSize = getWidth();
        if (viewSize <= 0) return;

        float padding = viewSize * 0.03f;
        float boardCorner = viewSize * 0.03f;
        float tileSize = (viewSize - padding * (GRID_SIZE + 1)) / GRID_SIZE;
        float tileCorner = tileSize * 0.12f;

        // Draw Board Background
        bgPaint.setColor(0xFFBBADA0);
        rectF.set(0, 0, viewSize, viewSize);
        canvas.drawRoundRect(rectF, boardCorner, boardCorner, bgPaint);

        // Draw Cells & Tiles
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                float left = padding + c * (tileSize + padding);
                float top = padding + r * (tileSize + padding);
                rectF.set(left, top, left + tileSize, top + tileSize);

                int val = grid[r][c];
                // Background of tile
                tilePaint.setColor(getTileColor(val));
                canvas.drawRoundRect(rectF, tileCorner, tileCorner, tilePaint);

                // Draw number text if > 0
                if (val > 0) {
                    textPaint.setColor(getTextColor(val));
                    float textSize = getTileTextSize(val, tileSize);
                    textPaint.setTextSize(textSize);

                    String text = String.valueOf(val);
                    textPaint.getTextBounds(text, 0, text.length(), textBounds);
                    float x = rectF.centerX();
                    float y = rectF.centerY() - textBounds.exactCenterY();
                    canvas.drawText(text, x, y, textPaint);
                }
            }
        }
    }

    private int getTileColor(int val) {
        switch (val) {
            case 0: return 0xFFCDC1B4;
            case 2: return 0xFFEEE4DA;
            case 4: return 0xFFEDE0C8;
            case 8: return 0xFFF2B179;
            case 16: return 0xFFF59563;
            case 32: return 0xFFF67C5F;
            case 64: return 0xFFF65E3B;
            case 128: return 0xFFEDCF72;
            case 256: return 0xFFEDCC61;
            case 512: return 0xFFEDC850;
            case 1024: return 0xFFEDC53F;
            case 2048: return 0xFFEDC22E;
            default: return 0xFF3C3A32;
        }
    }

    private int getTextColor(int val) {
        if (val == 2 || val == 4) {
            return 0xFF776E65;
        }
        return 0xFFFFFFFF;
    }

    private float getTileTextSize(int val, float tileSize) {
        if (val < 100) {
            return tileSize * 0.48f;
        } else if (val < 1000) {
            return tileSize * 0.40f;
        } else if (val < 10000) {
            return tileSize * 0.32f;
        } else {
            return tileSize * 0.26f;
        }
    }
}
