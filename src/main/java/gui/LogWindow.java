package gui;
import core.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class LogWindow extends SubWindow {

    public static String windowName = "Log Window";
    private final LogManager logManager;
    private final Log log;

    public LogWindow(MainWindow mainWindow, Inventory inventory, Log selectedLog, LogManager logManager) {
        super(mainWindow, windowName,inventory);

        this.inventory = inventory;
        this.logManager = logManager;
        this.log = selectedLog;

        if (selectedLog == null || logManager.logById.get(selectedLog.getLogID()) == null) {
            dispose();
            throw new RuntimeException("Log Window opened on non-existent log");
        }

        setupUI(selectedLog);
        setVisible(true);
    }

    @Override
    public void setupUI() {
        setupUI(log);
    }

    public void setupUI(Log l){
        if (l == null){
            throw new RuntimeException("Log Window opened on non-existent log and called setupUI");
        }

        JPanel mainPanel = buildLogPanel(l);
        add(mainPanel, BorderLayout.CENTER);
        pack();

        setSize(new Dimension(600, 700));
        setLocationRelativeTo(getOwner());
    }
    JPanel buildLogPanel(Log log){
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        //Header
        JLabel titleLabel = new JLabel("Log #" + log.getLogID() + " — " + log.getType());
        titleLabel.setFont(UIUtils.FONT_UI_TITLE_LARGE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JLabel severityLabel = new JLabel(log.getSeverity().toString());
        severityLabel.setOpaque(true);
        severityLabel.setFont(UIUtils.FONT_ARIAL_LARGE_BOLD);
        switch (log.getSeverity()) {
            case Normal -> severityLabel.setBackground(UIUtils.NORMAL_COLOR);
            case Warning -> severityLabel.setBackground(UIUtils.WARNING_COLOR);
            case Critical -> severityLabel.setBackground(UIUtils.CRITICAL_COLOR);
        }
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(severityLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        //Log details
        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        details.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                "Details",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UIUtils.FONT_ARIAL_LARGE_BOLD,
                UIUtils.BORDER_DARK
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8); // tighter, even spacing
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridy = 0;

        addRow(details, gbc, "Log ID:", new JLabel(String.valueOf(log.getLogID())));
        addRow(details, gbc, "Type:", new JLabel(log.getType().toString()));

        String message = "<html><body style='width:250px; white-space:normal;'>" +
                log.getMessage().replace("\n", "<br>") +  "</body></html>";
        addRow(details, gbc, "Message:", new JLabel(message));

        addRow(details, gbc, "Severity:", new JLabel(log.getSeverity().toString()));
        addRow(details, gbc, "Item Serial:", new JLabel(log.getSerial() != null ? log.getSerial() : "—"));
        addRow(details, gbc, "Amount:", new JLabel(log.getAmount() != null ? log.getAmount().toString() : "—"));
        addRow(details, gbc, "Timestamp:", new JLabel(log.getTime()));
        addRow(details, gbc, "Suppressed:", new JLabel(log.isSuppressed() ? "Yes" : "No"));

        mainPanel.add(details, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        JButton closeBtn = UIUtils.styleButton(new JButton("Close"));
        closeBtn.addActionListener(e -> dispose());

        //Solve btn
        JButton solveBtn = UIUtils.styleButton(new JButton("Solve Log"));
        solveBtn.setEnabled(!(log.getSeverity() == Log.Severity.Normal ||
                log.getType() == Log.LogType.LowStock ||
                log.getType() == Log.LogType.ItemOutOfStock));
        solveBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    """
                            Are you sure you want to mark this log as solved?
                            
                            This will permanently delete this log.
                            Make sure you have resolved the issue first.""",
                    "Confirm Log Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                logManager.solveLog(log);
                JOptionPane.showMessageDialog(this,
                        "Solved log #" + log.getLogID(),
                        "Log Solved and deleted", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to solve log: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        //Suppress btn
        JButton toggleSuppressBtn = UIUtils.styleButton(new JButton());

        Runnable updateSuppressButton = () -> {
            if (log.isSuppressed()) {
                toggleSuppressBtn.setText("Unsuppress Log");
                toggleSuppressBtn.setToolTipText("Restore this suppressed log");
            } else {
                toggleSuppressBtn.setText("Suppress Log");
                toggleSuppressBtn.setToolTipText("Hide this log from alerts");
            }

            toggleSuppressBtn.setEnabled(
                    !(log.getSeverity() == Log.Severity.Normal ||log.getType() ==  Log.LogType.ItemSoldAndNotRegisteredInInventory)
            );
        };
        updateSuppressButton.run();

        toggleSuppressBtn.addActionListener(e -> {
            try {
                if (log.isSuppressed()) {
                    logManager.unsuppressLog(log);
                } else {
                    logManager.suppressLog(log);
                    JOptionPane.showMessageDialog(this,
                            "Suppressed log #" + log.getLogID(),
                            "Log Suppressed", JOptionPane.INFORMATION_MESSAGE);
                }
                updateSuppressButton.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to toggle suppression: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(closeBtn);
        buttonPanel.add(solveBtn);
        buttonPanel.add(toggleSuppressBtn);

        mainPanel.add(buttonPanel, BorderLayout.PAGE_END);

        return mainPanel;
    }
    private void addRow(JPanel panel, GridBagConstraints gbc, String label, JComponent component) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);

        gbc.gridy++;
    }
}
