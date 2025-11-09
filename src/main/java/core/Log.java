package core;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.time.LocalDateTime;


public class Log {

    public enum Severity {
        Normal,
        Warning,
        Critical,
    }
    public enum LogType{
        ItemRemoved, //Sold
        ItemSoldOnPlatform, //Item order came in but not shipped yet
        ItemShippedOnPlatform, //Item was shipped out so reduce stock on the item in inventory
        ItemSoldViaComposition,
        OrderCancelled,
        ItemAdded, //Item added
        ItemUpdated, //Item name/picture/SKU/Composed changed
        LowStock, //Item quantity lower than stock floor trigger
        ItemOutOfStock, //If an item has lowStockTrigger and the amount in inventory is 0 then this log is created
        ItemSoldAndOutOfStock, //If sold on amazon/ebay and out of stock.
        ItemSoldAndNotRegisteredInInventory, //If an item is sold on amazon and ebay but not linked then log that
        NewItemCreated,
        ItemDeleted,
        ItemBrokenDown,
        ItemComposed,
        LogSuppressed,
        LogSolved,
    }
    @Expose
    private final int logID;
    @Expose
    private final Integer amount; //This tells the exact action that was taken in quantity
    @Expose
    private String serial; //Same as amount but for the serial number of the item. If this is for an item that does not exist then it would be the sku that was used to find the item
    @Expose
    private final String  timestamp;
    @Expose
    private final LogType type;
    @Expose
    private final Severity severity;
    @Expose
    private String message;

    //For Alert Logs
    @Expose
    private boolean suppressed; //For warning, user is aware but doesn't care it could be suppressed
                                //ItemSoldAndNotRegistered is a critical log but acts more like a warning, for this reason
                                //those logs cant be suppressed, only solved.


    public Log(LogType type, Integer amount, String message, String serial, int logID) {
        this.logID = logID;
        timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));
        this.amount = amount;
        this.type = type;
        this.message = message;
        this.serial = serial;
        // Automatically determine severity based on type
        severity = determineSeverity(type);
    }

    //Helper for constructors
    private Severity  determineSeverity(LogType type) {
        return switch (type) {
            case LowStock, ItemOutOfStock, ItemSoldViaComposition -> Severity.Warning;
            case ItemSoldAndNotRegisteredInInventory, ItemSoldAndOutOfStock -> Severity.Critical;
            default -> Severity.Normal;
        };
    }

    // --------------------------- Getters ---------------------------
    public int getLogID() { return logID; }
    public Integer getAmount() {return amount;}
    public String getTime() {return timestamp;}
    public LogType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getSerial() { return serial; }
    public boolean isSuppressed() {return suppressed;}

    // --------------------------- Setters ---------------------------
    public void setMessage(String message) { this.message = message; }
    public void setSerial(String itemSerial) { this.serial = itemSerial; }
    public void setSuppressed(boolean suppressed) {this.suppressed = suppressed;}


    @Override
    public String toString(){
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }
}

