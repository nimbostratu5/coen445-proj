import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
//to investigate: import java.util.logging.Logger;

public class Client {

    public Client() throws UnknownHostException {}

    /***********************    GLOBAL VARIABLES    ***********************/

    public static final String name = "nimbostratus";
    private static int rqNum = 1;
    private static int client_port;
    private static Logger logger = new Logger();
    private static DatagramSocket clientSocket;
    private static InetAddress currentServer;
    private static int currentServer_port = 3000; // THIS WILL BE USED AS DEFAULT PORT FOR ALL SERVERS

    static {
        try {
            currentServer = InetAddress.getByName("192.168.1.123"); //TO BE CHANGED TO DISTINCT IP
            clientSocket = new DatagramSocket(0);
            client_port = clientSocket.getLocalPort();
        } catch (UnknownHostException | SocketException e) {
            System.out.println("Server address error.");
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

    /***********************        CLIENT APP      ***********************/

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        /*  TODO: NEED SOLUTION TO DETERMINE WHICH SERVER IS CURRENTLY SERVING
         *    ANSWER: IF CLIENT SENDS TO NON-SERVING SERVER. LET THE NON-SERVING SERVER RE-DIRECT THE CLIENT.
         *               1. NON-SERVING SERVER SENDS "CHANGE SERVER" MESSAGE TO CLIENT
         *               2. CLIENT SILENTLY RE-SENDS MESSAGE TO SERVING SERVER.
         */

        System.out.println("Starting Client");
        System.out.println("Server address is set to: "+ currentServer.toString() + " on port "+ currentServer_port);
        System.out.println("The client is bound to port: "+client_port +" with IP address: " +clientSocket.getLocalSocketAddress());
        logger.LogEvent("client started");
        logger.LogEvent("current server is: " + currentServer.toString()+":"+currentServer_port);

        /*  USER PROMPT */
        Object[] message = null;
        String messageType;
        boolean bye = false;
        System.out.println("Input a command");
        Scanner sc = new Scanner(System.in);
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
                        message[2] = name;
                        message[3] = "123.456.789.0"; //fake ip
                        message[4] = clientSocket.getPort();
                        break;

                    case "DE-REGISTER":
                        message = new Object[2];
                        message[0] = messageType;
                        message[1] = name;
                        break;

                    case "SUBJECTS":
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = name;
                        String input = sc.next();
                        String[] splitter = input.split("\\s+");
                        ArrayList<String> subjectList = new ArrayList<>(Arrays.asList(splitter));
                        message[3] = subjectList;
                        break;

                    case "PUBLISH":
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = name;
                        System.out.print("Subject: ");
                        message[3] = sc.next();
                        System.out.println("Input text:");
                        sc.nextLine();
                        message[4] = sc.nextLine();
                        break;

                    case "LOG":
                        logger.DisplayLog();

                }
                
                sendMessage(message);
            }
        }

        /*  CLOSE SOCKET -- USER LOGOUT -- CLOSE SESSION  */
        clientSocket.close();
        System.out.println("Client session closed.");
        logger.LogEvent("client closed application.");

    }

    private static void sendMessage(Object[] message ) throws IOException, ClassNotFoundException   {
        if (message != null) {

            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];

            /*  SEND THE MESSAGE TO CURRENT SERVER  */
            sendData = serialize(message);
            DatagramPacket sendPacket  = new DatagramPacket(sendData,sendData.length,currentServer,currentServer_port);

            try {
                clientSocket.send(sendPacket);
            } catch (IOException e) {
                System.out.println("Message not sent.");
                logger.LogEvent("message failed to be sent");
                e.printStackTrace();
            }

            System.out.println("Sent! Awaiting server response...");
            logger.LogEvent("user "+name+" sent a msg of type "+ message[0].toString() +" to server");// TODO: 2020-11-08 which server?

            /*  RECEIVING  */

            try {
                clientSocket.setSoTimeout(10000);
            } catch (SocketException e) {
                System.out.println("Server has not responded within 10s");
                logger.LogEvent("client timed-out. Server "+ currentServer.toString() +" is not responding.");
                e.printStackTrace();
            }

            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
            try {
                clientSocket.receive(receivePacket);
            } catch (IOException e) {
                System.out.println("packet problem when receiving");
                logger.LogEvent("client socket receiving packet error");
                e.printStackTrace();
            }
            receiveData = receivePacket.getData();
            Object[] receivedMsg = deserialize(receiveData);
            //System.out.println(receivedMsg[0].toString());
            logger.LogEvent("received a message of type "+ receivedMsg[0].toString()+" from server ");

            switch (receivedMsg[0].toString()) {

                case "REGISTERED":
                    System.out.println("RQ#" + receivedMsg[1].toString() + ": Registered to Server");
                    logger.LogEvent("RQ#"+receivedMsg[1].toString() + " processed successfully." + name+" is registered to server");

                case "REGISTER-DENIED":
                    System.out.println("RQ#" + receivedMsg[1].toString() + ": registration denied: "+receivedMsg[2].toString());
                    logger.LogEvent("RQ#" + receivedMsg[1].toString() + ": registration denied: "+receivedMsg[2].toString());

                            /* TODO:  Upon reception of REGISTER-DENIED, the user will give up for a little while before
                                retrying again depending on the reason. (?)*/

                case "DE-REGISTER":
                    System.out.println( "You have been de-registered from the server.");
                    logger.LogEvent("user " + name + " has been de-registered from the server");

                    //do we need to do anything in the local database?

                case "UPDATE-CONFIRMED":
                    System.out.println("Your info (ip/socket#) have been updated. [RQ#"+receivedMsg[1].toString()+"]");
                    logger.LogEvent("client ip/socket updated to: " + receivedMsg[3] +":"+receivedMsg[4]);
                    break;

                case "UPDATE-DENIED":
                    System.out.println("Update denied [RQ#"+receivedMsg[1].toString()+"]:"+ receivedMsg[2].toString());
                    logger.LogEvent("client ip/socket updated to: " + receivedMsg[3] +":"+receivedMsg[4]);
                    break;

                case "SUBJECTS-UPDATED":
                    System.out.println( "Your subjects have been updated [RQ#"+receivedMsg[1].toString()+"]");
                    //TODO: list the subjects & update local database?
                    logger.LogEvent("subjects have been updated.");
                    break;

                case "SUBJECTS-REJECTED":
                    //for what reason would it get rejected...
                    System.out.println( "Your subjects have NOT been updated [RQ#"+receivedMsg[1].toString()+"]");
                    logger.LogEvent("subjects update request rejected.");
                    break;

                case "MESSAGE":
                    if(receivedMsg[1].toString().equals(name)){
                        System.out.println( "Your message on "+ receivedMsg[2].toString()+" was published.");
                        //TODO: update local database?
                        logger.LogEvent("client has published a message");
                    }
                    else {
                        System.out.println( "Message received from " + receivedMsg[1].toString() +" on " + receivedMsg[2].toString()+" :");
                        //TODO: display text + update local database?
                        logger.LogEvent("client has received message from "+ receivedMsg[1].toString());
                    }
                    break;

                case "PUBLISH-DENIED":
                    System.out.println( "Your message RQ#"+ receivedMsg[1].toString()+" was not published: "+ receivedMsg[2].toString());
                    logger.LogEvent("client message publish was denied [RQ#"+ receivedMsg[1].toString() +"]");
                    break;

                case "CHANGE-SERVER":
                    currentServer = InetAddress.getByName(receivedMsg[1].toString());
                    currentServer_port = (int)receivedMsg[2];
                    logger.LogEvent("server has been changed to: "+ currentServer.toString() + ":"+currentServer_port);

                case "ACK": //for testing purposes
                    System.out.println("received: "+receivedMsg[0].toString() +"\nServer says: " + receivedMsg[1].toString());
                    System.out.println();
            }
        }
    }

}
