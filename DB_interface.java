
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class DB_interface{
	
	// Parameters required to authenticate to the database: This is a very silly and fast way of doing it.
	private final String HOSTNAME="database-1.cgsj5cw6pwbs.ca-central-1.rds.amazonaws.com";
	private final int PORT=3306;
	private String dbName = "COEN445";
	private final String [] CREDENTIALS = new String [] {"admin","COEN4452020"};
	
	private Connection connection;

	public DB_interface(String database_name){
		dbName = database_name;
	}
	
	// Connect to the DB
	public void connect() throws SQLException{
		String jdbcUrl = "jdbc:mysql://" + HOSTNAME + ":" + PORT + "/" + dbName + "?user=" + CREDENTIALS[0] + "&password=" + CREDENTIALS[1];
		connection = DriverManager.getConnection(jdbcUrl);
		if (connection != null) {
			System.out.println("Sucessfully connected to the database!");
			connection.setAutoCommit(false);
		}
	}
	
	// Disconnect from the DB
	public void disconnect() throws SQLException {
		connection.close();
		connection = null;
	}
	
	// You must check that the user is not already registered!
	// Add new row to table 'users' if user not exists
	// Create new user-specific subjects table '%name%_subjects'
    public void createNewUserAccount(String user, String address) {
    	try {
    		String adding_new_user = String.format("INSERT INTO %s value(\'%s\',\'%s\',\'%s_subjects\')","users",user,address,user);
    		String creating_new_table = String.format("CREATE TABLE %s_subjects(id INT UNSIGNED NOT NULL, last_viewed DATETIME NULL)", user);
    		
    		Statement statement = connection.createStatement();
       		statement.executeUpdate(creating_new_table);
    		statement.executeUpdate(adding_new_user);

    		
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
    	String update_address = String.format("UPDATE users SET address = \'%s\' WHERE name = \'%s\' LIMIT 1", newAddress,user);
		try {
			connection.setAutoCommit(false);
	    	Statement statement = connection.createStatement();
	    	statement.execute(update_address);
	    	connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
    
	// Delete user from the system
    public void deleteUser(String user) {
		//    	find user in users table
		//    	go the user's subjects
		//    	traverse every row subtracting 1 from the user count on subjects
		//		once finished iterating drop user's subject table
				String remove_user = String.format("DELETE FROM users WHERE name = \'%s\' LIMIT 1",user);
				String decrement_user_count = String.format("UPDATE subjects SET usercount = (usercount - 1) WHERE id IN (SELECT id FROM %s_subjects)",user);
				String get_all_registered_subjects = String.format("SELECT id FROM %s_subjects", user);
				String delete_subjects = String.format("DROP TABLE %s_subjects", user);
				
				try {
					connection.setAutoCommit(false);
					
					Statement statement = connection.createStatement();
					
					statement.execute(remove_user);
					statement.executeLargeUpdate(decrement_user_count);
					
					ResultSet result = statement.executeQuery(get_all_registered_subjects);
					StringBuilder subject_tables = new StringBuilder();
					
					while(result.next()) {
						subject_tables.append("subject_"+result.getInt("id")+"_subs, ");
					}
					
					subject_tables.deleteCharAt(subject_tables.length()-1);
					subject_tables.deleteCharAt(subject_tables.length()-1);
					
					
					String [] subs = subject_tables.toString().split(",");
					for (int i = 0; i < subs.length; i++) {
						
						String target_table = subs[i].trim();
						String condition = target_table.replace("_subs", "_subs.user = \'"+user+"\'");
						String generated_query = "DELETE FROM "+target_table+" WHERE "+condition+" LIMIT 1";
						
						statement.executeUpdate(generated_query);
					}
			
					statement.execute(delete_subjects);
					connection.commit();
					
				} catch (SQLException e) {
					try {
						connection.rollback();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
				
				System.out.println("Successfully deleted user.");
			}

    // As an exception if the user is not in the system the function will return null
    // Generally the existence must be verified on the server side
    public ArrayList<String[]> getAllRegisteredSubjects(String user) {
    	
    	ArrayList<String[]> subjects = null;
    	
    	String fetch_subjects = String.format("SELECT id,subject FROM subjects WHERE id IN (SELECT id FROM %s_subjects)",user);
		try {
			connection.setAutoCommit(false);
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(fetch_subjects);
	    	
	    	subjects = new ArrayList<String[]>();
	    	while(result.next()) {
	    		subjects.add(new String[] {""+result.getInt("id"),result.getString("subject")});
	    	}
	    	
	    	connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			String err_msg = e.getMessage();
			if (err_msg.contains("Table") && err_msg.contains("doesn't exist")) {
				return null;
			} else {
				e.printStackTrace();
			}
		}
    	
		return subjects;
    }
    
    // Get a list of all existing subjects, their id's and registered user count
    public ArrayList<String []> getAllExistingSubjects() {
    	String query = "SELECT id,subject,usercount FROM subjects\r\n";
    	ArrayList<String[]> subjects = null;
		try {
			connection.setAutoCommit(false);
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(query);
	    	subjects = new ArrayList<String[]>();
	    	while(result.next()) {
	    		subjects.add(new String[] {""+result.getInt("id"),result.getString("subject"),""+result.getInt("usercount"),result.getString("last_post")});
	    	}
	    	connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		
		return subjects;
    }
    
    // Register user to a new or existing subject with name only provided
    public void registerToSubject(String user, String subject) {

    	String existance_test = String.format("SELECT id FROM subjects WHERE subject = \'%s\'",subject);
    	String new_row_insertion = String.format("INSERT INTO subjects VALUE(NULL,\'%s\',1,NULL)",subject);
    	String count_increment = String.format("UPDATE subjects SET usercount = (usercount + 1) WHERE subject = \'%s\'",subject);
    	
    	int subject_id;
    	
		try {
			connection.setAutoCommit(false);
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(existance_test);
	    	
	    	// Checking existence of the subject
	    	//		increment subject count in subjects table
	        //		obtained id of the subject
	        //		add the subject id to the user subjects table
	    	if(result.next()) {
	    		subject_id = result.getInt("id");
	    		statement.executeUpdate(count_increment);
	    		
	    		String last_viewed_default;
	    		
				// if checking if was ever registered to it before setting default last_viewed time
	    		String fetch_past_visit = String.format("SELECT time_sent FROM subject_%d WHERE user = \'%s\'",subject_id,user);
	    		ResultSet past_time = statement.executeQuery(fetch_past_visit);
	    		if (past_time.next()) {
	    			last_viewed_default = past_time.getString("time_sent");
	    		} else {
	    			last_viewed_default = formatDateTimeForDatabase(new Date(0l));
	    		}
	    		
	    		String subs_update = String.format("INSERT INTO subject_%d_subs VALUE('%s')", subject_id, user);
	    		statement.executeUpdate(subs_update);
	    		
				String update_user_table = String.format("INSERT INTO %s_subjects VALUE(%d,\'%s\')",user,subject_id,last_viewed_default);
				statement.executeUpdate(update_user_table);
				
				
	    	} 
	    	
	    	// Otherwise create a new subject row
	    	//    	add new row to subjects with count 1
	    	//		get the ID for the subject
	    	// 		create new subject table and subject_subs table
	    	//		add the user to the subject_subs table
	    	//    	add the subject id to the user subjects table
	    	else {
	    		statement.executeUpdate(new_row_insertion);
	    		
	    		result = statement.executeQuery(existance_test);
	    		result.next();
	    		subject_id = result.getInt("id");
	    		
	    		String creating_new_table = String.format("CREATE TABLE subject_%d (number BIGINT AUTO_INCREMENT KEY NOT NULL,user VARCHAR(30) NOT NULL, message VARCHAR(150) NOT NULL,time_sent DATETIME NOT NULL, address VARCHAR(30) NOT NULL)", subject_id);
	    		statement.executeUpdate(creating_new_table);
	    		
	    		String creating_new_subs_table = String.format("CREATE TABLE subject_%d_subs (user VARCHAR(30) NOT NULL)", subject_id);
	    		statement.executeUpdate(creating_new_subs_table);
	    		
	    		String subs_update = String.format("INSERT INTO subject_%d_subs VALUE('%s')", subject_id, user);
	    		statement.executeUpdate(subs_update);
	    		
				String update_user_table = String.format("INSERT INTO %s_subjects VALUE(%d,\'%s\')",user,subject_id,formatDateTimeForDatabase(new Date()));
				statement.executeUpdate(update_user_table);
	    	}
	    	
			
	    	connection.commit();
	    	
			System.out.println("Successfully registered new subject!");
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
    }
	
    // Register user to an existing new subject
    public void registerToSubject(String user, int subject_id) {

    	String count_increment = String.format("UPDATE subjects SET usercount = (usercount + 1) WHERE id = %d",subject_id);

		try {
			
			connection.setAutoCommit(false);
	    	Statement statement = connection.createStatement();

    		statement.executeUpdate(count_increment);
    		
    		String last_viewed_default;
    		
			// if checking if was ever registered to it before setting default last_viewed time
    		String fetch_past_visit = String.format("SELECT time_sent FROM subject_%d WHERE user = \'%s\'",subject_id,user);
    		ResultSet past_time = statement.executeQuery(fetch_past_visit);
    		if (past_time.next()) {
    			last_viewed_default = past_time.getString("time_sent");
    		} else {
    			last_viewed_default = formatDateTimeForDatabase(new Date(0l));
    		}
    		
    		String subs_update = String.format("INSERT INTO subject_%d_subs VALUE('%s')", subject_id, user);
    		statement.executeUpdate(subs_update);
    		
			String update_user_table = String.format("INSERT INTO %s_subjects VALUE(%d,\'%s\')",user,subject_id,last_viewed_default);
			statement.executeUpdate(update_user_table);
				
			
			
	    	connection.commit();
	    	
			System.out.println("Successfully registered new subject!");
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
    }
	
    // Remove the subject from user's list
	public void unregisterSubject(String user, int subject_id) {
		
		//	reduce user count from subjects row
		//  delete row from user's subjects
		String decrement_subject = String.format("UPDATE subjects SET usercount = (usercount-1) WHERE id = %d", subject_id);
		String remove_from_list = String.format("DELETE FROM %s_subjects WHERE id = %d LIMIT 1",user,subject_id);
		String remove_from_subs = String.format("DELETE FROM subject_%d_subs WHERE user = \'%s\' LIMIT 1",subject_id,user);
		try {
			connection.setAutoCommit(false);
			
	    	Statement statement = connection.createStatement();
	    	
	    	statement.executeUpdate(decrement_subject);
	    	statement.executeUpdate(remove_from_subs);
	    	statement.executeUpdate(remove_from_list);
	    	
	    	connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	
	}

	// Sends message to the topic
	public void sendMessage(String user, int subject_id, String message, String address) {
		
		String sending_time = formatDateTimeForDatabase(new Date());
		
		//	insert new row with message into the subject table
		String message_insertion_to_db = String.format("INSERT INTO subject_%d VALUE(NULL,\'%s\',\'%s\',\'%s\',\'%s\')", subject_id,user,message,sending_time,address);
		
		//  update time stamp in the subject row of the subjects table
		String update_last_post_tracker = String.format("UPDATE subjects SET last_post = \'%s\' WHERE id = %d",sending_time,subject_id);
		
		//  update time stamp in the subject row of the user's subjects table
		String update_last_viewed = String.format("UPDATE %s_subjects SET last_viewed = \'%s\' WHERE id = %d", user,sending_time,subject_id);
		
		try {
			connection.setAutoCommit(false);
			
	    	Statement statement = connection.createStatement();

	    	statement.executeUpdate(message_insertion_to_db);
	    	statement.executeUpdate(update_last_post_tracker);
	    	statement.executeUpdate(update_last_viewed);
	    	
	    	connection.commit();
	    	
			System.out.println("Message successfully sent!");
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		
	}
	
	// Fetches a list of subjects that the user is registered to which have new content
	public ArrayList<String[]> fetchSubjectsWithNewContent(String user) {
		String find_all_subjects_with_new_content = String.format("SELECT %s_subjects.id,subjects.subject FROM %s_subjects JOIN subjects ON (subjects.id=%s_subjects.id AND subjects.last_post > %s_subjects.last_viewed)",user,user,user,user);
		ArrayList<String[]> results = null;
		try {
			connection.setAutoCommit(false);
			
	    	Statement statement = connection.createStatement();

	    	ResultSet result = statement.executeQuery(find_all_subjects_with_new_content);
	    	results = new ArrayList<String[]>();
	    	while (result.next()) {
	    		results.add(new String[] {""+result.getInt("id"),result.getString("subject")});
	    	}
	
	    	connection.commit();
	    	
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		
		return results;
	}
	
	// Gets a number of un-viewed messages for a given subject defined by the limit
	public ArrayList<String[]> fetchUnviewedMessages(String user, int subject_id, int limit) {
		
		String fetch_new_messages = String.format("SELECT user,message,time_sent,address FROM subject_%d WHERE time_sent > (SELECT last_viewed FROM %s_subjects WHERE id = %d) LIMIT %d", subject_id,user,subject_id,limit);
		
		ArrayList<String[]> results = null;
		try {
			connection.setAutoCommit(false);
			
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(fetch_new_messages);
	    	
	    	if (result.next()) {
	    		results = new ArrayList<String[]>();
	    		do {
	    			results.add(new String[] {result.getString("user"),result.getString("message"),result.getString("time_sent"),result.getString("address")});
	    		} while (result.next());
	    	}

	    	// Update last viewed record in user's subjects
	    	if (results != null) {
	    		
	    		String most_recent_message_time = results.get(results.size()-1)[2];
	    		
	    		String update_users_last_viewed_time = String.format("UPDATE %s_subjects SET last_viewed = \'%s\' WHERE id = %d LIMIT 1",user,most_recent_message_time,subject_id);
	    		statement.executeUpdate(update_users_last_viewed_time);
	    		
	    	}

	    	connection.commit();
	    	
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	
		return results;
	}
	
	// Fetch last N messages defined by the limit, starting from the lastReceived message id.
	// If lastReceived is -1, fetches the latest messages from the subject
	public ArrayList<String[]> fetchMessages(String user,int lastReceived, int limit, int subject_id){
		
		ArrayList<String[]> messages = null;
		
		String fetch_messages = (lastReceived == -1) ? 
				String.format("SELECT * FROM (SELECT * FROM subject_%d ORDER BY number DESC LIMIT %d) temp ORDER BY number ASC",subject_id,limit) : 
					String.format("SELECT * FROM (SELECT * FROM subject_%d WHERE number < %d ORDER BY number DESC LIMIT %d) temp ORDER BY number ASC",subject_id,lastReceived,limit);
		
		try {
			connection.setAutoCommit(false);
			
	    	Statement statement = connection.createStatement();
	    	
	    	// Fetch messages from the subject
	    	ResultSet result = statement.executeQuery(fetch_messages);
	    	messages = new ArrayList<String[]>();
	    	while (result.next()) {
	    		messages.add(new String[] {""+result.getInt("number"),result.getString("user"),result.getString("message"),result.getString("time_sent"),result.getString("address")});
	    	}
	
	    	// Update user's last viewed timestamp if they are registered to the subject
    		String most_recent_message_time = messages.get(0)[3];
    		
    		String update_users_last_viewed_time = String.format("UPDATE %s_subjects SET last_viewed = \'%s\' WHERE id = %d AND last_viewed < '%s' LIMIT 1",user,most_recent_message_time,subject_id,most_recent_message_time);
    		statement.executeUpdate(update_users_last_viewed_time);
	    	
	    	
	    	connection.commit();
	    	
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}		
				
		return messages;
	}
	
	// Fetch all users subscribed to a subject
	public ArrayList<String[]> getAllUsersSubscribed(int subject_id){
		
		String query = String.format("SELECT name,address FROM users WHERE name IN (SELECT * FROM subject_%d_subs)",subject_id);
		ArrayList<String[]> users = null;
		
		try {
			connection.setAutoCommit(false);
			
	    	Statement statement = connection.createStatement();
	    	ResultSet result = statement.executeQuery(query);
	    	
	    	users = new ArrayList<String[]>();
	    	while (result.next()) {
	    		users.add(new String[] {result.getString("name"),result.getString("address")});
	    	}
	
	    	
	    	connection.commit();
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		
		return users;
	}

	// Transform Date object to Database-friendly DATETIME string
	public static String formatDateTimeForDatabase(Date date) {
		SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
}