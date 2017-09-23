package com.gimme.gimme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;

public class CommandListener implements MessageCreateListener {
    
    private interface ActionMethod {
        void action(String[] tokens, Message message);
    }
    
    /* Parameters to query a specific website */
    private class APIFormat {
        String baseUrl;
        String countUrl;
        int type;

        public APIFormat(int t, String bu, String cu) {
            baseUrl = bu;
            type = t;
            countUrl = cu;
        }
    }
    
    /* Channel specific state that includes image limit, safe mode, and past query */
    private static class GimmeChannelState implements Serializable {
        int limit;
        transient int moreExpiration;
        transient String[] lastTokens;
        transient String[] lastStatTokens;
        boolean safe;
        
        public GimmeChannelState() {
            limit = 3;
            lastTokens = null;
            lastStatTokens = null;
            moreExpiration = 0;
            safe = true;
        }
        
        @Override
        public String toString() {
            return "Per query image limit: " + limit + "\nSafesearch: " + safe;
        }
    }
    
    /* Contains usage numbers for a given entity represented by id; can be channel, user, etc */
    private static class UsageStatistics implements Serializable {
        String id;
        HashMap<String, Integer> tagCounts;
        int successSearches;
        int imagesReturned;
        int failedSearches;
        
        public UsageStatistics(String id) {
            this.id = id;
            tagCounts = new HashMap<String, Integer>();
            successSearches = 0;
            failedSearches = 0;
            imagesReturned = 0;
        }
        
        /* Update with results of an image search */
        public void update(boolean success, String[] tags, int ir) {
            if (success) {
                successSearches++;
                imagesReturned += ir;
                increaseTags(tags);
            } else {
                failedSearches++;
            }
        }
        
        private void increaseTag(String tag) {
            if (tagCounts.containsKey(tag) == false) {
                tagCounts.put(tag, 1);
            } else {
                tagCounts.put(tag, tagCounts.get(tag) + 1);
            }
        }
        
        private void increaseTags(String[] tags) {
            for (String tag : tags) {
                if (tag.contains("*") == false) {
                    increaseTag(tag.toLowerCase());
                }
            }
        }
        
        /* Returns the most popular number tags from successful searches by the entity represented by this object */
        public List<String> mostPopularTags(int number) {
            // Comparator for entries to compare by value
            Comparator<Entry<String, Integer>> comp = new Comparator<Entry<String, Integer>>() {
                @Override
                public int compare(Entry<String, Integer> e0, Entry<String, Integer> e1)
                {
                    Integer v0 = e0.getValue();
                    Integer v1 = e1.getValue();
                    return v0.compareTo(v1);
                }
            };
            
            // Insert all tagCount pairs into priority queue, keeping it at size number
            PriorityQueue<Entry<String, Integer>> pq = new PriorityQueue<Entry<String, Integer>>(number, comp);
            for (Entry<String, Integer> pair : tagCounts.entrySet()) {
                pq.add(pair);
                while (pq.size() > number) {
                    pq.poll();
                }
            }
            
            // Remaining ones in pq are the highest
            ArrayList<String> answer = new ArrayList<String>();
            while (pq.size() > 0) {
                answer.add(0, pq.poll().getKey());
            }
            
            return answer;
        }
    }

    public static final int COMMON = 1;
    public static final int ALTERNATE = 2;
    public static final int G = 3;

    public static String gUrl;
    public static String tUrl;
    public static String dUrl;
    public static String dUrl2;
    public static String dUrlBase;
    public static String helpMessage;
    public static String inviteLink;
    public static String safeTag;
    public static final int MORE_EXPIRATION_RENEWAL = 8;
    public static final int POPULAR_TAGS_NUMBER = 4;
    public static final int STATS_SAVE_INTERVAL = 5;
    
