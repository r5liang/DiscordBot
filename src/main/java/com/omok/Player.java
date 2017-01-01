package com.omok;

/* Represents an Omok player */
public class Player {
	private final String piece;
	private final String id;
	
	public Player(String id1, String p) {
		id = id1;
		piece = p;
	}
	
	public String getID() {
		return id;
	}
	
	public String getPiece() {
		return piece;
	}
}
