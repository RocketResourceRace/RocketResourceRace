


class Building{
  int type;
  int image_id;
  Building(int type){
    this(type, 0);
  }
  Building(int type, int image_id){
    this.type = type;
    this.image_id = image_id;
  }
}


class Party{
  private int trainingFocus;
  private int unitNumber;
  private int movementPoints;
  private float[] proficiencies;
  private int task;
  private int[] equipment;
  String id;
  int player;
  float strength;
  ArrayList<Action> actions;
  ArrayList<int[]> path;
  int[] target;
  int pathTurns;
  byte[] byteRep;
  
  Party(int player, int startingUnits, int startingTask, int movementPoints, String id){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
    actions = new ArrayList<Action>();
    strength = 1.5;
    clearPath();
    target = null;
    pathTurns = 0;
    this.id = id;
    
    // Default proficiencies = 1
    resetProficiencies();
    for (int i = 0; i < jsManager.getNumProficiencies(); i++){
      this.setProficiency(i, 1);
    }
    
    setTrainingFocus(jsManager.proficiencyIDToIndex("melee attack"));
    
    equipment = new int[jsManager.getNumEquipmentTypes()];
  }
  
  Party(int player, int startingUnits, int startingTask, int movementPoints, String id, float[] proficiencies, String trainingFocus, int[] equipment){
    // For parties that already exist and are being splitted or loaded from save
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
    this.actions = new ArrayList<Action>();
    this.strength = 1.5;
    this.clearPath();
    this.target = null;
    this.pathTurns = 0;
    this.id = id;
    this.equipment = equipment;
    
    // Load proficiencies given
    try{
      resetProficiencies();
      for (int i = 0; i < jsManager.getNumProficiencies(); i++){
        this.setProficiency(i, proficiencies[i]);
      }
    }
    catch (IndexOutOfBoundsException e){
      LOGGER_MAIN.severe(String.format("Not enough proficiencies given to party:%d (needs %d)  id:%s", proficiencies.length, jsManager.getNumProficiencies(), id));
    }
    
    setTrainingFocus(jsManager.proficiencyIDToIndex(trainingFocus));  // 'trainingFocus' is an id string
  }
  
  void setTrainingFocus(int value){
    // Training focus is the index of the proficiency in data.json
    this.trainingFocus = value;
  }
  
  int getTrainingFocus(){
    // Training focus is the index of the proficiency in data.json
    return this.trainingFocus;
  }
  
  void setAllEquipment(int[] v){
    equipment = v;
  }
  
  int[] getAllEquipment(){
    return equipment;
  }
  
  void setEquipment(int typeIndex, int equipmentIndex){
    equipment[typeIndex] = equipmentIndex;
  }
  
  int getEquipment(int typeIndex){
    return equipment[typeIndex];
  }
  
  void mergeEntireFrom(Party other){
    // Note: will need to remove other division
    LOGGER_GAME.fine(String.format("Merging entire party from id:%s into party with id:%s", other.id, this.id));
    boolean fullyMerged = mergeFrom(other, other.getUnitNumber());
    
    if (!fullyMerged){
      LOGGER_MAIN.warning(String.format("Party was not fully merged, id: %s, left=%d", id, other.getUnitNumber()));
    }
  }
  
  boolean mergeFrom(Party other, int unitsTransfered){
    // Take units from other party into this party and merge attributes, weighted by unit number
    LOGGER_GAME.fine(String.format("Merging %d units from party with id:%s into party with id:%s", unitsTransfered, other.id, this.id));
    
    // Merge all proficiencies with other party
    for (int i = 0; i < jsManager.getNumProficiencies(); i++){
      this.setProficiency(i, mergeAttribute(this.getUnitNumber(), this.getProficiency(i), unitsTransfered, other.getProficiency(i)));
    }
      
    LOGGER_GAME.finer(String.format("New proficiency values: %s for party with id:%s", Arrays.toString(proficiencies), id));
    // Note: other division attributes unaffected by merge
    
    this.changeUnitNumber(unitsTransfered);
    other.changeUnitNumber(unitsTransfered);
    
    return other.getUnitNumber() > 0; // Return true if any units left, else false
  }
  
  String getID(){
    return id;
  }
  
  void setID(String value){
    this.id = value;
  }
  
  float mergeAttribute(int units1, float attrib1, int units2, float attrib2){
    // Calcaulate the attributes for merge weighted by units number
    return (units1 * attrib1 + units2 * attrib2) / (units1 + units2);
  }
  
  void changeTask(int task){
    //LOGGER_GAME.info("Party changing task to:"+gameData.getJSONArray("tasks").getJSONObject(task).getString("id")); Removed as this is called too much for battle estimates
    try{
      this.task = task;
      JSONObject jTask = gameData.getJSONArray("tasks").getJSONObject(this.getTask());
      if (!jTask.isNull("strength")){
        this.strength = jTask.getInt("strength");
      }
      else{
        this.strength = 1.5;
      }
    }
    catch (NullPointerException e){
      LOGGER_MAIN.log(Level.WARNING, String.format("Error changing party task, id:%s, task=%s. Likely cause is something wrong in data.json",id, task), e);
    }
  }
  
