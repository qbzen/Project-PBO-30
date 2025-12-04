import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * OvertimePanel - Panel utama untuk input dan tampilan data lembur (Fulltime) dan hari kerja (Parttime).
 * Mengimplementasikan pemisahan logic agregasi dari SQL ke Java.
 */
public class OvertimePanel extends JPanel {

    // Komponen input periode (kiri, tanpa 'All')
    private JComboBox<Integer> inputYearBox;
    private JComboBox<Integer> inputMonthBox;
    private JComboBox<Integer> inputDayBox;
    private JLabel inputDayLbl;

    // Komponen filter tabel (kanan, dengan 'All' untuk hari)
    private JComboBox<Integer> filterYearBox;
    private JComboBox<Integer> filterMonthBox;
    private JComboBox<String> filterDayBox; 

    // Kontrol input adaptif (kiri)
    private JTextField nameField;
    private JButton checkBtn;

    // Kontrol dinamis (kiri) untuk FULLTIME (Waktu Lembur)
    private JLabel startLbl;
    private JTextField startTimeField;
    private JLabel endLbl;
    private JTextField endTimeField;

    // Kontrol PartTime (kiri) (Jumlah Hari Kerja)
    private JLabel daysLbl;
    private JTextField parttimeDaysField;

    private JButton saveBtn;

    // Tabel (kanan)
    private DefaultTableModel tableModel;
    private JTable table;

    // Data karyawan yang dipilih setelah 'Cek'
    private Integer selectedEmployeeId = null;
    private String selectedEmployeeName = null;
    private String selectedEmployeeType = null;

    // Repository untuk akses data karyawan
    private final EmployeeRepository repo = new EmployeeRepository();

    // Tinggi yang disarankan untuk card
    private static final int PREFERRED_CARD_HEIGHT = 720;

    // Verifier untuk input Parttime Days, disesuaikan dengan jumlah hari di bulan terpilih
    private ParttimeDaysVerifier parttimeVerifier = new ParttimeDaysVerifier(31); 

    /**
     * Konstruktor: Mengatur layout, style, dan memuat komponen UI.
     */
    public OvertimePanel() {
        // Mengatur layout utama dan style panel
        setLayout(new BorderLayout());
        UIConstants.stylePanelBackground(this);
        UIConstants.applyDefaultPadding(this);

        // Membangun panel input (kiri) dengan layout BorderLayout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        JPanel adaptiveCard = buildAdaptiveInputCard();
        adaptiveCard.setPreferredSize(new Dimension(0, PREFERRED_CARD_HEIGHT));
        leftPanel.add(adaptiveCard, BorderLayout.CENTER);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(4,4,4,8));

        // Membangun panel tabel (kanan) dengan layout BorderLayout
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        JPanel tableCard = buildTableCard();
        tableCard.setPreferredSize(new Dimension(0, PREFERRED_CARD_HEIGHT));
        rightPanel.add(tableCard, BorderLayout.CENTER);

