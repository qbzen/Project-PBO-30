import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PartTimeEmployee extends Employee {

    public PartTimeEmployee(int id, String name, boolean isActive) {
        super(id, name, "PARTTIME", isActive);
    }

    @Override
    public double calculatePay(int year, int month, Connection conn) throws Exception {
        // 1. Ambil Jumlah Hari Kerja
        String sql = "SELECT parttime_days FROM work_records WHERE employee_id=? AND year=? AND month=?";
        int days = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    days = rs.getInt("parttime_days");
                }
            }
        }
        
        // 2. Ambil Rate Harian
        // KITA GANTI: Tidak lagi panggil DB.getPartTimeDailyRate() karena method itu sudah dihapus.
        // Kita query langsung ke app_settings di sini.
        double rate = 100_000.0; // Default fallback
        try (PreparedStatement ps = conn.prepareStatement("SELECT setting_value FROM app_settings WHERE setting_key='PARTTIME_DAILY_RATE'")) {
             try(ResultSet rs = ps.executeQuery()) {
                 if(rs.next()) rate = rs.getDouble(1);
             }
        }
        
        return days * rate;
    }

    @Override
    public SalaryComponents getSalaryComponents(double totalPay, int standardDays, double partTimeRate) {
        // Reverse engineering jumlah hari dari total gaji
        int calculatedDays = (partTimeRate > 0) ? (int) Math.round(totalPay / partTimeRate) : 0;
        
        return new SalaryComponents(totalPay, 0, calculatedDays);
    }
}