package platform;

import core.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.Base64;

//Securely read,writes and stores tokens in encrypted folders
//File : ~/data/encrypted_tokens/(amazon/ebay/walmart)/token.enc
public class APIStorage {

    //Machine generated passphrase so that files copied to other machines need new tokens
    private final String passphrase;

    public APIStorage() {
        passphrase = generateMachineBoundKey();
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
            return "fallback-key";
        }
    }

    private SecretKeySpec getSecretKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    private String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
        byte[] decoded = Base64.getDecoder().decode(encryptedData.trim());
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    public synchronized void saveToken(PlatformType platform, String token) {
        try {
            Path dir = Path.of(platform.getStorageDir());
            Files.createDirectories(dir);

            String encrypted = encrypt(token);
            Path file = Path.of(platform.getTokenFilePath());
            Files.writeString(file, encrypted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public synchronized String loadToken(PlatformType platform) {
        try {
            Path file = Path.of(platform.getTokenFilePath());
            if (!Files.exists(file)) return null;

            String encrypted = Files.readString(file);
            String decrypted = decrypt(encrypted);

            if (decrypted.startsWith("VALID|")) {
                return decrypted.substring("VALID|".length());
            }

            return decrypted;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public synchronized void clearAll() {
        for (PlatformType p : PlatformType.values()) {
            removeToken(p);
        }
    }
    public boolean validateToken(APIStorage.PlatformType type, String token) {
        try {
            System.out.print("[BEGIN VALIDATING TOKEN]");
            HttpURLConnection conn = null;

            switch (type) {
                case AMAZON -> {
                    System.out.println("[BEGIN ADD AMAZON ACCOUNT]");
                    System.out.println("-> Token format check...");
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 3) {
                        System.out.println("Invalid token format (found " + parts.length + ") Expected : 3");
                        return false;
                    }

                    String clientId = parts[0];
                    String clientSecret = parts[1];
                    String refreshToken = parts[2];

                    System.out.println("-> Getting Amazon Token");
                    String accessToken = getAmazonAccessToken(clientId, clientSecret, refreshToken);
                    System.out.println("-> Returned accessToken=" + (accessToken == null ? "null" : "OK len=" + accessToken.length()));
                    if (accessToken == null) return false;

                    System.out.println("-> Opening SP-API connection...");
                    URL url = new URL("https://sandbox.sellingpartnerapi-na.amazon.com/sellers/v1/marketplaceParticipations");
                    conn = (HttpURLConnection) url.openConnection();

                    System.out.println("-> Setting request method...");
                    conn.setRequestMethod("GET");

                    System.out.println("-> Adding headers...");
                    conn.setRequestProperty("x-amz-access-token", accessToken);
                    conn.setRequestProperty("User-Agent", "InventoryApp/1.0");

                    System.out.println("->Setting timeouts");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

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
                        return false;
                    }

                    String clientId = parts[0];
                    String clientSecret = parts[1];
                    String refreshToken = parts[2];

                    URL url = new URL("https://api.ebay.com/sell/account/v1/privilege");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                }

                case WALMART -> {
                    // Walmart is consumer ID + private key joined by |
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 2){
                        System.out.println("Invalid token format (found " + parts.length + ") Expected : 2");
                        return false;
                    }

                    String consumerId = parts[0];
                    String privateKey = parts[1];

                    URL url = new URL("https://marketplace.walmartapis.com/v3/token");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                }
                default -> {
                    throw new RuntimeException("Platform type not implemented: " + type);
                }
            }

            if (conn == null) {
                return false;
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int response = conn.getResponseCode();
            String message = conn.getResponseMessage();

            conn.disconnect();
            System.out.print("[VALIDATING TOKEN END] Response code: " + response + " (" + message + ")");

            // 200–299 = valid, anything else = invalid
            if(response >= 200 && response < 300)
            {
                System.out.print("Success. Good HTTP response");
                return true;
            }else{
                System.out.print("ERROR. Bad HTTP response");
                return false;
            }
        } catch (IOException e) {
            System.out.print("ERROR: [VALIDATING TOKEN FAILED]");
            return false;
        }
    }
    public synchronized void removeToken(PlatformType platform) {
        try {
            Files.deleteIfExists(Path.of(platform.getTokenFilePath()));
        } catch (IOException ignored) {}
    }
    private String getAmazonAccessToken(String clientId, String clientSecret, String refreshToken) {
        try {
            URL url = new URL("https://api.amazon.com/auth/o2/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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

            if (conn.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();

            int start = json.indexOf("\"access_token\":\"");
            if (start == -1) return null;
            start += 16;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);

        } catch (Exception e) {
            return null;
        }
    }
    public enum PlatformType {
        AMAZON("Amazon"),
        EBAY("eBay"),
        WALMART("Walmart");

        private final String displayName;
        private final String storageDir;

        PlatformType(String displayName) {
            this.displayName = displayName;

            String baseDir = new File("data" + File.separator + "encrypted_tokens").getAbsolutePath();

            this.storageDir = baseDir + File.separator + displayName.toLowerCase();
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStorageDir() {
            return storageDir;
        }

        public String getTokenFilePath() {
            return storageDir + File.separator + "token.enc";
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
