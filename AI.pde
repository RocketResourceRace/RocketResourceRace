


class BanditController implements PlayerController{
  BanditController(){
    
  }
  GameEvent generateNextEvent(){
    return new EndTurn();
  }
}
