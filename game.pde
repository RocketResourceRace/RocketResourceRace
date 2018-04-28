
int terrainIndex(String terrain){
  int k = JSONIndex(gameData.getJSONArray("terrain"), terrain);
  if (k>=0){
    return k+1;
  }
  println("Invalid terrain type, "+terrain);
  return -1;
}
int buildingIndex(String building){
  int k = JSONIndex(gameData.getJSONArray("buildings"), building);
  if (k>=0){
    return k+1;
  }
  println("Invalid building type, "+building);
  return -1;
}


class Game extends State{
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezel = 10;
  final int mapElementWidth = round(width);
  final int mapElementHeight = round(height-bezel*2-buttonH);
  String[] tasks;
  String[] buildingTypes;
  float[][] taskCosts;
  float[][] taskOutcomes;
  int numResources;
  String[] resourceNames;
  float [] startingResources;
  int turnNumber;
  int mapHeight = mapSize;
  int mapWidth = mapSize;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  NotificationManager notificationManager;
  Tooltip tooltip;
  int turn;
  boolean changeTurn = false;
  int winner = -1;
  Map3D map3d;
  Map2D map2d;
  Map map;
  Player[] players;
  int cellX, cellY, cellSelectionX, cellSelectionY, cellSelectionW, cellSelectionH;
  boolean cellSelected=false, moving=false;
  color partyManagementColour;
  ArrayList<Integer[]> prevIdle;
  float[] totals;
  Party splittedParty;

