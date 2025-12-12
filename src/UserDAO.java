import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    /**
     * Authenticate and return a User object (with role) on success, or null on failure.
     * Queries the users table (authentication) joined with employees table (business data).
     * Checks hashed password column using PasswordUtil.verify().
     */
    public User authenticateUser(String username, String password) {
        String sql = "SELECT u.user_id, u.empid, u.username, u.password_hash, u.role " +
                     "FROM users u WHERE u.username = ? LIMIT 1";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                int empid = rs.getInt("empid");
                String storedPasswordHash = rs.getString("password_hash");

                // Normalize role: 'admin' -> 'HR', 'employee' -> 'EMPLOYEE'
                int roleId = rs.getInt("role");
                String role;

                switch (roleId) {
                    case 1:
                        role = "HR";
                        break;
                    case 2:
                        role = "MANAGER";
                        break;
                    default:
                        role = "EMPLOYEE";
                }

                // Verify password using PasswordUtil (supports bcrypt, SHA-256, and plaintext)
                if (storedPasswordHash != null && PasswordUtil.verify(password, storedPasswordHash)) {
                    return new User(empid, username, role);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Insert a new user with an automatically hashed password.
     * Returns true on success, false on failure.
     */
    public boolean insertNewUser(int empid, String username, String plainPassword, String role) {
        String hashedPassword = PasswordUtil.hash(plainPassword);
        String sql = "INSERT INTO users (empid, username, password_hash, role, created_at) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empid);
            ps.setString(2, username);
            ps.setString(3, hashedPassword);
            ps.setString(4, role);
            int rowsInserted = ps.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.err.println("Failed to insert user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
