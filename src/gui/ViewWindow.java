package gui;

import core.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

import java.io.File;

public class ViewWindow extends SubWindow {
    public static String windowName = "View Items and Update Inventory";

    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    protected JFrame mainWindow;



    public ViewWindow(JFrame mainWindow, Inventory inventory) {
        super(mainWindow, windowName, inventory);

        inventory.logManager.addChangeListener(() -> SwingUtilities.invokeLater(this::refreshTable));

        setupUI();
        setVisible(true);
    }

    public void setupUI() {
        setLayout(new BorderLayout());



        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        //Heade

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        topPanel.setOpaque(false);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Left Tools
        JPanel leftTools = new JPanel();
        leftTools.setLayout(new BoxLayout(leftTools, BoxLayout.Y_AXIS));
        leftTools.setOpaque(true);
        leftTools.setPreferredSize(new Dimension(200, 0));
        leftTools.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel searchLabel = new JLabel("Search Inventory:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(180, 30));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchField.setHorizontalAlignment(JTextField.CENTER);
        searchField.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 190)));


        leftTools.add(searchLabel);
        leftTools.add(Box.createRigidArea(new Dimension(0, 6)));
        leftTools.add(searchField);
        leftTools.add(Box.createRigidArea(new Dimension(0, 25)));


        JButton addBtn = new JButton("Add");
        JButton reduceBtn = new JButton("Reduce");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("🔄 Refresh");

        List<JButton> buttons = List.of(addBtn, reduceBtn, editBtn, deleteBtn, refreshBtn);
        for (JButton btn : buttons) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(160, 36));
            UIUtils.styleButton(btn);
            leftTools.add(btn);
            leftTools.add(Box.createRigidArea(new Dimension(0, 18)));
        }

        addBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            new AddWindow(mainWindow, inventory, selected);
        });

        reduceBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            new RemoveWindow(mainWindow, inventory, selected);
        });

        editBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to edit.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new EditWindow(mainWindow, inventory, selected);
        });

        deleteBtn.addActionListener(e -> {
            Item target = getSelectedItem();
            if (target == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select an item to delete.",
                        "No Item Selected",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (!target.getComposesInto().isEmpty()) {
                StringBuilder composeList = new StringBuilder();
                for (Item i : target.getComposesInto()) {
                    composeList.append("• ").append(i.getName()).append("\n");
                }
                String composeListStr = composeList.toString();

                int warnConfirm = JOptionPane.showConfirmDialog(
                        this,
                        "Warning: This item is used as a component in other composite items.\n" +
                                "Removing it may affect the following item(s):\n\n" +
                                composeListStr +
                                "\nProceed anyway?",
                        "Composition Warning",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (warnConfirm != JOptionPane.YES_OPTION) return;
            }

            String serial = target.getSerialNum();
            String serialConfirm = JOptionPane.showInputDialog(
                    this,
                    "Type the serial number to permanently delete:\n\n" +
                            serial +
                            "\n\nThis action cannot be undone! All data and logs associated with this item will be lost!",
                    "Confirm Deletion",
                    JOptionPane.WARNING_MESSAGE
            );

            if (serialConfirm == null || !serialConfirm.trim().equalsIgnoreCase(serial)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Serial numbers do not match. Item not removed.",
                        "Verification Failed",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            //Final confirmation pane
            JPanel confirmPanel = new JPanel(new BorderLayout(10, 10));
            JLabel message = new JLabel(
                    "<html><body width='400'>" +
                            "Are you absolutely sure you want to delete <b>" + target.getName() + "</b> (Serial: <b>" + serial + "</b>)?<br><br>" +
                            "This <b>CANNOT</b> be undone and will remove all logs linked to this item.<br><br>" +
                            "Type <b>CONFIRM</b> below to continue:" +
                            "</body></html>"
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

            if (!(choice == JOptionPane.YES_OPTION && "CONFIRM".equalsIgnoreCase(inputField.getText().trim()))) {
                JOptionPane.showMessageDialog(
                        this,
                        "You must type CONFIRM to proceed with deletion.",
                        "Deletion Canceled",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }


            try {
                inventory.removeItem(target);
                JOptionPane.showMessageDialog(
                        this,
                        "Successfully removed " + target.getName() + " (Serial: " + serial + ")",
                        "Item Removed",
                        JOptionPane.INFORMATION_MESSAGE
                );
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to remove item: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });


        refreshBtn.addActionListener(e -> refreshTable());

        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        leftWrapper.add(leftTools, BorderLayout.CENTER);
        mainPanel.add(leftWrapper, BorderLayout.WEST);

        //Table setup
        String[] cols = {
                "Icon","Amount", "Name", "Serial", "Low Trigger", "Composite",
                "Amazon SKU", "eBay SKU", "Walmart SKU"
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        itemTable = new JTable(tableModel);
        JTableHeader header = itemTable.getTableHeader();
        header.setBackground(new Color(230, 230, 240));
        header.setForeground(Color.DARK_GRAY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 235)));
        header.setPreferredSize(new Dimension(header.getWidth(), 30)); // same height as logs
        header.setReorderingAllowed(true);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        itemTable.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();


        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                String text = searchField.getText().trim().toLowerCase();

                sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                        if (text.isEmpty()) return true;
                        String name    = entry.getStringValue(2).toLowerCase(); //Name
                        String serial  = entry.getStringValue(3).toLowerCase(); //serial
                        String amazon  = entry.getStringValue(6).toLowerCase(); //Amazon SKU
                        String ebay    = entry.getStringValue(7).toLowerCase(); //eBay SKU
                        String walmart = entry.getStringValue(8).toLowerCase(); //Walmart SKU

                        return name.contains(text)
                                || serial.contains(text)
                                || amazon.contains(text)
                                || ebay.contains(text)
                                || walmart.contains(text);
                    }
                });
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });


        itemTable.setRowHeight(48);
        itemTable.setFillsViewportHeight(true);
        itemTable.getTableHeader().setReorderingAllowed(true);
        itemTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);


        // Row highlighting
        itemTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                Color normalColor   = new Color(230, 230, 235);
                Color warningColor  = new Color(255, 253, 220);
                Color criticalColor = new Color(255, 220, 220);

                //Alternating row color
                boolean even = (row % 2 == 0);

                Color rowColor = normalColor;
                int modelRow = table.convertRowIndexToModel(row);
                int serialColIndex = table.getColumnModel().getColumnIndex("Serial");
                int modelCol = table.convertColumnIndexToModel(serialColIndex);
                String serial = (String) table.getModel().getValueAt(modelRow, modelCol);

                Item item = inventory.getItemBySerial(serial);

                if (item != null) {
                    int qty = inventory.getQuantity(item);
                    int trigger = item.getLowStockTrigger();

                    if (qty <= 0) rowColor = criticalColor;
                    else if (trigger > 0 && qty <= trigger) rowColor = warningColor;
                }

                rowColor = even ? rowColor : new Color(rowColor.getRed()-10, rowColor.getGreen()-10,rowColor.getBlue()-10);

                if (!isSelected) {
                    c.setBackground(rowColor);
                }else {
                    c.setBackground(new Color(200, 210, 255)); // selection blue
                }

                setHorizontalAlignment(CENTER);
                return c;
            }
        });

        //Double click
        itemTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = itemTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        itemTable.setRowSelectionInterval(row, row); // ensure proper selection
                        Item selected = getSelectedItem();
                        if(selected == null) return;
                        if (e.isControlDown()) {
                            //ctrl double to edit
                            new EditWindow(mainWindow, inventory, selected);
                        } else {
                            //double click for info
                            new ItemInfoWindow(mainWindow, inventory, selected);
                        }
                    }
                }
            }
        });

        //Click enter or ctrl enter
        itemTable.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Item selected = getSelectedItem();
                    if (selected == null) return;

                    if (e.isControlDown()) {
                        //Ctrl enter edits
                        new EditWindow(mainWindow, inventory, selected);
                    } else {
                        //Regular enter views
                        new ItemInfoWindow(mainWindow, inventory, selected);
                    }

                    e.consume();
                }
            }
        });

        JScrollPane scrollPane = getJScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // footer summary
        summaryLabel = new JLabel("Total Items: 0 | Total Quantity: 0 | Low Stock: 0");
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 10, 10));
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mainPanel.add(summaryLabel, BorderLayout.SOUTH);

        TitledBorder tableBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 160), 2),
                "View Inventory",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );


        tableBorder.setTitleFont(new Font("Segoe UI", Font.PLAIN, 20));
        tableBorder.setTitleColor(new Color(100, 100, 110));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20),
                tableBorder
        ));
        tableWrapper.setOpaque(false);
        tableWrapper.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(tableWrapper, BorderLayout.CENTER);

        add(mainPanel);
        refreshTable();

        pack();
        setSize((new Dimension(1475,1000)));

        setLocationRelativeTo(mainWindow);
    }


    //Populate table
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Item> items = new ArrayList<>(inventory.MainInventory.keySet());

        for (Item i : items) {
            ImageIcon icon;

            //Get image icon
            if (i.getImagePath() != null && new File(i.getImagePath()).exists()) {
                //Load and scale it
                Image img = new ImageIcon(i.getImagePath()).getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                icon = new ImageIcon(img);
            } else {
                Image placeholder = new ImageIcon("icons\\itemIcons\\imageNotFound.png")
                        .getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                icon = new ImageIcon(placeholder);
            }

            tableModel.addRow(new Object[]{
                    icon,
                    i.getQuantity(),
                    i.getName(),
                    i.getSerialNum(),
                    i.getLowStockTrigger(),
                    i.isComposite() ? "Yes" : "No",
                    nullToNA(i.getAmazonSellerSKU()),
                    nullToNA(i.getEbaySellerSKU()),
                    nullToNA(i.getWalmartSellerSKU())
            });
        }

        updateSummary();
    }

    private void updateSummary() {
        int totalItems = tableModel.getRowCount();
        int totalQuantity = inventory.SerialToItemMap.values().stream()
                .mapToInt(Item::getQuantity)
                .sum();
        long lowStockCount = inventory.SerialToItemMap.values().stream()
                .filter(i -> i.getLowStockTrigger() > 0 && i.getQuantity() <= i.getLowStockTrigger())
                .count();

        summaryLabel.setText("Total Items: " + totalItems +
                " | Total Quantity: " + totalQuantity +
                " | Low Stock: " + lowStockCount);
    }

    private Item getSelectedItem() {
        int viewRow = itemTable.getSelectedRow();
        if (viewRow == -1) return null;

        int modelRow = itemTable.convertRowIndexToModel(viewRow);
        int serialColIndex = itemTable.getColumnModel().getColumnIndex("Serial");
        int modelCol = itemTable.convertColumnIndexToModel(serialColIndex);
        String serial = (String) itemTable.getModel().getValueAt(modelRow, modelCol);

        return inventory.SerialToItemMap.get(serial);
    }

    private String nullToNA(String s) {
        return (s == null || s.isBlank()) ? "N/A" : s;
    }

    private JScrollPane getJScrollPane(){
        JScrollPane scrollPane = new JScrollPane(
                itemTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = scrollPane.getViewport().getWidth();
                var cm = itemTable.getColumnModel();

                cm.getColumn(0).setPreferredWidth((int)(width * 0.11)); //Icon
                cm.getColumn(1).setPreferredWidth((int)(width * 0.07)); //Quantity
                cm.getColumn(2).setPreferredWidth((int)(width * 0.19)); //Name
                cm.getColumn(3).setPreferredWidth((int)(width * 0.11)); //Serial
                cm.getColumn(4).setPreferredWidth((int)(width * 0.07)); //Low Trigger
                cm.getColumn(5).setPreferredWidth((int)(width * 0.06)); //Composite
                cm.getColumn(6).setPreferredWidth((int)(width * 0.13)); //Amazon SKU
                cm.getColumn(7).setPreferredWidth((int)(width * 0.13)); //eBay SKU
                cm.getColumn(8).setPreferredWidth((int)(width * 0.13)); //Walmart SKU
            }
        });
        return scrollPane;
    }

    //Custom cell renderer for icons
    private static class ItemCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof ImageIcon icon) {
                cell.setText("");
                cell.setIcon(icon);
                cell.setHorizontalAlignment(CENTER);
            } else {
                cell.setIcon(null);
                cell.setHorizontalAlignment(LEFT);
            }

            int qtyCol = 3;
            int lowCol = 4;
            Object qtyObj = table.getValueAt(row, qtyCol);
            Object lowObj = table.getValueAt(row, lowCol);

            if (qtyObj instanceof Integer q && lowObj instanceof Integer l) {
                if (q == 0) cell.setBackground(new Color(255, 220, 220));
                else if (l > 0 && q <= l) cell.setBackground(new Color(255, 245, 200));
                else cell.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            }

            return cell;
        }
    }
}
