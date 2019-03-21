package event;

public class ChangePartyTrainingFocus extends GameEvent {
  int x, y;
  public int newFocus;
  public ChangePartyTrainingFocus(int x, int y, int newFocus) {
    this.x = x;
    this.y = y;
    this.newFocus = newFocus;
  }
}
