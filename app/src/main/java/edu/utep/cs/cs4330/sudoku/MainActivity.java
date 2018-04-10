package edu.utep.cs.cs4330.sudoku;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.utep.cs.cs4330.sudoku.model.Board;

/**
 * HW1 template for developing an app to play simple Sudoku games.
 * You need to write code for three callback methods:
 * newClicked(), numberClicked(int) and squareSelected(int,int).
 * Feel free to improved the given UI or design your own.
 *
 * <p>
 *  This template uses Java 8 notations. Enable Java 8 for your project
 *  by adding the following two lines to build.gradle (Module: app).
 * </p>
 *
 * <pre>
 *  compileOptions {
 *  sourceCompatibility JavaVersion.VERSION_1_8
 *  targetCompatibility JavaVersion.VERSION_1_8
 *  }
 * </pre>
 *
 * @author Yoonsik Cheon
 */
public class MainActivity extends AppCompatActivity {

    private Board board;
    private BoardView boardView;
    /**Buttons to represent levels of difficulty*/

    /** All the number buttons. */
    private List<View> numberButtons;
    private static final int[] numberIds = new int[] {
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9
    };

    /** Width of number buttons automatically calculated from the screen size. */
    private static int buttonWidth;

    private int x=-1, y=-1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mymenu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        board = new Board();
        boardView = findViewById(R.id.boardView);
        boardView.setBoard(board);
        boardView.addSelectionListener(this::squareSelected);
        board.setGrid();

        numberButtons = new ArrayList<>(numberIds.length);
        for (int i = 0; i < numberIds.length; i++) {
            final int number = i; // 0 for delete button
            View button = findViewById(numberIds[i]);
            button.setOnClickListener(e -> numberClicked(number));
            numberButtons.add(button);
            setButtonWidth(button);
            button.setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.easyMenu:
                board.setLevel(1);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                return true;
            case R.id.mediumMenu:
                board.setLevel(2);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                return true;
            case R.id.hardMenu:
                board.setLevel(3);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                return true;
            case R.id.action4x4menu:
                board.setSize(4);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                toast("Size changed to "+String.valueOf(board.getSize()));
                return true;
            case R.id.action9x9menu:
                board.setSize(9);
                boardView.setBoard(board);
                boardView.noneSelected();
                boardView.postInvalidate();
                for(int i = 0; i < board.getGrid().length; i++){
                    numberButtons.get(i).setEnabled(false);
                }
                toast("Size changed to "+String.valueOf(board.getSize()));
                return true;
        }
        return true;
    }

    /** Callback to be invoked when the new button is tapped. */
    public void newClicked(View view) {
        board.setGrid();
        boardView.noneSelected();
        boardView.postInvalidate();
        for(int i = 0; i < board.getGrid().length; i++){
            numberButtons.get(i).setEnabled(false);
        }
        toast("New game started");

    }

    public void solveClicked(View view){
        Solver.solveSudoku(board.getGrid());
        boardView.postInvalidate();
    }

    /** Callback to be invoked when a number button is tapped.
     *
     * @param n Number represented by the tapped button
     *          or 0 for the delete button.
     */
    public void numberClicked(int n) {
        toast("Number clicked: " + n);
        board.setNumber(x, y, n);
        boardView.postInvalidate();
        if(board.complete()){
            Toast.makeText(this, "Congratulations! The puzzle is complete!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback to be invoked when a square is selected in the board view.
     *
     * @param x 0-based column index of the selected square.
     * @param x 0-based row index of the selected square.
     */
    private void squareSelected(int x, int y) {
        toast(String.format("Square selected: (%d, %d)", x, y));
        boolean[] possible = board.possible(x,y);
        this.x = x;
        this.y = y;
        if(board.original(x,y)){
            for(int i = 0; i < possible.length; i++){
                numberButtons.get(i).setEnabled(false);
            }
            boardView.postInvalidate();
            return;
        }
        for(int i = 0; i < possible.length; i++){
            numberButtons.get(i).setEnabled(possible[i]);
        }
        if(board.getSize() == 4){
            for(int i = 5; i<10; i++){
                numberButtons.get(i).setEnabled(false);
            }
        }
        boardView.postInvalidate();
    }


    /** Show a toast message. */
    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Set the width of the given button calculated from the screen size. */
    private void setButtonWidth(View view) {
        if (buttonWidth == 0) {
            final int distance = 2;
            int screen = getResources().getDisplayMetrics().widthPixels;
            buttonWidth = (screen - ((9 + 1) * distance)) / 9; // 9 (1-9)  buttons in a row
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = buttonWidth;
        view.setLayoutParams(params);
    }

    /** Callback to be invoked when the solveable button is clicked */
    public void solveableClicked(View view) {
        if(Solver.solveable(board.getGrid())){
            Toast.makeText(this,"The board is solveable",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"The board is not solveable",Toast.LENGTH_LONG).show();
        }
    }

    /** Callback to be invoked when the help button is clicked */
    public void helpClicked(View view) {
        if(boardView.help == false){
            boardView.help = true;
        }else{
            boardView.help = false;
        }
        boardView.postInvalidate();
    }
}