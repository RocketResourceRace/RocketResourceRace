
int terrainIndex(String terrain) {
  try {
    int k = JSONIndex(gameData.getJSONArray("terrain"), terrain);
    if (k>=0) {
      return k+1;
    }
    LOGGER_MAIN.warning("Invalid terrain type, "+terrain);
    return 0;
  }
  catch (NullPointerException e) {
    LOGGER_MAIN.log(Level.WARNING, "Error most likely due to incorrect JSON code. building:"+terrain, e);
    return 0;
  }
  catch (Exception e) {
    LOGGER_MAIN.log(Level.SEVERE, "Error getting terrain index for:"+terrain, e);
    throw e;
  }
}
int buildingIndex(String building) {
  try {
    int k = JSONIndex(gameData.getJSONArray("buildings"), building);
    if (k>=0) {
      return k+1;
    }
    LOGGER_MAIN.warning("Invalid building type, "+building);
    return 0;
  }
  catch (NullPointerException e) {
    LOGGER_MAIN.log(Level.WARNING, "Error most likely due to incorrect JSON code. building:"+building, e);
    return 0;
  }
  catch (Exception e) {
    LOGGER_MAIN.log(Level.SEVERE, "Error getting building index for:"+building, e);
    throw e;
  }
}


class Game extends State {
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezel = 10;
  final int mapElementWidth = round(width);
  final int mapElementHeight = round(height);
  final int CLICKHOLD = 500;
  final int MOUSEPRESSTOLERANCE = 100;
  PGraphics gameUICanvas;
  String[] tasks;
  String[] buildingTypes;
  float[][] taskCosts;
  float[][] taskOutcomes;
  int numResources;
  String[] resourceNames;
  float [] startingResources;
  int turnNumber;
  int mapHeight = jsManager.loadIntSetting("map size");
  int mapWidth = jsManager.loadIntSetting("map size");
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  BattleEstimateManager battleEstimateManager;
  NotificationManager notificationManager;
  Tooltip tooltip;
  int turn;
  boolean changeTurn = false;
  int winner = -1;
  Map3D map3d;
  Map2D map2d;
  Map map;
  Player[] players;
  int selectedCellX, selectedCellY, sidePanelX, sidePanelY, sidePanelW, sidePanelH;
  boolean cellSelected=false, moving=false;
  color partyManagementColour;
  ArrayList<Integer[]> prevIdle;
  float[] totals;
  Party splittedParty;
  int[] mapClickPos = null;
  boolean cinematicMode;
  boolean rocketLaunching;
  int rocketBehaviour;
  PVector rocketPosition;
  PVector rocketVelocity;
  int rocketStartTime;

