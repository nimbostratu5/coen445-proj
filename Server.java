import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server extends Thread{

    public Server(String threadName){
        this.setName(threadName);
    }

    public void run(){

        try {
            DatagramSocket server = new DatagramSocket(7000);
            byte[] inData = new byte[1024];
            byte[] outData = new byte[1024];

            while(true){
                DatagramPacket inPacket = new DatagramPacket(inData,inData.length);
                server.receive(inPacket);
                System.out.println("Server received: "+new String(inPacket.getData()));

                String confirm = "got it, thanks";
                outData = confirm.getBytes();

                //datagram packet include the client's IP and port. the server doesnt need to know those information beforehand:
                DatagramPacket outPacket = new DatagramPacket(outData,outData.length,inPacket.getAddress(),inPacket.getPort());
                server.send(outPacket);
            }


        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
