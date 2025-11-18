package platform;

import storage.APIFileManager;

import java.time.LocalDateTime;

public class EbaySeller extends BaseSeller<EbaySeller.EbayOrder> {
    public EbaySeller(PlatformManager manager, APIFileManager api) {
        super(PlatformType.AMAZON, manager,api);
        manager.ebaySeller = this;
    }
    @Override
    public void fetchOrders() {
        //Call ebay api, if bad response, regenerate key
        //Pull orders
        //Update last call time
        //Parse the new orders get the order ID, SKU and quantity of each SKU
        //Create the ebay Order into a list and only let Platform Seller call this method
        return;
    }
    public static class EbayOrder extends BaseSeller.Order {
        public EbayOrder(String orderId, BaseSeller.OrderStatus status, LocalDateTime dateTime) {
            super(orderId, status, dateTime);
        }
    }
}
