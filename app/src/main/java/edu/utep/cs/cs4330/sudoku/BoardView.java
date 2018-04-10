package edu.utep.cs.cs4330.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import edu.utep.cs.cs4330.sudoku.model.Board;

/**
 * A special view class to display a Sudoku board modeled by the
 * {@link edu.utep.cs.cs4330.sudoku.model.Board} class. You need to write code for
 * the <code>onDraw()</code> method.
 *
 * @see edu.utep.cs.cs4330.sudoku.model.Board
 * @author cheon
 * edited by Eric Elguea and Ana Zepeda
 */

public class BoardView extends View {

    /** To notify a square selection. */
    public interface SelectionListener {

        /** Called when a square of the board is selected by tapping.
         * @param x 0-based column index of the selected square.
         * @param y 0-based row index of the selected square. */
        void onSelection(int x, int y);
    }

    /** Listeners to be notified when a square is selected. */
    private final List<SelectionListener> listeners = new ArrayList<>();

    /** Number of squares in rows and columns.*/
    private int boardSize = 9;

    /** Board to be displayed by this view. */
    private Board board;

    /** Width and height of each square. This is automatically calculated
     * this view's dimension is changed. */
    private float squareSize;

    /** Translation of screen coordinates to display the grid at the center. */
    private float transX;

    /** Translation of screen coordinates to display the grid at the center. */
    private float transY;

    /** The colors used in the display. */
    private int boardColor = Color.rgb(201, 186, 145);
    private int black = Color.rgb(0,0,0);
    private int grey = Color.rgb(140, 145, 153);
    private int green = Color.rgb(151, 188, 133);

    /** Values storing the coordinates of a selected square. */
    private static int x = -1;
    private static int y = -1;

    /** Describes when the help button is clicked. */
    public static boolean help;

