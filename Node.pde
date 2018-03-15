
class Node{
  int cost;
  boolean fixed;
  int prevX = -1, prevY = -1;
  
  Node(int cost, boolean fixed, int prevX, int prevY){
    this.fixed = fixed;
    this.cost = cost;
    this.prevX = prevX;
    this.prevY = prevY;
  }
  void setPrev(int prevX ,int prevY){
    this.prevX = prevX;
    this.prevY = prevY;
  }
}
