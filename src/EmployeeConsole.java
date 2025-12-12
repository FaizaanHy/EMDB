import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class EmployeeConsole {
    private final Scanner sc = new Scanner(System.in);
    private final ReportService reportService = new ReportService();

    public static void runFor(int empid) {
        new EmployeeConsole().loop(empid);
    }

    private void loop(int empid) {
        System.out.println("\n=== Employee Console ===");
        while (true) {
            System.out.println("\n1) View my profile");
            System.out.println("2) View pay statement history");
            System.out.println("3) Exit");
            System.out.print("Choose option: ");
            String opt = sc.nextLine().trim();
            
            if (opt.isEmpty()) continue;
            
            switch (opt) {
                case "1":
                    show(empid);
                    break;
                case "2":
                    viewPayHistory(empid);
                    break;
                case "3":
                    System.out.println("Exiting Employee Console.");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void viewPayHistory(int empid) {
        System.out.println("\n=== Pay Statement History (Most Recent First) ===");
        List<String> history = reportService.getPayHistory(empid);
        if (history.isEmpty()) {
            System.out.println("No pay history found.");
        } else {
            history.forEach(System.out::println);
        }
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
                System.out.println("\n--- Employee Profile ---");
                System.out.println("ID: " + rs.getInt("empid"));
                System.out.println("Name: " + rs.getString("Fname") + " " + rs.getString("Lname"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Hire Date: " + rs.getDate("HireDate"));
                System.out.println("Salary: $" + rs.getDouble("Salary"));
                System.out.println("SSN: " + rs.getString("SSN"));
                System.out.println("Username: " + rs.getString("username"));
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch employee: " + e.getMessage());
        }
    }

    public boolean employeeExists(int empId) {
        String sql = "SELECT empid FROM employees WHERE empid = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
