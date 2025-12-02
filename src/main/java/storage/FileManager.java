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
    private static final int MAX_BACKUPS = 800;
    private static final String BACKUP_DIR = "data/backups";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final String[] DATA_DIRS = {"orders", "inventory", "logs", "config"};

    private static int backUpCounter = 0; //Every 30 auto saves a backup is made unless it is on close on manual save
    private final static int backUpCounterMax = 30;
    Set<AbstractFileManager> fileManagers = new HashSet<>();

    boolean firstOpen;

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

        UserConfigManager.UserConfig uc = userConfigManager.getUserConfig();
        firstOpen = uc.firstOpen;

        if (!loadAll(firstOpen)) {
            System.out.println("[FileManager] Load failed, attempting restore from backup...");

            //Try to restore from backups
            if (restoreFromBackup()) {
                if (loadAll(firstOpen)) {
                    showError("<html>WARNING: Data restored from backup!<br> Your files were corrupted and have been recovered. Some data may be lost.</html>");
                } else {
                    showError("[FileManager] CRITICAL: Could not load files even after restore!");
                }
            } else {
                showError("[FileManager] CRITICAL: No valid backups available!");
            }
        } else {
            System.out.println("[FileManager] All files loaded successfully");
        }
        userConfigManager.setFirstOpenToFalse(uc);
    }

    private boolean loadAll(boolean firstOpen) {
        List<AbstractFileManager.LoadResult> loadResults = Collections.synchronizedList(new ArrayList<>());
        boolean allSuccess = true;

        List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
        for (AbstractFileManager fileManager : fileManagers) {
            Thread thread = new Thread(() -> {
                AbstractFileManager.LoadResult lr = fileManager.load(firstOpen);
                if (!lr.success()) {
                    loadResults.add(lr);
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }
        if(!firstOpen) {
            for (AbstractFileManager.LoadResult lr : loadResults) {
                showError("Error when loading file :" + lr.error().getMessage());
                System.out.println(Arrays.toString(lr.error().getStackTrace()));
            }
        }
        return allSuccess;
    }


    public void saveAll(boolean isAutoSave) {
        List<Thread> threads = new ArrayList<>();

        for (AbstractFileManager fileManager : fileManagers) {
            Thread thread = new Thread(() -> {
                try {
                    fileManager.save();
                } catch (Exception e) {
                    String managerName = fileManager.getClass().getSimpleName();
                    showError("[" + managerName + "] Save failed: " + e.getMessage());
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }

        if (!isAutoSave) {
            createBackup();
            return;
        }

        backUpCounter++;
        if (backUpCounter > backUpCounterMax) {
            createBackup();
            backUpCounter = 0;
        }
    }

    public void createBackup() {
        String backupPath = BACKUP_DIR + "/backup_" + LocalDateTime.now().format(TIMESTAMP);

        try {
            Files.createDirectories(Path.of(backupPath));

            for (String dir : DATA_DIRS) {
                copyDirectory("data/" + dir, backupPath + "/" + dir);
            }

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
            for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                deleteDirectory(backups.get(i));
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
        if(!firstOpen) {
            JOptionPane.showMessageDialog(null, message, "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public UserConfigManager getUserConfigManager(){
        for(AbstractFileManager fileManager : fileManagers){
            if(fileManager instanceof UserConfigManager){
                return ((UserConfigManager) fileManager);
            }
        }
        System.out.println("ERROR: NO USER CONFIG MANAGER EXISTS");
        return null;
    }
    public UserConfigManager.UserConfig getUserConfig() {
        UserConfigManager ucm = getUserConfigManager();
        if(ucm == null) return UserConfigManager.defaultConfig;
        return ucm.getUserConfig();
    }

    public void setHasConnected(boolean val) {
        getUserConfig().hasConnect = val;
        getUserConfigManager().save();
    }
}