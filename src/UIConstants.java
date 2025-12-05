import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

public class UIConstants {

    // Colors
    public static final Color NAVY = new Color(0x0A2342);
    public static final Color DARK_GRAY = new Color(0x444444);
    public static final Color GRAY = new Color(0x888888);
    public static final Color LIGHT_GRAY = new Color(0xF3F3F3);
    public static final Color WHITE = Color.WHITE;
    public static final Color SUCCESS = new Color(0x2E8B57);
    public static final Color DANGER = new Color(0xC0392B);

    // Fonts
    public static final Font HEADER_FONT = new Font("Impact", Font.BOLD, 34);
    public static final Font SUBHEADER_FONT = new Font("SansSerif", Font.BOLD, 18);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BOLD_BODY_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 13);

    // Layout
    public static final int PADDING = 10;
    public static final int TABLE_ROW_HEIGHT = 28;
    public static final Dimension DEFAULT_BUTTON_SIZE = new Dimension(120, 36);

    // ------------------------
    // Basic style helpers
    // ------------------------

    // AWT Button (backward compatible)
    public static void stylePrimaryButton(java.awt.Button button) {
        button.setBackground(NAVY);
        button.setForeground(WHITE);
    }

    // Swing JButton - preferred for modern Swing panels
    public static void stylePrimaryButton(JButton button) {
        button.setBackground(NAVY);
        button.setForeground(WHITE);
        button.setFont(BOLD_BODY_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.setPreferredSize(DEFAULT_BUTTON_SIZE);
    }

    public static void styleSecondaryButton(JButton button) {
        button.setBackground(LIGHT_GRAY);
        button.setForeground(DARK_GRAY);
        button.setFont(BODY_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(GRAY, 1));
        button.setPreferredSize(DEFAULT_BUTTON_SIZE);
    }

    public static void styleDangerButton(JButton button) {
        button.setBackground(DANGER);
        button.setForeground(WHITE);
        button.setFont(BOLD_BODY_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    // JLabel helpers
    public static void applyHeaderLabel(JLabel label) {
        label.setFont(HEADER_FONT);
        label.setForeground(NAVY);
    }

    public static void applySubheaderLabel(JLabel label) {
        label.setFont(SUBHEADER_FONT);
        label.setForeground(DARK_GRAY);
    }

    public static void applyBodyLabel(JLabel label) {
        label.setFont(BODY_FONT);
        label.setForeground(DARK_GRAY);
    }

    // JPanel background helper
    public static void stylePanelBackground(JPanel panel) {
        panel.setBackground(LIGHT_GRAY);
    }

    // Apply padding (useful for containers)
    public static void applyPadding(JComponent comp, int padding) {
        comp.setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    public static void applyDefaultPadding(JComponent comp) {
        applyPadding(comp, PADDING);
    }

    // JTable helpers
    public static void styleTable(JTable table) {
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setFont(BODY_FONT);
        table.setForeground(DARK_GRAY);
        table.setGridColor(new Color(0xE6E6E6));
        table.setShowGrid(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(BOLD_BODY_FONT);
        header.setBackground(NAVY);
        header.setForeground(WHITE);
        header.setReorderingAllowed(false);
    }

    // Adjust column widths proportionally (simple helper)
    public static void setColumnWidths(JTable table, int... widths) {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // TextField helper
    public static void styleTextField(JTextField field) {
        field.setFont(BODY_FONT);
        field.setForeground(DARK_GRAY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    public static void stylePasswordField(JPasswordField field) {
        styleTextField(field);
    }

    // Card-like panel
    public static void styleCardPanel(JPanel panel) {
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE8E8E8)),
                new EmptyBorder(12, 12, 12, 12)
        ));
    }

    // Utility: center a component in a panel (convenience)
    public static void center(Component comp) {
        if (comp instanceof JComponent) {
            ((JComponent) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
            ((JComponent) comp).setAlignmentY(Component.CENTER_ALIGNMENT);
        }
    }

    // Centralized Currency Formatter ---
    public static String formatRupiah(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        // Format #,### (tanpa desimal di belakang koma untuk gaji umumnya, atau #,###.## jika perlu)
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return "Rp. " + df.format(value);
    }

    public static String capitalizeEachWord(String str) {
        if (str == null || str.isBlank()) return str;
        String[] parts = str.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}