package storage;

import com.google.gson.reflect.TypeToken;
import core.*;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;


///Two files:
/// items.json stores the serial number of the item and the item information
/// quantities.json stores the serial and the quantity
public class InventoryFileManager extends AbstractFileManager{

    public static final String ITEMS_FILENAME = "items.json";
    public static final String QUANTITIES_FILENAME = "quantities.json";

    public final String itemDetailsFilePath = dataDir + File.separator + ITEMS_FILENAME;
    public final String itemQuantitiesFilePath = dataDir + File.separator + QUANTITIES_FILENAME;

    Inventory inventory;

    public InventoryFileManager(Inventory inventory, String dataDirName){
        super(dataDirName);
        this.inventory = inventory;
    }
    public Path getItemDetailsFilePath() {
        return Path.of(itemDetailsFilePath);
    }

    public Path getItemQuantitiesFilePath() {
        return Path.of(itemQuantitiesFilePath);
    }

    public LoadResult load(boolean firstOpen) {
        loading = true;
        Path itemsPath = getItemDetailsFilePath();
        Path quantitiesPath = getItemQuantitiesFilePath();
        try {
            //Get all items
            Map<String, Item> items;
            try (FileReader itemsReader = new FileReader(itemsPath.toFile())) {
                Type itemsType = new TypeToken<Map<String, Item>>(){}.getType();
                items = gson.fromJson(itemsReader, itemsType);
            }
            //Get their quantity and store with serial
            Map<String, Integer> quantities;
            try (FileReader quantitiesReader = new FileReader(quantitiesPath.toFile())) {
                Type quantitiesType = new TypeToken<Map<String, Integer>>() {
                }.getType();
                quantities = gson.fromJson(quantitiesReader, quantitiesType);

                if (items != null && quantities != null) {
                    //Get every item and store into inventory
                    for (Map.Entry<String, Item> entry : items.entrySet()) {
                        Item item = entry.getValue();
                        Integer quantity = quantities.getOrDefault(entry.getKey(), 0);
                        item.checkMissingIcon();

                        inventory.createItemFromSave(item, quantity);
                    }
                    System.out.println("SUCCESS: Loaded " + items.size() + " items from storage");
                }
                inventory.convertComposedSerialToItem();
            }
        } catch (Exception e) {
            if(firstOpen) {
                System.out.println("ERROR: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
                showError("ERROR: Could not load inventory." + e.getMessage() + "\n " +
                        "Load a working backup", firstOpen);
            }
            return new LoadResult(false, e);
        }finally {
            loading = false;
        }
        System.out.println("Loaded inventory from: " + itemsPath);
        return new LoadResult(true, null);
    }


    //Saves the whole inventory
    @Override
    public void save() {
        if(loading) return;
        Path itemDetailsFile = getItemDetailsFilePath();
        Path itemQuantitiesFile = getItemQuantitiesFilePath();

        try {
            //Write item details
            try (FileWriter writer = new FileWriter(itemDetailsFile.toFile())) {
                gson.toJson(inventory.SerialToItemMap, writer);
            }

            //Write item quantities
            try (FileWriter writer = new FileWriter(itemQuantitiesFile.toFile())) {

                HashMap<String, Integer> serialToQuantity = new HashMap<>();
                for (Map.Entry<Item, Integer> entry : inventory.MainInventory.entrySet()) {
                    serialToQuantity.put(entry.getKey().getSerial(), entry.getValue());
                }

                gson.toJson(serialToQuantity, writer);
            }

        } catch (Exception e) {
            showError("ERROR: Could not save inventory. "+ e.getMessage(),false);
        }
    }
}
