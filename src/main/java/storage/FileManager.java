package storage;

import core.Inventory;
import core.LogManager;
import gui.MainWindow;
import platform.PlatformManager;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FileManager {
    private static final int MAX_BACKUPS = 300;
    private static final String BACKUP_DIR = "data/backups";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final String[] DATA_DIRS = {"orders", "inventory", "logs", "config"};

    Set<AbstractFileManager> fileManagers = new HashSet<>();


    public FileManager(Inventory inventory, LogManager logManager, PlatformManager platformManager, MainWindow mainWindow) {
        createDirectory();

        InventoryFileManager inventoryFileManager = new InventoryFileManager(inventory, "inventory");
        LogFileManager logFileManager = new LogFileManager(logManager, "logs");
        OrderFileManager orderFileManager = new OrderFileManager(platformManager, "orders");
        UserConfigManager userConfigManager = new UserConfigManager(mainWindow, "config");

        fileManagers.add(inventoryFileManager);
        fileManagers.add(logFileManager);
        fileManagers.add(orderFileManager);
        fileManagers.add(userConfigManager);

        if (!loadAll()) {
            System.out.println("[FileManager] Load failed, attempting restore from backup...");

            //Try to restore from backups
            if (restoreFromBackup()) {
                if (loadAll()) {
                    showError("<html>WARNING: Data restored from backup!<br> Your files were corrupted and have been recovered.</html>");
                } else {
                    showError("[FileManager] CRITICAL: Could not load files even after restore!");
                }
            } else {
                showError("[FileManager] CRITICAL: No valid backups available!");
            }
        } else {
            System.out.println("[FileManager] All files loaded successfully");
        }
    }


    private boolean loadAll() {
        boolean allSuccess = true;

        for(AbstractFileManager fileManager: fileManagers){
            try{
                fileManager.load();
            }catch (Exception e){
                String managerName = fileManager.getClass().getSimpleName();
                showError("[" + managerName + "] Load failed : " + e.getMessage());
                allSuccess = false;
            }

        }
        return allSuccess;
    }


    public void saveAll() {
        System.out.println("[FileManager] Saving all data...");

        for(AbstractFileManager fileManager: fileManagers) {
            try {
                fileManager.save();
            } catch (Exception e) {
                String managerName = fileManager.getClass().getSimpleName();
                showError("[" + managerName + "] Save failed : " + e.getMessage());
            }
        }
        createBackup();
    }

    public void createBackup() {
        String backupPath = BACKUP_DIR + "/backup_" + LocalDateTime.now().format(TIMESTAMP);

        try {
            Files.createDirectories(Path.of(backupPath));

            for (String dir : DATA_DIRS) {
                copyDirectory("data/" + dir, backupPath + "/" + dir);
            }

            System.out.println("[FileManager] Backup created: " + backupPath);
            cleanupOldBackups();
        } catch (IOException e) {
            showError("[FileManager] Backup error: " + e.getMessage());
        }
    }

    //True if backup was successful
    private boolean restoreFromBackup() {
        List<Path> backups = getBackups();

        if (backups.isEmpty()) {
            showError("[FileManager] No backups available");
            return false;
        }

        //Try each backup from new to old
        for (Path backup : backups) {
            System.out.println("[FileManager] Trying backup: " + backup.getFileName());

            if (tryRestore(backup)) {
                System.out.println("[FileManager] Restore successful from: " + backup.getFileName());
                return true;
            }
        }

        showError("[FileManager] All backups failed");
        return false;
    }


    private boolean tryRestore(Path backup) {
        try {
            for (String dir : DATA_DIRS) {
                Path source = backup.resolve(dir);
                if (Files.exists(source)) {
                    copyDirectory(source.toString(), "data/" + dir);
                }
            }
            return true;
        } catch (IOException e) {
            showError("[FileManager] Restore failed: " + e.getMessage());
            return false;
        }
    }

    private void copyDirectory(String source, String target) throws IOException {
        Path src = Path.of(source);
        if (!Files.exists(src)) return;

        Files.createDirectories(Path.of(target));
        Files.walk(src).forEach(path -> {
            try {
                Path dest = Path.of(target).resolve(src.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                showError("[FileManager] Copy failed for: " + path);
            }
        });
    }
    private void cleanupOldBackups() {
        List<Path> backups = getBackups();

        if (backups.size() > MAX_BACKUPS) {
            System.out.println("[FileManager] Cleaning up old backups...");

            for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                deleteDirectory(backups.get(i));
                System.out.println("[FileManager] Deleted old backup: " + backups.get(i).getFileName());
            }
        }
    }

    private List<Path> getBackups() {
        try {
            return Files.list(Path.of(BACKUP_DIR))
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private void createDirectory() {
        try {
            Files.createDirectories(Path.of(BACKUP_DIR));
        } catch (IOException e) {
            showError("[FileManager] Cannot create backup directory: " + BACKUP_DIR);
        }
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            showError("[FileManager] Cannot delete: " + path);

                        }
                    });
        } catch (IOException e) {
            showError("Delete directory failed: " + dir);
        }
    }
    public void showError(String message){
        System.out.println(message);
        JOptionPane.showMessageDialog(null,message,"File Error", JOptionPane.ERROR_MESSAGE);
    }

    public UserConfigManager.UserConfig getUserConfig() {
        for(AbstractFileManager fileManager : fileManagers){
            if(fileManager instanceof UserConfigManager){
                return ((UserConfigManager) fileManager).getUserConfig();
            }
        }
        return UserConfigManager.defaultConfig;
    }
}