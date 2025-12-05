import java.awt.*;
import java.sql.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public class LoginPanel extends JPanel {

    // Komponen Input
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginBtn; 
    
    // Gambar background
    private final Image bg = new ImageIcon("assets/img/bg_login.png").getImage();

    public LoginPanel() {
        setLayout(new GridBagLayout());
        UIConstants.applyDefaultPadding(this);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(UIConstants.PADDING, UIConstants.PADDING, UIConstants.PADDING, UIConstants.PADDING);
        c.fill = GridBagConstraints.HORIZONTAL;

        // --- LOGIN CARD ---
        JPanel card = new JPanel(new GridBagLayout());
        UIConstants.styleCardPanel(card); 
        
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(12, 12, 12, 12);
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        int row = 0;

        // Label Judul
        JLabel title = new JLabel("LOGIN ADMIN", SwingConstants.CENTER);
        UIConstants.applyHeaderLabel(title); 
        cc.gridx = 0; cc.gridy = row; cc.gridwidth = 2; cc.anchor = GridBagConstraints.CENTER;
        card.add(title, cc);
        row++;

        cc.gridwidth = 1; 

        // Input Username
        cc.gridx = 0; cc.gridy = row; cc.weightx = 0.0;
        JLabel userLabel = new JLabel("Username:"); UIConstants.applyBodyLabel(userLabel);
        card.add(userLabel, cc);

        userField = new JTextField(20); UIConstants.styleTextField(userField);
        cc.gridx = 1; cc.weightx = 1.0;
        card.add(userField, cc);
        row++;

        // Input Password
        cc.gridx = 0; cc.gridy = row; cc.weightx = 0.0;
        JLabel passLabel = new JLabel("Password:"); UIConstants.applyBodyLabel(passLabel);
        card.add(passLabel, cc);

        passField = new JPasswordField(20); UIConstants.stylePasswordField(passField);
        cc.gridx = 1; cc.weightx = 1.0;
        card.add(passField, cc);
        row++;

        // Tombol Login
        loginBtn = new JButton("Login");
        UIConstants.stylePrimaryButton(loginBtn);
        
        // Action Listener
        loginBtn.addActionListener(a -> doLoginWithThread());
        
        cc.gridx = 0; cc.gridy = row; cc.gridwidth = 2; cc.weightx = 0.0; cc.anchor = GridBagConstraints.CENTER;
        card.add(loginBtn, cc);

        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.CENTER;
        add(card, c);
    }

    /**
     * Override addNotify:
     * Method ini dipanggil otomatis oleh Java saat Panel ditambahkan ke JFrame (Window).
     */
    @Override
    public void addNotify() {
        super.addNotify();
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null) {
            rootPane.setDefaultButton(loginBtn); // ENTER key trigger login
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bg != null) {
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private void doLoginWithThread() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan Password harus diisi!");
            return;
        }

        // 1. UI Preparation (EDT)
        loginBtn.setEnabled(false);
        loginBtn.setText("Authenticating...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // 2. Background Task
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                try (Connection conn = DB.getConnection()) {
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT username FROM users WHERE username=? AND password=?");
                    ps.setString(1, user);
                    ps.setString(2, pass);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("username");
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    String adminName = get(); 
                    
                    if (adminName != null) {
                        LandingPanel landing = new LandingPanel();
                        landing.setAdminName(adminName);
                        Main.setPanel(landing, "Home");
                    } else {
                        JOptionPane.showMessageDialog(LoginPanel.this, 
                            "Username atau password salah!", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        passField.setText("");
                        passField.requestFocus();
                    }
                    
                } catch (InterruptedException | ExecutionException e) {
                    JOptionPane.showMessageDialog(LoginPanel.this, 
                        "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } finally {
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Login");
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        worker.execute(); 
    }
}