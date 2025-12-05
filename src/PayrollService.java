import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class PayrollService {
    private final EmployeeRepository repo = new EmployeeRepository();

    public static class PayrollRow {
        public final int id;
        public final String name;
        public final String type;
        public final double base;
        public final double overtime;
        public final double total;

        public PayrollRow(int id, String name, String type, double base, double overtime, double total) {
            this.id = id; this.name = name; this.type = type; this.base = base; this.overtime = overtime; this.total = total;
        }
    }

    /**
     * Calculate payroll for all employees for given year/month.
     */
    public List<PayrollRow> calculateAll(int year, int month) throws Exception {
        List<PayrollRow> rows = new ArrayList<>();
        // repository.findAll returns ready Employee objects but repo methods open their own connections.
        // We'll open a single connection here and use model.calculatePay(conn)
        try (Connection conn = DB.getConnection()) {
            List<Employee> employees = repo.findAll();
            for (Employee e : employees) {
                double total = e.calculatePay(year, month, conn);
                double base = 0;
                double overtime = 0;
                if (e instanceof FullTimeEmployee) {
                    base = ((FullTimeEmployee) e).getBaseSalary();
                    overtime = total - base;
                } else {
                    base = 0;
                    overtime = 0;
                }
                rows.add(new PayrollRow(e.getId(), e.getName(), e.getEmploymentType(), base, overtime, total));
            }
        }
        return rows;
    }
}
