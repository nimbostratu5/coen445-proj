import java.io.*;
import java.net.*;
import java.util.*;

public class Server_A {

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
        DatagramSocket serverASocket = new DatagramSocket(3000);    //Server listen on this SOCKET
        DatagramPacket receivePacket = null;

        // For replying back to client
        DatagramPacket replyClientPacket = null;
        byte[] replyDataClient = new byte[65535];
        Object[] messageReplyClient = null;

        // For receiving from server B
        // DatagramPacket receiveServerPacket = null;
        InetAddress serverB_Address = InetAddress.getLocalHost();
        int serverB_port = 4001;      //TEMPORARY TO HOLD PORT FOR OTHER SERVER

        // For replying back to server B
        DatagramPacket replyServerPacket = null;
        byte[] replyDataServer = new byte[65535];
        Object[] messageReplyServer = null;


        // TODO: Implement timer duration for while loop, before the loop ends, and then tells the other server to take over
        Boolean serverSwap = false;
        Boolean serverActive = true;
        Scanner sc = new Scanner(System.in); // Used to take IP and SOCKET while server inactive
        // Timer time = new Timer();


        Boolean sendServerFlag = false;



        while (true) {
            //--------------------------------Receiving client packet data----------------------------------
            receivePacket = new DatagramPacket(replyDataClient, replyDataClient.length); //Datagram receiving size
            serverASocket.receive(receivePacket);    //Receive the data in a buffer
            
            // Print out message sent to the server
            Object[] messageListClient = deserialize(receivePacket.getData());

            // Hold message type from the message sent to the server
            String messageTypeReceived = messageListClient[0].toString(); 
            //----------------------------------------------------------------------------------------------


            // TODO: If client message needs a response to another server, create ANOTHER if condition to send the second message to the server!!

            // // TODO: Fix this so that server A ACTUALLY receives the client message (when this is uncommented, the server doesn't manage to get the message) 


            // TODO: Implement timer to set serverActive to true or false based on time and serverSwap to true


            // If server is inactive, allow to input UPDATE-SERVER and then input comma delimited IP and Socket
            if (serverActive.equals(false)) {
                /**/
                if (sc.hasNextLine()) {
                    String ipSocketChange = sc.next();

                    if (ipSocketChange.equalsIgnoreCase("UPDATE-SERVER")) {
                        System.out.println("Enter IP and Socket <IP,SOCKET>: ");
                        String ipAndSocket = sc.next();
                        String[] splitter = ipAndSocket.split("\\s+");
                        ArrayList<String> subjectList = new ArrayList<>(Arrays.asList(splitter));

                        messageReplyServer = new Object[3];
                        messageReplyServer[0] = "UPDATE-SERVER";
                        messageReplyServer[1] = InetAddress.getByName(subjectList.get(0).toString());
                        messageReplyServer[2] = subjectList.get(1).toString();
                    }
                }
                
            }


            // Add user to server 
            if (messageTypeReceived.equalsIgnoreCase("REGISTER") && serverActive.equals(true)) {
                // TODO: if (name doesnt exist)
                messageReplyClient = new Object[2];
                messageReplyClient[0] = "REGISTERED";
                messageReplyClient[1] = messageListClient[1].toString();

                // Print out contents from client's message
                for (int i = 0; i < messageListClient.length; i++) {
                    System.out.println("Received: " + messageListClient[i].toString());
                }
                System.out.println("Message Received From: " + receivePacket.getSocketAddress().toString());


                /*
                // TODO: else
                messageReplyClient = new Object[3];
                messageReplyClient[0] = "REGISTER-DENIED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = "Name already in use.";
                */

                sendServerFlag = true;  //Send to server
            }

            //Remove user from server
            else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER") && serverActive.equals(true)) {
                sendServerFlag = true;  //Send to server
            }

            // Update user's IP Address and Socket#
            else if (messageTypeReceived.equalsIgnoreCase("UPDATE") && serverActive.equals(true)) {
                //if (name exists)
                messageReplyClient = new Object[5];
                messageReplyClient[0] = "UPDATE-CONFIRMED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = messageListClient[2].toString();
                messageReplyClient[3] = messageListClient[3].toString();
                messageReplyClient[4] = messageListClient[4].toString();

                /*
                //if (name doesn't exist)
                messageReplyClient = new Object[3];
                messageReplyClient[0] = "UPDATE-DENIED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = "Name does not exist.";
                */

                sendServerFlag = true;  //Send to server
            }

