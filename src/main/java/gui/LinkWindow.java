package gui;
import core.*;
import storage.APIFileManager;
import platform.PlatformType;

import javax.swing.*;
import java.awt.*;

public class LinkWindow extends SubWindow {
    public static String windowName = "Link Accounts & Platforms";

    final APIFileManager apiFileManager;

    public LinkWindow(MainWindow mainWindow, Inventory inventory,APIFileManager apiFileManager) {
        super(mainWindow, windowName, inventory);
        this.apiFileManager = apiFileManager;
        setupUI();
        setVisible(true);
    }
    @Override
    public void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Connect Your Seller Accounts", SwingConstants.CENTER);
        header.setFont(UIUtils.FONT_ARIAL_BOLD);
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
                        "nor be shared with anyone, not even the developer.</html>",
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

        PlatformType type = switch (platformName) {
            case "Amazon" -> PlatformType.AMAZON;
            case "eBay" -> PlatformType.EBAY;
            case "Walmart" -> PlatformType.WALMART;
            default -> null;
        };

        if (type != null) {
            String existingToken = apiFileManager.loadToken(type);
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
                    """
                            Are you sure you want to disconnect this account?
                            
                            You will need to re-authorize this platform to use it again.
                            
                            """,
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
                    assert type != null;
                    apiFileManager.removeToken(type);
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
    private void handleAmazonConnect(PlatformType type, JLabel statusLabel, JButton connectBtn, JButton disconnectBtn) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(450, 280));

        JEditorPane info = createInfoPane(
                """
                <html><body style='font-family:Segoe UI; font-size:12px;'>
                <b>To connect your Amazon Seller Account:</b><br><br>
                1. Open <a href='https://solutionproviderportal.amazon.com/'>the amazon solution portal</a>.<br>
                2. Sign into your amazon account.
                4. When prompted verify with Amazon (This is usually automatic)
                3. Select your app (or create one via Self-Authorization).<br>
                4. Copy your <b>Client ID</b>, <b>Client Secret</b>, and <b>Refresh Token</b> below. Do not share these codes with anyone and do not save<br>
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

            String combined = clientId + "|::|" + clientSecret + "|::|" + refreshToken;
            int response = apiFileManager.validateToken(type, combined);

            processAndShowTokenDialog(type, response, combined);

            if (response >= 200 && response <= 299) {
                statusLabel.setText("Connected (" + type.getDisplayName() + ")");
                statusLabel.setForeground(UIUtils.LINK_SUCCESS);
                connectBtn.setEnabled(false);
                disconnectBtn.setEnabled(true);
            } else {
                statusLabel.setText("Not connected");
                connectBtn.setEnabled(true);
                disconnectBtn.setEnabled(false);
            }
        }
    }
    private void handleWalmartConnect(PlatformType type, JLabel statusLabel, JButton connectBtn, JButton disconnectBtn) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(420, 230));

        JEditorPane info = createInfoPane(
                """
                <html><body style='font-family:Segoe UI; font-size:12px;'>
                <b>To connect your Walmart Seller Account:</b><br><br>
                1. Go to your<a href='https://seller.walmart.com/'> Walmart Seller Center</a>.<br>
                2. On the top left corner by "Seller Center," press the 9 dots
                3. Open Developer Portal
                4. On the top right press "My Account" and log in with Marketplace
                4. Obtain your <i>Client ID</i> and <i>Client Secret</i>.<br>
                5. Paste both below to link your account.<br>
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

        inputPanel.add(new JLabel("Client ID:"));
        inputPanel.add(consumerIdField);
        inputPanel.add(new JLabel("Client Secret:"));
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
                JOptionPane.showMessageDialog(this, "Please enter both Consumer ID and Private Key.",
                        "Missing Keys",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String combined = consumerId + "|::|" + privateKey;
            int response = apiFileManager.validateToken(type, combined);

            processAndShowTokenDialog(type, response, combined);

            if (response >= 200 && response <= 299) {
                statusLabel.setText("Connected (" + type.getDisplayName() + ")");
                statusLabel.setForeground(UIUtils.LINK_SUCCESS);
                connectBtn.setEnabled(false);
                disconnectBtn.setEnabled(true);
            } else {
                statusLabel.setText("Not connected");
                statusLabel.setForeground(Color.RED);
                connectBtn.setEnabled(true);
                disconnectBtn.setEnabled(false);
            }
        }
    }
    private void handleEbayConnect(PlatformType type, JLabel statusLabel, JButton connectBtn, JButton disconnectBtn) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(450, 260));

        JEditorPane info = createInfoPane(
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

            int response = apiFileManager.validateToken(type, combined);

            processAndShowTokenDialog(type, response, combined);

            if (response >= 200 && response <= 299) {
                statusLabel.setText("Connected (" + type.getDisplayName() + ")");
                statusLabel.setForeground(UIUtils.LINK_SUCCESS);
                connectBtn.setEnabled(false);
                disconnectBtn.setEnabled(true);
            } else {
                statusLabel.setText("Not connected  ");
                statusLabel.setForeground(Color.RED);
                connectBtn.setEnabled(true);
                disconnectBtn.setEnabled(false);
            }
        }
    }


    private void processAndShowTokenDialog(PlatformType type, int responseCode,String token) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(420, 180));

        String platformName = type.getDisplayName();
        String messageText = "Response code : " + responseCode + "\n";
        String title = "Invalid or Expired Token";
        int messageType;

        if ( responseCode >= 200 && responseCode <= 299) { //Success
            messageText += "Account connected successfully. Orders can now be fetched and processed";
            title = "Account connected";
            apiFileManager.saveToken(type, token);
            messageType = JOptionPane.INFORMATION_MESSAGE;

        }else{ //Failure
            apiFileManager.removeToken(type);
            if (responseCode == 401 || responseCode == 403) {
                messageText += "\nYour " + platformName + " API token appears to be invalid, expired, or unauthorized.\n\n" +
                        "To fix this issue: Do the following:\n\n" +
                        "1. Open the \"Link Account Window\" and remove the connection for " + platformName  +
                        "\n2. Click connect and re-authorize to obtain a new token.\n\n If you recently changed your seller account or credentials, the old token may no longer work.";
            } else if (responseCode >= 500) {
                messageText += "No response received from " + platformName + ". Ensure the correct formating for the token or check your internet connection.";
            } else  {
                messageText += "An unexpected response (" + responseCode + ") was received from " + platformName + ".\n\n" +
                        "Try reauthorizing your account if this continues.";
            }
            messageType = JOptionPane.WARNING_MESSAGE;
        }
        messageText += """
                
                
                Security Tip:
                You should clear your clipboard history to prevent sensitive keys from being recovered.
                
                If you are using Windows:
                 - Press the Windows key + V
                 - Click 'Clear all'
                
                This helps ensure your API keys and tokens remain private and only on your device.""";
        JTextArea message = new JTextArea(messageText);
        message.setWrapStyleWord(true);
        message.setLineWrap(true);
        message.setEditable(false);
        message.setOpaque(false);
        message.setFont(UIUtils.FONT_UI_REGULAR);

        panel.add(message, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
                this,
                panel,
                title,
                messageType
        );
    }
    private JEditorPane createInfoPane(String str) {
        return new JEditorPane("text/html",
                str);
    }
}
