package party;

import map.Building;
import processing.core.PApplet;

import java.util.logging.Level;

import static json.JSONManager.JSONIndex;
import static json.JSONManager.gameData;
import static processing.core.PApplet.floor;
import static processing.core.PApplet.pow;
import static util.Logging.LOGGER_MAIN;
import static util.Util.random;

public class Siege extends Battle {
    Building defence;
    public Siege(Party attacker, Building defence, Party garrison, String id) {
        super(attacker, garrison, id);
        this.defence = defence;
        this.player = -2;
    }

    public Party doBattle() {
        try {
            int changeInParty1 = getSiegeUnitChange(attacker, defender, defence);
            int changeInParty2 = getSiegeUnitChange(defender, attacker, defence);
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
            LOGGER_MAIN.log(Level.SEVERE, "Error doing Siege", e);
            throw e;
        }
    }
    public int getSiegeUnitChange(Party p1, Party p2, Building defence) {
        float damageRating = p2.strength * p2.getEffectivenessMultiplier("melee attack") * (1 + PApplet.parseInt(defence.getPlayerID() == p2.player) * gameData.getJSONArray("buildings").getJSONObject(defence.type).getFloat("defence"))/
                (p1.strength * p1.getEffectivenessMultiplier("defence") * (1 + PApplet.parseInt(defence.getPlayerID() == p1.player) * defence.getHealth()));
        return floor(-0.2f * (p2.getUnitNumber() + pow(p2.getUnitNumber(), 2) / p1.getUnitNumber()) * random(0.75f, 1.5f) * damageRating);
    }
}