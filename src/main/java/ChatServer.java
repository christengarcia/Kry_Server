import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import packets.*;

import java.io.IOException;
import java.util.*;

/**
 * This class handles all the server operations. The related
 * classes the ChatServer uses is located in the packets package.
 * @author
 */
public class ChatServer {
    // Store active client connections in HashMap.
    private static HashMap<String, Connection> clients = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // Create a new server then start it with given port settings.
        Server server = new Server();
        // The start method starts a thread to handle incoming connections,
        // reading/writing to the socket, and notifying listeners.
        server.start();
        // Designated port address.
        server.bind(23900, 23901);

        // Creates Listener to handle receiving connections,
        // disconnections and messages.
        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof Packet) {
                    if (object instanceof Packet1Connect) {
                        Packet1Connect p1 = (Packet1Connect) object;

                        // If a client tries to log in with an identical active
                        // client username, print error message then close
                        // connection to prevent user from logging in.
                        if (clients.containsKey(p1.username)) {
                            PacketSameUser p_same = new PacketSameUser();
                            p_same.same_user = "Username already exists!";
                            server.sendToTCP(connection.getID(), p_same);
                            connection.close();
                        } else {
                            // Add active client username to PacketUserList.
                            PacketUserList user_list = new PacketUserList();
                            Set<String> user_set = clients.keySet();
                            // If user_set has existing data add their username
                            // to user_list else create a new user_list.
                            if (!user_set.isEmpty()){
                                user_list.user_list.addAll(clients.keySet());
                            }else {
                                user_list.user_list = new ArrayList<String>();
                            }
                            // Send connection ID to user_list.
                            server.sendToTCP(connection.getID(), user_list);

                            // When a client is successfully connected notify
                            // all active users by their username.
                            clients.put(p1.username, connection);
                            Packet2ClientConnected p2 = new Packet2ClientConnected();
                            p2.clientName = p1.username;
                            server.sendToAllExceptTCP(connection.getID(), p2);
                        }
                    }
                    // When a client sends a message it will be sent to all active clients.
                    else if (object instanceof Packet4Chat) {
                        Packet4Chat p4 = (Packet4Chat) object;
                        server.sendToAllTCP(p4);
                    }
                }
            }

            // Run when a client disconnects.
            public void disconnected(Connection connection) {
                Packet3ClientDisconnect p3 = new Packet3ClientDisconnect();

                // Go through each client in the clients HashMap.
                Iterator it = clients.entrySet().iterator();
                String username = "";

                // While there's still active connections in the clients
                // HashMap match the client's username who is disconnecting
                // then break out from function when found.
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry) it.next();
                    if (pairs.getValue().equals(connection)) {
                        username = (String) pairs.getKey();
                        break;
                    }
                }
                // If the username is not equal to "empty" remove clients
                // username from the clients HashMap then notify all active
                // clients except for the person disconnecting.
                if (!username.equalsIgnoreCase("")) {
                    p3.clientName = username;
                    clients.remove(p3.clientName);
                    server.sendToAllExceptTCP(connection.getID(), p3);
                }
            }
        });

        // Register Packet classes for server operations.
        server.getKryo().register(Packet.class);
        server.getKryo().register(Packet1Connect.class);
        server.getKryo().register(Packet2ClientConnected.class);
        server.getKryo().register(Packet3ClientDisconnect.class);
        server.getKryo().register(Packet4Chat.class);
        server.getKryo().register(PacketSameUser.class);
        server.getKryo().register(ArrayList.class);
        server.getKryo().register(PacketUserList.class);

    }
}