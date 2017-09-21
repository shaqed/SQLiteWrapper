
import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;


/**
 * DatabaseHandler is a utility that helps you manage your JDBC calls easily.<br>
 * The class will handle the connection calls to the database as well as closing the connections
 * after each call.
 * <br<br>
 * <p>
 * How to use: <br>
 * Extend this class:<br>
 * 1) Call the super constructor with the database url ("jdbc:sqlite:[your-url]")<br>
 * 2) Implement the getConnection(dbURL) like described in the method's documentation<br>
 * 3) Implement the onCreate() like described in the method's documentation<br>
 * <br>
 */
public abstract class DatabaseHandler {

    public static final String COL_TYPE_STRING = "VARCHAR(255)";
    public static final String COL_TYPE_INT = "INTEGER";
    private String dbURL;
    private File dbFile;

    private boolean verbose = true;

    /**
     * Given a jdbc-formatted-url, the DB File is created and the onCreate() method is being invoked if the
     * file is being created for the first time.
     *
     * @param dbURL jdbc:sqlite:[your-url]
     * @throws SQLException Being thrown in the case of a bad URL argument
     */
    public DatabaseHandler(String dbURL) throws SQLException {
        if (dbURL.startsWith("jdbc:sqlite:")) {
            this.dbURL = dbURL;
            dbFile = new File(dbURL.substring(12));

            try {
                if (!dbFile.exists()) {
                    if (dbFile.createNewFile()) {
                        debug("Database file created at : " + dbFile.getPath());
                        onCreate(); // Call the onCreate (because this DB was just created)
                    } else {
                        debug("Database file does not exist AND couldn't create it");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new SQLException("Bad url argument, " + dbURL + " doesn't start with: jdbc:sqlite:[url-here]");
        }
    }

    public DatabaseHandler(String dbURL, boolean verbose) throws SQLException {
        this(dbURL);
        this.verbose = verbose;
    }

    /**
     * Deletes the DB file
     *
     * @return true or false if successful or not
     */
    protected final boolean deleteDatabase() {
        return this.dbFile.delete();
    }


    // Abstract methods

    /**
     * Call the connection of the database here using:
     * <p>
     * Class.forName("jdbc.sqlite.JDBC)
     * Connection c = DriverManager.getConnection("dbURL")
     *
     * @param dbURL The same URL given in the constructor
     */
    @NotNull
    protected abstract Connection getConnection(String dbURL);


    /**
     * This method is called by the DatabaseHandler when a new .db file is being created for the first time
     * Implement this method to call for just-created operations like creating tables and having default data in the database
     */
    protected abstract void onCreate();


    /* Database interactions */

    /**
     * Creates a new table in the database
     *
     * @param tableName       The table name
     * @param idAutoIncrement Have an _id column that counts the rows in the table ?
     * @param columnsNames    Array of strings of the column names [HAS TO BE SAME SIZE AS columnsType]
     * @param columnsType     Array of SQL formatted data types in the columns [HAS TO BE SAME SIZE AS columnsNames]
     * @throws SQLException If the query didn't go as expected, an exception will be thrown
     */
    protected void createTable(String tableName, boolean idAutoIncrement, String[] columnsNames, String[] columnsType)
            throws SQLException {

        if (columnsNames.length != columnsType.length) {
            throw new SQLException("columnsNames and columnsTypes do not have the " +
                    "same length: names: " + columnsNames.length + " types: " + columnsType.length);
        }

        if (columnsNames.length == 0) {
            throw new SQLException("columns size is invalid: " + columnsNames.length);
        }


        StringBuilder query = new StringBuilder();
        query.append("CREATE TABLE IF NOT EXISTS ");
        query.append(tableName);
        query.append(" (");

        if (idAutoIncrement) {
            query.append("_id INTEGER PRIMARY KEY AUTOINCREMENT, ");
        } else {
            query.append("_id INTEGER, ");
        }

        for (int i = 0; i < columnsNames.length; i++) {
            query.append(columnsNames[i]);
            query.append(" ");
            query.append(columnsType[i]);
            if (i < columnsNames.length - 1) {
                query.append(", ");
            }
        }
        query.append(");");

        doSQL(query.toString());
    }

    /**
     * Deletes table from the database
     *
     * @param tableName The table name to be deleted
     */
    protected void deleteTable(String tableName) throws SQLException {
        doSQL("DROP TABLE " + tableName + ";");
    }

    /**
     * Deletes all rows from the table
     *
     * @param tableName The table name
     */
    protected void restartTable(String tableName) throws SQLException {
        doSQL("DELETE * FROM " + tableName + ";");
    }

    /**
     * Insert a new row to a given table in the database
     *
     * @param tableName The table name
     * @param values    [String] ColumnName, [Object] Value (String/int/boolean)
     */
    protected void insert(String tableName, Map<String, Object> values) throws SQLException {
        StringBuilder query = new StringBuilder();

        query.append("INSERT INTO ");
        query.append(tableName);


        query.append(" (");
        int i = 0;
        for (String key : values.keySet()) {
            // Build keys to enter
            query.append(key);
            i++;
            if (i != values.size()) {
                query.append(", ");
            }
        }
        query.append(") VALUES (");
        i = 0;
        for (Object value : values.values()) {
            // Build values array
            if (!(value instanceof String)) {
                query.append(value);
            } else {
                query.append("\"");
                query.append(value);
                query.append("\"");
            }
            i++;
            if (i != values.size()) {
                query.append(", ");
            }
        }
        query.append(");");

        doSQL(query.toString());

    }

    /**
     * Deletes a row from a given table in the database
     *
     * @param tableName    The table name
     * @param whereClauses [ColumnName], [Value] as a where clause. Rows that will contain these values will be deleted
     */
    protected void delete(String tableName, Map<String, Object> whereClauses) throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append(tableName);

        query.append(" WHERE ");
        Iterator iterator = whereClauses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry value = (Map.Entry) iterator.next();
            String key = (String) value.getKey();
            Object val = value.getValue();

            if (!(val instanceof String)) {
                query.append(key);
                query.append("=");
                query.append(String.valueOf(val));

            } else {

                query.append(key);
                query.append("=\"");
                query.append(String.valueOf(val));
                query.append("\"");
            }

            if (iterator.hasNext()) {
                query.append(" AND ");
            }

        }
        query.append(";");
        doSQL(query.toString());
    }

    /**
     * Update the a row in a given table
     *
     * @param tableName    The table name
     * @param updateValues New values to be inserted
     * @param whereClause  SQL-Format boolean statement that will select the rows that will be changed by the updateValues
     */
    protected void update(String tableName, Map<String, Object> updateValues, String whereClause) throws SQLException {
        StringBuilder query = new StringBuilder();
        query.append("UPDATE ");
        query.append(tableName);

        query.append(" SET ");
        Iterator iterator = updateValues.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry value = (Map.Entry) iterator.next();
            String key = (String) value.getKey();
            Object val = value.getValue();

            if (!(val instanceof String)) {
                query.append(key);
                query.append("=");
                query.append(String.valueOf(val));
            } else {
                query.append(key);
                query.append("=\"");
                query.append(String.valueOf(val));
                query.append("\"");
            }

            if (iterator.hasNext()) {
                query.append(" , ");
            }
        }

        if (whereClause != null) {
            query.append(" WHERE ");
            query.append(whereClause);
        }
        query.append(";");

        doSQL(query.toString());
    }

