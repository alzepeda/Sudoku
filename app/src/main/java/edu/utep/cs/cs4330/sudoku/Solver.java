package edu.utep.cs.cs4330.sudoku;

public class Solver {
    static int sqrt;

    /**
     * Checks what entries are valid in a given square
     * @param board the board on which valid entries are checked
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return a boolean array where the value at each index tells if the index is a valid entry
     */
    private static boolean[] possible(int[][] board, int x, int y) {
        int size = board.length;
        boolean[] possible = new boolean[size + 1];
        sqrt = (int)Math.sqrt(size);

        for (int i = 0; i <= size; i++) {
            possible[i] = true;
        }

        for (int i = 0; i < size; i++) {
            for (int n = 1; n < (size + 1); n++) {
                if (board[x][i] == n || board[i][y] == n) {
                    possible[n] = false;
                }
            }
        }


        for (int i = 0; i < sqrt; i++) {
            for (int j = 0; j < sqrt; j++) {
                for (int n = 1; n <= size; n++) {
                    if (board[(x / sqrt) * sqrt + i][(y / sqrt) * sqrt + j] == n) {
                        possible[n] = false;
                    }
                }
            }
        }
        return possible;
    }

    /**
     * checks if the current configuration is solveable
     * @param board
     * @return boolean describing if the current configuration is solveable
     */
    public static boolean solveable(int[][] board){
        int size = board.length;
        int[][] copy = new int[size][size];

        for(int i = 0; i < size; i++){
            for (int j = 0; j < size; j++) {
                copy[i][j] = board[i][j];
            }
        }

        return solveSudoku(copy);
    }



    /**
     * solves the given sudoku board
     * @param board
     * @return true if the sudoku board can be solved
     */
    public static boolean solveSudoku(int[][] board) {
        int x = -1;
        int y = -1;
        int size = board.length;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) {
                    x = i;
                    y = j;
                }
            }
        }

        if (x == -1) {
            return true;
        }

        for (int i = 1; i <= size; i++) {
            if (possible(board, x, y)[i]) {
                board[x][y] = i;
                if (solveSudoku(board)) {
                    return true;
                }
                board[x][y] = 0;
            }
        }
        return false;
    }
}
