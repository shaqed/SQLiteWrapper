# SQLiteWrapper

DatabaseHandler is a utility that helps you manage your JDBC calls easily.<br>
 The class will handle the connection calls to the database as well as closing the connections
 after each call.
 <br<br>
 
  How to use: <br>
      Extend this class:<br>
      1) Call the super constructor with the database url ("jdbc:sqlite:[your-url]")<br>
      2) Implement the getConnection(dbURL) like described in the method's documentation<br>
      3) Implement the onCreate() like described in the method's documentation<br>
 <br>