  void setPathTurns(int v){
    LOGGER_GAME.finer(String.format("Setting path turns to:%s, party id:%s", v, id));
    pathTurns = v;
  }
  
  void moved(){
    LOGGER_GAME.finest("Decreasing pathTurns due to party moving id: "+id);
    pathTurns = max(pathTurns-1, 0);
  }
  
  int getTask(){
    return task;
  }
  
  int[] nextNode(){
    try{
      return path.get(0);
    }
    catch (IndexOutOfBoundsException e){
      LOGGER_MAIN.log(Level.WARNING, "Party run out of nodes id:"+id, e);
      return null;
    }
  }
  
  void loadPath(ArrayList<int[]> p){
    LOGGER_GAME.finer("Loading path into party id:"+id);
    path = p;
  }
  
  void clearNode(){
    path.remove(0);
  }
  
  void clearPath(){
    //LOGGER_GAME.finer("Clearing party path"); Removed as this is called too much for battle estimates
    path = new ArrayList<int[]>();
    pathTurns=0;
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
  
  Action progressAction(){
    try{
      if (actions.size() == 0){
        return null;
      }
      LOGGER_GAME.finer(String.format("Party action progressing: '%s', id:%s", actions.get(0).type, id));
      if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0){
        return actions.get(0);
      }
      else{
        actions.get(0).turns -= sqrt((float)unitNumber)/10;
        if (gameData.getJSONArray("tasks").getJSONObject(actions.get(0).type).getString("id").contains("Build")) {
          if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0){
            return new Action(JSONIndex(gameData.getJSONArray("tasks"), "Construction End"), "Construction End", 0, null, null);
          } else {
            return new Action(JSONIndex(gameData.getJSONArray("tasks"), "Construction Mid"), "Construction Mid", 0, null, null);
          }
        }
        return null;
      }
    }
    catch(Exception e){
      LOGGER_MAIN.log(Level.SEVERE, "Progressing party action failed id:"+id);
      throw e;
    }
  }
  void clearCurrentAction(){
    if (actions.size() > 0){
      LOGGER_GAME.finest(String.format("Clearing party current action of type:%s, id:%s",actions.get(0).type, id));
      actions.remove(0);
    }
  }
  
  void clearActions(){
    actions = new ArrayList<Action>();
  }
  
  int currentAction(){
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
    unitNumber = (int)between(0, newUnitNumber, jsManager.loadIntSetting("party size"));
  }
  
  int changeUnitNumber(int changeInUnitNumber){
    int overflow = max(0, changeInUnitNumber+unitNumber-jsManager.loadIntSetting("party size"));
    this.setUnitNumber(unitNumber+changeInUnitNumber);
    return overflow;
  }
  
  Party clone(){
    Party newParty = new Party(player, unitNumber, task, movementPoints, id);
    newParty.actions = new ArrayList<Action>(actions);
    newParty.strength = strength;
    return newParty;
  }
  
  float getProficiency(String id){
    // Use this if have access to string id
    return proficiencies[jsManager.proficiencyIDToIndex(id)];
  }
  
  void setProficiency(String id, float value){
    // Use this if have access to string id
    proficiencies[jsManager.proficiencyIDToIndex(id)] = value;
  }
  
  float getProficiency(int index){
    // Use this if have access to index not string id
    return proficiencies[index];
  }
  
  void setProficiency(int index, float value){
    // Use this if have access to index not string id
    proficiencies[index] = value;
  }
  
  void resetProficiencies(){
    proficiencies = new float[jsManager.getNumProficiencies()];
  }
  
  float[] getProficiencies(){
    return proficiencies;
  }
  
  void setProficiencies(float[] values){
    this.proficiencies = values;
  }
}

class Battle extends Party{
  Party party1;
  Party party2;
  Battle(Party attacker, Party defender, String id){
    super(2, attacker.getUnitNumber()+defender.getUnitNumber(), JSONIndex(gameData.getJSONArray("tasks"), "Battle"), 0, id);
    party1 = attacker;
    party1.strength = 2;
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
      int overflow = max(0, changeInUnitNumber+party1.getUnitNumber()-jsManager.loadIntSetting("party size"));
      this.party1.setUnitNumber(party1.getUnitNumber()+changeInUnitNumber);
      return overflow;
    } else {
      int overflow = max(0, changeInUnitNumber+party2.getUnitNumber()-jsManager.loadIntSetting("party size"));
      this.party2.setUnitNumber(party2.getUnitNumber()+changeInUnitNumber);
      return overflow;
    }
  }
  Party doBattle(){
    try{
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
            party1.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            return party1;
          } else {
            party2.setUnitNumber(-endDifference);
            party2.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            return party2;
          }
        } else {
          party2.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          return party2;
        }
      } if(party2.getUnitNumber()==0){
        party1.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
        return party1;
      } else {
        return this;
      }
    }
    catch (Exception e){
      LOGGER_MAIN.log(Level.SEVERE, "Error doing battle", e);
      throw e;
    }
  }
  Battle clone(){
    Battle newParty = new Battle(this.party1.clone(), this.party2.clone(), id);
    return newParty;
  }
}
class Siege extends Party{
  Siege (Party attacker, Building defence, Party garrison, String id){
    super(3, attacker.getUnitNumber()+garrison.getUnitNumber(), JSONIndex(gameData.getJSONArray("tasks"), "Siege"), 0, id);
  }
}

