


class Building {
  int type;
  int image_id;
  Building(int type) {
    this(type, 0);
  }
  Building(int type, int image_id) {
    this.type = type;
    this.image_id = image_id;
  }
}

class Party {
  private int trainingFocus;
  private int unitNumber;
  private int unitCap;
  private int movementPoints, maxMovementPoints;
  private float[] proficiencies;
  private int task;
  private int[] equipment;
  private int [] equipmentQuantities;
  String id;
  int player;
  float strength;
  ArrayList<Action> actions;
  ArrayList<int[]> path;
  int[] target;
  int pathTurns;
  byte[] byteRep;
  boolean autoStockUp;
  int sightRadiusPerUnit;

  Party(int player, int startingUnits, int startingTask, int movementPoints, String id) {
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
    autoStockUp = false;
    
    this.unitCap = jsManager.loadIntSetting("party size");  // Set unit cap to default

    // Default proficiencies = 1
    resetRawProficiencies();
    for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
      this.setRawProficiency(i, 1);
    }

    setTrainingFocus(jsManager.proficiencyIDToIndex("melee attack"));
    
    this.equipmentQuantities = new int[jsManager.getNumEquipmentClasses()];

    equipment = new int[jsManager.getNumEquipmentClasses()];
    for (int i=0; i<equipment.length;i ++){
      equipment[i] = -1; // -1 represens no equipment
    }
    
