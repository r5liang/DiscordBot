package com.omok;



public class GameInstance {
	private String id1;
	private String id2;
	private OmokGame og;
	
	public GameInstance() {
		
	}
	
	public boolean isReady() {
		return !(id1 == null || id2 == null);
	}
	
	public String getUser1() {
		return id1;
	}
	
	public String getUser2() {
		return id2;
	}
	
	/* more practical, simpler than setters */
	public void addUser(String u) {
		if (id1 == null) {
			id1 = u;
		} else if (id2 == null) {
			id2 = u;
			createGame();
		}
	}
	
	/*public void setUser1(IUser u) {
		id1 = u;
	}

	public void setUser2(IUser u) {
		id2 = u;
	}*/
	
	/* Precondition: isReady() == true */
	private void createGame() {
		og = new OmokGame(new Player(id1, "X"), new Player(id2, "O"));
	}
	
	public OmokGame getGame() {
		return og;
	}
	
	public void resetGame() {
		//og.reset();
		og = null;
		id1 = null;
		id2 = null;
	}
}
