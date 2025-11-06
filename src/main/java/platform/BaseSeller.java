package platform;

import java.util.List;

public abstract class BaseSeller {
    protected final String platformName;

    protected final PlatformSellerManager platformManager;

    public BaseSeller(String platformName, PlatformSellerManager platformManager) {
        this.platformName = platformName;
        this.platformManager = platformManager;
    }

    // Every seller fetches its own orders with apis
    public abstract List<PlatformOrder> fetchRecentOrders();

    //After fetching send to the manager
    public void syncOrders() {
        List<PlatformOrder> orders = fetchRecentOrders();
        if (orders == null || orders.isEmpty()) return;
        platformManager.syncOrders(orders);
    }
}
