package platform;

import core.*;
import gui.MainWindow;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;


public class PlatformManager {
    private final Inventory inventory;
    private final LogManager logManager;
    public AmazonSeller amazonSeller;
    public EbaySeller ebaySeller;
    public WalmartSeller walmartSeller;

    MainWindow mainWindow;

    //Order ID lookup
    //A map where the key is the platform and the value is the hashmap of ID to order lookup
    private final Map<PlatformType, Map<String, BaseSeller.Order>> currentPlatformOrders =
            new EnumMap<>(PlatformType.class);

    // Map structure: Item -> Map of (OrderID -> Quantity Reserved)
    private final Map<Item, Map<String, Integer>> orderReservations = new HashMap<>();

    public PlatformManager(Inventory inv, LogManager lm) {
        inventory = inv;
        logManager = lm;

        for (PlatformType type : PlatformType.values()) {
            currentPlatformOrders.put(type, new HashMap<>());
        }

        pullOrdersFromFile();

    }
    //Returns amountInStock - amountReserved
    public int getAvailableQuantity(Item item) {
        int actual = inventory.getQuantity(item);
        int reserved = getTotalReservedForItem(item);
        return actual - reserved;
    }

    private int getTotalReservedForItem(Item item) {
        Map<String, Integer> reservationsForItem = orderReservations.get(item);
        if (reservationsForItem == null) {
            return 0;
        }
        return reservationsForItem.values().stream().mapToInt(Integer::intValue).sum();
    }

    private void reserveItemsForOrder(PlatformType platform, BaseSeller.Order order) {
        String orderId = order.getOrderId();
        for (BaseSeller.OrderPacket op : order.getItems()) {
            Item item = getItemByPlatformAndSKU(platform, op.sku());
            if (item != null) {
                orderReservations
                        .computeIfAbsent(item, k -> new HashMap<>())
                        .put(orderId, op.quantity());
            }
        }
    }

    private void releaseReservationForOrder(PlatformType platform, BaseSeller.Order order) {
        String orderId = order.getOrderId();
        for (BaseSeller.OrderPacket op : order.getItems()) {
            Item item = getItemByPlatformAndSKU(platform, op.sku());
            if (item != null) {
                Map<String, Integer> reservationsForItem = orderReservations.get(item);
                if (reservationsForItem != null) {
                    reservationsForItem.remove(orderId);
                    if (reservationsForItem.isEmpty()) {
                        orderReservations.remove(item);
                    }
                }
            }
        }
    }

