package platform;


import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import storage.APIFileManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class BaseSeller {

    protected final PlatformType platformType;
    protected final PlatformManager platformManager;
    protected final APIFileManager apiFileManager;

    public ZonedDateTime  lastAccessTokenGetTime;

    public ZonedDateTime  lastGetOrderTime;
    public ZonedDateTime  firstGetOrderTime;

    public int tokenExpirationTimeMinutes;

    public volatile List<Order> lastFetchedOrders = new ArrayList<>();

    public boolean fetchingOrders = false;

    public String accessToken;

    public int keyFailCounter = 0;


    public BaseSeller(PlatformType platformType, PlatformManager platformManager, APIFileManager apiFileManager) {
        this.platformType = platformType;
        this.platformManager = platformManager;
        this.apiFileManager = apiFileManager;

        this.lastAccessTokenGetTime = ZonedDateTime.of(1990, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        this.tokenExpirationTimeMinutes = switch (platformType) {
            case AMAZON -> 55;
            case EBAY -> 110;
            case WALMART -> 13;
        };
    }


    // Every seller fetches its own orders with apis
    public abstract void fetchOrders();

    public synchronized ZonedDateTime getLastGetOrderTimeForFetching(){

        if (firstGetOrderTime == null) {
            firstGetOrderTime = ZonedDateTime.now(ZoneOffset.UTC);
            return firstGetOrderTime;
        }

        ZonedDateTime cutoffTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(14);

        ZonedDateTime effectiveCutoff = firstGetOrderTime.isAfter(cutoffTime) ? firstGetOrderTime : cutoffTime;

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
        private ZonedDateTime lastUpdated;

        public Order(String orderId, OrderStatus status,ZonedDateTime lastUpdated) {
            this.orderId = orderId;
            this.status = status;
            this.items = new ArrayList<>();
            this.lastUpdated = lastUpdated;
        }
        public String getOrderId() {return orderId;}

        public OrderStatus getStatus() {return status;}

        public void setStatus(OrderStatus status) {this.status = status;}

        public List<OrderPacket> getItems() {return Collections.unmodifiableList(items);}

        public ZonedDateTime getLastUpdated(){
            return lastUpdated;
        }
        public void setLastUpdated(ZonedDateTime time){
            lastUpdated = time;
        }

        protected void addItem(OrderPacket op) {items.add(op);}
    }

        //Represents a single item in an order.
        public record OrderPacket(
                @Expose
                String sku,
                @Expose
                int quantity
        ) { }

    protected void log(String msg) {
        System.out.println("[" + this.getClass().getName() + "] " + msg);
    }

    public boolean isBadKey() {
        return keyFailCounter >= 2;
    }
    public void resetBadKeyCount() {
        keyFailCounter = 0;
    }
}
