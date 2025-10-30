package gui;

import core.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ItemInfoWindow extends SubWindow {

    private final Item item;
    private final Inventory inventory;
    private final LogManager logManager;

    public ItemInfoWindow(JFrame mainWindow, Inventory inventory, Item item) {
        super(mainWindow, item.getName() + " Information", inventory);
        this.item = item;
        this.inventory = inventory;
        this.logManager = inventory.logManager;

        setLocationRelativeTo(mainWindow);

        setupUI();
        setVisible(true);
    }

    @Override
    public void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        //header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        ImageIcon icon = (item.getImagePath() != null)
                ? new ImageIcon(new ImageIcon(item.getImagePath())
                .getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH))
                : new ImageIcon("default_icon.png");

        JLabel imageLabel = new JLabel(icon);
        headerPanel.add(imageLabel, BorderLayout.WEST);

        JTextArea titleArea = new JTextArea(item.getName() + "\n\nSerial: " + item.getSerialNum());
        titleArea.setEditable(false);
        titleArea.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleArea.setOpaque(false);
        headerPanel.add(titleArea, BorderLayout.CENTER);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        centerPanel.add(makeInfoSection());
        if (item.isComposite()) centerPanel.add(makeComposedOfSection());
        centerPanel.add(makeUsedInSection(item));
        centerPanel.add(makeLogsSection());

        JPanel detailsWrapper = new JPanel(new BorderLayout());
        TitledBorder detailsBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 160), 2),
                "Details",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        detailsBorder.setTitleFont(new Font("Arial", Font.PLAIN, 18));
        detailsBorder.setTitleColor(new Color(100, 100, 110));

        detailsWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 20, 20, 20),
                detailsBorder
        ));
        detailsWrapper.setOpaque(false);
        detailsWrapper.add(centerPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(detailsWrapper);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        //Buttons
        JPanel buttonPanel = new JPanel(new BorderLayout(30, 10));
        JButton okBtn = UIUtils.styleButton(new JButton("OK"));
        JButton editBtn = UIUtils.styleButton(new JButton("Edit"));

        Font btnFont = new Font("Segoe UI", Font.BOLD, 16);
        Dimension btnSize = new Dimension(120, 45);
        okBtn.setFont(btnFont);
        editBtn.setFont(btnFont);
        okBtn.setPreferredSize(btnSize);
        editBtn.setPreferredSize(btnSize);

        okBtn.addActionListener(e -> dispose());
        editBtn.addActionListener(e -> {
            dispose();
            new EditWindow((JFrame) getOwner().getOwner(), inventory, item); //Select main window and open the edit window
        });

        buttonPanel.add(okBtn, BorderLayout.WEST);
        buttonPanel.add(editBtn, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setSize(new Dimension(900,800));
    }

    private JPanel wrapSection(JPanel inner, String title) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 160), 2),
                title,
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        border.setTitleFont(new Font("Arial", Font.PLAIN, 16));
        border.setTitleColor(new Color(100, 100, 110));

        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20),
                border
        ));
        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel makeInfoSection() {
        JPanel inner = new JPanel(new GridLayout(0, 2, 5, 5));
        inner.setOpaque(false);

        inner.add(new JLabel("Quantity:"));
        inner.add(new JLabel(String.valueOf(inventory.MainInventory.get(item))));

        inner.add(new JLabel("Low Stock Trigger:"));
        inner.add(new JLabel(String.valueOf(item.getLowStockTrigger())));

        inner.add(new JLabel("Composite:"));
        inner.add(new JLabel(item.isComposite() ? "Yes" : "No"));

        inner.add(new JLabel("Amazon SKU:"));
        inner.add(new JLabel(item.getAmazonSellerSKU() != null ? item.getAmazonSellerSKU() : "—"));

        inner.add(new JLabel("eBay SKU:"));
        inner.add(new JLabel(item.getEbaySellerSKU() != null ? item.getEbaySellerSKU() : "—"));

        inner.add(new JLabel("Walmart SKU:"));
        inner.add(new JLabel(item.getWalmartSellerSKU() != null ? item.getWalmartSellerSKU() : "—"));

        return wrapSection(inner, "General Information");
    }

    private JPanel makeComposedOfSection() {
        JPanel inner = new JPanel(new GridLayout(0, 1, 5, 5));
        inner.setOpaque(false);

        for (ItemPacket p : item.getComposedOf()) {
            Item comp = p.getItem();
            ImageIcon icon = (comp.getImagePath() != null)
                    ? new ImageIcon(new ImageIcon(comp.getImagePath())
                    .getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH))
                    : new ImageIcon("default_icon.png");

            JLabel label = new JLabel("• " + comp.getName() + " ×" + p.getQuantity(), icon, JLabel.LEFT);
            inner.add(label);
        }

        return wrapSection(inner, "Composed Of");
    }

    private JPanel makeUsedInSection(Item item) {
        JPanel inner = new JPanel(new GridLayout(0, 1, 5, 5));
        inner.setOpaque(false);

        for (Item parent : inventory.MainInventory.keySet()) {
            if (parent.isComposedOf(item)) {
                inner.add(new JLabel("• " + parent.getName()));
            }
        }

        return wrapSection(inner, "Used In");
    }

    private JPanel makeLogsSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 160), 2),
                "Logs",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        border.setTitleFont(new Font("Arial", Font.PLAIN, 16));
        border.setTitleColor(new Color(100, 100, 110));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20),
                border
        ));

        List<Log> logs = logManager.itemToLogs.getOrDefault(item, new ArrayList<>());

        if (logs.isEmpty()) {
            JLabel empty = new JLabel("No logs for this item.", JLabel.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        MainWindow.LogTableModel logTableModel = new MainWindow.LogTableModel(new ArrayList<>(logs));
        JTable logTable = new JTable(logTableModel);
        logTable.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        logTable.setRowHeight(26);
        logTable.setFillsViewportHeight(true);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = logTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(230, 230, 240));
        header.setForeground(Color.DARK_GRAY);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            Color getAltered(Color c, boolean even) {
                int delta = even ? 10 : 0;
                return new Color(
                        Math.max(c.getRed() - delta, 0),
                        Math.max(c.getGreen() - delta, 0),
                        Math.max(c.getBlue() - delta, 0)
                );
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                int modelRow = table.convertRowIndexToModel(row);
                MainWindow.LogTableModel model = (MainWindow.LogTableModel) table.getModel();
                Log log = model.getLogAt(modelRow);

                boolean even = (row % 2 == 0);
                Color normalColor   = new Color(220, 220, 235);
                Color warningColor  = new Color(255, 250, 205);
                Color criticalColor = new Color(255, 204, 204);
                Color revertedColor = new Color(210, 200, 210);

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else if (log.isReverted()) {
                    c.setBackground(revertedColor);
                } else {
                    switch (log.getSeverity()) {
                        case Normal -> c.setBackground(getAltered(normalColor, even));
                        case Warning -> c.setBackground(getAltered(warningColor, even));
                        case Critical -> c.setBackground(getAltered(criticalColor, even));
                    }
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });

        JScrollPane scrollPane = getScrollPane(logTable, logs);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private static JScrollPane getScrollPane(JTable logTable, List<Log> logs) {
        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 210)));
        scrollPane.setPreferredSize(new Dimension(400, Math.min(150, 25 * logs.size() + 45)));

        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int width = scrollPane.getViewport().getWidth();
                TableColumnModel cm = logTable.getColumnModel();
                cm.getColumn(0).setPreferredWidth((int) (width * 0.08)); // Log #
                cm.getColumn(1).setPreferredWidth((int) (width * 0.18)); // Type
                cm.getColumn(2).setPreferredWidth((int) (width * 0.55)); // Action
                cm.getColumn(3).setPreferredWidth((int) (width * 0.19)); // Time
            }
        });
        return scrollPane;
    }
}