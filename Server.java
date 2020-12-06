import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    public static AtomicBoolean isServing = new AtomicBoolean(false);
    public static AtomicBoolean serverStatus = new AtomicBoolean(false);

    public static boolean shutdown = false;
    public static boolean busy = false;
    public static boolean serverUpdate = false;

    public static Logger logger;
    public static DatagramSocket serverSocket;
    public static String serverName;
    public static int serverPort;
    public static int otherServerPort;
    public static String otherServerIP;

    public static DB_interface db_int;

    private static ExecutorService threadPool;
    private static int numThreads;
    public static Semaphore logSem;

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        numThreads = 10;
        threadPool = Executors.newFixedThreadPool(numThreads);
        Semaphore runningThreadSem = new Semaphore(numThreads);
        logSem = new Semaphore(1, true);

        logger = new Logger();

        Scanner sc = new Scanner(System.in);

        System.out.println("\n\n***************** Server Setup *****************");
        System.out.print("Enter server name: ");
        serverName = sc.nextLine();
        logger.createFile(serverName);
        System.out.print("Enter " + serverName + " port #: ");
        serverPort = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter the other server's IP: ");
        otherServerIP = sc.nextLine();
        System.out.print("Enter the other server's port #: ");
        otherServerPort = sc.nextInt();

        sc.nextLine();

        System.out.println("Is this server serving first? y/n");
        String in = sc.nextLine();
        if(in.equals("y")) {
            isServing.set(true);
        }
        restartDB();
        System.out.println("Setup complete. "+serverName + " has been created with IP " + getIP() + ":" + serverPort);
        System.out.println("************************************************\n");

        byte[] receiveData = new byte[65535];
        serverSocket = new DatagramSocket(serverPort);

        //serverInterrupt gets user input from terminal to UPDATE-SERVER or to SHUTDOWN the server.
        ServerInterrupt serverInterrupt = new ServerInterrupt(sc);
        Thread siThread = new Thread(serverInterrupt);
        siThread.start();

        while(!shutdown){

            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                if(!busy) {
                    serverSocket.receive(receivedPacket);
                    if(isServing.get()) {
                        threadPool.execute(new Worker(receivedPacket.getAddress().getHostAddress(),receivedPacket.getPort(), receivedPacket.getData(), serverSocket, otherServerIP, otherServerPort, logSem, db_int));
                    }
                    else{
                        //check if the packet is from the other server. if yes, then process the incoming packet:
                        if( (receivedPacket.getAddress().getHostAddress().equals(otherServerIP)) && (receivedPacket.getPort() == otherServerPort) ){
                            threadPool.execute(new Worker(receivedPacket.getAddress().getHostAddress(), receivedPacket.getPort(), receivedPacket.getData(), serverSocket, otherServerIP, otherServerPort, logSem,db_int));
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                //e.printStackTrace();
            }
        }

        threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
        threadPool.shutdown();
        serverSocket.close();
        siThread.join();
        System.out.println("Good Bye!");

    }

    public static void updateServer(String newIP, int newPort) throws SocketException, InterruptedException, UnknownHostException {
        System.out.println("Closing current socket...");
        busy = true;
        System.out.println("Checking if workers have finished their tasks...");
        threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
        System.out.println("All workers have finished their tasks.");
        threadPool.shutdown();
        serverSocket.close();
        serverSocket = new DatagramSocket(newPort);
        threadPool = Executors.newFixedThreadPool(numThreads);

        threadPool.execute(new Worker(otherServerIP, otherServerPort, serverSocket, logSem));
        busy = false;
        System.out.println("Server socket has been updated to: " + newIP + ":" + serverSocket.getLocalPort());

    }

    public static void shutdownServer(){
        System.out.println("Shutting down the server...");
        shutdown=true;
    }

    public static void displayLog(){
        //acquire logSem
        logger.displayLog();
    }

    public static String getIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
              while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    final String ip = addr.getHostAddress();
                    if(Inet4Address.class == addr.getClass()) return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void restartDB() {
        // Create instance of DB
        db_int = new DB_interface();

        // Test if DB can connect
        try {
            db_int.connect();
            System.out.println("Initial SQL DB connection activated...");
            db_int.disconnect();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