    public void fetchAllRecentOrders() {

        System.out.println("Starting fetchAllRecentOrders()…");

        SwingWorker<Map<PlatformType, Map<String, BaseSeller.Order>>, Void> sellerWatcher
                = new SwingWorker<>() {

            Map<PlatformType, Map<String, BaseSeller.Order>> allOrders =
                    new EnumMap<>(PlatformType.class);

            private final EnumMap<PlatformType, List<? extends BaseSeller.Order>> oldOrders =
                    new EnumMap<>(PlatformType.class);

            private final EnumMap<PlatformType, List<? extends BaseSeller.Order>> newOrders =
                    new EnumMap<>(PlatformType.class);

            @Override
            protected Map<PlatformType, Map<String, BaseSeller.Order>> doInBackground() throws Exception {
                System.out.println("[Worker] Starting doInBackground");

                List<Exception> fetchExceptions = Collections.synchronizedList(new ArrayList<>());

                for (PlatformType platform : PlatformType.values()) {

                    BaseSeller seller = getSeller(platform);
                    List<BaseSeller.Order> oldList = seller.lastFetchedOrders;
                    System.out.println("[Worker] Preparing fetch for " + platform);

                    oldOrders.put(platform,
                            oldList != null
                                    ? new ArrayList<>(oldList)
                                    : new ArrayList<>());

                    CompletableFuture.runAsync(() -> {

                        System.out.println("[Async] Fetching orders for " + platform);

                        try {
                            seller.fetchOrders();
                        } catch (Exception e) {
                            fetchExceptions.add(e);
                            logManager.createLog(Log.LogType.SystemError, 0,
                                    "Error fetching orders from " + platform + ": " + e.getMessage(),
                                    "");
                            System.err.println("Error fetching " + platform + ": " + e.getMessage());
                        }
                    });
                }
                //wait until all sellers are done
                Thread.sleep(1000);
                int counter = 0;
                while (anySellersFetching()) {
                    //So we dont flood the console
                    counter++;
                    if(counter > 30){
                        System.out.println("[Worker] Still fetching...");
                        counter = 0;
                    }
                    Thread.sleep(200);
                }
                System.out.println("[Worker] All async fetches completed!");

                if (!fetchExceptions.isEmpty()) {
                    logManager.createLog(Log.LogType.SystemError, fetchExceptions.size(),
                            fetchExceptions.size() + " platform(s) failed to fetch orders",
                            "");
                }
                System.out.println("[Worker] Processing fetched results…");
                for (PlatformType platform : PlatformType.values()) {
                    BaseSeller seller = getSeller(platform);
                    List<BaseSeller.Order> list = seller.lastFetchedOrders;

                    newOrders.put(platform,
                            list != null
                                    ? new ArrayList<>(list)
                                    : new ArrayList<>());

                    System.out.println("[Worker] " + platform + " returned "

                            + newOrders.get(platform).size()
                            + " orders.");

                    Map<String, BaseSeller.Order> map = new HashMap<>();
                    for (BaseSeller.Order order : newOrders.get(platform)) {
                        map.put(order.getOrderId(), order);
                    }
                    allOrders.put(platform, map);
                }

                System.out.println("[Worker] Returning allOrders map with sizes: ");
                for (PlatformType p : PlatformType.values()) {
                    System.out.println("    " + p + ": " + allOrders.get(p).size() + " orders");
                }
                return allOrders;
            }

            protected void done() {
                System.out.println("[Worker] done() called");
                try {
                    Map<PlatformType, Map<String, BaseSeller.Order>> result = get();
                    handleFetchedOrders(result);
                } catch (Exception e) {
                    logManager.createLog(Log.LogType.SystemError, 0,
                            "Critical error in order fetch completion: " + e.getMessage(),
                            "");
                }
            }
        };
        System.out.println("Executing SwingWorker…");
        sellerWatcher.execute();
    }

    private void handleFetchedOrders(Map<PlatformType, Map<String, BaseSeller.Order>> allOrders) {
        System.out.println("Handling all fetched orders.");
        for (PlatformType platform : PlatformType.values()) {
            System.out.println("Handling "+platform.getDisplayName() + " orders");
            Map<String, BaseSeller.Order> newPlatformOrders = allOrders.get(platform);
            Map<String, BaseSeller.Order> currentOrders = currentPlatformOrders.get(platform);

            if (!currentOrders.equals(newPlatformOrders)) {
                processOrderMaps(platform, newPlatformOrders, currentOrders);
                currentOrders.clear();
                currentOrders.putAll(newPlatformOrders);
            }
        }
    }

    private void processOrderMaps(
            PlatformType platform,
            Map<String, BaseSeller.Order> newOrders,
            Map<String, BaseSeller.Order> oldOrders) {

        for (Map.Entry<String, BaseSeller.Order> entry : newOrders.entrySet()) {

            String id = entry.getKey();
            BaseSeller.Order newOrder = entry.getValue();
            BaseSeller.Order oldOrder = oldOrders.get(id);
            System.out.println("Processing " +platform.getDisplayName() + " order ID : " + id);

            if (oldOrder == null) { //Neworder has a brand-new order
                handleNewOrder(platform, newOrder);
            } else if (!oldOrder.equals(newOrder)) { //Order has changed
                handleOrderStatusChange(platform, oldOrder, newOrder);
            }
        }
    }

