package platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import storage.APIFileManager;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class WalmartSeller extends BaseSeller {
    private static final String API_BASE_URL = "https://marketplace.walmartapis.com/v3/orders";

    public WalmartSeller(PlatformManager manager, APIFileManager api) {
        super(PlatformType.WALMART, manager, api);
        manager.walmartSeller = this;
    }

    @Override
    public void fetchOrders() {
        //Walmart seller should not call fetch orders if Walmart is not linked return null
        fetchingOrders = true;
        Path file = Path.of(APIFileManager.getTokenFilePath(PlatformType.WALMART));
        if (!Files.exists(file)) {
            log("No file exist for token: " + PlatformType.WALMART.getDisplayName());
            fetchingOrders = false;
            return;
        }
        try {
            JFrame parent = platformManager.getMainWindow();

            //token age
            long tokenAgeMinutes = Duration.between(lastAccessTokenGetTime,  ZonedDateTime.now(ZoneOffset.UTC)).toMinutes();
            boolean tokenIsOld = tokenAgeMinutes > tokenExpirationTimeMinutes;

            String[] credentials = apiFileManager.getCredentialsFromFile(PlatformType.WALMART);
            String clientID = credentials[0];
            String clientSecret = credentials[1];

            //Get token if old or missing
            if (tokenIsOld || accessToken == null) {
                log("Token expired or missing. Fetching new token...");
                APIFileManager.AccessTokenResponse accessTokenResponse = apiFileManager.getWalmartAccessToken(clientID, clientSecret);
                accessToken = accessTokenResponse.accessToken();

                lastAccessTokenGetTime = ZonedDateTime.now(ZoneOffset.UTC);
            }

            //Validate current access token
            int response = apiFileManager.validateWalmartAccessToken(accessToken, clientID);

            if (response < 200 || response > 299) {
                log("Token validation failed with code: " + response);

                //Only retry if token was old, or we haven't tried refreshing yet
                if (tokenIsOld || tokenAgeMinutes > 5) {  // If token is more than 5 minutes old
                    log("Attempting token refresh...");

                    APIFileManager.AccessTokenResponse accessTokenResponse = apiFileManager.getWalmartAccessToken(clientID, clientSecret);
                    accessToken = accessTokenResponse.accessToken();

                    lastAccessTokenGetTime = ZonedDateTime.now(ZoneOffset.UTC);

                    //Validate the new token
                    int response2 = apiFileManager.validateWalmartAccessToken(accessToken, clientID);

                    if (response2 >= 200 && response2 <= 299) {
                        log("Token refresh successful!");
                        //Break here, continue to fetch orders
                    } else {
                        //Second attempt failed
                        log("Token refresh failed with code: " + response2);
                        if (response2 != 429 && response2 < 500) {
                            accessToken = null;
                        }

                        //Show error dialog
                        SwingUtilities.invokeLater(() -> {
                            String message = String.format(
                                    """
                                            Failed to authenticate with Walmart after retry.
                                            
                                            HTTP Response Code: %d
                                            
                                            Possible causes:
                                              1. Invalid client credentials
                                              2. Expired or revoked refresh token
                                              3. Network connectivity issues
                                              4. Walmart API service outage
                                            
                                            Please check your credentials in the link window and try again.""",
                                    response2
                            );
                            JOptionPane.showMessageDialog(parent, message,
                                    "Walmart Authentication Error", JOptionPane.ERROR_MESSAGE);
                        });

                        fetchingOrders = false;
                        return;
                    }
                } else {
                    //Token is fresh but still failed
                    log("Fresh token failed validation. Not retrying.");
                    if (response != 429 && response < 500) {
                        accessToken = null;
                    }
                    final int finalResponseCode = response;
                    keyFailCounter++;
                    SwingUtilities.invokeLater(() -> {
                        String message = String.format(
                                """
                                        Failed to authenticate with Walmart.
                                        
                                        HTTP Response Code: %d
                                        
                                        Possible causes:
                                          1. Invalid client credentials
                                          2. Expired or revoked refresh token
                                          3. Network connectivity issues
                                          4. Walmart API service outage
                                        
                                        Please check your credentials in the link window and try again.""",
                                finalResponseCode
                        );
                        JOptionPane.showMessageDialog(parent, message,
                                "Walmart Authentication Error", JOptionPane.ERROR_MESSAGE);
                    });
                    fetchingOrders = false;
                    return;
                }
            } else {
                log("Valid access token (code: " + response + ")");
            }
            //We have a valid non-expired token, so get recent orders and parse
            keyFailCounter = 0;
            String createdAfter = getLastGetOrderTimeForFetching()
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            String ordersEndpoint =
                    API_BASE_URL + "?createdStartDate=" + URLEncoder.encode(createdAfter, StandardCharsets.UTF_8);

            URL url = new URL(ordersEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            apiFileManager.addWalmartHeaders(conn, accessToken, clientID);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int ordersResponseCode = conn.getResponseCode();
            log("Orders API Response: " + ordersResponseCode);

            if (ordersResponseCode < 200 || ordersResponseCode > 299) {
                keyFailCounter++;
                SwingUtilities.invokeLater(() -> {
                    String message = String.format(
                            """
                                    Could not fetch orders from Walmart.
                                    
                                    HTTP Response Code: %d
                                    
                                    Possible causes:
                                      • Invalid client credentials
                                      • Expired or revoked refresh token
                                      • Network connectivity issues
                                      • Walmart API service outage
                                    
                                    Please check your credentials in the link window.""",
                            ordersResponseCode
                    );

                    JOptionPane.showMessageDialog(null, message,
                            "Walmart Authentication Error", JOptionPane.ERROR_MESSAGE);
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
            JsonObject list = root.getAsJsonObject("list");
            JsonObject elements = list.getAsJsonObject("elements");
            JsonArray ordersArray = elements.getAsJsonArray("order");

            List<Order> walmartOrders = new ArrayList<>();

            for (JsonElement orderEl : ordersArray) {
                JsonObject orderObj = orderEl.isJsonObject() && orderEl.getAsJsonObject().has("order")
                        ? orderEl.getAsJsonObject().getAsJsonObject("order")
                        : orderEl.getAsJsonObject();

                String orderId = orderObj.get("purchaseOrderId").getAsString();

                long orderDateMillis = orderObj.get("orderDate").getAsLong();
                ZonedDateTime orderDate = Instant.ofEpochMilli(orderDateMillis)
                        .atZone(ZoneId.systemDefault());

                OrderStatus orderStatus = OrderStatus.CONFIRMED; // default
                List<OrderPacket> newItems = new ArrayList<>();

                if (orderObj.has("orderLines")) {
                    JsonArray orderLines = orderObj.getAsJsonObject("orderLines").getAsJsonArray("orderLine");

                    for (JsonElement lineEl : orderLines) {
                        JsonObject lineObj = lineEl.getAsJsonObject();
                        JsonObject itemObj = lineObj.getAsJsonObject("item");
                        String sku = itemObj.get("sku").getAsString();

                        JsonObject qtyObj = lineObj.getAsJsonObject("orderLineQuantity");
                        int quantity = Integer.parseInt(qtyObj.get("amount").getAsString());

                        if (lineObj.has("orderLineStatuses")) {
                            JsonArray statuses = lineObj.getAsJsonObject("orderLineStatuses")
                                    .getAsJsonArray("orderLineStatus");
                            if (!statuses.isEmpty()) {
                                String statusStr = statuses.get(0).getAsJsonObject().get("status").getAsString();
                                orderStatus = switch (statusStr.trim().toLowerCase()) {
                                    case "shipped", "delivered" -> OrderStatus.SHIPPED;
                                    case "cancelled" -> OrderStatus.CANCELLED;
                                    default -> OrderStatus.CONFIRMED;
                                };
                            }
                        }

                        newItems.add(new OrderPacket(sku, quantity));
                    }
                }

                //Check if order already exists
                Order existingOrder = platformManager.getOrder(PlatformType.WALMART, orderId);

                if (existingOrder != null) {
                    List<OrderPacket> existingItems = existingOrder.getItems();

                    boolean itemsMatch = existingItems.size() == newItems.size() &&
                            new HashSet<>(existingItems).containsAll(newItems) &&
                            new HashSet<>(newItems).containsAll(existingItems);

                    if (!itemsMatch || existingOrder.getStatus() != orderStatus) {
                        //Update order if something changed
                        existingItems.clear();
                        for (OrderPacket op : newItems) {
                            existingOrder.addItem(op);
                        }
                        existingOrder.setStatus(orderStatus);
                        existingOrder.setLastUpdated(orderDate);
                        log("Updated order " + orderId);
                    } else {
                        log("No changes for order " + orderId);
                    }
                    walmartOrders.add(existingOrder);
                } else {
                    //New order
                    Order order = new Order(orderId, orderStatus, orderDate);
                    for (OrderPacket op : newItems) {
                        order.addItem(op);
                    }
                    walmartOrders.add(order);
                    log("Added new walmart order " + orderId);
                }
            }
            lastFetchedOrders = walmartOrders;
            log("Fetched " + walmartOrders.size() + " orders.");
            fetchingOrders = false;


        } catch (Exception e) {
            fetchingOrders = false;
            log(e.getMessage() + " Stack Trace: " + Arrays.toString(e.getStackTrace()));
        }
    }
}
