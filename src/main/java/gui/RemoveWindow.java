package gui;
import core.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class RemoveWindow extends SubWindow {
    public static String windowName = "Remove Item";
    public enum SendTo{
        Break,
        Delete,
        Reduce,
    }
    public Item selected;
    public RemoveWindow(MainWindow mainWindow, Inventory inventory,SendTo send) {
        this(mainWindow, inventory, null,send); //Delegate to unified constructor
    }

    public RemoveWindow(MainWindow mainWindow, Inventory inventory, Item selected, SendTo  send) {
        super(mainWindow, windowName,inventory);
        if (inventory.SerialToItemMap.isEmpty()) {
            JOptionPane.showMessageDialog(mainWindow,
                    "Inventory is empty.",
                    "Inventory Empty",
                    JOptionPane.WARNING_MESSAGE);
            dispose();
            mainWindow.destroyAllExistingInstance(this.getClass());
            return;
        }

        setupUI(selected,send);
    }

    @Override
    public void setupUI() {
        setupUI(null,SendTo.Reduce);
    }

    public void setupUI(Item selected,SendTo send) {
        JPanel mainPanel;
        this.selected = selected;
        switch(send){
            case Delete -> mainPanel = deleteItemPanel();
            case Break -> mainPanel = breakDownPanel();
            default -> mainPanel = reduceStockPanel();
        }
        add(mainPanel, BorderLayout.CENTER);
        pack();
    }
    public JPanel reduceStockPanel(){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Reduce Stock");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(UIUtils.FONT_ARIAL_BOLD);
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
        JComboBox<String> itemDropdown = DDRObj.menu();
        Map<String,String> displayToSerialMap = DDRObj.serialMap();
        panel.add(itemDropdown, gbc);

        if (selected != null) {
            String serial = selected.getSerial();
            for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                if (entry.getValue().equals(serial)) {
                    itemDropdown.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }
        itemDropdown.addActionListener(e -> {
            String selectedDisplay = (String) itemDropdown.getSelectedItem();
            if (selectedDisplay == null) return;
            String selectedSerial = displayToSerialMap.get(selectedDisplay);
            if (selectedSerial == null) return;
            Item newItem = inventory.getItemBySerial(selectedSerial);
            if (newItem == null) return;

            selected = newItem;
        });

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
        JButton removeItemButton = new JButton("Delete Item");
        JButton breakDownButton = new JButton("Break Down Item");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(reduceButton));
        buttonRow.add(UIUtils.styleButton(removeItemButton));
        buttonRow.add(UIUtils.styleButton(breakDownButton));
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
                JOptionPane.showMessageDialog(this, target.getName()+" is out of stock", "Error", JOptionPane.ERROR_MESSAGE);
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

            checkReserved(target,amount);

            int response = JOptionPane.showConfirmDialog(this,
                    String.format("Are you sure you want to reduce stock of '%s' by %d units?", target.getName(), amount),
                    "Confirm Stock Reduction",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if(response != JOptionPane.YES_OPTION){
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
            add(deleteItemPanel(), BorderLayout.CENTER);

            revalidate();
            repaint();
            pack();
        });
        breakDownButton.addActionListener(e -> {
            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);
            Item currSelected = inventory.SerialToItemMap.get(selectedSerial);
            if (currSelected != null){
                if (!currSelected.isComposite()){
                    currSelected = null;
                }
            }
            selected = currSelected;

            getContentPane().removeAll();
            add(breakDownPanel(), BorderLayout.CENTER);
            revalidate();
            pack();
        });
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }
    public JPanel deleteItemPanel(){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Delete Item");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(UIUtils.FONT_ARIAL_BOLD);
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
        JComboBox<String> itemDropdown = DDRObj.menu();
        Map<String,String> displayToSerialMap = DDRObj.serialMap();
        panel.add(itemDropdown, gbc);

        if (selected != null) {
            String serial = selected.getSerial();
            for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                if (entry.getValue().equals(serial)) {
                    itemDropdown.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }
        itemDropdown.addActionListener(e -> {
            String selectedDisplay = (String) itemDropdown.getSelectedItem();
            if (selectedDisplay == null) return;
            String selectedSerial = displayToSerialMap.get(selectedDisplay);
            if (selectedSerial == null) return;
            Item newItem = inventory.getItemBySerial(selectedSerial);
            if (newItem == null) return;

            selected = newItem;
        });

        //Remove buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton deleteButton = new JButton("Delete Item");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(deleteButton));
        buttonRow.add(UIUtils.styleButton(cancelButton));
        panel.add(buttonRow, gbc);

        panel.registerKeyboardAction(
                e -> {if (!((String) itemDropdown.getEditor().getItem()).isEmpty()) {
                    deleteButton.doClick();
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
        deleteButton.addActionListener(e ->{
            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);
            Item target = inventory.SerialToItemMap.get(selectedSerial);

            if (selectedItem == null || selectedItem.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter or select an item to delete.",
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
            confirmRemoveItem(target);
        });

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    public JPanel breakDownPanel() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        //Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Break Down Composite Item");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(UIUtils.FONT_ARIAL_BOLD);
        top.add(info);
        mainPanel.add(top, BorderLayout.NORTH);

        //Main panel
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        //Dropdown
        panel.add(new JLabel("Select Composite Item:"), gbc);
        gbc.gridx = 1;

        DropdownResult DDRObj = getDropDownMenuCompositeItems();
        JComboBox<String> itemDropdown = DDRObj.menu();
        Map<String,String> displayToSerialMap = DDRObj.serialMap();
        panel.add(itemDropdown, gbc);

        //Remove null and non-composite items
        if (selected != null) {
            if(selected.isComposite()){
                String serial = selected.getSerial();
                for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                    if (entry.getValue().equals(serial)) {
                        itemDropdown.setSelectedItem(entry.getKey());
                        break;
                    }
                }
            }
        }


        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Amount to break:"), gbc);
        gbc.gridx = 1;
        JTextField amountBox = new JTextField(10);
        amountBox.setRequestFocusEnabled(false);
        amountBox.setHorizontalAlignment(JTextField.LEFT);
        panel.add(amountBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;

        JPanel componentPanel = new JPanel(new GridBagLayout());
        TitledBorder tableBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                "Used components",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        componentPanel.setBorder(tableBorder);
        JScrollPane compScroll = new JScrollPane(componentPanel);
        compScroll.getHorizontalScrollBar().setUnitIncrement(20);
        compScroll.setPreferredSize(new Dimension(400,200));
        panel.add(compScroll, gbc);

        JTextArea disclaimer = new JTextArea("These are items that were used in composition.\n" +
                "If no items were used, then leave all fields as 0.");
        disclaimer.setFont(new Font("Arial", Font.PLAIN, 12));
        disclaimer.setForeground(Color.GRAY);
        disclaimer.setWrapStyleWord(true);
        disclaimer.setLineWrap(true);
        disclaimer.setEditable(false);


        Map<Map.Entry<Item,Integer>, JTextField> componentFields = new HashMap<>();

        // Buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton breakButton = new JButton("Break Down");
        JButton cancelButton = new JButton("Return to Reduce Stock");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(breakButton));
        buttonRow.add(UIUtils.styleButton(cancelButton));
        panel.add(buttonRow, gbc);

        amountBox.getDocument().addDocumentListener(new DocumentListener() {
            public void update(){
                updateUsedComponent(itemDropdown,amountBox,componentPanel,componentFields,displayToSerialMap,breakButton);
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        //Add components to reclaim
        itemDropdown.addActionListener(e -> {
            updateUsedComponent(itemDropdown,amountBox,componentPanel,componentFields,displayToSerialMap,breakButton);
        });

        breakButton.addActionListener(e -> {



            String display = (String) itemDropdown.getEditor().getItem();
            String serial = displayToSerialMap.get(display);
            Item target = inventory.SerialToItemMap.get(serial);
            Integer amountToBreak = getAmountFromTextBox(amountBox);

            if(amountToBreak == null){
                JOptionPane.showMessageDialog(this,"Please input a valid amount", "Invalid amount",JOptionPane.ERROR_MESSAGE);
                return;
            }


            if (target == null || !target.isComposite()) {
                JOptionPane.showMessageDialog(this,
                        "Please select a valid composite item.",
                        "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int parentItemQuant = inventory.getQuantity(target);
            if (parentItemQuant < amountToBreak) {
                JOptionPane.showMessageDialog(this,
                        target.getName() + " does not have sufficient stock. (Amount in stock: "+ parentItemQuant+")",
                        "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }

            checkReserved(target,amountToBreak);

            String targetNamePlural = target.getName();
            if(amountToBreak > 2){
                targetNamePlural = getPlural(targetNamePlural);
            }
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to break down "+amountToBreak +" " + targetNamePlural + "?",
                    "Confirm Breakdown",
                    JOptionPane.YES_NO_OPTION
            );
            if(confirm != JOptionPane.YES_OPTION){
                return;
            }
            //Validate reclaim amount
            Map<Item,Integer> usedComponents = new HashMap<>();

            for (Map.Entry<Map.Entry<Item,Integer>, JTextField> entry : componentFields.entrySet()) {

                Item i = entry.getKey().getKey();
                int amount = entry.getKey().getValue() * amountToBreak;
                JTextField tf = entry.getValue();

                try {
                    int val = Integer.parseInt(tf.getText().trim());

                    if (val < 0) {
                        JOptionPane.showMessageDialog(this,
                                "Negative values are not allowed.",
                                "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (val > amount) {
                        JOptionPane.showMessageDialog(this,
                                "You cannot use " + val +
                                        " of " + i.getName() +
                                        ". Maximum is " + amount,
                                "Too Many", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (val > 0) {
                        usedComponents.put(i, val);
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid number for " + i.getName(),
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            inventory.breakDownItem(target, usedComponents,amountToBreak);

            JOptionPane.showMessageDialog(this,
                    amountToBreak  + " " + targetNamePlural+ " broken down successfully.\n",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        cancelButton.addActionListener(e -> {
            getContentPane().removeAll();
            add(reduceStockPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });


        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
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
            String plural = getPlural(name);
            reduceButtonText = (amount > 1)
                    ? "Remove " + amount + " " + plural
                    : (amount <= 0 ? "Remove amount to " + name : "Remove " + amount + " " + name);
        }
        return reduceButtonText;
    }
    void updateReduceButtonText(JButton removeButton,
                                JComboBox<String> itemDropdown,
                                Map<String,String> displayToSerialMap,
                                JTextField textField){
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

    void updateBreakDownButtonText(JButton breakButton, Item target, int amount) {
        String breakButtonText;

        if (target == null) {
            breakButtonText = (amount > 0)
                    ? "Break down " + amount + " of this item"
                    : "Break down amount of this item";
        } else {
            String name = target.getName();
            String plural = getPlural(name);
            breakButtonText = (amount > 1)
                    ? "Break down " + amount + " " + plural
                    : (amount <= 0 ? "Break down amount of " + name
                    : "Break down " + amount + " " + name);
        }

        breakButton.setText(breakButtonText);
    }
    Integer getAmountFromTextBox(JTextField textField) {
        String text = textField.getText().trim();

        try {
            int amount = Integer.parseInt(text);
            if(amount <= 0){
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    void updateUsedComponent(
            JComboBox<String> itemDropdown,

            JTextField amountBox,

            JPanel componentPanel,

            Map<Map.Entry<Item, Integer>, JTextField> componentFields,

            Map<String, String> displayToSerialMap,
            JButton breakButton
            ){

        itemDropdown.hidePopup();


        JTextField editor = (JTextField) itemDropdown.getEditor().getEditorComponent();
        if (editor.hasFocus() && !editor.getText().trim().isEmpty()) {
            itemDropdown.showPopup();
        } else {
            itemDropdown.hidePopup();
        }

        componentFields.clear();
        componentPanel.removeAll();

        String display = (String) itemDropdown.getEditor().getItem();
        String serial = displayToSerialMap.get(display);
        Item target = inventory.SerialToItemMap.get(serial);


        if (target == null || !target.isComposite()) {
            componentFields.clear();
            componentPanel.removeAll();
            componentPanel.repaint();
            return;
        }
        Integer amount = getAmountFromTextBox(amountBox);

        if(amount == null){
            componentFields.clear();
            componentPanel.removeAll();
            componentPanel.repaint();
            updateBreakDownButtonText(breakButton, target, 0);
            return;
        }

        updateBreakDownButtonText(breakButton, target, amount);

        int row = 0;
        for (Map.Entry<Item,Integer> ip : target.getComposedOf().entrySet()) {
            GridBagConstraints cgbc = new GridBagConstraints();
            cgbc.insets = new Insets(4, 4, 4, 4);
            cgbc.anchor = GridBagConstraints.WEST;

            //component
            cgbc.gridx = 0;
            cgbc.gridy = row;
            JLabel lbl = new JLabel(ip.getKey().getName() +
                    " (Max: " + ip.getValue() * amount  + ")");
            componentPanel.add(lbl, cgbc);

            //Input fields
            cgbc.gridx = 1;
            JTextField field = new JTextField(5);
            field.setText("0");
            componentPanel.add(field, cgbc);

            componentFields.put(ip, field);
            row++;
        }

        componentPanel.revalidate();
        componentPanel.repaint();
    }

    void checkReserved(Item target, int amountToUse){
        int inStock = inventory.getQuantity(target);
        int inStockAfterAction = inStock - amountToUse;

        int availableStock = inventory.getAvailableQuantity(target);

        if (inStockAfterAction > availableStock) {
            String message = """
                            <html>
                            <b>Warning:</b><br><br>
                            This action will make some non-shipped orders <b>unfulfillable</b>.<br>
                            It will lower the stock of the reserved item.<br>
                            You may still proceed but this will impact any existing orders<br><br>
                            Please review your stock levels before proceeding.
                            </html>
                            """;

            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Reserved Item Warning",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }
}

