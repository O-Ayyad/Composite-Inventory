package storage;


import com.google.gson.reflect.TypeToken;
import core.*;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public class LogFileManager extends AbstractFileManager{

    public static final String LOG_FILENAME = "logs.json";

    public final String logFilePath = dataDir + File.separator + LOG_FILENAME;

    public static final String ITEM_LOG_LINK_FILENAME = "itemLogLink.json";

    public final String itemLogLinkFilePath = dataDir + File.separator + ITEM_LOG_LINK_FILENAME;

    private final LogManager logManager;
    private final Inventory inventory;

    private boolean loading = false;

    public LogFileManager(LogManager logManager, Inventory inventory, String dataDirName){
        super(dataDirName);
        this.inventory = inventory;
        this.logManager = logManager;
    }
    public Path getLogFilePath() {
        return Path.of(logFilePath);
    }
    public Path getItemLogLinkFilePath() {
        return Path.of(itemLogLinkFilePath);
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
                    System.out.println("[LogFileManager] Loaded log ID: " + id);
                    LogManager.nextlogID = Collections.max(logs.keySet()) + 1;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("[LogFileManager]ERROR:  Log file not found. Starting with no logs.");
            return new LoadResult(false, e);
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            showError("[LogFileManager]ERROR: Could not load logs. " + e.getMessage() + "\n " +
                    "Load a working backup",firstOpen);
            return new LoadResult(false, e);
        }finally {
            loading = false;
        }
        logManager.notifyListeners();
        System.out.println("[LogFileManager] Loading Logs from: " + logPath);
        return new LoadResult(true, null);
    }

    public void buildItemToLogLinks(){ //Only called after all logs and items are loaded
        loading = true;
        Path filePath = getItemLogLinkFilePath();

        Map<String, List<Integer>> serialToLogIds;
        try (FileReader linkReader = new FileReader(filePath.toFile())) {
            Type linkType = new TypeToken<Map<String, List<Integer>>>(){}.getType();
            serialToLogIds = gson.fromJson(linkReader, linkType);

            if(serialToLogIds != null){
                for(Map.Entry<String, List<Integer>> entry : serialToLogIds.entrySet()){
                    String serial = entry.getKey();

                    if (serial == null || serial.isEmpty()) {
                        System.out.println("[LogFileManager] Skipping link entry with empty serial.");
                        continue;
                    }

                    List<Integer> logIds = entry.getValue();

                    Item item = inventory.getItemBySerial(serial);
                    if(item == null){
                        System.out.println("[LogFileManager] WARNING: Item with serial " + serial + " not found. Skipping link.");
                        continue;
                    }

                    ArrayList<Log> logsForItem = new ArrayList<>();
                    for(Integer logId : logIds){
                        Log log = logManager.logById.get(logId);
                        if(log == null){
                            System.out.println("[LogFileManager] WARNING: Log with ID " + logId + " not found for item " + serial);
                            continue;
                        }
                        logsForItem.add(log);
                    }

                    if(!logsForItem.isEmpty()){
                        logManager.itemToLogs.put(item, logsForItem);
                        System.out.println("[LogFileManager] Linked " + logsForItem.size() + " logs to item: " + serial);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("[LogFileManager]ERROR: Log to item link file not found.");
            return;
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            System.out.println("[LogFileManager]ERROR: Could not load logs. " + e.getMessage() + "\n " +
                    "Load a working backup");
            return;
        }finally {
            loading = false;
        }

        logManager.notifyListeners();
        System.out.println("[LogFileManager] Loading Logs Item Links from: " + filePath);
    }

    @Override
    public void save() {
        if (loading) return;

        Path logFilePath = getLogFilePath();
        try {
            String json = gson.toJson(logManager.logById);
            Files.writeString(logFilePath, json, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("[LogFileManager] ERROR: Could not save logs");
        }

        Path itemLogLinkPath = getItemLogLinkFilePath();

        Map<String, List<Integer>> serialToLogIds= new HashMap<>();
        for(Map.Entry<Item,ArrayList<Log>> e : logManager.itemToLogs.entrySet()){

            Item item = e.getKey();
            if(item == null){
                System.out.println("[LogFileManager] ERROR: Could not save item to logs link. Item is null");
                continue;
            }
            String serial = item.getSerial();
            ArrayList<Log> logs = new ArrayList<>(e.getValue());
            if(logs.isEmpty()){
                continue;
            }

            //Convert to logIds
            List<Integer> logIds = new ArrayList<>();
            for(Log log : logs){
                if(log != null){
                    logIds.add(log.getLogID());
                }
            }

            if(!logIds.isEmpty()){
                serialToLogIds.put(serial, logIds);
            }

            serialToLogIds.put(serial,logIds);
        }
        try {
            String json = gson.toJson(serialToLogIds);
            Files.writeString(itemLogLinkPath, json, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("[LogFileManager] ERROR: Could not save item-log links");
        }
    }
}
