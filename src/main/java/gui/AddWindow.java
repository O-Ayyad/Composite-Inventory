package gui;
import constants.Constants;
import core.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;


public class AddWindow extends SubWindow {
    public static String windowName = "Add Item";
    private final Map<String, Integer> composedComponents = new LinkedHashMap<>();
    public Item selected;

    public AddWindow(MainWindow mainWindow, Inventory inventory) {
        this(mainWindow,inventory,null,false); //Delegate to unified constructor
    }
    public AddWindow(MainWindow mainWindow, Inventory inventory,Item selected,boolean compose) {
        super(mainWindow, windowName,inventory);
        setupUI(selected,compose);
        setVisible(true);
    }
    @Override
    public void setupUI() {
        setupUI(null,false);
    }
    public void setupUI(Item selected, boolean compose) {
        JPanel mainPanel;
        this.selected = selected;
        if (inventory.MainInventory.isEmpty()) {
            mainPanel = createNewItemPanel();
        }
        else{
            if(compose){
                mainPanel = composeItemPanel();
            }
            else {
                mainPanel = addToExistingItemPanel();
            }
        }

        add(mainPanel, BorderLayout.CENTER);
        pack();
    }
    public JPanel createNewItemPanel(){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Create new Item");
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

        //Item Name
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        panel.add(nameField, gbc);


        //Item Serial
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Item Serial:"), gbc);
        gbc.gridx = 1;
        JTextField serialField = new JTextField(20);
        panel.add(serialField, gbc);

        //Quantity
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JTextField quantityField = new JTextField(20);
        panel.add(quantityField, gbc);

        //Low stock trigger
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Low Stock Trigger (optional):"), gbc);
        gbc.gridx = 1;
        JTextField lowStockField = new JTextField(20);
        lowStockField.setToolTipText("Enter a number to trigger low stock alerts (leave empty for none)");
        panel.add(lowStockField, gbc);

        //SKUs
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("SKU (Amazon / eBay / Walmart):"), gbc);
        gbc.gridx = 1;

        JPanel skuPanel = new JPanel(new GridLayout(1, 3, 5, 0)); // 1 row, 3 columns, 5px spacing
        JTextField skuAmazon = new JTextField();
        JTextField skuEbay = new JTextField();
        JTextField skuWalmart = new JTextField();
        skuAmazon.setToolTipText("Amazon SKU");
        skuEbay.setToolTipText("eBay SKU");
        skuWalmart.setToolTipText("Walmart SKU");

        skuPanel.add(skuAmazon);
        skuPanel.add(skuEbay);
        skuPanel.add(skuWalmart);
        panel.add(skuPanel, gbc);

        //Image Selector
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Item Icon:"), gbc);
        gbc.gridx = 1;
        JButton imageButton = new JButton("Select Image");
        JLabel imageLabel = new JLabel("No image selected", SwingConstants.LEFT);
        String[] filePath = new String[] {Constants.NOT_FOUND_PNG};
        getImage(imageButton, imageLabel, file -> filePath[0] = file );

        panel.add(UIUtils.styleButton(imageButton), gbc);
        gbc.gridx = 1; gbc.gridy++;
        panel.add(imageLabel, gbc);

        //Is Composite
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Is Composite (Yes / No):"), gbc);
        gbc.gridx = 1;
        JCheckBox compositeCheck = new JCheckBox();
        panel.add(compositeCheck, gbc);

        //Composed of
        gbc.gridx = 0; gbc.gridy++;
        JLabel composedLabel = new JLabel("Composed Of:");
        panel.add(composedLabel, gbc);
        gbc.gridx = 1;

        JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tagPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        tagPanel.setLayout(new UIUtils.WrapLayout(FlowLayout.LEFT, 5, 5));

        DropdownResult DDRObj = getDropDownMenuAllItems();
        JComboBox<String> searchField = DDRObj.menu();
        Map<String, String> componentSerialMap = DDRObj.serialMap();

        Set<String> selectedTags = new LinkedHashSet<>();

