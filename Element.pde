
class Element{
  void draw(int xOffset, int yOffset){}
  void mouseEvent(String eventType, int button){}
  void keyboardEvent(String eventType, int _key){}
  ArrayList<String> _mouseEvent(String eventType, int button){
    mouseEvent(eventType, button);
    return null;
  } 
  void _keyboardEvent(String eventType, int _key){
    mouseEvent(eventType, _key);
  }
}