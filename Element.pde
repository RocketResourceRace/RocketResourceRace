
class Element{
  Element(){
    
  }
  void draw(int xOffset, int yOffset){
    // To be overriden by child
  }
  void mouseEvent(String eventType, int button){}
  void keyboardEvent(String eventType, int _key){}
  
  
  void _mouseEvent(String eventType, int button){
    mouseEvent(eventType, button);
  } 
  void _keyboardEvent(String eventType, int _key){
    mouseEvent(eventType, _key);
  }
}