  Game() {
    try {
      LOGGER_MAIN.fine("initializing game");
      gameUICanvas = createGraphics(width, height, P2D); 
      initialiseResources();
      initialiseTasks();
      initialiseBuildings();

      addElement("2dmap", new Map2D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
      addElement("3dmap", new Map3D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
      addElement("notification manager", new NotificationManager(0, 0, 0, 0, color(100), color(255), 8, turn));

      //map = (Map3D)getElement("2map", "default");
      notificationManager = (NotificationManager)getElement("notification manager", "default");
      players = new Player[2];
      totals = new float[resourceNames.length];

      // Initial positions will be focused on starting party
      //players[0] = new Player(map.focusedX, map.focusedY, map.zoom, startingResources, color(0,0,255));
      //players[1] = new Player(map.focusedX, map.focusedY, map.zoom, startingResources, color(255,0,0));
      addPanel("land management", 0, 0, width, height, false, true, color(50, 200, 50), color(0));
      addPanel("party management", 0, 0, width, height, false, true, color(110, 110, 255), color(0));
      addPanel("bottom bar", 0, height-70, width, 70, true, true, color(150), color(50));
      addPanel("end screen", 0, 0, width, height, false, true, color(50, 50, 50, 50), color(0));
      addPanel("pause screen", 0, 0, width, height, false, true, color(50, 50, 50, 50), color(0));
      addPanel("save screen", (int)(width/2+jsManager.loadFloatSetting("gui scale")*150+(int)(jsManager.loadFloatSetting("gui scale")*20)), (int)(height/2-5*jsManager.loadFloatSetting("gui scale")*40), (int)(jsManager.loadFloatSetting("gui scale")*500), (int)(jsManager.loadFloatSetting("gui scale")*500), false, false, color(50), color(0));
      addPanel("overlay", 0, 0, width, height, true, false, color(255, 255), color(255, 255));
      addPanel("console", 0, height/2, width, height/2, false, true, color(0, 220), color(255, 0));

      getPanel("save screen").setOverrideBlocking(true);

      addElement("0tooltip", new Tooltip(), "overlay");
      tooltip = (Tooltip)getElement("0tooltip", "overlay");

      addElement("end game button", new Button((int)(width/2-jsManager.loadFloatSetting("gui scale")*width/16), (int)(height/2+height/8), (int)(jsManager.loadFloatSetting("gui scale")*width/8), (int)(jsManager.loadFloatSetting("gui scale")*height/16), color(70, 70, 220), color(50, 50, 200), color(255), 14, CENTER, "End Game"), "end screen");
      addElement("winner", new Text(width/2, height/2, (int)(jsManager.loadFloatSetting("text scale")*10), "", color(255), CENTER), "end screen");

      addElement("main menu button", new Button((int)(width/2-jsManager.loadFloatSetting("gui scale")*150), (int)(height/2-jsManager.loadFloatSetting("gui scale")*40), (int)(jsManager.loadFloatSetting("gui scale")*300), (int)(jsManager.loadFloatSetting("gui scale")*60), color(70, 70, 220), color(50, 50, 200), color(255), 14, CENTER, "Exit to Main Menu"), "pause screen");
      addElement("desktop button", new Button((int)(width/2-jsManager.loadFloatSetting("gui scale")*150), (int)(height/2+jsManager.loadFloatSetting("gui scale")*40), (int)(jsManager.loadFloatSetting("gui scale")*300), (int)(jsManager.loadFloatSetting("gui scale")*60), color(70, 70, 220), color(50, 50, 200), color(255), 14, CENTER, "Exit to Desktop"), "pause screen");
      addElement("save as button", new Button((int)(width/2-jsManager.loadFloatSetting("gui scale")*150), (int)(height/2-3*jsManager.loadFloatSetting("gui scale")*40), (int)(jsManager.loadFloatSetting("gui scale")*300), (int)(jsManager.loadFloatSetting("gui scale")*60), color(70, 70, 220), color(50, 50, 200), color(255), 14, CENTER, "Save As"), "pause screen");
      addElement("resume button", new Button((int)(width/2-jsManager.loadFloatSetting("gui scale")*150), (int)(height/2-5*jsManager.loadFloatSetting("gui scale")*40), (int)(jsManager.loadFloatSetting("gui scale")*300), (int)(jsManager.loadFloatSetting("gui scale")*60), color(70, 70, 220), color(50, 50, 200), color(255), 14, CENTER, "Resume"), "pause screen");

      addElement("save button", new Button(bezel, bezel, (int)(jsManager.loadFloatSetting("gui scale")*300)-2*bezel, (int)(jsManager.loadFloatSetting("gui scale")*60), color(100), color(0), color(255), 14, CENTER, "Save"), "save screen");
      addElement("saving manager", new BaseFileManager(bezel, (int)(4*jsManager.loadFloatSetting("gui scale")*40), (int)(jsManager.loadFloatSetting("gui scale")*500)-2*bezel, (int)(jsManager.loadFloatSetting("gui scale")*320), "saves"), "save screen");
      addElement("save namer", new TextEntry(bezel, (int)(2*jsManager.loadFloatSetting("gui scale")*40)+bezel*2, (int)(jsManager.loadFloatSetting("gui scale")*300), (int)(jsManager.loadFloatSetting("gui scale")*50), LEFT, color(0), color(100), color(0), "", "Save Name"), "save screen");

      addElement("turns remaining", new Text(bezel*2+220, bezel*4+30+30, 8, "", color(255), LEFT), "party management");
      addElement("move button", new Button(bezel, bezel*3, 100, 30, color(150), color(50), color(0), 10, CENTER, "Move"), "party management");
      addElement("disband button", new Button(bezel, bezel*3, 100, 30, color(150), color(50), color(0), 10, CENTER, "Dispand"), "party management");
      addElement("split units", new Slider(bezel+10, bezel*3+30, 220, 30, color(255), color(150), color(0), color(0), 0, 0, 0, 1, 1, 1, true, ""), "party management");
      addElement("tasks", new TaskManager(bezel, bezel*4+30+30, 220, 8, color(150), color(50), tasks, 10), "party management");
      addElement("task text", new Text(0, 0, 10, "Tasks", color(0), LEFT), "party management");

      addElement("proficiency summary", new ProficiencySummary(bezel, bezel*5+30+200, 220, 100), "party management");
      addElement("proficiencies", new Text(0, 0, 10, "Proficiencies", color(0), LEFT), "party management");
      addElement("equipment manager", new EquipmentManager(0, 0, 1), "party management");

      DropDown partyTrainingFocusDropdown = new DropDown(0, 0, 1, 1, color(150), "Training Focus", "strings", 8);
      partyTrainingFocusDropdown.setOptions(jsManager.getProficiencies());
      addElement("party training focus", partyTrainingFocusDropdown, "party management");

      addElement("end turn", new Button(bezel, bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"), "bottom bar");
      addElement("idle party finder", new Button(bezel*2+buttonW, bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Idle Party"), "bottom bar");
      addElement("resource summary", new ResourceSummary(0, 0, 70, resourceNames, startingResources, totals), "bottom bar");
      int resSummaryX = width-((ResourceSummary)(getElement("resource summary", "bottom bar"))).totalWidth();
      addElement("resource expander", new Button(resSummaryX-50, bezel, 30, 30, color(150), color(50), color(0), 10, CENTER, "<"), "bottom bar");
      addElement("turn number", new TextBox(bezel*3+buttonW*2, bezel, -1, buttonH, 14, "Turn 0", color(0, 0, 255), 0), "bottom bar");
      addElement("2d 3d toggle", new ToggleButton(bezel*4+buttonW*3, bezel*2, buttonW/2, buttonH-bezel, color(100), color(0), jsManager.loadBooleanSetting("map is 3d"), "3D View"), "bottom bar");
      addElement("task icons toggle", new ToggleButton(round(bezel*5+buttonW*3.5), bezel*2, buttonW/2, buttonH-bezel, color(100), color(0), true, "Task Icons"), "bottom bar");
      addElement("unit number bars toggle", new ToggleButton(bezel*6+buttonW*4, bezel*2, buttonW/2, buttonH-bezel, color(100), color(0), true, "Unit Bars"), "bottom bar");
      addElement("console", new Console(0, 0, width, height/2, 10), "console");
      //int x, int y, int w, int h, color bgColour, color strokeColour, boolean value, String name

      prevIdle = new ArrayList<Integer[]>();
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error initializing game", e);
      throw e;
    }
  }

  void initialiseBuildings() {
    try {
      LOGGER_MAIN.fine("Initializing buildings");
      JSONObject js;
      int numBuildings = gameData.getJSONArray("buildings").size();
      buildingTypes = new String[numBuildings];
      for (int i=0; i<numBuildings; i++) {
        js = gameData.getJSONArray("buildings").getJSONObject(i);
        buildingTypes[i] = js.getString("id");
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error initializing buildings", e);
      throw e;
    }
  }
  void initialiseTasks() {
    try {
      LOGGER_MAIN.fine("Initializing tasks");
      JSONObject js;
      int numTasks = gameData.getJSONArray("tasks").size();
      taskOutcomes = new float[numTasks][numResources];
      taskCosts = new float[numTasks][numResources];
      tasks = new String[numTasks];
      for (int i=0; i<numTasks; i++) {
        js = gameData.getJSONArray("tasks").getJSONObject(i);
        tasks[i] = js.getString("id");
        if (!js.isNull("production")) {
          for (int r=0; r<js.getJSONArray("production").size(); r++) {
            taskOutcomes[i][jsManager.getResIndex((js.getJSONArray("production").getJSONObject(r).getString("id")))] = js.getJSONArray("production").getJSONObject(r).getFloat("quantity");
          }
        }
        if (!js.isNull("consumption")) {
          for (int r=0; r<js.getJSONArray("consumption").size(); r++) {
            taskCosts[i][jsManager.getResIndex((js.getJSONArray("consumption").getJSONObject(r).getString("id")))] = js.getJSONArray("consumption").getJSONObject(r).getFloat("quantity");
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error initializing tasks", e);
      throw e;
    }
  }
  void initialiseResources() {
    try {
      JSONObject js;
      numResources = gameData.getJSONArray("resources").size();
      resourceNames = new String[numResources];
      startingResources = new float[numResources];
      for (int i=0; i<numResources; i++) {
        js = gameData.getJSONArray("resources").getJSONObject(i);
        resourceNames[i] = js.getString("id");
        JSONObject sr = findJSONObject(gameData.getJSONObject("game options").getJSONArray("starting resources"), resourceNames[i]);
        if (sr != null) {
          startingResources[i] = sr.getFloat("quantity");
        }

        // If resource has specified starting resource on menu
        else if (resourceNames[i].equals("food")) {
          startingResources[i] = jsManager.loadFloatSetting("starting food");
        } else if (resourceNames[i].equals("wood")) {
          startingResources[i] = jsManager.loadFloatSetting("starting wood");
        } else if (resourceNames[i].equals("stone")) {
          startingResources[i] = jsManager.loadFloatSetting("starting stone");
        } else if (resourceNames[i].equals("metal")) {
          startingResources[i] = jsManager.loadFloatSetting("starting metal");
        } else {
          startingResources[i] = 0;
        }
        LOGGER_GAME.fine(String.format("Starting resource: %s = %f", resourceNames[i], startingResources[i]));
      }
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.WARNING, "Error most likely due to modified resources in data.json", e);
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error initializing resources", e);
      throw e;
    }
  }

  void leaveState() {
    map.clearShape();
  }

  JSONArray taskInitialCost(int type) {
    // Find initial cost for task (such as for buildings, 'Build Farm')
    try {
      JSONArray ja = gameData.getJSONArray("tasks").getJSONObject(type).getJSONArray("initial cost");
      return ja;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting task initial cost", e);
      throw e;
    }
  }
  int taskTurns(String task) {
    try {
      JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task);
      if (jo==null) {
        LOGGER_MAIN.warning("invalid task type: "+ task);
        return 0;
      }
      if (jo.isNull("action"))return 0;
      return jo.getJSONObject("action").getInt("turns");
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting task turn length", e);
      throw e;
    }
  }


  Action taskAction(int task) {
    try {
      JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(task).getJSONObject("action");
      if (jo != null)
        return new Action(task, jo.getString("notification"), jo.getInt("turns"), jo.getString("building"), jo.getString("terrain"));
      return null;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting task action", e);
      throw e;
    }
  }


  float[] JSONToCost(JSONArray ja) {
    try {
      float[] costs = new float[numResources];
      if (ja == null) {
        return null;
      }
      for (int i=0; i<ja.size(); i++) {
        costs[jsManager.getResIndex((ja.getJSONObject(i).getString("id")))] = ja.getJSONObject(i).getFloat("quantity");
      }
      return costs;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error converting JSON cost to float cost", e);
      throw e;
    }
  }

  String terrainString(int terrainI) {
    try {
      return gameData.getJSONArray("terrain").getJSONObject(terrainI-1).getString("id");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for terrain string", e);
      return null;
    }
  }

  String buildingString(int buildingI) {
    try {
      if (gameData.getJSONArray("buildings").isNull(buildingI-1)) {
        LOGGER_MAIN.warning("invalid building string "+(buildingI-1));
        return null;
      }
      return gameData.getJSONArray("buildings").getJSONObject(buildingI-1).getString("id");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for building string", e);
      return null;
    }
  }

  String taskString(int task) {
    try {
      if (gameData.getJSONArray("tasks").isNull(task)) {
        LOGGER_MAIN.warning("invalid building string "+(task));
        return null;
      }
      return gameData.getJSONArray("buildings").getJSONObject(task).getString("id");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for building string", e);
      return null;
    }
  }

  float[] buildingCost(int actionType) {
    try {
      float[] a = JSONToCost(taskInitialCost(actionType));
      if (a == null)
        return new float[numResources];
      else
        return a;
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for building cost", e);
      throw e;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting building cost", e);
      throw e;
    }
  }
  boolean postEvent(GameEvent event) {
    try {
      LOGGER_GAME.finer(String.format("Event triggered, player:%d. Cell in question:(%d, %d)", turn, selectedCellX, selectedCellY));
      boolean valid = true;
      // Returns true if event is valid

      battleEstimateManager.refresh();
      if (event instanceof Move) {
        LOGGER_GAME.fine("Move event");
        Move m = (Move)event;
        int x = m.endX;
        int y = m.endY;
        int selectedCellX = m.startX;
        int selectedCellY = m.startY;

        if (x<0 || x>=mapWidth || y<0 || y>=mapHeight) {
          LOGGER_MAIN.warning(String.format("invalid movement outside map boundries: (%d, %d)", x, y));
          valid = false;
        }

        Node[][] nodes = djk(selectedCellX, selectedCellY);

        if (canMove(selectedCellX, selectedCellY)) {
          int sliderVal = round(((Slider)getElement("split units", "party management")).getValue());
          if (sliderVal > 0 && parties[selectedCellY][selectedCellX].getUnitNumber() >= 2 && parties[selectedCellY][selectedCellX].getTask() != JSONIndex(gameData.getJSONArray("tasks"), "Battle")) {
            map.updateMoveNodes(nodes);
            moving = true;
            parties[selectedCellY][selectedCellX].changeUnitNumber(-sliderVal);
            String newPartyName;
            if (parties[selectedCellY][selectedCellX].unitNumber==0) {
              newPartyName = parties[selectedCellY][selectedCellX].id;
            } else {
              newPartyName = nextRollingId(players[turn].name);
            }
            LOGGER_GAME.finer(String.format("Splitting party (id:%s) from party (id:%s) at (%d, %d). Number = %d.", newPartyName, parties[selectedCellY][selectedCellX].id, selectedCellX, selectedCellY, sliderVal));
            splittedParty = new Party(turn, sliderVal, JSONIndex(gameData.getJSONArray("tasks"), "Rest"), parties[selectedCellY][selectedCellX].getMovementPoints(), newPartyName);
          }
        }

        if (splittedParty != null) {
          LOGGER_GAME.finer(String.format("Splitted target for movement: (%d, %d)", x, y));
          splittedParty.target = new int[]{x, y};
          splittedParty.path = getPath(selectedCellX, selectedCellY, x, y, nodes);
          int pathTurns;
          if (selectedCellX==x&&selectedCellY==y) {
            pathTurns = 0;
          } else if (!canMove(selectedCellX, selectedCellY)) {
            pathTurns = getMoveTurns(selectedCellX, selectedCellY, x, y, nodes);
          } else {
            pathTurns = 1+getMoveTurns(selectedCellX, selectedCellY, x, y, nodes);
          }
          splittedParty.setPathTurns(pathTurns);
          Collections.reverse(splittedParty.path);
          splittedParty.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          splittedParty.clearActions();
          ((Text)getElement("turns remaining", "party management")).setText("");
          if (selectedCellX==x&&selectedCellY==y) { // Party moving to same tile
            LOGGER_GAME.finer(String.format("Splitted party put into back tile: (%s, %s)", x, y));
            parties[y][x].changeUnitNumber(splittedParty.getUnitNumber());
            splittedParty = null;
            parties[y][x].clearPath();
          } else {
            moveParty(selectedCellX, selectedCellY, true);
          }
        } else {
          LOGGER_GAME.finer(String.format("Party at cell: (%s, %s) target for movement: (%d, %d)", selectedCellX, selectedCellY, x, y));
          parties[selectedCellY][selectedCellX].target = new int[]{x, y};
          parties[selectedCellY][selectedCellX].path = getPath(selectedCellX, selectedCellY, x, y, nodes);
          int pathTurns;
          if (selectedCellX==x&&selectedCellY==y) {
            pathTurns = 0;
          } else if (!canMove(selectedCellX, selectedCellY)) {
            pathTurns = getMoveTurns(selectedCellX, selectedCellY, x, y, nodes);
          } else {
            pathTurns = 1+getMoveTurns(selectedCellX, selectedCellY, x, y, nodes);
          }
          LOGGER_GAME.finest(String.format("Path turns set to %d", pathTurns));
          parties[selectedCellY][selectedCellX].setPathTurns(pathTurns);
          Collections.reverse(parties[selectedCellY][selectedCellX].path);
          parties[selectedCellY][selectedCellX].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          parties[selectedCellY][selectedCellX].clearActions();
          ((Text)getElement("turns remaining", "party management")).setText("");
          moveParty(selectedCellX, selectedCellY);
        }
        if (parties[selectedCellY][selectedCellX].getUnitNumber() <= 0) {
          parties[selectedCellY][selectedCellX] = null;
        }
      } else if (event instanceof EndTurn) {
        LOGGER_GAME.info("End turn event for turn:"+turn);
        if (!changeTurn)
          changeTurn();
        else
          valid = false;
      } else if (event instanceof ChangeTask) {
        LOGGER_GAME.finer(String.format("Change task event for party at: (%s, %s)", selectedCellX, selectedCellY));
        ChangeTask m = (ChangeTask)event;
        int selectedCellX = m.x;
        int selectedCellY = m.y;
        int task = m.task;
        parties[selectedCellY][selectedCellX].clearPath();
        parties[selectedCellY][selectedCellX].target = null;
        JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(parties[selectedCellY][selectedCellX].getTask());
        if (!jo.isNull("movement points")) {
          //Changing from defending
          parties[selectedCellY][selectedCellX].setMovementPoints(min(parties[selectedCellY][selectedCellX].getMovementPoints()+jo.getInt("movement points"), gameData.getJSONObject("game options").getInt("movement points")));
          LOGGER_GAME.fine("Changing party from defending");
        }
        parties[selectedCellY][selectedCellX].changeTask(task);
        if (parties[selectedCellY][selectedCellX].getTask() == JSONIndex(gameData.getJSONArray("tasks"), "Rest")) {
          parties[selectedCellY][selectedCellX].clearActions();
          ((Text)getElement("turns remaining", "party management")).setText("");
          LOGGER_GAME.finest("Party task is now rest, so turns remaining set to 0 and actions cleared");
        } else {
          moving = false;
          map.cancelMoveNodes();
          LOGGER_GAME.finest("Party task changed so move nodes canceled and moveing set to false");
        }
        jo = gameData.getJSONArray("tasks").getJSONObject(parties[selectedCellY][selectedCellX].getTask());

        if (!jo.isNull("movement points")) { // Check if enough movement points to change task
          if (parties[selectedCellY][selectedCellX].getMovementPoints()-jo.getInt("movement points") >= 0) {
            parties[selectedCellY][selectedCellX].subMovementPoints(jo.getInt("movement points"));
            LOGGER_GAME.finer("Sufficient resources to change task to selected");
          } else {
            LOGGER_GAME.fine("Insufficient movement points to change task to specified");
            parties[selectedCellY][selectedCellX].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          }
        } else if (jo.getString("id").equals("Launch Rocket")) {
          startRocketLaunch();
          LOGGER_GAME.finer("Starting rocket launch");
        } else if (parties[selectedCellY][selectedCellX].getTask()==JSONIndex(gameData.getJSONArray("tasks"), "Produce Rocket")) {
          if (players[turn].resources[jsManager.getResIndex(("rocket progress"))]==-1) {
            players[turn].resources[jsManager.getResIndex(("rocket progress"))] = 0;
            LOGGER_GAME.fine("Rocket progress set to zero becuase party task is to produce rocket");
          }
        } else {
          Action a = taskAction(parties[selectedCellY][selectedCellX].getTask());
          if (a != null) {
            LOGGER_GAME.fine("Adding task action"+a.type);
            float[] co = buildingCost(parties[selectedCellY][selectedCellX].getTask());
            if (sufficientResources(players[turn].resources, co, true)) {
              LOGGER_GAME.finer("Party has sufficient resources to change task to:"+parties[selectedCellY][selectedCellX].getTask());
              parties[selectedCellY][selectedCellX].clearActions();
              ((Text)getElement("turns remaining", "party management")).setText("");
              parties[selectedCellY][selectedCellX].addAction(taskAction(parties[selectedCellY][selectedCellX].getTask()));
              if (sum(co)>0) {
                spendRes(players[turn], co);
                buildings[selectedCellY][selectedCellX] = new Building(buildingIndex("Construction"));
                LOGGER_GAME.fine(String.format("Changing building at cell:(%d, %d) to construction", selectedCellX, selectedCellY));
              }
            } else {
              LOGGER_GAME.finer("Party has insufficient resources to change task to:"+parties[selectedCellY][selectedCellX].getTask());
              parties[selectedCellY][selectedCellX].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            }
          }
        }

        checkTasks();
        int selectedEquipmentType = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
        if (selectedEquipmentType != -1){
          checkEquipment(selectedEquipmentType);
        }
        
      } else if (event instanceof ChangePartyTrainingFocus) {
        int newFocus = ((ChangePartyTrainingFocus)event).newFocus;
        LOGGER_GAME.fine(String.format("Changing party focus for cell (%d, %d) id:%s to '%s'", selectedCellX, selectedCellY, parties[selectedCellY][selectedCellX].getID(), newFocus));
        parties[selectedCellY][selectedCellX].setTrainingFocus(newFocus);
      }
      else if (event instanceof ChangeEquipment){
        int newResID=-1, oldResID=-1;
        
        int equipmentClass = ((ChangeEquipment)event).equipmentClass;
        int newEqupmentType = ((ChangeEquipment)event).newEqupmentType;
        
        if (equipmentClass == -1){
          LOGGER_GAME.warning("No equipment class selected for change equipment event");
        }
        
        if (newEqupmentType != -1){
          newResID = jsManager.getResIndex(jsManager.getEquipmentTypeID(equipmentClass, newEqupmentType));
        }
        if (parties[selectedCellY][selectedCellX].getEquipment(equipmentClass) != -1){
          oldResID = jsManager.getResIndex(jsManager.getEquipmentTypeID(equipmentClass, parties[selectedCellY][selectedCellX].getEquipment(equipmentClass)));
        }
        
        try{
          if (newResID == -1 || players[turn].resources[newResID] >= parties[selectedCellY][selectedCellX].getUnitNumber()) {  // Check sufficient resources for equipment change
            // Subtract equipment resource
            if (newResID != -1){
              players[turn].resources[newResID] -= parties[selectedCellY][selectedCellX].getUnitNumber();
            }
            
            LOGGER_GAME.fine(String.format("Changing equipment type for cell (%d, %d) id:%s class:'%d' new equipment index:'%d'", selectedCellX, selectedCellY, parties[selectedCellY][selectedCellX].getID(), equipmentClass, newEqupmentType));
            
            if (parties[selectedCellY][selectedCellX].getEquipment(equipmentClass) != -1){
              // Recycle equipment if not nothing
              players[turn].resources[oldResID] += parties[selectedCellY][selectedCellX].getUnitNumber();
            }
            
            parties[selectedCellY][selectedCellX].setEquipment(equipmentClass, newEqupmentType);  // Change party equipment
            
            ((EquipmentManager)getElement("equipment manager", "party management")).setEquipment(parties[selectedCellY][selectedCellX].equipment);  // Update equipment manager with new equipment
          }
        }
        catch (ArrayIndexOutOfBoundsException e){
          LOGGER_MAIN.warning("Index problem with equipment change");
          throw e;
        }
      }
      else if (event instanceof DisbandParty){
        int x = ((DisbandParty)event).x;
        int y = ((DisbandParty)event).y;
        parties[y][x] = null;
        LOGGER_GAME.fine(String.format("Party at cell: (%d, %d) disbanded", x, y));
        selectCell(x, y, false);
      }

      if (valid) {
        LOGGER_GAME.finest("Event is valid, so updating things...");
        if (!changeTurn) {
          updateResourcesSummary();

          if (anyIdle(turn)) {
            LOGGER_GAME.finest("There are idle units so highlighting button red");
            ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
          } else {
            LOGGER_GAME.finest("There are no idle units so not highlighting button red");
            ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
          }
        }
      }
      return valid;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error posting event", e);
      throw e;
    }
  }
  boolean anyIdle(int turn) {
    for (int x=0; x<mapWidth; x++) {
      for (int y=0; y<mapWidth; y++) {
        if (parties[y][x] != null && parties[y][x].player == turn && isIdle(x, y)) {
          return true;
        }
      }
    }
    return false;
  }
  void updateSidePanelElementsSizes() {
    // Update the size of elements on the party panel and cell management panel
    sidePanelX = round(width-450*jsManager.loadFloatSetting("gui scale"));
    sidePanelY = bezel;
    sidePanelW = width-sidePanelX-bezel;
    sidePanelH = round(mapElementHeight)-70;
    ((NotificationManager)(getElement("notification manager", "default"))).transform(bezel, bezel, sidePanelW, round(sidePanelH*0.2)-bezel*2);
    ((Button)getElement("move button", "party management")).transform(bezel, round(13*jsManager.loadFloatSetting("text scale")+bezel), 60, 30);
    ((Slider)getElement("split units", "party management")).transform(round(10*jsManager.loadFloatSetting("gui scale")+bezel), round(bezel*3+2*jsManager.loadFloatSetting("text scale")*13), sidePanelW-2*bezel-round(20*jsManager.loadFloatSetting("gui scale")), round(jsManager.loadFloatSetting("text scale")*2*13));
    ((EquipmentManager)getElement("equipment manager", "party management")).transform(bezel, round(bezel*4+4*jsManager.loadFloatSetting("text scale")*13), sidePanelW-bezel*2);
    int equipmentBoxHeight = int(((EquipmentManager)getElement("equipment manager", "party management")).getBoxHeight());
    ((TaskManager)getElement("tasks", "party management")).transform(bezel, round(bezel*5+5*jsManager.loadFloatSetting("text scale")*13+equipmentBoxHeight), sidePanelW/2-int(1.5*bezel), 0);
    ((Text)getElement("task text", "party management")).translate(bezel, round(bezel*5+4*jsManager.loadFloatSetting("text scale")*13+equipmentBoxHeight));
    ((ProficiencySummary)getElement("proficiency summary", "party management")).transform(sidePanelW/2+int(bezel*0.5), round(bezel*5+5*jsManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight, sidePanelW/2-int(1.5*bezel), int(jsManager.getNumProficiencies()*jsManager.loadFloatSetting("text scale")*13));
    ((Text)getElement("proficiencies", "party management")).translate(sidePanelW/2+int(bezel*0.5), round(bezel*5+4*jsManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight);
    ((Text)getElement("turns remaining", "party management")).translate(100+bezel*2, round(13*jsManager.loadFloatSetting("text scale")*2 + bezel*3));
    ((DropDown)getElement("party training focus", "party management")).transform(sidePanelW/2+int(bezel*0.5), round(bezel*6+5*jsManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight+int(jsManager.getNumProficiencies()*jsManager.loadFloatSetting("text scale")*13), sidePanelW/2-int(bezel*(1.5)), int(jsManager.loadFloatSetting("text scale")*13));
    
    float taskRowHeight = ((TaskManager)getElement("tasks", "party management")).getH(new PGraphics());
    
    float partyManagementHeight = round(bezel*7+6*jsManager.loadFloatSetting("text scale")*13+equipmentBoxHeight) + taskRowHeight*10 + jsManager.loadFloatSetting("gui scale")*bezel*6;
    getPanel("land management").transform(sidePanelX, sidePanelY, sidePanelW, round(sidePanelH*0.15));
    getPanel("party management").transform(sidePanelX, sidePanelY+round(sidePanelH*0.15)+bezel, sidePanelW, round(partyManagementHeight)-bezel*3);
    ((Button)getElement("disband button", "party management")).transform(sidePanelW-bezel-80, int(partyManagementHeight-bezel*4-30), 80, 30);
  }


  void makeTaskAvailable(int task) {
    ((TaskManager)getElement("tasks", "party management")).makeAvailable(gameData.getJSONArray("tasks").getJSONObject(task).getString("id"));
  }
  void resetAvailableTasks() {
    ((TaskManager)getElement("tasks", "party management")).resetAvailable();
    ((TaskManager)getElement("tasks", "party management")).resetAvailableButOverBudget();
  }
  void makeAvailableButOverBudget(int task) {
    ((TaskManager)getElement("tasks", "party management")).makeAvailableButOverBudget(gameData.getJSONArray("tasks").getJSONObject(task).getString("id"));
  }
  
  void checkEquipment(int equipmentClass){
    // Check which equipment is available to buy
    LOGGER_GAME.finer("Starting checking available equipment");
    ((EquipmentManager)getElement("equipment manager", "party management")).resetAvailableEquipment();
    float equipmentAvailable = 0;
    if (parties[selectedCellY][selectedCellX].isTurn(turn)) {
      JSONArray equipmentTypesJSON = gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types");
      for (int i = 0; i<equipmentTypesJSON.size(); i++){
        try{
          equipmentAvailable = players[turn].resources[jsManager.getResIndex(equipmentTypesJSON.getJSONObject(i).getString("id"))];
        }
        catch (Exception e){
          LOGGER_MAIN.log(Level.WARNING, String.format("Error finding amount of equipment available class:'%s', type index:'%d'", equipmentClass, i), e);
        }
        if (equipmentAvailable >= parties[selectedCellY][selectedCellX].getUnitNumber()){  // Check if player has sufficient resources for equipment 
          ((EquipmentManager)getElement("equipment manager", "party management")).makeEquipmentAvailable(i);
        }
      }
    }
  }

  void checkTasks() {
    // Check which tasks should be made available
    try {
      LOGGER_GAME.finer("Starting checking available tasks");
      resetAvailableTasks();
      boolean correctTerrain, correctBuilding, enoughResources, enoughMovementPoints;
      JSONObject js;

      if (parties[selectedCellY][selectedCellX].hasActions()) {
        makeTaskAvailable(parties[selectedCellY][selectedCellX].currentAction());
        LOGGER_GAME.finer("Keeping current task available:"+parties[selectedCellY][selectedCellX].currentAction());
      }
      if (parties[selectedCellY][selectedCellX].isTurn(turn)) {
        for (int i=0; i<gameData.getJSONArray("tasks").size(); i++) {
          js = gameData.getJSONArray("tasks").getJSONObject(i);
          if (!js.isNull("terrain"))
            correctTerrain = JSONContainsStr(js.getJSONArray("terrain"), terrainString(terrain[selectedCellY][selectedCellX]));
          else
            correctTerrain = true;
          correctBuilding = false;
          enoughResources = true;
          enoughMovementPoints = true;
          if (!js.isNull("initial cost")) {
            for (int j=0; j<js.getJSONArray("initial cost").size(); j++) {
              JSONObject initialCost = js.getJSONArray("initial cost").getJSONObject(j);
              if (players[turn].resources[jsManager.getResIndex((initialCost.getString("id")))]<(initialCost.getInt("quantity"))) {
                enoughResources = false;
              }
            }
          }
          if (!js.isNull("movement points")) {
            if (parties[selectedCellY][selectedCellX].movementPoints < js.getInt("movement points")) {
              enoughMovementPoints = false;
            }
          }

          if (js.isNull("auto enabled")||!js.getBoolean("auto enabled")) {
            if (js.isNull("buildings")) {
              if (js.getString("id").equals("Demolish") && buildings[selectedCellY][selectedCellX] != null)
                correctBuilding = true;
              else if (!js.getString("id").equals("Demolish"))
                correctBuilding = true;
            } else {
              if (js.getJSONArray("buildings").size() > 0) {
                if (buildings[selectedCellY][selectedCellX] != null)
                  if (buildings[selectedCellY][selectedCellX] != null && JSONContainsStr(js.getJSONArray("buildings"), buildingString(buildings[selectedCellY][selectedCellX].type)))
                    correctBuilding = true;
              } else if (buildings[selectedCellY][selectedCellX] == null) {
                correctBuilding = true;
              }
            }
          }

          if (correctTerrain && correctBuilding) {
            if (enoughResources && enoughMovementPoints) {
              makeTaskAvailable(i);
            } else {
              makeAvailableButOverBudget(i);
            }
          }
        }
      } else {
        makeTaskAvailable(parties[selectedCellY][selectedCellX].getTask());
      }
      ((TaskManager)getElement("tasks", "party management")).select(jsManager.gameData.getJSONArray("tasks").getJSONObject(parties[selectedCellY][selectedCellX].getTask()).getString("id"));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error checking tasks", e);
      throw e;
    }
  }

  boolean UIHovering() {
    //To avoid doing things while hoving over important stuff
    NotificationManager nm = ((NotificationManager)(getElement("notification manager", "default")));
    return !((!getPanel("party management").mouseOver() || !getPanel("party management").visible) && (!getPanel("land management").mouseOver() || !getPanel("land management").visible) &&
      (!nm.moveOver()||nm.empty()));
  }
  
  float getResourceRequirementsAtCell(int x, int y, int resource) {
    float resourceRequirements = 0;
    for (int i = 0; i < tasks.length; i++) {
      if (parties[y][x].getTask() == i) {
        if (jsManager.resourceIsEquipment(resource)){
          // If resource is a type of equipment then check if it is this party's equipment
          int[] equipmentTypeClass = jsManager.getEquipmentTypeClassFromID(jsManager.getResString(resource));
          if (parties[y][x].getEquipment(equipmentTypeClass[0]) == equipmentTypeClass[1]){
            // Add cost for equipment equivilent to number of civilians that could be produced (usually zero, unless resting)
            resourceRequirements += floor(taskOutcomes[i][jsManager.getResIndex("civilians")] * parties[y][x].getUnitNumber());
          }
        } else{
          resourceRequirements += taskCosts[i][resource] * parties[y][x].getUnitNumber();
        }
      }
    }
    return resourceRequirements;
  }

  float[] getTotalResourceRequirements() {
    float[] totalResourceRequirements = new float[numResources];
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        if (parties[y][x] != null) {
          if (parties[y][x].player == turn) {
            for (int resource = 0; resource < numResources; resource++) {
              totalResourceRequirements[resource] += getResourceRequirementsAtCell(x, y, resource);
            }
          }
        }
      }
    }
    return totalResourceRequirements;
  }
  
  float[] getResourceProductivities(float[] totalResourceRequirements) {
    float [] resourceProductivities = new float[numResources];
    for (int i=0; i<numResources; i++) {
      if (totalResourceRequirements[i]==0) {
        resourceProductivities[i] = 1;
      } else {
        resourceProductivities[i] = min(1, players[turn].resources[i]/totalResourceRequirements[i]);
      }
    }
    return resourceProductivities;
  }
  
  float[] getResourceProductivities() {
    return getResourceProductivities(getTotalResourceRequirements());
  }
  
  float getProductivityAtCell(int x, int y, float[] resourceProductivities) {
    float productivity = 1;
    for (int task = 0; task<tasks.length; task++) {
      if (parties[y][x].getTask() == task) {
        for (int resource = 0; resource < numResources; resource++) {
          if (getResourceRequirementsAtCell(x, y, resource) > 0) {
            if (resource == 0 && players[turn].resources[resource] == 0) {
              productivity = min(productivity, resourceProductivities[resource] + 0.5);
            } else {
              productivity = min(productivity, resourceProductivities[resource]);
            }
          }
        }
      }
    }
    return productivity;
  }
  
  float getProductivityAtCell(int x, int y) {
    return getProductivityAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
  }
  
  float[] resourceProductionAtCell(int x, int y, float[] resourceProductivities) {
    float [] production = new float[numResources];
    if (parties[y][x] != null) {
      if (parties[y][x].player == turn) {
        float productivity = getProductivityAtCell(x, y, resourceProductivities);
        for (int task = 0; task < tasks.length; task++) {
          if (parties[y][x].getTask()==task) {
            for (int resource = 0; resource < numResources; resource++) {
              production[resource] = taskOutcomes[task][resource] * productivity * (float) parties[y][x].getUnitNumber();
            }
          }
        }
      }
    }
    return production; 
  }
  
  float[] resourceProductionAtCell(int x, int y) {
    return resourceProductionAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
  }
  
  float[] getTotalResourceProductions(float[] resourceProductivities) {
    float[] amount = new float[resourceNames.length];
    for (int x = 0; x < mapWidth; x++) {
      for (int y = 0; y < mapHeight; y++) {
        for (int res = 0; res < numResources; res++) {
          amount[res]+=resourceProductionAtCell(x, y, resourceProductivities)[res];
        }
      }
    }
    return amount;
  }
  
  float[] getTotalResourceProductions() {
    return getTotalResourceProductions(getResourceProductivities(getTotalResourceRequirements()));
  }
  
  float[] getResourceConsumptionAtCell(int x, int y, float[] resourceProductivities) {
    float [] consumption = new float[numResources];
    if (parties[y][x] != null) {
      if (parties[y][x].player == turn) {
        float productivity = getProductivityAtCell(x, y, resourceProductivities);
        for (int task = 0; task <tasks.length; task++) {
          if (parties[y][x].getTask() == task) {
            for (int resource = 0; resource < numResources; resource++) {
              if (resource == 0) {
                consumption[resource] += max(getResourceRequirementsAtCell(x, y, resource) * productivity, taskCosts[task][resource]*parties[y][x].getUnitNumber());
              } else {
                consumption[resource] += getResourceRequirementsAtCell(x, y, resource) * productivity;
              }
            }
          }
        }
      }
    }
    return consumption;
  }
  
  float[] getResourceConsumptionAtCell(int x, int y) {
    return getResourceConsumptionAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
  }
  
  float[] getTotalResourceConsumptions(float[] resourceProductivities) {
    float[] amount = new float[resourceNames.length];
    for (int x = 0; x < mapWidth; x++) {
      for (int y = 0; y < mapHeight; y++) {
        for (int res = 0; res < numResources; res++) {
          amount[res] += getResourceConsumptionAtCell(x, y, resourceProductivities)[res];
        }
      }
    }
    return amount;
  }
  
  float[] getTotalResourceConsumptions(){
    return getTotalResourceConsumptions(getResourceProductivities(getTotalResourceRequirements()));
  }
  
  float[] getTotalResourceChanges(float[] grossResources, float[] costsResources) {
    float[] amount = new float[resourceNames.length];
    for (int res = 0; res < numResources; res++) {
      amount[res] = grossResources[res] - costsResources[res];
    }
    return amount;
  }
  
  float[] getResourceChangesAtCell(int x, int y, float[] resourceProductivities){
    float[] amount = new float[resourceNames.length];
    for (int res = 0; res < numResources; res++) {
      amount[res] = resourceProductionAtCell(x, y, resourceProductivities)[res] - getResourceConsumptionAtCell(x, y, resourceProductivities)[res];
    }
    return amount;
  }
  
  float[] getResourceChangesAtCell(int x, int y){
    return getResourceChangesAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
  }
  
  byte[] getResourceWarnings(){
    return getResourceWarnings(getResourceProductivities(getTotalResourceRequirements()));
  }
  
  byte[] getResourceWarnings(float[] productivities){
    byte[] warnings = new byte[productivities.length];
    for (int i = 0; i < productivities.length; i++) {
      if (productivities[i] == 0){
        warnings[i] = 2;
      } else if (productivities[i] < 1) {
        warnings[i] = 1;
      }
    }
    return warnings;
  }
  
  void updateResourcesSummary(){
    float[] totalResourceRequirements = getTotalResourceRequirements();
    float[] resourceProductivities = getResourceProductivities(totalResourceRequirements);
    
    float[] gross = getTotalResourceProductions(resourceProductivities);
    float[] costs = getTotalResourceConsumptions(resourceProductivities);
    this.totals = getTotalResourceChanges(gross, costs);
    
    ResourceSummary rs = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
    rs.updateNet(totals);
    rs.updateStockpile(players[turn].resources);
    rs.updateWarnings(getResourceWarnings(resourceProductivities));
  }

  void updateResources(float[] resourceProductivities) {
    for (int y = 0; y < mapHeight; y++) {
      for (int x = 0; x < mapWidth; x++) {
        if (parties[y][x] != null) {
          if (parties[y][x].player == turn) {
            for (int task = 0; task < tasks.length; task++) {
              if (parties[y][x].getTask()==task) {
                for (int resource = 0; resource < numResources; resource++) {
                  if (resource != jsManager.getResIndex(("civilians"))) {
                    if (tasks[task] == "Produce Rocket") {
                      resource = jsManager.getResIndex(("rocket progress"));
                    }
                    
                    players[turn].resources[resource] += max(getResourceChangesAtCell(x, y, resourceProductivities)[resource], -players[turn].resources[resource]);
                    if (tasks[task] == "Produce Rocket") {
                      break;
                    }
                  } else if (resourceProductivities[jsManager.getResIndex(("food"))] < 1) {
                    float lost = (1 - resourceProductivities[jsManager.getResIndex(("food"))]) * taskOutcomes[task][resource] * parties[y][x].getUnitNumber();
                    parties[y][x].setUnitNumber(floor(parties[y][x].getUnitNumber() - lost));
                    if (parties[y][x].getUnitNumber() == 0) {
                      notificationManager.post("Party Starved", x, y, turnNumber, turn);
                      LOGGER_GAME.info(String.format("Party starved at cell:(%d, %d) player:%s", x, y, turn));
                    } else {
                      notificationManager.post(String.format("Party Starving - %d lost", ceil(lost)), x, y, turnNumber, turn);
                      LOGGER_GAME.fine(String.format("Party Starving - %d lost at  cell: (%d, %d) player:%s", ceil(lost), x, y, turn));
                    }
                  } else {
                    int prev = parties[y][x].getUnitNumber();
                    parties[y][x].setUnitNumber(floor(prev + getResourceChangesAtCell(x, y, resourceProductivities)[resource]));
                    if (prev != jsManager.loadIntSetting("party size") && parties[y][x].getUnitNumber() == jsManager.loadIntSetting("party size") && parties[y][x].task == JSONIndex(gameData.getJSONArray("tasks"), "Super Rest")) {
                      notificationManager.post("Party Full", x, y, turnNumber, turn);
                      LOGGER_GAME.fine(String.format("Party full at  cell: (%d, %d) player:%s", x, y, turn));
                    }
                  }
                }
              }
            }
            if (parties[y][x].getUnitNumber() == 0) {
              parties[y][x] = null;
              LOGGER_GAME.finest(String.format("Setting party at cell:(%s, %s) to null becuase it has no units left in it", x, y));
            }
          }
        }
      }
    }
    if (players[turn].resources[jsManager.getResIndex(("rocket progress"))] > 1000) {
      //display indicator saying rocket produced
      LOGGER_GAME.info("Rocket produced");
      for (int y = 0; y < mapHeight; y++) {
        for (int x = 0; x < mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x].player == turn) {
              if (parties[y][x].getTask() == JSONIndex(gameData.getJSONArray("tasks"), "Produce Rocket")) {
                notificationManager.post("Rocket Produced", x, y, turnNumber, turn);
                parties[y][x].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
                buildings[y][x].image_id = 1;
              }
            }
          }
        }
      }
    }
  }

  void processParties() {
    for (int y=0; y<mapHeight; y++) {
      for (int x=0; x<mapWidth; x++) {
        if (parties[y][x] != null) {
          if (parties[y][x].player == turn) {
            Action action = parties[y][x].progressAction();
            if (action != null) {
              if (!(action.type==JSONIndex(gameData.getJSONArray("tasks"), "Construction Mid")) && !(action.type==JSONIndex(gameData.getJSONArray("tasks"), "Construction End")))
                notificationManager.post(action.notification, x, y, turnNumber, turn);
              if (action.building != null) {
                if (action.building.equals("")) {
                  buildings[y][x] = null;
                  LOGGER_GAME.info(String.format("Building cleared at cell: (%d, %d)", x, y));
                } else {
                  LOGGER_GAME.info(String.format("Action completed building %s, at cell (%d, %d)", action.building, x, y));
                  buildings[y][x] = new Building(buildingIndex(action.building));
                  if (buildings[y][x].type == buildingIndex("Quarry")) {
                    LOGGER_GAME.fine("Quarry type detected so changing terrain...");
                    //map.setHeightsForCell(x, y, jsManager.loadFloatSetting("water level"));
                    terrain[y][x] = terrainIndex("quarry site");
                    map.replaceMapStripWithReloadedStrip(y);
                  }
                }
              }
              if (action.terrain != null) {
                if (terrain[y][x] == terrainIndex("forest")) { // Cut down forest
                  LOGGER_GAME.info("Cutting down forest");
                  players[turn].resources[jsManager.getResIndex(("wood"))]+=100;
                  map.removeTreeTile(x, y);
                }
                terrain[y][x] = terrainIndex(action.terrain);
              }
              if (action.type==JSONIndex(gameData.getJSONArray("tasks"), "Construction Mid")) {
                LOGGER_GAME.finer("Action reached mid phase so changing building to mid");
                buildings[y][x].image_id = 1;
                action = null;
              } else if (action.type==JSONIndex(gameData.getJSONArray("tasks"), "Construction End")) {
                LOGGER_GAME.finer("Action reached end phase so changing building to end");
                buildings[y][x].image_id = 2;
                action = null;
              }
            }
            if (action != null) {
              LOGGER_MAIN.finer("Action is null so clearing actions and setting task to rest");
              parties[y][x].clearCurrentAction();
              parties[y][x].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            }
            moveParty(x, y);
          } else {
            if (parties[y][x].player==2) {
              int player = ((Battle) parties[y][x]).party1.player;
              int otherPlayer = ((Battle) parties[y][x]).party2.player;
              parties[y][x] = ((Battle)parties[y][x]).doBattle();
              if (parties[y][x].player != 2) {
                LOGGER_GAME.fine(String.format("Battle ended at:(%d, %d) winner=&s", x, y, str(parties[y][x].player+1)));
                notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, player);
                notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
              }
            }
          }
        }
      }
    }
  }

  void turnChange() {
    try {
      LOGGER_GAME.finer(String.format("Turn changing - current player = %s, next player = %s", turn, (turn+1)%players.length));
      notificationManager.dismissAll();
      processParties();
      updateResources(getResourceProductivities(getTotalResourceRequirements()));
      partyMovementPointsReset();
      LOGGER_GAME.finer("Loading other player camera positions");
      float blockSize;
      if (map.isZooming()) {
        blockSize = map.getTargetZoom();
      } else {
        blockSize = map.getZoom();
      }
      float tempX=0, tempY=0;
      if (jsManager.loadBooleanSetting("map is 3d")) {
        tempX = (map.getTargetOffsetX()+width/2)/((Map3D)map).blockSize;
        tempY = (map.getTargetOffsetY()+height/2)/((Map3D)map).blockSize;
      } else {
        tempX = (width/2-map.getTargetOffsetX()-((Map2D)map).xPos)/((Map2D)map).targetBlockSize;
        tempY = (height/2-map.getTargetOffsetY()-((Map2D)map).yPos)/((Map2D)map).targetBlockSize;
      }
      players[turn].saveSettings(tempX, tempY, blockSize, selectedCellX, selectedCellY, cellSelected);
      turn = (turn + 1)%2;
      map.generateFog(turn);
      players[turn].loadSettings(this, map);
      changeTurn = false;
      TextBox t = ((TextBox)(getElement("turn number", "bottom bar")));
      t.setColour(players[turn].colour);
      t.setText("Turn "+turnNumber);
      updateResourcesSummary();
      notificationManager.turnChange(turn);

      if (turn==0)
        turnNumber++;

      if (anyIdle(turn)) {
        LOGGER_GAME.finest("Idle party set to red becuase idle parties found");
        ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
      } else {
        LOGGER_GAME.finest("Idle party set to grey becuase no idle parties found");
        ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error changing turn", e);
      throw e;
    }
  }

  boolean checkForPlayerWin() {
    if (winner == -1) {
      boolean player1alive = false;
      boolean player2alive = false;

      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x].player == 2) {
              player1alive = true;
              player2alive = true;
            } else if (parties[y][x].player == 1) {
              player2alive = true;
            } else if (parties[y][x].player == 0) {
              player1alive = true;
            }
          }
        }
      }
      if (!player1alive) {
        winner = 1;
        LOGGER_GAME.info("Player 2 wins");
      } else if (!player2alive) {
        winner = 0;
        LOGGER_GAME.info("Player 1 wins");
      } else {
        return false;
      }
    }
    Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
    winnerMessage.setText(winnerMessage.text.replace("/w", str(winner+1)));
    return true;
  }


  void drawPanels() {
    checkElementOnTop();
    if (rocketLaunching) {
      handleRocket();
    }
    // Draw the panels in reverse order (highest in the list are drawn last so appear on top)
    for (int i=panels.size()-1; i>=0; i--) {
      if (panels.get(i).visible) {
        panels.get(i).draw();
      }
    }
    //background(100);
    if (changeTurn) {
      turnChange();
    }

    if (map.isMoving()) {
      refreshTooltip();
    }

    gameUICanvas.beginDraw();
    gameUICanvas.clear();
    gameUICanvas.pushStyle();

    if (tooltip.visible&&tooltip.attacking) {
      int x = floor(map.scaleXInv());
      int y = floor(map.scaleYInv());
      if (0<=x&&x<mapWidth&&0<=y&&y<mapHeight && parties[y][x] != null) {
        BigDecimal chance = battleEstimateManager.getEstimate(selectedCellX, selectedCellY, x, y, splitUnitsNum());
        tooltip.setAttacking(chance);
      }
    }

    if (players[0].resources[jsManager.getResIndex(("rocket progress"))]!=-1||players[1].resources[jsManager.getResIndex(("rocket progress"))]!=-1) {
      drawRocketProgressBar(gameUICanvas);
    }
    if (cellSelected) {
      if (getPanel("land management").visible) {
        drawCellManagement(gameUICanvas);
      }
      if (parties[selectedCellY][selectedCellX] != null && getPanel("party management").visible)
        drawPartyManagement(gameUICanvas);
    }
    gameUICanvas.endDraw();
    gameUICanvas.popStyle();
    image(gameUICanvas, 0, 0);
    ResourceSummary resSum = ((ResourceSummary)getElement("resource summary", "bottom bar"));
    if (resSum.pointOver()) {
      String resource = resSum.getResourceAt(mouseX, mouseY);
      HashMap <String, Float> tasksMap = new HashMap <String, Float>();
      for (int y = 0; y < mapHeight; y++) {
        for (int x = 0; x < mapWidth; x++) {
          if (parties[y][x]!=null) {
            int taskId = parties[y][x].getTask();
          }
        }
      }
    }

    if (checkForPlayerWin()) {
      this.getPanel("end screen").visible = true;
    }
  }
  void partyMovementPointsReset() {
    for (int y=0; y<mapHeight; y++) {
      for (int x=0; x<mapWidth; x++) {
        if (parties[y][x] != null) {
          if (parties[y][x].player != 2) {
            parties[y][x].setMovementPoints(gameData.getJSONObject("game options").getInt("movement points"));
          }
        }
      }
    }
  }
  void changeTurn() {
    changeTurn = true;
  }
  boolean sufficientResources(float[] available, float[] required) {
    for (int i=0; i<numResources; i++) {
      if (available[i] < required[i]) {
        return false;
      }
    }
    return true;
  }
  boolean sufficientResources(float[] available, float[] required, boolean flash) {
    ResourceSummary rs = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
    boolean t = true;
    for (int i=0; i<numResources; i++) {
      if (available[i] < required[i] && !buildingString(i).equals("rocket progress")) {
        t = false;
        rs.flash(i);
      }
    }
    return t;
  }
  void spendRes(Player player, float[] required) {
    for (int i=0; i<numResources; i++) {
      player.resources[i] -= required[i];
      LOGGER_GAME.fine(String.format("Player spending: %f %s", required[i], resourceNames[i]));
    }
  }
  void reclaimRes(Player player, float[] required) {
    //reclaim half cost of building
    for (int i=0; i<numResources; i++) {
      player.resources[i] += required[i]/2;
      LOGGER_GAME.fine(String.format("Player reclaiming (half of building cost): %d %s", required[i], resourceNames[i]));
    }
  }
  int[] newPartyLoc() {
    // Unused
    try {
      ArrayList<int[]> locs = new ArrayList<int[]>();
      locs.add(new int[]{selectedCellX+1, selectedCellY});
      locs.add(new int[]{selectedCellX-1, selectedCellY});
      locs.add(new int[]{selectedCellX, selectedCellY+1});
      locs.add(new int[]{selectedCellX, selectedCellY-1});
      Collections.shuffle(locs);
      for (int i=0; i<4; i++) {
        if (parties[locs.get(i)[1]][locs.get(i)[0]] == null && terrain[locs.get(i)[1]][locs.get(i)[0]] != 1) {
          return locs.get(i);
        }
      }
      return null;
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error with indices in new party loc", e);
      return null;
    }
  }
  boolean canMove(int x, int y) {
    float points;
    int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};

    if (splittedParty!=null) {
      points = splittedParty.getMovementPoints();
      for (int[] n : mvs) {
        if (points >= cost(x+n[0], y+n[1], x, y)) {
          return true;
        }
      }
    } else {
      points = parties[y][x].getMovementPoints();
      for (int[] n : mvs) {
        if (points >= cost(x+n[0], y+n[1], x, y)) {
          return true;
        }
      }
    }
    return false;
  }

