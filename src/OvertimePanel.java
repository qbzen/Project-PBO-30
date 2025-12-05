import java.awt.*;
import java.sql.*;
import java.time.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * OvertimePanel - Panel Input Lembur & Hari Kerja.
 * - Menggunakan Repository untuk mengambil data karyawan & lembur.
 */
public class OvertimePanel extends JPanel {

    // --- COMPONENTS ---
    private JComboBox<Integer> inputYearBox;
    private JComboBox<Integer> inputMonthBox;
    private JComboBox<Integer> inputDayBox;
    private JLabel inputDayLbl;

    private JComboBox<Integer> filterYearBox;
    private JComboBox<Integer> filterMonthBox;
    private JComboBox<String> filterDayBox; 

    private JTextField nameField;
    private JButton checkBtn;

    // Fulltime Controls
    private JLabel startLbl;
    private JTextField startTimeField;
    private JLabel endLbl;
    private JTextField endTimeField;

    // Parttime Controls
    private JLabel daysLbl;
    private JTextField parttimeDaysField;

    private JButton saveBtn;

    private DefaultTableModel tableModel;
    private JTable table;

    // State Variables
    private Integer selectedEmployeeId = null;
    private String selectedEmployeeName = null;
    private String selectedEmployeeType = null;

    private final PayrollRepository repo = new EmployeeRepository();
    private static final int PREFERRED_CARD_HEIGHT = 720;
    private ParttimeDaysVerifier parttimeVerifier = new ParttimeDaysVerifier(31); 

    public OvertimePanel() {
        setLayout(new BorderLayout());
        UIConstants.stylePanelBackground(this);
        UIConstants.applyDefaultPadding(this);

        // Layout Split: Kiri (Input) - Kanan (Tabel)
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(4,4,4,8));
        JPanel adaptiveCard = buildAdaptiveInputCard();
        adaptiveCard.setPreferredSize(new Dimension(0, PREFERRED_CARD_HEIGHT));
        leftPanel.add(adaptiveCard, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        JPanel tableCard = buildTableCard();
        tableCard.setPreferredSize(new Dimension(0, PREFERRED_CARD_HEIGHT));
        rightPanel.add(tableCard, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        // Col 0: Input
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 2.0; gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH; gbc.insets = new Insets(0, 0, 0, 6);
        main.add(leftPanel, gbc);

        // Col 1: Table
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 3.0; gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH; gbc.insets = new Insets(0, 6, 0, 0);
        main.add(rightPanel, gbc);

        add(main, BorderLayout.CENTER);

        initFilterToLatestData();
        loadTableData();
    }

    // --- UI BUILDERS ---

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        UIConstants.styleCardPanel(card);

        // Header Filter
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false); 
        JLabel h = new JLabel("Daftar Overtime & Parttime", SwingConstants.LEFT);
        UIConstants.applyHeaderLabel(h);
        h.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        header.add(h, BorderLayout.WEST);

        JPanel filter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        filter.setOpaque(false); 

        filterYearBox = new JComboBox<>();
        int nowY = LocalDate.now().getYear();
        for (int y = nowY - 3; y <= nowY + 3; y++) filterYearBox.addItem(y);
        filterYearBox.setSelectedItem(nowY);
        filterYearBox.setFont(UIConstants.BODY_FONT);

        filterMonthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) filterMonthBox.addItem(m);
        filterMonthBox.setSelectedItem(LocalDate.now().getMonthValue());
        filterMonthBox.setFont(UIConstants.BODY_FONT);

        filterDayBox = new JComboBox<>(new DefaultComboBoxModel<>());
        filterDayBox.setFont(UIConstants.BODY_FONT);
        filterDayBox.setPrototypeDisplayValue("All");
        
        filterYearBox.addActionListener(a -> { fillFilterDayBox(); loadTableData(); });
        filterMonthBox.addActionListener(a -> { fillFilterDayBox(); loadTableData(); });
        filterDayBox.addActionListener(a -> { if (filterDayBox.getSelectedItem() != null) loadTableData(); });

        JLabel yLbl = new JLabel("Tahun:"); UIConstants.applyBodyLabel(yLbl);
        JLabel mLbl = new JLabel("Bulan:"); UIConstants.applyBodyLabel(mLbl);
        JLabel dLbl = new JLabel("Tanggal:"); UIConstants.applyBodyLabel(dLbl);

        filter.add(yLbl); filter.add(filterYearBox);
        filter.add(mLbl); filter.add(filterMonthBox);
        filter.add(dLbl); filter.add(filterDayBox);

        header.add(filter, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Nama", "Tipe", "OT Weekdays (jam)", "OT Weekend (jam)", "Parttime Days"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        UIConstants.styleTable(table);
        table.setOpaque(false);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);
        table.setShowGrid(true);
        
        UIConstants.setColumnWidths(table, 60, 220, 100, 120, 120, 100);
        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);
        table.setFillsViewportHeight(true);

        JScrollPane sp = new JScrollPane(table);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        card.add(sp, BorderLayout.CENTER);

        fillFilterDayBox();
        return card;
    }

    private JPanel buildAdaptiveInputCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_overtime.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        UIConstants.styleCardPanel(card);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,10,8,10);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        
        // Periode Input
        c.gridx = 0; c.gridy = row;
        JLabel periodeLbl = new JLabel("Periode:"); UIConstants.applyBodyLabel(periodeLbl);
        body.add(periodeLbl, c);

        JPanel inPeriode = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        inPeriode.setOpaque(false);

        inputYearBox = new JComboBox<>();
        int nowY = LocalDate.now().getYear();
        for (int y = nowY - 3; y <= nowY + 3; y++) inputYearBox.addItem(y);
        inputYearBox.setSelectedItem(nowY);
        inputYearBox.setFont(UIConstants.BODY_FONT);

        inputMonthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) inputMonthBox.addItem(m);
        inputMonthBox.setSelectedItem(LocalDate.now().getMonthValue());
        inputMonthBox.setFont(UIConstants.BODY_FONT);

        inputDayBox = new JComboBox<>();
        inputDayBox.setFont(UIConstants.BODY_FONT);
        fillInputDayBox();

        inputDayLbl = new JLabel("Tanggal:");
        UIConstants.applyBodyLabel(inputDayLbl);
        inputDayLbl.setVisible(false);
        inputDayBox.setVisible(false);

        inputYearBox.addActionListener(a -> fillInputDayBox());
        inputMonthBox.addActionListener(a -> fillInputDayBox());

        inPeriode.add(new JLabel("Tahun:")); inPeriode.add(inputYearBox);
        inPeriode.add(new JLabel("Bulan:")); inPeriode.add(inputMonthBox);
        inPeriode.add(inputDayLbl); inPeriode.add(inputDayBox);

        c.gridx = 1; c.gridy = row;
        body.add(inPeriode, c);
        row++;
        
        // Nama & Cek
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        JLabel nameLbl = new JLabel("Nama:"); UIConstants.applyBodyLabel(nameLbl);
        body.add(nameLbl, c);

        JPanel nameRow = new JPanel(new BorderLayout(6,0));
        nameRow.setOpaque(false);
        nameField = new JTextField(14); UIConstants.styleTextField(nameField);
        checkBtn = new JButton("Cek"); UIConstants.styleSecondaryButton(checkBtn);
        checkBtn.addActionListener(a -> doCheckByName());
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.add(checkBtn, BorderLayout.EAST);

        c.gridx = 1; c.gridy = row;
        body.add(nameRow, c);
        row++;
        
        // Adaptive Fields
        TimeFormatterVerifier timeVerifier = new TimeFormatterVerifier();

        // Start Time
        c.gridx = 0; c.gridy = row;
        startLbl = new JLabel("Mulai (HH:mm):"); UIConstants.applyBodyLabel(startLbl);
        startTimeField = new JTextField("15:00", 10); UIConstants.styleTextField(startTimeField);
        startTimeField.setInputVerifier(timeVerifier); 
        startLbl.setVisible(false); startTimeField.setVisible(false);
        body.add(startLbl, c);
        c.gridx = 1; body.add(startTimeField, c);
        row++;

        // End Time
        c.gridx = 0; c.gridy = row;
        endLbl = new JLabel("Selesai (HH:mm):"); UIConstants.applyBodyLabel(endLbl);
        endTimeField = new JTextField("18:00", 10); UIConstants.styleTextField(endTimeField);
        endTimeField.setInputVerifier(timeVerifier); 
        endLbl.setVisible(false); endTimeField.setVisible(false);
        body.add(endLbl, c);
        c.gridx = 1; body.add(endTimeField, c);
        row++;

        // Parttime Days
        c.gridx = 0; c.gridy = row;
        daysLbl = new JLabel("Hari Kerja (Parttime):"); UIConstants.applyBodyLabel(daysLbl);
        parttimeDaysField = new JTextField("0", 12); UIConstants.styleTextField(parttimeDaysField);
        parttimeDaysField.setInputVerifier(parttimeVerifier);
        daysLbl.setVisible(false); parttimeDaysField.setVisible(false);
        body.add(daysLbl, c);
        c.gridx = 1; body.add(parttimeDaysField, c);
        row++;

        // Save Btn
        saveBtn = new JButton("Simpan");
        UIConstants.stylePrimaryButton(saveBtn);
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(a -> doSaveAdaptive());
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        body.add(saveBtn, c);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // --- LOGIC ---

    private void initFilterToLatestData() {
        int defYear = LocalDate.now().getYear();
        int defMonth = LocalDate.now().getMonthValue();
        // Query SQL mencari tanggal MAX dari dua tabel
        String sql = """
            SELECT year_col AS y, month_col AS m FROM (
              SELECT MAX(CONCAT(LPAD(year,4,'0'),LPAD(month,2,'0'))) AS ym, MAX(year) AS year_col, MAX(month) AS month_col
              FROM (
                SELECT YEAR(ot_date) AS year, MONTH(ot_date) AS month FROM overtime_entries
                UNION ALL
                SELECT year, month FROM work_records
              ) t
            ) x
            WHERE ym IS NOT NULL
            LIMIT 1
            """;
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int y = rs.getInt("y");
                int m = rs.getInt("m");
                if (y > 0) defYear = y;
                if (m > 0) defMonth = m;
            }
        } catch (Exception ignored) {}

        final int fy = defYear;
        final int fm = defMonth;
        SwingUtilities.invokeLater(() -> {
            try {
                filterYearBox.setSelectedItem(fy);
                filterMonthBox.setSelectedItem(fm);
                fillFilterDayBox();
            } catch (Exception ignored) {}
        });
    }

    private void fillFilterDayBox() {
        int year = (int) filterYearBox.getSelectedItem();
        int month = (int) filterMonthBox.getSelectedItem();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        m.addElement("All");
        int days = YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= days; d++) m.addElement(String.valueOf(d));
        filterDayBox.setModel(m);
        filterDayBox.setSelectedItem("All");
    }

    private void fillInputDayBox() {
        int year = (int) inputYearBox.getSelectedItem();
        int month = (int) inputMonthBox.getSelectedItem();
        inputDayBox.removeAllItems();
        int days = YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= days; d++) inputDayBox.addItem(d);
        inputDayBox.setSelectedItem(1);
        if (parttimeVerifier != null) parttimeVerifier.setMaxDays(days);
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        int year = (int) filterYearBox.getSelectedItem();
        int month = (int) filterMonthBox.getSelectedItem();
        
        try {
            List<Employee> employees = repo.findAll();
            for (Employee e : employees) {
                double weekdayHours = 0.0;
                double weekendHours = 0.0;
                int parttimeDays = 0;

                if ("FULLTIME".equalsIgnoreCase(e.getEmploymentType())) {
                    List<OvertimeEntry> otEntries = repo.findOvertimeEntriesForMonth(e.getId(), year, month);
                    for (OvertimeEntry entry : otEntries) {
                        double hours = entry.getHours();
                        if (entry.isWeekday()) weekdayHours += hours;
                        else if (entry.isWeekend()) weekendHours += hours;
                    }
                }

                if ("PARTTIME".equalsIgnoreCase(e.getEmploymentType())) {
                    parttimeDays = getParttimeDaysFromDB(e.getId(), year, month);
                }
                
                if (weekdayHours > 0 || weekendHours > 0 || parttimeDays > 0) {
                    tableModel.addRow(new Object[]{
                        e.getId(), e.getName(), e.getEmploymentType(),
                        round2(weekdayHours), round2(weekendHours), parttimeDays
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Helper kecil untuk query parttime days
    private int getParttimeDaysFromDB(int empId, int year, int month) {
        String sql = "SELECT parttime_days FROM work_records WHERE employee_id=? AND year=? AND month=?";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId); ps.setInt(2, year); ps.setInt(3, month);
            try(ResultSet rs = ps.executeQuery()){ if(rs.next()) return rs.getInt(1); }
        } catch(Exception e) {}
        return 0;
    }

    private void doCheckByName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Masukkan nama."); return; }

        String sql = "SELECT id, name, employment_type, is_active FROM employees WHERE LOWER(name) = LOWER(?) ORDER BY is_active DESC";
        try (Connection conn = DB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    resetInputFields(); JOptionPane.showMessageDialog(this, "Nama tidak ditemukan."); return;
                }
                int id = rs.getInt("id");
                String dbName = rs.getString("name");
                String type = rs.getString("employment_type");
                boolean isActive = rs.getBoolean("is_active");

                if (!isActive) {
                    resetInputFields();
                    JOptionPane.showMessageDialog(this, "Karyawan Non-Aktif.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                selectedEmployeeId = id; selectedEmployeeName = dbName; selectedEmployeeType = type;
                nameField.setText(dbName);

                // Cek Paid Status
                if (isMonthPaid(conn, id, (int)inputYearBox.getSelectedItem(), (int)inputMonthBox.getSelectedItem())) {
                    JOptionPane.showMessageDialog(this, "Periode ini sudah DIBAYAR. Tidak bisa edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    resetInputFields();
                    return;
                }

                // Show Inputs based on Type
                if ("FULLTIME".equalsIgnoreCase(type)) {
                    inputDayLbl.setVisible(true); inputDayBox.setVisible(true);
                    setAdaptiveVisible(true, false);
                } else {
                    inputDayLbl.setVisible(false); inputDayBox.setVisible(false);
                    setAdaptiveVisible(false, true);
                    parttimeVerifier.setMaxDays(YearMonth.of((int)inputYearBox.getSelectedItem(), (int)inputMonthBox.getSelectedItem()).lengthOfMonth());
                }
                setInputsEditable(true); saveBtn.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Ditemukan: " + dbName + " (" + type + ")");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void doSaveAdaptive() {
        if (selectedEmployeeId == null) return;
        int year = (int) inputYearBox.getSelectedItem();
        int month = (int) inputMonthBox.getSelectedItem();

        try (Connection conn = DB.getConnection()) {
            if (isMonthPaid(conn, selectedEmployeeId, year, month)) {
                JOptionPane.showMessageDialog(this, "Sudah dibayar, batal simpan."); return;
            }
            ensureWorkRecord(conn, selectedEmployeeId, year, month);

            if ("FULLTIME".equalsIgnoreCase(selectedEmployeeType)) {
                // Save Overtime
                int day = (int) inputDayBox.getSelectedItem();
                LocalDate date = LocalDate.of(year, month, day);
                LocalTime start = LocalTime.parse(startTimeField.getText().trim());
                LocalTime end = LocalTime.parse(endTimeField.getText().trim());
                
                if (!end.isAfter(start)) { JOptionPane.showMessageDialog(this, "Jam selesai salah."); return; }
                
                repo.insertOvertimeEntry(selectedEmployeeId, Date.valueOf(date), Time.valueOf(start), Time.valueOf(end));
                JOptionPane.showMessageDialog(this, "Lembur tersimpan.");
            } else {
                // Save Parttime Days
                int days = Integer.parseInt(parttimeDaysField.getText().trim());
                // Update via SQL manual
                try(PreparedStatement ps = conn.prepareStatement("UPDATE work_records SET parttime_days=? WHERE employee_id=? AND year=? AND month=?")){
                    ps.setInt(1, days); ps.setInt(2, selectedEmployeeId); ps.setInt(3, year); ps.setInt(4, month);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Hari kerja tersimpan.");
            }
            loadTableData(); nameField.setText(""); resetInputFields();
        } catch (Exception e) {
            e.printStackTrace(); JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    
    private boolean isMonthPaid(Connection conn, int eid, int y, int m) {
        try(PreparedStatement ps = conn.prepareStatement("SELECT status FROM payrolls WHERE employee_id=? AND year=? AND month=?")){
            ps.setInt(1, eid); ps.setInt(2, y); ps.setInt(3, m);
            try(ResultSet rs=ps.executeQuery()){ if(rs.next()) return "PAID".equalsIgnoreCase(rs.getString("status")); }
        } catch(Exception e){}
        return false;
    }
    
    private void ensureWorkRecord(Connection conn, int eid, int y, int m) throws SQLException {
        try(PreparedStatement ps=conn.prepareStatement("SELECT 1 FROM work_records WHERE employee_id=? AND year=? AND month=?")){
            ps.setInt(1, eid); ps.setInt(2, y); ps.setInt(3, m);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next()){
                    try(PreparedStatement ins=conn.prepareStatement("INSERT INTO work_records(employee_id,year,month,parttime_days) VALUES (?,?,?,0)")){
                        ins.setInt(1, eid); ins.setInt(2, y); ins.setInt(3, m); ins.executeUpdate();
                    }
                }
            }
        }
    }
    
    private void resetInputFields() {
        selectedEmployeeId = null; selectedEmployeeName = null; selectedEmployeeType = null;
        setAdaptiveVisible(false, false); inputDayLbl.setVisible(false); inputDayBox.setVisible(false);
        saveBtn.setEnabled(false); setInputsEditable(true);
    }
    
    private void setAdaptiveVisible(boolean ot, boolean pt) {
        startLbl.setVisible(ot); startTimeField.setVisible(ot); endLbl.setVisible(ot); endTimeField.setVisible(ot);
        if(!ot){ startTimeField.setText("15:00"); endTimeField.setText("18:00"); }
        daysLbl.setVisible(pt); parttimeDaysField.setVisible(pt);
        if(!pt) parttimeDaysField.setText("0");
        revalidate(); repaint();
    }
    
    private void setInputsEditable(boolean b) {
        nameField.setEnabled(b); checkBtn.setEnabled(b); inputYearBox.setEnabled(b); inputMonthBox.setEnabled(b); inputDayBox.setEnabled(b);
        startTimeField.setEnabled(b); endTimeField.setEnabled(b); parttimeDaysField.setEnabled(b); saveBtn.setEnabled(b);
    }
    
    private double round2(double v) { return Math.round(v*100.0)/100.0; }

    // Inner Classes Verifier
    private static class TimeFormatterVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            if (!(input instanceof JTextField)) return true;
            JTextField field = (JTextField) input;
            String text = field.getText().trim();
            if (text.isEmpty() || text.equals("00:00")) return true; 
            if (text.length() == 4 && text.matches("\\d{4}")) {
                try {
                    int hour = Integer.parseInt(text.substring(0, 2));
                    int minute = Integer.parseInt(text.substring(2, 4));
                    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) return true;
                } catch (NumberFormatException ignored) {}
            }
            if (text.length() == 5 && text.matches("\\d{2}:\\d{2}")) {
                 try {
                    int hour = Integer.parseInt(text.substring(0, 2));
                    int minute = Integer.parseInt(text.substring(3, 5));
                    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) return true;
                } catch (NumberFormatException ignored) { }
            }
            return false; 
        }
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            boolean ok = verify(input);
            JTextField field = (JTextField) input;
            String text = field.getText().trim();
            if (!ok) {
                JOptionPane.showMessageDialog(input, "Format waktu tidak valid (HHMM atau HH:MM).", "Input Waktu Salah", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (text.length() == 4 && text.matches("\\d{4}")) {
                String formattedTime = text.substring(0, 2) + ":" + text.substring(2, 4);
                field.setText(formattedTime);
            }
            return true;
        }
    }

    private static class ParttimeDaysVerifier extends InputVerifier {
        private int maxDays;
        public ParttimeDaysVerifier(int maxDays) { this.maxDays = Math.max(1, maxDays); }
        public void setMaxDays(int maxDays) { this.maxDays = Math.max(1, maxDays); }
        @Override
        public boolean verify(JComponent input) {
            if (!(input instanceof JTextField)) return true;
            String txt = ((JTextField) input).getText().trim();
            if (txt.isEmpty()) return true;
            try {
                int v = Integer.parseInt(txt);
                if (v < 0 || v > maxDays) return false;
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            boolean ok = verify(input);
            if (!ok) {
                String msg = "Hari kerja parttime harus antara 0 hingga " + maxDays + ".";
                JOptionPane.showMessageDialog(input, msg, "Input Tidak Valid", JOptionPane.WARNING_MESSAGE);
            }
            return ok;
        }
    }
}