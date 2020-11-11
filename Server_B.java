import java.io.*;
import java.net.*;

public class Test_Server {

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

        System.out.print("Starting Server...");
        DatagramSocket serverSocket = new DatagramSocket(4000);
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        DatagramPacket serverPacket = null;
        byte[] receiveData = new byte[65535];


        while (true) {
            serverPacket = new DatagramPacket(receiveData, receiveData.length); 
            serverSocket.receive(serverPacket);    
            
            Object[] message = deserialize(serverPacket.getData());
            System.out.println("received: "+message[0].toString()+" from: "+ serverPacket.getSocketAddress().toString());
            
            message = new Object[2];
            message[0] = "ACK";
            message[1] = "Hello client!";
            byte[] sendData = serialize(message);
            
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,serverPacket.getAddress(),serverPacket.getPort());
            serverSocket.send(sendPacket);
            receiveData = new byte[65535]; 
        } 
    }
}
