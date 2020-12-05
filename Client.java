import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;

public class Client {

    public Client() throws UnknownHostException {}

    /***********************    GLOBAL VARIABLES    ***********************/

    public static Logger logger;
    private static String username;
    private static int rqNum = 1;
    private static int client_port;
    private static DatagramSocket clientSocket;
    private static InetAddress serverA;
    private static InetAddress serverB;
    private static int serverA_port;
    private static int serverB_port;

    private static InetAddress currentServer;
    private static int currentServer_port;

    static {
        try {
            clientSocket = new DatagramSocket(0);
            client_port = clientSocket.getLocalPort();
        } catch (SocketException e ) {
            System.out.println("DatagramSocket error");
            e.printStackTrace();
        }
    }

    /***********************     DE/SERIALIZATION FUNCTIONS    ***********************/

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    private static Object[] deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Object[]) is.readObject();
    }

    /***********************        GET MACHINE IP ADDRESS      ***********************/

    private static String getIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    final String ip = addr.getHostAddress();
                    if(Inet4Address.class == addr.getClass()) return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /***********************        CLIENT APP      ***********************/

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        Logger logger = new Logger();
        logger.createFile("clientlog");
        System.out.println("\n\n*************** Client started ***************");
        System.out.println("This client's IP is "+ getIP()  + " on port " + client_port);
        System.out.println("**********************************************\n");
        logger.logEvent("client started on " + getIP() + " on port " + client_port);

        boolean validIP = false;
        Scanner sc = new Scanner(System.in);
        String[] address;

        System.out.println("Enter Server_A IP:PORT (xxx.xxx.xxx.xxx:port#):");
        while(!validIP) {
            try {
                address = sc.nextLine().split(":");
                serverA = InetAddress.getByName(address[0]);
                serverA_port = Integer.parseInt(address[1]);
                validIP = true;

            } catch (UnknownHostException e) {
                System.out.println("Erroneous IP given for Server A. Try again:");
            } catch (NumberFormatException n){
                System.out.println("Erroneous port # given for Server A. Try again:");
            }
        }
        logger.logEvent("server A IP:port is " + serverA.toString()+":"+serverA_port);
        validIP = false;

        System.out.println("\nEnter Server_B IP:PORT (xxx.xxx.xxx.xxx:port#):");
        while(!validIP) {
            try {
                address = sc.nextLine().split(":");
                serverB = InetAddress.getByName(address[0]);
                serverB_port = Integer.parseInt(address[1]);
                if((serverA!=serverB) && (serverA_port!=serverB_port)){
                    validIP = true;
                }
                else{
                    System.out.println("Servers are duplicate of each other!");
                    throw new UnknownHostException();
                }

            } catch (UnknownHostException e) {
                System.out.println("Erroneous IP:Port given for Server B. Try again:");
            } catch (NumberFormatException n){
                System.out.println("Erroneous port # given for Server B. Try again:");
            }
        }
        logger.logEvent("server B IP:port is " + serverB.toString()+":"+serverB_port);

        //System.out.println("ServerA: "+ serverA.getHostAddress() + " : " + serverA_port);
        //System.out.println("ServerB: "+ serverB.getHostAddress() + " : " + serverB_port);

        currentServer = serverA;
        currentServer_port = serverA_port;
        logger.logEvent("current server is: " + currentServer.toString()+":"+currentServer_port);

        System.out.println("\nSetup complete!\n\nEnter your unique username:");
        username = sc.nextLine();
        logger.logEvent("user is " + username);


        /*  USER PROMPT */
        Object[] message = null;
        String messageType;
        boolean bye = false;
        System.out.println("Input a command");
        label:
        while (!bye) {
            if (sc.hasNextLine()) {
                messageType = sc.next();

                switch (messageType) {

                    case "BYE":
                        sc.close();
                        bye = true;
                        break label;

                    case "REGISTER":

                    case "UPDATE":
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        message[3] = getIP();
                        message[4] = client_port;
                        break;

                    case "DE-REGISTER":
                        message = new Object[3];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        break;

                    case "SUBJECTS":
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        String input = sc.next();
                        String[] splitter = input.split("\\s+");
                        ArrayList<String> subjectList = new ArrayList<>(Arrays.asList(splitter));
                        message[3] = subjectList;
                        break;

                    case "PUBLISH":
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        System.out.print("Subject: ");
                        message[3] = sc.next();
                        System.out.println("Input text:");
                        sc.nextLine();
                        message[4] = sc.nextLine();
                        break;

                    case "LOG":
                        logger.displayLog();
                        break;

                    default:
                        System.out.println("Unkown message type. Available options are REGISTER, DE-REGISTER, UPDATE, PUBLISH, SUBJECTS.\nLOG to display the log file, and BYE to close session.");
                }

                sendMessage(message,currentServer,currentServer_port,logger);
            }
        }

        /*  CLOSE SOCKET -- USER LOGOUT -- CLOSE SESSION  */
        clientSocket.close();
        System.out.println("\n\n*************** Client Session Closed ***************\n");
        logger.logEvent("client closed application.");

    }

    private static void sendMessage(Object[] message, InetAddress cS, int cSp, Logger logger ) throws IOException, ClassNotFoundException   {
        if (message != null) {

            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];            
            sendData = serialize(message);

            if(message[0].toString().equals("REGISTER")){
                /*  SEND THE MESSAGE TO THE 2 SERVERS  */
                DatagramPacket sendPacket  = new DatagramPacket(sendData,sendData.length,serverA,serverA_port);
                DatagramPacket sendPacket2  = new DatagramPacket(sendData,sendData.length,serverB,serverB_port);
                try {
                    clientSocket.send(sendPacket);
                    clientSocket.send(sendPacket2);
                } catch (IOException e) {
                    System.out.println("Message not sent.");
                    logger.logEvent("message failed to be sent");
                    e.printStackTrace();
                }

            }
            else{
                /*  SEND THE MESSAGE TO CURRENT SERVER  */
                DatagramPacket sendPacket  = new DatagramPacket(sendData,sendData.length,cS,cSp);            
                try {
                    clientSocket.send(sendPacket);
                } catch (IOException e) {
                    System.out.println("Message not sent.");
                    logger.logEvent("message failed to be sent");
                    e.printStackTrace();
                }
            }

            System.out.println("RQ#"+rqNum+" sent! Awaiting server response...");
            logger.logEvent("user "+username+" sent a msg of type "+ message[0].toString() +" to server");// TODO: 2020-11-08 which server?

            /*  RECEIVING  */

            try {
                clientSocket.setSoTimeout(50000);
            } catch (SocketException e) {
                System.out.println("Server has not responded within 10s");
                logger.logEvent("client timed-out. Server "+ currentServer.toString() +" is not responding.");
                e.printStackTrace();
            }

            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
            try {
                clientSocket.receive(receivePacket);
            } catch (IOException e) {
                System.out.println("packet problem when receiving");
                logger.logEvent("client socket receiving packet error");
                e.printStackTrace();
            }
            receiveData = receivePacket.getData();
            Object[] receivedMsg = deserialize(receiveData);
            //System.out.println(receivedMsg[0].toString());
            logger.logEvent("received a message of type "+ receivedMsg[0].toString()+" from server ");

            switch (receivedMsg[0].toString()) {

                case "REGISTERED":
                    System.out.println("RQ#" + receivedMsg[1].toString() + ": Registered to Server");
                    logger.logEvent("RQ#"+receivedMsg[1].toString() + " processed successfully." + username+" is registered to server");
                    break;

                case "REGISTER-DENIED":
                    System.out.println("RQ#" + receivedMsg[1].toString() + ": registration denied: "+receivedMsg[2].toString());
                    logger.logEvent("RQ#" + receivedMsg[1].toString() + ": registration denied: "+receivedMsg[2].toString());
                    //System.out.println("Would you like to re-send your message? y/n");
                    
                    break;

                            /* TODO:  Upon reception of REGISTER-DENIED, the user will give up for a little while before
                                retrying again depending on the reason. (?)*/

                case "DE-REGISTER":
                    System.out.println( "You have been de-registered from the server.");
                    logger.logEvent("user " + username + " has been de-registered from the server");
                    break;
                    //do we need to do anything in the local database?

                case "UPDATE-CONFIRMED":
                    System.out.println("Your info (ip/socket#) have been updated. [RQ#"+receivedMsg[1].toString()+"]");
                    logger.logEvent("client ip/socket updated to: " + receivedMsg[3] +":"+receivedMsg[4]);
                    break;

                case "UPDATE-DENIED":
                    System.out.println("Update denied [RQ#"+receivedMsg[1].toString()+"]:"+ receivedMsg[2].toString());
                    logger.logEvent("client ip/socket updated to: " + receivedMsg[3] +":"+receivedMsg[4]);
                    break;

                case "SUBJECTS-UPDATED":
                    System.out.println( "Your subjects have been updated [RQ#"+receivedMsg[1].toString()+"]");
                    //TODO: list the subjects & update local database?
                    logger.logEvent("subjects have been updated.");
                    break;

                case "SUBJECTS-REJECTED":
                    //for what reason would it get rejected...
                    System.out.println( "Your subjects have NOT been updated [RQ#"+receivedMsg[1].toString()+"]");
                    logger.logEvent("subjects update request rejected.");
                    break;

                case "MESSAGE":
                    if(receivedMsg[1].toString().equals(username)){
                        System.out.println( "Your message on "+ receivedMsg[2].toString()+" was published.");
                        //TODO: update local database?
                        logger.logEvent("client has published a message");
                    }
                    else {
                        System.out.println( "Message received from " + receivedMsg[1].toString() +" on " + receivedMsg[2].toString()+" :");
                        //TODO: display text + update local database?
                        logger.logEvent("client has received message from "+ receivedMsg[1].toString());
                    }
                    break;

                case "PUBLISH-DENIED":
                    System.out.println( "Your message RQ#"+ receivedMsg[1].toString()+" was not published: "+ receivedMsg[2].toString());
                    logger.logEvent("client message publish was denied [RQ#"+ receivedMsg[1].toString() +"]");
                    break;

                case "CHANGE-SERVER":
                    currentServer = InetAddress.getByName(receivedMsg[1].toString());
                    currentServer_port = (int)receivedMsg[2];
                    logger.logEvent("server has been changed to: "+ currentServer.toString() + ":"+currentServer_port);
                    break;

                case "ACK": //for testing purposes
                    System.out.println("received: "+receivedMsg[0].toString() +"\nServer says: " + receivedMsg[1].toString());
                    System.out.println();
                    break;

                default:
                    System.out.println("Unknown message received.");
                    logger.logEvent("received message with unknown type.");
            }
        }
    }
}