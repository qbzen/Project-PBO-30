import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * LandingPanel - Dashboard Utama.
 * - Navigasi ke Panel lain.
 * - Statistik ringkas.
 */
public class LandingPanel extends JPanel {

    private static final int SIDEBAR_WIDTH = 140;
    private static final Color SIDEBAR_BG = UIConstants.NAVY; 
    private static final Color SIDEBAR_HOVER = new Color(0x2A4A6F);
    private static final Color SIDEBAR_ACTIVE = new Color(0x15304F);
    private static final Color CONTENT_BG = UIConstants.LIGHT_GRAY;
    private static final Color HEADER_BG = UIConstants.WHITE;

    private JPanel sidebar;
    private JPanel contentArea;
    private JPanel header;
    private JLabel pageTitleLabel;
    private JLabel headerAdminLabel; 

    private JButton hamburgerMenu;
    private String currentActiveNav = "";
    private String adminName = "Admin"; 
    private boolean sidebarVisible = false;

    // Interface vs Implementation
    private final PayrollRepository employeeRepo;
    private final PayrollService payrollService;

    // Stats Labels
    private JLabel totalEmployeesLabel;
    private JLabel fulltimeLabel;
    private JLabel parttimeLabel;
    private JLabel inactiveLabel;
    private JLabel salaryThisMonthLabel;

    public LandingPanel() {
        // Init logic classes
        this.employeeRepo = new EmployeeRepository();
        this.payrollService = new PayrollService();

        setLayout(new BorderLayout());
        setBackground(CONTENT_BG);

        sidebar = createSidebar();

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(CONTENT_BG);

        header = createHeader();
        mainContent.add(header, BorderLayout.NORTH);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(CONTENT_BG);
        mainContent.add(contentArea, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        showHome();

        // Auto-refresh stats every 10s
        Timer t = new Timer(10000, e -> {
            if ("Home".equals(currentActiveNav)) refreshHomeStats();
        });
        t.setRepeats(true);
        t.start();
    }

    public void setAdminName(String name) {
        this.adminName = name;
        if (headerAdminLabel != null) {
            headerAdminLabel.setText("Halo, " + adminName);
        }
        updateProfileSection();
        if (pageTitleLabel != null) pageTitleLabel.setText(currentActiveNav.isEmpty() ? "Home" : currentActiveNav);
        revalidate(); repaint();
    }
    
    public String getAdminName() { return adminName; }

    private void updateProfileSection() {
        String saved = currentActiveNav;
        boolean wasVisible = sidebarVisible;
        if (sidebarVisible) closeSidebar();
        sidebar = createSidebar();
        if (wasVisible) openSidebar();
        currentActiveNav = saved;
    }

    private JPanel createSidebar() {
        JPanel side = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_sidebar.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                else { g.setColor(SIDEBAR_BG); g.fillRect(0, 0, getWidth(), getHeight()); }
            }
        };
        side.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        side.setBorder(new EmptyBorder(0,0,0,0));

        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
        topWrapper.setOpaque(false);
        topWrapper.add(makeBackButtonRow());
        topWrapper.add(createProfileSection());

        JPanel navCenter = createNavigationMenu();
        navCenter.setOpaque(false);

        JPanel logoutPanel = createLogoutSection();
        logoutPanel.setOpaque(false);

        side.add(topWrapper, BorderLayout.NORTH);
        side.add(navCenter, BorderLayout.CENTER);
        side.add(logoutPanel, BorderLayout.SOUTH);

