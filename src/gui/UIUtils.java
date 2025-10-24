package gui;

import javax.swing.*;
import java.awt.*;
public class UIUtils {

    public static JButton styleButton(JButton button) {

        Color normalColor = new Color(255, 255, 255);
        Color hoverColor = new Color(200, 200, 215);
        Color textColor = new Color(50, 50, 50);

        button.setBackground(normalColor);
        button.setForeground(textColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(normalColor);
            }
        });

        return button;
    }
}
