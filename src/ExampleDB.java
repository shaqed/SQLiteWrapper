import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ExampleDB extends DatabaseHandler {


    public static final String TABLE_NAME = "Students";
    public static final String COL_NAME = "Name";

    public ExampleDB(String dbURL) throws SQLException {
        super("jdbc:sqlite:" + dbURL);
    }

    @Override
    protected Connection getConnection(String dbURL) {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(dbURL);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        return c;

    }

    @Override
    protected void onCreate() {
        try {
            createTable(TABLE_NAME, true, new String[]{COL_NAME}, new String[]{DatabaseHandler.COL_TYPE_STRING});
            System.out.println("Created a table");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addNewStudent() {
        Map<String, Object> contentValues = new HashMap<>();
        contentValues.put(COL_NAME, "Shaked");
        try {
            insert(TABLE_NAME, contentValues);
            System.out.println("Added a new student");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }



}
