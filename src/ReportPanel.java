import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader; 

public class ReportPanel extends JPanel {

    private JComboBox<Integer> yearBox;
    private JComboBox<Integer> monthBox;
    private DefaultTableModel model;
    
    // Label Footer
    private JLabel totalPaidLabel;
    private JLabel totalPendingLabel;
    private JLabel grandTotalLabel;

    private final PayrollService payrollService = new PayrollService();
    private final EmployeeRepository employeeRepo = new EmployeeRepository();
    
    // Inner class untuk menampung detail log pembayaran
    private static class PaymentLogDetails {
        public final String paidBy;
        public final String paidAt;

        public PaymentLogDetails(String paidBy, String paidAt) {
            this.paidBy = paidBy;
            this.paidAt = paidAt;
        }
    }

    public ReportPanel() {
        setLayout(new BorderLayout());
        UIConstants.stylePanelBackground(this);
        UIConstants.applyDefaultPadding(this);

        JPanel mainCard = new JPanel(new BorderLayout()) {
            // Load gambar background tabel
            private final Image bg = new ImageIcon("assets/img/bg_table.png").getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        // ------------------------------------------------------------

        UIConstants.styleCardPanel(mainCard);

        // 1. FILTER AREA
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        filterPanel.setOpaque(false); // Transparan
        filterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE0E0E0))); 

        JLabel filterIcon = new JLabel("🔍 Filter Periode: ");
        filterIcon.setFont(UIConstants.BOLD_BODY_FONT);
        filterPanel.add(filterIcon);

        JLabel tahunLbl = new JLabel("Tahun:");
        UIConstants.applyBodyLabel(tahunLbl);
        filterPanel.add(tahunLbl);

        yearBox = new JComboBox<>();
        for (int y = 2020; y <= 2035; y++) yearBox.addItem(y);
        yearBox.setFont(UIConstants.BODY_FONT);
        filterPanel.add(yearBox);

        JLabel bulanLbl = new JLabel("Bulan:");
        UIConstants.applyBodyLabel(bulanLbl);
        filterPanel.add(bulanLbl);

        monthBox = new JComboBox<>();
        for (int m = 1; m <= 12; m++) monthBox.addItem(m);
        monthBox.setFont(UIConstants.BODY_FONT);
        filterPanel.add(monthBox);

        mainCard.add(filterPanel, BorderLayout.NORTH);

        // 2. TABEL AREA
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
        // --------------------------------------------------------------

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Atur Lebar Kolom
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(11).setPreferredWidth(80); // Dibayar Oleh
        table.getColumnModel().getColumn(12).setPreferredWidth(120); // Waktu Bayar

        table.setRowHeight(UIConstants.TABLE_ROW_HEIGHT);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 35));

        // Renderer
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        rightRenderer.setOpaque(false); // Transparan

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false); // Transparan

        // Renderers: 13 Kolom (0-12)
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);  // Gaji Pokok
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);  // Lembur
        table.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Hari Kerja
        table.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);  // Rate Harian
        table.getColumnModel().getColumn(9).setCellRenderer(rightRenderer);  // Total Gaji
        
        // Custom Renderer Status (Kolom 10)
        table.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                // Agar baris yang tidak diseleksi tetap transparan
                if (!isSelected) setOpaque(false); 
                else setOpaque(true); // Baris terpilih tetap punya warna highlight

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
        
        // Renderer untuk Paid By dan Paid At (Kolom 11 dan 12)
        table.getColumnModel().getColumn(11).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(12).setCellRenderer(centerRenderer); 


        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false); // Transparan
        sp.getViewport().setOpaque(false); // Viewport Transparan (Kunci agar gambar tembus)
        
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false); // Transparan
        tableWrapper.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        tableWrapper.add(sp, BorderLayout.CENTER);

        mainCard.add(tableWrapper, BorderLayout.CENTER);

        // 3. FOOTER TOTAL
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5)); 
        footer.setOpaque(false); // Transparan
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE0E0E0)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Label Total Paid
        totalPaidLabel = new JLabel("Paid: " + UIConstants.formatRupiah(0));
        totalPaidLabel.setFont(UIConstants.BOLD_BODY_FONT);
        totalPaidLabel.setForeground(UIConstants.SUCCESS); 
        footer.add(totalPaidLabel);

        // Label Total Pending
        totalPendingLabel = new JLabel("Pending: " + UIConstants.formatRupiah(0));
        totalPendingLabel.setFont(UIConstants.BOLD_BODY_FONT);
        totalPendingLabel.setForeground(new Color(0xD35400)); 
        footer.add(totalPendingLabel);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 20));
        footer.add(sep);

        // Label Grand Total
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

        SwingUtilities.invokeLater(this::loadReport);
    }

    private PaymentLogDetails getPaymentLogDetails(int empId, int year, int month) {
        String sql = """
            SELECT pl.paid_by, pl.paid_at
            FROM payrolls p
            JOIN payment_logs pl ON p.id = pl.payroll_id
            WHERE p.employee_id = ? AND p.year = ? AND p.month = ? AND p.status = 'PAID'
        """;
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String paidBy = rs.getString("paid_by");
                    Timestamp paidAtTs = rs.getTimestamp("paid_at");
                    // Format waktu: 25 Des 2025 10:30
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
                    String paidAtStr = paidAtTs.toLocalDateTime().format(formatter);
                    return new PaymentLogDetails(paidBy, paidAtStr);
                }
            }
        } catch (Exception e) {
            // Error ini tidak perlu menghentikan seluruh laporan, cukup log
            System.err.println("Error fetching payment log for employee " + empId + ": " + e.getMessage());
        }
        // Jika tidak ada log pembayaran (status PENDING atau error), kembalikan "-"
        return new PaymentLogDetails("-", "-"); 
    }

    private void loadReport() {
        model.setRowCount(0);
        
        if (yearBox.getSelectedItem() == null || monthBox.getSelectedItem() == null) return;
        
        int year = (int) yearBox.getSelectedItem();
        int month = (int) monthBox.getSelectedItem();
        
        double sumPaid = 0.0;
        double sumPending = 0.0;
        double partTimeRate = DB.getPartTimeDailyRate();

        try {
            List<PayrollService.PayrollRow> rows = payrollService.calculateAll(year, month);

            for (PayrollService.PayrollRow r : rows) {
                Employee e = employeeRepo.findById(r.id);
                if (e == null) continue;

                String statusPayroll = payrollService.getPayrollStatus(r.id, year, month);
                if (statusPayroll == null || statusPayroll.isEmpty()) {
                    statusPayroll = "PENDING";
                }

                String statusKaryawan = e.isActive() ? "Aktif" : "Non-Aktif";
                
                Integer gol = null;
                if (e instanceof FullTimeEmployee) {
                    gol = payrollService.getGolonganForEmployee(e.getId());
                }
                String golDisplay = (gol == null) ? "-" : String.valueOf(gol);

                String hariStr = "-";
                String rateStr = "-";

                if ("PARTTIME".equalsIgnoreCase(r.type)) {
                    hariStr = String.valueOf(r.daysWorked);
                    rateStr = UIConstants.formatRupiah(partTimeRate);
                }
                
                // Ambil detail pembayaran
                PaymentLogDetails log = new PaymentLogDetails("-", "-"); // Default: "-"
                if ("PAID".equalsIgnoreCase(statusPayroll)) {
                    log = getPaymentLogDetails(r.id, year, month);
                }

                // --- model.addRow dengan 13 Kolom ---
                model.addRow(new Object[]{
                        r.id, r.name, statusKaryawan, r.type, golDisplay,
                        UIConstants.formatRupiah(r.base), // Gaji Pokok
                        UIConstants.formatRupiah(r.overtime), // Lembur
                        hariStr, rateStr, // Hari Kerja & Rate Harian
                        UIConstants.formatRupiah(r.total), // Total Gaji
                        statusPayroll, // Status Payroll
                        log.paidBy,         // Dibayar Oleh
                        log.paidAt          // Waktu Bayar
                });

                // Hitung Subtotal
                if ("PAID".equalsIgnoreCase(statusPayroll)) {
                    sumPaid += r.total;
                } else {
                    sumPending += r.total;
                }
            }
            
            // Update Label Footer
            totalPaidLabel.setText("Paid: " + UIConstants.formatRupiah(sumPaid));
            totalPendingLabel.setText("Pending: " + UIConstants.formatRupiah(sumPending));
            grandTotalLabel.setText("Total: " + UIConstants.formatRupiah(sumPaid + sumPending));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error memuat laporan: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
