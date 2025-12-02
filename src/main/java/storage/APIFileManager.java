package storage;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.google.gson.*;
import gui.MainWindow;
import platform.*;


//Securely read,writes and stores tokens in encrypted files
//File : ~encrypted_tokens/(amazon/ebay/walmart)/token.enc
public class APIFileManager {

    private String password; //machine bound + user password
    private final SecureRandom secureRandom;

    MainWindow mainWindow;

    private static final String BASE_DIR =
            new File("encrypted_tokens").getAbsolutePath();

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final String VALIDATION_FILENAME = "password_validation.enc";
    private static final String VALIDATION_STRING = "VALID_PASSWORD_CHECK";

    public APIFileManager(MainWindow mainWindow) {
        try {
            this.mainWindow = mainWindow;
            secureRandom = SecureRandom.getInstanceStrong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize secure storage", e);
        }
    }
    private String generateMachineBoundKey(String personalPassword) {
        try {
            String osName = System.getProperty("os.name");
            String user = System.getProperty("user.name");
            String home = System.getProperty("user.home");
            String machineId = java.net.InetAddress.getLocalHost().getHostName();
            String combined = osName + "|" + user + "|" + home + "|" + machineId + "|" + personalPassword;

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("ERROR: Cannot generate machine key", e);
        }
    }

    public boolean validInputPassword(String input) {
        try {
            String osName = System.getProperty("os.name");
            String user = System.getProperty("user.name");
            String home = System.getProperty("user.home");
            String machineId = java.net.InetAddress.getLocalHost().getHostName();
            String combined = osName + "|" + user + "|" + home + "|" + machineId + "|" + input;

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(combined.getBytes(StandardCharsets.UTF_8));
            return password.equals(Base64.getEncoder().encodeToString(hash));
        } catch (Exception e) {
            throw new RuntimeException("ERROR: Cannot generate machine key", e);
        }
    }
    private void generatePassword(String personalPassword) {
        password = generateMachineBoundKey(personalPassword);
    }

