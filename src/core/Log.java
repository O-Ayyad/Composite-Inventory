package core;
import javax.swing.*;
import java.time.LocalDateTime;

public class Log {
    public enum Severity {
        Normal,
        Warning,
        Critical,
    }
    public enum LogType{
        ItemSold, //Sold
        ItemAdded, //Item added
        ItemUpdated, //Item name/picture/SKU/Composed changed
        LowStock, //Item quantity lower than stock floor trigger
        ItemOutOfStock, //If an item has lowStockTrigger and the amount in inventory is 0 then this log is created
        ItemSoldAndOutOfStock, //If sold on amazon/ebay and out of stock. Only critical Log
        ItemSoldAndNotListedOnPlatforms, //If an item is sold on amazon and ebay but not linked then log that
        NewItemCreated,
        ItemRemoved,
        ItemBrokenDown,
        ItemComboCreated;
    }

    private static int nextLogID = 1; // Auto-increment
    private final int logID;
    private final Integer amount; //Used to revert logs. Example: New log added two items, then it was reverted. This tells the new log that the amount added was two
    private String itemSerial; //Same as amount but for the serial number of the item
    private final LocalDateTime timestamp;
    private final LogType type;
    private final Severity severity;
    private String message;

    //For Alert Logs
    private boolean alert;
    private boolean suppressed; //For warning and critical logs, user is aware but doesn't care it could be suppressed
    private boolean solved; // //For warning and critical logs, user has resolved the issue in another way.
    //For example if an item is sold and out of stock and the user cancels the order then it is marked as resolved.
    //This cannot be done for Low/OutOfStock logs. These must be removed in the item themselves.
    //Log will no longer exist if it is solved.
    //Log will automatically be set as solved if the condition that created it is no longer true.

    //For normal logs (Sold and Added)
    private boolean allowRevert;
    private Integer revertedLogID = null; //Which log did this one revert?
    private Integer revertedByLogId = null; //Which log reverted this one?
    private boolean reverter; //If this log was created to revert a previous log
    private boolean reverted; //If a log is reverted, then it will be displayed but ignored in processing

    public Log(LogType Type, Integer Amount, String Message, String ItemSerial) {
        logID = nextLogID++;
        timestamp = LocalDateTime.now();
        amount = Amount;
        type = Type;
        message = Message;
        itemSerial = ItemSerial;
        reverter = false;
        reverted = false;

        // Automatically determine severity based on type
        severity = determineSeverity(type);

        // Allow revert only if normal and type is Sold or Added
        allowRevert = (severity == Severity.Normal &&
                (type == LogType.ItemSold || type == LogType.ItemAdded));

        // Alert if it's not revertible and type indicates a problem
        alert = !allowRevert && (
                type == LogType.LowStock ||
                        type == LogType.ItemOutOfStock ||
                        type == LogType.ItemSoldAndOutOfStock
        );
    }
    // Constructor for reverter logs
    public Log(LogType Type, Integer Amount, String Message, String ItemSerial, boolean Reverter, int RevLID) {
        this(Type, Amount, Message, ItemSerial);
        reverter = Reverter; //Bool
        revertedLogID = RevLID;
    }
    //Helper for constructors
    private Severity  determineSeverity(LogType type) {
        return switch (type) {
            case ItemSold, ItemUpdated, ItemRemoved, NewItemCreated, ItemAdded, ItemBrokenDown, ItemComboCreated -> Severity.Normal;
            case LowStock, ItemOutOfStock,ItemSoldAndNotListedOnPlatforms -> Severity.Warning;
            case ItemSoldAndOutOfStock -> Severity.Critical;
        };
    }

    // --------------------------- Getters ---------------------------
    public int getLogID() { return logID; }
    public Integer getAmount() {return amount;}
    public LocalDateTime getTimestamp() { return timestamp; }
    public LogType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getItemSerial() { return itemSerial; }
    public boolean isReverter() { return reverter; }
    public boolean isReverted() { return reverted; }
    public boolean isSuppressed() {return suppressed;}
    public boolean isSolved() {return solved;}

    // --------------------------- Setters ---------------------------
    public void setMessage(String message) { this.message = message; }
    public void setItemSerial(String itemSerial) { this.itemSerial = itemSerial; }
    public void setReverted(boolean reverted) { this.reverted = reverted; }
    public void setReverter(boolean reverter) { this.reverter = reverter; }
    public void setSolved(boolean solved) {this.solved = solved;}
    public void setSuppressed(boolean suppressed) {this.suppressed = suppressed;}


    // --------------------------- Methods ---------------------------
    //A primitive item packet that only holds the serial number
    public static class SerialAndAmount{
        private final String serialNumber;
        private final int quantity;

        public SerialAndAmount(String serialNumber, int quantity) {
            this.serialNumber = serialNumber;
            this.quantity = quantity;
        }
        public String getSerialNumber() {
            return serialNumber;
        }
        public int getQuantity() {
            return quantity;
        }
    }
    public SerialAndAmount getLogInverse() {
        if(this.severity !=Severity.Normal){
            return new SerialAndAmount(null,0);
        }
        return switch (this.type) {
            case ItemSold -> new SerialAndAmount(itemSerial, +amount);
            case ItemAdded -> new SerialAndAmount(itemSerial, -amount);
            default -> new SerialAndAmount(null, 0);
        };
    }
    public static SerialAndAmount getLogInverse(Log l) {
        if (l == null || l.severity != Severity.Normal) {
            return new SerialAndAmount(null, 0);
        }

        return switch (l.type) {
            case ItemSold -> new SerialAndAmount(l.itemSerial, +l.amount);
            case ItemAdded -> new SerialAndAmount(l.itemSerial, -l.amount);
            default -> new SerialAndAmount(null, 0);
        };
    }
    //Used to manage logs in a separate window
    public JPanel openLogWindow(){
        return null;
    }
}

