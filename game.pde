
class Game extends State{
  final int mapElementWidth = (int) (width*0.6);
  final int mapElementHeight = (int) (height*0.7);
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezel = 20;
  final int[] terrainCosts = new int[]{8, 6, 4, 3, 2, 3};
  final int MOVEMENTPOINTS = 24;
  final int DEFENDCOST = 6;
  final float [] STARTINGRESOURCES = new float[]{500, 300, 0, 0};
  final String[] tasks = {"Rest", "Farm", "Defend", "Demolish", "Build Farm", "Build Sawmill", "Build Homes", "Clear Forest", "Battle"};
  final String[] landTypes = {"Water", "Sand", "Grass", "Forest"};
  final String[] buildingTypes = {"Homes", "Farm", "Mine", "Smelter", "Factory", "Sawmill", "Big Factory"};
  final String[] tooltipText = {
    "Defending improves fighting\neffectiveness against enemy parties\nCosts: 6 movement points.",
    "The farm produces food when worked.\nCosts: 50 wood.\nConsumes: Nothing.\nProduces: 1 food/worker.\nThis takes 3 turns to build.",
    "The mine produces ore when worked.\nCosts: 200 wood.\nConsumes: 10 wood.\nProduces: 1 ore/worker",
    "Homes create new units when worked.\nCosts: 100 wood.\nConsumes: 2 wood.\n",
    "The smelter produces metal when worked.\nCosts: 200 wood.\nConsumes: 10 wood.\nProduces: 0.1 iron/worker",
    "The factory produces parts when worked.\nCosts: 200 wood.\nConsumes: 10 wood",
    "The sawmill produces wood when worked.\nCosts: 200 wood.\nConsumes: 10 wood\nProduces: 0.5 wood/worker\nThis takes 5 turns to build.",
    "Clearing a forest adds 100 wood to stockpile.\nThe tile is turned to grassland\nThis takes 3 turns.",
    "Demolishing destroys the building on this tile.\nThis takes 2 turns.",
    "While a unit is at rest it can move and attack.",
    "Merge parties.\nThis action will create a single party from two.\nThe new party has no action points this turn.",
    "Attack enemy party.\nThis action will cause a battle to occur.\nBoth parties are trapped in combat until one is eliminated.",
  };
  final float[][] costs = {
    {0, 100, 0, 0},
    {0, 50, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 0, 0, 0},
    {0, 200, 0, 0},
  };
  final int NUMRESOURCES = 4;
  int mapHeight = mapSize;
  int mapWidth = mapSize;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  int turn;
  boolean changeTurn = false;
  Map map;
  Player[] players;
  int cellX, cellY, cellSelectionX, cellSelectionY, cellSelectionW, cellSelectionH;
  boolean cellSelected=false, moving=false;
  color partyManagementColour;
  int toolTipSelected;
  
