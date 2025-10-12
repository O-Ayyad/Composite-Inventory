package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

//Called by Item class and updates Inventory
public class ItemManager {

    //This map holds all items that currently exist so that no two items can have two entries
    private ArrayList<Item> items = new ArrayList<>();

    private Inventory inventory; //Reference to inventory


    //-------------------------------<Methods>-------------------------------

    public boolean isComposedOfItem(Item item, Item searchItem) {
        for (ItemPacket packet : item.composedOf) {
            if (packet.getItem().equals(searchItem)) {
                return true; // found the item
            }
        }
        return false; // not found
    }

    public ItemPacket getItemPacket(Item item, Item searchItem) {
        for (ItemPacket packet : item.composedOf) {
            if (packet.getItem().equals(searchItem)) {
                return packet; // found the item
            }
        }
        return null; // not found
    }

    //Returns an array of items by the item's composed of and removes what was taken out
    public ArrayList<ItemPacket> breakDownItem(Item item, ArrayList<ItemPacket> removedItems){
        if (item == null || removedItems == null || inventory == null) return new ArrayList<>();
        ArrayList<ItemPacket> itemCopy = new ArrayList<>();
        Map<Item, ItemPacket> map = new HashMap<>();

        //Clone item.getComposed to itemCopy
        for(ItemPacket ip : item.getComposedOf()) {
            // Create a new ItemPacket with the same Item and quantity
            ItemPacket copy = new ItemPacket(ip.getItem(), ip.getQuantity());
            itemCopy.add(copy);
            map.put(copy.getItem(), copy);
        }

        for(ItemPacket removeIP : removedItems){
            ItemPacket main = map.get(removeIP.getItem());
            if (main != null) {
                main.setQuantity(main.getQuantity() - removeIP.getQuantity());
                if(main.getQuantity() <= 0){
                    itemCopy.remove(main);
                }
            }
        }
        inventory.processItemPacketList(itemCopy);
        inventory.decreaseItemAmount(item, 1);
        return itemCopy;
    }
    //-------------------------------</Methods>-------------------------------
}
