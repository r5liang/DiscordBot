package com.gimme.gimme;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.google.common.util.concurrent.FutureCallback;
import com.omok.OmokDriver;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;

public class MyGimmeBot {
    
    public static String masterId;

	public static void main(String[] args) {
	    if (args.length < 1) {
	        System.out.println("Use token as argument");
	        System.exit(1);
	    }
		DiscordAPI api = Javacord.getApi(args[0], true);
		api.setWaitForServersOnStartup(false);
		api.connectBlocking();
		try {
		    BufferedImage img = null;
		    img = ImageIO.read(new File("stuff/avatar.jpg"));
		    api.updateAvatar(img);
		} catch (IOException e) {
		    
		}
		
		Properties prop = new Properties();
	    InputStream input = null;
	    try {
	        input = new FileInputStream("stuff/config.properties");
	        prop.load(input);
	        CommandListener.gUrl = prop.getProperty("gUrl");
	        CommandListener.tUrl = prop.getProperty("tUrl");
	        CommandListener.dUrl = prop.getProperty("dUrl");
	        CommandListener.dUrlBase = prop.getProperty("dUrlBase");
	        CommandListener.dUrl2 = prop.getProperty("dUrl2");
	        CommandListener.helpMessage = prop.getProperty("helpMessage");
	        CommandListener.inviteLink = prop.getProperty("inviteLink");
	        CommandListener.safeTag = prop.getProperty("safeTag");
	        masterId = prop.getProperty("masterId");
	    } catch (IOException ex) {
	        ex.printStackTrace();
	        System.exit(1);
	    } finally {
	        if (input != null) {
	            try {
	                input.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
		
		CommandListener myListener = new CommandListener();
		api.registerListener(myListener);
		OmokDriver od = new OmokDriver();
        api.registerListener(od);
		System.out.println("hi");

	}
	
    /* Serializes object to a file */
    public static void saveState(Object o, String path) {
        FileOutputStream fout = null;
        ObjectOutputStream oos = null;
        try {
            File outputFile = new File(path);
            //outputFile.createNewFile();
            fout = new FileOutputStream(outputFile, false);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(o);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /* Deserializes an object stored in a file */
    public static Object restoreState(String path) {
        Object answer = null;
        FileInputStream fin = null;
        ObjectInputStream ois = null;

        try {
            fin = new FileInputStream(path);
            ois = new ObjectInputStream(fin);
            answer = ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return answer;
    }

}
