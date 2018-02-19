
class Node{
  float cost;
  float estimCost;
  boolean fixed;
  
  Node(float cost, boolean fixed){
    this.fixed = fixed;
    this.cost = cost;
  }
  Node(float cost, boolean fixed, float estimCost){
    this.fixed = fixed;
    this.cost = cost;
  }
}