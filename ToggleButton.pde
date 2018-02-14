
class ToggleButton extends Element{
  color bgColour, strokeColour;
  String name;
  boolean on;
  ToggleButton(int x, int y, int w, int h, color bgColour, color strokeColour, String name){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.name = name;
  }
  ArrayList<String> mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseClicked"&&mouseOver()){
      events.add("toggled");
      on = !on;
    }
    return events;
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  boolean getState(){
    return on;
  }
  void draw(){
    pushStyle();
    fill(bgColour);
    stroke(strokeColour);
    rect(x, y, w, h);
    if (on){
      fill(0, 255, 0);
      rect(x, y, w/2, h);
    }
    else{
      fill(255, 0, 0);
      rect(x+w/2, y, w/2, h);
    }
    fill(0);
    textSize(8*TextScale);
    textAlign(LEFT, BOTTOM);
    text(name, x, y);
    popStyle();
  }
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}