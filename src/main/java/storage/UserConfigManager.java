package storage;

import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import gui.MainWindow;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class UserConfigManager extends AbstractFileManager {

    public static final String CONFIG_FILENAME = "user_config.json";

    public final String configFilePath = dataDir + File.separator + CONFIG_FILENAME;

    MainWindow mainWindow;

    private UserConfig userConfig;

    static int DEFAULT_AUTOSAVE_TIMER = 15000; //15 seconds
    static int DEFAULT_AUTOFETCH_TIMER = 180000; //3 minutes

    public static final UserConfig defaultConfig =
            new UserConfig(true, DEFAULT_AUTOSAVE_TIMER, DEFAULT_AUTOFETCH_TIMER);
    

    public UserConfigManager(MainWindow mainWindow, String dataDirName){
        super(dataDirName);
        this.mainWindow = mainWindow;
        load();

        if(userConfig.firstOpen){
            userConfig.firstOpen = false;
            String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("win")){
                int result = JOptionPane.showConfirmDialog(
                        mainWindow,
                        "Would you like this Composite Inventory to automatically open when your computer starts?",
                        "Run on Startup?",
                        JOptionPane.YES_NO_OPTION
                );

                if (result == JOptionPane.YES_OPTION) {
                    try {
                        mainWindow.addToStartupWindows();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null,
                                "Failed to add to startup:\n" + e.getMessage());
                    }
                }
            }else{
                JOptionPane.showMessageDialog(
                        null,
                        "Please add Composite Inventory to automatically open when your computer starts.",
                        "Startup",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
        save();
    }

    public Path getConfigFilePath() {
        return Path.of(configFilePath);
    }
    
    @Override
    public void load(){
        loading = true;
        Path path = getConfigFilePath();
        userConfig = null;

        try (FileReader reader = new FileReader(path.toFile())) {
            Type userConfigType = new TypeToken<UserConfig>() {}.getType();
            userConfig = gson.fromJson(reader, userConfigType);
        } catch (FileNotFoundException e) {
            System.out.println("[UserConfigManager]INFO: Config file not found.");
        } catch (Exception e) {
            System.out.println("[UserConfigManager] ERROR: Could not load config");
            System.out.println(e.getMessage());
        }finally {
            loading = false;
        }
        System.out.println("[UserConfigManager] Loading config from: " + configFilePath);


        if(userConfig == null){
            userConfig = defaultConfig;
        }
    }
    @Override
    public void save() {
        if (loading) return;

        Path filePath = getConfigFilePath();
        try {
            String json = gson.toJson(userConfig);

            Files.writeString(filePath, json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("[UserConfigManager] ERROR: Could not save configs");
        }
    }
    public static class UserConfig{
        @Expose
        public boolean firstOpen;
        @Expose
        public int autoSaveTimer;
        @Expose
        public int autofetchTimer;

        public UserConfig(boolean firstOpen, int autoSaveTimer, int autofetchTimer){
            this.firstOpen = firstOpen;
            this.autoSaveTimer = autoSaveTimer;
            this.autofetchTimer = autofetchTimer;
        }
    }
    public UserConfigManager.UserConfig getUserConfig(){
        return userConfig != null ? userConfig : defaultConfig;
    }
}
