
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class DB_driver{
	
	private static DB_interface db_int;
    public static void main(String[] args) {
    	
        System.out.println("Attempting to test out JDBC connection");
        db_int = new DB_interface();
        
        try {
			db_int.connect();
//			db_int.sendMessage("bob", 135, "Hello", "127.0.0.1");
//	        db_int.sendMessage("ted", 135, "Extremely fast", "127.0.0.2");
//	        db_int.sendMessage("bob", 135, "What is?", "127.0.0.1");
			
//			ArrayList<String[]> users =db_int.fetchUnviewedMessages("bob",131,10);
//	        for (String [] item : users) 
//	        { 
//	        	for(String val: item) {
//	        		System.out.print(val+", ");
//	        	}
//	        	System.out.println();
//	        }
//			db_int.unregisterSubject("bob", 131);
//			db_int.registerToSubject("bob", "MC and cheese");
			
			db_int.disconnect();
		} catch (SQLException e) {

			e.printStackTrace();
		}
        
    }

}