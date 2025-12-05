import java.sql.Date;
import java.sql.Time;
import java.util.List;

public interface PayrollRepository {
    
    // --- Metode Data Karyawan ---
    List<Employee> findAll();
    Employee findById(int id); 
    int countInactive(); 
    double getPartTimeDailyRate();
    int countAll();
    int countFullTime();
    int countPartTime();
    
    // --- Metode Lembur ---
    List<OvertimeEntry> findOvertimeEntriesForMonth(int employeeId, int year, int month);
    int insertOvertimeEntry(int employeeId, Date date, Time start, Time end);
}