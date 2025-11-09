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

        return orders;
    }
}