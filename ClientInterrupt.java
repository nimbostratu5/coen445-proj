import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientInterrupt implements Runnable {

    //Scanner sc;
    Logger logger;
    String username;
    public ClientInterrupt(Logger logger,String username) {
        //this.sc = sc;
        this.logger = logger;
        this.username = username;
    }

    @Override
    public void run() {

        while(true) {
            /*  RECEIVING  */
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                Client.clientSocket.setSoTimeout(100211100);
            } catch (SocketException e) {
                Client.logger.logEvent("client timed-out, server not responding");
                System.out.println("Client timed-out, server not responding. Try again.");
            }

            try {
                Client.clientSocket.receive(receivePacket);
                receiveData = receivePacket.getData();
                Object[] receivedMsg = deserialize(receiveData);

                if (receivedMsg != null) {
                    logger.logEvent("received a message of type " + receivedMsg[0].toString() + " from server ");

                    switch (receivedMsg[0].toString()) {

                        case "REGISTERED":
                            if (Client.pendingRequestMap.get((int) receivedMsg[1]) == "REQUEST") {
                                System.out.println("RQ#" + receivedMsg[1].toString() + ": Registered to Server");
                                logger.logEvent("RQ#" + receivedMsg[1].toString() + " processed successfully." + username + " is registered to server");
                            }
                            break;

                        case "REGISTER-DENIED":
                            System.out.println("RQ#" + receivedMsg[1].toString() + ": registration denied: " + receivedMsg[2].toString());
                            logger.logEvent("RQ#" + receivedMsg[1].toString() + ": registration denied: " + receivedMsg[2].toString());
                            //System.out.println("Would you like to re-send your message? y/n");
                        /* TODO:  Upon reception of REGISTER-DENIED, the user will give up for a little while before
                                    retrying again depending on the reason. (?)*/

                            break;

                        case "DE-REGISTER":
                            System.out.println("You have been de-registered from the server.");
                            logger.logEvent("user " + username + " has been de-registered from the server");
                            break;

                        case "UPDATE-CONFIRMED":
                            System.out.println("Your info (ip/socket#) have been updated. [RQ#" + receivedMsg[1].toString() + "]");
                            logger.logEvent("client ip/socket updated to: " + receivedMsg[3] + ":" + receivedMsg[4]);
                            break;

                        case "UPDATE-DENIED":
                            System.out.println("Update denied [RQ#" + receivedMsg[1].toString() + "]:" + receivedMsg[2].toString());
                            logger.logEvent("client ip/socket updated to: " + receivedMsg[3] + ":" + receivedMsg[4]);
                            break;

                        case "SUBJECTS-UPDATED":
                            System.out.println("The following subjects have been updated [RQ#" + receivedMsg[1].toString() + "]:");
                            ArrayList<String> subjectList = (ArrayList<String>) receivedMsg[3];
                            for (int i = 0; i < subjectList.size(); i++) {
                                System.out.println(subjectList.get(i));
                            }
                            logger.logEvent("subjects of " + username + " have been updated.");
                            break;

                        case "SUBJECTS-REJECTED":
                            //for what reason would it get rejected...
                            System.out.println("Your subjects have NOT been updated [RQ#" + receivedMsg[1].toString() + "]");
                            logger.logEvent("subjects update request rejected.");
                            break;

                        case "MESSAGE":
                            if (receivedMsg[1].toString().equals(username)) {
                                System.out.println("Your message on " + receivedMsg[2].toString() + " was published.");
                                logger.logEvent("client has published a message");
                            } else {
                                System.out.println("Message received from " + receivedMsg[1].toString() + " about " + receivedMsg[2].toString() + " :");
                                System.out.println(receivedMsg[3].toString());
                                logger.logEvent("client has received message from " + receivedMsg[1].toString());
                            }
                            break;

                        case "PUBLISH-DENIED":
                            System.out.println("Your message RQ#" + receivedMsg[1].toString() + " was not published: " + receivedMsg[2].toString());
                            logger.logEvent("client message publish was denied [RQ#" + receivedMsg[1].toString() + "]");
                            break;

                        case "CHANGE-SERVER":
                            Client.currentServer = InetAddress.getByName(receivedMsg[1].toString());
                            Client.currentServer_port = (int) receivedMsg[2];
                            logger.logEvent("server has been changed to: " + Client.currentServer.toString() + ":" + Client.currentServer_port);
                            break;

                        case "FETCH-SUCCESS":
                            ArrayList<String> allSubjects = (ArrayList<String>) receivedMsg[3];
                            if (Client.pendingRequestMap.get(Integer.parseInt(receivedMsg[1].toString())) == "FETCH-SUCCESS") {
                                System.out.println("[RQ#" + receivedMsg[1].toString() + "] Fetched subjects: ");
                                logger.logEvent("RQ#" + receivedMsg[1].toString() + ". Subject list fetching successful.");
                            }
                            for (int i = 0; i < allSubjects.size(); i++) {
                                System.out.println(allSubjects.get(i));
                            }
                            break;

                        default:
                            System.out.println("Unknown message received.");
                            Client.logger.logEvent("received message of unknown type.");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Closing socket..");
                //e.printStackTrace();
            }
        }
    }


    private static Object[] deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Object[]) is.readObject();
    }



}