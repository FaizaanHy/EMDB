import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {
    private final Connection conn = DBConnection.getConnection();

    // Pay history for employee
    public List<String> getPayHistory(int empId) {
        String sql = "SELECT pay_date, earnings FROM payroll WHERE emp_id = ? ORDER BY pay_date DESC";
        List<String> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add("Date: " + rs.getDate("pay_date") + " | Earnings: $" + rs.getDouble("earnings"));
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }
        return results;
    }

    // Total pay by job title
    public List<String> getTotalPayByJobTitle(int year, int month) {
        String sql = """
            SELECT jt.job_title, SUM(p.earnings) AS total_pay
            FROM payroll p
            JOIN employees e ON p.emp_id = e.emp_id
            JOIN employee_job_titles ej ON e.emp_id = ej.emp_id
            JOIN job_titles jt ON ej.job_title_id = jt.job_title_id
            WHERE YEAR(p.pay_date) = ? AND MONTH(p.pay_date) = ?
            GROUP BY jt.job_title
            """;
        List<String> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("job_title") + ": $" + rs.getDouble("total_pay"));
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }
        return results;
    }

    // Total pay by division
    public List<String> getTotalPayByDivision(int year, int month) {
        String sql = """
            SELECT d.division_name, SUM(p.earnings) AS total_pay
            FROM payroll p
            JOIN employees e ON p.emp_id = e.emp_id
            JOIN employee_division ed ON e.emp_id = ed.emp_id
            JOIN division d ON ed.div_id = d.div_id
            WHERE YEAR(p.pay_date) = ? AND MONTH(p.pay_date) = ?
            GROUP BY d.division_name
            """;
        List<String> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("division_name") + ": $" + rs.getDouble("total_pay"));
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }
        return results;
    }

    // Employees hired between dates
    public List<String> getEmployeesHiredBetween(Date start, Date end) {
        String sql = "SELECT emp_id, Fname, Lname, HireDate FROM employees WHERE HireDate BETWEEN ? AND ? ORDER BY HireDate";
        List<String> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, start);
            ps.setDate(2, end);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(rs.getInt("emp_id") + " | " + rs.getString("Fname") + " " + rs.getString("Lname") + " | Hired: " + rs.getDate("HireDate"));
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }
        return results;
    }
}
