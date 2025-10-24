package gui;
import java.util.*;
import javax.swing.*;

//Used to return a dropdown menu and a map of the item name(serial number), serial number to access the item it represents
// Break down into it's parts then destroy when created
public class DropdownResult {
    public final JComboBox<String> menu;
    public final HashMap<String, String> serialMap;

    public DropdownResult(JComboBox<String> comboBox, HashMap<String, String> serialMap) {
        this.menu = comboBox;
        this.serialMap = serialMap;
    }
}
