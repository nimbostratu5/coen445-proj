import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.*;

public class Server_A {

    /***********************    GLOBAL VARIABLES    ***********************/
    private static DatagramSocket serverASocket; // For receiving from client
    
    // For receiving from server B
    private static InetAddress serverB_Address; 
    private static int serverB_Port;

    // For database
    private static DB_interface db_int;
    private static Boolean nameExists = false;

    //TODO: before demo, we need to modify the code to run with distinct IP addresses.
    static {
        try {
            serverB_Address = InetAddress.getLocalHost(); //TO BE CHANGED TO DISTINCT IP
            serverASocket = new DatagramSocket(3000);   //@@@@@@@@@@@@@CHANGE TO PROPER VALUE FOR SERVER_B
            serverB_Port = 4001;                        //@@@@@@@@@@@@@CHANGE TO PROPER VALUE FOR SERVER_B
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

    // If replying to the client using the same packet info
    public static void sendMessage(Object[] message, DatagramPacket packet) {
        
        // Create packet and array to send data with
        DatagramPacket replyPacket = null;
        byte[] replyDataServer = new byte[65535];

        // Serialize the message into data
        try {
            replyDataServer = serialize(message);
        } catch (IOException e) {
            System.out.println("Message not serialized.");
            e.printStackTrace();
        }

        // Populate the packet and send the message
        System.out.println("getSocketAddress: " + packet.getSocketAddress().toString());
        replyPacket = new DatagramPacket(replyDataServer, replyDataServer.length, packet.getSocketAddress());
        try {
            serverASocket.send(replyPacket);
        } catch (IOException e) {
            System.out.println("Message not sent.");
            e.printStackTrace();
        }
    }

    // To send a message to a specific ip and port as opposed to a reply
    public static void sendMessage(Object[] message, String ip, String port) {

        // Create packet and data array to use to reply with
        DatagramPacket replyPacket = null;
        byte[] replyDataServer = new byte[65535];

        // Serialize message first
        try {
            replyDataServer = serialize(message);
        } catch (IOException e) {
            System.out.println("Message not serialized.");
            e.printStackTrace();
        }

        // Create packet with specific ip and port to send to 
        InetAddress sendIp = null;
        int portNumber = 9000;

        // Split the address in case it is written as user/ip
        String[] serverAddressSplit = ip.toString().split("/");

        // If address contains user/IP, split and take only IP
        if (serverAddressSplit.length > 1) {
            ip = serverAddressSplit[1];
        } 
        // If address only contains IP
        else {
            ip = serverAddressSplit[0];
        }

        // Populate packet to send with
        try {
            sendIp = InetAddress.getByName(ip);
            portNumber = Integer.parseInt(port);
            replyPacket = new DatagramPacket(replyDataServer, replyDataServer.length, sendIp, portNumber);
        } catch (UnknownHostException e) {
            System.out.println("Server address error.");
            e.printStackTrace();
        }
        
        // Use socket to send packet
        try {
            serverASocket.send(replyPacket);
        } catch (IOException e) {
            System.out.println("Message not sent.");
            e.printStackTrace();
        }
    }

    // Return subjects if they are from coen (doesn't matter if capitalized or not, AND subject can be changed)
    public static ArrayList<String> areSubject(ArrayList<String> clientSubjectList) {

        // Create an array of subjects which only accept COEN subjects
        ArrayList<String> acceptedSubjectList = new ArrayList<>();

        for (int i = 0; i < clientSubjectList.size(); i++) {
            String subject = clientSubjectList.get(i).toLowerCase();

            if (subject.contains("coen")) {
                acceptedSubjectList.add(subject);
                System.out.println("Subject: " + subject);
            }
        }
        return acceptedSubjectList;
    }

    // Start DB make sure to compile with: javac -cp <path/.jar file>: Server_A.java
    // Run DB with: java -cp <path/.jar file>: Server_A
    public static void restartDB() {
        // Create instance of DB
        db_int = new DB_interface();

        // Test if DB can connect
        try {
            db_int.connect();
            System.out.println("Initial SQL DB connection activated...");
            db_int.disconnect();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("Starting Server...");

        // For receiving from client
        DatagramPacket receivePacket = null;

        // For replying back to client
        byte[] replyDataClient = new byte[65535];
        Object[] messageReplyClient = null;

        // For replying back to server B
        Object[] messageReplyServer = null;
        Boolean sendServerFlag = false;

        // TODO: Implement timer duration for while loop, before the loop ends, and then tells the other server to take over
        Boolean serverActive = true;    //@@@@@@@@@@@@@@@@@@@@@@@@@CHANGE TO FALSE WHEN COPIED TO SERVER_B
        Scanner sc = new Scanner(System.in);
        // Timer time = new Timer();

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
                    serverB_Port = (int) messageListClient[2];
                    System.out.println("IP Changed to: " + serverB_Address + " port: " + serverB_Port);
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

                        // TODO: Check if subject already exists in DB for specific user

                        // Get list of subjects sent
                        ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);
                        ArrayList<String> acceptedSubjectList = areSubject(subjectList);

                        if (nameExists.equals(true) && acceptedSubjectList.size() != 0) {
                            messageReplyClient = new Object[4];
                            messageReplyClient[0] = "SUBJECTS-UPDATED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = messageListClient[2].toString();
                            messageReplyClient[3] = acceptedSubjectList;

                            // Save subjects in DB
                            String stringList = "";
                            System.out.println("Number of valid subjects: " + acceptedSubjectList.size());
                            for (String subject : acceptedSubjectList) {
                                db_int.registerToSubject(messageListClient[2].toString(), subject);
                                stringList += subject + ","; 
                            }
                            System.out.println("Subject List: " + stringList.substring(0, stringList.length() - 1));
                        }

                        else {
                            messageReplyClient = new Object[4];
                            messageReplyClient[0] = "SUBJECTS-REJECTED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = messageListClient[2].toString();
                            messageReplyClient[3] = subjectList;
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    sendServerFlag = true;  //Send to server
                }

                // TODO: Questions to ask Ion:
                // 1. How can i use the subject name (not id) to find all users associated to subject (show interface method getAllUsersSubscribed)
                // 2. Looking at subject_313, this one returns an address which is missing the port (127.0.0.1) versus users shows address with port (127.0.0.1:9000)

                // Send message to users with subject of interest ONE SUBJECT PUBLISHED AT A TIME
                else if (messageTypeReceived.equalsIgnoreCase("PUBLISH")) {
                    try {
                        db_int.connect();

                        // Check if client sender's name exists in DB
                        String[] userInfo = db_int.getUserInfo(messageListClient[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        }
                        else {
                            nameExists = false;
                        }

                        // Get subject of interest and message to publish
                        String subject = messageListClient[3].toString();
                        String text = messageListClient[4].toString();

                        // Get subject ID from the list of all subjects
                        int subjectId = -1;
                        ArrayList<String[]> subjectList = db_int.getAllExistingSubjects();

                        for (int i = 0; i < subjectList.size(); i++) {
                            int tempId = Integer.parseInt(subjectList.get(i)[0]);
                            String tempSubject = subjectList.get(i)[1];

                            if (subject.equals(tempSubject)) {
                                subjectId = tempId;
                                break;
                            }
                        }

                        // Get all users subscribed to the subject of interest
                        ArrayList<String[]> usersSubscribed = new ArrayList();
                        if (subjectId != -1) {
                            usersSubscribed = db_int.getAllUsersSubscribed(subjectId);
                        }

                        // If the client sender is registered and if there are users subscribed to the subject
                        if (nameExists && usersSubscribed.size() > 0 && usersSubscribed != null && subjectId != -1) {

                            // For each user subscribed to the subject, send them a message
                            for (int i = 0; i < usersSubscribed.size(); i++) {

                                // Get the subscribed user's name and address to send to
                                String name = usersSubscribed.get(i)[0];
                                String fullAddress = usersSubscribed.get(i)[1];
                                String[] addressSplit = fullAddress.split(":"); // IP[0] and Port[1]

                                // Create message to send to the subscribed user
                                messageReplyClient = new Object[4];
                                messageReplyClient[0] = "MESSAGE";
                                messageReplyClient[1] = name;
                                messageReplyClient[2] = subject;
                                messageReplyClient[3] = text;

                                // Send message
                                if (messageReplyClient != null) {
                                    sendMessage(messageReplyClient, addressSplit[0], addressSplit[1]);
                                }
                            }
                            
                            // Skip sending message back to user publisher
                            messageReplyClient = null;
                        }
                        
                        // If sender isn't registered 
                        else if (!nameExists) {
                            // Create message to send
                            messageReplyClient = new Object[3];
                            messageReplyClient[0] = "PUBLISH-DENIED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = "Unregistered user not allowed to publish";
                        }
                        // If no users subscribed to the subject of interest
                        else {
                            // Create message to send
                            messageReplyClient = new Object[3];
                            messageReplyClient[0] = "PUBLISH-DENIED";
                            messageReplyClient[1] = messageListClient[1].toString();
                            messageReplyClient[2] = "No users subscribed to subject of interest";
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                // | FETCH-SUBJECTS | RQ# | USERNAME | IP | PORT
                else if (messageTypeReceived.equalsIgnoreCase("FETCH-SUBJECTS")) {
                    try {
                        db_int.connect(); 
                    
                        // Check if client sender's name exists in DB
                        String[] userInfo = db_int.getUserInfo(messageListClient[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        }
                        else {
                            nameExists = false;
                        }

                        // Ip and port of address passed
                        String ip = messageListClient[3].toString();
                        String port = messageListClient[3].toString();

                        // Return id, subject, usercount and last post
                        ArrayList<String[]> subjects = db_int.getAllExistingSubjects();
                        ArrayList<String> allSubjects = new ArrayList();

                        for (int i = 0; i < subjects.size(); i++) {
                            allSubjects.add(subjects.get(i)[1]);    // Get all subject names
                        }
                        
                        // | FETCH-SUCCESS | RQ# | USERNAME | SUBJECTLIST |
                        messageReplyClient = new Object[4];
                        messageReplyClient[0] = "FETCH-SUCCESS";
                        messageReplyClient[1] = messageListClient[1].toString(); //rq#
                        messageReplyClient[2] = messageListClient[2].toString(); //username
                        messageReplyClient[3] = allSubjects;

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    
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
                    sendMessage(messageReplyClient, receivePacket);
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
                    System.out.println("Registered: User <" + messageListClient[2].toString() + "> registered and received from: " + receivePacket.getSocketAddress().toString());
                }

                else if (messageTypeReceived.equalsIgnoreCase("REGISTER-DENIED")) {
                    for (int i = 0; i < messageListClient.length; i++) {
                        System.out.println("Received: " + messageListClient[i].toString());
                    }
                    System.out.println("Message Received From: " + receivePacket.getSocketAddress().toString());
                    System.out.println("Register-Denied: User <" + messageListClient[2].toString() + "> already exists...");
                }

                else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER")) {
                    System.out.println("De-Register: User <" + messageListClient[1].toString() + "> has been de-registered...");
                }

                // else if (messageTypeReceived.equalsIgnoreCase("INVALID-DE-REGISTER")) {
                //     System.out.println("Invalid de-register: user <" + messageListClient[1].toString() + "> does not exist...");
                // }

                else if (messageTypeReceived.equalsIgnoreCase("UPDATE-CONFIRMED")) {
                    System.out.println("User <" + messageListClient[2].toString() + "> IP Changed to: " + messageListClient[3].toString() + " port: " + messageListClient[4].toString());
                }

                // Receive server updated subjects and print
                else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS-UPDATED")) {

                    // Get list of correct subjects from other server
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);

                    String stringList = "";
                    for (String subject : subjectList) {
                        stringList += subject + ","; 
                    }

                    System.out.println("Subjects-Updated: User <" + messageListClient[2].toString() + "> updated " + subjectList.size() + " subjects: " + stringList.substring(0, stringList.length() - 1));
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("CHANGE-SERVER")) {
                    System.out.println("Change-Server: Server A active.");
                    serverActive = true;
                    sendServerFlag = false;
                }
                
                // TODO: Figure out how to place this within the while loop to allow it to be trigged while running
                // TODO: UPDATE-SERVER, then take input??
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
                    // else {
                    //     messageReplyServer = new Object[2];
                    //     messageReplyServer[0] = "INVALID-DE-REGISTER";
                    //     messageReplyServer[1] = messageListClient[2].toString();
                    // }
                }

                // Update user's IP Address and Socket#
                else if (messageTypeReceived.equalsIgnoreCase("UPDATE")) {
                    // ONLY reply to server if user exists
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

                    // Acquire list to send back to user
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);

                    // Acquire list of ONLY coen subjects
                    ArrayList<String> acceptedSubjectList = areSubject(subjectList);

                    if (nameExists.equals(true) && acceptedSubjectList.size() != 0) {
                        messageReplyServer = new Object[4];
                        messageReplyServer[0] = "SUBJECTS-UPDATED";
                        messageReplyServer[1] = messageListClient[1].toString();
                        messageReplyServer[2] = messageListClient[2].toString();
                        messageReplyServer[3] = acceptedSubjectList;
                    }
                }

                // Change active servers
                else if (messageTypeReceived.equalsIgnoreCase("CHANGE-SERVER")) {
                    System.out.println("Switched servers: Server A inactive.");
                    messageReplyServer = new Object[1];
                    messageReplyServer[0] = "CHANGE-SERVER";
                    serverActive = false;
                }

                //--------------------------Send message to server B----------------------------------
                if (messageReplyServer != null) {

                    // Send message to server
                    sendMessage(messageReplyServer, serverB_Address.toString(), String.valueOf(serverB_Port));

                    // Don't send to server again
                    sendServerFlag = false;
                }
            }
        }
    }
}
