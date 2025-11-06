package platform;

import java.util.ArrayList;
import java.util.List;

public class EbaySeller extends BaseSeller {

    public EbaySeller(PlatformSellerManager platformManager) {
        super("eBay", platformManager);
    }

    @Override
    public List<PlatformOrder> fetchRecentOrders() {
        List<PlatformOrder> orders = new ArrayList<>();

        //Todo: get api call for new orders
        //New eBay order
        PlatformOrder order1 = new PlatformOrder("EBY-E001", "eBay", PlatformOrder.Status.NEW);
        order1.addOrderPacket("EBY-SKU-101", 1);
        order1.addOrderPacket("EBY-SKU-202", 3);

        //Shipped eBay order
        PlatformOrder order2 = new PlatformOrder("EBY-E002", "eBay", PlatformOrder.Status.SHIPPED);
        order2.addOrderPacket("EBY-SKU-303", 2);

        //Cancelled eBay order
        PlatformOrder order3 = new PlatformOrder("EBY-E003", "eBay", PlatformOrder.Status.CANCELLED);
        order3.addOrderPacket("EBY-SKU-404", 1);

        orders.add(order1);
        orders.add(order2);
        orders.add(order3);

        return orders;
    }
}
