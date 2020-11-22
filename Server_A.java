import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

public class Server_A {

    /***********************    GLOBAL VARIABLES    ***********************/
    private static DatagramSocket serverASocket; // For receiving from client
    
    // For receiving from server B
    private static InetAddress serverB_Address; 
    private static int serverB_port;

    // For database
    private static DB_interface db_int;
    private static Boolean nameExists = false;

    //TODO: before demo, we need to modify the code to run with distinct IP addresses.
    static {
        try {
            serverB_Address = InetAddress.getLocalHost(); //TO BE CHANGED TO DISTINCT IP
            serverASocket = new DatagramSocket(3000);   //@@@@@@@@@@@@@CHANGE TO PROPER VALUE FOR SERVER_B
            serverB_port = 4001;                        //@@@@@@@@@@@@@CHANGE TO PROPER VALUE FOR SERVER_B
        } catch (UnknownHostException | SocketException e) {
            System.out.println("Server address error.");
            e.printStackTrace();
        }
    }
    /*********************************************************************/

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object[] deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Object[]) is.readObject();
    }

    public static void sendMessage(Object[] message, byte[] byteMessage, DatagramPacket packet) {
        // Send message to client
        if (message != null) {
            DatagramPacket replyPacket = null;

            try {
                byteMessage = serialize(message);
            } catch (IOException e) {
                System.out.println("Message not serialized.");
                e.printStackTrace();
            }

            replyPacket = new DatagramPacket(byteMessage, byteMessage.length, packet.getSocketAddress());
            try {
                serverASocket.send(replyPacket);
            } catch (IOException e) {
                System.out.println("Message not sent.");
                e.printStackTrace();
            }
        }
    }

    // Start DB make sure to compile with: javac -cp <path/.jar file>: Server_A.java
    // Run DB with: java -cp <path/.jar file>: Server_A
    public static void restartDB() {
        db_int = new DB_interface();
        try {
            db_int.connect();
            System.out.println("Initial SQL DB connection activated...");
            db_int.disconnect();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TODO: Implement database search function for NAME, LIST OF SUBJECTS

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("Starting Server...");

        // For receiving from client
        DatagramPacket receivePacket = null;

        // For replying back to client
        byte[] replyDataClient = new byte[65535];
        Object[] messageReplyClient = null;

        // For replying back to server B
        DatagramPacket replyServerPacket = null;
        Object[] messageReplyServer = null;
        Boolean sendServerFlag = false;

        // TODO: Implement timer duration for while loop, before the loop ends, and then tells the other server to take over
        Boolean serverActive = true;    //@@@@@@@@@@@@@@@@@@@@@@@@@CHANGE TO FALSE WHEN COPIED TO SERVER_B
        Scanner sc = new Scanner(System.in);
        // Timer time = new Timer();

        // Start DB make sure to compile with: javac -cp <path/.jar file>: Server_A.java
        // Run DB with: java -cp <path/.jar file>: Server_A
        restartDB(); 


        while (true) {

            // if (sc.hasNextLine() && serverActive.equals(false)) {
            //     String updateServer = sc.next();

            //     if (updateServer.equalsIgnoreCase("UPDATE-SERVER")) {
            //         System.out.println("Please input comma/space seperated IP and socket: ");
            //         String ipAndSocket = sc.next();
            //         String[] listIpAndSocket = ipAndSocket.split("\\s+"); //[0] = ip, [1] socket
            //         messageReplyServer = new Object[3];
            //         messageReplyServer[0] = "UPDATE-SERVER";
            //         messageReplyServer[1] = listIpAndSocket[0].toString();
            //         messageReplyServer[2] = listIpAndSocket[1].toString();

            //         sendServerFlag = true;
            //     }
            // }
            
            //--------------------------------Receiving client packet data----------------------------------
            receivePacket = new DatagramPacket(replyDataClient, replyDataClient.length); //Datagram receiving size
            serverASocket.receive(receivePacket);    //Receive the data in a buffer
            
            // Print out message sent to the server
            Object[] messageListClient = deserialize(receivePacket.getData());

            // Hold message type from the message sent to the server
            String messageTypeReceived = messageListClient[0].toString(); 
            //----------------------------------------------------------------------------------------------


            if (serverActive.equals(true)) {
                // Receive server updated IP and socket
                if (messageTypeReceived.equalsIgnoreCase("UPDATE-SERVER")) {
                    serverB_Address = InetAddress.getByName(messageListClient[1].toString());
                    serverB_port = (int) messageListClient[2];
                    System.out.println("IP Changed to: " + serverB_Address + " port: " + serverB_port);
                }

                // Add user to server
                if (messageTypeReceived.equalsIgnoreCase("REGISTER")) {
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        String[] userInfo = db_int.getUserInfo(messageListClient[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        }
                        else {
                            nameExists = false;
                        }

                        // Reply to client if the name doesn't exist
                        if (nameExists.equals(false)) {
                            messageReplyClient = new Object[2];
                            messageReplyClient[0] = "REGISTERED";
                            messageReplyClient[1] = messageListClient[1].toString();

                            // Print out contents from client's message
                            for (int i = 0; i < messageListClient.length; i++) {
                                System.out.println("Received: " + messageListClient[i].toString());
                            }
                            System.out.println("Message Received From: " + receivePacket.getSocketAddress().toString());

                            // Add user to DB
                            String userFullAddress = messageListClient[3].toString() + ":" + messageListClient[4].toString();
                            db_int.createNewUserAccount(messageListClient[2].toString(), userFullAddress);
                        }
                        else {
                            messageReplyClient = new Object[3];
                            messageReplyClient[0] = "REGISTER-DENIED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = "Name already in use.";

                            System.out.println("REGISTER-DENIED: User already exists!");
                        }

                        db_int.disconnect(); // Close DB after checking for user

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    sendServerFlag = true;  //Send to server
                }

                // Remove user from server
                else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER")) {
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        String[] userInfo = db_int.getUserInfo(messageListClient[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        }
                        else {
                            nameExists = false;
                        }

                        // Remove user if it exists in DB
                        if (nameExists.equals(true)) {
                            System.out.println("DE-REGISTER SUCCESS: deleting user <" + messageListClient[2].toString() + ">...");

                            // Delete user from DB
                            db_int.deleteUser(messageListClient[2].toString());
                        }

                        else {
                            System.out.println("DE-REGISTER FAILED: user <" + messageListClient[2].toString()  + "> does not exist...");
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    sendServerFlag = true;  //Send to server
                }

                // Update user's IP Address and Socket#
                else if (messageTypeReceived.equalsIgnoreCase("UPDATE")) {
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        String[] userInfo = db_int.getUserInfo(messageListClient[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        }
                        else {
                            nameExists = false;
                        }

                        if (nameExists.equals(true)) {
                            System.out.println("UPDATE-CONFIRMED: updating user <" + userInfo[0] + "> address...");
                            System.out.println("IP: " + messageListClient[3].toString() + " Socket#: " + messageListClient[4].toString());
                            
                            // Send message to client
                            messageReplyClient = new Object[5];
                            messageReplyClient[0] = "UPDATE-CONFIRMED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = messageListClient[2].toString();
                            messageReplyClient[3] = messageListClient[3].toString();
                            messageReplyClient[4] = messageListClient[4].toString();

                            // Update user info in DB
                            String userFullAddress = messageListClient[3].toString() + ":" + messageListClient[4].toString();
                            db_int.updateAddress(messageListClient[2].toString(), userFullAddress);
                        }

                        else {
                            System.out.println("UPDATE-DENIED: user <" + userInfo[0]  + "> does not exist... not deleting");

                            // Send message to client
                            messageReplyClient = new Object[3];
                            messageReplyClient[0] = "UPDATE-DENIED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = "Name does not exist";
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    sendServerFlag = true;  //Send to server
                }

                // Add subjects of interest to user
                else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS")) {
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);
                    
                    //if (if name exists && subject is from COEN)
                    messageReplyClient = new Object[4];
                    messageReplyClient[0] = "SUBJECTS-UPDATED";
                    messageReplyClient[1] = messageListClient[1].toString();
                    messageReplyClient[2] = messageListClient[2].toString();
                    messageReplyClient[3] = subjectList;

                    String stringList = "";
                    System.out.println("Number of subjects: " + subjectList.size());
                    for (String subject : subjectList) {
                        System.out.println("Subject: " + subject);
                        stringList += subject + ","; 
                    }
                    System.out.println("Subject List: " + stringList.substring(0, stringList.length() - 1));

                    /*
                    // (if name doesn't exist || subject is NOT COEN)
                    messageReplyClient = new Object[4];
                    messageReplyClient[0] = "SUBJECTS-REJECTED";
                    messageReplyClient[1] = messageListClient[1].toString();
                    messageReplyClient[2] = messageListClient[2].toString();
                    messageReplyClient[3] = subjectList;
                    */
                    sendServerFlag = true;  //Send to server
                }

                // Send message to users with subject of interest
                else if (messageTypeReceived.equalsIgnoreCase("PUBLISH")) {
                    /*
                    // if (name exists && user.subject exists)
                    // for (users with subject, do this for all)
                    currentServer = InetAddress.getByName("NAME.IP");
                    messageReplyClient = new Object[4];
                    messageReplyClient[0] = "MESSAGE";
                    messageReplyClient[1] = messageListClient[2].toString();
                    messageReplyClient[2] = messageListClient[3].toString();
                    messageReplyClient[3] = messageListClient[4].toString();

                    // if (name doesn't exist && subject doesn't exist)
                    messageReplyClient = new Object[3];
                    messageReplyClient[0] = "PUBLISH-DENIED";
                    messageReplyClient[1] = messageListClient[1].toString();
                    messageReplyClient[2] = "Name does not exist.";
                    */
                }

                // TODO: Implement timer to set serverActive to true or false based on time and serverSwap to true
                // TODO: Change this so that this is executable directly from file itself without needing user input
                //@@@@@@@@@@@@@@@@@@@@@@@@Important to change SOCKET to 3000 if on server_B@@@@@@@@@@@@@@@@@@@@@
                // Change server to receive client data.
                else if (messageTypeReceived.equalsIgnoreCase("CHANGE-SERVER")) {
                    //if (name exists)
                    messageReplyClient = new Object[3];
                    messageReplyClient[0] = "CHANGE-SERVER";
                    messageReplyClient[1] = InetAddress.getLocalHost(); //WARNING, CHANGE TO ACTUAL SERVER IP
                    messageReplyClient[2] = 4001;                       //WARNING, CHANGE TO ACTUAL SERVER SOCKET

                    sendServerFlag = true;  //Send to server
                }

                // Send message to client
                if (messageReplyClient != null) {
                    sendMessage(messageReplyClient, replyDataClient, receivePacket);
                }
                replyDataClient = new byte[65535]; // Clear the buffer after every message
            }

            //------------------------------------Receiving server messages--------------------------------
            //------------------------------------While server inactive------------------------------------
            else if (serverActive.equals(false)) {

                if (messageTypeReceived.equalsIgnoreCase("REGISTERED")) {
                    for (int i = 0; i < messageListClient.length; i++) {
                        System.out.println("Received: " + messageListClient[i].toString());
                    }
                    System.out.println("Message Received From: " + receivePacket.getSocketAddress().toString());
                }

                else if (messageTypeReceived.equalsIgnoreCase("REGISTER-DENIED")) {
                    for (int i = 0; i < messageListClient.length; i++) {
                        System.out.println("Received: " + messageListClient[i].toString());
                    }
                    System.out.println("Message Received From: " + receivePacket.getSocketAddress().toString());
                    System.out.println("Register-denied: user <" + messageListClient[2].toString() + "> already exists...");
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER")) {
                    System.out.println("De-register: user <" + messageListClient[1].toString() + "> has been de-registered...");
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("INVALID-DE-REGISTER")) {
                    System.out.println("Invalid de-register: user <" + messageListClient[1].toString() + "> does not exist...");
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("UPDATE-CONFIRMED")) {
                    System.out.println("User <" + messageListClient[2].toString() + "> IP Changed to: " + messageListClient[3].toString() + " port: " + messageListClient[4].toString());
                }

                // Receive server updated subjects and print
                else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS-UPDATED")) {
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);

                    String stringList = "";
                    for (String subject : subjectList) {
                        stringList += subject + ","; 
                    }

                    System.out.println("User <" + messageListClient[2].toString() + "> updated " + subjectList.size() + " subjects: " + stringList.substring(0, stringList.length() - 1));
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("CHANGE-SERVER")) {
                    System.out.println("Switched servers: Server A active.");
                    serverActive = true;
                    sendServerFlag = false;
                }
                
                //TODO: Why is this code not reachable while running the program? Only works when i put it at START of while loop
                else if (sc.hasNextLine()) {
                    String updateServer = sc.next();

                    if (updateServer.equalsIgnoreCase("UPDATE-SERVER")) {
                        System.out.println("Please input comma/space seperated IP and socket: ");
                        String ipAndSocket = sc.next();
                        String[] listIpAndSocket = ipAndSocket.split("\\s+"); //[0] = ip, [1] socket
                        messageReplyServer = new Object[3];
                        messageReplyServer[0] = "UPDATE-SERVER";
                        messageReplyServer[1] = listIpAndSocket[0].toString();
                        messageReplyServer[2] = listIpAndSocket[1].toString();

                        sendServerFlag = true;
                    }
                }
            }

            //---------------------------------------Send to server B----------------------------------------
            //-------------------------------------While server active---------------------------------------
            if (sendServerFlag.equals(true) && serverActive.equals(true)) {

                // Add user to server
                if (messageTypeReceived.equalsIgnoreCase("REGISTER")) {
                    // Name doesn't exist
                    if (nameExists.equals(false)) {
                        messageReplyServer = new Object[5];
                        messageReplyServer[0] = "REGISTERED";
                        messageReplyServer[1] = messageListClient[1].toString(); // RQ#
                        messageReplyServer[2] = messageListClient[2].toString(); // Name
                        messageReplyServer[3] = messageListClient[3].toString(); // IP ADDRESS
                        messageReplyServer[4] = messageListClient[4].toString(); // SOCKET#
                    }
                    // Name exists
                    else {
                    messageReplyServer = new Object[5];
                    messageReplyServer[0] = "REGISTER-DENIED";
                    messageReplyServer[1] = messageListClient[1].toString(); // RQ#
                    messageReplyServer[2] = messageListClient[2].toString(); // Name
                    messageReplyServer[3] = messageListClient[3].toString(); // IP ADDRESS
                    messageReplyServer[4] = messageListClient[4].toString(); // SOCKET#
                    }
                }

                // Remove user from server    
                else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER")) {
                    // If name exists
                    if (nameExists.equals(true)) {
                        messageReplyServer = new Object[2];
                        messageReplyServer[0] = "DE-REGISTER";
                        messageReplyServer[1] = messageListClient[2].toString();
                    }

                    else {
                        messageReplyServer = new Object[2];
                        messageReplyServer[0] = "INVALID-DE-REGISTER";
                        messageReplyServer[1] = messageListClient[2].toString();
                    }
                }

                // Update user's IP Address and Socket#
                else if (messageTypeReceived.equalsIgnoreCase("UPDATE")) {
                    // If user exists, ONLY tell other server
                    // Do not tell other server is user DOES NOT exist
                    if (nameExists.equals(true)) {
                        messageReplyServer = new Object[5];
                        messageReplyServer[0] = "UPDATE-CONFIRMED";
                        messageReplyServer[1] = messageListClient[1].toString();
                        messageReplyServer[2] = messageListClient[2].toString();
                        messageReplyServer[3] = messageListClient[3].toString();
                        messageReplyServer[4] = messageListClient[4].toString();
                    }
                }
                
                // Add subjects of interest to user
                else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS")) {
                    //if (if name exists && subject is from COEN)
                    messageReplyServer = new Object[4];
                    messageReplyServer[0] = "SUBJECTS-UPDATED";
                    messageReplyServer[1] = messageListClient[1].toString();
                    messageReplyServer[2] = messageListClient[2].toString();
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);
                    messageReplyServer[3] = subjectList;
                }

                else if (messageTypeReceived.equalsIgnoreCase("CHANGE-SERVER")) {
                    System.out.println("Switched servers: Server A inactive.");
                    messageReplyServer = new Object[1];
                    messageReplyServer[0] = "CHANGE-SERVER";
                    serverActive = false;
                }

                //--------------------------Send message to server B----------------------------------
                if (messageReplyServer != null) {
                    // sendMessage(messageReplyServer, replyDataServer, ) //TODO: Fix so it works with method

                    byte[] replyDataServer = new byte[65535];
                    sendServerFlag = false;
                    replyDataServer = serialize(messageReplyServer);
                    replyServerPacket = new DatagramPacket(replyDataServer, replyDataServer.length, serverB_Address, serverB_port);
                    serverASocket.send(replyServerPacket);
                    replyDataServer = new byte[65535]; // Clear the buffer after every message
                }
            }
        }
    }
}
