import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static Connection conn;

    public static Connection getConnection() {
        if (conn == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/employeeData2?useSSL=false&serverTimezone=UTC",
                        "root",
                        "root"
                );
                System.out.println("Database connection established!");
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found.");
                e.printStackTrace();
            } catch (SQLException e) {
                System.err.println("Failed to connect to the database.");
                e.printStackTrace();
            }
        }
        return conn;
    }
}