  Game(){
    initialiseResources();
    initialiseTasks();
    initialiseBuildings();

    addElement("2dmap", new Map2D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    addElement("3dmap", new Map3D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    addElement("4notification manager", new NotificationManager(0, 0, 0, 0, color(100), color(255), 10, turn));

    //map = (Map3D)getElement("2map", "default");
    notificationManager = (NotificationManager)getElement("4notification manager", "default");
    players = new Player[2];
    totals = new float[resourceNames.length];

    // Initial positions will be focused on starting party
    //players[0] = new Player(map.focusedX, map.focusedY, map.zoom, startingResources, color(0,0,255));
    //players[1] = new Player(map.focusedX, map.focusedY, map.zoom, startingResources, color(255,0,0));
    addPanel("land management", 0, 0, width, height, false, true, color(50, 200, 50), color(0));
    addPanel("party management", 0, 0, width, height, false, true, color(70, 70, 220), color(0));
    addPanel("bottom bar", 0, height-70, width, 70, true, true, color(150), color(50));
    addPanel("end screen", 0, 0, width, height, false, true, color(50, 50, 50, 50), color(0));
    addPanel("pause screen", 0, 0, width, height, false, true, color(50, 50, 50, 50), color(0));
    addPanel("overlay", 0, 0, width, height, true, false, color(255,255), color(255, 255));

    addElement("0tooltip", new Tooltip(), "overlay");
    tooltip = (Tooltip)getElement("0tooltip", "overlay");

    addElement("end game button", new Button((int)(width/2-GUIScale*width/16), (int)(height/2+height/8), (int)(GUIScale*width/8), (int)(GUIScale*height/16), color(70, 70, 220), color(50, 50, 200), color(255), (int)(TextScale*10), CENTER, "End Game"), "end screen");
    addElement("winner", new Text(width/2, height/2, (int)(TextScale*10), "", color(255), CENTER), "end screen");

    addElement("main menu button", new Button((int)(width/2-GUIScale*width/16), (int)(height/2-height/24), (int)(GUIScale*width/6), (int)(GUIScale*height/16), color(70, 70, 220), color(50, 50, 200), color(255), (int)(TextScale*10), CENTER, "Exit to Main Menu"), "pause screen");
    addElement("desktop button", new Button((int)(width/2-GUIScale*width/16), (int)(height/2+height/24), (int)(GUIScale*width/6), (int)(GUIScale*height/16), color(70, 70, 220), color(50, 50, 200), color(255), (int)(TextScale*10), CENTER, "Exit to Desktop"), "pause screen");
    addElement("resume button", new Button((int)(width/2-GUIScale*width/16), (int)(height/2-3*height/24), (int)(GUIScale*width/6), (int)(GUIScale*height/16), color(70, 70, 220), color(50, 50, 200), color(255), (int)(TextScale*10), CENTER, "Resume"), "pause screen");

    addElement("turns remaining", new Text(bezel*2+220, bezel*4+30+30, 8, "", color(255), LEFT), "party management");
    addElement("move button", new Button(bezel, bezel*3, 100, 30, color(150), color(50), color(0), 10, CENTER, "Move"), "party management");
    addElement("split units", new Slider(bezel+10, bezel*3+30, 220, 30, color(255), color(150), color(0), color(0), 0, 0, 0, 1, 1, 1, true, ""), "party management");
    addElement("tasks", new DropDown(bezel, bezel*4+30+30, 220, 10, color(150), color(50), tasks), "party management");

    addElement("end turn", new Button(bezel, bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"), "bottom bar");
    addElement("idle party finder", new Button(bezel*2+buttonW, bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Idle Party"), "bottom bar");
    addElement("resource summary", new ResourceSummary(0, 0, 70, resourceNames, startingResources, totals), "bottom bar");
    int resSummaryX = width-((ResourceSummary)(getElement("resource summary", "bottom bar"))).totalWidth();
    addElement("resource expander", new Button(resSummaryX-50, bezel, 30, 30, color(150), color(50), color(0), 10, CENTER, "<"), "bottom bar");
    addElement("turn number", new TextBox(bezel*3+buttonW*2, bezel, -1, buttonH, 14, "Turn 0", color(0,0,255), 0), "bottom bar");
    prevIdle = new ArrayList<Integer[]>();

  }

  void initialiseBuildings(){
    JSONObject js;
    int numBuildings = gameData.getJSONArray("buildings").size();
    buildingTypes = new String[numBuildings];
    for (int i=0; i<numBuildings; i++){
      js = gameData.getJSONArray("buildings").getJSONObject(i);
      buildingTypes[i] = js.getString("id");
    }
  }
  void initialiseTasks(){
    JSONObject js;
    int numTasks = gameData.getJSONArray("tasks").size();
    taskOutcomes = new float[numTasks][numResources];
    taskCosts = new float[numTasks][numResources];
    tasks = new String[numTasks];
    for (int i=0; i<numTasks; i++){
      js = gameData.getJSONArray("tasks").getJSONObject(i);
      tasks[i] = js.getString("id");
      if (!js.isNull("production"))
        for (int r=0; r<js.getJSONArray("production").size(); r++)
          taskOutcomes[i][getResIndex(js.getJSONArray("production").getJSONObject(r).getString("id"))] = js.getJSONArray("production").getJSONObject(r).getFloat("quantity");
      if (!js.isNull("consumption"))
        for (int r=0; r<js.getJSONArray("consumption").size(); r++){
          taskCosts[i][getResIndex(js.getJSONArray("consumption").getJSONObject(r).getString("id"))] = js.getJSONArray("consumption").getJSONObject(r).getFloat("quantity");
        }
    }
  }
  void initialiseResources(){
    JSONObject js;
    numResources = gameData.getJSONArray("resources").size();
    resourceNames = new String[numResources];
    startingResources = new float[numResources];
    for (int i=0; i<numResources; i++){
      js = gameData.getJSONArray("resources").getJSONObject(i);
      resourceNames[i] = js.getString("id");
      JSONObject sr = findJSONObject(gameData.getJSONObject("game options").getJSONArray("starting resources"), resourceNames[i]);
      if (sr != null)
        startingResources[i] = sr.getFloat("quantity");
    }
  }

  void leaveState(){
    map.clearShape();
  }

  int getResIndex(String s){
    return JSONIndex(gameData.getJSONArray("resources"), s);
  }
  String getResString(int r){
    return gameData.getJSONArray("resources").getJSONObject(r).getString("id");
  }
  JSONArray taskInitialCost(String type){
    // Find initial cost for task (such as for buildings, 'Build Farm')
    JSONArray ja = findJSONObject(gameData.getJSONArray("tasks"), type).getJSONArray("initial cost");
    return ja;
  }
  int taskTurns(String task){
    JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task);
    if (jo==null){
      print("invalid task type", task);
      return 0;
    }
    if (jo.isNull("action"))return 0;
    return jo.getJSONObject("action").getInt("turns");
  }
  Action taskAction(String task){
    JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task).getJSONObject("action");
    if (jo != null)
      return new Action(task, jo.getString("notification"), jo.getInt("turns"), jo.getString("building"), jo.getString("terrain"));
    return null;
  }
  float[] JSONToCost(JSONArray ja){
    float[] costs = new float[numResources];
    if (ja == null){
      return null;
    }
    for (int i=0; i<ja.size(); i++){
      costs[getResIndex(ja.getJSONObject(i).getString("id"))] = ja.getJSONObject(i).getFloat("quantity");
    }
    return costs;
  }

  String terrainString(int terrainI){
    return gameData.getJSONArray("terrain").getJSONObject(terrainI-1).getString("id");
  }
  String buildingString(int buildingI){
    if (gameData.getJSONArray("buildings").isNull(buildingI-1)){
      println("invalid building string ", buildingI-1);
      return null;
    }
    return gameData.getJSONArray("buildings").getJSONObject(buildingI-1).getString("id");
  }
  float[] buildingCost(String actionType){
    float[] a = JSONToCost(taskInitialCost(actionType));
    if (a == null)
      return new float[numResources];
    else
      return a;
  }
  boolean postEvent(GameEvent event){
    boolean valid = true;
    // Returns true if event is valid
    if (event instanceof Move){

      Move m = (Move)event;
      int x = m.endX;
      int y = m.endY;
      int cellX = m.startX;
      int cellY = m.startY;

      if (x<0 || x>=mapWidth || y<0 || y>=mapHeight){
        println("invalid movement");
        valid = false;
      }

      Node[][] nodes = djk(cellX, cellY);

      if (canMove(cellX, cellY)){
        int sliderVal = round(((Slider)getElement("split units", "party management")).getValue());
        if (sliderVal > 0 && parties[cellY][cellX].getUnitNumber() >= 2 && parties[cellY][cellX].getTask() != "Battle"){
          map.updateMoveNodes(nodes);
          moving = true;
          splittedParty = new Party(turn, sliderVal, "Rest", parties[cellY][cellX].getMovementPoints());
          parties[cellY][cellX].changeUnitNumber(-sliderVal);
          if (parties[cellY][cellX].getUnitNumber() <= 0){
            parties[cellY][cellX] = null;
          }
        }
      }

      if (splittedParty != null){
        splittedParty.target = new int[]{x, y};
        splittedParty.path = getPath(cellX, cellY, x, y, nodes);
        int pathTurns;
        if (cellX==x&&cellY==y){
          pathTurns = 0;
        }
        else if (!canMove(cellX, cellY)){
          pathTurns = getMoveTurns(cellX, cellY, x, y, nodes);
        }
        else{
          pathTurns = 1+getMoveTurns(cellX, cellY, x, y, nodes);
        }
        splittedParty.setPathTurns(pathTurns);
        Collections.reverse(splittedParty.path);
        splittedParty.changeTask("Rest");
        splittedParty.clearActions();
        ((Text)getElement("turns remaining", "party management")).setText("");
        moveParty(cellX, cellY, true);
      }
      else {
        parties[cellY][cellX].target = new int[]{x, y};
        parties[cellY][cellX].path = getPath(cellX, cellY, x, y, nodes);
        int pathTurns;
        if (cellX==x&&cellY==y){
          pathTurns = 0;
        }
        else if (!canMove(cellX, cellY)){
          pathTurns = getMoveTurns(cellX, cellY, x, y, nodes);
        }
        else{
          pathTurns = 1+getMoveTurns(cellX, cellY, x, y, nodes);
        }
        parties[cellY][cellX].setPathTurns(pathTurns);
        Collections.reverse(parties[cellY][cellX].path);
        parties[cellY][cellX].changeTask("Rest");
        parties[cellY][cellX].clearActions();
        ((Text)getElement("turns remaining", "party management")).setText("");
        moveParty(cellX, cellY);
      }
    }
    else if (event instanceof EndTurn){
      if (!changeTurn)
        changeTurn();
      else
        valid = false;
    }

    else if (event instanceof ChangeTask){
      ChangeTask m = (ChangeTask)event;
      int cellX = m.x;
      int cellY = m.y;
      String task = m.task;
      parties[cellY][cellX].clearPath();
      parties[cellY][cellX].target = null;
      JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), parties[cellY][cellX].getTask());
      if (!jo.isNull("movement points")){
        //Changing from defending
        parties[cellY][cellX].setMovementPoints(min(parties[cellY][cellX].getMovementPoints()+jo.getInt("movement points"), gameData.getJSONObject("game options").getInt("movement points")));
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
      jo = findJSONObject(gameData.getJSONArray("tasks"), parties[cellY][cellX].getTask());
      if (!jo.isNull("movement points")){
        if (parties[cellY][cellX].getMovementPoints()-jo.getInt("movement points") >= 0){
          parties[cellY][cellX].subMovementPoints(jo.getInt("movement points"));
        }
        else{
          parties[cellY][cellX].changeTask("Rest");
        }
      } else if (jo.getString("id").equals("Launch Rocket")){
        int rocketBehaviour = int(random(10));
        buildings[cellY][cellX].image_id=0;
        //Rocket Launch Animation with behaviour
        if (rocketBehaviour > 6){
          winner = turn;
        }
        else {
          players[turn].resources[getResIndex("rocket progress")] = 0;
          parties[cellY][cellX].changeTask("Rest");
        }
      }
      else if (parties[cellY][cellX].getTask().equals("Produce Rocket")){
        if(players[turn].resources[getResIndex("rocket progress")]==-1){
          players[turn].resources[getResIndex("rocket progress")] = 0;
        }
      }

      else{
        Action a = taskAction(parties[cellY][cellX].getTask());
        if (a != null && a.type != null){
          float[] co = buildingCost(parties[cellY][cellX].getTask());
          if (sufficientResources(players[turn].resources, co, true)){
            parties[cellY][cellX].clearActions();
            ((Text)getElement("turns remaining", "party management")).setText("");
            parties[cellY][cellX].addAction(taskAction(parties[cellY][cellX].getTask()));
            if (sum(co)>0){
              spendRes(players[turn], co);
              buildings[cellY][cellX] = new Building(buildingIndex("Construction"));
            }
          }
          else{
            parties[cellY][cellX].changeTask("Rest");
          }
        }
      }

      checkTasks();
    }
    if (valid){
      if (!changeTurn){
        this.totals = totalResources();
        ResourceSummary rs = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
        rs.updateNet(totals);
        rs.updateStockpile(players[turn].resources);

        if (anyIdle(turn)){
          ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
        }
        else{
          ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
        }
      }
    }
    return valid;
  }
  boolean anyIdle(int turn){
    for (int x=0; x<mapWidth; x++){
      for (int y=0; y<mapWidth; y++){
        if (parties[y][x] != null && parties[y][x].player == turn && isIdle(x, y)){
          return true;
        }
      }
    }
    return false;
  }
  void updateCellSelection(){
    cellSelectionX = round((width-400-bezel*2)/GUIScale)+bezel*2;
    cellSelectionY = bezel*2;
    cellSelectionW = width-cellSelectionX-bezel*2;
    cellSelectionH = round(mapElementHeight);
    getPanel("land management").transform(cellSelectionX, cellSelectionY, cellSelectionW, round(cellSelectionH*0.15));
    getPanel("party management").transform(cellSelectionX, cellSelectionY+round(cellSelectionH*0.15)+bezel, cellSelectionW, round(cellSelectionH*0.5)-bezel*3);
    ((NotificationManager)(getElement("4notification manager", "default"))).transform(cellSelectionX, cellSelectionY+round(cellSelectionH*0.65)-bezel, cellSelectionW, round(cellSelectionH*0.35)-bezel*2);
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
    boolean correctTerrain, correctBuilding, enoughResources;
    JSONObject js;

    if(parties[cellY][cellX].hasActions()){
      makeTaskAvailable(parties[cellY][cellX].currentAction());
    }

    for(int i=0; i<gameData.getJSONArray("tasks").size(); i++){
      js = gameData.getJSONArray("tasks").getJSONObject(i);
      if (!js.isNull("terrain"))
        correctTerrain = JSONContainsStr(js.getJSONArray("terrain"), terrainString(terrain[cellY][cellX]));
      else
        correctTerrain = true;
      correctBuilding = false;
      enoughResources = true;
      if (!js.isNull("initial cost")){
        for (int j=0; j<js.getJSONArray("initial cost").size(); j++){
          JSONObject initialCost = js.getJSONArray("initial cost").getJSONObject(j);
          if (players[turn].resources[getResIndex(initialCost.getString("id"))]<(initialCost.getInt("quantity"))){
            enoughResources = false;
          }
        }
      }

      if (js.isNull("auto enabled")||!js.getBoolean("auto enabled")){
        if (js.isNull("buildings")){
          if (js.getString("id").equals("Demolish") && buildings[cellY][cellX] != null)
            correctBuilding = true;
          else if(!js.getString("id").equals("Demolish"))
            correctBuilding = true;
        }
        else{
          if (js.getJSONArray("buildings").size() > 0){
            if (buildings[cellY][cellX] != null)
            if (buildings[cellY][cellX] != null && JSONContainsStr(js.getJSONArray("buildings"), buildingString(buildings[cellY][cellX].type)))
              correctBuilding = true;
          }
          else if (buildings[cellY][cellX] == null){
            correctBuilding = true;
          }
        }
      }

      if (correctTerrain && correctBuilding && enoughResources){
        makeTaskAvailable(js.getString("id"));
      }
    }

    ((DropDown)getElement("tasks", "party management")).select(parties[cellY][cellX].getTask());
  }

  boolean UIHovering(){
   //To avoid doing things while hoving over important stuff
   NotificationManager nm = ((NotificationManager)(getElement("4notification manager", "default")));
   return !((!getPanel("party management").mouseOver() || !getPanel("party management").visible) && (!getPanel("land management").mouseOver() || !getPanel("land management").visible) &&
   (!nm.moveOver()||nm.empty()));
  }

  void turnChange(){
    float[] totalResourceRequirements = new float[numResources];
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (parties[y][x] != null){
          if (parties[y][x].player == turn){
            for (int i=0; i<tasks.length;i++){
              if(parties[y][x].getTask().equals(tasks[i])){
                for(int resource = 0; resource < numResources; resource++){
                  totalResourceRequirements[resource]+=taskCosts[i][resource]*parties[y][x].getUnitNumber();
                }
              }
            }
            Action action = parties[y][x].progressAction();
            if (action != null){
              if (!action.type.equals("Construction Mid") && !action.type.equals("Construction End"))
                notificationManager.post(action.notification, x, y, turnNumber, turn);
              if (action.building != null){
                if (action.building.equals(""))
                  buildings[y][x] = null;
                else
                  buildings[y][x] = new Building(buildingIndex(action.building));
              }
              if (action.terrain != null){
                if (terrain[y][x] == terrainIndex("forest")){
                  players[turn].resources[getResIndex("wood")]+=100;
                  map.removeTreeTile(x, y);
                }
                terrain[y][x] = terrainIndex(action.terrain);
              }
              switch (action.type){
                //-1 building types
                case "Construction Mid":
                  buildings[y][x].image_id = 1;
                  action = null;
                  break;
                case "Construction End":
                  buildings[y][x].image_id = 2;
                  action = null;
                  break;
              }
            }
            if (action != null){
              parties[y][x].clearCurrentAction();
              parties[y][x].changeTask("Rest");
            }
            moveParty(x, y);
          }
          else {
            if(parties[y][x].player==2){
              int player = ((Battle) parties[y][x]).party1.player;
              int otherPlayer = ((Battle) parties[y][x]).party2.player;
              parties[y][x] = ((Battle)parties[y][x]).doBattle();
              if(parties[y][x].player != 2){
                notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, player);
                notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
              }
            }
          }
        }
      }
    }
    float [] resourceAmountsAvailable = new float[numResources];
    for(int i=0; i<numResources;i++){
      if(totalResourceRequirements[i]==0){
        resourceAmountsAvailable[i] = 1;
      } else{
       resourceAmountsAvailable[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);
      }
    }

    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (parties[y][x] != null){
          if (parties[y][x].player == turn){
            float productivity = 1;
            for (int task=0; task<tasks.length;task++){
              if(parties[y][x].getTask().equals(tasks[task])){
                for(int resource = 0; resource < numResources; resource++){
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
              if(parties[y][x].getTask().equals(tasks[task])){
                for(int resource = 0; resource < numResources; resource++){
                  if(resource!=getResIndex("civilians")){
                    if(tasks[task]=="Produce Rocket"){
                      resource = getResIndex("rocket progress");
                    }
                    players[turn].resources[resource] += max((taskOutcomes[task][resource]-taskCosts[task][resource])*productivity*parties[y][x].getUnitNumber(), -players[turn].resources[resource]);
                    if(tasks[task]=="Produce Rocket"){
                      break;
                    }
                  } else if(resourceAmountsAvailable[getResIndex("food")]<1){
                    float lost = (1-resourceAmountsAvailable[getResIndex("food")])*taskOutcomes[task][resource]*parties[y][x].getUnitNumber();
                    parties[y][x].setUnitNumber(floor(parties[y][x].getUnitNumber()-lost));
                    if (parties[y][x].getUnitNumber() == 0)
                      notificationManager.post("Party Starved", x, y, turnNumber, turn);
                    else
                      notificationManager.post(String.format("Party Starving - %d lost", ceil(lost)), x, y, turnNumber, turn);
                  } else{
                    int prev = parties[y][x].getUnitNumber();
                    parties[y][x].setUnitNumber(ceil(parties[y][x].getUnitNumber()+taskOutcomes[task][resource]*(float)parties[y][x].getUnitNumber()));
                    if (prev != 1000 && parties[y][x].getUnitNumber() == 1000 && parties[y][x].task == "Super Rest"){
                      notificationManager.post("Party Full", x, y, turnNumber, turn);
                    }
                  }

                }
              }
            }
            if(parties[y][x].getUnitNumber()==0){
              parties[y][x] = null;
            }
          }
        }
      }
    }
    if (players[turn].resources[getResIndex("rocket progress")] > 1000){
      //display indicator saying rocket produced
      for (int y=0; y<mapHeight; y++){
        for (int x=0; x<mapWidth; x++){
          if (parties[y][x] != null){
            if (parties[y][x].player == turn){
              if(parties[y][x].getTask()=="Produce Rocket"){
                notificationManager.post("Rocket Produced", x, y, turnNumber, turn);
                parties[y][x].changeTask("Rest");
                buildings[y][x].image_id=1;
              }
            }
          }
        }
      }
    }
    partyMovementPointsReset();
    float mapXOffset;
    float mapYOffset;
    if (map.isPanning()){
      mapXOffset = map.getFocusedX();
      mapYOffset = map.getFocusedY();
    } else {
      mapXOffset = map.getFocusedX();
      mapYOffset = map.getFocusedY();
    }
    float blockSize;
    if (map.isZooming()){
      blockSize = map.getTargetZoom();
    } else{
      blockSize = map.getZoom();
    }
    players[turn].saveSettings(map.getTargetOffsetX(), map.getTargetOffsetY(), blockSize, cellX, cellY, cellSelected);
    turn = (turn + 1)%2;
    players[turn].loadSettings(this, map);
    changeTurn = false;
    TextBox t = ((TextBox)(getElement("turn number", "bottom bar")));
    t.setColour(players[turn].colour);
    t.setText("Turn "+turnNumber);

    this.totals = totalResources();
    ResourceSummary rs = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
    rs.updateNet(totals);
    rs.updateStockpile(players[turn].resources);

    notificationManager.turnChange(turn);

    if (anyIdle(turn)){
      ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
    }
    else{
      ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
    }

    if (turn == 0)
      turnNumber ++;
  }

