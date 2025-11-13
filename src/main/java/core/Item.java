package core;
import java.awt.*;
import java.util.ArrayList;
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
    private final String serialNum; //Unique for every item. No two items can have the same serial number
    @Expose
    private Integer lowStockTrigger; // Creates a log when the quantity of the item is equal or lower than this

    @Expose
    private String walmartSellerSKU; //Used to connect to accounts
    @Expose
    private String amazonSellerSKU;
    @Expose
    private String ebaySellerSKU;

    @Expose
    private ArrayList<ItemPacket> composedOf = new ArrayList<>();
    private ArrayList<Item> composesInto = new ArrayList<>();

    @Expose
    private String iconPath;   //path to image file

    private final ItemManager itemManager;

    //-------------------------------<Getters and Setters>-------------------------------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSerialNum() { return serialNum; }

    public String getWalmartSellerSKU() {return walmartSellerSKU;}
    public void setWalmartSellerSKU(String walmartSellerSKU) {this.walmartSellerSKU = walmartSellerSKU;}

    public String getAmazonSellerSKU() {return amazonSellerSKU;}
    public void setAmazonSellerSKU(String amazonSellerSKU) {this.amazonSellerSKU = amazonSellerSKU;}

    public String getEbaySellerSKU() {return ebaySellerSKU;}
    public void setEbaySellerSKU(String ebaySellerSKU) {this.ebaySellerSKU = ebaySellerSKU;}

    public ArrayList<ItemPacket> getComposedOf() { return composedOf; }
    public ArrayList<Item> getComposesInto() { return composesInto; }

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
    public void replaceComposedOf(ArrayList<ItemPacket> newList) {
        if (newList == null) return;

        ArrayList<ItemPacket> valid = new ArrayList<>();
        for (ItemPacket ip : newList) {
            if (ip == null) {
                System.out.println("Skipping null ItemPacket.");
                continue;
            }
            Item item = ip.getItem();
            if (item == null) {
                System.out.println("ItemPacket has a null item. Skipping.");
                continue;
            }
            if (item.equals(this)) {
                System.out.println("Cannot compose with itself: " + item.getName() + " (Serial: " + item.getSerialNum() + ")");
                continue;
            }
            valid.add(ip);
        }

        composedOf.clear();
        composedOf.addAll(valid);
        syncCompositionDependencies();
    }
    public boolean isComposite(){ return !composedOf.isEmpty();}
    public Boolean isComposedOf(Item item){
        for(ItemPacket ip : composedOf){
            if(ip.getItem().equals(item)) {
                return true;
            }
        }
        return false;
    }
    //-------------------------------</Edit Composition>-------------------------------



    //-------------------------------<Constructor>-------------------------------

    //Duplicate item check is checked before creation
    public Item(String name, String serialNum, Integer lowStockTrigger,
                ArrayList<ItemPacket> composedOf,
                String iconPath,
                ItemManager itemManager,
                String amazonSellerSKU,
                String ebaySellerSKU,
                String walmartSellerSKU) {
        this.name = name;
        this.serialNum = serialNum;
        this.lowStockTrigger = lowStockTrigger;
        this.composedOf = composedOf != null ? composedOf : new ArrayList<>();
        this.iconPath = iconPath;

        this.amazonSellerSKU = amazonSellerSKU != null ? amazonSellerSKU : "";
        this.ebaySellerSKU = ebaySellerSKU != null ? ebaySellerSKU : "";
        this.walmartSellerSKU = walmartSellerSKU != null ? walmartSellerSKU : "";

        this.itemManager = itemManager;

        //Create dependencies of composed of and composes into
        syncCompositionDependencies();
    }
    //If item A holds item B composedOf then item B should hold Item A in composed into
    //This should be called on the item that is composed of other items. The kit/combo
    public void syncCompositionDependencies(){
        if (itemManager == null) throw new IllegalStateException("Item Manager is null for item: "+ getName());
        //Make sure all components of this item know what they compose into
        for (ItemPacket IP : this.composedOf) {
            Item component = IP.getItem();
            if (component == null || component.equals(this)) continue;
            boolean exists = false;
            for (Item ci : component.getComposesInto()) {
                if (ci.equals(this)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                component.getComposesInto().add(this);
            }
        }
    }
    //-------------------------------</Constructor>-------------------------------


    //-------------------------------<Overrides>-------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return serialNum != null && serialNum.equals(item.serialNum);
    }
    @Override
    public int hashCode() {
        return serialNum != null ? serialNum.hashCode() : 0;
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
