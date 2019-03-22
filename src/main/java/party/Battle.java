package party;


import json.JSONManager;
import player.Player;

import java.util.logging.Level;

import static json.JSONManager.JSONIndex;
import static json.JSONManager.gameData;
import static processing.core.PApplet.*;
import static util.Logging.LOGGER_GAME;
import static util.Logging.LOGGER_MAIN;
import static util.Util.random;

public class Battle extends Party {
    public Party attacker;
    public Party defender;

    public Battle(Party attacker, Party defender, String id) {
        super(-1, attacker.getUnitNumber()+defender.getUnitNumber(), JSONIndex(gameData.getJSONArray("tasks"), "Battle"), 0, id);
        this.attacker = attacker;
        attacker.strength = 2;
        this.defender = defender;
    }

    public int containsPartyFromPlayer(int p) {
        if (attacker.player == p) {
            return 1;
        } else if (defender.player == p) {
            return 2;
        }
        return 0;
    }

    public boolean isTurn(int turn) {
        return true;
    }

    public int getMovementPoints(int turn) {
        if (turn==attacker.player) {
            return attacker.getMovementPoints();
        } else {
            return defender.getMovementPoints();
        }
    }

    public void setUnitNumber(int turn, int newUnitNumber) {
        if (turn==attacker.player) {
            attacker.setUnitNumber(newUnitNumber);
        } else {
            defender.setUnitNumber(newUnitNumber);
        }
    }

    public int getUnitNumber() {
        return attacker.getUnitNumber()+defender.getUnitNumber();
    }

    public int getUnitNumber(int turn) {
        if (turn==attacker.player) {
            return attacker.getUnitNumber();
        } else {
            return defender.getUnitNumber();
        }
    }

    public int changeUnitNumber(int turn, int changeInUnitNumber) {
        if (turn==this.attacker.player) {
            int overflow = max(0, changeInUnitNumber+attacker.getUnitNumber()- JSONManager.loadIntSetting("party size"));
            this.attacker.setUnitNumber(attacker.getUnitNumber()+changeInUnitNumber);
            return overflow;
        } else {
            int overflow = max(0, changeInUnitNumber+defender.getUnitNumber()-JSONManager.loadIntSetting("party size"));
            this.defender.setUnitNumber(defender.getUnitNumber()+changeInUnitNumber);
            return overflow;
        }
    }

    public Party doBattle() {
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

    public Battle clone() {
        Battle newParty = new Battle(this.attacker.clone(), this.defender.clone(), id);
        return newParty;
    }

    public int mergeEntireFrom(Party other, int moveCost, Player player) {
        // Note: will need to remove other party
        LOGGER_GAME.fine(String.format("Merging entire party from id:%s into battle with id:%s", other.id, this.id));
        return mergeFrom(other, other.getUnitNumber(), moveCost, player);
    }

    public int mergeFrom(Party other, int unitsTransfered, int moveCost, Player player) {
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
    public int getBattleUnitChange(Party p1, Party p2) {
        float damageRating = p2.strength * p2.getEffectivenessMultiplier("melee attack") /
                (p1.strength * p1.getEffectivenessMultiplier("defence"));
        return floor(-0.2f * (p2.getUnitNumber() + pow(p2.getUnitNumber(), 2) / p1.getUnitNumber()) * random(0.75f, 1.5f) * damageRating);
    }
}
