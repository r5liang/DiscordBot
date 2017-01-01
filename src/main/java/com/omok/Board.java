package com.omok;

/* Represents the board */
public class Board {
	public static final int WIN_AMOUNT = 5;
	private int[][] omokBoard;
	
	/* Creates a new board with the given array */
	public Board(int[][] b) {
		omokBoard = b;
	}
	
	/* Creates a new empty board with the given dimensions */
	public Board(int length, int width) {
		omokBoard = new int[length][width];
		clear();
	}
	
	/* Creates a new empty board of size 15x15 */
	public Board() {
		omokBoard = new int[15][15];
		clear();
	}
	
	/* Clears the board */
	public void clear() {
		for (int i = 0; i < omokBoard.length; i++) {
			for (int j = 0; j < omokBoard[i].length; j++) {
				omokBoard[i][j] = 0;
			}
		}
	}
	
	/* returns reference to the board */
	public int[][] getBoard() {
		return omokBoard;
	}
	
	public int getBoardLength() {
		return omokBoard.length;
	}
	
	public int getBoardWidth() {
		return omokBoard[0].length;
	}
	
	/* Places a game piece at location x, y */
	public void placePiece(int piece, int x, int y) {
		omokBoard[x][y] = piece;
	}
	
	/* Gets current piece at location x, y. Returns 0 if space is empty. */
	public int getPieceAt(int x, int y) {
		return omokBoard[x][y];
	}
	
	/* Checks whether the piece at location x, y is part of any 5 in a row */
	public boolean checkConnections(int x, int y) {
		if (omokBoard[x][y] == 0) {
			return false;
		} else {
			int piece = omokBoard[x][y];
			return checkVertical(piece, x, y) || checkHorizontal(piece, x, y) || 
					checkDiagonal1(piece, x, y) || checkDiagonal2(piece, x, y);
		}
	}
	
	/* Checks whether the piece at x, y connects 5 in a row vertically. */
	private boolean checkVertical(int piece, int x, int y) {
		int count = 0;
		int coord = x;
		while (coord >= 0 && omokBoard[coord][y] == piece) { // counting up
			count++;
			coord--;
		}
		coord = x + 1;
		while (coord < omokBoard.length && omokBoard[coord][y] == piece) { // counting down
			count++;
			coord++;
		}
		
		return count >= WIN_AMOUNT;
	}
	
	/* Checks whether the piece at x, y connects 5 in a row horizontally. */
	private boolean checkHorizontal(int piece, int x, int y) {
		int count = 0;
		int coord = y;
		while (coord >= 0 && omokBoard[x][coord] == piece) { // counting left
			//System.out.println("count " + count + "coord " + coord + "y " + y);
			count++;
			coord--;
		}
		coord = y + 1;
		while (coord < omokBoard[x].length && omokBoard[x][coord] == piece) { // counting right
			//System.out.println("count " + count + "coord " + coord + "y " + y);
			count++;
			coord++;
		}
		
		return count >= WIN_AMOUNT;
	}
	
	/* Checks whether the piece at x, y connects 5 in a row diagonally from upper left to bottom right. */
	private boolean checkDiagonal1(int piece, int x, int y) {
		int count = 0;
		int xCoord = x;
		int yCoord = y;
		while (xCoord >= 0 && yCoord >= 0 && omokBoard[xCoord][yCoord] == piece) {
			count++;
			xCoord--;
			yCoord--;
		}
		xCoord = x + 1;
		yCoord = y + 1;
		while (xCoord < omokBoard.length && yCoord < omokBoard[x].length && omokBoard[xCoord][yCoord] == piece) {
			count++;
			xCoord++;
			yCoord++;
		}
		return count >= WIN_AMOUNT;
	}
	
	/* Checks whether the piece at x, y connects 5 in a row diagonally from bottom left to upper right. */
	private boolean checkDiagonal2(int piece, int x, int y) {
		int count = 0;
		int xCoord = x;
		int yCoord = y;
		while (xCoord >= 0 && yCoord < omokBoard[x].length && omokBoard[xCoord][yCoord] == piece) {
			count++;
			xCoord--;
			yCoord++;
		}
		xCoord = x + 1;
		yCoord = y - 1;
		while (xCoord < omokBoard.length && yCoord >= 0 && omokBoard[xCoord][yCoord] == piece) {
			count++;
			xCoord++;
			yCoord--;
		}
		return count >= WIN_AMOUNT;
	}
}
