package GUI;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class MainWindow extends JFrame {
    public final int windowWidth = 1200;
    public final int windowHeight = 800;

    private JFrame openSubWindow = null;

    public MainWindow() {
        setTitle("Composite Inventory");
        setSize(windowWidth, windowHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center
        setResizable(false);

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
        rightPanel.add(authorLabel);;

        //Links
        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new GridLayout(2, 1, 0, 0));
        JButton githubLink = createLinkButton("Github", "https://github.com/O-Ayyad", "icons/github");
        //Temp
        JButton docsLink = createLinkButton("Docs", "https://example.com/docs", "icons/temp");
        rightPanel.add(githubLink);
        rightPanel.add(docsLink);

        logoPanel.add(rightPanel, BorderLayout.EAST); //Add authorship and Links

        add(logoPanel, BorderLayout.NORTH);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 0, 0)); // 1 row, 4 columns

        JButton addButton = createIconButton("Add", "icons/plus.png");
        JButton removeButton = createIconButton("Remove", "icons/minus.png");
        JButton itemButton = createIconButton("Item", "icons/item.png");
        JButton linkButton = createIconButton("Link", "icons/link.png");

        // Add action listeners
        addButton.addActionListener(e -> new AddWindow(this));
        removeButton.addActionListener(e -> new RemoveWindow(this));
        itemButton.addActionListener(e -> new ItemWindow(this));
        linkButton.addActionListener(e -> new LinkWindow(this));

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(itemButton);
        buttonPanel.add(linkButton);

        // Combine
        topContainer.add(logoPanel, BorderLayout.NORTH);
        topContainer.add(buttonPanel, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        setVisible(true);
    }

    // Window Buttons
    private JButton createIconButton(String text, String iconPath) {
        ImageIcon icon = new ImageIcon(iconPath);
        Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton button = new JButton(text, new ImageIcon(scaled));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(windowWidth / 4, windowHeight / 10));

        Color normalColor = new Color(255, 255, 255);
        Color hoverColor = new Color(200, 200, 215);
        Color textColor = new Color(50, 50, 50);

        button.setBackground(normalColor);
        button.setForeground(textColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(normalColor);
            }
        });
        return button;
    }
    //Link buttons
    private JButton createLinkButton(String text, String url, String iconPath) {
        JButton link = new JButton(text);

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
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
