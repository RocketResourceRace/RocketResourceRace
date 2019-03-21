package event;

public class Notification {
    public String name;
    public int x;
    public int y;
    public int turn;
    public Notification(String name, int x, int y, int turn) {
      this.x = x;
      this.y = y;
      this.name = name;
      this.turn = turn;
    }
}