    updateMaxMovementPoints();
  }

  Party(int player, int startingUnits, int startingTask, int movementPoints, String id, float[] proficiencies, int trainingFocus, int[] equipment, int[] equipmentQuantities, int unitCap, boolean autoStockUp) {
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
    this.unitCap = unitCap;
    this.autoStockUp = autoStockUp;
    
    this.equipment = equipment;
    this.equipmentQuantities = equipmentQuantities;

    // Load proficiencies given
    try {
      resetRawProficiencies();
      for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
        this.setRawProficiency(i, proficiencies[i]);
      }
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.severe(String.format("Not enough proficiencies given to party:%d (needs %d)  id:%s", proficiencies.length, jsManager.getNumProficiencies(), id));
    }

    setTrainingFocus(trainingFocus);  // 'trainingFocus' is an id string
    
    updateMaxMovementPoints();
  }
  
  int containsPartyFromPlayer(int p) {
    return int(player == p);
  }
  
  float getTrainingRateMultiplier(float x){
    // x is the current value of the proficiency
    return 4*exp(x-1)/pow(exp(x-1)+1, 2);  // This function is based on the derivative of the logisitics function. The factor of 4 is to make it start at 1.
  }
  
  void trainParty(String proficiencyID, String trainingConstantID){
    // This method trains the party in one proficiency
    // The rate of training depends on the current level, and gets progressive more difficault to train parties
    // trainingConstantID is the ID for the raw gain, which is an arbitrary value that represents how significant an event is, and so how big the proficiency gain should be
    
    float currentProficiencyValue = getRawProficiency(jsManager.proficiencyIDToIndex(proficiencyID));
    float rawGain = jsManager.getRawProficiencyGain(trainingConstantID);
    float addedProficiencyValue = getTrainingRateMultiplier(currentProficiencyValue) * rawGain;
    if (trainingFocus == jsManager.proficiencyIDToIndex(proficiencyID)){ // If training focus, then 2x gains from training this proficiency
      addedProficiencyValue *= 2;
    }
    LOGGER_GAME.fine(String.format("Training party id:%s. Raw gain:%f. Added proficiency value: %f. New proficiency value:%f", id, rawGain, addedProficiencyValue, currentProficiencyValue+addedProficiencyValue));
    setRawProficiency(jsManager.proficiencyIDToIndex(proficiencyID), currentProficiencyValue+addedProficiencyValue);
  }
  
  float getEffectivenessMultiplier(String type){
    // This is the funciton to get the effectiveness of the party as different tasks based on proficiencies
    // 'type' is the ID used to determine which constant to use from data.json
    float proficiency = getTotalProficiency(jsManager.proficiencyIDToIndex(type));
    if (proficiency <= 0){ // proficiencies should never be negative because log(0) is undefined and log(<0) is complex. 
      return 0;
    } else {
      return jsManager.getEffectivenessConstant(type) * log(proficiency) + 1;
    }
  }
  
  boolean getAutoStockUp(){
    return autoStockUp;
  }
  
  void setAutoStockUp(boolean v){
    autoStockUp = v;
  }
  
  void updateMaxMovementPoints(){
    // Based on speed proficiency
    this.maxMovementPoints = floor(gameData.getJSONObject("game options").getInt("movement points") * getEffectivenessMultiplier("speed"));
    setMovementPoints(min(maxMovementPoints, getMovementPoints()));
  }
  
  void resetMovementPoints(){
    updateMaxMovementPoints();
    setMovementPoints(maxMovementPoints);
  }
  
  int getMaxMovementPoints(){
    return this.maxMovementPoints;
  }
  
  void setUnitCap(int value){
    this.unitCap = value;
  }
  
  int getUnitCap(){
    return unitCap;
  }

  void setTrainingFocus(int value) {
    // Training focus is the index of the proficiency in data.json
    this.trainingFocus = value;
  }
  String getID() {
    return id;
  }
  
  int getTask() {
    return task;
  }

  int getTrainingFocus() {
    // Training focus is the index of the proficiency in data.json
    return this.trainingFocus;
  }

  void setAllEquipment(int[] v) {
    equipment = v;
  }

  int[] getAllEquipment() {
    return equipment;
  }
  
  int getEquipmentQuantity(int classIndex){
    return equipmentQuantities[classIndex];
  }
  
  void setEquipmentQuantity(int classIndex, int quantity){
    equipmentQuantities[classIndex] = quantity;
  }
  
  void addEquipmentQuantity(int classIndex, int addedQuantity){
    setEquipmentQuantity(classIndex, getEquipmentQuantity(classIndex)+addedQuantity);
  }
  
  int[] getEquipmentQuantities(){
    return equipmentQuantities;
  }
  
  void setEquipmentQuantities(int[] v){
    equipmentQuantities = v;
  }

  void setEquipment(int classIndex, int equipmentIndex, int quantity) {
    equipment[classIndex] = equipmentIndex;
    equipmentQuantities[classIndex] = quantity;
    LOGGER_GAME.finer(String.format("changing equipment for party with id:'%s' which now has equipment:%s", id, Arrays.toString(equipment)));
  }

  int getEquipment(int classIndex) {
    return equipment[classIndex];
  }
  
  int[] splittedQuantities(int numUnitsSplitted){
    int[] splittedEquipmentQuantities = new int[equipment.length];
    for (int i=0; i < equipment.length; i ++){
      splittedEquipmentQuantities[i] = ceil(getEquipmentQuantity(i) * numUnitsSplitted / getUnitNumber());
    }
    return splittedEquipmentQuantities;
  }
  
  Party splitParty(int numUnitsSplitted, String newID){
    if (numUnitsSplitted <= getUnitNumber()){
      int[] splittedEquipmentQuantities = splittedQuantities(numUnitsSplitted);
      for (int i=0; i < equipment.length; i ++){
        setEquipmentQuantity(i, getEquipmentQuantity(i) - splittedEquipmentQuantities[i]);
      }
      changeUnitNumber(-numUnitsSplitted);
      return new Party(player, numUnitsSplitted, getTask(), getMovementPoints(), newID, Arrays.copyOf(getRawProficiencies(), getRawProficiencies().length), getTrainingFocus(), Arrays.copyOf(getAllEquipment(), getAllEquipment().length), splittedEquipmentQuantities, getUnitCap(), getAutoStockUp());
    }
    else{
      LOGGER_GAME.warning(String.format("Num units splitted more than in party. ID:%s", numUnitsSplitted));
      return null;
    }
  }
  
  int getOverflow(int unitsTransfered){
    return max((this.getUnitNumber()+unitsTransfered) - this.getUnitCap(), 0);
  }
  
  float[] mergeProficiencies(Party other, int unitsTransfered){
    // Other's proficiecies unaffected by merge
    float[] rawProficiencies = new float[proficiencies.length];
    for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
      rawProficiencies[i] = mergeAttribute(this.getUnitNumber(), this.getRawProficiency(i), unitsTransfered, other.getRawProficiency(i));
    }
    return rawProficiencies;
  }
  
  int[][] mergeEquipment(Party other, int unitsTransfered){
    //index 0 is equipment for this party, index 1 is equipment for other party, index 2 is for quantity of this party, index 3 is for quantity of other party
    int[][] equipments = new int[4][equipment.length];
    for (int i = 0; i < getAllEquipment().length; i ++){
      int amountTransfered = ceil((float)other.getEquipmentQuantity(i) * ((float)unitsTransfered / other.getUnitNumber()));
      if (this.getEquipment(i) != -1 && this.getEquipment(i) == other.getEquipment(i)){
        // If both parties have same equipment then add quantities togther
        equipments[0][i] = this.getEquipment(i);
        equipments[1][i] = other.getEquipment(i);
        equipments[2][i] = this.getEquipmentQuantity(i) + amountTransfered;
        equipments[3][i] = other.getEquipmentQuantity(i) - amountTransfered;
      }
      else if (this.getEquipment(i) == -1 && other.getEquipment(i) != -1){
        // If this party has nothing equipped but the other party has something equipped, equip that.
        equipments[0][i] = other.getEquipment(i);
        equipments[1][i] = other.getEquipment(i);
        equipments[2][i] = amountTransfered;
        equipments[3][i] = other.getEquipmentQuantity(i) - amountTransfered;
      }
      else {
        // Else: quantity stays same
        equipments[0][i] = this.getEquipment(i);
        equipments[1][i] = other.getEquipment(i);
        equipments[2][i] = this.getEquipmentQuantity(i);
        equipments[3][i] = other.getEquipmentQuantity(i);
      }
    }
    return equipments;
  }

  int mergeEntireFrom(Party other, int moveCost, Player player) {
    // Note: will need to remove other division
    LOGGER_GAME.fine(String.format("Merging entire party from id:%s into party with id:%s", other.id, this.id));
    return mergeFrom(other, other.getUnitNumber(), moveCost, player);
  }

  int mergeFrom(Party other, int unitsTransfered, int moveCost, Player player) {
    // Take units from other party into this party and merge attributes, weighted by unit number
    LOGGER_GAME.fine(String.format("Merging %d units from party with id:%s into party with id:%s", unitsTransfered, other.id, this.id));

    int overflow = getOverflow(unitsTransfered);
    
    unitsTransfered -= overflow;  // Dont do anything to the overflow units

    // Merge all proficiencies with other party
    this.setRawProficiencies(mergeProficiencies(other, unitsTransfered));
    
    // Merge equipment
    int[][] equipments = mergeEquipment(other, unitsTransfered);
    for (int i = 0; i < getAllEquipment().length; i ++){
      if (other.getEquipment(i) != equipments[1][i]){
        // Recycle all equipment when it changes type during merge
        player.resources[jsManager.getResIndex(jsManager.getEquipmentTypeID(i, other.getEquipment(i)))] += other.getEquipmentQuantity(i);
      }
    }
    this.setAllEquipment(equipments[0]);
    other.setAllEquipment(equipments[1]);
    this.setEquipmentQuantities(equipments[2]);
    other.setEquipmentQuantities(equipments[3]);

    LOGGER_GAME.finer(String.format("New proficiency values: %s for party with id:%s", Arrays.toString(proficiencies), id));
    // Note: other division attributes unaffected by merge
    
    int movementPoints = min(this.getMovementPoints(), other.getMovementPoints()-moveCost);
    this.setMovementPoints(movementPoints);
    
    this.changeUnitNumber(unitsTransfered); // Units left over after merging
    other.changeUnitNumber(-unitsTransfered);

    return overflow; // Return units left in othr party
  }

  void setID(String value) {
    this.id = value;
  }

  float mergeAttribute(int units1, float attrib1, int units2, float attrib2) {
    // Calcaulate the attributes for merge weighted by units number
    return (units1 * attrib1 + units2 * attrib2) / (units1 + units2);
  }

  void changeTask(int task) {
    //LOGGER_GAME.info("Party changing task to:"+gameData.getJSONArray("tasks").getJSONObject(task).getString("id")); Removed as this is called too much for battle estimates
    try {
      this.task = task;
      JSONObject jTask = gameData.getJSONArray("tasks").getJSONObject(this.getTask());
      if (!jTask.isNull("strength")) {
        this.strength = jTask.getInt("strength");
      } else {
        this.strength = 1.5;
      }
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.WARNING, String.format("Error changing party task, id:%s, task=%s. Likely cause is something wrong in data.json", id, task), e);
    }
  }

  void setPathTurns(int v) {
    LOGGER_GAME.finer(String.format("Setting path turns to:%s, party id:%s", v, id));
    pathTurns = v;
  }

  void moved() {
    LOGGER_GAME.finest("Decreasing pathTurns due to party moving id: "+id);
    pathTurns = max(pathTurns-1, 0);
  }

  int[] nextNode() {
    try {
      return path.get(0);
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.WARNING, "Party run out of nodes id:"+id, e);
      return null;
    }
  }

  void loadPath(ArrayList<int[]> p) {
    LOGGER_GAME.finer("Loading path into party id:"+id);
    path = p;
  }

  void clearNode() {
    path.remove(0);
  }
  
  void clearPath() {
    //LOGGER_GAME.finer("Clearing party path"); Removed as this is called too much for battle estimates
    path = new ArrayList<int[]>();
    pathTurns=0;
  }
  
  void addAction(Action a) {
    actions.add(a);
  }
  
  boolean hasActions() {
    return actions.size()>0;
  }
  
  int turnsLeft() {
    return calcTurns(actions.get(0).turns);
  }

  int calcTurns(float turnsCost) {
    //Use this to calculate the number of turns a task will take for this party
    return ceil(turnsCost/(sqrt(unitNumber)/10));
  }
  
  Action progressAction() {
    try {
      if (actions.size() == 0) {
        return null;
      }
      LOGGER_GAME.finer(String.format("Party action progressing: '%s', id:%s", actions.get(0).type, id));
      if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0) {
        return actions.get(0);
      } else {
        actions.get(0).turns -= sqrt((float)unitNumber)/10;
        if (gameData.getJSONArray("tasks").getJSONObject(actions.get(0).type).getString("id").contains("Build")) {
          if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0) {
            return new Action(JSONIndex(gameData.getJSONArray("tasks"), "Construction End"), "Construction End", 0, null, null);
          } else {
            return new Action(JSONIndex(gameData.getJSONArray("tasks"), "Construction Mid"), "Construction Mid", 0, null, null);
          }
        }
        return null;
      }
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Progressing party action failed id:"+id);
      throw e;
    }
  }
  void clearCurrentAction() {
    if (actions.size() > 0) {
      LOGGER_GAME.finest(String.format("Clearing party current action of type:%s, id:%s", actions.get(0).type, id));
      actions.remove(0);
    }
  }
  
  void clearActions() {
    actions = new ArrayList<Action>();
  }

  int currentAction() {
    return actions.get(0).type;
  }

  boolean isTurn(int turn) {
    return this.player==turn;
  }

  int getMovementPoints() {
    return movementPoints;
  }

  void subMovementPoints(int p) {
    movementPoints -= p;
  }

  void setMovementPoints(int p) {
    movementPoints = p;
  }

  int getMovementPoints(int turn) {
    return movementPoints;
  }

  int getUnitNumber() {
    return unitNumber;
  }

  int getUnitNumber(int turn) {
    return unitNumber;
  }

  void setUnitNumber(int newUnitNumber) {
    unitNumber = (int)between(0, newUnitNumber, jsManager.loadIntSetting("party size"));
  }

  int changeUnitNumber(int changeInUnitNumber) {
    int overflow = max(0, changeInUnitNumber+unitNumber-jsManager.loadIntSetting("party size"));
    this.setUnitNumber(unitNumber+changeInUnitNumber);
    return overflow;
  }

  int[][] removeExcessEquipment() {
    int[][] excessEquipment = new int[equipment.length][];
    int i = 0;
    for (int quantity: equipmentQuantities) {
      if (equipment[i] != -1) {
        excessEquipment[i] = new int[] {equipment[i], max(0, quantity-unitNumber)};
        LOGGER_GAME.fine(String.format("Removing %d excess %s from party %s", quantity-unitNumber, jsManager.getEquipmentTypeID(i, equipment[i]), id)); 
        equipmentQuantities[i] = min(quantity, unitNumber);
      }
      i++;
    }
    return excessEquipment;
  }

  Party clone() {
    Party newParty = new Party(player, unitNumber, task, movementPoints, id);
    newParty.actions = new ArrayList<Action>(actions);
    newParty.strength = strength;
    newParty.equipment = equipment.clone();
    newParty.equipmentQuantities = equipmentQuantities.clone();
    newParty.proficiencies = proficiencies.clone();
    newParty.trainingFocus = trainingFocus;
    newParty.unitCap = unitCap;
    newParty.autoStockUp = autoStockUp;
    return newParty;
  }

  float getRawProficiency(int index) {
    // index is index of proficiency in data.json
    return proficiencies[index];
  }

  void setRawProficiency(int index, float value) {
    // index is index of proficiency in data.json
    proficiencies[index] = value;
  }

  void resetRawProficiencies() {
    proficiencies = new float[jsManager.getNumProficiencies()];
  }

  float[] getRawProficiencies() {
    return proficiencies;
  }

  void setRawProficiencies(float[] values) {
    this.proficiencies = values;
  }
  
  float getProficiencyBonusMultiplier(int index){
    // index is index of proficiency in data.json
    float bonusMultiplier = 0;
    JSONObject equipmentClassJO;
    JSONObject equipmentTypeJO;
    String proficiencyID = jsManager.indexToProficiencyID(index);
    for (int i = 0 ; i < getAllEquipment().length; i++){
      if (getEquipment(i) != -1){
        try{
          equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(i);
          equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(getEquipment(i));
          
          // Check each equipment equipped for proficiencies to calculate bonus
          if (!equipmentTypeJO.isNull(proficiencyID)){
            bonusMultiplier += equipmentTypeJO.getFloat(proficiencyID) * ((float)getEquipmentQuantity(i)/getUnitNumber());  // Weight each bonus by the proportion of units that have access to equipment
          }
        }
        catch (Exception e){
          LOGGER_GAME.log(Level.WARNING, "Error loading equipment", e);
        }
      }
    }
    return bonusMultiplier;
  }
  
  ArrayList<String> getProficiencyBonusMultiplierBreakdown(int index){
    // index is index of proficiency in data.json
    // This method is for the breakdown used by tooltip of bonus
    JSONObject equipmentClassJO;
    JSONObject equipmentTypeJO;
    ArrayList<String> returnMe = new ArrayList<String>();
    String proficiencyID = jsManager.indexToProficiencyID(index);
    for (int i = 0 ; i < getAllEquipment().length; i++){
      if (getEquipment(i) != -1){
        try{
          equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(i);
          equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(getEquipment(i));
          
          // Check each equipment equipped for proficiencies to calculate bonus
          if (!equipmentTypeJO.isNull(proficiencyID)){
            if (equipmentTypeJO.getFloat(proficiencyID) > 0){
              returnMe.add(String.format("<g>+%s</g> from %s (%d/%d)", roundDpTrailing("+"+getRawProficiency(index)*equipmentTypeJO.getFloat(proficiencyID) * ((float)getEquipmentQuantity(i)/getUnitNumber()), 2), equipmentTypeJO.getString("display name"), getEquipmentQuantity(i), getUnitNumber()));
            }
            else if (equipmentTypeJO.getFloat(proficiencyID) < 0){
              returnMe.add(String.format("<g>%s</g> from %s (%d/%d)", roundDpTrailing("+"+getRawProficiency(index)*equipmentTypeJO.getFloat(proficiencyID) * ((float)getEquipmentQuantity(i)/getUnitNumber()), 2), equipmentTypeJO.getString("display name"), getEquipmentQuantity(i), getUnitNumber()));
            }
          }
        }
        catch (Exception e){
          LOGGER_GAME.log(Level.WARNING, "Error loading equipment", e);
        }
      }
    }
    return returnMe;
  }
  
  float getTotalProficiency(int index){
    // USE THIS METHOD FOR BATTLES
    // index is index of proficiency in data.json
    return getRawProficiency(index) * (1+getProficiencyBonusMultiplier(index));
  }
  
  float getRawBonusProficiency(int index){
    // For getting bonus amount
    return getProficiencyBonusMultiplier(index) * getRawProficiency(index);
  }
  
  float[] getRawBonusProficiencies(){
    float [] r = new float[getRawProficiencies().length];
    for (int i = 0; i < r.length; i++){
      r[i] = getRawBonusProficiency(i);
    }
    return r;
  }
  
  boolean capped() {
    return unitNumber == unitCap;
  }
  
  int getSightUnitsRadius() {
    return ceil(gameData.getJSONObject("game options").getInt("sight points") * getEffectivenessMultiplier("sight"));
  }
}

