package gui;

import core.Inventory;
import core.Item;

import java.awt.*;
import java.util.*;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicComboBoxEditor;

public abstract class SubWindow extends JDialog {
    public final String notFoundPNGPath = "icons/itemIcons/imageNotFound.png";
    Inventory inventory;

    public SubWindow(JFrame mainWindow, String name, Inventory inventory) {
        super(mainWindow, name, true);
        this.inventory = inventory;
        setLocationRelativeTo(mainWindow);
    }
    public abstract void setupUI();

    protected String formatItemCount(int count, String itemName, String action) {
        return String.format("%s %d × %s", action, count, itemName);
    }

    protected ImageIcon getItemIcon(Item item, int width, int height) {
        ImageIcon itemIcon = item.getIcon(width,height);
        if(itemIcon != null){
            return itemIcon;
        }
        String iconPath = item.getImagePath();
        if (iconPath == null || iconPath.isEmpty()) {
            iconPath = notFoundPNGPath;
        }

        ImageIcon icon = new ImageIcon(iconPath);
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    //Creates a searchable dropdown menu of all items
    public DropdownResult getDropDownMenuAllItems() {
        return createFilteredDropdown(item -> true); //include all items
    }

    public DropdownResult getDropDownMenuCompositeItems() {
        return createFilteredDropdown(Item::isComposite); //include only composites
    }
    //Returns true if user deleted the item
    public boolean confirmRemoveItem(Item target){
        if (!target.getComposesInto().isEmpty()) {
            StringBuilder composeList = new StringBuilder();
            for(Item i : target.getComposesInto()){
                composeList.append(i.getName()).append("\n");
            }
            String composeListStr = composeList.toString();
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Warning: This item is used as a component in other composite items.\n" +
                            "Removing it may affect those compositions of the following item(s):\n\n" + composeListStr +
                            "\n\nProceed anyway?",
                    "Composition Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return false;
        }
        //Confirmation
        String serial = target.getSerialNum();

        String inSerial = JOptionPane.showInputDialog(
                this,
                "Type the serial number to permanently delete:\n" + serial +
                        "\n\nThis action cannot be undone! All data about this item will be lost!\n" +
                        "This will also remove all logs associated with this item!",
                "Confirm Deletion",
                JOptionPane.WARNING_MESSAGE
        );
        if (!inSerial.equals(serial)) {
            JOptionPane.showMessageDialog(this, "Serial numbers do not match. Item not removed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String confirmWord = JOptionPane.showInputDialog(
                this,
                "Delete \"" + target.getName() + "\" (Serial: " + serial + ")?\n\n" +
                        "This CANNOT be undone and will delete all logs.\n\n" +
                        "Type CONFIRM below to continue:",
                "Confirm delete " + target.getName(),
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmWord == null || !confirmWord.trim().equalsIgnoreCase("CONFIRM")) {
            JOptionPane.showMessageDialog(
                    this,
                    "You must type CONFIRM to proceed with deletion.",
                    "Deletion Canceled",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you absolutely sure you want to delete this item?\n\nItem Serial: " + serial,
                "Final Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Deletion canceled.", "Canceled", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        //Success
        try {
            inventory.removeItem(target);
            JOptionPane.showMessageDialog(
                    this,
                    "Successfully removed " + target.getName() + " (Serial: " + serial + ")",
                    "Item Removed",
                    JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to remove item: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        return true;
    }
    private DropdownResult createFilteredDropdown(Predicate<Item> itemFilter) {
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
            @Override public void setItem(Object anObject) { }
        });
        itemDropdown.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

        JTextField editor = (JTextField) itemDropdown.getEditor().getEditorComponent();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        itemDropdown.setModel(model);

        HashMap<String, String> displayToSerialMap = new HashMap<>();
        ArrayList<String> displayList = new ArrayList<>();

        for (String serial : inventory.SerialToItemMap.keySet()) {
            Item item = inventory.getItemBySerial(serial);
            if (item != null && itemFilter.test(item)) {
                String display = item.getName() + " (" + serial + ")";
                displayList.add(display);
                displayToSerialMap.put(display, serial);
            }
        }

        displayList.forEach(model::addElement);

        final boolean[] rebuilding = { false };
        final String[] selectedItem = { null };

        int delay = 150;
        Timer debounceTimer = new Timer(delay, e -> {
            String text = editor.getText().trim().toLowerCase();
            if (selectedItem[0] != null && !editor.getText().equals(selectedItem[0])) {
                selectedItem[0] = null;
            }

            rebuilding[0] = true;
            model.removeAllElements();

            if (text.isEmpty()) {
                displayList.stream()
                        .filter(d -> !d.equals(selectedItem[0]))
                        .forEach(model::addElement);
            } else {
                displayList.stream()
                        .filter(d -> !d.equals(selectedItem[0]))
                        .filter(d -> d.toLowerCase().contains(text) ||
                                displayToSerialMap.get(d).toLowerCase().contains(text))
                        .forEach(model::addElement);
            }

            rebuilding[0] = false;
            if (model.getSize() > 0) itemDropdown.showPopup();
            else itemDropdown.hidePopup();

            SwingUtilities.invokeLater(() -> editor.setCaretPosition(editor.getText().length()));
        });
        debounceTimer.setRepeats(false);

        //Typing debounce
        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restartTimer() {
                if (debounceTimer.isRunning()) debounceTimer.restart();
                else debounceTimer.start();
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
        });

        //Handle selection
        itemDropdown.addActionListener(e -> {
            if (!rebuilding[0]) {
                Object selected = itemDropdown.getSelectedItem();
                if (selected != null) {
                    editor.setText(selected.toString());
                    editor.setCaretPosition(editor.getText().length());
                    itemDropdown.hidePopup();
                    selectedItem[0] = selected.toString();
                }
            }
        });

        //Focus behavior
        editor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (model.getSize() > 0) itemDropdown.showPopup();
            }
        });

        SwingUtilities.invokeLater(() -> {
            editor.requestFocusInWindow();
            editor.selectAll();
        });

        return new DropdownResult(itemDropdown, displayToSerialMap, debounceTimer);
    }
}
