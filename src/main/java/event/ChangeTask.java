package event;

public class ChangeTask extends GameEvent {
  public int x;
    public int y;
  public int task;
  public ChangeTask(int x, int y, int task) {
    this.x = x;
    this.y = y;
    this.task = task;
  }
}
