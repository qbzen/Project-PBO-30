import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.YearMonth;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * EmployeePanel - Updated Logic
 * - Toggle Status hanya input Nama (ID otomatis dicari).
 * - Tombol berubah warna dinamis setelah Cek Nama.
 */
public class EmployeePanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;

    // Filter Components (Left)
    private JComboBox<String> filterBox;
    private JComboBox<String> valueBox;
    private JTextField searchField;
    private JButton searchBtn;

    // Input/Edit Components (Right)
    private JTextField nameField;
    private JComboBox<String> typeField;
    private JComboBox<String> golonganField;
    private JButton saveBtn;

    // Toggle Status Components (Bottom Right)
    private JTextField statusNameField; // Ganti nama variabel agar jelas
    private JButton checkStatusBtn;
    private JButton executeStatusBtn;
    private int selectedIdForStatus = -1; // Menyimpan ID yang ditemukan

    private final EmployeeRepository repo = new EmployeeRepository();

    public EmployeePanel() {
        setLayout(new BorderLayout());
        UIConstants.stylePanelBackground(this);
        UIConstants.applyDefaultPadding(this);

        // Main Layout: 2 Column Grid
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 20));
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        main.setOpaque(false);

        main.add(buildLeft());
        main.add(buildRight());

        add(main, BorderLayout.CENTER);

        loadAll();
    }

    private JPanel buildLeft() {
        JPanel card = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        
        UIConstants.styleCardPanel(card);

        JLabel title = new JLabel("Daftar Karyawan", SwingConstants.LEFT);
        UIConstants.applyHeaderLabel(title);
        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        card.add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID","Nama","Tipe","Gol", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIConstants.styleTable(table);
        
        table.setOpaque(false);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);
        table.setShowGrid(true);
        
        UIConstants.setColumnWidths(table, 50, 200, 100, 50, 80);
        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);

        // Update listener tabel untuk mengisi nama di form bawah juga
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int r = table.getSelectedRow();
                if (r >= 0) {
                    int id = (int) tableModel.getValueAt(r, 0);
                    populateRightFields(id);
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        card.add(sp, BorderLayout.CENTER);

        // Filter Area
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filters.setOpaque(false);
        
        JLabel filterLbl = new JLabel("Filter:"); 
        UIConstants.applyBodyLabel(filterLbl);
        filters.add(filterLbl);

        filterBox = new JComboBox<>(new String[]{"All", "Golongan", "Tipe", "Status"});
        filterBox.setFont(UIConstants.BODY_FONT);
        filters.add(filterBox);

        valueBox = new JComboBox<>();
        valueBox.setFont(UIConstants.BODY_FONT);
        valueBox.setEnabled(false);
        filters.add(valueBox);

        JLabel searchLbl = new JLabel("Cari:"); 
        UIConstants.applyBodyLabel(searchLbl);
        filters.add(searchLbl);
        
        searchField = new JTextField(10); 
        UIConstants.styleTextField(searchField);
        filters.add(searchField);

        searchBtn = new JButton("🔍");
        UIConstants.styleSecondaryButton(searchBtn);
        searchBtn.setPreferredSize(new Dimension(40, 28));
        searchBtn.addActionListener(a -> doSearch());
        filters.add(searchBtn);

        filterBox.addActionListener(a -> { adaptValueBox(); doFilter(); });
        valueBox.addActionListener(a -> { if (valueBox.isEnabled()) doFilter(); });

        card.add(filters, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildRight() {
        JPanel card = new JPanel(new GridBagLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_manage.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };

        UIConstants.styleCardPanel(card);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // --- INPUT / EDIT SECTION ---
        JLabel addTitle = new JLabel("Input / Edit Data");
        addTitle.setFont(UIConstants.SUBHEADER_FONT);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        card.add(addTitle, c);
        row++;

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row;
        JLabel nameLbl = new JLabel("Nama:"); UIConstants.applyBodyLabel(nameLbl);
        card.add(nameLbl, c);
        nameField = new JTextField(15); UIConstants.styleTextField(nameField);
        c.gridx = 1; card.add(nameField, c);
        row++;

        c.gridx = 0; c.gridy = row;
        JLabel typeLbl = new JLabel("Tipe:"); UIConstants.applyBodyLabel(typeLbl);
        card.add(typeLbl, c);
        typeField = new JComboBox<>(new String[]{"FULLTIME","PARTTIME"});
        typeField.setFont(UIConstants.BODY_FONT);
        c.gridx = 1; card.add(typeField, c);
        row++;

        c.gridx = 0; c.gridy = row;
        JLabel golLbl = new JLabel("Golongan:"); UIConstants.applyBodyLabel(golLbl);
        card.add(golLbl, c);
        golonganField = new JComboBox<>(new String[]{"-", "1","2","3","4"});
        golonganField.setFont(UIConstants.BODY_FONT);
        c.gridx = 1; card.add(golonganField, c);
        row++;

        saveBtn = new JButton("Simpan / Update");
        UIConstants.stylePrimaryButton(saveBtn);
        saveBtn.addActionListener(a -> saveEmployee());
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        card.add(saveBtn, c);
        row++;

        typeField.addActionListener(e -> updateTypeDependentFields());
        updateTypeDependentFields();

        // --- SEPARATOR ---
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        card.add(new JSeparator(), c);
        row++;

        // --- STATUS TOGGLE SECTION (Only Name Input) ---
        JLabel delTitle = new JLabel("Ubah Status Karyawan");
        delTitle.setFont(UIConstants.SUBHEADER_FONT.deriveFont(14f));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        card.add(delTitle, c);
        row++;

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = row;
        JLabel delNameLbl = new JLabel("Nama:"); UIConstants.applyBodyLabel(delNameLbl);
        card.add(delNameLbl, c);
        
        // Panel kecil untuk input nama + tombol cek
        JPanel nameCheckPanel = new JPanel(new BorderLayout(5, 0));
        nameCheckPanel.setOpaque(false);
        
        statusNameField = new JTextField(12); 
        UIConstants.styleTextField(statusNameField);
        nameCheckPanel.add(statusNameField, BorderLayout.CENTER);
        
        checkStatusBtn = new JButton("Cek");
        UIConstants.styleSecondaryButton(checkStatusBtn);
        checkStatusBtn.setPreferredSize(new Dimension(60, 28));
        checkStatusBtn.addActionListener(a -> checkStatusByName());
        nameCheckPanel.add(checkStatusBtn, BorderLayout.EAST);
        
        c.gridx = 1; card.add(nameCheckPanel, c);
        row++;

        // Tombol Eksekusi (Awalnya disable, aktif setelah Cek)
        executeStatusBtn = new JButton("Cek Nama Dulu");
        UIConstants.styleSecondaryButton(executeStatusBtn);
        executeStatusBtn.setEnabled(false);
        executeStatusBtn.addActionListener(a -> executeToggleStatus());
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        card.add(executeStatusBtn, c);
        
        return card;
    }

    // --- LOGIC METHODS ---

    private void adaptValueBox() {
        String sel = (String) filterBox.getSelectedItem();
        valueBox.removeAllItems();
        if ("Golongan".equalsIgnoreCase(sel)) {
            valueBox.addItem("1"); valueBox.addItem("2"); valueBox.addItem("3"); valueBox.addItem("4");
            valueBox.setEnabled(true);
        } else if ("Tipe".equalsIgnoreCase(sel)) {
            valueBox.addItem("FULLTIME"); valueBox.addItem("PARTTIME");
            valueBox.setEnabled(true);
        } else if ("Status".equalsIgnoreCase(sel)) {
            valueBox.addItem("Aktif"); valueBox.addItem("Non-Aktif");
            valueBox.setEnabled(true);
        } else {
            valueBox.setEnabled(false);
        }
    }

    private void updateTypeDependentFields() {
        String type = (String) typeField.getSelectedItem();
        boolean isPart = "PARTTIME".equalsIgnoreCase(type);
        golonganField.setEnabled(!isPart);
        if (isPart) {
            golonganField.setSelectedItem("-");
        } else {
            if (golonganField.getSelectedItem() == null || "-".equals(golonganField.getSelectedItem())) {
                golonganField.setSelectedItem("1");
            }
        }
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        try {
            List<Employee> list = repo.findAll();
            for (Employee e : list) {
                addEmployeeToTable(e);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error load: " + ex.getMessage());
        }
    }

    private void addEmployeeToTable(Employee e) {
        Integer golNullable = getGolonganNullable(e.getId());
        String golDisplay = golNullable == null ? "-" : String.valueOf(golNullable);
        String statusStr = e.isActive() ? "Aktif" : "Non-Aktif";
        tableModel.addRow(new Object[]{
            e.getId(), 
            capitalizeEachWord(e.getName()), 
            e.getEmploymentType(), 
            golDisplay, 
            statusStr
        });
    }

    private void populateRightFields(int id) {
        try {
            Employee e = repo.findById(id);
            if (e == null) return;
            
            // Isi Form Input Atas
            nameField.setText(capitalizeEachWord(e.getName()));
            typeField.setSelectedItem(e.getEmploymentType());
            Integer gol = getGolonganNullable(id);
            golonganField.setSelectedItem(gol == null ? "-" : String.valueOf(gol));
            
            // Isi Form Status Bawah (Otomatis isi nama dan set tombol)
            statusNameField.setText(capitalizeEachWord(e.getName()));
            updateStatusButtonState(e); // Helper untuk update tombol
            
            updateTypeDependentFields();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private void doFilter() {
        tableModel.setRowCount(0);
        String f = (String) filterBox.getSelectedItem();
        String v = valueBox.isEnabled() && valueBox.getSelectedItem() != null ? (String) valueBox.getSelectedItem() : null;
        try {
            List<Employee> list = repo.findAll();
            for (Employee e : list) {
                boolean show = true;
                if ("Golongan".equalsIgnoreCase(f) && v != null) {
                    Integer gol = getGolonganNullable(e.getId());
                    show = gol != null && String.valueOf(gol).equals(v);
                } else if ("Tipe".equalsIgnoreCase(f) && v != null) {
                    show = e.getEmploymentType().equalsIgnoreCase(v);
                } else if ("Status".equalsIgnoreCase(f) && v != null) {
                    boolean wantActive = "Aktif".equalsIgnoreCase(v);
                    show = (e.isActive() == wantActive);
                }
                if (show) addEmployeeToTable(e);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    
    private void doSearch() {
        String k = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        try {
            List<Employee> list = repo.findAll();
            for (Employee e : list) {
                if (e.getName().toLowerCase().contains(k)) addEmployeeToTable(e);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private Integer getGolonganNullable(int id) {
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT golongan FROM employees WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt(1);
                    if (rs.wasNull()) return null;
                    return v;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- LOGIKA SAVE/UPDATE ---
    private void saveEmployee() {
        String rawName = nameField.getText().trim();
        String type = (String) typeField.getSelectedItem();
        String golSel = (String) golonganField.getSelectedItem();

        if (rawName.isEmpty()) { JOptionPane.showMessageDialog(this, "Nama tidak boleh kosong"); return; }
        String name = capitalizeEachWord(rawName);

        Integer gol = null;
        if ("FULLTIME".equalsIgnoreCase(type)) {
            if (golSel == null || golSel.equals("-")) { 
                JOptionPane.showMessageDialog(this, "Pilih golongan untuk FULLTIME."); 
                return; 
            }
            gol = Integer.parseInt(golSel);
        }

        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, employment_type, golongan FROM employees WHERE LOWER(name) = ?")) {

            ps.setString(1, rawName.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // --- UPDATE (Status tidak disentuh) ---
                    int existingId = rs.getInt("id");
                    
                    int c = JOptionPane.showConfirmDialog(this, 
                         "Nama karyawan sudah ada. Update data (Tipe/Golongan)?", 
                         "Konfirmasi Update", JOptionPane.YES_NO_OPTION);
                    if (c != JOptionPane.YES_OPTION) return;

                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE employees SET employment_type=?, golongan=? WHERE id=?")) {
                        upd.setString(1, type);
                        if (gol == null) upd.setNull(2, Types.INTEGER); else upd.setInt(2, gol);
                        upd.setInt(3, existingId);
                        upd.executeUpdate();
                    }
                    JOptionPane.showMessageDialog(this, "Data Karyawan Diperbarui.");
                    loadAll();
                    return;
                }
            }

            // --- INSERT BARU (Menggunakan ID Manual) ---
            // 1. Dapatkan ID terbesar + 1 dari Repository
            int newId = repo.getNextEmployeeId();
            
            // 2. Insert menggunakan ID yang didapatkan
            repo.insertEmployee(newId, name, type, gol);
            
            // 3. Lanjutkan proses work record
            YearMonth now = YearMonth.now();
            repo.ensureWorkRecord(newId, now.getYear(), now.getMonthValue());

            JOptionPane.showMessageDialog(this, "Karyawan Baru Tersimpan (Status: Aktif) dengan ID: " + newId);
            nameField.setText("");
            loadAll();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error simpan: " + e.getMessage());
        }
    }

    // --- LOGIKA TOGGLE STATUS BERDASARKAN NAMA ---
    
    private void checkStatusByName() {
        String name = statusNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan nama karyawan dulu.");
            return;
        }

        // Cari karyawan by name (ambil yang paling baru jika duplikat ID)
        String sql = "SELECT id FROM employees WHERE LOWER(name) = LOWER(?) ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    Employee e = repo.findById(id); // Ambil object lengkap
                    updateStatusButtonState(e); // Update tombol warna/teks
                } else {
                    JOptionPane.showMessageDialog(this, "Karyawan tidak ditemukan.");
                    resetStatusButton();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper untuk mengubah tampilan tombol berdasarkan status karyawan
    private void updateStatusButtonState(Employee e) {
        selectedIdForStatus = e.getId();
        executeStatusBtn.setEnabled(true);
        
        if (e.isActive()) {
            // Jika Aktif -> Tombol Merah (Matikan)
            executeStatusBtn.setText("Non-Aktifkan " + e.getName());
            executeStatusBtn.setBackground(UIConstants.DANGER);
            executeStatusBtn.setForeground(Color.WHITE);
        } else {
            // Jika Non-Aktif -> Tombol Hijau (Hidupkan)
            executeStatusBtn.setText("Aktifkan " + e.getName());
            executeStatusBtn.setBackground(UIConstants.SUCCESS);
            executeStatusBtn.setForeground(Color.WHITE);
        }
    }

    private void resetStatusButton() {
        selectedIdForStatus = -1;
        executeStatusBtn.setText("Cek Nama Dulu");
        UIConstants.styleSecondaryButton(executeStatusBtn);
    }

    private void executeToggleStatus() {
        if (selectedIdForStatus == -1) return;

        try {
            Employee e = repo.findById(selectedIdForStatus);
            if (e == null) {
                JOptionPane.showMessageDialog(this, "Data berubah, cek ulang.");
                resetStatusButton();
                return;
            }

            boolean newStatus = !e.isActive(); // Balik statusnya
            String action = newStatus ? "MENGAKTIFKAN" : "MENONAKTIFKAN";

            int confirm = JOptionPane.showConfirmDialog(this,
                "Konfirmasi " + action + " karyawan: " + e.getName() + "?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                repo.updateStatus(selectedIdForStatus, newStatus);
                JOptionPane.showMessageDialog(this, "Status berhasil diubah.");
                loadAll();
                
                // Refresh tombol status agar sesuai kondisi baru
                Employee updated = repo.findById(selectedIdForStatus);
                updateStatusButtonState(updated);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String capitalizeEachWord(String in) {
        if (in == null || in.isBlank()) return in;
        String[] parts = in.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) sb.append(parts[i].substring(1));
            }
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }
}