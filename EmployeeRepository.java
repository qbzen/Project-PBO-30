import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EmployeeRepository - Implementasi konkrit dari PayrollRepository.
 * Bertanggung jawab atas semua operasi database terkait karyawan.
 */
public class EmployeeRepository implements PayrollRepository {

    // --- [BARU] Logika Data Access yang dipindah dari DB.java (Clean Code: SRP) ---
    /**
     * Mengambil tarif harian untuk karyawan Part-Time dari tabel app_settings.
     * @return Tarif harian Part-Time (double). Default 100_000.0 jika gagal.
     */
    public double getPartTimeDailyRate() {
        double rate = 100_000.0;
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = 'PARTTIME_DAILY_RATE'";
        
        try (Connection conn = DB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            if (rs.next()) rate = rs.getDouble("setting_value");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rate;
    }

    // --- IMPLEMENTASI METODE DARI PAYROLLREPOSITORY ---

    @Override
    public List<Employee> findAll() {
        List<Employee> out = new ArrayList<>();
        String sql = """
            SELECT e.id, e.name, e.employment_type, e.golongan, e.is_active,
                   COALESCE(s.base_salary,0) AS base_salary 
            FROM employees e
            LEFT JOIN salary_scale s ON e.golongan = s.golongan
            ORDER BY e.is_active DESC, e.name ASC
        """;
        
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name"); 
                String type = rs.getString("employment_type"); 
                boolean active = rs.getBoolean("is_active"); 
                
                // Menggunakan string "PARTTIME" yang akan dikonversi ke Enum di dalam Constructor
                if ("PARTTIME".equalsIgnoreCase(type)) {
                    out.add(new PartTimeEmployee(id, name, active));
                } else {
                    double base = rs.getDouble("base_salary");
                    out.add(new FullTimeEmployee(id, name, base, active));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    @Override
    public Employee findById(int id) {
        String sql = """
            SELECT e.id, e.name, e.employment_type, e.golongan, e.is_active,
                   COALESCE(s.base_salary,0) AS base_salary
            FROM employees e
            LEFT JOIN salary_scale s ON e.golongan = s.golongan
            WHERE e.id = ?
        """;
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String name = rs.getString("name");
                String type = rs.getString("employment_type");
                boolean active = rs.getBoolean("is_active");

                if ("PARTTIME".equalsIgnoreCase(type)) return new PartTimeEmployee(id, name, active);
                return new FullTimeEmployee(id, name, rs.getDouble("base_salary"), active);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int countAll() {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int countFullTime() {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE employment_type='FULLTIME' AND is_active=1"); 
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int countPartTime() {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE employment_type='PARTTIME' AND is_active=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int countInactive() {
    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE is_active=0");
         ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt("c");
    } catch (Exception e) {
        e.printStackTrace();
    }
    return 0;
}

    @Override
    public List<OvertimeEntry> findOvertimeEntriesForMonth(int empId, int year, int month) {
        List<OvertimeEntry> out = new ArrayList<>();
        String sql = "SELECT id, employee_id, ot_date, start_time, end_time FROM overtime_entries WHERE employee_id=? AND YEAR(ot_date)=? AND MONTH(ot_date)=? ORDER BY ot_date, start_time";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int eId = rs.getInt("employee_id");
                    Date d = rs.getDate("ot_date");
                    Time s = rs.getTime("start_time");
                    Time e = rs.getTime("end_time");
                    
                    out.add(new OvertimeEntry(id, eId, d, s, e, false)); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    @Override
    public int insertOvertimeEntry(int employeeId, Date otDate, Time startTime, Time endTime) {
        String sql = "INSERT INTO overtime_entries (employee_id, ot_date, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, employeeId);
            ps.setDate(2, otDate);
            ps.setTime(3, startTime);
            ps.setTime(4, endTime);

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // --- METODE TAMBAHAN KHUSUS IMPLEMENTASI MYSQL ---

    public int getNextEmployeeId() throws Exception {
        int maxId = 0;
        String sql = "SELECT MAX(id) FROM employees";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                maxId = rs.getInt(1);
            }
        }
        return maxId > 0 ? maxId + 1 : 1; 
    }
    
    public int insertEmployee(int newId, String name, String type, Integer golongan) throws Exception {
        String sql = "INSERT INTO employees(id, name, golongan, employment_type, is_active) VALUES (?,?,?,?,1)";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newId);
            ps.setString(2, name);
            if (golongan == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, golongan);
            }
            ps.setString(4, type);
            ps.executeUpdate();
            
            return newId;
        }
    }

    public void updateStatus(int id, boolean isActive) throws Exception {
        Employee e = findById(id); 
        if (e == null) throw new IllegalArgumentException("Karyawan tidak ditemukan.");
        
        e.setActive(isActive); 

        String sql = "UPDATE employees SET is_active = ? WHERE id = ?";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, e.isActive() ? 1 : 0); 
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public boolean hasFinancialHistory(int id) throws Exception {
        String sql = """
            (SELECT 1 FROM payrolls WHERE employee_id=? LIMIT 1)
            UNION ALL
            (SELECT 1 FROM work_records WHERE employee_id=? LIMIT 1)
            UNION ALL
            (SELECT 1 FROM overtime_entries WHERE employee_id=? LIMIT 1)
        """;
        try (Connection conn = DB.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ps.setInt(3, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void deletePermanently(int id) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement p0 = conn.prepareStatement("DELETE FROM overtime_entries WHERE employee_id=?");
             PreparedStatement p1 = conn.prepareStatement("DELETE FROM work_records WHERE employee_id=?");
             PreparedStatement p2 = conn.prepareStatement("DELETE FROM payment_logs WHERE payroll_id IN (SELECT id FROM payrolls WHERE employee_id=?)");
             PreparedStatement p3 = conn.prepareStatement("DELETE FROM payrolls WHERE employee_id=?");
             PreparedStatement p4 = conn.prepareStatement("DELETE FROM employees WHERE id=?")) {

            p0.setInt(1, id); p0.executeUpdate();
            p1.setInt(1, id); p1.executeUpdate();
            p2.setInt(1, id); p2.executeUpdate();
            p3.setInt(1, id); p3.executeUpdate();
            p4.setInt(1, id); p4.executeUpdate();
        }
    }

    public void ensureWorkRecord(int empId, int year, int month) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement check = conn.prepareStatement(
                  "SELECT 1 FROM work_records WHERE employee_id=? AND year=? AND month=?")) {
            check.setInt(1, empId);
            check.setInt(2, year);
            check.setInt(3, month);
            try (ResultSet r = check.executeQuery()) {
                if (!r.next()) {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO work_records(employee_id,year,month,parttime_days) VALUES (?,?,?,?)" 
                    )) {
                        ins.setInt(1, empId);
                        ins.setInt(2, year);
                        ins.setInt(3, month);
                        ins.setInt(4, 0); 
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    public void updateParttimeDays(int empId, int year, int month, int days) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement upd = conn.prepareStatement(
                  "UPDATE work_records SET parttime_days = ? WHERE employee_id=? AND year=? AND month=?")) {
            upd.setInt(1, days);
            upd.setInt(2, empId);
            upd.setInt(3, year);
            upd.setInt(4, month);
            upd.executeUpdate();
        }
    }

    public void deleteOvertimeEntry(int overtimeId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM overtime_entries WHERE id = ?")) {
            ps.setInt(1, overtimeId);
            ps.executeUpdate();
        }
    }
}