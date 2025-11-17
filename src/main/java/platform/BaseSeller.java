package platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import storage.APIFileManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseSeller<T extends BaseSeller.Order> {

    protected final PlatformType platformType;
    protected final PlatformManager platformManager;
    protected final APIFileManager apiFileManager;

    public LocalDateTime lastAccessTokenGetTime;
    public LocalDateTime lastGetOrderTime;
    public int tokenExpirationTimeMinutes;
    public volatile List<T> lastFetchedOrders = new ArrayList<>();

    public boolean fetchingOrders = false;

    public String accessToken;

    Gson gson;

    public BaseSeller(PlatformType platformType, PlatformManager platformManager, APIFileManager apiFileManager) {
        this.platformType = platformType;
        this.platformManager = platformManager;
        this.apiFileManager = apiFileManager;

        this.lastGetOrderTime = LocalDateTime.now();
        this.lastAccessTokenGetTime = LocalDateTime.of(1990, 1, 1, 0, 0);



        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();

        this.tokenExpirationTimeMinutes = switch (platformType) {
            case AMAZON -> 55;
            case EBAY -> 110;
            case WALMART -> 13;
        };
    }

    public enum OrderStatus {
        CONFIRMED, //Order confirmed, ready to ship. Log but don't touch inventory
        SHIPPED, //Order has been shipped, reduce stock
        CANCELLED, //Order cancelled, delete the order
    }
    // Every seller fetches its own orders with apis
    public abstract void fetchOrders();

    protected String getString(JsonObject obj, String name) {
        if (!obj.has(name)) {
            System.out.println("JSON missing key: " + name);
            return null;
        }

        if (obj.get(name).isJsonNull()) {
            System.out.println("JSON key '" + name + "' is NULL");
            return null;
        }

        return obj.get(name).getAsString();

    }

    //Each platform has orders but each order is different
    public static abstract class Order {
        private final String orderId;
        private OrderStatus status;
        private final List<OrderPacket> items;

        public Order(String orderId, OrderStatus status) {
            this.orderId = orderId;
            this.status = status;
            this.items = new ArrayList<>();
        }
        public String getOrderId() {return orderId;}

        public OrderStatus getStatus() {return status;}

        public void setStatus(OrderStatus status) {this.status = status;}

        public List<OrderPacket> getItems() {return Collections.unmodifiableList(items);}

        protected void addItem(String sku, int quantity) {items.add(new OrderPacket(sku, quantity));}
        protected void addItem(OrderPacket op) {items.add(op);}
    }

        //Represents a single item in an order.
        public record OrderPacket(String sku, int quantity) {
    }

    protected void log(String msg) {
        System.out.println("[" + this.getClass().getName() + "] " + msg);
    }
}
