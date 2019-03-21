package util;

import party.Battle;
import party.Party;
import processing.core.PApplet;

import java.math.BigDecimal;
import java.util.logging.Level;

import static util.Logging.LOGGER_MAIN;

public class BattleEstimateManager {
    int currentWins = 0;
    int currentTrials = 0;
    int attackerX;
    int attackerY;
    int defenderX;
    int defenderY;
    int attackerUnits;
    boolean cached = false;
    Party[][] parties;
    public BattleEstimateManager(Party[][] parties) {
        this.parties = parties;
    }
    public BigDecimal getEstimate(int x1, int y1, int x2, int y2, int units) {
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
    public void refresh() {
        cached = false;
    }
    public int runTrial(Party attacker, Party defender) {
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

    public float getBattleEstimate(Party attacker, Party defender) {
        int TRIALS = 100000;

        int currentWins = 0;
        for (int i = 0; i<TRIALS; i++) {
            currentWins+=runTrial(attacker,defender);
        }

        return PApplet.parseFloat(currentWins)/PApplet.parseFloat(TRIALS);
    }
}
