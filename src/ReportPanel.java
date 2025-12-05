import java.awt.*;
import java.time.YearMonth;
import java.util.Comparator; // Import Comparator
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader; 

public class ReportPanel extends JPanel {

    private JComboBox<Integer> yearBox;
    private JComboBox<Integer> monthBox;
    private JComboBox<String> sortBox; // [BARU] Dropdown Sort
    private DefaultTableModel model;
    
    private JLabel totalPaidLabel;
    private JLabel totalPendingLabel;
    private JLabel grandTotalLabel;

    private final PayrollService payrollService = new PayrollService();
    private final EmployeeRepository employeeRepo = new EmployeeRepository(); 
    
    public ReportPanel() {
        setLayout(new BorderLayout());
        UIConstants.stylePanelBackground(this);
        UIConstants.applyDefaultPadding(this);

        // --- Main Card dengan Background Image ---
        JPanel mainCard = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        UIConstants.styleCardPanel(mainCard);

        // 1. FILTER AREA
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE0E0E0))); 

        JLabel filterIcon = new JLabel("🔍 Filter Periode: ");
        filterIcon.setFont(UIConstants.BOLD_BODY_FONT);
        filterPanel.add(filterIcon);

        // Year
        JLabel tahunLbl = new JLabel("Tahun:");
        UIConstants.applyBodyLabel(tahunLbl);
        filterPanel.add(tahunLbl);

        yearBox = new JComboBox<>();
        for (int y = 2020; y <= 2035; y++) yearBox.addItem(y);
        yearBox.setFont(UIConstants.BODY_FONT);
        filterPanel.add(yearBox);

        // Month
        JLabel bulanLbl = new JLabel("Bulan:");
        UIConstants.applyBodyLabel(bulanLbl);
        filterPanel.add(bulanLbl);

        monthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) monthBox.addItem(m);
        monthBox.setFont(UIConstants.BODY_FONT);
        filterPanel.add(monthBox);

        // [BARU] Sort
        JLabel sortLbl = new JLabel("Sort:");
        UIConstants.applyBodyLabel(sortLbl);
        filterPanel.add(sortLbl);

        sortBox = new JComboBox<>(new String[]{"ID", "Nama"});
        sortBox.setFont(UIConstants.BODY_FONT);
        // Default ID (index 0)
        filterPanel.add(sortBox);

        mainCard.add(filterPanel, BorderLayout.NORTH);

        // 2. TABEL AREA (13 Kolom)
        model = new DefaultTableModel(new String[]{
            "ID", "Nama", "Status Karyawan", "Tipe", "Gol", 
            "Gaji Pokok", "Lembur", "Hari Kerja", "Rate Harian", 
            "Total Gaji", "Status Payroll", "Dibayar Oleh", "Waktu Bayar"
        }, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(model);
        UIConstants.styleTable(table);
        
        table.setOpaque(false); 
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setOpaque(false);
        table.setShowGrid(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Atur Lebar Kolom
        UIConstants.setColumnWidths(table, 40, 140, 100, 80, 40, 100, 100, 70, 100, 100, 100, 100, 120);

        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 35));

        // Renderer
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        rightRenderer.setOpaque(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        // Assign Renderers
        int[] centerCols = {0, 2, 3, 4, 7, 11, 12};
        int[] rightCols = {5, 6, 8, 9};
        
        for (int col : centerCols) table.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
        for (int col : rightCols) table.getColumnModel().getColumn(col).setCellRenderer(rightRenderer);

        // Custom Renderer Status (Kolom 10)
        table.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (!isSelected) setOpaque(false); else setOpaque(true);

                String status = (String) value;
                if ("PAID".equalsIgnoreCase(status)) {
                    setForeground(UIConstants.SUCCESS);
                    setFont(UIConstants.BOLD_BODY_FONT);
                } else {
                    setForeground(new Color(0xD35400));
                    setFont(UIConstants.BODY_FONT);
                }
                return this;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        tableWrapper.add(sp, BorderLayout.CENTER);

        mainCard.add(tableWrapper, BorderLayout.CENTER);

        // 3. FOOTER TOTAL
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5)); 
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE0E0E0)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        totalPaidLabel = new JLabel("Paid: " + UIConstants.formatRupiah(0));
        totalPaidLabel.setFont(UIConstants.BOLD_BODY_FONT);
        totalPaidLabel.setForeground(UIConstants.SUCCESS); 
        footer.add(totalPaidLabel);

        totalPendingLabel = new JLabel("Pending: " + UIConstants.formatRupiah(0));
        totalPendingLabel.setFont(UIConstants.BOLD_BODY_FONT);
        totalPendingLabel.setForeground(new Color(0xD35400)); 
        footer.add(totalPendingLabel);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 20));
        footer.add(sep);

        grandTotalLabel = new JLabel("Total: " + UIConstants.formatRupiah(0));
        grandTotalLabel.setFont(UIConstants.HEADER_FONT.deriveFont(Font.PLAIN, 20f));
        grandTotalLabel.setForeground(UIConstants.NAVY);
        footer.add(grandTotalLabel);
        
        mainCard.add(footer, BorderLayout.SOUTH);
        add(mainCard, BorderLayout.CENTER);

        // --- INIT ---
        YearMonth now = YearMonth.now();
        yearBox.setSelectedItem(now.getYear());
        monthBox.setSelectedItem(now.getMonthValue());

        yearBox.addActionListener(a -> loadReport());
        monthBox.addActionListener(a -> loadReport());
        sortBox.addActionListener(a -> loadReport()); // Add listener to sort

        SwingUtilities.invokeLater(this::loadReport);
    }

    private void loadReport() {
        model.setRowCount(0);
        
        if (yearBox.getSelectedItem() == null || monthBox.getSelectedItem() == null) return;
        
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();
        
        double sumPaid = 0.0;
        double sumPending = 0.0;
        
        double partTimeRate = employeeRepo.getPartTimeDailyRate();

        try {
            List<PayrollService.PayrollRow> rows = payrollService.calculateAll(year, month);

            // [BARU] Sorting Logic
            String sortMode = (String) sortBox.getSelectedItem();
            if ("Nama".equals(sortMode)) {
                rows.sort((r1, r2) -> r1.name.compareToIgnoreCase(r2.name));
            } else {
                // Default ID
                rows.sort(Comparator.comparingInt(r -> r.id));
            }

            for (PayrollService.PayrollRow r : rows) {
                
                String statusPayroll = payrollService.getPayrollStatus(r.id, year, month);
                if (statusPayroll == null || statusPayroll.isEmpty()) statusPayroll = "PENDING";
                
                Employee e = employeeRepo.findById(r.id);
                String statusKaryawan = (e != null && e.isActive()) ? "Aktif" : "Non-Aktif";

                String displayType = r.type; 
                String golDisplay = (r.golongan == null) ? "-" : String.valueOf(r.golongan);

                String hariStr = "-";
                String rateStr = "-";

                if ("PARTTIME".equalsIgnoreCase(displayType)) {
                    hariStr = String.valueOf(r.daysWorked);
                    rateStr = UIConstants.formatRupiah(partTimeRate);
                }
                
                String paidBy = "-";
                String paidAt = "-";
                
                if ("PAID".equalsIgnoreCase(statusPayroll)) {
                    PayrollService.PaymentLogInfo log = payrollService.getPaymentLogInfo(r.id, year, month);
                    if (log != null) {
                        paidBy = log.paidBy;
                        paidAt = log.paidAt;
                    }
                }

                model.addRow(new Object[]{
                        r.id, 
                        r.name, 
                        statusKaryawan, 
                        displayType, 
                        golDisplay, 
                        UIConstants.formatRupiah(r.base), 
                        UIConstants.formatRupiah(r.overtime), 
                        hariStr, 
                        rateStr, 
                        UIConstants.formatRupiah(r.total), 
                        statusPayroll, 
                        paidBy,         
                        paidAt          
                });

                if ("PAID".equalsIgnoreCase(statusPayroll)) {
                    sumPaid += r.total;
                } else {
                    sumPending += r.total;
                }
            }
            
            totalPaidLabel.setText("Paid: " + UIConstants.formatRupiah(sumPaid));
            totalPendingLabel.setText("Pending: " + UIConstants.formatRupiah(sumPending));
            grandTotalLabel.setText("Total: " + UIConstants.formatRupiah(sumPaid + sumPending));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error memuat laporan: " + e.getMessage());
            e.printStackTrace();
        }
    }
}