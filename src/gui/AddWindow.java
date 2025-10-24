package gui;
import core.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;



public class AddWindow extends SubWindow {
    public static String windowName = "Add Item";

    private File selectedImageFile = null;
    private final Map<String, Integer> composedComponents = new LinkedHashMap<>();

    public AddWindow(JFrame mainWindow, Inventory inventory) {
        super(mainWindow, windowName,inventory);
        setupUI();
        setVisible(true);
    }
    @Override
    public void setupUI() {
        JPanel mainPanel;
        if (inventory.SerialToItemMap.isEmpty()) {
            mainPanel = addNewItem();
        } else {
            mainPanel = addToExistingItemPanel();
        }
        add(mainPanel, BorderLayout.CENTER);
        pack();
    }
    public JPanel addNewItem(){
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Add New Item or Add to Existing Item");
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
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        panel.add(quantitySpinner, gbc);

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
        imageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedImageFile = chooser.getSelectedFile();
                File file = chooser.getSelectedFile();
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText("");
            }
        });
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
        tagPanel.setPreferredSize(new Dimension(200, 80));

        JTextField searchField = new JTextField(15);
        JPopupMenu suggestionPopup = new JPopupMenu();

        DefaultListModel<String> suggestionModel = new DefaultListModel<>();
        for (String name : inventory.SerialToItemMap.keySet()) suggestionModel.addElement(name);

        JList<String> suggestionList = new JList<>(suggestionModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionPopup.add(new JScrollPane(suggestionList));

        Set<String> selectedTags = new LinkedHashSet<>();

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateSuggestions() {
                String text = searchField.getText().trim().toLowerCase();
                suggestionModel.clear();
                for (String name : inventory.SerialToItemMap.keySet()) {
                    if (name.toLowerCase().contains(text) && !selectedTags.contains(name)) {
                        suggestionModel.addElement(name);
                    }
                }
                if (!suggestionModel.isEmpty()) {
                    suggestionPopup.show(searchField, 0, searchField.getHeight());
                } else {
                    suggestionPopup.setVisible(false);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSuggestions(); }
        });

        suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String selected = suggestionList.getSelectedValue();
                if (selected != null && !selectedTags.contains(selected)) {
                    selectedTags.add(selected);
                    composedComponents.put(selected, 1);

                    JPanel tag = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                    tag.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

                    JLabel nameLabel = new JLabel(selected + " x1");
                    JButton editBtn = new JButton("Edit");
                    editBtn.setMargin(new Insets(0, 2, 0, 2));
                    JButton removeBtn = new JButton("x");
                    removeBtn.setMargin(new Insets(0, 2, 0, 2));

                    // Edit quantity
                    editBtn.addActionListener(ev -> {
                        String input = JOptionPane.showInputDialog(
                                tagPanel,
                                "Enter quantity for " + selected + ":",
                                composedComponents.get(selected)
                        );
                        if (input != null && !input.trim().isEmpty()) {
                            try {
                                int newQty = Integer.parseInt(input.trim());
                                if (newQty <= 0) throw new NumberFormatException();
                                composedComponents.put(selected, newQty);
                                nameLabel.setText(selected + " ×" + newQty);
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

                    //Remove
                    removeBtn.addActionListener(ev -> {
                        composedComponents.remove(selected);
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

                suggestionPopup.setVisible(false);
                searchField.setText("");
            }
        });

        JPanel composedContainer = new JPanel(new BorderLayout(5, 5));
        composedContainer.add(searchField, BorderLayout.NORTH);
        composedContainer.add(new JScrollPane(tagPanel), BorderLayout.CENTER);
        panel.add(composedContainer, gbc);

        composedLabel.setVisible(false);
        composedContainer.setVisible(false);


        compositeCheck.addActionListener(e -> {
            boolean visible = compositeCheck.isSelected();
            composedLabel.setVisible(visible);
            composedContainer.setVisible(visible);
            panel.revalidate();
            panel.repaint();
        });

        //Submit Button
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton submitButton = new JButton("Add Item");
        panel.add(UIUtils.styleButton(submitButton), gbc);

        submitButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String serial = serialField.getText().trim();
            int quantity = (int) quantitySpinner.getValue();
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


            // Name and serial is required
            if (name.isEmpty() || serial.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Serial are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(inventory.hasItem(serial)){
                JOptionPane.showMessageDialog(this, "Item of serial: " + serial + " already exists", "Error", JOptionPane.ERROR_MESSAGE);
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
                composedText.append(entry.getKey()).append(" x").append(entry.getValue()).append(", ");
            }
            if (composedText.length() > 2)
                composedText.setLength(composedText.length() - 2); //Remove last comma

            String summary = "Item Name: " + name +
                    "\nSerial: " + serial +
                    "\nQuantity: " + quantity +
                    "\nLow Stock Trigger: " + (lowStockTrigger != null ? lowStockTrigger : "None")+
                    "\nAmazon SKU: " + skuAmazonText +
                    "\nEbay SKU: " + skuEbayText +
                    "\nWalmart SKU: " + skuWalmartText +
                    "\nIs Composite: " + (isComposite ? "Yes" : "No") +
                    "\nComposed Of: " + composedText;

            if (lowStockTrigger == null) {
                lowStockTrigger = 0;
            }
            String iconPath = (selectedImageFile != null) ? selectedImageFile.getAbsolutePath() : null;
            JOptionPane.showMessageDialog(this, summary, "Confirm Add Item", JOptionPane.INFORMATION_MESSAGE);
            inventory.createItem(
                    name,
                    serial,
                    lowStockTrigger,
                    composedOfPackets,
                    iconPath,
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
        return mainPanel;
    }
    public JPanel addToExistingItemPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        //Header
        JLabel header = new JLabel("Add To Existing Item");
        header.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridwidth = 2;
        panel.add(header, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;

        //Dropdown
        panel.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 1;
        DropdownResult DDRObj = getDropDownMenuAllItems();
        JComboBox<String> itemDropdown = DDRObj.menu;
        Map<String,String> displayToSerialMap = DDRObj.serialMap;
        panel.add(itemDropdown, gbc);

        // Quantity
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Amount To Add:"), gbc);
        gbc.gridx = 1;
        JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        panel.add(amountSpinner, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton addButton = new JButton("Add to Item");
        JButton newItemButton = new JButton("Add New Item");
        JPanel buttonRow = new JPanel();
        buttonRow.add(UIUtils.styleButton(addButton));
        buttonRow.add(UIUtils.styleButton(newItemButton));
        panel.add(buttonRow, gbc);

        //Button Actions
        addButton.addActionListener(e -> {
            String selectedItem = (String) itemDropdown.getSelectedItem();
            String selectedSerial = displayToSerialMap.get(selectedItem);
            int amount = (int) amountSpinner.getValue();

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be positive.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (selectedSerial == null || !inventory.SerialToItemMap.containsKey(selectedSerial)) {
                JOptionPane.showMessageDialog(this, "Please select a valid item.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Item target = inventory.SerialToItemMap.get(selectedSerial);
            inventory.addItemAmount(target,amount);

            JOptionPane.showMessageDialog(this,
                    "Added " + amount + " to " + target.getName() + " (Serial: " + selectedSerial + ")",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        newItemButton.addActionListener(e -> {
            // Replace current content with addNewItem panel
            getContentPane().removeAll();
            add(addNewItem(), BorderLayout.CENTER);
            revalidate();
            repaint();
            pack();
        });

        return panel;
    }
}
