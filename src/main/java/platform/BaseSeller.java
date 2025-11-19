package platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import constants.Constants;
import storage.APIFileManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class BaseSeller<T extends BaseSeller.Order> {

    protected final PlatformType platformType;
    protected final PlatformManager platformManager;
    protected final APIFileManager apiFileManager;

    public LocalDateTime lastAccessTokenGetTime;

    public LocalDateTime lastGetOrderTime;
    public LocalDateTime firstGetOrderTime;

    public int tokenExpirationTimeMinutes;

    public volatile List<T> lastFetchedOrders = new ArrayList<>();

    public boolean fetchingOrders = false;

    public String accessToken;

    public BaseSeller(PlatformType platformType, PlatformManager platformManager, APIFileManager apiFileManager) {
        this.platformType = platformType;
        this.platformManager = platformManager;
        this.apiFileManager = apiFileManager;

        this.lastAccessTokenGetTime = LocalDateTime.of(1990, 1, 1, 0, 0);

        this.tokenExpirationTimeMinutes = switch (platformType) {
            case AMAZON -> 55;
            case EBAY -> 110;
            case WALMART -> 13;
        };
    }


    // Every seller fetches its own orders with apis
    public abstract void fetchOrders();

    //Called when loaded from file
    public void setLastGetOrderTime(LocalDateTime time){
        lastGetOrderTime = time;
    }
    public LocalDateTime getLastGetOrderTimeForFetching(){

        if (firstGetOrderTime == null) {
            firstGetOrderTime = LocalDateTime.now();
            return firstGetOrderTime;
        }
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(14);

        LocalDateTime effectiveCutoff = firstGetOrderTime.isAfter(cutoffTime) ? firstGetOrderTime : cutoffTime;

        if (lastGetOrderTime != null && lastGetOrderTime.isAfter(effectiveCutoff)) {
            return lastGetOrderTime;
        }

        return effectiveCutoff;
    }

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

    public enum OrderStatus {
        CONFIRMED, //Order confirmed, ready to ship. Log but don't touch inventory
        SHIPPED, //Order has been shipped, reduce stock
        CANCELLED, //Order cancelled, delete the order
    }
    //Each platform has orders but each order is different
    public static class Order {
        @Expose
        private final String orderId;
        @Expose
        private OrderStatus status;
        @Expose
        private final List<OrderPacket> items;
        @Expose
        private LocalDateTime lastUpdated;

        public Order(String orderId, OrderStatus status,LocalDateTime lastUpdated) {
            this.orderId = orderId;
            this.status = status;
            this.items = new ArrayList<>();
            this.lastUpdated = lastUpdated;
        }
        public String getOrderId() {return orderId;}

        public OrderStatus getStatus() {return status;}

        public void setStatus(OrderStatus status) {this.status = status;}

        public List<OrderPacket> getItems() {return Collections.unmodifiableList(items);}

        public LocalDateTime getLastUpdated(){
            return lastUpdated;
        }
        public void setLastUpdated(LocalDateTime time){
            lastUpdated = time;
        }


        public void addItem(String sku, int quantity) {items.add(new OrderPacket(sku, quantity));}
        protected void addItem(OrderPacket op) {items.add(op);}
    }

        //Represents a single item in an order.
        public record OrderPacket(
                @Expose
                String sku,
                @Expose
                int quantity
        ) {
    }

    protected void log(String msg) {
        System.out.println("[" + this.getClass().getName() + "] " + msg);
    }

}
