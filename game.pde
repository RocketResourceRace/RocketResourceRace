
class Game extends State{
  final int mapElementWidth = 1100;
  final int mapElementHeight = 700;
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezel = 20;
  final int[] terrainCosts = new int[]{4, 3, 2, 3};
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
  boolean cellSelected=false, moving=false;
  String[] landTypes;
  String[] buildingTypes;
  color partyManagementColour;
  
  Game(){
    addElement("map", new Map(bezel, bezel, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    addElement("end turn", new Button(bezel, height-buttonH-bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"));
    map = (Map)getElement("map", "default");
    players = new Player[2];
    
    // Initial positions will be focused on starting party
    players[0] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize);
    players[1] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize);
    addPanel("land management", 0, 0, width, height, false, color(50, 200, 50), color(0));
    addPanel("party management", 0, 0, width, height, false, color(220, 70, 70), color(0));
    //int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text
    addElement("move button", new Button(bezel, bezel*2, 100, 30, color(150), color(50), color(0), 10, CENTER, "Move"), "party management");
    
    landTypes = new String[]{"Water", "Sand", "Grass", "Forest"};
    buildingTypes = new String[]{"Homes", "Farm", "Mine", "Smelter", "Factory", "Sawmill", "Big Factory"};
  }
  void updateCellSelection(){
    cellSelectionX = round(mapElementWidth*GUIScale)+bezel*2;
    cellSelectionY = bezel;
    cellSelectionW = width-cellSelectionX-bezel;
    cellSelectionH = round(mapElementHeight*GUIScale);
    getPanel("land management").transform(cellSelectionX, cellSelectionY, cellSelectionW, round(cellSelectionH*0.3));
    getPanel("party management").transform(cellSelectionX, cellSelectionY+round(cellSelectionH*0.3)+bezel, cellSelectionW, round(cellSelectionH*0.7)-bezel);
  }
  String update(){
    if (changeTurn){  
      players[turn].saveMapSettings(map.mapXOffset, map.mapYOffset, map.blockSize);
      turn = (turn + 1)%2;
      players[turn].loadMapSettings(map);
      changeTurn = false;
    }
    drawBar();
    drawPanels();
    if (cellSelected){
      drawCellManagement();
      if(parties[cellY][cellX] != null)
        drawPartyManagement();
    }
    return getNewState();
  }
  void partyMovementPointsReset(int player){
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (map.parties[y][x] != null){
          if (map.parties[y][x].player == player){
            map.parties[y][x].movementPoints = 16;
          }
        }
      }
    }
  }
  void changeTurn(){
    changeTurn = true;
    partyMovementPointsReset(turn);
  }
  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type == "clicked"){
        if (event.id == "end turn"){
          changeTurn();
          
        }
        else if (event.id == "move button"){
          if (parties[cellY][cellX].player == turn){
            moving = !moving;
            if (moving){
              map.updateMoveNodes(djk(cellX, cellY, parties[cellY][cellX].movementPoints));
            }
            else{
              map.cancelMoveNodes();
            }
          }
        }
      }
    }
  }
  void deselectCell(){
    cellSelected = false;
    map.unselectCell();
    getPanel("land management").setVisible(false);
    getPanel("party management").setVisible(false);
    map.cancelMoveNodes();
    moving = false;
  }
  ArrayList<String> mouseEvent(String eventType, int button){
    if (button == LEFT){
      if (eventType == "mouseClicked"){
        if (moving){
          Node [][] nodes = map.moveNodes;
          int x = floor(map.scaleXInv(mouseX));
          int y = floor(map.scaleYInv(mouseY));
          if (nodes[y][x] != null && !(cellX == x && cellY == y)){
            map.parties[cellY][cellX].movementPoints-=nodes[y][x].cost;
            map.parties[y][x] = map.parties[cellY][cellX];
            map.parties[cellY][cellX] = null;
            deselectCell();
            if (map.parties[y][x].movementPoints > 0){
              selectCell(mouseX, mouseY);
              map.updateMoveNodes(djk(cellX, cellY, parties[cellY][cellX].movementPoints));
              moving = true;
              map.focusMapMouse(mouseX, mouseY);
            }
          }
        }
      }
    }
    if (button == RIGHT){
      if (eventType == "mouseClicked"){
        if (map.mouseOver()){
          if (cellSelected){
            deselectCell();
          }
          else{
            selectCell(mouseX, mouseY);
          }
        }
        else{
          deselectCell();
        }
      }
    }
    return new ArrayList<String>();
  }
  void selectCell(int x, int y){
    cellX = floor(map.scaleXInv(x));
    cellY = floor(map.scaleYInv(y));
    cellSelected = true;
    map.selectCell(cellX, cellY);
    getPanel("land management").setVisible(true);
    if (parties[cellY][cellX] != null){
      getPanel("party management").setVisible(true);
      if (parties[cellY][cellX].player == 1){
        partyManagementColour = color(170, 30, 30);
        getPanel("party management").setColour(color(220, 70, 70));
      }
      else{
        partyManagementColour = color(0, 0, 150);
        getPanel("party management").setColour(color(70, 70, 220));
      }
    }
  }
  void drawPartyManagement(){
    Panel pp = getPanel("party management");
    pushStyle();
    fill(partyManagementColour);
    rect(cellSelectionX, pp.y, cellSelectionW, 13*TextScale);
    fill(255);
    textSize(10*TextScale);
    textAlign(CENTER, TOP);
    text("Party Management", cellSelectionX+cellSelectionW/2, pp.y);
    
    textAlign(LEFT, CENTER);
    textSize(8*TextScale);
    float barY = cellSelectionY + 13*TextScale + cellSelectionH*0.3 + bezel*2;
    text("Movement Points Remaining: "+parties[cellY][cellX].movementPoints, 150+cellSelectionX, barY);
  }
  
  void drawCellManagement(){
    pushStyle();
    fill(0, 150, 0);
    rect(cellSelectionX, cellSelectionY, cellSelectionW, 13*TextScale);
    fill(255);
    textSize(10*TextScale);
    textAlign(CENTER, TOP);
    text("Land Management", cellSelectionX+cellSelectionW/2, cellSelectionY);
    
    textAlign(LEFT, TOP);
    float barY = cellSelectionY + 13*TextScale;
    text("Cell Type: "+landTypes[terrain[cellY][cellX]-1], 5+cellSelectionX, barY);
    barY += 13*TextScale;
    if (buildings[cellY][cellX] != null){
      text("Building: "+buildingTypes[buildings[cellY][cellX].type-1], 5+cellSelectionX, barY);
      barY += 13*TextScale;
    }
  }
  
  void drawBar(){
    float barX=buttonW+bezel*2;
    fill(200);
    stroke(170);
    rect(0, height-bezel*2-buttonH, width, buttonH+bezel*2);
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
    rect(barX, height-bezel-buttonH, textWidth(turnString)+10, buttonH);
    barX += textWidth(turnString)+10+bezel;
    fill(255);
    textAlign(CENTER, TOP);
    text(turnString, bezel*2+buttonW+(textWidth(turnString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    barX=width;
    String tempString = "food:"+players[turn].food;
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "wood:"+players[turn].wood;
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "metal:"+players[turn].metal;
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "energy:"+players[turn].energy;
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
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
    players[1] = new Player(conditions2[0], conditions2[1], 64);
    float[] conditions1 = map.targetCell((int)playerStarts[0].x, (int)playerStarts[0].y, 64);
    players[0] = new Player(conditions1[0], conditions1[1], 64);
    for(int i=0;i<NUMOFBUILDINGTYPES;i++){
      buildings[(int)playerStarts[0].y][(int)playerStarts[0].x+i] = new Building(1+i);
    }
    deselectCell();
  }
  int cost(int x, int y){
    if (0<x && x<mapSize && 0<y && y<mapSize){
      if (map.parties[y][x] == null){
        return terrainCosts[terrain[y][x]-1];
      }
    }
    //Not a valid location
    return mapWidth*2;
  }
  int[] minNode(Node[][] nodes, int w, int h){
    int[] m = {-1, -1};
    int cost = w;
    for (int y=0; y<h; y++){
      for (int x=0; x<w; x++){
        if (nodes[y][x] != null && !nodes[y][x].fixed && nodes[y][x].cost < cost){
          m = new int[]{x, y};
          cost = nodes[y][x].cost;
        }
      }
    }
    return m;
  }
  Node[][] djk(int x, int y, int availablePoints){
    int xOff = 0;
    int yOff = 0;
    int w = mapWidth;
    int h = mapHeight;
    Node[][] nodes = new Node[h][w];
    int cx = x-xOff;
    int cy = y-yOff;
    nodes[cy][cx] = new Node(0, false);
    int[] curMinNode = {cx, cy};
    while (curMinNode[0] != -1 && curMinNode[1] != -1){
      //println(curMinNode[0], curMinNode[1], cx, cy, nodes[curMinNode[1]][curMinNode[0]].fixed);
      nodes[curMinNode[1]][curMinNode[0]].fixed = true;
      if (0 < curMinNode[0] + 1 && curMinNode[0] + 1 < w && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0] + 1, curMinNode[1]) <= availablePoints){
        if (nodes[curMinNode[1]][curMinNode[0]+1] == null){
          nodes[curMinNode[1]][curMinNode[0]+1] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0] + 1, curMinNode[1]), false);
        }
        else{
          nodes[curMinNode[1]][curMinNode[0]+1].cost = min(nodes[curMinNode[1]][curMinNode[0]+1].cost, nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0] + 1, curMinNode[1]));
        }
      }
      if (0 < curMinNode[0] - 1 && curMinNode[0] + 1 < w && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0]-1, curMinNode[1]) <= availablePoints){
        if (nodes[curMinNode[1]][curMinNode[0]-1] == null){
          nodes[curMinNode[1]][curMinNode[0]-1] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0]-1, curMinNode[1]), false);
        }
        else{
          nodes[curMinNode[1]][curMinNode[0]-1].cost = min(nodes[curMinNode[1]][curMinNode[0]-1].cost, nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0]-1, curMinNode[1]));
        }
      }
      if (0 < curMinNode[1] + 1 && curMinNode[1] + 1 < h && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]+1) <= availablePoints){
        if (nodes[curMinNode[1]+1][curMinNode[0]] == null){
          nodes[curMinNode[1]+1][curMinNode[0]] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]+1), false);
        }
        else{
          nodes[curMinNode[1]+1][curMinNode[0]].cost = min(nodes[curMinNode[1]+1][curMinNode[0]].cost, nodes[curMinNode[1]+1][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]+1));
        }
      }
      if (0 < curMinNode[1] - 1 && curMinNode[1] - 1 < h && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]-1) <= availablePoints){
        if (nodes[curMinNode[1]-1][curMinNode[0]] == null){
          nodes[curMinNode[1]-1][curMinNode[0]] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]-1), false);
        }
        else{
          nodes[curMinNode[1]-1][curMinNode[0]].cost = min(nodes[curMinNode[1]-1][curMinNode[0]].cost, nodes[curMinNode[1]-1][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]-1));
        }
      }
      curMinNode = minNode(nodes, w, h);
    }
    return nodes;
  }
  boolean startInvalid(PVector p1, PVector p2){
    if(p1.dist(p2)<mapWidth/4){
      return true;
    }
    return false;
  }
  
  PVector[] generateStartingParties(){
    PVector player1 = PVector.random2D().mult(mapWidth/8).add(new PVector(mapWidth/4, mapHeight/2));
    PVector player2 = PVector.random2D().mult(mapWidth/8).add(new PVector(3*mapWidth/4, mapHeight/2));
    while(startInvalid(player1, player2)){
      player1 = PVector.random2D().mult(mapWidth/8).add(new PVector(mapWidth/4, mapHeight/2));
      player2 = PVector.random2D().mult(mapWidth/8).add(new PVector(3*mapWidth/4, mapHeight/2));
    }
    parties[(int)player1.y][(int)player1.x] = new Party(0, 100, 'r', 16);
    parties[(int)player2.y][(int)player2.x] = new Party(1, 100, 'r', 16);
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
      int[] counts = new int[NUMOFGROUNDTYPES+1];
      for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
       for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
         if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
           counts[terrain[y1][x1]]+=1;
         }
       }
      }
      int highest = terrain[coord[1]][coord[0]];
      for (int i=firstType; i<=NUMOFGROUNDTYPES;i++){
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
    for(int i=0;i<NUMOFGROUNDSPAWNS;i++){
      int type = (int)random(NUMOFGROUNDTYPES)+1;
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
      int type = (int)random(NUMOFGROUNDTYPES-1)+2;
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