import java.sql.Date;
import java.util.List;

public class ReportService {

    private final ReportRepository repo = new ReportRepository();

    public List<String> getPayHistory(int empId) {
        return repo.getPayHistory(empId);
    }

    public List<String> getTotalPayByJobTitle(int year, int month) {
        return repo.getTotalPayByJobTitle(year, month);
    }

    public List<String> getTotalPayByDivision(int year, int month) {
        return repo.getTotalPayByDivision(year, month);
    }

    public List<String> getEmployeesHiredBetween(Date start, Date end) {
        return repo.getEmployeesHiredBetween(start, end);
    }
}
