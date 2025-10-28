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
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

public class MainWindow extends JFrame {

    public final int windowWidth = 1200;
    public final int windowHeight = 800;

    private static final double COL_LOG_PCT = 9.0;
    private static final double COL_TYPE_PCT = 14.0;
    private static final double COL_ACTION_PCT = 65.0;
    private static final double COL_TIME_PCT = 12.0;

    private static int TABLE_WIDTH = 864;

    private final LogManager logManager;

    private final JTable logTable;
    private LogTableModel logTableModel;

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
        logoPanel.setBackground(new Color(240, 240, 240));

        // Logo
        ImageIcon logo = new ImageIcon("icons/logo.png"); // make sure logo.png exists
        Image scaledLogo = logo.getImage().getScaledInstance(300, 50, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoPanel.add(logoLabel, BorderLayout.WEST);

        // Links and Authorship
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15)); //
        rightPanel.setOpaque(false);

        JLabel authorLabel = new JLabel("Authorship Text");
        authorLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        rightPanel.add(authorLabel);

        //Links
        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new GridLayout(2, 1, 0, 0));
        JButton githubLink = createLinkButton("Github", "https://github.com/O-Ayyad", "icons/windowIcons/github.png");

        //Temp
        JButton docsLink = createLinkButton("Docs", "https://example.com/docs", "icons/temp");
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
        addButton.setToolTipText("Add or Create Items (Shift+D)");
        removeButton.setToolTipText("Sell or Remove Items (Shift+F)");
        viewButton.setToolTipText("View and Edit Inventory (Shift+G)");
        linkButton.setToolTipText("Link Accounts (Shift+H)");

        // Add action listeners

        //Shift + D,FG,H opens the windows form left to right
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK), "openAdd");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK), "openRemove");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.SHIFT_DOWN_MASK), "openView");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.SHIFT_DOWN_MASK), "openLink");

        am.put("openAdd", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new AddWindow(MainWindow.this, inventory); }
        });
        am.put("openRemove", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new RemoveWindow(MainWindow.this, inventory); }
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
        removeButton.addActionListener(e -> {new RemoveWindow(this,inventory);
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
        JCheckBox showWarningBox = new JCheckBox("⚠\uFE0F Warning", true);
        JCheckBox showCriticalBox = new JCheckBox("❌ Critical", true);
        JCheckBox showSuppressedBox = new JCheckBox("💤 Suppressed", true);

        for (JCheckBox box : new JCheckBox[]{showNormalBox, showWarningBox, showCriticalBox, showSuppressedBox}) {
            box.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
            box.setOpaque(false);
            filterPanel.add(box);
        }

        //Log display
        JPanel logsPanel = new JPanel(new BorderLayout());

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 160),2), "Logs", TitledBorder.CENTER,TitledBorder.DEFAULT_POSITION
        );
        border.setTitleFont(new Font("Arial",Font.PLAIN ,18));
        border.setTitleColor(new Color(100, 100, 110));

        logsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 30, 100, 40),
                border
        ));


        ArrayList<Log> sortedLogs = getSortedLogs();

        // Table setup
        logTableModel = new LogTableModel(sortedLogs);

        logTable = new JTable(logTableModel);

        JTableHeader header = logTable.getTableHeader();


        header.setBackground(new Color(230, 230, 240));
        header.setForeground(Color.DARK_GRAY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 235)));
        header.setPreferredSize(new Dimension(header.getWidth(), 30)); // taller header
        header.setReorderingAllowed(false); // optional: prevent column drag

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        sorter = new TableRowSorter<>(logTableModel);
        sorter.setComparator(0, (a, b) -> {
            int idA = Integer.parseInt(a.toString().replaceAll("\\D", ""));
            int idB = Integer.parseInt(b.toString().replaceAll("\\D", ""));
            return Integer.compare(idA, idB);
        });
        logTable.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            void update() { searchText = searchField.getText(); applyFilter(); }
            public void insertUpdate(DocumentEvent e){ update(); }
            public void removeUpdate(DocumentEvent e){ update(); }
            public void changedUpdate(DocumentEvent e){ update(); }
        });
        showNormalBox.addActionListener(e -> { showNormal = showNormalBox.isSelected(); applyFilter(); });
        showWarningBox.addActionListener(e -> { showWarning = showWarningBox.isSelected(); applyFilter(); });
        showCriticalBox.addActionListener(e -> { showCritical = showCriticalBox.isSelected(); applyFilter(); });
        showSuppressedBox.addActionListener(e -> { showSuppressed = showSuppressedBox.isSelected(); applyFilter(); });

        logsPanel.add(filterPanel, BorderLayout.SOUTH);


        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        var cm = logTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth((int)(TABLE_WIDTH * COL_LOG_PCT / 100));
        cm.getColumn(1).setPreferredWidth((int)(TABLE_WIDTH * COL_TYPE_PCT / 100));
        cm.getColumn(2).setPreferredWidth((int)(TABLE_WIDTH * COL_ACTION_PCT / 100));
        cm.getColumn(3).setPreferredWidth((int)(TABLE_WIDTH * COL_TIME_PCT / 100));

        logTable.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        logTable.setRowHeight(28);
        logTable.setFillsViewportHeight(true);

        //Scrolling
        JScrollPane scrollPane = new JScrollPane(
                logTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                int modelRow = table.convertRowIndexToModel(row);
                LogTableModel model = (LogTableModel) table.getModel();
                Log log = model.getLogAt(modelRow);

                Color normalColor = new Color(220, 220, 235); //light purple
                Color warningColor = new Color(255, 250, 205); //light yellow
                Color criticalColor = new Color(255, 204, 204); //light red

                Color revertedColor = new Color(210, 200, 210); //gray

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else if(log.isReverted()){
                    c.setBackground(revertedColor);
                }else {
                    switch (log.getSeverity()) {
                        case Normal -> c.setBackground(normalColor);
                        case Warning -> c.setBackground(warningColor);
                        case Critical -> c.setBackground(criticalColor);
                    }
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        });
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
    //Link buttons
    private JButton createLinkButton(String text, String url, String iconPath) {
        JButton link;

        if (!iconPath.isEmpty()) {
            // Load and scale icon
            ImageIcon icon = new ImageIcon(iconPath);
            Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            link = new JButton("<HTML><U>" + text + "</U></HTML>", new ImageIcon(scaled));
        } else {
            link = new JButton("<HTML><U>" + text + "</U></HTML>");
        }

        link.setForeground(Color.BLUE);
        link.setBorderPainted(false);
        link.setOpaque(false);
        link.setBackground(null);
        link.setContentAreaFilled(false);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { link.setForeground(new Color(0, 102, 204)); }
            @Override public void mouseExited(MouseEvent e) { link.setForeground(Color.BLUE); }
        });
        link.setFocusPainted(false);
        return link;
    }

    public void refresh() {
        ArrayList<Log> sortedLogs = getSortedLogs();
        logTableModel.setLogs(sortedLogs);
        logTableModel.fireTableDataChanged();
        applyFilter();

        applyFilter();

        SwingUtilities.invokeLater(() -> {


            var cm = logTable.getColumnModel();
            cm.getColumn(0).setPreferredWidth((int)(TABLE_WIDTH * COL_LOG_PCT / 100));
            cm.getColumn(1).setPreferredWidth((int)(TABLE_WIDTH * COL_TYPE_PCT / 100));
            cm.getColumn(2).setPreferredWidth((int)(TABLE_WIDTH * COL_ACTION_PCT / 100));
            cm.getColumn(3).setPreferredWidth((int)(TABLE_WIDTH * COL_TIME_PCT / 100));

            JScrollBar vertical = ((JScrollPane) logTable.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public static void applySort(JTable table, int columnIndex, SortOrder order) {
        if (table == null) return;

        RowSorter<? extends TableModel> sorter = table.getRowSorter();

        // If no sorter exists, create one
        if (sorter == null && table.getModel() != null) {
            sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);
        }

        if (sorter != null) {
            ArrayList<RowSorter.SortKey> keys = new ArrayList<>();
            keys.add(new RowSorter.SortKey(columnIndex, order));
            sorter.setSortKeys(keys);
            if (sorter instanceof TableRowSorter) {
                ((TableRowSorter<?>) sorter).sort();
            }
        }
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
        sorter.setRowFilter(new RowFilter<LogTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends LogTableModel, ? extends Integer> e) {
                LogTableModel m = e.getModel();
                Log log = m.logs.get(e.getIdentifier());

                //Severity filter
                if(log.isSuppressed() && !showSuppressed){
                    return false;
                }
                switch (log.getSeverity()) {
                    case Normal -> { if (!showNormal) return false; }
                    case Warning -> { if (!showWarning) return false; }
                    case Critical -> { if (!showCritical) return false; }
                }

                //Search filter
                if (searchText == null || searchText.isBlank()) return true;
                String s = searchText.toLowerCase();
                return     log.getMessage().toLowerCase().contains(s) //Contains message

                        || log.getType().toString().toLowerCase().contains(s) //Type

                        || String.valueOf(log.getLogID()).contains(s) //ID

                        || log.getItemSerial().toLowerCase().contains(s); //Serial
            }
        });
    }

    static class LogTableModel extends AbstractTableModel {
        private static final String[] cols = {"Log #", "Type", "Action", "Time"};
        private final ArrayList<Log> logs;

        LogTableModel(ArrayList<Log> logs){ this.logs = logs; }


        public int getRowCount(){ return logs.size(); }
        public int getColumnCount(){ return cols.length; }
        public String getColumnName(int c){ return cols[c]; }

        public Object getValueAt(int r, int c){
            Log l = logs.get(r);
            return switch(c){
                case 0 -> (switch(l.getSeverity()){
                    case Normal -> "✅ "; case Warning -> "⚠\uFE0F "; case Critical -> "❌ ";
                })  + l.getLogID();
                case 1 -> l.getType();
                case 2 -> l.getMessage();
                case 3 -> l.getTimestamp().format(
                        java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));
                default -> "";
            };
        }
        public void setLogs(ArrayList<Log> newLogs) {
            logs.clear();
            logs.addAll(newLogs);
        }

        public Log getLogAt(int row) {
            return logs.get(row);
        }
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


        //Creates main window
        SwingUtilities.invokeLater(() -> new MainWindow(inventory,logManager));
    }
}
