package event;

public class Split extends GameEvent {
    int startX, startY, endX, endY, units;
    Split(int startX, int startY, int endX, int endY, int units) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.units = units;
    }
}
