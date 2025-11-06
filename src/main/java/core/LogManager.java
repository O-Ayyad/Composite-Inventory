package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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


    public LogManager(){} //Constructor

    public void setInventory(Inventory i){
        inventory = i;
        if (i.logManager != this) {
            i.setLogManager(this);
        }

        /// -----In main
        /// LogManager logManager = new LogManager();
        /// Inventory inventory = new Inventory(new HashMap<>());
        ///
        /// inventory.setLogManager(logManager);
        /// logManager.setInventory(inventory);
    }
    // TODO: LOG SYSTEM OVERVIEW
    // - On startup, read logs.json ->deserialize all logs -> store in allLogs (oldest ->newest).
    // - Rebuild inventory by applying each log in order:
    //      * skip reverted / solved logs
    //      * apply only inventory-affecting logs
    // - Create 3 linked lists by criticality (Normal, Concerning, Urgent).
    // - Keep one master queue for chronological order.
    // - In UI scroll view from top to bottom Critical->Warning->Normal, newest-> oldest.
    // - Display logID, type, message, and note; dim solved/reverted logs.
    // - When new log created -> append to list + save to logs.json.
    // - Undo: create reverter log, mark target as reverted, append both to file.
    // - Ensure (amount of reverted == amount of reverter) always true.
    // - Optional: add filters (search, show unsolved only, export logs).
    // - Ability to solve and create alert logs

    //-------------------------------------Log Creation
    //All logs are created from the inventory except reversions
    public void createLog(Log.LogType type, int amount, String message, String itemSerial) {
        Log l = new Log(type, amount, message, itemSerial);
        addLogToCollections(l);
        //Todo add to list and file
    }
    //Reverter log creator
    public void createRevertedLog(Log.LogType type, int amount, String message, String itemSerial, boolean reverted, int revertedLogID) {
        Log l =new Log(type, amount, message, itemSerial,reverted,revertedLogID);

        addLogToCollections(l);
    }
    private void addLogToCollections(Log l) {
        if (l == null) return;
        Item i = inventory.getItemBySerial(l.getItemSerial());
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
        Item item = inventory.getItemBySerial(log.getItemSerial());
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
    public boolean canRevert(Log l) {
        return l != null && !l.isReverted() && l.getSeverity() == Log.Severity.Normal &&
                (l.getType() == Log.LogType.ItemSold || l.getType() == Log.LogType.ItemAdded);
    }

    public void revertLog(Log l){
        if (l == null || l.isReverted() || !l.getSeverity().equals(Log.Severity.Normal) ||
            !l.getType().equals(Log.LogType.ItemSold) && !l.getType().equals(Log.LogType.ItemAdded)) {

            throw new IllegalArgumentException("Attempted to revert an un-revertable log");
        }

        Log.SerialAndAmount inverse = l.getLogInverse();
        int quant = inverse.getQuantity();

        Log.LogType lt = quant > 0 ? Log.LogType.ItemAdded : Log.LogType.ItemRemoved;

        String message = "Reverted Log #" + l.getLogID() + " :\"" + l.getMessage() + "\"";
        createRevertedLog(lt,quant, message, l.getItemSerial(),true,l.getLogID());
        l.setReverted(true);

        SendSerialAndAmountToInventory(inverse);
    }
    public void SendSerialAndAmountToInventory(Log.SerialAndAmount SAA) {
        if (SAA == null || SAA.getSerialNumber() == null) return;

        Item item = inventory.getItemBySerial(SAA.getSerialNumber());
        if (item == null) return; // item not found, ignore

        ItemPacket ip = new ItemPacket(item, SAA.getQuantity());
        inventory.processItemPacket(ip);
    }
    public void suppressLog(Log l) {
        if (l == null) return;

        if (l.getSeverity() == Log.Severity.Normal) return;

        if (l.isSuppressed()) return;

        l.setSuppressed(true);

        String msg = "Suppressed log #" + l.getLogID() + " (" + l.getSeverity() + "): \"" + l.getMessage() + "\"";
        createLog(Log.LogType.LogSuppressed, 0, msg, l.getItemSerial());
    }

    public void unsuppressLog(Log l) {
        if (l == null) return;
        if (!l.isSuppressed()) return;
        l.setSuppressed(false);

        //Remove the previously created log
        Item i = inventory.getItemBySerial(l.getItemSerial());
        if (i == null) return;

        ArrayList<Log> itemLogs = itemToLogs.get(i);
        if (itemLogs == null || itemLogs.isEmpty()) return;

        Log toRemove = null;
        for (Log suppressLog : itemLogs) {
            if (suppressLog.getType() == Log.LogType.LogSuppressed &&
                    suppressLog.getMessage().contains("#" + l.getLogID())){ //Double check that the suppressed log is specifically for this log
                toRemove = suppressLog;
                break;
            }
        }

        if (toRemove != null) {
            removeLog(toRemove);
        }
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    //Does the action of a log without creating a for all logs, called once only when the mainWindow is created
    //Displays all logs and rebuilds inventory to see if logs and inventory are in sync
    public void processAllLogs(){
        for(Log l : AllLogs){
            switch(l.getSeverity()){
                case Normal ->{

                }
                case Warning -> {

                }
                case Critical -> {

                }
            }
        }
    }
    //Does the action of a log without creating a log
    public void processLog(Log l){
        switch(l.getSeverity()){
            case Normal ->{

            }
            case Warning -> {

            }
            case Critical -> {

            }
        }
    }
}
