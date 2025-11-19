package gui;

import platform.*;
import core.*;
import storage.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;


public class MainWindow extends JFrame implements Inventory.ItemListener {

    public final int windowWidth = 1200;
    public final int windowHeight = 800;

    private final LogManager logManager;

    JPanel accountSummary;
    private final JTable logTable;
    private final LogTableModel logTableModel;

    private String searchText = "";
    private boolean showNormal = true;
    private boolean showWarning = true;
    private boolean showCritical = true;
    private boolean showSuppressed = true;

    private final TableRowSorter<LogTableModel> sorter;

    private static final HashMap<Class<? extends SubWindow>, SubWindow> subWindowInstances = new HashMap<>();

    private final Map<PlatformType, JPanel> platformSquares = new HashMap<>();
    private final Map<PlatformType, JLabel> platformLabels = new HashMap<>();

    public MainWindow() {

        DebugConsole.init();

        Inventory inventory = new Inventory();
        logManager = new LogManager();

        inventory.setLogManager(logManager);
        logManager.setInventory(inventory);

        ItemManager itemManager = new ItemManager(inventory);
        inventory.setItemManager(itemManager);

        APIFileManager apiFileManager = new APIFileManager();

        PlatformManager platformManager = new PlatformManager(inventory, logManager, apiFileManager);
        platformManager.setMainWindow(this);

        InventoryFileManager inventoryFileManager = new InventoryFileManager(inventory);
        LogFileManager logFileManager = new LogFileManager(logManager);
        OrderFileManager orderFileManager = new OrderFileManager(platformManager);

        platformManager.setOrderFileManager(orderFileManager);



        // Autosave
        Timer autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                inventoryFileManager.saveInventory();
                logFileManager.saveLogs();
            }
        }, 3000, 3000);


        logManager.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));

        inventory.addChangeListener(this);


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                for(Class<? extends SubWindow> subWin : subWindowInstances.keySet()){
                    destroyExistingInstance(subWin);
                }
                System.out.println("Saving before exit...");
                inventoryFileManager.saveInventory();
                logFileManager.saveLogs();
            }
        });

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
            @Override public void actionPerformed(ActionEvent e) { new RemoveWindow(MainWindow.this, inventory, RemoveWindow.SendTo.Reduce); }
        });
        am.put("openView", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new ViewWindow(MainWindow.this, inventory,logManager); }
        });
        am.put("openLink", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { new LinkWindow(MainWindow.this, inventory,apiFileManager); }
        });
        requestFocusInWindow();

        addButton.addActionListener(e -> {new AddWindow(this,inventory);
                requestFocusInWindow();});
        removeButton.addActionListener(e -> {new RemoveWindow(this,inventory, RemoveWindow.SendTo.Reduce);
                requestFocusInWindow();});
        viewButton.addActionListener(e -> {new ViewWindow(this,inventory,logManager);
                requestFocusInWindow();});
        linkButton.addActionListener(e -> {new LinkWindow(this,inventory,apiFileManager);
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
        JButton[] toolButtons = new JButton[buttonLabels.length];
        for (int i = 0; i < buttonLabels.length; i++) {
            JButton btn = new JButton(buttonLabels[i]);
            toolButtons[i] = btn;
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(180, 35));
            UIUtils.styleButton(btn);
            leftTools.add(btn);
            leftTools.add(Box.createRigidArea(new Dimension(0, 20)));
        }
        toolButtons[0].addActionListener(e->platformManager.fetchAllRecentOrders());
        toolButtons[1].addActionListener(e->orderFileManager.saveOrders());
        toolButtons[2].addActionListener(e->orderFileManager.loadOrders());

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5)); // horizontal layout
        filterPanel.setOpaque(false);

        //Check Boxes
        JCheckBox showNormalBox = new JCheckBox("✅ Normal", true);
        JCheckBox showWarningBox = new JCheckBox("⚠️ Warning", true);
        JCheckBox showCriticalBox = new JCheckBox("❌ Critical", true);
        JCheckBox showSuppressedBox = new JCheckBox("💤 Suppressed", false);

        for (JCheckBox box : new JCheckBox[]{showNormalBox, showWarningBox, showCriticalBox, showSuppressedBox}) {
            box.setFont(UIUtils.FONT_UI_REGULAR);
            box.setOpaque(false);
            filterPanel.add(box);
        }

        //Log display
        JPanel logsPanel = new JPanel(new BorderLayout());
        logsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 30, 0, 40),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                        "Logs",
                        TitledBorder.CENTER,
                        TitledBorder.DEFAULT_POSITION,
                        UIUtils.FONT_ARIAL_LARGE_BOLD,
                        UIUtils.BORDER_DARK
                )
        ));


        logTableModel = new LogTableModel(logManager.AllLogs);

        logTable = new JTable(logTableModel);

        sorter = new TableRowSorter<>(logTableModel);
        logTable.setRowSorter(sorter);
        sorter.setSortsOnUpdates(true);

        LogTableModel.styleTable(logTable);

        logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        logTable.setFont(UIUtils.FONT_UI_REGULAR);
        logTable.setRowHeight(28);
        logTable.setFillsViewportHeight(true);

        LogTableModel.attachOpenListener(logTable,
                log -> new LogWindow(this, inventory, log, logManager));

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

        accountSummary = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        accountSummary.setLayout(new BoxLayout(accountSummary, BoxLayout.Y_AXIS));
        accountSummary.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        for (PlatformType p : PlatformType.values()) {


            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

            JPanel square = new JPanel();
            square.setPreferredSize(new Dimension(14, 14));
            square.setBackground(Color.GRAY);

            JLabel label = new JLabel(p.getDisplayName());
            label.setPreferredSize(new Dimension(300, 20));
            label.setFont(UIUtils.FONT_UI_BOLD);

            platformSquares.put(p, square);
            platformLabels.put(p, label);

            row.add(square);
            row.add(label);
            accountSummary.add(row);
            accountSummary.add(Box.createVerticalStrut(10));
        }
        Timer accountSquares = new Timer(true);
        accountSquares.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (PlatformType p : PlatformType.values()) {
                    JPanel square = platformSquares.get(p);
                    JLabel text = platformLabels.get(p);
                    if (apiFileManager.hasToken(p)) {
                        BaseSeller<?> seller = platformManager.getSeller(p);
                        if(seller.fetchingOrders){
                            square.setBackground(UIUtils.FETCHING_ORDERS);
                            text.setText(p.getDisplayName() + " (Fetching orders...)");
                        }else{
                            square.setBackground(UIUtils.LINK_SUCCESS);
                            text.setText(p.getDisplayName() + " (Connected)");
                        }
                    }else{
                        square.setBackground(Color.GRAY);
                        text.setText(p.getDisplayName() + " (Not Connected)");
                    }
                }
            }
        }, 500, 500);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.add(leftWrapper, BorderLayout.WEST);
        mainArea.add(logsPanel, BorderLayout.CENTER);
        mainArea.add(accountSummary,BorderLayout.SOUTH);

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

    public void addInstance(SubWindow subWindow) {
        subWindowInstances.put(subWindow.getClass(), subWindow);
    }
    public SubWindow getInstance(Class<? extends SubWindow> clazz) {
        return subWindowInstances.get(clazz);
    }
    public boolean hasInstance(Class<? extends SubWindow> clazz) {
        return subWindowInstances.containsKey(clazz);
    }
    public void removeInstance(SubWindow subWindow) {
        if (subWindow == null) return;

        SubWindow curr = getInstance(subWindow.getClass());
        if (curr == subWindow) {
            subWindowInstances.remove(subWindow.getClass());
        }
    }
    //Destroys the current window of type param
    public void destroyExistingInstance(Class<? extends SubWindow> clazz){
        SubWindow curr = getInstance(clazz);
        if(curr == null) return; // nothing to destroy
        removeInstance(curr);
        curr.dispose();
    }
    //Used to remove windows about an item when an item is deleted
    public void checkAndDestroy(Item i) {
        System.out.println("Called on item :" + i.getName());
        ArrayList<SubWindow> toDestroy = new ArrayList<>();
        for (SubWindow curr : subWindowInstances.values()) {
            if (curr instanceof EditWindow e && e.selectedItem == i) {
                toDestroy.add(curr);
            }
            else if (curr instanceof ItemInfoWindow e && e.item == i) {
                toDestroy.add(curr);
            }
            else if (curr instanceof AddWindow e && e.selected == i) {
                toDestroy.add(curr);
            }
            else if (curr instanceof RemoveWindow e && e.selected == i) {
                toDestroy.add(curr);
            }
        }
        for (SubWindow window : toDestroy) {
            destroyExistingInstance(window.getClass());
        }
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

                        || log.getSerial().toLowerCase().contains(s); //Serial
            }
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }

    private static void printSolutions(List<Map<Item, Integer>> solutions) {
        if (solutions.isEmpty()) {
            System.out.println("No solutions found!");
        } else {
            System.out.println("Found " + solutions.size() + " solution(s):");
            for (int i = 0; i < solutions.size(); i++) {
                System.out.print("  Solution " + (i + 1) + ": Break down ");
                Map<Item, Integer> solution = solutions.get(i);
                boolean first = true;
                int totalItems = 0;
                for (Map.Entry<Item, Integer> entry : solution.entrySet()) {
                    if (!first) System.out.print(", ");
                    System.out.print(entry.getValue() + "x " + entry.getKey().getName());
                    totalItems += entry.getValue();
                    first = false;
                }
                System.out.println(" (Total: " + totalItems + " items)");
            }
        }
    }
    public void onChange(Item item) {
        checkAndDestroy(item);
    }
}
