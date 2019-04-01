package util;

import json.JSONManager;

import java.util.PriorityQueue;

import static json.JSONManager.gameData;
import static processing.core.PApplet.min;
import static processing.core.PApplet.round;
import static util.Logging.LOGGER_MAIN;

public class Dijkstra {

    public static Node[][] LimitedKnowledgeDijkstra(int x, int y, int w, int h, Cell[][] visibleCells, int turnsRadius) {
        LOGGER_MAIN.finer(String.format("Starting dijkstra on cell: (%d, %d)", x, y));
        int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
        int maxCost = JSONManager.getMaxTerrainMovementCost();
        Node currentHeadNode;
        Node[][] nodes = new Node[h][w];
        nodes[y][x] = new Node(0, false, x, y, x, y);
        PriorityQueue<Node> curMinNodes = new PriorityQueue<>(new NodeComparator());
        curMinNodes.add(nodes[y][x]);
        while (curMinNodes.size() > 0) {
            currentHeadNode = curMinNodes.poll();
            currentHeadNode.fixed = true;

            for (int[] mv : mvs) {
                int nx = currentHeadNode.x+mv[0];
                int ny = currentHeadNode.y+mv[1];
                if (0 <= nx && nx < w && 0 <= ny && ny < h) {
                    boolean sticky = visibleCells[ny][nx] != null && visibleCells[ny][nx].getParty() != null;
                    int newCost = movementCost(nx, ny, currentHeadNode.x, currentHeadNode.y, visibleCells, maxCost);
                    int prevCost = currentHeadNode.cost;
                    if (newCost != -1){ // Check that the cost is valid
                        int totalNewCost = prevCost+newCost;
                        if (totalNewCost < visibleCells[y][x].getParty().getMaxMovementPoints()*turnsRadius) {
                            if (nodes[ny][nx] == null) {
                                nodes[ny][nx] = new Node(totalNewCost, false, currentHeadNode.x, currentHeadNode.y, nx, ny);
                                if (!sticky) {
                                    curMinNodes.add(nodes[ny][nx]);
                                }
                            } else if (!nodes[ny][nx].fixed) {
                                if (totalNewCost < nodes[ny][nx].cost) { // Updating existing node
                                    nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
                                    nodes[ny][nx].setPrev(currentHeadNode.x, currentHeadNode.y);
                                    if (!sticky) {
                                        curMinNodes.remove(nodes[ny][nx]);
                                        curMinNodes.add(nodes[ny][nx]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return nodes;
    }


    private static int movementCost(int x, int y, int prevX, int prevY, Cell[][] visibleCells, int maxCost) {
        float mult = 1;
        if (x!=prevX && y!=prevY) {
            mult = 1.42f;
        }
        if (0<=x && x< JSONManager.loadIntSetting("map size") && 0<=y && y<JSONManager.loadIntSetting("map size")) {
            if (visibleCells[y][x] != null){
                return round(gameData.getJSONArray("terrain").getJSONObject(visibleCells[y][x].terrain).getInt("movement cost")*mult);
            }
            else {
                return round(maxCost*mult);  // Assumes max cost if terrain is unexplored
            }
        }

        //Not a valid location
        return -1;
    }
}
