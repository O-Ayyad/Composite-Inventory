package platform;

import java.util.ArrayList;
import java.util.List;

public class AmazonSeller extends BaseSeller {

    public AmazonSeller(PlatformSellerManager platformManager) {
        super("Amazon", platformManager);
    }

    @Override
    public List<PlatformOrder> fetchRecentOrders() {
        List<PlatformOrder> orders = new ArrayList<>();
        //Todo: Implement api callscd " to get new orders

        //New order - not shipped yet
        PlatformOrder order1 = new PlatformOrder("AMZ-A001", "Amazon", PlatformOrder.Status.NEW);
        order1.addOrderPacket("AMZ-SKU-123", 2);
        order1.addOrderPacket("AMZ-SKU-789", 1);

        //Shipped order
        PlatformOrder order2 = new PlatformOrder("AMZ-A002", "Amazon", PlatformOrder.Status.SHIPPED);
        order2.addOrderPacket("AMZ-SKU-456", 1);

        //Cancelled order
        PlatformOrder order3 = new PlatformOrder("AMZ-A003", "Amazon", PlatformOrder.Status.CANCELLED);
        order3.addOrderPacket("AMZ-SKU-999", 1);

        orders.add(order1);
        orders.add(order2);
        orders.add(order3);

        return orders;
    }
}