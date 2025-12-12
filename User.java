public class User {
    private final int empid;
    private final String username;
    private final String role;

    public User(int empid, String username, String role) {
        this.empid = empid;
        this.username = username;
        this.role = role;
    }

    public int getEmpid() { return empid; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
