


class Building{
  int type;
  int image_id;
  Building(int type){
    this.type = type;
    this.image_id = 0;
  }
}






class Party{
  private int unitNumber;
  private int movementPoints;
  int player;
  float strength;
  private String task;
  ArrayList<Action> actions;
  ArrayList<int[]> path;
  int[] target;
  Party(int player, int startingUnits, String startingTask, int movementPoints){
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
  String getTask(){
    return task;
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
  int turnsLeft(){
    return calcTurns(actions.get(0).turns);
  }
  int calcTurns(float turnsCost){
    //Use this to calculate the number of turns a task will take for this party
    return ceil(turnsCost/(sqrt(unitNumber)/10));
  }
  String progressAction(){
    if (actions.size() == 0){
      return "";
    }
    else if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0){
      return actions.get(0).type;
    }
    else{
      actions.get(0).turns -= sqrt((float)unitNumber)/10;
      if (actions.get(0).type.contains("Build")) {
        if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0){
          return "Construction End";
        } else {
          return "Construction Mid";
        }
      }
      return "";
    }
  }
  void clearCurrentAction(){
    if (actions.size() > 0)
    actions.remove(0);
  }void clearActions(){
    actions = new ArrayList<Action>();
  }
  String currentAction(){
    return actions.get(0).type;
  }
  boolean isTurn(int turn){
    return this.player==turn;
  }
  int getMovementPoints(){
    return movementPoints;
  }
  void subMovementPoints(int p){
    movementPoints -= p;
  }
  void setMovementPoints(int p){
    movementPoints = p;
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
    unitNumber = (int)between(0, newUnitNumber, 1000);
  }
  int changeUnitNumber(int changeInUnitNumber){
    int overflow = max(0, changeInUnitNumber+unitNumber-1000);
    this.setUnitNumber(unitNumber+changeInUnitNumber);
    return overflow;
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
    super(2, attacker.getUnitNumber()+defender.getUnitNumber(), "Battle", 0);
    party1 = attacker;
    party1.strength = 2.25;
    party2 = defender;
  }
  boolean isTurn(int turn){
    return true;
  }
  int getMovementPoints(int turn){
    if(turn==party1.player){
      return party1.getMovementPoints();
    } else {
      return party2.getMovementPoints();
    }
  }
  void setUnitNumber(int turn, int newUnitNumber){
      if(turn==party1.player){
        party1.setUnitNumber(newUnitNumber);
      } else {
        party2.setUnitNumber(newUnitNumber);
      }
  }
  int getUnitNumber(int turn){
      if(turn==party1.player){
        return party1.getUnitNumber();
      } else {
        return party2.getUnitNumber();
      }
  }
  int changeUnitNumber(int turn, int changeInUnitNumber){
    if(turn==this.party1.player){
      int overflow = max(0, changeInUnitNumber+party1.getUnitNumber()-1000);
      this.party1.setUnitNumber(party1.getUnitNumber()+changeInUnitNumber);
      return overflow;
    } else {
      int overflow = max(0, changeInUnitNumber+party2.getUnitNumber()-1000);
      this.party2.setUnitNumber(party2.getUnitNumber()+changeInUnitNumber);
      return overflow;
    }
  }
  Party doBattle(){
    int changeInParty1 = getBattleUnitChange(party1, party2);
    int changeInParty2 = getBattleUnitChange(party2, party1);
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
          party1.changeTask("Rest");
          return party1;
        } else {
          party2.setUnitNumber(-endDifference);
          party2.changeTask("Rest");
          return party2;
        }
      } else {
        party2.changeTask("Rest");
        return party2;
      }
    } if(party2.getUnitNumber()==0){
      party1.changeTask("Rest");
      return party1;
    } else {
      return this;
    }
  }
  Battle clone(){
    Battle newParty = new Battle(this.party1.clone(), this.party2.clone());
    return newParty;
  }
}

int getBattleUnitChange(Party p1, Party p2){
  return floor(-0.2*(p2.getUnitNumber()+pow(p2.getUnitNumber(), 2)/p1.getUnitNumber())*random(0.80, 1.5)*p2.strength/p1.strength);
}

int getChanceOfBattleSuccess(Party attacker, Party defender){ 
  int TRIALS = 1000;
  int wins = 0;
  Party clone1;
  Party clone2;
  Battle battle;
  for (int i = 0;i<TRIALS;i++){
    if(defender.player==2){
      battle = (Battle) defender.clone();
      battle.changeUnitNumber(attacker.player, attacker.getUnitNumber());
      if(battle.party1.player==attacker.player){
        clone1 = battle.party1;
        clone2 = battle.party2;
      } else {
        clone1 = battle.party2;
        clone2 = battle.party1;
      }
    } else {
      clone1 = attacker.clone();
      clone2 = defender.clone();
      battle = new Battle(clone1, clone2); 
    }
    while (clone1.getUnitNumber()>0&&clone2.getUnitNumber()>0){
      battle.doBattle();
    }
    if(clone1.getUnitNumber()>0){
      wins+=1;
    }
  }
  return round((wins*50/TRIALS))*2;
}





class Player{
  float mapXOffset, mapYOffset, blockSize;
  float wood, food, energy, metal;
  float[] resources;
  int cellX, cellY;
  boolean cellSelected = false;
  // Resources: food wood metal energy concrete cable spaceship_parts ore people
  Player(float mapXOffset, float mapYOffset, float blockSize, float[] resources){
    this.mapXOffset = mapXOffset;
    this.mapYOffset = mapYOffset;
    this.blockSize = blockSize;
    this.resources = resources; 
  }
  void saveSettings(float mapXOffset, float mapYOffset, float blockSize, int cellX, int cellY, boolean cellSelected){
    this.mapXOffset = mapXOffset;
    this.mapYOffset = mapYOffset;
    this.blockSize = blockSize;
    this.cellX = cellX;
    this.cellY = cellY;
    this.cellSelected = cellSelected;
  }
  void loadSettings(Game g, Map m){
    m.loadSettings(mapXOffset, mapYOffset, blockSize);
    if(cellSelected){
      g.selectCell((int)this.cellX, (int)this.cellY, false);
    } else {
      g.deselectCell();
    }
  }
}





class Node{
  int cost;
  boolean fixed;
  int prevX = -1, prevY = -1;
  
  Node(int cost, boolean fixed, int prevX, int prevY){
    this.fixed = fixed;
    this.cost = cost;
    this.prevX = prevX;
    this.prevY = prevY;
  }
  void setPrev(int prevX ,int prevY){
    this.prevX = prevX;
    this.prevY = prevY;
  }
}
