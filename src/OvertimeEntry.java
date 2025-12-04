import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * OvertimeEntry - Kelas model data untuk menyimpan satu catatan entri lembur.
 * Digunakan untuk menghitung durasi jam lembur dan menentukan apakah lembur terjadi pada Hari Kerja atau Akhir Pekan.
 */
public class OvertimeEntry {
    // ID entri lembur (primary key)
    public final int id;
    // ID karyawan yang melakukan lembur
    public final int employeeId;
    // Tanggal lembur
    public final Date date;
    // Waktu mulai lembur (java.sql.Time)
    public final Time start;
    // Waktu selesai lembur (java.sql.Time)
    public final Time end;
    
    // Status apakah tanggal ini ditandai sebagai hari libur (dari DB, meskipun tidak digunakan untuk tarif)
    public final boolean isHoliday; 

    /**
     * Konstruktor untuk inisialisasi semua properti OvertimeEntry.
     */
    public OvertimeEntry(int id, int employeeId, Date date, Time start, Time end, boolean isHoliday) {
        this.id = id;
        this.employeeId = employeeId;
        this.date = date;
        this.start = start;
        this.end = end;
        this.isHoliday = isHoliday;
    }

    /**
     * Menghitung total durasi lembur dalam jam (double).
     * @return Durasi lembur dalam jam, atau 0.0 jika input waktu tidak valid.
     */
    public double getHours() {
        // Cek jika waktu mulai atau selesai kosong
        if (start == null || end == null) return 0.0;
        
        // Konversi java.sql.Time ke LocalTime untuk perhitungan durasi
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        
        // Memastikan waktu selesai harus setelah waktu mulai
        if (!endTime.isAfter(startTime)) return 0.0;
        
        // Hitung total durasi dalam menit
        long minutes = Duration.between(startTime, endTime).toMinutes();
        // Konversi menit ke jam (desimal)
        return minutes / 60.0;
    }

    /**
     * Memeriksa apakah tanggal lembur jatuh pada hari kerja (Senin-Jumat).
     * @return true jika hari kerja, false jika Sabtu atau Minggu.
     */
    public boolean isWeekday() {
        if (date == null) return false;
        // Konversi java.sql.Date ke LocalDate untuk mendapatkan DayOfWeek
        LocalDate localDate = date.toLocalDate();
        DayOfWeek day = localDate.getDayOfWeek();
        // Cek jika bukan Sabtu atau Minggu
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
    
    /**
     * Memeriksa apakah tanggal lembur jatuh pada akhir pekan (Sabtu-Minggu).
     * @return true jika Sabtu atau Minggu, false jika hari kerja.
     */
    public boolean isWeekend() {
        if (date == null) return false;
        // Konversi java.sql.Date ke LocalDate untuk mendapatkan DayOfWeek
        LocalDate localDate = date.toLocalDate();
        DayOfWeek day = localDate.getDayOfWeek();
        // Cek jika Sabtu atau Minggu
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}