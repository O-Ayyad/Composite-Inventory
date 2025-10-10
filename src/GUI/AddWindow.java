package GUI;

import javax.swing.*;

public class AddWindow extends SubWindow {
    public static String windowName = "Add Item";
    public static String description = "Add Items:";
    public AddWindow(JFrame mainWindow) {
        super(mainWindow, windowName, description);
    }
}
