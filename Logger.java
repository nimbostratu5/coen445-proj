import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    //this should be in constructor
    public void CreateFile() {

            try {
                File myObj = new File("log.txt");
                if (myObj.createNewFile()) {
                    System.out.println("File created: " + myObj.getName());
                } else {
                    System.out.println("File already exists.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

    }

    public void LogEvent(String event){

        try {

            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formattedDate = myDateObj.format(myFormatObj);

            FileWriter writer = new FileWriter("log.txt", true);
            writer.write(formattedDate+": "+event);
            writer.write("\r\n");   // write new line
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read the file and display on terminal
    public void DisplayLog() {
        try {
            FileReader reader = new FileReader("log.txt");
            int character;

            while ((character = reader.read()) != -1) {
                System.out.print((char) character);
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
