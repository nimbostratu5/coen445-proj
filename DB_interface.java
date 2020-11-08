
import java.sql.*;
import java.util.ArrayList;

public class DB_interface{
	
//	Parameters required to authenticate to the database: This is a very silly and fast way of doing it.
	private final String HOSTNAME="database-1.cgsj5cw6pwbs.ca-central-1.rds.amazonaws.com";
	private final int PORT=3306;
	private final String dbName = "COEN445";
	private final String [] CREDENTIALS = new String [] {"admin","COEN4452020"};
	
	private Connection connection;
	
//	Connect to the DB
	public void connect() throws SQLException{
		String jdbcUrl = "jdbc:mysql://" + HOSTNAME + ":" + PORT + "/" + dbName + "?user=" + CREDENTIALS[0] + "&password=" + CREDENTIALS[1];
		connection = DriverManager.getConnection(jdbcUrl);
		if (connection != null) {
			System.out.println("Sucessfully connected to the database!");
			connection.setAutoCommit(false);
		}
	}
	
//	Disconnect from the DB
	public void disconnect() throws SQLException {
		connection.close();
		connection = null;
	}
	
	// You must check that the user is not already registered!
	// Add new row to table 'users' if user not exists
	// Create new user-specific subjects table '%name%_subjects'
    public void createNewUserAccount(String user, String address) {
    	try {
    		String fixed_username = user.replace(' ', '_').trim();
    		String adding_new_user = String.format("INSERT INTO %s value(\'%s\',\'%s\',\'%s_subjects\')","users",fixed_username,address,fixed_username);
    		String creating_new_table = String.format("CREATE TABLE %s_subjects(id INT UNSIGNED NOT NULL, last_viewed DATETIME NOT NULL)", fixed_username);
    		
    		Statement addition_statement = connection.createStatement();
    		addition_statement.executeUpdate(adding_new_user);
    		
    		Statement creation_statement = connection.createStatement();
    		creation_statement.executeUpdate(creating_new_table);
    		
    		connection.commit();
    		
    	}catch(SQLException ex){
    		try {
    			ex.printStackTrace();
        		// Rolling back failed query;
				connection.rollback();
			} catch (SQLException e) {
				System.out.println("Fatal Error! Could not rollback the failing query.");
				e.printStackTrace();
			}
    	}
    }
    
    // Retrieve all users info
    public ArrayList<String[]> getAllUsers() {
    	
    	ArrayList<String []> users = null;
    	try {
    		
	    	String sql = "SELECT * FROM users";
	    	
	    	connection.setAutoCommit(true);
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(sql);
	    	
	    	users = new ArrayList<String []>();
	    	while(result.next()) {
	    		users.add(new String[] {result.getString("name"),result.getString("address")});
	    	}
	    	
	    	result.close();
	    	connection.setAutoCommit(false);
			
	    	
	    		
    	}catch(SQLException ex){
    		try {
    			ex.printStackTrace();
        		// Rolling back failed query;
				connection.rollback();
			} catch (SQLException e) {
				System.out.println("Fatal Error! Could not rollback the failing query.");
				e.printStackTrace();
			}
    	}
    	
		return users;
    }
    
	// Return the user's name and address row
    // If not found returns null
    public String[] getUserInfo(String user) {
    	String sql = String.format("SELECT * FROM users WHERE name = '%s'",user);
    	String [] userInfo = null;
    	
		try {
    		
	    	connection.setAutoCommit(true);
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(sql);
	    
	    	while(result.next()) {
	    		userInfo = new String[] {result.getString("name"),result.getString("address")};
	    	}
	    	
	    	result.close();
	    	connection.setAutoCommit(false);
	    		
    	}catch(SQLException ex){
    		try {
    			ex.printStackTrace();
        		// Rolling back failed query;
				connection.rollback();
			} catch (SQLException e) {
				System.out.println("Fatal Error! Could not rollback the failing query.");
				e.printStackTrace();
			}
    	}
		return userInfo;
		
		
    }
    
    // Must verify if the address was not already taken
    // Update the address field of specified user
	public void updateAddress(String user, String newAddress) {
		
	}
    
    public void deleteUser(String user) {
//    	find user in users table
//    	go the user's subjects
//    	traverse every row subtracting 1 from the user count on subjects
//		once finished iterating drop user's subject table
//		
    }

    public void registerNewSubject(String user, String subject) {
    	
//	check that subject not exists already
    	
/// IF NOT EXISTS:
	//    	add new row to subjects with count 1
	//		get the ID for the subject
	// 		create new subject table 
	//    	add the subject id to the user subjects table
    	
/// IF EXISTS:
    //		obtained id of the subject
    //		increment subject count in subjects table
    //		add the subject id to the user subjects table
    	
    }
	
	public void unregisterSubject(String user, String subject) {
		
//	reduce user count from subjects row
//  delete row from user's subjects
		
	}

	public void sendMessage(String user,String subject,String message) {
		
//	insert new row with message into the subject table
//  update time stamp in the subject row of the subjects table
//  Update user's last viewed time stamp
		
	}
	
	public void fetchUnviewedMessagesCount(String user){
		
//  for each subject in user's subjects verify if the last post time is bigger than last viewed time
//  got to each subject and count rows exceeding the user's last viewed timestamp
//  the returned data is a table with subject name, id, and count of unseen messages
		
	}
	
	public void fetchUnviewedMessages(String user, String subject) {
		
//	for the specific subject compare last post and user's last viewed timestamps
//  if the user is behind, fetch all items from the subject table with time sent timestamp greater than the last viewed timestamp
//     update user's last viewed to the latest post or later
//  if the user is same or advanced, well return empty
		
	}
}