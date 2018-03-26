
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