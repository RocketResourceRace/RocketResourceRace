package state.elements;

import json.JSONManager;
import party.Party;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import static json.JSONManager.findJSONObject;
import static json.JSONManager.gameData;
import static processing.core.PApplet.floor;
import static processing.core.PApplet.min;
import static util.Logging.LOGGER_MAIN;
import static util.Util.roundDp;
import static util.Util.roundDpTrailing;

public class AdvancedTooltip extends Tooltip {
    public boolean visible;
    public boolean attacking;


    //String resourcesList(float[] resources){
    //  String returnString = "";
    //  boolean notNothing = false;
    //  for (int i=0; i<numResources;i++){
    //    if (resources[i]>0){
    //      returnString += roundDp(""+resources[i], 1)+ " " +resourceNames[i]+ ", ";
    //      notNothing = true;
    //    }
    //  }
    //  if (!notNothing)
    //    returnString += "Nothing/Unknown";
    //  else if(returnString.length()-2 > 0)
    //    returnString = returnString.substring(0, returnString.length()-2);
    //  return returnString;
    //}
    public String getResourceList(JSONArray resArray) {
        String returnString = "";
        try {
            for (int i=0; i<resArray.size(); i++) {
                JSONObject jo = resArray.getJSONObject(i);
                returnString += String.format("  %s %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
            throw e;
        }
        return returnString;
    }
    public String getResourceList(JSONArray resArray, float[] availableResources) {
        // Colouring for insufficient resources
        String returnString = "";
        try {
            for (int i=0; i<resArray.size(); i++) {
                JSONObject jo = resArray.getJSONObject(i);
                if (availableResources[JSONManager.getResIndex(jo.getString("id"))] >= jo.getFloat("quantity")) { // Check if has enough resources
                    returnString += String.format("  %s %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
                } else {
                    returnString += String.format("  <r>%s</r> %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
                }
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
            throw e;
        }
        return returnString;
    }

    public void setMoving(int turns, boolean splitting, Party party, int numUnitsSplitting, int cost, boolean is3D) {
        //AdvancedTooltip text if moving. Turns is the number of turns in move
        JSONObject jo = gameData.getJSONObject("tooltips");
        String t = "";
        if (splitting) {
            t = String.format("Split %d units from party and move them to cell.\n", numUnitsSplitting);
            int[] splittedQuantities = party.splittedQuantities(numUnitsSplitting);

            boolean anyEquipment = false;
            for (int i = 0; i < party.getAllEquipment().length; i ++){
                if (party.getEquipment(i) != -1){
                    anyEquipment = true;
                }
            }
            if (anyEquipment){
                t += "\n\nThe equipment will be divided as follows:\n";
            }
            for (int i = 0; i < party.getAllEquipment().length; i ++){
                if (party.getEquipment(i) != -1){
                    // If the party has something equipped for this class
                    t += String.format("%s: New party will get %d, existing party will keep %d", JSONManager.getEquipmentTypeDisplayName(i, party.getEquipment(i)), splittedQuantities[i], party.getEquipmentQuantity(i) - splittedQuantities[i]);
                }
            }
        } else if (turns == 0) {
            t = jo.getString("moving");
            if (is3D) {
                t += String.format("\nMovement Cost: %d", cost);
            }
        }
        if (turns > 0) {
            t += String.format(jo.getString("moving turns"), turns);
        }
        setText(t);
    }

    public void setSieging() {
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(jo.getString("siege"));
    }

    public void setAttacking(BigDecimal chance) {
        attacking = true;
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(String.format(jo.getString("attacking"), chance.toString()));
    }

    public void setBombarding(int damage) {
        setText(String.format("Perform a ranged attack on the party.\nThis will eliminate %d units of the other party", damage));
    }

    public void setBombarding() {
        setText(String.format("Perform a ranged attack on the party."));
    }

    public void setTurnsRemaining() {
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(jo.getString("turns remaining"));
    }
    public void setMoveButton() {
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(jo.getString("move button"));
    }
    public void setMerging(Party p1, Party p2, int unitsTransfered) {
        // p1 is being merged into
        JSONObject jo = gameData.getJSONObject("tooltips");
        int overflow = p1.getOverflow(unitsTransfered);
        String t = String.format(jo.getString("merging"), p2.id, p1.id, unitsTransfered-overflow, overflow);

        int[][] equipments = p1.mergeEquipment(p2, unitsTransfered-overflow);

        boolean hasEquipment = false;
        // New equipment quantities + types for merged party
        t += "\n\nMerged party equipment:";
        for (int i=0; i<JSONManager.getNumEquipmentClasses(); i++){
            if (equipments[0][i] != -1){
                t += String.format("\n%d x %s", equipments[2][i], JSONManager.getEquipmentTypeDisplayName(i, equipments[0][i]));
                hasEquipment = true;
            }
        }
        if (!hasEquipment){
            t += " None\n";
        }

        // New equipment quantities + types for overflow party
        if (overflow > 0){
            hasEquipment = false;
            t += "\n\nOverflow party equipment:";
            for (int i=0; i<JSONManager.getNumEquipmentClasses(); i++){
                if (equipments[1][i] != -1){
                    t += String.format("\n%d x %s", equipments[3][i], JSONManager.getEquipmentTypeDisplayName(i, equipments[1][i]));
                    hasEquipment = true;
                }
            }
            if (!hasEquipment){
                t += " None\n";
            }
        }

        //Merged party proficiencies
        float[] mergedProficiencies = p1.mergeProficiencies(p2, unitsTransfered-overflow);
        t += "\n\nMerged party proficiencies:\n";
        for (int i=0; i < JSONManager.getNumProficiencies(); i ++){
            t += String.format("%s = %s\n", JSONManager.indexToProficiencyDisplayName(i), roundDpTrailing(""+mergedProficiencies[i], 2));
        }

        if (overflow > 0){
            t += "\n\n(Proficiencies for overflow party are the same as the original party)";
        }

        setText(t);
    }

    public void setStockUpAvailable(Party p, float[] resources) {
        int[] equipment = p.equipment;
        String text = "";
        for (int i = 0; i < equipment.length; i++) {
            if (p.equipment[i] != -1) {
                JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
                int stockUpTo = min(p.getUnitNumber(), p.equipmentQuantities[i]+floor(resources[JSONManager.getResIndex(equipmentObject.getString("id"))]));
                if (stockUpTo > p.equipmentQuantities[i]) {
                    String equipmentName = equipmentObject.getString("display name");
                    text += String.format("\n  Will stock %s up to %d.", equipmentName, stockUpTo);
                }
            }
        }
        setText("Stock up equipment. This will use all of the party's movement points."+text);
    }

    public void setStockUpUnavailable(Party p) {
        String text = "Stock up unavailable.";
        boolean hasEquipment = false;
        String list = "";
        for (int i = 0; i < p.equipment.length; i++) {
            if (p.equipment[i] != -1) {
                hasEquipment = true;
                JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
                String name = equipmentObject.getString("display name");
                JSONArray locations = equipmentObject.getJSONArray("valid collection sites");
                list += "\n  "+name+": ";
                for (int j = 0; j < locations.size(); j++) {
                    list += locations.getString(j)+", ";
                }
                list = list.substring(0, list.length()-2);
            }
        }
        if (hasEquipment) {
            text += "\nThe following is where each currently equiped item can be stocked up at:\n"+list;
        } else {
            text += "\nThis party has no equipment selected so it cannot be stocked up.";
        }
        setText(text);
    }

    public void setTask(String task, float[] availibleResources, int movementPoints) {
        try {
            JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task);
            String t="";
            if (jo == null){
                setText("Problem");
                LOGGER_MAIN.warning("Could not find task:"+task);
                return;
            }
            if (!jo.isNull("description")) {
                t += jo.getString("description")+"\n\n";
            }
            if (!jo.isNull("initial cost")) {
                t += String.format("Initial Resource Cost:\n%s\n", getResourceList(jo.getJSONArray("initial cost"), availibleResources));
            }
            if (!jo.isNull("movement points")) {
                if (movementPoints >= jo.getInt("movement points")) {
                    t += String.format("Movement Points: %d\n", jo.getInt("movement points"));
                } else {
                    t += String.format("Movement Points: <r>%d</r>\n", jo.getInt("movement points"));
                }
            }
            if (!jo.isNull("action")) {
                t += String.format("Turns: %d\n", jo.getJSONObject("action").getInt("turns"));
            }
            if (t.length()>2 && (t.charAt(t.length()-1)!='\n' || t.charAt(t.length()-2)!='\n'))
                t += "\n";
            if (!jo.isNull("production")) {
                t += "Production/Turn/Unit:\n"+getResourceList(jo.getJSONArray("production"));
            }
            if (!jo.isNull("consumption")) {
                t += "Consumption/Turn/Unit:\n"+getResourceList(jo.getJSONArray("consumption"));
            }
            //Strip
            setText(t.replaceAll("\\s+$", ""));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error changing tooltip to task: "+task, e);
            throw e;
        }
    }

    public void setEquipment(int equipmentClass, int equipmentType, float[] availableResources, Party party, boolean collectionAllowed){
        // AdvancedTooltip is hovering over equipment manager, specifically over one of the equipmment types
        String t="";
        try{
            if (equipmentClass >= JSONManager.getNumEquipmentClasses()){
                LOGGER_MAIN.warning("equipment class out of bounds");
                return;
            }
            if (equipmentType > JSONManager.getNumEquipmentTypesFromClass(equipmentClass)){
                LOGGER_MAIN.warning("equipment class out of bounds:"+equipmentType);
            } else if (equipmentType == JSONManager.getNumEquipmentTypesFromClass(equipmentClass)){
                JSONObject jo = gameData.getJSONObject("tooltips");
                t += jo.getString("unequip");
                if (collectionAllowed){
                    t += "All equipment will be returned to stockpile";
                }
                else{
                    t += "Equipment will be destroyed";
                }
            } else{
                JSONObject equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(equipmentClass);
                if (equipmentClassJO == null){
                    setText("Problem");
                    LOGGER_MAIN.warning("Equipment class not found with tooltip:"+equipmentClass);
                    return;
                }
                JSONObject equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(equipmentType);
                if (equipmentTypeJO == null){
                    setText("Problem");
                    LOGGER_MAIN.warning("Equipment type not found with tooltip:"+equipmentType);
                    return;
                }
                if (!equipmentTypeJO.isNull("display name")) {
                    t += equipmentTypeJO.getString("display name")+"\n\n";
                }

                if (!equipmentTypeJO.isNull("description")) {
                    t += equipmentTypeJO.getString("description")+"\n\n";
                }

                // Using 'display multipliers' array so ordering is consistant
                if (!equipmentClassJO.isNull("display multipliers")){
                    t += "Proficiency Bonus Multipliers:\n";
                    for (int i=0; i < equipmentClassJO.getJSONArray("display multipliers").size(); i++){
                        String multiplierName = equipmentClassJO.getJSONArray("display multipliers").getString(i);
                        if (!equipmentTypeJO.isNull(multiplierName)){
                            if (equipmentTypeJO.getFloat(multiplierName) > 0){
                                t += String.format("%s: <g>+%s</g>\n", multiplierName, roundDp("+"+equipmentTypeJO.getFloat(multiplierName), 2));
                            } else {
                                t += String.format("%s: <r>%s</r>\n", multiplierName, roundDp(""+equipmentTypeJO.getFloat(multiplierName), 2));
                            }
                        }
                    }
                    t += "\n";
                }

                // Other attributes e.g. range
                if (!equipmentClassJO.isNull("other attributes")){
                    t += "Other Attributes:\n";
                    for (int i=0; i < equipmentClassJO.getJSONArray("other attributes").size(); i++){
                        String attribute = equipmentClassJO.getJSONArray("other attributes").getString(i);
                        if (!equipmentTypeJO.isNull(attribute)){
                            t += String.format("%s: %s\n", attribute, roundDp("+"+equipmentTypeJO.getFloat(attribute), 0));
                        }
                    }
                    t += "\n";
                }

                // Display other classes that are blocked (if applicable)
                if (!equipmentTypeJO.isNull("other class blocking")){
                    t += "Equipment blocks other classes: ";
                    for (int i=0; i < equipmentTypeJO.getJSONArray("other class blocking").size(); i++){
                        if (i < equipmentTypeJO.getJSONArray("other class blocking").size()-1){
                            t += String.format("%s, ", equipmentTypeJO.getJSONArray("other class blocking").getString(i));
                        }
                        else{
                            t += String.format("%s", equipmentTypeJO.getJSONArray("other class blocking").getString(equipmentTypeJO.getJSONArray("other class blocking").size()-1));
                        }
                    }
                    t += "\n\n";
                }

                // Display amount of equipment available vs needed for party
                int resourceIndex = 0;
                try{
                    resourceIndex = JSONManager.getResIndex(equipmentTypeJO.getString("id"));
                }
                catch (Exception e){
                    LOGGER_MAIN.log(Level.WARNING, String.format("Error finding resource for equipment class:%d, type:%d", equipmentClass, equipmentType), e);
                    throw e;
                }
                if (floor(availableResources[resourceIndex]) >= party.getUnitNumber()){
                    t += String.format("Equipment Available: %d/%d", floor(availableResources[resourceIndex]), party.getUnitNumber());
                } else{
                    t += String.format("Equipment Available: <r>%d</r>/%d", floor(availableResources[resourceIndex]), party.getUnitNumber());
                }

                // Show where equipment can be stocked up
                if (!equipmentTypeJO.isNull("valid collection sites")){
                    t += String.format("\n\n%s can be stocked up at: ", equipmentTypeJO.getString("display name"));
                    for (int i=0; i < equipmentTypeJO.getJSONArray("valid collection sites").size(); i ++){
                        t += equipmentTypeJO.getJSONArray("valid collection sites").getString(i);
                        if (i+1 < equipmentTypeJO.getJSONArray("valid collection sites").size()){
                            t += ", ";
                        }
                    }
                }
                else{
                    t += String.format("\n\n%s can be stocked up anywhere", equipmentTypeJO.getString("display name"));
                }

                if (party.getMovementPoints() != party.getMaxMovementPoints()){
                    t += "\n<r>Equipment can only be changed\nif party has full movement points</r>";
                } else{
                    t += "\n(Equipment can only be changed\nif party has full movement points)";
                }
            }

            setText(t.replaceAll("\\s+$", ""));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to equipment class:%dk, type:%d", equipmentClass, equipmentType), e);
            throw e;
        }
    }

    public void setProficiencies(int proficiencyIndex, Party party){
        String t="";
        JSONObject proficiencyJO;
        if (!(0 <= proficiencyIndex && proficiencyIndex < JSONManager.getNumProficiencies())){
            LOGGER_MAIN.warning("Invalid proficiency index given:"+proficiencyIndex);
            return;
        }
        try{
            proficiencyJO = gameData.getJSONArray("proficiencies").getJSONObject(proficiencyIndex);
            if (!proficiencyJO.isNull("display name")){
                t += proficiencyJO.getString("display name") + "\n";
            }
            if (!proficiencyJO.isNull("tooltip")){
                t += proficiencyJO.getString("tooltip") + "\n";
            }
            float bonusMultiplier = party.getProficiencyBonusMultiplier(proficiencyIndex);
            if (bonusMultiplier != 0){
                ArrayList<String> bonusBreakdown = party.getProficiencyBonusMultiplierBreakdown(proficiencyIndex);
                t += "\nBonus breakdown:\n";
                for (int i = 0; i < bonusBreakdown.size(); i ++){
                    t += bonusBreakdown.get(i);
                }
            }

            setText(t.replaceAll("\\s+$", ""));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to proficiencies index:%d", proficiencyIndex), e);
            throw e;
        }
    }

    public void setHoveringParty(Party p){
        String t = String.format("Party '%s'\n", p.id);
        for (int i=0; i < p.proficiencies.length; i++){
            t += String.format("\n%s=%s", JSONManager.indexToProficiencyDisplayName(i), roundDpTrailing(""+p.getTotalProficiency(i), 2));
        }
        setText(t);
    }

    public void setResource(HashMap<String, Float> buildings, String resource) {
        try {
            String t = "";
            for (String building : buildings.keySet()) {
                if (buildings.get(resource)>0) {
                    t += String.format("%s: +%f", building, buildings.get(resource));
                } else {
                    t += String.format("%s: %f", building, buildings.get(resource));
                }
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Error changing tooltip to resource: "+resource, e);
            throw e;
        }
    }



    public void refresh() {
    }
}





