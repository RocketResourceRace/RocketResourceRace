package event;

public class StockUpEquipment extends GameEvent {
    public int x;
    public int y;
    public StockUpEquipment(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
