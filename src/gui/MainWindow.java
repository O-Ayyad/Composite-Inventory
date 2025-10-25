package gui;
import core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class MainWindow extends JFrame {
    public final int windowWidth = 1200;
    public final int windowHeight = 800;

    public MainWindow(Inventory inventory) {
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
        JButton githubLink = createLinkButton("Github", "https://github.com/O-Ayyad", "icons/github");
        //Temp
        JButton docsLink = createLinkButton("Docs", "https://example.com/docs", "icons/temp");
        rightPanel.add(githubLink);
        rightPanel.add(docsLink);

        logoPanel.add(rightPanel, BorderLayout.EAST); //Add authorship and LinksF

        add(logoPanel, BorderLayout.NORTH);

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
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isShiftDown()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_D:
                            new AddWindow(MainWindow.this, inventory);
                            e.consume();
                            break;
                        case KeyEvent.VK_F:
                            new RemoveWindow(MainWindow.this, inventory);
                            e.consume();
                            break;
                        case KeyEvent.VK_G:
                            new ViewWindow(MainWindow.this, inventory);
                            e.consume();
                            break;
                        case KeyEvent.VK_H:
                            new LinkWindow(MainWindow.this, inventory);
                            e.consume();
                            break;
                    }
                }
            }
        });
        setFocusable(true);
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

        return link;
    }

    public void refresh() {
        // Placeholder for refreshing GUI
    }

    public static void main(String[] args) {

        LogManager logManager = new LogManager();
        Inventory inventory = new Inventory();

        inventory.setLogManager(logManager);
        logManager.setInventory(inventory);
        ItemManager itemManager = new ItemManager(inventory);
        inventory.setItemManager(itemManager);

        //Creates main window
        SwingUtilities.invokeLater(() -> new MainWindow(inventory));
    }
}