  Game(){
    addElement("map", new Map(bezel, bezel, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    addElement("end turn", new Button(bezel, height-buttonH-bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"));
    map = (Map)getElement("map", "default");
    players = new Player[2];
    
    // Initial positions will be focused on starting party
    players[0] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize, STARTINGRESOURCES);
    players[1] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize, STARTINGRESOURCES);
    addPanel("land management", 0, 0, width, height, false, color(50, 200, 50), color(0));
    addPanel("party management", 0, 0, width, height, false, color(70, 70, 220), color(0));
    //int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text
    addElement("move button", new Button(bezel, bezel*2, 100, 30, color(150), color(50), color(0), 10, CENTER, "Move"), "party management");
    //int x, int y, int w, int h, color KnobColour, color bgColour, color strokeColour, color scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name
    addElement("split units", new Slider(bezel+10, bezel*3+30, 200, 30, color(255), color(150), color(0), color(0), 0, 0, 0, 1, 1, 1, true, ""), "party management");
    addElement("split button", new Button(bezel*2+220, bezel*3+30, 100, 30, color(150), color(50), color(0), 10, CENTER, "Split"), "party management");
    addElement("tasks", new DropDown(bezel, bezel*4+30+30, 200, 10, color(150), color(50), tasks), "party management");

    
    toolTipSelected=-1;
  }
  void updateCellSelection(){
    cellSelectionX = round(mapElementWidth*GUIScale)+bezel*2;
    cellSelectionY = bezel;
    cellSelectionW = width-cellSelectionX-bezel;
    cellSelectionH = round(mapElementHeight*GUIScale);
    getPanel("land management").transform(cellSelectionX, cellSelectionY, cellSelectionW, round(cellSelectionH*0.3));
    getPanel("party management").transform(cellSelectionX, cellSelectionY+round(cellSelectionH*0.3)+bezel, cellSelectionW, round(cellSelectionH*0.7)-bezel);
  }
  
  void makeTaskAvailable(String task){
    ((DropDown)getElement("tasks", "party management")).makeAvailable(task);
  }
  void resetAvailableTasks(){
    ((DropDown)getElement("tasks", "party management")).resetAvailable();
  }
  //tasks building
  //settings
  //moving
  //attacking
  //task not defend moving
  
  void checkTasks(){
    resetAvailableTasks();
    if(parties[cellY][cellX].player==2){
      makeTaskAvailable("Battle");
    } else {
      makeTaskAvailable("Rest");
      int cellTerrain = terrain[cellY][cellX];
      if (parties[cellY][cellX].movementPoints >= DEFENDCOST)
        makeTaskAvailable("Defend");
      if (buildings[cellY][cellX] != null){
        if (buildings[cellY][cellX].type==2){
          makeTaskAvailable("Farm");
        }
        makeTaskAvailable("Demolish");
      }
      else{
        if (cellTerrain == 3){
          makeTaskAvailable("Build Farm");
        } //<>//
        else if (cellTerrain == 4){
          makeTaskAvailable("Clear Forest");
          makeTaskAvailable("Build Sawmill");
        }
        if (cellTerrain != 1 && cellTerrain != 4){
          makeTaskAvailable("Build Homes");
        }
      } //<>//
    } //<>//
    ((DropDown)getElement("tasks", "party management")).select(parties[cellY][cellX].task);
  }
  
  ArrayList<String> getLines(String s){
    int j = 0;
    ArrayList<String> lines = new ArrayList<String>();
    for (int i=0; i<s.length(); i++){
      if(s.charAt(i) == '\n'){
        lines.add(s.substring(j, i));
        j=i+1;
      }
    }
    lines.add(s.substring(j, s.length()));
    return lines;
  }
  
  float maxWidthLine(ArrayList<String> lines){
    float ml = 0;
    for (int i=0; i<lines.size(); i++){
      if (textWidth(lines.get(i)) > ml){
        ml = textWidth(lines.get(i));
      }
    }
    return ml;
  }
  
  void drawToolTip(){
    ArrayList<String> lines = getLines(tooltipText[toolTipSelected]);
    textSize(8*TextScale);
    int tw = ceil(maxWidthLine(lines))+4;
    int gap = ceil(textAscent()+textDescent());
    int th = ceil(textAscent()+textDescent())*lines.size();
    int tx = mouseX-tw/2;
    int ty = mouseY+20;
    fill(255, 200);
    stroke(0);
    rectMode(CORNER);
    rect(tx, ty, tw, th);
    fill(0);
    textAlign(LEFT, TOP);
    for (int i=0; i<lines.size(); i++){
      text(lines.get(i), tx+2, ty+i*gap);
    }
  }
  
  void turnChange(){
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (map.parties[y][x] != null){
          if (map.parties[y][x].player == turn){
            String action = map.parties[y][x].progressAction();
            switch (action){
              //-1 building types
              case "Clear Forest":
                map.terrain[y][x] = 3;
                break;
              case "Build Farm":
                map.buildings[y][x] = new Building(2);
                break;
              case "Build Sawmill":
                map.buildings[y][x] = new Building(6);
                break;
              case "Build Homes":
                map.buildings[y][x] = new Building(1);
                break;
              case "Demolish":
                reclaimRes(players[turn], costs[5]);
                map.buildings[y][x] = null;
                break;
            }
            if (action != ""){
              map.parties[y][x].clearCurrentAction();
              map.parties[y][x].task="Rest";
            }
          } else {
            if(map.parties[y][x].player==2){
              map.parties[y][x] = ((Battle)map.parties[y][x]).doBattle();
            }
          }
        }
      }
    }
    deselectCell();
    partyMovementPointsReset(turn);
    
    players[turn].saveMapSettings(map.mapXOffset, map.mapYOffset, map.blockSize);
    turn = (turn + 1)%2;
    players[turn].loadMapSettings(map);
    changeTurn = false;
  }
  
