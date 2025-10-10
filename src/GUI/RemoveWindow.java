package GUI;

import javax.swing.*;

public class RemoveWindow extends SubWindow {
    public static String windowName = "Remove Item";
    public static String description = "Remove Items:";
    public RemoveWindow(JFrame mainWindow) {
        super(mainWindow, windowName, description);
    }
}

