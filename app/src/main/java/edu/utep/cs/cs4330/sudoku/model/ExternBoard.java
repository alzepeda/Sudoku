package edu.utep.cs.cs4330.sudoku.model;

public class ExternBoard {
    public static int externsize;
    public static int externX;
    public static int externY;

    public int[][]externGrid;

    private void changeGrid(int[] a){
        externGrid = new int[externsize][externsize];
        for (int i = 0; i <externGrid.length ; i++) {
            for (int j = 0; j < 9 ; j++) {
                externGrid[i][j] = a[i];
            }

        }

    }

    public void getAllvariables(int size, int x, int y, int[]a){
        externsize = size;
        externX = x;
        externY = y;
        changeGrid(a);

    }

}
