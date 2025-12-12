import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminConsole {
    private final Scanner in = new Scanner(System.in);

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
                System.out.println("4) Bulk salary update by percentage (HR Admin only)");
                System.out.println("5) Exit");
                System.out.print("Choose option: ");
                String opt = readLine(); if (opt == null) break;
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
                        String idStr = readLine(); if (idStr == null) { opt = null; break; }
                        if (!idStr.isEmpty()) {
                            try { int id = Integer.parseInt(idStr); editRecord(id); } catch (NumberFormatException nfe) { System.out.println("Invalid empid."); }
                        }
                        break;
                    case "4":
                        doBulkSalaryUpdate();
                        break;
                    case "5":
                        System.out.println("Exiting HR Admin Console.");
                        return;
                    default:
                        System.out.println("Unknown option.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void doSearchFlow() {
        System.out.print("Search by empid (or press Enter): ");
        String empid = readLine(); if (empid == null) return;
        System.out.print("Search by name (Fname or Lname, or press Enter): ");
        String name = readLine(); if (name == null) return;
        System.out.print("Search by DOB (YYYY-MM-DD, or press Enter): ");
        String dob = readLine(); if (dob == null) return;
        System.out.print("Search by SSN (or press Enter): ");
        String ssn = readLine(); if (ssn == null) return;

        List<Integer> found = doSearchAndPrint(empid, name, dob, ssn);
        if (found.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        System.out.print("Edit a record? Enter empid to edit or press Enter to continue: ");
        String choose = readLine(); if (choose == null) return;
        if (!choose.isEmpty()) {
            try {
                int id = Integer.parseInt(choose);
                if (found.contains(id)) {
                    editRecord(id);
                } else {
                    System.out.println("empid not in current results.");
                }
            } catch (NumberFormatException ex) {
                System.out.println("Invalid empid.");
            }
        }
    }

    private void listRecent() {
        // show recent 20 rows
        String sql = "SELECT e.empid, e.Fname, e.Lname, e.email, e.DOB, e.HireDate, e.Salary, e.SSN, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid ORDER BY e.empid DESC LIMIT 20";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    line.append(md.getColumnName(i)).append("=").append(rs.getString(i));
                    if (i < cols) line.append(" | ");
                }
                System.out.println(line.toString());
            }
        } catch (SQLException e) {
            System.err.println("List failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String readLine() {
        String s = in.nextLine().trim();
        if (s.equalsIgnoreCase("exit")) return null;
        return s;
    }

    private List<Integer> doSearchAndPrint(String empid, String name, String dob, String ssn) {
        List<Integer> ids = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT e.empid, e.Fname, e.Lname, e.email, e.DOB, e.HireDate, e.Salary, e.SSN, u.username " +
                                           "FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE 1=1");
        if (!empid.isEmpty()) sql.append(" AND e.empid = ?");
        if (!name.isEmpty()) sql.append(" AND (e.Fname LIKE ? OR e.Lname LIKE ?)");
        if (!dob.isEmpty()) sql.append(" AND e.DOB = ?");
        if (!ssn.isEmpty()) sql.append(" AND e.SSN = ?");

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (!empid.isEmpty()) ps.setInt(idx++, Integer.parseInt(empid));
            if (!name.isEmpty()) { ps.setString(idx++, "%"+name+"%"); ps.setString(idx++, "%"+name+"%"); }
            if (!dob.isEmpty()) ps.setString(idx++, dob);
            if (!ssn.isEmpty()) ps.setString(idx++, ssn);

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                while (rs.next()) {
                    int id = rs.getInt("empid");
                    ids.add(id);
                    StringBuilder line = new StringBuilder();
                    for (int i = 1; i <= cols; i++) {
                        line.append(md.getColumnName(i)).append("=").append(rs.getString(i));
                        if (i < cols) line.append(" | ");
                    }
                    System.out.println(line.toString());
                }
            }
        } catch (SQLException e) {
            System.err.println("Search failed: " + e.getMessage());
            e.printStackTrace();
        }
        return ids;
    }

    private void editRecord(int empid) {
        try (Connection conn = DBConnector.getConnection()) {
            String select = "SELECT e.empid, e.Fname, e.Lname, e.email, e.DOB, e.HireDate, e.Salary, e.SSN, u.username, u.password_hash FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.empid = ?";
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, empid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Record not found."); return; }
                    String currF = rs.getString("Fname");
                    String currL = rs.getString("Lname");
                    String currEmail = rs.getString("email");
                    String currDOB = rs.getString("DOB");
                    String currHire = rs.getString("HireDate");
                    String currSalary = rs.getString("Salary");
                    String currSSN = rs.getString("SSN");
                    String currUser = rs.getString("username");

                    System.out.println("Editing empid=" + empid + " (press Enter to keep current value)");
                    System.out.print("Fname [" + currF + "] : "); String f = in.nextLine().trim(); if (f.isEmpty()) f = currF;
                    System.out.print("Lname [" + currL + "] : "); String l = in.nextLine().trim(); if (l.isEmpty()) l = currL;
                    System.out.print("email [" + currEmail + "] : "); String email = in.nextLine().trim(); if (email.isEmpty()) email = currEmail;
                    System.out.print("DOB (YYYY-MM-DD) [" + currDOB + "] : "); String dob = in.nextLine().trim(); if (dob.isEmpty()) dob = currDOB;
                    System.out.print("HireDate (YYYY-MM-DD) [" + currHire + "] : "); String hire = in.nextLine().trim(); if (hire.isEmpty()) hire = currHire;
                    System.out.print("Salary [" + currSalary + "] : "); String salary = in.nextLine().trim(); if (salary.isEmpty()) salary = currSalary;
                    System.out.print("SSN [" + currSSN + "] : "); String ssn = in.nextLine().trim(); if (ssn.isEmpty()) ssn = currSSN;

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
                            System.out.println("No rows updated. Employee may not exist.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Edit failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Object parseSalary(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return s; }
    }

    /**
     * Feature 5: Bulk salary update by percentage for a salary range.
     * HR Admin only - updates all employees whose salary falls within the specified range.
     * Example: 3.2% increase for salaries >= 58000 and < 105000
     */
    private void doBulkSalaryUpdate() {
        try {
            System.out.println("\n=== Bulk Salary Update by Percentage ===");
            System.out.println("This will update salaries for employees within a specified salary range.");
            System.out.println("Example: 3.2% increase for salaries >= 58000 and < 105000");
            
            // Get percentage increase
            System.out.print("Enter percentage increase (e.g., 3.2 for 3.2%): ");
            String percentStr = readLine();
            if (percentStr == null || percentStr.isEmpty()) {
                System.out.println("Operation cancelled.");
                return;
            }
            
            double percentIncrease;
            try {
                percentIncrease = Double.parseDouble(percentStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid percentage. Please enter a number (e.g., 3.2).");
                return;
            }
            
            if (percentIncrease < 0 || percentIncrease > 100) {
                System.out.println("Warning: Percentage is outside typical range (0-100%). Continuing anyway...");
            }
            
            // Get minimum salary (inclusive)
            System.out.print("Enter minimum salary (inclusive, >=): $");
            String minSalaryStr = readLine();
            if (minSalaryStr == null || minSalaryStr.isEmpty()) {
                System.out.println("Operation cancelled.");
                return;
            }
            
            double minSalary;
            try {
                minSalary = Double.parseDouble(minSalaryStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid minimum salary. Please enter a number.");
                return;
            }
            
            // Get maximum salary (exclusive)
            System.out.print("Enter maximum salary (exclusive, <): $");
            String maxSalaryStr = readLine();
            if (maxSalaryStr == null || maxSalaryStr.isEmpty()) {
                System.out.println("Operation cancelled.");
                return;
            }
            
            double maxSalary;
            try {
                maxSalary = Double.parseDouble(maxSalaryStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid maximum salary. Please enter a number.");
                return;
            }
            
            if (minSalary >= maxSalary) {
                System.out.println("Error: Minimum salary must be less than maximum salary.");
                return;
            }
            
            // Show summary and confirm
            System.out.println("\n--- Summary ---");
            System.out.println("Percentage increase: " + percentIncrease + "%");
            System.out.println("Salary range: >= $" + String.format("%.2f", minSalary) + " and < $" + String.format("%.2f", maxSalary));
            System.out.print("Proceed with bulk update? (yes/no): ");
            String confirm = readLine();
            if (confirm == null || !confirm.equalsIgnoreCase("yes")) {
                System.out.println("Operation cancelled.");
                return;
            }
            
            // Perform the bulk update
            performBulkSalaryUpdate(percentIncrease, minSalary, maxSalary);
            
        } catch (Exception e) {
            System.err.println("Error during bulk salary update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Performs the actual bulk salary update in the database.
     * Updates all employees whose salary is >= minSalary and < maxSalary
     * by applying the percentage increase.
     */
    private void performBulkSalaryUpdate(double percentIncrease, double minSalary, double maxSalary) {
        try (Connection conn = DBConnector.getConnection()) {
            // First, get count and preview of employees that will be affected
            String previewSql = "SELECT empid, Fname, Lname, Salary FROM employees WHERE Salary >= ? AND Salary < ? ORDER BY empid";
            try (PreparedStatement previewPs = conn.prepareStatement(previewSql)) {
                previewPs.setDouble(1, minSalary);
                previewPs.setDouble(2, maxSalary);
                try (ResultSet previewRs = previewPs.executeQuery()) {
                    List<Integer> affectedIds = new ArrayList<>();
                    List<Double> currentSalaries = new ArrayList<>();
                    List<String> employeeNames = new ArrayList<>();
                    
                    while (previewRs.next()) {
                        affectedIds.add(previewRs.getInt("empid"));
                        currentSalaries.add(previewRs.getDouble("Salary"));
                        String fname = previewRs.getString("Fname");
                        String lname = previewRs.getString("Lname");
                        String name = (fname != null ? fname : "") + " " + (lname != null ? lname : "");
                        employeeNames.add(name.trim());
                    }
                    
                    if (affectedIds.isEmpty()) {
                        System.out.println("No employees found in the specified salary range.");
                        return;
                    }
                    
                    System.out.println("\n--- Preview of Changes ---");
                    System.out.println("Employees to be updated: " + affectedIds.size());
                    System.out.println("\nFirst 10 employees that will be affected:");
                    System.out.println("empid | Name | Current Salary | New Salary");
                    System.out.println("--------------------------------------------");
                    
                    int previewCount = Math.min(10, affectedIds.size());
                    for (int i = 0; i < previewCount; i++) {
                        int empid = affectedIds.get(i);
                        double currentSalary = currentSalaries.get(i);
                        double newSalary = currentSalary * (1 + percentIncrease / 100.0);
                        String name = employeeNames.get(i);
                        
                        System.out.printf("%5d | %-30s | $%,10.2f | $%,10.2f%n", 
                            empid, name, currentSalary, newSalary);
                    }
                    
                    if (affectedIds.size() > 10) {
                        System.out.println("... and " + (affectedIds.size() - 10) + " more employees.");
                    }
                }
            }
            
            // Perform the actual update
            String updateSql = "UPDATE employees SET Salary = Salary * (1 + ? / 100.0) WHERE Salary >= ? AND Salary < ?";
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setDouble(1, percentIncrease);
                updatePs.setDouble(2, minSalary);
                updatePs.setDouble(3, maxSalary);
                
                int rowsUpdated = updatePs.executeUpdate();
                
                System.out.println("\n--- Update Complete ---");
                System.out.println("Successfully updated " + rowsUpdated + " employee record(s).");
                System.out.println("All salaries in the range [$" + String.format("%.2f", minSalary) + 
                                 ", $" + String.format("%.2f", maxSalary) + ") have been increased by " + 
                                 percentIncrease + "%.");
            }
            
        } catch (SQLException e) {
            System.err.println("Bulk salary update failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
