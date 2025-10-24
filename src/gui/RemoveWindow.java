package gui;
import core.*;
import javax.swing.*;

public class RemoveWindow extends SubWindow {
    public static String windowName = "Remove Item";
    public RemoveWindow(JFrame mainWindow, Inventory inventory) {
        super(mainWindow, windowName,inventory);
    }
    @Override
    public void setupUI(){

    }
}

