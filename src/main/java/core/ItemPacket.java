package core;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

//This class includes the item  and quantity, used only to communicate between ItemPacket and ItemManager and to store composition
@JsonAdapter(ItemPacket.ItemPacketAdapter.class)
public class ItemPacket{

    private transient  Item item;

    @Expose
    private String serialNum; // For JSON serialization

    @Expose
    private int quantity;

    public ItemPacket(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int amount) { this.quantity = amount; }


    //used to print out composed of
    static class ItemPacketAdapter implements com.google.gson.JsonSerializer<ItemPacket> {
        @Override
        public com.google.gson.JsonElement serialize(ItemPacket src, java.lang.reflect.Type typeOfSrc,
                                                     com.google.gson.JsonSerializationContext context) {

            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("serialNum", src.getItem().getSerialNum());
            obj.addProperty("quantity", src.getQuantity());
            return obj;
        }
    }
    public void reconstructItemReference(Inventory inventory) {
        if (serialNum != null && item == null) {
            item = inventory.SerialToItemMap.get(serialNum);
        }
    }
}

