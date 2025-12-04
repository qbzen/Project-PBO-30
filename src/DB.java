import java.sql.*;

/**
 * DB - Kelas utilitas untuk mengelola koneksi database MySQL.
 * Menyediakan konfigurasi koneksi statis dan metode untuk mendapatkan koneksi.
 */
public class DB {
    // URL koneksi ke database MySQL (dengan timezone Asia/Makassar)
    private static final String URL = "jdbc:mysql://localhost:3306/payroll_db?useLegacyDatetimeCode=false&serverTimezone=Asia/Makassar";
    // Nama pengguna database
    private static final String USER = "root";
    // Kata sandi database
    private static final String PASS = "";

    /**
     * Blok statis untuk memuat driver JDBC MySQL saat kelas dimuat.
     */
    static {
        try {
            // Memuat driver JDBC MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            // Mencetak stack trace jika driver gagal dimuat
            e.printStackTrace();
        }
    }

    /**
     * Mendapatkan objek Connection baru ke database.
     * @return Objek Connection yang sudah terbuka.
     * @throws SQLException Jika terjadi kesalahan koneksi database.
     */
    public static Connection getConnection() throws SQLException {
        // Mengembalikan koneksi menggunakan URL, USER, dan PASS yang telah ditentukan
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * Mengambil tarif harian untuk karyawan Part-Time dari tabel app_settings.
     * @return Tarif harian Part-Time (double). Default 100_000.0 jika gagal.
     */
    public static double getPartTimeDailyRate() {
        // Nilai default tarif harian
        double rate = 100_000.0;
        
        // Membuka koneksi dan menjalankan query untuk mengambil PARTTIME_DAILY_RATE
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT setting_value FROM app_settings WHERE setting_key = 'PARTTIME_DAILY_RATE'")) {
            
            // Mengambil nilai tarif jika ditemukan dalam ResultSet
            if (rs.next()) rate = rs.getDouble("setting_value");
        } catch (Exception e) {
            // Mencetak stack trace jika terjadi kesalahan I/O atau SQL
            e.printStackTrace();
        }
        
        // Mengembalikan tarif yang ditemukan atau nilai default
        return rate;
    }
}