import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class FullTimeEmployee extends Employee {
    private double baseSalary;

    public FullTimeEmployee(int id, String name, double baseSalary, boolean isActive) {
        super(id, name, "FULLTIME", isActive);
        this.baseSalary = baseSalary;
    }

    public double getBaseSalary() { return baseSalary; }

    /**
     * Helper: Hitung upah lembur per jam (1/173 x Gaji Pokok).
     */
    private double getHourlyRate() {
        if (baseSalary <= 0) return 0;
        return baseSalary / 173.0;
    }

    /**
     * Helper: Logika tarif lembur berjenjang (Hari Kerja vs Hari Libur).
     */
    private double calculateOvertimePay(double hours, boolean isHoliday) {
        if (hours <= 0) return 0;
        
        double hourlyRate = getHourlyRate();
        double pay = 0.0;
        
        if (!isHoliday) {
            // Hari Kerja: 1.5x jam pertama, 2x jam berikutnya
            pay += Math.min(hours, 1) * 1.5 * hourlyRate;
            double remainingHours = hours - 1;
            if (remainingHours > 0) {
                pay += remainingHours * 2.0 * hourlyRate;
            }
        } else {
            // Hari Libur: 2x (7 jam pertama), 3x (jam ke-8), 4x (jam ke-9 dst)
            double hoursPhase1 = Math.min(hours, 7);
            pay += hoursPhase1 * 2.0 * hourlyRate;
            
            double hoursPhase2 = Math.min(Math.max(0, hours - 7), 1);
            pay += hoursPhase2 * 3.0 * hourlyRate;
            
            double hoursPhase3 = Math.max(0, hours - 8);
            if (hoursPhase3 > 0) {
                pay += hoursPhase3 * 4.0 * hourlyRate;
            }
        }
        return pay;
    }

    @Override
    public double calculatePay(int year, int month, Connection conn) throws Exception {
        // Query hanya mengambil data lembur
        String sql = "SELECT ot_date, start_time, end_time FROM overtime_entries WHERE employee_id=? AND YEAR(ot_date)=? AND MONTH(ot_date)=?";
        
        Map<LocalDate, Double> totalHoursPerDay = new HashMap<>(); 
        Map<LocalDate, Boolean> holidayStatus = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("ot_date");
                    Time s = rs.getTime("start_time");
                    Time e = rs.getTime("end_time");
                    
                    if (d == null || s == null || e == null) continue;
                    LocalDate date = d.toLocalDate();
                    LocalTime start = s.toLocalTime();
                    LocalTime end = e.toLocalTime();
                    
                    long minutes = Duration.between(start, end).toMinutes();
                    if (minutes <= 0) continue;
                    double hours = minutes / 60.0;
                    
                    totalHoursPerDay.merge(date, hours, Double::sum); 
                    
                    boolean isWeekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
                    holidayStatus.put(date, isWeekend); 
                }
            }
        }
        
        double overtimePay = 0.0;
        
        for (Map.Entry<LocalDate, Double> entry : totalHoursPerDay.entrySet()) {
            LocalDate date = entry.getKey();
            double hours = entry.getValue();
            boolean isHoliday = holidayStatus.getOrDefault(date, false);
            
            overtimePay += calculateOvertimePay(hours, isHoliday); 
        }
        
        return baseSalary + overtimePay;
    }

    @Override
    public SalaryComponents getSalaryComponents(double totalPay, int standardDays, double partTimeRate) {
        return new SalaryComponents(baseSalary, totalPay - baseSalary, standardDays);
    }
}