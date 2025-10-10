import javax.swing.*;
import java.awt.*;

public class SubWindow extends JDialog {
    public final int subWindowWidth = 800;
    public final int subWindowHeight = 600;

    public SubWindow(JFrame mainWindow, String name, String desc) {
        super(mainWindow, name, true);
        setLocationRelativeTo(mainWindow);
        setSize(subWindowWidth, subWindowHeight);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(mainWindow);

        JLabel label = new JLabel(desc, SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

        setVisible(true);
    }
}
