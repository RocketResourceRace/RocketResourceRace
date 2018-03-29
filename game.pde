
class Game extends State{
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezel = 10;
  final int mapElementWidth = round(width-bezel*2);
  final int mapElementHeight = round(height-bezel*4-buttonH);
  final int[] terrainCosts = new int[]{16, 12, 8, 12, 28};
  final int MOVEMENTPOINTS = 64;
  final int DEFENDCOST = 32;
  final float [] STARTINGRESOURCES = new float[]{500, 500, 0, 0, 0, 0, 0, 0, 0};
  final String[] tasks = {"Rest", "Work Farm", "Defend", "Demolish", "Build Farm", "Build Sawmill", "Build Homes", "Build Factory", "Build Mine", "Build Smelter", "Clear Forest", "Battle", "Super Rest", "Produce Ore", "Produce Metal", "Produce Concrete", "Produce Cable", "Produce Wood", "Produce Spacehip Parts"};
  final String[] landTypes = {"Water", "Sand", "Grass", "Forest", "Hills"};
  final String[] buildingTypes = {"Homes", "Farm", "Mine", "Smelter", "Factory", "Sawmill", "Big Factory"};
  final int WATER = 1;
  final int SAND = 2;
  final int GRASS = 3;
  final int FOREST = 4;
  final int HILLS = 5;
  final int CONSTRUCTION = 0;
  final int HOMES = 1;
  final int FARM = 2;
  final int MINE = 3;
  final int SMELTER = 4;
  final int FACTORY = 5;
  final int SAWMILL = 6;
  final int BIG_FACTORY = 6;
  final int FOOD = 0;
  final int WOOD = 1;
  final int METAL = 2;
  final int ENERGY = 3;
  final int CONCRETE = 4;
  final int CABLE = 5;
  final int SPACESHIP_PARTS = 6;
  final int ORE = 7;
  final int PEOPLE = 8;
  
  final String attackToolTipRaw = "Attack enemy party.\nThis action will cause a battle to occur.\nBoth parties are trapped in combat until one is eliminated. You have a /p% chance of winning this battle.";
  final String turnsToolTipRaw = "Move /i Turns";
  final String farmToolTipRaw = "The farm produces food when worked.\nCosts: 50 wood.\nConsumes: Nothing.\nProduces: 4 food/worker.\nThis party will take %d turns to build it";
  final String mineToolTipRaw = "The mine produces ore when worked.\nCosts: 200 wood.\nConsumes: 1 wood/worker.\nProduces: 1 ore/worker.\nThis party will take %d turns to build it";
  final String homesToolTipRaw = "Homes create new units when worked.\nCosts: 100 wood.\nConsumes: 2 wood.\nThis party will take %d turns to build it";
  final String smelterToolTipRaw = "The smelter produces metal when worked.\nCosts: 200 wood.\nConsumes: 10 wood.\nProduces: 0.1 iron/worker.\nThis party will take %d turns to build it";
  final String factoryToolTipRaw = "The factory produces parts when worked.\nCosts: 200 wood.\nConsumes: 10 wood.\nThis party will take %d turns to build it";
  final String sawmillToolTipRaw = "The sawmill produces wood when worked.\nCosts: 200 wood.\nConsumes: 10 wood\nProduces: 0.5 wood/worker\nThis party will take %d turns to build it";
  final String clearForestToolTipRaw = "Clearing a forest adds 100 wood to stockpile.\nThe tile is turned to grassland\nThis party will take %d turns to build it";
  final String demolishingToolTipRaw = "Demolishing destroys the building on this tile.\nThis party will take %i turns to build it";
  
  
  String[] tooltipText = {
    "Defending improves fighting\neffectiveness against enemy parties\nCosts: 32 movement points.",
    "farm",
    "mine",
    "homes",
    "smelter",
    "factory",
    "sawmill",
    "clear forest",
    "demolishing",
    "While a unit is at rest it can move and attack.",
    "Merge parties.\nThis action will create a single party from two.\nThe new party has no action points this turn.",
    "Attack enemy party.",
    "Move.",
    "Super Rest adds more units to a party each turn.",
    "The rate that an action is completed is affected\nby the number of units in a party\n(a square root relationship).\nThe turn time for 100 units is given for tasks.",
    "A party must be at rest to move",
    "This splits the number of units selected.\nCan only split when movement points > 0.",
    "",
  };
  final float[] buildingTimes = {0, 3, 2, 5, 8, 8, 4, 12};
  final String[] resourceNames = {"Food", "Wood", "Metal", "Energy", "", "", "", "", "Units"};
  final float[][] buildingCosts = {
    {0, 100, 0, 0, 0, 0, 0, 0, 0},
    {0, 50, 0, 0, 0, 0, 0, 0, 0},
    {0, 200, 0, 0, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 0, 0, 0, 0, 0},
    {0, 200, 0, 0, 0, 0, 0, 0, 0},
    {0, 200, 0, 0, 0, 0, 0, 0, 0},
  };
  
  // Costs per productivity per unit
  final float[][] taskCosts = {
    {0.1, 0, 0, 0, 0, 0, 0, 0, 0}, // Rest
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Farm
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Defend
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Demolish
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Farm
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Sawmill
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Homes
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Factory
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Mine
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Smelter
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Clear Forest
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Battle
    {0.5, 0, 0, 0, 0, 0, 0, 0, 0}, // Super Rest
    {0.2, 1, 0, 0, 0, 0, 0, 0, 0}, // Produce Ore
    {0.2, 0, 0, 0, 0, 0, 0, 1, 0}, // Produce Metal
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Produce Concrete
    {0.2, 0, 1, 0, 0, 0, 0, 0, 0}, // Produce Cable
    {0.2, 0, 0, 0, 0, 0, 0, 0, 0}, // Produce Wood
    {0.2, 0, 1, 0, 0, 0, 0, 0, 0}, // Produce Spaceship Parts
  };
  
