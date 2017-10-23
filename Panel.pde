
class Panel{
  HashMap<String, Element> elements;
  String id;
  Boolean visible;
  private int x, y, w, h;
  private color bgColour, strokeColour;
  
  Panel(String id, int x, int y, int w, int h, Boolean visible, color bgColour, color strokeColour){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.visible = visible;
    this.id = id;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    elements = new HashMap<String, Element>();
  }
  
  void setOffset(){
    for (Element elem : elements.values()){
      elem.setOffset(x, y);
    }
  }
  
  void draw(){
    pushStyle();
    fill(bgColour);
    stroke(strokeColour);
    rect(x, y, w, h);
    popStyle();
    
    for (Element elem : elements.values()){
      elem.draw();
    }
  }
  
  int getX(){
    return x;
  }
  int getY(){
    return y;
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}