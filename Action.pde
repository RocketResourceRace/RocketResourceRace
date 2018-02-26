
class Action{
  float turns, initialTurns;
  String type;
  Action(String type, float turns){
    this.type = type;
    this.turns = turns;
    initialTurns = turns;
  }
}