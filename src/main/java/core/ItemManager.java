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

    //Clones an item's composed of then removes the item and adds composed of - removed items
    public BreakdownResult breakDownItem(Item item, ArrayList<ItemPacket> removedItems){
        if (item == null || removedItems == null || inventory == null) throw new IllegalStateException("Item, removedItems, or inventory is null is breakDownItem()");

        Map<Item, Integer> originalMap = new HashMap<>();
        for (ItemPacket ip : item.getComposedOf()) {
            originalMap.put(ip.getItem(), ip.getQuantity());
        }

        //Make sure everything is right before breaking down
        for (ItemPacket userIP : removedItems) {
            Item component = userIP.getItem();
            int userQty = userIP.getQuantity();

            if (!originalMap.containsKey(component)) {
                throw new IllegalArgumentException("Cannot remove " + component.getName() +
                        ". It is not part of " + item.getName());
            }
            if (userQty < 0) {
                throw new IllegalArgumentException("Quantity is negative in using broken down item.");
            }
            if (userQty > originalMap.get(component)) {
                throw new IllegalArgumentException("Too many units of " + component.getName() +
                        " to remove. Max allowed: " + originalMap.get(component));
            }
        }

        //Map of what was used
        Map<Item, Integer> usedMap = new HashMap<>();
        for (ItemPacket ip : removedItems) {
            if (ip.getQuantity() > 0) {
                usedMap.put(ip.getItem(), ip.getQuantity());
            }
        }


        //Find remainders
        ArrayList<ItemPacket> remainderComponents = new ArrayList<>();
        for(Item toIP : originalMap.keySet()){
            Integer amountUsed = usedMap.get(toIP);
            amountUsed = (amountUsed == null) ? 0:amountUsed;

            ItemPacket IP = new ItemPacket(toIP, originalMap.get(toIP) - amountUsed);
            remainderComponents.add(IP);
        }

        int beforeQuantity = inventory.getQuantity(item);

        inventory.processItemPacketList(removedItems);
        inventory.decreaseItemAmountSilent(item, 1);

        int afterQuantity = inventory.getQuantity(item);

        return new BreakdownResult(
                originalMap,
                usedMap,
                remainderComponents,
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

    //-------------------------------</Methods>-------------------------------

    //Helper to log in inventory and not here
        public record BreakdownResult(Map<Item, Integer> original, Map<Item, Integer> used, ArrayList<ItemPacket> remained,
                                      int before, int after) {
    }
}
