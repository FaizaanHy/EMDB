import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {

    public List<String> getPayHistory(int empId) {
        String sql = """
            SELECT pay_date, salary
            FROM payroll
            WHERE empid = ?
            ORDER BY pay_date DESC
        """;

        List<String> results = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(
                        "Date: " + rs.getDate("pay_date") +
                        " | Salary: $" + rs.getDouble("salary")
                    );
                }
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }

        return results;
    }

    public List<String> getTotalPayByJobTitle(int year, int month) {
        String sql = """
            SELECT jt.title, SUM(p.salary) AS total_pay
            FROM payroll p
            JOIN employees e ON p.empid = e.empid
            JOIN employee_job_titles ej ON e.empid = ej.empid
            JOIN job_titles jt ON ej.job_title_id = jt.job_title_id
            WHERE YEAR(p.pay_date) = ?
              AND MONTH(p.pay_date) = ?
            GROUP BY jt.title
        """;

        List<String> results = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, year);
            ps.setInt(2, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(
                        rs.getString("title") +
                        ": $" + rs.getDouble("total_pay")
                    );
                }
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }

        return results;
    }

    public List<String> getTotalPayByDivision(int year, int month) {
        String sql = """
            SELECT d.division_name,
                   SUM(e.Salary) / 12 AS monthly_pay
            FROM employees e
            JOIN employee_division ed ON e.empid = ed.empid
            JOIN division d ON ed.div_ID = d.ID
            GROUP BY d.division_name
        """;

        List<String> results = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(
                    rs.getString("division_name") +
                    ": $" + String.format("%.2f", rs.getDouble("monthly_pay"))
                );
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }

        return results;
    }

    public List<String> getEmployeesHiredBetween(Date start, Date end) {
        String sql = """
            SELECT empid, Fname, Lname, HireDate
            FROM employees
            WHERE HireDate BETWEEN ? AND ?
            ORDER BY HireDate
        """;

        List<String> results = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, start);
            ps.setDate(2, end);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(
                        rs.getInt("empid") + " | " +
                        rs.getString("Fname") + " " +
                        rs.getString("Lname") +
                        " | Hired: " + rs.getDate("HireDate")
                    );
                }
            }
        } catch (SQLException e) {
            results.add("Error: " + e.getMessage());
        }

        return results;
    }
}