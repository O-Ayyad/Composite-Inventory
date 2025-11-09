package core;

import gui.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;


// All System.out/err prints here for debugging
public class DebugConsole extends JFrame {
    private static DebugConsole instance;
    private final JTextArea area = new JTextArea();

    private DebugConsole() {
        setTitle("Debug Console");
        setSize(800, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        area.setEditable(false);
        area.setFont(UIUtils.DEBUG_CONSOLE_FONT);
        area.setBackground(Color.BLACK);
        area.setForeground(Color.GREEN);

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

            String time = "[" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")) + "]";
            String formattedMsg = time + msg + "\n";

            if (msg.toLowerCase().contains("error") || msg.toLowerCase().contains("exception")) {
                area.setForeground(Color.RED);
                area.append(formattedMsg);
                area.setForeground(Color.GREEN);
            } else {
                area.append(formattedMsg);
            }

            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    public static void init() {
        if (instance == null) instance = new DebugConsole();

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED &&
                            e.getKeyCode() == KeyEvent.VK_BACK_QUOTE &&
                            e.isShiftDown()) {
                        if (instance.isVisible()) {
                            instance.setVisible(false);
                        } else {
                            instance.setVisible(true);
                            instance.toFront();
                        }
                        return true;
                    }
                    return false;
                });

        System.out.println("[DebugConsole] Initialized. Press Shift + ~ to toggle.");
    }
}
