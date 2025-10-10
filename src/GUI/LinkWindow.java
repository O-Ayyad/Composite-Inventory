package GUI;

import javax.swing.*;

public class LinkWindow extends SubWindow {
    public static String windowName = "Link Account";
    public static String description = "Link your account here:";
    public LinkWindow(JFrame mainWindow) {
        super(mainWindow, windowName, description);
    }
}
