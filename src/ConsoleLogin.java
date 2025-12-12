import java.util.Scanner;

public class ConsoleLogin {
    private final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        new ConsoleLogin().run();
    }

    public void run() {
        System.out.println("Console Login (type 'exit' to quit)");
        while (true) {
            System.out.print("Username: ");
            String user = in.nextLine().trim();
            if (user.equalsIgnoreCase("exit")) break;
            System.out.print("Password: ");
            String pass = in.nextLine();
            if (pass.equalsIgnoreCase("exit")) break;

                User u = new UserDAO().authenticateUser(user, pass);
                if (u == null) {
                    System.out.println("Invalid username or password. Try again.");
                    continue;
                }

            System.out.println("Login successful.");
                System.out.println("Role=" + u.getRole());
                if ("HR".equalsIgnoreCase(u.getRole())) {
                    AdminConsole.runConsole();
                    break;
                } else {
                    // employee: view-only
                    EmployeeConsole.runFor(u.getEmpid());
                    break;
                }
        }
        System.out.println("Goodbye.");
    }
}
