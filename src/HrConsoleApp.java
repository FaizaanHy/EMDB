import java.sql.*;
import java.sql.Date;
import java.util.*;

public class HrConsoleApp {
    private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/hr_system?serverTimezone=UTC";
    private static final String DB_USER   = "root";
    private static final String DB_PASS   = "root";

    private final Connection conn;
    private final Scanner sc = new Scanner(System.in);

    private HrConsoleApp(Connection conn) {
        this.conn = conn;
    }

    private enum Role { HR_ADMIN, EMPLOYEE }

    private Role chooseRoleAndMaybeEmpId() {
        while (true) {
            System.out.println("\nChoose role:");
            System.out.println("1) HR Admin");
            System.out.println("2) Employee (view only your data)");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            if ("1".equals(c)) return Role.HR_ADMIN;
            if ("2".equals(c)) return Role.EMPLOYEE;
            System.out.println("Invalid choice.");
        }
    }


    private void addEmployee() throws SQLException {
        System.out.println("Add new employee:");
        System.out.print("First name: "); String fn = sc.nextLine().trim();
        System.out.print("Last name: ");  String ln = sc.nextLine().trim();
        System.out.print("SSN (9 digits): "); String ssn = sc.nextLine().trim();
        System.out.print("DOB (YYYY-MM-DD): "); LocalDate dob = LocalDate.parse(sc.nextLine().trim());
        System.out.print("Email: "); String email = sc.nextLine().trim();
        System.out.print("Address line1: "); String a1 = sc.nextLine().trim();
        System.out.print("Address line2: "); String a2 = sc.nextLine().trim();
        System.out.print("City: "); String city = sc.nextLine().trim();
        System.out.print("State (2-letter): "); String state = sc.nextLine().trim();
        System.out.print("Postal code: "); String pc = sc.nextLine().trim();
        System.out.print("HireDate (YYYY-MM-DD): "); LocalDate hd = LocalDate.parse(sc.nextLine().trim());
        System.out.print("Salary (e.g. 55000.00): "); double sal = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Is full time? (y/n): "); boolean ft = sc.nextLine().trim().toLowerCase().startsWith("y");

        String sql = "INSERT INTO employees (Fname, Lname, SSN, DOB, email, addressLine1, addressLine2, city, `state`, postalCode, HireDate, Salary, is_full_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fn);
            ps.setString(2, ln);
            ps.setString(3, ssn);
            ps.setDate(4, Date.valueOf(dob));
            ps.setString(5, email);
            ps.setString(6, a1);
            ps.setString(7, a2);
            ps.setString(8, city);
            ps.setString(9, state);
            ps.setString(10, pc);
            ps.setDate(11, Date.valueOf(hd));
            ps.setDouble(12, sal);
            ps.setBoolean(13, ft);
            int updated = ps.executeUpdate();
            if (updated == 1) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) System.out.println("Inserted employee with emp_id=" + rs.getInt(1));
                }
            } else {
                System.out.println("Insert failed.");
            }
        }
    }

    private void updateEmployee() throws SQLException {
        System.out.print("Enter emp_id to update: ");
        int empId = Integer.parseInt(sc.nextLine().trim());
        System.out.println("Leave a value blank to keep existing.");
        Map<String,String> updates = new HashMap<>();
        System.out.print("First name: "); updates.put("Fname", sc.nextLine().trim());
        System.out.print("Last name: "); updates.put("Lname", sc.nextLine().trim());
        System.out.print("Email: "); updates.put("email", sc.nextLine().trim());
        System.out.print("Address line1: "); updates.put("addressLine1", sc.nextLine().trim());
        System.out.print("City: "); updates.put("city", sc.nextLine().trim());
        System.out.print("State: "); updates.put("state", sc.nextLine().trim());
        System.out.print("Postal code: "); updates.put("postalCode", sc.nextLine().trim());

        StringBuilder sb = new StringBuilder("UPDATE employees SET ");
        List<String> parts = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String,String> e : updates.entrySet()) {
            if (!e.getValue().isEmpty()) {
                parts.add(e.getKey() + " = ?");
                params.add(e.getValue());
            }
        }
        if (parts.isEmpty()) {
            System.out.println("No updates provided.");
            return;
        }
        sb.append(String.join(", ", parts));
        sb.append(" WHERE emp_id = ?");
        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int idx = 1;
            for (Object p : params) ps.setObject(idx++, p);
            ps.setInt(idx, empId);
            int n = ps.executeUpdate();
            System.out.println("Updated rows: " + n);
        }
    }

    private void deleteEmployee() throws SQLException {
        System.out.print("Enter emp_id to delete: ");
        int empId = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Confirm delete employee " + empId + " (y/n): ");
        if (!sc.nextLine().trim().toLowerCase().startsWith("y")) {
            System.out.println("Cancelled.");
            return;
        }
        String sql = "DELETE FROM employees WHERE emp_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            System.out.println("Deleted rows: " + ps.executeUpdate());
        }
    }

    private void searchEmployeeById() throws SQLException {
        System.out.print("Enter emp_id: ");
        int empId = Integer.parseInt(sc.nextLine().trim());
        printEmployeeDetails(empId);
    }

    private void searchEmployeeByName() throws SQLException {
        System.out.print("Enter name fragment: ");
        String like = "%" + sc.nextLine().trim() + "%";
        String sql = "SELECT emp_id, Fname, Lname, SSN, HireDate, Salary FROM employees WHERE Fname LIKE ? OR Lname LIKE ? ORDER BY Lname, Fname";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("ID:%d | %s %s | SSN:%s | Hired:%s | Salary: %.2f%n",
                        rs.getInt("emp_id"),
                        rs.getString("Fname"),
                        rs.getString("Lname"),
                        rs.getString("SSN"),
                        rs.getDate("HireDate"),
                        rs.getDouble("Salary"));
                }
                if (!any) System.out.println("No employees found.");
            }
        }
    }

    private void searchBySSN() throws SQLException {
        System.out.print("Enter SSN: ");
        String ssn = sc.nextLine().trim();
        String sql = "SELECT emp_id FROM employees WHERE SSN = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ssn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) printEmployeeDetails(rs.getInt(1));
                else System.out.println("No employee with that SSN.");
            }
        }
    }

    private void printEmployeeDetails(int empId) throws SQLException {
        String sql = "SELECT * FROM employees WHERE emp_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Employee record:");
                    System.out.printf("ID: %d | %s %s | SSN: %s | DOB: %s | Email: %s%n",
                            rs.getInt("emp_id"),
                            rs.getString("Fname"),
                            rs.getString("Lname"),
                            rs.getString("SSN"),
                            rs.getDate("DOB"),
                            rs.getString("email"));
                    System.out.printf("Address: %s, %s, %s %s%n",
                            rs.getString("addressLine1"),
                            rs.getString("city"),
                            rs.getString("state"),
                            rs.getString("postalCode"));
                    System.out.printf("HireDate: %s | Salary: %.2f | FullTime: %s%n",
                            rs.getDate("HireDate"),
                            rs.getDouble("Salary"),
                            rs.getBoolean("is_full_time"));
                } else {
                    System.out.println("Not found.");
                }
            }
        }
    }


    private void updateSalaryByPercentage() throws SQLException {
        System.out.print("Enter minimum salary (inclusive): ");
        double min = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Enter maximum salary (inclusive): ");
        double max = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Enter percentage increase (e.g. 5 for +5% or -3 for -3%): ");
        double pct = Double.parseDouble(sc.nextLine().trim());

        String sql = "UPDATE employees SET Salary = Salary * (1 + ?/100) WHERE Salary BETWEEN ? AND ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, pct);
            ps.setDouble(2, min);
            ps.setDouble(3, max);
            int rows = ps.executeUpdate();
            System.out.println("Rows updated: " + rows);
        }
    }


    private void addPayrollEntry() throws SQLException {
        System.out.print("emp_id: "); int empId = Integer.parseInt(sc.nextLine().trim());
        System.out.print("pay_date (YYYY-MM-DD): "); LocalDate pd = LocalDate.parse(sc.nextLine().trim());
        System.out.print("earnings (gross): "); double earnings = Double.parseDouble(sc.nextLine().trim());
        System.out.print("fed_tax: "); double fedTax = Double.parseDouble(sc.nextLine().trim());
        System.out.print("state_tax: "); double stateTax = Double.parseDouble(sc.nextLine().trim());
        System.out.print("fed_SS: "); double fedSS = Double.parseDouble(sc.nextLine().trim());
        System.out.print("fed_med: "); double fedMed = Double.parseDouble(sc.nextLine().trim());
        System.out.print("retire_401k: "); double r401k = Double.parseDouble(sc.nextLine().trim());
        System.out.print("health_care: "); double hc = Double.parseDouble(sc.nextLine().trim());

        String sql = "INSERT INTO payroll (emp_id, pay_date, earnings, fed_tax, state_tax, fed_SS, fed_med, retire_401k, health_care) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setDate(2, Date.valueOf(pd));
            ps.setDouble(3, earnings);
            ps.setDouble(4, fedTax);
            ps.setDouble(5, stateTax);
            ps.setDouble(6, fedSS);
            ps.setDouble(7, fedMed);
            ps.setDouble(8, r401k);
            ps.setDouble(9, hc);
            ps.executeUpdate();
            System.out.println("Payroll entry inserted.");
        }
    }

    private void printPayHistoryForEmployee(int empId) throws SQLException {
        String sql = "SELECT payID, pay_date, earnings, fed_tax, state_tax, fed_SS, fed_med, retire_401k, health_care " +
                     "FROM payroll WHERE emp_id = ? ORDER BY pay_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("Pay history for emp_id=" + empId);
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("payID:%d | date:%s | gross: %.2f | fed_tax: %.2f | state_tax: %.2f | fed_SS: %.2f | fed_med: %.2f | 401k: %.2f | health: %.2f%n",
                        rs.getInt("payID"),
                        rs.getDate("pay_date"),
                        rs.getDouble("earnings"),
                        rs.getDouble("fed_tax"),
                        rs.getDouble("state_tax"),
                        rs.getDouble("fed_SS"),
                        rs.getDouble("fed_med"),
                        rs.getDouble("retire_401k"),
                        rs.getDouble("health_care"));
                }
                if (!any) System.out.println("No payroll records found.");
            }
        }
    }


    private void printTotalPayByJobTitle(int year, int month) throws SQLException {
        String sql = "SELECT jt.job_title, SUM(p.earnings) AS total_pay " +
                     "FROM payroll p " +
                     "JOIN employees e ON p.emp_id = e.emp_id " +
                     "JOIN employee_job_titles ej ON e.emp_id = ej.emp_id " +
                     "JOIN job_titles jt ON ej.job_title_id = jt.job_title_id " +
                     "WHERE YEAR(p.pay_date) = ? AND MONTH(p.pay_date) = ? " +
                     "GROUP BY jt.job_title ORDER BY total_pay DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("Total pay for %04d-%02d by job title:%n", year, month);
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%s : $%.2f%n", rs.getString("job_title"), rs.getDouble("total_pay"));
                }
                if (!any) System.out.println("No data for that month.");
            }
        }
    }


    private void printTotalPayByDivision(int year, int month) throws SQLException {
        String sql = "SELECT d.division_name, SUM(p.earnings) AS total_pay " +
                     "FROM payroll p " +
                     "JOIN employees e ON p.emp_id = e.emp_id " +
                     "JOIN employee_division ed ON e.emp_id = ed.emp_id " +
                     "JOIN division d ON ed.div_id = d.div_id " +
                     "WHERE YEAR(p.pay_date) = ? AND MONTH(p.pay_date) = ? " +
                     "GROUP BY d.division_name ORDER BY total_pay DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("Total pay for %04d-%02d by division:%n", year, month);
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%s : $%.2f%n", rs.getString("division_name"), rs.getDouble("total_pay"));
                }
                if (!any) System.out.println("No data for that month.");
            }
        }
    }

    private void printEmployeesHiredBetween(LocalDate start, LocalDate end) throws SQLException {
        String sql = "SELECT emp_id, Fname, Lname, HireDate, Salary FROM employees WHERE HireDate BETWEEN ? AND ? ORDER BY HireDate";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("Employees hired between %s and %s:%n", start, end);
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("ID:%d | %s %s | Hired:%s | Salary: %.2f%n",
                        rs.getInt("emp_id"),
                        rs.getString("Fname"),
                        rs.getString("Lname"),
                        rs.getDate("HireDate"),
                        rs.getDouble("Salary"));
                }
                if (!any) System.out.println("No employees in that range.");
            }
        }
    }

    private void hrMenu() throws SQLException {
        while (true) {
            System.out.println("\nHR Admin menu:");
            System.out.println("1) Add employee");
            System.out.println("2) Update employee");
            System.out.println("3) Delete employee");
            System.out.println("4) Search employee by id");
            System.out.println("5) Search by name");
            System.out.println("6) Search by SSN");
            System.out.println("7) Add payroll entry");
            System.out.println("8) Update salary by % (range)");
            System.out.println("9) Reports");
            System.out.println("0) Logout / Exit");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            try {
                switch (c) {
                    case "1": addEmployee(); break;
                    case "2": updateEmployee(); break;
                    case "3": deleteEmployee(); break;
                    case "4": searchEmployeeById(); break;
                    case "5": searchEmployeeByName(); break;
                    case "6": searchBySSN(); break;
                    case "7": addPayrollEntry(); break;
                    case "8": updateSalaryByPercentage(); break;
                    case "9": reportsMenu(); break;
                    case "0": return;
                    default: System.out.println("Invalid.");
                }
            } catch (SQLException ex) {
                System.err.println("SQL error: " + ex.getMessage());
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }

    private void employeeMenu(int empId) throws SQLException {
        while (true) {
            System.out.println("\nEmployee menu (ID=" + empId + "):");
            System.out.println("1) View my profile");
            System.out.println("2) View my pay history");
            System.out.println("3) Search colleague by name (limited)");
            System.out.println("0) Logout / Exit");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            try {
                switch (c) {
                    case "1": printEmployeeDetails(empId); break;
                    case "2": printPayHistoryForEmployee(empId); break;
                    case "3": searchEmployeeByName(); break;
                    case "0": return;
                    default: System.out.println("Invalid.");
                }
            } catch (SQLException ex) {
                System.err.println("SQL error: " + ex.getMessage());
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }

    private void reportsMenu() throws SQLException {
        while (true) {
            System.out.println("\nReports:");
            System.out.println("1) Pay history for employee (6a)");
            System.out.println("2) Total pay by job title for month (6b)");
            System.out.println("3) Total pay by division for month (6c)");
            System.out.println("4) Employees hired between dates (6d)");
            System.out.println("0) Back");
            System.out.print("> ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1":
                    System.out.print("Enter emp_id: ");
                    int id = Integer.parseInt(sc.nextLine().trim());
                    printPayHistoryForEmployee(id);
                    break;
                case "2":
                    int[] ym = readYearMonth();
                    printTotalPayByJobTitle(ym[0], ym[1]);
                    break;
                case "3":
                    int[] ym2 = readYearMonth();
                    printTotalPayByDivision(ym2[0], ym2[1]);
                    break;
                case "4":
                    System.out.print("Start (YYYY-MM-DD): "); LocalDate s = LocalDate.parse(sc.nextLine().trim());
                    System.out.print("End (YYYY-MM-DD): "); LocalDate e = LocalDate.parse(sc.nextLine().trim());
                    printEmployeesHiredBetween(s, e);
                    break;
                case "0": return;
                default: System.out.println("Invalid.");
            }
        }
    }

    private int[] readYearMonth() {
        System.out.print("Year (e.g. 2025): ");
        int y = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Month (1-12): ");
        int m = Integer.parseInt(sc.nextLine().trim());
        return new int[]{y,m};
    }


    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            HrConsoleApp app = new HrConsoleApp(conn);
            System.out.println("Connected to DB.");
            Role role = app.chooseRoleAndMaybeEmpId();
            if (role == Role.HR_ADMIN) {
                app.hrMenu();
            } else {

                System.out.print("Enter your emp_id: ");
                int empId = Integer.parseInt(app.sc.nextLine().trim());
                app.employeeMenu(empId);
            }
            System.out.println("Goodbye.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
