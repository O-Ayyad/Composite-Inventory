package core;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import javax.swing.ImageIcon;


//Blueprint for all items
//Each item name is serial number is unique and the quantity is stored by inventory manager
public class Item {

    public String name;
    public String serialNum;

    public ArrayList<ItemPacket> composedOf = new ArrayList<>();
    public ArrayList<ItemPacket> composesInto = new ArrayList<>();

    private String iconPath;   //path to image file
    private ImageIcon cachedIcon; //Actual image
    private final int iconWidth = 50; //Dimension for the image
    private final int iconHeight = 50;

    private Object[] itemInfoArray = new Object[8];

    private ItemManager itemManager;

    //-------------------------------<Getters and Setters>-------------------------------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSerialNum() { return serialNum; }
    public void setSerialNum(String serial) { this.serialNum = serial; }

    public ArrayList<ItemPacket> getComposedOf() { return composedOf; }
    public ArrayList<ItemPacket> getComposesInto() { return composesInto; }

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

    //-------------------------------</Getters and Setters>-------------------------------




    //-------------------------------<Edit Composition>-------------------------------
    //Can add sole item or multiple at once
    public void addComposedOf(ItemPacket item) {composedOf.add(item);}
    public void addComposesInto(ItemPacket item) {composesInto.add(item);}
    public void addComposedOf(ArrayList<ItemPacket> items) {composedOf.addAll(items);}
    public void addComposesInto(ArrayList<ItemPacket> items) {composesInto.addAll(items);}
    public void removeComposedOf(ItemPacket item) {composedOf.remove(item);}
    public void removeComposedOf(ArrayList<ItemPacket> items) {composedOf.removeAll(items);}
    public void removeComposedInto(ItemPacket item) {composesInto.remove(item);}
    public void removeComposedInto(ArrayList<ItemPacket> items) {composesInto.removeAll(items);}
    public void removeComposesInto(ItemPacket item) {composesInto.remove(item);}
    public Boolean isComposedOf(ItemPacket item){return composedOf.contains(item);}
    public Boolean doesComposeInto(ItemPacket item){return composesInto.contains(item);}
    //-------------------------------</Edit Composition>-------------------------------



    //-------------------------------<Constructor>-------------------------------
    public Item(String name, String serialNum,
                ArrayList<ItemPacket> composed, ArrayList<ItemPacket> composes,
                String iconPath,
                ItemManager itemManager) {
        this.name = name;
        this.serialNum = serialNum;
        this.composedOf = composed;
        this.composesInto = composes;
        this.iconPath = iconPath;
        this.cachedIcon = this.getIcon(iconWidth,iconHeight);

        this.itemInfoArray = this.toArray();

        this.itemManager = itemManager;
    }
    //-------------------------------</Constructor>-------------------------------


    //-------------------------------<Item Array>---------------------------------

    /**
     * Returns all the information into one array.
     *
     * @param {Item} Item being evaluated
     * @return {Object[]} Holds all information on item
     *
     * Enum used to access array with more readability
     */

    public enum ItemField {
        IMAGE(0),
        NAME(1),
        SERIAL(2),
        COMPOSED_OF(3),
        COMPOSED_OF_SIZE(4),
        COMPOSES_INTO(5),
        COMPOSES_INTO_SIZE(6);
        private final int index;

        ItemField(int index) {
            this.index = index;
        }
        public int getIndex() {
            return index;
        }
    }
    public Object[] toArray(){
        Object[] arr = new Object[7];

        arr[ItemField.IMAGE.getIndex()] = getIcon(50,50); // or null if no image
        arr[ItemField.NAME.getIndex()] = name;
        arr[ItemField.SERIAL.getIndex()] = serialNum;
        arr[ItemField.COMPOSED_OF.getIndex()] = composedOf;
        arr[ItemField.COMPOSED_OF_SIZE.getIndex()] = composedOf.size();
        arr[ItemField.COMPOSES_INTO.getIndex()] = composesInto;
        arr[ItemField.COMPOSES_INTO_SIZE.getIndex()] = composesInto.size();

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
    public ArrayList<ItemPacket> breakDownItem(ArrayList<ItemPacket> UsedItem){
        return itemManager.breakDownItem(this, UsedItem);
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
