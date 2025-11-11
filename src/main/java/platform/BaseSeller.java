package platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseSeller<T extends BaseSeller.Order> {

    protected final PlatformType platformType;
    protected final PlatformSellerManager platformManager;

    public BaseSeller(PlatformType platformType, PlatformSellerManager platformManager) {
        this.platformType = platformType;
        this.platformManager = platformManager;
    }

    public enum OrderStatus {
        CONFIRMED, //Order confirmed, ready to ship. Log but don't touch inventory
        SHIPPED, //Order has been shipped, reduce stock
        CANCELLED, //Order cancelled, delete the order
    }
    // Every seller fetches its own orders with apis
    public abstract List<T> fetchOrders();

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
    }

        //Represents a single item in an order.
        public record OrderPacket(String sku, int quantity) {
    }
}
