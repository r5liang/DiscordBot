package com.omok;

public class OmokGame {
	
	public static final int INIT = 0;
	public static final int PLAYER1_TURN = 1;
	public static final int PLAYER2_TURN = 2;
	public static final int PLAYER1_WIN = 3;
	public static final int PLAYER2_WIN = 4;
	public static final int TERMINATED = 5;
	
	private int state;
	private Board b;
	private Player player1;
	private Player player2;
	
	public OmokGame(Player p1, Player p2) {
		b = new Board();
		player1 = p1;
		player2 = p2;
		state = INIT;
	}
	/*
	public void swapPlayer1(Player p) {
		player1 = p;
	}
	
	public void swapPlayer2(Player p) {
		player2 = p;
	}
	*/
	public int getState() {
		return state;
	}
	
	public void reset() {
		state = INIT;
		b.clear();
	}
	
	/* starts the game when state is INIT, or restarts when a player has won. returns true
	 * otherwise returns false
	 */
	public boolean start(boolean isPlayer1) {
		if (state == INIT) {
			state = isPlayer1 ? PLAYER2_TURN : PLAYER1_TURN;
			return true;
		} else if (state == PLAYER1_WIN) {
			b.clear();
			state = PLAYER2_TURN;
			return true;
		} else if (state == PLAYER2_WIN) {
			b.clear();
			state = PLAYER1_TURN;
			return true;
		}
		return false;
	}
	
	/* Resigns the game and awards win to other player. */
	public boolean resign(boolean isPlayer1) {
		if (state == PLAYER1_TURN || state == PLAYER2_TURN) { // resignation
			state = isPlayer1 ? PLAYER2_WIN : PLAYER1_WIN;
			return true;
		} else {
			return false;
		}
	}
	
	/* request to undo previous move */
	public boolean undo(boolean isPlayer1) {
		return false;
	}
	
	/* attempts to place a piece on the board */
	public boolean makeMove(int x, int y, boolean isPlayer1) {
		if (x < 0 || x >= b.getBoardLength() || y < 0 || y >= b.getBoardWidth()) { // out of bounds
			System.out.println("out of bounds");
			return false;
		}
		if (b.getPieceAt(x, y) != 0) { // already has existing piece
			System.out.println("existing piece");
			return false;		
		}
		if (state == PLAYER1_TURN && isPlayer1) {
			b.placePiece(1, x, y);
			if (b.checkConnections(x, y)) { // has this move won the game?
				state = PLAYER1_WIN;
				System.out.println("Player 1 has won");
			} else {
				state = PLAYER2_TURN;
				System.out.println("changed to player2's turn");
			}
			return true;
			
		} else if (state == PLAYER2_TURN && !isPlayer1) {
			b.placePiece(-1, x, y);
			if (b.checkConnections(x, y)) { // has this move won the game?
				state = PLAYER2_WIN;
				System.out.println("Player 2 has won");
			} else {
				state = PLAYER1_TURN;
				System.out.println("changed to player2's turn");
			}
			return true;
		}
		return false;
	}
	
	/* Returns an ASCII drawing of the board */
	public String toString() {
		/*String answer = player1.getID() + ": " + player1.getPiece() + "\n" + player2.getID() + ": " + player2.getPiece();
		answer += "\n\n"; */
		int[][] omokBoard = b.getBoard();
		String answer = ".  ";
		for (int j = 0; j < omokBoard[0].length; j++) {
			answer += (char)(j + 65) + " ";
		}
		answer += "\n";
		for (int i = 0; i < omokBoard.length; i++) {
			answer += (i + 1) + " ";
			if (i + 1 < 10) {
				answer += " ";
			}
			for (int j = 0; j < omokBoard[i].length; j++) {
				if (omokBoard[i][j] == 0) {
					answer += "- ";
				} else{
					answer += (omokBoard[i][j] == 1 ? player1.getPiece() : player2.getPiece()) + " ";
				}
			}
			answer += (i + 1) + "\n";
		}
		answer += ".  ";
		for (int j = 0; j < omokBoard[0].length; j++) {
			answer += (char)(j + 65) + " ";
		}
		return answer;
	}
}
