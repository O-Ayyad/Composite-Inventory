import javax.swing.*;
import java.awt.*;

public class ItemWindow extends SubWindow {
    public static String windowName = "View Items and Update Inventory";
    public static String description = "View Items here";
    public ItemWindow(JFrame mainWindow) {
        super(mainWindow, windowName, description);
    }
}