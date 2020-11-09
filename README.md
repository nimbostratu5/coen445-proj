# coen445-proj
oct. 14: 
=> single threaded client and server.
=> abstract serialization function for objects

# How To Run
1. Compile client and servers with 'javac Client.java Server_A.java Server_B.java'
2. Run Server_B (Server_A not updated before Server_B) with 'java Server_B' in one terminal
3. Run Client with 'java Client' in another terminal
4. Send message type 'REGISTER', 'UPDATE', etc. by typing it in the Client's terminal
5. Exit program by typing 'BYE' in Client's terminal

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
