
class Element{
  boolean active = true;
  int xOffset, yOffset;
  int x, y, w, h;
  void draw(){}
  ArrayList<String> mouseEvent(String eventType, int button){return new ArrayList<String>();}
  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){return new ArrayList<String>();}
  ArrayList<String> keyboardEvent(String eventType, char _key){return new ArrayList<String>();}
  void setOffset(int xOffset, int yOffset){
    this.xOffset = xOffset;
    this.yOffset = yOffset;
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  ArrayList<String> _mouseEvent(String eventType, int button){
    return mouseEvent(eventType, button);
  } 
  ArrayList<String> _mouseEvent(String eventType, int button, MouseEvent event){
    return mouseEvent(eventType, button, event);
  } 
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    return keyboardEvent(eventType, _key);
  }
  void activate(){
    active = true;
  }
  void deactivate(){
    active = false;
  }
}
