package platform;

import com.google.gson.*;
import storage.APIFileManager;
import storage.LogFileManager;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AmazonSeller extends BaseSeller<AmazonSeller.AmazonOrder> {

    private static final String API_BASE_URL = "https://sellingpartnerapi-na.amazon.com";
    private volatile List<AmazonOrder> lastFetchedOrders = new ArrayList<>();

    public AmazonSeller(PlatformManager manager, APIFileManager api) {
        super(PlatformType.AMAZON, manager,api);
        manager.amazonSeller = this;
    }


    @Override
    public void fetchOrders() {
        //Amazon seller should not call fetch orders if amazon is not linked return null
        Path file = Path.of(APIFileManager.getTokenFilePath(PlatformType.AMAZON));
        if (!Files.exists(file)) {
            log("No file exist for token: " + PlatformType.AMAZON.getDisplayName() );
            return;
        }
        fetchingOrders = true;
        try {
            //For errors
            JFrame parent = platformManager.getMainWindow();

            //There is no access token or expired, get new token
            if (Duration.between(lastAccessTokenGetTime, LocalDateTime.now()).toMinutes() > tokenExpirationTimeMinutes
                    || accessToken == null) {

                String [] t = apiFileManager.getCredentialsArray(PlatformType.AMAZON);
                accessToken = apiFileManager.getAmazonAccessToken(t[0], t[1], t[2]);
                lastAccessTokenGetTime = LocalDateTime.now();
            }

            //Try current access token
            int response = apiFileManager.vaildateAmazonAccessToken(accessToken);

            if (response> 299 || response < 200){
                //Delete current access token
                if(response != 429 && response <500){
                accessToken = null;
                }
                SwingUtilities.invokeLater(() -> {
                    String message = String.format(
                            "Failed to authenticate with Amazon.\n\n" +
                                    "HTTP Response Code: %d\n\n" +
                                    "Possible causes:\n" +
                                    "  1. Invalid client credentials\n" +
                                    "  2. Expired or revoked refresh token\n" +
                                    "  3. Network connectivity issues\n" +
                                    "  4. Amazon API service outage\n\n" +
                                    "Please check your credentials in the link window and try again.",
                            response
                    );

                    JOptionPane.showMessageDialog(parent, message, "Amazon Authentication Error", JOptionPane.ERROR_MESSAGE);
                });
                fetchingOrders = false;
                return;
            }
            //We have a valid non-expired token, so get recent orders and parse
            log(" Valid access token");
            String createdAfter = lastGetOrderTime
                    .minusDays(14)
                    .atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            String ordersEndpoint =
                    API_BASE_URL + "/orders/v0/orders?MarketplaceIds=ATVPDKIKX0DER&CreatedAfter=" + URLEncoder.encode(createdAfter, StandardCharsets.UTF_8);

            URL url = new URL(ordersEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            conn.setRequestProperty("x-amz-access-token", accessToken);
            conn.setRequestProperty("User-Agent", "InventoryApp/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int ordersResponseCode = conn.getResponseCode();
            log("Orders API Response: " + ordersResponseCode);

            if (ordersResponseCode < 200 || ordersResponseCode > 299) {
                SwingUtilities.invokeLater(() -> {
                    String message = String.format(
                            "Could not fetch orders from Amazon.\n\n" +
                                    "HTTP Response Code: %d\n\n" +
                                    "Possible causes:\n" +
                                    "  • Invalid client credentials\n" +
                                    "  • Expired or revoked refresh token\n" +
                                    "  • Network connectivity issues\n" +
                                    "  • Amazon API service outage\n\n" +
                                    "Please check your credentials in the link window.",
                            ordersResponseCode
                    );

                    JOptionPane.showMessageDialog(null, message,
                            "Amazon Authentication Error", JOptionPane.ERROR_MESSAGE);
                });
                fetchingOrders = false;
                return;
            }

            //We have the orders, now turn it into json
            lastGetOrderTime = LocalDateTime.now();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            conn.disconnect();

            String json = sb.toString();
            log("RAW JSON RESPONSE:\n" + json);

            JsonObject ordersResponse = JsonParser.parseString(sb.toString()).getAsJsonObject();

            JsonArray ordersList = ordersResponse.getAsJsonObject("payload").getAsJsonArray("Orders");
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("payload")) {
                log("Error: missing payload");
                fetchingOrders = false;
                return;
            }

            JsonObject payload = root.getAsJsonObject("payload");

            if (!payload.has("Orders")) {
                log("No Orders array in payload.");
                fetchingOrders = false;
                return;
            }
            JsonArray orders = payload.getAsJsonArray("Orders");
            log("Fetched " + orders.size() + " orders");
            ArrayList<IdAndStatus> orderIds = new ArrayList<>();
            for(JsonElement orderElement : ordersList) {
                JsonObject orderObj = orderElement.getAsJsonObject();

                String orderId = getString(orderObj, "AmazonOrderId");
                String orderStatusString = getString(orderObj, "OrderStatus");
                OrderStatus orderStatus = switch(orderStatusString.trim().toLowerCase()){
                    case "pending availability",
                         "pending",
                         "unshipped",
                         "partiallyshipped",
                         "invoiceunconfirmed" -> OrderStatus.CONFIRMED;

                    case "shipped" -> OrderStatus.SHIPPED;

                    case "canceled", "unfulfillable" -> OrderStatus.CANCELLED; default -> OrderStatus.CONFIRMED; };
                String updateDateString = getString(orderObj, "LastUpdateDate");
                LocalDateTime updateDate = LocalDateTime.parse(updateDateString,
                        DateTimeFormatter.ISO_DATE_TIME);

                AmazonOrder existing = (AmazonOrder) platformManager.getOrder(PlatformType.AMAZON,orderId);

                if(existing != null) {
                    //Order already exists and nothing changed, no need to parse again
                    if (existing.getStatus() == orderStatus &&
                            existing.getLastUpdated().isEqual(updateDate))
                    {
                        continue;
                    }
                }

                orderIds.add(new IdAndStatus(orderId, orderStatus, updateDate));
            }
            fetchOrderItemsAsync(orderIds);
        }catch (Exception e){
            fetchingOrders = false;
            log(e.getMessage());
        }
    }
    void fetchOrderItemsAsync(List<IdAndStatus> orderIds){
        SwingWorker<List<AmazonOrder>, String> worker = new SwingWorker<>() {

            @Override
            protected List<AmazonOrder> doInBackground() {
                List<AmazonOrder> allOrders = new ArrayList<>();
                int total = orderIds.size();

                for (int i = 0; i < total; i++) {
                    IdAndStatus info = orderIds.get(i);
                    try {

                        AmazonOrder order = new AmazonOrder(info.orderId, info.status, info.updateDate);
                        log("Fetching order " + (i + 1) + "/" + total + " (" + info.orderId + ")");


                        String url = API_BASE_URL + "/orders/v0/orders/" + info.orderId + "/orderItems";
                        HttpURLConnection conn = openGetConnection(url);

                        long start = System.currentTimeMillis();
                        int response = conn.getResponseCode();
                        long elapsed = System.currentTimeMillis() - start;
                        log("[Order " + info.orderId + "] Response: " + response + " (" + elapsed + " ms)");

                        String json = readResponse(conn);
                        conn.disconnect();

                        JsonObject payload = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("payload");
                        JsonArray items = payload.getAsJsonArray("OrderItems");

                        for (JsonElement item : items) {
                            JsonObject itemObj = item.getAsJsonObject();
                            String sku = getString(itemObj, "SellerSKU");
                            int qty = itemObj.has("QuantityOrdered") ? itemObj.get("QuantityOrdered").getAsInt() : 0;
                            order.addItem(new OrderPacket(sku, qty));
                        }

                        allOrders.add(order);

                        Thread.sleep(2200);

                    } catch (Exception e) {
                        publish("Error fetching order " + info.orderId + ": " + e.getMessage());
                    }
                }
                fetchingOrders = false;
                return allOrders;
            }

            @Override
            protected void done() {
                try {
                    lastFetchedOrders = get();
                    log("Fetched " + lastFetchedOrders.size() + " orders.");
                    fetchingOrders = false;
                } catch (Exception e) {
                    fetchingOrders = false;
                    log("Failed to fetch order items: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    private HttpURLConnection openGetConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-amz-access-token", accessToken);
        conn.setRequestProperty("User-Agent", "InventoryApp/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }
    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getResponseCode() < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    public static class AmazonOrder extends BaseSeller.Order {
        public LocalDateTime lastUpdated;
        public AmazonOrder(String orderId, BaseSeller.OrderStatus status, LocalDateTime lastUpdated) {
            super(orderId,status);
            this.lastUpdated = lastUpdated;
        }
        LocalDateTime getLastUpdated(){
            return lastUpdated;
        }
    }
    public record IdAndStatus(String orderId,OrderStatus status, LocalDateTime updateDate){}
}