class Battle extends Party {
  Party attacker;
  Party defender;
  Battle(Party attacker, Party defender, String id) {
    super(-1, attacker.getUnitNumber()+defender.getUnitNumber(), JSONIndex(gameData.getJSONArray("tasks"), "Battle"), 0, id);
    this.attacker = attacker;
    attacker.strength = 2;
    this.defender = defender;
  }
  
  int containsPartyFromPlayer(int p) {
    if (attacker.player == p) {
      return 1;
    } else if (defender.player == p) {
      return 2;
    }
    return 0;
  }
  
  boolean isTurn(int turn) {
    return true;
  }
  int getMovementPoints(int turn) {
    if (turn==attacker.player) {
      return attacker.getMovementPoints();
    } else {
      return defender.getMovementPoints();
    }
  }
  void setUnitNumber(int turn, int newUnitNumber) {
    if (turn==attacker.player) {
      attacker.setUnitNumber(newUnitNumber);
    } else {
      defender.setUnitNumber(newUnitNumber);
    }
  }
  int getUnitNumber(int turn) {
    if (turn==attacker.player) {
      return attacker.getUnitNumber();
    } else {
      return defender.getUnitNumber();
    }
  }
  int changeUnitNumber(int turn, int changeInUnitNumber) {
    if (turn==this.attacker.player) {
      int overflow = max(0, changeInUnitNumber+attacker.getUnitNumber()-jsManager.loadIntSetting("party size"));
      this.attacker.setUnitNumber(attacker.getUnitNumber()+changeInUnitNumber);
      return overflow;
    } else {
      int overflow = max(0, changeInUnitNumber+defender.getUnitNumber()-jsManager.loadIntSetting("party size"));
      this.defender.setUnitNumber(defender.getUnitNumber()+changeInUnitNumber);
      return overflow;
    }
  }
  Party doBattle() {
    try {
      int changeInParty1 = getBattleUnitChange(attacker, defender);
      int changeInParty2 = getBattleUnitChange(defender, attacker);
      attacker.strength = 1;
      defender.strength = 1;
      int newParty1Size = attacker.getUnitNumber()+changeInParty1;
      int newParty2Size = defender.getUnitNumber()+changeInParty2;
      int endDifference = newParty1Size-newParty2Size;
      attacker.setUnitNumber(newParty1Size);
      defender.setUnitNumber(newParty2Size);
      if (attacker.getUnitNumber()==0) {
        if (defender.getUnitNumber()==0) {
          if (endDifference==0) {
            return null;
          } else if (endDifference>0) {
            attacker.setUnitNumber(endDifference);
            attacker.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            return attacker;
          } else {
            defender.setUnitNumber(-endDifference);
            defender.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            return defender;
          }
        } else {
          defender.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          return defender;
        }
      } 
      if (defender.getUnitNumber()==0) {
        attacker.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
        return attacker;
      } else {
        return this;
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error doing battle", e);
      throw e;
    }
  }
  Battle clone() {
    Battle newParty = new Battle(this.attacker.clone(), this.defender.clone(), id);
    return newParty;
  }
  
  int mergeEntireFrom(Party other, int moveCost, Player player) {
    // Note: will need to remove other party
    LOGGER_GAME.fine(String.format("Merging entire party from id:%s into battle with id:%s", other.id, this.id));
    return mergeFrom(other, other.getUnitNumber(), moveCost, player);
  }

  int mergeFrom(Party other, int unitsTransfered, int moveCost, Player player) {
    // Take units from other party into this party and merge attributes, weighted by unit number
    LOGGER_GAME.fine(String.format("Merging %d units from party with id:%s into battle with id:%s", unitsTransfered, other.id, this.id));
    // THIS NEEDS TO BE CHANGED FOR DIPLOMACY
    if (attacker.player == other.player) {
      return attacker.mergeFrom(other, unitsTransfered, moveCost, player);
    } else if (defender.player == other.player) {
      return defender.mergeFrom(other, unitsTransfered, moveCost, player);
    } else {
      return unitsTransfered;
    }
    //
  }
}

class Siege extends Party {
  Siege (Party attacker, Building defence, Party garrison, String id) {
    super(-2, attacker.getUnitNumber()+garrison.getUnitNumber(), JSONIndex(gameData.getJSONArray("tasks"), "Siege"), 0, id);
  }
}

// THIS NEEDS TO BE CHANGED FOR DIPLOMACY
int getBattleUnitChange(Party p1, Party p2) {
  float damageRating = p2.strength * p2.getEffectivenessMultiplier("melee attack") /
  (p1.strength * p1.getEffectivenessMultiplier("defence"));
  return floor(-0.2 * (p2.getUnitNumber() + pow(p2.getUnitNumber(), 2) / p1.getUnitNumber()) * random(0.75, 1.5) * damageRating);
}
//

boolean playerExists(Player[] players, String name) {
  for (Player p: players) {
    if (p.name.trim().equals(name.trim())){
      return true;
    }
  }
  return false;
}

Player getPlayer(Player[] players, String name){
  for (Player p: players) {
    if (p.name.trim().equals(name.trim())){
      return p;
    }
  }
  LOGGER_MAIN.severe(String.format("Tried to find player: %s but this player was not found. Returned first player, this will likely cause problems", name));
  return players[0];
}


class Player {
  private int id;
  float cameraCellX, cameraCellY, blockSize;
  float[] resources;
  int cellX, cellY, colour;
  boolean cellSelected = false;
  String name;
  boolean isAlive = true;
  PlayerController playerController;
  int controllerType;  // 0 for local, 1 for bandits
  Cell[][] visibleCells;
  
