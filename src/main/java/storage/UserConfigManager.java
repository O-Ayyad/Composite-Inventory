package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import com.twelvemonkeys.util.Time;
import gui.MainWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class UserConfigManager {

    public final String configDir = new File("data" +
            File.separator + "config").getAbsolutePath();

    public static final String CONFIG_FILENAME = "user_config.json";

    public final String configFilePath = configDir + File.separator + CONFIG_FILENAME;

    MainWindow mainWindow;

    Gson gson;

    private boolean loading = false;

    public UserConfigManager(MainWindow mainWindow){
        this.mainWindow = mainWindow;
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        try {
            Path newDir = Path.of(configDir);
            Files.createDirectories(newDir);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    public Path getConfigFilePath() {
        return Path.of(configFilePath);
    }
    public UserConfig loadConfigs(){
        loading = true;
        Path path = getConfigFilePath();
        UserConfig config = null;

        try (FileReader reader = new FileReader(path.toFile())) {
            Type userConfigType = new TypeToken<UserConfig>() {}.getType();
            config = gson.fromJson(reader, userConfigType);
        } catch (FileNotFoundException e) {
            System.out.println("[UserConfigManager]INFO: Config file not found.");
        } catch (Exception e) {
            System.out.println("[]ERROR: Could not load config");
            System.out.println(e.getMessage());
        }finally {
            loading = false;
        }
        System.out.println("[UserConfigManager] Loading config from: " + configFilePath);

        int DEFAULT_AUTOSAVE_TIMER = 15000; //15 seconds
        int DEFAULT_AUTOFETCH_TIMER = 180000; //3 minutes
        return config != null ? config: new UserConfig(true, DEFAULT_AUTOSAVE_TIMER, DEFAULT_AUTOFETCH_TIMER); //
    }
    public void saveConfigs() {
        if (loading) return;

        Path filePath = getConfigFilePath();
        try {
            String json = gson.toJson(mainWindow.config);

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
}
