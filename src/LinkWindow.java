import javax.swing.*;
import java.awt.*;

public class LinkWindow extends JDialog {
    public LinkWindow(JFrame mainWindow, int width, int height) {
        super(mainWindow, "Link Account", true);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(mainWindow);

        JLabel label = new JLabel("Link your account here:");
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label);

        setVisible(true);
    }
}
