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

    // TODO: Implement database search function for NAME, LIST OF SUBJECTS

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("Starting Server...");
        //server_A uses a diff port # than server_B but uses the same IP address.
        //TODO: before demo, we need to modify the code to run with distinct IP addresses.

        // For receiving from client
        DatagramSocket clientSocket = new DatagramSocket(3000);
        DatagramPacket receiveClientPacket = null;
        byte[] receiveClientData = new byte[65535];

        // For replying back to client
        DatagramPacket replyClientPacket = null;
        byte[] replyDataClient = new byte[65535];
        Object[] messageReplyClient = null;

        // // For receiving from server B
        // DatagramSocket serverSocket = new DatagramSocket(4000);    //TEMPORARY, BUT SERVER_B PORT IS CHANGEABLE
        // DatagramPacket receiveServerPacket = null;
        // byte[] receiveServerData = new byte[65535];

        // // For replying back to server B
        // DatagramPacket replyServerPacket = null;
        // byte[] replyDataServer = new byte[65535];
        // Object[] messageReplyServer = null;


        // TODO: Implement timer duration for while loop, before the loop ends, and then tells the other server to take over
        Boolean serverSwap = false;
        Boolean serverActive = true;
        Scanner sc = new Scanner(System.in); // Used to take input to change server IP and Socket while not running
        Timer time = new Timer();


        while (true) {
            //--------------------------------Receiving client packet data----------------------------------
            receiveClientPacket = new DatagramPacket(receiveClientData, receiveClientData.length); //Datagram receiving size
            clientSocket.receive(receiveClientPacket);    //Receive the data in a buffer
            
            // Print out message sent to the server
            Object[] messageListClient = deserialize(receiveClientPacket.getData());

            // Hold message type from the message sent to the server
            String messageTypeClient = messageListClient[0].toString(); 
            //----------------------------------------------------------------------------------------------


            // //--------------------------------Receiving server packet data----------------------------------
            // receiveServerPacket = new DatagramPacket(receiveServerData, receiveServerData.length); //Datagram receiving size
            // serverSocket.receive(receiveServerPacket);    //Receive the data in a buffer
            
            // // Print out message sent to the server
            // Object[] messageListServer = deserialize(receiveServerPacket.getData());

            // // Hold message type from the message sent to the server
            // String messageTypeServer = messageListServer[0].toString(); 
            // //----------------------------------------------------------------------------------------------


            // TODO: Implement timer to set serverActive to true or false based on time and serverSwap to true
            // If server is inactive, allow to input UPDATE-SERVER and then input comma delimited IP and Socket
            if (serverActive.equals(false)) {
                /*
                String ipSocketChange = sc.nextLine();

                else if (ipSocketChange.equalsIgnoreCase("UPDATE-SERVER")) {
                    String ipSocket = sc.next();
                    String[] splitter = input.split("\\s+");
                    ArrayList<String> subjectList = new ArrayList<>(Arrays.asList(splitter));

                    messageReplyServer = new Object[3];
                    messageReplyServer[0] = "UPDATE-SERVER";
                    messageReplyServer[1] = InetAddress.getByName(subjectList.get(0).toString());
                    messageReplyServer[2] = subjectList.get(1).toString();
                }
                */
            }


            // Add user to server 
            if (messageTypeClient.equalsIgnoreCase("REGISTER") && serverActive.equals(true)) {
                // TODO: if (name doesnt exist)
                messageReplyClient = new Object[2];
                messageReplyClient[0] = "REGISTERED";
                messageReplyClient[1] = messageListClient[1].toString();

                // Print out contents from client's message
                for (int i = 0; i < messageListClient.length; i++) {
                    System.out.println("Received: " + messageListClient[i].toString());
                }
                System.out.println("Message Received From: " + receiveClientPacket.getSocketAddress().toString());

                /*
                messageReplyServer = new Object[5];
                messageReplyServer[0] = "REGISTERED";
                messageReplyServer[1] = messageListClient[1].toString(); // RQ#
                messageReplyServer[2] = messageListClient[2].toString(); // Name
                messageReplyServer[3] = messageListClient[3].toString(); // IP ADDRESS
                messageReplyServer[4] = messageListClient[4].toString(); // SOCKET#
                */

                /*
                // TODO: else
                messageReplyClient = new Object[3];
                messageReplyClient[0] = "REGISTER-DENIED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = "Name already in use.";

                
                messageReplyServer = new Object[5];
                messageReplyServer[0] = "REGISTER-DENIED";
                messageReplyServer[1] = messageListClient[1].toString(); // RQ#
                messageReplyServer[2] = messageListClient[2].toString(); // Name
                messageReplyServer[3] = messageListClient[3].toString(); // IP ADDRESS
                messageReplyServer[4] = messageListClient[4].toString(); // SOCKET#
                */
            }

            /*
            else if (messageTypeServer.equalsIgnoreCase("REGISTERED") && serverActive.equals(false)) {

            }
            */

            /*
            else if (messageTypeServer.equalsIgnoreCase("REGISTER-DENIED") && serverActive.equals(false)) {

            }
            */

            // Remove user from server    
            else if (messageTypeClient.equalsIgnoreCase("DE-REGISTER") && serverActive.equals(true)) {
                /*
                //If name exists
                messageReplyServer = new Object[2];
                messageReplyServer[0] = "DE-REGISTER";
                messageReplyServer[1] = messageListClient[2].toString();
                OPTIONAL: Say to client that it was removed

                //If name doesn't exist
                Do nothing
                OPTIONAL: Say to client that it doesn't exist
                */

            }

            /*
            else if (messageTypeServer.equalsIgnoreCase("DE-REGISTER") && serverActive.equals(false)) {

            }
            */

            // Update user's IP Address and Socket#
            else if (messageTypeClient.equalsIgnoreCase("UPDATE") && serverActive.equals(true)) {
                //if (name exists)
                messageReplyClient = new Object[5];
                messageReplyClient[0] = "UPDATE-CONFIRMED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = messageListClient[2].toString();
                messageReplyClient[3] = messageListClient[3].toString();
                messageReplyClient[4] = messageListClient[4].toString();

                /*
                messageReplyServer = new Object[5];
                messageReplyServer[0] = "UPDATE-CONFIRMED";
                messageReplyServer[1] = messageListClient[1].toString();
                messageReplyServer[2] = messageListClient[2].toString();
                messageReplyServer[3] = messageListClient[3].toString();
                messageReplyServer[4] = messageListClient[4].toString();


                //if (name doesn't exist)
                messageReplyClient = new Object[3];
                messageReplyClient[0] = "UPDATE-DENIED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = "Name does not exist.";
                */

            }

            /*
            else if (messageTypeServer.equalsIgnoreCase("UPDATE-CONFIRMED") && serverActive.equals(false)) {

            }
            */

            // Add subjects of interest to user
            else if (messageTypeClient.equalsIgnoreCase("SUBJECTS") && serverActive.equals(true)) {
                //if (if name exists && subject is from COEN)
                messageReplyClient = new Object[4];
                messageReplyClient[0] = "SUBJECTS-UPDATED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = messageListClient[2].toString();
                messageReplyClient[3] = messageListClient[3].toString();

                /*
                messageReplyServer = new Object[4];
                messageReplyServer[0] = "SUBJECTS-CONFIRMED";
                messageReplyServer[1] = messageListClient[1].toString();
                messageReplyServer[2] = messageListClient[2].toString();
                messageReplyServer[3] = messageListClient[3].toString();

                // (if name doesn't exist || subject is NOT COEN)
                messageReplyClient = new Object[4];
                messageReplyClient[0] = "SUBJECTS-REJECTED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = messageListClient[2].toString();
                messageReplyClient[3] = messageListClient[3].toString();
                */
            }

            // Send message to users with subject of interest
            else if (messageTypeClient.equalsIgnoreCase("PUBLISH") && serverActive.equals(true)) {
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

            
            

            // Swaps servers
            else if (serverSwap.equals(true)) {
                serverSwap = false;
                serverActive = false;

                /*
                for (all users in db)
                messageReplyClient = new Object[3];
                messageReplyClient[0] = "CHANGE-SERVER";
                messageReplyClient[1] = user.IP;
                messageReplyClient[2] = user.socket;
                replyDataClient = serialize(messageReplyClient);
                replyClientPacket = new DatagramPacket(replyDataClient, replyDataClient.length, user.ip, user.socket);
                clientSocket.send(replyClientPacket);
                */
                break;
            }

            // Response for invalid message
            else {
                messageReplyClient = new Object[3];
                messageReplyClient[0] = "INVALID-MESSAGE";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = "Invalid message has been sent.";
            }












            //TODO: Make BELOW a function

            // Send message to client
            if (messageReplyClient != null) {
                // TODO: Fix this so it also works to send to other SERVER and not only CLIENT! 
                //---------------------------------Send to client---------------------------------
                replyDataClient = serialize(messageReplyClient);
                replyClientPacket = new DatagramPacket(replyDataClient, replyDataClient.length, receiveClientPacket.getSocketAddress());
                clientSocket.send(replyClientPacket);
                //--------------------------------------------------------------------------------
            }

            // // Send message to server B
            // if (messageReplyServer != null) {
            //     //---------------------------------Send to ServerB---------------------------------
            //     replyDataServer = serialize(messageReplyServer);
            //     replyServerPacket = new DatagramPacket(replyDataServer, replyDataServer.length, receiveServerPacket.getSocketAddress());
            //     serverSocket.send(replyServerPacket);
            //     //--------------------------------------------------------------------------------
            // }

            // Clear the buffer after every message. 
            receiveClientData = new byte[65535];
            replyDataClient = new byte[65535];
            // receiveServerData = new byte[65535];
            // replyDataServer = new byte[65535]; 
        }
        
        
    }
}
