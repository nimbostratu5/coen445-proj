public class Main {

    public static void main(String[] args) {

        System.out.println("You are the client, write something to send to the server:");

        Client client = new Client("client_1");
        Server server = new Server("server_1");
        server.start();
        client.start();
    }
}
