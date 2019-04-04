package ui.element;

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
import static processing.core.PApplet.*;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class AdvancedTooltip extends Tooltip {
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
    private String getResourceList(JSONArray resArray) {
        StringBuilder returnString = new StringBuilder();
        try {
            for (int i=0; i<resArray.size(); i++) {
                JSONObject jo = resArray.getJSONObject(i);
                returnString.append(String.format("  %s %s\n", roundDp("" + jo.getFloat("quantity"), 2), jo.getString("id")));
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
            throw e;
        }
        return returnString.toString();
    }
    private String getResourceList(JSONArray resArray, float[] availableResources) {
        // Colouring for insufficient resources
        StringBuilder returnString = new StringBuilder();
        try {
            for (int i=0; i<resArray.size(); i++) {
                JSONObject jo = resArray.getJSONObject(i);
                if (availableResources[JSONManager.getResIndex(jo.getString("id"))] >= jo.getFloat("quantity")) { // Check if has enough resources
                    returnString.append(String.format("  %s %s\n", roundDp("" + jo.getFloat("quantity"), 2), jo.getString("id")));
                } else {
                    returnString.append(String.format("  <r>%s</r> %s\n", roundDp("" + jo.getFloat("quantity"), 2), jo.getString("id")));
                }
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
            throw e;
        }
        return returnString.toString();
    }

    public void setMoving(int turns, boolean splitting, Party party, int numUnitsSplitting, int cost, boolean is3D) {
        //AdvancedTooltip text if moving. Turns is the number of turns in move
        JSONObject jo = gameData.getJSONObject("tooltips");
        StringBuilder t = new StringBuilder();
        if (splitting) {
            t = new StringBuilder(String.format("Split %d units from party and move them to cell.\n", numUnitsSplitting));
            int[] splittedQuantities = party.splittedQuantities(numUnitsSplitting);

            boolean anyEquipment = false;
            for (int i = 0; i < party.getAllEquipment().length; i ++){
                if (party.getEquipment(i) != -1){
                    anyEquipment = true;
                }
            }
            if (anyEquipment){
                t.append("\n\nThe equipment will be divided as follows:\n");
            }
            for (int i = 0; i < party.getAllEquipment().length; i ++){
                if (party.getEquipment(i) != -1){
                    // If the party has something equipped for this class
                    t.append(String.format("%s: New party will get %d, existing party will keep %d", JSONManager.getEquipmentTypeDisplayName(i, party.getEquipment(i)), splittedQuantities[i], party.getEquipmentQuantity(i) - splittedQuantities[i]));
                }
            }
        } else if (turns == 0) {
            t = new StringBuilder(jo.getString("moving"));
            if (is3D) {
                t.append(String.format("\nMovement Cost: %d", cost));
            }
        }
        if (turns > 0) {
            t.append(String.format(jo.getString("moving turns"), turns));
        }
        setText(t.toString());
        show();
    }

    public void setSieging() {
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(jo.getString("siege"));
        show();
    }

    public void setAttacking(BigDecimal chance) {
        attacking = true;
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(String.format(jo.getString("attacking"), chance.toString()));
        show();
    }

    public void setBombarding(int damage) {
        setText(String.format("Perform a ranged attack on the party.\nThis will eliminate %d units of the other party", damage));
        show();
    }

    public void setTurnsRemaining() {
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(jo.getString("turns remaining"));
        show();
    }
    public void setMoveButton() {
        JSONObject jo = gameData.getJSONObject("tooltips");
        setText(jo.getString("move button"));
        show();
    }
    public void setMerging(Party p1, Party p2, int unitsTransfered) {
        // p1 is being merged into
        JSONObject jo = gameData.getJSONObject("tooltips");
        int overflow = p1.getOverflow(unitsTransfered);
        StringBuilder t = new StringBuilder(String.format(jo.getString("merging"), p2.id, p1.id, unitsTransfered - overflow, overflow));

        int[][] equipments = p1.mergeEquipment(p2, unitsTransfered-overflow);

        boolean hasEquipment = false;
        // New equipment quantities + types for merged party
        t.append("\n\nMerged party equipment:");
        for (int i=0; i<JSONManager.getNumEquipmentClasses(); i++){
            if (equipments[0][i] != -1){
                t.append(String.format("\n%d x %s", equipments[2][i], JSONManager.getEquipmentTypeDisplayName(i, equipments[0][i])));
                hasEquipment = true;
            }
        }
        if (!hasEquipment){
            t.append(" None\n");
        }

        // New equipment quantities + types for overflow party
        if (overflow > 0){
            hasEquipment = false;
            t.append("\n\nOverflow party equipment:");
            for (int i=0; i<JSONManager.getNumEquipmentClasses(); i++){
                if (equipments[1][i] != -1){
                    t.append(String.format("\n%d x %s", equipments[3][i], JSONManager.getEquipmentTypeDisplayName(i, equipments[1][i])));
                    hasEquipment = true;
                }
            }
            if (!hasEquipment){
                t.append(" None\n");
            }
        }

        //Merged party proficiencies
        float[] mergedProficiencies = p1.mergeProficiencies(p2, unitsTransfered-overflow);
        t.append("\n\nMerged party proficiencies:\n");
        for (int i=0; i < JSONManager.getNumProficiencies(); i ++){
            t.append(String.format("%s = %s\n", JSONManager.indexToProficiencyDisplayName(i), roundDpTrailing("" + mergedProficiencies[i], 2)));
        }

        if (overflow > 0){
            t.append("\n\n(Proficiencies for overflow party are the same as the original party)");
        }

        setText(t.toString());
        show();
    }

    public void setStockUpAvailable(Party p, float[] resources) {
        int[] equipment = p.equipment;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < equipment.length; i++) {
            if (p.equipment[i] != -1) {
                JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
                int stockUpTo = min(p.getUnitNumber(), p.equipmentQuantities[i]+floor(resources[JSONManager.getResIndex(equipmentObject.getString("id"))]));
                if (stockUpTo > p.equipmentQuantities[i]) {
                    String equipmentName = equipmentObject.getString("display name");
                    text.append(String.format("\n  Will stock %s up to %d.", equipmentName, stockUpTo));
                }
            }
        }
        setText("Stock up equipment. This will use all of the party's movement points."+text);
        show();
    }

    public void setStockUpUnavailable(Party p) {
        String text = "Stock up unavailable.";
        boolean hasEquipment = false;
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < p.equipment.length; i++) {
            if (p.equipment[i] != -1) {
                hasEquipment = true;
                JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
                String name = equipmentObject.getString("display name");
                JSONArray locations = equipmentObject.getJSONArray("valid collection sites");
                list.append("\n  ").append(name).append(": ");
                for (int j = 0; j < locations.size(); j++) {
                    list.append(locations.getString(j)).append(", ");
                }
                list = new StringBuilder(list.substring(0, list.length() - 2));
            }
        }
        if (hasEquipment) {
            text += "\nThe following is where each currently equiped item can be stocked up at:\n"+list;
        } else {
            text += "\nThis party has no equipment selected so it cannot be stocked up.";
        }
        setText(text);
        show();
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
            show();
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error changing tooltip to task: "+task, e);
            throw e;
        }
    }

    public void setEquipment(int equipmentClass, int equipmentType, float[] availableResources, Party party, boolean collectionAllowed){
        // AdvancedTooltip is hovering over equipment manager, specifically over one of the equipmment types
        StringBuilder t= new StringBuilder();
        try{
            if (equipmentClass >= JSONManager.getNumEquipmentClasses()){
                LOGGER_MAIN.warning("equipment class out of bounds");
                return;
            }
            if (equipmentType > JSONManager.getNumEquipmentTypesFromClass(equipmentClass)){
                LOGGER_MAIN.warning("equipment class out of bounds:"+equipmentType);
            } else if (equipmentType == JSONManager.getNumEquipmentTypesFromClass(equipmentClass)){
                JSONObject jo = gameData.getJSONObject("tooltips");
                t.append(jo.getString("unequip"));
                if (collectionAllowed){
                    t.append("All equipment will be returned to stockpile");
                }
                else{
                    t.append("Equipment will be destroyed");
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
                    t.append(equipmentTypeJO.getString("display name")).append("\n\n");
                }

                if (!equipmentTypeJO.isNull("description")) {
                    t.append(equipmentTypeJO.getString("description")).append("\n\n");
                }

                // Using 'display multipliers' array so ordering is consistant
                if (!equipmentClassJO.isNull("display multipliers")){
                    t.append("Proficiency Bonus Multipliers:\n");
                    for (int i=0; i < equipmentClassJO.getJSONArray("display multipliers").size(); i++){
                        String multiplierName = equipmentClassJO.getJSONArray("display multipliers").getString(i);
                        if (!equipmentTypeJO.isNull(multiplierName)){
                            if (equipmentTypeJO.getFloat(multiplierName) > 0){
                                t.append(String.format("%s: <g>+%s</g>\n", multiplierName, roundDp("+" + equipmentTypeJO.getFloat(multiplierName), 2)));
                            } else {
                                t.append(String.format("%s: <r>%s</r>\n", multiplierName, roundDp("" + equipmentTypeJO.getFloat(multiplierName), 2)));
                            }
                        }
                    }
                    t.append("\n");
                }

                // Other attributes e.g. range
                if (!equipmentClassJO.isNull("other attributes")){
                    t.append("Other Attributes:\n");
                    for (int i=0; i < equipmentClassJO.getJSONArray("other attributes").size(); i++){
                        String attribute = equipmentClassJO.getJSONArray("other attributes").getString(i);
                        if (!equipmentTypeJO.isNull(attribute)){
                            t.append(String.format("%s: %s\n", attribute, roundDp("+" + equipmentTypeJO.getFloat(attribute), 0)));
                        }
                    }
                    t.append("\n");
                }

                // Display other classes that are blocked (if applicable)
                if (!equipmentTypeJO.isNull("other class blocking")){
                    t.append("Equipment blocks other classes: ");
                    for (int i=0; i < equipmentTypeJO.getJSONArray("other class blocking").size(); i++){
                        if (i < equipmentTypeJO.getJSONArray("other class blocking").size()-1){
                            t.append(String.format("%s, ", equipmentTypeJO.getJSONArray("other class blocking").getString(i)));
                        }
                        else{
                            t.append(String.format("%s", equipmentTypeJO.getJSONArray("other class blocking").getString(equipmentTypeJO.getJSONArray("other class blocking").size() - 1)));
                        }
                    }
                    t.append("\n\n");
                }

                // Display amount of equipment available vs needed for party
                int resourceIndex;
                try{
                    resourceIndex = JSONManager.getResIndex(equipmentTypeJO.getString("id"));
                }
                catch (Exception e){
                    LOGGER_MAIN.log(Level.WARNING, String.format("Error finding resource for equipment class:%d, type:%d", equipmentClass, equipmentType), e);
                    throw e;
                }
                if (floor(availableResources[resourceIndex]) >= party.getUnitNumber()){
                    t.append(String.format("Equipment Available: %d/%d", floor(availableResources[resourceIndex]), party.getUnitNumber()));
                } else{
                    t.append(String.format("Equipment Available: <r>%d</r>/%d", floor(availableResources[resourceIndex]), party.getUnitNumber()));
                }

                // Show where equipment can be stocked up
                if (!equipmentTypeJO.isNull("valid collection sites")){
                    t.append(String.format("\n\n%s can be stocked up at: ", equipmentTypeJO.getString("display name")));
                    for (int i=0; i < equipmentTypeJO.getJSONArray("valid collection sites").size(); i ++){
                        t.append(equipmentTypeJO.getJSONArray("valid collection sites").getString(i));
                        if (i+1 < equipmentTypeJO.getJSONArray("valid collection sites").size()){
                            t.append(", ");
                        }
                    }
                }
                else{
                    t.append(String.format("\n\n%s can be stocked up anywhere", equipmentTypeJO.getString("display name")));
                }

                if (party.getMovementPoints() != party.getMaxMovementPoints()){
                    t.append("\n<r>Equipment can only be changed\nif party has full movement points</r>");
                } else{
                    t.append("\n(Equipment can only be changed\nif party has full movement points)");
                }
            }

            setText(t.toString().replaceAll("\\s+$", ""));
            show();
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to equipment class:%dk, type:%d", equipmentClass, equipmentType), e);
            throw e;
        }
    }

    public void setProficiencies(int proficiencyIndex, Party party){
        StringBuilder t= new StringBuilder();
        JSONObject proficiencyJO;
        if (!(0 <= proficiencyIndex && proficiencyIndex < JSONManager.getNumProficiencies())){
            LOGGER_MAIN.warning("Invalid proficiency index given:"+proficiencyIndex);
            return;
        }
        try{
            proficiencyJO = gameData.getJSONArray("proficiencies").getJSONObject(proficiencyIndex);
            if (!proficiencyJO.isNull("display name")){
                t.append(proficiencyJO.getString("display name")).append("\n");
            }
            if (!proficiencyJO.isNull("tooltip")){
                t.append(proficiencyJO.getString("tooltip")).append("\n");
            }
            float bonusMultiplier = party.getProficiencyBonusMultiplier(proficiencyIndex);
            if (bonusMultiplier != 0){
                ArrayList<String> bonusBreakdown = party.getProficiencyBonusMultiplierBreakdown(proficiencyIndex);
                t.append("\nBonus breakdown:\n");
                for (String s : bonusBreakdown) {
                    t.append(s);
                }
            }

            setText(t.toString().replaceAll("\\s+$", ""));
            show();
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to proficiencies index:%d", proficiencyIndex), e);
            throw e;
        }
    }

    public void setHoveringParty(Party p){
        StringBuilder t = new StringBuilder(String.format("Party '%s'\n", p.id));
        for (int i=0; i < p.proficiencies.length; i++){
            t.append(String.format("\n%s=%s", JSONManager.indexToProficiencyDisplayName(i), roundDpTrailing("" + p.getTotalProficiency(i), 2)));
        }
        setText(t.toString());
        show();
    }

    public void setResource(HashMap<String, Float> taskProductions, HashMap<String, Float> taskConsumptions, String resource) {
        try {
            StringBuilder t = new StringBuilder();
            t.append(String.format("%s:\n", resource));
            for (String task : taskProductions.keySet()) {
                if (taskProductions.get(task)>0 | taskConsumptions.get(task)>0) {
                    StringBuilder taskString = new StringBuilder();
                    taskString.append(String.format("\n%s: <g>+%s</g>", task, metricPrefix(taskProductions.get(task))));
                    taskString.append(String.format("\n<h>%s:</h> <r>-%s</r>", task, metricPrefix(taskConsumptions.get(task))));
                    float total = taskProductions.get(task) - taskConsumptions.get(task);
                    taskString.append(String.format("\n<h>%s:</h>=%s", task, metricPrefix(total)));
                    t.append(taskString);
                }
            }
            setText(t.toString());
            showFlipped();
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Error changing tooltip to resource: "+resource, e);
            throw e;
        }
    }
}





