package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import core.*;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class LogFileManager {
    private final String dataDir = new File("data" +
            File.separator + "logs").getAbsolutePath();

    private static final String LOG_FILENAME = "logs.json";

    private final String logFilePath = dataDir + File.separator + LOG_FILENAME;

    LogManager logManager;

    Gson gson;

    private boolean loading = false;

    public LogFileManager(LogManager logManager){
        this.logManager = logManager;
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        try {
            Path newDir = Path.of(dataDir);
            Files.createDirectories(newDir);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        loadLogs();
    }
    public Path getLogFilePath() {
        return Path.of(logFilePath);
    }
    public void loadLogs(){
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
            System.out.println("[LogFileManager]INFO: Log file not found. Starting with no logs.");
        } catch (Exception e) {
            System.out.println("[LogFileManager]ERROR: Could not load logs");
            System.out.println(e.getMessage());
        }finally {
            loading = false;
        }
        logManager.notifyListeners();
        System.out.println("[LogFileManager] Loading Logs from: " + logPath.toString());
    }
    public void saveLogs() {
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