  // Resources: food wood metal energy concrete cable spaceship_parts ore people
  Player(float x, float y, float blockSize, float[] resources, int colour, String name, int controllerType, int id) {
    this.cameraCellX = x;
    this.cameraCellY = y;
    this.blockSize = blockSize;
    this.resources = resources;
    this.colour = colour;
    this.name = name;
    this.id = id;
    
    this.visibleCells = new Cell[jsManager.loadIntSetting("map size")][jsManager.loadIntSetting("map size")];
    this.controllerType = controllerType;
    switch(controllerType){
      case 1:
        playerController = new BanditController(id, jsManager.loadIntSetting("map size"), jsManager.loadIntSetting("map size"));
        break;
      default:
        playerController = null;
        break;
    }
  }
  
  Node[][] sightDijkstra(int x, int y, Party[][] parties, int[][] terrain) {
    int w = visibleCells[0].length;
    int h = visibleCells.length;
    int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
    Node currentHeadNode;
    Node[][] nodes = new Node[h][w];
    nodes[y][x] = new Node(0, false, x, y, x, y);
    PriorityQueue<Node> curMinNodes = new PriorityQueue<Node>(new NodeComparator());
    curMinNodes.add(nodes[y][x]);
    while (curMinNodes.size() > 0) {
      currentHeadNode = curMinNodes.poll();
      currentHeadNode.fixed = true;
  
      for (int[] mv : mvs) {
        int nx = currentHeadNode.x+mv[0];
        int ny = currentHeadNode.y+mv[1];
        if (0 <= nx && nx < w && 0 <= ny && ny < h) {
          int newCost = sightCost(nx, ny, currentHeadNode.x, currentHeadNode.y, terrain);
          int prevCost = currentHeadNode.cost;
          if (newCost != -1){ // Check that the cost is valid
            int totalNewCost = prevCost+newCost;
            if (totalNewCost < parties[y][x].getSightUnitsRadius()) {
              if (nodes[ny][nx] == null) {
                nodes[ny][nx] = new Node(totalNewCost, false, currentHeadNode.x, currentHeadNode.y, nx, ny);
                curMinNodes.add(nodes[ny][nx]);
              } else if (!nodes[ny][nx].fixed) {
                if (totalNewCost < nodes[ny][nx].cost) { // Updating existing node
                  nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
                  nodes[ny][nx].setPrev(currentHeadNode.x, currentHeadNode.y);
                  curMinNodes.remove(nodes[ny][nx]);
                  curMinNodes.add(nodes[ny][nx]);
                }
              }
            }
          }
        }
      }
    }
    return nodes;
  }
  
