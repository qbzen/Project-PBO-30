import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginPanel extends JPanel {

    private JTextField userField;
    private JPasswordField passField;

    public LoginPanel() {

        setLayout(new GridBagLayout());
        setBackground(new Color(0xEEEEEE));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(12, 12, 12, 12);
        cc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        JLabel title = new JLabel("LOGIN ADMIN", SwingConstants.CENTER);
        title.setFont(new Font("Impact", Font.BOLD, 32));
        cc.gridx = 0; cc.gridy = row; cc.gridwidth = 2;
        card.add(title, cc);
        row++;

        cc.gridwidth = 1;

        cc.gridx = 0; cc.gridy = row;
        card.add(new JLabel("Username:"), cc);
        userField = new JTextField(20);
        cc.gridx = 1;
        card.add(userField, cc);
        row++;

        cc.gridx = 0; cc.gridy = row;
        card.add(new JLabel("Password:"), cc);
        passField = new JPasswordField(20);
        cc.gridx = 1;
        card.add(passField, cc);
        row++;

        JButton login = new JButton("Login");
        login.addActionListener(a -> loginAction());
        cc.gridx = 0; cc.gridy = row; cc.gridwidth = 2;
        card.add(login, cc);

        add(card, c);
    }

    private void loginAction() {
        try (Connection conn = DB.getConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?");

            ps.setString(1, userField.getText().trim());
            ps.setString(2, new String(passField.getPassword()));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                LandingPanel.adminName = rs.getString("username");
                MainApp.setPanel(new LandingPanel());
            } else {
                JOptionPane.showMessageDialog(this, "Username atau password salah!");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}
