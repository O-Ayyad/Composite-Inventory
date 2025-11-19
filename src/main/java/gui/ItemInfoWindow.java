package gui;

import constants.*;
import core.*;
import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemInfoWindow extends SubWindow {

    public final Item item;
    private final Inventory inventory;
    private final LogManager logManager;

    public ItemInfoWindow(MainWindow mainWindow, Inventory inventory, Item item) {
        super(mainWindow, item.getName() + " Information", inventory);
        this.item = item;
        this.inventory = inventory;
        this.logManager = inventory.logManager;
        this.mainWindow = mainWindow;

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
        ImageIcon tempIcon = item.getIcon(128);
        ImageIcon icon = (tempIcon != null)
                ? tempIcon
                : new ImageIcon(Constants.NOT_FOUND_PNG);

        JLabel imageLabel = new JLabel(icon);
        headerPanel.add(imageLabel, BorderLayout.WEST);

        JTextArea titleArea = new JTextArea(item.getName() + "\n\nSerial: " + item.getSerial());
        titleArea.setEditable(false);
        titleArea.setFont(UIUtils.FONT_UI_TITLE);
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
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                "Details",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        detailsBorder.setTitleFont(UIUtils.FONT_ARIAL_LARGE_BOLD);
        detailsBorder.setTitleColor(UIUtils.TEXT_SECONDARY);

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

        Font btnFont = UIUtils.FONT_UI_LARGE;
        Dimension btnSize = new Dimension(120, 45);
        okBtn.setFont(btnFont);
        editBtn.setFont(btnFont);
        okBtn.setPreferredSize(btnSize);
        editBtn.setPreferredSize(btnSize);

        okBtn.addActionListener(e -> dispose());
        editBtn.addActionListener(e -> {
            dispose();
            new EditWindow(mainWindow, inventory, item,logManager); //Select main window and open the edit window
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
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                title,
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        border.setTitleFont(UIUtils.FONT_ARIAL_LARGE);
        border.setTitleColor(UIUtils.BORDER_DARK);

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

        for (Map.Entry<Item,Integer> ip : item.getComposedOf().entrySet()) {
            Item comp = ip.getKey();
            ImageIcon icon = comp.getIcon(48);
            if (comp.getImagePath().equals(Constants.NOT_FOUND_PNG)) {
                icon = null;
            }

            JLabel label = new JLabel("• " + comp.getName() + " ×" + ip.getValue(), icon, JLabel.LEFT);
            label.setIconTextGap(10);
            inner.add(label);
        }

        return wrapSection(inner, "Composed Of");
    }

    private JPanel makeUsedInSection(Item item) {
        JPanel inner = new JPanel(new GridLayout(0, 1, 5, 5));
        inner.setOpaque(false);

        for (Item parent : inventory.MainInventory.keySet()) {
            if (parent.getComposedOf() != null && !parent.getComposedOf().isEmpty()) {
                for (Item component : parent.getComposedOf().keySet()) {
                    if (component.equals(item)) {

                        ImageIcon icon = parent.getIcon(48);
                        if (parent.getImagePath().equals(Constants.NOT_FOUND_PNG)) {
                            icon = null;
                        }

                        JLabel label = new JLabel("• " + parent.getName(), icon, JLabel.LEFT);
                        label.setIconTextGap(10);
                        inner.add(label);

                        break; // matching parent found
                    }
                }
            }
        }

        return wrapSection(inner, "Used In");
    }

    private JPanel makeLogsSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                "Logs",
                TitledBorder.CENTER,
                TitledBorder.DEFAULT_POSITION
        );
        border.setTitleFont(UIUtils.FONT_ARIAL_BOLD_MEDIUM);
        border.setTitleColor(UIUtils.TEXT_SECONDARY);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20),
                border
        ));

        List<Log> logs = logManager.itemToLogs.getOrDefault(item, new ArrayList<>());

        if (logs.isEmpty()) {
            JLabel empty = new JLabel("No logs for this item. Something went wrong.", JLabel.CENTER);
            empty.setFont(UIUtils.FONT_UI_ITALIC );
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        LogTableModel logTableModel = new LogTableModel(new ArrayList<>(logs));
        JTable logTable = new JTable(logTableModel);
        logTable.setFont(UIUtils.FONT_UI_REGULAR);
        logTable.setRowHeight(26);
        logTable.setFillsViewportHeight(true);
        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        LogTableModel.attachOpenListener(logTable, log -> new LogWindow(mainWindow, inventory, log, logManager));

        LogTableModel.styleTable(logTable);

        JScrollPane scrollPane = LogTableModel.createScrollPane(logTable);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
}