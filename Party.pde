

class Party{
  int unitNumber, player, movementPoints;
  char task;
  Party(int player, int startingUnits, char startingTask, int movementPoints){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
    this.movementPoints = movementPoints;
  }
}