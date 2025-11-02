package gui;
import core.*;
import platform.APIStorage;

import javax.swing.*;
import java.awt.*;

public class LinkWindow extends SubWindow {
    public static String windowName = "Link Accounts & Platforms";

    final APIStorage apiStorage;

    public LinkWindow(JFrame mainWindow, Inventory inventory) {
        super(mainWindow, windowName, inventory);
        apiStorage = new APIStorage();
        setupUI();
        setVisible(true);
    }
    @Override
    public void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Connect Your Seller Accounts", SwingConstants.CENTER);
        header.setFont(UIUtils.FONT_UI_LARGE_BOLD);
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new GridLayout(3, 1, 15, 15));

        content.add(createPlatformPanel("Amazon"));
        content.add(createPlatformPanel("eBay"));
        content.add(createPlatformPanel("Walmart"));

        mainPanel.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel footerText = new JLabel(
                "<html><div style='text-align:center;'>"
                        + "Note: Your credentials are securely saved locally<br>" +
                        "and will never leave your device <br>" +
                        "nor be shared with anyone, not even me, the developer.<br>" +
                        "Each platform can only access its own credentials when needed with read-only permissions."
                        + "</div></html>",
                SwingConstants.CENTER
        );
        footerText.setFont(UIUtils.FONT_UI_SMALL);
        footerText.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerText.setBorder(BorderFactory.createEmptyBorder(5, 0, 8, 0));


        footer.add(footerText);
        footer.add(UIUtils.styleButton(closeBtn));

        mainPanel.add(footer, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setResizable(false);
    }
    private JPanel createPlatformPanel(String platformName) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel nameLabel = new JLabel(platformName);
        nameLabel.setFont(UIUtils.FONT_UI_BOLD );
        panel.add(nameLabel, BorderLayout.WEST);

        JLabel statusLabel = new JLabel("Not Connected");
        statusLabel.setForeground(Color.GRAY);
        panel.add(statusLabel, BorderLayout.CENTER);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton connectButton = UIUtils.styleButton(new JButton("Connect"));
        JButton disconnectButton = UIUtils.styleButton(new JButton("Disconnect"));

        disconnectButton.setEnabled(false);
        buttonPanel.add(UIUtils.styleButton(connectButton));
        buttonPanel.add(UIUtils.styleButton(disconnectButton));
        panel.add(buttonPanel, BorderLayout.EAST);

        APIStorage.PlatformType type = switch (platformName) {
            case "Amazon" -> APIStorage.PlatformType.AMAZON;
            case "eBay" -> APIStorage.PlatformType.EBAY;
            case "Walmart" -> APIStorage.PlatformType.WALMART;
            default -> null;
        };

        if (type != null) {
            String existingToken = apiStorage.loadToken(type);
            if (existingToken != null) {
                statusLabel.setText("Connected (" + platformName + ")");
                statusLabel.setForeground(UIUtils.LINK_SUCCESS);
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
            }
        }
        connectButton.addActionListener(e -> {
            switch (platformName) {
                case "Amazon" -> handleAmazonConnect(type, statusLabel, connectButton, disconnectButton);
                case "eBay" -> handleEbayConnect(type, statusLabel, connectButton, disconnectButton);
                case "Walmart" -> handleWalmartConnect(type, statusLabel, connectButton, disconnectButton);
            }
        });
        disconnectButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to disconnect this account?\n\n" +
                            "You will need to re-authorize this platform to use it again.",
                    "Confirm Disconnect",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                String confirm = JOptionPane.showInputDialog(
                        this,
                        "Type CONFIRM to permanently remove your saved token:",
                        "Final Confirmation",
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm != null && confirm.trim().equalsIgnoreCase("CONFIRM")) {
                    apiStorage.removeToken(type);
                    statusLabel.setText("Not Connected");
                    statusLabel.setForeground(Color.GRAY);
                    connectButton.setEnabled(true);
                    disconnectButton.setEnabled(false);

                    JOptionPane.showMessageDialog(
                            this,
                            platformName + " account disconnected successfully.",
                            "Disconnected",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    System.out.println("[LinkWindow] " + platformName + " token removed successfully.");
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Disconnect cancelled. Token was not removed.",
                            "Cancelled",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });

        return panel;
    }
    private void handleAmazonConnect(APIStorage.PlatformType type, JLabel statusLabel, JButton connectBtn, JButton disconnectBtn) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(450, 280));

        JEditorPane info = new JEditorPane("text/html",
                """
                <html><body style='font-family:Segoe UI; font-size:12px;'>
                <b>To connect your Amazon Seller Account:</b><br><br>
                1. Open <a href='https://solutionproviderportal.amazon.com/account/onboarding?enrollmentApplicationId=hC8h11s1'></a>.<br>
                2. Register your account
                3. When prompted select Sandbox app
                3. Select your app (or create one via Self-Authorization).<br>
                4. Copy your <b>Client ID</b>, <b>Client Secret</b>, and <b>Refresh Token</b> below.<br>
                </body></html>
                """);
        info.setEditable(false);
        info.setOpaque(false);
        info.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try { Desktop.getDesktop().browse(e.getURL().toURI()); } catch (Exception ignored) {}
            }
        });

        panel.add(info, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 8, 8));

        JTextField clientIdField = new JTextField();
        JTextField clientSecretField = new JTextField();
        JTextField refreshTokenField = new JTextField();

        inputPanel.add(new JLabel("Client ID:"));
        inputPanel.add(clientIdField);
        inputPanel.add(new JLabel("Client Secret:"));
        inputPanel.add(clientSecretField);
        inputPanel.add(new JLabel("Refresh Token:"));
        inputPanel.add(refreshTokenField);

        panel.add(inputPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Connect Amazon Seller Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String clientId = clientIdField.getText().trim();
            String clientSecret = clientSecretField.getText().trim();
            String refreshToken = refreshTokenField.getText().trim();

            if (clientId.isEmpty() || clientSecret.isEmpty() || refreshToken.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter all three values: Client ID, Client Secret, and Refresh Token.",
                        "Missing Fields",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String combinedToken = clientId + "|::|" + clientSecret + "|::|" + refreshToken;

            boolean valid = apiStorage.validateToken(type, combinedToken);
            if (!valid) {
                apiStorage.removeToken(type);
                showInvalidTokenDialog("Amazon");
                return;
            }

            apiStorage.saveToken(type, combinedToken);
            statusLabel.setText("Connected (Amazon)");
            statusLabel.setForeground(UIUtils.LINK_SUCCESS);
            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);
        }
    }
    private void handleWalmartConnect(APIStorage.PlatformType type, JLabel statusLabel, JButton connectBtn, JButton disconnectBtn) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(420, 230));

        JEditorPane info = new JEditorPane("text/html",
                """
                <html><body style='font-family:Segoe UI; font-size:12px;'>
                <b>To connect your Walmart Seller Account:</b><br><br>
                1. Go to <a href='https://seller.walmart.com/'>Seller Center</a> → Settings → API Keys.<br>
                2. Generate your <i>Consumer ID</i> and <i>Private Key</i>.<br>
                3. Paste both below to link your account.<br>
                </body></html>
                """);
        info.setEditable(false);
        info.setOpaque(false);
        info.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try { Desktop.getDesktop().browse(e.getURL().toURI()); } catch (Exception ignored) {}
            }
        });

        panel.add(info, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        JTextField consumerIdField = new JTextField();
        JTextField privateKeyField = new JTextField();
        JButton pasteBtn = UIUtils.styleButton(new JButton("Paste Key"));
        pasteBtn.addActionListener(e -> privateKeyField.paste());

        inputPanel.add(new JLabel("Consumer ID:"));
        inputPanel.add(consumerIdField);
        inputPanel.add(new JLabel("Private Key:"));
        inputPanel.add(privateKeyField);
        panel.add(inputPanel, BorderLayout.CENTER);

        panel.add(inputPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Connect Walmart Seller Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String consumerId = consumerIdField.getText().trim();
            String privateKey = privateKeyField.getText().trim();

            if (consumerId.isEmpty() || privateKey.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both Consumer ID and Private Key.", "Missing Keys", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String combined = consumerId + "|::|" + privateKey;
            boolean valid = apiStorage.validateToken(type, combined);

            if (!valid) {
                apiStorage.removeToken(type);
                showInvalidTokenDialog("Walmart");
                return;
            }

            apiStorage.saveToken(type, combined);

            statusLabel.setText("Connected (Walmart)");
            statusLabel.setForeground(UIUtils.LINK_SUCCESS);
            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);
        }
    }
    private void handleEbayConnect(APIStorage.PlatformType type, JLabel statusLabel, JButton connectBtn, JButton disconnectBtn) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(450, 260));

        JEditorPane info = new JEditorPane("text/html",
                """
                <html><body style='font-family:Segoe UI; font-size:12px;'>
                <b>To connect your eBay Seller Account:</b><br><br>
                1. Visit <a href='https://developer.ebay.com/'>developer.ebay.com</a> and sign in.<br>
                2. Create or open your app to obtain your <b>Client ID</b> and <b>Client Secret</b>.<br>
                3. Go to <b>OAuth Token Tool</b> and copy your <b>Refresh Token</b> (with <i>offline_access</i> scope).<br>
                4. Paste all three fields below.<br>
                </body></html>
                """);
        info.setEditable(false);
        info.setOpaque(false);
        info.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ignored) {}
            }
        });

        panel.add(info, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField clientIdField = new JTextField();
        JTextField clientSecretField = new JTextField();
        JTextField refreshTokenField = new JTextField();

        inputPanel.add(new JLabel("Client ID:"));
        inputPanel.add(clientIdField);
        inputPanel.add(new JLabel("Client Secret:"));
        inputPanel.add(clientSecretField);
        inputPanel.add(new JLabel("Refresh Token:"));
        inputPanel.add(refreshTokenField);

        panel.add(inputPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Connect eBay Seller Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String clientId = clientIdField.getText().trim();
            String clientSecret = clientSecretField.getText().trim();
            String refreshToken = refreshTokenField.getText().trim();

            if (clientId.isEmpty() || clientSecret.isEmpty() || refreshToken.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter all three fields: Client ID, Client Secret, and Refresh Token.",
                        "Missing Fields",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String combined = clientId + "|::|" + clientSecret + "|::|" + refreshToken;

            boolean valid = apiStorage.validateToken(type, combined);

            if (!valid) {
                apiStorage.removeToken(type);
                showInvalidTokenDialog("eBay");
                return;
            }

            apiStorage.saveToken(type, combined);

            statusLabel.setText("Connected (eBay)");
            statusLabel.setForeground(UIUtils.LINK_SUCCESS);
            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);
        }
    }


    private void showInvalidTokenDialog(String platformName) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(420, 180));


        JTextArea message = new JTextArea(
                "Your " + platformName + " API token appears to be invalid, expired, or unauthorized.\n\n" +
                        "To fix this issue: Do the following:\n\n" +
                        "1. Open the \"Link Account Window\" and remove the connection for " + platformName  +
                        "\n2. Click connect and re-authorize to obtain a new token.\n\n If you recently changed your seller account or credentials, the old token may no longer work."
        );
        message.setWrapStyleWord(true);
        message.setLineWrap(true);
        message.setEditable(false);
        message.setOpaque(false);
        message.setFont(UIUtils.FONT_UI_REGULAR);

        panel.add(message, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
                this,
                panel,
                "Invalid or Expired Token",
                JOptionPane.WARNING_MESSAGE
        );
    }
}
