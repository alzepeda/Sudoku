package edu.utep.cs.cs4330.sudoku.model;
/**In order to save the past grid, that is, the grid before connecting to peer, then a new Board
 * class serves to represent the other grid from the server.
 * */
public class ExternBoard {
    public boolean isWifiBoard;
    public int externsize;
    public int externX;
    public  int externY;
    public int placeThisNumber;
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
