package platform;

import core.*;
import java.util.*;


public class PlatformSellerManager {

    private final Inventory inventory;
    private final LogManager logManager;
    private final Map<String, PlatformOrder> trackedOrders = new HashMap<>(); // last known state

    public PlatformSellerManager(Inventory inventory, LogManager logManager) {
        this.inventory = inventory;
        this.logManager = logManager;
    }

    // Called by each seller after fetching current orders from its API. */
    public void syncOrders(List<PlatformOrder> fetchedOrders) {
        for (PlatformOrder current : fetchedOrders) {
            PlatformOrder existing = trackedOrders.get(current.orderId);

            //New order detected
            if (existing == null && current.status == PlatformOrder.Status.NEW) {
                handleNewOrder(current);
                trackedOrders.put(current.orderId, current);
            }

            //Status change to shipped
            else if (existing != null &&
                    existing.status == PlatformOrder.Status.NEW &&
                    current.status == PlatformOrder.Status.SHIPPED) {
                handleShipment(current);
                trackedOrders.put(current.orderId, current);
            }

            // Cancelled orders - cleanup
            else if (current.status == PlatformOrder.Status.CANCELLED) {
                handleCancellation(current);
                trackedOrders.remove(current.orderId);
            }
        }
    }

    //Creates logs for new incoming orders and checks stock.
    private void handleNewOrder(PlatformOrder order) {

        for (PlatformOrder.OrderPacket packet : order.orderPackets) {
            Item item = getItemByPlatform(packet.platform, packet.sku);

            if (item == null) {
                logManager.createLog(Log.LogType.ItemSoldAndNotListedOnPlatforms,
                        packet.quantity,
                        "Order " + order.orderId + " on " + packet.platform +
                                " - SKU " + packet.sku + " not linked.",
                        packet.sku);
                continue;
            }
            boolean inStock = isItemInStockRecursive(item, packet.quantity);
            logManager.createLog(
                    inStock ? Log.LogType.ItemSoldOnPlatform : Log.LogType.ItemSoldAndOutOfStock,
                    packet.quantity,
                    "Order " + order.orderId + " on " + packet.platform +
                            " for " + item.getName() + (inStock ? "." : " but item is out of stock."),
                    item.getSerialNum()
            );
        }
    }

    // When a platform marks an order as shipped, reduce stock and create logs.
    private void handleShipment(PlatformOrder order) {
        for (PlatformOrder.OrderPacket packet : order.orderPackets) {
            Item item = getItemByPlatform(packet.platform, packet.sku);
            if (item == null) continue;

            boolean reduced = tryReduceStock(item, packet.quantity);

            logManager.createLog(
                    reduced ? Log.LogType.ItemShippedOnPlatform : Log.LogType.ItemSoldAndOutOfStock,
                    packet.quantity,
                    reduced
                            ? "Order " + order.orderId + " shipped on " + packet.platform +
                            " — reduced stock for " + item.getName() + "."
                            : "Shipment " + order.orderId + " on " + packet.platform +
                            " failed — insufficient stock for " + item.getName() + ".",
                    item.getSerialNum());
        }
    }

    private void handleCancellation(PlatformOrder order) {
        for (PlatformOrder.OrderPacket packet : order.orderPackets) {
            logManager.createLog(
                    Log.LogType.OrderCancelled,
                    packet.quantity,
                    "Order " + order.orderId + " on " + packet.platform +
                            " was cancelled. SKU: " + packet.sku,
                    packet.sku
            );
        }
    }

    //Maps SKU lookup to the correct platform
    private Item getItemByPlatform(String platform, String sku) {
        return switch (platform) {
            case "Amazon" -> inventory.getItemByAmazonSKU(sku);
            case "eBay" -> inventory.getItemByEbaySKU(sku);
            case "Walmart" -> inventory.getItemByWalmartSKU(sku);
            default -> null;
        };
    }

    // Tries to reduce the item directly or from composed items recursively if needed.
    private boolean tryReduceStock(Item item, int qty) {
        int available = inventory.getQuantity(item);
        if (available >= qty) {
            inventory.decreaseItemAmount(item, qty);
            return true;
        }
        for (ItemPacket p : item.getComposedOf()) {
            if (tryReduceStock(p.getItem(), qty)) return true;
        }
        return false;
    }

    // Recursively checks stock availability for an item and its subcomponents. 
    private boolean isItemInStockRecursive(Item item, int qty) {
        if (inventory.getQuantity(item) >= qty) return true;
        for (ItemPacket p : item.getComposedOf()) {
            if (isItemInStockRecursive(p.getItem(), qty)) return true;
        }
        return false;
    }
}
