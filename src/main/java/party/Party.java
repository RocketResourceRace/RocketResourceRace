package party;

import event.Action;
import json.JSONManager;
import player.Player;
import processing.core.PApplet;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import static json.JSONManager.JSONIndex;
import static json.JSONManager.gameData;
import static processing.core.PApplet.*;
import static util.Logging.LOGGER_GAME;
import static util.Logging.LOGGER_MAIN;
import static util.Util.between;
import static util.Util.roundDpTrailing;

public class Party {
    public int trainingFocus;
    private int unitNumber;
    public int unitCap;
    public int movementPoints;
    private int maxMovementPoints;
    public float[] proficiencies;
    public int task;
    public int[] equipment;
    public int [] equipmentQuantities;
    public String id;
    public int player;
    public float strength;
    public ArrayList<Action> actions;
    public ArrayList<int[]> path;
    public int[] target;
    public int pathTurns;
    public byte[] byteRep;
    public boolean autoStockUp;
    int sightRadiusPerUnit;

    public Party(int player, int startingUnits, int startingTask, int movementPoints, String id) {
        unitNumber = startingUnits;
        task = startingTask;
        this.player = player;
        this.movementPoints = movementPoints;
        actions = new ArrayList<>();
        strength = 1.5f;
        clearPath();
        target = null;
        pathTurns = 0;
        this.id = id;
        autoStockUp = false;

        this.unitCap = JSONManager.loadIntSetting("party size");  // Set unit cap to default

        // Default proficiencies = 1
        resetRawProficiencies();
        for (int i = 0; i < JSONManager.getNumProficiencies(); i++) {
            this.setRawProficiency(i, 1);
        }

        setTrainingFocus(JSONManager.proficiencyIDToIndex("melee attack"));

        this.equipmentQuantities = new int[JSONManager.getNumEquipmentClasses()];

        equipment = new int[JSONManager.getNumEquipmentClasses()];
        for (int i=0; i<equipment.length;i ++){
            equipment[i] = -1; // -1 represens no equipment
        }

        updateMaxMovementPoints();
    }

    private Party(int player, int startingUnits, int startingTask, int movementPoints, String id, float[] proficiencies, int trainingFocus, int[] equipment, int[] equipmentQuantities, int unitCap, boolean autoStockUp) {
        // For parties that already exist and are being splitted or loaded from save
        unitNumber = startingUnits;
        task = startingTask;
        this.player = player;
        this.movementPoints = movementPoints;
        this.actions = new ArrayList<>();
        this.strength = 1.5f;
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
            for (int i = 0; i < JSONManager.getNumProficiencies(); i++) {
                this.setRawProficiency(i, proficiencies[i]);
            }
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER_MAIN.severe(String.format("Not enough proficiencies given to party:%d (needs %d)  id:%s", proficiencies.length, JSONManager.getNumProficiencies(), id));
        }

        setTrainingFocus(trainingFocus);  // 'trainingFocus' is an id string

