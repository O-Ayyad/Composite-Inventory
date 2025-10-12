package gui;

import javax.swing.*;

public class SubWindow extends JDialog {
    public final int subWindowWidth = 800;
    public final int subWindowHeight = 600;

    public SubWindow(JFrame mainWindow, String name) {
        super(mainWindow, name, true);
        setLocationRelativeTo(mainWindow);
        setSize(subWindowWidth, subWindowHeight);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(mainWindow);

        setVisible(true);
    }
}
