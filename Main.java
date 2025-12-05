import java.awt.*;
import javax.swing.*;

public class Main {

    private static JFrame frame;

    public static void main(String[] args) {
  
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Payroll System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen

            setPanel(new LoginPanel());

            frame.setVisible(true);
        });
    }

    public static void setPanel(JPanel panel) {
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    public static void setPanel(JPanel panel, String title) {
        frame.setTitle("Payroll System - " + title);
        setPanel(panel);
    }
}
