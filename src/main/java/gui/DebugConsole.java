package gui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;


// All System.out/err prints here for debugging
public class DebugConsole extends JFrame {
    private final JTextPane area = new JTextPane();

    public DebugConsole() {
        setTitle("Debug Console");
        setSize(800, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        area.setEditable(false);
        area.setFont(UIUtils.DEBUG_CONSOLE_FONT);
        area.setBackground(Color.BLACK);

        JScrollPane scroll = new JScrollPane(area);
        add(scroll);
        PrintStream ps = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();
            @Override
            public void write(int b) {
                char c = (char) b;
                if (c == '\n') {
                    appendText(buffer + "\n");
                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
            }
        }, true);

        System.setOut(ps);
        System.setErr(ps);
    }

    private void appendText(String text) {
        SwingUtilities.invokeLater(() -> {
            if (text == null || text.isBlank()) return;
            String msg = text.strip();

            LocalDateTime timeTemp = LocalDateTime.now();
            if(timeTemp.getHour() > 12){
                timeTemp = timeTemp.minusHours(12);
            }
            String time = "[" + timeTemp.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")) + "]";
            String formattedMsg = time + msg + "\n";

            StyledDocument doc = area.getStyledDocument();
            Style style = area.addStyle("Style", null);
            if (msg.toLowerCase().contains("error") || msg.toLowerCase().contains("exception") || msg.startsWith("at")) {
                StyleConstants.setForeground(style, Color.RED);
            } else {
                StyleConstants.setForeground(style, Color.GREEN);
            }

            try {
                doc.insertString(doc.getLength(), formattedMsg, style);
            } catch (BadLocationException e) {
                System.out.println("ERROR: "+e.getMessage());
            }
        });
    }

    public static DebugConsole init() {
        DebugConsole dc = new DebugConsole();

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED &&
                            e.getKeyCode() == KeyEvent.VK_BACK_QUOTE &&
                            e.isShiftDown()) {
                        dc.toggle();
                        return true;
                    }
                    return false;
                });

        System.out.println("[DebugConsole] Initialized. Press Shift + ~ to toggle.");
        return dc;
    }
    public void toggle(){
        this.setVisible(!this.isVisible());
        if(this.isVisible()) this.toFront();
    }
}
