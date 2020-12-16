import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {

    public Client() throws UnknownHostException {}

    /***********************    GLOBAL VARIABLES    ***********************/

    public static volatile Logger logger;
    private static String username;
    public static int rqNum = 1;
    public static ConcurrentHashMap<Integer,String> pendingRequestMap;

    private static int client_port;
    public static DatagramSocket clientSocket;
    private static InetAddress serverA;
    private static InetAddress serverB;
    private static int serverA_port;
    private static int serverB_port;

    public static InetAddress currentServer;
    public static int currentServer_port;

    public static Semaphore logSem;
    public static Semaphore socketSem;

    public static AtomicBoolean run;

    static {
        try {
            clientSocket = new DatagramSocket(0);
            client_port = clientSocket.getLocalPort();
        } catch (SocketException e ) {
            System.out.println("DatagramSocket error");
            e.printStackTrace();
        }
    }

    /***********************        CLIENT APP      ***********************/

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        run = new AtomicBoolean(false);
        pendingRequestMap = new ConcurrentHashMap<Integer, String>();
        logSem = new Semaphore(1, true);
        socketSem = new Semaphore(1, true);

        Logger logger = new Logger();
        logger.createFile("clientlog");
        System.out.println("\n\n*************** Client started ***************");
        System.out.println("This client's IP is "+ getIP()  + " on port " + client_port);
        System.out.println("**********************************************\n");
        logger.logEvent("client started on " + getIP() + " on port " + client_port);

        boolean validIP = false;
        Scanner sc = new Scanner(System.in);
        String[] address;

        System.out.println("Enter Server_A IP:PORT (xxx.xxx.xxx.xxx:port#):");
        while(!validIP) {
            try {
                address = sc.nextLine().split(":");

                if((address.length==2)&& !address[0].equals(null) && !address[1].equals(null)){
                    serverA = InetAddress.getByName(address[0]);
                    serverA_port = Integer.parseInt(address[1]);
                    validIP = true;
                }
                else{
                    System.out.println("Input incomplete. Try again.");
                }

            } catch (UnknownHostException e) {
                System.out.println("Erroneous IP given for Server A. Try again:");
            } catch (NumberFormatException n){
                System.out.println("Erroneous port # given for Server A. Try again:");
            }
        }
        logger.logEvent("server A IP:port is " + serverA.toString()+":"+serverA_port);
        validIP = false;

        System.out.println("Enter Server_B IP:PORT (xxx.xxx.xxx.xxx:port#):");
        while(!validIP) {
            try {
                address = sc.nextLine().split(":");

                if((address.length==2)&& !address[0].equals(null) && !address[1].equals(null)){
                    serverB = InetAddress.getByName(address[0]);
                    serverB_port = Integer.parseInt(address[1]);
                    validIP = true;
                }
                else{
                    System.out.println("Input incomplete. Try again.");
                }

                if((serverA!=serverB) && (serverA_port!=serverB_port)){
                    validIP = true;
                }
                else{
                    System.out.println("Servers are duplicate of each other!");
                    throw new UnknownHostException();
                }

            } catch (UnknownHostException e) {
                System.out.println("Erroneous IP:Port given for Server B. Try again:");
            } catch (NumberFormatException n){
                System.out.println("Erroneous port # given for Server B. Try again:");
            }
        }
        logger.logEvent("server B IP:port is " + serverB.toString()+":"+serverB_port);

        //System.out.println("ServerA: "+ serverA.getHostAddress() + " : " + serverA_port);
        //System.out.println("ServerB: "+ serverB.getHostAddress() + " : " + serverB_port);

        currentServer = serverA;
        currentServer_port = serverA_port;
        logger.logEvent("current server is: " + currentServer.toString()+":"+currentServer_port);
        System.out.print("\nEnter your unique username: ");
        username = sc.next();
        System.out.println("\n~~~~~~~~~ Setup complete! Enter a command ~~~~~~~~~~\n");
        logger.logEvent("current user on this machine is " + username);

        run.set(true);
        ClientInterrupt clientInterrupt = new ClientInterrupt(logger,username);
        Thread clientT = new Thread(clientInterrupt);
        clientT.start();

        //Set the Client timeout duration

        /*  USER PROMPT */
        Object[] message = null;
        String messageType;
        boolean bye = false;
        
        label:
        while (!bye) {

            message = null;

            if (sc.hasNextLine()) {
                messageType = sc.next();
                messageType = messageType.toUpperCase();
                switch (messageType) {

                    case "BYE":

                        bye = true;
                        break label;

                    case "REGISTER":
                        pendingRequestMap.put(rqNum,"REGISTER");
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        message[3] = getIP();
                        message[4] = client_port;

                        break;

                    case "UPDATE":
                        pendingRequestMap.put(rqNum,"UPDATE");
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        System.out.print("New IP [type 'u' if unchanged]: ");
                        String in = sc.next();
                        if(in.equalsIgnoreCase("u")){
                            message[3] = getIP();
                        }
                        else if (!in.isEmpty()){
                            message[3] = in;
                        }
                        System.out.print("\nNew Port: ");
                        client_port=sc.nextInt();
                        message[4] = client_port;
                        break;

                    case "DE-REGISTER":
                        pendingRequestMap.put(rqNum,"DE-REGISTER");
                        message = new Object[3];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        break;

                    case "SUBJECTS":
                        pendingRequestMap.put(rqNum,"SUBJECTS");
                        message = new Object[4];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        System.out.print("List of subjects: ");
                        sc.nextLine();
                        String input = sc.nextLine();
                        String[] splitter = input.split("\\s+");
                        ArrayList<String> subjectList = new ArrayList<>(Arrays.asList(splitter));
                        message[3] = subjectList;
                        break;

                    case "PUBLISH":
                        pendingRequestMap.put(rqNum,"PUBLISH");
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        System.out.print("Subject: ");
                        message[3] = sc.next();
                        System.out.println("Input text:");
                        sc.nextLine();
                        message[4] = sc.nextLine();
                        break;

                    case "FETCH-SUBJECTS":
                        pendingRequestMap.put(rqNum,"FETCH-SUBJECTS");
                        message = new Object[5];
                        message[0] = messageType;
                        message[1] = rqNum++;
                        message[2] = username;
                        message[3] = getIP();
                        message[4] = client_port;
                        break;

                    case "LOG":
                        logger.displayLog();
                        break;

                    default:
                        System.out.println("Unknown message type. Available options are REGISTER, DE-REGISTER, UPDATE, PUBLISH, SUBJECTS.\nLOG to display the log file, and BYE to close session.");
                }

                if(rqNum==0) {
                    sendMessage(message, serverA, serverA_port, logger);
                    sendMessage(message, serverB, serverB_port, logger);
                }
                else{
                    sendMessage(message, currentServer, currentServer_port, logger);
                }
            }
        }

        /*  CLOSE SOCKET -- USER LOGOUT -- CLOSE SESSION  */
        logger.logEvent("client closed application.");
        run.set(false);
        clientSocket.close();
        clientT.join();

        System.out.println("Delete log file? y/n");
        String in = sc.next();
        if(in.equals("y"))
            logger.deleteLog();
        sc.close();

        System.out.println("\n*************** Client Session Closed ***************\n");
    }

    private static void sendMessage(Object[] message, InetAddress cS, int cSp, Logger logger ) throws IOException, ClassNotFoundException   {
        if (message != null) {

            byte[] sendData = new byte[1024];
            sendData = serialize(message);

            /*if(message[0].toString().equals("REGISTER")){
                *//*  SEND THE MESSAGE TO THE 2 SERVERS  *//*
                DatagramPacket sendPacket  = new DatagramPacket(sendData,sendData.length,serverA,serverA_port);
                DatagramPacket sendPacket2  = new DatagramPacket(sendData,sendData.length,serverB,serverB_port);
                try {
                    clientSocket.send(sendPacket);
                    clientSocket.send(sendPacket2);
                    logSem.acquire();
                    logger.logEvent("user "+username+" sent a msg of type REGISTER to both servers");
                    logSem.release();
                } catch (IOException | InterruptedException e) {
                    System.out.println("Message not sent.");
                    logger.logEvent("message failed to be sent");
                    e.printStackTrace();
                }

            }
            else{*/
                /*  SEND THE MESSAGE TO CURRENT SERVER  */
                DatagramPacket sendPacket  = new DatagramPacket(sendData,sendData.length,cS,cSp);            
                try {
                    clientSocket.send(sendPacket);
                    logSem.acquire();
                    logger.logEvent("user "+username+" sent a msg of type "+message[0].toString()+" to server "+currentServer.toString());
                    logSem.release();
                } catch (IOException | InterruptedException e) {
                    System.out.println("Message not sent.");
                    logger.logEvent("message failed to be sent");
                    e.printStackTrace();
                }
            //}

            System.out.println("[RQ#"+message[1].toString()+"] message sent!");

        }
    }

    /***********************     DE/SERIALIZATION FUNCTIONS    ***********************/

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    private static Object[] deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Object[]) is.readObject();
    }

    /***********************        GET MACHINE IP ADDRESS      ***********************/

    private static String getIP() {
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
}