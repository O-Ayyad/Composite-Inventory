package platform;

import com.google.gson.*;
import storage.APIFileManager;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AmazonSeller extends BaseSeller {

    private static final String API_BASE_URL = "https://sellingpartnerapi-na.amazon.com";

    public AmazonSeller(PlatformManager manager, APIFileManager api) {
        super(PlatformType.AMAZON, manager,api);
        manager.amazonSeller = this;
    }


    @Override
    public void fetchOrders() {
        //Amazon seller should not call fetch orders if amazon is not linked return null
        fetchingOrders = true;
        Path file = Path.of(APIFileManager.getTokenFilePath(PlatformType.AMAZON));
        if (!Files.exists(file)) {
            log("No file exist for token: " + PlatformType.AMAZON.getDisplayName() );
            fetchingOrders = false;
            return;
        }
        //Store all fetched orders
        ArrayList<IdAndStatus> orderIds = new ArrayList<>();
        try {
            JFrame parent = platformManager.getMainWindow();

            //token age
            long tokenAgeMinutes = Duration.between(lastAccessTokenGetTime, ZonedDateTime.now(ZoneOffset.UTC)).toMinutes();
            boolean tokenIsOld = tokenAgeMinutes > tokenExpirationTimeMinutes;

            //Get token if old or missing
            if (tokenIsOld || accessToken == null) {
                log("Token expired or missing. Fetching new token...");
                String[] credentials = apiFileManager.getCredentialsFromFile(PlatformType.AMAZON);

                APIFileManager.AccessTokenResponse accessTokenResponse = apiFileManager.getAmazonAccessToken(credentials[0], credentials[1], credentials[2]);
                accessToken = accessTokenResponse.accessToken();

                lastAccessTokenGetTime = ZonedDateTime.now(ZoneOffset.UTC);
            }

            //Validate current access token
            int response = apiFileManager.validateAmazonAccessToken(accessToken);

            if (response < 200 || response > 299) {
                log("Token validation failed with code: " + response);

                //Only retry if token was old, or we haven't tried refreshing yet
                if (tokenIsOld || tokenAgeMinutes > 5) {  // If token is more than 5 minutes old
                    log("Attempting token refresh...");

                    String[] credentials = apiFileManager.getCredentialsFromFile(PlatformType.AMAZON);

                    APIFileManager.AccessTokenResponse accessTokenResponse = apiFileManager.getAmazonAccessToken(credentials[0], credentials[1], credentials[2]);
                    accessToken = accessTokenResponse.accessToken();

                    //Validate the new token
                    int response2 = apiFileManager.validateAmazonAccessToken(accessToken);

                    if (response2 >= 200 && response2 <= 299) {
                        log("Token refresh successful!");
                        //Break here, continue to fetch orders
                    } else {
                        //Second attempt failed
                        log("Token refresh failed with code: " + response2);
                        if (response2 != 429 && response2 < 500) {
                            accessToken = null;
                            keyFailCounter++;
                        }

                        //Show error dialog
                        SwingUtilities.invokeLater(() -> {
                            String message = String.format(
                                    """
                                            Failed to authenticate with Amazon after retry.
                                            
                                            HTTP Response Code: %d
                                            
                                            Possible causes:
                                              1. Invalid client credentials
                                              2. Expired or revoked refresh token
                                              3. Network connectivity issues
                                              4. Amazon API service outage
                                            
                                            Please check your credentials in the link window and try again.""",
                                    response2
                            );
                            JOptionPane.showMessageDialog(parent, message,
                                    "Amazon Authentication Error", JOptionPane.ERROR_MESSAGE);
                        });

                        fetchingOrders = false;
                        return;
                    }
                } else {
                    //Token is fresh but still failed
                    log("Fresh token failed validation. Not retrying.");
                    if (response != 429 && response < 500) {
                        accessToken = null;
                        keyFailCounter++;
                    }
                    final int finalResponseCode = response;
                    SwingUtilities.invokeLater(() -> {
                        String message = String.format(
                                """
                                        Failed to authenticate with Amazon.
                                        
                                        HTTP Response Code: %d
                                        
                                        Possible causes:
                                          1. Invalid client credentials
                                          2. Expired or revoked refresh token
                                          3. Network connectivity issues
                                          4. Amazon API service outage
                                        
                                        Please check your credentials in the link window and try again.""",
                                finalResponseCode
                        );
                        JOptionPane.showMessageDialog(parent, message,
                                "Amazon Authentication Error", JOptionPane.ERROR_MESSAGE);
                    });
                    fetchingOrders = false;
                    return;
                }
            } else {
                log("Valid access token (code: " + response + ")");
                keyFailCounter = 0;
            }
            //We have a valid non-expired token, so get recent orders and parse
            String createdAfter = getLastGetOrderTimeForFetching()
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

            String ordersEndpoint =
                    API_BASE_URL + "/orders/v0/orders?MarketplaceIds=ATVPDKIKX0DER&CreatedAfter=" + URLEncoder.encode(createdAfter, StandardCharsets.UTF_8);

            URL url = new URL(ordersEndpoint);
            HttpURLConnection conn = apiFileManager.openAmazonConnection(url, accessToken);

            int ordersResponseCode = conn.getResponseCode();

            log("Orders API Response: " + ordersResponseCode);
            if (ordersResponseCode < 200 || ordersResponseCode > 299) {
                SwingUtilities.invokeLater(() -> {
                    String message = String.format(
                            """
                                    Could not fetch orders from Amazon.
                                    
                                    HTTP Response Code: %d
                                    
                                    Possible causes:
                                      • Invalid client credentials
                                      • Expired or revoked refresh token
                                      • Network connectivity issues
                                      • Amazon API service outage
                                    
                                    Please check your credentials in the link window.""",
                            ordersResponseCode
                    );

                    JOptionPane.showMessageDialog(null, message,
                            "Amazon Authentication Error", JOptionPane.ERROR_MESSAGE);
                });
                fetchingOrders = false;
                return;
            }

            //We have the orders, now turn it into json
            lastGetOrderTime = ZonedDateTime.now(ZoneOffset.UTC);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            conn.disconnect();

            String json = sb.toString();
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

            for(JsonElement orderElement : orders) {
                JsonObject orderObj = orderElement.getAsJsonObject();

                String orderId = getString(orderObj, "AmazonOrderId");
                String orderStatusString = getString(orderObj, "OrderStatus");
                OrderStatus orderStatus = switch(orderStatusString.trim().toLowerCase()){

                    case "shipped" -> OrderStatus.SHIPPED;

                    case "canceled", "unfulfillable" -> OrderStatus.CANCELLED;

                    //Everything else is confirmed
                    default -> OrderStatus.CONFIRMED;
                };
                String updateDateString = getString(orderObj, "LastUpdateDate");
                ZonedDateTime updateDate = ZonedDateTime.parse(updateDateString,
                        DateTimeFormatter.ISO_DATE_TIME);

                Order existing = platformManager.getOrder(PlatformType.AMAZON,orderId);

                if(existing != null) {
                    //Order already exists and nothing changed, no need to parse again
                    if (existing.getStatus() == orderStatus &&
                            existing.getLastUpdated().isEqual(updateDate)) {
                        continue;
                    }
                }

                orderIds.add(new IdAndStatus(orderId, orderStatus, updateDate));
            }
            try {
                log("Calling async function with " + orderIds.size() + " orders");
                fetchOrderItemsAsync(orderIds);
            } catch (Exception ex) {
                log("Failed to start async order fetch: " + ex.getMessage());
                fetchingOrders = false;
            }
        }catch (Exception e){
            fetchingOrders = false;
            log(e.getMessage() + " Stack Trace: " + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
    }
    void fetchOrderItemsAsync(List<IdAndStatus> orderIds){
        SwingWorker<List<Order>, String> worker = new SwingWorker<>() {

            @Override
            protected List<Order> doInBackground() {
                List<Order> allOrders = new ArrayList<>();
                int total = orderIds.size();

                int httpWait = 2200;
                int maxRetries = 12;

                for (int i = 0; i < total; i++) {

                    IdAndStatus info = orderIds.get(i);
                    log("Fetching order " + (i + 1) + "/" + total + " (" + info.orderId + ")");

                    Order order = new Order(info.orderId, info.status, info.updateDate);

                    int retry = 0;

                    while (true) {
                        HttpURLConnection conn = null;

                        try {
                            String url = API_BASE_URL + "/orders/v0/orders/" + info.orderId + "/orderItems";
                            conn = apiFileManager.openAmazonConnection(url,accessToken);

                            long start = System.currentTimeMillis();
                            int response = conn.getResponseCode();
                            long elapsed = System.currentTimeMillis() - start;

                            if (response == 429) {

                                if (retry >= maxRetries) {
                                    log("Max retries reached for " + info.orderId);
                                    break;
                                }

                                long wait = (long) (1000 * Math.pow(2, retry));
                                log("Throttled. Retry #" + retry + " after " + wait + " ms for order# " + info.orderId);

                                retry++;
                                Thread.sleep(wait);
                                continue; //retry in while loop
                            }

                            retry = 0;
                            log("[Order " + info.orderId + "] Response: " + response + " (" + elapsed + " ms)");

                            String json = readResponse(conn);

                            JsonObject payload = JsonParser.parseString(json)
                                    .getAsJsonObject()
                                    .getAsJsonObject("payload");

                            JsonArray items = payload.getAsJsonArray("OrderItems");

                            for (JsonElement itemEl : items) {
                                JsonObject itemObj = itemEl.getAsJsonObject();
                                String sku = getString(itemObj, "SellerSKU");
                                int qty = itemObj.has("QuantityOrdered")
                                        ? itemObj.get("QuantityOrdered").getAsInt()
                                        : 0;
                                order.addItem(new OrderPacket(sku, qty));
                            }

                            allOrders.add(order);

                            Thread.sleep(httpWait);
                            break;

                        } catch (Exception e) {
                            publish("Error fetching order " + info.orderId + ": " + e.getMessage());
                            break;

                        } finally {
                            if (conn != null) {
                                try { conn.disconnect(); } catch (Exception ignored) {}
                            }
                        }
                    }
                }
                return allOrders;
            }

            @Override
            protected void done() {
                try {
                    lastFetchedOrders = get();
                    log("Fetched " + lastFetchedOrders.size() + " orders.");
                } catch (Exception e) {
                    log("Failed to fetch order items: " + e.getMessage());
                }finally{
                    fetchingOrders = false;
                }
            }
        };
        worker.execute();
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

    public record IdAndStatus(String orderId,OrderStatus status, ZonedDateTime updateDate){}
}