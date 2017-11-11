
class Element{
  void draw(int xOffset, int yOffset){}
  void mouseEvent(String eventType, int button){}
  void keyboardEvent(String eventType, int _key){}
  ArrayList<String> _mouseEvent(String eventType, int button){
    mouseEvent(eventType, button);
    return new ArrayList<String>();
  } 
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    return new ArrayList<String>();
  }
}