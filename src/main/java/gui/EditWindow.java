package gui;

import core.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class EditWindow extends SubWindow {
    public static String windowName = "Edit Item";
    public final Item selectedItem;
    private final Map<String, Integer> componentsBySerial = new LinkedHashMap<>();
    private final LogManager logManager;

    public EditWindow(MainWindow mainWindow, Inventory inventory, Item selected,LogManager logManager) {
        super(mainWindow, windowName, inventory);
        this.selectedItem = selected;
        this.logManager = logManager;
        setupUI();
        setVisible(true);
    }

    @Override
    public void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        //Header
        JPanel top = new JPanel();
        JTextArea info = new JTextArea("Edit Existing Item");
        info.setEditable(false);
        info.setOpaque(false);
        info.setFont(UIUtils.FONT_ARIAL_BOLD);
        top.add(info);
        mainPanel.add(top, BorderLayout.NORTH);


        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        //Name
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(selectedItem.getName(), 20);
        panel.add(nameField, gbc);

        //Serial
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Item Serial:"), gbc);
        gbc.gridx = 1;
        JTextField serialField = new JTextField(selectedItem.getSerial(), 20);
        serialField.setEditable(false);
        serialField.setBackground(UIUtils.BACKGROUND_MAIN);
        panel.add(serialField, gbc);

        //Quantity
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JTextField quantityField = new JTextField(String.valueOf(inventory.getQuantity(selectedItem)), 20);
        panel.add(quantityField, gbc);

        //Low stock trigger
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Low Stock Trigger:"), gbc);
        gbc.gridx = 1;
        JTextField lowStockField = new JTextField(
                selectedItem.getLowStockTrigger() > 0 ? String.valueOf(selectedItem.getLowStockTrigger()) : "", 20
        );
        panel.add(lowStockField, gbc);

        //SKus
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("SKU (Amazon / eBay / Walmart):"), gbc);
        gbc.gridx = 1;
        JPanel skuPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        JTextField skuAmazon = new JTextField(selectedItem.getAmazonSellerSKU() != null ? selectedItem.getAmazonSellerSKU() : "");
        JTextField skuEbay = new JTextField(selectedItem.getEbaySellerSKU() != null ? selectedItem.getEbaySellerSKU() : "");
        JTextField skuWalmart = new JTextField(selectedItem.getWalmartSellerSKU() != null ? selectedItem.getWalmartSellerSKU() : "");
        skuPanel.add(skuAmazon);
        skuPanel.add(skuEbay);
        skuPanel.add(skuWalmart);
        panel.add(skuPanel, gbc);

        //Image
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Item Icon:"), gbc);
        gbc.gridx = 1;
        JButton imageButton = new JButton("Change Image");
        JLabel imageLabel = new JLabel();
        String[] filePath = new String[] {selectedItem.getImagePath()};

        getImage(imageButton, imageLabel, file -> filePath[0] = file );

        panel.add(UIUtils.styleButton(imageButton), gbc);
        gbc.gridx = 1; gbc.gridy++;
        panel.add(imageLabel, gbc);

        //Is composite
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Is Composite (Yes / No):"), gbc);
        gbc.gridx = 1;
        JCheckBox compositeCheck = new JCheckBox();
        compositeCheck.setSelected(selectedItem.isComposite());
        panel.add(compositeCheck, gbc);


        //Composed of edit
        gbc.gridx = 0; gbc.gridy++;
        JLabel composedLabel = new JLabel("Composed Of:");
        panel.add(composedLabel, gbc);
        gbc.gridx = 1;

        JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tagPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        tagPanel.setLayout(new UIUtils.WrapLayout(FlowLayout.LEFT, 5, 5));

        SwingUtilities.invokeLater(() -> {
            final int originalWidth = getWidth();
            final int originalHeight = getHeight();

            compositeCheck.addActionListener(e -> {
                boolean visible = compositeCheck.isSelected();
                composedLabel.setVisible(visible);
                tagPanel.setVisible(visible);

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

        DropdownResult DDRObj = getDropDownMenuAllItems();
        JComboBox<String> searchField = DDRObj.menu();
        Map<String, String> componentSerialMap = DDRObj.serialMap();

        Set<String> selectedTags = new LinkedHashSet<>();

        searchField.removeAllItems();
        Map<String, String> filteredSerialMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : componentSerialMap.entrySet()) {
            String displayName = entry.getKey();
            String serial = entry.getValue();

            Item component = inventory.getItemBySerial(serial);
            if (component == null) continue;
            if (serial.equals(selectedItem.getSerial())) continue;
            if (inventory.containsItemRecursively(component, selectedItem.getSerial())) continue;

            filteredSerialMap.put(displayName, serial);
            searchField.addItem(displayName);
        }
        final Map<String, String> finalSerialMap = filteredSerialMap;

        //Populate tags
        if (selectedItem.getComposedOf() != null || selectedItem.getComposedOf().isEmpty()) {
            for (Map.Entry<Item,Integer> packet : selectedItem.getComposedOf().entrySet()) {
                Item i = packet.getKey();
                int amount = packet.getValue();
                String serial = i.getSerial();
                componentsBySerial.put(serial, amount);
                selectedTags.add(serial);
                System.out.println("Called populate : " + i.getName());
                JPanel tag = makeTagPanel(i, serial, amount, tagPanel, selectedTags);
                tagPanel.add(tag);
            }
        }

        //Handle dropdown selection
        searchField.addActionListener(e -> {
            String selectedDisplay = (String) searchField.getEditor().getItem();
            String selectedSerial = finalSerialMap.get(selectedDisplay);

            if (selectedSerial != null && !selectedTags.contains(selectedSerial)) {
                Item component = inventory.SerialToItemMap.get(selectedSerial);
                if (component == null) return;

                selectedTags.add(selectedSerial);
                componentsBySerial.put(selectedSerial, 1);

                JPanel tag = makeTagPanel(component, selectedSerial, 1, tagPanel,selectedTags);
                tagPanel.add(tag);
            }
        });

        JPanel composedContainer = new JPanel(new BorderLayout(5, 5));
        composedContainer.add(searchField, BorderLayout.NORTH);
        composedContainer.add(new JScrollPane(tagPanel), BorderLayout.CENTER);
        panel.add(composedContainer, gbc);

        composedLabel.setVisible(selectedItem.isComposite());
        composedContainer.setVisible(selectedItem.isComposite());

        compositeCheck.addActionListener(e -> {
            boolean visible = compositeCheck.isSelected();
            composedLabel.setVisible(visible);
            composedContainer.setVisible(visible);
            revalidate();
            repaint();
        });

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton saveButton = new JButton("Save Changes");
        panel.add(UIUtils.styleButton(saveButton), gbc);

        saveButton.addActionListener(e -> {
            try {
                String newName = nameField.getText().trim();
                int newQty = Integer.parseInt(quantityField.getText().trim());
                int trigger = lowStockField.getText().isBlank() ? 0 : Integer.parseInt(lowStockField.getText().trim());
                String newAmazonSKU = skuAmazon.getText().trim();
                String newEbaySKU = skuEbay.getText().trim();
                String newWalmartSKU = skuWalmart.getText().trim();

                Map<Item,Integer> newComposition = new HashMap<>();
                for(Map.Entry<String,Integer> entry : componentsBySerial.entrySet()){

                    String serial = entry.getKey();
                    Integer amount = entry.getValue();

                    Item i = inventory.getItemBySerial(serial);

                    newComposition.put(i,amount);
                }


                String confirmation = JOptionPane.showInputDialog(
                        this,
                        "To confirm edits, please type the item's serial number:\n\n(" + selectedItem.getSerial() + ")",
                        "Confirm Serial",
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirmation == null) return; // user cancelled
                if (!confirmation.trim().equalsIgnoreCase(selectedItem.getSerial())) {
                    JOptionPane.showMessageDialog(this, "Serial number does not match. Edit canceled.", "Confirmation Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("<html><body width='400'>");
                sb.append("<b>Review your changes before saving:</b><br><br>");
                sb.append("<b>Name:</b> ").append(selectedItem.getName()).append(" → ").append(newName).append("<br>");
                sb.append("<b>Quantity:</b> ").append(inventory.getQuantity(selectedItem)).append(" → ").append(newQty).append("<br>");
                sb.append("<b>Low Stock Trigger:</b> ").append(selectedItem.getLowStockTrigger()).append(" → ").append(trigger).append("<br>");
                sb.append("<b>Amazon SKU:</b> ").append(selectedItem.getAmazonSellerSKU()).append(" → ").append(newAmazonSKU).append("<br>");
                sb.append("<b>eBay SKU:</b> ").append(selectedItem.getEbaySellerSKU()).append(" → ").append(newEbaySKU).append("<br>");
                sb.append("<b>Walmart SKU:</b> ").append(selectedItem.getWalmartSellerSKU()).append(" → ").append(newWalmartSKU).append("<br>");
                sb.append("<b>Is composite:</b> ").append(selectedItem.isComposite() ? "Yes" : "No").append(" → ").append(compositeCheck.isSelected()? "Yes" : "No").append("<br>");

                if ((selectedItem.isComposite() != compositeCheck.isSelected()) || (selectedItem.getComposedOf() != newComposition)) { //Composition changes
                    StringBuilder beforeText = new StringBuilder();
                    if (selectedItem.getComposedOf() != null && !selectedItem.getComposedOf().isEmpty()) {
                        for (Map.Entry<Item,Integer> packet : selectedItem.getComposedOf().entrySet()) {
                            Item i = packet.getKey();
                            int amount = packet.getValue();
                            beforeText.append("<br>&nbsp;&nbsp;• ")
                                    .append(i.getName())
                                    .append(" x")
                                    .append(amount);
                        }
                    } else {
                        beforeText.append("<br>&nbsp;&nbsp;• None");
                    }

                    StringBuilder afterText = new StringBuilder();
                    if (!newComposition.isEmpty() && compositeCheck.isSelected()) {
                        for (Map.Entry<Item,Integer> IP : newComposition.entrySet()) {
                            Item component = IP.getKey();
                            int qty = IP.getValue();
                            if (component != null) {
                                afterText.append("<br>&nbsp;&nbsp;• ").append(component.getName()).append(" x").append(qty);
                            }
                        }
                    } else {
                        afterText.append("<br>&nbsp;&nbsp;• None");
                        newComposition = selectedItem.getComposedOf();
                    }
                    sb.append("<b>Composition:</b>")
                            .append("<br><b>Before:</b>")
                            .append(beforeText)
                            .append("<br><b>After:</b>")
                            .append(afterText)
                            .append("<br>");
                }

                if (!filePath[0].equals(selectedItem.getImagePath()))
                    sb.append("<b>Image:</b> Updated<br>");
                sb.append("</body></html>");

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        new JLabel(sb.toString()),
                        "Confirm Changes",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(this, "Edit canceled.", "No Changes Made", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    return;
                }

                StringBuilder logMessage = new StringBuilder();
                logMessage.append("Updated item '").append(selectedItem.getName()).append("' (Serial: ").append(selectedItem.getSerial()).append("). \n");

                StringBuilder changes = new StringBuilder();

                if (!selectedItem.getName().equals(newName)) {
                    changes.append("Name: '").append(selectedItem.getName()).append("' → '").append(newName).append("'\n");
                }

                if (inventory.getQuantity(selectedItem) != newQty) {
                    changes.append("Quantity: ").append(inventory.getQuantity(selectedItem)).append(" → ").append(newQty).append("\n");
                }

                if (selectedItem.getLowStockTrigger() != trigger) {
                    changes.append("Low Stock Trigger: ").append(selectedItem.getLowStockTrigger()).append(" → ").append(trigger).append("\n");
                }

                if (!Objects.equals(selectedItem.getAmazonSellerSKU(), newAmazonSKU)) {
                    changes.append("Amazon SKU: '").append(selectedItem.getAmazonSellerSKU()).append("' → '").append(newAmazonSKU).append("'").append("\n");
                }

                if (!Objects.equals(selectedItem.getEbaySellerSKU(), newEbaySKU)) {
                    changes.append("eBay SKU: '").append(selectedItem.getEbaySellerSKU()).append("' → '").append(newEbaySKU).append("'").append("\n");
                }

                if (!Objects.equals(selectedItem.getWalmartSellerSKU(), newWalmartSKU)) {
                    changes.append("Walmart SKU: '").append(selectedItem.getWalmartSellerSKU()).append("' → '").append(newWalmartSKU).append("'").append("\n");
                }
                if (selectedItem.isComposite() != compositeCheck.isSelected()) {
                    changes.append("Is Composite: ").append(selectedItem.isComposite() ? "Yes" : "No").append(" → ").append(compositeCheck.isSelected() ? "Yes" : "No").append("\n");
                }
                if ((selectedItem.isComposite() != compositeCheck.isSelected()) || (selectedItem.getComposedOf() != newComposition)) {
                    changes.append("Composition modified").append("\n");
                }
                if (!filePath[0].equals(selectedItem.getImagePath())){
                    changes.append("Image updated").append("\n");
                }
                logMessage.append("Changes: \n").append(String.join(", ", changes));

                selectedItem.setName(newName);
                selectedItem.setLowStockTrigger(trigger);
                selectedItem.setAmazonSellerSKU(newAmazonSKU);
                selectedItem.setEbaySellerSKU(newEbaySKU);
                selectedItem.setWalmartSellerSKU(newWalmartSKU);
                selectedItem.setImagePath(filePath[0]);

                inventory.setQuantity(selectedItem, newQty);
                selectedItem.replaceComposedOf(newComposition);
                inventory.checkLowAndOutOfStock();

                logManager.createLog(Log.LogType.UpdatedItem,
                        0,
                        logMessage.toString(),
                        selectedItem.getSerial()
                );

                JOptionPane.showMessageDialog(
                        this,
                        " Changes saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
            }
        });

        JScrollPane scrollPane = new JScrollPane(panel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            JTextField text = (JTextField) searchField.getEditor().getEditorComponent();
            text.setText("");
            nameField.requestFocusInWindow();
            nameField.selectAll();
        });
        pack();
    }

    protected JPanel makeTagPanel(Item component, String serial, int qty, JPanel tagPanel, Set<String> selectedTags) {
        System.out.println("Called " + component.getName());
        JPanel tag = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        tag.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JLabel nameLabel = new JLabel(component.getName() + " x" + qty);
        JButton editBtn = new JButton("Edit");
        editBtn.setSize(27,editBtn.getHeight());
        JButton removeBtn = new JButton("x");
        editBtn.addActionListener(ev -> {
            String input = JOptionPane.showInputDialog(tagPanel, "Enter quantity for " + component.getName() + ":", qty);
            try {
                int newQty = Integer.parseInt(input.trim());
                componentsBySerial.put(serial, newQty);
                nameLabel.setText(component.getName() + " x" + newQty);
            } catch (Exception ignored) {}
        });
        removeBtn.addActionListener(ev -> {
            componentsBySerial.remove(serial);
            selectedTags.remove(serial);
            tagPanel.remove(tag);
            tagPanel.revalidate();
            tagPanel.repaint();
        });
        tag.add(nameLabel);
        tag.add(editBtn);
        tag.add(removeBtn);
        return tag;
    }
}