  boolean inPrevIdle(int x, int y) {
    for (int i=0; i<prevIdle.size(); i++) {
      if (prevIdle.get(i)[0] == x && prevIdle.get(i)[1] == y) {
        return true;
      }
    }
    return false;
  }
  void clearPrevIdle() {
    prevIdle.clear();
  }
  boolean isIdle(int x, int y) {
    try {
      return (parties[y][x].task == JSONIndex(gameData.getJSONArray("tasks"), "Rest") && canMove(x, y) && (parties[y][x].path==null||parties[y][x].path!=null&&parties[y][x].path.size()==0));
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.WARNING, "Error with indices in new party loc", e);
      return false;
    }
  }

  int[] findIdle(int player) {
    try {
      int[] backup = {-1, -1};
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null && parties[y][x].player == player && isIdle(x, y)) {
            if (inPrevIdle(x, y)) {
              backup = new int[]{x, y};
            } else {
              prevIdle.add(new Integer[]{x, y});
              return new int[]{x, y};
            }
          }
        }
      }
      clearPrevIdle();
      if (backup[0] == -1) {
        return backup;
      } else {
        return findIdle(player);
      }
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error with indices out of bounds in new party loc", e);
      return null;
    }
  }

  void saveGame() {
    float blockSize;
    float x=0, y=0;
    if (jsManager.loadBooleanSetting("map is 3d")) {
      blockSize = 0.66*exp(pow(0.9, 2.8*log(map.getZoom()/100000)));
      x = (map.getFocusedX()+width/2)/((Map3D)map).blockSize;
      y = (map.getFocusedY()+height/2)/((Map3D)map).blockSize;
    } else {
      if (map.isZooming()) {
        blockSize = map.getTargetZoom();
      } else {
        blockSize = map.getZoom();
      }
      x = (width/2-map.getFocusedX()-((Map2D)map).xPos)/((Map2D)map).blockSize;
      y = (height/2-map.getFocusedY()-((Map2D)map).yPos)/((Map2D)map).blockSize;
    }
    players[turn].saveSettings(x, y, blockSize, selectedCellX, selectedCellY, cellSelected);
    ((BaseMap)map).saveMap("saves/"+loadingName, this.turnNumber, this.turn, this.players);
  }

  void elementEvent(ArrayList<Event> events) {
    for (Event event : events) {
      if (event.type == "clicked") {
        if (event.id == "idle party finder") {
          int[] t = findIdle(turn);
          if (t[0] != -1) {
            selectCell(t[0], t[1], false);
            map.targetCell(t[0], t[1], 64);
          }
        } else if (event.id == "end turn") {
          postEvent(new EndTurn());
        } else if (event.id == "move button") {
          if (parties[selectedCellY][selectedCellX].player == turn) {
            moving = !moving;
            if (moving) {
              map.updateMoveNodes(djk(selectedCellX, selectedCellY));
            } else {
              map.cancelMoveNodes();
            }
          }
        } else if (event.id == "resource expander") {
          ResourceSummary r = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
          Button b = ((Button)(getElement("resource expander", "bottom bar")));
          r.toggleExpand();
          b.transform(width-r.totalWidth()-50, bezel, 30, 30);
          if (b.getText() == ">")
            b.setText("<");
          else
            b.setText(">");
        } else if (event.id.equals("end game button")) {
          jsManager.writeSettings();
          newState = "menu";
        } else if (event.id.equals("main menu button")) {
          // old save place
          jsManager.writeSettings();
          newState = "menu";
        } else if (event.id.equals("desktop button")) {
          jsManager.writeSettings();
          quitGame();
        } else if (event.id.equals("resume button")) {
          getPanel("pause screen").visible = false;
          getPanel("save screen").visible = false;
          // Enable map
          getElement("2dmap", "default").active = true;
          getElement("3dmap", "default").active = true;
        } else if (event.id.equals("save as button")) {
          // Show the save menu
          getPanel("save screen").visible = !getPanel("save screen").visible;
          if (loadingName == null) {
            loadingName = ((BaseFileManager)getElement("saving manager", "save screen")).getNextAutoName(); // Autogen name
          }
          ((TextEntry)getElement("save namer", "save screen")).setText(loadingName);
        } else if (event.id.equals("save button")) {
          loadingName = ((TextEntry)getElement("save namer", "save screen")).getText();
          saveGame();
          ((BaseFileManager)getElement("saving manager", "save screen")).loadSaveNames();
        } else if (event.id.equals("disband button")){
          postEvent(new DisbandParty(selectedCellX, selectedCellY));
        }
      }
      if (event.type.equals("valueChanged")) {
        if (event.id.equals("tasks")) {
          postEvent(new ChangeTask(selectedCellX, selectedCellY, JSONIndex(jsManager.gameData.getJSONArray("tasks"), ((TaskManager)getElement("tasks", "party management")).getSelected())));
        } else if (event.id.equals("unit number bars toggle")) {
          map.setDrawingUnitBars(((ToggleButton)(getElement("unit number bars toggle", "bottom bar"))).getState());
          LOGGER_MAIN.fine("Unit number bars visibility changed");
        } else if (event.id.equals("task icons toggle")) {
          map.setDrawingTaskIcons(((ToggleButton)(getElement("task icons toggle", "bottom bar"))).getState());
          LOGGER_MAIN.fine("Task icons bars visibility changed");
        } else if (event.id.equals("2d 3d toggle")) {
          // Save the game for toggle between 2d and 3d
          LOGGER_MAIN.info("Toggling between 2d and 3d, so saving now to 'Autosave.dat'");
          loadingName = "Autosave.dat";
          saveGame();
          jsManager.saveSetting("map is 3d", ((ToggleButton)(getElement("2d 3d toggle", "bottom bar"))).getState());
          LOGGER_MAIN.info("Reloading map in new dimension");
          reloadGame();
          LOGGER_MAIN.fine("Finished reloading map");
        } else if (event.id.equals("saving manager")) {
          loadingName = ((BaseFileManager)getElement("saving manager", "save screen")).selectedSaveName();
          LOGGER_MAIN.fine("Changing selected save name to: "+loadingName);
          ((TextEntry)getElement("save namer", "save screen")).setText(loadingName);
        } else if (event.id.equals("party training focus")) {
          postEvent(new ChangePartyTrainingFocus(selectedCellX, selectedCellY, ((DropDown)getElement("party training focus", "party management")).getOptionIndex()));
        }
        else if (event.id.equals("equipment manager")){
          for (int [] equipmentChange : ((EquipmentManager)getElement("equipment manager", "party management")).getEquipmentToChange()){
            postEvent(new ChangeEquipment(equipmentChange[0], equipmentChange[1]));
          }
        }
      }
      if (event.type.equals("dropped")){
        if (event.id.equals("equipment manager")){
          int selectedEquipmentType = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
          if (selectedEquipmentType != -1){
            checkEquipment(selectedEquipmentType);
          }
        }
      }
      else if (event.type.equals("notification selected")) {
        int x = notificationManager.lastSelected.x, y = notificationManager.lastSelected.y;
        LOGGER_GAME.fine(String.format("Notification '%s', cell selected: (%d, %d)", notificationManager.lastSelected.name, x, y));
        map.targetCell(x, y, 100);
        selectCell(x, y, false);
      }
    }
  }
  void deselectCell() {
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

  void moveParty(int px, int py) {
    moveParty(px, py, false);
  }

  void moveParty(int px, int py, boolean splitting) {
    try {
      boolean hasMoved = false;
      int startPx = px;
      int startPy = py;
      Party p;

      if (splitting) {
        p = splittedParty;
      } else {
        p = parties[py][px];
      }


      boolean cellFollow = (px==selectedCellX && py==selectedCellY);
      boolean stillThere = true;
      if (p.target == null || p.path == null)
        return;
      int tx = p.target[0];
      int ty = p.target[1];
      if (px == tx && py == ty) {
        if (splitting) {
          if (parties[py][px] == null) {
            parties[py][px] = p; 
            LOGGER_GAME.fine(String.format("Putting party '%s' back into empty cell because target is same as currrent location:(%d, %d)", parties[py][px].getID(), px, py));
          } else {
            LOGGER_GAME.fine(String.format("Merging party '%s' back into empty cell becuase target is same as currrent location:(%d, %d) changing unit number by:%d", parties[py][px].getID(), px, py, p.getUnitNumber()));
            parties[py][px].changeUnitNumber(p.getUnitNumber());
          }
        }
        p.clearPath();
        return;
      }

      ArrayList <int[]> path = p.path;
      int i=0;
      boolean moved = false;
      for (int node=1; node<path.size(); node++) {
        int cost = cost(path.get(node)[0], path.get(node)[1], px, py);
        if (p.getMovementPoints() >= cost) {
          hasMoved = true;
          if (parties[path.get(node)[1]][path.get(node)[0]] == null) {
            // empty cell
            p.subMovementPoints(cost);
            parties[path.get(node)[1]][path.get(node)[0]] = p;
            LOGGER_GAME.finer(String.format("Moving party with id:%s to (%d, %d) which is an empty cell, costing %d movement points. Movement points remaining:%d", p.getID(), path.get(node)[0], path.get(node)[1], cost, p.getMovementPoints()));
            if (splitting) {
              splittedParty = null;
              splitting = false;
            } else {
              parties[py][px] = null;
            }
            px = path.get(node)[0];
            py = path.get(node)[1];
            p = parties[py][px];
            if (!moved) {
              p.moved();
              moved = true;
            }
          } else if (path.get(node)[0] != px || path.get(node)[1] != py) {
            p.clearPath();
            if (parties[path.get(node)[1]][path.get(node)[0]].player == turn) {
              // merge parties
              notificationManager.post("Parties Merged", (int)path.get(node)[0], (int)path.get(node)[1], turnNumber, turn);
              int movementPoints = min(parties[path.get(node)[1]][path.get(node)[0]].getMovementPoints(), p.getMovementPoints()-cost);
              int overflow = parties[path.get(node)[1]][path.get(node)[0]].changeUnitNumber(p.getUnitNumber()); // Units left over after merging
              LOGGER_GAME.fine(String.format("Parties merged at (%d, %d) from party with id: %s to party with id:%s. Movement points of combined parties:%d. Units transfered:%d", 
                (int)path.get(node)[0], (int)path.get(node)[1], p.getID(), parties[path.get(node)[1]][path.get(node)[0]].getID(), movementPoints, p.getUnitNumber()));

              if (cellFollow) {
                selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
                stillThere = false;
              }
              if (splitting) {
                splittedParty = null;
                splitting = false;
              } else {
                parties[py][px] = null;
              }
              if (overflow>0) {
                if (parties[path.get(node-1)[1]][path.get(node-1)[0]]==null) {
                  p.setUnitNumber(overflow);
                  parties[path.get(node-1)[1]][path.get(node-1)[0]] = p;
                  LOGGER_GAME.finer(String.format("Setting units in party with id:%s to %d as there was overflow", p.getID(), p.getUnitNumber()));
                } else {
                  parties[path.get(node-1)[1]][path.get(node-1)[0]].changeUnitNumber(overflow);
                }
              }
              parties[path.get(node)[1]][path.get(node)[0]].setMovementPoints(movementPoints);
            } else if (parties[path.get(node)[1]][path.get(node)[0]].player == 2) {
              // merge cells battle
              notificationManager.post("Battle Reinforced", (int)path.get(node)[0], (int)path.get(node)[1], turnNumber, turn);
              int overflow = ((Battle) parties[path.get(node)[1]][path.get(node)[0]]).changeUnitNumber(turn, p.getUnitNumber());
              LOGGER_GAME.fine(String.format("Battle reinforced at cell:(%d, %d). Merging party id:%s. Overflow:%d", (int)path.get(node)[0], (int)path.get(node)[1], p.getID(), overflow));
              if (cellFollow) {
                selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
                stillThere = false;
              }
              if (splitting) {
                splittedParty = null;
                splitting = false;
              } else {
                parties[py][px] = null;
              }
              if (overflow>0) {
                if (parties[path.get(node-1)[1]][path.get(node-1)[0]]==null) {
                  p.setUnitNumber(overflow);
                  parties[path.get(node-1)[1]][path.get(node-1)[0]] = p;
                  LOGGER_GAME.finer(String.format("Setting units in party with id:%s to %d as there was overflow", p.getID(), p.getUnitNumber()));
                } else {
                  parties[path.get(node-1)[1]][path.get(node-1)[0]].changeUnitNumber(overflow);
                }
              }
            } else {
              int x, y;
              x = path.get(node)[0];
              y = path.get(node)[1];
              int otherPlayer = parties[y][x].player;
              LOGGER_GAME.fine(String.format("Battle started. Attacker party id:%s, defender party id:%s. Cell: (%d, %d)", p.getID(), parties[y][x].getID(), x, y));
              notificationManager.post("Battle Started", x, y, turnNumber, turn);
              notificationManager.post("Battle Started", x, y, turnNumber, otherPlayer);
              p.subMovementPoints(cost);
              parties[y][x] = new Battle(p, parties[y][x], ".battle");
              parties[y][x] = ((Battle)parties[y][x]).doBattle();
              if (parties[y][x].player != 2) {
                notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, turn);
                notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
                LOGGER_GAME.fine(String.format("Battle ended at cell: (%d, %d). Units remaining:", x, y, parties[y][x].getUnitNumber()));
              }
              if (cellFollow) {
                selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
                stillThere = false;
              }
              if (splitting) {
                splittedParty = null;
                splitting = false;
              } else {
                parties[py][px] = null;
              }
              if (buildings[path.get(node)[1]][path.get(node)[0]]!=null&&buildings[path.get(node)[1]][path.get(node)[0]].type==0) {
                // If there is a building under constuction, then delete it when battle
                buildings[path.get(node)[1]][path.get(node)[0]] = null;
                LOGGER_GAME.fine(String.format("Building in constuction destroyed due to battle at cell: (%d, %d)", path.get(node)[0], path.get(node)[1]));
              }
            }
            break;
          }
          i++;
        } else {
          p.path = new ArrayList(path.subList(i, path.size()));
          break;
        }
        if (tx==px&&ty==py) {
          p.clearPath();
        }
      }

      if (cellFollow&&stillThere) {
        selectCell((int)px, (int)py, false);
      }

      // if the party didnt move then put the splitted party back into the cell
      if (startPx == px && startPy == py && !hasMoved) {
        parties[py][px] = p;
      }
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error with indices out of bounds when moving party at cell:%s, %s", px, py), e);
    }

    map.generateFog(turn);
  }
  int getMoveTurns(int startX, int startY, int targetX, int targetY, Node[][] nodes) {
    int movementPoints;
    if (parties[startY][startX] != null)
      movementPoints = round(parties[startY][startX].getMovementPoints()); 
    else if (splittedParty != null)
      movementPoints = round(splittedParty.getMovementPoints());
    else
      return -1; 

    int turns = 0;
    ArrayList <int[]> path = getPath(startX, startY, targetX, targetY, nodes);
    Collections.reverse(path);
    for (int node=1; node<path.size(); node++) {
      int cost = cost(path.get(node)[0], path.get(node)[1], path.get(node-1)[0], path.get(node-1)[1]);
      if (movementPoints < cost) {
        turns += 1;
        movementPoints = gameData.getJSONObject("game options").getInt("movement points");
      }
      movementPoints -= cost;
    }
    return turns;
  }
  int splitUnitsNum() {
    return round(((Slider)getElement("split units", "party management")).getValue());
  }
  void refreshTooltip() {
    if (!getPanel("pause screen").visible) {
      if (((TaskManager)getElement("tasks", "party management")).moveOver() && getPanel("party management").visible) {
        tooltip.setTask(((TaskManager)getElement("tasks", "party management")).findMouseOver(), players[turn].resources, parties[selectedCellY][selectedCellX].getMovementPoints());
        tooltip.show();
      } else if (((Text)getElement("turns remaining", "party management")).mouseOver()&& getPanel("party management").visible) {
        tooltip.setTurnsRemaining();
        tooltip.show();
      } else if (((Button)getElement("move button", "party management")).mouseOver()&& getPanel("party management").visible) {
        tooltip.setMoveButton();
        tooltip.show();
      } else if (map.mouseOver()) {
        map.doUpdateHoveringScale();
        if (moving && !UIHovering()) {
          Node [][] nodes = map.getMoveNodes();
          int x = floor(map.scaleXInv()); 
          int y = floor(map.scaleYInv());
          if (selectedCellX == x && selectedCellY == y) {
            tooltip.hide();
            map.cancelPath();
          } else if (x < mapWidth && y<mapHeight && x>=0 && y>=0 && nodes[y][x] != null) {
            if (parties[selectedCellY][selectedCellX] != null) {
              map.updatePath(getPath(selectedCellX, selectedCellY, x, y, map.getMoveNodes()));
            }
            if (parties[y][x]==null) {
              //Moving into empty tile
              int turns = getMoveTurns(selectedCellX, selectedCellY, x, y, nodes);
              int cost = nodes[y][x].cost;
              boolean splitting = splitUnitsNum()!=parties[selectedCellY][selectedCellX].getUnitNumber();
              tooltip.setMoving(turns, splitting, cost, jsManager.loadBooleanSetting("map is 3d"));
              tooltip.show();
            } else {
              if (parties[y][x].player == turn) {
                //merge parties
                tooltip.setMerging();
                tooltip.show();
              } else {
                //Attack
                BigDecimal chance = battleEstimateManager.getEstimate(selectedCellX, selectedCellY, x, y, splitUnitsNum());
                tooltip.setAttacking(chance);
                tooltip.show();
              }
            }
          }
        } else {
          map.cancelPath();
          tooltip.hide();
        }
      } else {
        map.cancelPath();
        tooltip.hide();
      }
      map.setActive(!UIHovering());
    }
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    refreshTooltip();
    if (button == RIGHT) {
      if (eventType == "mousePressed") {
        if (parties[selectedCellY][selectedCellX] != null && parties[selectedCellY][selectedCellX].player == turn && cellSelected && !UIHovering()) {
          if (map.mouseOver()) {
            if (moving) {
              map.cancelPath();
              moving = false;
              map.cancelMoveNodes();
            } else {
              moving = true;
              map.updateMoveNodes(djk(selectedCellX, selectedCellY));
              refreshTooltip();
            }
          }
        }
      }
      if (eventType == "mouseReleased") {
        if (parties[selectedCellY][selectedCellX] != null && parties[selectedCellY][selectedCellX].player == turn && !UIHovering()) { 
          if (moving) {
            int x = floor(map.scaleXInv());
            int y = floor(map.scaleYInv());
            if (0<=x&&x<mapWidth&&0<=y&&y<mapHeight) {
              postEvent(new Move(selectedCellX, selectedCellY, x, y));
            }
            map.cancelPath();
            moving = false;
            map.cancelMoveNodes();
          }
        }
      }
    }
    if (button == LEFT) {
      if (eventType == "mousePressed") {
        mapClickPos = new int[]{mouseX, mouseY, millis()};
      }
      if (eventType == "mouseReleased" && mapClickPos != null && sqrt(pow(mapClickPos[0] - mouseX, 2) + pow(mapClickPos[1] - mouseY, 2))<MOUSEPRESSTOLERANCE && millis() - mapClickPos[2] < CLICKHOLD) { // Custom mouse click
        mapClickPos = null;
        if (activePanel == "default" && !UIHovering()) {
          if (map.mouseOver()) {
            if (moving) {
              //int x = floor(map.scaleXInv(mouseX));
              //int y = floor(map.scaleYInv(mouseY));
              //postEvent(new Move(selectedCellX, selectedCellY, x, y));
              //map.cancelPath();
              if (mousePressed) {
                map.cancelPath();
                moving = false;
                map.cancelMoveNodes();
              } else {
                int x = floor(map.scaleXInv());
                int y = floor(map.scaleYInv());
                if (0<=x&&x<mapWidth&&0<=y&&y<mapHeight) {
                  postEvent(new Move(selectedCellX, selectedCellY, x, y));
                }
                map.cancelPath();
                moving = false;
                map.cancelMoveNodes();
              }
            } else {
              if (floor(map.scaleXInv())==selectedCellX&&floor(map.scaleYInv())==selectedCellY&&cellSelected) {
                deselectCell();
              } else if (!cinematicMode) {
                selectCell();
              }
            }
          }
        }
      }
    }
    return new ArrayList<String>();
  }
  void selectCell() {
    // select cell based on mouse pos
    int x = floor(map.scaleXInv());
    int y = floor(map.scaleYInv());
    selectCell(x, y, false);
  }

  boolean cellInBounds(int x, int y) {
    return 0<=x&&x<mapWidth&&0<=y&&y<mapHeight;
  }

  void selectCell(int x, int y, boolean raw) {
    // raw means from mouse position
    deselectCell();
    if (raw) {
      selectCell();
    } else if (cellInBounds(x, y)) {
      LOGGER_GAME.finer(String.format("Cell selected at (%d, %d) which is in bounds", x, y));
      tooltip.hide();
      selectedCellX = x;
      selectedCellY = y;
      cellSelected = true;
      map.selectCell(selectedCellX, selectedCellY);
      //map.setWidth(round(width-bezel*2-400));
      getPanel("land management").setVisible(true);
      if (parties[selectedCellY][selectedCellX] != null && (parties[selectedCellY][selectedCellX].isTurn(turn) || jsManager.loadBooleanSetting("show all party managements"))) {
        if (parties[selectedCellY][selectedCellX].getTask() != JSONIndex(gameData.getJSONArray("tasks"), "Battle") && parties[selectedCellY][selectedCellX].isTurn(turn)) {
          ((Slider)getElement("split units", "party management")).show();
        } else {
          ((Slider)getElement("split units", "party management")).hide();
        }
        getPanel("party management").setVisible(true);
        if (parties[selectedCellY][selectedCellX].getUnitNumber() <= 1) {
          ((Slider)getElement("split units", "party management")).hide();
        } else {
          ((Slider)getElement("split units", "party management")).setScale(1, parties[selectedCellY][selectedCellX].getUnitNumber(), parties[selectedCellY][selectedCellX].getUnitNumber(), 1, parties[selectedCellY][selectedCellX].getUnitNumber()/2);
        }
        if (parties[selectedCellY][selectedCellX].player == 1) {
          partyManagementColour = color(170, 30, 30);
          getPanel("party management").setColour(color(220, 70, 70));
        } else {
          partyManagementColour = color(0, 0, 150);
          getPanel("party management").setColour(color(70, 70, 220));
        }
        checkTasks();
        int selectedEquipmentType = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
        if (selectedEquipmentType != -1){
          checkEquipment(selectedEquipmentType);
        }
        ((EquipmentManager)getElement("equipment manager", "party management")).setEquipment(parties[selectedCellY][selectedCellX].equipment);
        updatePartyManagementProficiencies();
        updateCurrentPartyTrainingFocus();
      }
    }
  }

  void updatePartyManagementProficiencies() {
    // Update proficiencies with those for current party
    ((ProficiencySummary)getElement("proficiency summary", "party management")).setProficiencies(parties[selectedCellY][selectedCellX].getProficiencies());
  }

  void updateCurrentPartyTrainingFocus() {
    int trainingFocus = parties[selectedCellY][selectedCellX].getTrainingFocus();
    ((DropDown)getElement("party training focus", "party management")).setValue(jsManager.indexToProficiencyDisplayName(trainingFocus));
  }

  void drawPartyManagement(PGraphics panelCanvas) {
    Panel pp = getPanel("party management");
    panelCanvas.pushStyle();
    panelCanvas.fill(partyManagementColour);
    panelCanvas.rect(sidePanelX, pp.y, sidePanelW, 13*jsManager.loadFloatSetting("text scale"));
    panelCanvas.fill(255);
    panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER, TOP);
    panelCanvas.text("Party Management", sidePanelX+sidePanelW/2, pp.y);

    panelCanvas.fill(0);
    panelCanvas.textAlign(LEFT, CENTER);
    panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
    float barY = sidePanelY + 13*jsManager.loadFloatSetting("text scale") + sidePanelH*0.15 + bezel*2;
    if (jsManager.loadBooleanSetting("show party id")) {
      panelCanvas.text("Party id: "+parties[selectedCellY][selectedCellX].id, 120+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    }
    panelCanvas.text("Movement Points Remaining: "+parties[selectedCellY][selectedCellX].getMovementPoints(turn) + "/"+gameData.getJSONObject("game options").getInt("movement points"), 120+sidePanelX, barY);
    barY += 13*jsManager.loadFloatSetting("text scale");
    if (jsManager.loadBooleanSetting("show all party managements")&&parties[selectedCellY][selectedCellX].player==2) {
      String t1 = ((Battle)parties[selectedCellY][selectedCellX]).party1.id;
      String t2 = "Units: "+((Battle)parties[selectedCellY][selectedCellX]).party1.getUnitNumber() + "/" + jsManager.loadIntSetting("party size");
      float offset = max(panelCanvas.textWidth(t1+" "), panelCanvas.textWidth(t2+" "));
      panelCanvas.text(t1, 120+sidePanelX, barY);
      panelCanvas.text(((Battle)parties[selectedCellY][selectedCellX]).party2.id, 120+sidePanelX+offset, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
      panelCanvas.text(t2, 120+sidePanelX, barY);
      panelCanvas.text("Units: "+((Battle)parties[selectedCellY][selectedCellX]).party2.getUnitNumber() + "/" + jsManager.loadIntSetting("party size"), 120+sidePanelX+offset, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    } else {
      panelCanvas.text("Units: "+parties[selectedCellY][selectedCellX].getUnitNumber(turn) + "/" + jsManager.loadIntSetting("party size"), 120+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    }
    if (parties[selectedCellY][selectedCellX].pathTurns > 0) {
      ((Text)getElement("turns remaining", "party management")).setText("Turns Remaining: "+ parties[selectedCellY][selectedCellX].pathTurns);
    } else if (parties[selectedCellY][selectedCellX].actions.size() > 0 && parties[selectedCellY][selectedCellX].actions.get(0).initialTurns > 0) {
      ((Text)getElement("turns remaining", "party management")).setText("Turns Remaining: "+parties[selectedCellY][selectedCellX].turnsLeft() + "/"+round(parties[selectedCellY][selectedCellX].calcTurns(parties[selectedCellY][selectedCellX].actions.get(0).initialTurns)));
    }
  }

  String resourcesList(float[] resources) {
    String returnString = "";
    boolean notNothing = false;
    for (int i=0; i<numResources; i++) {
      if (resources[i]>0) {
        returnString += roundDp(""+resources[i], 1)+ " " +resourceNames[i]+ ", ";
        notNothing = true;
      }
    }
    if (!notNothing)
      returnString += "Nothing/Unknown";
    else if (returnString.length()-2 > 0)
      returnString = returnString.substring(0, returnString.length()-2);
    return returnString;
  }

  void drawCellManagement(PGraphics panelCanvas) {
    panelCanvas.pushStyle();
    panelCanvas.fill(0, 150, 0);
    panelCanvas.rect(sidePanelX, sidePanelY, sidePanelW, 13*jsManager.loadFloatSetting("text scale"));
    panelCanvas.fill(255);
    panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER, TOP);
    panelCanvas.text("Land Management", sidePanelX+sidePanelW/2, sidePanelY);

    panelCanvas.fill(0);
    panelCanvas.textAlign(LEFT, TOP);
    float barY = sidePanelY + 13*jsManager.loadFloatSetting("text scale");
    if (jsManager.loadBooleanSetting("show cell coords")) {
      panelCanvas.text(String.format("Cell reference: %s, %s", selectedCellX, selectedCellY), 5+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    }
    panelCanvas.text("Cell Type: "+gameData.getJSONArray("terrain").getJSONObject(terrain[selectedCellY][selectedCellX]-1).getString("display name"), 5+sidePanelX, barY);
    barY += 13*jsManager.loadFloatSetting("text scale");
    if (buildings[selectedCellY][selectedCellX] != null) {
      if (buildings[selectedCellY][selectedCellX].type != 0)
        panelCanvas.text("Building: "+buildingTypes[buildings[selectedCellY][selectedCellX].type-1], 5+sidePanelX, barY);
      else
        panelCanvas.text("Building: Construction Site", 5+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    }
    float[] resourceProductivities = getResourceProductivities(getTotalResourceRequirements());
    float[] production = resourceProductionAtCell(selectedCellX, selectedCellY, resourceProductivities);
    float[] consumption = getResourceConsumptionAtCell(selectedCellX, selectedCellY, resourceProductivities);
    String pl = resourcesList(production);
    String cl = resourcesList(consumption);
    panelCanvas.fill(0);
    if (!pl.equals("Nothing/Unknown")) {
      panelCanvas.text("Producing: "+pl, 5+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    }
    if (!cl.equals("Nothing/Unknown")) {
      panelCanvas.fill(255, 0, 0);
      panelCanvas.text("Consuming: "+cl, 5+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
    }
  }

  String getResourceString(float amount) {
    String tempString = roundDp(""+amount, 1);
    if (amount >= 0) {
      fill(0);
      tempString = "+"+tempString;
    } else {
      fill(255, 0, 0);
    }
    return tempString;
  }

  void drawRocketProgressBar(PGraphics panelCanvas) {
    int x, y, w, h;
    String progressMessage;
    boolean both = players[0].resources[jsManager.getResIndex(("rocket progress"))] != 0 && players[1].resources[jsManager.getResIndex(("rocket progress"))] != 0;
    if (players[0].resources[jsManager.getResIndex(("rocket progress"))] ==0 && players[1].resources[jsManager.getResIndex(("rocket progress"))] == 0)return;
    int progress = int(players[turn].resources[jsManager.getResIndex(("rocket progress"))]);
    color fillColour;
    if (progress == 0) {
      progress = int(players[(turn+1)%2].resources[jsManager.getResIndex(("rocket progress"))]);
      progressMessage = "";
      fillColour = playerColours[(turn+1)%2];
    } else {
      fillColour = playerColours[turn];
      if (progress>=1000) {
        progressMessage = "Rocket Progress: Completed";
      } else {
        progressMessage = "Rocket Progress: "+str(progress)+"/1000";
      }
    }
    if (both) {
      x = round(width*0.25);
      y = round(height*0.05);
      w = round(width/2);
      h = round(height*0.015);
      panelCanvas.fill(200);
      panelCanvas.stroke(100);
      panelCanvas.rect(x, y, w, h);
      panelCanvas.noStroke();
      progress = int(players[0].resources[jsManager.getResIndex(("rocket progress"))]);
      w = round(min(w, w*progress/1000));
      panelCanvas.fill(playerColours[0]);
      panelCanvas.rect(x, y, w, h);
      y = round(height*0.065);
      w = round(width/2);
      panelCanvas.fill(200);
      panelCanvas.stroke(100);
      panelCanvas.rect(x, y, w, h);
      panelCanvas.noStroke();
      progress = int(players[1].resources[jsManager.getResIndex(("rocket progress"))]);
      w = round(min(w, w*progress/1000));
      panelCanvas.fill(playerColours[1]);
      panelCanvas.rect(x, y, w, h);
      y = round(height*0.05);
    } else {
      x = round(width*0.25);
      y = round(height*0.05);
      w = round(width/2);
      h = round(height*0.03);
      panelCanvas.fill(200);
      panelCanvas.stroke(100);
      panelCanvas.rect(x, y, w, h);
      panelCanvas.noStroke();
      w = round(min(w, w*progress/1000));
      panelCanvas.fill(fillColour);
      panelCanvas.rect(x, y, w, h);
    }
    panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER, BOTTOM);
    panelCanvas.fill(200);
    int tw = ceil((panelCanvas.textWidth(progressMessage)));
    panelCanvas.rect(width/2 -tw/2, y-10*jsManager.loadFloatSetting("text scale"), tw, 10*jsManager.loadFloatSetting("text scale"));
    panelCanvas.fill(0);
    panelCanvas.text(progressMessage, width/2, y);
  }

  ArrayList<String> keyboardEvent(String eventType, char _key) {
    if (eventType == "keyPressed" && _key==0 && keyCode == VK_F12) {
      getPanel("console").visible = !getPanel("console").visible;
      getElement("2dmap", "default").active = !getPanel("console").visible;
      getElement("3dmap", "default").active = !getPanel("console").visible;
    }
    if (eventType == "keyPressed" && _key == ESC) {
      getPanel("pause screen").visible = !getPanel("pause screen").visible;
      tooltip.hide();
      if (getPanel("pause screen").visible) {
        ((BaseFileManager)getElement("saving manager", "save screen")).loadSaveNames();
        // Disable map
        getElement("2dmap", "default").active = false;
        getElement("3dmap", "default").active = false;
      } else {
        getPanel("save screen").visible = false;
        // Enable map
        getElement("2dmap", "default").active = true;
        getElement("3dmap", "default").active = true;
      }
    }
    if (!getPanel("pause screen").visible&&!getPanel("console").visible) {
      refreshTooltip();
      if (eventType == "keyTyped") {
        if (_key == ' '&&!cinematicMode) {
          postEvent(new EndTurn());
        } else if (_key == 'i'&&!cinematicMode) {
          LOGGER_GAME.fine("Finding idle party as 'i' key pressed");
          int[] t = findIdle(turn);
          if (t[0] != -1) {
            selectCell(t[0], t[1], false);
            map.targetCell(t[0], t[1], 64);
          }
        }
      }
    }
    return new ArrayList<String>();
  }
  void enterState() {
    initialiseResources();
    reloadGame();
  }

  void reloadGame() {
    LOGGER_MAIN.fine("Reloading game...");
    mapWidth = jsManager.loadIntSetting("map size");
    mapHeight = jsManager.loadIntSetting("map size");
    updateSidePanelElementsSizes();

    // Default on for showing task icons and unit bars

    clearPrevIdle();
    ((Text)getElement("turns remaining", "party management")).setText("");
    ((Panel)getPanel("end screen")).visible = false;
    getPanel("save screen").visible = false;
    // Enable map
    getElement("2dmap", "default").active = true;
    getElement("3dmap", "default").active = true;

    Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
    winnerMessage.setText("Winner: player /w");

    if (jsManager.loadBooleanSetting("map is 3d")) {
      LOGGER_MAIN.finer("Map is 3d");
      map = (Map3D)getElement("3dmap", "default");
      ((Map3D)getElement("3dmap", "default")).visible = true;
      ((Map2D)getElement("2dmap", "default")).visible = false;
      getElement("unit number bars toggle", "bottom bar").visible = true;
      getElement("task icons toggle", "bottom bar").visible = true;
      getElement("unit number bars toggle", "bottom bar").active = true;
      getElement("task icons toggle", "bottom bar").active = true;
      ((Map3D)map).reset();
    } else {
      LOGGER_MAIN.finer("Map is 2d");
      map = (Map2D)getElement("2dmap", "default");
      ((Map3D)getElement("3dmap", "default")).visible = false;
      ((Map2D)getElement("2dmap", "default")).visible = true;
      getElement("unit number bars toggle", "bottom bar").visible = false;
      getElement("task icons toggle", "bottom bar").visible = false;
      getElement("unit number bars toggle", "bottom bar").active = false;
      getElement("task icons toggle", "bottom bar").active = false;
      ((Map2D)map).reset();
    }
    if (loadingName != null) {
      LOGGER_MAIN.finer("Loading save");
      MapSave mapSave = ((BaseMap)map).loadMap("saves/"+loadingName, resourceNames.length);
      terrain = mapSave.terrain;
      buildings = mapSave.buildings;
      parties = mapSave.parties;
      mapWidth = mapSave.mapWidth;
      mapHeight = mapSave.mapHeight;
      this.turnNumber = mapSave.startTurn;
      this.turn = mapSave.startPlayer;
      this.players = mapSave.players;
      if (jsManager.loadBooleanSetting("map is 3d")) {
        map.targetCell(int(this.players[turn].cameraCellX), int(this.players[turn].cameraCellY), this.players[turn].blockSize);
        ((Map3D)map).focusedX = ((Map3D)map).targetXOffset;
        ((Map3D)map).focusedY = ((Map3D)map).targetYOffset;
        ((Map3D)map).zoom = this.players[turn].blockSize;
      } else {
        map.targetCell(int(this.players[turn].cameraCellX), int(this.players[turn].cameraCellY), this.players[turn].blockSize);
        ((Map2D)map).mapXOffset = ((Map2D)map).targetXOffset;
        ((Map2D)map).mapYOffset = ((Map2D)map).targetYOffset;
        ((Map2D)map).blockSize = this.players[turn].blockSize;
      }
    } else {
      LOGGER_MAIN.finer("Creating new map");
      ((BaseMap)map).generateMap(mapWidth, mapHeight);
      terrain = ((BaseMap)map).terrain;
      buildings = ((BaseMap)map).buildings;
      parties = ((BaseMap)map).parties;
      PVector[] playerStarts = generateStartingParties();
      float[] conditions2 = map.targetCell((int)playerStarts[1].x, (int)playerStarts[1].y, jsManager.loadIntSetting("starting block size"));
      players[1] = new Player((int)playerStarts[1].x, (int)playerStarts[1].y, jsManager.loadIntSetting("starting block size"), startingResources.clone(), color(255, 0, 0), "Player 2  ");
      float[] conditions1 = map.targetCell((int)playerStarts[0].x, (int)playerStarts[0].y, jsManager.loadIntSetting("starting block size"));
      players[0] = new Player((int)playerStarts[0].x, (int)playerStarts[0].y, jsManager.loadIntSetting("starting block size"), startingResources.clone(), color(0, 0, 255), "Player 1  ");
      turn = 0;
      turnNumber = 0;
      deselectCell();
    }
    ((Console)getElement("console", "console")).giveObjects(map, players, this);

    map.generateFog(turn);
    battleEstimateManager = new BattleEstimateManager(parties);
    //for(int i=0;i<NUMOFBUILDINGTYPES;i++){
    //  buildings[(int)playerStarts[0].y][(int)playerStarts[0].x+i] = new Building(1+i);
    //}
    tooltip.hide();
    winner = -1;
    TextBox t = ((TextBox)(getElement("turn number", "bottom bar")));
    t.setColour(players[turn].colour);
    t.setText("Turn "+turnNumber);

    updateResourcesSummary();
    getPanel("pause screen").visible = false;

    notificationManager.reset();

    // If first turn start players looking at right places
    if (turnNumber == 0) {
      for (int i = players.length-1; i >= 0; i--) {
        int[] t1 = findIdle(i);
        float[] targetOffsets = map.targetCell(t1[0], t1[1], 64);
        players[i].saveSettings(t1[0], t1[1], 64, selectedCellX, selectedCellY, false);
      }
    }

    if (anyIdle(turn)) {
      ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
    } else {
      ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
    }
    map.generateShape();

    map.setDrawingTaskIcons(true);
    map.setDrawingUnitBars(true);
    LOGGER_MAIN.finer("Finished reloading game");
  }
  int cost(int x, int y, int prevX, int prevY) {
    float mult = 1;
    if (x!=prevX && y!=prevY) {
      mult = 1.41;
    }
    if (0<=x && x<jsManager.loadIntSetting("map size") && 0<=y && y<jsManager.loadIntSetting("map size")) {
      return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]-1).getInt("movement cost")*mult);
    }
    //Not a valid location
    return -1;
  }

  ArrayList<int[]> getPath(int startX, int startY, int targetX, int targetY, Node[][] nodes) {
    ArrayList<int[]> returnNodes = new ArrayList<int[]>();
    returnNodes.add(new int[]{targetX, targetY});
    int[] curNode = {targetX, targetY};
    if (nodes[curNode[1]][curNode[0]] == null) {
      return returnNodes;
    }
    while (curNode[0] != startX || curNode[1] != startY) {
      returnNodes.add(new int[]{nodes[curNode[1]][curNode[0]].prevX, nodes[curNode[1]][curNode[0]].prevY});
      curNode = returnNodes.get(returnNodes.size()-1);
    }
    return returnNodes;
  }
  Node[][] djk(int x, int y) {
    int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
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
    while (curMinNodes.size() > 0) {
      nodes[curMinNodes.get(0)[1]][curMinNodes.get(0)[0]].fixed = true;
      for (int[] mv : mvs) {
        int nx = curMinNodes.get(0)[0]+mv[0];
        int ny = curMinNodes.get(0)[1]+mv[1];
        if (0 <= nx && nx < w && 0 <= ny && ny < h) {
          boolean sticky = parties[ny][nx] != null;
          int newCost = cost(nx, ny, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
          int prevCost = curMinCosts.get(0);
          int totalNewCost = prevCost+newCost;
          if (totalNewCost < gameData.getJSONObject("game options").getInt("movement points")*100) {
            if (nodes[ny][nx] == null) {
              nodes[ny][nx] = new Node(totalNewCost, false, curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
              if (!sticky) {
                curMinNodes.add(search(curMinCosts, totalNewCost), new int[]{nx, ny});
                curMinCosts.add(search(curMinCosts, totalNewCost), totalNewCost);
              }
            } else if (!nodes[ny][nx].fixed) {
              if (totalNewCost < nodes[ny][nx].cost) {
                nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
                nodes[ny][nx].setPrev(curMinNodes.get(0)[0], curMinNodes.get(0)[1]);
                if (!sticky) {
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
  int search(ArrayList<int[]> nodes, int x, int y) {
    for (int i=0; i < nodes.size(); i++) {
      if (nodes.get(i)[0] == x && nodes.get(i)[1] == y) {
        return i;
      }
    }
    return -1;
  }
  int search(ArrayList<Integer> costs, float target) {
    //int upper = nodes.size();
    //int lower = 0;
    //while(nodes.get(lower)[2] > target || target > nodes.get(upper)[2]){

    //}
    //return lower;

    //linear search for now
    for (int i=0; i < costs.size(); i++) {
      if (costs.get(i) > target) {
        return i;
      }
    }
    return costs.size();
  }
  boolean startInvalid(PVector p1, PVector p2) {
    if (p1.dist(p2)<mapWidth/8||((BaseMap)map).isWater(int(p1.x), int(p1.y))||((BaseMap)map).isWater(int(p2.x), int(p2.y))) {
      return true;
    }
    return false;
  }
  PVector generatePartyPosition() {
    return new PVector(int(random(0, mapWidth)), int(random(0, mapHeight)));
  }

  PVector[] generateStartingParties() {
    LOGGER_GAME.fine("Generating starting positions");
    PVector player1 = generatePartyPosition();
    PVector player2 = generatePartyPosition();
    int counter = 0;
    while (startInvalid(player1, player2)&&counter<100) {
      counter++;
      player1 = generatePartyPosition();
      player2 = generatePartyPosition();
    }
    LOGGER_GAME.fine(String.format("Player 1 party positition: (%f, %f)", player1.x, player1.y));
    LOGGER_GAME.fine(String.format("Player 2 party positition: (%f, %f)", player2.x, player2.y));
    if (loadingName == null) {
      parties[(int)player1.y][(int)player1.x] = new Party(0, 100, JSONIndex(gameData.getJSONArray("tasks"), "Rest"), gameData.getJSONObject("game options").getInt("movement points"), "Player 1   #0");
      parties[(int)player2.y][(int)player2.x] = new Party(1, 100, JSONIndex(gameData.getJSONArray("tasks"), "Rest"), gameData.getJSONObject("game options").getInt("movement points"), "Player 2   #0");
    }
    return  new PVector[]{player1, player2};
  }

  void enterCinematicMode() {
    LOGGER_GAME.finer("Entering cinematic mode...");
    cinematicMode = true;
    getPanel("bottom bar").setVisible(false);
    getPanel("land management").setVisible(false);
    getPanel("party management").setVisible(false);
    ((BaseMap)map).cinematicMode = true;
  }
  void leaveCinematicMode() {
    LOGGER_GAME.finer("Leaving cinematic mode...");
    cinematicMode = false;
    getPanel("bottom bar").setVisible(true);
    if (cellSelected) {
      getPanel("land management").setVisible(true);
      if (parties[selectedCellY][selectedCellX] != null && parties[selectedCellY][selectedCellX].isTurn(turn)) {
        getPanel("party management").setVisible(true);
      }
    }
    ((BaseMap)map).cinematicMode = false;
  }

  void startRocketLaunch() {
    LOGGER_GAME.finer("Starting rocket launch");
    rocketVelocity = new PVector(0, 0, 0);
    rocketBehaviour = int(random(10));
    buildings[selectedCellY][selectedCellX].image_id=0;
    rocketLaunching = true;
    rocketPosition = new PVector(selectedCellX, selectedCellY, 0);
    map.enableRocket(rocketPosition, rocketVelocity);
    enterCinematicMode();
    rocketStartTime = millis();
  }
  void handleRocket() {
    float t = float(millis()-rocketStartTime)/1000;
    if (rocketBehaviour > 6) {
      rocketVelocity.z = 10*(exp(t)-1)/(exp(t)+1);
      if (rocketPosition.z>mapHeight) {
        rocketLaunchEnd();
      }
    } else {
      rocketVelocity.x = 0.5*t;
      rocketVelocity.z = 3*t-pow(t, 2);
      if (rocketPosition.z<0) {
        rocketLaunchEnd();
      }
    }
    rocketVelocity.div(frameRate);
    rocketPosition.add(rocketVelocity);
  }
  void rocketLaunchEnd() {
    map.disableRocket();
    rocketLaunching = false;
    if (rocketBehaviour > 6) {
      winner = turn;
    } else {
      players[turn].resources[jsManager.getResIndex(("rocket progress"))] = 0;
      parties[selectedCellY][selectedCellX].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
    }
    leaveCinematicMode();
  }

  String nextRollingId(String playerName) {
    int maxN = 0;
    for (int y=0; y<mapHeight; y++) {
      for (int x=0; x<mapWidth; x++) { 
        try {
          if (parties[y][x] != null && parties[y][x].id.substring(0, 10).equals(playerName) && parties[y][x].id.charAt(11)=='#') {
            int n = Integer.valueOf(parties[y][x].id.substring(12));
            maxN = max(maxN, n);
          }
        } 
        catch (NumberFormatException e) {
          LOGGER_GAME.severe("error finding next party id while looking at party "+parties[y][x].id);
        }
      }
    }
    return String.format("%s #%d", playerName, maxN+1);
  }
}
