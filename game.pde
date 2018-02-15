
class Game extends State{
  final int numOfGroundTypes = 3;
  final int numOfGroundSpawns = 100;
  final int mapElementWidth = 1100;
  final int mapElementHeight = 700;
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezle = 20;
  int mapHeight = mapSize;
  int mapWidth = mapSize;
  int waterLevel = 3;
  int initialSmooth = 7;
  int completeSmooth = 5;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  int turn;
  boolean changeTurn = false;
  Map map;
  Player[] players;
  int cellX, cellY, cellSelectionX, cellSelectionY, cellSelectionW, cellSelectionH;
  boolean cellSelected=false;
  
  Game(){
    addElement("map", new Map(bezle, bezle, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    addElement("end turn", new Button(bezle, height-buttonH-bezle, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"));
    map = (Map)getElement("map", "default");
    players = new Player[2];
    
    // Initial positions will be focused on starting party
    players[0] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize);
    players[1] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize);
    
    addPanel("cell selection", 0, 0, width, height, false, color(255, 255, 255, 255), color(0));
    //addElement("cell selection", new );
  }
  void updateCellSelection(){
    cellSelectionX = round(mapElementWidth*GUIScale)+bezle*2;
    cellSelectionY = bezle;
    cellSelectionW = width-cellSelectionX-bezle;
    cellSelectionH = round(mapElementHeight*GUIScale);
    
  }
  String update(){
    if (changeTurn){  
      players[turn].saveMapSettings(map.mapXOffset, map.mapYOffset, map.blockSize);
      turn = (turn + 1)%2;
      players[turn].loadMapSettings(map);
      changeTurn = false;
    }
    drawBar();
    if (cellSelected)
      drawCellSelection();
    drawPanels();
    return getNewState();
  }
  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type == "clicked"){
        if (event.id == "end turn"){
          changeTurn = true;
        }
      }
    }
  }
  ArrayList<String> mouseEvent(String eventType, int button){
    if (button == RIGHT){
      if (eventType == "mouseClicked"){
        if (map.mouseOver()){
          if (cellSelected){
            cellSelected = false;
            map.unselectCell();
          }
          else{
            cellX = floor(map.scaleXInv(mouseX));
            cellY = floor(map.scaleYInv(mouseY));
            cellSelected = true;
            map.selectCell(cellX, cellY);
            getPanel("cell selection").setVisible(true);
          }
        }
        else{
          cellSelected = false;
          map.unselectCell();
          getPanel("cell selection").setVisible(false);
        }
      }
    }
    return new ArrayList<String>();
  }
  
  void drawCellSelection(){
    pushStyle();
    fill(50, 50, 200);
    rect(cellSelectionX, cellSelectionY, cellSelectionW, cellSelectionH);
    fill(0);
    textSize(12);
    textAlign(CENTER, TOP);
    text("Cell Selection", cellSelectionX+cellSelectionW/2, cellSelectionY);
    popStyle();
  }
  
  void drawBar(){
    float barX=buttonW+bezle*2;
    fill(200);
    stroke(170);
    rect(0, height-bezle*2-buttonH, width, buttonH+bezle*2);
    textSize(10*TextScale);
    String turnString="";
    if (this.turn==0){
      fill(255, 0, 0);
      turnString = "Red Player's Turn";
    }
    else if (this.turn==1){
      fill(0, 0, 255);
      turnString = "Blue Player's Turn";
    }
    stroke(50);
    rect(barX, height-bezle-buttonH, textWidth(turnString)+10, buttonH);
    barX += textWidth(turnString)+10+bezle;
    fill(255);
    textAlign(CENTER, TOP);
    text(turnString, bezle*2+buttonW+(textWidth(turnString)+10)/2, height-bezle-(textDescent()+textAscent())/2-buttonH/2);
    
    barX=width;
    String tempString = "food:"+players[turn].food;
    barX -= textWidth(tempString)+10+bezle;
    fill(150);
    rect(barX, height-bezle-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezle-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "wood:"+players[turn].wood;
    barX -= textWidth(tempString)+10+bezle;
    fill(150);
    rect(barX, height-bezle-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezle-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "metal:"+players[turn].metal;
    barX -= textWidth(tempString)+10+bezle;
    fill(150);
    rect(barX, height-bezle-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezle-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "energy:"+players[turn].energy;
    barX -= textWidth(tempString)+10+bezle;
    fill(150);
    rect(barX, height-bezle-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezle-(textDescent()+textAscent())/2-buttonH/2);
  }
  
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (_key == ESC){
      newState = "menu";
    }
    return new ArrayList<String>();
  }
    
  void enterState(){
    mapWidth = mapSize;
    mapHeight = mapSize;
    updateCellSelection();
    
    parties = new Party[mapHeight][mapWidth];
    buildings = new Building[mapHeight][mapWidth];
    
    PVector[] playerStarts = generateStartingParties();
    
    terrain = generateMap(playerStarts);
    map.reset(mapWidth, mapHeight, terrain, parties, buildings);
    
    float[] conditions2 = map.targetCell((int)playerStarts[1].x, (int)playerStarts[1].y, 64);
    players[1] = new Player(conditions2[0], conditions2[1], conditions2[2]);
    float[] conditions1 = map.targetCell((int)playerStarts[0].x, (int)playerStarts[0].y, 64);
    players[0] = new Player(conditions1[0], conditions1[1], conditions1[2]);
    
    buildings[50][50] = new Building(1);
  }
  boolean startInvalid(PVector p1, PVector p2){
    if(p1.dist(p2)<mapWidth/4){
      return true;
    }
    return false;
  }
  
  PVector[] generateStartingParties(){
    PVector player1 = PVector.random2D().mult(mapWidth/4).add(new PVector(mapWidth/4, mapHeight/2));
    PVector player2 = PVector.random2D().mult(mapWidth/4).add(new PVector(3*mapWidth/4, mapHeight/2));
    while(startInvalid(player1, player2)){
      player1 = PVector.random2D().mult(mapWidth/4).add(new PVector(mapWidth/4, mapHeight/2));
      player2 = PVector.random2D().mult(mapWidth/4).add(new PVector(3*mapWidth/4, mapHeight/2));
    }
    parties[(int)player1.y][(int)player1.x] = new Party(0, 100, 'r');
    parties[(int)player2.y][(int)player2.x] = new Party(1, 100, 'r');
    return  new PVector[]{player1, player2};
  }
  
  int[][] smoothMap(int distance, int firstType, int[][] terrain){
    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=0; y<mapHeight;y++){
      for (int x=0; x<mapWidth;x++){
        order.add(new int[] {x, y});
      }
    }
    Collections.shuffle(order);
    int[][] newMap = new int[mapHeight][mapWidth];
    for (int[] coord: order){
      int[] counts = new int[numOfGroundTypes+1];
      for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
       for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
         if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
           counts[terrain[y1][x1]]+=1;
         }
       }
      }
      int highest = terrain[coord[1]][coord[0]];
      for (int i=firstType; i<=numOfGroundTypes;i++){
        if (counts[i] > counts[highest]){
          highest = i;
        }
      }
      newMap[coord[1]][coord[0]] = highest;
    }
    return newMap;
  }
  
  
  int[][] generateMap(PVector[] playerStarts){
    
    int [][] terrain = new int[mapHeight][mapWidth];
    
    for(int y=0; y<mapHeight; y++){
      terrain[y][0] = 1;
      terrain[y][mapWidth-1] = 1;
    }
    for(int x=1; x<mapWidth-1; x++){
      terrain[0][x] = 1;
      terrain[mapHeight-1][x] = 1;
    }
    for(int i=0;i<numOfGroundSpawns;i++){
      int type = (int)random(numOfGroundTypes)+1;
      int x = (int)random(mapWidth-2)+1;
      int y = (int)random(mapHeight-2)+1;
      terrain[y][x] = type;
      // Water will be type 1
      if (type==1){
        for (int y1=y-waterLevel+1;y1<y+waterLevel;y1++){
         for (int x1 = x-waterLevel+1; x1<x+waterLevel;x1++){
           if (y1 < mapHeight && y1 >= 0 && x1 < mapWidth && x1 >= 0)
             terrain[y1][x1] = type;
         }
        }
      }
    }
    for (PVector playerStart: playerStarts){
      int type = (int)random(numOfGroundTypes-1)+2;
      int x = (int)playerStart.x;
      int y = (int)playerStart.y;
      terrain[y][x] = type;
      // Player spawns will act as water but without water
      for (int y1=y-waterLevel+1;y1<y+waterLevel;y1++){
       for (int x1 = x-waterLevel+1; x1<x+waterLevel;x1++){
         if (y1 < mapHeight && y1 >= 0 && x1 < mapWidth && x1 >= 0)
           terrain[y1][x1] = type;
       }
      }
    }
    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=1; y<mapHeight-1;y++){
      for (int x=1; x<mapWidth-1;x++){
        order.add(new int[] {x, y});
      }
    }
    Collections.shuffle(order);
    for (int[] coord: order){
      int x = coord[0];
      int y = coord[1];
      while (terrain[y][x] == 0){
        int direction = (int) random(8);
        switch(direction){
          case 0:
            x= max(x-1, 0);
            break;
          case 1:
            x = min(x+1, mapWidth-1);
            break;
          case 2:
            y= max(y-1, 0);
            break;
          case 3:
            y = min(y+1, mapHeight-1);
            break;
          case 4:
            x = min(x+1, mapWidth-1);
            y = min(y+1, mapHeight-1);
            break;
          case 5:
            x = min(x+1, mapWidth-1);
            y= max(y-1, 0);
            break;
          case 6:
            y= max(y-1, 0);
            x= max(x-1, 0);
            break;
          case 7:
            y = min(y+1, mapHeight-1);
            x= max(x-1, 0);
            break;
        }
      }
      terrain[coord[1]][coord[0]] = terrain[y][x];
    }
    terrain = smoothMap(initialSmooth, 2, terrain);
    terrain = smoothMap(completeSmooth, 1, terrain);
    return terrain;
  }
}