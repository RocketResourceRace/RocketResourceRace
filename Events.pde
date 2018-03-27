
class Event{
  String id, type, panel;
  Event(String id, String panel, String type){
    this.id = id;
    this.type = type;
  }
  String info(){
    return "id:"+id+", type:"+type;
  }
}


class Action{
  float turns, initialTurns;
  String type;
  Action(String type, float turns){
    this.type = type;
    this.turns = turns;
    initialTurns = turns;
  }
}

//Events
//Move sx sy ex ey
//Split sx sy ex ey num
//ChangeTask sx sy task
//

class GameEvent{
  String type;
}

class Move extends GameEvent{
  int startX, startY, endX, endY;
  Move(int startX, int startY, int endX, int endY){
    this.startX = startX;
    this.startY = startY;
    this.endX = endX;
    this.endY = endY;
  }
}

class Split extends GameEvent{
  int startX, startY, endX, endY, units;
  Split(int startX, int startY, int endX, int endY, int units){
    this.startX = startX;
    this.startY = startY;
    this.endX = endX;
    this.endY = endY;
    this.units = units;
  }
  
}

class ChangeTask extends GameEvent{
  int x, y;
  String task;
  ChangeTask(int x, int y, String task){
    this.x = x;
    this.y = y;
    this.task = task;
  }
}

class EndTurn extends GameEvent{
  
}