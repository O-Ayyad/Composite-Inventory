package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;


public class UIUtils {

    // =============== COLORS==================
    //Base Backgrounds
    public static final Color BACKGROUND_MAIN       = new Color(240, 240, 240);
    public static final Color BACKGROUND_PANEL      = new Color(220, 220, 240);

    //Text
    public static final Color TEXT_PRIMARY          = Color.BLACK;
    public static final Color TEXT_SECONDARY        = new Color(80, 80, 90);

    //Log Colors
    public static final Color NORMAL_COLOR          = new Color(220, 220, 235);
    public static final Color WARNING_COLOR         = new Color(255, 245, 180);
    public static final Color CRITICAL_COLOR        = new Color(240, 160, 165);
    public static final Color SUPPRESSED_COLOR      = new Color(200, 200, 200);
    //Borders
    public static final Color BORDER_LIGHT          = new Color(200, 200, 210);
    public static final Color BORDER_MEDIUM         = new Color(150, 150, 160);
    public static final Color BORDER_DARK           = new Color(100, 100, 110);

    public static final Color HEADER_BORDER         = new Color(200, 200, 235);

    //Selection
    public static final Color SELECTION_BG          = new Color(200, 210, 255);

    //Buttons
    public static final Color BUTTON_BG             = new Color(255, 255, 255);
    public static final Color BUTTON_HOVER          = new Color(210, 210, 230);


    //Link Colors
    public static final Color LINK_SUCCESS        = new Color(0, 128, 0);
    public static final Color UNLINKED            = new Color(180, 240, 180);
    public static final Color LINK_ERROR          = new Color(255, 240, 180);

    // ================ FONTS ==============

    public static final Font FONT_UI_SMALL       = new Font("Segoe UI Emoji", Font.PLAIN, 11);
    public static final Font FONT_UI_REGULAR     = new Font("Segoe UI Emoji", Font.PLAIN, 13);
    public static final Font FONT_UI_ITALIC      = new Font("Segoe UI Emoji", Font.ITALIC, 13);
    public static final Font FONT_UI_LARGE       = new Font("Segoe UI Emoji", Font.PLAIN, 16);
    public static final Font FONT_UI_BOLD        = new Font("Segoe UI Emoji", Font.BOLD, 13);
    public static final Font FONT_UI_LARGE_BOLD  = new Font("Segoe UI Emoji", Font.BOLD, 16);
    public static final Font FONT_UI_TITLE       = new Font("Segoe UI Emoji", Font.BOLD, 20);
    public static final Font FONT_UI_TITLE_LARGE = new Font("Segoe UI Emoji", Font.BOLD, 22);

    public static final Font FONT_ARIAL_SMALL = new Font("Arial", Font.PLAIN, 11);
    public static final Font FONT_ARIAL_REGULAR  = new Font("Arial", Font.PLAIN, 14);
    public static final Font FONT_ARIAL_LARGE    = new Font("Arial", Font.PLAIN, 14);
    public static final Font FONT_ARIAL_BOLD_MEDIUM     = new Font("Arial", Font.BOLD, 16);
    public static final Font FONT_ARIAL_BOLD     = new Font("Arial", Font.BOLD, 14);
    public static final Font FONT_ARIAL_LARGE_BOLD = new Font("Arial", Font.BOLD, 18);

    public static final Font DEBUG_CONSOLE_FONT  = new Font("Consolas", Font.PLAIN, 12);

    public static JButton styleButton(JButton button) {

        Color normalColor = BUTTON_BG;
        Color textColor = new Color(50, 50, 50);

        button.setBackground(normalColor);
        button.setForeground(textColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(normalColor);
            }
        });

        return button;
    }
    //Link buttons
    public static JButton createLinkButton(String text, String url, String iconPath) {
        JButton link;

        if (!iconPath.isEmpty()) {
            // Load and scale icon
            ImageIcon icon = new ImageIcon(iconPath);
            Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            link = new JButton("<HTML><U>" + text + "</U></HTML>", new ImageIcon(scaled));
        } else {
            link = new JButton("<HTML><U>" + text + "</U></HTML>");
        }

        link.setForeground(Color.BLUE);
        link.setBorderPainted(false);
        link.setOpaque(false);
        link.setBackground(null);
        link.setContentAreaFilled(false);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ignored) {

            }
        });
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { link.setForeground(new Color(0, 102, 204)); }
            @Override public void mouseExited(MouseEvent e) { link.setForeground(Color.BLUE); }
        });
        link.setFocusPainted(false);
        return link;
    }
    static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (getHgap() * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }
                        if (rowWidth != 0) rowWidth += getHgap();
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                addRow(dim, rowWidth, rowHeight);
                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + getVgap() * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }
}
