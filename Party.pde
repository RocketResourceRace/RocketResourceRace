

class Party{
  private int unitNumber;
  private float movementPoints;
  int player;
  float strength;
  String task;
  ArrayList<Action> actions;
  ArrayList<int[]> path;
  int[] target;
  Party(int player, int startingUnits, String startingTask, float movementPoints){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
    actions = new ArrayList<Action>();
    strength = 1.5;
    clearPath();
    target = null;
  }
  void changeTask(String task){
    switch(task){
      case "Garrison": strength = 5; break;
      case "Defend": strength = 3; break;
      case "Rest": strength = 1.5; break;
      case "default": strength = 1; break;
    }
    this.task = task;
  }
  int[] nextNode(){
    return path.get(0);
  }
  void loadPath(ArrayList<int[]> p){
    path = p;
  }
  void clearNode(){
    path.remove(0);
  }
  void clearPath(){
    path = new ArrayList<int[]>();
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
  float getMovementPoints(){
    return movementPoints;
  }
  float getMovementPoints(int turn){
    return movementPoints;
  }
  int getUnitNumber(){
    return unitNumber;
  }
  int getUnitNumber(int turn){
    return unitNumber;
  }
  void setUnitNumber(int newUnitNumber){
    unitNumber = max(0, newUnitNumber);
  }
  Party clone(){
    Party newParty = new Party(player, unitNumber, task, movementPoints);
    newParty.actions = new ArrayList<Action>(actions);
    newParty.strength = strength;
    return newParty;
  }
}

class Battle extends Party{
  Party party1;
  Party party2;
  Battle(Party attacker, Party defender){
    super(2, attacker.unitNumber+defender.unitNumber, "Battle", 0);
    party1 = attacker;
    party1.strength*=1.5;
    party2 = defender;
    party1.task = "Rest";
    party2.task = "Rest";
  }
  boolean isTurn(int turn){
    return true;
  }
  float getMovementPoints(int turn){
    if(turn==0){
      return party1.getMovementPoints();
    } else {
      return party2.getMovementPoints();
    }
  }
  int getUnitNumber(int turn){
      if(turn==party1.player){
        return party1.getUnitNumber();
      } else {
        return party2.getUnitNumber();
      }
  }
  Party doBattle(){
    int changeInParty1 = getBattleUnitChange(party1, party2);
    int changeInParty2 = getBattleUnitChange(party2, party2);
    party1.strength = 1;
    party2.strength = 1;
    int newParty1Size = party1.getUnitNumber()+changeInParty1;
    int newParty2Size = party2.getUnitNumber()+changeInParty2;
    int endDifference = newParty1Size-newParty2Size; 
    party1.setUnitNumber(newParty1Size);
    party2.setUnitNumber(newParty2Size);
    if (party1.getUnitNumber()==0){
      if(party2.getUnitNumber()==0){
        if(endDifference==0){
          return null;
        } else if(endDifference>0){
          party1.setUnitNumber(endDifference);
          return party1;
        } else {
          party2.setUnitNumber(-endDifference);
          return party2;
        }
      } else {
        return party2;
      }
    } if(party2.getUnitNumber()==0){
      return party1;
    } else {
      return this;
    }
  }
}

int getBattleUnitChange(Party p1, Party p2){
  return floor(-0.2*(p2.getUnitNumber()+pow(p2.getUnitNumber(), 2)/p1.getUnitNumber())*random(0.85, 1.15)*p2.strength/p1.strength);
}

int getChanceOfBattleSuccess(Party attacker, Party defender){
  int TRIALS = 1000;
  int wins = 0;
  Party clone1;
  Party clone2;
  Battle battle;
  for (int i = 0;i<TRIALS;i++){
    clone1 = attacker.clone();
    clone2 = defender.clone();
    battle = new Battle(clone1, clone2); 
    while (clone1.getUnitNumber()>0&&clone2.getUnitNumber()>0){
      battle.doBattle();
    }
    if(clone1.getUnitNumber()>0){
      wins+=1;
    }
  }
  return round((wins*50/TRIALS))*2;
}