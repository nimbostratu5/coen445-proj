# coen445-proj

# How To Run Client and Server
1. Compile client and servers with 'javac -cp mysql-connector-java-8.0.22/mysql-connector-java-8.0.22.jar: Server_A.java Server_B.java Client.java'
2. Run Server_A with 'java -cp mysql-connector-java-8.0.22/mysql-connector-java-8.0.22.jar: Server_A' in one terminal (Same process but for Server_B)
3. Run Client with 'java Client' in another terminal
4. Send message type 'REGISTER', 'UPDATE', etc. by typing it in the Client's terminal
6. Exit server and client by using ctrl-c in their according terminal window

# How to setup and Run DB
1. From SQLWorkbench, press + symbol to add a server for "MySQL Connections"
2. Under host, add database-1.cgsj5cw6pwbs.ca-central-1.rds.amazonaws.com
2. Under username, add admin
3. Under password "store in vault/keychain", add COEN4452020
4. Change connection name to your liking
5. Expand COEN445 to display the tables
6. Before issueing a query from SQL, make sure to write and run (electricity symbol): USE COEN445
7. After running this SQL query, remove USE COEN445, and perform any other query (SELECT, INSERT, DROP, etc.)
8. To run in program, add .jar to program classpath 
9. Connect to the DB from the program
10. Use methods from  DB_interface.java

# JDBC setup
Before being able to run any Database operations you must include the JDBC client jar (system independent) in your build/project path.

# Database operations
All database operations are configured as atomic transactions in the DB_interface.class, which provides a simple interface that the servers need to call.
The a brief test of how it works is configured in DB_driver.class

It is highly advised to not play with it blindly but to install a MySQL workbench available here: https://dev.mysql.com/downloads/workbench/
It will allow you to perform any operations on the database and will help debugging.

Currently implemented actions:
    - Get all users
    - Register new user
    - Get user info
