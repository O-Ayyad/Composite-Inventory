package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import platform.PlatformManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//Stores ebay, amazon, and walmart orders in 3 separate files
public class OrderFileManager {

    private final String dataDir = new File("data" + File.separator +
            "orders").getAbsolutePath();

    private static final String EBAY_ORDERS_FILENAME = "ebay_orders.json";
    private static final String AMAZON_ORDERS_FILENAME = "amazon_orders.json";
    private static final String WALMART_ORDERS_FILENAME = "walmart_orders.json";

    private final String ebayOrdersFilePath = dataDir + File.separator + EBAY_ORDERS_FILENAME;
    private final String amazonOrdersFilePath = dataDir + File.separator + AMAZON_ORDERS_FILENAME;
    private final String walmartOrdersFilePath = dataDir + File.separator + WALMART_ORDERS_FILENAME;

    private final PlatformManager platformSellerManager;

    Gson gson;

    public OrderFileManager(PlatformManager platformSellerManager) {
        this.platformSellerManager = platformSellerManager;
        this.gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();

        try {
            Path newDir = Path.of(dataDir);
            Files.createDirectories(newDir);
        } catch (IOException e) {
            System.out.println("Error creating directory: " + e.getMessage());
        }

        loadOrders();
    }

    private void loadOrders() {

    }

    private void saveOrders() {

    }
}
