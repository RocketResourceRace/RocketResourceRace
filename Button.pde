
class Button extends Element{
  private int x, y, w, h, cx, cy, textSize, textAlign;
  private color bgColour, strokeColour, textColour;
  private String state, text;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  private ArrayList<String> lines;
  
  Button(int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text){
    state = "off";
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.textColour = textColour;
    this.textSize = textSize;
    this.textAlign = textAlign;
    this.text = text;
    centerCoords();
    
    setLines(text);
  }
  void centerCoords(){
    cx = x+w/2;
    cy = y+h/2;
  }
  void setText(String text){
    this.text = text;
    setLines(text);
  }
  void draw(int xOffset, int yOffset){
    int padding=0;
    float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
    pushStyle();
    fill(bgColour);
    if (state == "off"){
      fill(bgColour);
    }
    else if (state == "hovering"){
      fill(min(r+HOVERINGOFFSET, 255), min(g+HOVERINGOFFSET, 255), min(b+HOVERINGOFFSET, 255));
    }
    else if (state == "on"){
      fill(min(r+ONOFFSET, 255), min(g+ONOFFSET, 255), min(b+ONOFFSET, 255));
    }
    stroke(strokeColour);
    strokeWeight(3);
    rect(x, y, w, h);
    noTint();
    fill(textColour);
    textAlign(textAlign);
    textSize(textSize);
    if (lines.size() == 1){
      padding = (h/6);
    }
    for (int i=0; i<lines.size(); i++){
      if (textAlign == CENTER){
        text(lines.get(i), cx, cy+i*textSize+padding);
      }
      else{
        text(lines.get(i), x, y + i*textSize);
      }
    }
    popStyle();
  }
  
  ArrayList<String> setLines(String s){
    int j = 0;
    lines = new ArrayList<String>();
    for (int i=0; i<s.length(); i++){
      if(s.charAt(i) == '\n'){
        lines.add(s.substring(j, i));
        j=i+1;
      }
    }
    lines.add(s.substring(j, s.length()));
    
    return lines;
  }
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    mouseEvent(eventType, button);
    if(eventType == "mouseReleased"){
      state = "off";
    }
    if (mouseOver()){
      if (!state.equals("on")){
        state = "hovering";
      }
      if (eventType == "mousePressed"){
        state = "on";
        events.add("clicked");
      }
    }
    else{
      state = "off";
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}