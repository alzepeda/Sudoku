package edu.utep.cs.cs4330.sudoku.model;

import java.lang.Math;
import java.util.Random;


import edu.utep.cs.cs4330.sudoku.*;


/**
 * An abstraction of Sudoku puzzle.
 */
public class Board {
    private int size = 9;
    private int level = 1;
    private int[][] grid = new int[size][size];
    private int sqrt = (int) Math.sqrt(size);
    private boolean[][] original = new boolean[size][size];

    /**
     * Create a new random board with desired size and level as given by the global variables
     */
    public void setGrid(){
        Random r = new Random();
        int n;
        grid = new int[size][size];
        sqrt = (int)Math.sqrt(size);

        do {
            grid = new int[size][size];
            for(int i = 0; i <sqrt; i++){
                for(int j = 0; j<sqrt; j++){
                    n = r.nextInt(size)+1;
                    while(!possible(i, j)[n]){
                        n = r.nextInt(size)+1;
                    }
                    grid[i][j] = n;
                }
            }

            for(int i =sqrt; i <sqrt*2; i++){
                for(int j = sqrt; j< sqrt*2; j++){
                    n = r.nextInt(size)+1;
                    while(!possible(i, j)[n]){
                        n = r.nextInt(size)+1;
                    }
                    grid[i][j] = n;
                }
            }

            if(size == 9) {
                for(int i = sqrt*2; i <sqrt*3; i++){
                    for(int j = sqrt*2; j< sqrt*3; j++){
                        n = r.nextInt(size)+1;
                        while(!possible(i, j)[n]){
                            n = r.nextInt(size)+1;
                        }
                        grid[i][j] = n;
                    }
                }
            }
        } while(!Solver.solveSudoku(grid));

        int cells = filledSquares();
        int x, y;

        for(int i = size*size; i >= cells; i--){
            do{
                x = r.nextInt(size);
                y = r.nextInt(size);
            }while(grid[x][y] == 0);
            grid[x][y] = 0;
        }
        setOriginal();
    }

    /**
     * Configures the original array immmediately after a new grid is created
     */
    private void setOriginal(){
        original = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if(grid[i][j] != 0){
                    original[i][j] = true;
                }
            }
        }
    }

    /**
     * Determines how many squares should remain filled depending on the size/level
     * @return the amount of cells to remain filled
     */
    private int filledSquares(){
        if(size == 9){
            if(level == 1){
                return 30;
            }if(level == 2){
                return 26;
            }if(level == 3){
                return 22;
            }
        }if(size == 4){
            if(level == 1){
                return 8;
            }if(level == 2){
                return 6;
            }if(level == 3){
                return 4;
            }
        }
        return 0;
    }

    /**
     * The possible entries for the square in x,y
     * @param x the x-coordinate of the square
     * @param y the y-coordinate of the square
     * @return a boolean array where each value is true if the index is a valid entry
     */
    public boolean[] possible(int x, int y){
        boolean[] possible = new boolean[size+1];

        for(int i=0; i<=size; i++){
            possible[i] = true;
        }

        for(int i = 0; i<size; i++){
            for(int n = 1; n<(size+1); n++){
                if(grid[x][i]==n || grid[i][y]==n){
                    possible[n] = false;
                }
            }
        }

        for(int i = 0; i<sqrt; i++){
            for(int j = 0; j<sqrt; j++){
                for(int n = 1; n<(size+1); n++){
                    if(grid[(x/sqrt)*sqrt+i][(y/sqrt)*sqrt+j] == n){
                        possible[n] = false;
                    }
                }
            }
        }

        return possible;
    }

    /**
     * Allows the menu access to change the level
     * Reconfigures the grid
     * @param level the level the sudoku puzzle will now have
     */
    public void setLevel(int level) {
        this.level = level;
        setGrid();
    }

    /**
     * Allows the menu access to change the size
     * Reconfigures the grid
     * @param size  the size of the grid matrix will now have
     */
    public void setSize(int size) {
        this.size = size;
        setGrid();
    }

    /**
     * Return the size of this board.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the current sudoku puzzle
     * @return the grid representing the sudoku puzzle
     */
    public int[][] getGrid(){
        return grid;
    }

    /**
     * Sets a single square in the sudoku puzzle
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param n the value in the square
     */
    public void setNumber(int x, int y, int n){
        grid[x][y] = n;
    }

    /**
     * Reveals if the given square is original
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return if the number in x,y is part of the original configuration
     */
    public boolean original(int x, int y){
        return original[x][y];
    }

    /**
     * Checks if the grid is complete
     * @return the grid is complete
     */
    public boolean complete(){
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if(grid[i][j] == 0){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Created by sebas on 3/25/2018.
     */
}