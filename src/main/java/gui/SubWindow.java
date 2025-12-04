package gui;

import core.Inventory;
import core.Item;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

public abstract class SubWindow extends JFrame {

    MainWindow mainWindow;
    Inventory inventory;

    boolean ffmpegAvailable = false;
    FFmpeg ffmpeg = null;

    public SubWindow(MainWindow mainWindow, String name, Inventory inventory) {
        super(name);
        this.mainWindow = mainWindow;
        this.inventory = inventory;
        setLocationRelativeTo(mainWindow);


        mainWindow.addInstance(this);


        if(handleCloneSubwindow()){
            return;
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                mainWindow.removeInstance(SubWindow.this);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                mainWindow.removeInstance(SubWindow.this);
            }
        });
        setVisible(true);
    }
    public abstract void setupUI();

    public boolean handleCloneSubwindow(){

        Class<? extends SubWindow> clazz = getClass();

        List<SubWindow> existingList = mainWindow.getInstances(getClass());
        if(existingList.size() <= 1){
            return false;
        }
        if (existingList.size() > 2) {//This shouldnt happen
            mainWindow.destroyAllExistingInstance(getClass());
            return true;
        }

        SubWindow existing = (existingList.get(0) == this)
                ? existingList.get(1)
                : existingList.get(0);

        existing.toFront();
        existing.requestFocus();

        mainWindow.destroyExistingInstance(this);
        System.out.println(clazz.getSimpleName() + " is already open.");

        return true;
    }

    //Creates a searchable dropdown menu of all items
    public DropdownResult getDropDownMenuAllItems() {
        return createFilteredDropdown(item -> true); //include all items
    }

    public DropdownResult getDropDownMenuCompositeItems() {
        return createFilteredDropdown(Item::isComposite); //include only composites
    }
    //Returns true if user deleted the item
    private DropdownResult createFilteredDropdown(Predicate<Item> itemFilter) {
        JComboBox<String> itemDropdown = new JComboBox<>();
        itemDropdown.setEditable(true);

        Dimension fixedSize = new Dimension(200, 25);
        itemDropdown.setPreferredSize(fixedSize);
        itemDropdown.setMaximumSize(fixedSize);
        itemDropdown.setMinimumSize(fixedSize);

        itemDropdown.setEditor(new BasicComboBoxEditor() {
            private final JTextField tf = new JTextField();
            @Override public Component getEditorComponent() { return tf; }
            @Override public Object getItem() { return tf.getText(); }
            @Override public void setItem(Object anObject) { }
        });
        itemDropdown.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

        JTextField editor = (JTextField) itemDropdown.getEditor().getEditorComponent();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        itemDropdown.setModel(model);

        HashMap<String, String> displayToSerialMap = new HashMap<>();
        ArrayList<String> displayList = new ArrayList<>();

        for (String serial : inventory.SerialToItemMap.keySet()) {
            Item item = inventory.getItemBySerial(serial);
            if (item != null && itemFilter.test(item)) {
                String display = item.getName() + " (" + serial + ")";
                displayList.add(display);
                displayToSerialMap.put(display, serial);
            }
        }

        displayList.forEach(model::addElement);

        final boolean[] rebuilding = { false };
        final String[] selectedItem = { null };

        int delay = 150;
        Timer debounceTimer = new Timer(delay, e -> {
            String text = editor.getText().trim().toLowerCase();
            if (selectedItem[0] != null && !editor.getText().equals(selectedItem[0])) {
                selectedItem[0] = null;
            }
            rebuilding[0] = true;
            model.removeAllElements();

            if (text.isEmpty()) {
                displayList.stream()
                        .filter(d -> !d.equals(selectedItem[0]))
                        .forEach(model::addElement);
            } else {
                displayList.stream()
                        .filter(d -> !d.equals(selectedItem[0]))
                        .filter(d -> d.toLowerCase().contains(text) ||
                                displayToSerialMap.get(d).toLowerCase().contains(text))
                        .forEach(model::addElement);
            }
            rebuilding[0] = false;
            if (editor.hasFocus() && model.getSize() > 0) {
                itemDropdown.showPopup();
            }
            SwingUtilities.invokeLater(() -> editor.setCaretPosition(editor.getText().length()));

        });
        debounceTimer.setRepeats(false);

        //Typing debounce
        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void restartTimer() {
                if (debounceTimer.isRunning()) debounceTimer.restart();
                else debounceTimer.start();
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { restartTimer(); }
        });

        //Handle selection
        itemDropdown.addActionListener(e -> {
            if (!rebuilding[0]) {
                Object selected = itemDropdown.getSelectedItem();
                if (selected != null) {
                    editor.setText(selected.toString());
                    editor.setCaretPosition(editor.getText().length());
                    itemDropdown.hidePopup();
                    selectedItem[0] = selected.toString();
                }
            }
        });


        //Focus behavior
        editor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    editor.selectAll();
                    if (itemDropdown.isShowing() && model.getSize() > 0 && !editor.getText().trim().isEmpty()) {
                        itemDropdown.showPopup();
                    }
                });
            }
        });

        editor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                editor.setText("");
            }
        });

        SwingUtilities.invokeLater(() ->{
                editor.setCaretPosition(editor.getText().length());
                editor.requestFocusInWindow();
                editor.selectAll();
                itemDropdown.hidePopup();
        });

        return new DropdownResult(itemDropdown, displayToSerialMap);
    }
    public void confirmRemoveItem(Item target){
        if (!target.getComposesInto().isEmpty()) {
            StringBuilder composeList = new StringBuilder();
            for(Item i : target.getComposesInto()){
                composeList.append(i.getName()).append("\n");
            }
            String composeListStr = composeList.toString();
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Warning: This item is used as a component in other composite items.\n" +
                            "Removing it may affect those compositions of the following item(s):\n\n" + composeListStr +
                            "\n\nProceed anyway?",
                    "Composition Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        //Confirmation
        String serial = target.getSerial();

        String inSerial = JOptionPane.showInputDialog(
                this,
                "Type the serial number to permanently delete:\n" + serial +
                        "\n\nThis action cannot be undone! All data about this item will be lost!\n" +
                        "This will also remove all logs associated with this item!",
                "Confirm Deletion",
                JOptionPane.WARNING_MESSAGE
        );
        if (!inSerial.equals(serial)) {
            JOptionPane.showMessageDialog(this, "Serial numbers do not match. Item not removed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String confirmWord = JOptionPane.showInputDialog(
                this,
                "Delete \"" + target.getName() + "\" (Serial: " + serial + ")?\n\n" +
                        "This CANNOT be undone and will delete all logs.\n\n" +
                        "Type CONFIRM below to continue:",
                "Confirm delete " + target.getName(),
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmWord == null || !confirmWord.trim().equalsIgnoreCase("CONFIRM")) {
            JOptionPane.showMessageDialog(
                    this,
                    "You must type CONFIRM to proceed with deletion.",
                    "Deletion Canceled",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you absolutely sure you want to delete this item?\n\nItem Serial: " + serial,
                "Final Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Deletion canceled.", "Canceled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        //Success
        try {
            inventory.removeItem(target);
            JOptionPane.showMessageDialog(
                    this,
                    "Successfully removed " + target.getName() + " (Serial: " + serial + ")",
                    "Item Removed",
                    JOptionPane.INFORMATION_MESSAGE
            );

            //This is parentwindow
            if(!(this instanceof ViewWindow)){
                dispose();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to remove item: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
    Icon scaleImage(Image selectedImage){
        float aspectRatio = (float) selectedImage.getWidth(null) / (float) selectedImage.getHeight(null);
        int scaledWidth = 128,
                scaledHeight = 128;

        if(aspectRatio != 1f ){
            if (aspectRatio > 1) {
                scaledHeight = (int) (128 / aspectRatio);
            } else{
                scaledWidth = (int) (128 * aspectRatio);
            }
        }
        BufferedImage scaled = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = (128- scaledWidth) / 2;
        int y = (128 - scaledHeight) / 2;
        g2.drawImage(selectedImage, x, y, scaledWidth, scaledHeight, null);
        g2.dispose();

        return new ImageIcon(scaled);
    }
    private static JFileChooser getJFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an Image");

        String userHome = System.getProperty("user.home");
        File downloadsFolder = new File(userHome, "Downloads");
        if (downloadsFolder.exists()) {
            fileChooser.setCurrentDirectory(downloadsFolder);
        }

        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".png")
                        || name.endsWith(".jpg")
                        || name.endsWith(".jpeg")
                        || name.endsWith(".gif")
                        || name.endsWith(".bmp")
                        || name.endsWith(".webp")
                        || name.endsWith(".avif");
            }

            @Override
            public String getDescription() {
                return "Image Files (*.png, *.jpg, *.jpeg, *.gif, *.bmp, *.webp, *.avif)";
            }
        });
        return fileChooser;
    }
    public ImageIcon getScaledIconTo(ImageIcon icon, float scaled) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        float scale = scaled / Math.max(w, h);

        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);

        Image img = icon.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
    public record DropdownResult(JComboBox<String> menu, HashMap<String, String> serialMap) {
    }
    public String getPlural(String target){
        return target.endsWith("s") ? target + "es" : target + "s";
    }
    public void getImage(JButton imageButton, JLabel imageLabel, Consumer<String> onImageSelected){
        imageButton.addActionListener(e -> {

            JFileChooser fileChooser = getJFileChooser();
            int result = fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(imageButton));
            if (result != JFileChooser.APPROVE_OPTION) return;

            File selectedImageFile = fileChooser.getSelectedFile();
            processImageFile(selectedImageFile, imageLabel, onImageSelected);
        });
    }

    public void processImageFile(File imageFile, JLabel imageLabel, Consumer<String> onImageSelected) {
        try {
            ImageIO.scanForPlugins();

            File fileToRead = imageFile;

            if (ffmpegAvailable && imageFile.getName().toLowerCase().endsWith(".avif")) {
                File tmpPng = File.createTempFile("tmp-", ".png");
                tmpPng.deleteOnExit();

                FFmpegBuilder builder = new FFmpegBuilder()
                        .setInput(imageFile.getAbsolutePath())
                        .overrideOutputFiles(true)
                        .addOutput(tmpPng.getAbsolutePath())
                        .setFormat("image2")
                        .setVideoCodec("png")
                        .done();

                new FFmpegExecutor(ffmpeg).createJob(builder).run();

                fileToRead = tmpPng;
            }

            BufferedImage original = ImageIO.read(fileToRead);

            if (original == null) {
                imageLabel.setText("Unsupported or corrupt image");
                System.err.println("Failed to read: " + imageFile.getName());
                return;
            }

            Icon itemIcon = scaleImage(original);
            imageLabel.setIcon(itemIcon);
            imageLabel.setText("");

            String path = "src/main/resources/icons/itemIcons/";
            File resourcesDir = new File(path);
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = "item_" + timestamp + ".png";
            String savedPath = path + fileName;
            File outputFile = new File(savedPath);
            ImageIO.write(original, "png", outputFile);

            if (onImageSelected != null) {
                onImageSelected.accept(savedPath);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            imageLabel.setText("Failed to load image");
        }
    }
}