    /** Paint to draw the background of the grid. */
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        boardPaint.setColor(boardColor);
        boardPaint.setAlpha(80); // semi transparent
    }

    /** Create a new board view to be run in the given context. */
    public BoardView(Context context) { //@cons
        this(context, null);
    }

    /** Create a new board view by inflating it from XML. */
    public BoardView(Context context, AttributeSet attrs) { //@cons
        this(context, attrs, 0);
    }

    /** Create a new instance by inflating it from XML and apply a class-specific base
     * style from a theme attribute. */
    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSaveEnabled(true);
        getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    /** Set the board to be displayed by this view. */
    public void setBoard(Board board) {
        this.board = board;
        boardSize = board.getSize();
    }

    /** Draw a 2-D graphics representation of the associated board. */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(transX, transY);
        if (board != null) {
            drawGrid(canvas);
            drawSquares(canvas);
            if (x!=-1) {
                highlightSquare(canvas);
                drawSquares(canvas);
            }if(help){
                drawHelp(canvas);
            }
        }
        canvas.translate(-transX, -transY);
    }

    /** Draw horizontal and vertical grid lines. */
    private void drawGrid(Canvas canvas) {
        final float maxCoord = maxCoord();
        canvas.drawRect(0, 0, maxCoord, maxCoord, boardPaint);
        boardPaint.setColor(black);

        boardPaint.setStrokeWidth(2);
        for(int i = 1; i < boardSize; i++){
            canvas.drawLine(lineGap()*i,0,lineGap()*i, maxCoord, boardPaint);
            canvas.drawLine(0,lineGap()*i,maxCoord,lineGap()*i, boardPaint);
        }

        int sqrt = (int)Math.sqrt(boardSize);
        boardPaint.setStrokeWidth(4);
        for(int i = 1; i < sqrt; i++){
            canvas.drawLine(lineGap()*i*sqrt,0,lineGap()*i*sqrt, maxCoord, boardPaint);
            canvas.drawLine(0,lineGap()*i*sqrt,maxCoord,lineGap()*i*sqrt, boardPaint);
        }

        boardPaint.setColor(boardColor);
        boardPaint.setAlpha(80);
    }

    /** Draw all the squares (numbers) of the associated board. */
    private void drawSquares(Canvas canvas) {
        final float maxCoord = maxCoord();
        canvas.drawRect(0, 0, maxCoord, maxCoord, boardPaint);
        boardPaint.setTextSize(lineGap()*3/4);

        for(int i = 0; i < boardSize; i++){
            for(int j = 0; j < boardSize; j++){
                if(board.getGrid()[i][j] != 0){
                    if(board.original(i,j)){
                        boardPaint.setColor(black);
                    }else{
                        boardPaint.setColor(grey);
                    }
                    canvas.drawText(""+board.getGrid()[i][j],(lineGap()*i+lineGap()*1/4),(lineGap()*j+lineGap()*5/6),boardPaint);
                }
            }
        }

        boardPaint.setColor(boardColor);
        boardPaint.setAlpha(80);
    }

    /** Lists possibilities of entries at empty squares. */
    private void drawHelp(Canvas canvas) {
        final float maxCoord = maxCoord();
        int sqrt = (int)Math.sqrt(boardSize);
        canvas.drawRect(0, 0, maxCoord, maxCoord, boardPaint);
        boardPaint.setTextSize((float)(lineGap()*7/(8*sqrt)));
        boardPaint.setColor(grey);

        for(int i = 0; i < boardSize; i++){
            for(int j = 0; j < boardSize; j++){
                if(board.getGrid()[i][j] == 0){
                    for (int k = 1; k <= sqrt; k++) {
                        for (int l = 1; l <= sqrt; l++) {
                            if(board.possible(i,j)[(k-1)*sqrt+l]){
                                canvas.drawText(""+((k-1)*sqrt+l),(lineGap()*i+lineGap()*l*1/4-lineGap()*1/12),(lineGap()*j+lineGap()*k*1/4+lineGap()*1/12),boardPaint);
                            }
                        }
                    }
                }
            }
        }

        boardPaint.setColor(boardColor);
        boardPaint.setAlpha(80);
    }

    /** Highlights the selected square. */
    private void highlightSquare(Canvas canvas){
        boardPaint.setColor(green);

        canvas.drawRect((x)*lineGap(),(y+1)*lineGap(),(x+1)*lineGap(),(y)*lineGap(),boardPaint);

        boardPaint.setColor(boardColor);
        boardPaint.setAlpha(80);
    }

    /** Overridden here to detect tapping on the board and
     * to notify the selected square if exists. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                int xy = locateSquare(event.getX(), event.getY());
                if (xy >= 0) {
                    // xy encoded as: x * 100 + y
                    notifySelection(xy / 100, xy % 100);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    /**
     * Given screen coordinates, locate the corresponding square of the board, or
     * -1 if there is no corresponding square in the board.
     * The result is encoded as <code>x*100 + y</code>, where x and y are 0-based
     * column/row indexes of the corresponding square.
     */
    private int locateSquare(float x, float y) {
        x -= transX;
        y -= transY;
        if (x <= maxCoord() &&  y <= maxCoord()) {
            final float squareSize = lineGap();
            int ix = (int) (x / squareSize);
            int iy = (int) (y / squareSize);
            return ix * 100 + iy;
        }
        return -1;
    }

    /** To obtain the dimension of this view. */
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener
            =  new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            squareSize = lineGap();
            float width = Math.min(getMeasuredWidth(), getMeasuredHeight());
            transX = (getMeasuredWidth() - width) / 2f;
            transY = (getMeasuredHeight() - width) / 2f;
        }
    };

    /** Return the distance between two consecutive horizontal/vertical lines. */
    protected float lineGap() {
        return Math.min(getMeasuredWidth(), getMeasuredHeight()) / (float) boardSize;
    }

    /** Return the number of horizontal/vertical lines. */
    private int numOfLines() { //@helper
        return boardSize + 1;
    }

    /** Return the maximum screen coordinate. */
    protected float maxCoord() { //@helper
        return lineGap() * (numOfLines() - 1);
    }

    /** Register the given listener. */
    public void addSelectionListener(SelectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Unregister the given listener. */
    public void removeSelectionListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    /** Notify a square selection to all registered listeners.
     *
     * @param x 0-based column index of the selected square
     * @param y 0-based row index of the selected square
     */
    private void notifySelection(int x, int y) {
        for (SelectionListener listener: listeners) {
            listener.onSelection(x, y);
            this.x = x;
            this.y = y;
        }
    }

    /** sets x and y to invalid values if nothing is selected yet */
    public void noneSelected(){
        x = -1;
        y = -1;
    }

}