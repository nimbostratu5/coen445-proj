import java.util.Scanner;

public class MessageParser_draft {

    public static void main(String args[]) throws Exception {

        Object[] message=null;
        String messageType;
        boolean bye = false;

        Scanner sc = new Scanner(System.in);
        label:
        while(!bye) {
            if (sc.hasNextLine()) {
                messageType = sc.next();
                switch (messageType) {
                    case "BYE":
                        //log action
                        sc.close();
                        bye = true;
                        break label;
                    case "REGISTER":

                        message = new Object[5];
                        message[0] = messageType;
                        //message[1] = requestNum++;
                        System.out.print("Request #? ");
                        message[1] = sc.nextLine();

                        System.out.print("NAME? ");
                        message[2] = sc.nextLine();

                        //IP
                        //message[3] = get ip
                        //Socket#
                        //message[4] = get socket

                        break;
                    case "DE-REGISTER":
                        message = new Object[2];
                        message[0] = messageType;
                        System.out.print("NAME? ");
                        message[1] = sc.nextInt();
                        sc.nextLine();
                        break;

                    // and so on...
                }

                if (message != null) {
                    //proceed to send
                    //log action
                    //maybe use a blocking queue? or some kind of a priority queue if servers are offline
                }
            }
        }

    }
}
