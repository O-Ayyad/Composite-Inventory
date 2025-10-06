import javax.swing.*;
import java.awt.*;

public class ItemWindow extends JDialog {
    public ItemWindow(JFrame mainWindow, int width, int height) {
        super(mainWindow, "View and Update Inventory", true);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(mainWindow);

        JLabel label = new JLabel("View items:");
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label);

        setVisible(true);
    }
}