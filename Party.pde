

class Party{
  int unitNumber, player, movementPoints;
  String task;
  ArrayList<Action> actions;
  Party(int player, int startingUnits, String startingTask, int movementPoints){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
    actions = new ArrayList<Action>();
  }
  void changeTask(String task){
    this.task = task;
  }
  void addAction(Action a){
    actions.add(a);
  }
  boolean hasActions(){
    return actions.size()>0;
  }
  String progressAction(){
    if (actions.size() == 0){
      return "";
    }
    if (--actions.get(0).turns <= 0){
      return actions.get(0).type;
    }
    return "";
  }
  void clearCurrentAction(){
    if (actions.size() > 0)
    actions.remove(0);
  }
}