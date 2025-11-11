package platform;

import java.util.ArrayList;
import java.util.List;

public class EbaySeller extends BaseSeller<EbaySeller.EbayOrder> {
    public EbaySeller(PlatformSellerManager manager) {
        super(PlatformType.EBAY, manager);
        manager.ebaySeller = this;
    }
    @Override
    public List<EbayOrder> fetchOrders() {
        //Call ebay api, if bad response, regenerate key
        //Pull orders
        //Update last call time
        //Parse the new orders get the order ID, SKU and quantity of each SKU
        //Create the ebay Order into a list and only let Platform Seller call this method
        return null;
    }
    public static class EbayOrder extends BaseSeller.Order {
        public EbayOrder(String orderId, BaseSeller.OrderStatus status) {
            super(orderId,status);
        }
    }
}
