package storage;


import com.google.gson.annotations.Expose;
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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Stores ebay, amazon, and walmart orders in 3 separate files
public class OrderFileManager extends AbstractFileManager{


    public static final String EBAY_ORDERS_FILENAME = "ebay_orders.json";
    public static final String AMAZON_ORDERS_FILENAME = "amazon_orders.json";
    public static final String WALMART_ORDERS_FILENAME = "walmart_orders.json";

    public final String ebayOrdersFilePath = dataDir + File.separator + EBAY_ORDERS_FILENAME;
    public final String amazonOrdersFilePath = dataDir + File.separator + AMAZON_ORDERS_FILENAME;
    public final String walmartOrdersFilePath = dataDir + File.separator + WALMART_ORDERS_FILENAME;

    public final PlatformManager platformManager;


    public OrderFileManager(PlatformManager platformSellerManager, String dataDirName){
        super(dataDirName);
        this.platformManager = platformSellerManager;

        this.gson = zonedGson;
    }
    @Override
    public LoadResult load(boolean firstOpen) {
        loading = true;
        boolean allSuccess = true;
        Exception ex = null;

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
                        BaseSeller seller = platformManager.getSeller(platform);

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
                        showError("ERROR: Failed to parse orders file for " + platform.getDisplayName(),firstOpen);
                        allSuccess = false;
                        platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
                    }
                    System.out.println("Loaded Orders from: " + platformToFilePath(platform));
                }
            } catch (FileNotFoundException e) {
                showError("ERROR:  Orders file not found for " + platform.getDisplayName() +" " + e.getMessage() + "\n " +
                                "Load a working backup" +
                                "Starting with empty orders.",firstOpen);
                platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
                allSuccess = false;
                ex = e;
            } catch (Exception e) {
                showError("ERROR:  Could not load orders for " + platform.getDisplayName() +" " + e.getMessage() + "\n " +
                        "Load a working backup",firstOpen);
                System.out.println(Arrays.toString(e.getStackTrace()));
                platformManager.allOrders.put(platform, new ConcurrentHashMap<>());
                allSuccess = false;
                ex = e;
            }
        }
        loading = false;
        return new LoadResult(allSuccess, ex);
    }
    @Override
    public void save() {
        if (loading) return;
        if(platformManager.isFetching()) return;
        for(PlatformType p : PlatformType.values()){
            BaseSeller seller = platformManager.getSeller(p);
            Path filePath = platformToFilePath(p);

            Map<String, BaseSeller.Order> platformOrders = platformManager.allOrders.get(p);

            SellerData data = new SellerData(
                    seller.lastGetOrderTime,
                    seller.firstGetOrderTime,
                    platformOrders
            );

            if(data.firstGetOrderTime == null){
                continue;
            }

            try {
                String json = gson.toJson(data);
                Files.writeString(filePath, json, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.out.println("[OrderFileManger] ERROR: Could not save orders for "+ p.getDisplayName());
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
    
    public record SellerData(
            @Expose
            ZonedDateTime lastGetOrderTime,
            @Expose
            ZonedDateTime firstGetOrderTime,
            @Expose
            Map<String, BaseSeller.Order> allOrders
    ) {}
}
