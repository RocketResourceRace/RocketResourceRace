package controller;

import event.EndTurn;
import event.GameEvent;
import event.Move;
import util.Cell;
import util.Dijkstra;
import util.Node;
import party.Party;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Collections;

import static processing.core.PApplet.*;
import static util.Logging.LOGGER_GAME;


public class BanditController implements PlayerController {
    public int[][] cellsTargetedWeightings;
    private int player;
    public BanditController(int player, int mapWidth, int mapHeight) {
        this.player = player;
        cellsTargetedWeightings = new int[mapHeight][];
        for (int y = 0; y < mapHeight; y++) {
            cellsTargetedWeightings[y] = new int[mapWidth];
            for (int x = 0; x < mapHeight; x++) {
                cellsTargetedWeightings[y][x] = 0;
            }
        }
    }

    public GameEvent generateNextEvent(Cell[][] visibleCells, float[] resources) {
        LOGGER_GAME.finer("Generating next event for bandits");
        // Remove targeted cells that are no longer valid
        for (int y = 0; y < visibleCells.length; y++) {
            for (int x = 0; x < visibleCells[0].length; x++) {
                if (cellsTargetedWeightings[y][x] != 0 && visibleCells[y][x] != null && visibleCells[y][x].getParty() == null && visibleCells[y][x].getActiveSight()) {
                    cellsTargetedWeightings[y][x] = 0;
                }
            }
        }

        // Get an event from a party
        for (int y = 0; y < visibleCells.length; y++) {
            for (int x = 0; x < visibleCells[0].length; x++) {
                if (visibleCells[y][x] != null && visibleCells[y][x].getParty() != null) {
                    if (visibleCells[y][x].getParty().getPlayer() == player) {
                        LOGGER_GAME.finer(String.format("Getting event for party on cell: (%d, %d) with id:%s", x, y, visibleCells[y][x].getParty().getID()));
                        GameEvent event = getEventForParty(visibleCells, resources, x, y);
                        if (event != null) {
                            return event;
                        }
                    }
                }
            }
        }
        return new EndTurn();  // If no parties have events to do, end the turn
    }

    private GameEvent getEventForParty(Cell[][] visibleCells, float[] resources, int px, int py) {
        Node[][] moveNodes = Dijkstra.LimitedKnowledgeDijkstra(px, py, visibleCells[0].length, visibleCells.length, visibleCells, 5);
        Party p = visibleCells[py][px].getParty();
        cellsTargetedWeightings[py][px] = 0;
        int maximumWeighting = 0;
        if (p.getMovementPoints() > 0 && willMove(p, px, py, moveNodes)) {
            ArrayList<int[]> cellsToAttack = new ArrayList<>();
            for (int y = 0; y < visibleCells.length; y++) {
                for (int x = 0; x < visibleCells[0].length; x++) {
                    if (visibleCells[y][x] != null && moveNodes[y][x] != null) {
                        int weighting = 0;
                        if (visibleCells[y][x].getParty() != null && visibleCells[y][x].getParty().getPlayer() != p.getPlayer()) {
                            weighting += 5;
                            weighting -= floor(moveNodes[y][x].cost/p.getMaxMovementPoints());
                            if (visibleCells[y][x].getBuilding() != null) {
                                weighting += 5;
                                // Add negative weighting if building is a defence building once defence buildings are added
                            }
                        } else if (visibleCells[y][x].getBuilding() != null) {
                            weighting += 5;
                            weighting -= PApplet.parseInt(dist(px, py, x, y));
                        }
                        weighting += cellsTargetedWeightings[y][x];
                        if (weighting > 0) {
                            LOGGER_GAME.fine("At least one cell has a positive weight for attacking, so will attack");
                            maximumWeighting = max(maximumWeighting, weighting);
                            cellsToAttack.add(new int[]{x, y, weighting});
                        }
                    }
                }
            }
            Collections.shuffle(cellsToAttack);
            if (cellsToAttack.size() > 0) {
                for (int[] cell: cellsToAttack){
                    if (cell[2] == maximumWeighting) {
                        if (moveNodes[cell[1]][cell[0]].cost < p.getMaxMovementPoints()) {
                            cellsTargetedWeightings[cell[1]][cell[0]] += maximumWeighting;
                            return new Move(px, py, cell[0], cell[1], p.getUnitNumber());
                        } else {
                            int minimumCost = p.getMaxMovementPoints() * 5;
                            for (int y = max(0, cell[1] - 1); y < min(moveNodes.length, cell[1] + 2); y++) {
                                for (int x = max(0, cell[0] - 1); x < min(moveNodes[0].length, cell[0] + 2); x++) {
                                    if (moveNodes[y][x] != null) {
                                        minimumCost = min(minimumCost, moveNodes[y][x].cost);
                                    }
                                }
                            }
                            for (int y = max(0, cell[1] - 1); y < min(moveNodes.length, cell[1] + 2); y++) {
                                for (int x = max(0, cell[0] - 1); x < min(moveNodes[0].length, cell[0] + 2); x++) {
                                    if (moveNodes[y][x] != null && moveNodes[y][x].cost == minimumCost) {
                                        cellsTargetedWeightings[cell[1]][cell[0]] += maximumWeighting;
                                        return new Move(px, py, x, y, p.getUnitNumber());
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Bandit searching for becuase no parties to attack in area
                ArrayList<int[]> cellsToMoveTo = new ArrayList<>();
                for (int y = 0; y < visibleCells.length; y++) {
                    for (int x = 0; x < visibleCells[0].length; x++) {
                        if (moveNodes[y][x] != null) {  // Check in sight and within 1 turn movement
                            int weighting = 0;
                            if (visibleCells[y][x] == null){
                                weighting += 10;
                            }
                            else if (!visibleCells[y][x].getActiveSight()){
                                weighting += 5;
                            }
                            weighting -= moveNodes[y][x].cost/p.getMaxMovementPoints();
                            maximumWeighting = max(maximumWeighting, weighting);
                            cellsToMoveTo.add(new int[]{x, y, weighting});
                        }
                    }
                }
                Collections.shuffle(cellsToMoveTo);
                for(int[] cell : cellsToMoveTo){
                    if (cell[2] == maximumWeighting){
                        return new Move(px, py, cell[0], cell[1], p.getUnitNumber());
                    }
                }
            }
        }
        return null;
    }

    private boolean willMove(Party p, int px, int py, Node[][] moveNodes) {
        if (p.path==null||(p.path.size()==0)) {
            for (int y = max(0, py - 1); y < min(py + 2, moveNodes.length); y++) {
                for (int x = max(0, px - 1); x < min(px + 2, moveNodes[0].length); x++) {
                    if (moveNodes[y][x] != null && p.getMovementPoints() >= moveNodes[y][x].cost) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}