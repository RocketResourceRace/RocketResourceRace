package event;

public class SetAutoStockUp extends GameEvent {
    public int x;
    public int y;
    public boolean enabled;
    public SetAutoStockUp(int x, int y, boolean enabled){
        this.x = x;
        this.y = y;
        this.enabled = enabled;
    }
}
