import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminConsole {
    private final Scanner in = new Scanner(System.in);
    private final ReportService reportService = new ReportService();

    public static void runConsole() {
        new AdminConsole().loop();
    }

    public static void main(String[] args) {
        runConsole();
    }

    private void loop() {
        System.out.println("HR Admin Console - menu mode (type 'exit' to quit)");
        while (true) {
            try {
                System.out.println();
                System.out.println("1) Search employees");
                System.out.println("2) List recent employees");
                System.out.println("3) Edit employee by empid");
                System.out.println("4) Bulk salary update by percentage");
                System.out.println("5) Create new employee");
                System.out.println("6) View Reports");
                System.out.println("7) Exit");
                System.out.print("Choose option: ");
                String opt = readLine();
                if (opt == null) break;
                if (opt.isEmpty()) continue;

                switch (opt) {
                    case "1":
                        doSearchFlow();
                        break;
                    case "2":
                        listRecent();
                        break;
                    case "3":
                        System.out.print("Enter empid to edit: ");
                        try {
                            int id = Integer.parseInt(readLine());
                            editRecord(id);
                        } catch (NumberFormatException ex) {
                            System.out.println("Invalid empid.");
                        }
                        break;
                    case "4":
                        doBulkSalaryUpdate();
                        break;
                    case "5":
                        createNewEmployee();
                        break;
                    case "6":
                        showReportsMenu();
                        break;
                    case "7":
                        System.out.println("Exiting HR Admin Console.");
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void doSearchFlow() {
        System.out.print("Search by (id/name/dob/ssn): ");
        String type = readLine();
        if (type == null || type.isEmpty()) return;

        System.out.print("Enter search value: ");
        String value = readLine();
        if (value == null || value.isEmpty()) return;

        List<Integer> ids = doSearchAndPrint(
            type.equalsIgnoreCase("id") ? value : "",
            type.equalsIgnoreCase("name") ? value : "",
            type.equalsIgnoreCase("dob") ? value : "",
            type.equalsIgnoreCase("ssn") ? value : ""
        );

        if (!ids.isEmpty()) {
            System.out.print("Edit one of these? (enter empid or 'no'): ");
            String choice = readLine();
            if (choice != null && !choice.equalsIgnoreCase("no")) {
                try {
                    int id = Integer.parseInt(choice);
                    editRecord(id);
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid empid.");
                }
            }
        }
    }

    private void listRecent() {
        String sql = "SELECT e.empid, e.Fname, e.Lname, e.email, e.DOB, e.HireDate, e.Salary, e.SSN, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid ORDER BY e.empid DESC LIMIT 20";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.println("empid=" + rs.getInt("empid") + " | " + rs.getString("Fname") + " " + rs.getString("Lname") + " | " + rs.getString("email") + " | dob=" + rs.getString("DOB") + " | salary=" + rs.getDouble("Salary"));
            }
        } catch (SQLException e) {
            System.err.println("List failed: " + e.getMessage());
        }
    }

    private String readLine() {
        String s = in.nextLine().trim();
        return s.equalsIgnoreCase("exit") ? null : s;
    }

    private List<Integer> doSearchAndPrint(String empid, String name, String dob, String ssn) {
        List<Integer> ids = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT e.empid, e.Fname, e.Lname, e.email, e.DOB, e.HireDate, e.Salary, e.SSN, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE 1=1");
        if (!empid.isEmpty()) sql.append(" AND e.empid = ?");
        if (!name.isEmpty()) sql.append(" AND (e.Fname LIKE ? OR e.Lname LIKE ?)");
        if (!dob.isEmpty()) sql.append(" AND e.DOB = ?");
        if (!ssn.isEmpty()) sql.append(" AND e.SSN = ?");

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!empid.isEmpty()) ps.setInt(idx++, Integer.parseInt(empid));
            if (!name.isEmpty()) {
                ps.setString(idx++, "%" + name + "%");
                ps.setString(idx++, "%" + name + "%");
            }
            if (!dob.isEmpty()) ps.setString(idx++, dob);
            if (!ssn.isEmpty()) ps.setString(idx++, ssn);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("empid"));
                    System.out.println("  empid=" + rs.getInt("empid") + " | " + rs.getString("Fname") + " " + rs.getString("Lname"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Search failed: " + e.getMessage());
        }
        return ids;
    }

    private void editRecord(int empid) {
        try (Connection conn = DBConnector.getConnection()) {
            String select = "SELECT e.empid, e.Fname, e.Lname, e.email, e.DOB, e.HireDate, e.Salary, e.SSN FROM employees e WHERE e.empid = ?";
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, empid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Record not found.");
                        return;
                    }
                    String currF = rs.getString("Fname");
                    String currL = rs.getString("Lname");
                    String currEmail = rs.getString("email");
                    String currDOB = rs.getString("DOB");
                    String currHire = rs.getString("HireDate");
                    String currSalary = rs.getString("Salary");
                    String currSSN = rs.getString("SSN");

                    System.out.println("Editing empid=" + empid);
                    System.out.print("Fname [" + currF + "]: ");
                    String f = in.nextLine().trim();
                    if (f.isEmpty()) f = currF;
                    System.out.print("Lname [" + currL + "]: ");
                    String l = in.nextLine().trim();
                    if (l.isEmpty()) l = currL;
                    System.out.print("email [" + currEmail + "]: ");
                    String email = in.nextLine().trim();
                    if (email.isEmpty()) email = currEmail;
                    System.out.print("DOB (YYYY-MM-DD) [" + currDOB + "]: ");
                    String dob = in.nextLine().trim();
                    if (dob.isEmpty()) dob = currDOB;
                    System.out.print("HireDate (YYYY-MM-DD) [" + currHire + "]: ");
                    String hire = in.nextLine().trim();
                    if (hire.isEmpty()) hire = currHire;
                    System.out.print("Salary [" + currSalary + "]: ");
                    String salary = in.nextLine().trim();
                    if (salary.isEmpty()) salary = currSalary;
                    System.out.print("SSN [" + currSSN + "]: ");
                    String ssn = in.nextLine().trim();
                    if (ssn.isEmpty()) ssn = currSSN;

                    String update = "UPDATE employees SET Fname=?, Lname=?, email=?, DOB=?, HireDate=?, Salary=?, SSN=? WHERE empid=?";
                    try (PreparedStatement ups = conn.prepareStatement(update)) {
                        ups.setString(1, f);
                        ups.setString(2, l);
                        ups.setString(3, email);
                        ups.setString(4, dob);
                        ups.setString(5, hire);
                        ups.setObject(6, parseSalary(salary));
                        ups.setString(7, ssn);
                        ups.setInt(8, empid);
                        int u = ups.executeUpdate();
                        if (u > 0) {
                            System.out.println("Successfully updated employee record (empid=" + empid + ")");
                        } else {
                            System.out.println("No rows updated.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Edit failed: " + e.getMessage());
        }
    }

    private Object parseSalary(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return s;
        }
    }

    private void doBulkSalaryUpdate() {
        try {
            System.out.println("\n=== Bulk Salary Update by Percentage ===");
            System.out.print("Enter percentage increase (e.g., 3.2): ");
            String percentStr = readLine();
            if (percentStr == null || percentStr.isEmpty()) {
                System.out.println("Cancelled.");
                return;
            }

            double percentIncrease = Double.parseDouble(percentStr);

            System.out.print("Enter minimum salary (>=): $");
            String minStr = readLine();
            if (minStr == null || minStr.isEmpty()) {
                System.out.println("Cancelled.");
                return;
            }
            double minSalary = Double.parseDouble(minStr);

            System.out.print("Enter maximum salary (<): $");
            String maxStr = readLine();
            if (maxStr == null || maxStr.isEmpty()) {
                System.out.println("Cancelled.");
                return;
            }
            double maxSalary = Double.parseDouble(maxStr);

            if (minSalary >= maxSalary) {
                System.out.println("Error: min must be < max.");
                return;
            }

            System.out.println("\nIncreasing salaries by " + percentIncrease + "% for $" + minSalary + " - $" + maxSalary);
            System.out.print("Confirm? (yes/no): ");
            String confirm = readLine();
            if (confirm == null || !confirm.equalsIgnoreCase("yes")) {
                System.out.println("Cancelled.");
                return;
            }

            try (Connection conn = DBConnector.getConnection()) {
                String sql = "UPDATE employees SET Salary = Salary * (1 + ? / 100.0) WHERE Salary >= ? AND Salary < ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, percentIncrease);
                    ps.setDouble(2, minSalary);
                    ps.setDouble(3, maxSalary);
                    int rows = ps.executeUpdate();
                    System.out.println("Updated " + rows + " employee(s).");
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
        }
    }

    public String createUser(User currentUser, int empId,
                             String username, String password, String role) {

        if (!currentUser.getRole().equalsIgnoreCase("HR")) {
            return "Access denied: Only HR Admin may create users.";
        }

        if (username == null || username.isBlank()) return "Username cannot be empty.";
        if (password == null || password.isBlank()) return "Password cannot be empty.";
        if (!role.equalsIgnoreCase("HR") && !role.equalsIgnoreCase("EMPLOYEE") && !role.equalsIgnoreCase("ADMIN")) {
            return "Role must be HR, ADMIN, or EMPLOYEE.";
        }

        String hashed = PasswordUtil.hash(password);

        UserDAO userDao = new UserDAO();
        boolean success = userDao.insertNewUser(empId, username, hashed, role);

        return success ? "User created successfully."
                       : "Failed to create user.";
    }

    private void createNewEmployee() {
        System.out.println("\n=== Create New Employee ===");
        System.out.print("First Name: ");
        String fname = readLine();
        if (fname == null || fname.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        System.out.print("Last Name: ");
        String lname = readLine();
        if (lname == null || lname.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        System.out.print("Email: ");
        String email = readLine();
        if (email == null || email.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        System.out.print("DOB (YYYY-MM-DD): ");
        String dob = readLine();
        if (dob == null || dob.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        System.out.print("Hire Date (YYYY-MM-DD): ");
        String hireDate = readLine();
        if (hireDate == null || hireDate.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        System.out.print("Salary: $");
        String salaryStr = readLine();
        if (salaryStr == null || salaryStr.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }
        double salary = parseSalary(salaryStr) instanceof Double ? (Double) parseSalary(salaryStr) : 0.0;

        System.out.print("SSN: ");
        String ssn = readLine();
        if (ssn == null || ssn.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        try (Connection conn = DBConnector.getConnection()) {
            String sql = "INSERT INTO employees (Fname, Lname, email, DOB, HireDate, Salary, SSN) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, fname);
                ps.setString(2, lname);
                ps.setString(3, email);
                ps.setString(4, dob);
                ps.setString(5, hireDate);
                ps.setDouble(6, salary);
                ps.setString(7, ssn);

                int rowsInserted = ps.executeUpdate();
                if (rowsInserted > 0) {
                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newEmpId = generatedKeys.getInt(1);
                            System.out.println("\nEmployee created successfully! (empid=" + newEmpId + ")");

                            System.out.print("Create user account for this employee? (yes/no): ");
                            String createUser = readLine();
                            if (createUser != null && createUser.equalsIgnoreCase("yes")) {
                                System.out.print("Username: ");
                                String username = readLine();
                                System.out.print("Password: ");
                                String password = readLine();
                                if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                                    UserDAO userDao = new UserDAO();
                                    if (userDao.insertNewUser(newEmpId, username, password, "EMPLOYEE")) {
                                        System.out.println("User account created successfully.");
                                    } else {
                                        System.out.println("Failed to create user account.");
                                    }
                                } else {
                                    System.out.println("User account creation cancelled.");
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("Failed to create employee.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating employee: " + e.getMessage());
        }
    }

    private void showReportsMenu() {
        System.out.println("\n=== Reports Menu ===");
        while (true) {
            System.out.println();
            System.out.println("1) Total pay by job title (specific month)");
            System.out.println("2) Total pay by division (monthly average)");
            System.out.println("3) Employees hired within date range");
            System.out.println("4) Back");
            System.out.print("Choose option: ");
            String opt = readLine();
            if (opt == null) break;
            if (opt.isEmpty()) continue;

            switch (opt) {
                case "1":
                    reportPayByJobTitle();
                    break;
                case "2":
                    reportPayByDivision();
                    break;
                case "3":
                    reportEmployeesHiredBetween();
                    break;
                case "4":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void reportPayByJobTitle() {
        try {
            System.out.print("Enter year: ");
            int year = Integer.parseInt(readLine());
            System.out.print("Enter month (1-12): ");
            int month = Integer.parseInt(readLine());

            System.out.println("\n=== Total Pay by Job Title (Year=" + year + ", Month=" + month + ") ===");
            List<String> results = reportService.getTotalPayByJobTitle(year, month);
            if (results.isEmpty()) {
                System.out.println("No data found.");
            } else {
                results.forEach(System.out::println);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
    }

    private void reportPayByDivision() {
        System.out.println("\n=== Total Pay by Division (Monthly Average) ===");
        List<String> results = reportService.getTotalPayByDivision(0, 0);
        if (results.isEmpty()) {
            System.out.println("No data found.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private void reportEmployeesHiredBetween() {
        try {
            System.out.print("Enter start date (YYYY-MM-DD): ");
            Date start = Date.valueOf(readLine());
            System.out.print("Enter end date (YYYY-MM-DD): ");
            Date end = Date.valueOf(readLine());

            System.out.println("\n=== Employees Hired Between " + start + " and " + end + " ===");
            List<String> results = reportService.getEmployeesHiredBetween(start, end);
            if (results.isEmpty()) {
                System.out.println("No employees found in this date range.");
            } else {
                results.forEach(System.out::println);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
        }
    }
}