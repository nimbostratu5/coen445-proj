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
    public static int client_port = 9000;


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

    public static void main(String[] args) throws IOException {

        /*  CREATE LOG FILE  */
        Logger logger = new Logger();
        logger.CreateFile();

        /*  TODO: NEED FUNCTION TO DETERMINE WHICH SERVER IS CURRENTLY SERVING   */
        InetAddress currentServer = InetAddress.getByName("localhost"); // USING THIS FOR NOW
        int currentServer_port = 3000; // THIS WILL BE USED AS DEFAULT PORT FOR ALL SERVERS

        /*  CLIENT UDP INITIALIZATION  */

                    /*  TODO: 2020-11-08 MAKE INITIALIZATION FAIL-SAFE
                    *   CLIENT CAN USE ANY COMPUTER I.E. DIFFERENT IP/SOCKET
                    *   WHAT IF NO INTERNET CONNECTION ?
                    *   WHAT IF PORT NUMBER ALREADY IN USE BY ANOTHER APPLICATION ?
                    */

        System.out.println("Starting Client");
        logger.LogEvent("client started");
        DatagramSocket clientSocket = new DatagramSocket(client_port);
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        /*  END CLIENT UDP INITIALIZATION  */


        /*  USER PROMPT */
        Object[] message = null;
        String messageType;
        boolean bye = false;

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
                    DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                    clientSocket.receive(receivePacket);
                    receiveData = receivePacket.getData();
                    Object[] receiveObj = new Object[0];
                    
                    logger.LogEvent("received from server: " + receiveObj[0].toString() );
                }
            }
        }

        /*  CLOSE SOCKET -- USER LOGOUT -- CLOSE SESSION  */
        clientSocket.close();
        logger.LogEvent("client closed application.");

    }
}
