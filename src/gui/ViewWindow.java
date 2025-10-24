package gui;
import core.*;
import javax.swing.*;

public class ViewWindow extends SubWindow {
    public static String windowName = "View Items and Update Inventory";
    public ViewWindow(JFrame mainWindow, Inventory inventory) {

        super(mainWindow, windowName, inventory);
    }
    @Override
    public void setupUI(){

    }
}