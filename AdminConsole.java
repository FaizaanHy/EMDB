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
                System.out.println("4) Exit");
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
        String sql = "SELECT e.empid, e.Fname, e.Lname, e.email, e.HireDate, e.Salary, e.SSN, u.username FROM employees e LEFT JOIN users u ON e.empid = u.empid ORDER BY e.empid DESC LIMIT 20";
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
        StringBuilder sql = new StringBuilder("SELECT e.empid, e.Fname, e.Lname, e.email, e.HireDate, e.Salary, e.SSN, u.username " +
                                           "FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE 1=1");
        if (!empid.isEmpty()) sql.append(" AND e.empid = ?");
        if (!name.isEmpty()) sql.append(" AND (e.Fname LIKE ? OR e.Lname LIKE ?)");
        if (!dob.isEmpty()) sql.append(" AND e.HireDate = ?");
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
            String select = "SELECT e.empid, e.Fname, e.Lname, e.email, e.HireDate, e.Salary, e.SSN, u.username, u.password_hash FROM employees e LEFT JOIN users u ON e.empid = u.empid WHERE e.empid = ?";
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setInt(1, empid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { System.out.println("Record not found."); return; }
                    String currF = rs.getString("Fname");
                    String currL = rs.getString("Lname");
                    String currEmail = rs.getString("email");
                    String currHire = rs.getString("HireDate");
                    String currSalary = rs.getString("Salary");
                    String currSSN = rs.getString("SSN");
                    String currUser = rs.getString("username");

                    System.out.println("Editing empid=" + empid + " (press Enter to keep current value)");
                    System.out.print("Fname [" + currF + "] : "); String f = in.nextLine().trim(); if (f.isEmpty()) f = currF;
                    System.out.print("Lname [" + currL + "] : "); String l = in.nextLine().trim(); if (l.isEmpty()) l = currL;
                    System.out.print("email [" + currEmail + "] : "); String email = in.nextLine().trim(); if (email.isEmpty()) email = currEmail;
                    System.out.print("HireDate [" + currHire + "] : "); String hire = in.nextLine().trim(); if (hire.isEmpty()) hire = currHire;
                    System.out.print("Salary [" + currSalary + "] : "); String salary = in.nextLine().trim(); if (salary.isEmpty()) salary = currSalary;
                    System.out.print("SSN [" + currSSN + "] : "); String ssn = in.nextLine().trim(); if (ssn.isEmpty()) ssn = currSSN;

                    String update = "UPDATE employees SET Fname=?, Lname=?, email=?, HireDate=?, Salary=?, SSN=? WHERE empid=?";
                    try (PreparedStatement ups = conn.prepareStatement(update)) {
                        ups.setString(1, f);
                        ups.setString(2, l);
                        ups.setString(3, email);
                        ups.setString(4, hire);
                        ups.setObject(5, parseSalary(salary));
                        ups.setString(6, ssn);
                        ups.setInt(7, empid);
                        int u = ups.executeUpdate();
                        System.out.println("Updated rows: " + u);
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
}
