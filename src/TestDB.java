import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        String sql = "SELECT * FROM employees LIMIT 5";
        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            System.out.println("Connected to: " + conn.getMetaData().getURL());
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    System.out.print(md.getColumnName(i) + "=");
                    System.out.print(rs.getString(i));
                    if (i < cols) System.out.print(" | ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.err.println("TestDB error: ");
            e.printStackTrace();
        }
    }
}
