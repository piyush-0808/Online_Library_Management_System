package library.management.system.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public final class UITheme {

    public static final Font TITLE_FONT =
            new Font("Segoe UI", Font.BOLD, 24);

    public static final Font SECTION_FONT =
            new Font("Segoe UI", Font.BOLD, 18);

    public static final Font LABEL_FONT =
            new Font("Segoe UI", Font.BOLD, 13);

    public static final Font NORMAL_FONT =
            new Font("Segoe UI", Font.PLAIN, 13);

    public static final Font SMALL_FONT =
            new Font("Segoe UI", Font.PLAIN, 12);

    public static final Color PRIMARY =
            new Color(45, 55, 72);

    public static final Color ACCENT =
            new Color(59, 130, 246);

    public static final Color SUCCESS =
            new Color(34, 197, 94);

    public static final Color WARNING =
            new Color(245, 158, 11);

    public static final Color DANGER =
            new Color(239, 68, 68);

    public static final Color BACKGROUND =
            new Color(245, 247, 250);

    public static final Color CARD =
            Color.WHITE;

    public static final Color TEXT =
            new Color(31, 41, 55);

    public static final Color MUTED_TEXT =
            new Color(107, 114, 128);

    public static final Color BORDER =
            new Color(226, 232, 240);

    private UITheme() {
    }

    public static void styleTextField(
            JTextField field) {

        field.setFont(NORMAL_FONT);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(
                                8, 10, 8, 10
                        )
                )
        );
    }

    public static void stylePasswordField(
            JPasswordField field) {

        field.setFont(NORMAL_FONT);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(
                                8, 10, 8, 10
                        )
                )
        );
    }

    public static void styleTextArea(
            JTextArea area) {

        area.setFont(NORMAL_FONT);
        area.setForeground(TEXT);
        area.setBackground(Color.WHITE);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(
                                8, 10, 8, 10
                        )
                )
        );
    }

    public static void styleComboBox(
            JComboBox<?> comboBox) {

        comboBox.setFont(NORMAL_FONT);
        comboBox.setForeground(TEXT);
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );
    }

    public static void styleButton(
            JButton button) {

        styleButton(
                button,
                ACCENT
        );
    }

    public static void styleButton(
            JButton button,
            Color background) {

        button.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(background, 1),
                        new EmptyBorder(9, 17, 9, 17)
                )
        );

        button.setCursor(
                new java.awt.Cursor(
                        java.awt.Cursor.HAND_CURSOR
                )
        );
    }

    public static void styleLabel(
            JLabel label) {

        label.setFont(NORMAL_FONT);
        label.setForeground(TEXT);
    }

    public static void styleFormLabel(
            JLabel label) {

        label.setFont(LABEL_FONT);
        label.setForeground(TEXT);
    }

    public static void styleTable(
            JTable table) {

        table.setFont(NORMAL_FONT);
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);

        table.setRowHeight(32);

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        table.setSelectionBackground(
                new Color(219, 234, 254)
        );

        table.setSelectionForeground(TEXT);

        table.setGridColor(BORDER);

        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable source,
                    Object value, boolean selected, boolean focused,
                    int row, int column) {
                Component cell = super.getTableCellRendererComponent(
                        source, value, selected, focused, row, column);
                if (!selected && value != null) {
                    String status = value.toString().trim().toUpperCase();
                    if (status.equals("AVAILABLE") || status.equals("READY")) {
                        cell.setBackground(new Color(220, 252, 231));
                    } else if (status.equals("ISSUED")) {
                        cell.setBackground(new Color(255, 237, 213));
                    } else if (status.equals("OVERDUE")) {
                        cell.setBackground(new Color(254, 226, 226));
                    } else {
                        cell.setBackground(Color.WHITE);
                    }
                } else if (!selected) {
                    cell.setBackground(Color.WHITE);
                }
                return cell;
            }
        });

        JTableHeader header =
                table.getTableHeader();

        header.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        13
                )
        );

        header.setForeground(Color.WHITE);
        header.setBackground(PRIMARY);
        header.setOpaque(true);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setBackground(PRIMARY);
        headerRenderer.setOpaque(true);
        header.setDefaultRenderer(headerRenderer);

        header.setReorderingAllowed(false);
    }

    public static JScrollPane styleScrollPane(
            JComponent component) {

        JScrollPane scrollPane =
                new JScrollPane(component);

        scrollPane.setBorder(
                BorderFactory.createLineBorder(BORDER)
        );

        scrollPane.getViewport()
                .setBackground(Color.WHITE);

        return scrollPane;
    }
}