    private Random rng; // used for randomly selecting numbers
    private HashMap<String, APIFormat> formats; 
    private HashMap<String, ActionMethod> actions; // Contains all commands and the first token needed to activate them. 
    private HashMap<String, GimmeChannelState> channelStates; // Channel states with channel id as key
    private HashMap<String, UsageStatistics> userStats; // User stats with user id as key
    private HashMap<String, UsageStatistics> channelStats; // Channel stats with channel id as key
    private UsageStatistics overallStats; // Overall stats for this bot
    
    public CommandListener() {
        rng = new Random();
        
        formats = new HashMap<String, APIFormat>();
        // load from file should go here
        formats.put("d", new APIFormat(ALTERNATE, dUrl, dUrl2));
        formats.put("g", new APIFormat(G, gUrl, gUrl));
        formats.put("gimme", new APIFormat(COMMON, gUrl, gUrl));
        formats.put("t", new APIFormat(COMMON, tUrl, tUrl));
        
        // loading from file
        channelStates = (HashMap<String, GimmeChannelState>)(MyGimmeBot.restoreState("stuff/channelstates.ser"));
        if (channelStates == null) {
            System.out.println("Had to create new channel states file.");
            channelStates = new HashMap<String, GimmeChannelState>();
        }
        
        userStats = (HashMap<String, UsageStatistics>)(MyGimmeBot.restoreState("stuff/userstats.ser"));
        if (userStats == null) {
            System.out.println("Had to create new user stats file.");
            userStats = new HashMap<String, UsageStatistics>();
        }
        
        channelStats = (HashMap<String, UsageStatistics>)(MyGimmeBot.restoreState("stuff/channelstats.ser"));
        if (channelStats == null) {
            System.out.println("Had to create new channel stats file.");
            channelStats = new HashMap<String, UsageStatistics>();
        }
        
        overallStats = (UsageStatistics)(MyGimmeBot.restoreState("stuff/overallstats.ser"));
        if (overallStats == null) {
            overallStats = new UsageStatistics("");
        }
        
        // Any message whose first token matches one of these commands will call the function belonging to it
        // using the tokens and the message object
        // Actually tokens is redundant since it can be obtained from message.
        actions = new HashMap<String, ActionMethod>();
        actions.put("g", (String[] tokens, Message message) -> searchImageCommand(tokens, message));
        actions.put("d", (String[] tokens, Message message) -> searchImageCommand(tokens, message));
        actions.put("t", (String[] tokens, Message message) -> searchImageCommand(tokens, message));
        actions.put("n", (String[] tokens, Message message) -> toggleSafetyOffCommand(tokens, message));
        //actions.put("h", (String[] tokens, Message message) -> helpCommand(tokens, message));
        actions.put("s", (String[] tokens, Message message) -> toggleSafetyOnCommand(tokens, message));
        actions.put("more", (String[] tokens, Message message) -> moreCommand(tokens, message));
        actions.put("die", (String[] tokens, Message message) -> dieCommand(tokens, message));
        actions.put("limit", (String[] tokens, Message message) -> limitCommand(tokens, message));
        actions.put("cs", (String[] tokens, Message message) -> channelStatusCommand(tokens, message));
        actions.put("stats", (String[] tokens, Message message) -> statsCommand(tokens, message));
        actions.put("gimme", (String[] tokens, Message message) -> searchImageCommand(tokens, message));
        actions.put("stat", (String[] tokens, Message message) -> statCommand(tokens, message));
        actions.put("search", (String[] tokens, Message message) -> statSearchCommand(tokens, message));
        //actions.put("getid", (String[] tokens, Message message) -> message.reply(message.getAuthor().getId()));
        actions.put("!help", (String[] tokens, Message message) -> helpCommand(tokens, message));
        actions.put("!invite", (String[] tokens, Message message) -> inviteCommand(tokens, message));
    }    

    // Killswitch
    private void dieCommand(String[] tokens, Message message) {
        if (tokens.length == 1 && message.getAuthor().getId().equals(MyGimmeBot.masterId)) {
            saveStatistics();
            System.out.println("got killed");
            System.exit(1);
        }
    }
    
    public void saveChannelStates() {
        MyGimmeBot.saveState(channelStates, "stuff/channelstates.ser");
    }
    
