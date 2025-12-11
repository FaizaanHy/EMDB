import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ReportController reports = new ReportController();

        while (true) {
            System.out.println("\n=====================================");
            System.out.println("     EMPLOYEE REPORTING SYSTEM");
            System.out.println("=====================================");
            System.out.println("1. View Pay History (Employee Only)");
            System.out.println("2. Total Pay by Job Title (Admin Only)");
            System.out.println("3. Total Pay by Division (Admin Only)");
            System.out.println("4. Employees Hired Within Date Range");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine();

            switch (choice) {
                case "1": reports.showPayHistory(); break;
                case "2": reports.showTotalPayByJobTitle(); break;
                case "3": reports.showTotalPayByDivision(); break;
                case "4": reports.showEmployeesHiredBetween(); break;
                case "5":
                    System.out.println("Exiting... Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
                    break;
            }
        }
    }
}
