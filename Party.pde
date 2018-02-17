

class Party{
  int unitNumber, player, movementPoints;
  String task;
  Party(int player, int startingUnits, String startingTask, int movementPoints){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
  }
  void changeTask(String task){
    this.task = task;
  }
}