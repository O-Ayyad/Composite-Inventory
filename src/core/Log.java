package core;
import java.time.LocalDateTime;

public class Log {
    public enum Severity {
        Normal,
        Warning,
        Critical,
        Solved
    }
    public enum LogType{
        ItemSold, //Sold
        ItemAdded, //Item added
        ItemUpdated, //Item name/serial number changed
        LowStock, //Item quantity lower than stock floor limit
        ItemOutOfStock,
        ItemSoldAndOutOfStock, //If sold on amazon/ebay and out of stock
        NewItemCreated,
        ItemRemoved,
    }

    private static int nextLogID = 1; // Auto-increment
    private int logID;
    private Integer amount; //Used to revert logs. Example: New log added two items, then it was reverted. This tells the new log that the amount added was two
    private String serial; //Same as amount but for the serial number of the item
    private LocalDateTime timestamp;
    private LogType type;
    private Severity severity;
    private String message;
    private String itemSerial; // reference to affected item

    //For Alert Logs
    private boolean alert;
    private boolean suppressed; //For warning and critical logs, user is aware but doesn't care it could be suppressed
    private boolean solved; // //For warning and criticallogs, user has solved the issue in another way.
    //For example if an item is sold and out of stock and the user cancels the order then it is marked as solved.
    //Log will still exist but for all purposes it will not  be visible nor affect anything
    //Log will automatically be set as solved if the condition is no longer true.

    //For normal logs (Sold and Added)
    private boolean allowRevert;
    private boolean reverter; //If this log was created to revert a previous log
    private boolean reverted; //If a log is reverted, then it will be displayed but ignored in processing

    public Log(LogType type, Integer amount, String message, String itemSerial) {
        this.logID = nextLogID++;
        this.timestamp = LocalDateTime.now();
        this.amount = amount;
        this.type = type;
        this.message = message;
        this.itemSerial = itemSerial;
        this.reverter = false;
        this.reverted = false;

        // Automatically determine severity based on type
        this.severity = determineSeverity(type);

        // Allow revert only if normal and type is Sold or Added
        this.allowRevert = (severity == Severity.Normal &&
                (type == LogType.ItemSold || type == LogType.ItemAdded));

        // Alert if it's not revertable and type indicates a problem
        this.alert = !allowRevert && (
                type == LogType.LowStock ||
                        type == LogType.ItemOutOfStock ||
                        type == LogType.ItemSoldAndOutOfStock
        );
    }
    // Constructor for reverter logs
    public Log(LogType type, Integer amount, String message, String itemSerial, boolean reverter) {
        this(type, amount, message, itemSerial);
        this.reverter = reverter;
    }
    //Helper for constructors
    private Severity  determineSeverity(LogType type) {
        return switch (type) {
            case ItemSold, ItemUpdated, ItemRemoved, NewItemCreated, ItemAdded -> Severity.Normal;
            case LowStock, ItemOutOfStock -> Severity.Warning;
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

    public void setSuppressed(boolean suppressed) {this.suppressed = suppressed;}

    // --------------------------- Setters ---------------------------
    public void setMessage(String message) { this.message = message; }
    public void setItemSerial(String itemSerial) { this.itemSerial = itemSerial; }
    public void setReverted(boolean reverted) { this.reverted = reverted; }
    public void setReverter(boolean reverter) { this.reverter = reverter; }
    public boolean isSolved() {return solved;}
    public void setSolved(boolean solved) {this.solved = solved;}


    // --------------------------- Methods ---------------------------
    //A primitive item packet that only holds the serial number
    public static class SerialAndAmount{
        private final String serialNumber;
        private int quantity;

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
            case ItemSold -> new SerialAndAmount(serial, amount);
            case ItemAdded -> new SerialAndAmount(serial, -amount);
            default -> new SerialAndAmount(null, 0);
        };
    }
    public static SerialAndAmount getLogInverse(Log l) {
        if (l == null || l.severity != Severity.Normal) {
            return new SerialAndAmount(null, 0);
        }

        return switch (l.type) {
            case ItemSold -> new SerialAndAmount(l.serial, l.amount);
            case ItemAdded -> new SerialAndAmount(l.serial, -l.amount);
            default -> new SerialAndAmount(null, 0);
        };
    }
}