    private void handleNewOrder(PlatformType platform, BaseSeller.Order newOrder) {
        if(newOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED){
            return;
        }
        if(newOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED){
            if(getOrder(platform,newOrder.getOrderId()) != null){ //We already processed this order
                return;
            }else{
                BaseSeller.Order dummyOldOrder = createOrderWithStatus(newOrder, BaseSeller.OrderStatus.CONFIRMED);
                handleOrderStatusChange(platform, dummyOldOrder, newOrder);
            }
        }
        System.out.println("New order " +platform.getDisplayName() + " order ID : " + newOrder.getOrderId());

        if (newOrder.getStatus() == BaseSeller.OrderStatus.CONFIRMED) {
            List<BaseSeller.OrderPacket> soldItems = newOrder.getItems();

            boolean allInStock = true;
            boolean registered = true;
            boolean anyViaComposition = false;
            StringBuilder orderSummary = new StringBuilder();
            orderSummary.append("Order ").append(newOrder.getOrderId()).append(" received: \n");

            for (BaseSeller.OrderPacket op : soldItems) {
                String sku = op.sku();
                int quantitySold = op.quantity();
                Item itemSold = getItemByPlatformAndSKU(platform, sku);

                if (itemSold == null) {
                    registered = false;
                    orderSummary.append(quantitySold).append("x ").append(sku).append(" (NOT REGISTERED), \n");
                } else {
                    if (getAvailableQuantity(itemSold) >= quantitySold) {
                        orderSummary.append(quantitySold).append("x ").append(itemSold.getName()).append(", \n");
                    } else {
                        //Try via composition
                        int currentQuantity = getAvailableQuantity(itemSold);
                        int quantityNeeded = quantitySold - currentQuantity;

                        if (isItemInStockRecursive(itemSold, quantityNeeded)) {
                            anyViaComposition = true;
                            orderSummary.append(quantitySold).append("x ").append(itemSold.getName())
                                    .append(" (via composition), \n");
                        } else {
                            allInStock = false;
                            orderSummary.append(quantitySold).append("x ").append(itemSold.getName())
                                    .append(" (OUT OF STOCK), \n");
                        }
                    }
                }
            }

            if (orderSummary.length() >= 2 && orderSummary.substring(orderSummary.length() - 2).equals(", ")) {
                orderSummary.setLength(orderSummary.length() - 2);
            }

            if (allInStock || anyViaComposition) {
                reserveItemsForOrder(platform, newOrder);
            }

            Log.LogType type;

            if (!registered) {
                type = Log.LogType.OrderReceivedItemNotRegistered;
            } else if (!allInStock) {
                type = Log.LogType.OrderReceivedItemOutOfStock;
            } else if (anyViaComposition) {
                type = Log.LogType.OrderReceivedItemSellsViaComposition;
            } else {
                type = Log.LogType.OrderReceived;
            }

            logManager.createLog(type, soldItems.size(),
                    orderSummary.toString(), "");
        }
    }
    private void handleOrderStatusChange(PlatformType platform, BaseSeller.Order oldOrder, BaseSeller.Order newOrder) {
        if (newOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED) {
            releaseReservationForOrder(platform, newOrder);
            logManager.createLog(Log.LogType.OrderCancelled,
                    newOrder.getItems().size(),
                    "Order " + newOrder.getOrderId() + " cancelled. Reservations released.", "");
            return;
        }
        System.out.println("Existing order " + platform.getDisplayName() + " order ID : " + newOrder.getOrderId());
        //Shipped out, success
        if (newOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED && oldOrder.getStatus() == BaseSeller.OrderStatus.CONFIRMED) {
            List<BaseSeller.OrderPacket> soldItems = newOrder.getItems();

            boolean allItemsRegistered = true;
            for (BaseSeller.OrderPacket op : soldItems) {
                String sku = op.sku();
                Item itemSold = getItemByPlatformAndSKU(platform, sku);
                if (itemSold == null) {
                    allItemsRegistered = false;
                }
            }

            if (!allItemsRegistered) {
                logManager.createLog(Log.LogType.ItemShippedNotRegistered, soldItems.size(),
                        "Order " + newOrder.getOrderId() + " REJECTED - Contains unregistered items. No inventory changes made.", "");
                return; //Inventory not effected. This must be fixed manually
            }
            boolean allFulfilled = true;
            boolean anyViaComposition = false;
            StringBuilder orderSummary = new StringBuilder();
            orderSummary.append("Order ").append(newOrder.getOrderId()).append(" shipped:\n");


            for (BaseSeller.OrderPacket op : soldItems) {
                String sku = op.sku();
                int quantitySold = op.quantity();
                Item itemSold = getItemByPlatformAndSKU(platform, sku);

                if (inventory.getQuantity(itemSold) >= quantitySold) {
                    //Sell directly
                    inventory.decreaseItemAmount(itemSold, quantitySold);
                    orderSummary.append(quantitySold).append("x ").append(itemSold.getName()).append("\n");
                } else {
                    //Try to ship via composition
                    int currentQuantity = inventory.getQuantity(itemSold);
                    int quantityNeeded = quantitySold - currentQuantity;

                    if (isItemInStockRecursive(itemSold, quantityNeeded)) {
                        List<ItemPacket> partsNeeded = getAmountToReduceStockRecursive(itemSold, quantityNeeded);
                        StringBuilder packetResult = new StringBuilder();
                        packetResult.append(itemSold.getName()).append("x ").append(currentQuantity).append("\n---------------------------------\n");
                        for (ItemPacket p : partsNeeded) {
                            packetResult.append(p.getQuantity()).append("x ").append(p.getItem().getName()).append("\n");
                        }
                        anyViaComposition = true;
                        orderSummary.append(quantitySold).append("x ").append(itemSold.getName())
                                .append(" (via composition):\n").append(packetResult).append("\n");
                    } else {
                        allFulfilled = false;
                        orderSummary.append(quantitySold).append("x ").append(itemSold.getName())
                                .append(" (OUT OF STOCK)\n");
                    }
                }
            }

            releaseReservationForOrder(platform, newOrder);

            Log.LogType type;
            if (!allFulfilled) {
                type = Log.LogType.ItemShippedOutOfStock;
            } else if (anyViaComposition) {
                type = Log.LogType.ItemShippedViaComposition;
            } else {
                type = Log.LogType.ItemShipped;
            }

            logManager.createLog(type, soldItems.size(),
                    orderSummary.toString(), "");

            //Edge cases

            //Shipped order reverting to non-Shipped status
        } else if (oldOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED &&
                newOrder.getStatus() != BaseSeller.OrderStatus.SHIPPED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " reverted from SHIPPED status.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus(), "");
        }

        //Order un-cancelled
        else if (oldOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED &&
                newOrder.getStatus() != BaseSeller.OrderStatus.CANCELLED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " un-cancelled.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus(), "");
        }

        //Order shipped from not confirmed status
        else if (newOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED &&
                oldOrder.getStatus() != BaseSeller.OrderStatus.CONFIRMED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " shipped without confirmation.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus() +
                            "\nExpected: CONFIRMED → SHIPPED", "");
        }
        //Order cancelled from not confirmed status
        else if (newOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED &&
                oldOrder.getStatus() != BaseSeller.OrderStatus.CONFIRMED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " cancelled from unexpected status.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus() +
                            "\nExpected: CONFIRMED → CANCELLED", "");
        }
    }
    public void pullOrdersFromFile(){

    }
    // Recursively checks stock availability for an item and its subcomponents.
    private List<ItemPacket> getAmountToReduceStockRecursive(Item item, int qty) {
        if(!isItemInStockRecursive(item, qty)){
            System.out.println("ERROR : getAmountToReduceStockRecursive called when item is not in stock recursively");
            return new ArrayList<>(); //Return empty, but this should not happen
        }
        List<ItemPacket> itemsToShip = new ArrayList<>();

        int availableDirectly = inventory.getQuantity(item);
        int amountToReduceDirectly = Math.min(qty, availableDirectly);

        // Try to reduce stock directly if available
        if (amountToReduceDirectly > 0) {
            inventory.decreaseItemAmount(item, amountToReduceDirectly);
            itemsToShip.add(new ItemPacket(item, amountToReduceDirectly));
        }
        int remainingQtyToBuild = qty - amountToReduceDirectly;
        if (remainingQtyToBuild > 0) {
            // Try to fulfill using subcomponents
            for (ItemPacket p : item.getComposedOf()) {
                Item componentItem = p.getItem();
                int neededQty = remainingQtyToBuild * p.getQuantity();
                List<ItemPacket> partsUsed = getAmountToReduceStockRecursive(componentItem, neededQty);

                itemsToShip.addAll(partsUsed);
            }
        }

        return itemsToShip;
    }
    // Recursively checks stock availability for an item and its subcomponents.
    private boolean isItemInStockRecursive(Item item, int amountNeeded) {
        int inStock = inventory.getQuantity(item);
        if (!item.isComposite()) {
            return inStock >= amountNeeded;
        }
        if (inStock >= amountNeeded) {
            return true;
        }
        int shortage = amountNeeded - inStock;

        for (ItemPacket p : item.getComposedOf()) {
            Item component = p.getItem();
            int componentNeeded = p.getQuantity() * shortage;

            if (!isItemInStockRecursive(component, componentNeeded)) {
                return false;
            }
        }
        return true;
    }
    public BaseSeller.Order getOrder(PlatformType platform, String id){
        Map<String, BaseSeller.Order> orders = currentPlatformOrders.get(platform);
        return orders == null ? null : orders.get(id);
    }
    public List<BaseSeller.Order> getAllOrdersFromPlatform(PlatformType platform) {
        Map<String, BaseSeller.Order> orders = currentPlatformOrders.get(platform);
        if (orders == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(orders.values());
    }
    public void putOrder(PlatformType platform, BaseSeller.Order order) {
        currentPlatformOrders.get(platform).put(order.getOrderId(), order);
    }
    public void putOrderList(PlatformType platform, List<BaseSeller.Order> orderList) {
        if (orderList == null) return;
        Map<String, BaseSeller.Order> idToOrderMap =
                currentPlatformOrders.computeIfAbsent(platform, p -> new HashMap<>());
        idToOrderMap.clear();
        for (BaseSeller.Order order : orderList) {
            if (order != null) {
                idToOrderMap.put(order.getOrderId(), order);
            }
        }
    }
    public BaseSeller getSeller(PlatformType p){
        return switch (p){
            case EBAY -> ebaySeller;
            case AMAZON -> amazonSeller;
            case WALMART -> walmartSeller;
        };
    }
    public Item getItemByPlatformAndSKU(PlatformType p, String SKU){
        return switch (p){
                case EBAY -> inventory.getItemByEbaySKU(SKU);
                case AMAZON -> inventory.getItemByAmazonSKU(SKU);
                case WALMART -> inventory.getItemByWalmartSKU(SKU);
        };
    }
    public void setMainWindow(MainWindow minWin){
        mainWindow = minWin;
    }
    public MainWindow getMainWindow(){
        return mainWindow;
    }
    private boolean anySellersFetching(){
        for(PlatformType p : PlatformType.values()){
            if(getSeller(p).fetchingOrders){
                return true;
            }
        }
        return false;
    }
    //Only for new orders that have already been shipped
    private BaseSeller.Order createOrderWithStatus(BaseSeller.Order order, BaseSeller.OrderStatus status) {
        order.setStatus(status);
        return order;
    }
}