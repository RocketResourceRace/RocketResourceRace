
class Element{
  boolean active = true;
  int xOffset, yOffset;
  void draw(){}
  void mouseEvent(String eventType, int button){}
  void mouseEvent(String eventType, int button, MouseEvent event){}
  void keyboardEvent(String eventType, int _key){}
  void setOffset(int xOffset, int yOffset){
    this.xOffset = xOffset;
    this.yOffset = yOffset;
  }
  ArrayList<String> _mouseEvent(String eventType, int button){
    mouseEvent(eventType, button);
    return new ArrayList<String>();
  } 
  ArrayList<String> _mouseEvent(String eventType, int button, MouseEvent event){
    mouseEvent(eventType, button);
    return new ArrayList<String>();
  } 
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    return new ArrayList<String>();
  }
  void activate(){
    active = true;
  }
  void deactivate(){
    active = false;
  }
}