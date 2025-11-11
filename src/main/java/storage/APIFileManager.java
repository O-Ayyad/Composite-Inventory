package storage;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import platform.PlatformType;


//Securely read,writes and stores tokens in encrypted folders
//File : ~resources/data/encrypted_tokens/(amazon/ebay/walmart)/token.enc
public class APIFileManager {
    private final String passphrase;
    private final SecureRandom secureRandom;


    private static final String BASE_DIR =
            new File("data" + File.separator + "encrypted_tokens").getAbsolutePath();

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public APIFileManager() {
        try {
            passphrase = generateMachineBoundKey();
            secureRandom = SecureRandom.getInstanceStrong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize secure storage", e);
        }
    }

    private static String generateMachineBoundKey() {
        try {
            String osName = System.getProperty("os.name");
            String user = System.getProperty("user.name");
            String home = System.getProperty("user.home");
            String machineId = java.net.InetAddress.getLocalHost().getHostName();
            String combined = osName + "|" + user + "|" + home + "|" + machineId;

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Cannot generate machine key", e);
        }
    }

    private SecretKeySpec getSecretKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private String encrypt(String data) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec);

        byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String encryptedData) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData.trim());

        if (combined.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public synchronized void saveToken(PlatformType platform, String token) {
        try {
            Path dir = Path.of(getStorageDir(platform));
            Files.createDirectories(dir);

            String encrypted = encrypt(token);
            Path file = Path.of(getTokenFilePath(platform));

            Path tempFile = Paths.get(file.toString() + ".tmp");
            Files.writeString(tempFile, encrypted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
            }

            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            System.out.println("ERROR: Could not save token for "+ platform.name());
        }
    }
    public synchronized String loadToken(PlatformType platform) {
        try {
            Path file = Path.of(getTokenFilePath(platform));
            if (!Files.exists(file)) return null;

            String encrypted = Files.readString(file);
            String decrypted = decrypt(encrypted);

            if (decrypted.startsWith("VALID|")) {
                return decrypted.substring("VALID|".length());
            }

            return decrypted;
        }catch (Exception e) {
            System.out.println("ERROR: Could not load token for "+ platform.name());
            return null;
        }
    }
    public synchronized void clearAll() {
        for (PlatformType p : PlatformType.values()) {
            removeToken(p);
        }
    }
    public int validateToken(PlatformType type, String token) {
        try {
            System.out.println("[BEGIN VALIDATING TOKEN]");
            HttpURLConnection conn = null;

            switch (type) {
                case AMAZON -> {
                    System.out.println("[BEGIN ADD AMAZON ACCOUNT]");
                    System.out.println("-> Token format check...");
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 3) {
                        System.out.println("Invalid token format (found " + parts.length + ") Expected : 3");
                        return -1;
                    }
                    String clientId = parts[0];
                    String clientSecret = parts[1];
                    String refreshToken = parts[2];

                    System.out.println("-> Getting Amazon Token");
                    String accessToken = getAmazonAccessToken(clientId, clientSecret, refreshToken);
                    System.out.println("-> Returned accessToken=" + (accessToken == null ? "null" : "OK len=" + accessToken.length()));
                    if (accessToken == null) {
                        System.out.println("ERROR: Amazon access token is null");
                        return -1;
                    }

                    System.out.println("-> Opening SP-API connection...");
                    URL url = new URL("https://sellingpartnerapi-na.amazon.com/sellers/v1/marketplaceParticipations");
                    conn = (HttpURLConnection) url.openConnection();

                    System.out.println("-> Setting request method...");
                    conn.setRequestMethod("GET");

                    System.out.println("-> Adding headers...");
                    conn.setRequestProperty("x-amz-access-token", accessToken);
                    conn.setRequestProperty("User-Agent", "InventoryApp/1.0");

                    System.out.println("->Setting timeouts");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    System.out.println("-> Attempting to get response code");
                    int response = conn.getResponseCode();
                    System.out.println("-> Response code: " + response);

                    System.out.println("-> Reading body");
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            (response >= 200 && response < 300)
                                    ? conn.getInputStream()
                                    : conn.getErrorStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println("     " + line);
                        }
                    }

                    conn.disconnect();
                    System.out.println("-> Finished normally.");
                    System.out.println("[END AMAZON ACCOUNT]");
                }
                case EBAY -> {
                    //Same as amazon
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 3){
                        System.out.println("Invalid token format (found " + parts.length + ") Expected : 3");
                        return -1;
                    }

                    String clientId = parts[0];
                    String clientSecret = parts[1];
                    String refreshToken = parts[2];

                    URL url = new URL("https://api.ebay.com/sell/account/v1/privilege");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                }
                case WALMART -> {
                    // Walmart is client ID + client secret combined
                    System.out.println("[BEGIN ADD WALMART ACCOUNT]");
                    System.out.println("-> Token format check...");
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 2) {
                        System.out.println("Invalid token format (found " + parts.length + ") Expected : 2");
                        return -1;
                    }

                    String clientID = parts[0];
                    String clientSecret = parts[1];

                    System.out.println("-> Getting Walmart Token");
                    String accessToken = getWalmartAccessToken(clientID, clientSecret);
                    System.out.println("-> Returned accessToken=" + (accessToken == null ? "null" : "OK len=" + accessToken.length()));
                    if (accessToken == null) {
                        System.out.println("ERROR: Walmart access token is null or invalid.");
                        return -1;
                    }

                    System.out.println("-> Opening Walmart API connection...");
                    URL url = new URL("https://marketplace.walmartapis.com/v3/items?limit=1");
                    conn = (HttpURLConnection) url.openConnection();

                    System.out.println("-> Setting request method...");
                    conn.setRequestMethod("GET");

                    System.out.println("-> Adding headers...");

                    conn.setRequestProperty("WM_SEC.ACCESS_TOKEN", accessToken);
                    conn.setRequestProperty("WM_CONSUMER.CHANNEL.TYPE", clientID);
                    conn.setRequestProperty("WM_SVC.NAME", "Walmart Marketplace");
                    conn.setRequestProperty("WM_QOS.CORRELATION_ID", UUID.randomUUID().toString());
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-Type", "application/json");

                    System.out.println("-> Setting timeouts");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                }
                default -> throw new RuntimeException("Platform type not implemented: " + type);
            }

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            System.out.println("-> Attempting to get response code");
            int response = conn.getResponseCode();
            String message = conn.getResponseMessage();
            System.out.println("-> Response code: " + response);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    (response >= 200 && response < 300)
                            ? conn.getInputStream()
                            : conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("     " + line);
                }
            } catch (Exception e){
                System.out.println(e.getMessage());
            }

            conn.disconnect();
            System.out.println("[VALIDATING TOKEN END] Response code: " + response + " (" + message + ")");


            // 200–299 = valid, anything else = invalid
            if(response >= 200 && response < 300) {
                System.out.println("Success. Good HTTP response");
            }else{
                System.out.println("ERROR. Bad HTTP response");
            }
            return response;
        } catch (IOException e) {
            System.out.println("ERROR: [VALIDATING TOKEN FAILED]");
            return -1;
        }
    }
    public synchronized void removeToken(PlatformType platform) {
        try {
            Files.deleteIfExists(Path.of(getTokenFilePath(platform)));
        } catch (IOException ignored) {}
    }
    private String getAmazonAccessToken(String clientId, String clientSecret, String refreshToken) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.amazon.com/auth/o2/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String body = "grant_type=refresh_token"
                    + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                System.out.println("[Amazon] ERROR: HTTP " + code + " - " + conn.getResponseMessage());
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            String responseBody = sb.toString().trim();

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!json.has("access_token")) {
                System.out.println("[Amazon] ERROR: access_token not found in response.");
                return null;
            }

            System.out.println("[Amazon] Received token response.");

            String token = json.get("access_token").getAsString();
            System.out.println("[Amazon] Access token retrieved successfully (len=" + token.length() + ")");

            return token;

        } catch (Exception e) {
            return null;
        }
        finally{
            if(conn != null){
                conn.disconnect();
            }
        }
    }
    private String getWalmartAccessToken(String clientId, String clientSecret) {
        HttpURLConnection conn = null;
        try {
            System.out.println("[Walmart] Requesting new access token...");

            URL url = new URL("https://marketplace.walmartapis.com/v3/token");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");


            String basicAuth = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + basicAuth);
            conn.setRequestProperty("WM_SVC.NAME", "Walmart Marketplace");
            conn.setRequestProperty("WM_QOS.CORRELATION_ID", UUID.randomUUID().toString());

            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write("grant_type=client_credentials".getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            System.out.println("[Walmart] HTTP " + code + " (" + conn.getResponseMessage() + ")");

            InputStream stream = (code >= 200 && code < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (stream == null) {
                System.out.println("[Walmart] No response body from server.");
                return null;
            }

            String encoding = conn.getContentEncoding();
            if (encoding != null && encoding.toLowerCase().contains("gzip")) {
                stream = new java.util.zip.GZIPInputStream(stream);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            String responseBody = sb.toString().trim();

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("access_token")) {
                System.out.println("[Walmart] Received token response.");
            } else {
                System.out.println("[Walmart] ERROR: access_token not found in response.");
                return null;
            }


            String token = json.get("access_token").getAsString();
            System.out.println("[Walmart] Access token retrieved successfully (len=" + token.length() + ")");

            return token;

        } catch (Exception e) {
            System.out.println("[Walmart] EXCEPTION while getting token: " + e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static String getStorageDir(PlatformType platform) {
        return BASE_DIR + File.separator + platform.getDisplayName().toLowerCase();
    }

    public static String getTokenFilePath(PlatformType platform) {
        return getStorageDir(platform) + File.separator + "token.enc";
    }
}
