import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PayrollService - Core Logic (With Snapshot).
 * Menjamin data gaji dan status tidak berubah setelah dibayar (PAID).
 */
public class PayrollService {
    
    private final EmployeeRepository repo = new EmployeeRepository();

    // DTO untuk baris tabel perhitungan (Update: tambah field golongan)
    public static class PayrollRow {
        public final int id;
        public final String name;
        public final String type;
        public final Integer golongan; // [BARU] Menyimpan snapshot golongan
        public final double base;
        public final double overtime;
        public final int daysWorked; 
        public final double total;

        public PayrollRow(int id, String name, String type, Integer golongan, double base, double overtime, int daysWorked, double total) {
            this.id = id; 
            this.name = name; 
            this.type = type; 
            this.golongan = golongan;
            this.base = base; 
            this.overtime = overtime; 
            this.daysWorked = daysWorked;
            this.total = total;
        }
    }

    // Helper class untuk menampung data DB yang sudah ada
    private static class ExistingData {
        String status;
        String snapType;
        Integer snapGol;
        double base, overtime, total;
        
        public ExistingData(String status, String snapType, Integer snapGol, double base, double overtime, double total) {
            this.status = status; 
            this.snapType = snapType; 
            this.snapGol = snapGol;
            this.base = base; 
            this.overtime = overtime; 
            this.total = total;
        }
    }

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
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) days++;
        }
        return days;
    }

    /**
     * Menghitung gaji semua karyawan.
     * Jika status PAID, gunakan data SNAPSHOT (Frozen).
     * Jika status PENDING, hitung ulang (Live).
     */
    public List<PayrollRow> calculateAll(int year, int month) throws Exception {
        List<PayrollRow> rows = new ArrayList<>();
        List<Employee> employees = repo.findAll();
        int standardWorkDays = countBusinessDays(year, month);
        double partTimeRate = repo.getPartTimeDailyRate();

        // 1. Ambil data payroll yang sudah ada di DB untuk cek status & snapshot
        Map<Integer, ExistingData> existingMap = fetchExistingPayrolls(year, month);

        try (Connection conn = DB.getConnection()) {
            for (Employee e : employees) {
                ExistingData exist = existingMap.get(e.getId());
                
                // Jika karyawan tidak aktif DAN tidak punya record gaji bulan ini, skip
                if (!e.isActive() && exist == null) continue;

                if (exist != null && "PAID".equalsIgnoreCase(exist.status)) {
                    // --- KASUS PAID: GUNAKAN DATA BEKU (SNAPSHOT) ---
                    String effectiveType = (exist.snapType != null) ? exist.snapType : e.getEmploymentType();
                    Integer effectiveGol = (exist.snapGol != null) ? exist.snapGol : getGolonganNullable(conn, e.getId());
                    
                    // Display hari kerja (sekadar visual)
                    int displayDays = standardWorkDays;
                    if ("PARTTIME".equalsIgnoreCase(effectiveType)) {
                        displayDays = getPartTimeDays(conn, e.getId(), year, month);
                    }

                    rows.add(new PayrollRow(
                        e.getId(), e.getName(), 
                        effectiveType, effectiveGol, // Pakai Snapshot
                        exist.base, exist.overtime, displayDays, exist.total // Pakai Angka DB
                    ));

                } else {
                    // --- KASUS PENDING: HITUNG LIVE ---
                    if (!e.isActive()) continue; // Skip jika non-aktif dan belum dibayar

                    double total = e.calculatePay(year, month, conn);
                    Employee.SalaryComponents comp = e.getSalaryComponents(total, standardWorkDays, partTimeRate);
                    
                    Integer currentGol = getGolonganNullable(conn, e.getId());

                    rows.add(new PayrollRow(
                        e.getId(), e.getName(), 
                        e.getEmploymentType(), currentGol, // Pakai Data Live
                        comp.base, comp.overtime, comp.days, total
                    ));
                }
            }
        }
        return rows;
    }

    // Mengambil data payroll yang ada di DB
    private Map<Integer, ExistingData> fetchExistingPayrolls(int year, int month) {
        Map<Integer, ExistingData> map = new HashMap<>();
        String sql = "SELECT employee_id, status, snapshot_type, snapshot_golongan, base_salary, overtime_pay, total_salary FROM payrolls WHERE year=? AND month=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, year); ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("employee_id");
                    Integer sg = rs.getInt("snapshot_golongan");
                    if (rs.wasNull()) sg = null;
                    
                    map.put(id, new ExistingData(
                        rs.getString("status"),
                        rs.getString("snapshot_type"),
                        sg,
                        rs.getDouble("base_salary"),
                        rs.getDouble("overtime_pay"),
                        rs.getDouble("total_salary")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    // Helper ambil data live untuk persiapan save
    public List<PayrollRow> buildAndSaveForEmployees(List<Integer> empIds, int year, int month) throws Exception {
        // Logic ini hanya untuk PENDING (sebelum bayar), jadi hitung live
        List<PayrollRow> rows = new ArrayList<>();
        List<Employee> employees = repo.findAll();
        int standardWorkDays = countBusinessDays(year, month);
        double partTimeRate = repo.getPartTimeDailyRate();
        
        try (Connection conn = DB.getConnection()) {
            for (Employee e : employees) {
                if (!empIds.contains(e.getId()) || !e.isActive()) continue;

                double total = e.calculatePay(year, month, conn);
                Employee.SalaryComponents comp = e.getSalaryComponents(total, standardWorkDays, partTimeRate);
                Integer currentGol = getGolonganNullable(conn, e.getId());

                rows.add(new PayrollRow(
                    e.getId(), e.getName(), e.getEmploymentType(), currentGol,
                    comp.base, comp.overtime, comp.days, total
                ));
            }
        }
        if (!rows.isEmpty()) savePendingRowsToDB(rows, year, month);
        return rows;
    }

    private void savePendingRowsToDB(List<PayrollRow> rows, int year, int month) throws Exception {
        String upsertSql = """
            INSERT INTO payrolls(employee_id, year, month, base_salary, overtime_pay, total_salary, status)
            VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
            ON DUPLICATE KEY UPDATE
              base_salary = VALUES(base_salary),
              overtime_pay = VALUES(overtime_pay),
              total_salary = VALUES(total_salary),
              status = 'PENDING', created_at = CURRENT_TIMESTAMP
        """;
        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                for (PayrollRow r : rows) {
                    ps.setInt(1, r.id); ps.setInt(2, year); ps.setInt(3, month);
                    ps.setDouble(4, r.base); ps.setDouble(5, r.overtime); ps.setDouble(6, r.total);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        }
    }

    /**
     * Melakukan pembayaran.
     * UPDATE PENTING: Menyimpan Snapshot Type dan Golongan.
     */
    public int paySelected(List<Integer> empIds, int year, int month, String paidBy, String paymentMethod, String reference) throws Exception {
        if (empIds == null || empIds.isEmpty()) return 0;
        
        // Pastikan perhitungan PENDING sudah masuk DB
        buildAndSaveForEmployees(empIds, year, month);

        String updateSql = "UPDATE payrolls SET status='PAID', snapshot_type=?, snapshot_golongan=? WHERE id=?";
        String logSql = "INSERT INTO payment_logs(payroll_id, paid_by, amount, payment_method, reference) VALUES (?, ?, ?, ?, ?)";
        String selectSql = "SELECT id, total_salary, status FROM payrolls WHERE employee_id=? AND year=? AND month=? FOR UPDATE";

        int count = 0;
        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement sel = conn.prepareStatement(selectSql);
                 PreparedStatement up = conn.prepareStatement(updateSql);
                 PreparedStatement ins = conn.prepareStatement(logSql)) {

                for (Integer eid : empIds) {
                    // Ambil data LIVE karyawan untuk dibekukan
                    Employee curEmp = repo.findById(eid);
                    Integer curGol = getGolonganNullable(conn, eid);

                    sel.setInt(1, eid); sel.setInt(2, year); sel.setInt(3, month);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (!rs.next()) continue;
                        if ("PAID".equalsIgnoreCase(rs.getString("status"))) continue;
                        
                        long pid = rs.getLong("id");
                        double amount = rs.getDouble("total_salary");

                        // Update Status PAID + SIMPAN SNAPSHOT
                        up.setString(1, curEmp.getEmploymentType());
                        if (curGol == null) up.setNull(2, Types.INTEGER); else up.setInt(2, curGol);
                        up.setLong(3, pid);
                        up.executeUpdate();

                        // Insert Log
                        ins.setLong(1, pid); ins.setString(2, paidBy); ins.setDouble(3, amount);
                        ins.setString(4, paymentMethod); ins.setString(5, reference);
                        ins.executeUpdate();
                        
                        count++;
                    }
                }
                conn.commit();
            } catch (Exception e) { conn.rollback(); throw e; }
        }
        return count;
    }

    // --- Helpers ---
    
    private Integer getGolonganNullable(Connection conn, int empId) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT golongan FROM employees WHERE id=?")) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt(1);
                    return rs.wasNull() ? null : v;
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private int getPartTimeDays(Connection conn, int empId, int year, int month) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT parttime_days FROM work_records WHERE employee_id=? AND year=? AND month=?")) {
            ps.setInt(1, empId); ps.setInt(2, year); ps.setInt(3, month);
            try(ResultSet rs = ps.executeQuery()){ if(rs.next()) return rs.getInt(1); }
        } catch(Exception e){}
        return 0;
    }

    // Untuk ReportPanel ambil info log
    public PaymentLogInfo getPaymentLogInfo(int empId, int year, int month) {
        String sql = """
            SELECT pl.paid_by, pl.paid_at FROM payrolls p
            JOIN payment_logs pl ON p.id = pl.payroll_id
            WHERE p.employee_id=? AND p.year=? AND p.month=? AND p.status='PAID'
        """;
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId); ps.setInt(2, year); ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("paid_at");
                    String at = (ts != null) ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "-";
                    return new PaymentLogInfo(rs.getString("paid_by"), at);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
    
    // Method lama tetap ada agar kompatibel jika ada yang panggil, tapi sebaiknya gunakan data dari PayrollRow
    public Integer getGolonganForEmployee(int empId) {
        try (Connection conn = DB.getConnection()) { return getGolonganNullable(conn, empId); } 
        catch (Exception e) { return null; }
    }
    
    public String getPayrollStatus(int empId, int year, int month) {
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT status FROM payrolls WHERE employee_id=? AND year=? AND month=?")) {
            ps.setInt(1, empId); ps.setInt(2, year); ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString(1); }
        } catch (Exception e) {}
        return "PENDING";
    }

    public double getTotalPaidForMonth(int year, int month) {
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_salary),0) FROM payrolls WHERE year=? AND month=? AND status='PAID'")) {
            ps.setInt(1, year); ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) { if(rs.next()) return rs.getDouble(1); }
        } catch(Exception e) {}
        return 0;
    }
}