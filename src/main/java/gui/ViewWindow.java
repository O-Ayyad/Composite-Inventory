package gui;

import constants.Constants;
import core.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;


public class ViewWindow extends SubWindow {
    public static String windowName = "View Items and Update Inventory";

    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;
    private final LogManager logManager;

    final int imageSize = 64;
    final int leftToolsWidth = 250;

    private JTextField searchField;
    private JCheckBox hasNoAmazonBox;
    private JCheckBox hasNoEbayBox;
    private JCheckBox hasNoWalmartBox;

    Set<RowFilter.Entry<? extends DefaultTableModel, ? extends Integer>> exactMatch;

    public ViewWindow(MainWindow mainWindow, Inventory inventory, LogManager logManager) {
        super(mainWindow, windowName, inventory);

        this.logManager = logManager;
        logManager.addChangeListener(() -> SwingUtilities.invokeLater(this::refreshTable));

        exactMatch = new HashSet<>();

        setupUI();
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
        leftTools.setPreferredSize(new Dimension(leftToolsWidth + 50, 0));
        leftTools.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));



        JLabel searchLabel = new JLabel("Search Inventory:");
        searchLabel.setMaximumSize(new Dimension(leftToolsWidth, 30));
        searchLabel.setFont(UIUtils.FONT_UI_BOLD);
        searchLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);


        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(leftToolsWidth, 30));
        searchField.setFont(UIUtils.FONT_UI_REGULAR);
        searchField.setAlignmentX(Component.RIGHT_ALIGNMENT);
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

        hasNoAmazonBox = new JCheckBox("Missing Amazon SKU only");
        hasNoEbayBox   = new JCheckBox("Missing eBay SKU only");
        hasNoWalmartBox= new JCheckBox("Missing Walmart SKU only");

        List<JButton> buttons = List.of(addBtn, composeBtn, reduceBtn, breakBtn, editBtn, deleteBtn, refreshBtn);

        List<JCheckBox> checkBoxes = List.of(hasNoAmazonBox, hasNoEbayBox, hasNoWalmartBox);

        for (JButton btn : buttons) {
            btn.setAlignmentX(Component.RIGHT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(leftToolsWidth, 36));
            UIUtils.styleButton(btn);
            leftTools.add(btn);
            leftTools.add(Box.createRigidArea(new Dimension(0, 18)));
        }

        for(JCheckBox check : checkBoxes){
            check.setSelected(false);
            check.setMaximumSize(new Dimension(leftToolsWidth, 30));
            check.setAlignmentX(Component.RIGHT_ALIGNMENT);
            leftTools.add(check);
            leftTools.add(Box.createRigidArea(new Dimension(0, 18)));

            check.addActionListener(e -> updateTableFilter());
        }

        addBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to add.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new AddWindow(mainWindow,inventory,selected,false);
        });
        composeBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to compose.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new AddWindow(mainWindow, inventory, selected,true);
        });
        reduceBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to reduce stock.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new RemoveWindow(mainWindow, inventory, selected, RemoveWindow.SendTo.Reduce);
        });

        breakBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to break down", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new RemoveWindow(mainWindow, inventory, selected, RemoveWindow.SendTo.Break);
        });

        editBtn.addActionListener(e -> {
            Item selected = getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an item to edit.", "No item selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new EditWindow(mainWindow, inventory, selected, logManager);
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
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);


        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortable(0, false);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        itemTable.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                updateTableFilter();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });


        itemTable.setFillsViewportHeight(true);
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
                            new EditWindow(mainWindow, inventory, selected,logManager);
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
                        new EditWindow(mainWindow, inventory, selected,logManager);
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
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        if (itemTable.getRowSorter() != null && itemTable.getRowSorter().getSortKeys() != null) {
            sortKeys.addAll(itemTable.getRowSorter().getSortKeys());
            itemTable.setRowHeight(70);
        }

        itemTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object icon, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel();
                if (icon instanceof ImageIcon) {
                    label.setIcon((ImageIcon) icon);
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                if (isSelected) {
                    label.setBackground(table.getSelectionBackground());
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setBackground(table.getBackground());
                    label.setForeground(table.getForeground());
                }
                label.setOpaque(true);
                return label;
            }
        });
        if (!sortKeys.isEmpty() && itemTable.getRowSorter() != null) {
            itemTable.getRowSorter().setSortKeys(sortKeys);
        }

        tableModel.setRowCount(0);
        List<Item> items = new ArrayList<>(inventory.MainInventory.keySet());

        ImageIcon defaultIcon = new ImageIcon(Constants.NOT_FOUND_PNG);
        long[] summary = new long[] {0,0,0};
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() {
                for (Item i : items) {
                    updateSummary(summary,i);
                    ImageIcon icon;
                    String iconPath = i.getImagePath();
                    if (iconPath == null || iconPath.equals(Constants.NOT_FOUND_PNG) || !new File(iconPath).exists()) {
                        icon = defaultIcon;
                    } else {
                        icon = new ImageIcon(iconPath);
                    }

                    Icon scaledIcon = getScaledIconTo(icon, imageSize);

                    Object[] row = new Object[]{
                            scaledIcon,
                            inventory.getQuantity(i),
                            i.getName(),
                            i.getSerial(),
                            i.getLowStockTrigger(),
                            i.isComposite() ? "Yes" : "No",
                            nullToNA(i.getAmazonSellerSKU()),
                            nullToNA(i.getEbaySellerSKU()),
                            nullToNA(i.getWalmartSellerSKU())
                    };
                    publish(row);
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    tableModel.addRow(row);
                }
            }

            @Override
            protected void done() {
                if (!sortKeys.isEmpty() && itemTable.getRowSorter() != null) {
                    itemTable.getRowSorter().setSortKeys(sortKeys);
                }
            }
        }.execute();
    }
    private void updateSummary(long[] summary, Item i) {
        int quantity = inventory.getQuantity(i);
        summary[0] ++;
        summary[1] = quantity;
        if(quantity > i.getLowStockTrigger()){
            summary[2]++;
        }

        summaryLabel.setText("Total Items: " + summary[0] +
                " | Total Quantity: " + summary[1] +
                " | Low Stock: " + summary[2]);
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

                cm.getColumn(0).setPreferredWidth((int)(width * 0.10)); //Icon
                cm.getColumn(1).setPreferredWidth((int)(width * 0.07)); //Quantity
                cm.getColumn(2).setPreferredWidth((int)(width * 0.19)); //Name
                cm.getColumn(3).setPreferredWidth((int)(width * 0.11)); //Serial
                cm.getColumn(4).setPreferredWidth((int)(width * 0.10)); //Low Trigger
                cm.getColumn(5).setPreferredWidth((int)(width * 0.10)); //Composite
                cm.getColumn(6).setPreferredWidth((int)(width * 0.11)); //Amazon SKU
                cm.getColumn(7).setPreferredWidth((int)(width * 0.11)); //eBay SKU
                cm.getColumn(8).setPreferredWidth((int)(width * 0.11)); //Walmart SKU
            }
        });
        return scrollPane;
    }
    private void updateTableFilter() {
        TableRowSorter<DefaultTableModel> sorter =
                (TableRowSorter<DefaultTableModel>) itemTable.getRowSorter();

        Comparator<Object> exactFirst = (o1, o2) -> {
            String search = searchField.getText().trim().toLowerCase();
            String s1 = o1.toString().toLowerCase();
            String s2 = o2.toString().toLowerCase();

            boolean aExact = s1.equals(search);
            boolean bExact = s2.equals(search);

            if (aExact && !bExact) return -1;
            if (!aExact && bExact) return 1;

            Integer n1 = parseIntOrNull(s1);
            Integer n2 = parseIntOrNull(s2);

            if (n1 != null && n2 != null)
                return Integer.compare(n1, n2);

            return s1.compareToIgnoreCase(s2);
        };

        sorter.setComparator(2, exactFirst);
        sorter.setComparator(3, exactFirst);
        sorter.setComparator(6, exactFirst);
        sorter.setComparator(7, exactFirst);
        sorter.setComparator(8, exactFirst);

        String text = searchField.getText().trim().toLowerCase();
        sorter.setRowFilter(createRowFilter(text));

        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sorter.setRowFilter(createRowFilter(text));

        if (!text.isEmpty()) {
            int[] searchableCols = {2, 3, 6, 7, 8};


            Integer sortCol = null;
            for (int col : searchableCols) {
                for (int viewRow = 0; viewRow < itemTable.getRowCount(); viewRow++) {
                    int modelRow = itemTable.convertRowIndexToModel(viewRow);
                    String value = tableModel.getValueAt(modelRow, col).toString().toLowerCase();
                    if (value.equals(text)) {
                        sortCol = col;
                        break;
                    }
                }
                if (sortCol != null) break;
            }

            sortKeys.add(new RowSorter.SortKey(sortCol != null ? sortCol : 2, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
        } else {
            sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
            sorter.setSortKeys(sortKeys);
        }

        sorter.setSortKeys(sortKeys);

    }

    private RowFilter<DefaultTableModel, Integer> createRowFilter(String searchText) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String name    = entry.getStringValue(2).toLowerCase(); //Name
                String serial  = entry.getStringValue(3).toLowerCase(); //Serial
                String amazon  = entry.getStringValue(6).toLowerCase(); //Amazon SKU
                String ebay    = entry.getStringValue(7).toLowerCase(); //eBay SKU
                String walmart = entry.getStringValue(8).toLowerCase(); //Walmart SKU

                Item i = entryToItem(entry);
                if (i == null) return false;

                // Apply checkbox filters
                boolean hasNoAmazon = hasNoAmazonBox.isSelected();
                boolean hasNoEbay = hasNoEbayBox.isSelected();
                boolean hasNoWalmart = hasNoWalmartBox.isSelected();

                if (hasNoAmazon && i.getAmazonSellerSKU() != null && !i.getAmazonSellerSKU().isBlank()) return false;
                if (hasNoEbay && i.getEbaySellerSKU() != null && !i.getEbaySellerSKU().isBlank()) return false;
                if (hasNoWalmart && i.getWalmartSellerSKU() != null && !i.getWalmartSellerSKU().isBlank()) return false;

                // Apply search text filter
                if (searchText.isEmpty()) return true;

                return name.contains(searchText)
                        || serial.contains(searchText)
                        || amazon.contains(searchText)
                        || ebay.contains(searchText)
                        || walmart.contains(searchText);
            }
        };
    }
    Item entryToItem(RowFilter.Entry<? extends TableModel, ? extends Integer> entry){
        if(entry.getStringValue(3) == null) return null;
        String serial = entry.getStringValue(3);
        return inventory.getItemBySerial(serial);
    }

    private boolean isExactMatch(int row) {
        String search = searchField.getText().trim().toLowerCase();
        if (search.isEmpty()) return false;

        for (int col : new int[]{2,3,6,7,8}) {
            String value = itemTable.getValueAt(row, col).toString().toLowerCase();
            if (value.equals(search)) return true;
        }
        return false;
    }

    private Integer parseIntOrNull(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }
}
