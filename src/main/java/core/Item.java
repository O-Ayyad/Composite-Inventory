package core;
import java.awt.*;
import java.util.*;
import javax.swing.ImageIcon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
//Blueprint for all items
//Each item serial number is unique and the quantity is stored by inventory manager
public class Item {
    @Expose
    private String name;
    @Expose
    private final String serial; //Unique for every item. No two items can have the same serial number
    @Expose
    private Integer lowStockTrigger; // Creates a log when the quantity of the item is equal or lower than this

    @Expose
    private String walmartSellerSKU; //Used to connect to accounts
    @Expose
    private String amazonSellerSKU;
    @Expose
    private String ebaySellerSKU;

    @Expose
    private final Map<String, Integer> composedOfSerialized; //Used for saving and loading

    private transient Map<Item,Integer> composedOf;
    private final transient Set<Item> composesInto = new HashSet<>();

    @Expose
    private String iconPath;   //path to image file

    private transient final ItemManager itemManager;

    //-------------------------------<Getters and Setters>-------------------------------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSerial() { return serial; }

    public String getWalmartSellerSKU() {return walmartSellerSKU;}
    public void setWalmartSellerSKU(String walmartSellerSKU) {this.walmartSellerSKU = walmartSellerSKU;}

    public String getAmazonSellerSKU() {return amazonSellerSKU;}
    public void setAmazonSellerSKU(String amazonSellerSKU) {this.amazonSellerSKU = amazonSellerSKU;}

    public String getEbaySellerSKU() {return ebaySellerSKU;}
    public void setEbaySellerSKU(String ebaySellerSKU) {this.ebaySellerSKU = ebaySellerSKU;}

    public Map<Item, Integer> getComposedOf() { return composedOf; }
    public Set<Item> getComposesInto() { return composesInto; }

    public Map<String,Integer> getComposedOfSerialized() { return composedOfSerialized; }

    public String getImagePath() {
        return iconPath;
    }
    public void setImagePath(String newPath) {
        iconPath = newPath;
    }
    public ImageIcon getIcon(int maxSize) {

        ImageIcon icon = new ImageIcon(iconPath);
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        double scale = (double) maxSize / Math.max(w, h);

        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        Image scaled = icon.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
    public int getLowStockTrigger() { return lowStockTrigger;}
    public void setLowStockTrigger(int lowST){lowStockTrigger = lowST;}


    //-------------------------------</Getters and Setters>-------------------------------




    //-------------------------------<Edit Composition>-------------------------------
    //Can add sole item or multiple at once
    public void replaceComposedOf(Map<Item,Integer> newList) {
        if (newList == null) return;

        Map<Item,Integer> valid = new HashMap<>();
        for (Map.Entry<Item,Integer> entry : newList.entrySet()) {

            if (entry == null) continue;

            Item component = entry.getKey();
            Integer qty = entry.getValue();

            if (component  == null) {
                System.out.println("ItemPacket has a null item. Skipping.");
                continue;
            }
            if (component .equals(this)) {
                System.out.println("Cannot compose with itself: " + component .getName() + " (Serial: " + component .getSerial() + ")");
                continue;
            }
            valid.merge(component ,qty, Integer::sum);
        }

        composedOf = valid;
        for(Map.Entry<Item, Integer> e : composedOf.entrySet()){
            composedOfSerialized.put(
                    e.getKey().serial,
                    e.getValue());
        }
        syncCompositionDependencies();
    }
    public boolean isComposite(){ return !composedOf.isEmpty();}
    //-------------------------------</Edit Composition>-------------------------------



    //-------------------------------<Constructor>-------------------------------

    //Duplicate item check is checked before creation
    public Item(String name, String serialNum, Integer lowStockTrigger,
                Map<Item, Integer> composedOf,
                String iconPath,
                ItemManager itemManager,
                String amazonSellerSKU,
                String ebaySellerSKU,
                String walmartSellerSKU) {
        this.name = name;
        this.serial = serialNum;
        this.lowStockTrigger = lowStockTrigger;
        this.composedOf = composedOf != null ? composedOf : new HashMap<>();

        composedOfSerialized = new HashMap<>();
        if(composedOf != null){
            for(Map.Entry<Item, Integer> e : composedOf.entrySet()){
                composedOfSerialized.put(
                        e.getKey().serial,
                        e.getValue());
            }
        }
        this.iconPath = iconPath;

        this.amazonSellerSKU = amazonSellerSKU != null ? amazonSellerSKU : "";
        this.ebaySellerSKU = ebaySellerSKU != null ? ebaySellerSKU : "";
        this.walmartSellerSKU = walmartSellerSKU != null ? walmartSellerSKU : "";

        this.itemManager = itemManager;

        //Create dependencies of composed of and composes into
        syncCompositionDependencies();
    }
    //If item A holds item B composedOf then item B should hold Item A in composed into
    //This should be called on the composite item. The kit/combo
    public void syncCompositionDependencies(){
        if (itemManager == null) throw new IllegalStateException("Item Manager is null for item: "+ getName());
        //Make sure all components of this item know what they compose into
        for (Item component : composedOf.keySet()) {
            if (component != null && component != this) {
                component.getComposesInto().add(this);
            }
        }
    }



    //-------------------------------<Overrides>-------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return serial != null && serial.equals(item.serial);
    }
    @Override
    public int hashCode() {
        return serial != null ? serial.hashCode() : 0;
    }

    @Override
    public String toString(){
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }
    //-------------------------------</Overrides>-------------------------------
}
