import java.io.*;
import java.net.*;

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
        DatagramSocket serverSocket = new DatagramSocket(3000);
        DatagramPacket serverPacket = null;
        byte[] receiveData = new byte[65535];


        while (true) {
            serverPacket = new DatagramPacket(receiveData, receiveData.length); //Datagram receiving size
            serverSocket.receive(serverPacket);    //Receive the data in a buffer
            
            Object[] messageList = deserialize(serverPacket.getData());
            for (int i = 0; i < messageList.length; i++) {
                System.out.println("Received: " + messageList[i].toString());
            }
  
            if (messageList[0].toString().equalsIgnoreCase("bye")) {   //If data sent says "bye" (to end the program)
                System.out.println("Client sent bye.....EXITING"); 
                serverSocket.close();   //Close socket before exiting
                break; 
            } 

            // Clear the buffer after every message. 
            receiveData = new byte[65535]; 
        } 
    }
}