  String update(){
    if (changeTurn){  
      turnChange();
    }
    drawBar();
    drawPanels();
    if (cellSelected){
      drawCellManagement();
      if(parties[cellY][cellX] != null && getPanel("party management").visible)
        drawPartyManagement();
    }
    if (toolTipSelected >= 0){
      drawToolTip();
    }
    return getNewState();
  }
  void partyMovementPointsReset(int player){
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (map.parties[y][x] != null){
          if (map.parties[y][x].player == player){
            map.parties[y][x].movementPoints = MOVEMENTPOINTS;
          }
        }
      }
    }
  }
  void changeTurn(){
    changeTurn = true;
  }
  boolean sufficientResources(float[] available, float[] required){
    for (int i=0; i<required.length;i++){
      if (available[i] < required[i]){
        return false;
      }
    }
    return true;
  }
  void spendRes(Player player, float[] required){
    for (int i=0; i<player.resources.length;i++){
      player.resources[i] -= required[i];
    }
  }
  void reclaimRes(Player player, float[] required){
    //reclaim half cost of building
    for (int i=0; i<player.resources.length;i++){
      player.resources[i] += required[i]/2;
    }
  }
  int[] newPartyLoc(){
    ArrayList<int[]> locs = new ArrayList<int[]>();
    locs.add(new int[]{cellX+1, cellY});
    locs.add(new int[]{cellX-1, cellY});
    locs.add(new int[]{cellX, cellY+1});
    locs.add(new int[]{cellX, cellY-1});
    Collections.shuffle(locs);
    for (int i=0; i<4; i++){
      if (parties[locs.get(i)[1]][locs.get(i)[0]] == null && terrain[locs.get(i)[1]][locs.get(i)[0]] != 1){
        return locs.get(i);
      }
    }
    return null;
  }
  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type == "clicked"){
        if (event.id == "split button"){
          int[] loc = newPartyLoc();
          int sliderVal = round(((Slider)getElement("split units", "party management")).getValue());
          if (loc != null && sliderVal > 0 && parties[cellY][cellX].unitNumber >= 2 && parties[cellY][cellX].task != "Battle"){
            parties[loc[1]][loc[0]] = new Party(turn, sliderVal, "Rest", 0);
            parties[cellY][cellX].unitNumber -= sliderVal;
            deselectCell();
            selectCell(loc[0], loc[1]);
          }
        }
      }
      if (event.type == "valueChanged"){
        if (event.id == "tasks"){
          if (parties[cellY][cellX].task == "Defend"){
            //Changing from defending
            parties[cellY][cellX].movementPoints = min(parties[cellY][cellX].movementPoints+DEFENDCOST, MOVEMENTPOINTS);
          }
          parties[cellY][cellX].changeTask(((DropDown)getElement("tasks", "party management")).getSelected());
          if (parties[cellY][cellX].task != "Rest"){
            moving = false;
            map.cancelMoveNodes();
          }
          if (parties[cellY][cellX].task == "Defend"){
            parties[cellY][cellX].movementPoints -= DEFENDCOST;
          }
          else if (parties[cellY][cellX].task == "Demolish"){
            if (sufficientResources(players[turn].resources, costs[buildings[cellY][cellX].type])){
              parties[cellY][cellX].addAction(new Action("Demolish", 2));
            }
          }
          else if (parties[cellY][cellX].task == "Clear Forest"){
            parties[cellY][cellX].addAction(new Action("Clear Forest", 3));
          }
          else if (parties[cellY][cellX].task == "Build Farm"){
            if (sufficientResources(players[turn].resources, costs[1])){
              parties[cellY][cellX].addAction(new Action("Build Farm", 3));
              spendRes(players[turn], costs[1]);
              buildings[cellY][cellX] = new Building(0);
            }
          }
          else if (parties[cellY][cellX].task == "Build Sawmill"){
            if (sufficientResources(players[turn].resources, costs[5])){
              parties[cellY][cellX].addAction(new Action("Build Sawmill", 5));
              spendRes(players[turn], costs[5]);
              buildings[cellY][cellX] = new Building(0);
            }
          }
          else if (parties[cellY][cellX].task == "Build Homes"){
            if (sufficientResources(players[turn].resources, costs[0])){
              parties[cellY][cellX].addAction(new Action("Build Homes", 3));
              spendRes(players[turn], costs[0]);
              buildings[cellY][cellX] = new Building(0);
            }
          }
        }
      }
      if (event.type == "clicked"){
        if (event.id == "end turn"){
          changeTurn();
          
        }
        else if (event.id == "move button"){
          if (parties[cellY][cellX].player == turn && parties[cellY][cellX].task == "Rest"){
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
        if (moving&&map.mouseOver()){
          Node [][] nodes = map.moveNodes;
          int x = floor(map.scaleXInv(mouseX));
          int y = floor(map.scaleYInv(mouseY));
          if (nodes[y][x] != null && !(cellX == x && cellY == y)){
            map.parties[cellY][cellX].movementPoints-=nodes[y][x].cost;
            if(map.parties[y][x]==null){
              map.parties[y][x] = map.parties[cellY][cellX];
            }
            else {
              if (map.parties[y][x].player == turn){
                //merge parties
                map.parties[y][x].unitNumber += map.parties[cellY][cellX].unitNumber;
                map.parties[cellY][cellX] = null;
                map.parties[y][x].movementPoints = 0;
              }
              else {
                map.parties[y][x] = new Battle(map.parties[cellY][cellX], map.parties[y][x]);
                if(map.buildings[y][x]!=null&&map.buildings[y][x].type==0){
                  map.buildings[y][x] = null;
                }
                ((Slider)getElement("split units", "party management")).hide();
              }
            }
            map.parties[cellY][cellX] = null;
            deselectCell();
            selectCell(mouseX, mouseY);
            moving = false;
            map.focusMapMouse(mouseX, mouseY);
            if (map.parties[y][x].movementPoints > 0){
              map.updateMoveNodes(djk(cellX, cellY, parties[cellY][cellX].movementPoints));
              moving = true;
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
    if (eventType == "mouseMoved"){
      if (((DropDown)getElement("tasks", "party management")).moveOver()){
        switch (((DropDown)getElement("tasks", "party management")).findMouseOver()){
          case "Defend":toolTipSelected = 0;break;
          case "Build Farm":toolTipSelected = 1;break;
          case "Build Sawmill":toolTipSelected = 6;break;
          case "Build Homes":toolTipSelected = 3;break;
          case "Clear Forest":toolTipSelected = 7;break;
          case "Demolish":toolTipSelected = 8;break;
          case "Rest":toolTipSelected = 9;break;
          default: toolTipSelected = -1;break;
        }
      }
      else if (moving&&map.mouseOver()){
          Node [][] nodes = map.moveNodes;
          int x = floor(map.scaleXInv(mouseX));
          int y = floor(map.scaleYInv(mouseY));
          if (nodes[y][x] != null && !(cellX == x && cellY == y)){
            if(map.parties[y][x]==null){
              toolTipSelected = -1;
            }
            else {
              if (map.parties[y][x].player == turn){
                //merge parties
                toolTipSelected = 10;
              }
              else {
                toolTipSelected = 11;
              }
            }
          }
          else{
            toolTipSelected = -1;
          }
        }
      else{
        toolTipSelected = -1;
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
    if (parties[cellY][cellX] != null && parties[cellY][cellX].isTurn(turn)){
      if (parties[cellY][cellX].task != "Battle"){
        ((Slider)getElement("split units", "party management")).show();
      }
      else{
        ((Slider)getElement("split units", "party management")).hide();
      }
      getPanel("party management").setVisible(true);
      if (parties[cellY][cellX].unitNumber <= 1){
          ((Slider)getElement("split units", "party management")).hide();
      }
      else
      ((Slider)getElement("split units", "party management")).setScale(1, 1, parties[cellY][cellX].unitNumber-1, 1, parties[cellY][cellX].unitNumber);
      if (turn == 1){
        partyManagementColour = color(170, 30, 30);
        getPanel("party management").setColour(color(220, 70, 70));
      } else {
        partyManagementColour = color(0, 0, 150);
        getPanel("party management").setColour(color(70, 70, 220));
      }
      checkTasks();
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
    text("Movement Points Remaining: "+parties[cellY][cellX].getMovementPoints(turn) + "/"+MOVEMENTPOINTS, 150+cellSelectionX, barY);
    barY += 13*TextScale;
    text("Units: "+parties[cellY][cellX].getUnitNumber(turn) + "/1000", 150+cellSelectionX, barY);
    barY += 13*TextScale;
    if (parties[cellY][cellX].actions.size() > 0 && parties[cellY][cellX].actions.get(0).initialTurns > 0){
      text("Turns Remaining: "+parties[cellY][cellX].actions.get(0).turns + "/"+parties[cellY][cellX].actions.get(0).initialTurns, 150+cellSelectionX, barY);
      barY += 13*TextScale;
    }
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
      text("Building: "+buildingTypes[buildings[cellY][cellX].type], 5+cellSelectionX, barY);
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
    if (this.turn==1){
      fill(255, 0, 0);
      turnString = "Red Player's Turn";
    }
    else if (this.turn==0){
      fill(0, 0, 255);
      turnString = "Blue Player's Turn";
    }
    stroke(50);
    rect(barX, height-bezel-buttonH, textWidth(turnString)+10, buttonH);
    barX += textWidth(turnString)+10+bezel;
    fill(255);
    textAlign(CENTER, TOP);
    text(turnString, bezel*2+buttonW+(textWidth(turnString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    
    String tempString;
    barX=width;
    tempString = "energy:"+players[turn].resources[3];
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "metal:"+players[turn].resources[2];
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "wood:"+players[turn].resources[1];
    barX -= textWidth(tempString)+10+bezel;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    
    tempString = "food:"+players[turn].resources[0];
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
    
    float[] conditions2 = map.targetCell((int)playerStarts[1].x, (int)playerStarts[1].y, 42);
    players[1] = new Player(conditions2[0], conditions2[1], 42, STARTINGRESOURCES.clone());
    float[] conditions1 = map.targetCell((int)playerStarts[0].x, (int)playerStarts[0].y, 42);
    players[0] = new Player(conditions1[0], conditions1[1], 42, STARTINGRESOURCES.clone());
    //for(int i=0;i<NUMOFBUILDINGTYPES;i++){
    //  buildings[(int)playerStarts[0].y][(int)playerStarts[0].x+i] = new Building(1+i);
    //}
    deselectCell();
    turn = 0;
    toolTipSelected=-1;
  }
  int cost(int x, int y){
    if (0<x && x<mapSize && 0<y && y<mapSize){
      if (map.parties[y][x] == null){
        return terrainCosts[terrain[y][x]+1];
      } else if(map.parties[y][x].player!=this.turn){
        return terrainCosts[0];
      }else if(map.parties[y][x].player==this.turn){
        return terrainCosts[1];
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
      if (0 < curMinNode[0] + 1 && curMinNode[0] + 1 < w && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0] + 1, curMinNode[1]) <= availablePoints+MOVEMENTPOINTS){
        if (nodes[curMinNode[1]][curMinNode[0]+1] == null){
          nodes[curMinNode[1]][curMinNode[0]+1] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0] + 1, curMinNode[1]), false);
        }
        else{
          nodes[curMinNode[1]][curMinNode[0]+1].cost = min(nodes[curMinNode[1]][curMinNode[0]+1].cost, nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0] + 1, curMinNode[1]));
        }
      }
      if (0 < curMinNode[0] - 1 && curMinNode[0] + 1 < w && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0]-1, curMinNode[1]) <= availablePoints+MOVEMENTPOINTS){
        if (nodes[curMinNode[1]][curMinNode[0]-1] == null){
          nodes[curMinNode[1]][curMinNode[0]-1] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0]-1, curMinNode[1]), false);
        }
        else{
          nodes[curMinNode[1]][curMinNode[0]-1].cost = min(nodes[curMinNode[1]][curMinNode[0]-1].cost, nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0]-1, curMinNode[1]));
        }
      }
      if (0 < curMinNode[1] + 1 && curMinNode[1] + 1 < h && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]+1) <= availablePoints+MOVEMENTPOINTS){
        if (nodes[curMinNode[1]+1][curMinNode[0]] == null){
          nodes[curMinNode[1]+1][curMinNode[0]] = new Node(nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]+1), false);
        }
        else{
          nodes[curMinNode[1]+1][curMinNode[0]].cost = min(nodes[curMinNode[1]+1][curMinNode[0]].cost, nodes[curMinNode[1]+1][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]+1));
        }
      }
      if (0 < curMinNode[1] - 1 && curMinNode[1] - 1 < h && nodes[curMinNode[1]][curMinNode[0]].cost+cost(curMinNode[0], curMinNode[1]-1) <= availablePoints+MOVEMENTPOINTS){
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
    parties[(int)player1.y][(int)player1.x] = new Party(0, 100, "Rest", MOVEMENTPOINTS);
    parties[(int)player1.y][(int)player1.x+1] = new Party(1, 100, "Rest", MOVEMENTPOINTS);
    
    return  new PVector[]{player1, player1};
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
    for(int i=0;i<groundSpawns;i++){
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