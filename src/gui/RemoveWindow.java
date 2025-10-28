package gui;
import core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Map;

public class RemoveWindow extends SubWindow {
    public static String windowName = "Remove Item";
    public RemoveWindow(JFrame mainWindow, Inventory inventory) {
        super(mainWindow, windowName,inventory);
        if (inventory.SerialToItemMap.isEmpty()) {
            JOptionPane.showMessageDialog(mainWindow,
                    "Inventory is empty.",
                    "Inventory Empty",
                    JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }
        setupUI();
        setVisible(true);
    }

    @Override
    public void setupUI(){
        JPanel mainPanel = reduceStockPanel();
        add(mainPanel, BorderLayout.CENTER);
        pack();
    }
    public JPanel reduceStockPanel(){
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

        //Spinner
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Amount to reduce:"), gbc);
        gbc.gridx = 1;
        JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(0, -10000000, 1000000, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) amountSpinner.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

        panel.add(amountSpinner, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton reduceButton = new JButton("Reduce Stock");
        JButton removeItemButton = new JButton("Remove an item completely");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(reduceButton));
        buttonRow.add(UIUtils.styleButton(removeItemButton));
        panel.add(buttonRow, gbc);


        panel.registerKeyboardAction(
                e -> {if((int)amountSpinner.getValue() >0) {
                    reduceButton.doClick();
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

        amountSpinner.addChangeListener(e -> updateReduceButtonText(reduceButton,itemDropdown,displayToSerialMap,amountSpinner));
        reduceButton.addActionListener(e ->{

            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);
            int amount = Math.abs((int)amountSpinner.getValue());


            updateReduceButtonText(reduceButton,itemDropdown,displayToSerialMap,amountSpinner);


            if (amount == 0) {
                JOptionPane.showMessageDialog(this, "Amount cannot be 0.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
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
            add(removeItemPanel(), BorderLayout.CENTER);

            revalidate();
            repaint();
            pack();
        });

        JScrollPane scrollPane = new JScrollPane(panel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(550, 400));
        return mainPanel;
    }
    public JPanel removeItemPanel(){
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

            String confirm = JOptionPane.showInputDialog(
                    this,
                    "Type the serial number to permanently delete:\n" + serial +
                            "\n\nThis action cannot be undone! All data about this item will be lost!\n" +
                            "This will also remove all logs associated with this item!",
                    "Confirm Deletion",
                    JOptionPane.WARNING_MESSAGE
            );
            if (!confirm.equals(serial)) {
                JOptionPane.showMessageDialog(this, "Serial numbers do not match. Item not removed.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JPanel confirmPanel = new JPanel(new BorderLayout(10, 10));
            JLabel message = new JLabel(
                    "Delete \"" + target.getName() + "\" (Serial: " + serial + ")?\n\n" +
                            "This CANNOT be undone and will delete all logs.\n\n" +
                            "Type CONFIRM below to continue:"
            );
            JTextField inputField = new JTextField(10);

            confirmPanel.add(message, BorderLayout.NORTH);
            confirmPanel.add(inputField, BorderLayout.CENTER);

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    confirmPanel,
                    "Final Confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (!(choice == JOptionPane.YES_OPTION && "CONFIRM".equals(inputField.getText().trim()))) {
                JOptionPane.showMessageDialog(
                        this,
                        "You must type CONFIRM to proceed with deletion.",
                        "Deletion Canceled",
                        JOptionPane.WARNING_MESSAGE
                );
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
            reduceButtonText = "Remove " + amount + " of this item";
        } else {
            reduceButtonText = formatItemCount(amount, target.getName(), "Remove");
        }
        return reduceButtonText;
    }
    void updateReduceButtonText(JButton addButton,JComboBox<String> itemDropdown, Map<String,String> displayToSerialMap, JSpinner amountSpinner){
        String currentItem = (String) itemDropdown.getEditor().getItem();
        String currentSerial = displayToSerialMap.get(currentItem);
        int currentAmount = (int) amountSpinner.getValue();
        Item currentTarget = inventory.SerialToItemMap.get(currentSerial);

        addButton.setText(updateReduceText(currentTarget, currentAmount));
    }
}

