
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

int cost(int x, int y, int prevX, int prevY, Cell[][] visibleCells, int maxCost) {
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


Node[][] LimitedKnowledgeDijkstra(int x, int y, int w, int h, Cell[][] visibleCells, int turnsRadius) {
  int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
  int maxCost = jsManager.getMaxTerrainCost();
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
        int newCost = cost(nx, ny, currentHeadNode.x, currentHeadNode.y, visibleCells, maxCost);
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


class BanditController implements PlayerController {
  int[][] cellsTargetedWeightings;
  Node[][] moveNodes;
  int player;
  BanditController(int player, int mapWidth, int mapHeight){
    this.player = player;
     cellsTargetedWeightings = new int[mapHeight][];
     for (int y = 0; y < mapHeight; y++) {
       cellsTargetedWeightings[y] = new int[mapWidth];
       for (int x = 0; x < mapHeight; x++) {
         cellsTargetedWeightings[y][x] = 0;
       }
     }
  }
  GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]){
    //println(getBattleEstimate(new Party(0, 100, 0, 64, "test 1"), new Party(1, 100, 0, 64, "test 2"))); // Battle estimate test
    for (int y = 0; y < visibleCells.length; y++) {
      for (int x = 0; x < visibleCells[0].length; x++) {
        if (visibleCells[y][x] != null && visibleCells[y][x].party != null) {
          if (visibleCells[y][x].party.player == player) {
            getEventForParty(visibleCells, resources, x, y);
          }
        }
      }
    }
    return new EndTurn();  // Placeholder
  }
  
  GameEvent getEventForParty(Cell[][] visibleCells, float resources[], int px, int py) {
    moveNodes = LimitedKnowledgeDijkstra(px, py, visibleCells[0].length, visibleCells.length, visibleCells, 5);
    Party p = visibleCells[py][px].party;
    if (p.getMovementPoints() > 0) {
      ArrayList<int[]> cellsToAttack = new ArrayList<int[]>();
      for (int y = 0; y < visibleCells.length; y++) {
        for (int x = 0; x < visibleCells[0].length; x++) {
          if (visibleCells[y][x] != null) {
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
              cellsToAttack.add(new int[]{x, y, weighting});
            }
          }
        }
      }
      if (cellsToAttack.size() > 0) {
        println("bandit is attacking");
        // Run away from other bandits in order to spread out
      } else {
        println("bandit is searching");
        // Find a party to attack
      }
    }
    return null;
  }
}
