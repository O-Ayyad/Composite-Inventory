package core;
import java.util.*;

public class Inventory {

    //This inventory stores all the items that exist
    //If the item is not it stock then it is still in MainInventory with quantity 0
    //Item packets are sent by the ItemManager to here to update inventory
    //If an item is not in MainInventory then it does not exist in any way

    public Map<Item, Integer> MainInventory = new HashMap<>();
    public Map<String, Item> SerialToItemMap = new HashMap<>();

    public LogManager logManager;
    public ItemManager itemManager;

    public Inventory(){
    }
    public void setLogManager(LogManager lm){
        logManager = lm;
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

        MainInventory.put(newItem, amount);
        SerialToItemMap.put(serialNum, newItem);

        if (logManager != null) {
            logManager.createLog(Log.LogType.NewItemCreated,
                      0,
                    "Created new item: " + name + " (Serial: " + serialNum + ")",
                    serialNum);
            logManager.createLog(Log.LogType.ItemAdded,
                            amount,
                    "Added " + amount + " units of item '" + name +
                            "' (Serial: " + serialNum + "). " +
                            "New quantity: " + MainInventory.get(newItem),
                            serialNum
            );
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

    //Rarely used to remove an item completely from inventory
    public void RemoveItem(Item item){
        MainInventory.remove(item);
        SerialToItemMap.remove(item.getSerialNum());
        if (logManager != null) {
            logManager.createLog(
                    Log.LogType.ItemRemoved,
                    0,
                    "Removed item '" + item.getName() +
                            "' (Serial: " + item.getSerialNum() + ") from inventory.",
                        item.getSerialNum()
            );
        }
    }
    public void RemoveItem(String serial) {
        Item item = getItemBySerial(serial);
        if (item != null) {
            RemoveItem(item);
        }
    }
}
