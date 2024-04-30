package Project.Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import Project.Common.ConnectionPayload;
import Project.Common.Constants;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Common.PrivateMPayload;
import Project.Common.RollPayload;
import Project.Common.RoomResultsPayload;
import Project.Common.TextFX;
import Project.Common.Mute_UnmutePayload;
import Project.Common.TextFX.Color;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client;
    private String clientName;
    private boolean isRunning = false;
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private ObjectOutputStream out;// exposed here for send()
    // private Server server;// ref to our server so we can call methods on it
    // more easily
    private Room currentRoom;
    private Logger logger = Logger.getLogger(ServerThread.class.getName());

    // UCID - ob75 - April 13, 2024
    private List<String> isMutedClients = new ArrayList<String>();

    private void info(String message) {
        logger.info(String.format("Thread[%s]: %s", getClientName(), message));
    }

    public ServerThread(Socket myClient/* , Room room */) {
        info("Thread created");
        // get communication channels to single client
        this.client = myClient;
        // this.currentRoom = room;

    }

    protected void setClientId(long id) {
        clientId = id;
        if (id == Constants.DEFAULT_CLIENT_ID) {
            logger.info(TextFX.colorize("Client id reset", Color.WHITE));
        }
        sendClientId(id);
    }

    protected boolean isRunning() {
        return isRunning;
    }

    protected void setClientName(String name) {
        if (name == null || name.isBlank()) {
            logger.severe("Invalid client name being set");
            return;
        }
        clientName = name; 
        //UCID - ob75 - April 29, 2024
        try {
            loadClientMuteFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getClientName() {
        return clientName;
    }

    protected synchronized Room getCurrentRoom() {
        return currentRoom;
    }

    protected synchronized void setCurrentRoom(Room room) {
        if (room != null) {
            currentRoom = room;
        } else {
            info("Passed in room was null, this shouldn't happen");
        }
    }

    public void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }

    // send methods
    protected boolean sendClientMapping(long id, String name) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setPayloadType(PayloadType.SYNC_CLIENT);
        cp.setClientId(id);
        cp.setClientName(name);
        return send(cp);
    }

    protected boolean sendJoinRoom(String roomName) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(roomName);
        return send(p);
    }

    protected boolean sendClientId(long id) {
        ConnectionPayload cp = new ConnectionPayload();
        cp.setClientId(id);
        cp.setClientName(clientName);
        return send(cp);
    }

    private boolean sendListRooms(List<String> potentialRooms) {
        RoomResultsPayload rp = new RoomResultsPayload();
        rp.setRooms(potentialRooms);
        if (potentialRooms == null) {
            rp.setMessage("Invalid limit, please choose a value between 1-100");
        } else if (potentialRooms.size() == 0) {
            rp.setMessage("No rooms found matching your search criteria");
        }
        return send(rp);
    }

    public boolean sendMessage(long from, String message) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        // p.setClientName(from);
        p.setClientId(from);
        p.setMessage(message);
        return send(p);
    }

    // UCID - ob75 - April 13, 2024
    public void addMute(String clientMuteName) {
        clientMuteName = clientMuteName.trim();
        if (!isMutedClients.contains(clientMuteName)) {
            isMutedClients.add(clientMuteName);
        }
        // UCID - ob75 - April 29, 2024
        saveClientMuteFile();
    }

    // UCID - ob75 - April 13, 2024
    public void removeMute(String clientUnmuteName) {
        clientUnmuteName = clientUnmuteName.trim();
        if (isMutedClients.contains(clientUnmuteName)) {
            isMutedClients.remove(clientUnmuteName);
        }
        // UCID - ob75 - April 29, 2024
        saveClientMuteFile();
    }

    // UCID - ob75 - April 13, 2024
    public boolean checkMutedList(String clientMuteName) {
        if (isMutedClients.contains(clientMuteName)) {
            return true;
        } else {
            return false;
        }
    }

    // UCID - ob75 - April 29, 2024
    public void saveClientMuteFile() {
        try {
            FileWriter clientMuteFile = new FileWriter(getClientName() + ".txt");
            clientMuteFile.write(String.join(",", isMutedClients));   
            clientMuteFile.close();
            System.out.println("Sucessfully wrote to file " + clientMuteFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // UCID - ob75 - April 29, 2024
    public void loadClientMuteFile() throws IOException {
        File file = new File(getClientName() + ".txt");
        if (client.isConnected()) {
            if (file.exists()) {
                try (Scanner reader = new Scanner(file)) {
                    reader.hasNextLine();
                        String users = reader.nextLine();
                            String[] mutedUsers = users.split(",");
                            for(int i = 0; i < mutedUsers.length; i++){
                                addMute(mutedUsers[i]);
                                sendMuteClient(currentRoom.getClientIdFromName(mutedUsers[i]));
                                System.out.println("adding muted user to list" + mutedUsers[i]);
                            }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    // UCID - ob75 - April 24, 2024
    public void sendMuteClient(long clientId) throws IOException {
        Payload mu = new Payload();
        mu.setClientId(clientId);
        mu.setPayloadType(PayloadType.MUTE);
        out.writeObject(mu);
    }

    // UCID - ob75 - April 24, 2024
    public void sendUnmuteClient(long clientId) throws IOException {
        Payload um = new Payload();
        um.setClientId(clientId);
        um.setPayloadType(PayloadType.UNMUTE);
        out.writeObject(um);
    }

    /**
     * Used to associate client names and their ids from the server perspective
     * 
     * @param whoId       id of who is connecting/disconnecting
     * @param whoName     name of who is connecting/disconnecting
     * @param isConnected status of connection (true connecting, false,
     *                    disconnecting)
     * @return
     */
    public boolean sendConnectionStatus(long whoId, String whoName, boolean isConnected) {
        ConnectionPayload p = new ConnectionPayload(isConnected);
        // p.setClientName(who);
        p.setClientId(whoId);
        p.setClientName(whoName);
        p.setMessage(isConnected ? "connected" : "disconnected");
        return send(p);
    }

    private boolean send(Payload payload) {
        // added a boolean so we can see if the send was successful
        try {
            out.writeObject(payload);
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            // comment this out to inspect the stack trace
            // e.printStackTrace();
            cleanup();
            return false;
        } catch (NullPointerException ne) {
            info("Message was attempted to be sent before outbound stream was opened");
            return true;// true since it's likely pending being opened
        }
    }

    // end send methods
    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            Payload fromClient;
            while (isRunning && // flag to let us easily control the loop
                    (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream (null would
                                                                     // likely mean a disconnect)
            ) {

                info("Received from client: " + fromClient);
                processPayload(fromClient);

            } // close while loop
        } catch (Exception e) {
            // happens when client disconnects
            e.printStackTrace();
            info("Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }

    /**
     * Used to process payloads from the client and handle their data
     * 
     * @param p
     */
    private void processPayload(Payload p) {
        switch (p.getPayloadType()) {
            case CONNECT:
                try {
                    ConnectionPayload cp = (ConnectionPayload) p;
                    setClientName(cp.getClientName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case DISCONNECT:
                if (currentRoom != null) {
                    Room.disconnectClient(this, currentRoom);
                }
                break;
            case MESSAGE:
                if (currentRoom != null) {
                    currentRoom.sendMessage(this, p.getMessage());
                } else {
                    // TODO migrate to lobby
                    Room.joinRoom(Constants.LOBBY, this);
                }
                break;
            case CREATE_ROOM:
                Room.createRoom(p.getMessage(), this);
                break;
            case JOIN_ROOM:
                Room.joinRoom(p.getMessage(), this);
                break;
            case LIST_ROOMS:
                String searchString = p.getMessage() == null ? "" : p.getMessage();
                int limit = 10;
                try {
                    RoomResultsPayload rp = ((RoomResultsPayload) p);
                    limit = rp.getLimit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                List<String> potentialRooms = Room.listRooms(searchString, limit);
                this.sendListRooms(potentialRooms);
                break;

            // UCID - ob75 - March 30, 2024
            case ROLL:
                try {
                    RollPayload rp = (RollPayload) p;
                    (currentRoom).setRoll(this, rp.getDice(), rp.getSides());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            // UCID - ob75 - March 31, 2024
            case FLIP:
                try {
                    (currentRoom).setFlip(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            // UCID - ob75 - April 10, 2024
            case PM:
                try {
                    PrivateMPayload pm = (PrivateMPayload) p;
                    (currentRoom).setPrivateM(this, pm.getClientName(), pm.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            // UCID - ob75 - April 13, 2024
            case MUTE:
                try {
                    Mute_UnmutePayload mp = (Mute_UnmutePayload) p;
                    (currentRoom).setMute(this, mp.getClientMuteName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            // UCID - ob75 - Aprl 13, 2024
            case UNMUTE:
                try {
                    Mute_UnmutePayload mp = (Mute_UnmutePayload) p;
                    (currentRoom).setUnmute(this, mp.getClientUnmuteName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:
                break;

        }

    }

    private void cleanup() {
        info("Thread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("Thread cleanup() complete");
    }

    public long getClientId() {
        return clientId;
    }

    public boolean contains(String clientName2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }
}