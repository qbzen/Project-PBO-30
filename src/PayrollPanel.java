import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * PayrollPanel - Final Version.
 * - Logic perhitungan & akses DB dipindah ke Service/Repo.
 * - UI hanya bertugas menampilkan data (View).
 * - UPDATE: Tidak menampilkan karyawan dengan gaji 0.
 */
public class PayrollPanel extends JPanel {
    private JComboBox<Integer> yearBox;
    private JComboBox<Integer> monthBox;
    private JComboBox<String> typeCombo;
    private JComboBox<String> golonganCombo;
    private JLabel golLbl;
    private DefaultTableModel model;
    private JLabel totalAllLabel;
    
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

        // --- LEFT CARD ---
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

        // Input Tahun
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

        // Input Bulan
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

        fc.gridx = 0; fc.gridy = row; fc.gridwidth = 2;
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)), fc);
        row++; fc.gridwidth = 1;

        // Input Tipe
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        JLabel tipeLbl = new JLabel("Tipe:"); UIConstants.applyBodyLabel(tipeLbl);
        formPanel.add(tipeLbl, fc);
        fc.gridx = 1; fc.weightx = 1.0;
        typeCombo = new JComboBox<>(new String[]{"All", "FULLTIME", "PARTTIME"});
        typeCombo.setFont(UIConstants.BODY_FONT);
        typeCombo.addActionListener(e -> { if (!isInitializing) { updateFilterVisibility(); loadPayroll(); } });
        formPanel.add(typeCombo, fc);
        row++;

        // Input Golongan
        fc.gridx = 0; fc.gridy = row; fc.weightx = 0.0;
        golLbl = new JLabel("Golongan:"); UIConstants.applyBodyLabel(golLbl);
        formPanel.add(golLbl, fc);
        fc.gridx = 1; fc.weightx = 1.0;
        golonganCombo = new JComboBox<>(new String[]{"All", "1", "2", "3", "4"});
        golonganCombo.setFont(UIConstants.BODY_FONT);
        golonganCombo.addActionListener(e -> { if (!isInitializing) loadPayroll(); });
        formPanel.add(golonganCombo, fc);
        row++;

        leftCard.add(formPanel);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25; 
        gbc.insets = new Insets(0, 0, 0, 15);
        main.add(leftCard, gbc);

        // --- RIGHT CARD ---
        JPanel rightCard = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        UIConstants.styleCardPanel(rightCard);
        
        JLabel tableHeader = new JLabel("Daftar Gaji Pending (Karyawan Aktif)", SwingConstants.LEFT);
        tableHeader.setFont(UIConstants.BOLD_BODY_FONT);
        tableHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        rightCard.add(tableHeader, BorderLayout.NORTH);

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
        JButton payBtn = new JButton("Bayarkan");
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

    private void loadPayroll() {
        model.setRowCount(0);
        
        // Reset Total setiap kali load ulang
        totalAllLabel.setText("Total Gaji: " + UIConstants.formatRupiah(0));
        
        if (yearBox.getSelectedItem() == null || monthBox.getSelectedItem() == null) return;
        
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();
        
        String typeFilter = (String) typeCombo.getSelectedItem();
        String golFilter = (String) golonganCombo.getSelectedItem();
        
        long totalPendingDisplayed = 0L;

        try {
            List<PayrollService.PayrollRow> rows = payrollService.calculateAll(year, month);
            double partTimeRate = empRepo.getPartTimeDailyRate();

            for (PayrollService.PayrollRow r : rows) {
                // 1. Cek Status
                String status = payrollService.getPayrollStatus(r.id, year, month);
                if ("PAID".equalsIgnoreCase(status)) continue; 

                // 2. Filter Gaji > 0 (Hanya tampilkan jika ada yang perlu dibayar)
                if (r.total <= 0) continue;

                // 3. Filter UI
                Integer gol = payrollService.getGolonganForEmployee(r.id);
                boolean okType = typeFilter.equals("All") || r.type.equalsIgnoreCase(typeFilter);
                boolean okGol = golFilter.equals("All") || (gol != null && String.valueOf(gol).equals(golFilter));

                if (okType && okGol) {
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
            
            // Update Label Total
            totalAllLabel.setText("Total Gaji: " + UIConstants.formatRupiah(totalPendingDisplayed));
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error load: " + e.getMessage()); 
            e.printStackTrace();
        }
    }

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
            try {
                int paid = payrollService.paySelected(idsToPay, year, month, currentAdminName, "MANUAL", "");
                JOptionPane.showMessageDialog(this, "Berhasil bayar " + paid + " karyawan.");
                loadPayroll();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); e.printStackTrace();
            }
        }
    }
}