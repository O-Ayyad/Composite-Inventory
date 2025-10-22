package core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LogManager {
    public Inventory inventory;


    public LinkedList<Log> AllLogs = new LinkedList<>();
    public LinkedList<Log> CriticalLogs = new LinkedList<>();
    public LinkedList<Log> WarningLogs = new LinkedList<>();
    public LinkedList<Log> NormalLogs = new LinkedList<>();

    // Quick lookup by ID
    private Map<Integer, Log> logById = new HashMap<>();


    public LogManager(){} //Constructor

    public void setInventory(Inventory i){
        inventory = i;

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
        AllLogs.add(l);
        logById.put(l.getLogID(), l);

        switch (l.getSeverity()) {
            case Critical -> CriticalLogs.add(l);
            case Warning -> WarningLogs.add(l);
            case Normal -> NormalLogs.add(l);
        }
    }
    public void revertLog(Log l){
        if(l.isReverted()) return;
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
}
