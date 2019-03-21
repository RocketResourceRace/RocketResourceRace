package player;


import controller.BanditController;
import controller.PlayerController;
import event.GameEvent;
import json.JSONManager;
import map.Building;
import map.Map;
import party.Party;
import states.Game;
import util.Cell;
import util.Node;
import util.NodeComparator;

import java.util.PriorityQueue;

import static json.JSONManager.gameData;
import static processing.core.PApplet.*;
import static util.Logging.LOGGER_MAIN;

public class Player {
    private int id;
    public float cameraCellX;
    public float cameraCellY;
    public float blockSize;
    public float[] resources;
    public int cellX;
    public int cellY;
    public int colour;
    public boolean cellSelected = false;
    public String name;
    public boolean isAlive = true;
    public PlayerController playerController;
    public int controllerType;  // 0 for local, 1 for bandits
    public Cell[][] visibleCells;

    // Resources: food wood metal energy concrete cable spaceship_parts ore people
    public Player(float x, float y, float blockSize, float[] resources, int colour, String name, int controllerType, int id) {
        this.cameraCellX = x;
        this.cameraCellY = y;
        this.blockSize = blockSize;
        this.resources = resources;
        this.colour = colour;
        this.name = name;
        this.id = id;

        this.visibleCells = new Cell[JSONManager.loadIntSetting("map size")][JSONManager.loadIntSetting("map size")];
        this.controllerType = controllerType;
        switch(controllerType){
            case 1:
                playerController = new BanditController(id, JSONManager.loadIntSetting("map size"), JSONManager.loadIntSetting("map size"));
                break;
            default:
                playerController = null;
                break;
        }
    }

    public Node[][] sightDijkstra(int x, int y, Party[][] parties, int[][] terrain) {
        int w = visibleCells[0].length;
        int h = visibleCells.length;
        int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
        Node currentHeadNode;
        Node[][] nodes = new Node[h][w];
        nodes[y][x] = new Node(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("sight bonus"), false, x, y, x, y);
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

    public boolean[][] generateFogMap(Party[][] parties, int[][] terrain) {
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

    public void updateVisibleCells(int[][] terrain, Building[][] buildings, Party[][] parties, boolean[][] seenCells){
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

    public void updateVisibleCells(int[][] terrain, Building[][] buildings, Party[][] parties){
        updateVisibleCells(terrain, buildings, parties, null);
    }

    public void saveSettings(float x, float y, float blockSize, int cellX, int cellY, boolean cellSelected) {
        this.cameraCellX = x;
        this.cameraCellY = y;
        this.blockSize = blockSize;
        this.cellX = cellX;
        this.cellY = cellY;
        this.cellSelected = cellSelected;
    }
    public void loadSettings(Game g, Map m) {
        LOGGER_MAIN.fine("Loading player camera settings");
        m.loadSettings(cameraCellX, cameraCellY, blockSize);
        if (cellSelected) {
            g.selectCell(this.cellX, this.cellY, false);
        } else {
            g.deselectCell();
        }
    }

    public GameEvent generateNextEvent(){
        // This method will be run continuously until it returns an end turn event
        return playerController.generateNextEvent(visibleCells, resources);
    }

    public int sightCost(int x, int y, int prevX, int prevY, int[][] terrain) {
        float mult = 1;
        if (x!=prevX && y!=prevY) {
            mult = 1.42f;
        }
        if (0<=x && x<JSONManager.loadIntSetting("map size") && 0<=y && y<JSONManager.loadIntSetting("map size")) {
            return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("sight cost")*mult);
        }

        //Not a valid location
        return -1;
    }
}
