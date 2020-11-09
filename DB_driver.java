
import java.sql.SQLException;
import java.util.ArrayList;

public class DB_driver{
	
	private static DB_interface db_int;
    public static void main(String[] args) {
    	
        System.out.println("Attempting to test out JDBC connection");
        db_int = new DB_interface();
        
        try {
			db_int.connect();
		} catch (SQLException e) {

			e.printStackTrace();
		}
        
        db_int.createNewUserAccount("ted", "127.1.1.2:9001");
        ArrayList<String[]> users = db_int.getAllUsers();
        for (String [] user : users) 
        { 
            System.out.println(user[0]+", "+user[1]);
        }
        
        System.out.println(db_int.getUserInfo("ka"));
    }

}