            // Add subjects of interest to user
            else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS") && serverActive.equals(true)) {
                ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);
                
                //if (if name exists && subject is from COEN)
                messageReplyClient = new Object[4];
                messageReplyClient[0] = "SUBJECTS-UPDATED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = messageListClient[2].toString();
                messageReplyClient[3] = subjectList;

                for (String subject : subjectList) {
                    System.out.println("Subject: " + subject);
                }

                /*
                // (if name doesn't exist || subject is NOT COEN)
                messageReplyClient = new Object[4];
                messageReplyClient[0] = "SUBJECTS-REJECTED";
                messageReplyClient[1] = messageListClient[1].toString();
                messageReplyClient[2] = messageListClient[2].toString();
                messageReplyClient[3] = messageListClient[3].toString();
                */
                sendServerFlag = true;  //Send to server
            }

            // Send message to users with subject of interest
            else if (messageTypeReceived.equalsIgnoreCase("PUBLISH") && serverActive.equals(true)) {
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
                serverASocket.send(replyClientPacket);
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
                //---------------------------------Send to client---------------------------------
                replyDataClient = serialize(messageReplyClient);
                replyClientPacket = new DatagramPacket(replyDataClient, replyDataClient.length, receivePacket.getSocketAddress());
                serverASocket.send(replyClientPacket);
                //--------------------------------------------------------------------------------
            }
            replyDataClient = new byte[65535]; // Clear the buffer after every message
            
            

            //---------------------------------------Send to server B----------------------------------------
            if (sendServerFlag = true) {

                // //--------------------------------Receiving server packet data----------------------------------
                // receiveServerPacket = new DatagramPacket(replyDataServer, replyDataServer.length); //Datagram receiving size
                // serverASocket.receive(receiveServerPacket);    //Receive the data in a buffer
                
                // // Print out message sent to the server
                // Object[] messageListServer = deserialize(receiveServerPacket.getData());

                // // Hold message type from the message sent to the server
                // String messageTypeServer = messageListServer[0].toString(); 
                // //----------------------------------------------------------------------------------------------

                if (messageTypeReceived.equalsIgnoreCase("REGISTER") && serverActive.equals(true)) {
                    // TODO: if (name doesnt exist)
                    messageReplyServer = new Object[5];
                    messageReplyServer[0] = "REGISTERED";
                    messageReplyServer[1] = messageListClient[1].toString(); // RQ#
                    messageReplyServer[2] = messageListClient[2].toString(); // Name
                    messageReplyServer[3] = messageListClient[3].toString(); // IP ADDRESS
                    messageReplyServer[4] = messageListClient[4].toString(); // SOCKET#

                    // TODO: else
                    // messageReplyServer = new Object[5];
                    // messageReplyServer[0] = "REGISTER-DENIED";
                    // messageReplyServer[1] = messageListClient[1].toString(); // RQ#
                    // messageReplyServer[2] = messageListClient[2].toString(); // Name
                    // messageReplyServer[3] = messageListClient[3].toString(); // IP ADDRESS
                    // messageReplyServer[4] = messageListClient[4].toString(); // SOCKET#

                    sendServerFlag = false;
                }

                // Remove user from server    
                else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER") && serverActive.equals(true)) {
                    //If name exists
                    messageReplyServer = new Object[2];
                    messageReplyServer[0] = "DE-REGISTER";
                    messageReplyServer[1] = messageListClient[2].toString();
                    //OPTIONAL: Say to client that it was removed

                    //If name doesn't exist
                    // Do nothing
                    // OPTIONAL: Say to client that it doesn't exist
                    sendServerFlag = false;
                }

                // Update user's IP Address and Socket#
                else if (messageTypeReceived.equalsIgnoreCase("UPDATE") && serverActive.equals(true)) {
                    messageReplyServer = new Object[5];
                    messageReplyServer[0] = "UPDATE-CONFIRMED";
                    messageReplyServer[1] = messageListClient[1].toString();
                    messageReplyServer[2] = messageListClient[2].toString();
                    messageReplyServer[3] = messageListClient[3].toString();
                    messageReplyServer[4] = messageListClient[4].toString();

                    sendServerFlag = false;
                }
                
                // Add subjects of interest to user
                else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS") && serverActive.equals(true)) {
                    //if (if name exists && subject is from COEN)
                    messageReplyServer = new Object[4];
                    messageReplyServer[0] = "SUBJECTS-UPDATED";
                    messageReplyServer[1] = messageListClient[1].toString();
                    messageReplyServer[2] = messageListClient[2].toString();
                    messageReplyServer[3] = messageListClient[3].toString();

                    sendServerFlag = false;
                }

                // Response for invalid message
                else {
                    messageReplyClient = new Object[3];
                    messageReplyClient[0] = "INVALID-MESSAGE";
                    messageReplyClient[1] = messageListClient[1].toString();
                    messageReplyClient[2] = "Invalid message has been sent.";
                    sendServerFlag = false;
                }

                //--------------------------Send message to server B----------------------------------
                if (messageReplyServer != null) {
                    replyDataServer = serialize(messageReplyServer);
                    replyServerPacket = new DatagramPacket(replyDataServer, replyDataServer.length, serverB_Address, serverB_port);
                    serverASocket.send(replyServerPacket);
                }
                replyDataServer = new byte[65535]; // Clear the buffer after every message
            }



            //------------------------------------Receiving server messages--------------------------------
            //------------------------------------While server inactive------------------------------------
            if (serverActive.equals(false)) {

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
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("DE-REGISTER")) {
                    System.out.println("User <" + messageListClient[1].toString() + "> has been de-registed.");
                }

                // Receive server updated IP and socket
                else if (messageTypeReceived.equalsIgnoreCase("UPDATE-CONFIRMED")) {
                    System.out.println("User <" + messageListClient[2].toString() + "> IP Changed to: " + messageListClient[3].toString() + " port: " + messageListClient[4].toString());
                }

                // Receive server updated subjects and print
                else if (messageTypeReceived.equalsIgnoreCase("SUBJECTS-UPDATED")) {
                    ArrayList<String> subjectList = new ArrayList<>((ArrayList<String>) messageListClient[3]);

                    StringBuilder sb = new StringBuilder();
                    for (String subject : subjectList) {
                        sb.append(subject + " ");
                    }

                    System.out.println("User <" + messageListClient[2].toString() + "> updated subjects: " + sb.toString());
                }

                
            }

            //----------------------------While server active-------------------------------------------------
            else {
            // Receive server updated IP and socket
            if (messageTypeReceived.equalsIgnoreCase("UPDATE-SERVER")) {
                serverB_Address = InetAddress.getByName(messageListClient[1].toString());
                serverB_port = (int) messageListClient[2];
                System.out.println("IP Changed to: " + serverB_Address + " port: " + serverB_port);
            }
            }
        }
        serverASocket.close();
    }
}
