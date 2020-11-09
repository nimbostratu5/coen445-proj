import java.io.*;
import java.net.*;

public class Server_B {

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
        //server_B uses a diff port # than server_A but uses the same IP address.
        //TODO: before demo, we need to modify the code to run with distinct IP addresses.
        DatagramSocket serverSocket = new DatagramSocket(3000);
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];


        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        serverSocket.receive((receivePacket));
        Object[] message = deserialize(receivePacket.getData());
        System.out.println("Received: "+message[0].toString());

        serverSocket.close();

    }
}
