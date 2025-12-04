import java.sql.Connection;

/**
 * Employee - Kelas abstrak dasar (Superclass) untuk semua tipe karyawan (Fulltime/Parttime).
 * Menyimpan data dasar karyawan dan mendefinisikan kontrak perhitungan gaji.
 */
public abstract class Employee {
    // ID karyawan (protected untuk akses subclass)
    protected int id;
    // Nama karyawan
    protected String name;
    // Tipe pekerjaan (misalnya "FULLTIME", "PARTTIME")
    protected String employmentType;
    // Status keaktifan (true jika aktif, false jika non-aktif)
    protected boolean isActive; 

    /**
     * Konstruktor: Inisialisasi data dasar karyawan.
     */
    public Employee(int id, String name, String employmentType, boolean isActive) {
        this.id = id;
        this.name = name;
        this.employmentType = employmentType;
        this.isActive = isActive;
    }

    // --- Getter Methods ---

    /**
     * Mengembalikan ID karyawan.
     */
    public int getId() { return id; }
    
    /**
     * Mengembalikan nama karyawan.
     */
    public String getName() { return name; }
    
    /**
     * Mengembalikan tipe pekerjaan karyawan.
     */
    public String getEmploymentType() { return employmentType; }
    
    /**
     * Mengembalikan status keaktifan karyawan.
     */
    public boolean isActive() { return isActive; } 

    // --- Setter Methods ---

    /**
     * Mengatur nama karyawan dengan validasi non-kosong.
     * @param name Nama karyawan baru.
     * @throws IllegalArgumentException Jika nama kosong.
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama tidak boleh kosong.");
        }
        this.name = name.trim();
    }
    
    /**
     * Mengatur status keaktifan karyawan.
     * @param active Status keaktifan (true/false)
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Metode abstrak yang harus diimplementasikan oleh subclass untuk menghitung gaji.
     * Kontrak utama untuk Payroll Service.
     * @param year Tahun perhitungan gaji.
     * @param month Bulan perhitungan gaji.
     * @param conn Koneksi database yang digunakan untuk mengambil data kerja/lembur.
     * @return Total gaji untuk periode tersebut.
     * @throws Exception Jika terjadi error dalam perhitungan atau akses data.
     */
    public abstract double calculatePay(int year, int month, Connection conn) throws Exception;
}