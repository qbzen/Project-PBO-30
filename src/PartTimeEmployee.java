import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PartTimeEmployee extends Employee {
    private static final double DAILY = 100_000.0;

    public PartTimeEmployee(int id, String name) {
        super(id, name, "PARTTIME");
    }

    @Override
    public double calculatePay(int year, int month, Connection conn) throws Exception {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT parttime_days FROM work_records WHERE employee_id=? AND year=? AND month=?"
        );
        ps.setInt(1, id);
        ps.setInt(2, year);
        ps.setInt(3, month);
        ResultSet rs = ps.executeQuery();
        int days = 0;
        if (rs.next()) days = rs.getInt("parttime_days");
        return days * DAILY;
    }
}