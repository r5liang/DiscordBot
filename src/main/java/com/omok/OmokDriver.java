package com.omok;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;

import java.util.HashMap;
import java.util.HashSet;

import com.gimme.gimme.MyGimmeBot;

public class OmokDriver implements MessageCreateListener {
	
	public static final String OMOK_INIT = "o";
	public static final String OMOK_START = "start";
	public static final String OMOK_RESIGN = "resign";
	public static final String HELP = "!help";
	public static final String RULES = "!rules";
	public static final String DISABLE = "disableomok";
	public static final String ENABLE = "enableomok";
	public static final String RESET = "resetgame";
	public static final String HELP_MESSAGE = "I am a host for the Omok board game on Discord. \n\n" + 
			"Type `" + OMOK_INIT + "` to start a game.\n" + 
			"During the game, when it's your turn, type the letter of the column you want to place your piece " +
			"followed by the row number. For example, `C7`. \n" + 
			"Type `" + OMOK_RESIGN + "` to forfeit the game. \n\n" + 
			"If you are unfamiliar with Omok, type `" + RULES + "` to learn.\n" + 
			"Please contact xxdeathx for questions and complaints.";
	public static final String RULES_MESSAGE = "Omok is a simple game played on a 15x15 board. " + 
			"Two players take turns placing pieces on the board. " + 
			"The goal is to line up 5 in a row of one's own pieces, and doing so wins the game. " +
			"For more information, see https://en.wikipedia.org/wiki/Gomoku";
	
	private HashMap<String, GameInstance> games; // Used to keep track of all instances of the game on different channels.
	private HashSet<String> disabledChannels;
	
	public OmokDriver() {
		games = new HashMap<String, GameInstance>();
		disabledChannels = (HashSet<String>)(MyGimmeBot.restoreState("stuff/omokdisabledchannels.ser"));
        if (disabledChannels == null) {
            System.out.println("Had to create new disabled channel file.");
            disabledChannels = new HashSet<String>();
        }
	}
	
