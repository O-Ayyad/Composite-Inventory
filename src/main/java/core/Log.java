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
        SystemError,

        OrderReceived,                           // Normal order received
        OrderReceivedItemOutOfStock,                 // Order received, item out of stock
        OrderReceivedItemSellsViaComposition,          // Order received, fulfilled via components
        OrderReceivedItemNotRegistered,              // Order received, item not in inventory system

        ItemShipped,                             // Item shipped, stock reduced
        ItemShippedOutOfStock,                   // Item shipped but was out of stock (How?)
        ItemShippedViaComposition,               // Shipped using component items
        ItemShippedNotRegistered,                // Item shipped but not in inventory system


        OrderCancelled,                          // Order cancelled


        ReducedStock,                            // General stock reduction
        AddedItem,                               // Item added to inventory
        CreatedNewItem,                          // New item created
        UpdatedItem,                             // Item details modified
        DeletedItem,                             // Item removed from inventory
        ComposedItem,                            // Item assembled from components
        BrokenDownItem,                          // Item disassembled into components

        LowStock,                                // Stock below threshold
        ItemOutOfStock,                          // Stock at zero


        SuppressedLog,                           // Log hidden/suppressed
        SolvedLog,                               // Issue resolved
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
        LocalDateTime timeTemp = LocalDateTime.now();
        timestamp = timeTemp.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss a"));
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
            case OrderReceivedItemNotRegistered,
                 OrderReceivedItemOutOfStock,
                 ItemShippedNotRegistered,
                 ItemShippedOutOfStock,
                 SystemError -> Severity.Critical;

            case LowStock,
                 ItemOutOfStock,
                 OrderReceivedItemSellsViaComposition -> Severity.Warning;

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

