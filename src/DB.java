import java.sql.*;

public class DB {
    private static final String URL = "jdbc:mysql://localhost:3306/payroll_db?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void init() {
        try {
            try (Connection connRoot = DriverManager.getConnection("jdbc:mysql://localhost:3306/?serverTimezone=UTC", "root", "")) {
                try (Statement st = connRoot.createStatement()) {
                    st.executeUpdate("CREATE DATABASE IF NOT EXISTS payroll_db");
                }
            }

            try (Connection conn = DB.getConnection(); Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users(
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      username VARCHAR(50) UNIQUE NOT NULL,
                      password VARCHAR(50) NOT NULL
                    )
                """);

                insertUserIfMissing(conn, "iqbal", "123");
                insertUserIfMissing(conn, "nufa", "123");
                insertUserIfMissing(conn, "cindy", "123");
                insertUserIfMissing(conn, "nana", "123");

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS employees(
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(100) NOT NULL,
                      golongan INT DEFAULT 0 CHECK (golongan BETWEEN 0 AND 4),
                      employment_type VARCHAR(20) DEFAULT 'FULLTIME' CHECK (employment_type IN ('FULLTIME', 'PARTTIME')),
                      base_salary DOUBLE DEFAULT 0 CHECK (base_salary >= 0),
                      overtime_rate DOUBLE DEFAULT 0 CHECK (overtime_rate >= 0)
                    )
                """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS work_records(
                      employee_id INT NOT NULL,
                      year INT NOT NULL CHECK (year >= 2020 AND year <= 2100),
                      month INT NOT NULL CHECK (month >= 1 AND month <= 12),
                      weekday_overtime_hours DOUBLE DEFAULT 0 CHECK (weekday_overtime_hours >= 0),
                      holiday_overtime_hours DOUBLE DEFAULT 0 CHECK (holiday_overtime_hours >= 0),
                      parttime_days INT DEFAULT 0 CHECK (parttime_days >= 0),
                      PRIMARY KEY (employee_id, year, month),
                      FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
                    )
                """);

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS payrolls(
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      employee_id INT NOT NULL,
                      year INT NOT NULL CHECK (year >= 2020 AND year <= 2100),
                      month INT NOT NULL CHECK (month >= 1 AND month <= 12),
                      base_salary DOUBLE NOT NULL CHECK (base_salary >= 0),
                      overtime_pay DOUBLE NOT NULL CHECK (overtime_pay >= 0),
                      total_salary DOUBLE NOT NULL CHECK (total_salary >= 0),
                      status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID')),
                      UNIQUE KEY uniq_pay(employee_id, year, month),
                      FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                      INDEX idx_year_month (year, month),
                      INDEX idx_status (status)
                    )
                """);
            }

            System.out.println("Database initialized.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertUserIfMissing(Connection conn, String username, String pass) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO users(username,password) VALUES (?,?)")) {
                        ins.setString(1, username);
                        ins.setString(2, pass);
                        ins.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
        }
    }
}
