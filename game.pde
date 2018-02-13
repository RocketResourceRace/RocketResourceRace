
class Game extends State{
  Game(){
    addElement("map", new Map(100, 100, 1000, 700));
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (_key == ESC){
      newState = "menu";
    }
    return new ArrayList<String>();
  }
  void enterState(){
    Map m = (Map)(getElement("map", "default"));
    m.generateMap();
  }
}