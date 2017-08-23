
class Button extends Element{
  private int x, y, w, h, cx, cy, textSize, textAlign;
  private color bgColour, strokeColour, textColour;
  private String state, text;
  private final int HOVERINGOFFSET = 40, ONOFFSET = 80;
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
  }
  void centerCoords(){
    cx = x+w/2;
    cy = y+h/2;
  }
  void setText(String text){
    this.text = text;
  }
  void draw(int xOffset, int yOffset){
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
    rect(x, y, w, h);
    noTint();
    fill(textColour);
    textAlign(textAlign);
    textSize(textSize);
    if (textAlign == CENTER){
      text(text, cx, cy+h/4);
    }
    else{
      text(text, x, y);
    }
    popStyle();
  }
  void _mouseEvent(String eventType, int button){
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
      }
    }
    else{
      state = "off";
    }
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}