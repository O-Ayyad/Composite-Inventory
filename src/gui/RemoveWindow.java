package gui;
import core.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Map;

public class RemoveWindow extends SubWindow {
    public static String windowName = "Remove Item";

    public RemoveWindow(JFrame mainWindow, Inventory inventory,boolean sendToRemoveItem) {
        this(mainWindow, inventory, null,sendToRemoveItem); //Delegate to unified constructor
    }

    public RemoveWindow(JFrame mainWindow, Inventory inventory, Item selected, boolean sendToRemoveItem) {
        super(mainWindow, windowName,inventory);
        if (inventory.SerialToItemMap.isEmpty()) {
            JOptionPane.showMessageDialog(mainWindow,
                    "Inventory is empty.",
                    "Inventory Empty",
                    JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        setupUI(selected,sendToRemoveItem);
        setVisible(true);
    }

    @Override
    public void setupUI() {
        setupUI(null,false);
    }

    public void setupUI(Item selected,boolean sendToRemoveItem) {
        JPanel mainPanel;
        if(sendToRemoveItem){
            mainPanel = removeItemPanel(selected);
        }else{
            mainPanel = reduceStockPanel(selected);
        }
        add(mainPanel, BorderLayout.CENTER);
        pack();
    }
    public JPanel reduceStockPanel(Item selected){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Reduce Stock or Completely Remove Item");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(new Font("Arial", Font.BOLD, 14));
        top.add(info);
        mainPanel.add(top, BorderLayout.NORTH);

        //Create panel
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        //Dropdown box
        panel.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 1;
        DropdownResult DDRObj = getDropDownMenuAllItems();
        JComboBox<String> itemDropdown = DDRObj.menu;
        Map<String,String> displayToSerialMap = DDRObj.serialMap;
        panel.add(itemDropdown, gbc);

        if (selected != null) {
            String serial = selected.getSerialNum();
            for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                if (entry.getValue().equals(serial)) {
                    itemDropdown.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }

        //Reduce text
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Amount to reduce:"), gbc);
        gbc.gridx = 1;
        JTextField amountField = new JTextField(10); // width ~10 columns
        amountField.setHorizontalAlignment(JTextField.LEFT);
        panel.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton reduceButton = new JButton("Reduce Stock");
        JButton removeItemButton = new JButton("Remove an item completely");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(reduceButton));
        buttonRow.add(UIUtils.styleButton(removeItemButton));
        panel.add(buttonRow, gbc);

        amountField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                updateReduceButtonText(reduceButton, itemDropdown, displayToSerialMap, amountField);
            }

            @Override
            public void insertUpdate(DocumentEvent e) { update(); }

            @Override
            public void removeUpdate(DocumentEvent e) { update(); }

            @Override
            public void changedUpdate(DocumentEvent e) { update(); }
        });


