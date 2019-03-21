package event;

public class DisbandParty extends GameEvent {
    public int x;
    public int y;
    public DisbandParty(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
