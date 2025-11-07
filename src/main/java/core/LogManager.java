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

    private final Map<String, Log> latestLogPerItem = new HashMap<>();


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
    public void createReverterLog(Log.LogType type, int amount, String message, String itemSerial, boolean reverted, int revertedLogID) {
        Log l = new Log(type, amount, message, itemSerial,reverted,revertedLogID);
        Log revertedLog = logById.get(revertedLogID);
        revertedLog.setRevertedLogID(l.getLogID());

        addLogToCollections(l);
    }
    private void addLogToCollections(Log l) {
        if (l == null) return;
        Item i = inventory.getItemBySerial(l.getSerial());
        AllLogs.add(l);
        logById.put(l.getLogID(), l);
        itemToLogs.computeIfAbsent(i, k -> new ArrayList<>()).add(l);
        if (l.getSerial() != null) {
            latestLogPerItem.put(l.getSerial(), l);
        }

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
        if(log.getSerial() != null){
            latestLogPerItem.remove(log.getSerial());
        }
        notifyListeners();
    }
    public boolean canRevert(Log l) {
        if (l == null) return false;
        if (l.isReverted()) return false;
        if (l.getSeverity() != Log.Severity.Normal) return false;

        return l.getType() == Log.LogType.ItemSold ||
                l.getType() == Log.LogType.ItemAdded ||
                l.getType() ==Log.LogType.NewItemCreated;
    }
    public Boolean canRevertSafely(Log l){
        Log latest = latestLogPerItem.get(l.getSerial());
        return (latest != null && latest.getLogID() == l.getLogID()) && canRevert(l);
    }

    public void revertLog(Log l){
        if (!canRevert(l)) {
            throw new IllegalArgumentException("Attempted to revert an un-revertable log");
        }
        if(canRevertSafely(l)){
            Log.SerialAndAmount inverse = l.getLogInverse();
            int quant = inverse.getQuantity();

            Log.LogType lt = quant > 0 ? Log.LogType.ItemAdded : Log.LogType.ItemRemoved;

            String message = "Reverted Log #" + l.getLogID() + " :\"" + l.getMessage() + "\"";
            createReverterLog(lt,quant, message, l.getSerial(),true,l.getLogID());
            l.setReverted(true);

            SendSerialAndAmountToInventory(inverse);
        }else if(l.getType() == Log.LogType.NewItemCreated){
            inventory.removeItemSilent(l.getSerial());
            l.setReverted(true);
            String message = "Reverted Log #" + l.getLogID() + " :\"" + l.getMessage() + "\"";
            createReverterLog(Log.LogType.ItemRemoved,0, message, l.getSerial(),true,l.getLogID());
        }
    }
    public void SendSerialAndAmountToInventory(Log.SerialAndAmount SAA) {
        if (SAA == null || SAA.getSerialNumber() == null) return;

        Item item = inventory.getItemBySerial(SAA.getSerialNumber());
        if (item == null) return; // item not found, ignore

        ItemPacket ip = new ItemPacket(item, SAA.getQuantity());
        inventory.processItemPacket(ip);
    }
    public void solveLog(Log l) {
        if (l == null) return;

        if (l.getSeverity() == Log.Severity.Normal||
        l.getType() == Log.LogType.ItemOutOfStock||
        l.getType() == Log.LogType.LowStock) return;

        removeLog(l);
        String msg = "Solved log #" + l.getLogID() + " (" + l.getSeverity() + "): \"" + l.getMessage() + "\"";
        createLog(Log.LogType.LogSolved, 0, msg, l.getSerial());
    }
    public void suppressLog(Log l) throws Exception {
        if (l == null) {
            throw new RuntimeException("suppressLog() called with a null Log reference.");
        }
        if (l.getSeverity() == Log.Severity.Normal) {
            throw new RuntimeException("suppressLog() called on a Normal-severity log (Log ID: "
                    + l.getLogID() + "). Only Warning or Critical logs can be suppressed.");
        }

        if (l.getType() == Log.LogType.ItemSoldAndNotRegisteredInInventory) {
            throw new RuntimeException("suppressLog() called on ItemSoldAndNotRegisteredInInventory log (Log ID: "
                    + l.getLogID() + "). These logs cannot be suppressed since they represent external platform mismatches.");
        }

        if (l.isSuppressed()) {
            throw new RuntimeException("suppressLog() called, but log #" + l.getLogID()
                    + " is already suppressed.");
        }

        String msg = "Suppressed log #" + l.getLogID() + " (" + l.getSeverity() + "): \"" + l.getMessage() + "\"";
        createLog(Log.LogType.LogSuppressed, 0, msg, l.getSerial());
    }

    public void unsuppressLog(Log l) {
        if (l == null) {
            throw new RuntimeException("unsuppressLog() called with a null Log reference.");
        }
        if (l.getSeverity() == Log.Severity.Normal) {
            throw new RuntimeException("unsuppressLog() called on a Normal-severity log (Log ID: "
                    + l.getLogID() + "). Only Warning or Critical logs can be suppressed.");
        }

        if (l.getType() == Log.LogType.ItemSoldAndNotRegisteredInInventory) {
            throw new RuntimeException("unsuppressLog() called on ItemSoldAndNotRegisteredInInventory log (Log ID: "
                    + l.getLogID() + "). These logs cannot be suppressed since they represent external platform mismatches.");
        }

        if (!l.isSuppressed()) {
            throw new RuntimeException("unsuppressLog() called, but log #" + l.getLogID()
                    + " is not suppressed.");
        }

        //Remove the previously created log
        Item i = inventory.getItemBySerial(l.getSerial());
        if (i == null) {
            throw new RuntimeException("Item is null in unsuppressLog()");
        }

        ArrayList<Log> itemLogs = itemToLogs.get(i);
        if (itemLogs == null || itemLogs.isEmpty()) {
            throw new RuntimeException("Item has no logs to remove");
        }

        Log toRemove = null;
        for (Log suppressLog : itemLogs) {
            if (suppressLog.getType() == Log.LogType.LogSuppressed &&
                    suppressLog.getMessage().contains("#" + l.getLogID())){ //Double check that the suppressed log is specifically for this log
                toRemove = suppressLog;
                System.out.println("found");
                break;
            }
        }
        if (toRemove != null) {
            removeLog(toRemove);
            l.setSuppressed(false);
            System.out.println("deleted");
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
