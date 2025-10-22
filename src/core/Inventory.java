package core;
import java.util.*;

public class Inventory {

    //This inventory stores all the items that exist
    //If the item is not it stock then it is still in MainInventory with quantity 0
    //Item packets are sent by the ItemManager to here to update inventory
    //If an item is not in MainInventory then it does not exist in any way

    public Map<Item, Integer> MainInventory = new HashMap<Item,Integer>();

    public LogManager logManager;

    public Inventory(HashMap<Item, Integer> map){
        MainInventory = map;
    }
    public void setLogManager(LogManager lm){
        logManager = lm;
        /// -----In main
        /// LogManager logManager = new LogManager();
        /// Inventory inventory = new Inventory(new HashMap<>(), logManager);
        ///
        /// inventory.setLogManager(logManager);
        /// logManager.setInventory(inventory);
    }
    //Edit the amount of an item
    public void addItemAmount(Item item, int amount){
        if (!hasItem(item) || amount <= 0) return;
        int quantity = MainInventory.getOrDefault(item, 0);
        MainInventory.put(item, quantity + amount);

    }
    public void decreaseItemAmount(Item item, int amount){
        if (!hasItem(item)) return;

        int quantity = Math.abs(MainInventory.get(item));
        quantity = Math.max(0,quantity-amount);
        MainInventory.put(item, quantity);
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
        for (Item item : MainInventory.keySet()) {
            if (item.getSerialNum().equals(serial)) {
                return item;
            }
        }
        return null;
    }
    //Get current quantity of an item
    public int getQuantity(Item item){
        return MainInventory.getOrDefault(item, 0);
    }

    //Check if an item exists in inventory
    public boolean hasItem(Item item){
        return MainInventory.containsKey(item);
    }
    //Rarely used to remove an item completely from inventory
    public void RemoveItem(Item item){
        MainInventory.remove(item);
    }
}
