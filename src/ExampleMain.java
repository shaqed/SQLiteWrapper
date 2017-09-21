import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class ExampleMain {
    public static void main(String[] args) {

        try {
            ExampleDB db = new ExampleDB("mydb1.db");

            db.printDB();
            System.out.println("All went well");


            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put(ExampleDB.COL_NAME, "Kuku3");


            db.insert(ExampleDB.TABLE_NAME, hashMap);

            db.printDB();


        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