    public void saveStatistics() {
        MyGimmeBot.saveState(userStats, "stuff/userstats.ser");
        MyGimmeBot.saveState(channelStats, "stuff/channelstats.ser");
        MyGimmeBot.saveState(overallStats, "stuff/overallstats.ser");
    }

    @Override
    public void onMessageCreate(DiscordAPI api, Message message) {        
        String[] tokens = message.getContent().split("\\s+");
        if (actions.containsKey(tokens[0])) {
            actions.get(tokens[0]).action(tokens, message);
        } else {
            GimmeChannelState chan = getChannelState(message);
            if (chan.moreExpiration > 0 && message.getAuthor().isYourself() == false) {
                chan.moreExpiration--;
            }
        }
    }
    
    /* Displays invite link */
    private void inviteCommand(String[] tokens, Message message) {
        if (tokens.length == 1) {
            message.reply("For public use: " + inviteLink);
        }
    }
    
    /* Shows someone's stats and most popular tags */
    private void statsCommand(String[] tokens, Message message) {
        String name = "";
        String id = "";
        UsageStatistics stats = null;
        if (tokens.length == 2) {
            List<User> mentioned = message.getMentions();
            if (mentioned.size() == 1) {
                User usr = mentioned.get(0);
                stats = getUserStats(usr.getId());
                name = "**" + usr.getName() + "**";
                id = usr.getId();
            } else if (tokens[1].equals("overall")) {
                stats = overallStats;
                name = "overall";
            } else {
                return;
            }
        } else if (tokens.length == 1) {
            stats = getChannelStats(message.getChannelReceiver().getId());
            name = "this channel";
        } else {
            return;
        }
        
        //if (message.getAuthor().getId().equals(MyGimmeBot.MASTER_ID) || id.equals(MyGimmeBot.MASTER_ID) == false) {
            List<String> tagsWithCounts = new ArrayList<String>();
            for (String tag : stats.mostPopularTags(POPULAR_TAGS_NUMBER)) {
                tagsWithCounts.add(tag + " (" + stats.tagCounts.get(tag) + ")");
            }
            
            String output = "Stats for " + name + ":\n**" + stats.successSearches + "/" + (stats.failedSearches + stats.successSearches) + 
                            "** successful searches\n**" + stats.imagesReturned + "** images returned\n\n" +
                            "Favorite tags are " + String.join(", ", tagsWithCounts);
            
            message.reply(output);
        //}
    }
    
    /* Shows image limit and safe mode */
    private void channelStatusCommand(String[] tokens, Message message) {
        if (tokens.length == 1) {
            GimmeChannelState chan = getChannelState(message);
            message.reply(chan.toString());
        }
    }
    
    /* Sets per query limit on images if user has permission */
    private void limitCommand(String[] tokens, Message message) {
        if (tokens.length == 2) {
            GimmeChannelState chan = getChannelState(message);
            try {
                int newLimit = Integer.parseInt(tokens[1]);
                if (newLimit > 0 && newLimit <= 100) {
                    chan.limit = newLimit;
                    message.reply("New image limit is " + newLimit);
                    saveChannelStates();
                }
            } catch (Exception e) {
                System.out.println(tokens[1] + " failed to parse properly");
            }
        }
    }
    
