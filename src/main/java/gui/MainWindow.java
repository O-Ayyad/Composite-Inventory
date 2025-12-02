package gui;

import platform.*;
import core.*;
import storage.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


public class MainWindow extends JFrame implements Inventory.ItemListener {

    public final int windowWidth = 1200;
    public final int windowHeight = 800;

    private Inventory inventory;
    private LogManager logManager;
    private APIFileManager apiFileManager;
    private PlatformManager platformManager;
    private FileManager fileManager;
    public UserConfigManager.UserConfig config;

    JPanel accountSummary;

    private JTable logTable;
    private LogTableModel logTableModel;

    private String searchText = "";
    private boolean showNormal = true;
    private boolean showWarning = true;
    private boolean showCritical = true;
    private boolean showSuppressed = false;

    private TableRowSorter<LogTableModel> sorter;

    private static final HashMap<Class<? extends SubWindow>, List<SubWindow>> subWindowInstances = new HashMap<>();

    private final Map<PlatformType, JPanel> platformSquares = new HashMap<>();
    private final Map<PlatformType, JLabel> platformLabels = new HashMap<>();

    Timer fetchTimer;
    Timer autoSaveTimer;


    public JFrame unlinkedFrame = null;

    private boolean doneLoading = false;

    DebugConsole debugConsole;


    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(MainWindow::new);
    }

    public MainWindow() {
        debugConsole = DebugConsole.init();
        debugConsole.toggle();

        setTitle("Composite Inventory");
        setSize(windowWidth, windowHeight);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JLabel loadingLabel = new JLabel("Loading Composite Inventory...", JLabel.CENTER);
        add(loadingLabel, BorderLayout.CENTER);
        setSize(300, 200);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fetchTimer.stop();
                autoSaveTimer.stop();
                if (platformManager.isFetching()) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Fetching is still in progress. Please wait...",
                            "Please wait",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    confirmCloseAllAndExit();
                }
            }

            private void confirmCloseAllAndExit() {
                int choice = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to exit? \nMake sure to save all data manually before exiting.",
                        "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if(choice == 0){
                    JDialog savingDialog = new JDialog((Frame) null, "Exiting", true);
                    savingDialog.setLayout(new BoxLayout(savingDialog.getContentPane(), BoxLayout.Y_AXIS));

                    JLabel label = new JLabel("Saving...");
                    label.setAlignmentX(Component.CENTER_ALIGNMENT);
                    label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

                    new Thread(() -> {
                        try {
                            for (Class<? extends SubWindow> subWin : subWindowInstances.keySet()) {
                                destroyAllExistingInstance(subWin);
                            }
                            fileManager.saveAll(false);
                        } finally {
                            SwingUtilities.invokeLater(() -> {
                                savingDialog.dispose();
                                dispose();
                                System.exit(0);
                            });
                        }
                    }).start();

                    savingDialog.setVisible(true);

                }else{
                    fetchTimer.restart();
                    autoSaveTimer.restart();
                }
            }
        });


        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    bootstrap();
                } catch (Exception ex) {
                    System.out.println(Arrays.toString(ex.getStackTrace()));
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(MainWindow.this,
                                    "Error during initialization: " + ex.getMessage(),
                                    "Initialization Error",
                                    JOptionPane.ERROR_MESSAGE)
                    );
                }
                return null;
            }

            @Override
            protected void done() {
                doneLoading = true;
                getContentPane().removeAll();

                revalidate();
                repaint();
                setUI();

                revalidate();
                repaint();
            }
        }.execute();


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
        if(iconPath.equals("icons/windowIcons/link.png")){
            iconHeight = 70;
            iconWidth = 70;
        }
        Image scaled = icon.getImage().getScaledInstance(iconWidth, iconHeight, Image.SCALE_SMOOTH);
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
        subWindowInstances
                .computeIfAbsent(subWindow.getClass(), k -> new ArrayList<>())
                .add(subWindow);
    }
    public List<SubWindow> getInstances(Class<? extends SubWindow> clazz) {
        return subWindowInstances.getOrDefault(clazz, new ArrayList<>());
    }
    public boolean hasInstance(Class<? extends SubWindow> clazz) {
        List<SubWindow> instances = subWindowInstances.get(clazz);
        if(instances == null){
            return false;
        }
        return !instances.isEmpty();
    }

    public void removeInstance(SubWindow subWindow) {
        if (subWindow == null) return;

        List<SubWindow> instances = subWindowInstances.get(subWindow.getClass());
        if (instances != null) {
            instances.remove(subWindow);

            if (instances.isEmpty()) {
                subWindowInstances.remove(subWindow.getClass());
            }
        }
    }
    //Destroys the current window of type param
    public void destroyExistingInstance(SubWindow subWindow) {
        if (subWindow == null) return;
        removeInstance(subWindow);
        subWindow.dispose();
    }
    public void destroyAllExistingInstance(Class<? extends SubWindow> clazz) {
        List<SubWindow> instances = new ArrayList<>(getInstances(clazz));

        for (SubWindow window : instances) {
            removeInstance(window);
            window.dispose();
        }
    }
    //Used to remove windows about an item when an item is deleted
    public void checkAndDestroy(Item i) {

        ArrayList<SubWindow> toDestroy = new ArrayList<>();

        for (List<SubWindow> instances : new ArrayList<>(subWindowInstances.values())) {
            for (SubWindow curr : new ArrayList<>(instances)) {
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
        }
        for (SubWindow window : toDestroy) {
            destroyExistingInstance(window);
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

    public void onChange(Item item) {
        checkAndDestroy(item);
    }

    public void addToStartupWindows(){

    }

    private void bootstrap() {
        // Create managers
        inventory = new Inventory();
        logManager = new LogManager();

        inventory.setLogManager(logManager);
        logManager.setInventory(inventory);

        ItemManager itemManager = new ItemManager(inventory);
        inventory.setItemManager(itemManager);

        apiFileManager = new APIFileManager(this);

        platformManager = new PlatformManager(inventory, logManager, apiFileManager);
        platformManager.setMainWindow(this);

        fileManager = new FileManager(inventory, logManager, platformManager, this);
        config = fileManager.getUserConfig();

        if(config.hasConnect){
            apiFileManager.initializePassword(false);
        }

        platformManager.setFileManager(fileManager);

        logManager.addChangeListener(() -> SwingUtilities.invokeLater(this::refresh));
        inventory.addChangeListener(this);

        // Autosave
        autoSaveTimer = new Timer(config.autoSaveTimer, e -> {
            try {
                if (platformManager.isFetching()) {
                    System.out.println("Did not autosave since orders are being fetched.");
                    return;
                }
                fileManager.saveAll(true);
            } catch (Exception ex) {
                System.out.println("Could not autosave. " + ex.getMessage());
            }
        });


        // Auto fetch
        fetchTimer = new Timer(config.autofetchTimer, e -> {
            try {
                if ( platformManager.isFetching() || platformManager.onCooldown() || !doneLoading) {
                    return;
                }
                platformManager.fetchAllRecentOrders();
            }
            catch (Exception ex) {
                System.out.println("Could not auto fetch. " + ex.getMessage());
            }
        });
    }
    void setUI(){
        autoSaveTimer.setInitialDelay(config.autofetchTimer);
        autoSaveTimer.start();
        fetchTimer.start();


        setLocationRelativeTo(null); // center
        setLayout(new BorderLayout());



        // ---------------- LOGO, BUTTONS, INFO BAR ----------------
        JPanel topContainer = new JPanel(new BorderLayout()); // Holds buttons and top bar

        // Logo Panel
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBorder(new EmptyBorder(10, 120, 10, 10));

        logoPanel.setBackground(UIUtils.BACKGROUND_MAIN);


        URL logoURL = getClass().getClassLoader().getResource("icons/windowIcons/logo.png");
        if (logoURL == null) {
            System.err.println("Logo image not found!");
        } else {
            ImageIcon logo = new ImageIcon(logoURL);
            Image scaledLogo = logo.getImage().getScaledInstance(400, 100, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));

            JPanel leftWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            leftWrapper.setOpaque(false);
            leftWrapper.add(logoLabel);

            logoPanel.add(logoLabel, BorderLayout.WEST);
        }

        // Links and Authorship
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15)); //
        rightPanel.setOpaque(false);

        JLabel authorLabel = new JLabel("<html><div style=\"text-align: center;\">Made by O-Ayyad<br><br>oayyad1@proton.me<br><br>Contact for any bugs or questions</div></html>");
        authorLabel.setFont(UIUtils.FONT_ARIAL_LARGE );
        rightPanel.add(authorLabel);

        //Links
        JButton githubLink = UIUtils.createLinkButton("Github", "https://github.com/O-Ayyad", "icons/windowIcons/github.png");
        rightPanel.add(githubLink);

        JPanel rightWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightWrapper.setOpaque(false);
        rightWrapper.add(rightPanel);

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


        addButton.setEnabled(true);
        removeButton.setEnabled(true);
        viewButton.setEnabled(true);
        linkButton.setEnabled(true);



        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK), "openAdd");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK), "openRemove");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.SHIFT_DOWN_MASK), "openView");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.SHIFT_DOWN_MASK), "openLink");

        am.put("openAdd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (doneLoading) new AddWindow(MainWindow.this, inventory);
            }
        });
        am.put("openRemove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (doneLoading) new RemoveWindow(MainWindow.this, inventory, RemoveWindow.SendTo.Reduce);
            }
        });
        am.put("openView", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (doneLoading) new ViewWindow(MainWindow.this, inventory, logManager);
            }
        });
        am.put("openLink", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (doneLoading) new LinkWindow(MainWindow.this, inventory, apiFileManager);
            }
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


        add(topContainer, BorderLayout.PAGE_START);

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
        JButton fetchOrdersBtn = new JButton("Fetch Orders");
        JButton unlinkedItemsBtn = new JButton("<html><div style='text-align:center;'>Find unlinked items<br>on platforms</div></html>");
        JButton saveInfoBtn = new JButton("Save information");
        JButton openDebugBtn = new JButton("<html><div style='text-align:center;'>Open Debug Console<br>(Shift + ~)</div></html>");

        JButton[] toolButtons = {fetchOrdersBtn, unlinkedItemsBtn,saveInfoBtn, openDebugBtn};
        for (JButton btn : toolButtons) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(200, 45));
            UIUtils.styleButton(btn);
            leftTools.add(btn);
            leftTools.add(Box.createRigidArea(new Dimension(0, 20)));
        }
        fetchOrdersBtn.addActionListener(e -> {
            if(!fetchOrdersBtn.isEnabled()) return;
            platformManager.fetchAllRecentOrders();
        });


        Timer fetchOrderButtonChecker = new Timer(500, e -> {
            boolean fetching = platformManager.isFetching();
            boolean onCooldown = platformManager.onCooldown();

            String timeRemaining = "";
            if(onCooldown) {
                long totalSeconds = java.time.Duration.between(
                        LocalDateTime.now(),
                        platformManager.getLastFetchTime().plusSeconds(platformManager.fetchTimeCooldownSeconds)
                ).getSeconds();

                long seconds = totalSeconds % 60;
                timeRemaining = seconds + " s";
            }

            final String finalTimeRemaining = timeRemaining;
            final boolean finalFetching = fetching;
            final boolean finalOnCooldown = onCooldown;

            SwingUtilities.invokeLater(() -> {
                fetchOrdersBtn.setEnabled(!finalFetching && !finalOnCooldown);

                if(finalFetching) {
                    fetchOrdersBtn.setText("<html>Fetch Orders<br>(fetching...)</html>");
                } else if(finalOnCooldown) {
                    fetchOrdersBtn.
                            setText("<html><div style='text-align: center;'>Fetch Orders<br>(" + finalTimeRemaining + ")</div></html>");
                } else {
                    fetchOrdersBtn.setText("Fetch Orders");
                }
            });
        });

        fetchOrderButtonChecker.start();


        unlinkedItemsBtn.setEnabled(false);
        Timer unlinkedItemsBtnChecker = new Timer(50, e -> {
            boolean anyConnected = false;
            for (PlatformType p : PlatformType.values()) {
                if (apiFileManager.hasToken(p)) {
                    anyConnected = true;
                    break;
                }
            }
            boolean alreadyExist = unlinkedFrame != null;
            unlinkedItemsBtn.setEnabled(anyConnected && !alreadyExist || platformManager.isFetching());
        });
        unlinkedItemsBtnChecker.start();

        unlinkedItemsBtn.addActionListener(e->{
            int choice = JOptionPane.showConfirmDialog(
                    null,
                    """
                    
                    
                    This action will give you all items
                    on your connected platforms via their SKU.
            
                    If an item is linked then it will tell you so,
                    on the other hand if an item is not linked
                    then it will tell you the name of the unlinked item
                    as it appears on the platform.
            
                    This action will take around 15 seconds for smaller inventories.
                    Proceed?
                    
                    """,
                    "Unlinked Item Confirmation",
                    JOptionPane.YES_NO_OPTION
            );
            if(choice != JOptionPane.YES_OPTION){
                return;
            }
            unlinkedFrame = new JFrame("Unlinked Items") {
                @Override
                public void dispose() {
                    super.dispose();
                    unlinkedFrame = null;
                }
            };

            unlinkedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


            JTextPane textPane = new JTextPane();
            textPane.setEditable(false);

            textPane.setFont(UIUtils.FONT_UI_BOLD);

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setBackground(UIUtils.BACKGROUND_MAIN);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

            unlinkedFrame.add(scrollPane);
            unlinkedFrame.setSize(1400, 800);
            unlinkedFrame.setLocationRelativeTo(this);

            JDialog loading = new JDialog(unlinkedFrame, "Loading", false);
            JLabel label = new JLabel("Loading unlinked items report...", JLabel.CENTER);
            label.setFont(UIUtils.FONT_UI_LARGE_BOLD);
            loading.add(label, BorderLayout.CENTER);
            loading.setSize(300,200);
            loading.setLocationRelativeTo(unlinkedFrame);
            loading.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            loading.setVisible(true);


            SwingWorker<List<List<String>>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<List<String>> doInBackground(){
                    return platformManager.getAllUnlinkedItems();
                }

                @Override
                protected void done() {
                    try {
                        List<List<String>> message = get();
                        StyledDocument doc = textPane.getStyledDocument();

                        Style critical = textPane.addStyle("critical", null);
                        StyleConstants.setBackground(critical, UIUtils.CRITICAL_COLOR);

                        Style success = textPane.addStyle("success", null);
                        StyleConstants.setBackground(success, UIUtils.LINK_SUCCESS);

                        Style separator = textPane.addStyle("separator", null);
                        StyleConstants.setBackground(separator, UIUtils.NORMAL_COLOR);
                        StyleConstants.setFontSize(separator, 24);

                        for (List<String> list : message) {
                            for (String item : list) {
                                String[] lines = item.split("\n");

                                for (String line : lines) {

                                    if (line.isEmpty()) continue;

                                    Style style;
                                    String lowerLine = line.toLowerCase();

                                    if (lowerLine.contains("is registered") || lowerLine.contains("•")) { //Registered item
                                        style = success;

                                    } else if (lowerLine.contains("not registered") || lowerLine.contains("||--")) { //Not registered
                                        style = critical;

                                    }else{ //Seperator
                                        style = separator;
                                    }

                                    try {
                                        doc.insertString(doc.getLength(), line + "\n", style);
                                    } catch (BadLocationException ex) {
                                        System.out.println(Arrays.toString(ex.getStackTrace()));
                                    }
                                }
                                try {
                                    doc.insertString(doc.getLength(),  "\n", null);
                                } catch (BadLocationException ex) {
                                    System.out.println(Arrays.toString(ex.getStackTrace()));
                                }
                            }
                        }
                        textPane.setBorder(new EmptyBorder(40, 60,0,40));
                        scrollPane.setViewportView(textPane);
                        unlinkedFrame.add(scrollPane);
                        unlinkedFrame.setVisible(true);
                    } catch (Exception e) {
                        System.out.println(Arrays.toString(e.getStackTrace()));
                    } finally {
                        loading.dispose();
                    }
                }
            };
            worker.execute();
        });



        saveInfoBtn.addActionListener(e->{
            try{
                if(platformManager.isFetching()){
                    JOptionPane.showMessageDialog(this,
                            "Could not save information since orders are being fetched",
                            "Cannot save",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                fileManager.saveAll(false);

                JOptionPane.showMessageDialog(this,
                        "Successfully saved Inventory, Logs and Orders to disk",
                        "Saving Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }catch (Exception ex){
                JOptionPane.showMessageDialog(this,
                        ex.getMessage(),
                        "Saving Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        });

        fetchOrderButtonChecker.start();



        openDebugBtn.addActionListener(e->{
            if(debugConsole.isVisible()){
                debugConsole.toFront();
            }else {
                debugConsole.toggle();
            }
        });

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
        new Timer(500, e -> {
            for (PlatformType p : PlatformType.values()) {
                JPanel square = platformSquares.get(p);
                JLabel text = platformLabels.get(p);

                if (apiFileManager.hasToken(p)) {
                    BaseSeller seller = platformManager.getSeller(p);

                    if (seller.fetchingOrders) {
                        square.setBackground(UIUtils.FETCHING_ORDERS);
                        text.setText(p.getDisplayName() + " (Fetching orders...)");
                    } else {
                        square.setBackground(UIUtils.LINK_SUCCESS);
                        text.setText(p.getDisplayName() + " (Connected)");
                    }
                } else {
                    square.setBackground(Color.GRAY);
                    text.setText(p.getDisplayName() + " (Not Connected)");
                }
            }
        }).start();

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.add(leftWrapper, BorderLayout.WEST);
        mainArea.add(logsPanel, BorderLayout.CENTER);
        mainArea.add(accountSummary,BorderLayout.SOUTH);

        add(mainArea, BorderLayout.CENTER);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true);
        System.out.println("Welcome to Composite Inventory!");
    }
    public void setHasConnected(boolean val){
        fileManager.setHasConnected(val);
    }
}

