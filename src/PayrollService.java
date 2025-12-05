import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PayrollService {
    
    private final EmployeeRepository repo = new EmployeeRepository();

    // DTO untuk baris tabel perhitungan
    public static class PayrollRow {
        public final int id;
        public final String name;
        public final String type;
        public final double base;
        public final double overtime;
        public final int daysWorked; 
        public final double total;

        public PayrollRow(int id, String name, String type, double base, double overtime, int daysWorked, double total) {
            this.id = id; 
            this.name = name; 
            this.type = type; 
            this.base = base; 
            this.overtime = overtime; 
            this.daysWorked = daysWorked;
            this.total = total;
        }
    }

    // untuk menampung info log pembayaran (Who & When)
    public static class PaymentLogInfo {
        public final String paidBy;
        public final String paidAt;
        public PaymentLogInfo(String paidBy, String paidAt) {
            this.paidBy = paidBy;
            this.paidAt = paidAt;
        }
    }

    private int countBusinessDays(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        int days = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days++;
            }
        }
        return days;
    }

    /**
     * Menghitung gaji semua karyawan aktif.
     */
    public List<PayrollRow> calculateAll(int year, int month) throws Exception {
        List<PayrollRow> rows = new ArrayList<>();
        List<Employee> employees = repo.findAll();
        int standardWorkDays = countBusinessDays(year, month);
        
        // Ambil rate dari Repository
        double partTimeRate = repo.getPartTimeDailyRate();

        try (Connection conn = DB.getConnection()) {
            for (Employee e : employees) {
                if (!e.isActive()) continue; 

                double total = e.calculatePay(year, month, conn);
                
                // OOP: Polimorfisme untuk memecah komponen gaji
                Employee.SalaryComponents comp = e.getSalaryComponents(total, standardWorkDays, partTimeRate);

                rows.add(new PayrollRow(
                    e.getId(), 
                    e.getName(), 
                    e.getEmploymentType(), 
                    comp.base, 
                    comp.overtime, 
                    comp.days, 
                    total
                ));
            }
        }
        return rows;
    }

    /**
     * Membangun dan menyimpan data payroll (Snapshot) sebelum pembayaran.
     */
    public List<PayrollRow> buildAndSaveForEmployees(List<Integer> empIds, int year, int month) throws Exception {
        List<PayrollRow> rows = new ArrayList<>();
        List<Employee> employees = repo.findAll();
        int standardWorkDays = countBusinessDays(year, month);
        double partTimeRate = repo.getPartTimeDailyRate();
        
        try (Connection conn = DB.getConnection()) {
            for (Employee e : employees) {
                if (!empIds.contains(e.getId())) continue;
                if (!e.isActive()) continue;

                double total = e.calculatePay(year, month, conn);
                Employee.SalaryComponents comp = e.getSalaryComponents(total, standardWorkDays, partTimeRate);

                rows.add(new PayrollRow(
                    e.getId(), e.getName(), e.getEmploymentType(), 
                    comp.base, comp.overtime, comp.days, total
                ));
            }
        }

        if (!rows.isEmpty()) {
            saveRowsToDB(rows, year, month);
        }
        return rows;
    }

    private void saveRowsToDB(List<PayrollRow> rows, int year, int month) throws Exception {
        String upsertSql = """
            INSERT INTO payrolls(employee_id, year, month, base_salary, overtime_pay, total_salary, status)
            VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
            ON DUPLICATE KEY UPDATE
              base_salary = VALUES(base_salary),
              overtime_pay = VALUES(overtime_pay),
              total_salary = VALUES(total_salary),
              status = 'PENDING',
              created_at = CURRENT_TIMESTAMP
        """;

        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                for (PayrollRow r : rows) {
                    ps.setInt(1, r.id);
                    ps.setInt(2, year);
                    ps.setInt(3, month);
                    ps.setDouble(4, r.base);
                    ps.setDouble(5, r.overtime);
                    ps.setDouble(6, r.total);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        }
    }

    /**
     * Melakukan pembayaran (Update status PAID + Insert Log).
     */
    public int paySelected(List<Integer> empIds, int year, int month, String paidBy, String paymentMethod, String reference) throws Exception {
        if (empIds == null || empIds.isEmpty()) return 0;
        
        // Step 1: Hitung ulang dan simpan snapshot terbaru
        buildAndSaveForEmployees(empIds, year, month);

        String selectByEmp = "SELECT id, status, total_salary FROM payrolls WHERE employee_id = ? AND year = ? AND month = ? FOR UPDATE";
        String updatePayroll = "UPDATE payrolls SET status='PAID' WHERE id = ?";
        String insertLog = "INSERT INTO payment_logs(payroll_id, paid_by, amount, payment_method, reference) VALUES (?, ?, ?, ?, ?)";

        int paidCount = 0;
        try (Connection conn = DB.getConnection()) {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement sel = conn.prepareStatement(selectByEmp);
                     PreparedStatement up = conn.prepareStatement(updatePayroll);
                     PreparedStatement ins = conn.prepareStatement(insertLog)) {

                    for (Integer empId : empIds) {
                        sel.setInt(1, empId);
                        sel.setInt(2, year);
                        sel.setInt(3, month);
                        try (ResultSet rs = sel.executeQuery()) {
                            if (!rs.next()) continue;
                            long pid = rs.getLong("id");
                            String status = rs.getString("status");
                            double amt = rs.getDouble("total_salary");

                            if ("PAID".equalsIgnoreCase(status)) continue; 

                            up.setLong(1, pid);
                            up.executeUpdate();
                            
                            ins.setLong(1, pid);
                            ins.setString(2, paidBy);
                            ins.setDouble(3, amt);
                            ins.setString(4, paymentMethod);
                            ins.setString(5, reference);
                            ins.executeUpdate();
                            
                            paidCount++;
                        }
                    }
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return paidCount;
    }

    public String getPayrollStatus(int empId, int year, int month) {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM payrolls WHERE employee_id=? AND year=? AND month=?")) {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "PENDING";
    }

    public Integer getGolonganForEmployee(int empId) {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT golongan FROM employees WHERE id = ?")) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt(1);
                    if (rs.wasNull()) return null;
                    return v;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
    
    public double getTotalPaidForMonth(int year, int month) {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_salary),0) FROM payrolls WHERE year=? AND month=? AND status='PAID'")) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) return rs.getDouble(1);
            }
        } catch(Exception e) { e.printStackTrace(); }
        return 0;
    }

    // --- [BARU] Method untuk mengambil info log pembayaran (Dipanggil oleh ReportPanel) ---
    public PaymentLogInfo getPaymentLogInfo(int empId, int year, int month) {
        String sql = """
            SELECT pl.paid_by, pl.paid_at
            FROM payrolls p
            JOIN payment_logs pl ON p.id = pl.payroll_id
            WHERE p.employee_id = ? AND p.year = ? AND p.month = ? AND p.status = 'PAID'
        """;
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String by = rs.getString("paid_by");
                    Timestamp ts = rs.getTimestamp("paid_at");
                    String at = "-";
                    if (ts != null) {
                        at = ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                    }
                    return new PaymentLogInfo(by, at);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}