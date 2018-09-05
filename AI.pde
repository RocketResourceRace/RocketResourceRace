

static class AIUtil {
  static Node[][] djk(int x, int y, int w, int h, Cell[][] visibleCells) {
    int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
    Node[][] nodes = new Node[h][w];
    nodes[y][x] = (new RocketResourceRace()) new.Node(0, false, x, y);
    
    return nodes;
  }
  //static Node[][] djk(int x, int y, int w, int h) {
  //  int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
  //  Node[][] nodes = new Node[h][w];
  //  nodes[y][x] = new Node(0, false, x, y);
  //  ArrayList<Integer> curMinCosts = new ArrayList<Integer>();
  //  ArrayList<int[]> curMinNodes = new ArrayList<int[]>();
  //  curMinNodes.add(new int[]{x, y});
  //  curMinCosts.add(0);
  //  while (curMinNodes.size() > 0) {
  //    nodes[curMinNodes.get(0)[1]][curMinNodes.get(0)[0]].fixed = true;
  //    for (int[] mv : mvs) {
  //      int nx = curMinNodes.get(0)[0]+mv[0];
  //      int ny = curMinNodes.get(0)[1]+mv[1];
  //      if (0 <= nx && nx < w && 0 <= ny && ny < h) {
  //        boolean sticky = parties[ny][nx] != null;
  //        int newCost = cost(nx, ny, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
  //        int prevCost = curMinCosts.get(0);
  //        int totalNewCost = prevCost+newCost;
  //        if (totalNewCost < parties[y][x].getMaxMovementPoints()*100) {
  //          if (nodes[ny][nx] == null) {
  //            nodes[ny][nx] = new Node(totalNewCost, false, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
  //            if (!sticky) {
  //              curMinNodes.add(search(curMinCosts, totalNewCost), new int[]{nx, ny});
  //              curMinCosts.add(search(curMinCosts, totalNewCost), totalNewCost);
  //            }
  //          } else if (!nodes[ny][nx].fixed) {
  //            if (totalNewCost < nodes[ny][nx].cost) {
  //              nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
  //              nodes[ny][nx].setPrev(curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
  //              if (!sticky) {
  //                curMinNodes.remove(search(curMinNodes, nx, ny));
  //                curMinNodes.add(search(curMinCosts, totalNewCost), new int[]{nx, ny});
  //                curMinCosts.remove(search(curMinNodes, nx, ny));
  //                curMinCosts.add(search(curMinCosts, totalNewCost), totalNewCost);
  //              }
  //            }
  //          }
  //        }
  //      }
  //    }
  //    curMinNodes.remove(0);
  //    curMinCosts.remove(0);
  //  }
  //  return nodes;
  //}
}


class BanditController implements PlayerController{
  BanditController(){
    
  }
  GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]){
    
    return new EndTurn();  // Placeholder
  }
}
