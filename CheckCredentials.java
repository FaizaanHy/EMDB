import java.util.Scanner;

public class CheckCredentials {
    public static void main(String[] args) {
        String user = null;
        String pass = null;
        if (args.length >= 2) {
            user = args[0];
            pass = args[1];
        } else {
            Scanner in = new Scanner(System.in);
            System.out.print("Username: ");
            user = in.nextLine().trim();
            System.out.print("Password: ");
            pass = in.nextLine();
        }

        User u = new UserDAO().authenticateUser(user, pass);
        if (u != null) {
            System.out.println("OK: credentials valid for user='" + user + "', role=" + u.getRole());
            System.exit(0);
        } else {
            System.out.println("FAIL: invalid credentials for user='" + user + "'.");
            System.exit(2);
        }
    }
}
