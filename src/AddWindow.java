import javax.swing.*;
import java.awt.*;

public class AddWindow extends JDialog {
    public AddWindow(JFrame mainWindow, int width, int height) {
        super(mainWindow, "Add Item", true);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(mainWindow);

        JLabel label = new JLabel("Add new item here:");
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        add(label);

        setVisible(true);
    }
}
