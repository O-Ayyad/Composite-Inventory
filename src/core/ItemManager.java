package core;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.ImageIcon;

//Called by Item class and updates Inventory
public class ItemManager {

    private Inventory inventory; //Reference to inventory

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

    public ItemPacket getItemPacket(Item item, Item searchItem) {
        for (ItemPacket packet : item.getComposedOf()) {
            if (packet.getItem().equals(searchItem)) {
                return packet; // found the item
            }
        }
        return null; // not found
    }

    //Clones an item's composed of then removes the item and adds composed of - removed items
    public void breakDownItem(Item item, ArrayList<ItemPacket> removedItems){
        if (item == null || removedItems == null || inventory == null) return;
        ArrayList<ItemPacket> itemCopy = new ArrayList<>();
        Map<Item, ItemPacket> map = new HashMap<>();

        //Clone item.getComposed to itemCopy
        for(ItemPacket ip : item.getComposedOf()) {
            // Create a new ItemPacket with the same Item and quantity
            ItemPacket copy = new ItemPacket(ip.getItem(), ip.getQuantity());
            itemCopy.add(copy);
            map.put(copy.getItem(), copy);
        }

        for (ItemPacket ip : item.getComposedOf()) {
            boolean found = false;
            for (ItemPacket removeIP : removedItems) {
                if (removeIP.getItem().equals(ip.getItem())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return;
            }
        }
        for (ItemPacket removeIP : removedItems) {
            ItemPacket main = map.get(removeIP.getItem());
            if (main != null) {
                main.setQuantity(main.getQuantity() - removeIP.getQuantity());
                if (main.getQuantity() <= 0) {
                    itemCopy.remove(main);
                }
            }
        }
        String parts = "";
        for (int i = 0; i < itemCopy.size(); i++) {
            ItemPacket ip = itemCopy.get(i);
            parts += ip.getItem().getName() + " x" + ip.getQuantity();
            if (i < itemCopy.size() - 1) parts += ", ";
        }
        inventory.processItemPacketList(itemCopy);
        inventory.decreaseItemAmount(item, 1);

        logItemBreak(item, "Broke down " + item.getName() + " into parts: " + parts);
    }
    // Takes a list of components (ItemPackets) and assembles them into a single composed item.
    // Each ItemPacket is consumed from inventory, and the composed item is added.
    // Can only add new items, cannot create a new item
    // If a new item needs to be created via composition, it must be created before then passed into this method
    public void composeItem(Item composedItem, ArrayList<ItemPacket> usedComponents) {
        if (composedItem == null || usedComponents == null || inventory == null) return;

        //Verify all components exist in inventory
        for (ItemPacket required : composedItem.getComposedOf()) {
            boolean found = false;
            for (ItemPacket used : usedComponents) {
                if (used.getItem().equals(required.getItem())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Missing required component so abort
                return;
            }
        }

        // Verify all used components exist in sufficient quantity in inventory
        for (ItemPacket ip : usedComponents) {
            Item component = ip.getItem();
            int have = inventory.getQuantity(component);
            if (have < ip.getQuantity()) {
                return;
            }
        }

        //Consume component quantities
        for (ItemPacket ip : usedComponents) {
            inventory.decreaseItemAmount(ip.getItem(), ip.getQuantity());
        }

        //Add the new composed item
        inventory.addItemAmount(composedItem, 1);

        //Log the composition
        if (inventory.logManager != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Composed new item '")
                    .append(composedItem.getName())
                    .append("' (Serial: ").append(composedItem.getSerialNum()).append(") using: ");

            for (ItemPacket ip : usedComponents) {
                sb.append("[").append(ip.getItem().getName())
                        .append(" x").append(ip.getQuantity()).append("], ");
            }

            inventory.logManager.createLog(
                    Log.LogType.ItemAdded,
                    1,
                    sb.toString(),
                    composedItem.getSerialNum()
            );
        }
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
    private void logItemBreak(Item item, String message) {
        if (inventory != null && inventory.logManager != null) {
            inventory.logManager.createLog(
                    Log.LogType.ItemBrokenDown,
                    0, //amount not relevant
                    message,
                    item.getSerialNum()
            );
        }
    }
    private void logComboCreated(Item item, String message) {
        if (inventory != null && inventory.logManager != null) {
            inventory.logManager.createLog(
                    Log.LogType.ItemComboCreated,
                    0, //amount not relevant
                    message,
                    item.getSerialNum()
            );
        }
    }
    public void editItemAmount(Item item, int newAmount) {
        if (item == null) return;

        int currentAmount = inventory.getQuantity(item);
        int difference = newAmount - currentAmount;

        if (difference > 0) {
            inventory.addItemAmount(item, difference);
            logUpdate(item, "Increased quantity of '" + item.getName() +
                    "' (Serial: " + item.getSerialNum() + ") by " + difference +
                    ". New total: " + newAmount);
        } else if (difference < 0) {
            inventory.decreaseItemAmount(item, Math.abs(difference));
            logUpdate(item, "Decreased quantity of '" + item.getName() +
                    "' (Serial: " + item.getSerialNum() + ") by " + Math.abs(difference) +
                    ". New total: " + newAmount);
        }
    }

    public void updateItemName(Item item, String newName) {
        if (item != null && !Objects.equals(item.getName(), newName)) {
            item.setName(newName);
            logUpdate(item, "Item name updated to '" + newName + "'");
        }
    }

    public void updateLowStockTrigger(Item item, Integer newTrigger) {
        if (item != null && !Objects.equals(item.getLowStockTrigger(), newTrigger)) {
            item.setLowStockTrigger(newTrigger);
            logUpdate(item, "Low stock trigger updated to " + newTrigger);
        }
    }

    public void updateAmazonSKU(Item item, String newSKU) {
        if (item != null && !Objects.equals(item.getAmazonSellerSKU(), newSKU)) {
            item.setAmazonSellerSKU(newSKU);
            logUpdate(item, "Amazon SKU updated to '" + newSKU + "'");
        }
    }

    public void updateEbaySKU(Item item, String newSKU) {
        if (item != null && !Objects.equals(item.getEbaySellerSKU(), newSKU)) {
            item.setEbaySellerSKU(newSKU);
            logUpdate(item, "eBay SKU updated to '" + newSKU + "'");
        }
    }

    public void updateWalmartSKU(Item item, String newSKU) {
        if (item != null && !Objects.equals(item.getWalmartSellerSKU(), newSKU)) {
            item.setWalmartSellerSKU(newSKU);
            logUpdate(item, "Walmart SKU updated to '" + newSKU + "'");
        }
    }

    public void updateIconPath(Item item, String newPath) {
        if (item != null && !Objects.equals(item.getIconPath(), newPath)) {
            item.setIconPath(newPath);
            logUpdate(item, "Icon path updated to '" + newPath + "'");
        }
    }

    public void setComposedOf(Item target, ArrayList<ItemPacket> newComposedOf) {
        if (target == null) return;

        // Remove all old reverse links
        for (ItemPacket old : target.getComposedOf()) {
            Item component = old.getItem();
            component.getComposesInto().removeIf(ci -> ci.equals(target));
        }

        // Replace entire composedOf list
        target.replaceComposedOf(newComposedOf);

        // Rebuild dependencies
        target.syncCompositionDependencies();

        // Log the change
        StringBuilder sb = new StringBuilder();
        sb.append("Rebuilt composedOf for '")
                .append(target.getName())
                .append("' (Serial: ").append(target.getSerialNum()).append("): ");

        for (ItemPacket ip : newComposedOf) {
            sb.append("[").append(ip.getItem().getName())
                    .append(" x").append(ip.getQuantity()).append("], ");
        }

        logUpdate(target, sb.toString());
    }
    //-------------------------------</Methods>-------------------------------
}
