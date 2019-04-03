package event;

public class ChangePartyTrainingFocus extends GameEvent {
    private int x;
    private int y;
    public int newFocus;
    public ChangePartyTrainingFocus(int x, int y, int newFocus) {
        this.x = x;
        this.y = y;
        this.newFocus = newFocus;
    }
}
