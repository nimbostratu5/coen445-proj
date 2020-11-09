import java.io.*;
import java.net.*;
import java.util.Scanner;
//to investigate: import java.util.logging.Logger;

public class Client {

    /*                          TODO: 2020-11-08 [FOR CLIENT SIDE ONLY]
     *
     *   FUNCTION TO DETERMINE WHICH SERVER IS CURRENTLY SERVING
     *   FAIL-SAFE INITIALIZATION OF UDP CONNECTION
     *   THREADING OF RECEIVING/SENDING
     *
     */


    public Client() throws UnknownHostException {}

    /***********************    GLOBAL VARIABLES    ***********************/
    /*      unique username
     *       request number counter
     *       client IP address and port number
     */

    public static final String name = "nimbostratus";
    private static int rqNum;
    public static int client_port;


    /***********************     DE/SERIALIZATION FUNCTIONS    ***********************/

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


    /***********************        CLIENT APP      ***********************/

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        /*  CREATE LOG FILE  */
        Logger logger = new Logger();
        logger.CreateFile();

        /*  TODO: NEED FUNCTION TO DETERMINE WHICH SERVER IS CURRENTLY SERVING   */
        InetAddress currentServer = InetAddress.getByName("localhost"); // USING THIS FOR NOW
        int currentServer_port = 3000; // THIS WILL BE USED AS DEFAULT PORT FOR ALL SERVERS

        /*  CLIENT UDP INITIALIZATION  */

        /*  TODO: 2020-11-08
         *   MAKE INITIALIZATION FAIL-SAFE
         *   CLIENT CAN USE ANY COMPUTER I.E. DIFFERENT IP/SOCKET
         *   WHAT IF NO INTERNET CONNECTION ?
         */

        System.out.println("Starting Client");
        logger.LogEvent("client started");
        System.out.println("Server address is set to: "+ currentServer.toString() + " on port "+ currentServer_port);
        DatagramSocket clientSocket = new DatagramSocket(0);
        client_port = clientSocket.getLocalPort();

        System.out.println("The client is bound to port: "+client_port +" with IP address: " +clientSocket.getLocalSocketAddress());
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        /*  END CLIENT UDP INITIALIZATION  */


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
                        message[3] = clientSocket.getInetAddress();
                        message[4] = clientSocket.getPort();
                        System.out.println("message input complete");
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
                        //message[3] = input list of subjects from user using , delimiter
                        break;

                    case "PUBLISH":
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = name;
                        //message[3] = subject
                        //message[4] = text
                        break;
                }

                if (message != null) {
                    /*  SEND THE MESSAGE TO CURRENT SERVER  */
                    sendData = serialize(message);
                    DatagramPacket sendPacket  = new DatagramPacket(sendData,sendData.length,currentServer,currentServer_port);
                    clientSocket.send(sendPacket);

                    logger.LogEvent("user "+name+" sent a msg of type "+ messageType +" to server");// TODO: 2020-11-08 which server?

                    /*  RECEIVING  */

                    clientSocket.setSoTimeout(10000);
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                    clientSocket.receive(receivePacket);
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
                    }
                }
            }
        }

        /*  CLOSE SOCKET -- USER LOGOUT -- CLOSE SESSION  */
        clientSocket.close();
        logger.LogEvent("client closed application.");

    }
}
