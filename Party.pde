

class Party{
  private int unitNumber, movementPoints;
  int player;
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
  boolean isTurn(int turn){
    return this.player==turn;
  }
  int getMovementPoints(int turn){
    return movementPoints;
  }
  int getUnitNumber(int turn){
    return unitNumber;
  }
}

class Battle extends Party{
  Party party1;
  Party party2;
  Battle(Party p1, Party p2){
    super(2, p1.unitNumber+p2.unitNumber, "Battle", 0);
    party1 = p1;
    party2 = p2;
  }
  boolean isTurn(int turn){
    return true;
  }
  int getMovementPoints(int turn){
    if(turn==0){
      return party1.getMovementPoints(turn);
    } else {
      return party2.getMovementPoints(turn);
    }
  }
  int getUnitNumber(int turn){
      if(turn==1){
        return party1.getUnitNumber(turn);
      } else {
        return party2.getUnitNumber(turn);
      }
  }
}