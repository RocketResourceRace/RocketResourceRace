package event;

public class UnitCapChange extends GameEvent {
  public int x;
    public int y;
    public int newCap;
  public UnitCapChange(int x, int y, int newCap) {
    this.x = x;
    this.y = y;
    this.newCap = newCap;
  }
}
