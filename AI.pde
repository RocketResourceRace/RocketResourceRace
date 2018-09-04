


class BanditController implements PlayerController{
  BanditController(){
    
  }
  GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]){
    
    return new EndTurn();  // Placeholder
  }
}
