package event;

public class Bombard extends GameEvent {
    public int fromX;
    public int fromY;
    public int toX;
    public int toY;
    public Bombard(int x1, int y1, int x2, int y2) {
        fromX = x1;
        fromY = y1;
        toX = x2;
        toY = y2;
    }
}
