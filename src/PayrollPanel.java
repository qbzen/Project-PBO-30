import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * PayrollPanel - menampilkan total sesuai baris yang tampil (filtered)
 *
 * Reference image (local): sandbox:/mnt/data/79fc5df4-99ea-4347-ba16-57aadbfc275f.png
 */
public class PayrollPanel extends JPanel {
    private JComboBox<Integer> yearBox;
    private JComboBox<Integer> monthBox;

    // filter dropdowns
    private JComboBox<String> typeCombo;       // All / FULLTIME / PARTTIME
    private JComboBox<String> golonganCombo;   // All / 1..4
    private JComboBox<String> overtimeCombo;   // All / Has Overtime / No Overtime

    private DefaultTableModel model;
    private JLabel totalAllLabel;

    private final PayrollService payrollService = new PayrollService();

    public PayrollPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF3F3F3));

        // Top area: back button left, period controls centered
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        topContainer.setBackground(new Color(0xF3F3F3));

        // Back button top-left
        JButton back = new JButton("Kembali");
        back.addActionListener(a -> MainApp.setPanel(new LandingPanel()));
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backWrap.setOpaque(false);
        backWrap.add(back);
        topContainer.add(backWrap, BorderLayout.WEST);

        // Centered period controls
        JPanel centerPeriodWrap = new JPanel();
        centerPeriodWrap.setOpaque(false);
        centerPeriodWrap.setLayout(new BoxLayout(centerPeriodWrap, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Batch Payroll", SwingConstants.CENTER);
        title.setFont(new Font("Impact", Font.BOLD, 34));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPeriodWrap.add(title);

        centerPeriodWrap.add(Box.createRigidArea(new Dimension(0,8)));

        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        periodPanel.setOpaque(false);
        periodPanel.add(new JLabel("Tahun:"));
        yearBox = new JComboBox<>();
        for (int y = 2020; y <= 2035; y++) yearBox.addItem(y);
        yearBox.setSelectedItem(java.time.LocalDate.now().getYear());
        periodPanel.add(yearBox);

        periodPanel.add(new JLabel("Bulan:"));
        monthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) monthBox.addItem(m);
        monthBox.setSelectedItem(java.time.LocalDate.now().getMonthValue());
        periodPanel.add(monthBox);

        JButton loadBtn = new JButton("Muat Data");
        loadBtn.addActionListener(a -> loadPayroll());
        periodPanel.add(loadBtn);

        periodPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPeriodWrap.add(periodPanel);

        topContainer.add(centerPeriodWrap, BorderLayout.CENTER);

        // right area (empty)
        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setOpaque(false);
        topContainer.add(rightWrap, BorderLayout.EAST);

        add(topContainer, BorderLayout.NORTH);

        // Main center: left parameter filters, center table
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);

        // Left filters panel
        JPanel leftFilters = new JPanel();
        leftFilters.setPreferredSize(new Dimension(360, 200));
        leftFilters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10,10,10,10),
                BorderFactory.createLineBorder(Color.LIGHT_GRAY)
        ));
        leftFilters.setLayout(new GridBagLayout());
        leftFilters.setBackground(new Color(0xFFFFFF));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JLabel filterTitle = new JLabel("Filter Parameter");
        filterTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        leftFilters.add(filterTitle, c);
        row++;

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row;
        leftFilters.add(new JLabel("Tipe:"), c);
        typeCombo = new JComboBox<>(new String[]{"All", "FULLTIME", "PARTTIME"});
        c.gridx = 1;
        leftFilters.add(typeCombo, c);
        row++;

        c.gridx = 0; c.gridy = row;
        leftFilters.add(new JLabel("Golongan:"), c);
        golonganCombo = new JComboBox<>(new String[]{"All", "1", "2", "3", "4"});
        c.gridx = 1;
        leftFilters.add(golonganCombo, c);
        row++;

        c.gridx = 0; c.gridy = row;
        leftFilters.add(new JLabel("Punya Lembur:"), c);
        overtimeCombo = new JComboBox<>(new String[]{"All", "Has Overtime", "No Overtime"});
        c.gridx = 1;
        leftFilters.add(overtimeCombo, c);
        row++;

        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(a -> loadPayroll());
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        leftFilters.add(applyBtn, c);
        row++;

        main.add(leftFilters, BorderLayout.WEST);

        // Table center
        model = new DefaultTableModel(new String[]{"ID","Nama","Tipe","Golongan","Base","Overtime","Total","Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);
        JScrollPane sc = new JScrollPane(table);
        main.add(sc, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        // Footer: left (total) and right (pay buttons)
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        footer.setBackground(new Color(0xF6F6F6));

        // Left: total label
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalPanel.setOpaque(false);
        totalAllLabel = new JLabel("Total Semua Gaji: Rp 0");
        totalAllLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalPanel.add(totalAllLabel);
        footer.add(totalPanel, BorderLayout.WEST);

        // Right: pay buttons
        JPanel payPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        payPanel.setOpaque(false);

        JButton payVisibleBtn = new JButton("Bayarkan yang Tampil");
        payVisibleBtn.addActionListener(a -> confirmAndPayVisible());
        payPanel.add(payVisibleBtn);

        JButton payAllBtn = new JButton("Bayarkan Semua");
        payAllBtn.addActionListener(a -> confirmAndPayAll());
        payPanel.add(payAllBtn);

        footer.add(payPanel, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);

        // initial load
        loadPayroll();
    }

    // loads payroll rows and apply filter dropdowns
    private void loadPayroll() {
        model.setRowCount(0);
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();

        String typeFilter = (String) typeCombo.getSelectedItem();
        String golFilter = (String) golonganCombo.getSelectedItem();
        String otFilter = (String) overtimeCombo.getSelectedItem();

        long totalDisplayed = 0L;
        try {
            List<PayrollService.PayrollRow> rows = payrollService.calculateAll(year, month);
            for (PayrollService.PayrollRow r : rows) {
                int gol = fetchGolongan(r.id);
                boolean okType = typeFilter.equals("All") || r.type.equalsIgnoreCase(typeFilter);
                boolean okGol = golFilter.equals("All") || String.valueOf(gol).equals(golFilter);
                boolean okOT;
                if (otFilter.equals("All")) okOT = true;
                else if (otFilter.equals("Has Overtime")) okOT = r.overtime > 0;
                else okOT = r.overtime == 0;

                if (okType && okGol && okOT) {
                    int totalInt = (int) r.total;
                    model.addRow(new Object[]{
                            r.id,
                            r.name,
                            r.type,
                            gol,
                            (int) r.base,
                            (int) r.overtime,
                            totalInt,
                            getPayrollStatus(r.id, year, month)
                    });
                    totalDisplayed += totalInt;
                }
            }
            // **Important change**: set label to total of rows displayed (filtered)
            totalAllLabel.setText("Total Semua Gaji: Rp " + formatLong(totalDisplayed));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error load payroll: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // helper to read golongan from employees table
    private int fetchGolongan(int empId) {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT golongan FROM employees WHERE id = ?")) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            // ignore and return 0
        }
        return 0;
    }

    // helper to get status from payrolls table if exists
    private String getPayrollStatus(int empId, int year, int month) {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM payrolls WHERE employee_id=? AND year=? AND month=?")) {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) { }
        return "PENDING";
    }

    // show confirmation for visible rows, then pay if confirmed
    private void confirmAndPayVisible() {
        int rowCount = model.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Tidak ada karyawan yang tampil untuk dibayarkan.");
            return;
        }

        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();

        // compute total and build detail summary
        long total = 0L;
        StringBuilder details = new StringBuilder();
        details.append("Rincian pembayaran untuk ").append(rowCount).append(" karyawan:\n\n");
        for (int i = 0; i < rowCount; i++) {
            int id = (int) model.getValueAt(i, 0);
            String name = (String) model.getValueAt(i, 1);
            int t = ((Number) model.getValueAt(i, 6)).intValue();
            total += t;
            details.append(id).append(" - ").append(name).append(": Rp ").append(formatLong(t)).append("\n");
            if (i >= 20) { details.append("...\n"); break; }
        }
        details.append("\nTotal: Rp ").append(formatLong((int) total)).append("\n\nLanjutkan pembayaran?");

        int confirm = JOptionPane.showConfirmDialog(this, new JScrollPane(new JTextArea(details.toString())), "Konfirmasi Pembayaran", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // execute payment
            try (Connection conn = DB.getConnection()) {
                for (int i = 0; i < rowCount; i++) {
                    int id = (int) model.getValueAt(i, 0);
                    double base = ((Number) model.getValueAt(i, 4)).doubleValue();
                    double overtime = ((Number) model.getValueAt(i, 5)).doubleValue();
                    double tot = ((Number) model.getValueAt(i, 6)).doubleValue();

                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO payrolls(employee_id,year,month,base_salary,overtime_pay,total_salary,status) " +
                                    "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE base_salary=VALUES(base_salary), overtime_pay=VALUES(overtime_pay), total_salary=VALUES(total_salary), status=VALUES(status)"
                    );
                    ps.setInt(1, id);
                    ps.setInt(2, year);
                    ps.setInt(3, month);
                    ps.setDouble(4, base);
                    ps.setDouble(5, overtime);
                    ps.setDouble(6, tot);
                    ps.setString(7, "PAID");
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error saat membayar: " + e.getMessage());
                e.printStackTrace();
            }
            // reload
            loadPayroll();
            JOptionPane.showMessageDialog(this, "Pembayaran selesai.");
        }
    }

    // show confirmation for ALL employees, then pay all if confirmed
    private void confirmAndPayAll() {
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();

        try {
            List<PayrollService.PayrollRow> rows = payrollService.calculateAll(year, month);
            if (rows.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada karyawan untuk dibayarkan.");
                return;
            }
            long total = 0;
            StringBuilder details = new StringBuilder();
            details.append("Rincian pembayaran untuk SEMUA (").append(rows.size()).append(") karyawan:\n\n");
            for (int i = 0; i < rows.size(); i++) {
                PayrollService.PayrollRow r = rows.get(i);
                total += (long) r.total;
                details.append(r.id).append(" - ").append(r.name).append(": Rp ").append(formatLong((int) r.total)).append("\n");
                if (i >= 20) { details.append("...\n"); break; }
            }
            details.append("\nTotal: Rp ").append(formatLong((int) total)).append("\n\nLanjutkan pembayaran SEMUA?");

            int confirm = JOptionPane.showConfirmDialog(this, new JScrollPane(new JTextArea(details.toString())), "Konfirmasi Pembayaran Semua", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DB.getConnection()) {
                    for (PayrollService.PayrollRow r : rows) {
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO payrolls(employee_id,year,month,base_salary,overtime_pay,total_salary,status) " +
                                        "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE base_salary=VALUES(base_salary), overtime_pay=VALUES(overtime_pay), total_salary=VALUES(total_salary), status=VALUES(status)"
                        );
                        ps.setInt(1, r.id);
                        ps.setInt(2, year);
                        ps.setInt(3, month);
                        ps.setDouble(4, r.base);
                        ps.setDouble(5, r.overtime);
                        ps.setDouble(6, r.total);
                        ps.setString(7, "PAID");
                        ps.executeUpdate();
                    }
                }
                loadPayroll();
                JOptionPane.showMessageDialog(this, "Semua karyawan telah dibayarkan.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error membayar semua: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // small helper to pretty-print integer (dot for thousand separators)
    private String formatLong(long v) {
        String s = Long.toString(v);
        StringBuilder out = new StringBuilder();
        int cnt = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            out.append(s.charAt(i));
            cnt++;
            if (cnt == 3 && i != 0) {
                out.append('.');
                cnt = 0;
            }
        }
        return out.reverse().toString();
    }
}
