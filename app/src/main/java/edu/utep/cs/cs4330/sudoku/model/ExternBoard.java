package edu.utep.cs.cs4330.sudoku.model;

public class ExternBoard {
    public int externsize;


    public int externX;
    public  int externY;

    public int[][]externGrid;

   public void changeGrid(int[] a){
        externGrid = new int[externsize][externsize];
        for (int i = 0; i <externGrid.length ; i++) {
            for (int j = 0; j < 9 ; j++) {
                externGrid[i][j] = a[i];
            }

        }

    }

    public  void setExternsize(int externsize) {
     this.externsize = externsize;
    }

    public void setExternX(int externX) {
        this.externX = externX;
    }

    public void setExternY(int externY) {
       this.externY = externY;
    }


}
