import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Asumsi OvertimeEntry berada di package yang sama atau diimpor secara eksplisit
// Jika OvertimeEntry berada di package yang berbeda, tambahkan: import com.yourpackage.OvertimeEntry;

public class EmployeeRepository {

    // KELAS INNER public static class OvertimeEntry LAMA DIHAPUS DI SINI.

    /**
     * Mengambil SEMUA karyawan (Aktif & Non-Aktif)
     */
    public List<Employee> findAll() throws Exception {
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
                
                if ("PARTTIME".equalsIgnoreCase(type)) {
                    out.add(new PartTimeEmployee(id, name, active));
                } else {
                    double base = rs.getDouble("base_salary");
                    // Constructor FullTimeEmployee sekarang hanya menerima 4 parameter data
                    out.add(new FullTimeEmployee(id, name, base, active));
                }
            }
        }
        return out;
    }

    public Employee findById(int id) throws Exception {
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
                // Constructor FullTimeEmployee sekarang hanya menerima 4 parameter data
                return new FullTimeEmployee(id, name, rs.getDouble("base_salary"), active);
            }
        }
    }

    /**
     * BARU: Mendapatkan ID Karyawan terbesar saat ini + 1.
     */
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
        // Jika maxId 0 (tabel kosong), mulai dari 1. Jika ada, tambahkan 1.
        return maxId > 0 ? maxId + 1 : 1; 
    }
    
    /**
     * MODIFIKASI: Insert employee baru (Default Status: AKTIF) dengan ID yang ditentukan (Manual ID).
     */
    public int insertEmployee(int newId, String name, String type, Integer golongan) throws Exception {
        // SQL diubah untuk menyertakan kolom ID dan tidak menggunakan RETURN_GENERATED_KEYS
        String sql = "INSERT INTO employees(id, name, golongan, employment_type, is_active) VALUES (?,?,?,?,1)";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newId); // Set the manual ID
            ps.setString(2, name);
            if (golongan == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, golongan);
            }
            ps.setString(4, type);
            ps.executeUpdate();
            
            // Mengembalikan ID yang berhasil dimasukkan
            return newId;
        }
    }

    /**
     * Mengubah status karyawan (Aktif <-> Non-Aktif).
     * Digunakan untuk Soft Delete.
     */
    public void updateStatus(int id, boolean isActive) throws Exception {
    // 1. Dapatkan objek Model
    Employee e = findById(id); 
    if (e == null) throw new IllegalArgumentException("Karyawan tidak ditemukan.");
    
    // 2. Ubah status menggunakan Setter (Validasi sudah ada di Model)
    e.setActive(isActive); 

    // 3. Simpan perubahan ke DB
    String sql = "UPDATE employees SET is_active = ? WHERE id = ?";
    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        // Gunakan status yang sudah divalidasi dari objek Model
        ps.setInt(1, e.isActive() ? 1 : 0); 
        ps.setInt(2, id);
        ps.executeUpdate();
    }
}

    /**
     * Cek apakah karyawan memiliki riwayat keuangan (Payroll, Work Record, Overtime).
     * Digunakan untuk menentukan apakah boleh dihapus permanen atau harus soft delete.
     */
    public boolean hasFinancialHistory(int id) throws Exception {
    // Tambahkan tanda kurung di setiap SELECT
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
                return rs.next(); // True jika ada data
            }
        }
    }

    /**
     * Hapus Permanen (Hard Delete).
     * Hanya digunakan jika karyawan TIDAK memiliki riwayat keuangan (misal salah input baru).
     */
    public void deletePermanently(int id) throws Exception {
        // Hapus manual secara berurutan untuk keamanan constraint
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

    // ---- Work Record & Overtime Helpers ----

    /**
     * Memastikan ada work_records untuk bulan/tahun ini (hanya untuk parttime days/inisiasi)
     * Dihapus: kolom weekday_overtime_hours dan holiday_overtime_hours.
     */
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
                            // UPDATE: Hapus weekday_overtime_hours dan holiday_overtime_hours
                            "INSERT INTO work_records(employee_id,year,month,parttime_days) VALUES (?,?,?,?)" 
                    )) {
                        ins.setInt(1, empId);
                        ins.setInt(2, year);
                        ins.setInt(3, month);
                        // ins.setDouble(4, 0); // Dihapus
                        // ins.setDouble(5, 0); // Dihapus
                        ins.setInt(4, 0); // parttime_days
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    // UPDATE: Metode updateWeekdayOT dan updateHolidayOT DIHAPUS karena redundan
    // dan diganti dengan perhitungan agregasi di OvertimePanel.java.

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

    // ---- Granular Overtime Entries ----

    public int insertOvertimeEntry(int employeeId, java.sql.Date otDate, java.sql.Time startTime, java.sql.Time endTime) throws SQLException {
    String sql = "INSERT INTO overtime_entries (employee_id, ot_date, start_time, end_time) VALUES (?, ?, ?, ?)";
    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setInt(1, employeeId);
        ps.setDate(2, otDate);
        ps.setTime(3, startTime);
        ps.setTime(4, endTime);

        int affected = ps.executeUpdate();
        if (affected == 0) {
            return -1;
        }

        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        return -1;
    }
}


   public List<OvertimeEntry> findOvertimeEntriesForMonth(int empId, int year, int month) throws Exception {
    // PERBAIKAN: Menghapus 'is_holiday' dari klausa SELECT.
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
                    // boolean hol = rs.getBoolean("is_holiday"); // DIHAPUS

                    // INSTANSIASI MENGGUNAKAN KELAS OVERTIMEENTRY BARU
                    // MENGIRIMKAN NILAI FALSE DEFAULT UNTUK isHoliday (Parameter terakhir)
                    out.add(new OvertimeEntry(id, eId, d, s, e, false)); 
                }
            }
        }
        return out;
    }

    public void deleteOvertimeEntry(int overtimeId) throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM overtime_entries WHERE id = ?")) {
            ps.setInt(1, overtimeId);
            ps.executeUpdate();
        }
    }

    // ---- Dashboard / Stats Helpers ----
    // PENTING: Hanya menghitung karyawan yang AKTIF (is_active = 1)

    public int countAll() throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        }
        return 0;
    }

    public int countFullTime() throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE employment_type='FULLTIME' AND is_active=1"); 
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        }
        return 0;
    }

    public int countPartTime() throws Exception {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM employees WHERE employment_type='PARTTIME' AND is_active=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("c");
        }
        return 0;
    }
}