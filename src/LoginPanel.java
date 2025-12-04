import java.awt.*;
import java.sql.*;
import javax.swing.*;

/**
 * LoginPanel - Panel antarmuka grafis untuk autentikasi admin.
 * Menggunakan background image kustom dan menempatkan form login di tengah.
 */
public class LoginPanel extends JPanel {

    // Bidang input username dan password
    private JTextField userField;
    private JPasswordField passField;
    
    // Gambar background dimuat saat inisialisasi kelas
    private final Image bg = new ImageIcon("assets/img/bg_login.png").getImage();

    /**
     * Konstruktor: Mengatur layout utama dan membangun form login card.
     */
    public LoginPanel() {
        // Mengatur layout utama GridBagLayout untuk menengahkan komponen
        setLayout(new GridBagLayout());
        
        // Menerapkan padding default ke panel utama
        UIConstants.applyDefaultPadding(this);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(UIConstants.PADDING, UIConstants.PADDING, UIConstants.PADDING, UIConstants.PADDING);
        c.fill = GridBagConstraints.HORIZONTAL;

        // --- LOGIN CARD: Kontainer form dengan style card ---
        JPanel card = new JPanel(new GridBagLayout());
        // Menerapkan gaya card (background putih dan border) agar kontras dengan background image
        UIConstants.styleCardPanel(card); 
        
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(12, 12, 12, 12);
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        int row = 0;

        // Label Judul "LOGIN ADMIN"
        JLabel title = new JLabel("LOGIN ADMIN", SwingConstants.CENTER);
        UIConstants.applyHeaderLabel(title); 
        cc.gridx = 0;
        cc.gridy = row;
        cc.gridwidth = 2; // Span dua kolom
        cc.anchor = GridBagConstraints.CENTER;
        card.add(title, cc);
        row++;

        cc.gridwidth = 1; // Kembali ke satu kolom

        // Label dan Input Username
        cc.gridx = 0;
        cc.gridy = row;
        cc.weightx = 0.0;
        JLabel userLabel = new JLabel("Username:");
        UIConstants.applyBodyLabel(userLabel);
        card.add(userLabel, cc);

        userField = new JTextField(20);
        UIConstants.styleTextField(userField);
        cc.gridx = 1;
        cc.weightx = 1.0;
        card.add(userField, cc);
        row++;

        // Label dan Input Password
        cc.gridx = 0;
        cc.gridy = row;
        cc.weightx = 0.0;
        JLabel passLabel = new JLabel("Password:");
        UIConstants.applyBodyLabel(passLabel);
        card.add(passLabel, cc);

        passField = new JPasswordField(20);
        UIConstants.stylePasswordField(passField);
        cc.gridx = 1;
        cc.weightx = 1.0;
        card.add(passField, cc);
        row++;

        // Tombol Login (centered, span dua kolom)
        JButton login = new JButton("Login");
        UIConstants.stylePrimaryButton(login);
        // Menambahkan action listener untuk proses login
        login.addActionListener(a -> loginAction());
        cc.gridx = 0;
        cc.gridy = row;
        cc.gridwidth = 2;
        cc.weightx = 0.0;
        cc.anchor = GridBagConstraints.CENTER;
        card.add(login, cc);

        // Menambahkan card ke panel utama, menempatkannya di tengah
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        add(card, c);
    }

    /**
     * Override method untuk menggambar background image, 
     * memastikan gambar di-scale sesuai ukuran panel.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bg != null) {
            // Menggambar background image, di-scale agar memenuhi seluruh area panel
            g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    /**
     * Menjalankan logika autentikasi admin ke database.
     */
    private void loginAction() {
        // Menggunakan try-with-resources untuk memastikan koneksi ditutup
        try (Connection conn = DB.getConnection()) {

            // Query SQL untuk mencari username dan password yang cocok
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username FROM users WHERE username=? AND password=?");

            // Mengatur parameter PreparedStatement
            ps.setString(1, userField.getText().trim());
            ps.setString(2, new String(passField.getPassword()));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Login berhasil: Mengambil username, membuat LandingPanel, dan navigasi
                String username = rs.getString("username");
                LandingPanel landing = new LandingPanel();
                landing.setAdminName(username); // Mengatur nama admin di LandingPanel
                Main.setPanel(landing); // Navigasi ke LandingPanel
            } else {
                // Login gagal: Menampilkan pesan error
                JOptionPane.showMessageDialog(this, "Username atau password salah!");
            }

        } catch (Exception e) {
            // Menampilkan pesan error jika terjadi masalah koneksi/SQL
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}