        searchField.addActionListener(e -> {
            searchField.hidePopup();
            String selectedDisplay = (String) searchField.getEditor().getItem();
            String selectedSerial = componentSerialMap.get(selectedDisplay);

            if (selectedSerial != null && !selectedTags.contains(selectedSerial)) {
                Item selectedItem = inventory.SerialToItemMap.get(selectedSerial);
                if (selectedItem == null) return;

                selectedTags.add(selectedSerial);
                composedComponents.put(selectedSerial, 1);

                JPanel tag = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                tag.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

                JLabel nameLabel = new JLabel(selectedItem.getName() + " ×1");
                JButton editBtn = new JButton("Edit");
                editBtn.setMargin(new Insets(0, 2, 0, 2));
                JButton removeBtn = new JButton("×");
                removeBtn.setMargin(new Insets(0, 2, 0, 2));

                editBtn.addActionListener(ev -> {
                    String input = JOptionPane.showInputDialog(
                            tagPanel,
                            "Enter quantity for " + selectedItem.getName() + ":",
                            composedComponents.get(selectedSerial)
                    );
                    if (input != null && !input.trim().isEmpty()) {
                        try {
                            int newQty = Integer.parseInt(input.trim());
                            if (newQty <= 0) throw new NumberFormatException();
                            composedComponents.put(selectedSerial, newQty);
                            nameLabel.setText(selectedItem.getName() + " ×" + newQty);
                            tagPanel.revalidate();
                            tagPanel.repaint();
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(tagPanel,
                                    "Please enter a valid positive number.",
                                    "Invalid Quantity",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                removeBtn.addActionListener(ev -> {
                    composedComponents.remove(selectedSerial);
                    selectedTags.remove(selectedSerial);
                    tagPanel.remove(tag);
                    tagPanel.revalidate();
                    tagPanel.repaint();
                });
                tag.add(nameLabel);
                tag.add(editBtn);
                tag.add(removeBtn);
                tagPanel.add(tag);
                tagPanel.revalidate();
                tagPanel.repaint();

            }
        });

        JPanel composedContainer = new JPanel(new BorderLayout(5, 5));
        composedContainer.add(searchField, BorderLayout.NORTH);
        composedContainer.add(new JScrollPane(tagPanel), BorderLayout.CENTER);
        panel.add(composedContainer, gbc);

        composedLabel.setVisible(false);
        composedContainer.setVisible(false);

        SwingUtilities.invokeLater(() -> {
            final int originalWidth = getWidth();
            final int originalHeight = getHeight();

            compositeCheck.addActionListener(e -> {
                boolean visible = compositeCheck.isSelected();
                composedLabel.setVisible(visible);
                composedContainer.setVisible(visible);

                pack();
                if (visible) {
                    setSize(originalWidth * 4 / 3, originalHeight * 4 / 3);
                } else {
                    setSize(originalWidth, originalHeight);
                }

                revalidate();
                repaint();
            });
        });

        //Submit Button
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton submitButton = new JButton("Create Item");
        panel.add(UIUtils.styleButton(submitButton), gbc);
        panel.registerKeyboardAction(
                e -> submitButton.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        panel.registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        submitButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String serial = serialField.getText().trim();
            String inputQuantity = quantityField.getText().trim();
            String skuAmazonText = skuAmazon.getText().trim();
            String skuEbayText = skuEbay.getText().trim();
            String skuWalmartText = skuWalmart.getText().trim();
            boolean isComposite = compositeCheck.isSelected();
            ArrayList<ItemPacket> composedOfPackets = new ArrayList<>();
            String lowStockText = lowStockField.getText().trim();
            Integer lowStockTrigger = null;


            if (isComposite) {
                for (Map.Entry<String, Integer> entry : composedComponents.entrySet()) {
                    String serialKey = entry.getKey();
                    int qty = entry.getValue();
                    Item component = inventory.SerialToItemMap.get(serialKey);
                    if (component != null) {
                        composedOfPackets.add(new ItemPacket(component, qty));
                    }
                }

                if (composedOfPackets.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Composite items must include at least one component.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            if (!lowStockText.isEmpty()) { //Check if good input for low stock trigger
                try {
                    lowStockTrigger = Integer.parseInt(lowStockText);
                    if (lowStockTrigger < 0) {
                        JOptionPane.showMessageDialog(this,
                                "Low stock trigger cannot be negative.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a valid number for the low stock trigger.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            int quantity;
            try {
                if(inputQuantity.isEmpty()){
                    quantity = 0;
                }else{
                    quantity = Integer.parseInt(inputQuantity);
                    if(quantity < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for quantity.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Name and serial is required
            if (name.isEmpty() || serial.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Serial are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Duplicate SKU, name, and serial check
            Item duplicateSerial = inventory.getItemBySerial(serial);
            if (duplicateSerial != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Item with Serial \"" + serial + "\" already exists.\n\n" +
                                "Existing Item Details:\n\n" +
                                "Name: " + duplicateSerial.getName() + "\n" +
                                "Serial: " + duplicateSerial.getSerialNum(),
                        "Duplicate Serial",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            Item duplicateNameItem = inventory.getItemByName(name);
            if (duplicateNameItem != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Warning: Another item already uses the name \"" + name + "\".\n\n" +
                                "Existing Item Details:\n\n" +
                                "Name: " + duplicateNameItem.getName() + "\n" +
                                "Serial: " + duplicateNameItem.getSerialNum() + "\n" +
                                "Amazon SKU: " + (duplicateNameItem.getAmazonSellerSKU() != null ? duplicateNameItem.getAmazonSellerSKU() : "N/A") + "\n" +
                                "eBay SKU: " + (duplicateNameItem.getEbaySellerSKU() != null ? duplicateNameItem.getEbaySellerSKU() : "N/A") + "\n" +
                                "Walmart SKU: " + (duplicateNameItem.getWalmartSellerSKU() != null ? duplicateNameItem.getWalmartSellerSKU() : "N/A") + "\n\n" +
                                "You can still proceed, but it’s recommended to use a unique name.",
                        "Duplicate Name Warning",
                        JOptionPane.WARNING_MESSAGE
                );
                //No return just warn user
            }

            // Amazon SKU
            Item duplicateAmazon = inventory.getItemByAmazonSKU(skuAmazonText);
            if (duplicateAmazon != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Item with Amazon SKU \"" + skuAmazonText + "\" already exists.\n\n" +
                                "Existing Item Details:\n\n" +
                                "Name: " + duplicateAmazon.getName() + "\n" +
                                "Serial: " + duplicateAmazon.getSerialNum() + "\n" +
                                "Amazon SKU: " + duplicateAmazon.getAmazonSellerSKU(),
                        "Duplicate Amazon SKU",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // eBay SKU
            Item duplicateEbay = inventory.getItemByEbaySKU(skuEbayText);
            if (duplicateEbay != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Item with eBay SKU \"" + skuEbayText + "\" already exists.\n\n" +
                                "Existing Item Details:\n\n" +
                                "Name: " + duplicateEbay.getName() + "\n" +
                                "Serial: " + duplicateEbay.getSerialNum() + "\n" +
                                "eBay SKU: " + duplicateEbay.getEbaySellerSKU(),
                        "Duplicate eBay SKU",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

                // Walmart SKU
            Item duplicateWalmart = inventory.getItemByWalmartSKU(skuWalmartText);
            if (duplicateWalmart != null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Item with Walmart SKU \"" + skuWalmartText + "\" already exists.\n\n" +
                                "Existing Item Details:\n\n" +
                                "Name: " + duplicateWalmart.getName() + "\n" +
                                "Serial: " + duplicateWalmart.getSerialNum() + "\n" +
                                "Walmart SKU: " + duplicateWalmart.getWalmartSellerSKU(),
                        "Duplicate Walmart SKU",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            //Confirmation
            String confirm = JOptionPane.showInputDialog(
                    this,
                    "Please retype the serial number to confirm:\n(" + serial + ")",
                    "Confirm Serial",
                    JOptionPane.WARNING_MESSAGE

            );

            if (confirm == null) return;
            if (!confirm.equals(serial)) {
                JOptionPane.showMessageDialog(this, "Serial numbers do not match. Item not added.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            //Success

            StringBuilder composedText = new StringBuilder();
            for (Map.Entry<String, Integer> entry : composedComponents.entrySet()) {
                String serialKey = entry.getKey();
                int qty = entry.getValue();
                Item component = inventory.SerialToItemMap.get(serialKey);
                if (component != null) {
                    composedText.append("\n  • ")
                            .append(component.getName())
                            .append(" x")
                            .append(qty);
                }
            }
            String summary = "Item Name: " + name +
                    "\nSerial: " + serial +
                    "\nQuantity: " + quantity +
                    "\nLow Stock Trigger: " + (lowStockTrigger != null ? lowStockTrigger : "None")+
                    "\nAmazon SKU: " + skuAmazonText +
                    "\nEbay SKU: " + skuEbayText +
                    "\nWalmart SKU: " + skuWalmartText +
                    "\nIs Composite: " + (isComposite ? "Yes \nComposed Of:" + composedText : "No");

            if (lowStockTrigger == null) {
                lowStockTrigger = 0;
            }
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    summary,
                    "Confirm Add Item",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
               return;
            }
            inventory.createItem(
                    name,
                    serial,
                    lowStockTrigger,
                    composedOfPackets,
                    filePath[0],
                    inventory.itemManager,
                    skuAmazonText,
                    skuEbayText,
                    skuWalmartText,
                    quantity
            );
            dispose();
        });

        //Scroll and Add
        JScrollPane scrollPane = new JScrollPane(panel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        //Select the name field
        SwingUtilities.invokeLater(() -> {
            nameField.requestFocusInWindow();
            nameField.selectAll();
        });
        return mainPanel;
    }
    public JPanel addToExistingItemPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Add to Existing Item");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(UIUtils.FONT_ARIAL_BOLD_MEDIUM);
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


        //Dropdown
        panel.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 0;
        DropdownResult DDRObj = getDropDownMenuAllItems();
        JComboBox<String> itemDropdown = DDRObj.menu();
        Map<String,String> displayToSerialMap = DDRObj.serialMap();

        if (selected != null) {
            String serial = selected.getSerialNum();
            for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                if (entry.getValue().equals(serial)) {
                    itemDropdown.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }
        gbc.gridx = 1;
        panel.add(itemDropdown, gbc);

        JLabel previewLabel = new JLabel();
        previewLabel.setPreferredSize(new Dimension(64, 64));

        itemDropdown.addActionListener(e -> {
            String displayName = (String) itemDropdown.getSelectedItem();
            if (displayName == null) return;

            String serial = displayToSerialMap.get(displayName);
            if (serial == null) return;

            Item selectedItem = inventory.getItemBySerial(serial);
            if (selectedItem == null) {
                return;
            }

            if (!selectedItem.getImagePath().equals(Constants.NOT_FOUND_PNG)) {
                previewLabel.setIcon(selectedItem.getIcon(64));
            } else {
                previewLabel.setIcon(null);
            }
        });

        gbc.gridx = 2;
        panel.add(previewLabel, gbc);




        // Quantity
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Amount To Add:"), gbc);
        gbc.gridx = 1;

        JTextField quantityField = new JTextField(20);
        panel.add(quantityField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton addButton = new JButton("Add to item");
        JButton newItemButton = new JButton("Create brand new Item");
        JButton composeButton = new JButton("Compose Items");


        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(addButton));
        buttonRow.add(UIUtils.styleButton(newItemButton));
        buttonRow.add(UIUtils.styleButton(composeButton));
        panel.add(buttonRow, gbc);

        FontMetrics fm = addButton.getFontMetrics(addButton.getFont());
        int fixedWidth = fm.stringWidth("Add 9999 Sample Item Names") + 30;
        Dimension fixedSize = new Dimension(fixedWidth, addButton.getPreferredSize().height);
        addButton.setPreferredSize(fixedSize);
        addButton.setMinimumSize(fixedSize);
        addButton.setMaximumSize(fixedSize);

        //Button Actions
        //If valid input then allow "enter" to add item
        panel.registerKeyboardAction(
                e -> {
                    String text = quantityField.getText().trim();
                    if (!text.isEmpty()) {
                        try {
                            int quantity = Integer.parseInt(text);
                            if (quantity > 0) {
                                addButton.doClick(); // trigger add if valid
                            }
                        } catch (NumberFormatException ex) {
                            // optional: ignore or show a message
                            JOptionPane.showMessageDialog(panel, "Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        }
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

        quantityField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                updateAddButtonText(addButton, itemDropdown, displayToSerialMap, quantityField);
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        addButton.addActionListener(e -> {
            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);

            // Read numeric value safely
            String text = quantityField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid amount.", "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Amount must be a valid number.", "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Item target = inventory.SerialToItemMap.get(selectedSerial);

            updateAddButtonText(addButton, itemDropdown, displayToSerialMap, quantityField);


            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be at least 1.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedSerial == null || !inventory.SerialToItemMap.containsKey(selectedSerial)) {
                JOptionPane.showMessageDialog(this, "Please select a valid item.", "Invalid Item", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (target == null){
                JOptionPane.showMessageDialog(this, "Error: Item could not be accessed.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String name = target.getName();
            String plural = (amount == 1)
                    ? name
                    : (name.endsWith("s") ? name : name + "s");
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to add " + amount + " " + plural + " to inventory?",
                    "Confirm Add",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            inventory.addItemAmount(target,amount);

            JOptionPane.showMessageDialog(this,
                    "Added " + amount + " "  + (amount == 1 ? name : plural) + " to inventory (Serial: " + selectedSerial + ")",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        newItemButton.addActionListener(e -> {
            getContentPane().removeAll();
            add(createNewItemPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });

        composeButton.addActionListener(e ->{
            //Get currently selected item
            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);
            selected = inventory.SerialToItemMap.get(selectedSerial);

            getContentPane().removeAll();
            add(composeItemPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });

        mainPanel.add(panel, BorderLayout.CENTER);
        return mainPanel;
    }
    public JPanel composeItemPanel(){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Compose Items");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(UIUtils.FONT_ARIAL_BOLD_MEDIUM);
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

        panel.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 1;

        DropdownResult DDRObj = getDropDownMenuCompositeItems();
        JComboBox<String> itemDropdown = DDRObj.menu();
        Map<String,String> displayToSerialMap = DDRObj.serialMap();

        //Remove null and non-composite items
        if (selected != null) {
            if(selected.isComposite()){
                String serial = selected.getSerialNum();
                for (Map.Entry<String, String> entry : displayToSerialMap.entrySet()) {
                    if (entry.getValue().equals(serial)) {
                        itemDropdown.setSelectedItem(entry.getKey());
                        break;
                    }
                }
            }
        }
        panel.add(itemDropdown, gbc);

        JLabel previewLabel = new JLabel();
        previewLabel.setPreferredSize(new Dimension(64, 64));

        itemDropdown.addActionListener(e -> {
            String displayName = (String) itemDropdown.getSelectedItem();
            if (displayName == null) return;

            String serial = displayToSerialMap.get(displayName);
            if (serial == null) return;

            Item selectedItem = inventory.getItemBySerial(serial);
            if (selectedItem == null) {
                return;
            }

            if (!selectedItem.getImagePath().equals(Constants.NOT_FOUND_PNG)) {
                previewLabel.setIcon(selectedItem.getIcon(64));
            } else {
                previewLabel.setIcon(null);
            }
        });

        gbc.gridx = 2;
        panel.add(previewLabel, gbc);

        // Quantity
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Amount To Compose:"), gbc);
        gbc.gridx = 1;

        JTextField quantityField = new JTextField(20);
        panel.add(quantityField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton composeButton = new JButton("Compose Item");
        JButton newItemButton = new JButton("Create New Item");
        JButton returnButton = new JButton("Return to Add items ");

        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(composeButton));
        buttonRow.add(UIUtils.styleButton(newItemButton));
        buttonRow.add(UIUtils.styleButton(returnButton));
        panel.add(buttonRow, gbc);


        FontMetrics fm = composeButton.getFontMetrics(composeButton.getFont());
        int fixedWidth = fm.stringWidth("Compose 9999 Sample Item Names") + 30;
        Dimension fixedSize = new Dimension(fixedWidth, composeButton.getPreferredSize().height);
        composeButton.setPreferredSize(fixedSize);
        composeButton.setMinimumSize(fixedSize);
        composeButton.setMaximumSize(fixedSize);


        quantityField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                updateComposeButtonText(composeButton, itemDropdown, displayToSerialMap, quantityField);
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });


        composeButton.addActionListener(e -> {
            String selectedItem = (String) itemDropdown.getEditor().getItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);

            // Read numeric value safely
            String text = quantityField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid amount.", "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int amount;

            try {
                amount = Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Amount must be a valid number.", "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be at least 1.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedSerial == null || !inventory.SerialToItemMap.containsKey(selectedSerial)) {
                JOptionPane.showMessageDialog(this, "Please select a valid item.", "Invalid Item", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Item target = inventory.SerialToItemMap.get(selectedSerial);

            updateComposeButtonText(composeButton, itemDropdown, displayToSerialMap, quantityField);

            if (target == null) {
                JOptionPane.showMessageDialog(this, "Error: Item could not be accessed.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!target.isComposite()){ //double check
                JOptionPane.showMessageDialog(this, "Item is not composite", "Not composite", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for(ItemPacket ip: target.getComposedOf()){ //Check if we have enough of each part
                long required = (long) ip.getQuantity() * (long) amount;
                if(required > inventory.getQuantity(ip.getItem())){
                    JOptionPane.showMessageDialog(this,"Not enough "+ ip.getItem().getName() + " to compose item: "+ target.getName() +". \n " +
                            "(Amount needed = "+ip.getQuantity()*amount + " || Amount available = "+inventory.getQuantity(ip.getItem()) + ")",
                            "Not enough items to compose item",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String name = target.getName();
            String plural = (amount == 1)
                    ? name
                    : (name.endsWith("s") ? name : name + "s");
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to compose " + amount + " " + plural + " to inventory?\n\n" +
                            "This will reduce the stock of the items used to compose it from the inventory",
                    "Confirm Add",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;


            inventory.composeItem(target, amount);


            JOptionPane.showMessageDialog(this,
                    "Composed " + amount + " " + (amount == 1 ? name : plural) +
                            " and added to inventory (Serial: " + selectedSerial + ")",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        newItemButton.addActionListener(e -> {
            getContentPane().removeAll();
            add(createNewItemPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });

        returnButton.addActionListener(e -> {
            getContentPane().removeAll();
            add(addToExistingItemPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });

        mainPanel.add(panel,BorderLayout.CENTER);
        return mainPanel;
    }
    void updateAddButtonText(JButton addButton, JComboBox<String> itemDropdown,
                             Map<String, String> displayToSerialMap, JTextField textField) {

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
        addButton.setText(updateAddText(currentTarget, currentAmount));
    }
    void updateComposeButtonText(JButton composeButton, JComboBox<String> itemDropdown,
                             Map<String, String> displayToSerialMap, JTextField textField) {

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
        composeButton.setText(updateComposeText(currentTarget, currentAmount));
    }
    String updateAddText(Item target, int amount) {
        String addButtonText;
        if (target == null) {
            addButtonText = (amount > 0)
                    ? "Add " + amount + " to this item"
                    : "Add amount to this item";
        } else {
            String name = target.getName();
            String plural = name.endsWith("s") ? name + "es" : name + "s";
            addButtonText = (amount > 1)
                    ? "Add " + amount + " " + plural
                    : (amount <= 0 ? "Add amount to " + name : "Add " + amount + " " + name);
        }
        return addButtonText;
    }
    String updateComposeText(Item target, int amount) {
        String composeButtonText;
        if (target == null) {
            composeButtonText= (amount > 0)
                    ? "Compose " + amount + " of this item"
                    : "Compose amount of this item";
        } else {
            String name = target.getName();
            String plural = name.endsWith("s") ? name + "es" : name + "s";
            composeButtonText = (amount > 1)
                    ? "Compose " + amount + " " + plural
                    : (amount <= 0 ? "Compose amount of " + name : "Compose " + amount + " " + name);
        }
        return composeButtonText;
    }
}
