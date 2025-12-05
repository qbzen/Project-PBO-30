import java.sql.Connection;

/**
 * Employee - Kelas abstrak dasar (Superclass).
 * Updated: Menggunakan Enum untuk Tipe dan Polimorfisme untuk komponen gaji.
 */
public abstract class Employee {
    
    // 1. Nested Enum untuk Tipe Karyawan (Clean Code: Type Safety)
    public enum Type {
        FULLTIME,
        PARTTIME
    }

    protected int id;
    protected String name;
    
    // Menggunakan Enum, bukan String mentah
    protected Type type; 
    
    protected boolean isActive; 

    /**
     * Konstruktor: Inisialisasi data dasar karyawan.
     * Menerima String untuk tipe agar kompatibel dengan data dari Database/Repository,
     * tapi menyimpannya sebagai Enum.
     */
    public Employee(int id, String name, String typeStr, boolean isActive) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
        
        // Konversi String dari DB ke Enum (Safety Check)
        try {
            if (typeStr != null) {
                this.type = Type.valueOf(typeStr.toUpperCase().trim());
            } else {
                this.type = Type.FULLTIME; // Default fallback
            }
        } catch (IllegalArgumentException e) {
            // Default fallback jika data di DB tidak dikenali
            this.type = Type.FULLTIME; 
        }
    }

    // --- Getter Methods ---

    public int getId() { return id; }
    
    public String getName() { return name; }
    
    /**
     * Mengembalikan nama tipe sebagai String agar kompatibel dengan kode lama 
     * (misal: untuk ditampilkan di JTable).
     */
    public String getEmploymentType() { return type.name(); }
    
    public boolean isActive() { return isActive; } 

    // --- Setter Methods ---

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama tidak boleh kosong.");
        }
        this.name = name.trim();
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Metode abstrak untuk menghitung total gaji.
     */
    public abstract double calculatePay(int year, int month, Connection conn) throws Exception;

    /**
     * [BARU] Metode abstrak untuk memecah komponen gaji (Polimorfisme).
     * Setiap subclass (Fulltime/Parttime) wajib menjelaskan bagaimana gajinya dipecah 
     * (Gaji Pokok vs Lembur vs Hari Kerja).
     * * Ini menggantikan logika "if (instanceof ...)" di PayrollService.
     */
    public abstract SalaryComponents getSalaryComponents(double totalPay, int standardDays, double partTimeRate);

    /**
     * [BARU] Helper class (DTO) untuk menampung rincian komponen gaji.
     * Static nested class agar tidak bergantung pada instance Employee tertentu.
     */
    public static class SalaryComponents {
        public double base;     // Gaji Pokok (atau Total Gaji untuk Parttime)
        public double overtime; // Gaji Lembur
        public int days;        // Hari Kerja (Real atau Standar)

        public SalaryComponents(double base, double overtime, int days) {
            this.base = base;
            this.overtime = overtime;
            this.days = days;
        }
    }
}