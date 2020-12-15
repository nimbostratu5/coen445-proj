import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ServerInterrupt implements Runnable {

    Scanner sc;
    int delay;
    public ServerInterrupt(Scanner sc, int delay) {
        this.sc = sc;
        this.delay = delay;
    }

    @Override
    public void run() {

        //reference for the timer task: https://www.tutorialspoint.com/java/util/timer_scheduleatfixedrate_delay.htm
       /* Timer timer = new Timer();
        int delay = 1000; // delay of 1s before starting task
        int period = 5000; // task is executed every x seconds
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {

            }
        }, delay, period);*/

        String input;
        boolean out = false;
        while(!out){
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
                        System.out.print("System shutdown requested. ");
                        try {
                           // Server.shutdownServer();

                            System.out.println("Shutting down the server...");
                            Server.threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
                            System.out.print("All workers have terminated ");
                            Server.threadPool.shutdown();
                            System.out.println(" : Threadpool shutdown.");
                            Server.serverSocket.close();
                            System.out.print("Server socket closed.");
                            Server.shutdown=true;
                            out = true;

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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
