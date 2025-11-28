package storage;

import com.google.gson.reflect.TypeToken;
import core.*;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    @Override
    public void load() {
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
                Type quantitiesType = new TypeToken<Map<String, Integer>>(){}.getType();
                quantities = gson.fromJson(quantitiesReader, quantitiesType);
            }

            if (items != null && quantities != null) {
                //Get every item and store into inventory
                for (Map.Entry<String, Item> entry : items.entrySet()) {
                    Item item = entry.getValue();
                    Integer quantity = quantities.getOrDefault(entry.getKey(), 0);
                    if(item.getIcon(64) == null){
                        item.setImagePath("src/resources/icons/itemIcons/imageNotFound.png");
                    }
                    inventory.createItemFromSave(item,quantity);
                }
                System.out.println("SUCCESS: Loaded " + items.size() + " items from storage");
            }

        } catch (FileNotFoundException e) {
            System.out.println("INFO: Inventory files not found. Starting with empty inventory.");
        } catch (Exception e) {
            System.out.println("ERROR: Could not load inventory");
            System.out.println(e.getMessage() + Arrays.toString(e.getStackTrace()));
        }finally {
            loading = false;
        }
        System.out.println("Loaded inventory from: " + itemsPath.toString());
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

        } catch (IOException e) {
            System.out.println("ERROR: Could not save inventory");
            System.out.println(e.getMessage());
        }
    }
}