  // Outcomes per productivity per unit
  final float[][] taskOutcomes = {
    {0, 0, 0, 0, 0, 0, 0, 0, 0.01}, // Rest
    {0.4, 0, 0, 0, 0, 0, 0, 0, 0}, // Farm
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Defend
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Demolish
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Farm
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Sawmill
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Homes
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Factory
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Mine
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Build Smelter
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Clear Forest
    {0, 0, 0, 0, 0, 0, 0, 0, 0}, // Battle
    {0, 0, 0, 0, 0, 0, 0, 0, 0.1}, // Super Rest
    {0, 0, 0, 0, 0, 0, 0, 1, 0}, // Produce Ore
    {0, 0, 0.1, 0, 0, 0, 0, 0, 0}, // Produce Metal
    {0, 0, 0, 0, 0.1, 0, 0, 0, 0}, // Produce Concrete
    {0, 0, 0, 0, 0, 0.1, 0, 0, 0}, // Produce Cable
    {0, 0.1, 0, 0, 0, 0, 0, 0, 0}, // Produce Wood
    {0, 0, 0, 0, 0, 0, 0.1, 0, 0}, // Produce Spaceship Parts
  };
  final int NUMRESOURCES = 9;
  int turnNumber;
  int mapHeight = mapSize;
  int mapWidth = mapSize;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  int turn;
  boolean changeTurn = false;
  int winner = -1;
  Map map;
  Player[] players;
  int cellX, cellY, cellSelectionX, cellSelectionY, cellSelectionW, cellSelectionH;
  boolean cellSelected=false, moving=false;
  color partyManagementColour;
  int toolTipSelected;
  ArrayList<Integer[]> prevIdle;
  float[] totals = {0,0,0,0,0,0,0,0,0};
  Party splittedParty;
  Game(){
    addElement("map", new Map(bezel, bezel, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    addElement("end turn", new Button(bezel, height-buttonH-bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"));
    addElement("idle party finder", new Button(bezel*2+buttonW, height-buttonH-bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Idle Party"));
    map = (Map)getElement("map", "default");
    players = new Player[2];
    
    // Initial positions will be focused on starting party
    players[0] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize, STARTINGRESOURCES);
    players[1] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize, STARTINGRESOURCES);
    addPanel("land management", 0, 0, width, height, false, color(50, 200, 50), color(0));
    addPanel("party management", 0, 0, width, height, false, color(70, 70, 220), color(0));
    addPanel("end screen", 0, 0, width, height, false, color(50, 50, 50, 50), color(0));
    addElement("end game button", new Button((int)(width/2-GUIScale*width/16), (int)(height/2+height/8), (int)(GUIScale*width/8), (int)(GUIScale*height/16), color(70, 70, 220), color(50, 50, 200), color(255), (int)(TextScale*10), CENTER, "End Game"), "end screen");
    addElement("winner", new Text(width/2, height/2, (int)(TextScale*10), "", color(255), CENTER), "end screen");
    addElement("turns remaining", new Text(bezel*2+220, bezel*4+30+30, 8, "", color(255), LEFT), "party management");
    //int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text
    addElement("move button", new Button(bezel, bezel*3, 100, 30, color(150), color(50), color(0), 10, CENTER, "Move"), "party management");
    //int x, int y, int w, int h, color KnobColour, color bgColour, color strokeColour, color scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name
    addElement("split units", new Slider(bezel+10, bezel*3+30, 220, 30, color(255), color(150), color(0), color(0), 0, 0, 0, 1, 1, 1, true, ""), "party management");
    addElement("tasks", new DropDown(bezel, bezel*4+30+30, 220, 10, color(150), color(50), tasks), "party management");
    prevIdle = new ArrayList<Integer[]>();
  }     
  boolean postEvent(GameEvent event){
    if (event instanceof Move){
      
      Move m = (Move)event;
      int x = m.endX;
      int y = m.endY;
      int cellX = m.startX;
      int cellY = m.startY;
      
      if (canMove(cellX, cellY)){
        int sliderVal = round(((Slider)getElement("split units", "party management")).getValue());
        if (sliderVal > 0 && parties[cellY][cellX].getUnitNumber() >= 2 && parties[cellY][cellX].getTask() != "Battle"){
          map.updateMoveNodes(djk(cellX, cellY));
          moving = true;
          splittedParty = new Party(turn, sliderVal, "Rest", parties[cellY][cellX].getMovementPoints());
          splittedParty.setPathTurns(1+getMoveTurns(cellX, cellY, x, y,map.moveNodes));
          parties[cellY][cellX].changeUnitNumber(-sliderVal);
          if (parties[cellY][cellX].getUnitNumber() <= 0){
            parties[cellY][cellX] = null;
          }
        }
      }
      
      if (splittedParty != null){
        splittedParty.target = new int[]{x, y};
        Node[][] nodes = djk(cellX, cellY);
        splittedParty.path = getPath(cellX, cellY, x, y, nodes);
        Collections.reverse(splittedParty.path);
        splittedParty.changeTask("Rest");
        splittedParty.clearActions();
        ((Text)getElement("turns remaining", "party management")).setText("");
        moveParty(cellX, cellY, true);
      } 
      else {
        parties[cellY][cellX].target = new int[]{x, y};
        Node[][] nodes = djk(cellX, cellY);
        parties[cellY][cellX].path = getPath(cellX, cellY, x, y, nodes);
        Collections.reverse(parties[cellY][cellX].path);
        parties[cellY][cellX].changeTask("Rest");
        parties[cellY][cellX].clearActions();         
        ((Text)getElement("turns remaining", "party management")).setText("");
        moveParty(cellX, cellY);
      }
    }
    else if (event instanceof EndTurn){
      changeTurn();
    }
    
    else if (event instanceof ChangeTask){
      ChangeTask m = (ChangeTask)event;
      int cellX = m.x;
      int cellY = m.y;
      String task = m.task;
        parties[cellY][cellX].clearPath();
        parties[cellY][cellX].target = null;
        if (parties[cellY][cellX].getTask() == "Defend"){
          //Changing from defending
          parties[cellY][cellX].setMovementPoints(min(parties[cellY][cellX].getMovementPoints()+DEFENDCOST, MOVEMENTPOINTS));
        }
        parties[cellY][cellX].changeTask(task);
        if (parties[cellY][cellX].getTask() == "Rest"){
          parties[cellY][cellX].clearActions();         
          ((Text)getElement("turns remaining", "party management")).setText("");
        }
        else{
          moving = false;
          map.cancelMoveNodes();
        }
        if (parties[cellY][cellX].getTask() == "Defend"){
          parties[cellY][cellX].subMovementPoints(DEFENDCOST);
        }
        else if (parties[cellY][cellX].getTask() == "Demolish"){
          parties[cellY][cellX].clearActions();         
          ((Text)getElement("turns remaining", "party management")).setText("");
          parties[cellY][cellX].addAction(new Action("Demolish", 2));
        }
        else if (parties[cellY][cellX].getTask() == "Clear Forest"){
          parties[cellY][cellX].clearActions();         
          ((Text)getElement("turns remaining", "party management")).setText("");
          parties[cellY][cellX].addAction(new Action("Clear Forest", 2));
        }
        else if (parties[cellY][cellX].getTask() == "Build Farm"){
          if (sufficientResources(players[turn].resources, buildingCosts[1])){
            parties[cellY][cellX].clearActions();         
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(new Action("Build Farm", buildingTimes[FARM]));
            spendRes(players[turn], buildingCosts[1]);
            buildings[cellY][cellX] = new Building(CONSTRUCTION);
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
        else if (parties[cellY][cellX].getTask() == "Build Sawmill"){
          if (sufficientResources(players[turn].resources, buildingCosts[5])){
            parties[cellY][cellX].clearActions();         
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(new Action("Build Sawmill", buildingTimes[SAWMILL]));
            spendRes(players[turn], buildingCosts[5]);
            buildings[cellY][cellX] = new Building(CONSTRUCTION);
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
        else if (parties[cellY][cellX].getTask() == "Build Homes"){
          if (sufficientResources(players[turn].resources, buildingCosts[0])){
            parties[cellY][cellX].clearActions();         
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(new Action("Build Homes", buildingTimes[HOMES]));
            spendRes(players[turn], buildingCosts[0]);
            buildings[cellY][cellX] = new Building(CONSTRUCTION);
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
        else if (parties[cellY][cellX].getTask() == "Build Factory"){
          if (sufficientResources(players[turn].resources, buildingCosts[4])){
            parties[cellY][cellX].clearActions();         
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(new Action("Build Factory", buildingTimes[FACTORY]));
            spendRes(players[turn], buildingCosts[4]);
            buildings[cellY][cellX] = new Building(CONSTRUCTION);
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
        else if (parties[cellY][cellX].getTask() == "Build Mine"){
          if (sufficientResources(players[turn].resources, buildingCosts[2])){
            parties[cellY][cellX].clearActions();         
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(new Action("Build Mine", buildingTimes[MINE]));
            spendRes(players[turn], buildingCosts[2]);
            buildings[cellY][cellX] = new Building(CONSTRUCTION);
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
        else if (parties[cellY][cellX].getTask() == "Build Smelter"){
          if (sufficientResources(players[turn].resources, buildingCosts[3])){
            parties[cellY][cellX].clearActions();         
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(new Action("Build Smelter", buildingTimes[SMELTER]));
            spendRes(players[turn], buildingCosts[3]);
            buildings[cellY][cellX] = new Building(CONSTRUCTION);
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
        checkTasks();
      }
    this.totals = totalResources();
    return false;
  }
  void updateCellSelection(){
    cellSelectionX = round((width-400-bezel*2)/GUIScale)+bezel*2;     
    cellSelectionY = bezel*2;
    cellSelectionW = width-cellSelectionX-bezel*2;
    cellSelectionH = round(mapElementHeight);
    getPanel("land management").transform(cellSelectionX, cellSelectionY, cellSelectionW, round(cellSelectionH*0.3));
    getPanel("party management").transform(cellSelectionX, cellSelectionY+round(cellSelectionH*0.3)+bezel, cellSelectionW, round(cellSelectionH*0.7)-bezel*3);
    ((Button)getElement("move button", "party management")).transform(bezel, round(13*TextScale+bezel), 100, 30);
    ((Slider)getElement("split units", "party management")).transform(round(10*GUIScale+bezel), round(bezel*3+2*TextScale*13), cellSelectionW-2*bezel-round(20*GUIScale),round(TextScale*2*13));
    ((DropDown)getElement("tasks", "party management")).transform(bezel, round(bezel*4+4*TextScale*13), cellSelectionW-2*bezel, 30);
    ((Text)getElement("turns remaining", "party management")).translate(100+bezel*2, round(13*TextScale*2 + bezel*3));
  }
       
  void makeTaskAvailable(String task){
    ((DropDown)getElement("tasks", "party management")).makeAvailable(task);     
  }
  void resetAvailableTasks(){
    ((DropDown)getElement("tasks", "party management")).resetAvailable();
  }
  //tasks building
  //settings
  
  void checkTasks(){     
    resetAvailableTasks();     
    if(parties[cellY][cellX].player==2){
      makeTaskAvailable("Battle");
    }
    else {
      makeTaskAvailable("Rest");
      int cellTerrain = terrain[cellY][cellX];
      if(parties[cellY][cellX].hasActions()){
        makeTaskAvailable(parties[cellY][cellX].currentAction());
      }
      if (parties[cellY][cellX].getMovementPoints() >= DEFENDCOST && cellTerrain != WATER)
        makeTaskAvailable("Defend");
      if (buildings[cellY][cellX] != null){
        if (buildings[cellY][cellX].type==FARM){
          makeTaskAvailable("Work Farm");
        }
        if (buildings[cellY][cellX].type==HOMES){
          makeTaskAvailable("Super Rest");
        }
        if (buildings[cellY][cellX].type==MINE){
          makeTaskAvailable("Produce Ore");
        }
        if (buildings[cellY][cellX].type==SMELTER){
          makeTaskAvailable("Produce Metal");
        }
        if (buildings[cellY][cellX].type==FACTORY){
          makeTaskAvailable("Produce Concrete");
          makeTaskAvailable("Produce Cable");
        }  
        if (buildings[cellY][cellX].type==SAWMILL){
          makeTaskAvailable("Produce Wood");
        }
        if (buildings[cellY][cellX].type==BIG_FACTORY){   
          makeTaskAvailable("Produce Spaceship Parts");
        }
        makeTaskAvailable("Demolish");
      }
      else{
        if (cellTerrain == GRASS){
          makeTaskAvailable("Build Farm");  
        }
        else if (cellTerrain == FOREST){
          makeTaskAvailable("Clear Forest");
          if (parties[cellY][cellX].getTask() != "Clear Forest")
            makeTaskAvailable("Build Sawmill");
        }
        if (cellTerrain != WATER && cellTerrain != FOREST){ 
          makeTaskAvailable("Build Homes");
        }  
        if (cellTerrain != WATER && cellTerrain != FOREST && cellTerrain != HILLS){
          makeTaskAvailable("Build Factory");
          makeTaskAvailable("Build Smelter");
        }
        if (cellTerrain == HILLS){
          makeTaskAvailable("Build Mine");
        }
      }  
    }  
    ((DropDown)getElement("tasks", "party management")).select(parties[cellY][cellX].getTask());
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
    int tx = round(between(0, mouseX-tw/2, width-tw));
    int ty = round(between(0, mouseY+20, height-th-20));
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
    float[] totalResourceRequirements = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (map.parties[y][x] != null){
          if (map.parties[y][x].player == turn){
            for (int i=0; i<tasks.length;i++){
              if(map.parties[y][x].getTask()==tasks[i]){
                for(int resource = 0; resource < NUMRESOURCES; resource++){
                  totalResourceRequirements[resource]+=taskCosts[i][resource]*map.parties[y][x].getUnitNumber();
                }
              }
            }
            String action = map.parties[y][x].progressAction();
            switch (action){
              //-1 building types
              case "Clear Forest":
                map.terrain[y][x] = GRASS;
                players[turn].resources[WOOD]+=100;
                break;
              case "Build Farm":
                map.buildings[y][x] = new Building(FARM);
                break;
              case "Build Sawmill":
                map.buildings[y][x] = new Building(SAWMILL);
                break;
              case "Build Homes":
                map.buildings[y][x] = new Building(HOMES);
                break;
              case "Build Factory":
                map.buildings[y][x] = new Building(FACTORY);
                break;
              case "Build Mine":
                map.buildings[y][x] = new Building(MINE);
                break;
              case "Build Smelter":
                map.buildings[y][x] = new Building(SMELTER);
                break;
              case "Demolish":
                reclaimRes(players[turn], buildingCosts[5]);
                map.buildings[y][x] = null;
                break;
              case "Construction Mid":
                map.buildings[y][x].image_id = 1;
                action = "";
                break;
              case "Construction End":
                map.buildings[y][x].image_id = 2;
                action = "";
                break;
            }
            if (action != ""){
              map.parties[y][x].clearCurrentAction();
              map.parties[y][x].changeTask("Rest");
            }
            moveParty(x, y);
          } 
          else {
            if(map.parties[y][x].player==2){
              map.parties[y][x] = ((Battle)map.parties[y][x]).doBattle();
            }
          }
        }
      }
    }
    float [] resourceAmountsAvailable = new float[9];
    for(int i=0; i<NUMRESOURCES;i++){
      if(totalResourceRequirements[i]==0){
        resourceAmountsAvailable[i] = 1;
      } else{
       resourceAmountsAvailable[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);
      }
    }
    
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (map.parties[y][x] != null){
          if (map.parties[y][x].player == turn){
            float productivity = 1;
            for (int task=0; task<tasks.length;task++){
              if(map.parties[y][x].getTask()==tasks[task]){
                for(int resource = 0; resource < NUMRESOURCES; resource++){
                  if(taskCosts[task][resource]>0){
                    if(resource==0){
                      productivity = min(productivity, resourceAmountsAvailable[resource]+0.5);
                    } else {
                      productivity = min(productivity, resourceAmountsAvailable[resource]);
                    }
                  }
                }
              }
            }
            for (int task=0; task<tasks.length;task++){
              if(map.parties[y][x].getTask()==tasks[task]){
                for(int resource = 0; resource < NUMRESOURCES; resource++){
                  if(resource<NUMRESOURCES-1){
                    players[turn].resources[resource] += max((taskOutcomes[task][resource]-taskCosts[task][resource])*productivity*map.parties[y][x].getUnitNumber(), -players[turn].resources[resource]);
                  } else if(resourceAmountsAvailable[0]<1){
                    map.parties[y][x].setUnitNumber(floor(map.parties[y][x].getUnitNumber()-(1-resourceAmountsAvailable[0])*taskOutcomes[task][resource]*map.parties[y][x].getUnitNumber()));
                  } else{
                    map.parties[y][x].setUnitNumber(ceil(map.parties[y][x].getUnitNumber()+taskOutcomes[task][resource]*(float)map.parties[y][x].getUnitNumber()));
                  }
                  
                }
              }
            }
            if(map.parties[y][x].getUnitNumber()==0){
              map.parties[y][x] = null;
            }
          }
        }
      }
    }
    partyMovementPointsReset();
    float mapXOffset;
    float mapYOffset;
    if (map.panning){
      mapXOffset = map.targetXOffset;
      mapYOffset = map.targetYOffset;
    } else {
      mapXOffset = map.mapXOffset;
      mapYOffset = map.mapYOffset;
    }
    float blockSize;
    if (map.zooming){
      blockSize = map.targetBlockSize;
    } else{
      blockSize = map.blockSize;
    }
    players[turn].saveSettings(mapXOffset, mapYOffset, blockSize, cellX, cellY, cellSelected);
    turn = (turn + 1)%2;
    players[turn].loadSettings(this, map);
    changeTurn = false;
    this.totals = totalResources();
    if (turn == 0)
      turnNumber ++;
  }
  
  boolean checkForPlayerWin(){
    boolean player1alive = false;
    boolean player2alive = false;
    
    for (int y=0;y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (parties[y][x] != null){
          if(parties[y][x].player == 2){
            player1alive = true;
            player2alive = true;
          } else if (parties[y][x].player == 1){
            player2alive = true;
          } else if (parties[y][x].player == 0){
            player1alive = true;
          }
        }
      }
    }
    if (!player1alive){
      winner = 1;
    } else if (!player2alive) {
      winner = 0;
    } else {
      return false;
    }
    Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
    winnerMessage.setText(winnerMessage.text.replace("/w", str(winner+1)));
    return true;
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
    if (checkForPlayerWin()){
      this.getPanel("end screen").visible = true;
    }
    return getNewState();
  }
  void partyMovementPointsReset(){
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (map.parties[y][x] != null){
          if (map.parties[y][x].player != 2){
            map.parties[y][x].setMovementPoints(MOVEMENTPOINTS);
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
  boolean canMove(int x, int y){
    float points = map.parties[y][x].getMovementPoints();
    int[][] mvs = {{1,0}, {0,1}, {1,1}, {-1,0}, {0,-1}, {-1,-1}, {1,-1}, {-1,1}};
    for (int[] n : mvs){
      if (points >= cost(x+n[0], y+n[1], x, y)){
        return true;
      }
    }
    return false;
  }
  
  boolean inPrevIdle(int x, int y){
    for (int i=0; i<prevIdle.size();i++){
      if (prevIdle.get(i)[0] == x && prevIdle.get(i)[1] == y){
        return true;
      }
    }
    return false;
  }
  void clearPrevIdle(){
    prevIdle.clear();
  }
  
  int[] findIdle(int player){
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (parties[y][x] != null && parties[y][x].player == player && (parties[y][x].task == "Rest" && !inPrevIdle(x, y))){
          prevIdle.add(new Integer[]{x, y});
          return new int[]{x, y};
        }
      }
    }
    clearPrevIdle();
    return new int[]{cellX, cellY};
  }
  
  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type == "clicked"){
        if (event.id == "idle party finder"){
          int[] t = findIdle(turn);
          selectCell(floor(map.scaleX(t[0]+1)), floor(map.scaleY(t[1]+1)));
          map.targetCell(t[0], t[1], 64);
        }
        else if (event.id == "end turn"){
          postEvent(new EndTurn());
        }
        else if (event.id == "move button"){
          if (parties[cellY][cellX].player == turn){
            moving = !moving;
            if (moving){
              map.updateMoveNodes(djk(cellX, cellY));
            }
            else{
              map.cancelMoveNodes();
            }
          }
        }
        else if (event.id == "end game button"){
          newState = "menu";
        }
      }
      if (event.type == "valueChanged"){
        if (event.id == "tasks"){
          postEvent(new ChangeTask(cellX, cellY, ((DropDown)getElement("tasks", "party management")).getSelected()));
        }
      }
    }
  }
  void deselectCell(){
    toolTipSelected = -1;
    cellSelected = false;
    map.unselectCell();
    getPanel("land management").setVisible(false);
    getPanel("party management").setVisible(false);
    map.cancelMoveNodes();
    moving = false;
    //map.setWidth(round(width-bezel*2));
    ((Text)getElement("turns remaining", "party management")).setText("");
  }
  
  void moveParty(int px, int py){
    moveParty(px, py, false);
  }
  
  void moveParty(int px, int py, boolean splitting){
    
    Party p;
    
    if (splitting){
      p = splittedParty;
    } else {
      p = map.parties[py][px];
    }
    
    
    boolean cellFollow = (px==cellX && py==cellY);
    boolean stillThere = true;
    if (p.target == null || p.path == null)
      return;
    int tx = p.target[0];
    int ty = p.target[1];
    if (px == tx && py == ty){
      if (splitting){
        if(parties[py][px] == null){
          parties[py][px] = p;
        } else {
          parties[py][px].changeUnitNumber(p.getUnitNumber());
        }
      }
      p.path = null;
      return;
    }
    
    ArrayList <int[]> path = p.path;
    int i=0; //<>//
    boolean moved = false;
    for (int node=1; node<path.size(); node++){
      int cost = cost(path.get(node)[0], path.get(node)[1], px, py);
      if (p.getMovementPoints() >= cost){
        if (map.parties[path.get(node)[1]][path.get(node)[0]] == null){
          // empty cell
          p.subMovementPoints(cost);
          map.parties[path.get(node)[1]][path.get(node)[0]] = p;
          if (splitting){
            splittedParty = null;
            splitting = false;
          } else{
            map.parties[py][px] = null;
          }
          px = path.get(node)[0];
          py = path.get(node)[1];
          p = map.parties[py][px];
          if (!moved){
            p.moved();
            moved = true;
          }
        }
        else if(path.get(node)[0] != px || path.get(node)[1] != py){
          p.clearPath();
          if (map.parties[path.get(node)[1]][path.get(node)[0]].player == turn){
            // merge cells
            int movementPoints = min(map.parties[path.get(node)[1]][path.get(node)[0]].getMovementPoints(), p.getMovementPoints()-cost);
            int overflow = map.parties[path.get(node)[1]][path.get(node)[0]].changeUnitNumber(p.getUnitNumber());
            if(cellFollow){
              selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
              stillThere = false;
            }
            if (overflow>0){
              p.setUnitNumber(overflow);
            } else {
              if (splitting){
                splittedParty = null;
                splitting = false;
              } else{
                map.parties[py][px] = null;
              }
            }
            map.parties[path.get(node)[1]][path.get(node)[0]].setMovementPoints(movementPoints);
          } else if (map.parties[path.get(node)[1]][path.get(node)[0]].player == 2){
            // merge cells battle
            int overflow = ((Battle) map.parties[path.get(node)[1]][path.get(node)[0]]).changeUnitNumber(turn, p.getUnitNumber());
            if(cellFollow){
              selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
              stillThere = false;
            }
            if (overflow>0){
              p.setUnitNumber(overflow);
            } else {
              if (splitting){
                splittedParty = null;
                splitting = false;
              } else{

                map.parties[py][px] = null;
              }
            }
          }
          else{
            print(1);
            p.subMovementPoints(cost);
            map.parties[path.get(node)[1]][path.get(node)[0]] = new Battle(p, map.parties[path.get(node)[1]][path.get(node)[0]]);
            map.parties[path.get(node)[1]][path.get(node)[0]] = ((Battle)map.parties[path.get(node)[1]][path.get(node)[0]]).doBattle();
            if(cellFollow){
              selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
              stillThere = false;
            }
            if (splitting){
                splittedParty = null;
                splitting = false;
              } else{
                map.parties[py][px] = null;
              }
            if(map.buildings[path.get(node)[1]][path.get(node)[0]]!=null&&map.buildings[path.get(node)[1]][path.get(node)[0]].type==0){
              map.buildings[path.get(node)[1]][path.get(node)[0]] = null;
            }
          }
          break;
        }
        i++;
      }
      else{
        p.path = new ArrayList(path.subList(i, path.size()));
        break;
      }
      if (tx==px&&ty==py){
        p.path = null;
      }
    }
    
    if(cellFollow&&stillThere){
      selectCell((int)px, (int)py, false);
    }
  }
  int getMoveTurns(int startX, int startY, int targetX, int targetY, Node[][] nodes){
    int movementPoints = round(parties[startY][startX].getMovementPoints());
    int turns = 0;
    ArrayList <int[]> path = getPath(startX, startY, targetX, targetY, nodes);
    Collections.reverse(path);
    for (int node=1; node<path.size(); node++){
      int cost = cost(path.get(node)[0], path.get(node)[1], path.get(node-1)[0], path.get(node-1)[1]);
      if (movementPoints < cost){
        turns += 1;
        movementPoints = MOVEMENTPOINTS;
      }
      movementPoints -= cost;
    }
    return turns;
  }
  ArrayList<String> mouseEvent(String eventType, int button){
    if (button == RIGHT){
      if (eventType == "mouseClicked"){
        deselectCell();
      }
    }
    if (button == LEFT){
      if (eventType == "mouseClicked"){
        if (activePanel == "default" && ((!getPanel("party management").mouseOver() || !getPanel("party management").visible) && (!getPanel("land management").mouseOver() || !getPanel("land management").visible))){
          if (map.mouseOver()){
            if (moving){
              int x = floor(map.scaleXInv(mouseX));
              int y = floor(map.scaleYInv(mouseY));
              postEvent(new Move(cellX, cellY, x, y));
              map.cancelPath();
            }
            else{
              if(floor(map.scaleXInv(mouseX))==cellX&&floor(map.scaleYInv(mouseY))==cellY&&cellSelected){
                deselectCell();
              } else {
                selectCell(mouseX, mouseY);
              }
            }
          }
        }
      }
    }
    if (eventType == "mouseMoved"){
      if (((DropDown)getElement("tasks", "party management")).moveOver() && getPanel("party management").visible){
        switch (((DropDown)getElement("tasks", "party management")).findMouseOver()){
          case "Defend":toolTipSelected = 0;break;
          case "Build Farm":
            tooltipText[1] = String.format(farmToolTipRaw,parties[cellY][cellX].calcTurns(buildingTimes[FARM]));
            toolTipSelected = 1;
            break;
          case "Build Sawmill":
            tooltipText[6] = String.format(sawmillToolTipRaw,parties[cellY][cellX].calcTurns(buildingTimes[SAWMILL]));
            toolTipSelected = 6;
            break;
          case "Build Homes":
            tooltipText[3] = String.format(homesToolTipRaw,parties[cellY][cellX].calcTurns(buildingTimes[HOMES]));
            toolTipSelected = 3;
            break;
          case "Build Factory":
            tooltipText[5] = String.format(factoryToolTipRaw,parties[cellY][cellX].calcTurns(buildingTimes[FACTORY]));
            toolTipSelected = 5;
            break;
          case "Clear Forest":
            tooltipText[7] = String.format(clearForestToolTipRaw,parties[cellY][cellX].calcTurns(2));
            toolTipSelected = 7;
            break;
          case "Build Mine":
            tooltipText[2] = String.format(mineToolTipRaw,parties[cellY][cellX].calcTurns(buildingTimes[MINE]));
            toolTipSelected = 2;
            break;
          case "Build Smelter":
            tooltipText[4] = String.format(smelterToolTipRaw,parties[cellY][cellX].calcTurns(buildingTimes[SMELTER]));
            toolTipSelected = 4;
            break;
          case "Demolish":
            tooltipText[8] = String.format(demolishingToolTipRaw,parties[cellY][cellX].calcTurns(2));
            toolTipSelected = 8;
            break;
          case "Rest":toolTipSelected = 9;break;
          case "Super Rest":toolTipSelected = 13;break;
          default: toolTipSelected = -1;break;
        }
      }
      else if(((Text)getElement("turns remaining", "party management")).mouseOver()&& getPanel("party management").visible){
        toolTipSelected = 14;
      }
      else if(((Button)getElement("move button", "party management")).mouseOver()&& getPanel("party management").visible){
        toolTipSelected = 15;
      }
      else if (moving&&map.mouseOver() && ((!getPanel("party management").mouseOver() || !getPanel("party management").visible) && (!getPanel("land management").mouseOver() || !getPanel("land management").visible))){
          Node [][] nodes = map.moveNodes;
          int x = floor(map.scaleXInv(mouseX));
          int y = floor(map.scaleYInv(mouseY));
          if (x < mapWidth && y<mapHeight && x>=0 && y>=0 && nodes[y][x] != null && !(cellX == x && cellY == y)){
            if (parties[cellY][cellX] != null){
              map.updatePath(getPath(cellX, cellY, x, y, map.moveNodes));
            }
            if(map.parties[y][x]==null){
              //Moving into empty tile
              if (nodes[y][x].cost>MOVEMENTPOINTS)
                tooltipText[12] = turnsToolTipRaw.replace("/i", str(getMoveTurns(cellX, cellY, x, y, nodes)));
              else
                tooltipText[12] = "Move";
                toolTipSelected = 12;
            }
            else {
              if (map.parties[y][x].player == turn){
                //merge parties
                toolTipSelected = 10;
              }
              else {
                //Attack
                int chance = getChanceOfBattleSuccess(map.parties[cellY][cellX], map.parties[y][x]);
                tooltipText[11] = attackToolTipRaw.replace("/p", str(chance));
                toolTipSelected = 11;
              }
            } 
          }
          else{
            map.cancelPath();
            toolTipSelected = -1;
          }
        } 
      else{
        map.cancelPath();
        toolTipSelected = -1;
      }
    }
    return new ArrayList<String>();
  }
  void selectCell(int x, int y){
    x = floor(map.scaleXInv(x));
    y = floor(map.scaleYInv(y));
    selectCell(x, y, false);
  }
  
  boolean cellInBounds(int x, int y){
    return 0<=x&&x<mapWidth&&0<=y&&y<=mapHeight;
  }
  
  void selectCell(int x, int y, boolean raw){
    deselectCell();
    if(raw){
      selectCell(x, y);
    } else if (cellInBounds(x, y)){
      toolTipSelected = -1;
      cellX = x;
      cellY = y;
      cellSelected = true;
      map.selectCell(cellX, cellY);
      //map.setWidth(round(width-bezel*2-400));
      getPanel("land management").setVisible(true);
      if (parties[cellY][cellX] != null && parties[cellY][cellX].isTurn(turn)){
        if (parties[cellY][cellX].getTask() != "Battle"){
          ((Slider)getElement("split units", "party management")).show();
        }
        else{
          ((Slider)getElement("split units", "party management")).hide();
        }
        getPanel("party management").setVisible(true); 
        if (parties[cellY][cellX].getUnitNumber() <= 1){
          ((Slider)getElement("split units", "party management")).hide();
        } else {
          ((Slider)getElement("split units", "party management")).setScale(1, parties[cellY][cellX].getUnitNumber(), parties[cellY][cellX].getUnitNumber(), 1, parties[cellY][cellX].getUnitNumber()/2);
        }
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
  }
  float[] resourceProduction(int x, int y){
    float[] totalResourceRequirements = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    float [] resourceAmountsAvailable = new float[9];
    float [] production = new float[9]; 
    for(int i=0; i<NUMRESOURCES;i++){
      if(totalResourceRequirements[i]==0){
        resourceAmountsAvailable[i] = 1;
      } else{
       resourceAmountsAvailable[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);
      } 
    }
    if (map.parties[y][x] != null){
      if (map.parties[y][x].player == turn){
        float productivity = 1;
        for (int task=0; task<tasks.length;task++){
          if(map.parties[y][x].getTask()==tasks[task]){ 
            for(int resource = 0; resource < NUMRESOURCES; resource++){
              if(taskCosts[task][resource]>0){ 
                productivity = min(productivity, resourceAmountsAvailable[resource]);
              }
            }
          }
        }
        for (int task=0; task<tasks.length;task++){
          if(map.parties[y][x].getTask()==tasks[task]){ 
            for(int resource = 0; resource < NUMRESOURCES; resource++){  
              if(resource<NUMRESOURCES-1){
                production[resource] = (taskOutcomes[task][resource])*productivity*map.parties[y][x].getUnitNumber();
              }
            }
          }
        }
      }
    }
    return production; 
  }
  float[] resourceConsumption(int x, int y){ 
    float[] totalResourceRequirements = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    float [] resourceAmountsAvailable = new float[9];
    float [] production = new float[9];
    for(int i=0; i<NUMRESOURCES;i++){
      if(totalResourceRequirements[i]==0){
        resourceAmountsAvailable[i] = 1;
      } else{ 
       resourceAmountsAvailable[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);  
      }
    }
    if (map.parties[y][x] != null){
      if (map.parties[y][x].player == turn){
        float productivity = 1;
        for (int task=0; task<tasks.length;task++){
          if(map.parties[y][x].getTask()==tasks[task]){
            for(int resource = 0; resource < NUMRESOURCES; resource++){
              if(taskCosts[task][resource]>0){
                productivity = min(productivity, resourceAmountsAvailable[resource]);
              }
            }
          }
        }
        for (int task=0; task<tasks.length;task++){
          if(map.parties[y][x].getTask()==tasks[task]){
            for(int resource = 0; resource < NUMRESOURCES; resource++){
              if(resource<NUMRESOURCES-1){
                production[resource] = (taskCosts[task][resource])*productivity*map.parties[y][x].getUnitNumber();
              }
            }
          }
        }
      }
    }
    return production;
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
    text("Movement Points Remaining: "+parties[cellY][cellX].getMovementPoints(turn) + "/"+MOVEMENTPOINTS, 120+cellSelectionX, barY);
    barY += 13*TextScale;
    text("Units: "+parties[cellY][cellX].getUnitNumber(turn) + "/1000", 120+cellSelectionX, barY);
    barY += 13*TextScale;
    if (parties[cellY][cellX].pathTurns > 0){
      ((Text)getElement("turns remaining", "party management")).setText("Turns Remaining: "+ parties[cellY][cellX].pathTurns);
    }
    else if (parties[cellY][cellX].actions.size() > 0 && parties[cellY][cellX].actions.get(0).initialTurns > 0){
      ((Text)getElement("turns remaining", "party management")).setText("Turns Remaining: "+parties[cellY][cellX].turnsLeft() + "/"+round(parties[cellY][cellX].calcTurns(parties[cellY][cellX].actions.get(0).initialTurns)));
    }
  }
  
  String resourcesList(float[] resources){
    String returnString = "";
    boolean notNothing = false;
    for (int i=0; i<NUMRESOURCES;i++){
      if (resources[i]>0){
        returnString += getResourceString(resources[i])+ " " +resourceNames[i]+ ", "; 
        notNothing = true;
      }
    }
    if (!notNothing)
      returnString += "Nothing/Unknown";
    return returnString;
  }
  
  float[] totalResources(){
    float[] amount={0,0,0,0,0,0,0,0,0};
    for (int x=0; x<mapWidth; x++){
      for (int y=0; y<mapHeight; y++){
        for (int res=0; res<9; res++){
          amount[res]+=resourceProduction(x, y)[res];
          amount[res]-=resourceConsumption(x, y)[res];
        }
      }
    }
    return amount;
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
      if (buildings[cellY][cellX].type != 0)
        text("Building: "+buildingTypes[buildings[cellY][cellX].type-1], 5+cellSelectionX, barY);
      else
        text("Building: Construction Site", 5+cellSelectionX, barY);
      barY += 13*TextScale;
    }
    float[] production = resourceProduction(cellX, cellY);
    float[] consumption = resourceConsumption(cellX, cellY);
    text("Producing: "+resourcesList(production), 5+cellSelectionX, barY);
    barY += 13*TextScale;
    fill(255,0,0);
    text("Consuming: "+resourcesList(consumption), 5+cellSelectionX, barY);
    barY += 13*TextScale;
  }
  
  String getResourceString(float amount){
    String tempString = (new BigDecimal(""+amount).divide(new BigDecimal("1"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString();
    if (amount >= 0){
      fill(0);
      tempString = "+"+tempString;
    }
    else{
      fill(255, 0, 0);
    }
    return tempString;
  }
  
  void drawBar(){
    float barX=buttonW*2+bezel*3;
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
    fill(255);
    textAlign(CENTER, TOP);
    text(turnString, barX+(textWidth(turnString)+10)/2, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    barX += textWidth(turnString)+10+bezel;
    
    turnString = "Turn: "+turnNumber;
    fill(150);
    rect(barX, height-bezel-buttonH, textWidth(turnString)+10, buttonH);
    fill(0);
    textAlign(LEFT, TOP);
    text(turnString, barX+5, height-bezel-(textDescent()+textAscent())/2-buttonH/2);
    barX += textWidth(turnString)+10+bezel;
    
    textAlign(CENTER, TOP);
    String tempString;
    barX=width;
    tempString = "energy:"+round(players[turn].resources[3]);
    textSize(12*TextScale);
    barX -= textWidth(tempString)+10+bezel;
    if (players[turn].resources[3]>0)
      fill(150);
    else
      fill(255,0,0);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())-buttonH/2);
    fill(0);
    textSize(8*TextScale);
    tempString=getResourceString(totals[3]);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-buttonH/2);
    
    tempString = "metal:"+round(players[turn].resources[2]);
    textSize(12*TextScale);
    barX -= textWidth(tempString)+10+bezel;
    if (players[turn].resources[2]>0)
      fill(150);
    else
      fill(255,0,0);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())-buttonH/2);
    textSize(8*TextScale);
    tempString=getResourceString(totals[2]);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-buttonH/2);
    
    tempString = "wood:"+round(players[turn].resources[1]);
    textSize(12*TextScale);
    barX -= textWidth(tempString)+10+bezel;
    if (players[turn].resources[1]>0)
      fill(150);
    else
      fill(255,0,0);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())-buttonH/2);
    textSize(8*TextScale);
    tempString=getResourceString(totals[1]);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-buttonH/2);
     
    tempString = "food:"+round(players[turn].resources[0]);
    textSize(12*TextScale);
    barX -= textWidth(tempString)+10+bezel;
    if (players[turn].resources[0]>0)
      fill(150);
    else
      fill(255,0,0);
    rect(barX, height-bezel-buttonH, textWidth(tempString)+10, buttonH);
    fill(255);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-(textDescent()+textAscent())-buttonH/2);
    textSize(8*TextScale);
    tempString=getResourceString(totals[0]);
    text(tempString, barX+(textWidth(tempString)+10)/2, height-bezel-buttonH/2);
  }
  
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (_key == ESC){
      newState = "menu";
    }
    if (eventType == "keyTyped"){
      if (key == ' '){
        postEvent(new EndTurn());
      }
    }
    return new ArrayList<String>();
  }
    
  void enterState(){
    mapWidth = mapSize;
    mapHeight = mapSize;
    updateCellSelection();
    
    clearPrevIdle();
    ((Text)getElement("turns remaining", "party management")).setText("");
    ((Panel)getPanel("end screen")).visible = false;
    Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
    winnerMessage.setText("Winner: player /w");
    
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
    turnNumber = 0;
    toolTipSelected=-1;
    this.totals = totalResources();
  }
  int cost(int x, int y, int prevX, int prevY){
    float mult = 1;
    if (x!=prevX && y!=prevY){
      mult = 1.41;
    }
    if (0<x && x<mapSize && 0<y && y<mapSize){
      return round(float(terrainCosts[terrain[y][x]-1])*mult);
    }
    //Not a valid location
    return -1;
  }
  
  ArrayList<int[]> getPath(int startX, int startY, int targetX, int targetY, Node[][] nodes){
    ArrayList<int[]> returnNodes = new ArrayList<int[]>();
    returnNodes.add(new int[]{targetX, targetY});
    int[] curNode = {targetX, targetY};
    while (curNode[0] != startX || curNode[1] != startY){
      returnNodes.add(new int[]{nodes[curNode[1]][curNode[0]].prevX, nodes[curNode[1]][curNode[0]].prevY});
      curNode = returnNodes.get(returnNodes.size()-1);
    }
    return returnNodes;
  }
  Node[][] djk(int x, int y){
    int[][] mvs = {{1,0}, {0,1}, {1,1}, {-1,0}, {0,-1}, {-1,-1}, {1,-1}, {-1,1}};
    int xOff = 0;
    int yOff = 0;
    int w = mapWidth;
    int h = mapHeight;
    Node[][] nodes = new Node[h][w];
    int cx = x-xOff;
    int cy = y-yOff;
    nodes[cy][cx] = new Node(0, false, cx, cy);
    ArrayList<Integer> curMinCosts = new ArrayList<Integer>();
    ArrayList<int[]> curMinNodes = new ArrayList<int[]>();
    curMinNodes.add(new int[]{cx, cy});
    curMinCosts.add(0);
    while (curMinNodes.size() > 0){
      nodes[curMinNodes.get(0)[1]][curMinNodes.get(0)[0]].fixed = true;
      for (int[] mv : mvs){
        int nx = curMinNodes.get(0)[0]+mv[0];
        int ny = curMinNodes.get(0)[1]+mv[1];
        if (0 < nx && nx < w && 0 < ny && ny < h){
          boolean sticky = map.parties[ny][nx] != null;
          int newCost = cost(nx, ny, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
          int prevCost = curMinCosts.get(0);
          int totalNewCost = prevCost+newCost;
          if (totalNewCost < MOVEMENTPOINTS*100){
            if (nodes[ny][nx] == null){
              nodes[ny][nx] = new Node(totalNewCost, false, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
              if (!sticky){
                curMinNodes.add(search(curMinCosts, totalNewCost), new int[]{nx, ny});
                curMinCosts.add(search(curMinCosts, totalNewCost), totalNewCost);
              }
            }
            else if (!nodes[ny][nx].fixed){
              if (totalNewCost < nodes[ny][nx].cost){
                nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
                nodes[ny][nx].setPrev(curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
                if (!sticky){
                  curMinNodes.remove(search(curMinNodes, nx, ny));
                  curMinNodes.add(search(curMinCosts, totalNewCost), new int[]{nx, ny});
                  curMinCosts.remove(search(curMinNodes, nx, ny));
                  curMinCosts.add(search(curMinCosts, totalNewCost), totalNewCost);
                }
              }
            }
          }
        }
      }
      curMinNodes.remove(0);
      curMinCosts.remove(0);
    }
    return nodes;
  }
  int search(ArrayList<int[]> nodes, int x, int y){
    for (int i=0; i < nodes.size(); i++){
      if (nodes.get(i)[0] == x && nodes.get(i)[1] == y){
        return i;
      }
    }
    return -1;
  }
  int search(ArrayList<Integer> costs, float target){
    //int upper = nodes.size();
    //int lower = 0;
    //while(nodes.get(lower)[2] > target || target > nodes.get(upper)[2]){
      
    //}
    //return lower;
    
    //linear search for now
    for (int i=0; i < costs.size(); i++){
      if (costs.get(i) > target){
        return i;
      }
    }
    return costs.size();
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
    parties[(int)player2.y][(int)player2.x] = new Party(1, 100, "Rest", MOVEMENTPOINTS);
    
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
  
  int getRandomGroundType(HashMap<Integer, Float> groundWeightings, float total){
    float randomNum = random(0, 1);
    float min = 0;
    int lastType = 1;
    for (int type: groundWeightings.keySet()){
      if(randomNum>min&&randomNum<min+groundWeightings.get(type)/total){
        return type;
      }
      min += groundWeightings.get(type)/total;
      lastType = type;
    }
    return lastType;
  }
  
  
  int[][] generateMap(PVector[] playerStarts){
    HashMap<Integer, Float> groundWeightings = new HashMap(5);
    groundWeightings.put(WATER, 1.0);
    groundWeightings.put(SAND, 1.0);
    groundWeightings.put(GRASS, 3.0);
    groundWeightings.put(FOREST, 1.0);
    groundWeightings.put(HILLS, 0.0);
    
    float totalWeighting = 0;
    for (float weight: groundWeightings.values()){
      totalWeighting+=weight;
    }
    
    int [][] terrain = new int[mapHeight][mapWidth];
    
    for(int y=0; y<mapHeight; y++){
      terrain[y][0] = WATER;
      terrain[y][mapWidth-1] = WATER;
    }
    for(int x=1; x<mapWidth-1; x++){
      terrain[0][x] = WATER;
      terrain[mapHeight-1][x] = WATER;
    }
    for(int i=0;i<groundSpawns;i++){
      int type = getRandomGroundType(groundWeightings, totalWeighting);
      int x = (int)random(mapWidth-2)+1;
      int y = (int)random(mapHeight-2)+1;
      terrain[y][x] = type;
      // Water will be type 1
      if (type==WATER){
        for (int y1=y-waterLevel+1;y1<y+waterLevel;y1++){
         for (int x1 = x-waterLevel+1; x1<x+waterLevel;x1++){
           if (y1 < mapHeight && y1 >= 0 && x1 < mapWidth && x1 >= 0)
             terrain[y1][x1] = type;
         }
        }
      }
    }
    for (PVector playerStart: playerStarts){
      int type = (int)random(NUMOFGROUNDTYPES-2)+2;
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
    for (int y=0; y<mapHeight; y++){
      for(int x=0; x<mapWidth; x++){
        if(terrain[y][x] == GRASS && random(0,1) > 0.75){
          terrain[y][x] = HILLS;
        }
      }      
    }
    terrain = smoothMap(initialSmooth, 2, terrain);
    terrain = smoothMap(completeSmooth, 1, terrain);
    for (int y=0; y<mapHeight; y++){
      for(int x=0; x<mapWidth; x++){
        if(terrain[y][x] == GRASS && random(0,1) > 0.9){
          terrain[y][x] = HILLS;
        }
      }      
    }
    return terrain;
  }
}
