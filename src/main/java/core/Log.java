package core;
import java.time.LocalDateTime;


public class Log {

    public enum Severity {
        Normal,
        Warning,
        Critical,
    }
    public enum LogType{
        ItemSold, //Sold
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
        ItemRemoved,
        ItemBrokenDown,
        ItemComposed,
        LogSuppressed,
        LogSolved,
    }

    private static int nextLogID = 1; // Auto-increment
    private final int logID;
    private final Integer amount; //Used to revert logs. Example: New log added two items, then it was reverted. This tells the new log that the amount added was two
    private String serial; //Same as amount but for the serial number of the item. If this is for an item that does not exist then it would be the sku
    private final LocalDateTime timestamp;
    private final LogType type;
    private final Severity severity;
    private String message;

    //For Alert Logs
    private boolean suppressed; //For warning, user is aware but doesn't care it could be suppressed
                                //ItemSoldAndNotRegistered is a critical log but acts more like a warning, for this reason
                                //those logs cant be suppressed, only solved.

    private boolean solved;    //For warning and critical logs, user has resolved the issue in another way.
    //For example if an item is sold and out of stock and the user cancels the order then it is marked as resolved.
    //This cannot be done for Low/OutOfStock logs. These must be removed in the item themselves.
    //Log will no longer exist if it is solved.
    //Log will automatically be set as solved if the condition that created it is no longer true.

    //For normal logs (Sold and Added)
    private final boolean allowRevert;

    //For reverter log
    private Integer revertedLogID; //Which log did this one revert?

    //For logs that have been reverted
    private Integer revertedByLogId; //Which log reverted this one?

    private boolean reverter; //If this log was created to revert a previous log
    private boolean reverted; //If a log is reverted, then it will be displayed but ignored in processing

    public Log(LogType Type, Integer Amount, String Message, String Serial) {
        logID = nextLogID++;
        timestamp = LocalDateTime.now();
        amount = Amount;
        type = Type;
        message = Message;
        serial = Serial;
        reverter = false;
        reverted = false;

        // Automatically determine severity based on type
        severity = determineSeverity(type);

        // Allow revert only if normal and type is Sold or Added
        allowRevert = (severity == Severity.Normal &&
                (type == LogType.ItemSold || type == LogType.ItemAdded));

    }
    // Constructor for reverter logs
    public Log(LogType Type, Integer Amount, String Message, String ItemSerial, boolean Reverter, int RevLID) {
        this(Type, Amount, Message, ItemSerial);
        reverter = Reverter; //Bool
        revertedLogID = RevLID;

        //Other log set by logManager
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
    public String getTimestamp() {
        return timestamp.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));
    }
    public String getTime() {
        return getTimestamp();
    }
    public LogType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getSerial() { return serial; }
    public boolean isReverter() { return reverter; }
    public boolean isReverted() { return reverted; }
    public boolean isSuppressed() {return suppressed;}
    public boolean isSolved() {return solved;}
    public boolean canRevert() { return allowRevert && !reverted; }

    // --------------------------- Setters ---------------------------
    public void setMessage(String message) { this.message = message; }
    public void setSerial(String itemSerial) { this.serial = itemSerial; }
    public void setReverted(boolean reverted) { this.reverted = reverted; }
    public void setReverter(boolean reverter) { this.reverter = reverter; }
    public void setSolved(boolean solved) {this.solved = solved;}
    public void setSuppressed(boolean suppressed) {this.suppressed = suppressed;}
    public void setRevertedLogID(int id){ revertedLogID = id;}


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
        if(severity !=Severity.Normal){
            return new SerialAndAmount(null,0);
        }
        return switch (type) {
            case ItemSold -> new SerialAndAmount(serial, +amount);
            case ItemAdded -> new SerialAndAmount(serial, -amount);
            default -> new SerialAndAmount(null, 0);
        };
    }
    public static SerialAndAmount getLogInverse(Log l) {
        if (l == null || l.severity != Severity.Normal) {
            return new SerialAndAmount(null, 0);
        }

        return switch (l.type) {
            case ItemSold -> new SerialAndAmount(l.serial, +l.amount);
            case ItemAdded -> new SerialAndAmount(l.serial, -l.amount);
            default -> new SerialAndAmount(null, 0);
        };
    }
}

