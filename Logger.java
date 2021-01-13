import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Logger {

    private File myObj;

    //this should be in constructor
    public void createFile(String filename) {
            try {
                myObj = new File((filename+".txt"));
                if (myObj.createNewFile()) {
                    System.out.println("\nLog file created: " + myObj.getName());
                } else {
                    System.out.println("\nWarning: log file with name "+myObj.getName()+" already exists. Re-using "+myObj.getName() +".");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
    }

    public void logEvent(String event){

        try {
            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formattedDate = myDateObj.format(myFormatObj);
            FileWriter writer = new FileWriter(myObj.getName(), true);
            writer.write(formattedDate+": "+event);
            writer.write("\r\n");   // write new line
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read the file and display on terminal
    public void displayLog() {
        try {
            FileReader reader = new FileReader(myObj.getName());
            int character;

            while ((character = reader.read()) != -1) {
                System.out.print((char) character);
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read the file and display on terminal
    public void deleteLog() throws IOException {


        BufferedWriter out = new BufferedWriter(new FileWriter(myObj.getName()));
        //out.write("aString1\n");
        out.close();
        boolean success = (new File(myObj.getName())).delete();
        if (success) {
            System.out.println("The log file has been successfully deleted");
        }

    }

}
