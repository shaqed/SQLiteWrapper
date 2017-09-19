import java.sql.SQLException;

public class ExampleMain {
    public static void main(String[] args) {

        try {
            ExampleDB db = new ExampleDB("mydb1.db");

            db.addNewStudent();
            System.out.println("All went well");
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