  boolean checkForPlayerWin(){
    if(winner == -1){
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
    }
    Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
    winnerMessage.setText(winnerMessage.text.replace("/w", str(winner+1)));
    return true;
  }

  String update(){
    //background(100);
    if (changeTurn){
      turnChange();
    }

    drawPanels();
    if(players[0].resources[getResIndex("rocket progress")]!=-1||players[1].resources[getResIndex("rocket progress")]!=-1){
      drawRocketProgressBar();
    }
    if (cellSelected){
      drawCellManagement();
      if(parties[cellY][cellX] != null && getPanel("party management").visible)
        drawPartyManagement();
    }
    if (checkForPlayerWin()){
      this.getPanel("end screen").visible = true;
    }
    return getNewState();
  }
  void partyMovementPointsReset(){
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (parties[y][x] != null){
          if (parties[y][x].player != 2){
            parties[y][x].setMovementPoints(gameData.getJSONObject("game options").getInt("movement points"));
          }
        }
      }
    }
  }
  void changeTurn(){
    changeTurn = true;
  }
  boolean sufficientResources(float[] available, float[] required){
    for (int i=0; i<numResources;i++){
      if (available[i] < required[i]){
        return false;
      }
    }
    return true;
  }
  boolean sufficientResources(float[] available, float[] required, boolean flash){
    ResourceSummary rs = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
    boolean t = true;
    for (int i=0; i<numResources;i++){
      if (available[i] < required[i] && !buildingString(i).equals("rocket progress")){
        t = false;
        rs.flash(i);
      }
    }
    return t;
  }
  void spendRes(Player player, float[] required){
    for (int i=0; i<numResources;i++){
      player.resources[i] -= required[i];
    }
  }
  void reclaimRes(Player player, float[] required){
    //reclaim half cost of building
    for (int i=0; i<numResources;i++){
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
    float points;
    int[][] mvs = {{1,0}, {0,1}, {1,1}, {-1,0}, {0,-1}, {-1,-1}, {1,-1}, {-1,1}};

    if (splittedParty!=null){
      points = splittedParty.getMovementPoints();
      for (int[] n : mvs){
        if (points >= cost(x+n[0], y+n[1], x, y)){
          return true;
        }
      }
    }
    else{
      points = parties[y][x].getMovementPoints();
      for (int[] n : mvs){
        if (points >= cost(x+n[0], y+n[1], x, y)){
          return true;
        }
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
  boolean isIdle(int x, int y){
    return (parties[y][x].task.equals("Rest") && canMove(x, y) && (parties[y][x].path==null||parties[y][x].path!=null&&parties[y][x].path.size()==0));
  }

  int[] findIdle(int player){
    int[] backup = {-1, -1};
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if (parties[y][x] != null && parties[y][x].player == player && isIdle(x, y)){
          if (inPrevIdle(x, y)){
            backup = new int[]{x, y};
          }
          else{
            prevIdle.add(new Integer[]{x, y});
            return new int[]{x, y};
          }
        }
      }
    }
    clearPrevIdle();
    if (backup[0] == -1){
      return backup;
    }
    else{
      return findIdle(player);
    }
  }

  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type == "clicked"){
        if (event.id == "idle party finder"){
          int[] t = findIdle(turn);
          if (t[0] != -1){
            selectCell(t[0], t[1], false);
            map.targetCell(t[0], t[1], 64);
          }
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
        else if (event.id == "resource expander"){
          ResourceSummary r = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
          Button b = ((Button)(getElement("resource expander", "bottom bar")));
          r.toggleExpand();
          b.transform(width-r.totalWidth()-50, bezel, 30, 30);
          if (b.getText() == ">")
            b.setText("<");
          else
            b.setText(">");
        }
        else if (event.id == "end game button"){
          newState = "menu";
        }
        else if (event.id == "main menu button"){
          newState = "menu";
        }
        else if (event.id == "desktop button"){
          exit();
        }
        else if (event.id == "resume button"){
          getPanel("pause screen").visible = false;
        }
      }
      if (event.type == "valueChanged"){
        if (event.id == "tasks"){
          postEvent(new ChangeTask(cellX, cellY, ((DropDown)getElement("tasks", "party management")).getSelected()));
        }
      }
      if (event.type == "notification selected"){
        int x = notificationManager.lastSelected.x, y = notificationManager.lastSelected.y;
        map.targetCell(x, y, 100);
        selectCell(x, y, false);
      }
    }
  }
  void deselectCell(){
    tooltip.hide();
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
      p = parties[py][px];
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
    int i=0;
    boolean moved = false;
    for (int node=1; node<path.size(); node++){
      int cost = cost(path.get(node)[0], path.get(node)[1], px, py);
      if (p.getMovementPoints() >= cost){
        if (parties[path.get(node)[1]][path.get(node)[0]] == null){
          // empty cell
          p.subMovementPoints(cost);
          parties[path.get(node)[1]][path.get(node)[0]] = p;
          if (splitting){
            splittedParty = null;
            splitting = false;
          } else{
            parties[py][px] = null;
          }
          px = path.get(node)[0];
          py = path.get(node)[1];
          p = parties[py][px];
          if (!moved){
            p.moved();
            moved = true;
          }
        }
        else if(path.get(node)[0] != px || path.get(node)[1] != py){
          p.clearPath();
          if (parties[path.get(node)[1]][path.get(node)[0]].player == turn){
            // merge cells
            notificationManager.post("Parties Merged", (int)path.get(node)[0], (int)path.get(node)[1], turnNumber, turn);
            int movementPoints = min(parties[path.get(node)[1]][path.get(node)[0]].getMovementPoints(), p.getMovementPoints()-cost);
            int overflow = parties[path.get(node)[1]][path.get(node)[0]].changeUnitNumber(p.getUnitNumber());
            if(cellFollow){
              selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
              stillThere = false;
            }
            if (splitting){
              splittedParty = null;
              splitting = false;
            } else{
              parties[py][px] = null;
            }
            if (overflow>0){
              if(parties[path.get(node-1)[1]][path.get(node-1)[0]]==null){
                p.setUnitNumber(overflow);
                parties[path.get(node-1)[1]][path.get(node-1)[0]] = p;
              } else {
                parties[path.get(node-1)[1]][path.get(node-1)[0]].changeUnitNumber(overflow);
              }
            }
            parties[path.get(node)[1]][path.get(node)[0]].setMovementPoints(movementPoints);
          } else if (parties[path.get(node)[1]][path.get(node)[0]].player == 2){
            // merge cells battle
            notificationManager.post("Battle Reinforced", (int)path.get(node)[0], (int)path.get(node)[1], turnNumber, turn);
            int overflow = ((Battle) parties[path.get(node)[1]][path.get(node)[0]]).changeUnitNumber(turn, p.getUnitNumber());
            if(cellFollow){
              selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
              stillThere = false;
            }
              if (splitting){
                splittedParty = null;
                splitting = false;
              } else{
                parties[py][px] = null;
              }
            if (overflow>0){
              if(parties[path.get(node-1)[1]][path.get(node-1)[0]]==null){
                p.setUnitNumber(overflow);
                parties[path.get(node-1)[1]][path.get(node-1)[0]] = p;
              } else {
                parties[path.get(node-1)[1]][path.get(node-1)[0]].changeUnitNumber(overflow);
              }
            }
          }
          else{
            int x, y;
            x = path.get(node)[0];
            y = path.get(node)[1];
            int otherPlayer = parties[y][x].player;
            notificationManager.post("Battle Started", x, y, turnNumber, turn);
            notificationManager.post("Battle Started", x, y, turnNumber, otherPlayer);
            p.subMovementPoints(cost);
            parties[y][x] = new Battle(p, parties[y][x]);
            parties[y][x] = ((Battle)parties[y][x]).doBattle();
            if(parties[y][x].player != 2){
              notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, turn);
              notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
            }
            if(cellFollow){
              selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
              stillThere = false;
            }
            if (splitting){
                splittedParty = null;
                splitting = false;
              } else{
                parties[py][px] = null;
              }
            if(buildings[path.get(node)[1]][path.get(node)[0]]!=null&&buildings[path.get(node)[1]][path.get(node)[0]].type==0){
              buildings[path.get(node)[1]][path.get(node)[0]] = null;
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
    int movementPoints;
    if (parties[startY][startX] != null)
      movementPoints = round(parties[startY][startX].getMovementPoints());
    else if (splittedParty != null)
      movementPoints = round(splittedParty.getMovementPoints());
    else
      return -1;

    int turns = 0;
    ArrayList <int[]> path = getPath(startX, startY, targetX, targetY, nodes); //<>// //<>// //<>//
    Collections.reverse(path);
    for (int node=1; node<path.size(); node++){
      int cost = cost(path.get(node)[0], path.get(node)[1], path.get(node-1)[0], path.get(node-1)[1]);
      if (movementPoints < cost){ //<>// //<>// //<>// //<>// //<>//
        turns += 1;
        movementPoints = gameData.getJSONObject("game options").getInt("movement points");
      }
      movementPoints -= cost; //<>// //<>// //<>// //<>// //<>// //<>// //<>//
    }
    return turns;
  }
  int splitUnitsNum(){ //<>// //<>// //<>// //<>// //<>//
    return round(((Slider)getElement("split units", "party management")).getValue());
  }
  void refreshTooltip(){
    if (((DropDown)getElement("tasks", "party management")).moveOver() && getPanel("party management").visible){
      tooltip.setTask(((DropDown)getElement("tasks", "party management")).findMouseOver());
      tooltip.show();
    }
    else if(((Text)getElement("turns remaining", "party management")).mouseOver()&& getPanel("party management").visible){
      tooltip.setTurnsRemaining();
      tooltip.show();
    }
    else if(((Button)getElement("move button", "party management")).mouseOver()&& getPanel("party management").visible){
      tooltip.setMoveButton();
      tooltip.show();
    }
    else if (moving&&map.mouseOver() && !UIHovering()){
      Node [][] nodes = map.getMoveNodes();
      int x = floor(map.scaleXInv(mouseX));
      int y = floor(map.scaleYInv(mouseY));
      if (x < mapWidth && y<mapHeight && x>=0 && y>=0 && nodes[y][x] != null && !(cellX == x && cellY == y)){
        if (parties[cellY][cellX] != null){ //<>// //<>// //<>//
          map.updatePath(getPath(cellX, cellY, x, y, map.getMoveNodes()));
        }
        if(parties[y][x]==null){
          //Moving into empty tile //<>// //<>// //<>// //<>// //<>//
          int turns = getMoveTurns(cellX, cellY, x, y, nodes);
          boolean splitting = splitUnitsNum()!=parties[cellY][cellX].getUnitNumber();
          tooltip.setMoving(turns, splitting);
          tooltip.show(); //<>// //<>// //<>// //<>// //<>// //<>// //<>//
        }
        else {
          if (parties[y][x].player == turn){
            //merge parties //<>// //<>// //<>// //<>// //<>//
            tooltip.setMerging();
            tooltip.show();
          }
          else {
            //Attack
            Party tempAttacker = parties[cellY][cellX].clone();
            int units = round(splitUnitsNum());
            tempAttacker.setUnitNumber(units);
            int chance = getChanceOfBattleSuccess(tempAttacker, parties[y][x]);
            tooltip.setAttacking(chance);
            tooltip.show();
          }
        }
      }
      else{
        map.cancelPath();
        tooltip.hide();
      }
    }
    else{
      map.cancelPath();
      tooltip.hide();
    }
    map.setActive(!UIHovering());
  }

  ArrayList<String> mouseEvent(String eventType, int button){
    refreshTooltip();
    if (button == RIGHT){
      if (eventType == "mousePressed"){
        if (parties[cellY][cellX] != null && parties[cellY][cellX].player == turn && cellSelected && !UIHovering()){
          if (map.mouseOver()){
            if (moving){
              map.cancelPath();
              moving = false;
              map.cancelMoveNodes();
            }
            else{
              moving = true;
              map.updateMoveNodes(djk(cellX, cellY));
            }
          }
        }
      }
      if (eventType == "mouseReleased"){
        if (parties[cellY][cellX] != null && parties[cellY][cellX].player == turn && !UIHovering()){
          if (moving){
            int x = floor(map.scaleXInv(mouseX));
            int y = floor(map.scaleYInv(mouseY)); //<>// //<>// //<>//
            postEvent(new Move(cellX, cellY, x, y));
            map.cancelPath();
            moving = false;
            map.cancelMoveNodes(); //<>// //<>// //<>// //<>// //<>//
          }
        }
      }
    } //<>// //<>// //<>// //<>// //<>// //<>// //<>//
    if (button == LEFT){
      if (eventType == "mouseClicked"){
        if (activePanel == "default" && !UIHovering()){
          if (map.mouseOver()){ //<>// //<>// //<>// //<>// //<>//
            if (moving){
              //int x = floor(map.scaleXInv(mouseX));
              //int y = floor(map.scaleYInv(mouseY));
              //postEvent(new Move(cellX, cellY, x, y));
              //map.cancelPath();
              if (mousePressed){
                map.cancelPath();
                moving = false;
                map.cancelMoveNodes();
              }
              else{
                int x = floor(map.scaleXInv(mouseX));
                int y = floor(map.scaleYInv(mouseY));
                postEvent(new Move(cellX, cellY, x, y));
                map.cancelPath();
                moving = false;
                map.cancelMoveNodes();
              }
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
    return new ArrayList<String>();
  }
  void selectCell(int x, int y){
    x = floor(map.scaleXInv(x));
    y = floor(map.scaleYInv(y));
    selectCell(x, y, false);
  }

  boolean cellInBounds(int x, int y){
    return 0<=x&&x<mapWidth&&0<=y&&y<mapHeight;
  }

  void selectCell(int x, int y, boolean raw){
    deselectCell();
    if(raw){
      selectCell(x, y);
    }
    else if (cellInBounds(x, y)){
      tooltip.hide();
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
    float[] totalResourceRequirements = new float[numResources];
    float [] resourceAmountsAvailable = new float[numResources];
    float [] production = new float[numResources];
    for(int i=0; i<numResources;i++){
      if(totalResourceRequirements[i]==0){
        resourceAmountsAvailable[i] = 1;
      } else{
       resourceAmountsAvailable[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);
      }
    }
    if (parties[y][x] != null){
      if (parties[y][x].player == turn){
        float productivity = 1;
        for (int task=0; task<tasks.length;task++){
          if(parties[y][x].getTask().equals(tasks[task])){
            for(int resource = 0; resource < numResources; resource++){
              if(taskCosts[task][resource]>0){
                productivity = min(productivity, resourceAmountsAvailable[resource]);
              }
            }
          }
        }
        for (int task=0; task<tasks.length;task++){
          if(parties[y][x].getTask().equals(tasks[task])){
            for(int resource = 0; resource < numResources; resource++){
              if(resource<numResources){
                production[resource] = (taskOutcomes[task][resource])*productivity*parties[y][x].getUnitNumber();
              }
            }
          }
        }
      }
    }
    return production;
  }
  float[] resourceConsumption(int x, int y){
    float[] totalResourceRequirements = new float[numResources];
    float [] resourceAmountsAvailable = new float[numResources];
    float [] production = new float[numResources];
    for(int i=0; i<numResources;i++){
      if(totalResourceRequirements[i]==0){
        resourceAmountsAvailable[i] = 1;
      } else{
       resourceAmountsAvailable[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);
      }
    }
    if (parties[y][x] != null){
      if (parties[y][x].player == turn){
        float productivity = 1;
        for (int task=0; task<tasks.length;task++){
          if(parties[y][x].getTask().equals(tasks[task])){
            for(int resource = 0; resource < numResources; resource++){
              if(taskCosts[task][resource]>0){
                productivity = min(productivity, resourceAmountsAvailable[resource]);
              }
            }
          }
        }
        for (int task=0; task<tasks.length;task++){
          if(parties[y][x].getTask().equals(tasks[task])){
            for(int resource = 0; resource < numResources; resource++){
              if(resource<numResources){
                production[resource] = (taskCosts[task][resource])*productivity*parties[y][x].getUnitNumber();
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
      textFont(getFont(10*TextScale));
    textAlign(CENTER, TOP);
    text("Party Management", cellSelectionX+cellSelectionW/2, pp.y);

    textAlign(LEFT, CENTER);
    textFont(getFont(8*TextScale));
    float barY = cellSelectionY + 13*TextScale + cellSelectionH*0.15 + bezel*2;
    text("Movement Points Remaining: "+parties[cellY][cellX].getMovementPoints(turn) + "/"+gameData.getJSONObject("game options").getInt("movement points"), 120+cellSelectionX, barY);
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
    for (int i=0; i<numResources;i++){
      if (resources[i]>0){
        returnString += roundDp(""+resources[i], 1)+ " " +resourceNames[i]+ ", ";
        notNothing = true;
      }
    }
    if (!notNothing)
      returnString += "Nothing/Unknown";
    else if(returnString.length()-2 > 0)
      returnString = returnString.substring(0, returnString.length()-2);
    return returnString;
  }

  float[] totalResources(){
    float[] amount=new float[resourceNames.length];
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
    textFont(getFont(10*TextScale));
    textAlign(CENTER, TOP);
    text("Land Management", cellSelectionX+cellSelectionW/2, cellSelectionY);

    fill(0);
    textAlign(LEFT, TOP);
    float barY = cellSelectionY + 13*TextScale;
    text("Cell Type: "+gameData.getJSONArray("terrain").getJSONObject(terrain[cellY][cellX]-1).getString("display name"), 5+cellSelectionX, barY);
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
    String pl = resourcesList(production);
    String cl = resourcesList(consumption);
    fill(0);
    if (!pl.equals("Nothing/Unknown")){
      text("Producing: "+pl, 5+cellSelectionX, barY);
      barY += 13*TextScale;
    }
    if (!cl.equals("Nothing/Unknown")){
      fill(255,0,0);
      text("Consuming: "+cl, 5+cellSelectionX, barY);
      barY += 13*TextScale;
    }
  }

  String getResourceString(float amount){
    String tempString = roundDp(""+amount, 1);
    if (amount >= 0){
      fill(0);
      tempString = "+"+tempString;
    }
    else{
      fill(255, 0, 0);
    }
    return tempString;
  }

  void drawRocketProgressBar(){
    int x, y, w, h;
    String progressMessage;
    boolean both = players[0].resources[getResIndex("rocket progress")] != 0 && players[1].resources[getResIndex("rocket progress")] != 0;
    if (players[0].resources[getResIndex("rocket progress")] ==0 && players[1].resources[getResIndex("rocket progress")] == 0)return;
    int progress = int(players[turn].resources[getResIndex("rocket progress")]);
    color fillColour;
    if(progress == 0){
      progress = int(players[(turn+1)%2].resources[getResIndex("rocket progress")]);
      progressMessage = "";
      fillColour = playerColours[(turn+1)%2];
    } else {
      fillColour = playerColours[turn];
      if(progress>=1000){
         progressMessage = "Rocket Progress: Completed";
      } else {
        progressMessage = "Rocket Progress: "+str(progress)+"/1000";
      }
    }
    if (both){
      x = round(width*0.25);
      y = round(height*0.05);
      w = round(width/2);
      h = round(height*0.015);
      fill(200);
      stroke(100);
      rect(x, y, w, h);
      noStroke();
      progress = int(players[0].resources[getResIndex("rocket progress")]);
      w = round(min(w, w*progress/1000));
      fill(playerColours[0]);
      rect(x, y, w, h);
      y = round(height*0.065);
      w = round(width/2);
      fill(200);
      stroke(100);
      rect(x, y, w, h);
      noStroke();
      progress = int(players[1].resources[getResIndex("rocket progress")]);
      w = round(min(w, w*progress/1000));
      fill(playerColours[1]);
      rect(x, y, w, h);
      y = round(height*0.05);
    } else {
      x = round(width*0.25);
      y = round(height*0.05);
      w = round(width/2);
      h = round(height*0.03);
      fill(200);
      stroke(100);
      rect(x, y, w, h);
      noStroke();
      w = round(min(w, w*progress/1000));
      fill(fillColour);
      rect(x, y, w, h);
    }
    textFont(getFont(10*TextScale));
    textAlign(CENTER, BOTTOM);
    fill(200);
    int tw = ceil((textWidth(progressMessage)));
    rect(width/2 -tw/2, y-10*TextScale, tw, 10*TextScale);
    fill(0);
    text(progressMessage, width/2, y);
  }

  ArrayList<String> keyboardEvent(String eventType, char _key){
    refreshTooltip();
    if (eventType == "keyPressed" && _key == ESC){
      getPanel("pause screen").visible = !getPanel("pause screen").visible;
    }
    if (eventType == "keyTyped"){
      if (key == ' '){
        postEvent(new EndTurn());
      }
      else if (key == 'i'){
        int[] t = findIdle(turn);
        if (t[0] != -1){
          selectCell(t[0], t[1], false);
          map.targetCell(t[0], t[1], 64);
        }
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
    if (mapIs3D){
      map = (Map3D)getElement("3dmap", "default");
      ((Map3D)getElement("3dmap", "default")).visible = true;
      ((Map2D)getElement("2dmap", "default")).visible = false;
    } else {
      map = (Map2D)getElement("2dmap", "default");
      ((Map3D)getElement("3dmap", "default")).visible = false;
      ((Map2D)getElement("2dmap", "default")).visible = true;
    }
    terrain = generateMap(playerStarts);
    map.reset(mapWidth, mapHeight, terrain, parties, buildings);

    float[] conditions2 = map.targetCell((int)playerStarts[1].x, (int)playerStarts[1].y, 42);
    players[1] = new Player(conditions2[0], conditions2[1], 42, startingResources.clone(), color(255,0,0));
    float[] conditions1 = map.targetCell((int)playerStarts[0].x, (int)playerStarts[0].y, 42);
    players[0] = new Player(conditions1[0], conditions1[1], 42, startingResources.clone(), color(0,0,255));
    //for(int i=0;i<NUMOFBUILDINGTYPES;i++){
    //  buildings[(int)playerStarts[0].y][(int)playerStarts[0].x+i] = new Building(1+i);
    //}
    deselectCell();
    turn = 0;
    turnNumber = 0;
    tooltip.hide();
    winner = -1;
    this.totals = totalResources();
    TextBox t = ((TextBox)(getElement("turn number", "bottom bar")));
    t.setColour(players[turn].colour);
    t.setText("Turn "+turnNumber);

    ResourceSummary rs = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
    rs.updateNet(totals);
    rs.updateStockpile(players[turn].resources);
    getPanel("pause screen").visible = false;

    notificationManager.reset();

    if (anyIdle(turn)){
      ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
    }
    else{
      ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
    }
    map.generateShape();
  }
  int cost(int x, int y, int prevX, int prevY){
    float mult = 1;
    if (x!=prevX && y!=prevY){
      mult = 1.41;
    }
    if (0<=x && x<mapSize && 0<=y && y<mapSize){
      return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]-1).getInt("movement cost")*mult);
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
        if (0 <= nx && nx < w && 0 <= ny && ny < h){
          boolean sticky = parties[ny][nx] != null;
          int newCost = cost(nx, ny, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
          int prevCost = curMinCosts.get(0);
          int totalNewCost = prevCost+newCost;
          if (totalNewCost < gameData.getJSONObject("game options").getInt("movement points")*100){
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
    if(p1.dist(p2)<mapWidth/8||noise(p1.x*MAPNOISESCALE, p1.y*MAPNOISESCALE)<waterLevel||noise(p2.x*MAPNOISESCALE, p2.y*MAPNOISESCALE)<waterLevel){
      return true;
    }
    return false;
  }
  PVector generatePartyPosition(){
    return new PVector(int(random(0, mapWidth)), int(random(0, mapHeight)));
  }

  PVector[] generateStartingParties(){
    PVector player1 = generatePartyPosition();
    PVector player2 = generatePartyPosition();
    int counter = 0;
    while(startInvalid(player1, player2)&&counter<100){
      counter++;
      player1 = generatePartyPosition();
      player2 = generatePartyPosition();
    }
    parties[(int)player1.y][(int)player1.x] = new Party(0, 100, "Rest", gameData.getJSONObject("game options").getInt("movement points"));
    parties[(int)player2.y][(int)player2.x] = new Party(1, 100, "Rest", gameData.getJSONObject("game options").getInt("movement points"));

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
      if(terrain[coord[1]][coord[0]]==terrainIndex("water")){
        newMap[coord[1]][coord[0]] = terrain[coord[1]][coord[0]];
      } else {
        int[] counts = new int[NUMOFGROUNDTYPES+1];
        for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
          for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
            if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
              if(terrain[y1][x1]!=terrainIndex("water")){
                counts[terrain[y1][x1]]+=1;
              }
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
    ((BaseMap)map).generateNoiseMaps(mapWidth, mapHeight);
    HashMap<Integer, Float> groundWeightings = new HashMap();
    for (Integer i=1; i<gameData.getJSONArray("terrain").size()+1; i++){
      groundWeightings.put(i, gameData.getJSONArray("terrain").getJSONObject(i-1).getFloat("weighting"));
    }

    float totalWeighting = 0;
    for (float weight: groundWeightings.values()){
      totalWeighting+=weight;
    }

    int [][] terrain = new int[mapHeight][mapWidth];

    //for(int y=0; y<mapHeight; y++){
    //  terrain[y][0] = terrainIndex("water");
    //  terrain[y][mapWidth-1] = terrainIndex("water");
    //}
    //for(int x=1; x<mapWidth-1; x++){
    //  terrain[0][x] = terrainIndex("water");
    //  terrain[mapHeight-1][x] = terrainIndex("water");
    //}
    for(int i=0;i<groundSpawns;i++){
      int type = getRandomGroundType(groundWeightings, totalWeighting);
      int x = (int)random(mapWidth);
      int y = (int)random(mapHeight);
      if(isWater(x, y)){
        i--;
      } else {
        terrain[y][x] = type;
      }
    }

    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=0; y<mapHeight;y++){
      for (int x=0; x<mapWidth;x++){
        if(isWater(x, y)){
          terrain[y][x] = terrainIndex("water");
        } else {
          order.add(new int[] {x, y});
        }
      }
    }
    Collections.shuffle(order);
    for (int[] coord: order){
      int x = coord[0];
      int y = coord[1];
      while (terrain[y][x] == 0||terrain[y][x]==terrainIndex("water")){
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
    for (int y=0; y<mapHeight; y++){
      for(int x=0; x<mapWidth; x++){
        if(terrain[y][x] != terrainIndex("water") && (((BaseMap)map).groundMaxRawHeightAt(x, y) > 0.5+waterLevel/2.0) || getMaxSteepness(x, y)>HILLSTEEPNESS){
          terrain[y][x] = terrainIndex("hills");
        }
      }
    }
    return terrain;
  }
}
