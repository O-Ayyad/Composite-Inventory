package platform;

import java.util.List;

public interface OrderListener {
    void onNewOrders(String platformName, List<PlatformOrder> orders);
}