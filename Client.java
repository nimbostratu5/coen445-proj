import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
//to investigate: import java.util.logging.Logger;

public class Client {

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static void main(String[] args) throws IOException {


        //test logger class:
        Logger logger = new Logger();
        logger.CreateFile();
        logger.logEvent("hello this is an event #12310 01237");


        System.out.println("Starting Client");

        DatagramSocket clientSocket = new DatagramSocket(3000);
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        String data = "hello";

        InetAddress serverAddress = InetAddress.getByName("localhost");
        sendData = serialize(data);
        DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,serverAddress,9000);

        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
        clientSocket.receive(receivePacket);
        receiveData = receivePacket.getData();
        String receivedString = new String(receiveData);
        System.out.println("From server: " + receivedString);

        clientSocket.close();
    }
}
