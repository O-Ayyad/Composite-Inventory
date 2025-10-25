package gui;

import core.Inventory;
import core.Item;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicComboBoxEditor;

public abstract class SubWindow extends JDialog {
    public final int subWindowWidth = 1200;
    public final int subWindowHeight = 1000;
    public final String notFoundPNGPath = "icons/itemIcons/imageNotFound.png";
    Inventory inventory;

    public SubWindow(JFrame mainWindow, String name, Inventory inventory) {
        super(mainWindow, name, true);
        this.inventory = inventory;
        setSize(subWindowWidth, subWindowHeight);

        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        setLocation(mouseLocation.x - 10, mouseLocation.y -30);
    }
    public abstract void setupUI();


    //Creates a searchable dropdown menu of all items
    public DropdownResult getDropDownMenuAllItems() {
        JComboBox<String> itemDropdown = new JComboBox<>();
        itemDropdown.setEditable(true);

        Dimension fixedSize = new Dimension(200, 25);
        itemDropdown.setPreferredSize(fixedSize);
        itemDropdown.setMaximumSize(fixedSize);
        itemDropdown.setMinimumSize(fixedSize);

        itemDropdown.setEditor(new BasicComboBoxEditor() {
            private final JTextField tf = new JTextField();
            @Override public Component getEditorComponent() { return tf; }
            @Override public Object getItem() { return tf.getText(); }
            @Override public void setItem(Object anObject) {  }
        });
        itemDropdown.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

        JTextField editor = (JTextField) itemDropdown.getEditor().getEditorComponent();

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        itemDropdown.setModel(model);

        HashMap<String, String> displayToSerialMap = new HashMap<>();
        ArrayList<String> displayList = new ArrayList<>();

        for (String serial : inventory.SerialToItemMap.keySet()) {
            Item item = inventory.getItemBySerial(serial);
            if (item != null) {
                String display = item.getName() + " (" + serial + ")";
                displayList.add(display);
                displayToSerialMap.put(display, serial);
            }
        }

        // Populate initial items
        displayList.forEach(model::addElement);


        final boolean[] rebuilding = { false};
        final String[] selectedItem = { null};

        int delay = 150; // ms delay for typing
        Timer debounceTimer = new Timer(delay, e -> {
            String text = editor.getText().trim().toLowerCase();
            if (selectedItem[0] != null && !editor.getText().equals(selectedItem[0])) {
                selectedItem[0] = null;
            }

            rebuilding[0] = true;
            model.removeAllElements();

            if (text.isEmpty()) {
                // Show all items when text is cleared
                for (String display : displayList) {
                    if (!display.equals(selectedItem[0])) {
                        model.addElement(display);
                    }
                }
            } else {
                // Filter items based on name or serial
                for (String display : displayList) {
                    if (display.equals(selectedItem[0])) continue; // skip current selection
                    String serial = displayToSerialMap.get(display).toLowerCase();
                    if (display.toLowerCase().contains(text) || serial.contains(text)) {
                        model.addElement(display);
                    }
                }
            }

            rebuilding[0] = false;

            // Show popup only if there are matches
            if (model.getSize() > 0) {
                itemDropdown.showPopup();
            } else {
                itemDropdown.hidePopup();
            }
            SwingUtilities.invokeLater(() ->
                    editor.setCaretPosition(editor.getText().length())
            );
        });
        debounceTimer.setRepeats(false);

        // Restart timer when typing
        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restartTimer() {
                if (debounceTimer.isRunning()) debounceTimer.restart();
                else debounceTimer.start();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
        });

        itemDropdown.addActionListener(e -> {
            if (!rebuilding[0]) {
                Object selected = itemDropdown.getSelectedItem();
                if (selected != null) {
                    editor.setText(selected.toString());
                    editor.setCaretPosition(editor.getText().length());
                    itemDropdown.hidePopup();

                    String selectedDisplay = selected.toString();
                    selectedItem[0] = selectedDisplay;
                }
            }
        });

        editor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (model.getSize() > 0) itemDropdown.showPopup();
            }
        });
        SwingUtilities.invokeLater(() -> {
            editor.requestFocusInWindow();
            editor.selectAll();
        });
        return new DropdownResult(itemDropdown, displayToSerialMap);
    }
}
