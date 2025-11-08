package gui;

import com.sun.tools.javac.Main;
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

    public ViewWindow(MainWindow mainWindow, Inventory inventory) {
        super(mainWindow, windowName, inventory);

        inventory.logManager.addChangeListener(() -> SwingUtilities.invokeLater(this::refreshTable));

        setupUI();
        setVisible(true);
    }

    public void setupUI() {
        setLayout(new BorderLayout());



        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(UIUtils.BACKGROUND_MAIN);

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
        searchLabel.setFont(UIUtils.FONT_UI_BOLD);
        searchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(180, 30));
        searchField.setFont(UIUtils.FONT_UI_REGULAR);
        searchField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchField.setHorizontalAlignment(JTextField.CENTER);
        searchField.setBorder(BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM));


        leftTools.add(searchLabel);
        leftTools.add(Box.createRigidArea(new Dimension(0, 6)));
        leftTools.add(searchField);
        leftTools.add(Box.createRigidArea(new Dimension(0, 25)));


        JButton addBtn = new JButton("Add");
        JButton composeBtn = new JButton("Compose Item");
        JButton reduceBtn = new JButton("Reduce Stock");
        JButton breakBtn = new JButton("Break Down Item");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton refreshBtn = new JButton("🔄 Refresh");

        List<JButton> buttons = List.of(addBtn, composeBtn, reduceBtn, breakBtn, editBtn, deleteBtn, refreshBtn);

        for (JButton btn : buttons) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(160, 36));
            UIUtils.styleButton(btn);
            leftTools.add(btn);
            leftTools.add(Box.createRigidArea(new Dimension(0, 18)));
        }

        addBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to add.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            mainWindow.destroyExistingInstance(AddWindow.class);
            new AddWindow(mainWindow,inventory,selected,false);
        });
        composeBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to compose.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            mainWindow.destroyExistingInstance(AddWindow.class);
            new AddWindow(mainWindow, inventory, selected,true);
        });
        reduceBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to reduce stock.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            mainWindow.destroyExistingInstance(RemoveWindow.class);
            new RemoveWindow(mainWindow, inventory, selected,RemoveWindow.SendTo.Break);
        });

        breakBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to break down", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            mainWindow.destroyExistingInstance(RemoveWindow.class);
            new RemoveWindow(mainWindow, inventory, selected, RemoveWindow.SendTo.Break);
        });

        editBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to edit.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            mainWindow.destroyExistingInstance(EditWindow.class);
            new EditWindow(mainWindow, inventory, selected);
        });

        deleteBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please select an item to delete.",
                        "No Item Selected",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            confirmRemoveItem(selected);
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
        header.setBackground(UIUtils.BACKGROUND_PANEL);
        header.setForeground(Color.DARK_GRAY);
        header.setFont(UIUtils.FONT_UI_BOLD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.BORDER_MEDIUM));
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

            private Color getAltered(Color c, boolean even) {
                int delta = even ? 10 : 0;
                return new Color(
                        Math.max(0, c.getRed() - delta),
                        Math.max(0, c.getGreen() - delta),
                        Math.max(0, c.getBlue() - delta)
                );
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                Color normalColor   = UIUtils.NORMAL_COLOR;
                Color warningColor  = UIUtils.WARNING_COLOR;
                Color criticalColor = UIUtils.CRITICAL_COLOR;


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

                //Alternating row color
                boolean even = (row % 2 == 0);
                rowColor = getAltered(rowColor,even);

                if (!isSelected) {
                    c.setBackground(rowColor);
                }else {
                    c.setBackground(UIUtils.SELECTION_BG);
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
                            mainWindow.destroyExistingInstance(EditWindow.class);
                            new EditWindow(mainWindow, inventory, selected);
                        } else {
                            //double click for info
                            mainWindow.destroyExistingInstance(ItemInfoWindow.class);
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
        summaryLabel.setFont(UIUtils.FONT_UI_REGULAR);
        mainPanel.add(summaryLabel, BorderLayout.SOUTH);

        TitledBorder tableBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                "View Inventory",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );


        tableBorder.setTitleFont(UIUtils.FONT_UI_TITLE);
        tableBorder.setTitleColor(UIUtils.BORDER_DARK);

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
}
