package platform;

import core.*;
import gui.MainWindow;
import storage.APIFileManager;
import storage.FileManager;
import javax.swing.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


public class PlatformManager {
    private final Inventory inventory;
    private final LogManager logManager;

    private FileManager fileManager;

    public AmazonSeller amazonSeller;
    public EbaySeller ebaySeller;
    public WalmartSeller walmartSeller;

    private ZonedDateTime lastFetchTime;
    public final int fetchTimeCooldownSeconds= 30;

    private final APIFileManager apiFileManager;

    private boolean fetching;

    MainWindow mainWindow;

    //Order ID lookup
    //A map where the key is the platform and the value is the hashmap of ID to order lookup
    public Map<PlatformType, Map<String, BaseSeller.Order>> allOrders =
            new ConcurrentHashMap<>();

    final Object fetchLock = new Object();
    public PlatformManager(Inventory inv, LogManager lm, APIFileManager api) {
        inventory = inv;
        logManager = lm;
        apiFileManager = api;
        for (PlatformType type : PlatformType.values()) {
            allOrders.put(type, new ConcurrentHashMap<>());
        }

        // Initialize sellers BEFORE loading orders
        amazonSeller = new AmazonSeller(this, apiFileManager);
        ebaySeller = new EbaySeller(this, apiFileManager);
        walmartSeller = new WalmartSeller(this, apiFileManager);

        lastFetchTime = ZonedDateTime.ofInstant(
                Instant.parse("1990-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );

    }

    public void setFileManager(FileManager fm){
        this.fileManager = fm;
    }
    public void fetchAllRecentOrders() {
        boolean anyConnected = false;
        for(PlatformType p : PlatformType.values()){
            try{
                if(apiFileManager.hasToken(p)){
                    anyConnected = true;
                }
            }catch (Exception e){
                throw new AssertionError(e);
            }
        }
        if(!anyConnected){
            return; //Exit silently
        }

        apiFileManager.checkBadKeys(new ArrayList<>() {{
            add(amazonSeller);
            add(ebaySeller);
            add(walmartSeller);
        }});

        if(ZonedDateTime.now().minusSeconds(fetchTimeCooldownSeconds).isBefore(lastFetchTime)){
            System.out.println("Fetching orders is on cooldown.");
            return;
        }

        if(fetching || anySellersFetching()){
            System.out.println("Already fetching orders, skipped fetchAllRecentOrders for now.");
            return;
        }

        System.out.println("Starting fetchAllRecentOrders()...");

        SwingWorker<Map<PlatformType, Map<String, BaseSeller.Order>>, Void> sellerWatcher
                = new SwingWorker<>() {

            final Map<PlatformType, Map<String, BaseSeller.Order>> allOrdersLocal =
                    new EnumMap<>(PlatformType.class);

            private final EnumMap<PlatformType, List<BaseSeller.Order>> newOrders =
                    new EnumMap<>(PlatformType.class);

            @Override
            protected Map<PlatformType, Map<String, BaseSeller.Order>> doInBackground() throws Exception {
                fetching = true;

                System.out.println("[Worker] Starting doInBackground");

                List<Exception> fetchExceptions = Collections.synchronizedList(new ArrayList<>());

                for (PlatformType platform : PlatformType.values()) {

                    final BaseSeller seller = getSeller(platform);
                    final PlatformType currentPlatform = platform;

                    System.out.println("[Worker] Preparing fetch for " + platform);

                    CompletableFuture.runAsync(() -> {

                        System.out.println("[Async] Fetching orders for " + currentPlatform);

                        try {
                            seller.fetchOrders();
                        } catch (Exception e) {
                            seller.fetchingOrders = false;
                            System.out.println("Error with seller " + platform.getDisplayName()+ " " + Arrays.toString(e.getStackTrace()));
                            fetchExceptions.add(e);
                        }
                    });
                }

                //wait until all sellers are done
                Thread.sleep(3000);

                synchronized(fetchLock) {
                    while (anySellersFetching()) {
                        fetchLock.wait(5000); // Wake up periodically to log
                        System.out.println("[Worker] Still fetching...");
                    }
                }
                synchronized(fetchLock) {
                    fetchLock.notifyAll(); // Wake up waiting thread
                }
                System.out.println("[Worker] All async fetches completed!");
                fetching = false;

                if (!fetchExceptions.isEmpty()) {
                    logManager.createLog(Log.LogType.SystemError, fetchExceptions.size(),
                            fetchExceptions.size() + " platform(s) failed to fetch orders",
                            "");
                }
                System.out.println("[Worker] Processing fetched results...");

                for (PlatformType platform : PlatformType.values()) {

                    //Get orders from the seller
                    BaseSeller seller = getSeller(platform);
                    List<BaseSeller.Order> fetchedOrderlist =
                            seller.lastFetchedOrders != null
                                    ? seller.lastFetchedOrders :
                                        new ArrayList<>();

                    System.out.println("[Worker] " + platform + " returned "
                            + fetchedOrderlist.size()
                            + " orders.");

                    newOrders.put(platform, fetchedOrderlist);

                    Map<String, BaseSeller.Order> existingMap = new HashMap<>(allOrders.get(platform));

                    for (BaseSeller.Order order : newOrders.get(platform)) {
                        existingMap.put(order.getOrderId(), order);
                    }
                    allOrdersLocal.put(platform, existingMap);
                }

                System.out.println("[Worker] Returning allOrders map with sizes: ");
                for (PlatformType p : PlatformType.values()) {
                    System.out.println("    " + p + ": " + allOrdersLocal.get(p).size() + " orders");
                }
                return allOrdersLocal;
            }

            protected void done() {
                System.out.println("[Worker] done() called");
                try {
                    Map<PlatformType, Map<String, BaseSeller.Order>> result = get();
                    handleFetchedOrders(result);
                    lastFetchTime = ZonedDateTime.now();
                    fetching = false;
                    saveToFile();
                } catch (Exception e) {

                    logManager.createLog(Log.LogType.SystemError, 0,
                            "Critical error in order fetch completion: " + e.getMessage(),
                            "");

                    lastFetchTime = ZonedDateTime.now();
                    fetching = false;

                    System.out.println(Arrays.toString(e.getStackTrace()));
                }
            }
        };
        System.out.println("Executing SwingWorker...");
        sellerWatcher.execute();
    }

    private void handleFetchedOrders(Map<PlatformType, Map<String, BaseSeller.Order>> allFetchedOrders) {
        System.out.println("Handling all fetched orders.");
        for (PlatformType platform : PlatformType.values()) {
            System.out.println("Handling "+platform.getDisplayName() + " orders");
            Map<String, BaseSeller.Order> newPlatformOrders = allFetchedOrders.get(platform);

            processOrderMaps(platform, newPlatformOrders, allOrders.get(platform));
        }
    }

    private void processOrderMaps(
            PlatformType platform,
            final Map<String, BaseSeller.Order> newOrders,
            final Map<String, BaseSeller.Order> allOrders) {

        for (Map.Entry<String, BaseSeller.Order> entry : newOrders.entrySet()) {

            String id = entry.getKey();
            BaseSeller.Order newOrder = entry.getValue();
            BaseSeller.Order oldOrder = allOrders.get(id);
            System.out.println("Processing " +platform.getDisplayName() + " order ID : " + id);

            if (oldOrder == null) { //Neworder has a brand-new order
                handleNewOrder(platform, newOrder);
            } else if (!oldOrder.equals(newOrder)) { //Order has changed
                handleOrderStatusChange(platform, oldOrder, newOrder);
            }
        }
        allOrders.putAll(newOrders);
    }
    private void handleNewOrder(PlatformType platform, BaseSeller.Order newOrder) {
        if(newOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED){ //Dont care
            return;
        }
        if(newOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED){
            if(getOrder(platform,newOrder.getOrderId()) == null){ //We have never processed this order
                BaseSeller.Order dummyOldOrder = createDummyConfirmedOrder(newOrder);
                handleOrderStatusChange(platform, dummyOldOrder, newOrder);
            }

            //Order shipped and is saved
            return;
        }
        System.out.println("New order " +platform.getDisplayName() + " order ID : " + newOrder.getOrderId());

        if (newOrder.getStatus() == BaseSeller.OrderStatus.CONFIRMED) {
            List<BaseSeller.OrderPacket> soldItems = newOrder.getItems();

            boolean allInStock = true;
            boolean registered = true;
            boolean anyViaComposition = false;
            StringBuilder orderSummary = new StringBuilder();
            StringBuilder breakdownSuggestions = new StringBuilder();
            orderSummary.append(platform.getDisplayName()).append("Order ").append(newOrder.getOrderId()).append(" received: \n");

            for (BaseSeller.OrderPacket op : soldItems) {
                String sku = op.sku();
                int quantitySold = op.quantity();
                Item itemSold = inventory.getItemByPlatformAndSKU(platform, sku);

                if (itemSold == null) {
                    registered = false;
                    orderSummary.append(quantitySold).append("x ").append(sku).append(" (NOT REGISTERED), \n");
                } else {
                    if (inventory.getAvailableQuantity(itemSold) >= quantitySold) {
                        orderSummary.append(quantitySold).append("x ").append(itemSold.getName()).append(", \n");
                    } else {
                        //Try via composition
                        int currentQuantity = inventory.getAvailableQuantity(itemSold);
                        int quantityNeeded = quantitySold - currentQuantity;

                        if (inventory.isItemInStockRecursive(itemSold, quantityNeeded)) {
                            anyViaComposition = true;
                            orderSummary.append(quantitySold).append("x ").append(itemSold.getName())
                                    .append(" (via composition), \n");
                        } else {
                            allInStock = false;
                            orderSummary.append(quantitySold).append("x ").append(itemSold.getName())
                                    .append(" (OUT OF STOCK), \n");
                            List<Map<Item, Integer>> breakdowns = inventory.possibleBreakDownsForItem(itemSold, quantityNeeded);

                            if (!breakdowns.isEmpty()) {
                                if (!breakdownSuggestions.isEmpty()) {
                                    breakdownSuggestions.append("\n\nThis can be solved by breaking down the following items:\n");
                                }
                                breakdownSuggestions.append("For ").append(quantityNeeded).append("x ").append(itemSold.getName()).append(":\n");
                                for (int i = 0; i < Math.min(breakdowns.size(), 3); i++) {
                                    breakdownSuggestions.append("  Option ").append(i + 1).append(": ");
                                    Map<Item, Integer> breakdown = breakdowns.get(i);
                                    boolean first = true;
                                    for (Map.Entry<Item, Integer> entry : breakdown.entrySet()) {
                                        if (!first) breakdownSuggestions.append(", ");
                                        breakdownSuggestions.append(entry.getValue()).append("x ").append(entry.getKey().getName());
                                        first = false;
                                    }
                                    breakdownSuggestions.append("\n");
                                }
                            }else{
                                breakdownSuggestions.append("\n\nNo viable combination of breakdowns could be found to fulfill this order");
                            }
                        }
                    }
                }
            }

            if (orderSummary.length() >= 2 && orderSummary.substring(orderSummary.length() - 2).equals(", ")) {
                orderSummary.setLength(orderSummary.length() - 2);
            }
            orderSummary.append(breakdownSuggestions);
            if (allInStock || anyViaComposition) {
                inventory.reserveItemsForOrder(platform, newOrder);
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
            inventory.releaseReservationForOrder(platform, newOrder);
            logManager.createLog(Log.LogType.OrderCancelled,
                    newOrder.getItems().size(),
                    "Order " + newOrder.getOrderId() + " cancelled. Reservations released.", "");
            return;
        }
        System.out.println("Existing order " + platform.getDisplayName() + " order ID : " + newOrder.getOrderId());

        //Shipped out normally
        if (newOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED && oldOrder.getStatus() == BaseSeller.OrderStatus.CONFIRMED) {
            List<BaseSeller.OrderPacket> soldItems = newOrder.getItems();

            boolean allItemsRegistered = true;
            for (BaseSeller.OrderPacket op : soldItems) {
                String sku = op.sku();
                Item itemSold = inventory.getItemByPlatformAndSKU(platform, sku);
                if (itemSold == null) {
                    allItemsRegistered = false;
                }
            }

            if (!allItemsRegistered) {
                StringBuilder sb = new StringBuilder();
                sb.append(platform.getDisplayName()).append("Order ").append(newOrder.getOrderId()).append(" REJECTED - Contains unregistered items. No inventory changes made for all items.");
                for (BaseSeller.OrderPacket op : soldItems) {
                    String sku = op.sku();
                    int amountSold = op.quantity();
                    Item i = inventory.getItemByPlatformAndSKU(platform,sku);
                    if(i == null){
                        sb.append(amountSold).append("x ").append(sku).append(" (NOT REGISTERED), \n");
                    }else{
                        sb.append(amountSold).append("x ").append(sku).append("  (").append(i.getName()).append(")").append("  Amount in stock: ").append(inventory.getQuantity(i));
                    }

                }
                logManager.createLog(Log.LogType.ItemShippedNotRegistered, soldItems.size(),
                        sb.toString(), "");
                return; //Inventory not effected. This must be fixed manually
            }
            boolean allFulfilled = true;
            boolean anyViaComposition = false;
            StringBuilder orderSummary = new StringBuilder();
            StringBuilder breakdownSuggestions = new StringBuilder();
            orderSummary.append(platform.getDisplayName()).append("Order ").append(newOrder.getOrderId()).append(" shipped:\n");


            for (BaseSeller.OrderPacket op : soldItems) {

                String sku = op.sku();
                int quantitySold = op.quantity();
                Item itemSold = inventory.getItemByPlatformAndSKU(platform, sku);

                int available = inventory.getAvailableQuantity(itemSold);

                //In stock
                if (available >= quantitySold) {
                    inventory.decreaseItemAmount(itemSold, quantitySold);
                    orderSummary.append(quantitySold).append("x ").append(itemSold.getName()).append("\n");
                    continue;
                }

                //Attempt composition
                int quantityNeeded = quantitySold - available;

                if (inventory.isItemInStockRecursive(itemSold, quantityNeeded)) {

                    Map<Item, Integer> partsNeeded =
                            inventory.getAmountToReduceStockRecursive(itemSold, quantityNeeded);

                    anyViaComposition = true;

                    StringBuilder packetResult = new StringBuilder();
                    packetResult.append(itemSold.getName())
                            .append("x ").append(available)
                            .append("\n---------------------------------\n");

                    for (Map.Entry<Item, Integer> p : partsNeeded.entrySet()) {
                        packetResult.append(p.getValue())
                                .append("x ")
                                .append(p.getKey().getName())
                                .append("\n");
                    }

                    orderSummary.append(quantitySold)
                            .append("x ")
                            .append(itemSold.getName())
                            .append(" (via composition):\n")
                            .append(packetResult)
                            .append("\n");

                    continue;
                }

                //Out of stock attempt breakdown suggestions
                allFulfilled = false;

                orderSummary.append(quantitySold)
                        .append("x ")
                        .append(itemSold.getName())
                        .append(" (OUT OF STOCK)\n");

                List<Map<Item, Integer>> breakdowns =
                        inventory.possibleBreakDownsForItem(itemSold, quantityNeeded);

                if (!breakdowns.isEmpty()) {

                    breakdownSuggestions.append("\n\nThis can be solved by breaking down the following items:\n");

                    breakdownSuggestions.append("For ")
                            .append(quantityNeeded)
                            .append("x ")
                            .append(itemSold.getName())
                            .append(":\n");

                    for (int i = 0; i < Math.min(breakdowns.size(), 3); i++) {
                        breakdownSuggestions.append("  Option ")
                                .append(i + 1)
                                .append(": ");

                        Map<Item, Integer> breakdown = breakdowns.get(i);

                        boolean first = true;
                        for (Map.Entry<Item, Integer> entry : breakdown.entrySet()) {
                            if (!first) breakdownSuggestions.append(", ");
                            breakdownSuggestions.append(entry.getValue())
                                    .append("x ")
                                    .append(entry.getKey().getName());
                            first = false;
                        }
                        breakdownSuggestions.append("\n");
                    }
                    continue;
                }

                //Out of stock + no compositions + no breakdowns
                breakdownSuggestions.append("\n\nNo viable combination of breakdowns could be found to fulfill this order.");
            }

            //Build log message
            if (orderSummary.length() >= 2 && orderSummary.substring(orderSummary.length() - 2).equals(", ")) {
                orderSummary.setLength(orderSummary.length() - 2);
            }
            orderSummary.append(breakdownSuggestions);
            inventory.releaseReservationForOrder(platform, newOrder);

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
        }

        //Edge cases

        //Shipped order reverting to non-Shipped status
        else if (oldOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED &&
                newOrder.getStatus() != BaseSeller.OrderStatus.SHIPPED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    platform.getDisplayName() + "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " reverted from SHIPPED status.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus(), "");
        }

        //Order un-cancelled
        else if (oldOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED &&
                newOrder.getStatus() != BaseSeller.OrderStatus.CANCELLED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    platform.getDisplayName() +  "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " un-cancelled.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus(), "");
        }

        //Order shipped from not confirmed status
        else if (newOrder.getStatus() == BaseSeller.OrderStatus.SHIPPED &&
                oldOrder.getStatus() != BaseSeller.OrderStatus.CONFIRMED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    platform.getDisplayName() +  "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " shipped without confirmation.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus() +
                            "\nExpected: CONFIRMED → SHIPPED", "");
        }
        //Order cancelled from not confirmed status
        else if (newOrder.getStatus() == BaseSeller.OrderStatus.CANCELLED &&
                oldOrder.getStatus() != BaseSeller.OrderStatus.CONFIRMED) {
            logManager.createLog(Log.LogType.SystemError, 0,
                    platform.getDisplayName() +  "Order " + oldOrder.getOrderId() + " on " + platform.getDisplayName() + " cancelled from unexpected status.\n" +
                            "Old status: " + oldOrder.getStatus() + " → New status: " + newOrder.getStatus() +
                            "\nExpected: CONFIRMED → CANCELLED", "");
        }
    }
    public void saveToFile(){
        fileManager.saveAll(true);
    }
    public BaseSeller.Order getOrder(PlatformType platform, String id){
        Map<String, BaseSeller.Order> orders = allOrders.get(platform);
        return orders == null ? null : orders.get(id);
    }
    public BaseSeller getSeller(PlatformType p){
        return switch (p){
            case EBAY -> ebaySeller;
            case AMAZON -> amazonSeller;
            case WALMART -> walmartSeller;
        };
    }
    public void setMainWindow(MainWindow minWin){
        mainWindow = minWin;
    }
    public MainWindow getMainWindow(){
        return mainWindow;
    }
    boolean anySellersFetching(){
        for(PlatformType p : PlatformType.values()){
            if(getSeller(p).fetchingOrders){
                return true;
            }
        }
        return false;
    }
    //Only for new orders that have already been shipped
    private BaseSeller.Order createDummyConfirmedOrder(BaseSeller.Order order) {
        return new BaseSeller.Order(order.getOrderId(), BaseSeller.OrderStatus.CONFIRMED, order.getLastUpdated());
    }
    public ZonedDateTime getLastFetchTime(){
        return lastFetchTime;
    }
    public boolean isFetching(){
        return fetching || anySellersFetching();
    }
    public boolean onCooldown(){
        return ZonedDateTime.now().minusSeconds(fetchTimeCooldownSeconds)
                .isBefore(lastFetchTime);
    }

    public List<String> getAllUnlinkedItems() {
        List<String> list = new ArrayList<>();
        for(PlatformType p : PlatformType.values()){
            HttpURLConnection conn = null;
            if(!apiFileManager.hasToken(p))continue;
            try{
                switch (p){
                    case AMAZON -> {

                    }
                    case EBAY -> {

                    }
                    case WALMART -> {
                        URL url = new URL("https://marketplace.walmartapis.com/v3/items");
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");

                        String[] creds = apiFileManager.getCredentialsFromFile(PlatformType.WALMART);
                        String clientID = creds[0];
                        String clientSecret = creds[1];
                        String accessToken = apiFileManager.getWalmartAccessToken(clientID,clientSecret);
                        if(accessToken == null){
                            list.add("\n\nCould not fetch inventory for walmart\n\n");
                            break;
                        }
                        apiFileManager.addWalmartHeaders(conn,accessToken, clientID);
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(10000);
                        int response = conn.getResponseCode();
                        String responseMessage = conn.getResponseMessage();
                        if(response > 300 || response <200){
                            list.add("\n\nCould not fetch inventory for walmart\n\n");
                            break;
                        }
                    }
                }
            }catch (Exception e){

            }
        }
        return list;
    }
}