    /**
     * Performs a raw query directly to the database that expects results
     *
     * @param query A valid SQLite query
     * @return QueryData of the data returned by the query
     * @throws SQLException When a problem in the execution occurs (probably due to a bad SQL syntax)
     */
    protected QueryData rawQuery(String query) throws SQLException {
        return doQuery(query);
    }

    /**
     * Performs a raw query directly to the database that expects no results
     *
     * @param query A valid SQLite query
     * @throws SQLException When a problem in the execution occurs (probably due to a bad SQL syntax)
     */
    protected void rawSQL(String query) throws SQLException {
        doSQL(query);
    }


    /* Private inner-methods */
    private QueryData doQuery(String query) throws SQLException {
        QueryData queryData = null;
        Connection connection = getConnection();
        if (connection != null) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = null;
            try {
                debug("Trying to execute query: " + query);
                resultSet = statement.executeQuery(query);

                queryData = new QueryData(resultSet);


                debug("Successful");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
                statement.close();
                connection.close();
            }
        } else {
            System.out.println("Couldn't perform doQuery cause connection was null");
        }
        return queryData;
    }

    private void doSQL(String query) throws SQLException {
        Connection connection = getConnection();
        if (connection != null) {
            Statement statement = connection.createStatement();
            try {
                debug("Trying to execute query: " + query);
                int result = statement.executeUpdate(query);
                debug("Successful: " + result);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                statement.close();
                connection.close();
                debug("Connection closed successfully");
            }
        } else {
            debug("Couldn't perform doSQL cause connection was null");
        }
    }

    private Connection getConnection() {
        Connection connection = getConnection(this.dbURL);
        if (connection == null) {
            debug("getConnection(" + this.dbURL + ") returned null... database connection denied");
        }
        return connection;
    }

    private void debug(String msg) {
        if (this.verbose) {
            System.out.println("DB DEBUG: " + msg);
        }
    }


    /**
     * This class will be used to represent the data given from a query to the database
     * You will not be dealing with the ResultSet class, but only with this
     */
    public final class QueryData {

        private LinkedList<Row> rows;
        private Iterator<Row> currentRowIterator;
        private Row currentRow;

        public QueryData(ResultSet resultSet) throws SQLException {
            rows = new LinkedList<>();
            // Create all rows from the ResultSet and store them in the linked list
            String[] cols = getColumns(resultSet.getMetaData()); // All the columns in the table

            // Extract each row from the ResultSet and add it to the LinkedList
            while (resultSet.next()) {
                LinkedHashMap<String, Object> cells = new LinkedHashMap<>();
                for (String col : cols) {
                    cells.put(col, resultSet.getObject(col));
                }
                rows.add(new Row(cells));
            }
            debug("Loaded: " + rows.size() + " rows from DB");
            currentRowIterator = rows.iterator();
            currentRow = currentRowIterator.next();
        }

        /**
         * On the current row: retrieve a String from a certain column
         *
         * @param colName The column name
         * @return The value inside the table, null if column name did not find anything
         */
        public String getString(String colName) {
            return currentRow.getString(colName);
        }


        public int getInt(String colName) {
            return currentRow.getInt(colName);
        }


        /**
         * Move the cursor to the next row in the table
         *
         * @return true if managed to do so - false if that was the last row in the table
         */
        public boolean nextRow() {
            if (currentRowIterator.hasNext()) {
                currentRow = currentRowIterator.next();
                return true;
            } else {
                return false;
            }
        }

        /**
         * Moves the cursor the the first row in the table
         */
        public void firstRow() {
            currentRowIterator = this.rows.iterator();
            currentRow = currentRowIterator.next();
        }


        private String[] getColumns(ResultSetMetaData metaData) {
            LinkedList<String> strings = new LinkedList<>();
            try {
                int i = 1;
                while (true) {
                    strings.addLast(metaData.getColumnLabel(i++));
                }
            } catch (Exception e) {

            }
            String[] arr = new String[strings.size()];
            strings.toArray(arr);
            return arr;
        }

        private class Row {
            private LinkedHashMap<String, Object> cells;

            public Row(LinkedHashMap<String, Object> cells) {
                this.cells = cells;
            }

            public String getString(String colName) {
                return cells.get(colName).toString();
            }

            public int getInt(String colName) {
                return Integer.valueOf(cells.get(colName).toString());
            }

        }
    }

}
