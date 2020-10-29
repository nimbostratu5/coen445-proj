import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

public class Server_B {

    boolean active = false;

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static void main(String[] args) throws IOException {

        System.out.println("Starting Server...");
        //server_B uses a diff port # than server_A but uses the same IP address.
        //TODO: before demo, we need to modify the code to run with distinct IP addresses.
        DatagramSocket serverSocket = new DatagramSocket(9001);
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];


        while(true){
            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
            serverSocket.receive((receivePacket));
            String msg = new String(receivePacket.getData());
            System.out.println("Received: "+msg);
            String ack = "hello client";
            sendData = serialize(ack);
            DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,receivePacket.getAddress(),receivePacket.getPort());
            serverSocket.send(sendPacket);
        }

    }
}
