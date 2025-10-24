package gui;
import core.*;
import javax.swing.*;

public class LinkWindow extends SubWindow {
    public static String windowName = "Link Account and Items";
    public LinkWindow(JFrame mainWindow, Inventory inventory) {

        super(mainWindow, windowName, inventory);
    }
    @Override
    public void setupUI(){

    }
}
