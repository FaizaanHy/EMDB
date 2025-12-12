import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class EmployeeConsole {
    public static void runFor(int empid) {
        new EmployeeConsole().show(empid);
    }

    private void show(int empid) {
        String sql = "SELECT e.empid, e.Fname, e.Lname, e.email, e.HireDate, e.Salary, e.SSN, u.username " +
                     "FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.empid = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empid);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Employee not found: " + empid);
                    return;
                }
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                System.out.println("--- Employee " + empid + " ---");
                for (int i = 1; i <= cols; i++) {
                    System.out.println(md.getColumnName(i) + " = " + rs.getString(i));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch employee: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
