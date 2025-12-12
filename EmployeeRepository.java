import java.sql.*;
import java.util.*;

public class EmployeeRepository {

    public Employee findByEmpID(int empid) {
        String sql = "SELECT e.*, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.empid = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, empid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractEmployee(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Employee> findByName(String name) {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.Fname LIKE ? OR e.Lname LIKE ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name + "%");
            stmt.setString(2, "%" + name + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(extractEmployee(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Employee findBySSN(String ssn) {
        String sql = "SELECT e.*, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.SSN = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ssn);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return extractEmployee(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Employee> findByDOB(String dob) {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT e.*, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.DOB = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dob);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(extractEmployee(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Employee extractEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setEmpId(rs.getInt("empid"));
        emp.setFirstName(rs.getString("Fname"));
        emp.setLastName(rs.getString("Lname"));
        emp.setDob(rs.getString("DOB"));
        emp.setSsn(rs.getString("SSN"));
        emp.setEmail(rs.getString("email"));
        emp.setHireDate(rs.getString("HireDate"));
        emp.setSalary(rs.getDouble("Salary"));
        emp.setUsername(rs.getString("username"));
        return emp;
    }

    /**
     * Search for employee data by type (id, name, dob, ssn).
     * Only admins can search for editing purposes.
     */
    public Object searchForEditing(String type, String value, User currentUser) {
        // Only admins can search to edit
        if (!currentUser.getRole().equalsIgnoreCase("HR")) {
            System.out.println("Access denied: Admin only.");
            return null;
        }

        // Route based on search type
        switch (type.toLowerCase()) {
            case "id":
                try {
                    int id = Integer.parseInt(value);
                    return findByEmpID(id);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid ID format.");
                    return null;
                }
            case "name":
                return findByName(value);
            case "dob":
                return findByDOB(value);
            case "ssn":
                return findBySSN(value);
            default:
                System.out.println("Invalid search type. Use: id, name, dob, ssn");
                return null;
        }
    }

    /**
     * Get employee's own profile for view-only access.
     * Employees can only view their own profile; SSN is masked for non-admins.
     */
    public Employee getOwnProfile(int empId, User currentUser) {
        // General employees can ONLY view their own profile
        if (currentUser.getRole().equalsIgnoreCase("EMPLOYEE") &&
            currentUser.getEmpId() != empId) {
            System.out.println("Access denied: cannot view another employee.");
            return null;
        }

        // Retrieve employee
        Employee emp = findByEmpID(empId);

        if (emp == null) {
            System.out.println("Employee not found.");
            return null;
        }

        // Mask SSN for non-admins
        if (!currentUser.getRole().equalsIgnoreCase("HR")) {
            String ssn = emp.getSsn();
            if (ssn != null && ssn.length() >= 4) {
                emp.setSsn("***-**-" + ssn.substring(ssn.length() - 4));
            }
        }

        return emp;
    }
}
