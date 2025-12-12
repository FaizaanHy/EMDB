import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HashPasswords {
    // Scans users table and hashes plaintext password_hash values using bcrypt.
    public static void main(String[] args) {
        try (Connection conn = DBConnector.getConnection()) {
            String select = "SELECT user_id, username, password_hash FROM users";
            try (PreparedStatement ps = conn.prepareStatement(select); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String username = rs.getString("username");
                    String hash = rs.getString("password_hash");
                    
                    // Check if password is already hashed (bcrypt starts with $2a$, $2y$, $2b$)
                    if (hash != null && !hash.isEmpty() && !hash.startsWith("$2")) {
                        String h = PasswordUtil.hash(hash);
                        try (PreparedStatement ups = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
                            ups.setString(1, h);
                            ups.setInt(2, userId);
                            int u = ups.executeUpdate();
                            System.out.println("Hashed user_id=" + userId + " username=" + username + " rows=" + u);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
