package gui;

import core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class EditWindow extends SubWindow {
    public static String windowName = "Edit Item";
    private final Item selectedItem;
    private File selectedImageFile;
    private final Map<String, Integer> composedComponents = new LinkedHashMap<>();

    public EditWindow(JFrame mainWindow, Inventory inventory, Item selected) {
        super(mainWindow, windowName, inventory);
        this.selectedItem = selected;
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
        info.setFont(new Font("Arial", Font.BOLD, 14));
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
        JTextField serialField = new JTextField(selectedItem.getSerialNum(), 20);
        serialField.setEditable(false);
        serialField.setBackground(new Color(245, 245, 245));
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
        if (selectedItem.getImagePath() != null && new File(selectedItem.getImagePath()).exists()) {
            ImageIcon icon = new ImageIcon(selectedItem.getImagePath());
            Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
        } else {
            imageLabel.setText("No image selected");
        }

        imageButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedImageFile = chooser.getSelectedFile();
                ImageIcon icon = new ImageIcon(selectedImageFile.getAbsolutePath());
                Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText("");
            }
        });

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
        JComboBox<String> searchField = DDRObj.menu;
        Map<String, String> componentSerialMap = DDRObj.serialMap;
        Set<String> selectedTags = new LinkedHashSet<>();


        if (selectedItem.getComposedOf() != null) {
            for (ItemPacket packet : selectedItem.getComposedOf()) {
                String serial = packet.getItem().getSerialNum();
                composedComponents.put(serial, packet.getQuantity());
                selectedTags.add(serial);

                JPanel tag = makeTagPanel(packet.getItem(), serial, packet.getQuantity(), tagPanel, selectedTags);
                tagPanel.add(tag);
            }
        }

        //Handle dropdown selection
        searchField.addActionListener(e -> {
            String selectedDisplay = (String) searchField.getEditor().getItem();
            String selectedSerial = componentSerialMap.get(selectedDisplay);
            if (selectedSerial == null || selectedTags.contains(selectedSerial)) return;

            Item component = inventory.SerialToItemMap.get(selectedSerial);
            if (component == null) return;

            composedComponents.put(selectedSerial, 1);
            selectedTags.add(selectedSerial);
            JPanel tag = makeTagPanel(component, selectedSerial, 1, tagPanel, selectedTags);
            tagPanel.add(tag);
            tagPanel.revalidate();
            tagPanel.repaint();
            searchField.getEditor().setItem("");
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


                String confirmation = JOptionPane.showInputDialog(
                        this,
                        "To confirm edits, please type the item's serial number:\n\n(" + selectedItem.getSerialNum() + ")",
                        "Confirm Serial",
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirmation == null) return; // user cancelled
                if (!confirmation.trim().equalsIgnoreCase(selectedItem.getSerialNum())) {
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
                if (selectedImageFile != null)
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
                dispose();
                selectedItem.setName(newName);
                selectedItem.setLowStockTrigger(trigger);
                selectedItem.setAmazonSellerSKU(newAmazonSKU);
                selectedItem.setEbaySellerSKU(newEbaySKU);
                selectedItem.setWalmartSellerSKU(newWalmartSKU);
                if (selectedImageFile != null)
                    selectedItem.setImagePath(selectedImageFile.getAbsolutePath());

                inventory.setQuantity(selectedItem, newQty);
                inventory.checkLowAndOutOfStock();

                JOptionPane.showMessageDialog(
                        this,
                        " Changes saved successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JScrollPane scrollPane = new JScrollPane(panel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            nameField.requestFocusInWindow();
            nameField.selectAll();
        });
        pack();
    }

    private JPanel makeTagPanel(Item component, String serial, int qty, JPanel tagPanel, Set<String> selectedTags) {
        JPanel tag = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        tag.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        JLabel nameLabel = new JLabel(component.getName() + " x" + qty);
        JButton editBtn = new JButton("Edit");
        JButton removeBtn = new JButton("x");
        editBtn.addActionListener(ev -> {
            String input = JOptionPane.showInputDialog(tagPanel, "Enter quantity for " + component.getName() + ":", qty);
            try {
                int newQty = Integer.parseInt(input.trim());
                composedComponents.put(serial, newQty);
                nameLabel.setText(component.getName() + " x" + newQty);
            } catch (Exception ignored) {}
        });
        removeBtn.addActionListener(ev -> {
            composedComponents.remove(serial);
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
