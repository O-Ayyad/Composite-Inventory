import javax.swing.*;
import java.awt.*;

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

        // Layout
        setLayout(new BorderLayout()); // overall layout for the frame
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(1, 4, 0, 0)); // 1 row, 4 columns, no gaps

        // Buttons
        JButton addButton = createIconButton("Add", "icons/plus.png");
        JButton removeButton = createIconButton("Remove", "icons/minus.png");
        JButton itemButton = createIconButton("Item", "icons/item.png");
        JButton linkButton = createIconButton("Link", "icons/link.png");

        // 4 main Actions
        addButton.addActionListener(e -> new AddWindow(this));
        removeButton.addActionListener(e -> new RemoveWindow(this));
        itemButton.addActionListener(e -> new ItemWindow(this));
        linkButton.addActionListener(e -> new LinkWindow(this));


        // Add buttons
        topPanel.add(addButton);
        topPanel.add(removeButton);
        topPanel.add(itemButton);
        topPanel.add(linkButton);

        add(topPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    private JButton createIconButton(String text, String iconPath) {
        //Image
        ImageIcon icon = new ImageIcon(iconPath);
        Image scaled = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaled);

        //Actual Button
        JButton button = new JButton(text, new ImageIcon(scaled));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(windowWidth/4, windowHeight/10));

        //Button Color
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
    public void refresh(){

    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
