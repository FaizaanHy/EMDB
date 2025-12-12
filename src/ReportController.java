import java.sql.Date;
import java.util.List;
import java.util.Scanner;

public class ReportController {
    private final ReportService service = new ReportService();
    private final Scanner sc = new Scanner(System.in);

    public void showPayHistory() {
        System.out.print("Enter employee ID: ");
        int id = Integer.parseInt(sc.nextLine());
        List<String> results = service.getPayHistory(id);
        results.forEach(System.out::println);
    }

    public void showTotalPayByJobTitle() {
        System.out.print("Year: ");
        int year = Integer.parseInt(sc.nextLine());
        System.out.print("Month (1–12): ");
        int month = Integer.parseInt(sc.nextLine());
        List<String> results = service.getTotalPayByJobTitle(year, month);
        results.forEach(System.out::println);
    }

    public void showTotalPayByDivision() {
        System.out.print("Year: ");
        int year = Integer.parseInt(sc.nextLine());
        System.out.print("Month (1–12): ");
        int month = Integer.parseInt(sc.nextLine());
        List<String> results = service.getTotalPayByDivision(year, month);
        results.forEach(System.out::println);
    }

    public void showEmployeesHiredBetween() {
        System.out.print("Start date (YYYY-MM-DD): ");
        Date start = Date.valueOf(sc.nextLine());
        System.out.print("End date (YYYY-MM-DD): ");
        Date end = Date.valueOf(sc.nextLine());
        List<String> results = service.getEmployeesHiredBetween(start, end);
        results.forEach(System.out::println);
    }
}