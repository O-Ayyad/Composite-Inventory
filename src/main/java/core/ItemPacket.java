package core;


//This class includes the item  and quantity, used only to communicate between ItemPacket and ItemManager and to store composition
public class ItemPacket{

    private transient final Item item;
    private int quantity;

    public ItemPacket(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int amount) { this.quantity = amount; }

}

