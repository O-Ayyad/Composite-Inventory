package gui;
import core.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.function.Consumer;

public class LogTableModel extends AbstractTableModel {
    private static final String[] cols = {"Log #", "Type", "Action", "Time"};
    public ArrayList<Log> logs;

    private static final double COL_LOG_PCT = 9.0;
    private static final double COL_TYPE_PCT = 14.0;
    private static final double COL_ACTION_PCT = 65.0;
    private static final double COL_TIME_PCT = 12.0;


    LogTableModel(ArrayList<Log> logs) {
        this.logs = logs;
        sortLogs();
    }

    public int getRowCount(){ return logs.size(); }
    public int getColumnCount(){ return cols.length; }
    public String getColumnName(int c){ return cols[c]; }

    public Object getValueAt(int r, int c){
        Log l = logs.get(r);
        return switch(c){
            case 0 -> (switch(l.getSeverity()){
                case Normal -> "✅ "; case Warning -> "⚠️ "; case Critical -> "❌ ";
            })  + l.getLogID();
            case 1 -> l.getType();
            case 2 -> l.getMessage();
            case 3 -> l.getTimestamp();
            default -> "";
        };
    }
    public void setLogs(ArrayList<Log> newLogs) {
        logs.clear();
        logs.addAll(newLogs);
        sortLogs();
        fireTableDataChanged();
    }

    public Log getLogAt(int row) {
        return logs.get(row);
    }

    private void sortLogs() {

        ArrayList<Log> criticalLogs = new ArrayList<>();
        ArrayList<Log> warningLogs = new ArrayList<>();
        ArrayList<Log> normalLogs = new ArrayList<>();

        for(Log l : logs){
            switch (l.getSeverity()){
                case Normal -> normalLogs.add(l);
                case Warning -> warningLogs.add(l);
                case Critical -> criticalLogs.add(l);

            }
        }
        criticalLogs.sort((a, b) -> Integer.compare(b.getLogID(), a.getLogID()));
        ArrayList<Log> sorted = new ArrayList<>(criticalLogs);

        warningLogs.sort((a, b) -> Integer.compare(b.getLogID(), a.getLogID()));
        sorted.addAll(warningLogs);

        normalLogs.sort((a, b) -> Integer.compare(b.getLogID(), a.getLogID()));
        sorted.addAll(normalLogs);

        logs = sorted;
    }
    public static void styleTable(JTable table) {
        table.setFont(UIUtils.FONT_UI_REGULAR);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = table.getTableHeader();
        header.setFont(UIUtils.FONT_UI_BOLD);
        header.setBackground(UIUtils.BACKGROUND_PANEL);
        header.setForeground(UIUtils.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIUtils.HEADER_BORDER));
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private Color getAltered(Color c, boolean even) {
                int delta = even ? 10 : 0;
                return new Color(
                        Math.max(0, c.getRed() - delta),
                        Math.max(0, c.getGreen() - delta),
                        Math.max(0, c.getBlue() - delta)
                );
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!(table.getModel() instanceof LogTableModel model)) return c;
                int modelRow = table.convertRowIndexToModel(row);
                Log log = model.getLogAt(modelRow);
                boolean even = (row % 2 == 0);

                Color normalColor   = UIUtils.NORMAL_COLOR;
                Color warningColor  = UIUtils.WARNING_COLOR;
                Color criticalColor = UIUtils.CRITICAL_COLOR;
                Color revertedColor = UIUtils.REVERTED_COLOR;
                Color suppressedColor = UIUtils.SUPPRESSED_COLOR;

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else if (log.isReverted()) {
                    c.setBackground(revertedColor);
                    c.setForeground(Color.DARK_GRAY);
                } else if (log.isSuppressed()) {
                    c.setBackground(getAltered(suppressedColor, even));
                    c.setForeground(Color.DARK_GRAY);
                } else {
                    switch (log.getSeverity()) {
                        case Normal -> c.setBackground(getAltered(normalColor, even));
                        case Warning -> c.setBackground(getAltered(warningColor, even));
                        case Critical -> c.setBackground(getAltered(criticalColor, even));
                    }
                    c.setForeground(Color.BLACK);
                }

                return c;
            }
        });
    }
    public static JScrollPane createScrollPane(JTable logTable) {
        JScrollPane scrollPane = new JScrollPane(
                logTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = scrollPane.getViewport().getWidth();
                var cm = logTable.getColumnModel();
                cm.getColumn(0).setPreferredWidth((int) (width * COL_LOG_PCT / 100));
                cm.getColumn(1).setPreferredWidth((int) (width * COL_TYPE_PCT / 100));
                cm.getColumn(2).setPreferredWidth((int) (width * COL_ACTION_PCT / 100));
                cm.getColumn(3).setPreferredWidth((int) (width * COL_TIME_PCT / 100));
            }
        });
        return scrollPane;
    }
    public static void attachOpenListener(JTable table, Consumer<Log> onOpen) {
        //Double click
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = table.rowAtPoint(e.getPoint());
                    if (viewRow == -1) return;
                    int modelRow = table.convertRowIndexToModel(viewRow);

                    if (table.getModel() instanceof LogTableModel model) {
                        Log log = model.getLogAt(modelRow);
                        onOpen.accept(log);
                    }
                }
            }
        });

        //Ctrl click
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow == -1) return;
                    int modelRow = table.convertRowIndexToModel(viewRow);

                    if (table.getModel() instanceof LogTableModel model) {
                        Log log = model.getLogAt(modelRow);
                        onOpen.accept(log);
                        e.consume();
                    }
                }
            }
        });
    }
}
