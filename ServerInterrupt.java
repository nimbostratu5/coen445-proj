import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ServerInterrupt implements Runnable {

    Scanner sc;

    public ServerInterrupt(Scanner sc) {
        this.sc = sc;
    }

    @Override
    public void run() {

        String input;

        while(true){
            if (sc.hasNextLine()){
                input = sc.nextLine();

                switch(input.toUpperCase()){

                    case "UPDATE-SERVER":
                        if(!Server.isServing.get()) {
                            System.out.print("Enter new IP: ");
                            String newIP = sc.nextLine();
                            System.out.print("Enter new port: ");
                            int newPort = sc.nextInt();
                            sc.nextLine();
                            try {
                                Server.updateServer(newIP, newPort);
                            } catch (SocketException | InterruptedException | UnknownHostException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            System.out.println("Cannot change server IP/Port while serving.");
                        }
                        break;

                    case "SHUTDOWN":
                        System.out.println("System shutdown requested.");
                        Server.shutdownServer();
                        break;

                    case "LOG":
                        Server.displayLog();
                        break;

                    default:
                        System.out.println("Unknown command given. Options are: UPDATE-SERVER and SHUTDOWN.\n Type LOG to view the log file.");
                }
            }
        }
    }
}
