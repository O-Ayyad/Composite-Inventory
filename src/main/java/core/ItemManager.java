package core;
import java.util.HashMap;
import java.util.Map;

//Called by Item class and updates Inventory
public class ItemManager {

    public Inventory inventory; //Reference to inventory

    public ItemManager(Inventory inventory) {
        this.inventory = inventory;
    }

    //-------------------------------<Methods>-------------------------------

    //Clones an item's composed of then removes the item and adds composed of - removed items
    public BreakdownResult breakDownItem(Item item, Map<Item,Integer> removedItems){
        if (item == null || removedItems == null || inventory == null) throw new IllegalStateException("Item, removedItems, or inventory is null is breakDownItem()");

        Map<Item, Integer> originalMap = new HashMap<>(item.getComposedOf());

        //Make sure everything is right before breaking down
        for (Map.Entry<Item, Integer> entry : removedItems.entrySet()) {
            Item component = entry.getKey();
            int userQty = entry.getValue();

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
        for (Map.Entry<Item, Integer> e : removedItems.entrySet()) {
            if (e.getValue() > 0) {
                usedMap.put(e.getKey(), e.getValue());
            }
        }


        //Find remainders
        Map<Item, Integer> remainderComponents = new HashMap<>();
        for (Map.Entry<Item, Integer> orig : originalMap.entrySet()) {

            Item component = orig.getKey();
            int originalQty = orig.getValue();

            int usedQty = usedMap.getOrDefault(component, 0);
            int remainderQty = originalQty - usedQty;

            remainderComponents.put(component, remainderQty);
        }

        int beforeQuantity = inventory.getQuantity(item);

        inventory.processItemMap(remainderComponents);

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
        for (Item component : composedItem.getComposedOf().keySet()) {

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

        for (Map.Entry<Item,Integer> ip : composedItem.getComposedOf().entrySet()){ //We have all the items so consume
            inventory.decreaseItemAmountSilent(ip.getKey(),ip.getValue());
        }

        //Add the newly composed item
        inventory.addItemAmountSilent(composedItem, 1);
    }

    //-------------------------------</Methods>-------------------------------

    //Helper to log in inventory and not here
        public record BreakdownResult(Map<Item, Integer> original, Map<Item, Integer> used, Map<Item,Integer> remained,
                                      int before, int after) {
    }
}