int getBattleUnitChange(Party p1, Party p2){
  return floor(-0.2*(p2.getUnitNumber()+pow(p2.getUnitNumber(), 2)/p1.getUnitNumber())*random(0.75, 1.5)*p2.strength/p1.strength);
}



class Player{
  float cameraCellX, cameraCellY, blockSize;
  float[] resources;
  int cellX, cellY, colour;
  boolean cellSelected = false;
  String name;
  // Resources: food wood metal energy concrete cable spaceship_parts ore people
  Player(float x, float y, float blockSize, float[] resources, int colour, String name){
    this.cameraCellX = x;
    this.cameraCellY = y;
    this.blockSize = blockSize;
    this.resources = resources;
    this.colour = colour;
    this.name = name;
  }
  void saveSettings(float x, float y, float blockSize, int cellX, int cellY, boolean cellSelected){
    this.cameraCellX = x;
    this.cameraCellY = y;
    this.blockSize = blockSize;
    this.cellX = cellX;
    this.cellY = cellY;
    this.cellSelected = cellSelected;
  }
  void loadSettings(Game g, Map m){
    LOGGER_MAIN.fine("Loading player camera settings");
    m.loadSettings(cameraCellX, cameraCellY, blockSize);
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



class MapSave{
  float[] heightMap;
  int mapWidth, mapHeight;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  int startTurn;
  int startPlayer;
  Player[] players;
  MapSave(float[] heightMap,int mapWidth, int mapHeight, int[][] terrain, Party[][] parties, Building[][] buildings, int startTurn, int startPlayer, Player[] players){
    this.heightMap = heightMap;
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    this.terrain = terrain;
    this.parties = parties;
    this.buildings = buildings;
    this.startTurn = startTurn;
    this.startPlayer = startPlayer;
    this.players = players;
  }
}

class BattleEstimateManager{
  int currentWins = 0;
  int currentTrials = 0;
  int attackerX;
  int attackerY;
  int defenderX;
  int defenderY;
  int attackerUnits;
  boolean cached = false;
  Party[][] parties;
  BattleEstimateManager(Party[][] parties){
    this.parties = parties;
  }
  BigDecimal getEstimate(int x1, int y1, int x2, int y2, int units){
    try{
      if (parties[y2][x2] == null){
        LOGGER_MAIN.warning("Invalid player location");
      }
      Party tempAttacker = parties[y1][x1].clone();
      tempAttacker.setUnitNumber(units);
      if (cached&&attackerX==x1&&attackerY==y1&&defenderX==x2&&defenderY==y2&&attackerUnits==units){
        int TRIALS = 1000;
        for (int i = 0;i<TRIALS;i++){
          currentWins+=runTrial(tempAttacker, parties[y2][x2]);
        }
        currentTrials+=TRIALS;
      } else {
        cached = true;
        currentWins = 0;
        currentTrials = 0;
        attackerX = x1;
        attackerY = y1;
        defenderX = x2;
        defenderY = y2;
        attackerUnits = units;
        int TRIALS = 10000;
        for (int i = 0;i<TRIALS;i++){
          currentWins+=runTrial(tempAttacker, parties[y2][x2]);
        }
        currentTrials = TRIALS;
      }
      BigDecimal chance = new BigDecimal(""+currentWins).multiply(new BigDecimal(100)).divide(new BigDecimal(""+currentTrials), 1, BigDecimal.ROUND_HALF_UP);
      return chance;
    }
    catch (Exception e){
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting estimate for battle between party at (%s, %s) and (%s, %s)", x1, y1, x2, y2));
      throw e;
    }
  }
  
  int runTrial(Party attacker, Party defender){
    try{
      Battle battle;
      Party clone1;
      Party clone2;
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
        battle = new Battle(clone1, clone2, ".battle");
      }
      while (clone1.getUnitNumber()>0&&clone2.getUnitNumber()>0){
        battle.doBattle();
      }
      if(clone1.getUnitNumber()>0){
        return 1;
      } else {
        return 0;
      }
    }
    catch (Exception e){
      LOGGER_MAIN.log(Level.SEVERE, "Error running battle trial", e);
      throw e;
    }
  }
  void refresh(){
    cached = false;
  }
}
