package core;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
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
    public String getIconPath() {
        return iconPath;
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
    public Boolean isComposedOf(ItemPacket item){return composedOf.contains(item);}
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
        if (itemManager == null) return;
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


    //-------------------------------<Item Array>---------------------------------

    /**
     * Returns all the information into one array.
     *
     * param {Item} Item being evaluated
     * return {Object[]} Holds all information on item
     *
     * Enum used to access array with more readability
     */

    public enum ItemField {
        IMAGE(0),
        NAME(1),
        SERIAL(2),
        LOW_STOCK_TRIGGER(3),
        COMPOSED_OF(4),
        COMPOSED_OF_SIZE(5),
        COMPOSES_INTO(6),
        COMPOSES_INTO_SIZE(7),
        AMAZON_SKU(8),
        EBAY_SKU(9),
        WALMART_SKU(10);

        private final int index;
        ItemField(int index) { this.index = index; }
        public int getIndex() { return index; }
    }
    public Object[] toArray() {
        Object[] arr = new Object[11];

        arr[ItemField.IMAGE.getIndex()] = getIcon(50, 50);
        arr[ItemField.NAME.getIndex()] = name;
        arr[ItemField.SERIAL.getIndex()] = serialNum;
        arr[ItemField.LOW_STOCK_TRIGGER.getIndex()] = lowStockTrigger; // new field
        arr[ItemField.COMPOSED_OF.getIndex()] = composedOf;
        arr[ItemField.COMPOSED_OF_SIZE.getIndex()] = composedOf.size();
        arr[ItemField.COMPOSES_INTO.getIndex()] = composesInto;
        arr[ItemField.COMPOSES_INTO_SIZE.getIndex()] = composesInto.size();
        arr[ItemField.AMAZON_SKU.getIndex()] = amazonSellerSKU;
        arr[ItemField.EBAY_SKU.getIndex()] = ebaySellerSKU;
        arr[ItemField.WALMART_SKU.getIndex()] = walmartSellerSKU;

        return arr;
    }
    //Returns the field of the item
    public Object getField(Object[] itemArray, ItemField field) {
        return itemArray[field.getIndex()];
    }
    //-------------------------------</Item Array>---------------------------------

    //-------------------------------<Other Methods>---------------------------------

    //If an item is broken down, then return a list of its composition and remove what was used.
    //Then destroy this item
    //This is all handled in ItemManager
    public void breakDownItem(ArrayList<ItemPacket> UsedItems){
        itemManager.breakDownItem(this, UsedItems);
    }
    public void composeItem(Item composedItem, ArrayList<ItemPacket> usedComponents){
        itemManager.composeItem(this, usedComponents);
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
