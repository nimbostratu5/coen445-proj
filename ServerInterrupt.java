import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class ServerInterrupt implements Runnable {

    Scanner sc;
    public static int period;

    public static TimerTask task;
    public static Timer timer;
    public ServerInterrupt(Scanner sc, int period) {
        this.sc = sc;
        this.period = period;
    }


    public static void resumeTimerTask() {
       timer = new Timer();

        int delay = 5000; // delay of 1s before starting task
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(Server.isServing.get()) {
                    Server.busy.set(true); //don't think necessary
                    try {
                        Server.threadPool.execute(new Worker(Server.otherServerIP, Server.otherServerPort, Server.serverSocket, Server.logSem, true));
                    } catch (RejectedExecutionException r) {
                        System.out.println("Server swap attempt was blocked because the Executor is closing.");
                    }
                    Server.busy.set(false); //don't think necessary
                }
            }
        };
        timer.schedule( task, delay, period );
    }

    public static void pauseTimerTask() {
        timer.cancel();
    }


    @Override
    public void run() {

        resumeTimerTask();

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
                            pauseTimerTask();
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
