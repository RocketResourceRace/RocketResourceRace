

class Party{
  int unitNumber, player;
  char task;
  Party(int player, int startingUnits, char startingTask){
    unitNumber = startingUnits;
    task = startingTask;
    this.player = player;
  }
}