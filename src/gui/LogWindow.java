package gui;
import core.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class LogWindow extends SubWindow {

    public static String windowName = "Log Window";
    private final LogManager logManager;
    private final Inventory inventory;
    private final Log log;

    public LogWindow(JFrame mainWindow, Inventory inventory, Log selectedLog, LogManager logManager) {
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

        setSize(new Dimension(650, 480));
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
        severityLabel.setFont(UIUtils.FONT_UI_LARGE_BOLD);
        switch (log.getSeverity()) {
            case Normal -> severityLabel.setForeground(UIUtils.NORMAL_COLOR);
            case Warning -> severityLabel.setForeground(UIUtils.WARNING_COLOR);
            case Critical -> severityLabel.setForeground(UIUtils.CRITICAL_COLOR);
        }

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(severityLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        //Log details
        JPanel details = new JPanel(new GridLayout(0, 2, 10, 10));
        details.setOpaque(false);
        details.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_MEDIUM, 2),
                "Details",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UIUtils.FONT_UI_LARGE_BOLD,
                UIUtils.BORDER_DARK
        ));

        details.add(new JLabel("Log ID:"));
        details.add(new JLabel(String.valueOf(log.getLogID())));

        details.add(new JLabel("Type:"));
        details.add(new JLabel(log.getType().toString()));

        details.add(new JLabel("Severity:"));
        details.add(new JLabel(log.getSeverity().toString()));

        details.add(new JLabel("Item Serial:"));
        details.add(new JLabel(log.getItemSerial() != null ? log.getItemSerial() : "—"));

        details.add(new JLabel("Amount:"));
        details.add(new JLabel(log.getAmount() != null ? log.getAmount().toString() : "—"));

        details.add(new JLabel("Timestamp:"));
        details.add(new JLabel(log.getTimestamp()));

        details.add(new JLabel("Reverted:"));
        details.add(new JLabel(log.isReverted() ? "Yes" : "No"));

        details.add(new JLabel("Suppressed:"));
        details.add(new JLabel(log.isSuppressed() ? "Yes" : "No"));

        mainPanel.add(details, BorderLayout.CENTER);

        //log message
        JTextArea msgArea = new JTextArea(log.getMessage());
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setEditable(false);
        msgArea.setFont(UIUtils.FONT_UI_REGULAR);
        msgArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIUtils.BORDER_LIGHT, 2),
                "Message",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UIUtils.FONT_UI_LARGE_BOLD,
                UIUtils.BORDER_DARK
        ));
        mainPanel.add(new JScrollPane(msgArea), BorderLayout.SOUTH);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        JButton closeBtn = UIUtils.styleButton(new JButton("Close"));
        closeBtn.addActionListener(e -> dispose());

        //Revert btn
        JButton revertBtn = UIUtils.styleButton(new JButton("Revert Log"));
        revertBtn.setEnabled(logManager.canRevert(log));
        revertBtn.addActionListener(e -> {
            try {
                logManager.revertLog(log);
                JOptionPane.showMessageDialog(this,
                        "Reverted log #" + log.getLogID(),
                        "Log Reverted", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Failed to revert log: " + ex.getMessage(),
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
                    log.getSeverity() != Log.Severity.Normal
            );
        };
        updateSuppressButton.run();

        toggleSuppressBtn.addActionListener(e -> {
            try {
                if (log.isSuppressed()) {
                    logManager.unsuppressLog(log);
                    JOptionPane.showMessageDialog(this,
                            "Unsuppressed log #" + log.getLogID(),
                            "Log Unsuppressed", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    logManager.suppressLog(log);
                    JOptionPane.showMessageDialog(this,
                            "Suppressed log #" + log.getLogID(),
                            "Log Suppressed", JOptionPane.INFORMATION_MESSAGE);
                }
                updateSuppressButton.run();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Failed to toggle suppression: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(closeBtn);
        buttonPanel.add(revertBtn);
        buttonPanel.add(toggleSuppressBtn);

        mainPanel.add(buttonPanel, BorderLayout.PAGE_END);

        return mainPanel;
    }
}
