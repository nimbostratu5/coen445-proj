import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

public class Server {

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static void main(String[] args) throws IOException {

        System.out.println("Starting Server...");

        DatagramSocket serverSocket = new DatagramSocket(9000);
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
