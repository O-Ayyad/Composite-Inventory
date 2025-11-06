package platform;

import java.util.ArrayList;
import java.util.List;

public class WalmartSeller extends BaseSeller {

    public WalmartSeller(PlatformSellerManager platformManager) {
        super("Walmart", platformManager);
    }

    @Override
    public List<PlatformOrder> fetchRecentOrders() {
        List<PlatformOrder> orders = new ArrayList<>();

        //New Walmart order
        PlatformOrder order1 = new PlatformOrder("WMT-W001", "Walmart", PlatformOrder.Status.NEW);
        order1.addOrderPacket("WMT-SKU-500", 1);

        //Shipped Walmart order
        PlatformOrder order2 = new PlatformOrder("WMT-W002", "Walmart", PlatformOrder.Status.SHIPPED);
        order2.addOrderPacket("WMT-SKU-501", 2);

        //Cancelled Walmart order
        PlatformOrder order3 = new PlatformOrder("WMT-W003", "Walmart", PlatformOrder.Status.CANCELLED);
        order3.addOrderPacket("WMT-SKU-502", 1);

        orders.add(order1);
        orders.add(order2);
        orders.add(order3);

        return orders;
    }
}