        panel.registerKeyboardAction(
                e -> {
                    try {
                        int amount = Integer.parseInt(amountField.getText().trim());
                        if (amount > 0) {
                            reduceButton.doClick();
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(panel, "Please enter a valid number.",
                                "Invalid Input", JOptionPane.WARNING_MESSAGE);
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        panel.registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        reduceButton.addActionListener(e ->{

            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);

            int amount = 0;

            String text = amountField.getText().trim();
            if (!text.isEmpty()) {
                try {
                    amount = Math.abs(Integer.parseInt(text));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a valid number.",
                            "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            updateReduceButtonText(reduceButton, itemDropdown, displayToSerialMap, amountField);

            if (amount == 0) {
                JOptionPane.showMessageDialog(this,
                        "Amount cannot be 0.",
                        "Invalid amount",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedSerial == null || !inventory.SerialToItemMap.containsKey(selectedSerial)) {
                JOptionPane.showMessageDialog(this, "Please select a valid item.", "Invalid item", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Item target = inventory.SerialToItemMap.get(selectedSerial);
            if (target == null){
                JOptionPane.showMessageDialog(this, "Error: Item could not be accessed.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int currentStock = inventory.getQuantity(target);
            if (currentStock == 0){
                JOptionPane.showMessageDialog(this, "Item stock is already 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(amount > currentStock){
                JOptionPane.showMessageDialog(this,
                        String.format("Cannot reduce stock by %d.\nCurrent stock: %d\nMaximum you can reduce: %d",
                                amount, currentStock, currentStock),
                        "Insufficient Stock",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Success
            try {
                inventory.decreaseItemAmount(target, amount);
                JOptionPane.showMessageDialog(this,
                        String.format("Reduced stock of %s by %d (Serial: %s)\nNew Stock: %d",
                                target.getName(), amount, selectedSerial, inventory.getQuantity(target)),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to reduce item amount: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        removeItemButton.addActionListener(e -> {
            // Replace current content with remove item panel
            getContentPane().removeAll();
            //Create panel
            add(removeItemPanel(null), BorderLayout.CENTER);

            revalidate();
            repaint();
            pack();
        });

        JScrollPane scrollPane = new JScrollPane(panel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(550, 400));
        return mainPanel;
    }
    public JPanel removeItemPanel(Item selected){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Remove Item");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(new Font("Arial", Font.BOLD, 14));
        top.add(info);
        mainPanel.add(top, BorderLayout.NORTH);

        //Create panel
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.setPreferredSize(new Dimension(500, 400));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        //Dropdown box
        panel.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 1;
        DropdownResult DDRObj = getDropDownMenuAllItems();
        JComboBox<String> itemDropdown = DDRObj.menu;
        Map<String,String> displayToSerialMap = DDRObj.serialMap;
        panel.add(itemDropdown, gbc);

        if (selected != null) {
            String serial = selected.getSerialNum();
            for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                if (entry.getValue().equals(serial)) {
                    itemDropdown.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }

        //Remove buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton removeButton = new JButton("Remove Item");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(removeButton));
        buttonRow.add(UIUtils.styleButton(cancelButton));
        panel.add(buttonRow, gbc);

        panel.registerKeyboardAction(
                e -> {if (!((String) itemDropdown.getEditor().getItem()).isEmpty()) {
                    removeButton.doClick();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        panel.registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        cancelButton.addActionListener(e-> dispose());
        removeButton.addActionListener(e ->{
            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);
            Item target = inventory.SerialToItemMap.get(selectedSerial);

            if (selectedItem == null || selectedItem.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter or select an item to remove.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            if (selectedSerial == null) {
                JOptionPane.showMessageDialog(this,
                        "Invalid choice: no such item found in the dropdown list.",
                        "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (target == null) {
                JOptionPane.showMessageDialog(this,
                        "The selected item no longer exists in the inventory.",
                        "Item Not Found",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!target.getComposesInto().isEmpty()) {
                StringBuilder composeList = new StringBuilder();
                for(Item i : target.getComposesInto()){
                    composeList.append(i.getName()).append("\n");
                }
                String composeListStr = composeList.toString();
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Warning: This item is used as a component in other composite items.\n" +
                                "Removing it may affect those compositions of the following item(s):" + composeListStr +
                                "\n\nProceed anyway?",
                        "Composition Warning",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) return;
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
                return;
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
                return;
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
                return;
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
            }
        });

        JScrollPane scrollPane = new JScrollPane(panel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(400, 300));
        return mainPanel;
    }
    String updateReduceText( Item target, int amount){
        String reduceButtonText;
        if (target == null) {
            reduceButtonText = (amount > 0)
                    ? "Remove " + amount + " of this item"
                    : "Remove amount to this item";
        } else {
            String name = target.getName();
            String plural = name.endsWith("s") ? name + "es" : name + "s";
            reduceButtonText = (amount > 1)
                    ? "Remove " + amount + " " + plural
                    : (amount <= 0 ? "Remove amount to " + name : "Remove " + amount + " " + name);
        }
        return reduceButtonText;
    }
    void updateReduceButtonText(JButton removeButton,JComboBox<String> itemDropdown, Map<String,String> displayToSerialMap, JTextField textField){
        String currentItem = (String) itemDropdown.getEditor().getItem();
        String currentSerial = displayToSerialMap.get(currentItem);
        String quantityText = textField.getText().trim();

        int currentAmount = 0; // default when empty or invalid
        if (!quantityText.isEmpty()) {
            try {
                currentAmount = Integer.parseInt(quantityText);
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        Item currentTarget = inventory.SerialToItemMap.get(currentSerial);
        removeButton.setText(updateReduceText(currentTarget, currentAmount));
    }
}

