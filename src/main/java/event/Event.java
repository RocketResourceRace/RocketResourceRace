package event;

public class Event {
  public String id;
  public String type;
  public String panel;
  public Event(String id, String panel, String type) {
    this.id = id;
    this.type = type;
    this.panel = panel;
  }
  public String info() {
    return "id:"+id+", type:"+type+", panel:"+panel;
  }
}
