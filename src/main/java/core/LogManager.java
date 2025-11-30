package core;

import java.util.*;

public class LogManager {
    public Inventory inventory;


    public ArrayList<Log> AllLogs = new ArrayList<>();
    public ArrayList<Log> CriticalLogs = new ArrayList<>();
    public ArrayList<Log> WarningLogs = new ArrayList<>();
    public ArrayList<Log> NormalLogs = new ArrayList<>();

    private final ArrayList<Runnable> listeners = new ArrayList<>();

    // Quick lookup by ID
    public Map<Integer, Log> logById = new HashMap<>();

    public Map<Item, ArrayList<Log>> itemToLogs = new HashMap<>();

    public static int nextlogID = 1;

    public LogManager(){} //Constructor

    public void setInventory(Inventory i){
        inventory = i;
        if (i.logManager != this) {
            i.setLogManager(this);
        }
    }

    //-------------------------------------Log Creation
    //All logs are created from the inventory
    public void createLog(Log.LogType type, int amount, String message, String itemSerial) {
        if(inventory.getItemBySerial(itemSerial) == null && !itemSerial.isEmpty()){
            itemSerial = "";
        }

        Log l = new Log(type, amount, message, itemSerial, nextlogID);
        nextlogID++;
        addLogToCollections(l);

        if(type == Log.LogType.SystemError){
            System.out.println(message);
        }
    }
    public void addLogToCollections(Log l) {
        if (l == null) return;
        Item i = inventory.getItemBySerial(l.getSerial());
        AllLogs.add(l);
        logById.put(l.getLogID(), l);
        itemToLogs.computeIfAbsent(i, k -> new ArrayList<>()).add(l);

        switch (l.getSeverity()) {
            case Critical -> CriticalLogs.add(l);
            case Warning -> WarningLogs.add(l);
            case Normal -> NormalLogs.add(l);
        }
        notifyListeners();
    }
    public void addLogToCollectionsWithoutNotify(Log l) {
        if (l == null) return;
        Item i = inventory.getItemBySerial(l.getSerial());
        AllLogs.add(l);
        logById.put(l.getLogID(), l);
        itemToLogs.computeIfAbsent(i, k -> new ArrayList<>()).add(l);

        switch (l.getSeverity()) {
            case Critical -> CriticalLogs.add(l);
            case Warning -> WarningLogs.add(l);
            case Normal -> NormalLogs.add(l);
        }
    }
    public void removeLog(Log log) {
        if (log == null) return;

        //Remove from all lists
        AllLogs.remove(log);

        switch (log.getSeverity()) {
            case Critical -> CriticalLogs.remove(log);
            case Warning -> WarningLogs.remove(log);
            case Normal  -> NormalLogs.remove(log);
        }

        //Remove from ID map
        logById.remove(log.getLogID());

        //Remove from item logs
        Item item = inventory.getItemBySerial(log.getSerial());
        if (item != null) {

            ArrayList<Log> itemLogs = itemToLogs.get(item);
            if (itemLogs != null) {
                itemLogs.remove(log);
                if (itemLogs.isEmpty()) {
                    itemToLogs.remove(item);
                }
            }
        }
        notifyListeners();
    }
    public void solveLog(Log l) {
        if (l == null) return;

        if (l.getSeverity() == Log.Severity.Normal||
        l.getType() == Log.LogType.ItemOutOfStock||
        l.getType() == Log.LogType.LowStock) return;

        removeLog(l);
        String msg = "Solved log #" + l.getLogID() + " (" + l.getSeverity() + "): \"" + l.getMessage() + "\"";
        createLog(Log.LogType.SolvedLog, 0, msg, l.getSerial());
    }
    public void suppressLog(Log l){
        if (l == null) {
            throw new RuntimeException("ERROR: suppressLog() called with a null Log reference.");
        }
        if (l.getSeverity() == Log.Severity.Normal) {
            throw new RuntimeException("ERROR: suppressLog() called on a Normal-severity log (Log ID: "
                    + l.getLogID() + "). Only Warning or Critical logs can be suppressed.");
        }

        if (l.getType() == Log.LogType.ItemShippedNotRegistered) {
            throw new RuntimeException("ERROR: unsuppressLog() called on ItemShippedNotRegistered log (Log ID: "
                    + l.getLogID() + "). These logs cannot be suppressed since they represent external platform mismatches.");
        }
        if (l.getType() == Log.LogType.OrderReceivedItemNotRegistered) {
            throw new RuntimeException("ERROR: unsuppressLog() called on OrderReceivedItemNotRegistered log (Log ID: "
                    + l.getLogID() + "). These logs cannot be suppressed since they represent external platform mismatches.");
        }

        if (l.isSuppressed()) {
            throw new RuntimeException("ERROR: suppressLog() called, but log #" + l.getLogID()
                    + " is already suppressed.");
        }

        String msg = "Suppressed log #" + l.getLogID() + " (" + l.getSeverity() + "): \"" + l.getMessage() + "\"";
        createLog(Log.LogType.SuppressedLog, 0, msg, l.getSerial());
    }

    public void unsuppressLog(Log l) {
        if (l == null) {
            throw new RuntimeException("ERROR: unsuppressLog() called with a null Log reference.");
        }
        if (l.getSeverity() == Log.Severity.Normal) {
            throw new RuntimeException("ERROR: unsuppressLog() called on a Normal-severity log (Log ID: "
                    + l.getLogID() + "). Only Warning or Critical logs can be suppressed.");
        }

        if (l.getType() == Log.LogType.ItemShippedNotRegistered) {
            throw new RuntimeException("ERROR: unsuppressLog() called on ItemShippedNotRegistered log (Log ID: "
                    + l.getLogID() + "). These logs cannot be suppressed since they represent external platform mismatches.");
        }
        if (l.getType() == Log.LogType.OrderReceivedItemNotRegistered) {
            throw new RuntimeException("ERROR: unsuppressLog() called on OrderReceivedItemNotRegistered log (Log ID: "
                    + l.getLogID() + "). These logs cannot be suppressed since they represent external platform mismatches.");
        }
        if (!l.isSuppressed()) {
            throw new RuntimeException("ERROR: unsuppressLog() called, but log #" + l.getLogID()
                    + " is not suppressed.");
        }

        //Remove the previously created log
        Item i = inventory.getItemBySerial(l.getSerial());
        if (i == null) {
            throw new RuntimeException("ERROR: Item is null in unsuppressLog()");
        }

        ArrayList<Log> itemLogs = itemToLogs.get(i);
        if (itemLogs == null || itemLogs.isEmpty()) {
            throw new RuntimeException("ERROR: Item has no logs to remove");
        }

        Log toRemove = null;
        for (Log suppressLog : itemLogs) {
            if (suppressLog.getType() == Log.LogType.SuppressedLog &&
                    suppressLog.getMessage().contains("#" + l.getLogID())){ //Double check that the suppressed log is specifically for this log
                toRemove = suppressLog;
                break;
            }
        }
        if (toRemove != null) {
            removeLog(toRemove);
            l.setSuppressed(false);
        }
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
