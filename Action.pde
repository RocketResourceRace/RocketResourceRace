
class Action{
  int turns, initialTurns;
  String type;
  Action(String type, int turns){
    this.type = type;
    this.turns = turns;
    initialTurns = turns;
  }
}