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

    static int DEFAULT_AUTOSAVE_TIMER = 8000; //8 seconds
    static int DEFAULT_AUTOFETCH_TIMER = 180000; //3 minutes

    public static final UserConfig defaultConfig =
            new UserConfig(true, DEFAULT_AUTOSAVE_TIMER, DEFAULT_AUTOFETCH_TIMER,false);

    private UserConfig userConfig;

    public UserConfigManager(MainWindow mainWindow, String dataDirName){
        super(dataDirName);
        this.mainWindow = mainWindow;
        load(false);

        if(userConfig == null){
            userConfig = defaultConfig;
        }

        if(userConfig.firstOpen){
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
    }

    public Path getConfigFilePath() {
        return Path.of(configFilePath);
    }
    
    @Override
    public LoadResult load(boolean firstOpen){
        loading = true;
        Path path = getConfigFilePath();

        try (FileReader reader = new FileReader(path.toFile())) {
            Type userConfigType = new TypeToken<UserConfig>() {}.getType();
            userConfig = gson.fromJson(reader, userConfigType);
        } catch (FileNotFoundException e) {
            System.out.println("[UserConfigManager]INFO: Config file not found.");
            return new LoadResult(false, e);
        } catch (Exception e) {
            System.out.println("[UserConfigManager] ERROR: Could not load config");
            System.out.println("ERROR: "+e.getMessage());
            return new LoadResult(false, e);
        }finally {
            loading = false;
        }
        System.out.println("[UserConfigManager] Loading config from: " + configFilePath);

        return new LoadResult(true, null);
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
        @Expose
        public boolean hasConnect;

        public UserConfig(boolean firstOpen, int autoSaveTimer, int autofetchTimer,boolean hasConnect){
            this.firstOpen = firstOpen;
            this.autoSaveTimer = autoSaveTimer;
            this.autofetchTimer = autofetchTimer;
            this.hasConnect = hasConnect;
        }
    }
    public UserConfigManager.UserConfig getUserConfig(){
        return userConfig != null ? userConfig : defaultConfig;
    }
    public void setFirstOpenToFalse(UserConfig uc){
        uc.firstOpen = false;
        save();
    }
}
