package core;
import constants.Constants;
import platform.BaseSeller;
import platform.PlatformType;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Inventory {

    //This inventory stores all the items that exist
    //If the item is not it stock then it is still in MainInventory with quantity 0
    //Item packets are sent by the ItemManager to here to update inventory

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

    //Reserved items are for orders only. Users will be notified that amount that is reserved
    // Map structure: Item -> Map of (OrderID -> Quantity Reserved)
    private final Map<Item, Map<String, Integer>> orderReservations = new ConcurrentHashMap<>();

    //Returns amountInStock - amountReserved
    public int getAvailableQuantity(Item item) {
        int actual = getQuantity(item);
        int reserved = getTotalReservedForItem(item);
        return actual - reserved;
    }

    public int getTotalReservedForItem(Item item) {
        Map<String, Integer> reservationsForItem = orderReservations.get(item);
        if (reservationsForItem == null) {
            return 0;
        }
        return reservationsForItem.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void reserveItemsForOrder(PlatformType platform, BaseSeller.Order order) {
        String orderId = order.getOrderId();
        for (BaseSeller.OrderPacket op : order.getItems()) {
            Item item = getItemByPlatformAndSKU(platform, op.sku());
            if (item != null) {
                orderReservations
                        .computeIfAbsent(item, k -> new ConcurrentHashMap<>())
                        .put(orderId, op.quantity());
            }
        }
    }

    public void releaseReservationForOrder(PlatformType platform, BaseSeller.Order order) {
        String orderId = order.getOrderId();
        for (BaseSeller.OrderPacket op : order.getItems()) {
            Item item = getItemByPlatformAndSKU(platform, op.sku());
            if (item != null) {
                Map<String, Integer> reservationsForItem = orderReservations.get(item);
                if (reservationsForItem != null) {
                    reservationsForItem.remove(orderId);
                    if (reservationsForItem.isEmpty()) {
                        orderReservations.remove(item);

                        orderReservations.computeIfPresent(item, (k, v) ->
                                v.isEmpty() ? null : v
                        );
                    }
                }
            }
        }
    }

    public Inventory(){}
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
            Map<Item,Integer> composedOf,
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
                composedOf == null ? new HashMap<>(): composedOf,
                iconPath,
                itemManager,
                amazonSellerSKU,
                ebaySellerSKU,
                walmartSellerSKU
        );

        registerItemMapping(newItem, amount);

        String quantityMessage = amount > 0 ? "Quantity : "+ amount : "";
        if (logManager != null) {
            logManager.createLog(Log.LogType.CreatedNewItem,
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
        if (SerialToItemMap.containsKey(item.getSerial())) {
            Item i = SerialToItemMap.get(item.getSerial());
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
        //Create new item with serialized components
        Item newItem = new Item(
                item.getName(),
                item.getSerial(),
                item.getLowStockTrigger(),
                item.getComposedOfSerialized(),
                item.getImagePath(),
                itemManager,
                item.getAmazonSellerSKU(),
                item.getEbaySellerSKU(),
                item.getWalmartSellerSKU(),
                true
        );

        registerItemMapping(newItem, amount);
    }
    //Edit the amount of an item
    public void addItemAmount(Item item, int amount){
        if( item == null){
            throw new IllegalStateException("ERROR: Null item called in addItemAmount()");
        }
        addItemAmountSilent(item,amount);
        logManager.createLog(Log.LogType.AddedItem,
                amount,
                "Added " + amount + " units of item '" + item.getName() +
                        "' (Serial: " + item.getSerial() + "). " +
                        "New quantity: " + MainInventory.get(item),
                item.getSerial()
        );
    }
    //Does the same without logs
    public void addItemAmountSilent(Item item, int amount){
        if( item == null){
            throw new IllegalStateException("ERROR: Null item called in addItemAmount()");
        }
        if (!hasItem(item)) {
            throw new IllegalStateException("ERROR: Item not found in addItemAmount: " + item.getName());
        }
        int quantity = MainInventory.getOrDefault(item, 0);
        MainInventory.put(item, quantity + amount);
    }

    public void decreaseItemAmount(Item item, int amount){
        if (!hasItem(item)) {
            throw new IllegalStateException("ERROR: Item not found in addItemAmount: " + item.getName());
        }

        int quantity = MainInventory.get(item);
        quantity = Math.max(0,quantity-amount);
        MainInventory.put(item, quantity);
        logManager.createLog(Log.LogType.ReducedStock,
                amount,
                "Removed " + amount + " units of item '" + item.getName() +
                        "' (Serial: " + item.getSerial() + "). " +
                        "New quantity: " + MainInventory.get(item),
                item.getSerial()
        );
    }

    public void decreaseItemAmountSilent(Item item, int amount){
        if (!hasItem(item)) {
            throw new IllegalStateException("ERROR: Item not found in decreaseItemAmountSilent: " + item.getName());
        }

        int quantity = MainInventory.get(item);
        quantity = Math.max(0,quantity-amount);
        MainInventory.put(item, quantity);
    }
    public void setQuantity(Item item,int quantity){
        if (!hasItem(item)) {
            throw new IllegalStateException("ERROR: Item not found in setQuantity: " + item.getName());
        }
        if(quantity < 0) return;
        MainInventory.put(item, quantity);
    }

    public void processItemMap(Map<Item, Integer> items) {
        for(Map.Entry<Item,Integer> e : items.entrySet()){
            processItemPacket(new ItemPacket(e.getKey(),e.getValue()));
        }
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

    public Item getItemBySerial(String serial) {
        return SerialToItemMap.get(serial);
    }
    //Get current quantity of an item
    public int getQuantity(Item item){
        return MainInventory.getOrDefault(item, 0);
    }

    //Check if an item exists in inventory
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasItem(Item item){
        return MainInventory.containsKey(item);
    }
    //Only for duplicate checks since this is o(n) no 0(1)
    public List<Item> getItemByName(String name) {
        List<Item> returnList = new ArrayList<>();
        if (name == null || name.isEmpty()) return returnList;
        for (Item item : MainInventory.keySet()) {
            if (item == null) {
                System.err.println("ERROR: : null item key found in MainInventory!");
                continue;
            }
            if (name.equalsIgnoreCase(item.getName())) {
                returnList.add(item);
            }
        }
        return returnList;
    }

    public void composeItem(Item item, int amount) {
        if (item == null || !item.isComposite() || amount <= 0) {
            throw new RuntimeException("ERROR: ComposeItem called on non-existent item, or non-composite Item, or amount is invalid");
        }
        for(Map.Entry<Item,Integer> ip: item.getComposedOf().entrySet()){ //Check if we have enough of each part
            Item i = ip.getKey();
            int ipAmount = ip.getValue();
            long required = (long)ipAmount * (long)amount;
            if(required > getQuantity(i)){
                throw new RuntimeException("ERROR: Not enough "+ i.getName() + " to compose item: "+ item.getName() +". \n" +
                        "(Amount needed = "+ipAmount*amount + " || Amount available = "+getQuantity(i) +")");
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
                    .append("' (Serial: ").append(item.getSerial()).append(") using: \n");

            for (Map.Entry<Item,Integer> ip : item.getComposedOf().entrySet()) {
                sb.append("[").append(ip.getKey().getName())
                        .append(" x").append(ip.getValue()).append("], \n");
            }

           logManager.createLog(
                    Log.LogType.ComposedItem,
                    1,
                    sb.toString(),
                    item.getSerial()
            );
        }
    }
    public void breakDownItem(Item item, Map<Item,Integer> used) {
        if (item == null || !item.isComposite()) {
            throw new RuntimeException("ERROR: breakDownItem() called on non-existent item, or non-composite Item.");
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
        for (Map.Entry<Item,Integer> IP : result.remained().entrySet()) {
            reclaimedSB.append(IP.getKey().getName())
                    .append(" x")
                    .append(IP.getValue())
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

        logManager.createLog(Log.LogType.BrokenDownItem,1, logMessage,item.getSerial());
    }
    //Rarely used to delete an item completely from inventory
    public void removeItem(Item item){
        removeItemSilent(item);

        logManager.createLog(
                Log.LogType.DeletedItem,
                0,
                "Deleted item '" + item.getName() +
                        "' (Serial: " + item.getSerial() + ") from inventory and all associated logs.",
                item.getSerial()
        );
    }

    //Checks if for any X, if X has a composite part that contains X anywhere then it returns false.
    public boolean containsItemRecursively(Item item, String searchSerial) {
        if (item.getComposedOf() == null) return false;

        for (Map.Entry<Item,Integer> packet : item.getComposedOf().entrySet()) {
            Item component = packet.getKey();
            if (component.getSerial().equals(searchSerial)) {
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

            other.getComposedOf().remove(item);
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
                System.out.println("ERROR: Could not delete image files for item: " + item.getName());
            }
        }
        notifyListeners(item);
    }


    //Ensures all  hashmaps are always in sync
    public void registerItemMapping(Item item, int amount){
        if (item == null || amount < 0) {
            throw new RuntimeException("ERROR: registerItemMapping called on null item or negative quantity");
        }

        MainInventory.put(item, amount);

        if (item.getSerial() != null && !item.getSerial().isEmpty())
            SerialToItemMap.put(item.getSerial(), item);

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

        if (item.getSerial() != null && !item.getSerial().isEmpty())
            SerialToItemMap.remove(item.getSerial());
        if (item.getAmazonSellerSKU() != null && !item.getAmazonSellerSKU().isEmpty())
            AmazonSKUToItemMap.remove(item.getAmazonSellerSKU());
        if (item.getEbaySellerSKU() != null && !item.getEbaySellerSKU().isEmpty())
            EbaySKUToItemMap.remove(item.getEbaySellerSKU());
        if (item.getWalmartSellerSKU() != null && !item.getWalmartSellerSKU().isEmpty())
            WalmartSKUToItemMap.remove(item.getWalmartSellerSKU());
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
                            i.getName() + " (" + i.getSerial() + ") out of stock!" + lowStockReminder,
                            i.getSerial()
                    );
                } else {
                    outOfStockLog.setMessage(
                            i.getName() + " (" + i.getSerial() + ") out of stock!" + lowStockReminder
                    );
                }
            }
            //Low stock
            else if (currentQuantity <= trigger) {
                if (lowStockLog == null) {
                    logManager.createLog(
                            Log.LogType.LowStock,
                            currentQuantity,
                            i.getName() + " (" + i.getSerial() + ") is low on stock!" + lowStockReminder,
                            i.getSerial()
                    );
                } else {
                    lowStockLog.setMessage(
                            i.getName() + " (" + i.getSerial() + ") is still low on stock!" + lowStockReminder
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
    public Item getItemByPlatformAndSKU(PlatformType p, String SKU){
        return switch (p){
            case EBAY -> getItemByEbaySKU(SKU);
            case AMAZON -> getItemByAmazonSKU(SKU);
            case WALMART -> getItemByWalmartSKU(SKU);
        };
    }
    //If an item is not in stock, but we need it, find composite items that could be broken down to compose fulfill neededItem
    //Only for orders
    //Each map is a solution where the item is the one to be broken down and the int is the amount that needs to be broken down.
    public List<Map<Item, Integer>> possibleBreakDownsForItem(Item neededItem, int amountNeeded){
        if(isItemInStockRecursive(neededItem, amountNeeded)){
            return new ArrayList<>();
        }
        List<Map<Item, Integer>> solutions = new ArrayList<>();

        // Step 1, what are we missing?
        Map<Item, Integer> missing = getMissingItemsRecursive(neededItem, amountNeeded);

        //Check if all missing items can actually be made
        for(Map.Entry<Item,Integer> e : missing.entrySet()){
            if(e.getKey().getComposedOf() == null || e.getKey().getComposesInto().isEmpty()){
                System.out.println("There are no possible available breakdowns to get item:" + e.getKey().getName());
                return solutions;
            }
        }

        //Step 2, find all composite items that produce what we are missing
        Set<Item> compositeItemsWeCareAbout = new HashSet<>();

        for (Item missingItem : missing.keySet()) {
            compositeItemsWeCareAbout.addAll(missingItem.getComposesInto());
        }

        List<Item> compositeList = new ArrayList<>(compositeItemsWeCareAbout);

        //Step 3, solve
        int MAX_SOLUTIONS = 5;
        int MAX_COMBINATION_SIZE = 10;
        outerLoop:
        for(int i = 1; i <= MAX_COMBINATION_SIZE; i++) {
            List<List<Item>> combos = getAllPossibleCombinations(compositeList, i);

            //Check if any list has composite items that can map to every missing
            for (List<Item> combo : combos) {

                // Step 3 Compute base parts produced by the combo
                Map<Item, Integer> producedTotals = new HashMap<>();

                for (Item composite : combo) {
                    Map<Item, Integer> flat = getBaseComposition(composite, 1);
                    mergeInto(producedTotals, flat);
                }
                //Check if the combo covers ALL missing items
                boolean coversAll = true;
                for (Map.Entry<Item, Integer> need : missing.entrySet()) {
                    int have = producedTotals.getOrDefault(need.getKey(), 0);
                    if (have < need.getValue()) {
                        coversAll = false;
                        break;
                    }
                }
                if (!coversAll) continue;

                //Count how many of each composite item the combo uses
                Map<Item, Integer> counted = new HashMap<>();
                for (Item c : combo) {
                    counted.merge(c, 1, Integer::sum);
                }
                //Make sure all these items are in stock
                boolean allInStock = true;
                for (Map.Entry<Item, Integer> entry : counted.entrySet()) {
                    if (getAvailableQuantity(entry.getKey()) < entry.getValue()) {
                        allInStock = false;
                        break;
                    }
                }
                if (!allInStock) continue;


                //IF a solution is 1 of Item A, then don't add item A and ITem b
                boolean isSuperset = false;
                for (Map<Item, Integer> existing : solutions) {
                    boolean containsAll = true;
                    for (Map.Entry<Item, Integer> entry : existing.entrySet()) {
                        if (counted.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                            containsAll = false;
                            break;
                        }
                    }
                    if (containsAll && counted.size() >= existing.size()) {
                        isSuperset = true;
                        break;
                    }
                }

                if (!isSuperset) {
                    solutions.add(counted);
                    if(solutions.size() >= MAX_SOLUTIONS) break outerLoop;
                }
            }
        }

        return solutions;
    }
    //Give all possible Combinations of a list,
    //If list = 1,2,3,4 and amount is 2, then it will return
    // 11, 12, 13, 14, 22, 23, 24, 33, 34, 44 without duplicates
    <T> List<List<T>> getAllPossibleCombinations(List<T> list, int length) {
        List<List<T>> result = new ArrayList<>();
        getCombination(list, length, 0, new ArrayList<>(), result);
        return result;
    }
    private <T> void getCombination(List<T> list,
                              int length,
                              int start,
                              List<T> current,
                              List<List<T>> result) {
        if (length == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            getCombination(list, length - 1, i, current, result);
            current.remove(current.size() - 1);
        }
    }
    //Returns the composition of items in from elementary parts
    //A is composed of 2C and 1 B and each B is composed of 3 D and each. So getBaseComposition of A is 2 C, 3 D.
    // So getBaseComposition of (1A) = 2C, 3D
    private transient final Map<Item, Map<Item, Integer>> baseCompositionCache = new HashMap<>(); //Cache items we already broke down into parts, but only for 1
    public Map<Item, Integer> getBaseComposition(Item item, int amount) {
        Map<Item, Integer> baseUnit = baseCompositionCache.get(item);
        if (baseUnit == null) {
            baseUnit = new HashMap<>();

            if (!item.isComposite()) {
                baseUnit.put(item, 1);
            } else {
                for (Map.Entry<Item, Integer> component : item.getComposedOf().entrySet()) {
                    Item subItem = component.getKey();
                    int componentAmount = component.getValue();

                    Map<Item, Integer> subMap = getBaseComposition(subItem, componentAmount);
                    mergeInto(baseUnit, subMap);
                }
            }

            baseCompositionCache.put(item, baseUnit);
        }
        if (amount == 1) {
            return new HashMap<>(baseUnit);
        }

        Map<Item, Integer> scaled = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : baseUnit.entrySet()) {
            scaled.put(entry.getKey(), entry.getValue() * amount);
        }
        return scaled;
    }

    public void mergeInto(Map<Item, Integer> target, Map<Item, Integer> source) {
        for (Map.Entry<Item, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }
    // Recursively checks stock availability for an item and its subcomponents.
    public Map<Item, Integer> getAmountToReduceStockRecursive(Item item, int qty) {
        if (!isItemInStockRecursive(item, qty)) {
            System.out.println("ERROR : getAmountToReduceStockRecursive called when item is not in stock recursively");
            return new HashMap<>(); //Return empty, but this should not happen
        }

        Map<Item, Integer> itemsToShip = new HashMap<>();

        int availableDirectly = getAvailableQuantity(item);
        int amountToReduceDirectly = Math.min(qty, availableDirectly);

        // Try to reduce stock directly if available
        if (amountToReduceDirectly > 0) {
            decreaseItemAmount(item, amountToReduceDirectly);
            itemsToShip.put(item, amountToReduceDirectly);
        }

        int remainingQtyToBuild = qty - amountToReduceDirectly;

        if (remainingQtyToBuild > 0) {
            // Try to fulfill using subcomponents
            for (Map.Entry<Item, Integer> entry : item.getComposedOf().entrySet()) {
                Item componentItem = entry.getKey();
                int neededQty = remainingQtyToBuild * entry.getValue();

                Map<Item, Integer> partsUsed =
                        getAmountToReduceStockRecursive(componentItem, neededQty);

                mergeInto(itemsToShip, partsUsed);
            }
        }

        return itemsToShip;
    }
    //If an item is not in stock, what are we missing?
    //Only returns non-composite items that can be used to build
    public Map<Item, Integer> getMissingItemsRecursive(Item item, int qty) {
        if (isItemInStockRecursive(item, qty)) {
            System.out.println("ERROR : getMissingItemsRecursive called when item is in stock recursively");
            return new HashMap<>();
        }

        Map<Item, Integer> missing = new HashMap<>();
        int available = getAvailableQuantity(item);

        //We simply don't have enough, base case
        if (!item.isComposite()) {
            int shortage = qty - available;
            if (shortage > 0) {
                missing.put(item, shortage);
            }
            return missing;
        }

        int shortage = qty - available;
        if (shortage <= 0) {
            return missing;
        }

        for (Map.Entry<Item, Integer> entry : item.getComposedOf().entrySet()) {
            Item component = entry.getKey();
            int componentQtyNeeded = shortage * entry.getValue();

            if (isItemInStockRecursive(component, componentQtyNeeded)) { //If in stock, great!
                continue;
            }

            Map<Item, Integer> componentMissing =
                    getMissingItemsRecursive(component, componentQtyNeeded); //Get what's missing

            mergeInto(missing, componentMissing);
        }

        return missing;
    }
    // Recursively checks stock availability for an item and its subcomponents.
    public boolean isItemInStockRecursive(Item item, int amountNeeded) {
        int inStock = getAvailableQuantity(item);

        if (!item.isComposite()) {
            return inStock >= amountNeeded;
        }

        if (inStock >= amountNeeded) {
            return true;
        }

        int shortage = amountNeeded - inStock;

        for (Map.Entry<Item, Integer> entry : item.getComposedOf().entrySet()) {
            Item component = entry.getKey();
            int componentNeeded = entry.getValue() * shortage;

            if (!isItemInStockRecursive(component, componentNeeded)) {
                return false;
            }
        }

        return true;
    }
    public void addChangeListener(ItemListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(Item i) {
        for (ItemListener listener : listeners) {
            listener.onChange(i);
        }
    }

    public void convertComposedSerialToItem() throws Exception {
        for(Item i : MainInventory.keySet()){
            Map<Item,Integer> composedOf = new HashMap<>();
            for(Map.Entry<String,Integer> serializedComposedOf : i.getComposedOfSerialized().entrySet()){
                String serial = serializedComposedOf.getKey();
                int amount = serializedComposedOf.getValue();
                Item baseItem = getItemBySerial(serial);

                if(baseItem == null){
                    Exception err =
                            new Exception("FATAL ERROR: Item " + i.getName() + " (" + i.getSerial() + ") is composed of non-existent item.");
                    throw new Exception(err);
                }

                composedOf.put(baseItem,amount);
            }
            i.replaceComposedOf(composedOf);
        }
    }


    @FunctionalInterface
    public interface ItemListener {
        void onChange(Item item);
    }
}
