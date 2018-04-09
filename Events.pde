
class Notification{
  String name;
  int x, y, turn;
  Notification(String name, int x, int y, int turn){
    this.x = x;
    this.y = y;
    this.name = name;
    this.turn = turn;
  }
}

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
  String type, notification, terrain, building;
  Action(String type, String notification, float turns, String building, String terrain){
    this.type = type;
    this.turns = turns;
    this.notification = notification;
    this.building = building;
    this.terrain = terrain;
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
