import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client extends Thread {

    public Client(String threadName){
        this.setName(threadName);
    }

    public void run(){

        try {
            //we first create a socket that will send & receive data.
            //if port 8000 is already being used on your computer, change it.
            //can be anything from 0 to ~65xxx. make sure to update Sever.java
            DatagramSocket client = new DatagramSocket(8000);

            byte[] outData = new byte[1024];
            byte[] inData = new  byte[1024];
            Scanner scanner = new Scanner(System.in);
            String someData = scanner.nextLine();

            //we need to serialize someData: transform into bunch of 0's and 1's :
            outData = someData.getBytes();
            //note: it is easy to do it for Strings but at some point we will need to serialize objects created by ourselves.

            //the ip address used for both the client and the server is the same because it's the same computer => localhost.
            InetAddress serverIP = InetAddress.getByName("localhost");
            //the server port is predefined in the server class as 7000. make sure it matches in Server.java
            int serverPort = 7000;

            //we can now create a packet that will contain the serialized data and relevant parameters
            DatagramPacket outPacket = new DatagramPacket(outData,outData.length,serverIP,serverPort);
            //and send it.
            client.send(outPacket);

            //that's all for sending. now for receiving from server (self-explanatory):
            DatagramPacket inPacket = new DatagramPacket(inData,inData.length);
            client.receive(inPacket); //this is a blocking function.
            inData = inPacket.getData();

            System.out.println("Client received: "+new String(inData));

            //done. end the session:
            client.close();


        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
