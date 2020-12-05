import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Worker implements Runnable{

    private Object[] receivedMsg;
    private String workerName;
    private static DatagramSocket serverSocket;
    protected String clientIP;
    protected int clientPort;

    protected String otherServerIP;
    protected int otherServerPort;

    private Semaphore logSem; 
    // For database
    private static DB_interface db_int;
    private static Boolean nameExists = false;
    private static boolean serverUpdate = false;


    public Worker(String clientIP, int clientPort, byte[] receivedMsg,DatagramSocket serverSocket, String otherServerIP, int otherServerPort, Semaphore logSem, DB_interface db_int) throws IOException, ClassNotFoundException {
        this.clientIP = clientIP;
        this.clientPort =clientPort;
        this.receivedMsg = deserialize(receivedMsg);
        this.otherServerIP = otherServerIP;
        this.otherServerPort = otherServerPort;
        this.serverSocket = serverSocket;
        this.logSem = logSem;
        this.db_int = db_int;
    }

    public Worker(String otherServerIP, int otherServerPort, DatagramSocket serverSocket, Semaphore logSem){
        this.serverSocket = serverSocket;
        this.otherServerIP = otherServerIP;
        this.otherServerPort = otherServerPort;
        this.logSem = logSem;
        serverUpdate = true;
    }

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

    public static void sendMessage(Object[] message, String ip, int port) throws UnknownHostException {

        byte[] replyDataServer = new byte[65535];
        // Serialize the message into data
        try {
            replyDataServer = serialize(message);
        } catch (IOException e) {
            System.out.println("Message not serialized.");
            e.printStackTrace();
        }

        // Populate the packet and send the message
       // System.out.println("getSocketAddress: " + packet.getSocketAddress().toString());
        DatagramPacket replyPacket = new DatagramPacket(replyDataServer, replyDataServer.length, InetAddress.getByName(ip), port);
        try {
            serverSocket.send(replyPacket);
        } catch (IOException e) {
            System.out.println("Message not sent.");
            e.printStackTrace();
        }
    }

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

    @Override
    public void run() {

        Object[] replyMsgClient = null;
        Object[] replyMsgServer = null;

        if(serverUpdate){
            replyMsgClient = new Object[3];
            try {
                sendMessage(replyMsgClient,otherServerIP,otherServerPort);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            serverUpdate=false;
        }
        else {

            switch (receivedMsg[0].toString()) {

                case "REGISTER":
                    //to client: reply REGISTERED or REGISTER-DENIED
                    //to other server: reply REGISTERED or REGISTER-DENIED
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        String[] userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        } else {
                            nameExists = false;
                        }

                        // Reply to client if the name doesn't exist
                        if (nameExists.equals(false)) {
                            replyMsgClient = new Object[2];
                            replyMsgClient[0] = "REGISTERED";
                            replyMsgClient[1] = receivedMsg[1].toString();

                            // Print out contents from client's message
                            for (int i = 0; i < receivedMsg.length; i++) {
                                System.out.println("Received: " + receivedMsg[i].toString());
                            }
                            System.out.println("Message Received From: " + receivedMsg[3] + ":" + receivedMsg[4]);

                            // Add user to DB
                            String userFullAddress = receivedMsg[3].toString() + ":" + receivedMsg[4].toString();
                            db_int.createNewUserAccount(receivedMsg[2].toString(), userFullAddress);
                        } else {
                            replyMsgClient = new Object[3];
                            replyMsgClient[0] = "REGISTER-DENIED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = "Name already in use.";
                            System.out.println("REGISTER-DENIED: User already exists!");
                        }

                        db_int.disconnect(); // Close DB after checking for user

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // TODO: 2020-11-28 sendServerFlag = true;  //Send to server
                    break;

                case "DE-REGISTER":

                    if (Server.isServing.get()) {
                        try {
                            db_int.connect();

                            // Check if name exists in DB
                            String[] userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                            if (userInfo != null) {
                                nameExists = true;
                            } else {
                                nameExists = false;
                            }

                            // Remove user if it exists in DB
                            if (nameExists.equals(true)) {
                                System.out.println("DE-REGISTER SUCCESS: deleting user <" + receivedMsg[2].toString() + ">...");

                                // Delete user from DB
                                db_int.deleteUser(receivedMsg[2].toString());
                            } else {
                                System.out.println("DE-REGISTER FAILED: user <" + receivedMsg[2].toString() + "> does not exist...");
                            }

                            db_int.disconnect(); // Close DB after checking for user
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        // TODO: 2020-11-28  sendServerFlag = true;  //Send to server
                    } else {
                        System.out.println("De-Register: User <" + receivedMsg[1].toString() + "> has been de-registered...");
                    }

                    break;

                case "UPDATE":
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        String[] userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        } else {
                            nameExists = false;
                        }

                        if (nameExists.equals(true)) {
                            System.out.println("UPDATE-CONFIRMED: updating user <" + userInfo[0] + "> address...");
                            System.out.println("IP: " + receivedMsg[3].toString() + " Socket#: " + receivedMsg[4].toString());

                            // Send message to client
                            replyMsgClient = new Object[5];
                            replyMsgClient[0] = "UPDATE-CONFIRMED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = receivedMsg[2].toString();
                            replyMsgClient[3] = receivedMsg[3].toString();
                            replyMsgClient[4] = receivedMsg[4].toString();

                            // Update user info in DB
                            String userFullAddress = receivedMsg[3].toString() + ":" + receivedMsg[4].toString();
                            db_int.updateAddress(receivedMsg[2].toString(), userFullAddress);
                        } else {
                            System.out.println("UPDATE-DENIED: user <" + receivedMsg[0] + "> does not exist... not deleting");

                            // Send message to client
                            replyMsgClient = new Object[3];
                            replyMsgClient[0] = "UPDATE-DENIED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = "Name does not exist";
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // TODO: 2020-11-28   sendServerFlag = true;  //Send to server

                    break;

                case "SUBJECTS":
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        String[] userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        } else {
                            nameExists = false;
                        }

                        // TODO: Check if subject already exists in DB for specific user

                        // Get list of subjects sent
                        ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) receivedMsg[3]);
                        ArrayList<String> acceptedSubjectList = areSubject(subjectList);

                        if (nameExists.equals(true) && acceptedSubjectList.size() != 0) {
                            replyMsgClient = new Object[4];
                            replyMsgClient[0] = "SUBJECTS-UPDATED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = receivedMsg[2].toString();
                            replyMsgClient[3] = acceptedSubjectList;

                            // Save subjects in DB
                            String stringList = "";
                            System.out.println("Number of valid subjects: " + acceptedSubjectList.size());
                            for (String subject : acceptedSubjectList) {
                                db_int.registerToSubject(receivedMsg[2].toString(), subject);
                                stringList += subject + ",";
                            }
                            System.out.println("Subject List: " + stringList.substring(0, stringList.length() - 1));
                        } else {
                            replyMsgClient = new Object[4];
                            replyMsgClient[0] = "SUBJECTS-REJECTED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = receivedMsg[2].toString();
                            replyMsgClient[3] = subjectList;
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    // TODO: 2020-11-28  sendServerFlag = true;  //Send to server
                    break;

                case "PUBLISH":
                    //to client: reply MESSAGE to all users subscribed to specified subject else PUBLISH-DENIED to client only
                    //to other server: nothing
                    break;


                case "UPDATE-SERVER":
                    //update the IP/Port of other server

                    //serverB_Address = InetAddress.getByName(messageListClient[1].toString());
                    //serverB_Port = (int) messageListClient[2];
                    //System.out.println("IP Changed to: " + serverB_Address + " port: " + serverB_Port);
                    break;

                case "REGISTERED":
                    for (int i = 0; i < receivedMsg.length; i++) {
                        System.out.println("Received: " + receivedMsg[i].toString());
                    }
                    System.out.println("Registered: User <" + receivedMsg[2].toString() + "> registered and received from: " + clientIP+":"+clientPort);
                    break;

                case "REGISTER-DENIED":
                    for (int i = 0; i < receivedMsg.length; i++) {
                        System.out.println("Received: " + receivedMsg[i].toString());
                    }
                    System.out.println("Message Received From: " + clientIP+":"+clientPort);
                    System.out.println("Register-Denied: User <" + receivedMsg[2].toString() + "> already exists...");
                    break;

                case "UPDATE-CONFIRMED":
                    System.out.println("User <" + receivedMsg[2].toString() + "> IP Changed to: " + receivedMsg[3].toString() + " port: " + receivedMsg[4].toString());
                    break;

                // Receive server updated subjects and print
                case "SUBJECTS-UPDATED":

                    // Get list of correct subjects from other server
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) receivedMsg[3]);

                    String stringList = "";
                    for (String subject : subjectList) {
                        stringList += subject + ",";
                    }

                    System.out.println("Subjects-Updated: User <" + receivedMsg[2].toString() + "> updated " + subjectList.size() + " subjects: " + stringList.substring(0, stringList.length() - 1));
                    break;

                // Receive server updated IP and socket
                case "CHANGE-SERVER":
                    System.out.println("Change-Server: Server A active.");
                    Server.serverStatus.set(false);
                    //sendServerFlag = false;
                    break;

                default:
            }

            if (replyMsgClient != null) {
                try {
                    sendMessage(replyMsgClient, clientIP,clientPort);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            if(replyMsgServer != null){
                try {
                    sendMessage(replyMsgServer,otherServerIP,otherServerPort);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

