
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
//			db_int.createNewUserAccount("bob", "127.0.0.1:9000");
//			ArrayList<String[]> users = db_int.getAllUsersSubscribed(131);
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