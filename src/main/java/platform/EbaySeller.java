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

        return orders;
    }
}
