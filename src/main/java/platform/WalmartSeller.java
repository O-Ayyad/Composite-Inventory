package platform;

import java.util.List;

public class WalmartSeller extends BaseSeller<WalmartSeller.WalmartOrder> {
    public WalmartSeller(PlatformSellerManager manager) {
        super(PlatformType.WALMART, manager);
        manager.walmartSeller = this;
    }
    @Override
    public List<WalmartOrder> fetchOrders() {
        //Call walmart api, if bad response, regenerate key
        //Pull orders
        //Update last call time
        //Parse the new orders get the order ID, SKU and quantity of each SKU
        //Create the walmart Order into a list and only let Platform Seller call this method
        return null;
    }
    public static class WalmartOrder extends BaseSeller.Order {
        public WalmartOrder(String orderId, BaseSeller.OrderStatus status) {
            super(orderId,status);
        }
    }
}
