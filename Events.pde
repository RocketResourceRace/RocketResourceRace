
class Notification {
  String name;
  int x, y, turn;
  Notification(String name, int x, int y, int turn) {
    this.x = x;
    this.y = y;
    this.name = name;
    this.turn = turn;
  }
}

class Event {
  String id, type, panel;
  Event(String id, String panel, String type) {
    this.id = id;
    this.type = type;
    this.panel = panel;
  }
  String info() {
    return "id:"+id+", type:"+type+", panel:"+panel;
  }
}


class Action {
  float turns, initialTurns;
  int type;
  String notification, terrain, building;
  Action(int type, String notification, float turns, String building, String terrain) {
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

class GameEvent {
  String type;
}

class Move extends GameEvent {
  int startX, startY, endX, endY;
  Move(int startX, int startY, int endX, int endY) {
    this.startX = startX;
    this.startY = startY;
    this.endX = endX;
    this.endY = endY;
  }
}

class Split extends GameEvent {
  int startX, startY, endX, endY, units;
  Split(int startX, int startY, int endX, int endY, int units) {
    this.startX = startX;
    this.startY = startY;
    this.endX = endX;
    this.endY = endY;
    this.units = units;
  }
}

class ChangeTask extends GameEvent {
  int x, y;
  int task;
  ChangeTask(int x, int y, int task) {
    this.x = x;
    this.y = y;
    this.task = task;
  }
}

class ChangePartyTrainingFocus extends GameEvent { 
  int x, y;
  int newFocus;
  ChangePartyTrainingFocus(int x, int y, int newFocus) {
    this.x = x;
    this.y = y;
    this.newFocus = newFocus;
  }
}

class ChangeEquipment extends GameEvent{
  int equipmentClass;
  int newEqupmentType;
  ChangeEquipment(int equipmentClass, int newEqupmentType){
    this.equipmentClass = equipmentClass;
    this.newEqupmentType = newEqupmentType;
  }
}

class DisbandParty extends GameEvent { 
  int x, y;
  DisbandParty(int x, int y) {
    this.x = x;
    this.y = y;
  }
}

class StockUpEquipment extends GameEvent { 
  int x, y;
  StockUpEquipment(int x, int y) {
    this.x = x;
    this.y = y;
  }
}

class SetAutoStockUp extends GameEvent {
  int x, y;
  boolean enabled;
  SetAutoStockUp(int x, int y, boolean enabled){
    this.x = x;
    this.y = y;
    this.enabled = enabled;
  }
}

class EndTurn extends GameEvent {
}
