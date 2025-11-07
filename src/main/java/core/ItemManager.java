package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//Called by Item class and updates Inventory
public class ItemManager {

    public Inventory inventory; //Reference to inventory

    public ItemManager(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    //-------------------------------<Methods>-------------------------------

    public boolean isComposedOfItem(Item item, Item searchItem) {
        for (ItemPacket packet : item.getComposedOf()) {
            if (packet.getItem().equals(searchItem)) {
                return true; // found the item
            }
        }
        return false; // not found
    }

    public ItemPacket getItemPacketInComposedOf(Item item, Item searchItem) {
        for (ItemPacket packet : item.getComposedOf()) {
            if (packet.getItem().equals(searchItem)) {
                return packet; // found the item
            }
        }
        return null; // not found
    }

    //Clones an item's composed of then removes the item and adds composed of - removed items
    public BreakdownResult breakDownItem(Item item, ArrayList<ItemPacket> removedItems){
        if (item == null || removedItems == null || inventory == null) throw new IllegalStateException("Item, reclaimedItems, or inventory is null is breakDownItem()");

        Map<Item, Integer> originalMap = new HashMap<>();
        for (ItemPacket ip : item.getComposedOf()) {
            originalMap.put(ip.getItem(), ip.getQuantity());
        }

        //Make sure everything is right before breaking down
        for (ItemPacket userIP : removedItems) {
            Item component = userIP.getItem();
            int userQty = userIP.getQuantity();

            if (!originalMap.containsKey(component)) {
                throw new IllegalArgumentException("Cannot reclaim " + component.getName() +
                        ". It is not part of " + item.getName());
            }
            if (userQty < 0) {
                throw new IllegalArgumentException("Quantity is negative in reclaiming broken down item.");
            }
            if (userQty > originalMap.get(component)) {
                throw new IllegalArgumentException("Too many units of " + component.getName() +
                        " to reclaim. Max allowed: " + originalMap.get(component));
            }
        }

        Map<Item, Integer> reclaimedMap = new HashMap<>();
        for (ItemPacket ip : removedItems) {
            if (ip.getQuantity() > 0) {
                reclaimedMap.put(ip.getItem(), ip.getQuantity());
            }
        }

        //Build list for processing
        ArrayList<ItemPacket> reclaimed = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : reclaimedMap.entrySet()) {
            reclaimed.add(new ItemPacket(e.getKey(), e.getValue()));
        }

        int beforeQuantity = inventory.getQuantity(item);

        inventory.processItemPacketList(reclaimed);
        inventory.decreaseItemAmountSilent(item, 1);

        int afterQuantity = inventory.getQuantity(item);

        Map<Item, Integer> usedMap = new HashMap<>();
        for (Map.Entry<Item, Integer> e : originalMap.entrySet()) {
            Item comp = e.getKey();
            int original = e.getValue();
            int recl = reclaimedMap.getOrDefault(comp, 0);
            int used = original - recl;
            if (used > 0) usedMap.put(comp, used);
        }

        return new BreakdownResult(
                originalMap,
                reclaimedMap,
                usedMap,
                beforeQuantity,
                afterQuantity
        );
    }
    // Takes a list of components (ItemPackets) and assembles them into a single composed item.
    // Each ItemPacket is consumed from inventory, and the composed item is added.
    // Can only add items, cannot create a new item
    // If a new item needs to be created via composition, it must be created before then passed into this method
    public void composeItem(Item composedItem) {
        if (composedItem == null || inventory == null) throw new IllegalStateException("composeItem called on null item or inventory is null");

        //Verify all components exist in inventory
        for (ItemPacket required : composedItem.getComposedOf()) {

            Item component = required.getItem();

            if (!inventory.hasItem(component)) { //Does item part exist in inventory
                throw new RuntimeException("Unable to find item " + component.getName() + " to compose item: "+ composedItem.getName());
            }

            int amountNeeded = inventory.getQuantity(component);
            int have = inventory.getQuantity(component);


            if (amountNeeded < have) { //Do we have enough of the item to compose
                throw new RuntimeException(
                        "Insufficient quantity for '" + component.getName() +
                                "' (have " + have + ", need " + amountNeeded + ") to compose item: " + composedItem.getName()
                );
            }
        }

        for (ItemPacket ip : composedItem.getComposedOf()){ //We have all the items so consume
            inventory.decreaseItemAmountSilent(ip.getItem(),ip.getQuantity());
        }

        //Add the newly composed item
        inventory.addItemAmountSilent(composedItem, 1);
    }
    public boolean checkIfSerialExists(String serial){
        return inventory.hasItem(serial);
    }
    private void logUpdate(Item item, String message) {
        if (inventory != null && inventory.logManager != null) {
            inventory.logManager.createLog(
                    Log.LogType.ItemUpdated,
                    0, //amount not relevant
                    message,
                    item.getSerialNum()
            );
        }
    }

    //-------------------------------</Methods>-------------------------------

    //Helper to log in inventory and not here
    public static class BreakdownResult {
        public final Map<Item, Integer> original;
        public final Map<Item, Integer> reclaimed;
        public final Map<Item, Integer> wasted;
        public final int before;
        public final int after;

        public BreakdownResult(Map<Item, Integer> original,
                               Map<Item, Integer> reclaimed,
                               Map<Item, Integer> wasted,
                               int before,
                               int after) {
            this.original = original;
            this.reclaimed = reclaimed;
            this.wasted = wasted;
            this.before = before;
            this.after = after;
        }
    }
}
