package party;

import map.Building;
import processing.core.PApplet;

import java.util.logging.Level;

import static json.JSONManager.JSONIndex;
import static json.JSONManager.gameData;
import static processing.core.PApplet.*;
import static util.Logging.LOGGER_MAIN;
import static util.Util.between;
import static util.Util.random;

public class Siege extends Battle {
    private Building defence;
    public Siege(Party attacker, Building defence, Party garrison, String id) {
        super(attacker, garrison, id);
        this.defence = defence;
        this.player = -2;
    }

    public Party doBattle() {
        try {
            int changeInAttacker = getSiegeUnitChange(attacker, defender, defence);
            int changeInDefender = getSiegeUnitChange(defender, attacker, defence);
            attacker.strength = 1;
            defender.strength = 1;
            int newParty1Size = attacker.getUnitNumber()+changeInAttacker;
            int newParty2Size = defender.getUnitNumber()+changeInDefender;
            int endDifference = newParty1Size-newParty2Size;
            defence.setHealth(between(0, defence.getHealth()-endDifference / (float) defender.getUnitNumber(), defence.getHealth()));
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
                if (defence.getHealth()==0) {
                    return new Battle(attacker, defender, id);
                }
                return this;
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error doing Siege", e);
            throw e;
        }
    }

    public Building getDefence() {
        return defence;
    }

    private int getSiegeUnitChange(Party p1, Party p2, Building defence) {
        float d = defence.getHealth() * exp(gameData.getJSONArray("buildings").getJSONObject(defence.getType()).getFloat("defence"));
        float defenceMultiplier = defence.getPlayerId() == p1.getPlayer() ? d : 1/d;
        float damageRating = p2.strength * p2.getEffectivenessMultiplier("melee attack") * (1 + PApplet.parseInt(defence.getPlayerId() == p2.player) /
                (p1.strength * p1.getEffectivenessMultiplier("defence") * (1 + PApplet.parseInt(defence.getPlayerId() == p1.player) * defenceMultiplier)));
        return floor(-0.2f * (p2.getUnitNumber() + pow(p2.getUnitNumber(), 2) / p1.getUnitNumber()) * random(0.75f, 1.5f) * damageRating);
    }
}