        return side;
    }

    private JPanel makeBackButtonRow() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false); p.setBorder(new EmptyBorder(8,8,8,8));
        p.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 44));

        JButton backBtn = new JButton("←");
        backBtn.setFont(UIConstants.BODY_FONT);
        backBtn.setForeground(UIConstants.WHITE);
        backBtn.setBackground(SIDEBAR_BG); 
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setPreferredSize(new Dimension(36,32));
        backBtn.setBorder(BorderFactory.createLineBorder(new Color(0x203646)));
        backBtn.addActionListener(e -> closeSidebar());

        p.add(backBtn, BorderLayout.WEST);
        return p;
    }

    private JPanel createProfileSection() {
        JPanel profile = new JPanel();
        profile.setLayout(new BoxLayout(profile, BoxLayout.Y_AXIS));
        profile.setOpaque(false);
        profile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0, SIDEBAR_HOVER),
                new EmptyBorder(12,8,12,8)
        ));

        JLabel avatar = new JLabel("👤", SwingConstants.CENTER);
        avatar.setFont(UIConstants.BODY_FONT.deriveFont(24f));
        avatar.setForeground(UIConstants.WHITE);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        profile.add(avatar);

        profile.add(Box.createRigidArea(new Dimension(0,8)));

        JLabel nameLbl = new JLabel(adminName);
        nameLbl.setFont(UIConstants.BODY_FONT.deriveFont(Font.BOLD, 14f));
        nameLbl.setForeground(UIConstants.WHITE);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        profile.add(nameLbl);

        profile.add(Box.createRigidArea(new Dimension(0,4)));

        JLabel compLbl = new JLabel("Payroll System");
        compLbl.setFont(UIConstants.BODY_FONT.deriveFont(11f));
        compLbl.setForeground(new Color(0xBDC3C7));
        compLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        profile.add(compLbl);

        return profile;
    }

    private JPanel createNavigationMenu() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(8, 0, 8, 0));

        String[] navItems = { "Home", "Manajemen Karyawan", "Input Lembur", "Batch Payroll", "Laporan" };

        for (String item : navItems) {
            JPanel navItem = createNavItem(item);
            nav.add(navItem);
            nav.add(Box.createRigidArea(new Dimension(0,6)));
        }
        nav.add(Box.createVerticalGlue());
        return nav;
    }

    private JPanel createNavItem(String label) {
        JPanel item = new JPanel(new BorderLayout());
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(10,0,10,0));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        item.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 44));
        item.setMaximumSize(new Dimension(SIDEBAR_WIDTH, 44));

        JLabel iconLabel = new JLabel(getIconForNavItem(label), SwingConstants.CENTER);
        iconLabel.setFont(UIConstants.BODY_FONT.deriveFont(18f));
        iconLabel.setForeground(UIConstants.WHITE);
        item.add(iconLabel, BorderLayout.CENTER);

        item.putClientProperty("navItem", Boolean.TRUE);
        item.putClientProperty("navLabel", label);

        if (label.equals(currentActiveNav)) {
            item.setOpaque(true);
            item.setBackground(SIDEBAR_ACTIVE);
        }

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!label.equals(currentActiveNav)) {
                    item.setOpaque(true); item.setBackground(SIDEBAR_HOVER); item.repaint();
                }
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (!label.equals(currentActiveNav)) {
                    item.setOpaque(false); item.repaint();
                }
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { navigateTo(label); }
        });
        return item;
    }

    private String getIconForNavItem(String label) {
        switch (label) {
            case "Home": return "🏠";
            case "Manajemen Karyawan": return "👥";
            case "Input Lembur": return "⏰";
            case "Batch Payroll": return "💰";
            case "Laporan": return "📋";
            default: return "•";
        }
    }

    private JPanel createLogoutSection() {
        JPanel logout = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logout.setOpaque(false);
        logout.setBorder(new EmptyBorder(8,8,12,8));
        logout.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 68));

        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.setFont(UIConstants.BODY_FONT.deriveFont(12f));
        logoutBtn.setForeground(new Color(0xE8E8E8));
        logoutBtn.setBackground(SIDEBAR_HOVER);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setPreferredSize(new Dimension(100, 32));
        logoutBtn.addActionListener(e -> Main.setPanel(new LoginPanel()));

        logout.add(logoutBtn);
        return logout;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_header.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0, new Color(0xE0E0E0)),
            new EmptyBorder(25,25,25,25) 
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        hamburgerMenu = new JButton("☰");
        hamburgerMenu.setFont(UIConstants.BODY_FONT.deriveFont(Font.BOLD, 18f));
        hamburgerMenu.setForeground(SIDEBAR_BG);
        hamburgerMenu.setBackground(HEADER_BG);
        hamburgerMenu.setFocusPainted(false);
        hamburgerMenu.setBorder(new EmptyBorder(5,10,5,10));
        hamburgerMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));
        hamburgerMenu.addActionListener(e -> toggleSidebar());
        left.add(hamburgerMenu);

        pageTitleLabel = new JLabel(currentActiveNav.isEmpty() ? "Home" : currentActiveNav);
        pageTitleLabel.setFont(UIConstants.HEADER_FONT.deriveFont(26f));
        pageTitleLabel.setForeground(SIDEBAR_BG);
        left.add(pageTitleLabel);

        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        
        headerAdminLabel = new JLabel("Halo, " + adminName);
        headerAdminLabel.setFont(UIConstants.BODY_FONT);
        headerAdminLabel.setForeground(UIConstants.GRAY);
        right.add(headerAdminLabel);
        
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void navigateTo(String destination) {
        if (sidebarVisible) closeSidebar();
        if (destination.equals(currentActiveNav)) return;

        currentActiveNav = destination;
        pageTitleLabel.setText(destination);
        if (sidebar != null) resetNavHoverStates(sidebar);

        contentArea.removeAll();

        JPanel panel;
        switch (destination) {
            case "Home": panel = createHomeView(); break;
            case "Manajemen Karyawan": panel = new EmployeePanel(); break;
            case "Input Lembur": panel = new OvertimePanel(); break;
            case "Batch Payroll": 
                PayrollPanel payrollPanel = new PayrollPanel();
                payrollPanel.setCurrentAdminName(this.adminName);
                panel = payrollPanel;
                break;
            case "Laporan": panel = new ReportPanel(); break;
            default: panel = new JPanel(); break;
        }

        if (panel != null) {
            panel.setBackground(CONTENT_BG);
            if ("Home".equals(destination)) {
                contentArea.add(panel, BorderLayout.CENTER);
                refreshHomeStats();
            } else {
                JScrollPane sp = new JScrollPane(panel);
                sp.setBorder(BorderFactory.createEmptyBorder());
                sp.getVerticalScrollBar().setUnitIncrement(16);
                sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                sp.getViewport().setBackground(CONTENT_BG);
                contentArea.add(sp, BorderLayout.CENTER);
            }
        }
        revalidate(); repaint();
    }

    private void showHome() { navigateTo("Home"); }

    private JPanel createHomeView() {
        JPanel home = new JPanel(new GridBagLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_dashboard.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        home.setBackground(CONTENT_BG);
        
        JPanel wrapper = new JPanel(new BorderLayout(0, 20)); 
        wrapper.setOpaque(false);

        JLabel welcome = new JLabel("Selamat Datang", SwingConstants.CENTER);
        welcome.setFont(UIConstants.HEADER_FONT.deriveFont(28f));
        welcome.setForeground(new Color(0x2C3E50));
        wrapper.add(welcome, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 25, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(850, 400));

        // CARD 1: EMPLOYEES
        JPanel empCard = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_card_emp.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        empCard.setBackground(UIConstants.WHITE);
        empCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE6E6E6), 1), new EmptyBorder(30, 30, 30, 30)
        ));

        JLabel empTitle = new JLabel("Total Karyawan (Aktif)", SwingConstants.LEFT);
        empTitle.setFont(UIConstants.BODY_FONT.deriveFont(Font.BOLD, 18f));
        empTitle.setForeground(UIConstants.GRAY);
        empCard.add(empTitle, BorderLayout.NORTH);

        JPanel empCenter = new JPanel();
        empCenter.setLayout(new BoxLayout(empCenter, BoxLayout.Y_AXIS));
        empCenter.setOpaque(false);
        empCenter.add(Box.createVerticalGlue());

        totalEmployeesLabel = new JLabel("—", SwingConstants.LEFT);
        totalEmployeesLabel.setFont(UIConstants.HEADER_FONT.deriveFont(56f));
        totalEmployeesLabel.setForeground(UIConstants.NAVY);
        totalEmployeesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        empCenter.add(totalEmployeesLabel);

        empCenter.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel detailRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        detailRow.setOpaque(false);
        detailRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        fulltimeLabel = new JLabel("Full-time: —"); 
        fulltimeLabel.setFont(UIConstants.BOLD_BODY_FONT.deriveFont(13f));
        fulltimeLabel.setForeground(new Color(0x0056B3)); 
        JLabel spacer = new JLabel("  |  ");
        spacer.setFont(UIConstants.BODY_FONT);
        spacer.setForeground(Color.LIGHT_GRAY);
        parttimeLabel = new JLabel("Part-time: —"); 
        parttimeLabel.setFont(UIConstants.BOLD_BODY_FONT.deriveFont(13f));
        parttimeLabel.setForeground(new Color(0xD35400)); 
        detailRow.add(fulltimeLabel); detailRow.add(spacer); detailRow.add(parttimeLabel);
        empCenter.add(detailRow);

        empCenter.add(Box.createRigidArea(new Dimension(0, 5)));
        inactiveLabel = new JLabel("Non-Aktif: —");
        inactiveLabel.setFont(UIConstants.BODY_FONT.deriveFont(Font.ITALIC, 12f));
        inactiveLabel.setForeground(UIConstants.DANGER); 
        inactiveLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        empCenter.add(inactiveLabel);
        empCenter.add(Box.createVerticalGlue());
        empCard.add(empCenter, BorderLayout.CENTER);

        // CARD 2: SALARY
        JPanel salCard = new JPanel(new BorderLayout()) {
            private final Image bg = new ImageIcon("assets/img/bg_card_salary.png").getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        salCard.setBackground(UIConstants.WHITE);
        salCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE6E6E6), 1), new EmptyBorder(30, 30, 30, 30)
        ));

        String monthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
        JLabel salTitle = new JLabel("Gaji Dibayarkan (" + monthName + ")", SwingConstants.LEFT);
        salTitle.setFont(UIConstants.BODY_FONT.deriveFont(Font.BOLD, 18f));
        salTitle.setForeground(UIConstants.GRAY);
        salCard.add(salTitle, BorderLayout.NORTH);

        JPanel salCenter = new JPanel();
        salCenter.setLayout(new BoxLayout(salCenter, BoxLayout.Y_AXIS));
        salCenter.setOpaque(false);
        salCenter.add(Box.createVerticalGlue());

        salaryThisMonthLabel = new JLabel("—", SwingConstants.LEFT);
        salaryThisMonthLabel.setFont(UIConstants.HEADER_FONT.deriveFont(44f)); 
        salaryThisMonthLabel.setForeground(UIConstants.SUCCESS); 
        salaryThisMonthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        salCenter.add(salaryThisMonthLabel);
        salCenter.add(Box.createVerticalGlue());
        salCard.add(salCenter, BorderLayout.CENTER);

        cards.add(empCard);
        cards.add(salCard);
        wrapper.add(cards, BorderLayout.CENTER);
        home.add(wrapper);
        return home;
    }

    /**
     * CLEAN CODE UPDATE:
     * Logic Raw SQL untuk hitung inactive employee dipindah ke Repository.
     * Tidak ada lagi akses DB langsung di UI.
     */
    private void refreshHomeStats() {
        int total = -1, full = -1, part = -1, inactive = -1;
        double totalSalaryPaid = -1;

        try { 
            total = employeeRepo.countAll(); 
            full = employeeRepo.countFullTime(); 
            part = employeeRepo.countPartTime(); 
            
            // [FIXED] Menggunakan method repository, bukan Raw SQL
            inactive = employeeRepo.countInactive();
        } catch (Exception ignored) {}

        try {
            YearMonth ym = YearMonth.now();
            totalSalaryPaid = payrollService.getTotalPaidForMonth(ym.getYear(), ym.getMonthValue());
        } catch (Exception ignored) {}

        final int fTotal = total;
        final int fFull = full;
        final int fPart = part;
        final int fInactive = inactive;
        final double fSalary = totalSalaryPaid;

        SwingUtilities.invokeLater(() -> {
            totalEmployeesLabel.setText(fTotal >= 0 ? String.valueOf(fTotal) : "—");
            fulltimeLabel.setText(fFull >= 0 ? "Full-time: " + fFull : "Full-time: —");
            parttimeLabel.setText(fPart >= 0 ? "Part-time: " + fPart : "Part-time: —");
            inactiveLabel.setText(fInactive >= 0 ? "Non-Aktif: " + fInactive : "Non-Aktif: —");
            if (fSalary >= 0) salaryThisMonthLabel.setText(UIConstants.formatRupiah(fSalary));
            else salaryThisMonthLabel.setText("—");
        });
    }

    private void resetNavHoverStates(Container c) {
        if (c == null) return;
        for (Component comp : c.getComponents()) {
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent) comp;
                Object isNav = jc.getClientProperty("navItem");
                if (Boolean.TRUE.equals(isNav)) {
                    String label = (String) jc.getClientProperty("navLabel");
                    if (label != null && label.equals(currentActiveNav)) {
                        jc.setOpaque(true); jc.setBackground(SIDEBAR_ACTIVE);
                    } else {
                        jc.setOpaque(false);
                    }
                }
                if (jc.getComponentCount() > 0) resetNavHoverStates(jc);
            } else if (comp instanceof Container) {
                resetNavHoverStates((Container) comp);
            }
        }
    }

    private void closeSidebar() {
        if (sidebarVisible) { try { remove(sidebar); } catch (Exception ignored) {} sidebarVisible = false; }
        if (hamburgerMenu != null) hamburgerMenu.setVisible(true);
        resetNavHoverStates(sidebar);
        revalidate(); repaint();
    }

    private void openSidebar() {
        if (!sidebarVisible) {
            if (sidebar == null) sidebar = createSidebar();
            add(sidebar, BorderLayout.WEST);
            sidebarVisible = true;
            if (hamburgerMenu != null) hamburgerMenu.setVisible(false);
            resetNavHoverStates(sidebar);
            revalidate(); repaint();
        }
    }

    private void toggleSidebar() { if (sidebarVisible) closeSidebar(); else openSidebar(); }
}