  boolean[][] generateFogMap(Party[][] parties, int[][] terrain) {
    int w = parties[0].length;
    int h = parties.length;
    boolean[][] fogMap = new boolean[h][w];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if (parties[y][x] != null && (parties[y][x].player == id || parties[y][x].containsPartyFromPlayer(id) > 0) && parties[y][x].getUnitNumber() > 0) {
          Node[][] nodes = sightDijkstra(x, y, parties, terrain);
          for (int y1 = max(0, y - parties[y][x].getSightUnitsRadius()); y1 < min(h, y + parties[y][x].getSightUnitsRadius()+1); y1++) {
            for (int x1 = max(0, x - parties[y][x].getSightUnitsRadius()); x1 < min(w, x + parties[y][x].getSightUnitsRadius()+1); x1++) {
              if (nodes[y1][x1] != null && nodes[y1][x1].cost <= parties[y][x].getSightUnitsRadius()) {
                fogMap[y1][x1] = true;
              }
            }
          }
        }
      }
    }
    
    return fogMap;
  }
  
  void updateVisibleCells(int[][] terrain, Building[][] buildings, Party[][] parties, boolean[][] seenCells){
    /* 
    Run after every event for this player, and it updates the visibleCells taking into account fog of war.
    Cells that have not been discovered yet will be null, and cells that are in active sight will be updated with the latest infomation.
    */
    LOGGER_MAIN.fine("Updating visible cells for player " + name);
    boolean[][] fogMap = generateFogMap(parties, terrain);
    
    for (int y = 0; y < visibleCells.length; y++) {
      for (int x = 0; x < visibleCells[0].length; x++) {
        if (visibleCells[y][x] == null && (seenCells == null || !seenCells[y][x])) {
          if (fogMap[y][x]) {
            visibleCells[y][x] = new Cell(terrain[y][x], buildings[y][x], parties[y][x]);
            visibleCells[y][x].setActiveSight(true);
          }
        } else {
          if (visibleCells[y][x] == null) {
            visibleCells[y][x] = new Cell(terrain[y][x], buildings[y][x], null);
          } else {
            visibleCells[y][x].setTerrain(terrain[y][x]);
            visibleCells[y][x].setBuilding(buildings[y][x]);
          }
          visibleCells[y][x].setActiveSight(fogMap[y][x]);
          if (visibleCells[y][x].getActiveSight()) {
            visibleCells[y][x].setParty(parties[y][x]);
          } else {
            visibleCells[y][x].setParty(null);
          }
        }
      }
    }
  }
  
  void updateVisibleCells(int[][] terrain, Building[][] buildings, Party[][] parties){
    updateVisibleCells(terrain, buildings, parties, null);
  }
  
  void saveSettings(float x, float y, float blockSize, int cellX, int cellY, boolean cellSelected) {
    this.cameraCellX = x;
    this.cameraCellY = y;
    this.blockSize = blockSize;
    this.cellX = cellX;
    this.cellY = cellY;
    this.cellSelected = cellSelected;
  }
  void loadSettings(Game g, Map m) {
    LOGGER_MAIN.fine("Loading player camera settings");
    m.loadSettings(cameraCellX, cameraCellY, blockSize);
    if (cellSelected) {
      g.selectCell((int)this.cellX, (int)this.cellY, false);
    } else {
      g.deselectCell();
    }
  }
  
  GameEvent generateNextEvent(){
    // This method will be run continuously until it returns an end turn event
    return playerController.generateNextEvent(visibleCells, resources);
  }
}