        // Kontainer utama menggunakan GridBagLayout untuk pembagian kolom kiri/kanan
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        // Mengatur kolom kiri (input)
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
        gbc.weightx = 2.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 6);
        main.add(leftPanel, gbc);

        // Mengatur kolom kanan (tabel)
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 3.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 6, 0, 0);
        main.add(rightPanel, gbc);

        add(main, BorderLayout.CENTER);

        // Inisialisasi filter tabel ke periode data terbaru dan memuat data awal
        initFilterToLatestData();
        loadTableData();
    }

    /**
     * Membangun card panel (kanan) yang berisi filter periode dan JTable.
     */
    private JPanel buildTableCard() {
        // Card panel dengan custom background image
        JPanel card = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        UIConstants.styleCardPanel(card);

        // Area header dengan judul dan kontrol filter
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false); 

        // Label judul
        JLabel h = new JLabel("Daftar Overtime & Parttime", SwingConstants.LEFT);
        UIConstants.applyHeaderLabel(h);
        h.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        header.add(h, BorderLayout.WEST);

        // Kontrol filter Tahun, Bulan, dan Tanggal
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        filter.setOpaque(false); 

        // Inisialisasi JComboBox untuk filter periode
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
        filterDayBox.setEditable(false);

        // Menambahkan listener untuk memuat ulang data saat filter periode berubah
        filterYearBox.addActionListener(a -> {
            fillFilterDayBox((int) filterYearBox.getSelectedItem(), (int) filterMonthBox.getSelectedItem());
            loadTableData();
        });
        filterMonthBox.addActionListener(a -> {
            fillFilterDayBox((int) filterYearBox.getSelectedItem(), (int) filterMonthBox.getSelectedItem());
            loadTableData();
        });
        filterDayBox.addActionListener(a -> {
            if (filterDayBox.getSelectedItem() != null) {
                // Memuat data, meskipun filter hari tidak signifikan untuk agregasi bulanan
                loadTableData();
            }
        });

        // Menambahkan kontrol filter ke panel header
        JLabel yLbl = new JLabel("Tahun:"); UIConstants.applyBodyLabel(yLbl);
        JLabel mLbl = new JLabel("Bulan:"); UIConstants.applyBodyLabel(mLbl);
        JLabel dLbl = new JLabel("Tanggal:"); UIConstants.applyBodyLabel(dLbl);

        filter.add(yLbl); filter.add(filterYearBox);
        filter.add(mLbl); filter.add(filterMonthBox);
        filter.add(dLbl); filter.add(filterDayBox);

        header.add(filter, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Bagian tabel utama
        tableModel = new DefaultTableModel(new String[]{"ID", "Nama", "Tipe", "OT Weekdays (jam)", "OT Weekend (jam)", "Parttime Days"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIConstants.styleTable(table);

        // Mengatur transparansi tabel
        table.setOpaque(false);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);
        table.setShowGrid(true);

        // Mengatur lebar kolom dan properti tabel lainnya
        int[] widths = new int[]{60, 220, 100, 120, 120, 100};
        UIConstants.setColumnWidths(table, widths);
        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);
        table.setFillsViewportHeight(true);

        // Membuat JScrollPane transparan untuk tabel
        JScrollPane sp = new JScrollPane(table);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());

        card.add(sp, BorderLayout.CENTER);

        // Mengisi pilihan hari pada filter
        fillFilterDayBox((int)filterYearBox.getSelectedItem(), (int)filterMonthBox.getSelectedItem());

        return card;
    }

    /**
     * Mencari periode Tahun dan Bulan data terbaru dari tabel `overtime_entries` atau `work_records` 
     * dan menginisialisasi filter tabel ke periode tersebut.
     */
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

        // Eksekusi query untuk mendapatkan tahun dan bulan terbaru
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

        // Mengatur nilai JComboBox filter setelah inisialisasi
        final int fy = defYear;
        final int fm = defMonth;
        SwingUtilities.invokeLater(() -> {
            try {
                filterYearBox.setSelectedItem(fy);
                filterMonthBox.setSelectedItem(fm);
                fillFilterDayBox(fy, fm);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Mengisi JComboBox filter hari (`filterDayBox`) dengan opsi "All" dan tanggal 1 sampai akhir bulan.
     */
    private void fillFilterDayBox(int year, int month) {
        try {
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement("All");
            int days = YearMonth.of(year, month).lengthOfMonth();
            for (int d = 1; d <= days; d++) m.addElement(String.valueOf(d));
            filterDayBox.setModel(m);
            filterDayBox.setSelectedItem("All");
        } catch (Exception ex) {
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement("All");
            filterDayBox.setModel(m);
            filterDayBox.setSelectedItem("All");
        }
    }

    /**
     * Mengisi JComboBox input hari (`inputDayBox`) dengan tanggal 1 sampai akhir bulan 
     * dan memperbarui validator hari parttime (`parttimeVerifier`).
     */
    private void fillInputDayBox(int year, int month) {
        inputDayBox.removeAllItems();
        int days = YearMonth.of(year, month).lengthOfMonth();
        for (int d = 1; d <= days; d++) inputDayBox.addItem(d);
        inputDayBox.setSelectedItem(1);

        // Memperbarui verifier max days saat bulan/tahun input berubah
        if (parttimeVerifier != null) {
            parttimeVerifier.setMaxDays(days);
        }
    }

    /**
     * Memuat data lembur (FullTime) dan hari kerja (PartTime) untuk bulan terpilih.
     * Agregasi jam lembur dilakukan di Java, bukan di SQL.
     */
    private void loadTableData() {
        // Mengosongkan tabel
        tableModel.setRowCount(0);
        int year = (filterYearBox == null) ? LocalDate.now().getYear() : (int) filterYearBox.getSelectedItem();
        int month = (filterMonthBox == null) ? LocalDate.now().getMonthValue() : (int) filterMonthBox.getSelectedItem();
        
        try {
            // Step 1: Ambil semua karyawan (Aktif & Non-Aktif)
            List<Employee> employees = repo.findAll();
            
            for (Employee e : employees) {
                double weekdayHours = 0.0;
                double weekendHours = 0.0;
                int parttimeDays = 0;

                // Step 2: Aggregation data lembur (hanya untuk FullTime) dari OvertimeEntry
                if ("FULLTIME".equalsIgnoreCase(e.getEmploymentType())) {
                    List<OvertimeEntry> otEntries = repo.findOvertimeEntriesForMonth(e.getId(), year, month);
                    
                    for (OvertimeEntry entry : otEntries) {
                        double hours = entry.getHours();
                        if (entry.isWeekday()) {
                            weekdayHours += hours;
                        } else if (entry.isWeekend()) {
                            weekendHours += hours;
                        }
                    }
                }

                // Step 3: Ambil data hari kerja (hanya untuk PartTime) dari work_records
                if ("PARTTIME".equalsIgnoreCase(e.getEmploymentType())) {
                    try (Connection conn = DB.getConnection();
                         PreparedStatement ps = conn.prepareStatement("SELECT parttime_days FROM work_records WHERE employee_id=? AND year=? AND month=?")) {
                        ps.setInt(1, e.getId());
                        ps.setInt(2, year);
                        ps.setInt(3, month);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                parttimeDays = rs.getInt("parttime_days");
                            }
                        }
                    }
                }
                
                // Step 4: Tambahkan baris ke tabel jika ada data (lembur atau hari kerja)
                if (weekdayHours > 0 || weekendHours > 0 || parttimeDays > 0) {
                    tableModel.addRow(new Object[]{
                        e.getId(),
                        e.getName(),
                        e.getEmploymentType(),
                        round2(weekdayHours),
                        round2(weekendHours),
                        parttimeDays
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error load tabel: " + e.getMessage());
        }
    }

    /**
     * Membulatkan nilai double ke dua tempat desimal.
     */
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Melakukan pencarian karyawan berdasarkan nama. 
     * Jika ditemukan, mengatur kontrol input adaptif sesuai tipe karyawan dan mengecek status pembayaran.
     */
    private void doCheckByName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan nama untuk dicek.");
            return;
        }

        // Query mencari karyawan berdasarkan nama
        String sql = "SELECT id, name, employment_type, is_active FROM employees WHERE LOWER(name) = LOWER(?) ORDER BY is_active DESC";

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    resetInputFields();
                    JOptionPane.showMessageDialog(this, "Nama tidak ditemukan.");
                    return;
                }

                int id = rs.getInt("id");
                String dbName = rs.getString("name");
                String type = rs.getString("employment_type");
                boolean isActive = rs.getBoolean("is_active");

                // Memeriksa status aktif karyawan
                if (!isActive) {
                    resetInputFields();
                    JOptionPane.showMessageDialog(this,
                        "Karyawan ditemukan: " + dbName + "\nStatus: NON-AKTIF\n\nTidak dapat menginput data lembur/kerja.",
                        "Peringatan", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Menyimpan detail karyawan yang terpilih
                selectedEmployeeId = id;
                selectedEmployeeName = dbName;
                selectedEmployeeType = type;

                nameField.setText(dbName);

                // Cek apakah periode input sudah dibayar (paid)
                int iny = (int) inputYearBox.getSelectedItem();
                int inm = (int) inputMonthBox.getSelectedItem();
                boolean alreadyPaid = false;
                try (Connection conn2 = DB.getConnection()) {
                    alreadyPaid = isMonthPaid(conn2, selectedEmployeeId, iny, inm);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Logic proteksi: jika sudah paid, nonaktifkan input
                if (alreadyPaid) {
                    JOptionPane.showMessageDialog(this,
                        "Periode " + iny + "-" + String.format("%02d", inm) + " untuk karyawan " + dbName + " sudah DIBAYAR.\n" +
                        "Input lembur/hari kerja tidak dapat diubah untuk bulan ini.",
                        "Informasi", JOptionPane.INFORMATION_MESSAGE);
                    
                    resetInputFields(); 
                    return; 
                } else {
                    // Jika belum paid, aktifkan input sesuai tipe karyawan
                    if ("FULLTIME".equalsIgnoreCase(type)) {
                        inputDayLbl.setVisible(true);
                        inputDayBox.setVisible(true);
                        setAdaptiveVisible(true, false); // Tampilkan input Overtime
                    } else {
                        inputDayLbl.setVisible(false);
                        inputDayBox.setVisible(false);
                        setAdaptiveVisible(false, true); // Tampilkan input Parttime Days

                        // Memperbarui max days di verifier parttime
                        int maxDays = YearMonth.of((int) inputYearBox.getSelectedItem(), (int) inputMonthBox.getSelectedItem()).lengthOfMonth();
                        parttimeVerifier.setMaxDays(maxDays);
                    }
                    setInputsEditable(true);
                    saveBtn.setEnabled(true);
                    revalidate();
                    repaint();
                    JOptionPane.showMessageDialog(this, "Ditemukan: " + dbName + " (" + type + ")");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cek nama: " + e.getMessage());
        }
    }

    /**
     * Mengatur ulang semua status input, data karyawan terpilih, dan visibilitas input adaptif.
     */
    private void resetInputFields() {
        selectedEmployeeId = null;
        selectedEmployeeName = null;
        selectedEmployeeType = null;
        setAdaptiveVisible(false, false);
        inputDayLbl.setVisible(false);
        inputDayBox.setVisible(false);
        saveBtn.setEnabled(false);
        setInputsEditable(true); // Memastikan periode selector tetap bisa diubah
    }

    /**
     * Mengatur visibilitas bidang input lembur (Fulltime) atau hari kerja (Parttime).
     */
    private void setAdaptiveVisible(boolean showOvertimeInputs, boolean showParttimeDays) {
        // Mengatur visibilitas kontrol Fulltime
        startLbl.setVisible(showOvertimeInputs);
        startTimeField.setVisible(showOvertimeInputs);
        endLbl.setVisible(showOvertimeInputs);
        endTimeField.setVisible(showOvertimeInputs);
        if (!showOvertimeInputs) {
            startTimeField.setText("15:00");
            endTimeField.setText("18:00");
        }

        // Mengatur visibilitas kontrol Parttime
        daysLbl.setVisible(showParttimeDays);
        parttimeDaysField.setVisible(showParttimeDays);
        if (!showParttimeDays) parttimeDaysField.setText("0");

        revalidate();
        repaint();
    }

    /**
     * Mengaktifkan atau menonaktifkan semua kontrol input (digunakan untuk proteksi 'paid').
     */
    private void setInputsEditable(boolean editable) {
        nameField.setEnabled(editable);
        checkBtn.setEnabled(editable);

        if (inputYearBox != null) inputYearBox.setEnabled(editable);
        if (inputMonthBox != null) inputMonthBox.setEnabled(editable);
        if (inputDayBox != null) inputDayBox.setEnabled(editable);

        if (startTimeField != null) startTimeField.setEnabled(editable);
        if (endTimeField != null) endTimeField.setEnabled(editable);

        if (parttimeDaysField != null) parttimeDaysField.setEnabled(editable);
        if (saveBtn != null) saveBtn.setEnabled(editable);
    }

    /**
     * Menyimpan data lembur (Fulltime) atau hari kerja (Parttime) ke database.
     * Menerapkan validasi waktu, hari, dan proteksi 'paid'.
     */
    private void doSaveAdaptive() {
        if (selectedEmployeeId == null) {
            JOptionPane.showMessageDialog(this, "Silakan klik Cek dan pastikan nama ditemukan terlebih dahulu.");
            return;
        }
        int year = (int) inputYearBox.getSelectedItem();
        int month = (int) inputMonthBox.getSelectedItem();

        try (Connection conn = DB.getConnection()) {
            // Cek status paid sebelum memproses simpan
            boolean alreadyPaid = isMonthPaid(conn, selectedEmployeeId, year, month);
            
            if (alreadyPaid) {
                JOptionPane.showMessageDialog(this,
                    "Tidak dapat menyimpan. Periode " + year + "-" + String.format("%02d", month) +
                    " untuk karyawan ini sudah DIBAYAR.",
                    "Operasi Dibatalkan", JOptionPane.WARNING_MESSAGE);
                
                nameField.setText("");
                resetInputFields(); // Reset status input
                return;
            }

            // Memastikan Work Record ada untuk bulan tersebut (Work Record menampung data Parttime Days)
            ensureWorkRecord(conn, selectedEmployeeId, year, month);

            if ("FULLTIME".equalsIgnoreCase(selectedEmployeeType)) {
                // Logic simpan Overtime Entry (Fulltime)
                int day = (inputDayBox == null || !inputDayBox.isVisible()) ? 1 : (int) inputDayBox.getSelectedItem();
                LocalDate date = LocalDate.of(year, month, day);
                
                LocalTime start, end;
                try {
                    start = LocalTime.parse(startTimeField.getText().trim());
                    end = LocalTime.parse(endTimeField.getText().trim());
                } catch (DateTimeParseException dtpe) {
                    JOptionPane.showMessageDialog(this, "Format waktu harus HH:mm, misal 15:00");
                    return;
                }

                if (!end.isAfter(start)) {
                    JOptionPane.showMessageDialog(this, "Waktu selesai harus setelah waktu mulai.");
                    return;
                }

                // Konversi LocalTime/LocalDate ke SQL type
                java.sql.Date sqlDate = java.sql.Date.valueOf(date);
                java.sql.Time sqlStart = java.sql.Time.valueOf(start);
                java.sql.Time sqlEnd = java.sql.Time.valueOf(end);

                // Menyimpan entry lembur
                int insertedId = repo.insertOvertimeEntry(selectedEmployeeId, sqlDate, sqlStart, sqlEnd);
                if (insertedId > 0) {
                    JOptionPane.showMessageDialog(this, "Entry lembur tersimpan untuk " + selectedEmployeeName);
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menyimpan entry lembur.");
                }

            } else {
                // Logic simpan Parttime Days (Parttime)
                int maxDays = YearMonth.of(year, month).lengthOfMonth();
                int days;
                try {
                    days = Integer.parseInt(parttimeDaysField.getText().trim());
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Format angka hari kerja tidak valid.");
                    return;
                }

                // Validasi rentang hari kerja
                if (days < 0) {
                    JOptionPane.showMessageDialog(this, "Hari kerja tidak boleh negatif.");
                    return;
                }
                if (days > maxDays) {
                    JOptionPane.showMessageDialog(this, "Hari kerja parttime tidak boleh melebihi " + maxDays + " (jumlah tanggal pada bulan tersebut).");
                    return;
                }

                // Update Parttime Days di work_records
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE work_records SET parttime_days = ? WHERE employee_id=? AND year=? AND month=?")) {
                    upd.setInt(1, days);
                    upd.setInt(2, selectedEmployeeId);
                    upd.setInt(3, year);
                    upd.setInt(4, month);
                    upd.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Hari kerja parttime tersimpan untuk " + selectedEmployeeName);
            }

            // Refresh tabel dan reset input
            loadTableData();
            nameField.setText("");
            resetInputFields();

        } catch (NumberFormatException nf) {
            JOptionPane.showMessageDialog(this, "Format angka tidak valid.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error simpan: " + e.getMessage());
        }
    }

    /**
     * Memastikan ada record di tabel `work_records` untuk bulan/tahun karyawan yang diberikan.
     * Work records diperlukan untuk menampung `parttime_days`.
     */
    private void ensureWorkRecord(Connection conn, int employeeId, int year, int month) throws SQLException {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM work_records WHERE employee_id=? AND year=? AND month=?")) {
            check.setInt(1, employeeId);
            check.setInt(2, year);
            check.setInt(3, month);
            try (ResultSet r = check.executeQuery()) {
                if (!r.next()) {
                    // Jika work record tidak ada, masukkan record baru dengan parttime_days = 0
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO work_records(employee_id,year,month,parttime_days) VALUES (?,?,?,?)"
                    )) {
                        ins.setInt(1, employeeId);
                        ins.setInt(2, year);
                        ins.setInt(3, month);
                        ins.setInt(4, 0); // parttime_days default
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Memeriksa status pembayaran karyawan untuk bulan/tahun tertentu dari tabel `payrolls`.
     * Mengembalikan true jika status adalah "PAID".
     */
    private boolean isMonthPaid(Connection conn, int employeeId, int year, int month) {
        String sql = "SELECT status FROM payrolls WHERE employee_id=? AND year=? AND month=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    return "PAID".equalsIgnoreCase(status);
                }
            }
        } catch (Exception ignore) {
            // Jika terjadi error, anggap belum paid
        }

        return false;
    }

    /**
     * Membangun card panel (kiri) yang berisi kontrol input periode, pencarian nama, 
     * dan input adaptif (lembur atau hari kerja).
     */
    private JPanel buildAdaptiveInputCard() {
        // Card panel dengan custom background image
        JPanel card = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_overtime.png").getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        UIConstants.styleCardPanel(card);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,10,8,10);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        
        // 1. Kontrol Input Periode (Tahun, Bulan, Tanggal)
        c.gridx = 0; c.gridy = row;
        JLabel periodeLbl = new JLabel("Periode:"); UIConstants.applyBodyLabel(periodeLbl);
        body.add(periodeLbl, c);

        JPanel inPeriode = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        inPeriode.setOpaque(false);

        // Inisialisasi input Tahun
        inputYearBox = new JComboBox<>();
        int nowY = LocalDate.now().getYear();
        for (int y = nowY - 3; y <= nowY + 3; y++) inputYearBox.addItem(y);
        inputYearBox.setSelectedItem(nowY);
        inputYearBox.setFont(UIConstants.BODY_FONT);

        // Inisialisasi input Bulan
        inputMonthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) inputMonthBox.addItem(m);
        inputMonthBox.setSelectedItem(LocalDate.now().getMonthValue());
        inputMonthBox.setFont(UIConstants.BODY_FONT);

        // Inisialisasi input Tanggal
        inputDayBox = new JComboBox<>();
        inputDayBox.setFont(UIConstants.BODY_FONT);
        fillInputDayBox((int)inputYearBox.getSelectedItem(), (int)inputMonthBox.getSelectedItem());

        inputDayLbl = new JLabel("Tanggal:");
        UIConstants.applyBodyLabel(inputDayLbl);

        // Sembunyikan input tanggal secara default
        inputDayLbl.setVisible(false);
        inputDayBox.setVisible(false);

        // Listener untuk memperbarui daftar tanggal dan verifier parttime
        inputYearBox.addActionListener(a -> {
            fillInputDayBox((int)inputYearBox.getSelectedItem(), (int)inputMonthBox.getSelectedItem());
        });
        inputMonthBox.addActionListener(a -> {
            fillInputDayBox((int)inputYearBox.getSelectedItem(), (int)inputMonthBox.getSelectedItem());
        });

        // Menambahkan kontrol periode ke panel input
        inPeriode.add(new JLabel("Tahun:")); inPeriode.add(inputYearBox);
        inPeriode.add(new JLabel("Bulan:")); inPeriode.add(inputMonthBox);
        inPeriode.add(inputDayLbl); inPeriode.add(inputDayBox);

        c.gridx = 1; c.gridy = row;
        body.add(inPeriode, c);
        row++;
        
        // 2. Input Nama dan Tombol Cek
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
        
        // 3. Input Detail Adaptif (Fulltime: Waktu Lembur / Parttime: Hari Kerja)
        
        // Inisialisasi verifier untuk format waktu (HH:MM)
        TimeFormatterVerifier timeVerifier = new TimeFormatterVerifier();

        // Input Waktu Mulai (Fulltime)
        c.gridx = 0; c.gridy = row;
        startLbl = new JLabel("Mulai (HH:mm):"); UIConstants.applyBodyLabel(startLbl);
        startTimeField = new JTextField("15:00", 10); UIConstants.styleTextField(startTimeField);
        startTimeField.setInputVerifier(timeVerifier); 

        startLbl.setVisible(false);
        startTimeField.setVisible(false);

        body.add(startLbl, c);
        c.gridx = 1; c.gridy = row;
        body.add(startTimeField, c);
        row++;

        // Input Waktu Selesai (Fulltime)
        c.gridx = 0; c.gridy = row;
        endLbl = new JLabel("Selesai (HH:mm):"); UIConstants.applyBodyLabel(endLbl);
        endTimeField = new JTextField("18:00", 10); UIConstants.styleTextField(endTimeField);
        endTimeField.setInputVerifier(timeVerifier); 

        endLbl.setVisible(false);
        endTimeField.setVisible(false);

        body.add(endLbl, c);
        c.gridx = 1; c.gridy = row;
        body.add(endTimeField, c);
        row++;

        // Input Hari Kerja (Parttime)
        c.gridx = 0; c.gridy = row;
        daysLbl = new JLabel("Hari Kerja (Parttime):"); UIConstants.applyBodyLabel(daysLbl);
        parttimeDaysField = new JTextField("0", 12); UIConstants.styleTextField(parttimeDaysField);
        
        // Inisialisasi Parttime Days Verifier dengan max days di bulan ini
        int initialMax = YearMonth.of((int)inputYearBox.getSelectedItem(), (int)inputMonthBox.getSelectedItem()).lengthOfMonth();
        if (parttimeVerifier == null) {
            parttimeVerifier = new ParttimeDaysVerifier(initialMax); 
        } else {
             parttimeVerifier.setMaxDays(initialMax);
        }
        parttimeDaysField.setInputVerifier(parttimeVerifier);
        
        daysLbl.setVisible(false);
        parttimeDaysField.setVisible(false);

        body.add(daysLbl, c);
        c.gridx = 1; c.gridy = row;
        body.add(parttimeDaysField, c);
        row++;

        // Tombol Simpan
        saveBtn = new JButton("Simpan");
        UIConstants.stylePrimaryButton(saveBtn);
        saveBtn.setEnabled(false); // Dinonaktifkan hingga karyawan dicek
        saveBtn.addActionListener(a -> doSaveAdaptive());
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        body.add(saveBtn, c);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /**
     * InputVerifier untuk JTextField Waktu (HH:MM).
     * Memvalidasi format waktu, termasuk konversi dari format HHMM ke HH:MM.
     */
    private static class TimeFormatterVerifier extends InputVerifier {
        /**
         * Logika verifikasi: memastikan format HHMM (4 digit angka) atau HH:MM (5 karakter dengan ':') 
         * dan rentang jam/menit yang valid.
         */
        @Override
        public boolean verify(JComponent input) {
            if (!(input instanceof JTextField)) return true;
            JTextField field = (JTextField) input;
            String text = field.getText().trim();
            
            // Mengizinkan kosong atau 00:00 untuk ditangani saat save
            if (text.isEmpty() || text.equals("00:00")) return true; 

            // 1. Coba memproses format 4 digit angka (HHMM)
            if (text.length() == 4 && text.matches("\\d{4}")) {
                try {
                    int hour = Integer.parseInt(text.substring(0, 2));
                    int minute = Integer.parseInt(text.substring(2, 4));

                    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            
            // 2. Coba memproses format HH:MM
            if (text.length() == 5 && text.matches("\\d{2}:\\d{2}")) {
                 try {
                    int hour = Integer.parseInt(text.substring(0, 2));
                    int minute = Integer.parseInt(text.substring(3, 5));
                    if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                        return true;
                    }
                } catch (NumberFormatException ignored) { }
            }

            return false; // Gagal format atau validasi
        }

        /**
         * Menampilkan pesan kesalahan jika verifikasi gagal, 
         * dan memformat HHMM menjadi HH:MM jika valid.
         */
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            boolean ok = verify(input);
            JTextField field = (JTextField) input;
            String text = field.getText().trim();
            
            if (!ok) {
                JOptionPane.showMessageDialog(input, "Format waktu tidak valid. Gunakan format HHMM (cth: 1800) atau HH:MM (cth: 18:00).", "Input Waktu Salah", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // Memformat HHMM menjadi HH:MM
            if (text.length() == 4 && text.matches("\\d{4}")) {
                String formattedTime = text.substring(0, 2) + ":" + text.substring(2, 4);
                field.setText(formattedTime);
            }

            return true;
        }
    }

    /**
     * InputVerifier untuk `parttimeDaysField`.
     * Memastikan input adalah integer non-negatif dan tidak melebihi jumlah hari di bulan terpilih.
     */
    private static class ParttimeDaysVerifier extends InputVerifier {
        private int maxDays;

        /**
         * Konstruktor: Mengatur batas maksimum hari.
         */
        public ParttimeDaysVerifier(int maxDays) {
            this.maxDays = Math.max(1, maxDays);
        }

        /**
         * Mengatur ulang batas maksimum hari (dipanggil saat bulan input berubah).
         */
        public void setMaxDays(int maxDays) {
            this.maxDays = Math.max(1, maxDays);
        }

        /**
         * Logika verifikasi: memastikan nilai adalah integer, $v \geq 0$, dan $v \leq maxDays$.
         */
        @Override
        public boolean verify(JComponent input) {
            if (!(input instanceof JTextField)) return true;
            String txt = ((JTextField) input).getText().trim();
            if (txt.isEmpty()) return true; // Mengizinkan kosong
            try {
                int v = Integer.parseInt(txt);
                if (v < 0 || v > maxDays) return false;
                return true;
            } catch (NumberFormatException nfe) {
                return false;
            }
        }

        /**
         * Menampilkan pesan kesalahan jika verifikasi gagal dan menolak fokus.
         */
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            boolean ok = verify(input);
            if (!ok) {
                String msg = "Hari kerja parttime harus antara 0 hingga " + maxDays + " (jumlah tanggal pada bulan ini).";
                JOptionPane.showMessageDialog(input, msg, "Input Tidak Valid", JOptionPane.WARNING_MESSAGE);
            }
            return ok;
        }
    }
}