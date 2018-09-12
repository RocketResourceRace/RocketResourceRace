
class NodeComparator implements Comparator {
  int compare (Object o1, Object o2){
    Node a = (Node)o1;
    Node b = (Node)o2;
    if (a.cost < b.cost){
      return -1;
    } else if (a.cost > b.cost){
      return 1;
    } else {
      return 0;
    }
  }
  boolean equals(Node a, Node b){
    return a.cost == b.cost;
  }
}

int movementCost(int x, int y, int prevX, int prevY, Cell[][] visibleCells, int maxCost) {
  float mult = 1;
  if (x!=prevX && y!=prevY) {
    mult = 1.42;
  }
  if (0<=x && x<jsManager.loadIntSetting("map size") && 0<=y && y<jsManager.loadIntSetting("map size")) {
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

int sightCost(int x, int y, int prevX, int prevY, int[][] terrain) {
  float mult = 1;
  if (x!=prevX && y!=prevY) {
    mult = 1.42;
  }
  if (0<=x && x<jsManager.loadIntSetting("map size") && 0<=y && y<jsManager.loadIntSetting("map size")) {
    return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("sight cost")*mult);
  }
  
  //Not a valid location
  return -1;
}


Node[][] LimitedKnowledgeDijkstra(int x, int y, int w, int h, Cell[][] visibleCells, int turnsRadius) {
  int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
  int maxCost = jsManager.getMaxTerrainMovementCost();
  Node currentHeadNode;
  Node[][] nodes = new Node[h][w];
  nodes[y][x] = new Node(0, false, x, y, x, y);
  PriorityQueue<Node> curMinNodes = new PriorityQueue<Node>(new NodeComparator());
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

float getBattleEstimate(Party attacker, Party defender) {
  int TRIALS = 100000;
  
  int currentWins = 0;
  for (int i = 0; i<TRIALS; i++) {
    currentWins+=runTrial(attacker,defender);
  }
  
  return float(currentWins)/float(TRIALS);
}

boolean willMove(Party p, int px, int py, Node[][] moveNodes) {
  if (p.path==null||(p.path!=null&&p.path.size()==0)) {
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


class BanditController implements PlayerController {
  int[][] cellsTargetedWeightings;
  Node[][] moveNodes;
  int player;
  BanditController(int player, int mapWidth, int mapHeight) {
    this.player = player;
     cellsTargetedWeightings = new int[mapHeight][];
     for (int y = 0; y < mapHeight; y++) {
       cellsTargetedWeightings[y] = new int[mapWidth];
       for (int x = 0; x < mapHeight; x++) {
         cellsTargetedWeightings[y][x] = 0;
       }
     }
  }
  
  GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]) {
    
    // Remove targeted cells that are no longer valid
    for (int y = 0; y < visibleCells.length; y++) {
      for (int x = 0; x < visibleCells[0].length; x++) {
        if (cellsTargetedWeightings[y][x] != 0 && visibleCells[y][x] != null && visibleCells[y][x].party == null && visibleCells[y][x].activeSight) {
          cellsTargetedWeightings[y][x] = 0;
        }
      }
    }
    
    // Get an event from a party
    for (int y = 0; y < visibleCells.length; y++) {
      for (int x = 0; x < visibleCells[0].length; x++) {
        if (visibleCells[y][x] != null && visibleCells[y][x].party != null) {
          if (visibleCells[y][x].party.player == player) {
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
  
  GameEvent getEventForParty(Cell[][] visibleCells, float resources[], int px, int py) {
    moveNodes = LimitedKnowledgeDijkstra(px, py, visibleCells[0].length, visibleCells.length, visibleCells, 5);
    Party p = visibleCells[py][px].party;
    cellsTargetedWeightings[py][px] = 0;
    int maximumWeighting = 0;
    if (p.getMovementPoints() > 0 && willMove(p, px, py, moveNodes)) {
      ArrayList<int[]> cellsToAttack = new ArrayList<int[]>();
      for (int y = 0; y < visibleCells.length; y++) {
        for (int x = 0; x < visibleCells[0].length; x++) {
          if (visibleCells[y][x] != null && moveNodes[y][x] != null) {
            int weighting = 0;
            if (visibleCells[y][x].party != null && visibleCells[y][x].party.player != p.player) {
              weighting += 5;
              weighting -= floor(moveNodes[y][x].cost/p.getMaxMovementPoints());
              if (visibleCells[y][x].building != null) {
                weighting += 5;
                // Add negative weighting if building is a defence building once defence buildings are added
              }
            } else if (visibleCells[y][x].building != null) {
              weighting += 5;
              weighting -= int(dist(px, py, x, y));
            }
            weighting += cellsTargetedWeightings[y][x];
            if (weighting > 0) {
              maximumWeighting = max(maximumWeighting, weighting);
              cellsToAttack.add(new int[]{x, y, weighting});
            }
          }
        }
      }
      if (cellsToAttack.size() > 0) {
        for (int[] cell: cellsToAttack){
          if (cell[2] == maximumWeighting) {
            if (moveNodes[cell[1]][cell[0]].cost < p.getMaxMovementPoints()) {
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
                    return new Move(px, py, x, y, p.getUnitNumber());
                  }
                }
              }
            }
          }
        }
      } else {
        // Bandit searching for becuase no parties to attack in area
        ArrayList<int[]> cellsToMoveTo = new ArrayList<int[]>();
        for (int y = 0; y < visibleCells.length; y++) {
          for (int x = 0; x < visibleCells[0].length; x++) {
            if (visibleCells[y][x] != null && moveNodes[y][x] != null && moveNodes[y][x].cost <= p.getMovementPoints()) {  // Check in sight and within 1 turn movement
              int weighting = 0;
              if (visibleCells[y][x].activeSight){
                weighting += 5;
              }
              maximumWeighting = max(maximumWeighting, weighting);
              cellsToMoveTo.add(new int[]{x, y, weighting});
            }
          }
        }
        for(int[] cell : cellsToMoveTo){
          if (cell[2] == maximumWeighting){
            if (visibleCells[cell[1]][cell[0]] != null){
              return new Move(px, py, cell[0], cell[1], p.getUnitNumber());
            }
          }
        }
      }
    }
    return null;
  }
}
