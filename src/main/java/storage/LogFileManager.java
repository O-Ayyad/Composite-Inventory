package storage;


import com.google.gson.reflect.TypeToken;
import core.*;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class LogFileManager extends AbstractFileManager{

    public static final String LOG_FILENAME = "logs.json";

    public final String logFilePath = dataDir + File.separator + LOG_FILENAME;

    private final LogManager logManager;

    private boolean loading = false;

    public LogFileManager(LogManager logManager, String dataDirName){
        super(dataDirName);
        this.logManager = logManager;
    }
    public Path getLogFilePath() {
        return Path.of(logFilePath);
    }
    @Override
    public LoadResult load(boolean firstOpen){
        loading = true;
        Path logPath = getLogFilePath();
        //Get all logs
        Map<Integer,Log> logs;
        try (FileReader itemsReader = new FileReader(logPath.toFile())) {
            Type logType = new TypeToken<Map<Integer,Log>>(){}.getType();
            logs = gson.fromJson(itemsReader, logType);
            if(logs != null){
                for(Map.Entry<Integer,Log> entry : logs.entrySet()){
                    Log l = entry.getValue();
                    Integer id = entry.getKey();
                    if(logManager.logById.get(id) != null){
                        System.out.println("[LogFileManager] Duplicate log found with ID: "+ id);
                        continue; //Log already exists
                    }
                    logManager.addLogToCollectionsWithoutNotify(l);
                    LogManager.nextlogID = Collections.max(logs.keySet()) + 1;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("[LogFileManager]ERROR:  Log file not found. Starting with no logs.");
            return new LoadResult(false, e);
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            showError("[LogFileManager]ERROR: Could not load logs. " + e.getMessage() + "\n " +
                    "Load a working backup and contact support at O-Ayyad@proton.me",firstOpen);
            return new LoadResult(false, e);
        }finally {
            loading = false;
        }
        logManager.notifyListeners();
        System.out.println("[LogFileManager] Loading Logs from: " + logPath);
        return new LoadResult(true, null);
    }
    @Override
    public void save() {
        if (loading) return;

        Path filePath = getLogFilePath();
        try {
            String json = gson.toJson(logManager.logById);
            Files.writeString(filePath, json, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("[LogFileManager] ERROR: Could not save logs");
        }
    }
}
