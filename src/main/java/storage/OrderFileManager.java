package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import constants.Constants;
import platform.BaseSeller;
import platform.PlatformManager;
import platform.PlatformType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final PlatformManager platformManager;

    private boolean loading;

    Gson gson;

    public OrderFileManager(PlatformManager platformSellerManager) {
        this.platformManager = platformSellerManager;

        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new Constants.LocalDateTimeAdapter())
                .create();

        try {
            Path newDir = Path.of(dataDir);
            Files.createDirectories(newDir);
        } catch (IOException e) {
            System.out.println("Error creating directory: " + e.getMessage());
        }

        loadOrders();
    }

    public void loadOrders() {
        loading = true;
        for (PlatformType platform : PlatformType.values()) {
            Path filePath = platformToFilePath(platform);

            try {
                if (!Files.exists(filePath)) {
                    System.out.println("[INFO] No saved orders file for " + platform.getDisplayName());
                    continue;
                }

                try (FileReader reader = new FileReader(filePath.toFile())) {
                    SellerData data = gson.fromJson(reader, SellerData.class);

                    if (data != null) {
                        BaseSeller<?> seller = platformManager.getSeller(platform);

                        seller.lastGetOrderTime = data.lastGetOrderTime();
                        seller.firstGetOrderTime = data.firstGetOrderTime();

                        if (data.allOrders() != null && !data.allOrders().isEmpty()) {
                            platformManager.allOrders.put(platform, new ConcurrentHashMap<>(data.allOrders()));
                            System.out.println("[SUCCESS] Loaded " + data.allOrders().size() +
                                    " orders for " + platform.getDisplayName());
                        } else {
                            platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
                        }
                    } else {
                        System.out.println("[WARN] Failed to parse orders file for " + platform.getDisplayName());
                        platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
                    }
                    System.out.println("Loaded Orders from: " + platformToFilePath(platform));
                }
            } catch (FileNotFoundException e) {
                System.out.println("[INFO] Orders file not found for " + platform.getDisplayName() +
                        ". Starting with empty orders.");
                platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
            } catch (Exception e) {
                System.out.println("[ERROR] Could not load orders for " + platform.getDisplayName());
                System.out.println(e.getMessage());
                platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
            }
        }

        loading = false;
    }
    public void saveOrders() {
        if (loading) return;
        if(platformManager.anySellersFetching()) return;
        for(PlatformType p : PlatformType.values()){
            BaseSeller<?> seller = platformManager.getSeller(p);
            Path filePath = platformToFilePath(p);

            Map<String, BaseSeller.Order> platformOrders = platformManager.allOrders.get(p);


            SellerData data = new SellerData(
                    seller.lastGetOrderTime,
                    seller.firstGetOrderTime,
                    platformOrders
            );

            if(data.firstGetOrderTime == null){
                System.out.println("[DEBUG] Skipped platform "+ p.getDisplayName() + " since orders were never pulled.");
                continue;
            }


            try {
                String json = gson.toJson(data);
                Files.writeString(filePath, json, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Saved orders to file.");
            } catch (IOException e) {
                System.out.println("[OrderFileManger] ERROR: Could not save logs for "+ p.getDisplayName());
            }
        }
    }
    private Path platformToFilePath(PlatformType p){
        return switch (p){
            case AMAZON -> Path.of(amazonOrdersFilePath);
            case WALMART -> Path.of(walmartOrdersFilePath);
            case EBAY -> Path.of(ebayOrdersFilePath);
        };
    }

    public boolean isLoading() {
        return loading;
    }
    public record SellerData(
            @Expose
            LocalDateTime lastGetOrderTime,
            @Expose
            LocalDateTime firstGetOrderTime,
            @Expose
            Map<String, BaseSeller.Order> allOrders
    ) {}
}
