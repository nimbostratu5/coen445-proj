import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    public static AtomicBoolean isServing = new AtomicBoolean(false);
    public static AtomicBoolean busy = new AtomicBoolean(false);

    public static boolean shutdown = false;
    public static boolean serverUpdate = false;

    public static volatile Logger logger;
    public static DatagramSocket serverSocket;
    public static String serverName;
    public static int serverPort;
    public static int otherServerPort;
    public static String otherServerIP;

    public static DB_interface db_int;
    public static Thread siThread;
    public static ExecutorService threadPool;
    private static int numThreads;
    public static Semaphore logSem;
    public static int period;
    public static Scanner sc;


    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        serverSetup();

        byte[] receiveData = new byte[65535];
        serverSocket = new DatagramSocket(serverPort);

        //serverInterrupt gets user input from terminal to UPDATE-SERVER or to SHUTDOWN the server.
        period=period*1000;
        ServerInterrupt serverInterrupt = new ServerInterrupt(sc, period);
        siThread = new Thread(serverInterrupt);
        siThread.start();


        while(!shutdown){
            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                if(!busy.get()){
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

        //threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
        //threadPool.shutdown();
       // serverSocket.close();
        //ServerInterrupt.pauseTimerTask();
        siThread.join();
        System.out.println("Good Bye!");
    }

    public static void serverSetup() throws SocketException {

        numThreads = 10;
        threadPool = Executors.newFixedThreadPool(numThreads);
        logSem = new Semaphore(1, true);

        logger = new Logger();

        sc = new Scanner(System.in);

        System.out.println("\n\n********************************** Server Setup **********************************");
        System.out.println("This machine's IP is: "+getIP());
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

        System.out.print("Specify which database to connect to [DB_1 or DB_2]: ");
        String db_name = sc.nextLine();
        restartDB(db_name);

        System.out.println("Is this server serving first? y/n");
        String in = sc.nextLine();
        if(in.equals("y")) {
            isServing.set(true);
        }

        System.out.print("Specify the server serving time interval in s: ");
        period = sc.nextInt();
        sc.nextLine();
        System.out.println("Setup complete. "+serverName + " has been created with IP " + getIP() + ":" + serverPort);
        System.out.println("**********************************************************************************\n");
;
    }

    public static void updateServer(String newIP, int newPort) throws SocketException, InterruptedException, UnknownHostException {
        System.out.println("Closing current socket...");
        busy.set(true);
        System.out.println("Checking if workers have finished their tasks...");
        threadPool.awaitTermination(2500, TimeUnit.MILLISECONDS);
        System.out.println("All workers have finished their tasks.");
        threadPool.shutdown();
        serverSocket.close();
        serverSocket = new DatagramSocket(newPort);
        threadPool = Executors.newFixedThreadPool(numThreads);
        busy.set(false);
        System.out.println("Server socket has been updated to: " + newIP + ":" + serverSocket.getLocalPort());
        Server.threadPool.execute(new Worker(Server.otherServerIP, Server.otherServerPort, Server.serverSocket, Server.logSem));
    }

    public static void shutdownServer() throws InterruptedException {
        System.out.print("Shutting down the server...");
        threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
        threadPool.shutdown();
        serverSocket.close();
        siThread.join();
        shutdown=true;
        System.out.print("ret");
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

    public static void restartDB(String db_name) {

        // Create instance of a specified DB
        //UNCOMMENT THIS WHEN DB_interface is update
        //db_int = new DB_interface("COEN445");
        if(db_name.equalsIgnoreCase("db_1")) {
            db_int = new DB_interface("COEN445");
        }
        else if(db_name.equalsIgnoreCase("db_2")) {
            db_int = new DB_interface("COEN445_2");
        }
        else {db_int = new DB_interface("COEN445");}

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
