package platform;

import java.util.List;

public class AmazonSeller extends BaseSeller<AmazonSeller.AmazonOrder> {
    public AmazonSeller(PlatformSellerManager manager) {
        super(PlatformType.AMAZON, manager);
        manager.amazonSeller = this;
    }

    @Override
    public List<AmazonOrder> fetchOrders() {
        //Call amazon api, if bad response, regenerate key
        //Pull orders
        //Update last call time
        //Parse the new orders get the order ID, SKU and quantity of each SKU
        //Create the amazon Order into a list and only let Platform Seller call this method
        return null;
    }
    public static class AmazonOrder extends BaseSeller.Order {
        public AmazonOrder(String orderId, BaseSeller.OrderStatus status) {
            super(orderId,status);
        }
    }
}