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

        return orders;
    }
}
