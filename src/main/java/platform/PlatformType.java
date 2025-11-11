package platform;

import java.io.File;

public enum PlatformType {
    AMAZON("Amazon"),
    EBAY("eBay"),
    WALMART("Walmart");

    private final String displayName;

    PlatformType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}