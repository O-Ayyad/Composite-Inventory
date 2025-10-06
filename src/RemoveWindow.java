import javax.swing.*;
import java.awt.*;

public class RemoveWindow extends JDialog {
    public RemoveWindow(JFrame mainWindow, int width, int height) {
        super(mainWindow, "Remove Item", true);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(mainWindow);

        JLabel label = new JLabel("Remove item:");
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label);

        setVisible(true);
    }
}

