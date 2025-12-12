import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Scanner;

public class UserInspector {
    public static void main(String[] args) {
        String username = null;
        if (args.length >= 1) username = args[0];
        else {
            Scanner in = new Scanner(System.in);
            System.out.print("Username to inspect: ");
            username = in.nextLine().trim();
        }

        String sql = "SELECT u.user_id, u.empid, u.username, u.password_hash, u.role, u.created_at, u.last_login, e.Fname, e.Lname, e.email, e.HireDate, e.Salary, e.SSN FROM users u JOIN employees e ON u.empid = e.empid WHERE u.username = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            System.out.println("Connected to: " + conn.getMetaData().getURL());
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println("--- User Row ---");
                    for (int i = 1; i <= cols; i++) {
                        System.out.println(md.getColumnName(i) + " = " + rs.getString(i));
                    }
                }
                if (!found) System.out.println("No user found with username='" + username + "'");
            }
        } catch (Exception e) {
            System.err.println("Inspection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
