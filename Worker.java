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
    private static boolean serverSwap = false;

    private static String command;

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

    public Worker(String otherServerIP, int otherServerPort, DatagramSocket serverSocket, Semaphore logSem, boolean serverSwap){
        this.serverSocket = serverSocket;
        this.otherServerIP = otherServerIP;
        this.otherServerPort = otherServerPort;
        this.logSem = logSem;
        this.serverSwap = serverSwap;
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
        String[] userInfo = null;

        if(serverUpdate){
            replyMsgServer = new Object[3];
            replyMsgServer[0] = "UPDATE-SERVER";
            replyMsgServer[1] = Server.getIP();
            replyMsgServer[2] = serverSocket.getLocalPort();

            try {
                sendMessage(replyMsgServer,otherServerIP,otherServerPort);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            serverUpdate=false;
        }
        else if(serverSwap){

            Server.isServing.set(false);

            System.out.println("Change server triggered by timer. This server is no longer serving.");
            try {
                logSem.acquire();
                Server.logger.logEvent("change server command triggered by timer. This server is no longer serving.");
                logSem.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            replyMsgClient = new Object[3];
            replyMsgClient[0] = "CHANGE-SERVER";
            replyMsgClient[1] = otherServerIP;
            replyMsgClient[2] = otherServerPort;

        }
        else {

            try {
                logSem.acquire();
                Server.logger.logEvent("received msg from "+clientIP+":"+clientPort+": "+receivedMsg[0].toString());
                logSem.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            switch (receivedMsg[0].toString()) {

                /***************************************************************************************************************/
                /*                  2.1. Registration and De-registration                                                      */
                /***************************************************************************************************************/

                case "REGISTER": //    FROM CLIENT

                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        } else {
                            nameExists = false;
                        }
                        
                        replyMsgServer = new Object[5];
                        
                        // Reply to client if the name doesn't exist
                        if (nameExists.equals(false)) {

                            replyMsgServer[0] = "REGISTERED";

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

                            replyMsgServer[0] = "REGISTER-DENIED";
                            
                            replyMsgClient = new Object[3];
                            replyMsgClient[0] = "REGISTER-DENIED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = "Name already in use.";
                            System.out.println("REGISTER-DENIED: User already exists!");
                        }

                        db_int.disconnect(); // Close DB after checking for user

                        replyMsgServer[1] = receivedMsg[1];
                        replyMsgServer[2] = receivedMsg[2];
                        replyMsgServer[3] = receivedMsg[3];
                        replyMsgServer[4] = receivedMsg[4];

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    break;

                case "DE-REGISTER": //    FROM CLIENT + SERVING SERVER

                    if (Server.isServing.get()) {
                        userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        replyMsgServer = new Object[2];
                        replyMsgServer[0] = "DE-REGISTER";
                        replyMsgServer[1] = receivedMsg[2];
                    }
                    else{
                        userInfo = db_int.getUserInfo(receivedMsg[1].toString());
                    }

                    try {
                        db_int.connect();

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
                    } 
                    catch (SQLException e) {
                        e.printStackTrace();
                    }   

                    break;

                case "REGISTERED": //    FROM SERVING SERVER
                    for (int i = 0; i < receivedMsg.length; i++) {
                        System.out.println("Received: " + receivedMsg[i].toString());
                    }
                    System.out.println("Registered: User <" + receivedMsg[2].toString() + "> registered and received from: " + clientIP+":"+clientPort);
                    try {
                        logSem.acquire();
                        Server.logger.logEvent("user "+receivedMsg[2].toString()+" has been registered to the database.");
                        logSem.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    break;

                case "REGISTER-DENIED": //    FROM SERVING SERVER
                    for (int i = 0; i < receivedMsg.length; i++) {
                        System.out.println("Received: " + receivedMsg[i].toString());
                    }
                    System.out.println("Register-Denied: User <" + receivedMsg[2].toString() + "> already exists...");
                    try {
                        logSem.acquire();
                        Server.logger.logEvent("user "+receivedMsg[2].toString()+" has been denied registration because name already exists.");
                        logSem.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    break;


                /***************************************************************************************************************/
                /*                  2.2. Users updating their information                                                      */
                /***************************************************************************************************************/

                case "UPDATE": //    FROM CLIENT

                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        userInfo = db_int.getUserInfo(receivedMsg[2].toString());
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
                            replyMsgServer = new Object[5];
                            replyMsgClient[0] = "UPDATE-CONFIRMED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = receivedMsg[2].toString();
                            replyMsgClient[3] = receivedMsg[3].toString();
                            replyMsgClient[4] = receivedMsg[4].toString();
                            replyMsgServer = replyMsgClient;
                            // Update user info in DB
                            String userFullAddress = receivedMsg[3].toString() + ":" + receivedMsg[4].toString();
                            db_int.updateAddress(receivedMsg[2].toString(), userFullAddress);
                        }
                        else {
                            System.out.println("UPDATE-DENIED: user <" + receivedMsg[2] + "> does not exist... not deleting");

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

                    break;

                case "UPDATE-CONFIRMED": //    FROM SERVING SERVER

                    try {
                        db_int.connect();
                        String event;
                        // Check if name exists in DB
                        userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        } else {
                            nameExists = false;
                        }

                        if (nameExists.equals(true)) {
                            System.out.println("UPDATE-CONFIRMED: updating user <" + userInfo[0] + "> address...");
                            System.out.println("IP: " + receivedMsg[3].toString() + " Socket#: " + receivedMsg[4].toString());

                            // Update user info in DB
                            String userFullAddress = receivedMsg[3].toString() + ":" + receivedMsg[4].toString();
                            db_int.updateAddress(receivedMsg[2].toString(), userFullAddress);
                            event = "update-confirmed: user "+receivedMsg[2].toString()+" IP:Port updated to "+receivedMsg[3].toString()+":"+receivedMsg[4].toString();
                        }
                        else {
                            System.out.println("update request received but could not process: user <" + receivedMsg[2] + "> does not exist.");
                            event = "update request received but could not process: user <" + receivedMsg[2] + "> does not exist.";
                        }

                        db_int.disconnect(); // Close DB after checking for user

                        logSem.acquire();
                        Server.logger.logEvent(event);
                        logSem.release();

                    } catch (SQLException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("User <" + receivedMsg[2].toString() + "> IP Changed to: " + receivedMsg[3].toString() + " port: " + receivedMsg[4].toString());
                    break;


                /***************************************************************************************************************/
                /*                  2.3. Users updating their subjects of interest                                             */
                /***************************************************************************************************************/

                case "SUBJECTS": //    FROM CLIENT
                    try {
                        db_int.connect();

                        // Check if name exists in DB
                        userInfo = db_int.getUserInfo(receivedMsg[2].toString());
                        if (userInfo != null) {
                            nameExists = true;
                        } else {
                            nameExists = false;
                        }

                        // TODO: Check if subject already exists in DB for specific user

                        // Get list of subjects sent
                        ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) receivedMsg[3]);
                        ArrayList<String> acceptedSubjectList = areSubject(subjectList);
                        replyMsgClient = new Object[4];

                        if (nameExists.equals(true) && acceptedSubjectList.size() != 0) {
                            replyMsgServer = new Object[4];
                            replyMsgClient[0] = "SUBJECTS-UPDATED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = receivedMsg[2].toString();
                            replyMsgClient[3] = acceptedSubjectList;
                            replyMsgServer = replyMsgClient;
                            // Save subjects in DB
                            String stringList = "";
                            System.out.println("Number of valid subjects: " + acceptedSubjectList.size());
                            for (String subject : acceptedSubjectList) {
                                db_int.registerToSubject(receivedMsg[2].toString(), subject);
                                stringList += subject + ",";
                            }
                            System.out.println("Subject List: " + stringList.substring(0, stringList.length() - 1));
                        } else {
                            replyMsgClient[0] = "SUBJECTS-REJECTED";
                            replyMsgClient[1] = receivedMsg[1].toString();
                            replyMsgClient[2] = receivedMsg[2].toString();
                            replyMsgClient[3] = subjectList;
                        }

                        db_int.disconnect(); // Close DB after checking for user
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    break;

                case "SUBJECTS-UPDATED": //    SERVING SERVER

                    // Get list of correct subjects from other server
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) receivedMsg[3]);

                    String stringList = "";
                    for (String subject : subjectList) {
                        stringList += subject + ",";
                    }

                    System.out.println("Subjects-Updated: User <" + receivedMsg[2].toString() + "> updated " + subjectList.size() + " subjects: " + stringList.substring(0, stringList.length() - 1));
                    try {
                        logSem.acquire();
                        Server.logger.logEvent("Subjects-Updated: User <" + receivedMsg[2].toString() + "> updated " + subjectList.size() + " subjects: " + stringList.substring(0, stringList.length() - 1));
                        logSem.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    break;

                /***************************************************************************************************************/
                /*                  2.4. Users publishing and receiving messages on subjects of interest                       */
                /***************************************************************************************************************/

                case "PUBLISH": //    FROM CLIENT
                    //to client: reply MESSAGE to all users subscribed to specified subject else PUBLISH-DENIED to client only
                    //to other server: nothing
                    break;

                // TODO: 2020-12-07 : MUST UPDATE OTHER SERVER ????


                /***************************************************************************************************************/
                /*                  2.5. Server overtaking and mobility                                                        */
                /***************************************************************************************************************/

                case "CHANGE-SERVER": //    FROM OTHER SERVER

                    Server.isServing.set(true);
                    if(Server.isServing.get()){
                        System.out.println("CHANGE-SERVER request received. This server is now serving.");
                    }

                    try {
                        logSem.acquire();
                        Server.logger.logEvent("CHANGE-SERVER request received. This server is now serving.");
                        logSem.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;

                case "UPDATE-SERVER": //    FROM SERVER ITSELF
                    Server.otherServerIP = receivedMsg[1].toString();
                    Server.otherServerPort = (int)receivedMsg[2];
                    System.out.println(("Other server IP:Port were updated to "+ receivedMsg[1].toString() +":"+receivedMsg[2].toString()));
                    try {
                        logSem.acquire();
                        Server.logger.logEvent("Other server IP:Port were updated to "+ receivedMsg[1].toString() +":"+receivedMsg[2].toString());
                        logSem.release();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;

                /***************************************************************************************************************/
                /*                  2.6. UNKNOWN MESSAGE TYPE                                                                  */
                /***************************************************************************************************************/
                default:

            }

            /***************************************************************************************************************/
            /*                      2.7. MESSAGE SENDING                                                                   */
            /***************************************************************************************************************/

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

