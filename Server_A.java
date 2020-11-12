import java.io.*;
import java.net.*;
import java.util.*;

public class Server_A {

    boolean active = false;

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

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("Starting Server...");
        //server_A uses a diff port # than server_B but uses the same IP address.
        //TODO: before demo, we need to modify the code to run with distinct IP addresses.
        DatagramSocket receiveSocket = new DatagramSocket(3000);
        DatagramPacket receivePacket = null;
        byte[] receiveData = new byte[65535];


        DatagramPacket replyPacket = null;
        byte[] replyData = new byte[65535];
        Object[] messageReplyClient = null;
        Object[] messageReplyServer = null;


        // TODO: Implement timer duration for while loop, before the loop ends, and then tells the other server to take over
        Boolean serverSwap = false;
        Timer time = new Timer();


        while (true) {
            receivePacket = new DatagramPacket(receiveData, receiveData.length); //Datagram receiving size
            receiveSocket.receive(receivePacket);    //Receive the data in a buffer
            
            // Print out message sent to the server
            Object[] messageList = deserialize(receivePacket.getData());

            // Hold message type from the message sent to the server
            String messageType = messageList[0].toString(); 

            // Server can accept or refuse and reply to client
            // Message: REGISTER, RQ#, NAME, IP ADDRESS, SOCKET#
            // To Client Refuse: Name already in use -> REGISTER-DENIED, RQ#, REASON AND make user sleep for a bit
            // To Client Accept: Name available for use -> REGISTERED, RQ#

            // Server can accept or refuse and reply to other server
            // To Server Accept: REGISTERED, RQ#, NAME, IP ADDRESS, SOCKET#
            // To Server Deny: REGISTER-DENIED, RQ#, NAME, IP ADDRESS, SOCKET#
            if (messageType.equalsIgnoreCase("REGISTER")) {
                // TODO: if (name exists)
                // TODO: else
                messageReplyClient = new Object[2];
                messageReplyClient[0] = "REGISTERED";
                messageReplyClient[1] = messageList[1].toString();

                for (int i = 0; i < messageList.length; i++) {
                    System.out.println("Received: " + messageList[i].toString());
                }
            }

            // Server can accept or refuse and reply to server only 
            // Message: DE-REGISTER, RQ#, NAME

            // To Server Accept: Name Exists -> DE-REGISTER, NAME   
            // To Server Refuse: Name Doesn't Exists -> Do nothing
            // TODO: Create ability for server's to send to one another
            // TODO: Server must be able to read this message type as well and reply    
            else if (messageType.equalsIgnoreCase("DE-REGISTER")) {

            }

            // Server can accept or refuse and reply to client
            // Message: UPDATE, RQ#, NAME, IP ADDRESS, SOCKET#
            // To Client Accept: Name exists -> UPDATE-CONFIRMED, RQ#, NAME, IP ADDRESS, SOCKET#
            // To Client Refuse: Name already in use -> UPDATE-DENIED, RQ#, REASON

            // Server can accept or refuse and reply to other server
            // To Server Accept: UPDATE-CONFIRMED, RQ#, NAME, IP ADDRESS, SOCKET#
            // To Server Deny: Nothing
            else if (messageType.equalsIgnoreCase("UPDATE")) {

            }
            

            // Server can accept or refuse and reply to client
            // Message: SUBJECTS, RQ#, NAME, LIST OF SUBJECTS
            // To Client Accept: Name exists -> SUBJECTS-UPDATED, RQ#, NAME, LIST OF SUBJECTS
            // To Client Refuse: Name doesn't exist OR subject list error -> SUBJECTS-REJECTED, RQ#, NAME, LIST OF SUBJECTS

            // Server can accept or refuse and reply to other server
            // To Server Accept: SUBJECTS-UPDATED, RQ#, NAME, LIST OF SUBJECTS
            // To Server Deny: Nothing
            else if (messageType.equalsIgnoreCase("SUBJECTS")) {

            }

            // Message: PUBLISH, RQ#, NAME, SUBJECT, TEXT
            // To Client Accept: If name exists -> MESSAGE, NAME, SUBJECT, TEXT
            // To Client Deny: If name DOESNT exists -> PUBLISH-DENIED, RQ#, REASON

            else if (messageType.equalsIgnoreCase("PUBLISH")) {

            }

            // If server needs to swap to other server
            // To ALL REGISTERED Client: CHANGE-SERVER, IP ADDRESS, SOCKET# (change to the ip + socket for other server)
            // To server: When NOT running, it can change ip and socket: -> UPDATE-SERVER, IP ADDRESS, Socket#
            else if (serverSwap.equals(true)) {
                serverSwap = false;
                break;
            }

            // Response as INCORRECT MESSAGE TYPE
            else {

            }

            if (messageReplyClient != null) {

                // TODO: Fix this so it also works to send to other SERVER and not only CLIENT! 

                //---------------------------------Send to client---------------------------------
                replyData = serialize(messageReplyClient);
                replyPacket = new DatagramPacket(replyData, replyData.length, receivePacket.getAddress(), receivePacket.getPort());
                receiveSocket.send(replyPacket);
                //--------------------------------------------------------------------------------
            }
  
            if (messageList[0].toString().equalsIgnoreCase("bye")) {   //If data sent says "bye" (to end the program)
                System.out.println("Client sent bye.....EXITING"); 
                receiveSocket.close();   //Close socket before exiting
                break; 
            } 

            // Clear the buffer after every message. 
            receiveData = new byte[65535]; 
        }
        
        
    }
}
