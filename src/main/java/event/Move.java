package event;

public class Move extends GameEvent {
  public int startX;
    public int startY;
    public int endX;
    public int endY;
    public int num;
  public Move(int startX, int startY, int endX, int endY, int num) {
    this.startX = startX;
    this.startY = startY;
    this.endX = endX;
    this.endY = endY;
    this.num = num;
  }
}
