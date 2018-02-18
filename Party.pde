

class Party{
  private int unitNumber, movementPoints;
  int player;
  float strength;
  String task;
  ArrayList<Action> actions;
  Party(int player, int startingUnits, String startingTask, int movementPoints){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
    actions = new ArrayList<Action>();
    strength = 1.5;
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
  int getMovementPoints(){
    return movementPoints;
  }
  int getMovementPoints(int turn){
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
}

class Battle extends Party{
  Party party1;
  Party party2;
  Battle(Party attacker, Party defender){
    super(2, attacker.unitNumber+defender.unitNumber, "Battle", 0);
    party1 = attacker;
    party1.strength*=1.5;
    party2 = defender;
  }
  boolean isTurn(int turn){
    return true;
  }
  int getMovementPoints(int turn){
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
    int changeInParty1 = floor(-0.2*(party2.getUnitNumber()+pow(party2.getUnitNumber(), 2)/party1.getUnitNumber())*party2.strength/party1.strength);
    int changeInParty2 = floor(-0.2*(party1.getUnitNumber()+pow(party1.getUnitNumber(), 2)/party2.getUnitNumber())*party1.strength/party2.strength);
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