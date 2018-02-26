
class Panel{
  HashMap<String, Element> elements;
  String id;
  PImage img;
  Boolean visible = true;
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
  
  Panel(String id, int x, int y, int w, int h, Boolean visible, String fileName, color strokeColour){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.visible = visible;
    this.id = id;
    this.img = loadImage(fileName);
    this.strokeColour = strokeColour;
    elements = new HashMap<String, Element>();
  }
  
  void setOffset(){
    for (Element elem : elements.values()){
      elem.setOffset(x, y);
    }
  }
  void setColour(color c){
    bgColour = c;
  }
  
  void setVisible(boolean a){
    visible = a;
    for (Element elem:elements.values()){
      elem.mouseEvent("mouseMoved", mouseButton);
    }
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    setOffset();
  }
  
  void draw(){
    pushStyle();
    if (img == null){
      if (bgColour != color(255, 255)){
        fill(bgColour);
        stroke(strokeColour);
        rect(x, y, w, h);
      }
    }
    else{
      //imageMode(CENTER);
      image(img, x, y, w, h);
    }
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