interface PlayerController {
  GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]);
}


class Cell {
  private int terrain;
  private Building building;
  private Party party;
  private boolean activeSight;
  
  Cell(int terrain, Building building, Party party){
    this.terrain = terrain;
    this.building = building;
    this.party = party;
    this.activeSight = false; // Needs to be updated later
  }
  
  int getTerrain(){
    return terrain;
  }
  Building getBuilding(){
    return building;
  }
  Party getParty(){
    return party;
  }
  boolean getActiveSight(){
    return activeSight;
  }
  void setTerrain(int terrain){
    this.terrain = terrain;
  }
  void setBuilding(Building building){
    this.building = building;
  }
  void setParty(Party party){
    this.party = party;
  }
  void setActiveSight(boolean activeSight){
    this.activeSight = activeSight;
  }
}



class Node {
  int cost;
  boolean fixed;
  int prevX = -1, prevY = -1;
  int x, y;

  Node(int cost, boolean fixed, int prevX, int prevY) {
    this.fixed = fixed;
    this.cost = cost;
    this.prevX = prevX;
    this.prevY = prevY;
  }

  Node(int cost, boolean fixed, int prevX, int prevY, int x, int y) {
    this.fixed = fixed;
    this.cost = cost;
    this.prevX = prevX;
    this.prevY = prevY;
    this.x = x;
    this.y = y;
  }
  void setPrev(int prevX, int prevY) {
    this.prevX = prevX;
    this.prevY = prevY;
  }
}



