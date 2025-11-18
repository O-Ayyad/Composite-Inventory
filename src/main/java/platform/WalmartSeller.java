package platform;

import storage.APIFileManager;

import java.time.LocalDateTime;

public class WalmartSeller extends BaseSeller<WalmartSeller.WalmartOrder> {
    public WalmartSeller(PlatformManager manager, APIFileManager api) {
        super(PlatformType.AMAZON, manager,api);
        manager.walmartSeller = this;
    }
    @Override
    public void fetchOrders() {
        //Call walmart api, if bad response, regenerate key
        //Pull orders
        //Update last call time
        //Parse the new orders get the order ID, SKU and quantity of each SKU
        //Create the walmart Order into a list and only let Platform Seller call this method
        return;
    }
    public static class WalmartOrder extends BaseSeller.Order {
        public WalmartOrder(String orderId, BaseSeller.OrderStatus status, LocalDateTime dateTime) {
            super(orderId,status,dateTime);
        }
    }
}
