
class Node{
  int cost;
  float estimCost;
  boolean fixed;
  
  Node(int cost, boolean fixed){
    this.fixed = fixed;
    this.cost = cost;
  }
  Node(int cost, boolean fixed, float estimCost){
    this.fixed = fixed;
    this.cost = cost;
  }
}