package platform;

import java.util.ArrayList;
import java.util.List;

public class PlatformOrder {

    public enum Status {
        NEW,// Just placed
        SHIPPED, // Fulfilled
        CANCELLED
    }


    public final String orderId;
    public final String platform; //Amazaon ebay etc
    public final List<OrderPacket> orderPackets = new ArrayList<>();
    public Status status;

    public PlatformOrder(String orderId, String platform, Status status) {
        this.orderId = orderId;
        this.platform = platform;
        this.status = status;
    }

    public void addOrderPacket(String sku, int quantity) {
        orderPackets.add(new OrderPacket(platform, sku, quantity));
    }


    //Item packet but sku platform and amount
    public static class OrderPacket {
        public final String platform;
        public final String sku;
        public final int quantity;

        public OrderPacket(String platform, String sku, int quantity) {
            this.platform = platform;
            this.sku = sku;
            this.quantity = quantity;
        }
    }
}