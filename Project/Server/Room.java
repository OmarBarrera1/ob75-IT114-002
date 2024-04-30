package Project.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import Project.Common.Constants;

public class Room implements AutoCloseable {
    // protected static Server server;// used to refer to accessible server
    // functions
    private String name;
    private List<ServerThread> clients = new ArrayList<ServerThread>();

    private boolean isRunning = false;
    // Commands
    private final static String COMMAND_TRIGGER = "/";
    // private final static String CREATE_ROOM = "createroom";
    // private final static String JOIN_ROOM = "joinroom";
    // private final static String DISCONNECT = "disconnect";
    // private final static String LOGOUT = "logout";
    // private final static String LOGOFF = "logoff";

    private Logger logger = Logger.getLogger(Room.class.getName());

    public Room(String name) {
        this.name = name;
        isRunning = true;
    }

    private void info(String message) {
        logger.info(String.format("Room[%s]: %s", name, message));
    }

    public String getName() {
        return name;
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        client.setCurrentRoom(this);
        client.sendJoinRoom(getName());// clear first
        if (clients.indexOf(client) > -1) {
            info("Attempting to add a client that already exists");
        } else {
            clients.add(client);
            // connect status second
            sendConnectionStatus(client, true);
            syncClientList(client);

            // UCID - ob75 - April 29, 2024
            Iterator<ServerThread> iter = clients.iterator();
            while (iter.hasNext()) {
                ServerThread user = iter.next();
                System.out.println(user.getClientName() + " and " + client.getClientName());
                if (user.checkMutedList(client.getClientName())) {
                    try {
                        user.sendMuteClient(client.getClientId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                client.loadClientMuteFile();
            } catch (IOException e) {
                
                e.printStackTrace();
            }
        }
    }

    // UCID - ob75 - April 29, 2024
    protected long getClientIdFromName(String clientName) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            if (client.getClientName().equals(clientName)) {
                return client.getClientId();
            }

        }
        return Constants.DEFAULT_CLIENT_ID;

    }

    // UCID - ob75 - March 30, 2024
    public void setRoll(ServerThread client, int dice, int sides) {
        Random rand = new Random();

        if (dice > 0) {
            int result = dice + rand.nextInt((sides * dice) - dice);
            sendMessage(client, String.format("Rolled a %sd%s die and got %s", dice, sides, result));

        } else {
            int result = (int) (sides * Math.random() + 1);
            sendMessage(client, String.format("Rolled a %s sided die and got %s", sides, result));
        }

    }

    // UCID - ob75 - March 31, 2024
    public void setFlip(ServerThread client) {
        if (Math.random() < 0.5) {
            sendMessage(client, "Flipped a coin: tails");

        } else {
            sendMessage(client, "Flipped a coin: heads");
        }
    }

    // UCID - ob75 - April 10, 2024
    public void setPrivateM(ServerThread client, String clientName, String message) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread st = iter.next();
            if (client != null && client.checkMutedList(st.getClientName()) == false
                    && (st.checkMutedList(client.getClientName()) == false)) {
                if (st.getClientName().equalsIgnoreCase(clientName)) {
                    st.sendMessage(client.getClientId(), message);

                }
            }

        }
        client.sendMessage(client.getClientId(), message);
    }

    // UCID - ob75 - April 13, 2024
    public void setMute(ServerThread client, String clientMuteName) {
        client.sendMessage(client.getClientId(), String.format("/mute %s", clientMuteName));
        Iterator<ServerThread> muteIter = clients.iterator();
        while (muteIter.hasNext()) {
            ServerThread mClients = muteIter.next();
            if (mClients.getClientName().equalsIgnoreCase(clientMuteName)) {
                if (client.checkMutedList(mClients.getClientName()) == false) {
                    // UCID - ob75 - April 20, 2024
                    client.sendMessage(client.getClientId(), String.format(" You muted %s", clientMuteName));
                    mClients.sendMessage(client.getClientId(), String.format(" %s muted you", client.getClientName()));
                    client.addMute(clientMuteName);
                    // UCID - ob75 - April 24, 2024
                    try {
                        client.sendMuteClient(mClients.getClientId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }

        }
    }

    // UCID - ob75 - April 13, 2024
    public void setUnmute(ServerThread client, String clientUnmuteName) {
        client.sendMessage(client.getClientId(), String.format("/unmute %s", clientUnmuteName));
        Iterator<ServerThread> muteIter = clients.iterator();
        while (muteIter.hasNext()) {
            ServerThread umClients = muteIter.next();
            if (umClients.getClientName().equalsIgnoreCase(clientUnmuteName)) {
                if (client.checkMutedList(umClients.getClientName()) == true) {
                    // UCID - ob75 - April 20, 2024
                    client.sendMessage(client.getClientId(), String.format(" You unmuted %s", clientUnmuteName));
                    umClients.sendMessage(client.getClientId(),
                            String.format(" %s unmuted you", client.getClientName()));
                    client.removeMute(clientUnmuteName);
                    // UCID - ob75 - April 24, 2024
                    try {
                        client.sendUnmuteClient(umClients.getClientId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        clients.remove(client);
        // we don't need to broadcast it to the server
        // only to our own Room
        if (clients.size() > 0) {
            // sendMessage(client, "left the room");
            sendConnectionStatus(client, false);
        }
        checkClients();
    }

    /***
     * Checks the number of clients.
     * If zero, begins the cleanup process to dispose of the room
     */
    private void checkClients() {
        // Cleanup if room is empty and not lobby
        if (!name.equalsIgnoreCase(Constants.LOBBY) && clients.size() == 0) {
            close();
        }
    }

    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    private boolean processCommands(String message, ServerThread client) {
        boolean wasCommand = false;
        try {
            if (message.startsWith(COMMAND_TRIGGER)) {
                String[] comm = message.split(COMMAND_TRIGGER);
                String part1 = comm[1];
                String[] comm2 = part1.split(" ");
                String command = comm2[0];
                // String roomName;
                wasCommand = true;
                switch (command) {
                    /*
                     * case CREATE_ROOM:
                     * roomName = comm2[1];
                     * Room.createRoom(roomName, client);
                     * break;
                     * case JOIN_ROOM:
                     * roomName = comm2[1];
                     * Room.joinRoom(roomName, client);
                     * break;
                     */
                    /*
                     * case DISCONNECT:
                     * case LOGOUT:
                     * case LOGOFF:
                     * Room.disconnectClient(client, this);
                     * break;
                     */

                    default:
                        wasCommand = false;
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wasCommand;
    }

    // Command helper methods
    private synchronized void syncClientList(ServerThread joiner) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread st = iter.next();
            if (st.getClientId() != joiner.getClientId()) {
                joiner.sendClientMapping(st.getClientId(), st.getClientName());
            }
        }
    }

    protected static void createRoom(String roomName, ServerThread client) {
        if (Server.INSTANCE.createNewRoom(roomName)) {
            // server.joinRoom(roomName, client);
            Room.joinRoom(roomName, client);
        } else {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s already exists", roomName));
        }
    }

    protected static void joinRoom(String roomName, ServerThread client) {
        if (!Server.INSTANCE.joinRoom(roomName, client)) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s doesn't exist", roomName));
        }
    }

    protected static List<String> listRooms(String searchString, int limit) {
        return Server.INSTANCE.listRooms(searchString, limit);
    }

    protected static void disconnectClient(ServerThread client, Room room) {
        client.setCurrentRoom(null);
        client.disconnect();
        room.removeClient(client);
    }
    // end command helper methods

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
        info("Sending message to " + clients.size() + " clients");
        if (sender != null && processCommands(message, sender)) {
            // it was a command, don't broadcast
            return;
        }

        // UCID - ob75 - March 27, 2024
        if (message.contains("<!") && message.contains("!>")) {
            message = message.replace("<!", "<b>");
            message = message.replace("!>", "</b>");
        }

        if (message.contains("<@") && message.contains("@>")) {
            message = message.replace("<@", "<i>");
            message = message.replace("@>", "</i>");
        }

        if (message.contains("<#") && message.contains("#>")) {
            message = message.replace("<#", "<u>");
            message = message.replace("#>", "</u>");
        }

        // UCID - ob75 - April 16, 2024
        if (message.contains("<%") && message.contains("%>")) {
            message = message.replace("<%", "<FONT COLOR=red>");
            message = message.replace("%>", "</FONT COLOR=red>");
        }

        if (message.contains("<*") && message.contains("*>")) {
            message = message.replace("<*", "<FONT COLOR=green>");
            message = message.replace("*>", "</FONT COLOR=green>");
        }

        if (message.contains("<&") && message.contains("&>")) {
            message = message.replace("<&", "<FONT COLOR=blue>");
            message = message.replace("&>", "</FONT COLOR=blue>");
        }

        /// String from = (sender == null ? "Room" : sender.getClientName());
        long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();

            // UCID - ob75 - April 13, 2024
            if (sender != null && sender.checkMutedList(client.getClientName()) == false
                    && (client.checkMutedList(sender.getClientName()) == false)) {
                boolean messageSent = client.sendMessage(from, message);

                if (!messageSent) {
                    handleDisconnect(iter, client);

                }
            }
        }
    }

    protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendConnectionStatus(sender.getClientId(), sender.getClientName(),
                    isConnected);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
        iter.remove();
        info("Removed client " + client.getClientName());
        checkClients();
        sendMessage(null, client.getClientName() + " disconnected");
    }

    public void close() {
        Server.INSTANCE.removeRoom(this);
        // server = null;
        isRunning = false;
        clients = null;
    }
}