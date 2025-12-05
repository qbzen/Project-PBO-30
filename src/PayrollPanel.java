import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * PayrollPanel - Multi-threaded Version (SwingWorker).
 * - Mencegah UI Freeze saat memuat data atau melakukan pembayaran.
 * - [UPDATED] Fitur Sort ID/Nama di atas tabel (Default ID).
 * - Filter tombol lebar (Navy) di panel kiri.
 */
public class PayrollPanel extends JPanel {
    private JComboBox<Integer> yearBox;
    private JComboBox<Integer> monthBox;
    private JComboBox<String> typeCombo;
    private JComboBox<String> golonganCombo;
    private JTextField searchNameField; 
    private JLabel golLbl;
    private JComboBox<String> sortBox; // [BARU] Dropdown Sort
    
    private DefaultTableModel model;
    private JLabel totalAllLabel;
    private JButton payBtn; 
    
    private final PayrollService payrollService = new PayrollService();
    private final EmployeeRepository empRepo = new EmployeeRepository();

    private boolean isInitializing = true;
    private String currentAdminName = "admin"; 

    public PayrollPanel() {
        setLayout(new BorderLayout());
        UIConstants.stylePanelBackground(this);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10)); 

        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; 

        // --- LEFT CARD (FILTER) ---
        JPanel leftCard = new JPanel(new GridBagLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_payroll_filter.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        UIConstants.styleCardPanel(leftCard);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false); 
        GridBagConstraints fc = new GridBagConstraints();
        fc.insets = new Insets(8, 5, 8, 5);
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.anchor = GridBagConstraints.WEST;
        int row = 0;

        JLabel filterTitle = new JLabel("Filter Parameter", SwingConstants.CENTER);
        filterTitle.setFont(UIConstants.SUBHEADER_FONT);
        fc.gridx = 0; fc.gridy = row; fc.gridwidth = 2; 
        fc.insets = new Insets(0, 0, 20, 0); 
        formPanel.add(filterTitle, fc);
        row++;
        fc.insets = new Insets(8, 5, 8, 5); fc.gridwidth = 1;

        // 1. Input Tahun
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        JLabel tahunLbl = new JLabel("Tahun:"); UIConstants.applyBodyLabel(tahunLbl);
        formPanel.add(tahunLbl, fc);
        fc.gridx = 1; fc.weightx = 1.0;
        yearBox = new JComboBox<>();
        for (int y = 2020; y <= 2035; y++) yearBox.addItem(y);
        yearBox.setSelectedItem(java.time.LocalDate.now().getYear());
        yearBox.setFont(UIConstants.BODY_FONT);
        yearBox.addActionListener(e -> { if (!isInitializing) loadPayroll(); });
        formPanel.add(yearBox, fc);
        row++;

        // 2. Input Bulan
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        JLabel bulanLbl = new JLabel("Bulan:"); UIConstants.applyBodyLabel(bulanLbl);
        formPanel.add(bulanLbl, fc);
        fc.gridx = 1; fc.weightx = 1.0;
        monthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) monthBox.addItem(m);
        monthBox.setSelectedItem(java.time.LocalDate.now().getMonthValue());
        monthBox.setFont(UIConstants.BODY_FONT);
        monthBox.addActionListener(e -> { if (!isInitializing) loadPayroll(); });
        formPanel.add(monthBox, fc);
        row++;

        // Spacer
        fc.gridx = 0; fc.gridy = row; fc.gridwidth = 2;
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)), fc);
        row++; fc.gridwidth = 1;

        // 3. Input Search Nama
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        JLabel searchLbl = new JLabel("Nama:"); UIConstants.applyBodyLabel(searchLbl);
        formPanel.add(searchLbl, fc);

        fc.gridx = 1; fc.weightx = 1.0;
        searchNameField = new JTextField();
        try { UIConstants.styleTextField(searchNameField); } catch (Exception ignored) { searchNameField.setFont(UIConstants.BODY_FONT); }
        searchNameField.addActionListener(e -> { if (!isInitializing) loadPayroll(); }); 
        formPanel.add(searchNameField, fc);
        row++;

        // 4. Input Tipe
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        JLabel tipeLbl = new JLabel("Tipe:"); UIConstants.applyBodyLabel(tipeLbl);
        formPanel.add(tipeLbl, fc);
        fc.gridx = 1; fc.weightx = 1.0;
        typeCombo = new JComboBox<>(new String[]{"All", "FULLTIME", "PARTTIME"});
        typeCombo.setFont(UIConstants.BODY_FONT);
        typeCombo.addActionListener(e -> { if (!isInitializing) { updateFilterVisibility(); loadPayroll(); } });
        formPanel.add(typeCombo, fc);
        row++;

        // 5. Input Golongan
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        golLbl = new JLabel("Golongan:"); UIConstants.applyBodyLabel(golLbl);
        formPanel.add(golLbl, fc);
        fc.gridx = 1; fc.weightx = 1.0;
        golonganCombo = new JComboBox<>(new String[]{"All", "1", "2", "3", "4"});
        golonganCombo.setFont(UIConstants.BODY_FONT);
        golonganCombo.addActionListener(e -> { if (!isInitializing) loadPayroll(); });
        formPanel.add(golonganCombo, fc);
        row++;

        // 6. Tombol Filter (Full Width)
        fc.gridx = 0; fc.gridy = row; 
        fc.gridwidth = 2; 
        fc.fill = GridBagConstraints.HORIZONTAL; 
        fc.insets = new Insets(10, 5, 10, 5); 

        JButton filterBtn = new JButton("Filter");
        UIConstants.stylePrimaryButton(filterBtn);
        filterBtn.setBackground(UIConstants.NAVY);
        filterBtn.setFont(UIConstants.BOLD_BODY_FONT.deriveFont(12f));
        filterBtn.addActionListener(e -> loadPayroll());
        
        formPanel.add(filterBtn, fc);
        
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(8, 5, 8, 5);
        fc.gridwidth = 1; 
        row++;

        leftCard.add(formPanel);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25; 
        gbc.insets = new Insets(0, 0, 0, 15);
        main.add(leftCard, gbc);

        // --- RIGHT CARD (TABLE) ---
        JPanel rightCard = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        UIConstants.styleCardPanel(rightCard);
        
        // --- [UPDATED] Header Panel (Title + Sort) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel tableHeader = new JLabel("Daftar Gaji Pending (Karyawan Aktif)", SwingConstants.LEFT);
        tableHeader.setFont(UIConstants.BOLD_BODY_FONT);
        headerPanel.add(tableHeader, BorderLayout.WEST);

        // Sort Control
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        sortPanel.setOpaque(false);
        JLabel sortLbl = new JLabel("Sort:");
        UIConstants.applyBodyLabel(sortLbl);
        sortPanel.add(sortLbl);

        sortBox = new JComboBox<>(new String[]{"ID", "Nama"});
        sortBox.setFont(UIConstants.BODY_FONT);
        // Trigger reload (sorting happens in populateTable)
        sortBox.addActionListener(e -> { if (!isInitializing) loadPayroll(); });
        sortPanel.add(sortBox);

        headerPanel.add(sortPanel, BorderLayout.EAST);
        rightCard.add(headerPanel, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel(new String[]{"ID", "Nama", "Tipe", "Gol", "Base", "Overtime", "Hari", "Rate", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        UIConstants.styleTable(table);
        
        table.setOpaque(false);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);
        table.setShowGrid(true);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); 
        UIConstants.setColumnWidths(table, 40, 150, 70, 40, 90, 80, 50, 80, 100);
        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);
        table.setFillsViewportHeight(true);
        
        JScrollPane sc = new JScrollPane(table);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.setOpaque(false); sc.getViewport().setOpaque(false);
        
        rightCard.add(sc, BorderLayout.CENTER);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.75; 
        gbc.insets = new Insets(0, 0, 0, 0);
        main.add(rightCard, gbc);
        add(main, BorderLayout.CENTER);

        // FOOTER
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5)); 
        footer.setOpaque(false);
        
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalPanel.setOpaque(false);
        totalAllLabel = new JLabel("Total Gaji: " + UIConstants.formatRupiah(0));
        totalAllLabel.setFont(UIConstants.HEADER_FONT.deriveFont(Font.PLAIN, 20f)); 
        totalAllLabel.setForeground(UIConstants.NAVY);
        totalPanel.add(totalAllLabel);
        
        footer.add(totalPanel, BorderLayout.WEST);
        
        JPanel payPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        payPanel.setOpaque(false);
        payBtn = new JButton("Bayarkan");
        UIConstants.stylePrimaryButton(payBtn);
        payBtn.addActionListener(a -> confirmAndPayVisible());
        payPanel.add(payBtn);
        
        footer.add(payPanel, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        updateFilterVisibility(); 
        isInitializing = false;
        loadPayroll();
    }
    
    public void setCurrentAdminName(String adminName) {
        this.currentAdminName = adminName;
    }

    private void updateFilterVisibility() {
        String selectedType = (String) typeCombo.getSelectedItem();
        boolean isFullTime = "FULLTIME".equalsIgnoreCase(selectedType);
        golLbl.setEnabled(isFullTime);
        golonganCombo.setEnabled(isFullTime);
        if (!isFullTime && !"All".equals(golonganCombo.getSelectedItem())) {
            golonganCombo.setSelectedItem("All"); 
        }
    }

    // --- SWING WORKER IMPLEMENTATION FOR LOADING DATA ---
    private void loadPayroll() {
        if (yearBox.getSelectedItem() == null || monthBox.getSelectedItem() == null) return;
        
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();

        // 1. UI Preparation (EDT)
        model.setRowCount(0);
        totalAllLabel.setText("Total Gaji: " + UIConstants.formatRupiah(0));
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); 

        // 2. Background Task
        SwingWorker<List<PayrollService.PayrollRow>, Void> worker = new SwingWorker<>() {
            private double partTimeRate = 0;

            @Override
            protected List<PayrollService.PayrollRow> doInBackground() throws Exception {
                // Proses Berat: Koneksi DB dan Kalkulasi Gaji
                partTimeRate = empRepo.getPartTimeDailyRate();
                return payrollService.calculateAll(year, month);
            }

            @Override
            protected void done() {
                // 3. UI Update (EDT)
                try {
                    List<PayrollService.PayrollRow> rows = get(); 
                    populateTable(rows, partTimeRate, year, month);
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(PayrollPanel.this, "Error load data: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor()); 
                }
            }
        };

        worker.execute(); 
    }

    private void populateTable(List<PayrollService.PayrollRow> rows, double partTimeRate, int year, int month) {
        String typeFilter = (String) typeCombo.getSelectedItem();
        String golFilter = (String) golonganCombo.getSelectedItem();
        String searchKeyword = searchNameField.getText().trim().toLowerCase();
        
        // [BARU] Sort Logic
        String sortMode = (String) sortBox.getSelectedItem();
        if ("Nama".equals(sortMode)) {
            rows.sort(Comparator.comparing(r -> r.name, String::compareToIgnoreCase));
        } else {
            rows.sort(Comparator.comparingInt(r -> r.id));
        }
        
        long totalPendingDisplayed = 0L;

        for (PayrollService.PayrollRow r : rows) {
            String status = payrollService.getPayrollStatus(r.id, year, month);
            if ("PAID".equalsIgnoreCase(status)) continue; 

            if (r.total <= 0) continue;

            Integer gol = payrollService.getGolonganForEmployee(r.id);
            
            // Logic Filter
            boolean okType = typeFilter.equals("All") || r.type.equalsIgnoreCase(typeFilter);
            boolean okGol = golFilter.equals("All") || (gol != null && String.valueOf(gol).equals(golFilter));
            boolean okName = searchKeyword.isEmpty() || r.name.toLowerCase().contains(searchKeyword);

            if (okType && okGol && okName) {
                long totalRounded = Math.round(r.total);
                String hariStr = "-";
                String rateStr = "-";
                
                if ("PARTTIME".equalsIgnoreCase(r.type)) {
                    hariStr = String.valueOf(r.daysWorked);
                    rateStr = UIConstants.formatRupiah(partTimeRate);
                }
                
                model.addRow(new Object[]{
                        r.id, r.name, r.type, (gol == null ? "-" : gol),
                        UIConstants.formatRupiah(r.base), 
                        UIConstants.formatRupiah(r.overtime),
                        hariStr, rateStr, 
                        UIConstants.formatRupiah(totalRounded)
                });
                
                totalPendingDisplayed += totalRounded;
            }
        }
        totalAllLabel.setText("Total Gaji: " + UIConstants.formatRupiah(totalPendingDisplayed));
    }

    // --- SWING WORKER IMPLEMENTATION FOR PAYING ---
    private void confirmAndPayVisible() {
        int rowCount = model.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Tidak ada data gaji yang perlu dibayar (Total > 0)."); return;
        }
        
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();
        
        long totalToPay = 0L;
        List<Integer> idsToPay = new ArrayList<>();
        
        for (int i = 0; i < rowCount; i++) {
            int id = (int) model.getValueAt(i, 0);
            String totalStr = (String) model.getValueAt(i, 8);
            String raw = totalStr.replaceAll("[^0-9]", ""); 
            long t = 0;
            if (!raw.isEmpty()) t = Long.parseLong(raw);
            totalToPay += t;
            idsToPay.add(id);
        }
        
        String msg = "Bayar " + idsToPay.size() + " karyawan aktif?\nTotal: " + UIConstants.formatRupiah(totalToPay);
        int confirm = JOptionPane.showConfirmDialog(this, msg, "Konfirmasi Pembayaran", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            payBtn.setEnabled(false);
            payBtn.setText("Processing...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return payrollService.paySelected(idsToPay, year, month, currentAdminName, "MANUAL", "");
                }

                @Override
                protected void done() {
                    try {
                        int paid = get();
                        JOptionPane.showMessageDialog(PayrollPanel.this, "Berhasil bayar " + paid + " karyawan.");
                        loadPayroll(); 
                    } catch (InterruptedException | ExecutionException e) {
                        JOptionPane.showMessageDialog(PayrollPanel.this, "Error: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        payBtn.setEnabled(true);
                        payBtn.setText("Bayarkan");
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            };
            
            worker.execute(); 
        }
    }
}