    private void statCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        chan.lastStatTokens = tokens;
        chan.moreExpiration = MORE_EXPIRATION_RENEWAL;
    }
    
    /* Queries using search terms of previous stat command */
    private void statSearchCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        
        if (chan.moreExpiration > 0 && tokens.length == 2) { 
            if (formats.containsKey(tokens[1])) {
                String[] lst = chan.lastStatTokens;
                lst[0] = tokens[1];
                searchImageCommand(lst, message);
            }    
        }
    }
    
    /* Calls last successful query again if not expired */
    private void moreCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        
        if (chan.moreExpiration > 0 && tokens.length == 1) {
            searchImageCommand(chan.lastTokens, message);
        }
    }
    
    /* Lists known public commands and their functions */
    private void helpCommand(String[] tokens, Message message) {
        if (tokens.length == 1 && tokens[0].equals("!help")) {
            message.reply(helpMessage);
        }
    }
    
    /* Turns safe mode off */
    private void toggleSafetyOffCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        
        if (tokens.length == 1 && chan.safe == true) {
            message.reply("Safe mode turned off");
            chan.safe = false;
            saveChannelStates();
        }
    }
    
    /* Turns safe mode on */
    private void toggleSafetyOnCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        
        if (tokens.length == 1 && chan.safe == false) {
            message.reply("Safe mode turned on");
            chan.safe = true;
            saveChannelStates();
        }
    }
    
    /*private void toggleUnsafetyCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        
        if (tokens.length == 2) {
            message.reply("Not safe allowed");
            chan.unsafe = false;
            saveChannelStates();
        }
    }*/
    
    /* Gets the GimmeChannelState object corresponding to the channel in which message was sent */
    private GimmeChannelState getChannelState(Message message) {
        String cid = message.getChannelReceiver().getId();
        if (channelStates.containsKey(cid) == false) {
            channelStates.put(cid, new GimmeChannelState());
        }
        return channelStates.get(cid);
    }
    
    /* Gets the UsageStatistics object corresponding to the user who sent message */
    private UsageStatistics getUserStats(Message message) {
        return getUserStats(message.getAuthor().getId());
    }
    
    private UsageStatistics getUserStats(String uid) {
        if (userStats.containsKey(uid) == false) {
            userStats.put(uid,  new UsageStatistics(uid));
        }
        return userStats.get(uid);
    }
    
    /* Gets the UsageStatistics object corresponding to the channel in which message was sent */
    private UsageStatistics getChannelStats(Message message) {
        return getChannelStats(message.getChannelReceiver().getId());
    }
    
    private UsageStatistics getChannelStats(String cid) {
        if (channelStats.containsKey(cid) == false) {
            channelStats.put(cid,  new UsageStatistics(cid));
        }
        return channelStats.get(cid);
    }
    
    /* Determines the API and tags and calls API to search for images. Updates stats and replies with image links. */
    private void searchImageCommand(String[] tokens, Message message) {
        GimmeChannelState chan = getChannelState(message);
        UsageStatistics userStats = getUserStats(message);
        UsageStatistics channelStats = getChannelStats(message);
        
        if (tokens.length < 2) {
            return;
        }
        
        APIFormat format;
        String[] tags;
        format = formats.get(tokens[0]);
        tags = new String[tokens.length - 1];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = tokens[i + 1];
        }
        
        try {
            List<String> results = callApi(tags, format, chan.limit, chan.safe);

            if (tokens[0].equals("gimme") == false) {
                for (String result : results) {
                    message.reply(result);
                }
                chan.lastTokens = tokens;
                chan.moreExpiration = MORE_EXPIRATION_RENEWAL;
            }
            
            userStats.update(true, tags, results.size());
            channelStats.update(true, tags, results.size());
            overallStats.update(true, tags, results.size());
        } catch (IllegalArgumentException e) {
            userStats.update(false, null, 0);
            channelStats.update(false, null, 0);
            overallStats.update(false, null, 0);
            
            /*StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);*/
            if (tokens[0].equals("gimme") == false) {
                //System.out.println(sw.toString());
                message.reply(e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if ((overallStats.successSearches + overallStats.failedSearches) % STATS_SAVE_INTERVAL == 0) {
            //System.out.println("saving");
            saveStatistics();
        }
    }

    /* Calls the given API with the given tags, limit, and safe option. Returns a list of image links */
    private List<String> callApi(String[] tags, APIFormat format, int limit, boolean safe) throws Exception {
        String debugInfo = "";

        String tagString = String.join(format.type == ALTERNATE ? "+" : " ", tags);
        if (safe) {
            tagString += (format.type == ALTERNATE ? "+" : " ") + safeTag;
        }
        String countCheckUrl = format.countUrl + "&tags=" + tagString + "&limit=1";
        debugInfo += "First url query: " + countCheckUrl + "\n";
        
        // URL request
        URL url = new URL(countCheckUrl);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        
        //System.out.println(debugInfo);
        //System.out.println(getStringFromInputStream(connection.getInputStream()));
        
        // Opening response as XML
        Document doc = parseXML(connection.getInputStream());
        NodeList descNodes = doc.getElementsByTagName("posts");
        Node postsNode = descNodes.item(0);
        Element pne = (Element)postsNode; // Initial node that contains information about number of results
        
        int postCount;
        if (format.type == ALTERNATE) {
            postCount = Integer.parseInt(pne.getTextContent().trim());
        } else {
            postCount = Integer.parseInt(pne.getAttribute("count"));
        }
        debugInfo += "Post count: " + postCount + "\n";
        if (postCount <= 0) {
            throw new IllegalArgumentException("No results");
        }

        // int page = rng.nextInt(postCount) / 100;
        // Randomly select offset post to begin at
        int newOffset = rng.nextInt(postCount);

        String urlPage;
        String urlLimit;
        if (format.type == ALTERNATE) {
            int tentPage = newOffset / limit + 1;
            //System.out.println("tentPage " + tentPage + " new offset " + newOffset + " postCount " + postCount);
            if (tentPage > 1000) {
                // 
                urlLimit = "&limit=" + postCount / 1000;
                urlPage = "&page=" + (1000 * newOffset / postCount + 1);
            } else {
                urlPage = "&page=" + tentPage;
                urlLimit = "&limit=" + limit;
            }
        } else {
            urlPage = "&pid=" + (newOffset / limit);
            urlLimit = "&limit=" + limit;
        }
        String searchUrl = format.baseUrl + "&tags=" + tagString + urlLimit + urlPage;
        debugInfo += "second url query: " + searchUrl + "\n";
        
        URL url2 = new URL(searchUrl);
        URLConnection connection2 = url2.openConnection();
        connection2.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        
        Document doc2 = parseXML(connection2.getInputStream());
        NodeList postNodes = doc2.getElementsByTagName((format.type == ALTERNATE) ? "file-url" : "post");
        int postsFound = postNodes.getLength();
        debugInfo += postsFound + " posts found\n";
        
        ArrayList<String> answer = new ArrayList<String>();
        debugInfo += "Showing results ";
        
        if (format.type == ALTERNATE) {
            int[] randomInts;
            if (postsFound > limit) {
                randomInts = new Random().ints(0, postsFound).distinct().limit(limit).toArray();
            } else {
                randomInts = new int[postsFound];
                for (int i = 0; i < postsFound; i++) {
                    randomInts[i] = i;
                }
            }
            
            for (int i = 0; i < randomInts.length; i++) {
                debugInfo += randomInts[i] + ", ";
                //NodeList nl = .getElementsByTagName("file-url")
                answer.add(dUrlBase + ((Element)(postNodes.item(randomInts[i]))).getTextContent());
            }
        } else if (format.type == G) {
            for (int i = 0; i < postsFound; i++) {
                debugInfo += i + ", ";
                String sample_url = ((Element) (postNodes.item(i))).getAttribute("sample_url");
                /*if (sample_url.substring(2, 7).equals("simg4")) {
                    sample_url = "//" + sample_url.substring(8);
                }*/
                answer.add(sample_url);
            }
        } else {
            for (int i = 0; i < postsFound; i++) {
                debugInfo += i + ", ";
                answer.add(((Element) (postNodes.item(i))).getAttribute("sample_url"));
            }
        }
        
        //answer.add(debugInfo);
        System.out.println(debugInfo);
        return answer;
    }

    private Document parseXML(InputStream stream) throws Exception {
        DocumentBuilderFactory objDocumentBuilderFactory = null;
        DocumentBuilder objDocumentBuilder = null;
        Document doc = null;
        try {
            objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
            objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();

            doc = objDocumentBuilder.parse(stream);
        } catch (Exception ex) {
            throw ex;
        }

        return doc;
    }
    
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }
}