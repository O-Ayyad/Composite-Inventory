package core;
import constants.Constants;

import javax.swing.*;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Inventory {

    //This inventory stores all the items that exist
    //If the item is not it stock then it is still in MainInventory with quantity 0
    //Item packets are sent by the ItemManager to here to update inventory
    //If an item is not in MainInventory then it does not exist in any way

    public Map<Item, Integer> MainInventory = new HashMap<>();
    public Map<String, Item> SerialToItemMap = new HashMap<>();

    //O(1) sku lookup
    public Map<String, Item> AmazonSKUToItemMap = new HashMap<>();
    public Map<String, Item> EbaySKUToItemMap = new HashMap<>();
    public Map<String, Item> WalmartSKUToItemMap = new HashMap<>();

    public LogManager logManager;
    public ItemManager itemManager;

    //Used to close edit and view window when an item is deleted. Main window is the only one here
    private final ArrayList<ItemListener> listeners = new ArrayList<>();

    public Inventory(){
    }
    public void setLogManager(LogManager lm){
        logManager = lm;
        logManager.addChangeListener(() ->
            SwingUtilities.invokeLater(() -> {
                try {
                    checkLowAndOutOfStock();
                } catch (Exception ex) {
                    try {

                        Thread.sleep(500);
                        checkLowAndOutOfStock();

                    }catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            })
        );
    }
    public void setItemManager(ItemManager im){
        itemManager = im;
    }
    public void createItem(
            String name,
            String serialNum,
            Integer lowStockTrigger,
            ArrayList<ItemPacket> composedOf,
            String iconPath,
            ItemManager itemManager,
            String amazonSellerSKU,
            String ebaySellerSKU,
            String walmartSellerSKU,
            int amount
    ){
        //Check if item with this serial number already exists then just add the amount added
        if (SerialToItemMap.containsKey(serialNum)) {
           Item i = SerialToItemMap.get(serialNum);
           addItemAmount(i,amount);
           return;
        }
        if (AmazonSKUToItemMap.containsKey(amazonSellerSKU)) {
            Item i = AmazonSKUToItemMap.get(amazonSellerSKU);
            addItemAmount(i,amount);
            return;
        }
        if (EbaySKUToItemMap.containsKey(ebaySellerSKU)) {
            Item i = EbaySKUToItemMap.get(ebaySellerSKU);
            addItemAmount(i,amount);
            return;
        }
        if (WalmartSKUToItemMap.containsKey(walmartSellerSKU)) {
            Item i = WalmartSKUToItemMap.get(walmartSellerSKU);
            addItemAmount(i,amount);
            return;
        }


        //Create new item
        Item newItem = new Item(
                name,
                serialNum,
                lowStockTrigger,
                composedOf,
                iconPath,
                itemManager,
                amazonSellerSKU,
                ebaySellerSKU,
                walmartSellerSKU
        );

        registerItemMapping(newItem, amount);

        String quantityMessage = amount > 0 ? "Quantity : "+ amount : "";
        if (logManager != null) {
            logManager.createLog(Log.LogType.NewItemCreated,
                      0,
                    "Created new item: " + name + " (Serial: " + serialNum + ") " +quantityMessage,
                    serialNum);
        }
    }
    public void createItemFromSave(Item item, int amount) {
        //Check if item with this serial number already exists then just add the amount added
        if(MainInventory.get(item) != null){ //Duplicate item
            return;
        }
        if (SerialToItemMap.containsKey(item.getSerialNum())) {
            Item i = SerialToItemMap.get(item.getSerialNum());
            addItemAmount(i,amount);
            return;
        }
        if (AmazonSKUToItemMap.containsKey(item.getAmazonSellerSKU())) {
            Item i = AmazonSKUToItemMap.get(item.getAmazonSellerSKU());
            addItemAmount(i,amount);
            return;
        }
        if (EbaySKUToItemMap.containsKey(item.getEbaySellerSKU())){
            Item i = EbaySKUToItemMap.get(item.getEbaySellerSKU());
            addItemAmount(i,amount);
            return;
        }
        if (WalmartSKUToItemMap.containsKey(item.getWalmartSellerSKU())) {
            Item i = WalmartSKUToItemMap.get(item.getWalmartSellerSKU());
            addItemAmount(i,amount);
            return;
        }

        //Create new item
        Item newItem = new Item(
                item.getName(),
                item.getSerialNum(),
                item.getLowStockTrigger(),
                item.getComposedOf(),
                item.getImagePath(),
                itemManager,
                item.getAmazonSellerSKU(),
                item.getEbaySellerSKU(),
                item.getWalmartSellerSKU()
        );

        registerItemMapping(newItem, amount);
    }
    //Edit the amount of an item
    public void addItemAmount(Item item, int amount){
        if( item == null){
            throw new IllegalStateException("Null item called in addItemAmount()");
        }
        addItemAmountSilent(item,amount);
        logManager.createLog(Log.LogType.ItemAdded,
                amount,
                "Added " + amount + " units of item '" + item.getName() +
                        "' (Serial: " + item.getSerialNum() + "). " +
                        "New quantity: " + MainInventory.get(item),
                item.getSerialNum()
        );
    }
    //Does the same without logs
    public void addItemAmountSilent(Item item, int amount){
        if( item == null){
            throw new IllegalStateException("Null item called in addItemAmount()");
        }
        if (!hasItem(item)) {
            throw new IllegalStateException("Item not found in addItemAmount: " + item.getName());
        }
        int quantity = MainInventory.getOrDefault(item, 0);
        MainInventory.put(item, quantity + amount);
    }

    public void decreaseItemAmount(Item item, int amount){
        if (!hasItem(item)) {
            throw new IllegalStateException("Item not found in addItemAmount: " + item.getName());
        }

        int quantity = MainInventory.get(item);
        quantity = Math.max(0,quantity-amount);
        MainInventory.put(item, quantity);
        logManager.createLog(Log.LogType.ItemRemoved,
                amount,
                "Removed " + amount + " units of item '" + item.getName() +
                        "' (Serial: " + item.getSerialNum() + "). " +
                        "New quantity: " + MainInventory.get(item),
                item.getSerialNum()
        );
    }

    public void decreaseItemAmountSilent(Item item, int amount){
        if (!hasItem(item)) {
            throw new IllegalStateException("Item not found in addItemAmount: " + item.getName());
        }

        int quantity = MainInventory.get(item);
        quantity = Math.max(0,quantity-amount);
        MainInventory.put(item, quantity);
    }
    public void setQuantity(Item item,int quantity){
        if (!hasItem(item)) {
            throw new IllegalStateException("Item not found in setQuantity: " + item.getName());
        }
        if(quantity < 0) return;
        MainInventory.put(item, quantity);
    }


    public void processItemPacket(ItemPacket ip){
        int IPQuant = ip.getQuantity();
        if(IPQuant == 0) return;
        if(IPQuant > 0){
            addItemAmountSilent(ip.getItem(), IPQuant);
        } else{
            decreaseItemAmountSilent(ip.getItem(), IPQuant);
        }
    }
    public void processItemPacketList(ArrayList<ItemPacket> list){
        for(ItemPacket ip : list){
            processItemPacket(ip);
        }
    }
    public Item getItemBySerial(String serial) {
        return SerialToItemMap.get(serial);
    }
    //Get current quantity of an item
    public int getQuantity(Item item){
        return MainInventory.getOrDefault(item, 0);
    }

    //Check if an item exists in inventory
    public boolean hasItem(Item item){
        return MainInventory.containsKey(item);
    }
    public boolean hasItem(String serial) {
        Item item = getItemBySerial(serial);
        return (item != null) && hasItem(item);
    }
    //Only for duplicate checks since this is o(n) no 0(1)
    public Item getItemByName(String name) {
        if (name == null || name.isEmpty()) return null;

        for (Item item : MainInventory.keySet()) {
            if (item == null) {
                System.err.println("Warning: null item key found in MainInventory!");
                continue;
            }
            if (name.equalsIgnoreCase(item.getName())) {
                return item;
            }
        }
        return null;
    }

    public void composeItem(Item item, int amount) {
        if (item == null || !item.isComposite() || amount <= 0) {
            throw new RuntimeException("ComposeItem called on non-existent item, or non-composite Item, or amount is invalid");
        }
        for(ItemPacket ip: item.getComposedOf()){ //Check if we have enough of each part
            long required = (long)ip.getQuantity() * (long)amount;
            if(required > getQuantity(ip.getItem())){
                throw new RuntimeException("Not enough "+ ip.getItem().getName() + " to compose item: "+ item.getName() +". \n" +
                        "(Amount needed = "+ip.getQuantity()*amount + " || Amount available = "+getQuantity(ip.getItem()) +")");
            }
        }
        for(int i = 0; i <amount; i++){
            itemManager.composeItem(item);
        }
        //Log the composition
        if (logManager != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Composed ")
                    .append(amount)
                    .append(" of ")
                    .append(item.getName())
                    .append("' (Serial: ").append(item.getSerialNum()).append(") using: \n");

            for (ItemPacket ip : item.getComposedOf()) {
                sb.append("[").append(ip.getItem().getName())
                        .append(" x").append(ip.getQuantity()).append("], \n");
            }

           logManager.createLog(
                    Log.LogType.ItemComposed,
                    1,
                    sb.toString(),
                    item.getSerialNum()
            );
        }
    }
    public void breakDownItem(Item item, ArrayList<ItemPacket> used) {
        if (item == null || !item.isComposite()) {
            throw new RuntimeException("breakDownItem() called on non-existent item, or non-composite Item, or amount is invalid");
        }

        ItemManager.BreakdownResult result = itemManager.breakDownItem(item, used);

        StringBuilder originalSB  = new StringBuilder();
        StringBuilder reclaimedSB = new StringBuilder();
        StringBuilder usedSB = new StringBuilder();

        //Original
        for (Map.Entry<Item, Integer> e : result.original().entrySet()) {
            originalSB.append(e.getKey().getName())
                    .append(" x")
                    .append(e.getValue())
                    .append(", ");
        }

        //Reclaimed
        for (ItemPacket IP : result.remained()) {
            reclaimedSB.append(IP.getItem().getName())
                    .append(" x")
                    .append(IP.getQuantity())
                    .append(", ");
        }

        //USed
        for (Map.Entry<Item, Integer> e : result.used().entrySet()) {
            usedSB.append(e.getKey().getName())
                    .append(" x")
                    .append(e.getValue())
                    .append(", ");
        }
        if (originalSB.length() > 2)  originalSB.setLength(originalSB.length() - 2);
        if (reclaimedSB.length() > 2) reclaimedSB.setLength(reclaimedSB.length() - 2);
        if (usedSB.length() > 2)    usedSB.setLength(usedSB.length() - 2);

        String logMessage =
                "Broke down 1 " + item.getName() + "\n" +
                        "  Original: ["   + originalSB  + "]\n" +

                        "\n  Used : [" + usedSB + "]\n" +

                        "\n  Kept: ["  + reclaimedSB + "]\n" +

                        "\n  Inventory: " + result.before() + " → " + result.after();

        logManager.createLog(Log.LogType.ItemBrokenDown,1, logMessage,item.getSerialNum());
    }
    //Rarely used to delete an item completely from inventory
    public void removeItem(Item item){
        removeItemSilent(item);

        logManager.createLog(
                Log.LogType.ItemDeleted,
                0,
                "Deleted item '" + item.getName() +
                        "' (Serial: " + item.getSerialNum() + ") from inventory and all associated logs.",
                item.getSerialNum()
        );
    }

    //Checks if for any X, if X has a composite part that contains X anywhere then it returns false.
    public boolean containsItemRecursively(Item item, String searchSerial) {
        if (item.getComposedOf() == null) return false;

        for (ItemPacket packet : item.getComposedOf()) {
            Item component = packet.getItem();
            if (component.getSerialNum().equals(searchSerial)) {
                return true;
            }
            if (containsItemRecursively(component, searchSerial)) {
                return true;
            }
        }
        return false;
    }
    public void removeItemSilent(Item item){

        //Remove composition links
        for(Item other: item.getComposesInto()){

            if (other == item) continue;

            other.getComposesInto().remove(item);

            other.getComposedOf().removeIf(packet -> {
                Item component = packet.getItem();
                return component != null && component.equals(item);
            });
        }

        item.getComposesInto().clear();
        item.getComposedOf().clear();

        //Remove all mappings
        unregisterItemMapping(item);

        //Remove all logs
        ArrayList<Log> logsToRemove = logManager.itemToLogs.get(item);

        if (logsToRemove != null) {
            ArrayList<Log> copy = new ArrayList<>(logsToRemove);
            for (Log l : copy) {
                logManager.removeLog(l);
            }
        }

        if(!item.getImagePath().equals(Constants.NOT_FOUND_PNG)){
            try{
                Files.deleteIfExists(Paths.get(item.getImagePath()));
            } catch (Exception e){
                System.out.println("Could not delete image files for item: " + item.getName());
            }
        }
        notifyListeners(item);
    }


    //Ensures all  hashmaps are always in sync
    public void registerItemMapping(Item item, int amount){
        if (item == null || amount < 0) {
            throw new RuntimeException("registerItemMapping called on null item or negative quantity");
        }

        MainInventory.put(item, amount);

        if (item.getSerialNum() != null && !item.getSerialNum().isEmpty())
            SerialToItemMap.put(item.getSerialNum(), item);

        if (item.getAmazonSellerSKU() != null && !item.getAmazonSellerSKU().isEmpty())
            AmazonSKUToItemMap.put(item.getAmazonSellerSKU(), item);
        if (item.getEbaySellerSKU() != null && !item.getEbaySellerSKU().isEmpty())
            EbaySKUToItemMap.put(item.getEbaySellerSKU(), item);
        if (item.getWalmartSellerSKU() != null && !item.getWalmartSellerSKU().isEmpty())
            WalmartSKUToItemMap.put(item.getWalmartSellerSKU(), item);
    }
    private void unregisterItemMapping(Item item) {
        if (item == null) return;

        MainInventory.remove(item);

        if (item.getSerialNum() != null && !item.getSerialNum().isEmpty())
            SerialToItemMap.remove(item.getSerialNum());
        if (item.getAmazonSellerSKU() != null && !item.getAmazonSellerSKU().isEmpty())
            AmazonSKUToItemMap.remove(item.getAmazonSellerSKU());
        if (item.getEbaySellerSKU() != null && !item.getEbaySellerSKU().isEmpty())
            EbaySKUToItemMap.remove(item.getEbaySellerSKU());
        if (item.getWalmartSellerSKU() != null && !item.getWalmartSellerSKU().isEmpty())
            WalmartSKUToItemMap.remove(item.getWalmartSellerSKU());
    }
    //Amazon SKU
    public boolean findAmazonSKU(Item item) {
        if (item == null || item.getAmazonSellerSKU() == null) return false;
        return AmazonSKUToItemMap.containsKey(item.getAmazonSellerSKU());
    }

    public boolean findAmazonSKU(String amazonSKU) {
        if (amazonSKU == null || amazonSKU.isEmpty()) return false;
        return AmazonSKUToItemMap.containsKey(amazonSKU);
    }

    // Ebay SKU
    public boolean findEbaySKU(Item item) {
        if (item == null || item.getEbaySellerSKU() == null) return false;
        return EbaySKUToItemMap.containsKey(item.getEbaySellerSKU());
    }
    public boolean findEbaySKU(String ebaySKU) {
        if (ebaySKU == null || ebaySKU.isEmpty()) return false;
        return EbaySKUToItemMap.containsKey(ebaySKU);
    }

    //Walmart SKU
    public boolean findWalmartSKU(Item item) {
        if (item == null || item.getWalmartSellerSKU() == null) return false;
        return WalmartSKUToItemMap.containsKey(item.getWalmartSellerSKU());
    }
    public boolean findWalmartSKU(String walmartSKU) {
        if (walmartSKU == null || walmartSKU.isEmpty()) return false;
        return WalmartSKUToItemMap.containsKey(walmartSKU);
    }

    //Sku lookups
    public Item getItemByAmazonSKU(String amazonSKU) {
        if (amazonSKU == null || amazonSKU.isEmpty()) return null;
        return AmazonSKUToItemMap.get(amazonSKU);
    }
    public Item getItemByEbaySKU(String ebaySKU) {
        if (ebaySKU == null || ebaySKU.isEmpty()) return null;
        return EbaySKUToItemMap.get(ebaySKU);
    }

    public Item getItemByWalmartSKU(String walmartSKU) {
        if (walmartSKU == null || walmartSKU.isEmpty()) return null;
        return WalmartSKUToItemMap.get(walmartSKU);
    }

    public synchronized void checkLowAndOutOfStock() {
        List<Item> items = new ArrayList<>(MainInventory.keySet());

        for (Item i : items) {
            Integer currentQuantityObj = MainInventory.get(i);
            if (currentQuantityObj == null) continue; // in case removed mid-way
            int currentQuantity = currentQuantityObj;
            int trigger = i.getLowStockTrigger();
            if (trigger == 0) continue;

            ArrayList<Log> logsForI = logManager.itemToLogs.getOrDefault(i, new ArrayList<>());

            Log lowStockLog = null;
            Log outOfStockLog = null;

            for (Log l : logsForI) {
                if (l.getType() == Log.LogType.LowStock) lowStockLog = l;
                if (l.getType() == Log.LogType.ItemOutOfStock) outOfStockLog = l;
            }

            String lowStockReminder = " (Current quantity : " + currentQuantity + " | Low stock trigger : " + trigger + ")";

            //Out of stock
            if (currentQuantity == 0) {
                if (lowStockLog != null) logManager.removeLog(lowStockLog);
                if (outOfStockLog == null) {
                    logManager.createLog(
                            Log.LogType.ItemOutOfStock,
                            0,
                            i.getName() + " (" + i.getSerialNum() + ") out of stock!" + lowStockReminder,
                            i.getSerialNum()
                    );
                } else {
                    outOfStockLog.setMessage(
                            i.getName() + " (" + i.getSerialNum() + ") out of stock!" + lowStockReminder
                    );
                }
            }
            //Low stock
            else if (currentQuantity <= trigger) {
                if (lowStockLog == null) {
                    logManager.createLog(
                            Log.LogType.LowStock,
                            currentQuantity,
                            i.getName() + " (" + i.getSerialNum() + ") is low on stock!" + lowStockReminder,
                            i.getSerialNum()
                    );
                } else {
                    lowStockLog.setMessage(
                            i.getName() + " (" + i.getSerialNum() + ") is still low on stock!" + lowStockReminder
                    );
                }
                if (outOfStockLog != null) logManager.removeLog(outOfStockLog);
            }
            // Stock normal
            else {
                if (lowStockLog != null) logManager.removeLog(lowStockLog);
                if (outOfStockLog != null) logManager.removeLog(outOfStockLog);
            }
        }
    }
    public void addChangeListener(ItemListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Item i) {
        for (ItemListener listener : listeners) {
            listener.onChange(i);
        }
    }
    @FunctionalInterface
    public interface ItemListener {
        void onChange(Item item);
    }
}
