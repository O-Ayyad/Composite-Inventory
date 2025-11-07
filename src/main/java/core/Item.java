package core;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.ImageIcon;


//Blueprint for all items
//Each item serial number is unique and the quantity is stored by inventory manager
public class Item {

    private String name;
    private final String serialNum; //Unique for every item. No two items can have the same serial number
    private Integer lowStockTrigger; // Creates a log when the quantity of the item is equal or lower than this

    private String walmartSellerSKU; //Used to connect to accounts
    private String amazonSellerSKU;
    private String ebaySellerSKU;

    private ArrayList<ItemPacket> composedOf = new ArrayList<>();
    private ArrayList<Item> composesInto = new ArrayList<>();

    private String iconPath;   //path to image file
    private ImageIcon cachedIcon; //Actual image
    private final int iconWidth = 50; //Dimension for the image
    private final int iconHeight = 50;

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

    public void setIconPath(String path) {
        this.iconPath = path;
        this.cachedIcon = new ImageIcon(path); // automatically cache
    }
    public String getImagePath() {
        return iconPath;
    }
    public void setImagePath(String newPath) {
        iconPath = newPath;
    }
    public ImageIcon getIcon(int width, int height) {
        if (cachedIcon == null && iconPath != null) {
            cachedIcon = new ImageIcon(iconPath);
        }
        if (cachedIcon != null) {
            Image img = cachedIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        }
        return null; //no image set
    }
    public int getLowStockTrigger() { return lowStockTrigger;}
    public void setLowStockTrigger(int lowST){lowStockTrigger = lowST;}

    public int getQuantity(){
        return itemManager.inventory.getQuantity(this);
    }

    //-------------------------------</Getters and Setters>-------------------------------




    //-------------------------------<Edit Composition>-------------------------------
    //Can add sole item or multiple at once
    public void replaceComposedOf(ArrayList<ItemPacket> newList) {
        if (newList == null) return;

        ArrayList<ItemPacket> valid = new ArrayList<>();
        for (ItemPacket ip : newList) {
            if (ip == null || ip.getItem() == null) continue;
            if (ip.getItem().equals(this)) continue;
            valid.add(ip);
        }
        if (valid.isEmpty()) return;

        composedOf.clear();
        composedOf.addAll(valid);
        syncCompositionDependencies();
    }
    public boolean isComposite(){ return !composedOf.isEmpty();}
    public Boolean isComposedOf(Item item){
        for(ItemPacket ip : composedOf){
            if(ip.getItem().equals(item)){
                return true;
            }
        }
        return false;
    }
    public Boolean doesComposeInto(Item item){return composesInto.contains(item);}
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
        this.cachedIcon = this.getIcon(iconWidth, iconHeight);

        this.amazonSellerSKU = amazonSellerSKU;
        this.ebaySellerSKU = ebaySellerSKU;
        this.walmartSellerSKU = walmartSellerSKU;

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


    //-------------------------------<Other Methods>---------------------------------

    //If an item is broken down, then return a list of its composition and remove what was used.
    //Then destroy this item
    //This is all handled in ItemManager
    public void breakDownItem(ArrayList<ItemPacket> UsedItems){
        itemManager.breakDownItem(this, UsedItems);
    }
    public void composeItem(Item composedItem){
        itemManager.composeItem(this);
    }
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
    //-------------------------------</Overrides>-------------------------------
}
