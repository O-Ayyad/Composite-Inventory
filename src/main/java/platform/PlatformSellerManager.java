package platform;

import core.*;
import java.util.*;
import java.util.stream.Stream;


public class PlatformSellerManager {
    private final Inventory inventory;
    private final LogManager logManager;
    public AmazonSeller amazonSeller;
    public EbaySeller ebaySeller;
    public WalmartSeller walmartSeller;


    //Order ID lookup
    //A map where the key is the platform and the value is the hashmap of ID to order lookup
    private final Map<PlatformType, Map<String, BaseSeller.Order>> platformOrders =
            new EnumMap<>(PlatformType.class);

    public PlatformSellerManager(Inventory inv, LogManager lm){
        inventory = inv;
        logManager = lm;

        for (PlatformType type : PlatformType.values()) {
            platformOrders.put(type, new HashMap<>());
        }

        pullOrdersFromFile();

    }
    public void fetchAllRecentOrders(){

    }
    public void pullOrdersFromFile(){

    }
    public BaseSeller.Order getOrder(PlatformType platform, String id){
        Map<String, BaseSeller.Order> orders = platformOrders.get(platform);
        return orders == null ? null : orders.get(id);
    }
    public void putOrder(PlatformType platform, BaseSeller.Order order) {
        platformOrders.get(platform).put(order.getOrderId(), order);
    }
}