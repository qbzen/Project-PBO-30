import java.sql.*;

public class DB {
    private static final String URL = "jdbc:mysql://localhost:3306/payroll_db?useLegacyDatetimeCode=false&serverTimezone=Asia/Makassar";
    private static final String USER = "root";
    private static final String PASS = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}