	@Override
    public void onMessageCreate(DiscordAPI api, Message message) {
		String msg = message.getContent();
		String channel = message.getChannelReceiver().getId();
		String sender = message.getAuthor().getId();
		
		if (message.getAuthor().getId().equals(MyGimmeBot.masterId)) {
		    if (msg.equals(DISABLE)) {
	            disabledChannels.add(channel);
	            MyGimmeBot.saveState(disabledChannels, "stuff/omokdisabledchannels.ser");
	        } else if (msg.equals(ENABLE) && disabledChannels.contains(channel)) {
	            disabledChannels.remove(channel);
	            MyGimmeBot.saveState(disabledChannels, "stuff/omokdisabledchannels.ser");
	        }
		}
		
		if (disabledChannels.contains(channel)) {
		    return;
		}

		if (msg.equalsIgnoreCase(HELP)) { // base commands
			message.reply(HELP_MESSAGE);
		} else if (msg.equalsIgnoreCase(RULES)) {
			message.reply(RULES_MESSAGE);
		}
		
		if (msg.equalsIgnoreCase(OMOK_INIT)) {
			if (!(games.containsKey(channel))) { // never started forming a game before in this channel
				games.put(channel, new GameInstance());
			} 
			
			if (!(games.get(channel).isReady())) { // gathering players for this channel
				games.get(channel).addUser(sender);
				if (!(games.get(channel).isReady())) {
					message.reply("Forming Omok game. Type `" + OMOK_INIT + "` to become player 2.");
				} else {
					message.reply("Got both players. Type `" + OMOK_START + "` to begin.");
				}
			} 
		} else if (games.containsKey(channel) && games.get(channel).isReady()) { // if channel has active game
			GameInstance gi = games.get(channel);
			OmokGame og = gi.getGame();
			String id1 = gi.getUser1();
			String id2 = gi.getUser2();
			
			// functions as a state machine
			if (msg.equals(RESET) && message.getAuthor().getId().equals(MyGimmeBot.masterId)) {
			    message.reply("Game reset by admin");
			    gi.resetGame();
	        } else if (og.getState() == OmokGame.INIT || og.getState() == OmokGame.PLAYER1_WIN || 
					og.getState() == OmokGame.PLAYER2_WIN ) { // starting phase
				if (msg.equalsIgnoreCase(OMOK_START)) {
					if (sender.equals(id1) || sender.equals(id2)) {
						boolean isPlayer1 = sender.equals(id1) ? true : false;
						boolean startSuccess = og.start(isPlayer1);
						System.out.println("startSuccess: " + startSuccess);
						displayBoard(message);
					}
				}
			} else if (og.getState() == OmokGame.PLAYER1_TURN) { // player 1's turn
				if (msg.equalsIgnoreCase(OMOK_RESIGN) && 
						(sender.equals(id1) || sender.equals(id2))) { // resignation
					boolean isPlayer1 = sender.equals(id1) ? true : false;
					boolean resignSuccess = og.resign(isPlayer1);
					System.out.println("resignSuccess: " + resignSuccess);
					message.reply("Player " + (isPlayer1 ? 1 : 2) + " has resigned.");
					gi.resetGame();
				} else { // check for making move
					if (sender.equals(id1)) {
						int[] move = parseMove(msg);
						if (move[0] != -1) { // valid move
							boolean makeMoveSuccess = og.makeMove(move[1], move[0], true);
							System.out.println("makeMoveSuccess: " + makeMoveSuccess);
							if (!makeMoveSuccess) {
								message.reply("Invalid move!");
							} else {
								displayBoard(message);
								if (og.getState() == OmokGame.PLAYER1_WIN) {
									message.reply("Player 1 has won!");
									gi.resetGame();
								}
							}
						}
					}
				}
			} else if (og.getState() == OmokGame.PLAYER2_TURN) { // player 2's turn
				if (msg.equalsIgnoreCase(OMOK_RESIGN) && 
						(sender.equals(id1) || sender.equals(id2))) { // resignation
					boolean isPlayer1 = sender.equals(id1) ? true : false;
					boolean resignSuccess = og.resign(isPlayer1);
					System.out.println("resignSuccess: " + resignSuccess);
					message.reply("Player " + (isPlayer1 ? 1 : 2) + " has resigned.");
					gi.resetGame();
				} else { // check for making move
					if (sender.equals(id2)) {
						int[] move = parseMove(msg);
						if (move[0] != -1) { // valid move
							boolean makeMoveSuccess = og.makeMove(move[1], move[0], false);
							System.out.println("makeMoveSuccess: " + makeMoveSuccess);
							if (!makeMoveSuccess) {
								message.reply("Invalid move!");
							} else {
								displayBoard(message);
								if (og.getState() == OmokGame.PLAYER2_WIN) {
									message.reply("Player 2 has won!");
									gi.resetGame();
								}
							}
						}
					}
				}
			}
		} 
	}
	
	/* Formats and sends the board as a discord message */
	private void displayBoard(Message message) {
	    String channel = message.getChannelReceiver().getId();
		if (games.containsKey(channel) == false || games.get(channel).isReady() == false) { // no board to display
			return;
		}

		OmokGame og = games.get(channel).getGame();
		String before = "";
		switch (og.getState()) {
			case OmokGame.PLAYER1_TURN: 
				before = "Player 1's turn";
				break;
			case OmokGame.PLAYER2_TURN:
				before = "Player 2's turn";
				break;
			case OmokGame.PLAYER1_WIN:
				before = "Player 1 has won!";
				break;
			case OmokGame.PLAYER2_WIN:
				before = "Player 2 has won!";
				break;
		}
 		
		message.reply(before + "\n```" + og.toString() + "```");
	}
	
	/* Gets the integer coordinates from the input. */
	private static int[] parseMove(String input) {
		input = input.trim().toUpperCase();
		String[] parts = input.split("\\s+");
		
		char first; // WITH THIS IMPLEMENTATION THE BOARD CAN'T BE TOO BIG 
		String toParse;
		int second; // OR LOWERCASE CHARACTERS (HIGHER ASCII VALUES) WILL CONVERT TO UPPERCASE 
		
		if (parts.length == 2) { // not 2 parts
			if (parts[0].length() != 1) {
				//System.out.println("not a single char");
				return new int[]{ -1, -1 };
			}
			first = parts[0].charAt(0);
			toParse = parts[1];
		} else if (parts.length == 1) {
			first = parts[0].charAt(0);
			toParse = parts[0].substring(1);
		} else {
			//System.out.println("not 1 or 2 parts");
			return new int[]{ -1, -1 };
		}
		
		try {
			second = Integer.parseInt(toParse);
		} catch (Exception e) { // second part is not a number
			//System.out.println("not a number");
			return new int[]{ -1, -1 };
		}
		
		return new int[]{ (int)first - 65, second - 1 };
	}

}
