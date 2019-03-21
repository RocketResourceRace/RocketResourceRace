package event;

public class Action {
  public float turns;
  public float initialTurns;
  public int type;
  public String notification;
    public String terrain;
    public String building;
  public Action(int type, String notification, float turns, String building, String terrain) {
    this.type = type;
    this.turns = turns;
    this.notification = notification;
    this.building = building;
    this.terrain = terrain;
    initialTurns = turns;
  }
}