        updateMaxMovementPoints();
    }

    public int containsPartyFromPlayer(int p) {
        return parseInt(player == p);
    }

    private float getTrainingRateMultiplier(float x){
        // x is the current value of the proficiency
        return 4*exp(x-1)/pow(exp(x-1)+1, 2);  // This function is based on the derivative of the logisitics function. The factor of 4 is to make it start at 1.
    }

    public void trainParty(String proficiencyID, String trainingConstantID){
        // This method trains the party in one proficiency
        // The rate of training depends on the current level, and gets progressive more difficault to train parties
        // trainingConstantID is the ID for the raw gain, which is an arbitrary value that represents how significant an event is, and so how big the proficiency gain should be

        float currentProficiencyValue = getRawProficiency(JSONManager.proficiencyIDToIndex(proficiencyID));
        float rawGain = JSONManager.getRawProficiencyGain(trainingConstantID);
        float addedProficiencyValue = getTrainingRateMultiplier(currentProficiencyValue) * rawGain;
        if (trainingFocus == JSONManager.proficiencyIDToIndex(proficiencyID)){ // If training focus, then 2x gains from training this proficiency
            addedProficiencyValue *= 2;
        }
        LOGGER_GAME.fine(String.format("Training party id:%s. Raw gain:%f. Added proficiency value: %f. New proficiency value:%f", id, rawGain, addedProficiencyValue, currentProficiencyValue+addedProficiencyValue));
        setRawProficiency(JSONManager.proficiencyIDToIndex(proficiencyID), currentProficiencyValue+addedProficiencyValue);
    }

    public float getEffectivenessMultiplier(String type){
        // This is the funciton to get the effectiveness of the party as different tasks based on proficiencies
        // 'type' is the ID used to determine which constant to use from data.json
        float proficiency = getTotalProficiency(JSONManager.proficiencyIDToIndex(type));
        if (proficiency <= 0){ // proficiencies should never be negative because log(0) is undefined and log(<0) is complex.
            return 0;
        } else {
            return JSONManager.getEffectivenessConstant(type) * log(proficiency) + 1;
        }
    }

    public boolean getAutoStockUp(){
        return autoStockUp;
    }

    public void setAutoStockUp(boolean v){
        autoStockUp = v;
    }

    public void updateMaxMovementPoints(){
        // Based on speed proficiency
        this.maxMovementPoints = floor(gameData.getJSONObject("game options").getInt("movement points") * getEffectivenessMultiplier("speed"));
        setMovementPoints(min(maxMovementPoints, getMovementPoints()));
    }

    public void resetMovementPoints(){
        updateMaxMovementPoints();
        setMovementPoints(maxMovementPoints);
    }

    public int getMaxMovementPoints(){
        return this.maxMovementPoints;
    }

    public void setUnitCap(int value){
        this.unitCap = value;
    }

    public int getUnitCap(){
        return unitCap;
    }

    public void setTrainingFocus(int value) {
        // Training focus is the index of the proficiency in data.json
        this.trainingFocus = value;
    }
    public String getID() {
        return id;
    }

    public int getTask() {
        return task;
    }

    public int getTrainingFocus() {
        // Training focus is the index of the proficiency in data.json
        return this.trainingFocus;
    }

    private void setAllEquipment(int[] v) {
        equipment = v;
    }

    public int[] getAllEquipment() {
        return equipment;
    }

    public int getEquipmentQuantity(int classIndex){
        return equipmentQuantities[classIndex];
    }

    private void setEquipmentQuantity(int classIndex, int quantity){
        equipmentQuantities[classIndex] = quantity;
    }

    public void addEquipmentQuantity(int classIndex, int addedQuantity){
        setEquipmentQuantity(classIndex, getEquipmentQuantity(classIndex)+addedQuantity);
    }

    public int[] getEquipmentQuantities(){
        return equipmentQuantities;
    }

    private void setEquipmentQuantities(int[] v){
        equipmentQuantities = v;
    }

    public void setEquipment(int classIndex, int equipmentIndex, int quantity) {
        if (classIndex >= 0) {
            equipment[classIndex] = equipmentIndex;
            equipmentQuantities[classIndex] = quantity;
            LOGGER_GAME.finer(String.format("changing equipment for party with id:'%s' which now has equipment:%s", id, Arrays.toString(equipment)));
        } else {
            LOGGER_GAME.warning("Attempted to set equipment for invalid class: " + classIndex);
        }
    }

    public int getEquipment(int classIndex) {
        if (classIndex >= 0) {
            return equipment[classIndex];
        } else {
            LOGGER_GAME.warning("Attempted to get equipment for invalid class: " + classIndex);
            return -1;
        }
    }

    public int[] splittedQuantities(int numUnitsSplitted){
        int[] splittedEquipmentQuantities = new int[equipment.length];
        for (int i=0; i < equipment.length; i ++){
            splittedEquipmentQuantities[i] = ceil((getEquipmentQuantity(i) * numUnitsSplitted) / getUnitNumber());
        }
        return splittedEquipmentQuantities;
    }

    public Party splitParty(int numUnitsSplitted, String newID){
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

    public int getOverflow(int unitsTransfered){
        return max((this.getUnitNumber()+unitsTransfered) - this.getUnitCap(), 0);
    }

    public float[] mergeProficiencies(Party other, int unitsTransfered){
        // Other's proficiecies unaffected by merge
        float[] rawProficiencies = new float[proficiencies.length];
        for (int i = 0; i < JSONManager.getNumProficiencies(); i++) {
            rawProficiencies[i] = mergeAttribute(this.getUnitNumber(), this.getRawProficiency(i), unitsTransfered, other.getRawProficiency(i));
        }
        return rawProficiencies;
    }

    public int[][] mergeEquipment(Party other, int unitsTransfered){
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

    public int mergeEntireFrom(Party other, int moveCost, Player player) {
        // Note: will need to remove other division
        LOGGER_GAME.fine(String.format("Merging entire party from id:%s into party with id:%s", other.id, this.id));
        return mergeFrom(other, other.getUnitNumber(), moveCost, player);
    }

    public int mergeFrom(Party other, int unitsTransfered, int moveCost, Player player) {
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
                player.resources[JSONManager.getResIndex(JSONManager.getEquipmentTypeID(i, other.getEquipment(i)))] += other.getEquipmentQuantity(i);
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

    public void setID(String value) {
        this.id = value;
    }

    private float mergeAttribute(int units1, float attrib1, int units2, float attrib2) {
        // Calcaulate the attributes for merge weighted by units number
        return (units1 * attrib1 + units2 * attrib2) / (units1 + units2);
    }

    public void changeTask(int task) {
        //LOGGER_GAME.info("Party changing task to:"+gameData.getJSONArray("tasks").getJSONObject(task).getString("id")); Removed as this is called too much for battle estimates
        try {
            this.task = task;
            JSONObject jTask = gameData.getJSONArray("tasks").getJSONObject(this.getTask());
            if (!jTask.isNull("strength")) {
                this.strength = jTask.getInt("strength");
            } else {
                this.strength = 1.5f;
            }
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, String.format("Error changing party task, id:%s, task=%s. Likely cause is something wrong in data.json", id, task), e);
        }
    }

    public void setPathTurns(int v) {
        LOGGER_GAME.finer(String.format("Setting path turns to:%s, party id:%s", v, id));
        pathTurns = v;
    }

    public void moved() {
        LOGGER_GAME.finest("Decreasing pathTurns due to party moving id: "+id);
        pathTurns = max(pathTurns-1, 0);
    }

    public int[] nextNode() {
        try {
            return path.get(0);
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.WARNING, "Party run out of nodes id:"+id, e);
            return null;
        }
    }

    public void loadPath(ArrayList<int[]> p) {
        LOGGER_GAME.finer("Loading path into party id:"+id);
        path = p;
    }

    public void clearNode() {
        path.remove(0);
    }

    public void clearPath() {
        //LOGGER_GAME.finer("Clearing party path"); Removed as this is called too much for battle estimates
        path = new ArrayList<>();
        pathTurns=0;
    }

    public void addAction(Action a) {
        actions.add(a);
    }

    public boolean hasActions() {
        return actions.size()>0;
    }

    public int turnsLeft() {
        return calcTurns(actions.get(0).turns);
    }

    public int calcTurns(float turnsCost) {
        //Use this to calculate the number of turns a task will take for this party
        return ceil(turnsCost/(sqrt(unitNumber)/10));
    }

    public Action progressAction() {
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
    public void clearCurrentAction() {
        if (actions.size() > 0) {
            LOGGER_GAME.finest(String.format("Clearing party current action of type:%s, id:%s", actions.get(0).type, id));
            actions.remove(0);
        }
    }

    public void clearActions() {
        actions = new ArrayList<Action>();
    }

    public int currentAction() {
        return actions.get(0).type;
    }

    public boolean isTurn(int turn) {
        return this.player==turn;
    }

    public int getMovementPoints() {
        return movementPoints;
    }

    public void subMovementPoints(int p) {
        movementPoints -= p;
    }

    public void setMovementPoints(int p) {
        movementPoints = p;
    }

    public int getMovementPoints(int turn) {
        return movementPoints;
    }

    public int getUnitNumber() {
        return unitNumber;
    }

    public int getUnitNumber(int turn) {
        return unitNumber;
    }

    public void setUnitNumber(int newUnitNumber) {
        unitNumber = (int)between(0, newUnitNumber, JSONManager.loadIntSetting("party size"));
    }

    public int changeUnitNumber(int changeInUnitNumber) {
        int overflow = max(0, changeInUnitNumber+unitNumber-JSONManager.loadIntSetting("party size"));
        this.setUnitNumber(unitNumber+changeInUnitNumber);
        return overflow;
    }

    public int[][] removeExcessEquipment() {
        int[][] excessEquipment = new int[equipment.length][];
        int i = 0;
        for (int quantity: equipmentQuantities) {
            if (equipment[i] != -1) {
                excessEquipment[i] = new int[] {equipment[i], max(0, quantity-unitNumber)};
                LOGGER_GAME.fine(String.format("Removing %d excess %s from party %s", quantity-unitNumber, JSONManager.getEquipmentTypeID(i, equipment[i]), id));
                equipmentQuantities[i] = min(quantity, unitNumber);
            }
            i++;
        }
        return excessEquipment;
    }

    public Party clone() {
        Party newParty = new Party(player, unitNumber, task, movementPoints, id);
        newParty.actions = new ArrayList<>(actions);
        newParty.strength = strength;
        newParty.equipment = equipment.clone();
        newParty.equipmentQuantities = equipmentQuantities.clone();
        newParty.proficiencies = proficiencies.clone();
        newParty.trainingFocus = trainingFocus;
        newParty.unitCap = unitCap;
        newParty.autoStockUp = autoStockUp;
        return newParty;
    }

    private float getRawProficiency(int index) {
        // index is index of proficiency in data.json
        return proficiencies[index];
    }

    private void setRawProficiency(int index, float value) {
        // index is index of proficiency in data.json
        proficiencies[index] = value;
    }

    private void resetRawProficiencies() {
        proficiencies = new float[JSONManager.getNumProficiencies()];
    }

    public float[] getRawProficiencies() {
        return proficiencies;
    }

    private void setRawProficiencies(float[] values) {
        this.proficiencies = values;
    }

    public float getProficiencyBonusMultiplier(int index){
        // index is index of proficiency in data.json
        float bonusMultiplier = 0;
        JSONObject equipmentClassJO;
        JSONObject equipmentTypeJO;
        String proficiencyID = JSONManager.indexToProficiencyID(index);
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

    public ArrayList<String> getProficiencyBonusMultiplierBreakdown(int index){
        // index is index of proficiency in data.json
        // This method is for the breakdown used by tooltip of bonus
        JSONObject equipmentClassJO;
        JSONObject equipmentTypeJO;
        ArrayList<String> returnMe = new ArrayList<>();
        String proficiencyID = JSONManager.indexToProficiencyID(index);
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

    public float getTotalProficiency(int index){
        // USE THIS METHOD FOR BATTLES
        // index is index of proficiency in data.json
        return getRawProficiency(index) * (1+getProficiencyBonusMultiplier(index));
    }

    public float getRawBonusProficiency(int index){
        // For getting bonus amount
        return getProficiencyBonusMultiplier(index) * getRawProficiency(index);
    }

    public float[] getRawBonusProficiencies(){
        float [] r = new float[getRawProficiencies().length];
        for (int i = 0; i < r.length; i++){
            r[i] = getRawBonusProficiency(i);
        }
        return r;
    }

    public boolean capped() {
        return unitNumber == unitCap;
    }

    public int getSightUnitsRadius() {
        return ceil(gameData.getJSONObject("game options").getInt("sight points") * getEffectivenessMultiplier("sight"));
    }

    public int getPlayer() {
        return player;
    }
}
