package gui;

import platform.*;
import core.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class MainWindow extends JFrame {

    public final int windowWidth = 1200;
    public final int windowHeight = 800;

    private final LogManager logManager;

    private final JTable logTable;
    private final LogTableModel logTableModel;

    private String searchText = "";
    private boolean showNormal = true;
    private boolean showWarning = true;
    private boolean showCritical = true;
    private boolean showSuppressed = true;

    private final TableRowSorter<LogTableModel> sorter;

    public MainWindow(Inventory inventory,LogManager logManager) {

        this.logManager = logManager;

        logManager.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));

        setTitle("Composite Inventory");
        setSize(windowWidth, windowHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center

        setLayout(new BorderLayout());

        // ---------------- LOGO, BUTTONS, INFO BAR ----------------

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BorderLayout()); // Holds buttons and top bar

        // Logo Panel
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BorderLayout());
        logoPanel.setPreferredSize(new Dimension(windowWidth, 50));
        logoPanel.setBackground(UIUtils.BACKGROUND_MAIN);

        // Logo
        ImageIcon logo = new ImageIcon("icons/logo.png"); // make sure logo.png exists
        Image scaledLogo = logo.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoPanel.add(logoLabel, BorderLayout.WEST);

        // Links and Authorship
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15)); //
        rightPanel.setOpaque(false);

        JLabel authorLabel = new JLabel("Authorship Text");
        authorLabel.setFont(UIUtils.FONT_ARIAL_REGULAR );
        rightPanel.add(authorLabel);

        //Links
        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new GridLayout(2, 1, 0, 0));
        JButton githubLink = UIUtils.createLinkButton("Github", "https://github.com/O-Ayyad", "icons/windowIcons/github.png");

        //Temp
        JButton docsLink = UIUtils.createLinkButton("Docs", "https://example.com/docs", "icons/temp");
        rightPanel.add(githubLink);
        rightPanel.add(docsLink);

        logoPanel.add(rightPanel, BorderLayout.EAST); //Add authorship and LinksF


        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 0, 0)); // 1 row, 4 columns

        JButton addButton = createIconButton("Add or Create Items", "icons/windowIcons/plus.png");
        JButton removeButton = createIconButton("Sell or Remove Items", "icons/windowIcons/remove.png");
        JButton viewButton = createIconButton("View and Edit Inventory", "icons/windowIcons/view.png");
        JButton linkButton = createIconButton("Link Accounts", "icons/windowIcons/link.png");

        //Tool tip to tell user about shortcuts
        addButton.setToolTipText("Add or Create Items (Shift+A)");
        removeButton.setToolTipText("Sell or Remove Items (Shift+R)");
        viewButton.setToolTipText("View and Edit Inventory (Shift+V)");
        linkButton.setToolTipText("Link Accounts (Shift+L)");

        // Add action listeners

        //Shift + A,R,V,L opens the windows form left to right
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK), "openAdd");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK), "openRemove");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.SHIFT_DOWN_MASK), "openView");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.SHIFT_DOWN_MASK), "openLink");

        am.put("openAdd", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new AddWindow(MainWindow.this, inventory); }
        });
        am.put("openRemove", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new RemoveWindow(MainWindow.this, inventory,false); }
        });
        am.put("openView", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new ViewWindow(MainWindow.this, inventory); }
        });
        am.put("openLink", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new LinkWindow(MainWindow.this, inventory); }
        });
        requestFocusInWindow();

        addButton.addActionListener(e -> {new AddWindow(this,inventory);
                requestFocusInWindow();});
        removeButton.addActionListener(e -> {new RemoveWindow(this,inventory,false);
                requestFocusInWindow();});
        viewButton.addActionListener(e -> {new ViewWindow(this,inventory);
                requestFocusInWindow();});
        linkButton.addActionListener(e -> {new LinkWindow(this,inventory);
                requestFocusInWindow();});

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(linkButton);

        // Combine
        topContainer.add(logoPanel, BorderLayout.NORTH);
        topContainer.add(buttonPanel, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // Left tool panel

        JPanel leftTools = new JPanel();
        leftTools.setLayout(new BoxLayout(leftTools, BoxLayout.Y_AXIS));
        leftTools.setOpaque(false);
        leftTools.setPreferredSize(new Dimension(180, 0));
        leftTools.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JPanel leftWrapper = new JPanel(new BorderLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
        leftWrapper.add(leftTools, BorderLayout.CENTER);

        // Search Field
        JLabel searchLabel = new JLabel("Search Logs:");
        searchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JTextField searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(160, 30));
        searchField.setAlignmentX(Component.CENTER_ALIGNMENT);
        searchField.setToolTipText("Type text to search for logs.");
        leftTools.add(searchLabel);
        leftTools.add(Box.createRigidArea(new Dimension(0, 5)));
        leftTools.add(searchField);
        leftTools.add(Box.createRigidArea(new Dimension(0, 30)));


        //Left buttons
        String[] buttonLabels = {"Btn1", "Btn2", "Btn3", "Btn4"};
        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(180, 35));
            UIUtils.styleButton(btn);
            leftTools.add(btn);
            leftTools.add(Box.createRigidArea(new Dimension(0, 20)));
        }

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5)); // horizontal layout
        filterPanel.setOpaque(false);

        //Check Boxes
        JCheckBox showNormalBox = new JCheckBox("✅ Normal", true);
        JCheckBox showWarningBox = new JCheckBox("⚠️ Warning", true);
        JCheckBox showCriticalBox = new JCheckBox("❌ Critical", true);
        JCheckBox showSuppressedBox = new JCheckBox("💤 Suppressed", true);

        for (JCheckBox box : new JCheckBox[]{showNormalBox, showWarningBox, showCriticalBox, showSuppressedBox}) {
            box.setFont(UIUtils.FONT_UI_REGULAR);
            box.setOpaque(false);
            filterPanel.add(box);
        }

        //Log display
        JPanel logsPanel = new JPanel(new BorderLayout());
        logsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 30, 100, 40),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                        "Logs",
                        TitledBorder.CENTER,
                        TitledBorder.DEFAULT_POSITION,
                        UIUtils.FONT_ARIAL_HEADER,
                        UIUtils.BORDER_DARK
                )
        ));


        logTableModel = new LogTableModel(logManager.AllLogs);

        logTable = new JTable(logTableModel);

        sorter = new TableRowSorter<>(logTableModel);
        logTable.setRowSorter(sorter);
        sorter.setSortsOnUpdates(true);

        LogTableModel.styleTable(logTable);

        JTableHeader header = logTable.getTableHeader();
        header.setFont(UIUtils.FONT_UI_BOLD);
        header.setBackground(UIUtils.BACKGROUND_PANEL);
        header.setForeground(Color.DARK_GRAY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.HEADER_BORDER));
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        logTable.setFont(UIUtils.FONT_UI_REGULAR);
        logTable.setRowHeight(28);
        logTable.setFillsViewportHeight(true);

        LogTableModel.attachOpenListener(logTable, log -> {
            new LogWindow(this, inventory, log, logManager);
        });

        //Scroll Panel
        JScrollPane scrollPane = LogTableModel.createScrollPane(logTable);

        //Search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { searchText = searchField.getText(); applyFilter(); }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        showNormalBox.addActionListener(e -> { showNormal = showNormalBox.isSelected(); applyFilter(); });
        showWarningBox.addActionListener(e -> { showWarning = showWarningBox.isSelected(); applyFilter(); });
        showCriticalBox.addActionListener(e -> { showCritical = showCriticalBox.isSelected(); applyFilter(); });
        showSuppressedBox.addActionListener(e -> { showSuppressed = showSuppressedBox.isSelected(); applyFilter(); });

        //Table clicker
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(MainWindow.this, e.getPoint(), logTable);
                if (!logTable.contains(p)) {
                    logTable.clearSelection();
                    logTable.repaint();
                }
            }
        });

        logsPanel.add(scrollPane, BorderLayout.CENTER);
        logsPanel.add(filterPanel, BorderLayout.SOUTH);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.add(leftWrapper, BorderLayout.WEST);
        mainArea.add(logsPanel, BorderLayout.CENTER);

        add(mainArea, BorderLayout.CENTER);
        setVisible(true);
    }


    // Window Buttons
    private JButton createIconButton(String text, String iconPath) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(iconPath);
        ImageIcon icon;
        if (imgURL == null) {
            System.err.println("Icon not found: " + iconPath);
            icon = new ImageIcon(iconPath);
        }else{
            icon = new ImageIcon(imgURL);
        }
        int iconWidth = 60;
        int iconHeight = 60;
        Image scaled = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(windowWidth / 4, windowHeight / 10));
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.setLayout(null);
        button.setPreferredSize(new Dimension(windowWidth / 4, windowHeight / 10));

        int buttonWidth = button.getPreferredSize().width;
        int buttonHeight = button.getPreferredSize().height;

        JLabel iconLabel = new JLabel(new ImageIcon(scaled));
        iconLabel.setBounds(buttonWidth / 4 - iconWidth / 2, (buttonHeight - iconHeight) / 2, iconWidth, iconHeight);
        button.add(iconLabel);

        JLabel textLabel = new JLabel(text);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        int tLabelWidth = textLabel.getPreferredSize().width;
        textLabel.setBounds((buttonWidth/16)+tLabelWidth/6, 0, buttonWidth, buttonHeight);
        button.add(textLabel);


        return UIUtils.styleButton(button);
    }


    public void refresh() {
        ArrayList<Log> sortedLogs = getSortedLogs();
        logTableModel.setLogs(sortedLogs);
        logTableModel.fireTableDataChanged();
        applyFilter();
    }

    private ArrayList<Log> getSortedLogs() {

        ArrayList<Log> criticalLogs = new ArrayList<>(logManager.CriticalLogs);
        criticalLogs.sort((a, b) -> Integer.compare(b.getLogID(), a.getLogID()));
        ArrayList<Log> sorted = new ArrayList<>(criticalLogs);

        ArrayList<Log> warningLogs = new ArrayList<>(logManager.WarningLogs);
        warningLogs.sort((a, b) -> Integer.compare(b.getLogID(), a.getLogID()));
        sorted.addAll(warningLogs);

        ArrayList<Log> normalLogs = new ArrayList<>(logManager.NormalLogs);
        normalLogs.sort((a, b) -> Integer.compare(b.getLogID(), a.getLogID()));
        sorted.addAll(normalLogs);

        return sorted;
    }

    private void applyFilter() {
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends LogTableModel, ? extends Integer> e) {
                LogTableModel m = e.getModel();
                Log log = m.logs.get(e.getIdentifier());

                //Severity filter
                if (log.isSuppressed() && !showSuppressed) {
                    return false;
                }
                switch (log.getSeverity()) {
                    case Normal -> {
                        if (!showNormal) return false;
                    }
                    case Warning -> {
                        if (!showWarning) return false;
                    }
                    case Critical -> {
                        if (!showCritical) return false;
                    }
                }

                //Search filter
                if (searchText == null || searchText.isBlank()) return true;
                String s = searchText.toLowerCase();
                return log.getMessage().toLowerCase().contains(s) //Contains message

                        || log.getType().toString().toLowerCase().contains(s) //Type

                        || String.valueOf(log.getLogID()).contains(s) //ID

                        || log.getItemSerial().toLowerCase().contains(s); //Serial
            }
        });
    }

    public static void main(String[] args) {

        LogManager logManager = new LogManager();
        Inventory inventory = new Inventory();

        inventory.setLogManager(logManager);
        logManager.setInventory(inventory);
        ItemManager itemManager = new ItemManager(inventory);
        inventory.setItemManager(itemManager);

        PlatformSellerManager manager = new PlatformSellerManager(inventory, logManager);

        AmazonSeller amazon = new AmazonSeller(manager);
        EbaySeller ebay = new EbaySeller(manager);
        WalmartSeller walmart = new WalmartSeller(manager);

        // Sync all platforms
        amazon.syncOrders();
        ebay.syncOrders();
        walmart.syncOrders();



        for (int i = 1; i <= 10; i++) {
            String name = "Test Item " + i;
            String serial = "SER-" + String.format("%03d", i);
            int quantity = (int) (Math.random() * 50 + 1); // random 1–50
            int lowTrigger = (i % 3 == 0) ? 100 : 0; // every 3rd item has a low stock trigger

            String amazonSKU = "AMZ-" + serial;
            String ebaySKU = "EBY-" + serial;
            String walmartSKU = "WMT-" + serial;

            inventory.createItem(
                    name,
                    serial,
                    lowTrigger,
                    new ArrayList<>(), // no composition yet
                    null,
                    itemManager,
                    amazonSKU,
                    ebaySKU,
                    walmartSKU,
                    quantity
            );
        }

        Random rand = new Random();

        for (int i = 1; i <= 4; i++) {
            String name = "Composite " + i;
            String serial = "CMP-" + String.format("%03d", i);

            ArrayList<ItemPacket> components = new ArrayList<>();
            int numComponents = rand.nextInt(3) + 2; // 2–4 components

            for (int j = 0; j < numComponents; j++) {
                int componentIndex = rand.nextInt(10) + 1; // from 1–10 (existing items)
                String componentSerial = "SER-" + String.format("%03d", componentIndex);
                Item component = inventory.getItemBySerial(componentSerial);
                if (component == null) continue;

                int qty = rand.nextInt(3) + 1; // 1–3 units per component
                components.add(new ItemPacket(component, qty));
            }

            // skip if no valid components
            if (components.isEmpty()) continue;

            int quantity = rand.nextInt(10) + 1; // random stock for composite

            inventory.createItem(
                    name,
                    serial,
                    0, // optional trigger
                    components,
                    null, // no image for now
                    itemManager,
                    "AMZ-" + serial,
                    "EBY-" + serial,
                    "WMT-" + serial,
                    quantity
            );
        }
        //Creates main window
        SwingUtilities.invokeLater(() -> {
            DebugConsole.init();
            new MainWindow(inventory,logManager);

        });
    }
}
