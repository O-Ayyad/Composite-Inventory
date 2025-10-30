package core;
import javax.swing.*;
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

    public Inventory(){
    }
    public void setLogManager(LogManager lm){
        logManager = lm;
        logManager.addChangeListener(() -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    checkLowAndOutOfStock();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {

                        Thread.sleep(500);
                        checkLowAndOutOfStock();

                    }catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
        /// -----In main
        /// LogManager logManager = new LogManager();
        /// Inventory inventory = new Inventory();
        ///
        /// inventory.setLogManager(logManager);
        /// logManager.setInventory(inventory);
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
    //Edit the amount of an item
    public void addItemAmount(Item item, int amount){
        if (!hasItem(item)) return;
        int quantity = MainInventory.getOrDefault(item, 0);
        MainInventory.put(item, quantity + amount);
        logManager.createLog(Log.LogType.ItemAdded,
                amount,
                "Added " + amount + " units of item '" + item.getName() +
                        "' (Serial: " + item.getSerialNum() + "). " +
                        "New quantity: " + MainInventory.get(item),
                item.getSerialNum()
        );
    }
    public void decreaseItemAmount(Item item, int amount){
        if (!hasItem(item)) return;

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
    public void setQuantity(Item item,int quantity){
        if (!hasItem(item)) return;
        if(quantity < 0) return;
        MainInventory.put(item, quantity);
        logManager.createLog(Log.LogType.ItemUpdated,
                0,
                "Set stock of item '" + item.getName() +
                        "' (Serial: " + item.getSerialNum() + "). " +
                        "New quantity: " + MainInventory.get(item),
                item.getSerialNum()
        );
    }


    public void processItemPacket(ItemPacket ip){
        int IPQuant = ip.getQuantity();
        if(IPQuant == 0) return;
        if(IPQuant > 0){
            addItemAmount(ip.getItem(), IPQuant);
        } else{
            decreaseItemAmount(ip.getItem(), IPQuant);
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
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    //Rarely used to remove an item completely from inventory
    public void removeItem(Item item){

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
        ArrayList<Log> logsToRemove = logManager.itemToLogs.remove(item);

        if (logsToRemove != null) {

            logManager.AllLogs.removeAll(logsToRemove);
            logManager.CriticalLogs.removeAll(logsToRemove);
            logManager.WarningLogs.removeAll(logsToRemove);
            logManager.NormalLogs.removeAll(logsToRemove);
        }

        logManager.createLog(
                Log.LogType.ItemRemoved,
                0,
                "Removed item '" + item.getName() +
                        "' (Serial: " + item.getSerialNum() + ") from inventory and all associated logs.",
                item.getSerialNum()
        );
    }
    public void removeItem(String serial) {
        Item item = getItemBySerial(serial);
        if (item != null) {
            removeItem(item); //Not recursive
        }
    }


    //Ensures all 5 hashmaps are always in sync
    private void registerItemMapping(Item item, int amount){
        if (item == null) return;

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

        if (item.getSerialNum() != null)
            SerialToItemMap.remove(item.getSerialNum());
        if (item.getAmazonSellerSKU() != null)
            AmazonSKUToItemMap.remove(item.getAmazonSellerSKU());
        if (item.getEbaySellerSKU() != null)
            EbaySKUToItemMap.remove(item.getEbaySellerSKU());
        if (item.getWalmartSellerSKU() != null)
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

    public boolean findSerial(Item item) {
        if (item == null || item.getSerialNum() == null) return false;
        return SerialToItemMap.containsKey(item.getSerialNum());
    }
    public boolean findSerial(String serial) {
        if (serial == null || serial.isEmpty()) return false;
        return SerialToItemMap.containsKey(serial);
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
}