    private String createPassword() {
        while (true) {
            JPasswordField pf1 = new JPasswordField(20);
            JPasswordField pf2 = new JPasswordField(20);

            JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
            panel.add(new JLabel("<html>Create a new password:<br>"
                    + "Do not forget this! If lost, orders cannot be fetched.<br></html>"));
            panel.add(pf1);
            panel.add(new JLabel("Confirm password:"));
            panel.add(pf2);

            int result = JOptionPane.showConfirmDialog(
                    null, panel, "Create Password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) continue;

            String pass1 = new String(pf1.getPassword());
            String pass2 = new String(pf2.getPassword());

            if (pass1.equals(pass2)) {
                return pass1;
            }

            JOptionPane.showMessageDialog(null,
                    "Passwords do not match. Please try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private String getPassword() {
        final String[] enteredPassword = {null};

        //Check if password is empty
        generatePassword("");
        boolean emptyValid = validPassword();

        if (emptyValid){
            return "";
        }

        do {
            JPasswordField pf = new JPasswordField(20);

            JPanel contentPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            contentPanel.add(new JLabel("Enter your password:"));
            contentPanel.add(pf);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            JButton resetButton = new JButton("Reset");
            resetButton.setPreferredSize(new Dimension(75, 25));

            buttonPanel.add(okButton);
            buttonPanel.add(resetButton);

            JDialog dialog = new JDialog((Frame) null, "Enter Password", true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setLayout(new BorderLayout());
            dialog.add(contentPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            okButton.addActionListener(e -> {
                String pass = new String(pf.getPassword()).trim();
                enteredPassword[0] = pass;
                generatePassword(pass);

                if (validPassword()) {
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Invalid password. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    enteredPassword[0] = null;
                }
            });

            resetButton.addActionListener(e -> {
                int reset = JOptionPane.showConfirmDialog(
                        dialog,
                        "Do you want to reset your password?\nThis will remove all connections.",
                        "Reset Password",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (reset == JOptionPane.YES_OPTION) {

                    for (PlatformType p : PlatformType.values()) {
                        removeToken(p);
                    }

                    deleteValidationFile();
                    enteredPassword[0] = createPassword();
                    generatePassword(enteredPassword[0]);
                    savePasswordValidation();

                    JOptionPane.showMessageDialog(
                            dialog,
                            "Password has been reset. All connections have been removed.",
                            "Password Reset",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    dialog.dispose();
                }
            });

            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

        } while (enteredPassword[0] == null);

        return enteredPassword[0];
    }
    public synchronized boolean validPassword() {
        if (password == null) {
            return false;
        }
        if (!hasValidationFile()) {
            return false;
        }

        try {
            Path file = Path.of(getValidationFilePath());
            String encrypted = Files.readString(file);
            String decrypted = decrypt(encrypted);

            return VALIDATION_STRING.equals(decrypted);

        } catch (Exception e) {
            return false;
        }
    }
    public void initializePassword(boolean firstLink) {
        try {
            String personalPassword;

            if (firstLink) {
                personalPassword = createPassword();
                generatePassword(personalPassword);

                savePasswordValidation();
            } else {

                personalPassword = getPassword();
                generatePassword(personalPassword);
            }
            if (personalPassword == null) {
                throw new RuntimeException("Password creation failed.");
            }


        } catch (Exception e) {
            throw new RuntimeException("ERROR: Cannot generate machine key", e);
        }
    }
    private String getValidationFilePath() {
        return BASE_DIR + File.separator + VALIDATION_FILENAME;
    }

    private boolean hasValidationFile() {
        return Files.exists(Path.of(getValidationFilePath()));
    }

    private void savePasswordValidation() {
        try {
            Path dir = Path.of(BASE_DIR);
            Files.createDirectories(dir);

            String encrypted = encrypt(VALIDATION_STRING);
            Path file = Path.of(getValidationFilePath());
            Path tempFile = Paths.get(file + ".tmp");

            Files.writeString(tempFile, encrypted,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.setPosixFilePermissions(tempFile,
                        PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
            }

            Files.move(tempFile, file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            throw new RuntimeException("ERROR: Could not save password validation", e);
        }
    }

    public void deleteValidationFile() {
        try {
            Path file = Path.of(getValidationFilePath());
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (Exception e) {
            System.out.println("ERROR: Could not delete validation file");
            System.out.println(e.getMessage());
        }
    }

    private SecretKeySpec getSecretKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes(StandardCharsets.UTF_8));
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

            Path tempFile = Paths.get(file + ".tmp");
            Files.writeString(tempFile, encrypted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
            }

            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            System.out.println("ERROR: Could not save token for "+ platform.name());
            System.out.println(e.getMessage());
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
    public synchronized boolean hasToken(PlatformType platform) {
        return Files.exists(Path.of(getTokenFilePath(platform)));
    }
    public synchronized void removeToken(PlatformType platform) {
        try {
            Files.deleteIfExists(Path.of(getTokenFilePath(platform)));
        } catch (IOException ignored) {}

        if(!hasAnyConnected()){
            mainWindow.setHasConnected(false);
            deleteValidationFile();
        }
    }
    public boolean hasAnyConnected(){
        for(PlatformType p : PlatformType.values()){
            if(hasToken(p)) return true;
        }
        return false;
    }
    public int validateToken(PlatformType type, String token) {
        try {
            System.out.println("[BEGIN VALIDATING TOKEN]");
            HttpURLConnection conn;

            switch (type) {
                case AMAZON -> {
                    System.out.println("[BEGIN ADD AMAZON ACCOUNT]");
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 3) {
                        System.out.println("ERROR: Invalid token format (found " + parts.length + ") Expected : 3");
                        return -1;
                    }
                    String clientId = parts[0];
                    String clientSecret = parts[1];
                    String refreshToken = parts[2];

                    AccessTokenResponse accessTokenResponse = getAmazonAccessToken(clientId, clientSecret, refreshToken);

                    int responseCode = accessTokenResponse.response;
                    String accessToken = accessTokenResponse.accessToken;

                    System.out.println("-> Returned accessToken=" + (accessToken == null ? "null" : "OK len=" + accessToken.length()));
                    if (accessToken == null) {
                        System.out.println("ERROR: Amazon access token is null");
                        return -responseCode;
                    }

                    URL url = new URL("https://sellingpartnerapi-na.amazon.com/sellers/v1/marketplaceParticipations");
                    conn = openAmazonConnection(url,accessToken);
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
                    String[] parts = token.split("\\|::\\|");
                    if (parts.length != 2) {
                        System.out.println("ERROR: Invalid token format (found " + parts.length + ") Expected : 2");
                        return -1;
                    }

                    String clientID = parts[0];
                    String clientSecret = parts[1];

                    AccessTokenResponse accessTokenResponse = getWalmartAccessToken(clientID, clientSecret);

                    int responseCode = accessTokenResponse.response;
                    String accessToken = accessTokenResponse.accessToken;

                    if (accessToken == null) {
                        System.out.println("ERROR: Walmart access token is null or invalid.");
                        return responseCode;
                    }

                    URL url = new URL("https://marketplace.walmartapis.com/v3/orders");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    addWalmartHeaders(conn, accessToken,clientID);
                }
                default -> throw new RuntimeException("Platform type not implemented: " + type);
            }

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int response = conn.getResponseCode();
            String message = "Response from "+type.getDisplayName();
            System.out.println("-> Response code: " + response);
            boolean goodResponse = (response >= 200 && response < 300);
            if(!goodResponse){
                System.out.println("ERROR. Bad HTTP response");
                message = conn.getResponseMessage();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
                String rawJson = sb.toString();

                //pretty print
                try {
                    String pretty = new GsonBuilder()
                            .setPrettyPrinting()
                            .create()
                            .toJson(JsonParser.parseString(rawJson));

                    System.out.println(pretty);

                }catch (Exception e){
                    System.out.println("ERROR: "+ e.getMessage());
                }
            }else{
                System.out.println("Success. Good HTTP response");
            }

            conn.disconnect();
            System.out.println("[VALIDATING TOKEN END] Response code: " + response + " (" + message + ")");

            return response;
        } catch (IOException e) {
            System.out.println("ERROR: [VALIDATING TOKEN FAILED]");
            System.out.println(Arrays.toString(e.getStackTrace()));
            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public String[] getCredentialsFromFile(PlatformType platform) {
        String token = loadToken(platform);
        if (token == null) return null;
        String[] parts = token.split("\\|::\\|");

        if ((platform == PlatformType.WALMART && parts.length != 2) ||
                (platform == PlatformType.AMAZON && parts.length != 3) ||
        (platform == PlatformType.EBAY && parts.length != 3)) {
            System.out.println("ERROR: Invalid token format for " + platform);
            return null;
        }

        return parts;
    }
    public AccessTokenResponse getAmazonAccessToken(String clientId, String clientSecret, String refreshToken) {
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
            if (code <200 || code >=300) {
                System.out.println("[Amazon] ERROR: HTTP " + code + " - " + conn.getResponseMessage());
                return new AccessTokenResponse(code,null);
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
                return new AccessTokenResponse(code,null);
            }

            System.out.println("[Amazon] Received token response.");

            String token = json.get("access_token").getAsString();
            System.out.println("[Amazon] Access token retrieved successfully (len=" + token.length() + ")");

            return new AccessTokenResponse(code,token);

        } catch (Exception e) {
            return new AccessTokenResponse(-1,null);
        }
        finally{
            if(conn != null){
                conn.disconnect();
            }
        }
    }
    public String getSellerIdFromLwaToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();

            return json.has("selling_partner_id") ? json.get("selling_partner_id").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    public String getSellerId(String accessToken) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://sellingpartnerapi-na.amazon.com/sellers/v1/marketplaceParticipations");

            conn = openAmazonConnection(url, accessToken);
            conn.setRequestProperty("Accept", "application/json");

            int response = conn.getResponseCode();
            InputStream inputStream = (response >= 200 && response < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String responseBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[SP-API] marketplaceParticipations response: " + responseBody);

            if (response < 200 || response >= 300) {
                System.out.println("[SP-API] Failed to fetch marketplace participations. HTTP " + response);
                return null;
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!json.has("payload") || !json.get("payload").isJsonArray()) {
                System.out.println("[SP-API] No participations found in payload.");
                return null;
            }

            JsonArray payloadArray = json.getAsJsonArray("payload");

            for (JsonElement elem : payloadArray) {
                if (!elem.isJsonObject()) continue;
                JsonObject participationObj = elem.getAsJsonObject();

                if (participationObj.has("marketplace") && participationObj.get("marketplace").isJsonObject()) {
                    JsonObject marketplace = participationObj.getAsJsonObject("marketplace");
                    String marketplaceId = marketplace.has("id") ? marketplace.get("id").getAsString() : null;

                    if (!"ATVPDKIKX0DER".equals(marketplaceId)) continue;

                    System.out.println("[SP-API] Participation found for US marketplace, but sellerId is no longer provided.");
                    return null;
                }
            }

            System.out.println("[SP-API] US Seller ID not found in participations.");
            return null;

        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    public AccessTokenResponse getWalmartAccessToken(String clientId, String clientSecret) {
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
            conn.setRequestProperty("WM_SVC.VERSION", "1.0.0");


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
                return new AccessTokenResponse(code,null);
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

            if(responseBody.equals("Bad Request")){
                return new AccessTokenResponse(code,null);
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (json.has("access_token")) {
                System.out.println("[Walmart] Received token response.");

            } else {
                System.out.println("[Walmart] ERROR: access_token not found in response.");
                return new AccessTokenResponse(code,null);

            }
            String token = json.get("access_token").getAsString();
            System.out.println("[Walmart] Access token retrieved successfully (len=" + token.length() + ")");
            return new AccessTokenResponse(code,token);

        } catch (Exception e) {
            System.out.println("[Walmart] EXCEPTION while getting token: " + e);
            System.out.println(Arrays.toString(e.getStackTrace()));
            return new AccessTokenResponse(-1,null);

        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    //If we have the access token the just try the token
    public int validateAmazonAccessToken(String accessToken){
        System.out.println("[BEGIN VALIDATING ACCESS TOKEN]");
        try{
            System.out.println("-> Opening SP-API connection...");
            URL url = new URL("https://sellingpartnerapi-na.amazon.com/sellers/v1/marketplaceParticipations");
            HttpURLConnection conn = openAmazonConnection(url, accessToken);

            System.out.println("-> Attempting to get response code");
            int response = conn.getResponseCode();
            String responseBody = conn.getResponseMessage();
            System.out.println("-> Response code: " + response);
            System.out.println("-> Response body: " + responseBody);

            if(!(response >= 200 && response < 300)){ //If bad response
                StringBuilder sb = new StringBuilder();
                System.out.println("-> Reading error response body");
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        sb.append(line);
                    }
                }catch (Exception e) {
                    System.out.println("-> Could not read error stream: " + e.getMessage());
                }
                System.out.println("-> Error response: " + sb);
            }

            conn.disconnect();
            System.out.println("[VALIDATING ACCESS TOKEN END] Response code: " + response);

            if(response >= 200 && response < 300) {
                System.out.println("Success. Good HTTP response");
            }else{
                System.out.println("ERROR. Bad HTTP response");
            }
            return response;
        }catch (Exception e) {
            System.out.println("ERROR: "+e.getMessage());
        }
        return -1;
    }
    public int validateWalmartAccessToken(String accessToken, String clientID){
        System.out.println("[BEGIN VALIDATING ACCESS TOKEN]");
        try{

            HttpURLConnection conn;
            System.out.println("-> Opening Walmart API connection...");
            URL url = new URL("https://marketplace.walmartapis.com/v3/items");
            conn = (HttpURLConnection) url.openConnection();

            System.out.println("-> Setting request method...");
            conn.setRequestMethod("GET");

            System.out.println("-> Adding headers...");

            addWalmartHeaders(conn,accessToken, clientID);

            System.out.println("->Setting timeouts");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            System.out.println("-> Attempting to get response code");
            int response = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();
            System.out.println("-> Response code: " + response);
            System.out.println("-> Response message: " + responseMessage);


            if(!(response >= 200 && response < 300)){
                StringBuilder sb = new StringBuilder();
                System.out.println("-> Reading error response body");
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        sb.append(line);
                    }
                }catch (Exception e) {
                    System.out.println("-> Could not read error stream: " + e.getMessage());
                }
                System.out.println("-> Error response: " + sb);
            }

            conn.disconnect();
            System.out.println("[VALIDATING ACCESS TOKEN END] Response code: " + response);

            if(response >= 200 && response < 300) {
                System.out.println("Success. Good HTTP response");
            }else{
                System.out.println("ERROR. Bad HTTP response");
            }
            return response;
        }catch (Exception e) {
            System.out.println("Exception during access token validation: " + e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return -1;
    }
    public void addWalmartHeaders(HttpURLConnection conn, String accessToken, String clientID){

        conn.setRequestProperty("WM_SEC.ACCESS_TOKEN", accessToken);
        conn.setRequestProperty("WM_CONSUMER.CHANNEL.TYPE", clientID);
        conn.setRequestProperty("WM_SVC.NAME", "Walmart Marketplace");
        conn.setRequestProperty("WM_QOS.CORRELATION_ID", UUID.randomUUID().toString());
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
    }

    public HttpURLConnection openAmazonConnection(String urlStr,String accessToken) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-amz-access-token", accessToken);
        conn.setRequestProperty("User-Agent", "InventoryApp/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }
    public HttpURLConnection openAmazonConnection(URL url,String accessToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-amz-access-token", accessToken);
        conn.setRequestProperty("User-Agent", "InventoryApp/1.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }
    public static String getStorageDir(PlatformType platform) {
        return BASE_DIR + File.separator + platform.getDisplayName().toLowerCase();
    }

    public static String getTokenFilePath(PlatformType platform) {
        return getStorageDir(platform) + File.separator + "token.enc";
    }

    public void checkBadKeys(List<BaseSeller> sellers){
        for(BaseSeller seller : sellers){
            if (seller.isBadKey()){
                removeToken(sellerToPlatform(seller));
                seller.resetBadKeyCount();
            }
        }
    }
    public PlatformType sellerToPlatform(BaseSeller seller) {
        if (seller instanceof AmazonSeller) {
            return PlatformType.AMAZON;
        } else if (seller instanceof EbaySeller) {
            return PlatformType.EBAY;
        } else if (seller instanceof WalmartSeller) {
            return PlatformType.WALMART;
        } else {
            System.out.println("sellerToPlatform called on seller that has no platform");
            return null;
        }
    }

    public void clearPassword() {
        password = null;
    }

    public record AccessTokenResponse(int response, String accessToken){};
}