class MapSave {
  float[] heightMap;
  int mapWidth, mapHeight;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  int startTurn;
  int startPlayer;
  Player[] players;
  MapSave(float[] heightMap, int mapWidth, int mapHeight, int[][] terrain, Party[][] parties, Building[][] buildings, int startTurn, int startPlayer, Player[] players) {
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

class BattleEstimateManager {
  int currentWins = 0;
  int currentTrials = 0;
  int attackerX;
  int attackerY;
  int defenderX;
  int defenderY;
  int attackerUnits;
  boolean cached = false;
  Party[][] parties;
  BattleEstimateManager(Party[][] parties) {
    this.parties = parties;
  }
  BigDecimal getEstimate(int x1, int y1, int x2, int y2, int units) {
    try {
      if (parties[y2][x2] == null) {
        LOGGER_MAIN.warning("Invalid player location");
      }
      Party tempAttacker = parties[y1][x1].clone();
      tempAttacker.setUnitNumber(units);
      if (cached&&attackerX==x1&&attackerY==y1&&defenderX==x2&&defenderY==y2&&attackerUnits==units) {
        int TRIALS = 1000;
        for (int i = 0; i<TRIALS; i++) {
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
        for (int i = 0; i<TRIALS; i++) {
          currentWins+=runTrial(tempAttacker, parties[y2][x2]);
        }
        currentTrials = TRIALS;
      }
      BigDecimal chance = new BigDecimal(""+currentWins).multiply(new BigDecimal(100)).divide(new BigDecimal(""+currentTrials), 1, BigDecimal.ROUND_HALF_UP);
      return chance;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting estimate for battle between party at (%s, %s) and (%s, %s)", x1, y1, x2, y2));
      throw e;
    }
  }
  
  // THIS NEEDS TO BE CHANGED FOR DIPLOMACY
  //
  void refresh() {
    cached = false;
  }
}

int runTrial(Party attacker, Party defender) {
  try {
    Battle battle;
    Party clone1;
    Party clone2;
    if (defender instanceof Battle) {
      battle = (Battle) defender.clone();
      battle.changeUnitNumber(attacker.player, attacker.getUnitNumber());
      if (battle.attacker.player==attacker.player) {
        clone1 = battle.attacker;
        clone2 = battle.defender;
      } else {
        clone1 = battle.defender;
        clone2 = battle.attacker;
      }
    } else {
      clone1 = attacker.clone();
      clone2 = defender.clone();
      battle = new Battle(clone1, clone2, ".battle");
    }
    while (clone1.getUnitNumber()>0&&clone2.getUnitNumber()>0) {
      battle.doBattle();
    }
    if (clone1.getUnitNumber()>0) {
      return 1;
    } else {
      return 0;
    }
  }
  catch (Exception e) {
    LOGGER_MAIN.log(Level.SEVERE, "Error running battle trial", e);
    throw e;
  }
}
