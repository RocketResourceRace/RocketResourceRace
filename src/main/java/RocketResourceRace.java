import processing.core.*;
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*;

import java.io.*;
import java.math.BigDecimal;
import processing.sound.*; 
import java.util.Arrays; 
import java.nio.file.Files; 
import java.nio.file.Paths; 
import java.nio.file.Path; 
import java.util.logging.*; 
import static com.jogamp.newt.event.KeyEvent.*; 
import java.text.SimpleDateFormat; 
import java.util.Date; 
import java.util.Comparator; 
import java.util.PriorityQueue; 
import java.util.Collections; 
import java.nio.ByteBuffer; 

import java.util.HashMap; 
import java.util.ArrayList;

public class RocketResourceRace extends PApplet {











  String resourcesRoot = "data/";


  // Create loggers
  final Logger LOGGER_MAIN = Logger.getLogger("RocketResourceRaceMain"); // Most logs belong here INCLUDING EXCEPTION LOGS. Also I have put saving logs here rather than game
  final Logger LOGGER_GAME = Logger.getLogger("RocketResourceRaceGame"); // For game algorithm related logs (not exceptions here, just things like party moving or ai making decision)
  final Level FILELOGLEVEL = Level.FINEST;


  String activeState;
  HashMap<String, State> states;
  int lastClickTime = 0;
  final int DOUBLECLICKWAIT = 500;
  final String LETTERSNUMBERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890/\\_ ";
  HashMap<String, SoundFile> sfx;
  int prevT;
  HashMap<Integer, PFont> fonts;
  PShader toon;
  String loadingName;

  JSONManager jsManager;
  JSONObject gameData;

  // Event-driven methods
  public void mouseClicked() {
    mouseEvent("mouseClicked", mouseButton);
    doubleClick();
  }
  public void mouseDragged() {
    mouseEvent("mouseDragged", mouseButton);
  }
  public void mouseMoved() {
    mouseEvent("mouseMoved", mouseButton);
  }
  public void mousePressed() {
    mouseEvent("mousePressed", mouseButton);
  }
  public void mouseReleased() {
    mouseEvent("mouseReleased", mouseButton);
  }
  public void mouseWheel(MouseEvent event) {
    mouseEvent("mouseWheel", mouseButton, event);
  }
  public void keyPressed() {
    keyboardEvent("keyPressed", key);
  }
  public void keyReleased() {
    keyboardEvent("keyReleased", key);
  }
  public void keyTyped() {
    keyboardEvent("keyTyped", key);
  }

  public float between(float lower, float v, float upper) {
    return max(min(upper, v), lower);
  }

  public int brighten(int old, int off) {
    return color(between(0, red(old)+off, 255), between(0, green(old)+off, 255), between(0, blue(old)+off, 255));
  }

  public String roundDp(String val, int dps) {
    return (new BigDecimal(""+val).divide(new BigDecimal("1"), dps, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString();
  }

  public String roundDpTrailing(String val, int dps){
    return (new BigDecimal(""+val).divide(new BigDecimal("1"), dps, BigDecimal.ROUND_HALF_EVEN)).toPlainString();
  }

  /*
  Gets the index of the JSONObject with 'id' set to id in the JSONArray j
  */
  public int JSONIndex(JSONArray j, String id) {
    for (int i=0; i<j.size(); i++) {
      if (j.getJSONObject(i).getString("id").equals(id)) {
        return i;
      }
    }
    LOGGER_MAIN.warning(String.format("Invalid JSON index '%s'", id));
    return -1;
  }

  /*
  Gets the JSONObject with 'id' set to id in the JSONArray j
  */
  public JSONObject findJSONObject(JSONArray j, String id) {
    try {
      for (int i=0; i<j.size(); i++) {
        if (j.getJSONObject(i).getString("id").equals(id)) {
          return j.getJSONObject(i);
        }
      }
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error finding object in JSON array, with id:'%s'", id), e);
      throw e;
    }
    return null;
  }

  public boolean JSONContainsStr(JSONArray j, String id) {
    try {
      if (id == null || j == null)
        return false;
      for (int i=0; i<j.size(); i++) {
        if (j.getString(i).equals(id)) {
          return true;
        }
      }
      return false;
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error finding string in JSON array, '%s'", id), e);
      throw e;
    }
  }
  /*
  Handles double clicks
  */
  public void doubleClick() {
    if (millis() - lastClickTime < DOUBLECLICKWAIT) {
      mouseEvent("mouseDoubleClicked", mouseButton);
      lastClickTime = 0;
    } else {
      lastClickTime = millis();
    }
  }

  public float sigmoid(float x) {
    return 1-2/(exp(x)+1);
  }

  public void mouseEvent(String eventType, int button) {
    getActiveState()._mouseEvent(eventType, button);
  }
  public void mouseEvent(String eventType, int button, MouseEvent event) {
    getActiveState()._mouseEvent(eventType, button, event);
  }
  public void keyboardEvent(String eventType, char _key) {
    if (key==ESC) {
      key = 0;
    }
    getActiveState()._keyboardEvent(eventType, _key);
  }

  public void setVolume() {
    try {
      for (SoundFile fect : sfx.values()) {
        fect.amp(jsManager.loadFloatSetting("volume"));
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Something wrong with setting volume", e);
      throw e;
    }
  }

  public void setFrameRateCap() {
    LOGGER_MAIN.finer("Setting framerate cap");
    if (jsManager.loadBooleanSetting("framerate cap")) {
      frameRate(60);
    } else {
      frameRate(1000);
    }
  }

  int NUMOFGROUNDTYPES = 5;
  int NUMOFBUILDINGTYPES = 9;
  int TILESIZE = 1;
  int MAPWIDTH = 100;
  int MAPHEIGHT = 100;
  float MAPHEIGHTNOISESCALE = 0.08f;
  float MAPTERRAINNOISESCALE = 0.08f;
  float HILLSTEEPNESS = 0.1f;

  HashMap<String, PImage> tileImages;
  HashMap<String, PImage[]> buildingImages;
  PImage[] partyBaseImages;
  PImage[] partyImages;
  PImage[] taskImages;
  HashMap<String, PImage> lowImages;
  HashMap<String, PImage> tile3DImages;
  HashMap<String, PImage> equipmentImages;
  PImage bombardImage;

  public void quitGame() {
    LOGGER_MAIN.info("Exitting game...");
    exit();
  }

  public void loadSounds() {
    try {
      if (jsManager.loadBooleanSetting("sound on")) {
        sfx = new HashMap<String, SoundFile>();
        sfx.put("click3", new SoundFile(this, resourcesRoot+"wav/click3.wav"));
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading sounds", e);
      throw e;
    }
  }

  public void loadImages() {
    try {
      LOGGER_MAIN.fine("Loading images");
      tileImages = new HashMap<String, PImage>();
      lowImages = new HashMap<String, PImage>();
      tile3DImages = new HashMap<String, PImage>();
      buildingImages = new HashMap<String, PImage[]>();
      equipmentImages = new HashMap<String, PImage>();

      partyBaseImages = new PImage[]{
        loadImage(resourcesRoot+"img/party/battle.png"),
        loadImage(resourcesRoot+"img/party/flag.png"),
        loadImage(resourcesRoot+"img/party/bandit.png")
      };

      bombardImage = loadImage(resourcesRoot+"img/ui/bombard.png");
      LOGGER_MAIN.finer("Loading task images");
      taskImages = new PImage[gameData.getJSONArray("tasks").size()];
      for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
        JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
        tileImages.put(tileType.getString("id"), loadImage(resourcesRoot+"img/terrain/"+tileType.getString("img")));
        if (!tileType.isNull("low img")) {
          lowImages.put(tileType.getString("id"), loadImage(resourcesRoot+"img/terrain/"+tileType.getString("low img")));
        }
        if (!tileType.isNull("img3d")) {
          tile3DImages.put(tileType.getString("id"), loadImage(resourcesRoot+"img/terrain/"+tileType.getString("img3d")));
        }
      }
      LOGGER_MAIN.finer("Loading building images");
      for (int i=0; i<gameData.getJSONArray("buildings").size(); i++) {
        JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
        PImage[] p = new PImage[buildingType.getJSONArray("img").size()];
        for (int i2=0; i2< buildingType.getJSONArray("img").size(); i2++)
          p[i2] = loadImage(resourcesRoot+"img/building/"+buildingType.getJSONArray("img").getString(i2));
        buildingImages.put(buildingType.getString("id"), p);
      }
      LOGGER_MAIN.finer("Loading task images");
      for (int i=0; i<gameData.getJSONArray("tasks").size(); i++) {
        JSONObject task = gameData.getJSONArray("tasks").getJSONObject(i);
        if (!task.isNull("img")) {
          taskImages[i] = loadImage(resourcesRoot+"img/task/"+task.getString("img"));
        }
      }
      LOGGER_MAIN.finer("Loading equipment images");

      // Load equipment icons
      for (int c=0; c < jsManager.getNumEquipmentClasses(); c++){
        for (int t=0; t < jsManager.getNumEquipmentTypesFromClass(c); t++){
          String fn = jsManager.getEquipmentImageFileName(c, t);
          if (!fn.equals("")){
            equipmentImages.put(jsManager.getEquipmentTypeID(c, t), loadImage(fn));
            LOGGER_MAIN.finest("Loading equipment image id:"+jsManager.getEquipmentTypeID(c, t));
          } else {
            equipmentImages.put(jsManager.getEquipmentTypeID(c, t), createImage(1, 1, ALPHA));
            LOGGER_MAIN.finest("Loading empty image for equipment id:"+jsManager.getEquipmentTypeID(c, t));
          }
        }
      }

      LOGGER_MAIN.finer("Finished loading images");
    }

    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading images", e);
      throw e;
    }
  }

  public PFont getFont(float size) {
    try {
      PFont f=fonts.get(round(size));
      if (f == null) {
        fonts.put(round(size), createFont("GillSans", size));
        return fonts.get(round(size));
      } else {
        return f;
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading font", e);
      throw e;
    }
  }


  public float sum(float[] l) {
    float c=0;
    for (int i=0; i<l.length; i++)
      c += l[i];
    return c;
  }

  public void settings() {
    System.setProperty("jogl.disable.openglcore", "true");
    fullScreen(P3D);
  }


  float halfScreenWidth;
  float halfScreenHeight;
  public void setup() {
    try {
      // Set up loggers
      FileHandler mainHandler = new FileHandler(sketchPath("main_log.log"));
      mainHandler.setFormatter(new LoggerFormatter());
      mainHandler.setLevel(FILELOGLEVEL);
      LOGGER_MAIN.addHandler(mainHandler);
      LOGGER_MAIN.setLevel(FILELOGLEVEL);

      FileHandler gameHandler = new FileHandler(sketchPath("game_log.log"));
      gameHandler.setFormatter(new LoggerFormatter());
      gameHandler.setLevel(FILELOGLEVEL);
      LOGGER_GAME.addHandler(gameHandler);
      LOGGER_GAME.setLevel(FILELOGLEVEL);
      LOGGER_GAME.addHandler(mainHandler);
      LOGGER_GAME.setLevel(FILELOGLEVEL);

      //Logger.getLogger("global").setLevel(Level.WARNING);
      //Logger.getLogger("").setLevel(Level.WARNING);
      //LOGGER_MAIN.setUseParentHandlers(false);

      LOGGER_MAIN.fine("Starting setup");

      fonts = new HashMap<Integer, PFont>();
      jsManager = new JSONManager();
      gameData = jsManager.gameData;
      loadSounds();
      setFrameRateCap();
      textFont(createFont("GillSans", 32));


      loadImages();
      LOGGER_MAIN.fine("Loading states");

      states = new HashMap<String, State>();
      addState("menu", new Menu());
      addState("map", new Game());
      activeState = "menu";
      //noSmooth();
      smooth();
      noStroke();
      //hint(DISABLE_OPTIMIZED_STROKE);
      halfScreenWidth = width/2;
      halfScreenHeight= height/2;
      //toon = loadShader("ToonFrag.glsl", "ToonVert.glsl");
      //toon.set("fraction", 1.0);

      LOGGER_MAIN.fine("Setup finished");
    }

    catch(IOException e) {
      LOGGER_MAIN.log(Level.SEVERE, "IO exception occured duing setup", e);
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error occured during setup", e);
      throw e;
    }
  }
  boolean smoothed = false;

  public void draw() {
    try {
      background(0);
      prevT = millis();
      String newState = getActiveState().update();
      if (!newState.equals("")) {
        for (Panel panel : states.get(newState).panels) {
          for (Element elem : panel.elements) {
            elem.mouseEvent("mouseMoved", LEFT);
          }
        }
        states.get(activeState).leaveState();
        states.get(newState).enterState();
        activeState = newState;
      }
      if (jsManager.loadBooleanSetting("show fps")) {
        textFont(getFont(10));
        textAlign(LEFT, TOP);
        fill(255, 0, 0);
        text(frameRate, 0, 0);
      }
      if (jsManager.loadBooleanSetting("show fps")) {
        textFont(getFont(10));
        textAlign(LEFT, TOP);
        fill(255, 0, 0);
        text(frameRate, 0, 0);
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Uncaught exception occured during draw", e);
      throw e;
    }
  }

  public State getActiveState() {
    State state = states.get(activeState);
    if (state == null) {
      LOGGER_MAIN.severe("State not found "+activeState);
    }
    return state;
  }

  public void addState(String name, State state) {
    states.put(name, state);
  }

  class NodeComparator implements Comparator {
    public int compare (Object o1, Object o2){
      Node a = (Node)o1;
      Node b = (Node)o2;
      if (a.cost < b.cost){
        return -1;
      } else if (a.cost > b.cost){
        return 1;
      } else {
        return 0;
      }
    }
    public boolean equals(Node a, Node b){
      return a.cost == b.cost;
    }
  }

  public int movementCost(int x, int y, int prevX, int prevY, Cell[][] visibleCells, int maxCost) {
    float mult = 1;
    if (x!=prevX && y!=prevY) {
      mult = 1.42f;
    }
    if (0<=x && x<jsManager.loadIntSetting("map size") && 0<=y && y<jsManager.loadIntSetting("map size")) {
      if (visibleCells[y][x] != null){
        return round(gameData.getJSONArray("terrain").getJSONObject(visibleCells[y][x].terrain).getInt("movement cost")*mult);
      }
      else {
        return round(maxCost*mult);  // Assumes max cost if terrain is unexplored
      }
    }

    //Not a valid location
    return -1;
  }

  public int sightCost(int x, int y, int prevX, int prevY, int[][] terrain) {
    float mult = 1;
    if (x!=prevX && y!=prevY) {
      mult = 1.42f;
    }
    if (0<=x && x<jsManager.loadIntSetting("map size") && 0<=y && y<jsManager.loadIntSetting("map size")) {
      return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("sight cost")*mult);
    }

    //Not a valid location
    return -1;
  }


  public Node[][] LimitedKnowledgeDijkstra(int x, int y, int w, int h, Cell[][] visibleCells, int turnsRadius) {
    LOGGER_MAIN.finer(String.format("Starting dijkstra on cell: (%d, %d)", x, y));
    int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
    int maxCost = jsManager.getMaxTerrainMovementCost();
    Node currentHeadNode;
    Node[][] nodes = new Node[h][w];
    nodes[y][x] = new Node(0, false, x, y, x, y);
    PriorityQueue<Node> curMinNodes = new PriorityQueue<Node>(new NodeComparator());
    curMinNodes.add(nodes[y][x]);
    while (curMinNodes.size() > 0) {
      currentHeadNode = curMinNodes.poll();
      currentHeadNode.fixed = true;

      for (int[] mv : mvs) {
        int nx = currentHeadNode.x+mv[0];
        int ny = currentHeadNode.y+mv[1];
        if (0 <= nx && nx < w && 0 <= ny && ny < h) {
          boolean sticky = visibleCells[ny][nx] != null && visibleCells[ny][nx].getParty() != null;
          int newCost = movementCost(nx, ny, currentHeadNode.x, currentHeadNode.y, visibleCells, maxCost);
          int prevCost = currentHeadNode.cost;
          if (newCost != -1){ // Check that the cost is valid
            int totalNewCost = prevCost+newCost;
            if (totalNewCost < visibleCells[y][x].getParty().getMaxMovementPoints()*turnsRadius) {
              if (nodes[ny][nx] == null) {
                nodes[ny][nx] = new Node(totalNewCost, false, currentHeadNode.x, currentHeadNode.y, nx, ny);
                if (!sticky) {
                  curMinNodes.add(nodes[ny][nx]);
                }
              } else if (!nodes[ny][nx].fixed) {
                if (totalNewCost < nodes[ny][nx].cost) { // Updating existing node
                  nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
                  nodes[ny][nx].setPrev(currentHeadNode.x, currentHeadNode.y);
                  if (!sticky) {
                    curMinNodes.remove(nodes[ny][nx]);
                    curMinNodes.add(nodes[ny][nx]);
                  }
                }
              }
            }
          }
        }
      }
    }
    return nodes;
  }

  public float getBattleEstimate(Party attacker, Party defender) {
    int TRIALS = 100000;

    int currentWins = 0;
    for (int i = 0; i<TRIALS; i++) {
      currentWins+=runTrial(attacker,defender);
    }

    return PApplet.parseFloat(currentWins)/PApplet.parseFloat(TRIALS);
  }

  public boolean willMove(Party p, int px, int py, Node[][] moveNodes) {
    if (p.path==null||(p.path!=null&&p.path.size()==0)) {
      for (int y = max(0, py - 1); y < min(py + 2, moveNodes.length); y++) {
        for (int x = max(0, px - 1); x < min(px + 2, moveNodes[0].length); x++) {
          if (moveNodes[y][x] != null && p.getMovementPoints() >= moveNodes[y][x].cost) {
            return true;
          }
        }
      }
    }
    return false;
  }


  class BanditController implements PlayerController {
    int[][] cellsTargetedWeightings;
    Node[][] moveNodes;
    int player;
    BanditController(int player, int mapWidth, int mapHeight) {
      this.player = player;
       cellsTargetedWeightings = new int[mapHeight][];
       for (int y = 0; y < mapHeight; y++) {
         cellsTargetedWeightings[y] = new int[mapWidth];
         for (int x = 0; x < mapHeight; x++) {
           cellsTargetedWeightings[y][x] = 0;
         }
       }
    }

    public GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]) {
      LOGGER_GAME.finer("Generating next event for bandits");
      // Remove targeted cells that are no longer valid
      for (int y = 0; y < visibleCells.length; y++) {
        for (int x = 0; x < visibleCells[0].length; x++) {
          if (cellsTargetedWeightings[y][x] != 0 && visibleCells[y][x] != null && visibleCells[y][x].party == null && visibleCells[y][x].activeSight) {
            cellsTargetedWeightings[y][x] = 0;
          }
        }
      }

      // Get an event from a party
      for (int y = 0; y < visibleCells.length; y++) {
        for (int x = 0; x < visibleCells[0].length; x++) {
          if (visibleCells[y][x] != null && visibleCells[y][x].party != null) {
            if (visibleCells[y][x].party.player == player) {
              LOGGER_GAME.finer(String.format("Getting event for party on cell: (%d, %d) with id:%s", x, y, visibleCells[y][x].party.getID()));
              GameEvent event = getEventForParty(visibleCells, resources, x, y);
              if (event != null) {
                return event;
              }
            }
          }
        }
      }
      return new EndTurn();  // If no parties have events to do, end the turn
    }

    public GameEvent getEventForParty(Cell[][] visibleCells, float resources[], int px, int py) {
      moveNodes = LimitedKnowledgeDijkstra(px, py, visibleCells[0].length, visibleCells.length, visibleCells, 5);
      Party p = visibleCells[py][px].party;
      cellsTargetedWeightings[py][px] = 0;
      int maximumWeighting = 0;
      if (p.getMovementPoints() > 0 && willMove(p, px, py, moveNodes)) {
        ArrayList<int[]> cellsToAttack = new ArrayList<int[]>();
        for (int y = 0; y < visibleCells.length; y++) {
          for (int x = 0; x < visibleCells[0].length; x++) {
            if (visibleCells[y][x] != null && moveNodes[y][x] != null) {
              int weighting = 0;
              if (visibleCells[y][x].party != null && visibleCells[y][x].party.player != p.player) {
                weighting += 5;
                weighting -= floor(moveNodes[y][x].cost/p.getMaxMovementPoints());
                if (visibleCells[y][x].building != null) {
                  weighting += 5;
                  // Add negative weighting if building is a defence building once defence buildings are added
                }
              } else if (visibleCells[y][x].building != null) {
                weighting += 5;
                weighting -= PApplet.parseInt(dist(px, py, x, y));
              }
              weighting += cellsTargetedWeightings[y][x];
              if (weighting > 0) {
                LOGGER_GAME.fine(String.format("At least one cell has a positive weight for attacking, so will attack"));
                maximumWeighting = max(maximumWeighting, weighting);
                cellsToAttack.add(new int[]{x, y, weighting});
              }
            }
          }
        }
        Collections.shuffle(cellsToAttack);
        if (cellsToAttack.size() > 0) {
          for (int[] cell: cellsToAttack){
            if (cell[2] == maximumWeighting) {
              if (moveNodes[cell[1]][cell[0]].cost < p.getMaxMovementPoints()) {
                cellsTargetedWeightings[cell[1]][cell[0]] += maximumWeighting;
                return new Move(px, py, cell[0], cell[1], p.getUnitNumber());
              } else {
                int minimumCost = p.getMaxMovementPoints() * 5;
                for (int y = max(0, cell[1] - 1); y < min(moveNodes.length, cell[1] + 2); y++) {
                  for (int x = max(0, cell[0] - 1); x < min(moveNodes[0].length, cell[0] + 2); x++) {
                    if (moveNodes[y][x] != null) {
                      minimumCost = min(minimumCost, moveNodes[y][x].cost);
                    }
                  }
                }
                for (int y = max(0, cell[1] - 1); y < min(moveNodes.length, cell[1] + 2); y++) {
                  for (int x = max(0, cell[0] - 1); x < min(moveNodes[0].length, cell[0] + 2); x++) {
                    if (moveNodes[y][x] != null && moveNodes[y][x].cost == minimumCost) {
                      cellsTargetedWeightings[cell[1]][cell[0]] += maximumWeighting;
                      return new Move(px, py, x, y, p.getUnitNumber());
                    }
                  }
                }
              }
            }
          }
        } else {
          // Bandit searching for becuase no parties to attack in area
          ArrayList<int[]> cellsToMoveTo = new ArrayList<int[]>();
          for (int y = 0; y < visibleCells.length; y++) {
            for (int x = 0; x < visibleCells[0].length; x++) {
              if (moveNodes[y][x] != null) {  // Check in sight and within 1 turn movement
                int weighting = 0;
                if (visibleCells[y][x] == null){
                  weighting += 10;
                }
                else if (!visibleCells[y][x].getActiveSight()){
                  weighting += 5;
                }
                weighting -= moveNodes[y][x].cost/p.getMaxMovementPoints();
                maximumWeighting = max(maximumWeighting, weighting);
                cellsToMoveTo.add(new int[]{x, y, weighting});
              }
            }
          }
          Collections.shuffle(cellsToMoveTo);
          for(int[] cell : cellsToMoveTo){
            if (cell[2] == maximumWeighting){
              return new Move(px, py, cell[0], cell[1], p.getUnitNumber());
            }
          }
        }
      }
      return null;
    }
  }

  class State {
    ArrayList<Panel> panels;
    String newState, activePanel;

    State() {
      panels = new ArrayList<Panel>();
      addPanel("default", 0, 0, width, height, true, true, color(255, 255), color(0));
      newState = "";
      activePanel = "default";
    }

    public String getNewState() {
      // Once called, newState is cleared, so only for use state management code
      String t = newState;
      newState = "";
      return t;
    }

    public String update() {
      drawPanels();
      return getNewState();
    }
    public void enterState() {
    }
    public void leaveState() {
    }
    public void hidePanels() {
      for (Panel panel : panels) {
        panel.visible = false;
      }
      LOGGER_MAIN.finer("Panels hidden");
    }

    public void resetPanels() {
      panels.clear();
      LOGGER_MAIN.finer("Panels cleared");
    }

    public void addPanel(String id, int x, int y, int w, int h, Boolean visible, Boolean blockEvent, int bgColour, int strokeColour) {
      // Adds new panel to front
      panels.add(new Panel(id, x, y, w, h, visible, blockEvent, bgColour, strokeColour));
      LOGGER_MAIN.finer("Panel added " + id);
      panelToTop(id);
    }
    public void addPanel(String id, int x, int y, int w, int h, Boolean visible, String fileName, int strokeColour) {
      // Adds new panel to front
      panels.add(new Panel(id, x, y, w, h, visible, fileName, strokeColour));
      LOGGER_MAIN.finer("Panel added " + id);
      panelToTop(id);
    }
    public void addElement(String id, Element elem) {
      elem.setID(id);
      getPanel("default").elements.add(elem);
      elem.setOffset(getPanel("default").x, getPanel("default").y);
      LOGGER_MAIN.finer("Element added " + id);
    }
    public void addElement(String id, Element elem, String panel) {
      elem.setID(id);
      getPanel(panel).elements.add(elem);
      elem.setOffset(getPanel(panel).x, getPanel(panel).y);
      LOGGER_MAIN.finer("Elements added " + id);
    }

    public Element getElement(String id, String panel) {
      for (Element elem : getPanel(panel).elements) {
        if (elem.id.equals(id)) {
          return  elem;
        }
      }
      LOGGER_MAIN.warning(String.format("Element not found %s panel:%s", id, panel));
      return null;
    }

    public void removeElement(String elementID, String panelID) {
      getPanel(panelID).elements.remove(elementID);
      LOGGER_MAIN.finer("Elements removed " + elementID);
    }
    public void removePanel(String id) {
      panels.remove(findPanel(id));
      LOGGER_MAIN.finer("Panels removed " + id);
    }

    public void panelToTop(String id) {
      Panel tempPanel = getPanel(id);
      for (int i=findPanel(id); i>0; i--) {
        panels.set(i, panels.get(i-1));
      }
      panels.set(0, tempPanel);
      LOGGER_MAIN.finest("Panel sent to top " + id);
    }

    public void elementToTop(String id, String panelID) {
      Element tempElem = getElement(id, panelID);
      boolean found = false;
      for (int i=0; i<getPanel(panelID).elements.size()-1; i++) {
        if (getPanel(panelID).elements.get(i).id.equals(id)) {
          found = true;
        }
        if (found) {
          getPanel(panelID).elements.set(i, getPanel(panelID).elements.get(i+1));
        }
      }
      getPanel(panelID).elements.set(getPanel(panelID).elements.size()-1, tempElem);
      LOGGER_MAIN.finest("Element sent to top " + id);
    }

    public void printPanels() {
      for (Panel panel : panels) {
        print(panel.id);
      }
      println();
    }

    public int findPanel(String id) {
      for (int i=0; i<panels.size(); i++) {
        if (panels.get(i).id.equals(id)) {
          return i;
        }
      }
      LOGGER_MAIN.warning("Invalid panel " + id);
      return -1;
    }
    public Panel getPanel(String id) {
      Panel p = panels.get(findPanel(id));
      if (p == null) {
        LOGGER_MAIN.warning("Invalid panel " + id);
      }
      return p;
    }

    public void drawPanels() {
      checkElementOnTop();
      // Draw the panels in reverse order (highest in the list are drawn last so appear on top)
      for (int i=panels.size()-1; i>=0; i--) {
        if (panels.get(i).visible) {
          panels.get(i).draw();
        }
      }
    }
    // Empty method for use by children
    public ArrayList<String> mouseEvent(String eventType, int button) {
      return new ArrayList<String>();
    }
    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      return new ArrayList<String>();
    }
    public ArrayList<String> keyboardEvent(String eventType, char _key) {
      return new ArrayList<String>();
    }

    public void elementEvent(ArrayList<Event> events) {
      //for (Event event : events){
      //  println(event.info(), 1);
      //}
    }

    public void _elementEvent(ArrayList<Event> events) {
      for (Event event : events) {
        if (LOGGER_MAIN.isLoggable(Level.FINEST)) {
          LOGGER_MAIN.finest(String.format("Element event id: '%s', Panel:'%s', Type:'%s'", event.id, event.panel, event.type));
        }
        if (event.type.equals("element to top")) {
          elementToTop(event.id, event.panel);
        }
      }
    }

    public void _mouseEvent(String eventType, int button) {
      try {
        ArrayList<Event> events = new ArrayList<Event>();
        mouseEvent(eventType, button);
        if (eventType == "mousePressed") {
          for (int i=0; i<panels.size(); i++) {
            if (panels.get(i).mouseOver()&& panels.get(i).visible&&panels.get(i).blockEvent) {
              activePanel = panels.get(i).id;
              break;
            }
          }
        }
        for (Panel panel : panels) {
          if (activePanel == panel.id || eventType.equals("mouseMoved") || panel.overrideBlocking) {
            // Iterate in reverse order
            for (int i=panel.elements.size()-1; i>=0; i--) {
              if (panel.elements.get(i).active && panel.visible) {
                try {
                  for (String eventName : panel.elements.get(i)._mouseEvent(eventType, button)) {
                    events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
                    if (eventName.equals("stop events")) {
                      elementEvent(events);
                      _elementEvent(events);
                      return;
                    }
                  }
                }
                catch(Exception e) {
                  LOGGER_MAIN.log(Level.SEVERE, String.format("Error during mouse event elem id:%s, panel id:%s", panel.elements.get(i).id, panel.id), e);
                  throw e;
                }
              }
            }
            if (!eventType.equals("mouseMoved") && !panel.overrideBlocking)
              break;
          }
        }
        elementEvent(events);
        _elementEvent(events);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error during mouse event", e);
        throw e;
      }
    }

    public void checkElementOnTop(){
      String elemID = null;
      String panelID = null;
      boolean blocked = false;
      //Find panel and elem on top
      for (int i=panels.size()-1; i>=0; i--) {
        Panel panel = panels.get(i);
        if (panel.mouseOver() && panel.visible) {
          if (panel.blockEvent){
            blocked = true;
          }
          for (Element elem : panel.elements) {
            if (elem.pointOver() && elem.visible){
              elemID = elem.id;
              panelID = panel.id;
              blocked = false;
            }
          }
        }
      }
      for (Panel panel : panels) {
        for (Element elem : panel.elements) {
          if (elem.id.equals(elemID) && panel.id.equals(panelID) && !blocked){
            elem.setElemOnTop(true);
          }
          else{
            elem.setElemOnTop(false);
          }
        }
      }
    }

    public void _mouseEvent(String eventType, int button, MouseEvent event) {
      try {
        ArrayList<Event> events = new ArrayList<Event>();
        mouseEvent(eventType, button, event);
        if (eventType == "mouseWheel") {
          for (int i=0; i<panels.size(); i++) {
            if (panels.get(i).mouseOver()&& panels.get(i).visible&&panels.get(i).blockEvent) {
              activePanel = panels.get(i).id;
              break;
            }
          }
        }
        for (Panel panel : panels) {
          if (activePanel == panel.id && panel.mouseOver() && panel.visible || panel.overrideBlocking) {
            // Iterate in reverse order
            for (int i=panel.elements.size()-1; i>=0; i--) {
              if (panel.elements.get(i).active) {
                try {
                  for (String eventName : panel.elements.get(i)._mouseEvent(eventType, button, event)) {
                    events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
                    if (eventName.equals("stop events")) {
                      elementEvent(events);
                      _elementEvent(events);
                      return;
                    }
                  }
                }
                catch(Exception e) {
                  LOGGER_MAIN.log(Level.SEVERE, String.format("Error during mouse event elem id:%s, panel id:%s", panel.id, panel.elements.get(i)), e);
                  throw e;
                }
              }
            }
          }
        }
        elementEvent(events);
        _elementEvent(events);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error during mouse event", e);
        throw e;
      }
    }
    public void _keyboardEvent(String eventType, char _key) {
      try {
        ArrayList<Event> events = new ArrayList<Event>();
        keyboardEvent(eventType, _key);
        for (Panel panel : panels) {
          for (int i=panel.elements.size()-1; i>=0; i--) {
            if (panel.elements.get(i).active && panel.visible) {
              for (String eventName : panel.elements.get(i)._keyboardEvent(eventType, _key)) {
                events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
              }
            }
          }
        }
        elementEvent(events);
        _elementEvent(events);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error during keyboard event", e);
        throw e;
      }
    }
  }






  class Panel {
    ArrayList<Element> elements;
    String id;
    PImage img;
    Boolean visible, blockEvent, overrideBlocking;
    private int x, y, w, h;
    private int bgColour, strokeColour;
    PGraphics panelCanvas, elemGraphics;

    Panel(String id, int x, int y, int w, int h, Boolean visible, Boolean blockEvent, int bgColour, int strokeColour) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.visible = visible;
      this.blockEvent = blockEvent;
      this.id = id;
      this.bgColour = bgColour;
      this.strokeColour = strokeColour;
      elements = new ArrayList<Element>();
      panelCanvas = createGraphics(w, h, P2D);
      overrideBlocking = false;
    }

    Panel(String id, int x, int y, int w, int h, Boolean visible, String fileName, int strokeColour) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.visible = visible;
      this.id = id;
      this.img = loadImage(fileName);
      this.strokeColour = strokeColour;
      elements = new ArrayList<Element>();
      panelCanvas = createGraphics(w, h, P2D);
      overrideBlocking = false;
    }

    public void setOverrideBlocking(boolean v) {
      overrideBlocking = v;
    }

    public void setOffset() {
      for (Element elem : elements) {
        elem.setOffset(x, y);
      }
    }
    public void setColour(int c) {
      bgColour = c;
      LOGGER_MAIN.finest("Colour changed");
    }

    public void setVisible(boolean a) {
      visible = a;
      for (Element elem : elements) {
        elem.mouseEvent("mouseMoved", mouseButton);
      }
      LOGGER_MAIN.finest("Visiblity changed to " + a);
    }
    public void transform(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      setOffset();
      panelCanvas = createGraphics(w+1, h+1, P2D);
      LOGGER_MAIN.finest("Panel transformed");
    }

    public void draw() {
      panelCanvas.beginDraw();
      panelCanvas.clear();
      panelCanvas.pushStyle();
      if (img == null) {
        if (bgColour != color(255, 255)) {
          panelCanvas.fill(bgColour);
          panelCanvas.stroke(strokeColour);
          panelCanvas.rect(0, 0, w, h);
        }
      } else {
        //imageMode(CENTER);
        panelCanvas.image(img, 0, 0, w, h);
      }
      panelCanvas.popStyle();

      for (Element elem : elements) {
        if (elem.visible) {
          elem.draw(panelCanvas);
        }
      }
      panelCanvas.endDraw();
      image(panelCanvas, x, y);
    }

    public int getX() {
      return x;
    }
    public int getY() {
      return y;
    }

    public Boolean mouseOver() {
      return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
    }
  }





  class Element {
    boolean active = true;
    boolean visible = true;
    boolean elemOnTop;
    int x, y, w, h, xOffset, yOffset;
    String id;

    public void setElemOnTop(boolean value){
      elemOnTop = value;
    }
    public boolean getElemOnTop(){
      // For checking if hover highlighting is needed
      return elemOnTop;
    }

    public void show() {
      visible = true;
    }

    public void hide() {
      visible = false;
    }

    public void draw(PGraphics panelCanvas) {
    }
    public ArrayList<String> mouseEvent(String eventType, int button) {
      return new ArrayList<String>();
    }
    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      return new ArrayList<String>();
    }
    public ArrayList<String> keyboardEvent(String eventType, char _key) {
      return new ArrayList<String>();
    }
    public void transform(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }
    public void setOffset(int xOffset, int yOffset) {
      this.xOffset = xOffset;
      this.yOffset = yOffset;
    }

    public void setID(String id) {
      this.id = id;
    }

    public ArrayList<String> _mouseEvent(String eventType, int button) {
      return mouseEvent(eventType, button);
    }
    public ArrayList<String> _mouseEvent(String eventType, int button, MouseEvent event) {
      return mouseEvent(eventType, button, event);
    }
    public ArrayList<String> _keyboardEvent(String eventType, char _key) {
      return keyboardEvent(eventType, _key);
    }
    public void activate() {
      active = true;
    }
    public void deactivate() {
      active = false;
    }

    public boolean pointOver() {
      return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
    }
  }
  /*
  In-game console for debugging
  */
  class Console extends Element {
    private int textSize, cursorX, maxLength=-1;
    private ArrayList<StringBuilder> text;
    private boolean drawCursor = false;
    private String allowedChars;
    private Map map;
    private JSONObject commands;
    private Player[] players;
    private PFont monoFont;
    private Game game;
    private ArrayList<String> commandLog;
    private int commandLogPosition;
    private String LINESTART = " > ";
    private PGraphics canvas;

    Console(int x, int y, int w, int h, int textSize) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.textSize = textSize;
      text = new ArrayList<StringBuilder>();
      text.add(new StringBuilder(LINESTART));
      commandLog = new ArrayList<String>();
      commandLogPosition = 0;
      cursorX = 3;
      commands = loadJSONObject("json/commands.json");
      monoFont = createFont("Monospaced", textSize*jsManager.loadFloatSetting("text scale"));
    }

    /*
    Takes Map, Players, and Game so that they can be accessed when acting on commands
    */
    public void giveObjects(Map map, Player[] players, Game game) {
      this.map = map;
      this.players = players;
      this.game = game;
    }

    /*
    Converts lines of console into a single StringBuilding object
    */
    public StringBuilder toStr() {
      StringBuilder s = new StringBuilder();
      for (StringBuilder s1 : text) {
        s.append(s1+"\n");
      }
      s.deleteCharAt(s.length()-1);
      return s;
    }

    /*
    Converts a String into an ArrayList of StringBuilders to be used when storing console text
    */
    public ArrayList<StringBuilder> strToText(String s) {
      ArrayList<StringBuilder> t = new ArrayList<StringBuilder>();
      t.add(new StringBuilder());
      char c;
      for (int i=0; i<s.length(); i++) {
        c = s.charAt(i);
        if (c == '\n') {
          t.add(new StringBuilder());
        } else {
          getInputString(t).append(c);
        }
      }
      return t;
    }

    /*
    Draws console
    */
    public void draw(PGraphics canvas) {
      this.canvas = canvas;
      canvas.pushStyle();
      float ts = textSize*jsManager.loadFloatSetting("text scale");
      canvas.textFont(monoFont);
      canvas.textAlign(LEFT, BOTTOM);
      int time = millis();
      drawCursor = (time/500)%2==0 || keyPressed;
      canvas.fill(255);
      for (int i=0; i < text.size(); i++) {
        canvas.text(""+text.get(i), x, height/2-((text.size()-i-1)*ts*1.2f));
      }
      if (drawCursor) {
        canvas.stroke(255);
        canvas.rect(x+canvas.textWidth(getInputString().substring(0, cursorX)), y+height/2-ts*1.2f, 1, ts*1.2f);
      }
      canvas.popStyle();
    }

    /*
    Get the input text line of the console
    */
    public StringBuilder getInputString() {
      return getInputString(text);
    }

    /*
    Gets the input text line of some console text t
    */
    public StringBuilder getInputString(ArrayList<StringBuilder> t) {
      return t.get(t.size()-1);
    }

    /*
    Sets the input text line to some String t
    */
    public void setInputString(String t) {
      setInputString(text, t);
    }

    /*
    Sets the input text line of some console text t1 to some String t2
    */
    public StringBuilder setInputString(ArrayList<StringBuilder> t1, String t2) {
      return t1.set(t1.size()-1, new StringBuilder(t2));
    }

    /*
    Finds position on a raw single String of the console text of some x and y
    */
    public int cxyToC(int cx, int cy) {
      int a=0;
      for (int i=0; i<cy; i++) {
        a += text.get(i).length()+1;
      }
      return a + cx;
    }

    /*
    Gets the position of the mouse on the line cy
    */
    public int getCurX(int cy) {
      int i=0;
      float ts = textSize*jsManager.loadFloatSetting("text scale");
      canvas.textSize(ts);
      int x2 = x;
      for (; i<text.get(cy).length(); i++) {
        float dx = canvas.textWidth(text.get(cy).substring(i, i+1));
        if (x2+dx/2 > mouseX)
          break;
        x2 += dx;
      }
      if (0 <= i && i <= text.get(cy).length()) {
        return i;
      }
      return cursorX;
    }

    public ArrayList<String> _mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mousePressed" && button == LEFT) {
        cursorX = getCurX(text.size()-1);
      }
      return events;
    }

    /*
    Backspace is applied at the current cursor position
    */
    public void clearTextAt() {
      StringBuilder s = toStr();
      String s2 = s.substring(0, cxyToC(cursorX-1, text.size()-1)) + (s.substring(cxyToC(cursorX, text.size()-1), s.length()));
      text = strToText(s2);
    }

    /*
    Add a line to the console just above the input line
    */
    public void sendLine(String line) {
      text.add(text.size()-1, new StringBuilder(line));
    }

    /*
    Sends a line with "Invalid command. " at the start
    */
    public void invalid(String message) {
      sendLine("Invalid command. "+message);
    }

    /*
    Generic invalid message
    */
    public void invalid() {
      invalid("");
    }

    /*
    Returns an array of all possible sub commands for a command
    */
    public String[] getPossibleSubCommands(JSONObject command) {
      Iterable keys = command.getJSONObject("sub commands").keys();
      String[] commandsList = new String[command.getJSONObject("sub commands").size()];
      int i=0;
      for (Object subCommand : keys) {
        commandsList[i] = subCommand.toString();
        i++;
      }
      return commandsList;
    }

    public void invalidSubcommand(JSONObject command, String[] args, int position) {
      String[] commandsList = getPossibleSubCommands(command);
      invalid(String.format("Sub-command not found: %s. Possible sub-commands for command %s: %s", args[position], join(Arrays.copyOfRange(args, 0, position), " "), join(commandsList, " ")));
    }

    public void invalidMissingSubCommand(JSONObject command, String[] args, int position) {
      String[] commandsList = getPossibleSubCommands(command);
      getPossibleSubCommands(command);
      invalid(String.format("Sub-command required for %s. Possible sub-commands for command %s: %s", args[position], join(Arrays.copyOfRange(args, 0, position), " "), join(commandsList, " ")));
    }

    public void invalidMissingValue(JSONObject command, String[] args, int position) {
      invalid(String.format("Value required for %s. Value type: %s", args[position], command.getString("value type")));
    }

    public String getRequiredArgs(JSONObject command){
      JSONArray requiredArgs = command.getJSONArray("args");
      String requiredArgsString = "";
      for (int i = 0; i < requiredArgs.size(); i++){
        JSONArray requiredArg = requiredArgs.getJSONArray(i);
        requiredArgsString += String.format("%s (%s) ", requiredArg.getString(0), requiredArg.getString(1));
      }
      return requiredArgsString;
    }

    public void invalidArg(JSONObject command, String[] args, int commandPosition, int argPosition) {
      if (command.hasKey("args")) {
        String requiredArgsString = getRequiredArgs(command);
        invalid(String.format("Invalid Argument #%d (%s) for %s. Arguments required: %s", argPosition-commandPosition, args[argPosition], args[commandPosition], requiredArgsString));
      } else {
        commandFileError("No args array for command which requires args");
      }
    }

    public void invalidMissingArg(JSONObject command, String[] args, int position) {
      if (command.hasKey("args")) {
        String requiredArgsString = getRequiredArgs(command);
        invalid(String.format("Missing argument required for %s. Arguments required: %s", args[position], requiredArgsString));
      } else {
        commandFileError("No args array for command which requires args");
      }
    }

    public void invalidValue(JSONObject command, String[] args, int position) {
      invalid(String.format("Invalid value for %s. Value type: %s", args[position], command.getString("value type")));
    }

    public void invalidHelp(String command) {
      invalid(String.format("help: invalid command '%s'", command));
    }

    public void commandFileError(String error) {
      invalid(error);
      LOGGER_GAME.severe(error);
    }

    public void getHelp(String[] splitCommand) {
      try {
        if (splitCommand.length == 1) {
          sendLine("Command list:");
          for (Object c : commands.keys()) {
            String c1 = c.toString();
            sendLine(String.format("%-22s%-22s", c1, commands.getJSONObject(c1).getString("basic description")));
          }
        } else {
          if (commands.hasKey(splitCommand[1])) {
            JSONObject command = commands.getJSONObject(splitCommand[1]);
            for (int i = 1; i < splitCommand.length; i++) {
              if (command.getString("type").equals("container")) {
                if (i == splitCommand.length-1) {
                  sendLine(String.format("%s: %s", splitCommand[i], command.getString("detailed description")));
                  sendLine("Command list:");
                  for (Object c : command.getJSONObject("sub commands").keys()) {
                    String c1 = c.toString();
                    sendLine(String.format("%-22s%-22s", c1, command.getJSONObject("sub commands").getJSONObject(c1).getString("basic description")));
                  }
                  return;
                } else if (command.getJSONObject("sub commands").hasKey(splitCommand[i+1])) {
                  command = command.getJSONObject("sub commands").getJSONObject(splitCommand[i+1]);
                } else {
                  invalidHelp(splitCommand[i+1]);
                  return;
                }
              } else if (i == splitCommand.length-1) {
                String c1 = splitCommand[i];
                sendLine(c1+":");
                sendLine(command.getString("detailed description"));
                return;
              } else {
                invalidHelp(splitCommand[i+1]);
                return;
              }
            }
          } else {
            invalidHelp(splitCommand[1]);
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.severe("Error getting help in console");
        throw (e);
      }
    }

    public String[] getSplitCommand(String rawCommand) {
      String[] rawSplitCommand = rawCommand.split(" ");
      String[] tempSplitCommand = new String[rawSplitCommand.length];
      boolean connected = false;
      int i = 0;
      for (String commandComponent: rawSplitCommand){
        if (commandComponent.length()==0) {
          if (connected) {
            tempSplitCommand[i] += " ";
          }
        } else {
          if (PApplet.parseByte(commandComponent.charAt(0)) == 34) {
            connected = true;
          }
          if (PApplet.parseByte(commandComponent.charAt(commandComponent.length()-1))==34) {
            connected = false;
          }
          if (PApplet.parseByte(commandComponent.charAt(0)) == 34 && !connected) {
            tempSplitCommand[i] = commandComponent.replace('"', ' ').trim();
            i++;
            continue;
          }
          if (connected){
            if (tempSplitCommand[i] == null){
              tempSplitCommand[i] = commandComponent.replace('"', ' ').trim();
            } else {
              tempSplitCommand[i] += " " + commandComponent.replace('"', ' ').trim();
            }
          } else {
            if (tempSplitCommand[i] == null){
              tempSplitCommand[i] = commandComponent;
            } else {
              tempSplitCommand[i] += " " + commandComponent.replace('"', ' ').trim();
            }
            i++;
          }
        }
      }
      String[] splitCommand = new String[i];
      for (int j = 0; j < i; j++) {
        splitCommand[j] = tempSplitCommand[j];
      }
      return splitCommand;
    }

    public void doCommand(String rawCommand) {
      String[] splitCommand = getSplitCommand(rawCommand);
      if (splitCommand.length==0) {
        invalid();
        return;
      }
      if (commands.hasKey(splitCommand[0])) {
        JSONObject command = commands.getJSONObject(splitCommand[0]);
        if (command.getString("type").equals("help")) {
          getHelp(splitCommand);
        } else {
          handleCommand(command, splitCommand, 0);
        }
      } else {
        invalid();
      }
    }

    public void handleCommand(JSONObject command, String[] arguments, int position) {
      switch(command.getString("type")) {
      case "container":
        if (arguments.length>position+1) {
          JSONObject subCommands = command.getJSONObject("sub commands");
          if (subCommands.hasKey(arguments[position+1])) {
            handleCommand(subCommands.getJSONObject(arguments[position+1]), arguments, position+1);
          } else {
            invalidSubcommand(command, arguments, position+1);
          }
        } else {
          invalidMissingSubCommand(command, arguments, position);
        }
        break;
      case "setting":
        if (command.hasKey("value type")) {
          if (arguments.length>position+1) {
            switch(command.getString("value type")) {
            case "boolean":
              String value = arguments[position+1].toLowerCase();
              Boolean setting;
              if (value.equals("true") || value.equals("t") || value.equals("1")) {
                setting = true;
              } else if (value.equals("false") || value.equals("f")|| value.equals("0")) {
                setting = false;
              } else {
                invalidValue(command, arguments, position);
                return;
              }
              sendLine(String.format("Changing %s setting", arguments[position]));
              jsManager.saveSetting(command.getString("setting id"), setting);
              if (command.hasKey("regenerate map")&&command.getBoolean("regenerate map")&&map != null) {
                sendLine("This requires regenerating the map. This might take a moment and will mean some randomised features will change");
                map.generateShape();
              }
              if (command.hasKey("update cells")&&command.getBoolean("update cells")&&map != null) {
                players[game.turn].updateVisibleCells(game.terrain, game.buildings, game.parties);
                map.updateVisibleCells(players[game.turn].visibleCells);
              }
              sendLine(String.format("%s setting changed!", arguments[position]));
              break;
            default:
              commandFileError("Command defines invalid value type");
              break;
            }
          } else {
            invalidMissingValue(command, arguments, position);
          }
        } else {
          commandFileError("Command doesn't define a value type");
        }
        break;
      case "resource":
        if (command.hasKey("action")){
          Player p;
          switch (command.getString("action")){
            case "reset":
              if(arguments.length > position+2){
                String playerId = arguments[position + 1];
                if (playerExists(players, playerId)) {
                  p = getPlayer(players, playerId);
                } else {
                  invalidArg(command, arguments, position, position+1);
                  break;
                }
              } else if (position == 1 && arguments.length > position+1) {
                p = game.players[game.turn];
                position--;
              } else {
                invalidMissingArg(command, arguments, position);
                break;
              }
              if (jsManager.resourceExists(arguments[position+2])) {
                int resourceId = jsManager.getResIndex(arguments[position+2]);
                p.resources[resourceId] = 0;
                game.updateResourcesSummary();
                sendLine(String.format("Set %s for %s to 0", arguments[position+2], p.name));
              } else {
                invalidArg(command, arguments, position, position+2);
              }
              break;
            case "set":
              if(arguments.length>position+3){
                String playerId = arguments[position+1];
                if (playerExists(players, playerId)) {
                  p = getPlayer(players, playerId);
                } else {
                  invalidArg(command, arguments, position, position+1);
                  break;
                }
              } else if (position == 1 && arguments.length > position+2) {
                p = game.players[game.turn];
                position--;
              } else {
                invalidMissingArg(command, arguments, position);
                break;
              }
              if (jsManager.resourceExists(arguments[position+2])) {
                int resourceId = jsManager.getResIndex(arguments[position+2]);
                try{
                  float amount = Float.parseFloat(arguments[position+3]);
                  p.resources[resourceId] = amount;
                  game.updateResourcesSummary();
                  sendLine(String.format("Set %s for %s to %s", arguments[position+2], arguments[position+1], p.name));
                } catch (NumberFormatException e){
                  invalidArg(command, arguments, position, position+3);
                }
              } else {
                invalidArg(command, arguments, position, position+2);
              }
              break;
            case "add":
              if(arguments.length>position+3){
                String playerId = arguments[position+1];
                if (playerExists(players, playerId)) {
                  p = getPlayer(players, playerId);
                } else {
                  invalidArg(command, arguments, position, position+1);
                  break;
                }
              } else if (position == 1 && arguments.length > position+2) {
                p = game.players[game.turn];
                position--;
              } else {
                invalidMissingArg(command, arguments, position);
                break;
              }
              if (jsManager.resourceExists(arguments[position+2])) {
                int resourceId = jsManager.getResIndex(arguments[position+2]);
                try{
                  float amount = Float.parseFloat(arguments[position+3]);
                  p.resources[resourceId] += amount;
                  game.updateResourcesSummary();
                  sendLine(String.format("Added %s %s to %s", arguments[position+3], arguments[position+2], p.name));
                } catch (NumberFormatException e){
                  invalidArg(command, arguments, position, position+3);
                }
              } else {
                invalidArg(command, arguments, position, position+2);
              }
              break;
            case "subtract":
              if(arguments.length>position+3){
                String playerId = arguments[position+1];
                if (playerExists(players, playerId)) {
                  p = getPlayer(players, playerId);
                } else {
                  invalidArg(command, arguments, position, position+1);
                  break;
                }
              } else if (position == 1 && arguments.length > position+2) {
                p = game.players[game.turn];
                position--;
              } else {
                invalidMissingArg(command, arguments, position);
                break;
              }
              if (jsManager.resourceExists(arguments[position+2])) {
                int resourceId = jsManager.getResIndex(arguments[position+2]);
                try{
                  float amount = Float.parseFloat(arguments[position+3]);
                  p.resources[resourceId] -= amount;
                  game.updateResourcesSummary();
                  sendLine(String.format("Subtracted %s %s from %s", arguments[position+3], arguments[position+2], p.name));
                } catch (NumberFormatException e){
                  invalidArg(command, arguments, position, position+3);
                }
              } else {
                invalidArg(command, arguments, position, position+2);
              }
              break;
            default:
              commandFileError("Command defines invalid action but is of type resource");
              break;
          }
        } else {
          commandFileError("Command doesn't define an action but is of type resource");
        }
        break;
      case "building-fill":
        if (arguments.length > position + 1) {
          String building = arguments[position+1];
          int buildingIndex = jsManager.findJSONObjectIndex(gameData.getJSONArray("buildings"), building);
          if (buildingIndex != -1) {
            sendLine("Filling map with building "+building);
            for (int y = 0; y < game.mapHeight; y++) {
              for (int x = 0; x < game.mapWidth; x++) {
                game.buildings[y][x] = new Building(buildingIndex);
              }
            }
          } else {
            invalidValue(command, arguments, position);
          }
        } else {
          invalidMissingValue(command, arguments, position);
        }
        break;
      default:
        commandFileError("Command has invalid type");
        break;
      }
    }

    public void saveCommandLog() {
      if (commandLog.size() == commandLogPosition) {
        commandLog.add("");
      }
      commandLog.set(commandLogPosition, getInputString().substring(LINESTART.length()));
    }

    public void accessCommandLog() {
      setInputString(LINESTART+commandLog.get(commandLogPosition));
      cursorX = getInputString().length();
    }

    public ArrayList<String> _keyboardEvent(String eventType, char _key) {
      keyboardEvent(eventType, _key);
      if (eventType == "keyTyped") {
        if (maxLength == -1 || this.toStr().length() < maxLength) {
          if (_key == '\n') {
            //clearTextAt();
            text.add(text.size(), new StringBuilder(getInputString().substring(cursorX, getInputString().length())));
            getInputString().replace(cursorX, text.get(text.size()-1).length(), "");
            cursorX=3;
          } else if (_key == '\t') {
            for (int i=0; i<4-cursorX%4; i++) {
              getInputString().insert(cursorX, " ");
            }
            cursorX += 4-cursorX%4;
          } else if (_key != 0 && (allowedChars == null || allowedChars.indexOf(_key) != -1)) {
            //clearTextAt();
            getInputString().insert(cursorX, _key);
            cursorX++;
          }
        }
      }
      if (eventType == "keyPressed") {
        if (_key == CODED) {
          if (keyCode == LEFT) {
            cursorX = max(cursorX-1, 3);
          }
          if (keyCode == RIGHT) {
            cursorX = min(cursorX+1, getInputString().length());
          }
          if (keyCode == UP && commandLogPosition > 0){
            saveCommandLog();
            commandLogPosition--;
            accessCommandLog();
          }
          if(keyCode == DOWN && commandLog.size() > commandLogPosition+1){
            saveCommandLog();
            commandLogPosition++;
            accessCommandLog();
          }
          //if (keyCode == SHIFT){
          //  lshed = true;
          //}
        }
        if (_key == ENTER) {
          boolean commandThere = getInputString().length() > LINESTART.length();
          String rawCommand = getInputString().substring(LINESTART.length());
          if (commandThere) {
            saveCommandLog();
          }
          commandLogPosition = commandLog.size();
          text.add(text.size(), new StringBuilder(LINESTART));
          if (commandThere) {
            doCommand(rawCommand);
          }
          cursorX=3;
        }
        if (_key == VK_BACK_SPACE&&cursorX>3) {
          clearTextAt();
          cursorX--;
        }
        if (keyCode == VK_DELETE&&cursorX<getInputString().length()) {
          cursorX++;
          clearTextAt();
          cursorX--;
        }
      }
      //if (eventType == "keyReleased"){
      //  if(key == CODED){
      //    if (keyCode == SHIFT){
      //      lshed = false;
      //    }
      //  }
      //}
      return new ArrayList<String>();
    }
  }


  class PlayerSelector extends Element {
    PlayerSelector(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }
  }

  class IncrementElement extends Element {
    final int TEXTSIZE = 8;
    final int SIDEBOXESWIDTH = 15;
    final int ARROWOFFSET = 4;
    final float FULLPANPROPORTION = 0.25f;  // Adjusts how much mouse dragging movement is needed to change value as propotion of screen width
    private int upper, lower, value, step, bigStep;
    int startingX, startingValue, pressing;
    boolean grabbed;

    IncrementElement(int x, int y, int w, int h, int upper, int lower, int startingValue, int step, int bigStep){
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.upper = upper;
      this.lower = lower;
      this.value = startingValue;
      this.step = step;
      this.bigStep = bigStep;
      grabbed = false;
      startingX = 0;
      startingValue = value;
      pressing = -1;
    }

    public void setUpper(int upper){
      this.upper = upper;
    }

    public int getUpper(){
      return this.upper;
    }

    public void setLower(int lower){
      this.lower = lower;
    }

    public int getLower(){
      return this.lower;
    }

    public void setValue(int value){
      this.value = value;
    }

    public int getValue(){
      return this.value;
    }

    public void setValueWithinBounds(){
      value = PApplet.parseInt(between(lower, value, upper));
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType.equals("mouseClicked")){
        int change = 0;
        if (button == LEFT){
          change = step;
        } else if (button == RIGHT){  // Right clicking increments by more
          change = bigStep;
        }

        if (mouseOverLeftBox()){
          setValue(getValue()-change);
          setValueWithinBounds();
          events.add("valueChanged");
        } else if (mouseOverRightBox()){
          setValue(getValue()+change);
          setValueWithinBounds();
          events.add("valueChanged");
        }
      }
      if (eventType.equals("mousePressed")){
        if (mouseOverRightBox()){
          pressing = 0;
        } else if (mouseOverLeftBox()){
          pressing = 1;
        } else if (mouseOverMiddleBox()){
          grabbed = true;
          startingX = mouseX;
          startingValue = getValue();
          pressing = 2;
        } else{
          pressing = -1;
        }
      }
      if (eventType.equals("mouseReleased")){
        if (grabbed){
          events.add("valueChanged");
        }
        grabbed = false;
        pressing = -1;
      }
      if (eventType.equals("mouseDragged")){
        if (grabbed){
          int change = floor((mouseX-startingX)*(upper-lower)/(width*FULLPANPROPORTION));
          if (change != 0){
            setValue(startingValue+change);
            setValueWithinBounds();
          }
        }
      }
      return events;
    }

    public void draw(PGraphics panelCanvas){
      panelCanvas.pushStyle();

      //Draw middle box
      panelCanvas.strokeWeight(2);
      if (grabbed){
        panelCanvas.fill(150);
      } else if (getElemOnTop() && mouseOverMiddleBox()){
        panelCanvas.fill(200);
      } else{
        panelCanvas.fill(170);
      }
      panelCanvas.rect(x, y, w, h);

      //draw left side box
      panelCanvas.strokeWeight(1);
      if (getElemOnTop() && mouseOverLeftBox()){
        if (pressing == 1){
          panelCanvas.fill(100);
        } else{
          panelCanvas.fill(130);
        }
      } else{
        panelCanvas.fill(120);
      }
      panelCanvas.rect(x, y, SIDEBOXESWIDTH, h-1);
      panelCanvas.fill(0);
      panelCanvas.strokeWeight(2);
      panelCanvas.line(x-ARROWOFFSET+SIDEBOXESWIDTH, y+ARROWOFFSET, x+ARROWOFFSET, y+h/2);
      panelCanvas.line(x+ARROWOFFSET, y+h/2, x-ARROWOFFSET+SIDEBOXESWIDTH, y+h-ARROWOFFSET);

      //draw right side box
      if (getElemOnTop() && mouseOverRightBox()){
        if (pressing == 0){
          panelCanvas.fill(100);
        } else{
          panelCanvas.fill(130);
        }
      } else{
        panelCanvas.fill(120);
      }
      panelCanvas.rect(x+w-SIDEBOXESWIDTH, y, SIDEBOXESWIDTH, h-1);
      panelCanvas.fill(0);
      panelCanvas.strokeWeight(2);
      panelCanvas.line(x+w+ARROWOFFSET-SIDEBOXESWIDTH, y+ARROWOFFSET, x+w-ARROWOFFSET, y+h/2);
      panelCanvas.line(x+w-ARROWOFFSET, y+h/2, x+w+ARROWOFFSET-SIDEBOXESWIDTH, y-ARROWOFFSET+h);

      // Draw value
      panelCanvas.fill(0);
      panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(CENTER, CENTER);
      panelCanvas.text(value, x+w/2, y+h/2);

      panelCanvas.popStyle();
    }

    public boolean mouseOverMiddleBox(){
      return mouseOver() && !mouseOverRightBox() && !mouseOverLeftBox();
    }

    public boolean mouseOverRightBox() {
      return mouseX-xOffset >= x+w-SIDEBOXESWIDTH && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }

    public boolean mouseOverLeftBox() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+SIDEBOXESWIDTH && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }

    public boolean mouseOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }

    public boolean pointOver(){
      return mouseOver();
    }
  }

  class EquipmentManager extends Element {
    final int TEXTSIZE = 8;
    final float BOXWIDTHHEIGHTRATIO = 0.75f;
    final int SHADOWSIZE = 20;
    final float EXTRATYPEWIDTH = 1.5f;
    final float BIGIMAGESIZE = 0.5f;
    String[] equipmentClassDisplayNames;
    int[] currentEquipment, currentEquipmentQuantities;
    int currentUnitNumber;
    float boxWidth, boxHeight;
    int selectedClass;
    float dropX, dropY, dropW, dropH, oldDropH;
    ArrayList<int[]> equipmentToChange;
    HashMap<String, PImage> tempEquipmentImages;
    HashMap<String, PImage> bigTempEquipmentImages;

    EquipmentManager(int x, int y, int w) {
      this.x = x;
      this.y = y;
      this.w = w;

      // Load display names for equipment classes
      equipmentClassDisplayNames = new String[jsManager.getNumEquipmentClasses()];
      for (int i = 0; i < equipmentClassDisplayNames.length; i ++) {
        equipmentClassDisplayNames[i] = jsManager.getEquipmentClassDisplayName(i);
      }

      currentEquipment = new int[jsManager.getNumEquipmentClasses()];
      currentEquipmentQuantities = new int[jsManager.getNumEquipmentClasses()];
      currentUnitNumber = 0;
      for (int i=0; i<currentEquipment.length;i ++){
        currentEquipment[i] = -1; // -1 represens no equipment
        currentEquipmentQuantities[i] = 0;
      }

      updateSizes();

      selectedClass = -1;  // -1 represents nothing being selected
      equipmentToChange = new ArrayList<int[]>();
      tempEquipmentImages = new HashMap<String, PImage>();
      bigTempEquipmentImages = new HashMap<String, PImage>();
      oldDropH = 0;
    }

    public void updateSizes(){

      boxWidth = w/jsManager.getNumEquipmentClasses();
      boxHeight = boxWidth*BOXWIDTHHEIGHTRATIO;
      updateDropDownPositionAndSize();
    }

    public void updateDropDownPositionAndSize(){
      dropY = y+boxHeight;
      dropW = boxWidth * EXTRATYPEWIDTH;
      dropH = jsManager.loadFloatSetting("gui scale") * 32;
      dropX = between(x, x+boxWidth*selectedClass-(dropW-boxWidth)/2, x+w-dropW);
    }


    public void resizeImages(){
      // Resize equipment icons
      for (int c=0; c < jsManager.getNumEquipmentClasses(); c++){
        for (int t=0; t < jsManager.getNumEquipmentTypesFromClass(c); t++){
          try{
            String id = jsManager.getEquipmentTypeID(c, t);
            tempEquipmentImages.put(id, equipmentImages.get(id).copy());
            tempEquipmentImages.get(id).resize(PApplet.parseInt(dropH/0.75f), PApplet.parseInt(dropH-1));
            bigTempEquipmentImages.put(id, equipmentImages.get(id).copy());
            bigTempEquipmentImages.get(id).resize(PApplet.parseInt(boxWidth*BIGIMAGESIZE), PApplet.parseInt(boxHeight*BIGIMAGESIZE));
          }
          catch (NullPointerException e){
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error resizing image for equipment icon class:%d, type:%d, id:%s", c, t, jsManager.getEquipmentTypeID(c, t)), e);
            throw e;
          }
        }
      }
    }

    public void transform(int x, int y, int w) {
      this.x = x;
      this.y = y;
      this.w = w;

      updateSizes();
    }

    public void setEquipment(Party party) {
      this.currentEquipment = party.getAllEquipment();
      LOGGER_GAME.finer(String.format("changing equipment for manager to :%s", Arrays.toString(party.getAllEquipment())));
      currentEquipmentQuantities = party.getEquipmentQuantities();
      currentUnitNumber = party.getUnitNumber();
    }

    public float getBoxHeight(){
      return boxHeight;
    }

    public ArrayList<int[]> getEquipmentToChange(){
      // Also clears equipmentToChange
      ArrayList<int[]> temp = new ArrayList<int[]>(equipmentToChange);
      equipmentToChange.clear();
      return temp;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType.equals("mouseClicked")) {
        boolean[] blockedClasses = getBlockedClasses();
        if (mouseOverClasses()) {
          if (button == LEFT){
            int newSelectedClass = hoveringOverClass();
            if (newSelectedClass == selectedClass){  // If selecting same option
              selectedClass = -1;
            }
            else if (newSelectedClass == -1 || !blockedClasses[newSelectedClass]){
              selectedClass = newSelectedClass;
              events.add("dropped");
              events.add("stop events");
            }
          }
          else if (button == RIGHT){  // Unequip if right clicking on class
            events.add("valueChanged");
            events.add("stop events");
            equipmentToChange.add(new int[] {selectedClass, -1});
          }
        }
        else if (mouseOverTypes()){
          int newSelectedType = hoveringOverType();
          if (newSelectedType == jsManager.getNumEquipmentTypesFromClass(selectedClass)){
            // Unequip (final option)
            events.add("valueChanged");
            events.add("stop events");
            equipmentToChange.add(new int[] {selectedClass, -1});
          }
          else if (newSelectedType != currentEquipment[selectedClass] && !blockedClasses[selectedClass]){
            events.add("valueChanged");
            events.add("stop events");
            equipmentToChange.add(new int[] {selectedClass, newSelectedType});
          }
          selectedClass = -1;
        }
        else{
          selectedClass = -1;
        }
      }
      return events;
    }

    public int getSelectedClass(){
      return selectedClass;
    }

    public void drawShadow(PGraphics panelCanvas, float shadowX, float shadowY, float shadowW, float shadowH){
      panelCanvas.noStroke();
      for (int i = SHADOWSIZE; i > 0; i --){
        panelCanvas.fill(0, 255-255*pow(((float)i/SHADOWSIZE), 0.1f));
        //panelCanvas.rect(shadowX-i, shadowY-i, shadowW+i*2, shadowH+i*2, i);
      }
    }

    public boolean[] getBlockedClasses(){
      boolean[] blockedClasses = new boolean[jsManager.getNumEquipmentClasses()];
      for (int i = 0; i < blockedClasses.length; i ++) {
        if (currentEquipment[i] != -1){
          String[] otherBlocking = jsManager.getOtherClassBlocking(i, currentEquipment[i]);
          if (otherBlocking != null){
            for (int j=0; j < otherBlocking.length; j ++){
              int classIndex = jsManager.getEquipmentClassFromID(otherBlocking[j]);
              blockedClasses[classIndex] = true;
            }
          }
        }
      }
      return blockedClasses;
    }

    public boolean[] getHoveringBlockedClasses(){
      boolean[] blockedClasses = new boolean[jsManager.getNumEquipmentClasses()];
      if (selectedClass != -1){
        if (hoveringOverType() != -1 && hoveringOverType() < jsManager.getNumEquipmentTypesFromClass(selectedClass)){
          String[] otherBlocking = jsManager.getOtherClassBlocking(selectedClass, hoveringOverType());
          if (otherBlocking != null){
            for (int j=0; j < otherBlocking.length; j ++){
              int classIndex = jsManager.getEquipmentClassFromID(otherBlocking[j]);
              blockedClasses[classIndex] = true;
            }
          }
        }
      }
      return blockedClasses;
    }

    public void draw(PGraphics panelCanvas) {

      if (oldDropH != dropH){  // If height of boxes has changed
        resizeImages();
        oldDropH = dropH;
      }

      updateDropDownPositionAndSize();
      panelCanvas.pushStyle();

      panelCanvas.strokeWeight(2);
      panelCanvas.fill(170);
      panelCanvas.rect(x, y, w, boxHeight);

      boolean[] blockedClasses = getBlockedClasses();
      boolean[] potentialBlockedClasses = getHoveringBlockedClasses();

      for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
        if (!blockedClasses[i]){
          if (selectedClass == i){
            panelCanvas.strokeWeight(3);
            panelCanvas.fill(140);
          }
          else{
            if (!potentialBlockedClasses[i]){
              panelCanvas.strokeWeight(1);
              panelCanvas.noFill();
            }
            else{
              //Hovering over equipment type that blocks this class
              panelCanvas.strokeWeight(1);
              panelCanvas.fill(110);
            }
          }
          panelCanvas.rect(x+boxWidth*i, y, boxWidth, boxHeight);
          if (currentEquipment[i] != -1){
            panelCanvas.image(bigTempEquipmentImages.get(jsManager.getEquipmentTypeID(i, currentEquipment[i])), PApplet.parseInt(x+boxWidth*i)+(1-BIGIMAGESIZE)*boxWidth/2, y+(1-BIGIMAGESIZE)*boxHeight/2);
          }
          panelCanvas.fill(0);
          panelCanvas.textAlign(CENTER, TOP);
          panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
          panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5f), y);
          if (currentEquipment[i] != -1){
            panelCanvas.textFont(getFont((TEXTSIZE-1)*jsManager.loadFloatSetting("text scale")));
            panelCanvas.textAlign(CENTER, BOTTOM);
            panelCanvas.text(jsManager.getEquipmentTypeDisplayName(i, currentEquipment[i]), x+boxWidth*(i+0.5f), y+boxHeight);
            if (currentEquipmentQuantities[i] < currentUnitNumber){
              panelCanvas.fill(255, 0, 0);
            }
            panelCanvas.text(String.format("%d/%d", currentEquipmentQuantities[i], currentUnitNumber), x+boxWidth*(i+0.5f), y+boxHeight-TEXTSIZE*jsManager.loadFloatSetting("text scale")+5);
          }
        }
        else{
          panelCanvas.strokeWeight(2);
          panelCanvas.fill(80);
          panelCanvas.rect(x+boxWidth*i, y, boxWidth, boxHeight);
          panelCanvas.fill(0);
          panelCanvas.textAlign(CENTER, TOP);
          panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
          panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5f), y);
        }
      }

      // Draw dropdown if an equipment class is selected
      panelCanvas.stroke(0);
      if (selectedClass != -1){
        panelCanvas.textAlign(LEFT, TOP);
        panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
        String[] equipmentTypes = jsManager.getEquipmentFromClass(selectedClass);
        for (int i = 0; i < jsManager.getNumEquipmentTypesFromClass(selectedClass);   i ++){
          panelCanvas.strokeWeight(1);
          if (currentEquipment[selectedClass] != i){
            panelCanvas.fill(170);
            panelCanvas.rect(dropX, dropY+i*dropH, dropW, dropH);
            panelCanvas.fill(0);
            panelCanvas.text(equipmentTypes[i], 3+dropX, dropY+i*dropH);
            try{
              panelCanvas.image(tempEquipmentImages.get(jsManager.getEquipmentTypeID(selectedClass, i)), dropX+dropW-dropH/0.75f-2, dropY+dropH*i+2);
            }
            catch (NullPointerException e){
              LOGGER_MAIN.log(Level.WARNING, String.format("Error drawing image for equipment icon class:%d, type:%d, id:%s", selectedClass, i, jsManager.getEquipmentTypeID(selectedClass, i)), e);
            }
          }
          else{
            panelCanvas.fill(220);
            panelCanvas.rect(dropX, dropY+i*dropH, dropW, dropH);
            panelCanvas.fill(150);
            panelCanvas.text(equipmentTypes[i], 3+dropX, dropY+i*dropH);
            try{
              panelCanvas.image(tempEquipmentImages.get(jsManager.getEquipmentTypeID(selectedClass, i)), dropX+dropW-dropH/0.75f-2, dropY+dropH*i+2);
            }
            catch (NullPointerException e){
              LOGGER_MAIN.log(Level.WARNING, String.format("Error drawing image for equipment icon class:%d, type:%d, id:%s", selectedClass, i, jsManager.getEquipmentTypeID(selectedClass, i)), e);
            }
          }
        }
        if (currentEquipment[selectedClass] != -1){
          panelCanvas.fill(170);
          panelCanvas.rect(dropX, dropY+jsManager.getNumEquipmentTypesFromClass(selectedClass)*dropH, dropW, dropH);
          panelCanvas.fill(0);
          panelCanvas.text("Unequip", 3+dropX, dropY+jsManager.getNumEquipmentTypesFromClass(selectedClass)*dropH);
        }
      }
      if (selectedClass != -1){
        panelCanvas.strokeWeight(2);
        panelCanvas.stroke(0);
        panelCanvas.noFill();
        if (currentEquipment[selectedClass] == -1){  // If nothing equipped, there is not unequip option at the bottom
          panelCanvas.rect(dropX, dropY, dropW, dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass))+1);
        } else{
          panelCanvas.rect(dropX, dropY, dropW, dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass)+1)+1);
        }
      }

      panelCanvas.popStyle();
    }

    public boolean mouseOverClasses() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+boxHeight;
    }

    public boolean mouseOverTypes() {
      if (selectedClass == -1){
        return false;
      } else if (currentEquipment[selectedClass] == -1){  // If nothing equipped, there is not unequip option at the bottom
        return mouseX-xOffset >= dropX && mouseX-xOffset <= dropX+dropW && mouseY-yOffset >= dropY && mouseY-yOffset <= dropY+dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass));
      } else{
        return mouseX-xOffset >= dropX && mouseX-xOffset <= dropX+dropW && mouseY-yOffset >= dropY && mouseY-yOffset <= dropY+dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass)+1);
      }
    }

    public int hoveringOverType() {
      int num = jsManager.getNumEquipmentTypesFromClass(selectedClass);
      for (int i = 0; i < num+1; i ++) {
        if (mouseX-xOffset >= dropX && mouseX-xOffset <= dropX+dropW && mouseY-yOffset >= dropY+dropH*i && mouseY-yOffset <= dropY+dropH*(i+1)){
          return i;
        }
      }
      return -1;
    }

    public int hoveringOverClass() {
      for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
        if (mouseX-xOffset >= x+boxWidth*i && mouseX-xOffset <= x+boxWidth*(i+1) && mouseY-yOffset >= y && mouseY-yOffset <= y+boxHeight){
          return i;
        }
      }
      return -1;
    }

    public boolean pointOver() {
      return mouseOverTypes() || mouseOverClasses();
    }
  }

  class ProficiencySummary extends Element {
    final int TEXTSIZE = 8;
    final int DECIMALPLACES = 2;
    String[] proficiencyDisplayNames;
    float[] proficiencies, bonuses;
    int rowHeight;

    ProficiencySummary(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      updateProficiencyDisplayNames();
      proficiencies = new float[jsManager.getNumProficiencies()];
      bonuses = new float[jsManager.getNumProficiencies()];
    }

    public void transform(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      updateProficiencyDisplayNames();
      updateRowHeight();
    }

    public void setProficiencies(float[] proficiencies) {
      this.proficiencies = proficiencies;
    }

    public void setProficiencyBonuses(float[] bonuses){
      this.bonuses = bonuses;
    }

    public void updateProficiencyDisplayNames() {
      proficiencyDisplayNames = new String[jsManager.getNumProficiencies()];
      for (int i = 0; i < jsManager.getNumProficiencies(); i ++) {
        proficiencyDisplayNames[i] = jsManager.indexToProficiencyDisplayName(i);
      }
      LOGGER_MAIN.finer("Updated proficiency display names to: "+Arrays.toString(proficiencyDisplayNames));
    }

    public void updateRowHeight() {
      rowHeight = h/proficiencyDisplayNames.length;
    }

    public void draw(PGraphics panelCanvas) {
      panelCanvas.pushStyle();

      //Draw background
      panelCanvas.strokeWeight(2);
      panelCanvas.fill(150);
      panelCanvas.rect(x, y, w, h); // Added and subtracted values are for stroke to line up well with other boxes

      //Draw each proficiency box
      panelCanvas.strokeWeight(1);
      for (int i = 0; i < proficiencyDisplayNames.length; i ++) {
        panelCanvas.noFill();
        panelCanvas.line(x, y+rowHeight*i, x+w, y+rowHeight*i);
        panelCanvas.fill(0);
        panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(LEFT, CENTER);
        panelCanvas.text(proficiencyDisplayNames[i], x+5, y+rowHeight*(i+0.5f)); // Display name aligned left, middle height within row
        panelCanvas.textAlign(RIGHT, CENTER);
        panelCanvas.text(roundDpTrailing(""+proficiencies[i], DECIMALPLACES), x+w-10-panelCanvas.textWidth("0")*(DECIMALPLACES+4), y+rowHeight*(i+0.5f));
        if (bonuses[i] > 0){
          panelCanvas.fill(0, 255, 0);
          panelCanvas.text("+"+roundDpTrailing(""+bonuses[i], DECIMALPLACES), x+w-5, y+rowHeight*(i+0.5f)); // Display bonus aligned right, middle height within row
        }
        else if (bonuses[i] < 0){
          panelCanvas.fill(255, 0, 0);
          panelCanvas.text(roundDpTrailing(""+bonuses[i], DECIMALPLACES), x+w-5, y+rowHeight*(i+0.5f)); // Display bonus aligned right, middle height within row
        }
      }

      panelCanvas.popStyle();
    }

    public int hoveringOption(){
      for (int i = 0; i < proficiencyDisplayNames.length; i++){
        if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+rowHeight*i && mouseY-yOffset <= y+rowHeight*(i+1)) {
          return i;
        }
      }
      return -1;
    }
    public boolean mouseOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }
  }

  class BaseFileManager extends Element {
    // Basic file manager that scans a folder and makes a selectable list for all the files
    final int TEXTSIZE = 14, SCROLLWIDTH = 30;
    String folderString;
    String[] saveNames;
    int selected, rowHeight, numDisplayed, scroll;
    boolean scrolling;
    float fakeHeight;


    BaseFileManager(int x, int y, int w, int h, String folderString) {
      super.x = x;
      super.y = y;
      super.w = w;
      super.h = h;
      this.folderString = folderString;
      saveNames = new String[0];
      selected = 0;
      rowHeight = ceil(TEXTSIZE * jsManager.loadFloatSetting("text scale"))+5;
      scroll = 0;
      scrolling = false;
      updateFakeHeight();
    }

    public void updateFakeHeight() {
      fakeHeight = rowHeight * numDisplayed;
    }

    public String getNextAutoName() {
      // Find the next automatic name for save
      loadSaveNames();
      int mx = 1;
      for (int i=0; i<saveNames.length; i++) {
        if (saveNames[i].length() > 8) {// 'Untitled is 8 characters
          if (saveNames[i].substring(0, 8).equals("Untitled")) {
            try {
              mx = max(mx, Integer.parseInt(saveNames[i].substring(8, saveNames[i].length())));
            }
            catch(NumberFormatException e) {
              LOGGER_MAIN.log(Level.WARNING, "Save name confusing becuase in autogen format", e);
            }
            catch(Exception e) {
              LOGGER_MAIN.log(Level.SEVERE, "An error occured with finding autogen name", e);
              throw e;
            }
          }
        }
      }
      String name = "Untitled"+(mx+1);
      LOGGER_MAIN.info("Created autogenerated file name: " + name);
      return name;
    }

    public void loadSaveNames() {
      try {
        File dir = new File(sketchPath("saves"));
        if (!dir.exists()) {
          LOGGER_MAIN.info("Creating new 'saves' directory");
          dir.mkdir();
        }
        saveNames = dir.list();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Files scanning failed", e);
      }
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      int d = saveNames.length - numDisplayed;
      if (eventType.equals("mouseClicked")) {
        if (moveOver()) {
          if (d <= 0 || mouseX-xOffset<x+w-SCROLLWIDTH) {
            // If not hovering over scroll bar, then select item
            selected = hoveringOption();
            events.add("valueChanged");
            scrolling = false;
          }
        }
      } else if (eventType.equals("mousePressed")) {
        if (d > 0 && moveOver() && mouseX-xOffset>x+w-SCROLLWIDTH) {
          // If hovering over scroll bar, set scroll to mouse pos
          scrolling = true;
          scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/h, d));
        } else {
          scrolling = false;
        }
      } else if (eventType.equals("mouseDragged")) {
        if (scrolling && d > 0) {
          // If scrolling, set scroll to mouse pos
          scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/h, d));
        }
      } else if (eventType.equals("mouseReleased")) {
        scrolling = false;
      }

      return events;
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mouseWheel") {
        float count = event.getCount();
        if (moveOver()) { // Check mouse over element
          if (saveNames.length > numDisplayed) {
            scroll = round(between(0, scroll+count, saveNames.length-numDisplayed));
            LOGGER_MAIN.finest("Changing scroll to: "+scroll);
          }
        }
      }
      return events;
    }

    public String selectedSaveName() {
      if (saveNames.length == 0) {
        return "Untitled";
      } else if (saveNames.length <= selected) {
        LOGGER_MAIN.severe("Selected name is out of range " + selected);
      }
      LOGGER_MAIN.info("Selected save name is : " + saveNames[selected]);
      return saveNames[selected];
    }

    public void draw(PGraphics panelCanvas) {

      rowHeight = ceil(TEXTSIZE * jsManager.loadFloatSetting("text scale"))+5;
      updateFakeHeight();

      numDisplayed = ceil(h/rowHeight);
      panelCanvas.pushStyle();

      panelCanvas.textSize(TEXTSIZE * jsManager.loadFloatSetting("text scale"));
      panelCanvas.textAlign(LEFT, TOP);
      for (int i=scroll; i<min(numDisplayed+scroll, saveNames.length); i++) {
        if (selected == i) {
          panelCanvas.strokeWeight(2);
          panelCanvas.fill(color(100));
        } else {
          panelCanvas.strokeWeight(1);
          panelCanvas.fill(color(150));
        }
        panelCanvas.rect(x, y+rowHeight*(i-scroll), w, rowHeight);
        panelCanvas.fill(0);
        panelCanvas.text(saveNames[i], x, y+rowHeight*(i-scroll));
      }

      // Draw the scroll bar
      panelCanvas.strokeWeight(2);
      int d = saveNames.length - numDisplayed;
      if (d > 0) {
        panelCanvas.fill(120);
        panelCanvas.rect(x+w-SCROLLWIDTH, y, SCROLLWIDTH, fakeHeight);
        if (scrolling) {
          panelCanvas.fill(40);
        } else {
          panelCanvas.fill(70);
        }
        panelCanvas.stroke(0);
        panelCanvas.rect(x+w-SCROLLWIDTH, y+(fakeHeight-fakeHeight/(d+1))*scroll/d, SCROLLWIDTH, fakeHeight/(d+1));
      }

      panelCanvas.popStyle();
    }

    public boolean moveOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+fakeHeight;
    }
    public boolean pointOver() {
      return moveOver();
    }

    public int hoveringOption() {
      int s = (mouseY-yOffset-y)/rowHeight;
      if (!(0 <= s && s < numDisplayed)) {
        return selected;
      }
      return s+scroll;
    }
  }




  class DropDown extends Element {
    String[] options;  // Either strings or floats
    int selected, bgColour, textSize;
    String name, optionTypes;
    boolean expanded, postExpandedEvent;

    DropDown(int x, int y, int w, int h, int bgColour, String name, String optionTypes, int textSize) {
      // h here means the height of one dropper box
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.bgColour = bgColour;
      this.name = name;
      this.expanded = false;
      this.optionTypes = optionTypes;
      this.textSize = textSize;
    }

    public void setOptions(String[] options) {
      this.options = options;
      LOGGER_MAIN.finer("Options changed to: " + Arrays.toString(options));
    }

    public void setValue(String value) {
      for (int i=0; i < options.length; i++) {
        if (value.equals(options[i])) {
          selected = i;
          return;
        }
      }
      LOGGER_MAIN.warning("Invalid value, "+ value);
    }

    public void draw(PGraphics panelCanvas) {
      int hovering = hoveringOption();
      panelCanvas.pushStyle();

      // draw selected option
      panelCanvas.stroke(color(0));
      if (moveOver() && hovering == -1 && getElemOnTop()) {  // Hovering over top selected option
        panelCanvas.fill(brighten(bgColour, -20));
      } else {
        panelCanvas.fill(brighten(bgColour, -40));
      }
      panelCanvas.rect(x, y, w, h);
      panelCanvas.textAlign(LEFT, TOP);
      panelCanvas.textFont(getFont((min(h*0.8f, textSize))*jsManager.loadFloatSetting("text scale")));
      panelCanvas.fill(color(0));
      panelCanvas.text(String.format("%s: %s", name, options[selected]), x+3, y);

      // Draw expand box
      if (expanded) {
        panelCanvas.line(x+w-h, y+1, x+w-h/2, y+h-1);
        panelCanvas.line(x+w-h/2, y+h-1, x+w, y+1);
      } else {
        panelCanvas.line(x+w-h, y+h-1, x+w-h/2, y+1);
        panelCanvas.line(x+w-h/2, y+1, x+w, y+h-1);
      }

      // Draw other options
      if (expanded) {
        for (int i=0; i < options.length; i++) {
          if (i == selected) {
            panelCanvas.fill(brighten(bgColour, 50));
          } else {
            if (moveOver() && i == hovering && getElemOnTop()) {
              panelCanvas.fill(brighten(bgColour, 20));
            } else {
              panelCanvas.fill(bgColour);
            }
          }
          panelCanvas.rect(x, y+(i+1)*h, w, h);
          if (i == selected) {
            panelCanvas.fill(brighten(bgColour, 20));
          } else {
            panelCanvas.fill(0);
          }
          panelCanvas.text(options[i], x+3, y+(i+1)*h);
        }
      }

      panelCanvas.popStyle();
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType.equals("mouseClicked")) {
        int hovering = hoveringOption();
        if (moveOver()) {
          if (hovering == -1) {
            toggleExpanded();
          } else {
            events.add("valueChanged");
            selected = hovering;
            contract();
            events.add("stop events");
          }
        } else {
          contract();
        }
      }
      if (postExpandedEvent) {
        events.add("element to top");
        postExpandedEvent = false;
      }
      return events;
    }

    public void setSelected(String s) {
      for (int i=0; i < options.length; i++) {
        if (options[i].equals(s)) {
          selected = i;
          return;
        }
      }
      LOGGER_MAIN.warning("Invalid selected:"+s);
    }

    public void contract() {
      expanded = false;
    }

    public void expand() {
      postExpandedEvent = true;
      expanded = true;
    }

    public void toggleExpanded() {
      expanded = !expanded;
      if (expanded) {
        postExpandedEvent = true;
      }
    }

    public int getIntVal() {
      try {
        int val = Integer.parseInt(options[selected]);
        LOGGER_MAIN.finer("Value of dropdown "+ val);
        return val;
      }
      catch(IndexOutOfBoundsException e) {
        LOGGER_MAIN.severe("Selected option is out of bounds of dropbox " + selected);
        return -1;
      }
    }

    public String getStrVal() {
      try {
        String val = options[selected];
        LOGGER_MAIN.finer("Value of dropdown "+ val);
        return val;
      }
      catch(IndexOutOfBoundsException e) {
        LOGGER_MAIN.severe("Selected option is out of bounds of dropbox " + selected);
        return "";
      }
    }

    public float getFloatVal() {
      try {
        float val = Float.parseFloat(options[selected]);
        LOGGER_MAIN.finer("Value of dropdown "+ val);
        return val;
      }
      catch(IndexOutOfBoundsException e) {
        LOGGER_MAIN.severe("Selected option is out of bounds of dropbox " + selected);
        return -1;
      }
    }

    public int getOptionIndex() {
      return selected;
    }

    public boolean moveOver() {
      if (expanded) {
        return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset < y+h*(options.length+1);
      } else {
        return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset < y+h;
      }
    }
    public boolean pointOver() {
      return moveOver();
    }

    public int hoveringOption() {
      if (!expanded) {
        return -1;
      }
      return (mouseY-yOffset-y)/h-1;
    }
  }


  class Tickbox extends Element {
    boolean val;
    String name;

    Tickbox(int x, int y, int w, int h, boolean defaultVal, String name) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.val = defaultVal;
      this.name = name;
    }

    public void toggle() {
      val = !val;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType.equals("mouseClicked")) {
        if (moveOver()) {
          toggle();
          events.add("valueChanged");
        }
      }
      return events;
    }

    public boolean getState() {
      return val;
    }
    public void setState(boolean state) {
      LOGGER_MAIN.finer("Tickbox state changed to: "+ state);
      val = state;
    }

    public boolean moveOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+h && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
      return moveOver();
    }

    public void draw(PGraphics panelCanvas) {
      panelCanvas.pushStyle();

      panelCanvas.fill(color(255));
      panelCanvas.stroke(color(0));
      panelCanvas.rect(x, y, h*jsManager.loadFloatSetting("gui scale"), h*jsManager.loadFloatSetting("gui scale"));
      if (val) {
        panelCanvas.line(x+1, y+1, x+h*jsManager.loadFloatSetting("gui scale")-1, y+h*jsManager.loadFloatSetting("gui scale")-1);
        panelCanvas.line(x+h*jsManager.loadFloatSetting("gui scale")-1, y+1, x+1, y+h*jsManager.loadFloatSetting("gui scale")-1);
      }
      panelCanvas.fill(0);
      panelCanvas.textAlign(LEFT, CENTER);
      panelCanvas.textSize(8*jsManager.loadFloatSetting("text scale"));
      panelCanvas.text(name, x+h*jsManager.loadFloatSetting("gui scale")+5, y+h*jsManager.loadFloatSetting("gui scale")/2);
      panelCanvas.popStyle();
    }
  }

  class Tooltip extends Element {
    boolean visible;
    String text;
    boolean attacking;

    Tooltip() {
      hide();
      setText("");
    }

    public void show() {
      visible = true;
    }
    public void hide() {
      visible = false;
    }

    public ArrayList<String> getLines(String s) {
      try {
        int j = 0;
        ArrayList<String> lines = new ArrayList<String>();
        for (int i=0; i<s.length(); i++) {
          if (s.charAt(i) == '\n') {
            lines.add(s.substring(j, i));
            j=i+1;
          }
        }
        lines.add(s.substring(j, s.length()));
        return lines;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error occured getting lines of tooltip in: "+s, e);
        throw e;
      }
    }

    public float maxWidthLine(ArrayList<String> lines) {
      float ml = 0;
      for (int i=0; i<lines.size(); i++) {
        if (textWidth(lines.get(i)) > ml) {
          ml = textWidth(lines.get(i));
        }
      }
      return ml;
    }
    public void setText(String text) {
      if (!text.equals(this.text)) {
        LOGGER_MAIN.finest(String.format("Tooltip text changing to: '%s'", text.replace("\n", "\\n")));
      }
      this.text = text;
    }
    //String resourcesList(float[] resources){
    //  String returnString = "";
    //  boolean notNothing = false;
    //  for (int i=0; i<numResources;i++){
    //    if (resources[i]>0){
    //      returnString += roundDp(""+resources[i], 1)+ " " +resourceNames[i]+ ", ";
    //      notNothing = true;
    //    }
    //  }
    //  if (!notNothing)
    //    returnString += "Nothing/Unknown";
    //  else if(returnString.length()-2 > 0)
    //    returnString = returnString.substring(0, returnString.length()-2);
    //  return returnString;
    //}
    public String getResourceList(JSONArray resArray) {
      String returnString = "";
      try {
        for (int i=0; i<resArray.size(); i++) {
          JSONObject jo = resArray.getJSONObject(i);
          returnString += String.format("  %s %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
        throw e;
      }
      return returnString;
    }
    public String getResourceList(JSONArray resArray, float[] availableResources) {
      // Colouring for insufficient resources
      String returnString = "";
      try {
        for (int i=0; i<resArray.size(); i++) {
          JSONObject jo = resArray.getJSONObject(i);
          if (availableResources[jsManager.getResIndex(jo.getString("id"))] >= jo.getFloat("quantity")) { // Check if has enough resources
            returnString += String.format("  %s %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
          } else {
            returnString += String.format("  <r>%s</r> %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
        throw e;
      }
      return returnString;
    }

    public void setMoving(int turns, boolean splitting, Party party, int numUnitsSplitting, int cost, boolean is3D) {
      //Tooltip text if moving. Turns is the number of turns in move
      JSONObject jo = gameData.getJSONObject("tooltips");
      String t = "";
      if (splitting) {
        t = String.format("Split %d units from party and move them to cell.\n", numUnitsSplitting);
        int[] splittedQuantities = party.splittedQuantities(numUnitsSplitting);

        boolean anyEquipment = false;
        for (int i = 0; i < party.getAllEquipment().length; i ++){
          if (party.getEquipment(i) != -1){
            anyEquipment = true;
          }
        }
        if (anyEquipment){
          t += "\n\nThe equipment will be divided as follows:\n";
        }
        for (int i = 0; i < party.getAllEquipment().length; i ++){
          if (party.getEquipment(i) != -1){
            // If the party has something equipped for this class
            t += String.format("%s: New party will get %d, existing party will keep %d", jsManager.getEquipmentTypeDisplayName(i, party.getEquipment(i)), splittedQuantities[i], party.getEquipmentQuantity(i) - splittedQuantities[i]);
          }
        }
      } else if (turns == 0) {
        t = jo.getString("moving");
        if (is3D) {
          t += String.format("\nMovement Cost: %d", cost);
        }
      }
      if (turns > 0) {
        t += String.format(jo.getString("moving turns"), turns);
      }
      setText(t);
    }

    public void setSieging() {
      JSONObject jo = gameData.getJSONObject("tooltips");
      setText(jo.getString("siege"));
    }

    public void setAttacking(BigDecimal chance) {
      attacking = true;
      JSONObject jo = gameData.getJSONObject("tooltips");
      setText(String.format(jo.getString("attacking"), chance.toString()));
    }

    public void setBombarding(int damage) {
      setText(String.format("Perform a ranged attack on the party.\nThis will eliminate %d units of the other party", damage));
    }

    public void setBombarding() {
      setText(String.format("Perform a ranged attack on the party."));
    }

    public void setTurnsRemaining() {
      JSONObject jo = gameData.getJSONObject("tooltips");
      setText(jo.getString("turns remaining"));
    }
    public void setMoveButton() {
      JSONObject jo = gameData.getJSONObject("tooltips");
      setText(jo.getString("move button"));
    }
    public void setMerging(Party p1, Party p2, int unitsTransfered) {
      // p1 is being merged into
      JSONObject jo = gameData.getJSONObject("tooltips");
      int overflow = p1.getOverflow(unitsTransfered);
      String t = String.format(jo.getString("merging"), p2.id, p1.id, unitsTransfered-overflow, overflow);

      int[][] equipments = p1.mergeEquipment(p2, unitsTransfered-overflow);

      boolean hasEquipment = false;
      // New equipment quantities + types for merged party
      t += "\n\nMerged party equipment:";
      for (int i=0; i<jsManager.getNumEquipmentClasses(); i++){
        if (equipments[0][i] != -1){
          t += String.format("\n%d x %s", equipments[2][i], jsManager.getEquipmentTypeDisplayName(i, equipments[0][i]));
          hasEquipment = true;
        }
      }
      if (!hasEquipment){
        t += " None\n";
      }

      // New equipment quantities + types for overflow party
      if (overflow > 0){
        hasEquipment = false;
        t += "\n\nOverflow party equipment:";
        for (int i=0; i<jsManager.getNumEquipmentClasses(); i++){
          if (equipments[1][i] != -1){
            t += String.format("\n%d x %s", equipments[3][i], jsManager.getEquipmentTypeDisplayName(i, equipments[1][i]));
            hasEquipment = true;
          }
        }
        if (!hasEquipment){
          t += " None\n";
        }
      }

      //Merged party proficiencies
      float[] mergedProficiencies = p1.mergeProficiencies(p2, unitsTransfered-overflow);
      t += "\n\nMerged party proficiencies:\n";
      for (int i=0; i < jsManager.getNumProficiencies(); i ++){
        t += String.format("%s = %s\n", jsManager.indexToProficiencyDisplayName(i), roundDpTrailing(""+mergedProficiencies[i], 2));
      }

      if (overflow > 0){
        t += "\n\n(Proficiencies for overflow party are the same as the original party)";
      }

      setText(t);
    }

    public void setStockUpAvailable(Party p, float[] resources) {
      int[] equipment = p.equipment;
      String text = "";
      for (int i = 0; i < equipment.length; i++) {
        if (p.equipment[i] != -1) {
          JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
          int stockUpTo = min(p.getUnitNumber(), p.equipmentQuantities[i]+floor(resources[jsManager.getResIndex(equipmentObject.getString("id"))]));
          if (stockUpTo > p.equipmentQuantities[i]) {
            String equipmentName = equipmentObject.getString("display name");
            text += String.format("\n  Will stock %s up to %d.", equipmentName, stockUpTo);
          }
        }
      }
      setText("Stock up equipment. This will use all of the party's movement points."+text);
    }

    public void setStockUpUnavailable(Party p) {
      String text = "Stock up unavailable.";
      boolean hasEquipment = false;
      String list = "";
      for (int i = 0; i < p.equipment.length; i++) {
        if (p.equipment[i] != -1) {
          hasEquipment = true;
          JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
          String name = equipmentObject.getString("display name");
          JSONArray locations = equipmentObject.getJSONArray("valid collection sites");
          list += "\n  "+name+": ";
          for (int j = 0; j < locations.size(); j++) {
            list += locations.getString(j)+", ";
          }
          list = list.substring(0, list.length()-2);
        }
      }
      if (hasEquipment) {
        text += "\nThe following is where each currently equiped item can be stocked up at:\n"+list;
      } else {
        text += "\nThis party has no equipment selected so it cannot be stocked up.";
      }
      setText(text);
    }

    public void setTask(String task, float[] availibleResources, int movementPoints) {
      try {
        JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task);
        String t="";
        if (jo == null){
          setText("Problem");
          LOGGER_MAIN.warning("Could not find task:"+task);
          return;
        }
        if (!jo.isNull("description")) {
          t += jo.getString("description")+"\n\n";
        }
        if (!jo.isNull("initial cost")) {
          t += String.format("Initial Resource Cost:\n%s\n", getResourceList(jo.getJSONArray("initial cost"), availibleResources));
        }
        if (!jo.isNull("movement points")) {
          if (movementPoints >= jo.getInt("movement points")) {
            t += String.format("Movement Points: %d\n", jo.getInt("movement points"));
          } else {
            t += String.format("Movement Points: <r>%d</r>\n", jo.getInt("movement points"));
          }
        }
        if (!jo.isNull("action")) {
          t += String.format("Turns: %d\n", jo.getJSONObject("action").getInt("turns"));
        }
        if (t.length()>2 && (t.charAt(t.length()-1)!='\n' || t.charAt(t.length()-2)!='\n'))
          t += "\n";
        if (!jo.isNull("production")) {
          t += "Production/Turn/Unit:\n"+getResourceList(jo.getJSONArray("production"));
        }
        if (!jo.isNull("consumption")) {
          t += "Consumption/Turn/Unit:\n"+getResourceList(jo.getJSONArray("consumption"));
        }
        //Strip
        setText(t.replaceAll("\\s+$", ""));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error changing tooltip to task: "+task, e);
        throw e;
      }
    }

    public void setEquipment(int equipmentClass, int equipmentType, float availableResources[], Party party, boolean collectionAllowed){
      // Tooltip is hovering over equipment manager, specifically over one of the equipmment types
      String t="";
      try{
        if (equipmentClass >= jsManager.getNumEquipmentClasses()){
          LOGGER_MAIN.warning("equipment class out of bounds");
          return;
        }
        if (equipmentType > jsManager.getNumEquipmentTypesFromClass(equipmentClass)){
          LOGGER_MAIN.warning("equipment class out of bounds:"+equipmentType);
        } else if (equipmentType == jsManager.getNumEquipmentTypesFromClass(equipmentClass)){
          JSONObject jo = gameData.getJSONObject("tooltips");
          t += jo.getString("unequip");
          if (collectionAllowed){
            t += "All equipment will be returned to stockpile";
          }
          else{
            t += "Equipment will be destroyed";
          }
        } else{
          JSONObject equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(equipmentClass);
          if (equipmentClassJO == null){
            setText("Problem");
            LOGGER_MAIN.warning("Equipment class not found with tooltip:"+equipmentClass);
            return;
          }
          JSONObject equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(equipmentType);
          if (equipmentTypeJO == null){
            setText("Problem");
            LOGGER_MAIN.warning("Equipment type not found with tooltip:"+equipmentType);
            return;
          }
          if (!equipmentTypeJO.isNull("display name")) {
            t += equipmentTypeJO.getString("display name")+"\n\n";
          }

          if (!equipmentTypeJO.isNull("description")) {
            t += equipmentTypeJO.getString("description")+"\n\n";
          }

          // Using 'display multipliers' array so ordering is consistant
          if (!equipmentClassJO.isNull("display multipliers")){
            t += "Proficiency Bonus Multipliers:\n";
            for (int i=0; i < equipmentClassJO.getJSONArray("display multipliers").size(); i++){
              String multiplierName = equipmentClassJO.getJSONArray("display multipliers").getString(i);
              if (!equipmentTypeJO.isNull(multiplierName)){
                if (equipmentTypeJO.getFloat(multiplierName) > 0){
                  t += String.format("%s: <g>+%s</g>\n", multiplierName, roundDp("+"+equipmentTypeJO.getFloat(multiplierName), 2));
                } else {
                  t += String.format("%s: <r>%s</r>\n", multiplierName, roundDp(""+equipmentTypeJO.getFloat(multiplierName), 2));
                }
              }
            }
            t += "\n";
          }

          // Other attributes e.g. range
          if (!equipmentClassJO.isNull("other attributes")){
            t += "Other Attributes:\n";
            for (int i=0; i < equipmentClassJO.getJSONArray("other attributes").size(); i++){
              String attribute = equipmentClassJO.getJSONArray("other attributes").getString(i);
              if (!equipmentTypeJO.isNull(attribute)){
                t += String.format("%s: %s\n", attribute, roundDp("+"+equipmentTypeJO.getFloat(attribute), 0));
              }
            }
            t += "\n";
          }

          // Display other classes that are blocked (if applicable)
          if (!equipmentTypeJO.isNull("other class blocking")){
            t += "Equipment blocks other classes: ";
            for (int i=0; i < equipmentTypeJO.getJSONArray("other class blocking").size(); i++){
              if (i < equipmentTypeJO.getJSONArray("other class blocking").size()-1){
                t += String.format("%s, ", equipmentTypeJO.getJSONArray("other class blocking").getString(i));
              }
              else{
                t += String.format("%s", equipmentTypeJO.getJSONArray("other class blocking").getString(equipmentTypeJO.getJSONArray("other class blocking").size()-1));
              }
            }
            t += "\n\n";
          }

          // Display amount of equipment available vs needed for party
          int resourceIndex = 0;
          try{
            resourceIndex = jsManager.getResIndex(equipmentTypeJO.getString("id"));
          }
          catch (Exception e){
            LOGGER_MAIN.log(Level.WARNING, String.format("Error finding resource for equipment class:%d, type:%d", equipmentClass, equipmentType), e);
            throw e;
          }
          if (floor(availableResources[resourceIndex]) >= party.getUnitNumber()){
            t += String.format("Equipment Available: %d/%d", floor(availableResources[resourceIndex]), party.getUnitNumber());
          } else{
            t += String.format("Equipment Available: <r>%d</r>/%d", floor(availableResources[resourceIndex]), party.getUnitNumber());
          }

          // Show where equipment can be stocked up
          if (!equipmentTypeJO.isNull("valid collection sites")){
            t += String.format("\n\n%s can be stocked up at: ", equipmentTypeJO.getString("display name"));
            for (int i=0; i < equipmentTypeJO.getJSONArray("valid collection sites").size(); i ++){
              t += equipmentTypeJO.getJSONArray("valid collection sites").getString(i);
              if (i+1 < equipmentTypeJO.getJSONArray("valid collection sites").size()){
                t += ", ";
              }
            }
          }
          else{
            t += String.format("\n\n%s can be stocked up anywhere", equipmentTypeJO.getString("display name"));
          }

          if (party.getMovementPoints() != party.getMaxMovementPoints()){
            t += "\n<r>Equipment can only be changed\nif party has full movement points</r>";
          } else{
            t += "\n(Equipment can only be changed\nif party has full movement points)";
          }
        }

        setText(t.replaceAll("\\s+$", ""));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to equipment class:%dk, type:%d", equipmentClass, equipmentType), e);
        throw e;
      }
    }

    public void setProficiencies(int proficiencyIndex, Party party){
      String t="";
      JSONObject proficiencyJO;
      if (!(0 <= proficiencyIndex && proficiencyIndex < jsManager.getNumProficiencies())){
        LOGGER_MAIN.warning("Invalid proficiency index given:"+proficiencyIndex);
        return;
      }
      try{
        proficiencyJO = gameData.getJSONArray("proficiencies").getJSONObject(proficiencyIndex);
        if (!proficiencyJO.isNull("display name")){
          t += proficiencyJO.getString("display name") + "\n";
        }
        if (!proficiencyJO.isNull("tooltip")){
          t += proficiencyJO.getString("tooltip") + "\n";
        }
        float bonusMultiplier = party.getProficiencyBonusMultiplier(proficiencyIndex);
        if (bonusMultiplier != 0){
          ArrayList<String> bonusBreakdown = party.getProficiencyBonusMultiplierBreakdown(proficiencyIndex);
          t += "\nBonus breakdown:\n";
          for (int i = 0; i < bonusBreakdown.size(); i ++){
            t += bonusBreakdown.get(i);
          }
        }

        setText(t.replaceAll("\\s+$", ""));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to proficiencies index:%d", proficiencyIndex), e);
        throw e;
      }
    }

    public void setHoveringParty(Party p){
      String t = String.format("Party '%s'\n", p.id);
      for (int i=0; i < p.proficiencies.length; i++){
        t += String.format("\n%s=%s", jsManager.indexToProficiencyDisplayName(i), roundDpTrailing(""+p.getTotalProficiency(i), 2));
      }
      setText(t);
    }

    public void setResource(HashMap<String, Float> buildings, String resource) {
      try {
        String t = "";
        for (String building : buildings.keySet()) {
          if (buildings.get(resource)>0) {
            t += String.format("%s: +%f", building, buildings.get(resource));
          } else {
            t += String.format("%s: %f", building, buildings.get(resource));
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.WARNING, "Error changing tooltip to resource: "+resource, e);
        throw e;
      }
    }

    public void drawColouredLine(PGraphics canvas, String line, float startX, float startY, int colour, char indicatingChar) {
      int start=0, end=0;
      float tw=0;
      boolean coloured = false;
      try {
        while (end != line.length()) {
          start = end;
          if (coloured) {
            canvas.fill(colour);
            end = line.indexOf("</"+indicatingChar+">", end);
          } else {
            canvas.fill(0);
            end = line.indexOf("<"+indicatingChar+">", end);
          }
          if (end == -1) { // indexOf returns -1 when not found
            end = line.length();
          }
          canvas.text(line.substring(start, end).replace("<"+indicatingChar+">", "").replace("</"+indicatingChar+">", ""), startX+tw, startY);
          tw += canvas.textWidth(line.substring(start, end).replace("<"+indicatingChar+">", "").replace("</"+indicatingChar+">", ""));
          coloured = !coloured;
        };
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Invalid index used drawing line", e);
      }
    }

    public void draw(PGraphics panelCanvas) {
      if (visible && text.length() > 0) {
        ArrayList<String> lines = getLines(text);
        panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
        int tw = ceil(maxWidthLine(lines))+4;
        int gap = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
        int th = ceil(panelCanvas.textAscent()+panelCanvas.textDescent())*lines.size();
        int tx = round(between(0, mouseX-xOffset-tw/2, width-tw));
        int ty = round(between(0, mouseY-yOffset+20, height-th-20));
        panelCanvas.fill(200, 240);
        panelCanvas.stroke(0);
        panelCanvas.rectMode(CORNER);
        panelCanvas.rect(tx, ty, tw, th);
        panelCanvas.fill(0);
        panelCanvas.textAlign(LEFT, TOP);
        for (int i=0; i<lines.size(); i++) {
          if (lines.get(i).contains("<r>")) {
            drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap, color(255,0,0), 'r');
          } else if (lines.get(i).contains("<g>")) {
            drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap, color(50,255,50), 'g');
          } else {
            panelCanvas.text(lines.get(i), tx+2, ty+i*gap);
          }
        }
      }
    }
  }

  class NotificationManager extends Element {
    ArrayList<ArrayList<Notification>> notifications;
    int bgColour, textColour, displayNots, notHeight, topOffset, scroll, turn, numPlayers;
    Notification lastSelected;
    boolean scrolling;

    NotificationManager(int x, int y, int w, int h, int bgColour, int textColour, int displayNots, int turn, int numPlayers) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.turn = turn;
      this.bgColour = bgColour;
      this.textColour = textColour;
      this.displayNots = displayNots;
      this.notHeight = h/displayNots;
      this.notifications = new ArrayList<ArrayList<Notification>>();
      this.numPlayers = numPlayers;
      for (int i = 0; i < numPlayers; i ++){
        notifications.add(new ArrayList<Notification>());
      }
      this.scroll = 0;
      lastSelected = null;
      scrolling = false;
    }

    public boolean moveOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+notHeight*(notifications.get(turn).size()+1);
    }
    public boolean pointOver() {
      return moveOver();
    }

    public boolean mouseOver(int i) {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+notHeight*i+topOffset && mouseY-yOffset <= y+notHeight*(i+1)+topOffset;
    }

    public int findMouseOver() {
      if (!moveOver()) {
        return -1;
      }
      for (int i=0; i<notifications.get(turn).size(); i++) {
        if (mouseOver(i)) {
          return i;
        }
      }
      return -1;
    }
    public boolean hoveringDismissAll() {
      return x<mouseX-xOffset&&mouseX-xOffset<x+notHeight&&y<mouseY-yOffset&&mouseY-yOffset<y+topOffset;
    }

    public void turnChange(int turn) {
      this.turn = turn;
      this.scroll = 0;
    }

    public void dismiss(int i) {
      LOGGER_MAIN.fine(String.format("Dismissing notification at index: %d which equates to:%s", i, notifications.get(turn).get(i)));
      try {
        notifications.get(turn).remove(i);
        scroll = round(between(0, scroll, notifications.get(turn).size()-displayNots));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error dismissing notification", e);
        throw e;
      }
    }

    public void dismissAll() {
      // Dismisses all notification for the current player
      LOGGER_MAIN.fine("Dismissing all notifications");
      notifications.get(turn).clear();
    }

    public void reset() {
      // Clears all notificaitions for all players
      LOGGER_MAIN.fine("Dismissing notifications for all players");
      notifications.clear();
      for (int i = 0; i < numPlayers; i ++){
        notifications.add(new ArrayList<Notification>());
      }
    }

    public void post(Notification n, int turn) {
      try {
        LOGGER_MAIN.fine("Posting notification: "+n.name);
        notifications.get(turn).add(0, n);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.WARNING, "Failed to post notification", e);
        throw e;
      }
    }

    public void post(String name, int x, int y, int turnNum, int turn) {
      try {
        LOGGER_MAIN.fine(String.format("Posting notification: %s at cell:%s, %s turn:%d player:%d", name, x, y, turnNum, turn));
        notifications.get(turn).add(0, new Notification(name, x, y, turnNum));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.WARNING, "Failed to post notification", e);
        throw e;
      }
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mouseWheel") {
        float count = event.getCount();
        if (moveOver()) {
          scroll = round(between(0, scroll+count, notifications.get(turn).size()-displayNots));
        }
      }
      // Lazy fix for bug
      if (moveOver() && visible && active && !empty()) {
        events.add("stop events");
      }
      return events;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mousePressed") {
        if (moveOver() && mouseX-xOffset>x+w-20*jsManager.loadFloatSetting("gui scale") && mouseY-yOffset > topOffset && notifications.get(turn).size() > displayNots) {
          scrolling = true;
          scroll = round(between(0, (mouseY-yOffset-y-topOffset)*(notifications.get(turn).size()-displayNots+1)/(h-topOffset), notifications.get(turn).size()-displayNots));
        } else {
          scrolling = false;
        }
      }
      if (eventType == "mouseDragged") {
        if (scrolling && notifications.get(turn).size() > displayNots) {
          scroll = round(between(0, (mouseY-yOffset-y-topOffset)*(notifications.get(turn).size()-displayNots+1)/(h-topOffset), notifications.get(turn).size()-displayNots));
        }
      }
      if (eventType == "mouseClicked") {
        int hovering = findMouseOver();
        if (hovering >=0) {
          if (mouseX-xOffset<x+notHeight) {
            dismiss(hovering+scroll);
            events.add("notification dismissed");
          } else if (!(notifications.get(turn).size() > displayNots) || !(mouseX-xOffset>x+w-20*jsManager.loadFloatSetting("gui scale"))) {
            lastSelected = notifications.get(turn).get(hovering+scroll);
            events.add("notification selected");
          }
        } else if (mouseX-xOffset<x+notHeight && hoveringDismissAll()) {
          dismissAll();
        }
      }
      return events;
    }

    public boolean empty() {
      return notifications.get(turn).size() == 0;
    }

    public void draw(PGraphics panelCanvas) {
      if (empty())return;
      panelCanvas.pushStyle();
      panelCanvas.fill(bgColour);
      this.notHeight = (h-topOffset)/displayNots;
      panelCanvas.rect(x, y, w, notHeight);
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.fill(brighten(bgColour, -50));
      topOffset = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
      panelCanvas.rect(x, y, w, topOffset);
      panelCanvas.fill(textColour);
      panelCanvas.textAlign(CENTER, TOP);
      panelCanvas.text("Notification Manager", x+w/2, y);

      if (hoveringDismissAll() && getElemOnTop()) {
        panelCanvas.fill(brighten(bgColour, 80));
      } else {
        panelCanvas.fill(brighten(bgColour, -20));
      }
      panelCanvas.rect(x, y, notHeight, topOffset);
      panelCanvas.strokeWeight(3);
      panelCanvas.line(x+5, y+5, x+notHeight-5, y+topOffset-5);
      panelCanvas.line(x+notHeight-5, y+5, x+5, y+topOffset-5);
      panelCanvas.strokeWeight(1);

      int hovering = findMouseOver();
      for (int i=0; i<min(notifications.get(turn).size(), displayNots); i++) {

        if (hovering == i && getElemOnTop()) {
          panelCanvas.fill(brighten(bgColour, 20));
        } else {
          panelCanvas.fill(brighten(bgColour, -10));
        }
        panelCanvas.rect(x, y+i*notHeight+topOffset, w, notHeight);

        panelCanvas.fill(brighten(bgColour, -20));
        if (mouseX-xOffset<x+notHeight) {
          if (hovering == i) {
            panelCanvas.fill(brighten(bgColour, 80));
          } else {
            panelCanvas.fill(brighten(bgColour, -20));
          }
        }
        panelCanvas.rect(x, y+i*notHeight+topOffset, notHeight, notHeight);
        panelCanvas.strokeWeight(3);
        panelCanvas.line(x+5, y+i*notHeight+topOffset+5, x+notHeight-5, y+(i+1)*notHeight+topOffset-5);
        panelCanvas.line(x+notHeight-5, y+i*notHeight+topOffset+5, x+5, y+(i+1)*notHeight+topOffset-5);
        panelCanvas.strokeWeight(1);

        panelCanvas.fill(textColour);
        panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(LEFT, CENTER);
        panelCanvas.text(notifications.get(turn).get(i+scroll).name, x+notHeight+5, y+topOffset+i*notHeight+notHeight/2);
        panelCanvas.textAlign(RIGHT, CENTER);
        panelCanvas.text("Turn "+notifications.get(turn).get(i+scroll).turn, x-notHeight+w, y+topOffset+i*notHeight+notHeight/2);
      }

      //draw scroll
      int d = notifications.get(turn).size() - displayNots;
      if (d > 0) {
        panelCanvas.fill(brighten(bgColour, 100));
        panelCanvas.rect(x-20*jsManager.loadFloatSetting("gui scale")+w, y+topOffset, 20*jsManager.loadFloatSetting("gui scale"), h-topOffset);
        panelCanvas.fill(brighten(bgColour, -20));
        panelCanvas.rect(x-20*jsManager.loadFloatSetting("gui scale")+w, y+(h-topOffset-(h-topOffset)/(d+1))*scroll/d+topOffset, 20*jsManager.loadFloatSetting("gui scale"), (h-topOffset)/(d+1));
      }
      panelCanvas.popStyle();
    }
  }



  class TextBox extends Element {
    int textSize, bgColour, textColour;
    String text;
    boolean autoSizing;

    TextBox(int x, int y, int w, int h, int textSize, String text, int bgColour, int textColour) {
      //w=-1 means get width from text
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      if (this.w == -1)
        autoSizing = true;
      else
        autoSizing = false;
      this.textSize = textSize;
      this.bgColour = bgColour;
      this.textColour = textColour;
      setText(text);
    }

    public void setText(String text) {
      this.text = text;
      LOGGER_MAIN.finer("Text set to: " + text);
    }

    public void updateWidth(PGraphics panelCanvas) {
      if (autoSizing) {
        this.w = ceil(panelCanvas.textWidth(text))+10;
      }
    }

    public String getText() {
      return text;
    }

    public void setColour(int c) {
      bgColour = c;
    }

    public void draw(PGraphics panelCanvas) {
      panelCanvas.pushStyle();
      panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(CENTER, CENTER);
      panelCanvas.rectMode(CORNER);
      updateWidth(panelCanvas);
      if (bgColour != color(255, 255)) {
        panelCanvas.fill(bgColour);
        panelCanvas.rect(x, y, w, h);
      }
      panelCanvas.fill(textColour);
      panelCanvas.text(text, x+w/2, y+h/2);
      panelCanvas.popStyle();
    }
  }



  class ResourceSummary extends Element {
    float[] stockPile, net;
    String[] resNames;
    int numRes, scroll;
    boolean expanded;
    int[] timings;
    byte[] warnings;

    final int GAP = 10;
    final int FLASHTIMES = 500;

    ResourceSummary(int x, int y, int h, String[] resNames, float[] stockPile, float[] net) {
      this.x = x;
      this.y = y;
      this.h = h;
      this.resNames = resNames;
      this.numRes = resNames.length;
      this.stockPile = stockPile;
      this.net = net;
      this.expanded = false;
      this.timings = new int[resNames.length];
      this.warnings = new byte[resNames.length];
    }

    public void updateStockpile(float[] v) {
      try {
        stockPile = v;
        LOGGER_MAIN.finest("Stockpile update: " + Arrays.toString(v));
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.WARNING, "Error updating stockpile", e);
        throw e;
      }
    }
    public void updateNet(float[] v) {
      try {
        LOGGER_MAIN.finest("Net update: " + Arrays.toString(v));
        net = v;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.WARNING, "Error updating net", e);
        throw e;
      }
    }

    public void updateWarnings(byte[] v) {
      try {
        LOGGER_MAIN.finest("Warnings update: " + Arrays.toString(v));
        warnings = v;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.WARNING, "Error updating warnings", e);
        throw e;
      }
    }

    public void toggleExpand() {
      expanded = !expanded;
      LOGGER_MAIN.finest("Expanded changed to: " + expanded);
    }
    public String prefix(String v) {
      try {
        float i = Float.parseFloat(v);
        if (i >= 1000000)
          return (new BigDecimal(v).divide(new BigDecimal("1000000"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString()+"M";
        else if (i >= 1000)
          return (new BigDecimal(v).divide(new BigDecimal("1000"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString()+"K";

        return (new BigDecimal(v).divide(new BigDecimal("1"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error creating prefix", e);
        throw e;
      }
    }

    public String getResString(int i) {
      return resNames[i];
    }
    public String getStockString(int i) {
      String tempString = prefix(""+stockPile[i]);
      return tempString;
    }
    public String getNetString(int i) {
      String tempString = prefix(""+net[i]);
      if (net[i] >= 0) {
        return "+"+tempString;
      }
      return tempString;
    }
    public int columnWidth(int i) {
      int m=0;
      textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      m = max(m, ceil(textWidth(getResString(i))));
      textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      m = max(m, ceil(textWidth(getStockString(i))));
      textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      m = max(m, ceil(textWidth(getNetString(i))));
      return m;
    }
    public int totalWidth() {
      int tot = 0;
      for (int i=numRes-1; i>=0; i--) {
        if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0:1)) continue;
        tot += columnWidth(i)+GAP;
      }
      return tot;
    }

    public void flash(int i) {
      timings[i] = millis()+FLASHTIMES;
    }
    public int getFill(int i) {
      if (timings[i] < millis()) {
        return color(100);
      }
      return color(155*(timings[i]-millis())/FLASHTIMES+100, 100, 100);
    }

    public String getResourceAt(int x, int y) {
      return "";
    }

    public void draw(PGraphics panelCanvas) {
      int cw = 0;
      int w, yLevel, tw = totalWidth();
      panelCanvas.pushStyle();
      panelCanvas.textAlign(LEFT, TOP);
      panelCanvas.fill(120);
      panelCanvas.rect(width-tw-x-GAP/2, y, tw, h);
      panelCanvas.rectMode(CORNERS);
      for (int i=numRes-1; i>=0; i--) {
        if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0:1)) continue;
        w = columnWidth(i);
        panelCanvas.fill(getFill(i));
        panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
        panelCanvas.rect(width-cw+x-GAP/2, y, width-cw+x-GAP/2-(w+GAP), y+panelCanvas.textAscent()+panelCanvas.textDescent());
        cw += w+GAP;
        panelCanvas.line(width-cw+x-GAP/2, y, width-cw+x-GAP/2, y+h);
        panelCanvas.fill(0);

        yLevel=0;
        panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
        panelCanvas.text(getResString(i), width-cw, y);
        yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();

        panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
        if (warnings[i] == 1) {
          panelCanvas.fill(255, 127, 0);
        } else if (warnings[i] == 2){
          panelCanvas.fill(255, 0, 0);
        }
        panelCanvas.text(getStockString(i), width-cw, y+yLevel);
        yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();

        if (net[i] < 0)
          panelCanvas.fill(255, 0, 0);
        else
          panelCanvas.fill(0, 255, 0);
        panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
        panelCanvas.text(getNetString(i), width-cw, y+yLevel);
        yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();
      }
      panelCanvas.popStyle();
    }
  }



  class TaskManager extends Element {
    ArrayList<String> options;
    ArrayList<Integer> availableOptions;
    ArrayList<Integer> availableButOverBudgetOptions;
    int textSize;
    int scroll;
    int numDisplayed;
    int oldH;
    boolean taskMActive;
    boolean scrolling;
    int bgColour, strokeColour;
    private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
    final int SCROLLWIDTH = 20;
    PImage[] resizedImages;

    TaskManager(int x, int y, int w, int textSize, int bgColour, int strokeColour, String[] options, int numDisplayed) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.textSize = textSize;
      this.h = 10;
      this.bgColour = bgColour;
      this.strokeColour = strokeColour;
      this.options = new ArrayList<String>();
      this.availableOptions = new ArrayList<Integer>();
      this.availableButOverBudgetOptions = new ArrayList<Integer>();
      removeAllOptions();
      for (String option : options) {
        this.options.add(option);
      }
      resetAvailable();
      taskMActive = true;
      resetScroll();
      this.numDisplayed = numDisplayed;
      oldH = -1;
    }

    public void updateImages(){
      LOGGER_MAIN.finer("Resizing task images, h="+h);
      resizedImages = new PImage[taskImages.length];
      for (int i=0; i < taskImages.length; i ++){
        if (taskImages[i] != null){
          resizedImages[i] = taskImages[i].copy();
          resizedImages[i].resize(h, h);
        }
      }
    }

    public void resetScroll(){
      scroll = 0;
      scrolling = false;
    }

    public void setOptions(ArrayList<String> options) {
      LOGGER_MAIN.finer("Options changed to:["+String.join(", ", options));
      this.options = options;
      resetScroll();
    }
    public void addOption(String option) {
      LOGGER_MAIN.finer("Option added: " + option);
      this.options.add(option);
      resetScroll();
    }
    public void removeOption(String option) {
      LOGGER_MAIN.finer("Option removed: " + option);
      for (int i=0; i <options.size(); i++) {
        if (option.equals(options.get(i))) {
          options.remove(i);
        }
      }
      resetScroll();
    }
    public void removeAllOptions() {
      LOGGER_MAIN.finer("Options all removed");
      this.options.clear();
      resetScroll();
    }
    public void resetAvailable() {
      LOGGER_MAIN.finer("Available Options all removed");
      this.availableOptions.clear();
      resetScroll();
    }
    public void resetAvailableButOverBudget() {
      LOGGER_MAIN.finer("Available But Over Budget Options all removed");
      this.availableButOverBudgetOptions.clear();
      resetScroll();
    }
    public String getSelected() {
      return options.get(availableOptions.get(0));
    }
    public void makeAvailable(String option) {
      try {
        LOGGER_MAIN.finer("Making option availalbe: " + option);
        for (int i=0; i<availableOptions.size(); i++) {
          if (options.get(availableOptions.get(i)).equals(option)) {
            return;
          }
        }
        for (int i=0; i<options.size(); i++) {
          if (options.get(i).equals(option)) {
            this.availableOptions.add(i);
            return;
          }
        }
        resetScroll();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error making task available", e);
        throw e;
      }
    }
    public void makeAvailableButOverBudget(String option) {
      try {
        LOGGER_MAIN.finer("Making option available but over buject: " + option);
        for (int i=0; i<availableButOverBudgetOptions.size(); i++) {
          if (options.get(availableButOverBudgetOptions.get(i)).equals(option)) {
            return;
          }
        }
        for (int i=0; i<options.size(); i++) {
          if (options.get(i).equals(option)) {
            this.availableButOverBudgetOptions.add(i);
            return;
          }
        }
        resetScroll();
        LOGGER_MAIN.warning("Could not find option to make available but over budject:"+option);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error making task available", e);
        throw e;
      }
    }
    public void makeUnavailableButOverBudget(String option) {
      LOGGER_MAIN.finer("Making unavilablae but over over buject option:"+option);
      for (int i=0; i<options.size(); i++) {
        if (options.get(i).equals(option)) {
          this.availableButOverBudgetOptions.remove(i);
          return;
        }
      }
      resetScroll();
    }
    public void makeUnavailable(String option) {
      LOGGER_MAIN.finer("Making unavailable:"+option);
      for (int i=0; i<options.size(); i++) {
        if (options.get(i).equals(option)) {
          this.availableOptions.remove(i);
          return;
        }
      }
      resetScroll();
    }
    public void selectAt(int j) {
      LOGGER_MAIN.finer("Selecting based on position, " + j);
      if (j < availableOptions.size()) {
        int temp = availableOptions.get(0);
        availableOptions.set(0, availableOptions.get(j));
        availableOptions.set(j, temp);
      }
    }
    public void select(String s) {
      LOGGER_MAIN.finer("Selecting based on string: "+s);
      for (int j=0; j<availableOptions.size(); j++) {
        if (options.get(availableOptions.get(j)).equals(s)) {
          selectAt(j);
          return;
        }
      }
      LOGGER_MAIN.warning("String for selection not found: "+s);
    }
    public int getH(PGraphics panelCanvas) {
      return ceil(textSize*jsManager.loadFloatSetting("text scale")+5);
    }
    public boolean optionAvailable(int i) {
      for (int option : availableOptions) {
        if (option == i) {
          return true;
        }
      }
      return false;
    }
    public void draw(PGraphics panelCanvas) {
      panelCanvas.pushStyle();

      h = getH(panelCanvas); //Also sets the font
      if (h != oldH){
        updateImages();
        oldH = h;
      }

      //Draw background
      panelCanvas.strokeWeight(2);
      panelCanvas.stroke(0);
      panelCanvas.fill(170);
      panelCanvas.rect(x, y, w+1, h*numDisplayed+1);

      // Draw current task box
      panelCanvas.strokeWeight(1);
      panelCanvas.fill(brighten(bgColour, ONOFFSET));
      panelCanvas.stroke(strokeColour);
      panelCanvas.rect(x, y, w, h);
      panelCanvas.fill(0);
      panelCanvas.textAlign(LEFT, TOP);
      panelCanvas.text("Current Task: "+options.get(availableOptions.get(0)), x+5+h, y);
      if (resizedImages[availableOptions.get(0)] != null){
        panelCanvas.image(resizedImages[availableOptions.get(0)], x+3, y);
      }

      // Draw other tasks
      int j;
      for (j=1; j < min(availableOptions.size()-scroll, numDisplayed); j++) {
        if (taskMActive && mouseOver(j) && getElemOnTop()) {
          panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET));
        } else {
          panelCanvas.fill(bgColour);
        }
        panelCanvas.rect(x, y+h*j, w, h);
        panelCanvas.fill(0);
        panelCanvas.text(options.get(availableOptions.get(j+scroll)), x+5+h, y+h*j);
        if (resizedImages[availableOptions.get(j+scroll)] != null){
          panelCanvas.image(resizedImages[availableOptions.get(j+scroll)], x+3, y+h*j);
        }
      }
      for (; j < min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed); j++) {
        panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET/2));
        panelCanvas.rect(x, y+h*j, w, h);
        panelCanvas.fill(120);
        panelCanvas.text(options.get(availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))), x+5+h, y+h*j);
        if (resizedImages[availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))] != null){
          panelCanvas.image(resizedImages[availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))], x+3, y+h*j);
        }
      }

      //draw scroll
      int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
      if (d > 0) {
        panelCanvas.strokeWeight(1);
        panelCanvas.fill(brighten(bgColour, 100));
        panelCanvas.rect(x-SCROLLWIDTH*jsManager.loadFloatSetting("gui scale")+w, y, SCROLLWIDTH*jsManager.loadFloatSetting("gui scale"), h*numDisplayed);
        panelCanvas.strokeWeight(2);
        panelCanvas.fill(brighten(bgColour, -20));
        panelCanvas.rect(x-SCROLLWIDTH*jsManager.loadFloatSetting("gui scale")+w, y+(h*numDisplayed-(h*numDisplayed)/(d+1))*scroll/d, SCROLLWIDTH*jsManager.loadFloatSetting("gui scale"), (h*numDisplayed)/(d+1));
      }

      panelCanvas.popStyle();
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
      if (eventType == "mouseMoved") {
        taskMActive = moveOver();
      }
      if (eventType == "mouseClicked" && button == LEFT) {
        for (int j=1; j < availableOptions.size(); j++) {
          if (mouseOver(j)) {
            if (d <= 0 || mouseX-xOffset<x+w-SCROLLWIDTH) {
              selectAt(j+scroll);
              events.add("valueChanged");
              scrolling = false;
            }
          }
        }
      } else if (eventType.equals("mousePressed")) {
        if (hovingOverScroll()) {
          // If hovering over scroll bar, set scroll to mouse pos
          scrolling = true;
          scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/(h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed)), d));
        } else {
          scrolling = false;
        }
      } else if (eventType.equals("mouseDragged")) {
        if (scrolling && d > 0) {
          // If scrolling, set scroll to mouse pos
          scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/(h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed)), d));
        }
      } else if (eventType.equals("mouseReleased")) {
        scrolling = false;
      }
      return events;
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mouseWheel") {
        float count = event.getCount();
        if (moveOver()) { // Check mouse over element
          if (availableOptions.size() + availableButOverBudgetOptions.size() > numDisplayed) {
            scroll = round(between(0, scroll+count, availableOptions.size() + availableButOverBudgetOptions.size()-numDisplayed));
            LOGGER_MAIN.finest("Changing scroll to: "+scroll);
          }
        }
      }
      return events;
    }

    public String findMouseOver() {
      try {
        int j;
        if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h) {
          return options.get(availableOptions.get(0));
        }
        for (j=0; j<min(availableOptions.size()-scroll, numDisplayed); j++) {
          if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+h*j && mouseY-yOffset <= y+h*(j+1)) {
            return options.get(availableOptions.get(j+scroll));
          }
        }
        for (; j<min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed); j++) {
          if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+h*j && mouseY-yOffset <= y+h*(j+1)) {
            return options.get(availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed)));
          }
        }
        return "";
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error finding mouse over option", e);
        throw e;
      }
    }
    public boolean hovingOverScroll(){
      int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
      return d > 0 && moveOver() && mouseX-xOffset>x+w-SCROLLWIDTH;
    }

    public boolean moveOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset > y && mouseY-yOffset < y+h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed);
    }
    public boolean pointOver() {
      return moveOver();
    }
    public boolean mouseOver(int j) {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset > y+h*j && mouseY-yOffset <= y+h*(j+1);
    }
  }





  class Button extends Element {
    private int x, y, w, h, cx, cy, textSize, textAlign;
    private int bgColour, strokeColour, textColour;
    private String state, text;
    private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
    private ArrayList<String> lines;

    Button(int x, int y, int w, int h, int bgColour, int strokeColour, int textColour, int textSize, int textAlign, String text) {
      state = "off";
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.bgColour = bgColour;
      this.strokeColour = strokeColour;
      this.textColour = textColour;
      this.textSize = textSize;
      this.textAlign = textAlign;
      this.text = text;
      centerCoords();

      setLines(text);
    }
    public void transform(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      centerCoords();
    }
    public void centerCoords() {
      cx = x+w/2;
      cy = y+h/2;
    }
    public void setText(String text) {
      LOGGER_MAIN.finer("Setting text to: " + text);
      this.text = text;
      setLines(text);
    }
    public void setColour(int colour) {
      LOGGER_MAIN.finest("Setting colour to: " + colour);
      this.bgColour = colour;
    }
    public String getText() {
      return this.text;
    }
    public void draw(PGraphics panelCanvas) {
      //println(xOffset, yOffset);
      int padding=0;
      float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
      panelCanvas.pushStyle();
      panelCanvas.fill(bgColour);
      if (state == "off") {
        panelCanvas.fill(bgColour);
      } else if (state == "hovering" && getElemOnTop()) {
        panelCanvas.fill(min(r+HOVERINGOFFSET, 255), min(g+HOVERINGOFFSET, 255), min(b+HOVERINGOFFSET, 255));
      } else if (state == "on") {
        panelCanvas.fill(min(r+ONOFFSET, 255), min(g+ONOFFSET, 255), min(b+ONOFFSET, 255));
      }
      panelCanvas.stroke(strokeColour);
      panelCanvas.strokeWeight(3);
      panelCanvas.rect(x, y, w, h);
      panelCanvas.noTint();
      panelCanvas.fill(textColour);
      panelCanvas.textAlign(textAlign, TOP);
      panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
      if (lines.size() == 1) {
        padding = h/10;
      }
      padding = (lines.size()*(int)(textSize*jsManager.loadFloatSetting("text scale"))-h/2)/2;
      for (int i=0; i<lines.size(); i++) {
        if (textAlign == CENTER) {
          panelCanvas.text(lines.get(i), cx, y+(h*0.9f-textSize*jsManager.loadFloatSetting("text scale"))/2);
        } else {
          panelCanvas.text(lines.get(i), x, y );
        }
      }
      panelCanvas.popStyle();
    }

    public ArrayList<String> setLines(String s) {
      LOGGER_MAIN.finer("Setting lines to: " + s);
      lines = new ArrayList<String>();
      try {
        int j = 0;
        for (int i=0; i<s.length(); i++) {
          if (s.charAt(i) == '\n') {
            lines.add(s.substring(j, i));
            j=i+1;
          }
        }
        lines.add(s.substring(j, s.length()));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error setting lines", e);
        throw e;
      }
      return lines;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mouseReleased") {
        if (state == "on") {
          events.add("clicked");
        }
        state = "off";
      }
      if (mouseOver()) {
        if (!state.equals("on")) {
          state = "hovering";
        }
        if (eventType == "mousePressed") {
          state = "on";
          if (jsManager.loadBooleanSetting("sound on")) {
            try {
              sfx.get("click3").play();
            }
            catch(Exception e) {
              LOGGER_MAIN.log(Level.SEVERE, "Error playing sound click 3", e);
              throw e;
            }
          }
        }
      } else {
        state = "off";
      }
      return events;
    }

    public Boolean mouseOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
      return mouseOver();
    }
  }





  class Slider extends Element {
    private int x, y, w, h, cx, cy, major, minor, lw, lx;
    private int padding = 20;
    private BigDecimal value, step, upper, lower;
    private float knobSize;
    private int KnobColour, bgColour, strokeColour, scaleColour;
    private boolean horizontal, pressed=false;
    final int boxHeight = 20, boxWidth = 10;
    private final int PRESSEDOFFSET = 50;
    private String name;
    boolean visible = true;

    Slider(int x, int y, int w, int h, int KnobColour, int bgColour, int strokeColour, int scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name) {
      this.lx = x;
      this.x = x;
      this.y = y;
      this.lw = w;
      this.w = w;
      this.h = h;
      this.KnobColour = KnobColour;
      this.bgColour = bgColour;
      this.strokeColour = strokeColour;
      this.scaleColour = scaleColour;
      this.major = major;
      this.minor = minor;
      this.upper = new BigDecimal(""+upper);
      this.lower = new BigDecimal(""+lower);
      this.horizontal = horizontal;
      this.step = new BigDecimal(""+step);
      this.value = new BigDecimal(""+value);
      this.name = name;
    }

    public void scaleKnob(PGraphics panelCanvas, BigDecimal value) {
      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      this.knobSize = max(this.knobSize, panelCanvas.textWidth(""+getInc(value)));
    }
    public void transform(int x, int y, int w, int h) {
      this.lx = x;
      this.x = x;
      this.lw = w;
      this.w = w;
      this.y = y;
      this.h = h;
    }
    public void setScale(float lower, float value, float upper, int major, int minor) {
      this.major = major;
      this.minor = minor;
      this.upper = new BigDecimal(""+upper);
      this.lower = new BigDecimal(""+lower);
      this.value = new BigDecimal(""+value);
    }
    public void setValue(float value) {
      LOGGER_MAIN.finer("Setting value to: " + value);
      setValue(new BigDecimal(""+value));
    }

    public void setValue(BigDecimal value) {
      LOGGER_MAIN.finer("Setting value to: " + value.toString());
      if (value.compareTo(lower) < 0) {
        this.value = lower;
      } else if (value.compareTo(upper)>0) {
        this.value = new BigDecimal(""+upper);
      } else {
        this.value = value.divideToIntegralValue(step).multiply(step);
      }
    }

    public float getValue() {
      return value.floatValue();
    }
    public BigDecimal getPreciseValue() {
      return value;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (button == LEFT) {
        if (mouseOver() && eventType == "mousePressed") {
          pressed = true;
          setValue((new BigDecimal(mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
          events.add("valueChanged");
        } else if (eventType == "mouseReleased") {
          pressed = false;
        }
        if (eventType == "mouseDragged" && pressed) {
          setValue((new BigDecimal(mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
          events.add("valueChanged");
        }
      }
      return events;
    }

    public Boolean mouseOver() {
      try {
        BigDecimal range = upper.subtract(lower);
        int xKnobPos = round(x+(value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue())-knobSize/2);
        return (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h) ||
          (mouseX-xOffset >= xKnobPos && mouseX-xOffset <= xKnobPos+knobSize && mouseY-yOffset >= y && mouseY-yOffset <= y+h); // Over slider or knob box
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error finding if mouse over", e);
        throw e;
      }
    }
    public boolean pointOver() {
      return mouseOver();
    }

    public BigDecimal getInc(BigDecimal i) {
      return i.stripTrailingZeros();
    }

    public void draw(PGraphics panelCanvas) {
      if (!visible)return;
      BigDecimal range = upper.subtract(lower);
      float r = red(KnobColour), g = green(KnobColour), b = blue(KnobColour);
      panelCanvas.pushStyle();
      panelCanvas.fill(255, 100);
      panelCanvas.stroke(strokeColour, 50);
      //rect(lx, y, lw, h);
      //rect(xOffset+x, y+yOffset+padding+2, w, h-padding);
      panelCanvas.stroke(strokeColour);


      for (int i=0; i<=minor; i++) {
        panelCanvas.fill(scaleColour);
        panelCanvas.line(x+w*i/minor, y+padding+(h-padding)/6, x+w*i/minor, y+5*(h-padding)/6+padding);
      }
      for (int i=0; i<=major; i++) {
        panelCanvas.fill(scaleColour);
        panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER);
        panelCanvas.text(getInc((new BigDecimal(""+i).multiply(range).divide(new BigDecimal(""+major), 15, BigDecimal.ROUND_HALF_EVEN).add(lower))).toPlainString(), x+w*i/major, y+padding);
        panelCanvas.line(x+w*i/major, y+padding, x+w*i/major, y+h);
      }

      if (pressed) {
        panelCanvas.fill(min(r-PRESSEDOFFSET, 255), min(g-PRESSEDOFFSET, 255), min(b+PRESSEDOFFSET, 255));
      } else {
        panelCanvas.fill(KnobColour);
      }

      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(CENTER);
      panelCanvas.rectMode(CENTER);
      scaleKnob(panelCanvas, value);
      panelCanvas.rect(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2+padding/2, knobSize, boxHeight);
      panelCanvas.rectMode(CORNER);
      panelCanvas.fill(scaleColour);
      panelCanvas.text(getInc(value).toPlainString(), x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2+boxHeight/4+padding/2);
      panelCanvas.stroke(0);
      panelCanvas.textAlign(CENTER);
      panelCanvas.stroke(255, 0, 0);
      panelCanvas.line(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight/2+padding/2, x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight+padding/2);
      panelCanvas.stroke(0);
      panelCanvas.fill(0);
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(LEFT, BOTTOM);
      panelCanvas.text(name, x, y);
      panelCanvas.popStyle();
    }
  }




  class Text extends Element {
    int x, y, size, colour, align;
    PFont font;
    String text;

    Text(int x, int y, int size, String text, int colour, int align) {
      this.x = x;
      this.y = y;
      this.size = size;
      this.text = text;
      this.colour = colour;
      this.align = align;
    }
    public void translate(int x, int y) {
      this.x = x;
      this.y = y;
    }
    public void setText(String text) {
      this.text = text;
    }
    public void calcSize(PGraphics panelCanvas) {
      panelCanvas.textFont(getFont(size*jsManager.loadFloatSetting("text scale")));
      this.w = ceil(panelCanvas.textWidth(text));
      this.h = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
    }
    public void draw(PGraphics panelCanvas) {
      calcSize(panelCanvas);
      if (font != null) {
        panelCanvas.textFont(font);
      }
      panelCanvas.textAlign(align, TOP);
      panelCanvas.textFont(getFont(size*jsManager.loadFloatSetting("text scale")));
      panelCanvas.fill(colour);
      panelCanvas.text(text, x, y);
    }
    public boolean mouseOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
      return mouseOver();
    }
  }





  class TextEntry extends Element {
    StringBuilder text;
    int x, y, w, h, textSize, textAlign, cursor, selected;
    int textColour, boxColour, borderColour, selectionColour;
    String allowedChars, name;
    final int BLINKTIME = 500;
    boolean texActive;

    TextEntry(int x, int y, int w, int h, int textAlign, int textColour, int boxColour, int borderColour, String allowedChars) {
      this.x = x;
      this.y = y;
      this.h = h;
      this.w = w;
      this.textColour = textColour;
      this.textSize = 10;
      this.textAlign = textAlign;
      this.boxColour = boxColour;
      this.borderColour = borderColour;
      this.allowedChars = allowedChars;
      text = new StringBuilder();
      selectionColour = brighten(selectionColour, 150);
      texActive = false;
    }
    TextEntry(int x, int y, int w, int h, int textAlign, int textColour, int boxColour, int borderColour, String allowedChars, String name) {
      this.x = x;
      this.y = y;
      this.h = h;
      this.w = w;
      this.textColour = textColour;
      this.textSize = 20;
      this.textAlign = textAlign;
      this.boxColour = boxColour;
      this.borderColour = borderColour;
      this.allowedChars = allowedChars;
      this.name = name;
      text = new StringBuilder();
      selectionColour = brighten(selectionColour, 150);
      texActive = false;
    }

    public void setText(String t) {
      LOGGER_MAIN.finest("Changing text to: " + t);
      this.text = new StringBuilder(t);
    }
    public String getText() {
      return this.text.toString();
    }

    public void draw(PGraphics panelCanvas) {
      boolean showCursor = ((millis()/BLINKTIME)%2==0 || keyPressed) && texActive;
      panelCanvas.pushStyle();

      // Draw a box behind the text
      panelCanvas.fill(boxColour);
      panelCanvas.stroke(borderColour);
      panelCanvas.rect(x, y, w, h);
      panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(textAlign);
      // Draw selection box
      if (selected != cursor && texActive && cursor >= 0 ) {
        panelCanvas.fill(selectionColour);
        panelCanvas.rect(x+panelCanvas.textWidth(text.substring(0, min(cursor, selected)))+5, y+2, panelCanvas.textWidth(text.substring(min(cursor, selected), max(cursor, selected))), h-4);
      }

      // Draw the text
      panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(textAlign);
      panelCanvas.fill(textColour);
      panelCanvas.text(text.toString(), x+5, y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2, w, h);

      // Draw cursor
      if (showCursor) {
        panelCanvas.fill(0);
        panelCanvas.noStroke();
        panelCanvas.rect(x+panelCanvas.textWidth(text.toString().substring(0, cursor))+5, y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2, 1, textSize*jsManager.loadFloatSetting("text scale"));
      }
      if (name != null) {
        panelCanvas.fill(0);
        panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(LEFT);
        panelCanvas.text(name, x, y-12);
      }

      panelCanvas.popStyle();
    }

    public void resetSelection() {
      selected = cursor;
    }
    public void transform(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }

    public int getCursorPos(int mx, int my) {
      try {
        int i=0;
        for (; i<text.length(); i++) {
          textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
          if ((textWidth(text.substring(0, i)) + textWidth(text.substring(0, i+1)))/2 + x > mx)
            break;
        }
        if (0 <= i && i <= text.length() && y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2<= my && my <= y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2+textSize*jsManager.loadFloatSetting("text scale")) {
          return i;
        }
        return cursor;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting cursor position", e);
        throw e;
      }
    }

    public void doubleSelectWord() {
      try {
        if (!(y <= mouseY-yOffset && mouseY-yOffset <= y+h)) {
          return;
        }
        int c = getCursorPos(mouseX-xOffset, mouseY-yOffset);
        int i;
        for (i=min(c, text.length()-1); i>0; i--) {
          if (text.charAt(i) == ' ') {
            i++;
            break;
          }
        }
        cursor = (int)between(0, i, text.length());
        for (i=c; i<text.length(); i++) {
          if (text.charAt(i) == ' ') {
            break;
          }
        }
        LOGGER_MAIN.finer("Setting selected characetr to position: " + i);
        selected = i;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error double selecting word", e);
        throw e;
      }
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mouseClicked") {
        if (button == LEFT) {
          if (mouseOver()) {
            texActive = true;
          }
        }
      } else if (eventType == "mousePressed") {
        if (button == LEFT) {
          cursor = round(between(0, getCursorPos(mouseX-xOffset, mouseY-yOffset), text.length()));
          selected = getCursorPos(mouseX-xOffset, mouseY-yOffset);
        }
        if (!mouseOver()) {
          texActive = false;
        }
      } else if (eventType == "mouseDragged") {
        if (button == LEFT) {
          selected = getCursorPos(mouseX-xOffset, mouseY-yOffset);
        }
      } else if (eventType == "mouseDoubleClicked") {
        doubleSelectWord();
      }
      return events;
    }

    public ArrayList<String> keyboardEvent(String eventType, char _key) {
      ArrayList<String> events = new ArrayList<String>();
      if (texActive) {
        if (eventType == "keyTyped") {
          if (allowedChars.equals("") || allowedChars.contains(""+_key)) {
            if (cursor != selected) {
              text = new StringBuilder(text.substring(0, min(cursor, selected)) + text.substring(max(cursor, selected), text.length()));
              cursor = min(cursor, selected);
              resetSelection();
            }
            text.insert(cursor++, _key);
            resetSelection();
          }
        } else if (eventType == "keyPressed") {
          if (_key == '\n') {
            events.add("enterPressed");
            texActive = false;
          }
          if (_key == BACKSPACE) {
            if (selected == cursor) {
              if (cursor > 0) {
                text.deleteCharAt(--cursor);
                resetSelection();
              }
            } else {
              text = new StringBuilder(text.substring(0, min(cursor, selected)) + text.substring(max(cursor, selected), text.length()));
              cursor = min(cursor, selected);
              resetSelection();
            }
          }
          if (_key == CODED) {
            if (keyCode == LEFT) {
              cursor = max(0, cursor-1);
              resetSelection();
            }
            if (keyCode == RIGHT) {
              cursor = min(text.length(), cursor+1);
              resetSelection();
            }
          }
        }
      }
      return events;
    }

    public Boolean mouseOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
      return mouseOver();
    }
  }





  class ToggleButton extends Element {
    int bgColour, strokeColour;
    String name;
    boolean on;
    ToggleButton(int x, int y, int w, int h, int bgColour, int strokeColour, boolean value, String name) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.bgColour = bgColour;
      this.strokeColour = strokeColour;
      this.name = name;
      this.on = value;
    }
    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType == "mouseClicked"&&mouseOver()) {
        events.add("valueChanged");
        on = !on;
      }
      return events;
    }
    public void transform(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }
    public boolean getState() {
      return on;
    }
    public void setState(boolean state) {
      LOGGER_MAIN.finest("Setting toggle state to: " + state);
      on = state;
    }
    public void draw(PGraphics panelCanvas) {
      panelCanvas.pushStyle();
      panelCanvas.fill(bgColour);
      panelCanvas.stroke(strokeColour);
      panelCanvas.rect(x, y, w, h);
      if (on) {
        panelCanvas.fill(0, 255, 0);
        panelCanvas.rect(x, y, w/2, h);
      } else {
        panelCanvas.fill(255, 0, 0);
        panelCanvas.rect(x+w/2, y, w/2, h);
      }
      panelCanvas.fill(0);
      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(LEFT, BOTTOM);
      panelCanvas.text(name, x, y);
      panelCanvas.popStyle();
    }
    public Boolean mouseOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
      return mouseOver();
    }
  }

  class BombardButton extends Button {
    PImage img;
    BombardButton (int x, int y, int w, int bgColour) {
      super(x, y, w, w, bgColour, color(0), color(0), 1, 0, "");
      img = bombardImage;
    }

    public void draw(PGraphics panelCanvas) {
      super.draw(panelCanvas);
      panelCanvas.image(img, super.x+2, super.y+2);
    }
  }

  class ResourceManagementTable extends Element {
    private int page;
    String[][] headings;
    ArrayList<ArrayList<String>> names;
    ArrayList<ArrayList<Float>> production, consumption, net, storage;
    int pages;
    int rows;
    int TEXTSIZE = 13;
    HashMap<String, PImage> tempEquipmentImages;
    int rowThickness;
    int rowGap;
    int columnGap;
    int headerSize;
    int imgHeight;

    ResourceManagementTable(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      pages = 2;
      names = new ArrayList<ArrayList<String>>();
      headings = new String[pages][];
      tempEquipmentImages = new HashMap<String, PImage>();
      rowThickness = ceil(TEXTSIZE*1.6f*jsManager.loadFloatSetting("text scale"));
      imgHeight = rowThickness;
      rowGap = ceil(TEXTSIZE/4*jsManager.loadFloatSetting("text scale"));
      columnGap = ceil(TEXTSIZE*jsManager.loadFloatSetting("text scale"));
      headerSize = ceil(1.3f*TEXTSIZE*jsManager.loadFloatSetting("text scale"));
      resizeImages();
    }

    public void update(String[][] headings,
    ArrayList<ArrayList<String>> names,
    ArrayList<ArrayList<Float>> production,
    ArrayList<ArrayList<Float>> consumption,
    ArrayList<ArrayList<Float>> net,
    ArrayList<ArrayList<Float>> storage) {
      this.names = names;
      this.production = production;
      this.consumption = consumption;
      this.net = net;
      this.storage = storage;
      this.headings = headings;
      this.rows = names.get(page).size();
      rowThickness = ceil(TEXTSIZE*1.6f*jsManager.loadFloatSetting("text scale"));
      rowGap = ceil(TEXTSIZE/4*jsManager.loadFloatSetting("text scale"));
      columnGap = ceil(TEXTSIZE*jsManager.loadFloatSetting("text scale"));
      headerSize = ceil(1.3f*TEXTSIZE*jsManager.loadFloatSetting("text scale"));
      resizeImages();
    }

    public void setPage(int p) {
      page = p;
      this.rows = names.get(page).size();
    }

    public void draw(PGraphics canvas) {
      canvas.fill(0);
      canvas.textAlign(LEFT, BOTTOM);
      canvas.textSize(headerSize);

      float[] cumulativeWidth = new float[headings[page].length+1];
      for (int i = 0; i < headings[page].length; i++) {
        cumulativeWidth[i+1] = canvas.textWidth(headings[page][i]) + columnGap + cumulativeWidth[i];
      }

      float[] headingXs = new float[headings[page].length];
      for (int i = 0; i < headings[page].length; i++) {
        float headingX;
        if (i == 0) {
          headingX = x+cumulativeWidth[i];
        } else {
          headingX = w-x-cumulativeWidth[headings[page].length]+cumulativeWidth[i];
        }
        headingXs[i] = headingX;
        canvas.text(headings[page][i], headingX, y+headerSize);
        canvas.line(headingX, y+headerSize, headingX+canvas.textWidth(headings[page][i]), y+headerSize);
      }


      int yPos = y+headerSize+2*rowGap;
      for (int i = 0; i < rows; i++) {
        canvas.fill(150);
        canvas.rect(x, yPos+i*(rowThickness+rowGap), w, rowThickness);
        canvas.fill(0);
        canvas.textSize(TEXTSIZE*jsManager.loadFloatSetting("text scale"));
        int offset = 0;
        int startColumn = 0;
        if (page == 1) {
          canvas.image(tempEquipmentImages.get(names.get(page).get(i)), x+2, yPos+i*(rowThickness+rowGap));
          offset = PApplet.parseInt(imgHeight/0.75f);
          canvas.text(
            jsManager.getEquipmentClass(jsManager.getEquipmentTypeClassFromID(names.get(page).get(i))[0]),
            headingXs[1], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
          startColumn = 1;
        }
        canvas.text(names.get(page).get(i), x+offset+columnGap, yPos+(i+1)*(rowThickness+rowGap) - rowGap);
        canvas.fill(0, 255, 0);
        canvas.text(production.get(page).get(i), headingXs[startColumn+1], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
        canvas.fill(255, 0, 0);
        canvas.text(consumption.get(page).get(i), headingXs[startColumn+2], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
        canvas.fill(0);
        canvas.text(net.get(page).get(i), headingXs[startColumn+3], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
        canvas.text(storage.get(page).get(i), headingXs[startColumn+4], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
      }
    }

    public void resizeImages(){
      // Resize equipment icons
      for (int c=0; c < jsManager.getNumEquipmentClasses(); c++){
        for (int t=0; t < jsManager.getNumEquipmentTypesFromClass(c); t++){
          try{
            String id = jsManager.getEquipmentTypeID(c, t);
            tempEquipmentImages.put(id, equipmentImages.get(id).copy());
            tempEquipmentImages.get(id).resize(ceil(PApplet.parseFloat(imgHeight)/0.75f), imgHeight);
          }
          catch (NullPointerException e){
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error resizing image for equipment icon class:%d, type:%d, id:%s", c, t, jsManager.getEquipmentTypeID(c, t)), e);
            throw e;
          }
        }
      }
    }
  }

  class HorizontalOptionsButton extends DropDown {
    HorizontalOptionsButton(int x, int y, int w, int h, int bgColour, int textSize, String[] options) {
      super(x, y, w, h, bgColour, "", "", textSize);
      setOptions(options);
      expanded = true;
    }

    public void draw(PGraphics canvas) {
      int hovering = hoveringOption();
      canvas.pushStyle();

      // draw selected option
      canvas.stroke(color(0));
      if (moveOver() && hovering == -1 && getElemOnTop()) {  // Hovering over top selected option
        canvas.fill(brighten(bgColour, -20));
      } else {
        canvas.fill(brighten(bgColour, -40));
      }
      canvas.rect(x, y, w, h);
      canvas.textAlign(LEFT, TOP);
      canvas.textFont(getFont((min(h*0.8f, textSize))*jsManager.loadFloatSetting("text scale")));
      canvas.fill(color(0));

      // Draw expand box
      canvas.line(x+w-h, y+1, x+w-h/2, y+h-1);
      canvas.line(x+w-h/2, y+h-1, x+w, y+1);

      int boxX = x;
      for (int i=0; i < options.length; i++) {
        if (i == selected) {
          canvas.fill(brighten(bgColour, 50));
        } else {
          if (moveOver() && i == hovering && getElemOnTop()) {
            canvas.fill(brighten(bgColour, 20));
          } else {
            canvas.fill(bgColour);
          }
        }
        canvas.rect(boxX, y, w, h);
        if (i == selected) {
          canvas.fill(brighten(bgColour, 20));
        } else {
          canvas.fill(0);
        }
        canvas.text(options[i], boxX+3, y);
        boxX += w;
      }
      canvas.popStyle();
    }
    public boolean moveOver() {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w*(options.length) && mouseY-yOffset >= y && mouseY-yOffset < y+h;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      ArrayList<String> events = new ArrayList<String>();
      if (eventType.equals("mouseClicked")) {
        int hovering = hoveringOption();
        if (moveOver()) {
          if (hovering != -1) {
            events.add("valueChanged");
            selected = hovering;
            contract();
            events.add("stop events");
          }
        }
      }
      return events;
    }

    public int hoveringOption() {
      return (mouseX-xOffset-x)/w;
    }
  }

  class Notification {
    String name;
    int x, y, turn;
    Notification(String name, int x, int y, int turn) {
      this.x = x;
      this.y = y;
      this.name = name;
      this.turn = turn;
    }
  }

  class Event {
    String id, type, panel;
    Event(String id, String panel, String type) {
      this.id = id;
      this.type = type;
      this.panel = panel;
    }
    public String info() {
      return "id:"+id+", type:"+type+", panel:"+panel;
    }
  }


  class Action {
    float turns, initialTurns;
    int type;
    String notification, terrain, building;
    Action(int type, String notification, float turns, String building, String terrain) {
      this.type = type;
      this.turns = turns;
      this.notification = notification;
      this.building = building;
      this.terrain = terrain;
      initialTurns = turns;
    }
  }

  //Events
  //Move sx sy ex ey
  //Split sx sy ex ey num
  //ChangeTask sx sy task
  //

  class GameEvent {
    String type;
  }

  class Move extends GameEvent {
    int startX, startY, endX, endY, num;
    Move(int startX, int startY, int endX, int endY, int num) {
      this.startX = startX;
      this.startY = startY;
      this.endX = endX;
      this.endY = endY;
      this.num = num;
    }
  }

  class Split extends GameEvent {
    int startX, startY, endX, endY, units;
    Split(int startX, int startY, int endX, int endY, int units) {
      this.startX = startX;
      this.startY = startY;
      this.endX = endX;
      this.endY = endY;
      this.units = units;
    }
  }

  class ChangeTask extends GameEvent {
    int x, y;
    int task;
    ChangeTask(int x, int y, int task) {
      this.x = x;
      this.y = y;
      this.task = task;
    }
  }

  class ChangePartyTrainingFocus extends GameEvent {
    int x, y;
    int newFocus;
    ChangePartyTrainingFocus(int x, int y, int newFocus) {
      this.x = x;
      this.y = y;
      this.newFocus = newFocus;
    }
  }

  class ChangeEquipment extends GameEvent{
    int equipmentClass;
    int newEquipmentType;
    ChangeEquipment(int equipmentClass, int newEqupmentType){
      this.equipmentClass = equipmentClass;
      this.newEquipmentType = newEqupmentType;
    }
  }

  class DisbandParty extends GameEvent {
    int x, y;
    DisbandParty(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  class StockUpEquipment extends GameEvent {
    int x, y;
    StockUpEquipment(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  class Bombard extends GameEvent {
    int fromX;
    int fromY;
    int toX;
    int toY;
    Bombard(int x1, int y1, int x2, int y2) {
      fromX = x1;
      fromY = y1;
      toX = x2;
      toY = y2;
    }
  }

  class UnitCapChange extends GameEvent {
    int x, y, newCap;
    UnitCapChange(int x, int y, int newCap) {
      this.x = x;
      this.y = y;
      this.newCap = newCap;
    }
  }

  class SetAutoStockUp extends GameEvent {
    int x, y;
    boolean enabled;
    SetAutoStockUp(int x, int y, boolean enabled){
      this.x = x;
      this.y = y;
      this.enabled = enabled;
    }
  }

  class EndTurn extends GameEvent {
  }
  class JSONManager {
    JSONObject menu, gameData, settings;
    JSONArray defaultSettings;

    JSONManager() {
      try {
        LOGGER_MAIN.fine("Initializing JSON Manager");
        menu = loadJSONObject(resourcesRoot+"json/menu.json");
        defaultSettings = loadJSONObject(resourcesRoot+"json/default_settings.json").getJSONArray("default settings");
        gameData = loadJSONObject(resourcesRoot+"json/data.json");
        try {
          settings = loadJSONObject("settings.json");
        }
        catch (NullPointerException e) {
          // Create new settings.json
          LOGGER_MAIN.info("creating new settings file");
          PrintWriter w = createWriter("settings.json");
          w.print("{}\n");
          w.flush();
          w.close();
          LOGGER_MAIN.info("Finished creating new settings file");
          settings = loadJSONObject("settings.json");
          LOGGER_MAIN.info("loading settings... ");
          loadDefaultSettings();
        }
        loadInitialSettings();
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading JSON", e);
        throw e;
      }
    }

    public float getRawProficiencyGain(String id){
     if (!gameData.getJSONObject("raw training gains").isNull(id)){
       return gameData.getJSONObject("raw training gains").getFloat(id);
     }
     else{
       LOGGER_MAIN.warning("No training gains found for id:"+id);
       return 0;
     }
    }

    public int getMaxTerrainMovementCost(){
      // Find the maximum possible terrain cost
      int mx = 0;
      for(int i=0; i < gameData.getJSONArray("terrain").size(); i++){
        mx = max(gameData.getJSONArray("terrain").getJSONObject(i).getInt("movement cost"), mx);
      }
      return mx;
    }

    public int getMaxTerrainSightCost(){
      // Find the maximum possible terrain cost
      int mx = 0;
      for(int i=0; i < gameData.getJSONArray("terrain").size(); i++){
        mx = max(gameData.getJSONArray("terrain").getJSONObject(i).getInt("sight cost"), mx);
      }
      return mx;
    }

    public String[] getProficiencies() {
      String[] returnArray = new String[getNumProficiencies()];
      for (int i = 0; i < returnArray.length; i ++) {
        returnArray[i] = indexToProficiencyDisplayName(i);
      }
      return returnArray;
    }

    public int getNumProficiencies() {
      try {
        JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
        return proficienciesJSON.size();
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.WARNING, "Error loading proficiencies from data.json", e);
        return 0;
      }
    }

    public String indexToProficiencyID(int index) {
      try {
        JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
        String rs = proficienciesJSON.getJSONObject(index).getString("id");
        if (rs == null) {
          LOGGER_MAIN.warning("Could not find proficiency id with index: "+index);
        }
        return rs;
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.warning("Proficiency index out of range: "+index);
        return "";
      }
    }

    public String indexToProficiencyDisplayName(int index) {
      try {
        if (index < 0) {
          LOGGER_MAIN.warning("Could not find proficiency display name with index: "+index);
          return "";
        }
        JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
        String rs = proficienciesJSON.getJSONObject(index).getString("display name");
        if (rs == null) {
          LOGGER_MAIN.warning("Could not find proficiency display name with index: "+index);
        }
        return rs;
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.warning("Proficiency index out of range: "+index);
        return "";
      }
    }

    public int proficiencyIDToIndex(String id) {
      try {
        JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
        for (int i = 0; i < proficienciesJSON.size(); i++) {
          if (proficienciesJSON.getJSONObject(i).getString("id").equals(id)) {
            return i;
          }
        }
        LOGGER_MAIN.severe("Could not find proficiency id: "+id);
        return -1;
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.WARNING, "Error loading proficiencies from data.json", e);
        return -1;
      }
    }

    public JSONObject getEquipmentObject(int classIndex, int typeIndex){
      try{
        return gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex);
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading equipment other class blocking. Class:%d, type:%d id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)), e);
        throw e;
      }
    }

    public String[] getOtherClassBlocking(int classIndex, int typeIndex){
      try{
        JSONObject typeObject = gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex);
        if (typeObject.isNull("other class blocking")){
          return null;
        }
        else {
          String[] rs = new String[typeObject.getJSONArray("other class blocking").size()];
          for (int i=0; i < typeObject.getJSONArray("other class blocking").size(); i ++){
            rs[i] = typeObject.getJSONArray("other class blocking").getString(i);
          }
          return rs;
        }
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading equipment other class blocking. Class:%d, type:%d id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)), e);
        throw e;
      }
    }

    public String getEquipmentImageFileName(int classIndex, int typeIndex){
      try{
        if (!gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex).isNull("img")){
          return "data/img/equipment/"+gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex).getString("img");
        }
        else{
          LOGGER_MAIN.warning(String.format("Could not find img file for equipment class:%d, type:%d, id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)));
          return "";
        }
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading equipment file name from data.json. Class:%d, type:%d id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)), e);
        throw e;
      }
    }

    public int getNumEquipmentTypesFromClass(int classType){
      // type is the index of the type in data.json
      if (classType<0){
        LOGGER_MAIN.warning("Class is invalid");
        return 0;
      }
      try {
        return gameData.getJSONArray("equipment").getJSONObject(classType).getJSONArray("types").size();
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public String[] getEquipmentFromClass(int type) {
      // type is the index of the type in data.json
      try {
        String[] rss = new String[getNumEquipmentTypesFromClass(type)];
        JSONArray types = gameData.getJSONArray("equipment").getJSONObject(type).getJSONArray("types");
        for(int i = 0; i < rss.length; i ++) {
          rss[i] = types.getJSONObject(i).getString("display name");
          if (rss[i] == null){
            LOGGER_MAIN.warning("No value for display name found for equipment type:"+type);
          }
        }
        return rss;
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public int getNumEquipmentClasses() {
      try {
        return gameData.getJSONArray("equipment").size();
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public String getEquipmentClass(int index) {
      try {
        return gameData.getJSONArray("equipment").getJSONObject(index).getString("id");
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public String getEquipmentClassDisplayName(int index) {
      try {
        return gameData.getJSONArray("equipment").getJSONObject(index).getString("display name");
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public String getEquipmentTypeID(int equipmentClass, int equipmentType){
      if (equipmentType == -1){
        LOGGER_MAIN.warning("No equipment type selected");
      }
      if (equipmentClass == -1){
        LOGGER_MAIN.warning("No equipment class selected");
      }
      try {
        return gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types").getJSONObject(equipmentType).getString("id");
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public int getEquipmentClassFromID(String classID){
      for (int c=0; c < getNumEquipmentClasses(); c ++){
        if (getEquipmentClass(c).equals(classID)){
          return c;
        }
      }
      LOGGER_MAIN.warning("Equipment class not found id:"+classID);
      return -1;
    }

    public int[] getEquipmentTypeClassFromID(String id){
      for (int c=0; c < getNumEquipmentClasses(); c ++){
        for (int t=0; t < getNumEquipmentTypesFromClass(c); t ++){
          if (getEquipmentTypeID(c, t).equals(id)){
            return new int[] {c, t};
          }
        }
      }
      LOGGER_MAIN.warning("Equipment type not found id:"+id);
      return null;
    }

    public String getEquipmentTypeDisplayName(int equipmentClass, int equipmentType){
      try {
        return gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types").getJSONObject(equipmentType).getString("display name");
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
        throw e;
      }
    }

    public int getResIndex(String s) {
      // Get the index for a resource
      try {
        return JSONIndex(gameData.getJSONArray("resources"), s);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting resource index for: " + s);
        throw e;
      }
    }
    public String getResString(int r) {
      // Get the string for an index
      try {
        return gameData.getJSONArray("resources").getJSONObject(r).getString("id");
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting resource string for: " + r, e);
        throw e;
      }
    }

    public boolean resourceIsEquipment(int r){
      // Check if a resource represents a type of equipment
      try {
        if (!gameData.getJSONArray("resources").getJSONObject(r).isNull("is equipment")){
          return gameData.getJSONArray("resources").getJSONObject(r).getBoolean("is equipment");
        } else{
          return false;
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error checking if resource is equipment index: " + r, e);
        throw e;
      }
    }

    public float getEffectivenessConstant(String type){
      try{
        if (!gameData.getJSONObject("effectiveness constants").isNull(type)){
          return gameData.getJSONObject("effectiveness constants").getFloat(type);
        }
        else {
          LOGGER_MAIN.warning("Error finding effectiveness type in data.json: "+type);
          return 0;
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting effectiveness constant: " + type, e);
        throw e;
      }
    }

    public void saveSetting(String id, int val) {
      // Save the setting to settings and write settings to file
      try {
        LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
        settings.setInt(id, val);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
        throw e;
      }
    }

    public void saveSetting(String id, float val) {
      // Save the setting to settings and write settings to file
      try {
        LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
        settings.setFloat(id, val);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
        throw e;
      }
    }

    public void saveSetting(String id, String val) {
      // Save the setting to settings and write settings to file
      try {
        LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
        settings.setString(id, val);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
        throw e;
      }
    }

    public void saveSetting(String id, boolean val) {
      // Save the setting to settings and write settings to file
      try {
        LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
        settings.setBoolean(id, val);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
        throw e;
      }
    }

    public void writeSettings() {
      try {
        LOGGER_MAIN.info("Saving settings to file");
        saveJSONObject(settings, "settings.json");
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error saving settings");
        throw e;
      }
    }

    public boolean hasFlag(String panelID, String elemID, String flag) {
      try {
        JSONObject panel = findJSONObject(menu.getJSONArray("states"), panelID);
        JSONObject elem = findJSONObject(panel.getJSONArray("elements"), elemID);
        JSONArray flags = elem.getJSONArray("flags");
        if (flags != null) {
          for (int i=0; i<flags.size(); i++) {
            if (flags.getString(i).equals(flag)) {
              return true;
            }
          }
        }
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.WARNING, String.format("Could not find flag for panel:'%s', element:'%s', flag:'%s'", panelID, elemID, flag), e);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error finding flag for panel:'%s', element:'%s', flag:'%s'", panelID, elemID, flag), e);
        throw e;
      }
      return false;
    }

    public void loadDefaultSettings() {
      // Reset all the settings to their default values
      LOGGER_MAIN.info("Loading default settings for all settings");
      try {
        for (int i=0; i<defaultSettings.size(); i++) {
          JSONObject setting = defaultSettings.getJSONObject(i);
          if (setting.getString("type").equals("int")) {
            saveSetting(setting.getString("id"), setting.getInt("value"));
          } else if (setting.getString("type").equals("float")) {
            saveSetting(setting.getString("id"), setting.getFloat("value"));
          } else if (setting.getString("type").equals("string")) {
            saveSetting(setting.getString("id"), setting.getString("value"));
          } else if (setting.getString("type").equals("boolean")) {
            saveSetting(setting.getString("id"), setting.getBoolean("value"));
          } else {
            LOGGER_MAIN.warning("Invalid setting type: " + setting.getString("id"));
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading default settings", e);
        throw e;
      }
    }

    public void loadInitialSettings() {
      // Set all the settings to either the default value, or the value already set
      LOGGER_MAIN.info("Loading initial settings");
      try {
        for (int i=0; i<defaultSettings.size(); i++) {
          JSONObject setting = defaultSettings.getJSONObject(i);
          if (settings.get(setting.getString("id")) == null) {
            if (setting.getString("type").equals("int")) {
              saveSetting(setting.getString("id"), setting.getInt("value"));
            } else if (setting.getString("type").equals("float")) {
              saveSetting(setting.getString("id"), setting.getFloat("value"));
            } else if (setting.getString("type").equals("string")) {
              saveSetting(setting.getString("id"), setting.getString("value"));
            } else if (setting.getString("type").equals("boolean")) {
              saveSetting(setting.getString("id"), setting.getBoolean("value"));
            } else {
              LOGGER_MAIN.warning("Invalid setting type: "+setting.getString("type"));
            }
          }
        }
        writeSettings();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading initial settings", e);
        throw e;
      }
    }

    public int loadIntSetting(String id) {
      // Load a setting that is an int
      try {
        return  settings.getInt(id);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading string setting: " + id, e);
        throw e;
      }
    }

    public float loadFloatSetting(String id) {
      // Load a setting that is an float
      try {
        return  settings.getFloat(id);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading float setting: "+ id, e);
        throw e;
      }
    }

    public String loadStringSetting(String id) {
      // Load a setting that is an string
      try {
        return  settings.getString(id);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading string setting " + id, e);
        throw e;
      }
    }

    public boolean loadBooleanSetting(String id) {
      // Load a setting that is an string
      try {
        return  settings.getBoolean(id);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading int setting: "+ id, e);
        throw e;
      }
    }

    public void saveDefault(String id) {
      LOGGER_MAIN.info("Saving all default settings");
      for (int i=0; i<defaultSettings.size(); i++) {
        if (defaultSettings.getJSONObject(i).getString("id").equals(id)) {
          JSONObject setting = defaultSettings.getJSONObject(i);
          if (setting.getString("type").equals("int")) {
            saveSetting(setting.getString("id"), setting.getInt("value"));
          } else if (setting.getString("type").equals("float")) {
            saveSetting(setting.getString("id"), setting.getFloat("value"));
          } else if (setting.getString("type").equals("string")) {
            saveSetting(setting.getString("id"), setting.getString("value"));
          } else if (setting.getString("type").equals("boolean")) {
            saveSetting(setting.getString("id"), setting.getBoolean("value"));
          } else {
            LOGGER_MAIN.warning("Invalid setting type: "+ setting.getString("type"));
          }
        }
      }
      writeSettings();
    }

    public JSONObject findJSONObject(JSONArray j, String id) {
      // search for a json object in a json array with correct id
      try {
        for (int i=0; i<j.size(); i++) {
          if (j.getJSONObject(i).getString("id").equals(id)) {
            return j.getJSONObject(i);
          }
        }
        return null;
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.WARNING, "Error finding JSON object with id likely cause by issue with code in data.json: "+ id, e);
        return null;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error finding JSON object with id: "+ id, e);
        throw e;
      }
    }

    public int findJSONObjectIndex(JSONArray j, String id) {
      // search for a json object in a json array with correct id
      try {
        for (int i=0; i<j.size(); i++) {
          if (j.getJSONObject(i).getString("id").equals(id)) {
            return i;
          }
        }
        return -1;
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.WARNING, "Error finding JSON object with id likely cause by issue with code in data.json: "+ id, e);
        return -1;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error finding JSON object with id: "+ id, e);
        throw e;
      }
    }

    public String getElementType(String panel, String element) {
      try {
        JSONArray elems = findJSONObject(menu.getJSONArray("states"), panel).getJSONArray("elements");
        return findJSONObject(elems, element).getString("type");
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error finding element type with id: "+ element + " on panel " + panel, e);
        throw e;
      }
    }

    public HashMap<String, String[]> getChangeStateButtons() {
      // Store all the buttons that when clicked change the state
      try {
        LOGGER_MAIN.fine("Loading buttons that change the state");
        HashMap returnHash = new HashMap<String, String[]>();
        JSONArray panels = menu.getJSONArray("states");
        for (int i=0; i<panels.size(); i++) {
          JSONObject panel = panels.getJSONObject(i);
          JSONArray panelElems = panel.getJSONArray("elements");
          for (int j=0; j<panelElems.size(); j++) {
            if (!panelElems.getJSONObject(j).isNull("new state")) {
              returnHash.put(panelElems.getJSONObject(j).getString("id"), new String[]{panelElems.getJSONObject(j).getString("new state"), panel.getString("id")});
            }
          }
        }
        return returnHash;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting change state buttons", e);
        throw e;
      }
    }

    public HashMap<String, String[]> getChangeSettingButtons() {
      // Store all the buttons that when clicked change a setting
      try {
        LOGGER_MAIN.fine("Loading buttons that change a setting");
        HashMap returnHash = new HashMap<String, String[]>();
        JSONArray panels = menu.getJSONArray("states");
        for (int i=0; i<panels.size(); i++) {
          JSONObject panel = panels.getJSONObject(i);
          JSONArray panelElems = panel.getJSONArray("elements");
          for (int j=0; j<panelElems.size(); j++) {
            if (!panelElems.getJSONObject(j).isNull("setting")) {
              returnHash.put(panelElems.getJSONObject(j).getString("id"), new String[]{panelElems.getJSONObject(j).getString("setting"), panel.getString("id")});
            }
          }
        }
        return returnHash;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting buttons that chagne settings", e);
        throw e;
      }
    }

    public String getSettingName(String id, String panelID) {
      // Gets the name of the setting for an element or null if it doesnt have a settting
      try {
        JSONObject panel = findJSONObject(menu.getJSONArray("states"), panelID);
        JSONObject element = findJSONObject(panel.getJSONArray("elements"), id);
        return element.getString("setting");
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting setting name with id:"+id+", panel: "+panelID, e);
        throw e;
      }
    }
    public String menuStateTitle(String id) {
      // Gets the titiel for menu state. Reutnrs null if there is no title defined
      try {
        JSONObject panel = findJSONObject(menu.getJSONArray("states"), id);
        return panel.getString("title");
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting menu state title with id:"+id, e);
        throw e;
      }
    }

    public void loadMenuElements(State state, float guiScale) {
      // Load all the menu panels in to menu state
      LOGGER_MAIN.info("Loading in menu elements using JSON");
      try {
        JSONArray panels = menu.getJSONArray("states");
        for (int i=0; i<panels.size(); i++) {
          JSONObject panel = panels.getJSONObject(i);
          state.addPanel(panel.getString("id"), 0, 0, width, height, true, true, color(255, 255, 255, 255), color(0));
          loadPanelMenuElements(state, panel.getString("id"), guiScale);
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading menu elements", e);
        throw e;
      }
    }

    public void loadPanelMenuElements(State state, String panelID, float guiScale) {
      // Load in the elements from JSON menu into panel
      // NOTE: "default value" in elements object means value is not saved to setting (and if not defined will be saved)
      try {
        int bgColour, strokeColour, textColour, textSize, major, minor;
        float x, y, w, h, scale, lower, upper, step;
        String type, id, text, setting;
        String[] options;
        JSONArray elements = findJSONObject(menu.getJSONArray("states"), panelID).getJSONArray("elements");


        scale = 20 * guiScale;

        for (int i=0; i<elements.size(); i++) {
          JSONObject elem = elements.getJSONObject(i);

          // Transform the normalised coordinates to screen coordinates
          x = elem.getInt("x")*scale+width/2;
          y = elem.getInt("y")*scale+height/2;
          w = elem.getInt("w")*scale;
          h = elem.getInt("h")*scale;

          // Other attributes
          type = elem.getString("type");
          id = elem.getString("id");

          // Optional attributes
          if (elem.isNull("bg colour")) {
            bgColour = color(100);
          } else {
            bgColour = elem.getInt("bg colour");
          }

          if (elem.isNull("setting")) {
            setting = "";
          } else {
            setting = elem.getString("setting");
          }

          if (elem.isNull("stroke colour")) {
            strokeColour = color(150);
          } else {
            strokeColour = elem.getInt("stroke colour");
          }

          if (elem.isNull("text colour")) {
            textColour = color(255);
          } else {
            textColour = elem.getInt("text colour");
          }

          if (elem.isNull("text size")) {
            textSize = 16;
          } else {
            textSize = elem.getInt("text size");
          }

          if (elem.isNull("text")) {
            text = "";
          } else {
            text = elem.getString("text");
          }

          if (elem.isNull("lower")) {
            lower = 0;
          } else {
            lower = elem.getFloat("lower");
          }

          if (elem.isNull("upper")) {
            upper = 1;
          } else {
            upper = elem.getFloat("upper");
          }

          if (elem.isNull("major")) {
            major = 2;
          } else {
            major = elem.getInt("major");
          }

          if (elem.isNull("minor")) {
            minor = 1;
          } else {
            minor = elem.getInt("minor");
          }

          if (elem.isNull("step")) {
            step = 0.5f;
          } else {
            step = elem.getFloat("step");
          }

          if (elem.isNull("options")) {
            options = new String[0];
          } else {
            options = elem.getJSONArray("options").getStringArray();
          }

          // Check if there is a defualt value. If not try loading from settings
          switch (type) {
          case "button":
            state.addElement(id, new Button((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text), panelID);
            break;
          case "slider":
            if (elem.isNull("default value")) {
              state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(150), bgColour, strokeColour, color(0), lower, loadFloatSetting(setting), upper, major, minor, step, true, text), panelID);
            } else {
              state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(150), bgColour, strokeColour, color(0), lower, elem.getFloat("default value"), upper, major, minor, step, true, text), panelID);
            }
            break;
          case "tickbox":
            if (elem.isNull("default value")) {
              state.addElement(id, new Tickbox((int)x, (int)y, (int)w, (int)h, loadBooleanSetting(setting), text), panelID);
            } else {
              state.addElement(id, new Tickbox((int)x, (int)y, (int)w, (int)h, elem.getBoolean("default value"), text), panelID);
            }
            break;
          case "dropdown":
            DropDown dd = new DropDown((int)x, (int)y, (int)w, (int)h, color(150), text, elem.getString("options type"), 10);
            dd.setOptions(options);
            if (elem.isNull("default value")) {
              switch (dd.optionTypes) {
              case "floats":
                dd.setSelected(""+jsManager.loadFloatSetting(setting));
                break;
              case "strings":
                dd.setSelected(jsManager.loadStringSetting(setting));
                break;
              case "ints":
                dd.setSelected(""+jsManager.loadIntSetting(setting));
                break;
              }
            } else {
              dd.setValue(elem.getString("default value"));
            }
            state.addElement(id, dd, panelID);
            break;
          default:
            LOGGER_MAIN.warning("Invalid element type: "+ type);
            break;
          }
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading menu elements", e);
        throw e;
      }
    }

    public boolean resourceExists(String id) {
      for (int i = 0; i < gameData.getJSONArray("resources").size(); i++) {
        if (gameData.getJSONArray("resources").getJSONObject(i).getString("id").equals(id)) {
          return true;
        }
      }
      return false;
    }

    public int getTaskIndex(String id) {
      try {
        return JSONIndex(gameData.getJSONArray("tasks"), id);
      } catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error getting resource index for: " + id);
        throw e;
      }
    }
  }


  class LoggerFormatter extends Formatter {

    public String format(LogRecord rec) {
      StringBuffer buffer = new StringBuffer(1000);
      buffer.append(rec.getSequenceNumber());
      buffer.append(" | ");
      buffer.append(rec.getLevel());
      buffer.append(" | ");
      buffer.append(calcDate(rec.getMillis()));
      buffer.append(" | ");
      buffer.append(rec.getMessage());
      buffer.append(" | ");
      buffer.append(rec.getSourceClassName());
      buffer.append(" | ");
      buffer.append(rec.getSourceMethodName());
      if (rec.getThrown() != null) {
        buffer.append(" | ");
        for (StackTraceElement st : rec.getThrown().getStackTrace()) {
          buffer.append("\n    at ");
          buffer.append(st.toString());
        }
      }
      buffer.append("\n");

      return buffer.toString();
    }

    public String calcDate(long millisecs) {
      SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss:SS");
      Date date = new Date(millisecs);
      return date_format.format(date);
    }

    public String getHead(Handler h) {
      return "\nStarting new session...\n";
    }

    public String getTail(Handler h) {
      return "\n";
    }
  }



  interface Map {
    public void updateMoveNodes(Node[][] nodes, Player[] players);
    public void cancelMoveNodes();
    public void removeTreeTile(int cellX, int cellY);
    public void setDrawingTaskIcons(boolean v);
    public void setDrawingUnitBars(boolean v);
    public void setHeightsForCell(int x, int y, float h);
    public void replaceMapStripWithReloadedStrip(int y);
    public boolean isPanning();
    public float getFocusedX();
    public float getFocusedY();
    public boolean isZooming();
    public float getTargetZoom();
    public float getZoom();
    public float getTargetOffsetX();
    public float getTargetOffsetY();
    public float getTargetBlockSize();
    public float[] targetCell(int x, int y, float zoom);
    public void loadSettings(float x, float y, float bs);
    public void unselectCell();
    public boolean mouseOver();
    public Node[][] getMoveNodes();
    public float scaleXInv();
    public float scaleYInv();
    public void updatePath(ArrayList<int[]> nodes);
    public void updateHoveringScale();
    public void doUpdateHoveringScale();
    public void cancelPath();
    public void setActive(boolean a);
    public void selectCell(int x, int y);
    public void generateShape();
    public void clearShape();
    public boolean isMoving();
    public void enableRocket(PVector pos, PVector vel);
    public void disableRocket();
    public void enableBombard(int range);
    public void disableBombard();
    public void setPlayerColours(int[] playerColours);
    public void updateVisibleCells(Cell[][] visibleCells);
  }


  public int getPartySize(Party p) {
    LOGGER_MAIN.finer("Getting party size for save");
    try {
      int totalSize = 0;
      totalSize += Character.BYTES*16;
      ByteBuffer[] actions = new ByteBuffer[p.actions.size()];
      int index = 0;
      for (Action a : p.actions) {
        int notificationSize;
        int terrainSize;
        int buildingSize;
        byte[] notification = new byte[0];
        byte[] terrain = new byte[0];
        byte[] building = new byte[0];
        if (a.notification == null) {
          notificationSize = 0;
        } else {
          notification = a.notification.getBytes();
          notificationSize = notification.length;
        }
        if (a.terrain == null) {
          terrainSize = 0;
        } else {
          terrain = a.terrain.getBytes();
          terrainSize = terrain.length;
        }
        if (a.building == null) {
          buildingSize = 0;
        } else {
          building = a.building.getBytes();
          buildingSize = building.length;
        }
        int actionLength = Float.BYTES*2+Integer.BYTES*4+notificationSize+terrainSize+buildingSize;
        totalSize += actionLength;
        actions[index] = ByteBuffer.allocate(actionLength);
        actions[index].putInt(notificationSize);
        actions[index].putInt(terrainSize);
        actions[index].putInt(buildingSize);
        actions[index].putFloat(a.turns);
        actions[index].putFloat(a.initialTurns);
        actions[index].putInt(a.type);
        if (notificationSize>0) {
          actions[index].put(notification);
        }
        if (terrainSize>0) {
          actions[index].put(terrain);
        }
        if (buildingSize>0) {
          actions[index].put(building);
        }
        index++;
      }
      totalSize+=Integer.BYTES; // For action count
      int pathSize = Integer.BYTES*(2*p.path.size()+1);
      totalSize += pathSize;

      ByteBuffer path = ByteBuffer.allocate(pathSize);
      path.putInt(p.path.size());
      for (int[] l : p.path) {
        path.putInt(l[0]);
        path.putInt(l[1]);
      }
      totalSize += Integer.BYTES * (7 + p.equipment.length * 2) + Float.BYTES * (1 + p.proficiencies.length)+1;


      ByteBuffer partyBuffer = ByteBuffer.allocate(totalSize);
      for (int i=0; i<16; i++) {
        if (i<p.id.length()) {
          partyBuffer.putChar(p.id.charAt(i));
        } else {
          partyBuffer.putChar(' ');
        }
      }

      partyBuffer.putInt(p.actions.size());
      for (ByteBuffer action : actions) {
        partyBuffer.put(action.array());
      }
      partyBuffer.put(path.array());
      partyBuffer.putInt(p.getUnitNumber());
      partyBuffer.putInt(p.getMovementPoints());
      partyBuffer.putInt(p.player);
      partyBuffer.putFloat(p.strength);
      partyBuffer.putInt(p.getTask());
      partyBuffer.putInt(p.pathTurns);
      partyBuffer.putInt(p.trainingFocus);
      for (int type: p.equipment) {
        partyBuffer.putInt(type);
      }
      for (int quantity: p.equipmentQuantities) {
        partyBuffer.putInt(quantity);
      }
      for (float prof: p.proficiencies) {
        partyBuffer.putFloat(prof);
      }
      partyBuffer.putInt(p.unitCap);
      partyBuffer.put(PApplet.parseByte(p.autoStockUp));
      p.byteRep = partyBuffer.array();
      return totalSize;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting party size", e);
      throw e;
    }
  }
  public void saveParty(ByteBuffer b, Party p) {
    try {
      b.put(p.byteRep);
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error saving party", e);
      throw e;
    }
  }
  public Party loadParty(ByteBuffer b, String id) {
    LOGGER_MAIN.finer("Loading party from save: "+id);
    try {
      int actionCount = b.getInt();
      ArrayList<Action> actions = new ArrayList<Action>();
      for (int i=0; i<actionCount; i++) {
        String notification;
        String terrain;
        String building;
        int notificationTextSize = b.getInt();
        int terrainTextSize = b.getInt();
        int buildingTextSize = b.getInt();
        Float turns = b.getFloat();
        Float initialTurns = b.getFloat();
        int type = b.getInt();
        if (notificationTextSize>0) {
          byte[] notificationTemp = new byte[notificationTextSize];
          b.get(notificationTemp);
          notification = new String(notificationTemp);
        } else {
          notification = null;
        }
        if (terrainTextSize>0) {
          byte[] terrainTemp = new byte[terrainTextSize];
          b.get(terrainTemp);
          terrain = new String(terrainTemp);
        } else {
          terrain = null;
        }
        if (buildingTextSize>0) {
          byte[] buildingTemp = new byte[buildingTextSize];
          b.get(buildingTemp);
          building = new String(buildingTemp);
        } else {
          building = null;
        }
        actions.add(new Action(type, notification, turns, building, terrain));
        actions.get(i).initialTurns = initialTurns;
      }
      int pathSize = b.getInt();
      ArrayList<int[]> path = new ArrayList<int[]>();
      for (int i=0; i<pathSize; i++) {
        path.add(new int[]{b.getInt(), b.getInt()});
      }
      int unitNumber = b.getInt();
      int movementPoints = b.getInt();
      int player = b.getInt();
      float strength = b.getFloat();
      int task = b.getInt();
      int pathTurns = b.getInt();
      int trainingFocus = b.getInt();
      int[] equipment = new int[jsManager.getNumEquipmentClasses()];
      for (int i = 0; i < jsManager.getNumEquipmentClasses(); i++) {
        equipment[i] = b.getInt();
      }
      int[] equipmentQuantities = new int[jsManager.getNumEquipmentClasses()];
      for (int i = 0; i < jsManager.getNumEquipmentClasses(); i++) {
        equipmentQuantities[i] = b.getInt();
      }
      float[] proficiencies = new float[jsManager.getNumProficiencies()];
      for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
        proficiencies[i] = b.getFloat();
      }
      int unitCap = b.getInt();
      boolean autoStockUp = b.get()==PApplet.parseByte(1);
      Party p = new Party(player, unitNumber, task, movementPoints, id.trim());
      p.strength = strength;
      p.pathTurns = pathTurns;
      p.actions = actions;
      if (path.size() > 0) {
        p.target = path.get(path.size()-1);
        p.loadPath(path);
      }
      p.trainingFocus = trainingFocus;
      p.equipment = equipment;
      p.equipmentQuantities = equipmentQuantities;
      p.proficiencies = proficiencies;
      p.unitCap = unitCap;
      p.autoStockUp = autoStockUp;

      LOGGER_MAIN.finer(String.format("Loaded party with id: %s, player: %d, unitNumber: %d pathSize: %d, actionCount: %d", id, player, unitNumber, pathSize, actionCount));

      return p;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading party", e);
      throw e;
    }
  }

  int SAVEVERSION = 2;


  class BaseMap extends Element {
    float[] heightMap;
    int mapWidth, mapHeight;
    long heightMapSeed;
    int[][] terrain;
    Party[][] parties;
    Building[][] buildings;
    boolean updateHoveringScale, drawingTaskIcons, drawingUnitBars;
    boolean cinematicMode;
    HashMap<Character, Boolean> keyState;
    boolean[][] fogMap;
    Cell[][] visibleCells;

    public void updateVisibleCells(Cell[][] visibleCells){
      this.visibleCells = visibleCells;
    }

    public void saveMap(String filename, int turnNumber, int turnPlayer, Player[] players) {
      LOGGER_MAIN.info("Starting saving progress");
      try {
        int partiesByteCount = 0;
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (parties[y][x] != null) {
              if (parties[y][x] instanceof Battle) {
                partiesByteCount+= Character.BYTES*16;
                partiesByteCount+=getPartySize(((Battle)parties[y][x]).attacker);
                partiesByteCount+=getPartySize(((Battle)parties[y][x]).defender);
              } else {
                partiesByteCount+=getPartySize(parties[y][x]);
              }
            }
            partiesByteCount++;
          }
        }
        int playersByteCount = ((3+players[0].resources.length)*Float.BYTES+4*Integer.BYTES+Character.BYTES*10+1+mapWidth*mapHeight)*players.length;
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES*10+Long.BYTES+Integer.BYTES*mapWidth*mapHeight*4+partiesByteCount+playersByteCount+Float.BYTES);
        buffer.putInt(-SAVEVERSION);
        LOGGER_MAIN.finer("Saving version: "+(-SAVEVERSION));
        buffer.putInt(mapWidth);
        LOGGER_MAIN.finer("Saving map width: "+mapWidth);
        buffer.putInt(mapHeight);
        LOGGER_MAIN.finer("Saving map height: "+mapHeight);
        buffer.putInt(partiesByteCount);
        LOGGER_MAIN.finer("Saving parties byte count: "+partiesByteCount);
        buffer.putInt(playersByteCount);
        LOGGER_MAIN.finer("Saving players' byte count: "+playersByteCount);
        buffer.putInt(jsManager.loadIntSetting("party size"));
        LOGGER_MAIN.finer("Saving party size: "+jsManager.loadIntSetting("party size"));
        buffer.putInt(players.length);
        LOGGER_MAIN.finer("Saving number of players: "+players.length);
        buffer.putInt(players[0].resources.length);
        LOGGER_MAIN.finer("Saving number of resources: "+players[0].resources.length);
        buffer.putInt(turnNumber);
        LOGGER_MAIN.finer("Saving turn number: "+turnNumber);
        buffer.putInt(turnPlayer);
        LOGGER_MAIN.finer("Saving player turn: "+turnPlayer);
        buffer.putLong(heightMapSeed);
        LOGGER_MAIN.finer("Saving height map seed: "+heightMapSeed);
        buffer.putFloat(jsManager.loadFloatSetting("water level"));
        LOGGER_MAIN.finer("Saving water level: "+jsManager.loadFloatSetting("water level"));

        LOGGER_MAIN.finer("Saving terrain and buildings");
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            buffer.putInt(terrain[y][x]);
          }
        }
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (buildings[y][x]==null) {
              buffer.putInt(-1);
              buffer.putInt(-1);
            } else {
              buffer.putInt(buildings[y][x].type);
              buffer.putInt(buildings[y][x].image_id);
            }
          }
        }
        LOGGER_MAIN.finer("Saving parties");
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (parties[y][x]==null) {
              buffer.put(PApplet.parseByte(0));
            } else if (parties[y][x] instanceof Battle) {
              buffer.put(PApplet.parseByte(2));
              for (int i=0; i<16; i++) {
                if (i<parties[y][x].id.length()) {
                  buffer.putChar(parties[y][x].id.charAt(i));
                } else {
                  buffer.putChar(' ');
                }
              }
              saveParty(buffer, ((Battle)parties[y][x]).attacker);
              saveParty(buffer, ((Battle)parties[y][x]).defender);
            } else {
              buffer.put(PApplet.parseByte(1));
              saveParty(buffer, parties[y][x]);
            }
          }
        }
        LOGGER_MAIN.finer("Saving players");
        for (Player p : players) {
          buffer.putFloat(p.cameraCellX);
          buffer.putFloat(p.cameraCellY);
          if (p.blockSize==0) {
            p.blockSize = jsManager.loadIntSetting("starting block size");
          }
          buffer.putFloat(p.blockSize);

          for (float r : p.resources) {
            buffer.putFloat(r);
          }
          buffer.putInt(p.cellX);
          buffer.putInt(p.cellY);
          buffer.putInt(p.colour);
          buffer.putInt(p.controllerType);
          for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
              if (p.visibleCells[y][x] == null) {
                buffer.put(PApplet.parseByte(0));
              } else {
                buffer.put(PApplet.parseByte(1));
              }
            }
          }
          buffer.put(PApplet.parseByte(p.cellSelected));
          for (int i=0; i<10; i++) {
            if (i<p.name.length()) {
              buffer.putChar(p.name.charAt(i));
            } else {
              buffer.putChar(' ');
            }
          }
        }
        LOGGER_MAIN.finer("Saving bandit memory");
        if (players[players.length-1].playerController instanceof BanditController) {
          BanditController bc = (BanditController)players[players.length-1].playerController;
          for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
              buffer.putInt(bc.cellsTargetedWeightings[y][x]);
            }
          }
        } else {
          LOGGER_MAIN.warning("Final player isn't a bandit");
        }
        LOGGER_MAIN.fine("Saving map to file");
        saveBytes(filename, buffer.array());
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error saving map", e);
        throw e;
      }
    }

    public MapSave loadMap(String filename, int resourceCountNew) {
      try {
        boolean versionCheckInt = false;
        byte tempBuffer[] = loadBytes(filename);
        int headerSize = Integer.BYTES*5;
        int versionSpecificData = 0;
        ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize);
        headerBuffer.put(Arrays.copyOfRange(tempBuffer, 0, headerSize));
        headerBuffer.flip();//need flip
        int versionCheck = -headerBuffer.getInt();
        if (versionCheck>0) {
          versionCheckInt = true;
          mapWidth = headerBuffer.getInt();
          versionSpecificData += Integer.BYTES;
        } else {
          LOGGER_MAIN.info("Loading old save with party size 1000");
          mapWidth = -versionCheck;
          jsManager.saveSetting("party size", 1000);
        }
        mapHeight = headerBuffer.getInt();
        int partiesByteCount = headerBuffer.getInt();
        int playersByteCount = headerBuffer.getInt();
        int dataSize = Long.BYTES+partiesByteCount+playersByteCount+(4+mapWidth*mapHeight*4)*Integer.BYTES+Float.BYTES+versionSpecificData;
        ByteBuffer buffer = ByteBuffer.allocate(dataSize);
        if (versionCheckInt) {
          buffer.put(Arrays.copyOfRange(tempBuffer, headerSize, headerSize+dataSize));
        } else {
          buffer.put(Arrays.copyOfRange(tempBuffer, headerSize-Integer.BYTES, headerSize-Integer.BYTES+dataSize));
        }
        buffer.flip();//need flip
        if (versionCheckInt) {
          jsManager.saveSetting("party size", buffer.getInt());
        }
        int playerCount = buffer.getInt();
        int resourceCountOld = buffer.getInt();
        int turnNumber = buffer.getInt();
        int turnPlayer = buffer.getInt();
        heightMapSeed = buffer.getLong();
        float newWaterLevel = buffer.getFloat();
        jsManager.saveSetting("water level", newWaterLevel);
        LOGGER_MAIN.finer("Loading water level: "+newWaterLevel);
        terrain = new int[mapHeight][mapWidth];
        parties = new Party[mapHeight][mapWidth];
        buildings = new Building[mapHeight][mapWidth];

        LOGGER_MAIN.finer("Loading terrain");
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            terrain[y][x] = buffer.getInt();
          }
        }
        LOGGER_MAIN.finer("Loading buildings");
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            int type = buffer.getInt();
            int image_id = buffer.getInt();
            if (type!=-1) {
              buildings[y][x] = new Building(type, image_id);
            }
            if (!versionCheckInt) {
              if (type==9) {
                terrain[y][x] = terrainIndex("quarry site stone");
                LOGGER_MAIN.finer("Changing old quarry tiles into quarry sites - only on old maps");
              }
            }
          }
        }
        LOGGER_MAIN.finer("Loading parties");
        int battleCount = 0;
        int partyCount = 0;
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            Byte partyType = buffer.get();
            if (partyType == 2) {
              char[] rawid;
              char[] p1id;
              char[] p2id;
              if (versionCheck>1) {
                rawid = new char[16];
                for (int i=0; i<16; i++) {
                  rawid[i] = buffer.getChar();
                }
                p1id = new char[16];
                for (int i=0; i<16; i++) {
                  p1id[i] = buffer.getChar();
                }
                p2id = new char[16];
              } else {
                rawid = String.format("Old Battle #%d", battleCount).toCharArray();
                battleCount++;
                p1id = String.format("Old Party #%s", partyCount).toCharArray();
                partyCount++;
                p2id = String.format("Old Party #%s", partyCount).toCharArray();
                partyCount++;
              }
              Party p1 = loadParty(buffer, new String(p1id));

              if (versionCheck>1) {
                for (int i=0; i<16; i++) {
                  p2id[i] = buffer.getChar();
                }
              }
              Party p2 = loadParty(buffer, new String(p2id));
              float savedStrength = p1.strength;
              Battle b = new Battle(p1, p2, new String(rawid));
              b.attacker.strength = savedStrength;
              parties[y][x] = b;
            } else if (partyType == 1) {
              char[] rawid;
              if (versionCheck>1) {
                rawid = new char[16];
                for (int i=0; i<16; i++) {
                  rawid[i] = buffer.getChar();
                }
              } else {
                rawid = String.format("Old Party #%s", partyCount).toCharArray();
                partyCount++;
              }
              parties[y][x] = loadParty(buffer, new String(rawid));
            }
          }
        }
        LOGGER_MAIN.finer("Loading players, count="+playerCount);
        Player[] players = new Player[playerCount];
        for (int i=0; i<playerCount; i++) {
          float cameraCellX = buffer.getFloat();
          float cameraCellY = buffer.getFloat();
          float blockSize = buffer.getFloat();
          float[] resources = new float[resourceCountNew];
          LOGGER_MAIN.finer(String.format("player %d. Camera Position: (%f, %f) blocksize:%f, resources:%s", i, cameraCellX, cameraCellY, blockSize, Arrays.toString(resources)));
          for (int r=0; r<resourceCountOld; r++) {
            resources[r] = buffer.getFloat();
          }
          int selectedCellX = buffer.getInt();
          int selectedCellY = buffer.getInt();
          int colour = buffer.getInt();
          int controllerType = buffer.getInt();
          boolean[][] seenCells = new boolean[mapHeight][];
          for (int y = 0; y < mapHeight; y++) {
            seenCells[y] = new boolean[mapWidth];
            for (int x = 0; x < mapWidth; x++) {
              seenCells[y][x] = PApplet.parseBoolean(buffer.get());
            }
          }
          boolean cellSelected = PApplet.parseBoolean(buffer.get());
          char[] playerName = new char[10];
          if (versionCheck>1) {
            for (int j=0; j<10; j++) {
              playerName[j] = buffer.getChar();
            }
          } else {
            playerName = String.format("Player %d", i).toCharArray();
          }
          players[i] = new Player(cameraCellX, cameraCellY, blockSize, resources, colour, new String(playerName), controllerType, i);
          players[i].updateVisibleCells(terrain, buildings, parties, seenCells);
          players[i].cellSelected = cellSelected;
          players[i].cellX = selectedCellX;
          players[i].cellY = selectedCellY;
        }

        LOGGER_MAIN.finer("Loading bandit memory");
        if (players[players.length-1].playerController instanceof BanditController) {
          BanditController bc = (BanditController)players[players.length-1].playerController;
          for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
              bc.cellsTargetedWeightings[y][x] = buffer.getInt();
            }
          }
        } else {
          LOGGER_MAIN.warning("Final player isn't a bandit");
        }
        LOGGER_MAIN.fine("Seeding height map noise with: "+heightMapSeed);
        noiseSeed(heightMapSeed);
        generateNoiseMaps();
        LOGGER_MAIN.fine("Finished loading save");
        return new MapSave(heightMap, mapWidth, mapHeight, terrain, parties, buildings, turnNumber, turnPlayer, players);
      }

      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading map", e);
        throw e;
      }
    }


    public int toMapIndex(int x, int y, int x1, int y1) {
      try {
        return PApplet.parseInt(x1+x*jsManager.loadFloatSetting("terrain detail")+y1*jsManager.loadFloatSetting("terrain detail")*(mapWidth+1/jsManager.loadFloatSetting("terrain detail"))+y*pow(jsManager.loadFloatSetting("terrain detail"), 2)*(mapWidth+1/jsManager.loadFloatSetting("terrain detail")));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error converting coordinates to map index", e);
        throw e;
      }
    }

    public void setDrawingUnitBars(boolean v) {
      drawingUnitBars = v;
    }

    public void setDrawingTaskIcons(boolean v) {
      drawingTaskIcons = v;
    }

    //int getRandomGroundType(HashMap<Integer, Float> groundWeightings, float total){
    //  float randomNum = random(0, 1);
    //  float min = 0;
    //  int lastType = 1;
    //  for (int type: groundWeightings.keySet()){
    //    if(randomNum>min&&randomNum<min+groundWeightings.get(type)/total){
    //      return type;
    //    }
    //    min += groundWeightings.get(type)/total;
    //    lastType = type;
    //  }
    //  return lastType;
    //}
    //int getRandomGroundTypeAt(int x, int y, ArrayList<Integer> shuffledTypes, HashMap<Integer, Float> groundWeightings, float total){
    //  double randomNum = Math.cbrt(noise(x*MAPTERRAINNOISESCALE, y*MAPTERRAINNOISESCALE, 1)*2-1)*0.5+0.5;
    //  float min = 0;
    //  int lastType = 1;
    //  for (int type: shuffledTypes){
    //    if(randomNum>min&&randomNum<min+groundWeightings.get(type)/total){
    //      return type;
    //    }
    //    min += groundWeightings.get(type)/total;
    //    lastType = type;
    //  }
    //  return lastType;
    //}
    //int[][] smoothMap(int distance, int firstType, int[][] terrain){
    //  ArrayList<int[]> order = new ArrayList<int[]>();
    //  for (int y=0; y<mapHeight;y++){
    //    for (int x=0; x<mapWidth;x++){
    //      order.add(new int[] {x, y});
    //    }
    //  }
    //  Collections.shuffle(order);
    //  int[][] newMap = new int[mapHeight][mapWidth];
    //  for (int[] coord: order){
    //    if(terrain[coord[1]][coord[0]]==terrainIndex("water")){
    //      newMap[coord[1]][coord[0]] = terrain[coord[1]][coord[0]];
    //    } else {
    //      int[] counts = new int[NUMOFGROUNDTYPES+1];
    //      for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
    //        for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
    //          if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
    //            if(terrain[y1][x1]!=terrainIndex("water")){
    //              counts[terrain[y1][x1]]+=1;
    //            }
    //          }
    //        }
    //      }
    //      int highest = terrain[coord[1]][coord[0]];
    //      for (int i=firstType; i<=NUMOFGROUNDTYPES;i++){
    //        if (counts[i] > counts[highest]){
    //          highest = i;
    //        }
    //      }
    //      newMap[coord[1]][coord[0]] = highest;
    //    }
    //  }
    //  return newMap;
    //}
    public void generateTerrain() {
      try {
        LOGGER_MAIN.info("Generating terrain");
        noiseDetail(3, 0.25f);
        HashMap<Integer, Float> groundWeightings = new HashMap();
        for (Integer i=0; i<gameData.getJSONArray("terrain").size(); i++) {
          if (gameData.getJSONArray("terrain").getJSONObject(i).isNull("weighting")) {
            groundWeightings.put(i, jsManager.loadFloatSetting(gameData.getJSONArray("terrain").getJSONObject(i).getString("id")+" weighting"));
          } else {
            groundWeightings.put(i, gameData.getJSONArray("terrain").getJSONObject(i).getFloat("weighting"));
          }
        }

        float totalWeighting = 0;
        for (float weight : groundWeightings.values()) {
          totalWeighting+=weight;
        }
        //for(int i=0;i<jsManager.loadIntSetting("ground spawns");i++){
        //  int type = getRandomGroundType(groundWeightings, totalWeighting);
        //  int x = (int)random(mapWidth);
        //  int y = (int)random(mapHeight);
        //  if(isWater(x, y)){
        //    i--;
        //  } else {
        //    terrain[y][x] = type;
        //  }
        //}
        class TempTerrainDetail implements Comparable<TempTerrainDetail> {
          int x;
          int y;
          float noiseValue;
          TempTerrainDetail(int x, int y, float noiseValue) {
            super();
            this.x = x;
            this.y = y;
            this.noiseValue = noiseValue;
          }
          public int compareTo(TempTerrainDetail otherDetail) {
            if (this.noiseValue>otherDetail.noiseValue) {
              return 1;
            } else if (this.noiseValue<otherDetail.noiseValue) {
              return -1;
            } else {
              return 0;
            }
          }
        }
        ArrayList<TempTerrainDetail> cells = new ArrayList<TempTerrainDetail>();
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (isWater(x, y)) {
              terrain[y][x] = terrainIndex("water");
            } else {
              cells.add(new TempTerrainDetail(x, y, noise(x*MAPTERRAINNOISESCALE, y*MAPTERRAINNOISESCALE, 1)));
            }
          }
        }
        TempTerrainDetail[] cellsArray = new TempTerrainDetail[cells.size()];
        cells.toArray(cellsArray);
        Arrays.sort(cellsArray);
        int terrainIndex = 0;
        int totalBelow = 0;
        int lastType = 0;
        for (int type : groundWeightings.keySet()) {
          if (groundWeightings.get(type)>0) {
            while (PApplet.parseFloat(terrainIndex-totalBelow)/cells.size()<groundWeightings.get(type)/totalWeighting) {
              terrain[cellsArray[terrainIndex].y][cellsArray[terrainIndex].x] = type;
              cellsArray[terrainIndex].noiseValue = 0;
              terrainIndex++;
            }
            totalBelow = terrainIndex-1;
          }
          lastType = type;
        }
        for (TempTerrainDetail t : cellsArray) {
          if (t.noiseValue!=0) {
            terrain[t.y][t.x] = lastType;
            //println("map generation possible issue here");
          }
        }
        //ArrayList<int[]> order = new ArrayList<int[]>();
        //for (int y=0; y<mapHeight;y++){
        //  for (int x=0; x<mapWidth;x++){
        //    if(isWater(x, y)){
        //      terrain[y][x] = terrainIndex("water");
        //    } else {
        //      order.add(new int[] {x, y});
        //    }
        //  }
        //}
        //Collections.shuffle(order);
        //for (int[] coord: order){
        //  int x = coord[0];
        //  int y = coord[1];
        //  while (terrain[y][x] == 0||terrain[y][x]==terrainIndex("water")){
        //    int direction = (int) random(8);
        //    switch(direction){
        //      case 0:
        //        x= max(x-1, 0);
        //        break;
        //      case 1:
        //        x = min(x+1, mapWidth-1);
        //        break;
        //      case 2:
        //        y= max(y-1, 0);
        //        break;
        //      case 3:
        //        y = min(y+1, mapHeight-1);
        //        break;
        //      case 4:
        //        x = min(x+1, mapWidth-1);
        //        y = min(y+1, mapHeight-1);
        //        break;
        //      case 5:
        //        x = min(x+1, mapWidth-1);
        //        y= max(y-1, 0);
        //        break;
        //      case 6:
        //        y= max(y-1, 0);
        //        x= max(x-1, 0);
        //        break;
        //      case 7:
        //        y = min(y+1, mapHeight-1);
        //        x= max(x-1, 0);
        //        break;
        //    }
        //  }
        //  terrain[coord[1]][coord[0]] = terrain[y][x];
        //}
        //terrain = smoothMap(jsManager.loadIntSetting("smoothing"), 2, terrain);
        //terrain = smoothMap(jsManager.loadIntSetting("smoothing")+2, 1, terrain);
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (terrain[y][x] != terrainIndex("water") && (groundMaxRawHeightAt(x, y) > jsManager.loadFloatSetting("hills height")) || getMaxSteepness(x, y)>HILLSTEEPNESS) {
              terrain[y][x] = terrainIndex("hills");
            }
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error generating map", e);
        throw e;
      }
    }
    public void generateMap(int mapWidth, int mapHeight, int players) {
      try {
        LOGGER_MAIN.fine("Generating map");
        terrain = new int[mapHeight][mapWidth];
        buildings = new Building[mapHeight][mapWidth];
        parties = new Party[mapHeight][mapWidth];
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        heightMapSeed = (long)random(Long.MIN_VALUE, Long.MAX_VALUE);
        noiseSeed(heightMapSeed);
        generateNoiseMaps();
        generateTerrain();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error generating map", e);
        throw e;
      }
    }
    public void generateNoiseMaps() {
      try {
        LOGGER_MAIN.info("Generating noise map");
        noiseDetail(4, 0.5f);
        heightMap = new float[PApplet.parseInt((mapWidth+1/jsManager.loadFloatSetting("terrain detail"))*(mapHeight+1/jsManager.loadFloatSetting("terrain detail"))*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
        for (int y = 0; y<mapHeight; y++) {
          for (int y1 = 0; y1<jsManager.loadFloatSetting("terrain detail"); y1++) {
            for (int x = 0; x<mapWidth; x++) {
              for (int x1 = 0; x1<jsManager.loadFloatSetting("terrain detail"); x1++) {
                heightMap[toMapIndex(x, y, x1, y1)] = noise((x+x1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE, (y+y1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE);
              }
            }
            heightMap[toMapIndex(mapWidth, y, 0, y1)] = noise(((mapWidth+1))*MAPHEIGHTNOISESCALE, (y+y1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE);
          }
        }
        for (int x = 0; x<mapWidth; x++) {
          for (int x1 = 0; x1<jsManager.loadFloatSetting("terrain detail"); x1++) {
            heightMap[toMapIndex(x, mapHeight, x1, 0)] = noise((x+x1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE, (mapHeight)*MAPHEIGHTNOISESCALE);
          }
        }
        heightMap[toMapIndex(mapWidth, mapHeight, 0, 0)] = noise(((mapWidth+1))*MAPHEIGHTNOISESCALE, (mapHeight)*MAPHEIGHTNOISESCALE);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error generating noise map", e);
        throw e;
      }
    }
    public void setHeightsForCell(int x, int y, float h) {
      // Set all the heightmap heights in cell to h
      try {
        int cellIndex;
        for (int x1 = 0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
          for (int y1 = 0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
            cellIndex = toMapIndex(x, y, x1, y1);
            heightMap[cellIndex] = h;
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error setting height for cell with height %s and pos: (%s, %s)", x, y, h), e);
        throw e;
      }
    }
    public float getRawHeight(int x, int y, int x1, int y1) {
      try {
        if ((x>=0&&y>=0)&&((x<mapWidth||(x==mapWidth&&x1==0))&&(y<mapHeight||(y==mapHeight&&y1==0)))) {
          return max(heightMap[toMapIndex(x, y, x1, y1)], jsManager.loadFloatSetting("water level"));
        } else {
          // println("A request for the height at a tile outside the map has been made. Ideally this should be prevented earlier"); // Uncomment this when we want to fix it
          return jsManager.loadFloatSetting("water level");
        }
      }
      catch (ArrayIndexOutOfBoundsException e) {
        LOGGER_MAIN.warning(String.format("Uncaught request for height at (%s, %s) (%s, %s)", x, y, x1, y1));
        return jsManager.loadFloatSetting("water level");
      }
    }
    public float getRawHeight(int x, int y, float x1, float y1) {
      try {
        if ((x>=0&&y>=0)&&((x<mapWidth||(x==mapWidth&&x1==0))&&(y<mapHeight||(y==mapHeight&&y1==0)))) {
          float x2 = x1*jsManager.loadFloatSetting("terrain detail");
          float y2 = y1*jsManager.loadFloatSetting("terrain detail");
          float xVal1 = lerp(heightMap[toMapIndex(x, y, floor(x2), floor(y2))], heightMap[toMapIndex(x, y, ceil(x2), floor(y2))], x1);
          float xVal2 = lerp(heightMap[toMapIndex(x, y, floor(x2), ceil(y2))], heightMap[toMapIndex(x, y, ceil(x2), ceil(y2))], x1);
          float yVal = lerp(xVal1, xVal2, y1);
          return max(yVal, jsManager.loadFloatSetting("water level"));
        } else {
          // println("A request for the height at a tile outside the map has been made. Ideally this should be prevented earlier"); // Uncomment this when we want to fix it
          return jsManager.loadFloatSetting("water level");
        }
      }
      catch (ArrayIndexOutOfBoundsException e) {
        println("this message should never appear. Uncaught request for height at ", x, y, x1, y1);
        return jsManager.loadFloatSetting("water level");
      }
    }
    public float getRawHeight(int x, int y) {
      return getRawHeight(x, y, 0, 0);
    }
    public float getRawHeight(float x, float y) {
      if (x<0||y<0) {
        return jsManager.loadFloatSetting("water level");
      }
      return getRawHeight(PApplet.parseInt(x), PApplet.parseInt(y), (x-PApplet.parseInt(x)), (y-PApplet.parseInt(y)));
    }
    public float groundMinRawHeightAt(int x1, int y1) {
      try {
        int x = floor(x1);
        int y = floor(y1);
        return min(new float[]{getRawHeight(x, y), getRawHeight(x+1, y), getRawHeight(x, y+1), getRawHeight(x+1, y+1)});
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting min raw ground height at: (%s, %s)", x1, y1), e);
        throw e;
      }
    }
    public float groundMaxRawHeightAt(int x1, int y1) {
      try {
        int x = floor(x1);
        int y = floor(y1);
        return max(new float[]{getRawHeight(x, y), getRawHeight(x+1, y), getRawHeight(x, y+1), getRawHeight(x+1, y+1)});
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting max raw ground height at: (%s, %s)", x1, y1), e);
        throw e;
      }
    }
    public boolean isWater(int x, int y) {
      return groundMaxRawHeightAt(x, y) == jsManager.loadFloatSetting("water level");
    }

    public float getMaxSteepness(int x, int y) {
      try {
        float maxZ, minZ;
        maxZ = 0;
        minZ = 1;
        for (float y1 = y; y1<=y+1; y1+=1.0f/jsManager.loadFloatSetting("terrain detail")) {
          for (float x1 = x; x1<=x+1; x1+=1.0f/jsManager.loadFloatSetting("terrain detail")) {
            float z = getRawHeight(x1, y1);
            if (z>maxZ) {
              maxZ = z;
            } else if (z<minZ) {
              minZ = z;
            }
          }
        }
        return maxZ-minZ;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting max steepness at: (%s, %s)", x, y), e);
        throw e;
      }
    }
  }


  class Map2D extends BaseMap implements Map {
    final int EW, EH, INITIALHOLD=1000;
    float blockSize, targetBlockSize;
    float mapXOffset, mapYOffset, targetXOffset, targetYOffset, panningSpeed, resetTime;
    boolean panning=false, zooming=false;
    float mapMaxSpeed;
    int elementWidth;
    int elementHeight;
    float[] mapVelocity = {0, 0};
    int startX;
    int startY;
    int frameStartTime;
    int xPos, yPos;
    boolean zoomChanged;
    boolean mapFocused, mapActive;
    int selectedCellX, selectedCellY;
    boolean cellSelected;
    int partyManagementColour;
    Node[][] moveNodes;
    ArrayList<int[]> drawPath;
    boolean drawRocket;
    PVector rocketPosition;
    PVector rocketVelocity;
    boolean showingBombard;
    int bombardRange;
    int[] playerColours;
    Player[] players;

    Map2D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight, Player[] players) {
      LOGGER_MAIN.fine("Initialsing map");
      xPos = x;
      yPos = y;
      EW = w;
      EH = h;
      this.mapWidth = mapWidth;
      this.mapHeight = mapHeight;
      elementWidth = round(EW);
      elementHeight = round(EH);
      mapXOffset = 0;
      mapYOffset = 0;
      mapMaxSpeed = 15;
      this.terrain = terrain;
      this.parties = parties;
      this.buildings = buildings;
      this.mapWidth = mapWidth;
      this.mapHeight = mapHeight;
      limitCoords();
      frameStartTime = 0;
      cancelMoveNodes();
      cancelPath();
      heightMap = new float[PApplet.parseInt((mapWidth+1)*(mapHeight+1)*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
      this.keyState = new HashMap<Character, Boolean>();
      this.players = players;
    }


    public void generateShape() {
      cinematicMode = false;
      drawRocket = false;
      this.keyState = new HashMap<Character, Boolean>();
    }
    public void clearShape() {
    }
    public void updateHoveringScale() {
    }
    public void doUpdateHoveringScale() {
    }
    public void replaceMapStripWithReloadedStrip(int y) {
    }
    public boolean isMoving() {
      return mapVelocity[0] != 0 || mapVelocity[1] != 0;
    }
    public Node[][] getMoveNodes() {
      return moveNodes;
    }
    public float getTargetZoom() {
      return targetBlockSize;
    }
    public float getZoom() {
      return blockSize;
    }
    public boolean isZooming() {
      return zooming;
    }
    public boolean isPanning() {
      return panning;
    }
    public float getFocusedX() {
      return mapXOffset;
    }
    public float getFocusedY() {
      return mapYOffset;
    }
    public void removeTreeTile(int cellX, int cellY) {
      terrain[cellY][cellX] = terrainIndex("grass");
    }
    public void setActive(boolean a) {
      this.mapActive = a;
    }
    public void selectCell(int x, int y) {
      cellSelected = true;
      selectedCellX = x;
      selectedCellY = y;
    }
    public void unselectCell() {
      cellSelected = false;
      showingBombard = false;
    }
    public void setPanningSpeed(float s) {
      panningSpeed = s;
    }
    public void limitCoords() {
      mapXOffset = min(max(mapXOffset, -mapWidth*blockSize+elementWidth*0.5f), elementWidth*0.5f);
      mapYOffset = min(max(mapYOffset, -mapHeight*blockSize+elementHeight*0.5f), elementHeight*0.5f);
    }
    public void reset() {
      mapXOffset = 0;
      mapYOffset = 0;
      mapVelocity[0] = 0;
      mapVelocity[1] = 0;
      blockSize = min(elementWidth/(float)mapWidth, elementWidth/10);
      setPanningSpeed(0.02f);
      resetTime = millis();
      frameStartTime = 0;
      cancelMoveNodes();
      showingBombard = false;
    }
    public void loadSettings(float x, float y, float bs) {
      targetCell(PApplet.parseInt(x), PApplet.parseInt(y), bs);
    }
    public void targetOffset(float x, float y) {
      targetXOffset = x;
      targetYOffset = y;
      limitCoords();
      panning = true;
    }
    public void targetZoom(float bs) {
      zooming = true;
      targetBlockSize = bs;
    }
    public float getTargetOffsetX() {
      return targetXOffset;
    }
    public float getTargetOffsetY() {
      return targetYOffset;
    }
    public float getTargetBlockSize() {
      return targetBlockSize;
    }
    public float [] targetCell(int x, int y, float bs) {
      LOGGER_MAIN.finer(String.format("Targetting cell: %s, %s block size: ", x, y, bs));
      targetBlockSize = bs;
      targetXOffset = -(x+0.5f)*targetBlockSize+elementWidth/2+xPos;
      targetYOffset = -(y+0.5f)*targetBlockSize+elementHeight/2+yPos;
      panning = true;
      zooming = true;
      return new float[]{targetXOffset, targetYOffset, targetBlockSize};
    }
    public void focusMapMouse() {
      // based on mouse click
      if (mouseOver()) {
        targetXOffset = -scaleXInv()*blockSize+elementWidth/2+xPos;
        targetYOffset = -scaleYInv()*blockSize+elementHeight/2+yPos;
        limitCoords();
        panning = true;
      }
    }
    public void resetTarget() {
      targetXOffset = mapXOffset;
      targetYOffset = mapYOffset;
      panning = false;
    }
    public void resetTargetZoom() {
      zooming = false;
      targetBlockSize = blockSize;
      setPanningSpeed(0.05f);
    }

    public void updateMoveNodes(Node[][] nodes, Player[] players) {
      moveNodes = nodes;
    }
    public void updatePath(ArrayList<int[]> nodes) {
      drawPath = nodes;
    }
    public void cancelMoveNodes() {
      moveNodes = null;
    }
    public void cancelPath() {
      drawPath = null;
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      if (eventType == "mouseWheel") {
        float count = event.getCount();
        if (mouseOver() && mapActive) {
          float zoom = pow(0.9f, count);
          float newBlockSize = max(min(blockSize*zoom, (float)elementWidth/10), (float)elementWidth/(float)jsManager.loadIntSetting("map size"));
          if (blockSize != newBlockSize) {
            mapXOffset = scaleX(((mouseX-mapXOffset-xPos)/blockSize))-xPos-((mouseX-mapXOffset-xPos)*newBlockSize/blockSize);
            mapYOffset = scaleY(((mouseY-mapYOffset-yPos)/blockSize))-yPos-((mouseY-mapYOffset-yPos)*newBlockSize/blockSize);
            blockSize = newBlockSize;
            limitCoords();
            resetTarget();
            resetTargetZoom();
          }
        }
      }
      return new ArrayList<String>();
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      if (button == LEFT&mapActive) {
        if (eventType=="mouseDragged" && mapFocused) {
          mapXOffset += (mouseX-startX);
          mapYOffset += (mouseY-startY);
          limitCoords();
          startX = mouseX;
          startY = mouseY;
          resetTarget();
          resetTargetZoom();
        }
        if (eventType == "mousePressed") {
          if (mouseOver()) {
            startX = mouseX;
            startY = mouseY;
            mapFocused = true;
          } else {
            mapFocused = false;
          }
        }
      }
      return new ArrayList<String>();
    }
    public ArrayList<String> keyboardEvent(String eventType, char _key) {
      if (eventType == "keyPressed") {
        keyState.put(_key, true);
      }
      if (eventType == "keyReleased") {
        keyState.put(_key, false);
      }
      return new ArrayList<String>();
    }

    public void setWidth(int w) {
      this.elementWidth = w;
    }

    public void drawSelectedCell(PVector c, PGraphics panelCanvas) {
      //cell selection
      panelCanvas.stroke(0);
      if (cellSelected) {
        if (max(c.x, xPos)+min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)>xPos && max(c.x, xPos) < elementWidth+xPos && max(c.y, yPos)+min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos)>yPos && max(c.y, yPos) < elementHeight+yPos) {
          panelCanvas.fill(50, 100);
          panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize-1, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize-1, yPos+elementHeight-c.y, blockSize+c.y-yPos));
        }
      }
    }

    public void draw(PGraphics panelCanvas) {

      // Terrain
      PImage[] tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
      PImage[] tempTileImagesDark = new PImage[gameData.getJSONArray("terrain").size()];
      PImage[][] tempBuildingImages = new PImage[gameData.getJSONArray("buildings").size()][];
      PImage[][] tempBuildingImagesDark = new PImage[gameData.getJSONArray("buildings").size()][];
      PImage[] tempPartyImages = new PImage[players.length+1]; // Index 0 is battle
      PImage[] tempTaskImages = new PImage[taskImages.length];
      if (frameStartTime == 0) {
        frameStartTime = millis();
      }
      int frameTime = millis()-frameStartTime;
      if (millis()-resetTime < INITIALHOLD) {
        frameTime = 0;
      }
      if (zooming) {
        blockSize += (targetBlockSize-blockSize)*panningSpeed*frameTime*60/1000;
      }

      // Resize map based on scale
      if (panning) {
        mapXOffset -= (mapXOffset-targetXOffset)*panningSpeed*frameTime*60/1000;
        mapYOffset -= (mapYOffset-targetYOffset)*panningSpeed*frameTime*60/1000;
      }
      if ((zooming || panning) && pow(mapXOffset-targetXOffset, 2) + pow(mapYOffset-targetYOffset, 2) < blockSize*0.02f && abs(blockSize-targetBlockSize) < 1) {
        resetTargetZoom();
        resetTarget();
      }
      mapVelocity[0] = 0;
      mapVelocity[1] = 0;
      if (keyState.containsKey('a')) {
        if (keyState.get('a')) {
          mapVelocity[0] -= mapMaxSpeed;
        }
      }
      if (keyState.containsKey('d')) {
        if (keyState.get('d')) {
          mapVelocity[0] += mapMaxSpeed;
        }
      }
      if (keyState.containsKey('w')) {
        if (keyState.get('w')) {
          mapVelocity[1] -= mapMaxSpeed;
        }
      }
      if (keyState.containsKey('s')) {
        if (keyState.get('s')) {
          mapVelocity[1] += mapMaxSpeed;
        }
      }

      if (mapVelocity[0]!=0||mapVelocity[1]!=0) {
        mapXOffset -= mapVelocity[0]*frameTime*60/1000;
        mapYOffset -= mapVelocity[1]*frameTime*60/1000;
        resetTargetZoom();
        resetTarget();
      }
      frameStartTime = millis();
      limitCoords();

      if (blockSize <= 0)
        return;

      for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
        JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
        if (blockSize<24&&!tileType.isNull("low img")) {
          tempTileImages[i] = lowImages.get(tileType.getString("id")).copy();
          tempTileImages[i].resize(ceil(blockSize), 0);
          tempTileImagesDark[i] = lowImages.get(tileType.getString("id")).copy();
          tempTileImagesDark[i].resize(ceil(blockSize), 0);
          tempTileImagesDark[i].loadPixels();
          for (int j = 0; j < partyImages[i].pixels.length; j++) {
            tempTileImagesDark[i].pixels[j] = brighten(tempTileImagesDark[i].pixels[j], -20);
          }
        } else {
          tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
          tempTileImages[i].resize(ceil(blockSize), 0);
          tempTileImagesDark[i] = tileImages.get(tileType.getString("id")).copy();
          tempTileImagesDark[i].resize(ceil(blockSize), 0);
          tempTileImagesDark[i].loadPixels();
          for (int j = 0; j < tempTileImagesDark[i].pixels.length; j++) {
            tempTileImagesDark[i].pixels[j] = brighten(tempTileImagesDark[i].pixels[j], -100);
          }
        }
      }
      for (int i=0; i<gameData.getJSONArray("buildings").size(); i++) {
        JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
        tempBuildingImages[i] = new PImage[buildingImages.get(buildingType.getString("id")).length];
        for (int j=0; j<buildingImages.get(buildingType.getString("id")).length; j++) {
          tempBuildingImages[i][j] = buildingImages.get(buildingType.getString("id"))[j].copy();
          tempBuildingImages[i][j].resize(ceil(blockSize*48/64), 0);
        }
      }

      tempPartyImages[0] = partyBaseImages[0].copy(); // Battle
      tempPartyImages[0].resize(ceil(blockSize), 0);
      for (int i=1; i<partyImages.length+1; i++) {
        tempPartyImages[i] = partyImages[i-1].copy();
        tempPartyImages[i].resize(ceil(blockSize), 0);
      }

      for (int i=0; i<taskImages.length; i++) {
        if (taskImages[i] != null) {
          tempTaskImages[i] = taskImages[i].copy();
          tempTaskImages[i].resize(ceil(3*blockSize/16), 0);
        }
      }
      int lx = max(0, -ceil((mapXOffset)/blockSize));
      int ly = max(0, -ceil((mapYOffset)/blockSize));
      int hx = min(floor((elementWidth-mapXOffset)/blockSize)+1, mapWidth);
      int hy = min(floor((elementHeight-mapYOffset)/blockSize)+1, mapHeight);

      PVector c;
      PVector selectedCell = new PVector(scaleX(selectedCellX), scaleY(selectedCellY));

      for (int y=ly; y<hy; y++) {
        for (int x=lx; x<hx; x++) {
          float x2 = round(scaleX(x));
          float y2 = round(scaleY(y));
          if (jsManager.loadBooleanSetting("fog of war") && visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
            panelCanvas.image(tempTileImagesDark[terrain[y][x]], x2, y2);
          } else if (!jsManager.loadBooleanSetting("fog of war") || visibleCells[y][x] != null) {
            panelCanvas.image(tempTileImages[terrain[y][x]], x2, y2);
          }

          //Buildings
          if (buildings[y][x] != null) {
            c = new PVector(scaleX(x), scaleY(y));
            int border = round((64-48)*blockSize/(2*64));
            int imgSize = round(blockSize*48/60);
            PImage p;
            if (jsManager.loadBooleanSetting("fog of war") && visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
              p = tempBuildingImagesDark[buildings[y][x].type][buildings[y][x].image_id];
            } else {
              p = tempBuildingImages[buildings[y][x].type][buildings[y][x].image_id];
            }
            drawCroppedImage(round(c.x+border), round(c.y+border*2), imgSize, imgSize, p, panelCanvas);
          }
          //Parties
          if (!jsManager.loadBooleanSetting("fog of war") || (visibleCells[y][x] != null && visibleCells[y][x].party != null)) {
            c = new PVector(scaleX(x), scaleY(y));
            if (c.x<xPos+elementWidth&&c.y+blockSize/8+blockSize>yPos&&c.y<yPos+elementHeight) {
              panelCanvas.noStroke();
              if (parties[y][x] instanceof Battle) {
                Battle battle = (Battle) parties[y][x];
                if (c.x+blockSize>xPos) {
                  panelCanvas.fill(120, 120, 120);
                  panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
                }
                if (c.x+blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")>xPos) {
                  panelCanvas.fill(playerColours[battle.attacker.player]);
                  panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size")+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
                }
                if (c.x+blockSize>xPos) {
                  panelCanvas.fill(120, 120, 120);
                  panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/16+c.y-yPos)));
                }
                if (c.x+blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")>xPos) {
                  panelCanvas.fill(playerColours[battle.defender.player]);
                  panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size")+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/8+c.y-yPos)));
                }
              } else {
                if (c.x+blockSize>xPos) {
                  panelCanvas.fill(120, 120, 120);
                  panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
                }
                if (c.x+blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")>xPos) {
                  panelCanvas.fill(playerColours[parties[y][x].player]);
                  panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
                }
              }
              int imgSize = round(blockSize);
              if (parties[y][x].player == -1) { // Is a battle
                Party attacker = ((Battle)parties[y][x]).attacker;
                Party defender = ((Battle)parties[y][x]).defender;
                drawCroppedImage(floor(c.x), floor(c.y), imgSize, imgSize, tempPartyImages[0], panelCanvas); // Swords
                drawCroppedImage(floor(c.x-blockSize*9.0f/32.0f), floor(c.y+blockSize/4.0f), imgSize, imgSize, tempPartyImages[attacker.player+1], panelCanvas); // Attacker
                panelCanvas.pushMatrix();
                panelCanvas.translate(floor(c.x+blockSize*41.0f/32.0f), floor(c.y+blockSize/4.0f));
                panelCanvas.scale(-1, 1);
                panelCanvas.image(tempPartyImages[defender.player+1], 0, 0); // Defender
                panelCanvas.popMatrix();
              } else {
                drawCroppedImage(floor(c.x), floor(c.y), imgSize, imgSize, tempPartyImages[parties[y][x].player+1], panelCanvas);
              }

              JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(parties[y][x].getTask());
              if (jo != null && !jo.isNull("img")) {
                drawCroppedImage(floor(c.x+13*blockSize/32), floor(c.y+blockSize/2), ceil(3*blockSize/16), ceil(3*blockSize/16), tempTaskImages[parties[y][x].getTask()], panelCanvas);
              }
            }
            if (parties[y][x]!=null) {
              c = new PVector(scaleX(x), scaleY(y));
              if (c.x<xPos+elementWidth&&c.y+blockSize/8+blockSize>yPos&&c.y<yPos+elementHeight) {
                panelCanvas.textFont(getFont(blockSize/7));
                panelCanvas.fill(255);
                panelCanvas.textAlign(CENTER, CENTER);
                if (parties[y][x].actions.size() > 0 && parties[y][x].actions.get(0).initialTurns>0) {
                  int totalTurns = parties[y][x].calcTurns(parties[y][x].actions.get(0).initialTurns);
                  String turnsLeftString = str(totalTurns-parties[y][x].turnsLeft())+"/"+str(totalTurns);
                  if (c.x+textWidth(turnsLeftString) < elementWidth+xPos && c.y+2*(textAscent()+textDescent()) < elementHeight+yPos) {
                    panelCanvas.text(turnsLeftString, c.x+blockSize/2, c.y+3*blockSize/4);
                  }
                }
              }
            }
          }
          if (cellSelected&&y==selectedCellY&&x==selectedCellX&&!cinematicMode) {
            drawSelectedCell(selectedCell, panelCanvas);
          }
          //if (millis()-pt > 10){
          //  println(millis()-pt);
          //}
        }
      }

      if (moveNodes != null && parties[selectedCellY][selectedCellX] != null) {
        for (int y1=0; y1<mapHeight; y1++) {
          for (int x=0; x<mapWidth; x++) {
            if (moveNodes[y1][x] != null) {
              c = new PVector(scaleX(x), scaleY(y1));
              if (max(c.x, xPos)+min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)>xPos && max(c.x, xPos) < elementWidth+xPos && max(c.y, yPos)+min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos)>yPos && max(c.y, yPos) < elementHeight+yPos) {
                if (blockSize > 10*jsManager.loadFloatSetting("text scale") && moveNodes[y1][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()) {
                  shadeCell(panelCanvas, c.x, c.y, color(50, 150));
                  panelCanvas.fill(255);
                  panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
                  panelCanvas.textAlign(CENTER, CENTER);
                  String s = ""+moveNodes[y1][x].cost;
                  s = s.substring(0, min(s.length(), 3));
                  BigDecimal cost = new BigDecimal(s);
                  String s2 = cost.stripTrailingZeros().toPlainString();
                  if (c.x+panelCanvas.textWidth(s2) < elementWidth+xPos && c.y+2*(textAscent()+textDescent()) < elementHeight+yPos) {
                    panelCanvas.text(s2, c.x+blockSize/2, c.y+blockSize/2);
                  }
                }
              }
            }
          }
        }
      }

      //if (jsManager.loadBooleanSetting("fog of war")) {
      //  for (int y1=0; y1<mapHeight; y1++) {
      //    for (int x=0; x<mapWidth; x++) {
      //      if (!fogMap[y1][x]) {
      //        c = new PVector(scaleX(x), scaleY(y1));
      //        panelCanvas.fill(0, 50);
      //        panelCanvas.noStroke();
      //        panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos));
      //      }
      //    }
      //  }
      //}

      if (drawRocket) {
        drawRocket(panelCanvas, tempBuildingImages);
      }

      if (drawPath != null) {
        for (int i=0; i<drawPath.size()-1; i++) {
          if (lx <= drawPath.get(i)[0] && drawPath.get(i)[0] < hx && ly <= drawPath.get(i)[1] && drawPath.get(i)[1] < hy) {
            panelCanvas.pushStyle();
            panelCanvas.stroke(255, 0, 0);
            panelCanvas.line(scaleX(drawPath.get(i)[0])+blockSize/2, scaleY(drawPath.get(i)[1])+blockSize/2, scaleX(drawPath.get(i+1)[0])+blockSize/2, scaleY(drawPath.get(i+1)[1])+blockSize/2);
            panelCanvas.popStyle();
          }
        }
      } else if (cellSelected && parties[selectedCellY][selectedCellX] != null) {
        ArrayList<int[]> path = parties[selectedCellY][selectedCellX].path;
        if (path != null) {
          for (int i=0; i<path.size()-1; i++) {
            if (lx <= path.get(i)[0] && path.get(i)[0] < hx && ly <= path.get(i)[1] && path.get(i)[1] < hy) {
              panelCanvas.pushStyle();
              panelCanvas.stroke(100);
              panelCanvas.line(scaleX(path.get(i)[0])+blockSize/2, scaleY(path.get(i)[1])+blockSize/2, scaleX(path.get(i+1)[0])+blockSize/2, scaleY(path.get(i+1)[1])+blockSize/2);
              panelCanvas.popStyle();
            }
          }
        }
      }

      if (showingBombard) {
        for (int y = max(0, selectedCellY-bombardRange); y <= min(selectedCellY+bombardRange, mapHeight-1); y++) {
          for (int x = max(0, selectedCellX-bombardRange); x <= min(selectedCellX+bombardRange, mapWidth-1); x++) {
            if (dist(x, y, selectedCellX, selectedCellY) <= bombardRange) {
              shadeCell(panelCanvas, scaleX(x), scaleY(y), color(255, 0, 0, 100));
            }
          }
        }
        drawBombard(panelCanvas);
      }

      panelCanvas.noFill();
      panelCanvas.stroke(0);
      panelCanvas.rect(xPos, yPos, elementWidth, elementHeight);
    }

    public void shadeCell(PGraphics canvas, float x, float y, int c) {
      canvas.fill(c);
      canvas.rect(max(x, xPos), max(y, yPos), min(blockSize, xPos+elementWidth-x, blockSize+x-xPos), min(blockSize, yPos+elementHeight-y, blockSize+y-yPos));
    }

    public int sign(float x) {
      if (x > 0) {
        return 1;
      } else if (x < 0) {
        return -1;
      }
      return 0;
    }
    public void drawCroppedImage(int x, int y, int w, int h, PImage img, PGraphics panelCanvas) {
      if (x+w>xPos && x<elementWidth+xPos && y+h>yPos && y<elementHeight+yPos) {
        //int newX = max(min(x, xPos+elementWidth), xPos);
        //int newY = max(min(y, yPos+elementHeight), yPos);
        //int imgX = max(0, newX-x, 0);
        //int imgY = max(0, newY-y, 0);
        //int imgW = min(max(elementWidth+xPos-x, -sign(elementWidth+xPos-x)*(x+w-newX)), img.width);
        //int imgH = min(max(elementHeight+yPos-y, -sign(elementHeight+yPos-y)*(y+h-newY)), img.height);
        panelCanvas.image(img, x, y);
      }
    }
    public float scaleX(float x) {
      return x*blockSize + mapXOffset + xPos;
    }
    public float scaleY(float y) {
      return y*blockSize + mapYOffset + yPos;
    }
    public float scaleXInv() {
      return (mouseX-mapXOffset-xPos)/blockSize;
    }
    public float scaleYInv() {
      return (mouseY-mapYOffset-yPos)/blockSize;
    }
    public boolean mouseOver() {
      return mouseX > xPos && mouseX < xPos+elementWidth && mouseY > yPos && mouseY < yPos+elementHeight;
    }

    public void enableRocket(PVector pos, PVector vel) {
      LOGGER_MAIN.fine("Rocket enabled in 2d map");
      drawRocket = true;
      rocketPosition = pos;
      rocketVelocity = vel;
    }

    public void disableRocket() {
      LOGGER_MAIN.fine("Rocket disabled in 2d map");
      drawRocket = false;
    }

    public void drawRocket(PGraphics canvas, PImage[][] buildingImages) {
      PVector c = new PVector(scaleX(rocketPosition.x+0.5f), scaleY(rocketPosition.y+0.5f-rocketPosition.z));
      int border = round((64-48)*blockSize/(2*64));
      canvas.pushMatrix();
      canvas.translate(round(c.x+border), round(c.y+border*2));
      canvas.rotate(atan2(rocketVelocity.x, rocketVelocity.z));
      canvas.translate(-blockSize/2, -blockSize/2);
      canvas.image(buildingImages[buildingIndex("Rocket Factory")][2], 0, 0);
      canvas.popMatrix();
    }

    public void drawBombard(PGraphics canvas) {
      int x = floor(scaleXInv());
      int y = floor(scaleYInv());
      if (0 <= x && x < mapWidth && 0 <= y && y < mapHeight) {
        if (parties[y][x] != null && parties[y][x].player != parties[selectedCellY][selectedCellX].player && dist(x, y, selectedCellX, selectedCellY) < bombardRange) {
          canvas.pushMatrix();
          canvas.translate(scaleX(x), scaleY(y));
          canvas.scale(blockSize/64, blockSize/64);
          canvas.image(bombardImage, 16, 16);
          canvas.popMatrix();
        }
      }
    }

    public void enableBombard(int range) {
      showingBombard = true;
      bombardRange = range;
    }
    public void disableBombard() {
      showingBombard = false;
    }

    public void setPlayerColours(int[] playerColours) {
      this.playerColours = playerColours;
    }
  }
  //boolean isWater(int x, int y) {
  //  //return max(new float[]{
  //  //  noise(x*MAPNOISESCALE, y*MAPNOISESCALE),
  //  //  noise((x+1)*MAPNOISESCALE, y*MAPNOISESCALE),
  //  //  noise(x*MAPNOISESCALE, (y+1)*MAPNOISESCALE),
  //  //  noise((x+1)*MAPNOISESCALE, (y+1)*MAPNOISESCALE),
  //  //  })<jsManager.loadFloatSetting("water level");
  //  for (float y1 = y; y1<=y+1;y1+=1.0/jsManager.loadFloatSetting("terrain detail")){
  //    for (float x1 = x; x1<=x+1;x1+=1.0/jsManager.loadFloatSetting("terrain detail")){
  //      if(noise(x1*MAPHEIGHTNOISESCALE, y1*MAPHEIGHTNOISESCALE)>jsManager.loadFloatSetting("water level")){
  //        return false;
  //      }
  //    }
  //  }
  //  return true;
  //}





  class Map3D extends BaseMap implements Map {
    final int thickness = 10;
    final float PANSPEED = 0.5f, ROTSPEED = 0.002f;
    final float STUMPR = 1, STUMPH = 4, LEAVESR = 5, LEAVESH = 15, TREERANDOMNESS=0.3f;
    final float HILLRAISE = 1.05f;
    final float GROUNDHEIGHT = 5;
    final float VERYSMALLSIZE = 0.01f;
    private int numTreeTiles;
    float panningSpeed = 0.05f;
    int x, y, w, h, prevT, frameTime;
    float hoveringX, hoveringY, oldHoveringX, oldHoveringY;
    float targetXOffset, targetYOffset;
    int selectedCellX, selectedCellY;
    PShape tiles, flagPole, battle, trees, selectTile, water, tileRect, pathLine, highlightingGrid, drawPossibleMoves, drawPossibleBombards, obscuredCellsOverlay, unseenCellsOverlay, dangerousCellsOverlay, bombardArrow, bandit;
    PShape[] flags;
    HashMap<String, PShape> taskObjs;
    HashMap<String, PShape[]> buildingObjs;
    PShape[] unitNumberObjects;
    PImage[] tempTileImages;
    float targetZoom, zoom, zoomv, tilt, tiltv, rot, rotv, focusedX, focusedY;
    PVector focusedV, heldPos;
    Boolean panning, zooming, mapActive, cellSelected, updateHoveringScale;
    Node[][] moveNodes;
    float blockSize = 16;
    ArrayList<int[]> drawPath;
    HashMap<Integer, Integer> forestTiles;
    PGraphics canvas, refractionCanvas;
    HashMap<Integer, HashMap<Integer, Float>> downwardAngleCache;
    PShape tempRow, tempSingleRow;
    PGraphics tempTerrain;
    boolean drawRocket;
    PVector rocketPosition;
    PVector rocketVelocity;
    boolean showingBombard;
    int bombardRange;
    int[] playerColours;

    Map3D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight) {
      LOGGER_MAIN.fine("Initialising map 3d");
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.terrain = terrain;
      this.parties = parties;
      this.buildings = buildings;
      this.mapWidth = mapWidth;
      this.mapHeight = mapHeight;
      blockSize = 32;
      zoom = height/2;
      tilt = PI/3;
      focusedX = round(mapWidth*blockSize/2);
      focusedY = round(mapHeight*blockSize/2);
      focusedV = new PVector(0, 0);
      heldPos = null;
      cellSelected = false;
      panning = false;
      zooming = false;
      buildingObjs = new HashMap<String, PShape[]>();
      taskObjs = new HashMap<String, PShape>();
      forestTiles = new HashMap<Integer, Integer>();
      canvas = createGraphics(width, height, P3D);
      refractionCanvas = createGraphics(width/4, height/4, P3D);
      downwardAngleCache = new HashMap<Integer, HashMap<Integer, Float>>();
      heightMap = new float[PApplet.parseInt((mapWidth+1)*(mapHeight+1)*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
      targetXOffset = mapWidth/2*blockSize;
      targetYOffset = mapHeight/2*blockSize;
      updateHoveringScale = false;
      this.keyState = new HashMap<Character, Boolean>();
      showingBombard = false;
    }



    public float getDownwardAngle(int x, int y) {
      try {
        if (!downwardAngleCache.containsKey(y)) {
          downwardAngleCache.put(y, new HashMap<Integer, Float>());
        }
        if (downwardAngleCache.get(y).containsKey(x)) {
          return downwardAngleCache.get(y).get(x);
        } else {
          PVector maxZCoord = new PVector();
          PVector minZCoord = new PVector();
          float maxZ = 0;
          float minZ = 1;
          for (float y1 = y; y1<=y+1; y1+=1.0f/jsManager.loadFloatSetting("terrain detail")) {
            for (float x1 = x; x1<=x+1; x1+=1.0f/jsManager.loadFloatSetting("terrain detail")) {
              float z = getRawHeight(x1, y1);
              if (z > maxZ) {
                maxZCoord = new PVector(x1, y1);
                maxZ = z;
              } else if (z < minZ) {
                minZCoord = new PVector(x1, y1);
                minZ = z;
              }
            }
          }
          PVector direction = minZCoord.sub(maxZCoord);
          float angle = atan2(direction.y, direction.x);

          downwardAngleCache.get(y).put(x, angle);
          return angle;
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting downward angle: (%s, %s)", x, y), e);
        throw e;
      }
    }

    public Node[][] getMoveNodes() {
      return moveNodes;
    }
    public boolean isPanning() {
      return panning;
    }
    public float getTargetZoom() {
      return targetZoom;
    }
    public boolean isMoving() {
      return focusedV.x != 0 || focusedV.y != 0;
    }

    public float getTargetOffsetX() {
      return targetXOffset;
    }
    public float getTargetOffsetY() {
      return targetYOffset;
    }
    public float getTargetBlockSize() {
      return zoom;
    }
    public float getZoom() {
      return zoom;
    }
    public boolean isZooming() {
      return zooming;
    }
    public float getFocusedX() {
      return focusedX;
    }
    public float getFocusedY() {
      return focusedY;
    }
    public void setActive(boolean a) {
      this.mapActive = a;
    }
    public void updateMoveNodes(Node[][] nodes, Player[] players) {
      LOGGER_MAIN.finer("Updating move nodes");
      moveNodes = nodes;
      updatePossibleMoves();
      updateDangerousCellsOverlay(visibleCells, players);
    }

    public void updatePath (ArrayList<int[]> path, int[] target) {
      // Use when updaing with a target node
      ArrayList<int[]> tempPath = new ArrayList<int[]>(path);
      tempPath.add(target);
      updatePath(tempPath);
    }

    public void updatePath(ArrayList<int[]> path) {
      //LOGGER_MAIN.finer("Updating path");
      float x0, y0;
      pathLine = createShape();
      pathLine.beginShape();
      pathLine.noFill();
      if (drawPath == null) {
        pathLine.stroke(150);
      } else {
        pathLine.stroke(255, 0, 0);
      }
      for (int i=0; i<path.size()-1; i++) {
        for (int u=0; u<blockSize/8; u++) {
          x0 = path.get(i)[0]+(path.get(i+1)[0]-path.get(i)[0])*u/8+0.5f;
          y0 = path.get(i)[1]+(path.get(i+1)[1]-path.get(i)[1])*u/8+0.5f;
          pathLine.vertex(x0*blockSize, y0*blockSize, 5+getHeight(x0, y0));
        }
      }
      if (drawPath != null) {
        pathLine.vertex((selectedCellX+0.5f)*blockSize, (selectedCellY+0.5f)*blockSize, 5+getHeight(selectedCellX+0.5f, selectedCellY+0.5f));
      }
      pathLine.endShape();
      drawPath = path;
    }

    public void loadUnseenCellsOverlay(Cell[][] visibleCells) {
      // For the shape that indicates cells that have not been seen
      try {
        LOGGER_MAIN.finer("Loading unseen cells overlay");
        float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
        unseenCellsOverlay = createShape();
        unseenCellsOverlay.beginShape(TRIANGLES);
        unseenCellsOverlay.fill(0);
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (visibleCells[y][x] == null) {
              if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && forestTiles.containsKey(x+y*mapWidth)) {
                removeTreeTile(x, y);
              }
              for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                  unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));

                  unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                }
              }
            } else {
              if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && !forestTiles.containsKey(x+y*mapWidth)) {
                PShape cellTree = generateTrees(jsManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
                cellTree.translate((x)*blockSize, (y)*blockSize, 0);
                trees.addChild(cellTree);
                addTreeTile(x, y, numTreeTiles++);
              }
              for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                  unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, 0);
                  unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                  unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);

                  unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);
                  unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                  unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                }
              }
            }
          }
        }
        unseenCellsOverlay.endShape();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading unseen cells overlay", e);
        throw e;
      }
    }

    public void updateUnseenCellsOverlay(Cell[][] visibleCells) {
      // For the shape that indicates cells that have not been seen
      try {
        LOGGER_MAIN.finer("Updating unseen cells overlay");
        float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
        if (unseenCellsOverlay == null) {
          loadUnseenCellsOverlay(visibleCells);
        } else {
          for (int y=0; y<mapHeight; y++) {
            for (int x=0; x<mapWidth; x++) {
              int c = PApplet.parseInt(pow(jsManager.loadFloatSetting("terrain detail"), 2) * (y*mapWidth+x) * 6);
              if (visibleCells[y][x] == null) {
                if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && forestTiles.containsKey(x+y*mapWidth)) {
                  removeTreeTile(x, y);
                }
                if (unseenCellsOverlay.getVertex(c).z == 0) {
                  for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                    for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                      unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                      c++;

                      unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                    }
                  }
                }
              } else {
                if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && !forestTiles.containsKey(x+y*mapWidth)) {
                  PShape cellTree = generateTrees(jsManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
                  cellTree.translate((x)*blockSize, (y)*blockSize, 0);
                  trees.addChild(cellTree);
                  addTreeTile(x, y, numTreeTiles++);
                }

                if (unseenCellsOverlay.getVertex(c).z != 0) {
                  for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                    for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                      unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, 0);
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);
                      c++;

                      unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                      c++;
                      unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                      c++;
                    }
                  }
                }
              }
            }
          }
          unseenCellsOverlay.endShape();
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating unseen cells overlay", e);
        throw e;
      }
    }

    public void loadObscuredCellsOverlay(Cell[][] visibleCells) {
      // For the shape that indicates cells that are not currently under party sight
      try {
        LOGGER_MAIN.finer("Loading obscured cells overlay");
        float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
        obscuredCellsOverlay = createShape();
        obscuredCellsOverlay.beginShape(TRIANGLES);
        obscuredCellsOverlay.fill(0, 0, 0, 200);
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
              for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                  obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));

                  obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                }
              }
            } else {
              for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                  obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, 0);
                  obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                  obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);

                  obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);
                  obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                  obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                }
              }
            }
          }
        }
        obscuredCellsOverlay.endShape();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading obscured cells overlay", e);
        throw e;
      }
    }

    public void updateObscuredCellsOverlay(Cell[][] visibleCells) {
      // For the shape that indicates cells that are not currently under party sight
      try {
        LOGGER_MAIN.finer("Updating obscured cells overlay");
        float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
        if (obscuredCellsOverlay == null) {
          loadObscuredCellsOverlay(visibleCells);
        } else {
          for (int y=0; y<mapHeight; y++) {
            for (int x=0; x<mapWidth; x++) {
              int c = PApplet.parseInt(pow(jsManager.loadFloatSetting("terrain detail"), 2) * (y*mapWidth+x) * 6);
              if (visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
                if (obscuredCellsOverlay.getVertex(c).z == 0) {
                  for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                    for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                      obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                      c++;

                      obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                      c++;
                    }
                  }
                }
              } else {
                if (obscuredCellsOverlay.getVertex(c).z != 0) {
                  for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                    for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                      obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, 0);
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);
                      c++;

                      obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, 0);
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                      c++;
                      obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, 0);
                      c++;
                    }
                  }
                }
              }
            }
          }
          obscuredCellsOverlay.endShape();
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating obscured cells overlay", e);
        throw e;
      }
    }


    public void updateVisibleCells(Cell[][] visibleCells) {
      super.updateVisibleCells(visibleCells);
      updateOverlays(visibleCells);
    }

    public void updateOverlays(Cell[][] visibleCells) {
      if (jsManager.loadBooleanSetting("fog of war")) {
        updateObscuredCellsOverlay(visibleCells);
        updateUnseenCellsOverlay(visibleCells);
      }
    }

    public void updatePossibleMoves() {
      // For the shape that indicateds where a party can move
      try {
        LOGGER_MAIN.finer("Updating possible move nodes");
        float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
        drawPossibleMoves = createShape();
        drawPossibleMoves.beginShape(TRIANGLES);
        drawPossibleMoves.fill(0, 0, 0, 100);
        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (moveNodes[y][x] != null && parties[selectedCellY][selectedCellX] != null && moveNodes[y][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()) {
              for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                  drawPossibleMoves.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleMoves.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleMoves.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));

                  drawPossibleMoves.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleMoves.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleMoves.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                }
              }
            }
          }
        }
        drawPossibleMoves.endShape();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating possible moves", e);
        throw e;
      }
    }

    public void updateDangerousCellsOverlay(Cell[][] visibleCells, Player[] players) {
      // For the shape that indicates cells that are dangerous
      try {
        if (visibleCells[selectedCellY][selectedCellX] != null && visibleCells[selectedCellY][selectedCellX].party != null) {
          LOGGER_MAIN.finer("Updating dangerous cells overlay");
          float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
          dangerousCellsOverlay = createShape();
          dangerousCellsOverlay.beginShape(TRIANGLES);
          dangerousCellsOverlay.fill(255, 0, 0, 150);
          boolean[][] dangerousCells = new boolean[mapHeight][mapWidth];

          for (int y=0; y<mapHeight; y++) {
            for (int x=0; x<mapWidth; x++) {
              if (visibleCells[y][x] != null && visibleCells[y][x].party != null && visibleCells[y][x].party.player != visibleCells[selectedCellY][selectedCellX].party.player && visibleCells[y][x].party.player >= 0) {
                players[visibleCells[y][x].party.player].updateVisibleCells(terrain, buildings, parties);
                Node[][] tempMoveNodes = LimitedKnowledgeDijkstra(x, y, mapWidth, mapHeight, players[visibleCells[y][x].party.player].visibleCells, 1);
                for (int x1=0; x1<mapWidth; x1++) {
                  for (int y1=0; y1<mapHeight; y1++) {
                    if (tempMoveNodes[y1][x1] != null && tempMoveNodes[y1][x1].cost <= visibleCells[y][x].party.getMaxMovementPoints() && visibleCells[y1][x1] != null) {
                      dangerousCells[y1][x1] = true;
                    }
                  }
                }
              }
            }
          }
          for (int y=0; y<mapHeight; y++) {
            for (int x=0; x<mapWidth; x++) {
              if (dangerousCells[y][x]) {
                for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                  for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                    dangerousCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                    dangerousCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                    dangerousCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));

                    dangerousCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                    dangerousCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                    dangerousCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  }
                }
              }
            }
          }
          dangerousCellsOverlay.endShape();
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating dangerous cells overlay", e);
        throw e;
      }
    }

    public void updatePossibleBombards() {
      // For the shape that indicateds where a party can bombard
      try {
        LOGGER_MAIN.finer("Updating possible bombards");
        float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
        drawPossibleBombards = createShape();
        drawPossibleBombards.beginShape(TRIANGLES);
        drawPossibleBombards.fill(255, 0, 0, 100);
        for (int y = max(0, selectedCellY - bombardRange); y < min(selectedCellY + bombardRange + 1, mapHeight); y++) {
          for (int x = max(0, selectedCellX - bombardRange); x < min(selectedCellX + bombardRange + 1, mapWidth); x++) {
            if (dist(x, y, selectedCellX, selectedCellY) <= bombardRange) {
              if (parties[y][x] != null && parties[y][x].player != parties[selectedCellY][selectedCellX].player){
                drawPossibleBombards.fill(255, 0, 0, 150);
              } else {
                drawPossibleBombards.fill(128, 128, 128, 150);
              }
              for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
                for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                  drawPossibleBombards.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleBombards.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleBombards.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));

                  drawPossibleBombards.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleBombards.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                  drawPossibleBombards.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                }
              }
            }
          }
        }
        drawPossibleBombards.endShape();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating possible bombards", e);
        throw e;
      }
    }

    public void cancelMoveNodes() {
      moveNodes = null;
    }
    public void cancelPath() {
      drawPath = null;
    }

    public void loadSettings(float x, float y, float blockSize) {
      LOGGER_MAIN.fine(String.format("Loading camera settings. cellX:%s, cellY:%s, block size: %s", x, y, blockSize));
      targetCell(PApplet.parseInt(x), PApplet.parseInt(y), blockSize);

      panning = true;
    }
    public float[] targetCell(int x, int y, float zoom) {
      LOGGER_MAIN.finer(String.format("Targetting cell:%s, %s and zoom:%s", x, y, zoom));
      targetXOffset = (x+0.5f)*blockSize-width/2;
      targetYOffset = (y+0.5f)*blockSize-height/2;
      panning = true;
      return new float[]{targetXOffset, targetYOffset};
    }


    public void selectCell(int x, int y) {
      cellSelected = true;
      selectedCellX = x;
      selectedCellY = y;
    }
    public void unselectCell() {
      cellSelected = false;
      showingBombard = false;
    }

    public float scaleXInv() {
      return hoveringX;
    }
    public float scaleYInv() {
      return hoveringY;
    }
    public void updateHoveringScale() {
      try {
        PVector mo = getMousePosOnObject();
        hoveringX = (mo.x)/getObjectWidth()*mapWidth;
        hoveringY = (mo.y)/getObjectHeight()*mapHeight;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updaing hovering scale", e);
        throw e;
      }
    }

    public void doUpdateHoveringScale() {
      updateHoveringScale = true;
    }

    public void addTreeTile(int cellX, int cellY, int i) {
      forestTiles.put(cellX+cellY*mapWidth, i);
    }
    public void removeTreeTile(int cellX, int cellY) {
      try {
        trees.removeChild(forestTiles.get(cellX+cellY*mapWidth));
        for (Integer i : forestTiles.keySet()) {
          if (forestTiles.get(i) > forestTiles.get(cellX+cellY*mapWidth)) {
            forestTiles.put(i, forestTiles.get(i)-1);
          }
        }
        numTreeTiles--;
        forestTiles.remove(cellX+cellY*mapWidth);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error removing tree tile", e);
        throw e;
      }
    }

    // void generateWater(int vertices){
    //  water = createShape(GROUP);
    //  PShape w;
    //  float scale = getObjectWidth()/vertices;
    //  w = createShape();
    //  w.setShininess(10);
    //  w.setSpecular(10);
    //  w.beginShape(TRIANGLE_STRIP);
    //  w.fill(color(0, 50, 150));
    //  for(int x = 0; x < vertices; x++){
    //    w.vertex(x*scale, y*scale, 1);
    //    w.vertex(x*scale, (y+1)*scale, 1);
    //  }
    //  w.endShape(CLOSE);
    //  water.addChild(w);
    //}

    public float getWaveHeight(float x, float y, float t) {
      return sin(t/1000+y)+cos(t/1000+x)+2;
    }

    //void updateWater(int vertices){
    //  float scale = getObjectWidth()/vertices;
    //  for (int y = 0; y < vertices+1; y++){
    //    PShape w = water.getChild(y);
    //    for(int x = 0; x < vertices*2; x++){
    //      PVector v = w.getVertex(x);
    //      w.setVertex(x, v.x, v.y, getWaveHeight(v.x, v.y, millis()));
    //    }
    //  }
    //}

    public PShape generateTrees(int num, int vertices, float x1, float y1) {
      try {
        //LOGGER_MAIN.info(String.format("Generating trees at %s, %s", x1, y1));
        PShape shapes = createShape(GROUP);
        PShape stump;
        colorMode(HSB, 100);
        for (int i=0; i<num; i++) {
          float x = random(0, blockSize), y = random(0, blockSize);
          float h = getHeight((x1+x)/blockSize, (y1+y)/blockSize);
          float randHeight = LEAVESH*random(1-TREERANDOMNESS, 1+TREERANDOMNESS);
          if (h <= jsManager.loadFloatSetting("water level")*blockSize*GROUNDHEIGHT) continue; // Don't put trees underwater
          int leafColour = color(random(35, 40), random(90, 100), random(30, 60));
          int stumpColour = color(random(100, 125), random(100, 125), random(50, 30));
          PShape leaves = createShape();
          leaves.setShininess(0.1f);
          leaves.beginShape(TRIANGLE_FAN);
          leaves.fill(leafColour);
          int tempVertices = round(random(4, vertices));
          // create leaves
          leaves.vertex(x, y, STUMPH+randHeight+h);
          for (int j=0; j<tempVertices+1; j++) {
            leaves.vertex(x+cos(j*TWO_PI/tempVertices)*LEAVESR, y+sin(j*TWO_PI/tempVertices)*LEAVESR, STUMPH+h);
          }
          leaves.endShape(CLOSE);
          shapes.addChild(leaves);

          //create trunck
          stump = createShape();
          stump.beginShape(QUAD_STRIP);
          stump.fill(stumpColour);
          for (int j=0; j<4; j++) {
            stump.vertex(x+cos(j*TWO_PI/3)*STUMPR, y+sin(j*TWO_PI/3)*STUMPR, h);
            stump.vertex(x+cos(j*TWO_PI/3)*STUMPR, y+sin(j*TWO_PI/3)*STUMPR, STUMPH+h);
          }
          stump.endShape();
          shapes.addChild(stump);
        }
        colorMode(RGB, 255);
        return shapes;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error generating trees", e);
        throw e;
      }
    }

    public void loadMapStrip(int y, PShape tiles, boolean loading) {
      try {
        LOGGER_MAIN.finer("Loading map strip y:"+y+" loading: "+loading);
        tempTerrain = createGraphics(round((1+mapWidth)*jsManager.loadIntSetting("terrain texture resolution")), round(jsManager.loadIntSetting("terrain texture resolution")));
        tempSingleRow = createShape();
        tempRow = createShape(GROUP);
        tempTerrain.beginDraw();
        for (int x=0; x<mapWidth; x++) {
          tempTerrain.image(tempTileImages[terrain[y][x]], x*jsManager.loadIntSetting("terrain texture resolution"), 0);
        }
        tempTerrain.endDraw();

        for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail"); y1++) {
          tempSingleRow = createShape();
          tempSingleRow.setTexture(tempTerrain);
          tempSingleRow.beginShape(TRIANGLE_STRIP);
          resetMatrix();
          if (jsManager.loadBooleanSetting("tile stroke")) {
            tempSingleRow.stroke(0);
          }
          tempSingleRow.vertex(0, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, 0, 0, y1*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
          tempSingleRow.vertex(0, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, 0, 0, (y1+1)*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
          for (int x=0; x<mapWidth; x++) {
            if (terrain[y][x] == terrainIndex("quarry site stone") || terrain[y][x] == terrainIndex("quarry site clay")) {
              // End strip and start new one, skipping out cell
              tempSingleRow.vertex(x*blockSize, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x, (y+y1/jsManager.loadFloatSetting("terrain detail"))), x*jsManager.loadIntSetting("terrain texture resolution"), y1*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
              tempSingleRow.vertex(x*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x, y+(1+y1)/jsManager.loadFloatSetting("terrain detail")), x*jsManager.loadIntSetting("terrain texture resolution"), (y1+1)*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
              tempSingleRow.endShape();
              tempRow.addChild(tempSingleRow);
              tempSingleRow = createShape();
              tempSingleRow.setTexture(tempTerrain);
              tempSingleRow.beginShape(TRIANGLE_STRIP);

              if (y1 == 0) {
                // Add replacement cell for quarry site
                PShape quarrySite = loadShape(resourcesRoot+"obj/building/quarry_site.obj");
                float quarryheight = groundMinHeightAt(x, y);
                quarrySite.rotateX(PI/2);
                quarrySite.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, quarryheight);
                quarrySite.setTexture(loadImage(resourcesRoot+"img/terrain/hill.png"));
                tempRow.addChild(quarrySite);

                // Create sides for quarry site
                float smallStripSize = blockSize/jsManager.loadFloatSetting("terrain detail");
                float terrainDetail = jsManager.loadFloatSetting("terrain detail");
                PShape sides = createShape();
                sides.setFill(color(120));
                sides.beginShape(QUAD_STRIP);
                sides.fill(color(120));
                for (int i=0; i<terrainDetail; i++) {
                  sides.vertex(x*blockSize+i*smallStripSize+blockSize/16, y*blockSize+blockSize/16, quarryheight);
                  sides.vertex(x*blockSize+i*smallStripSize, y*blockSize, getHeight(x+i/terrainDetail, y));
                }
                for (int i=0; i<terrainDetail; i++) {
                  sides.vertex((x+1)*blockSize-blockSize/16, y*blockSize+i*smallStripSize+blockSize/16, quarryheight);
                  sides.vertex((x+1)*blockSize, y*blockSize+i*smallStripSize, getHeight(x+1, y+i/terrainDetail));
                }
                for (int i=0; i<terrainDetail; i++) {
                  sides.vertex((x+1)*blockSize-i*smallStripSize-blockSize/16, (y+1)*blockSize-blockSize/16, quarryheight);
                  sides.vertex((x+1)*blockSize-i*smallStripSize, (y+1)*blockSize, getHeight(x+1-i/terrainDetail, y+1));
                }
                for (int i=0; i<terrainDetail; i++) {
                  sides.vertex(x*blockSize+blockSize/16, (y+1)*blockSize-i*smallStripSize-blockSize/16, quarryheight);
                  sides.vertex(x*blockSize, (y+1)*blockSize-i*smallStripSize, getHeight(x, y+1-i/terrainDetail));
                }
                sides.vertex(x*blockSize+blockSize/16, y*blockSize+blockSize/16, quarryheight);
                sides.vertex(x*blockSize, y*blockSize, getHeight(x, y));
                sides.endShape();
                tempRow.addChild(sides);
              }
            } else {
              for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail"); x1++) {
                tempSingleRow.vertex((x+x1/jsManager.loadFloatSetting("terrain detail"))*blockSize, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), (y+y1/jsManager.loadFloatSetting("terrain detail"))), (x+x1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"), y1*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
                tempSingleRow.vertex((x+x1/jsManager.loadFloatSetting("terrain detail"))*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(1+y1)/jsManager.loadFloatSetting("terrain detail")), (x+x1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"), (y1+1)*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
              }
            }
          }
          tempSingleRow.vertex(mapWidth*blockSize, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, (y+y1/jsManager.loadFloatSetting("terrain detail"))), mapWidth*jsManager.loadIntSetting("terrain texture resolution"), (y1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
          tempSingleRow.vertex(mapWidth*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, y+(1.0f+y1)/jsManager.loadFloatSetting("terrain detail")), mapWidth*jsManager.loadIntSetting("terrain texture resolution"), ((y1+1.0f)/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
          tempSingleRow.endShape();
          tempRow.addChild(tempSingleRow);
        }
        if (loading) {
          tiles.addChild(tempRow);
        } else {
          tiles.addChild(tempRow, y);
        }

        // Clean up for garbage collector
        tempRow = null;
        tempTerrain = null;
        tempSingleRow = null;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading mao strip: y:%s", y), e);
        throw e;
      }
    }

    public void replaceMapStripWithReloadedStrip(int y) {
      try {
        LOGGER_MAIN.fine("Replacing strip y: "+y);
        tiles.removeChild(y);
        loadMapStrip(y, tiles, false);
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error replacing map strip: %s", y), e);
        throw e;
      }
    }

    public void clearShape() {
      // Use to clear references to large objects when exiting state
      try {
        LOGGER_MAIN.info("Clearing 3D models");
        water = null;
        trees = null;
        tiles = null;
        buildingObjs = new HashMap<String, PShape[]>();
        taskObjs = new HashMap<String, PShape>();
        forestTiles = new HashMap<Integer, Integer>();
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error clearing shape", e);
        throw e;
      }
    }

    public void generateShape() {
      try {
        LOGGER_MAIN.info("Generating 3D models");
        pushStyle();
        noFill();
        noStroke();
        LOGGER_MAIN.fine("Generating terrain textures");
        tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
        for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
          JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
          if (tile3DImages.containsKey(tileType.getString("id"))) {
            tempTileImages[i] = tile3DImages.get(tileType.getString("id")).copy();
          } else {
            tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
          }
          tempTileImages[i].resize(jsManager.loadIntSetting("terrain texture resolution"), jsManager.loadIntSetting("terrain texture resolution"));
        }

        tiles = createShape(GROUP);
        textureMode(IMAGE);
        trees = createShape(GROUP);

        LOGGER_MAIN.fine("Generating trees and terrain model");
        for (int y=0; y<mapHeight; y++) {
          loadMapStrip(y, tiles, true);

          // Load trees
          //for (int x=0; x<mapWidth; x++) {
          //  if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest")) {
          //    PShape cellTree = generateTrees(jsManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
          //    cellTree.translate((x)*blockSize, (y)*blockSize, 0);
          //    trees.addChild(cellTree);
          //    addTreeTile(x, y, numTreeTiles++);
          //  }
          //}
        }
        resetMatrix();

        LOGGER_MAIN.fine("Generating player flags");

        flagPole = loadShape(resourcesRoot+"obj/party/flagpole.obj");
        flagPole.rotateX(PI/2);
        flagPole.scale(2, 2.5f, 2.5f);
        flags = new PShape[playerColours.length-1];
        for (int i = 0; i < playerColours.length-1; i++) {
          flags[i] = createShape(GROUP);
          PShape edge = loadShape(resourcesRoot+"obj/party/flagedges.obj");
          edge.setFill(brighten(playerColours[i], 20));
          PShape side = loadShape(resourcesRoot+"obj/party/flagsides.obj");
          side.setFill(brighten(playerColours[i], -40));
          flags[i].addChild(edge);
          flags[i].addChild(side);
          flags[i].rotateX(PI/2);
          flags[i].scale(2, 2.5f, 2.5f);
        }
        bandit = loadShape(resourcesRoot+"obj/party/bandit.obj");
        bandit.rotateX(PI/2);
        bandit.scale(0.8f);
        battle = loadShape(resourcesRoot+"obj/party/battle.obj");
        battle.rotateX(PI/2);
        battle.scale(0.8f);

        LOGGER_MAIN.fine("Generating Water");
        fill(10, 50, 180);
        water = createShape(RECT, 0, 0, getObjectWidth(), getObjectHeight());
        water.translate(0, 0, jsManager.loadFloatSetting("water level")*blockSize*GROUNDHEIGHT+4*VERYSMALLSIZE);
        generateHighlightingGrid(8, 8);

        int players = playerColours.length;
        fill(255);

        LOGGER_MAIN.fine("Generating units number objects");
        unitNumberObjects = new PShape[players];
        for (int i=0; i < players; i++) {
          unitNumberObjects[i] = createShape();
          unitNumberObjects[i].beginShape(QUADS);
          unitNumberObjects[i].stroke(0);
          unitNumberObjects[i].fill(120, 120, 120);
          unitNumberObjects[i].vertex(blockSize, 0, 0);
          unitNumberObjects[i].fill(120, 120, 120);
          unitNumberObjects[i].vertex(blockSize, blockSize*0.125f, 0);
          unitNumberObjects[i].fill(120, 120, 120);
          unitNumberObjects[i].vertex(blockSize, blockSize*0.125f, 0);
          unitNumberObjects[i].fill(120, 120, 120);
          unitNumberObjects[i].vertex(blockSize, 0, 0);
          unitNumberObjects[i].fill(playerColours[i]);
          unitNumberObjects[i].vertex(0, 0, 0);
          unitNumberObjects[i].fill(playerColours[i]);
          unitNumberObjects[i].vertex(0, blockSize*0.125f, 0);
          unitNumberObjects[i].fill(playerColours[i]);
          unitNumberObjects[i].vertex(blockSize, blockSize*0.125f, 0);
          unitNumberObjects[i].fill(playerColours[i]);
          unitNumberObjects[i].vertex(blockSize, 0, 0);
          unitNumberObjects[i].endShape();
          unitNumberObjects[i].rotateX(PI/2);
        }


        tileRect = createShape();
        tileRect.beginShape();
        tileRect.noFill();
        tileRect.stroke(0);
        tileRect.strokeWeight(3);
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        int[] curLoc = {0, 0};
        for (int[] dir : directions) {
          for (int i=0; i<jsManager.loadFloatSetting("terrain detail"); i++) {
            tileRect.vertex(curLoc[0]*blockSize/jsManager.loadFloatSetting("terrain detail"), curLoc[1]*blockSize/jsManager.loadFloatSetting("terrain detail"), 0);
            curLoc[0] += dir[0];
            curLoc[1] += dir[1];
          }
        }
        LOGGER_MAIN.fine("Loading task icon objects");
        tileRect.endShape(CLOSE);
        for (int i=0; i<gameData.getJSONArray("tasks").size(); i++) {
          JSONObject task = gameData.getJSONArray("tasks").getJSONObject(i);
          if (!task.isNull("obj")) {
            taskObjs.put(task.getString("id"), loadShape(resourcesRoot+"obj/task/"+task.getString("obj")));
            taskObjs.get(task.getString("id")).translate(blockSize*0.125f, -blockSize*0.2f);
            taskObjs.get(task.getString("id")).rotateX(PI/2);
          } else if (!task.isNull("img")) {
            PShape object = createShape(RECT, 0, 0, blockSize/4, blockSize/4);
            object.setFill(color(255, 255, 255));
            object.setTexture(taskImages[i]);
            taskObjs.put(task.getString("id"), object);
            taskObjs.get(task.getString("id")).rotateX(-PI/2);
          }
        }

        LOGGER_MAIN.fine("Loading buildings");
        for (int i=0; i<gameData.getJSONArray("buildings").size(); i++) {
          JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
          if (!buildingType.isNull("obj")) {
            buildingObjs.put(buildingType.getString("id"), new PShape[buildingType.getJSONArray("obj").size()]);
            for (int j=0; j<buildingType.getJSONArray("obj").size(); j++) {
              if (buildingType.getString("id").equals("Quarry")) {
                buildingObjs.get(buildingType.getString("id"))[j] = loadShape(resourcesRoot+"obj/building/quarry.obj");
                buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
                //buildingObjs.get(buildingType.getString("id"))[j].setFill(color(86, 47, 14));
              } else {
                buildingObjs.get(buildingType.getString("id"))[j] = loadShape(resourcesRoot+"obj/building/"+buildingType.getJSONArray("obj").getString(j));
                buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
                buildingObjs.get(buildingType.getString("id"))[j].scale(0.625f);
                buildingObjs.get(buildingType.getString("id"))[j].translate(0, 0, -6);
              }
            }
          }
        }

        bombardArrow = createShape();

        popStyle();
        cinematicMode = false;
        drawRocket = false;
        this.keyState = new HashMap<Character, Boolean>();
        obscuredCellsOverlay = null;
        unseenCellsOverlay = null;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error loading models", e);
        throw e;
      }
    }

    public void generateHighlightingGrid(int horizontals, int verticles) {
      try {
        LOGGER_MAIN.fine("Generating highlighting grid");
        PShape line;
        // Load horizontal lines first
        highlightingGrid = createShape(GROUP);
        for (int i=0; i<horizontals; i++) {
          line = createShape();
          line.beginShape();
          line.noFill();
          for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
            float x2 = -horizontals/2+0.5f+x1/jsManager.loadFloatSetting("terrain detail");
            float y1 = -verticles/2+i+0.5f;
            line.stroke(255, 255, 255, 255-sqrt(pow(x2, 2)+pow(y1, 2))/3*255);
            line.vertex(0, 0, 0);
          }
          line.endShape();
          highlightingGrid.addChild(line);
        }
        // Next do verticle lines
        for (int i=0; i<verticles; i++) {
          line = createShape();
          line.beginShape();
          line.noFill();
          for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
            float y2 = -verticles/2+0.5f+y1/jsManager.loadFloatSetting("terrain detail");
            float x1 = -horizontals/2+i+0.5f;
            line.stroke(255, 255, 255, 255-sqrt(pow(y2, 2)+pow(x1, 2))/3*255);
            line.vertex(0, 0, 0);
          }
          line.endShape();
          highlightingGrid.addChild(line);
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error generating highlighting grid", e);
        throw e;
      }
    }

    public void updateHighlightingGrid(float x, float y, int horizontals, int verticles) {
      // x, y are cell coordinates
      try {
        //LOGGER_MAIN.finer(String.format("Updating selection rect at:%s, %s", x, y));
        PShape line;
        float alpha;
        if (jsManager.loadBooleanSetting("active cell highlighting")) {
          for (int i=0; i<horizontals; i++) {
            line = highlightingGrid.getChild(i);
            for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
              float x2 = PApplet.parseInt(x)-horizontals/2+1+x1/jsManager.loadFloatSetting("terrain detail");
              float y1 = PApplet.parseInt(y)-verticles/2+1+i;
              float x3 = -horizontals/2+x1/jsManager.loadFloatSetting("terrain detail");
              float y3 = -verticles/2+i;
              float dist = sqrt(pow(y3-y%1+1, 2)+pow(x3-x%1+1, 2));
              if (0 < x2 && x2 < mapWidth && 0 < y1 && y1 < mapHeight) {
                alpha = 255-dist/(verticles/2-1)*255;
              } else {
                alpha = 0;
              }
              line.setStroke(x1, color(255, alpha));
              line.setVertex(x1, x2*blockSize, y1*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x2, y1));
            }
          }
          // verticle lines
          for (int i=0; i<verticles; i++) {
            line = highlightingGrid.getChild(i+horizontals);
            for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
              float x1 = PApplet.parseInt(x)-horizontals/2+1+i;
              float y2 = PApplet.parseInt(y)-verticles/2+1+y1/jsManager.loadFloatSetting("terrain detail");
              float y3 = -verticles/2+y1/jsManager.loadFloatSetting("terrain detail");
              float x3 = -horizontals/2+i;
              float dist = sqrt(pow(y3-y%1+1, 2)+pow(x3-x%1+1, 2));
              if (0 < x1 && x1 < mapWidth && 0 < y2 && y2 < mapHeight) {
                alpha = 255-dist/(horizontals/2-1)*255;
              } else {
                alpha = 0;
              }
              line.setStroke(y1, color(255, alpha));
              line.setVertex(y1, x1*blockSize, y2*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x1, y2));
            }
          }
        } else {
          for (int i=0; i<horizontals; i++) {
            line = highlightingGrid.getChild(i);
            for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
              float x2 = PApplet.parseInt(x)-horizontals/2+1+x1/jsManager.loadFloatSetting("terrain detail");
              float y1 = PApplet.parseInt(y)-verticles/2+1+i;
              line.setVertex(x1, x2*blockSize, y1*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x2, y1));
            }
          }
          // verticle lines
          for (int i=0; i<verticles; i++) {
            line = highlightingGrid.getChild(i+horizontals);
            for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
              float x1 = PApplet.parseInt(x)-horizontals/2+1+i;
              float y2 = PApplet.parseInt(y)-verticles/2+1+y1/jsManager.loadFloatSetting("terrain detail");
              line.setVertex(y1, x1*blockSize, y2*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x1, y2));
            }
          }
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating highlighting grid", e);
        throw e;
      }
    }

    public void updateSelectionRect(int cellX, int cellY) {
      try {
        //LOGGER_MAIN.finer(String.format("Updating selection rect at:%s, %s", cellX, cellY));
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        int[] curLoc = {0, 0};
        int a = 0;
        for (int[] dir : directions) {
          for (int i=0; i<jsManager.loadFloatSetting("terrain detail"); i++) {
            tileRect.setVertex(a++, curLoc[0]*blockSize/jsManager.loadFloatSetting("terrain detail"), curLoc[1]*blockSize/jsManager.loadFloatSetting("terrain detail"), getHeight(cellX+curLoc[0]/jsManager.loadFloatSetting("terrain detail"), cellY+curLoc[1]/jsManager.loadFloatSetting("terrain detail")));
            curLoc[0] += dir[0];
            curLoc[1] += dir[1];
          }
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updating selection rect", e);
        throw e;
      }
    }

    public PVector getMousePosOnObject() {
      try {
        applyCameraPerspective();
        PVector floorPos = new PVector(focusedX+width/2, focusedY+height/2, 0);
        PVector floorDir = new PVector(0, 0, -1);
        PVector mousePos = getUnProjectedPointOnFloor(mouseX, mouseY, floorPos, floorDir);
        camera();
        return mousePos;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error finding mouse position on object", e);
        throw e;
      }
    }

    public float getObjectWidth() {
      return mapWidth*blockSize;
    }
    public float getObjectHeight() {
      return mapHeight*blockSize;
    }
    public void setZoom(float zoom) {
      this.zoom =  between(height/3, zoom, min(mapHeight*blockSize, height*4));
    }
    public void setTilt(float tilt) {
      this.tilt = between(0.01f, tilt, 3*PI/8);
    }
    public void setRot(float rot) {
      this.rot = rot;
    }
    public void setFocused(float focusedX, float focusedY) {
      this.focusedX = between(-width/2, focusedX, getObjectWidth()-width/2);
      this.focusedY = between(-height/2, focusedY, getObjectHeight()-height/2);
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
      if (eventType.equals("mouseDragged")) {
        if (mouseButton == LEFT) {
          //if (heldPos != null){

          //camera(prevFx+width/2+zoom*sin(tilt)*sin(rot), prevFy+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), prevFy+width/2, focusedY+height/2, 0, 0, 0, -1);
          //PVector newHeldPos = MousePosOnObject(mouseX, mouseY);
          //camera();
          //focusedX = heldX-(newHeldPos.x-heldPos.x);
          //focusedY = heldY-(newHeldPos.y-heldPos.y);
          //prevFx = heldX-(newHeldPos.x-heldPos.x);
          //prevFy = heldY-(newHeldPos.y-heldPos.y);
          //heldPos.x = newHeldPos.x;
          //heldPos.y = newHeldPos.y;
          //}
        } else if (mouseButton != RIGHT) {
          setTilt(tilt-(mouseY-pmouseY)*0.01f);
          setRot(rot-(mouseX-pmouseX)*0.01f);
          doUpdateHoveringScale();
        }
      }
      //else if (eventType.equals("mousePressed")){
      //  if (button == LEFT){
      //    camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
      //    heldPos = MousePosOnObject(mouseX, mouseY);
      //    heldX = focusedX;
      //    heldY = focusedY;
      //    camera();
      //  }
      //}
      //else if (eventType.equals("mouseReleased")){
      //  if (button == LEFT){
      //    heldPos = null;
      //  }
      //}
      return new ArrayList<String>();
    }


    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
      if (eventType == "mouseWheel") {
        doUpdateHoveringScale();
        float count = event.getCount();
        setZoom(zoom+zoom*count*0.15f);
      }
      return new ArrayList<String>();
    }
    public ArrayList<String> keyboardEvent(String eventType, char _key) {
      if (eventType == "keyPressed") {
        keyState.put(_key, true);
      }
      if (eventType == "keyReleased") {
        keyState.put(_key, false);
      }
      return new ArrayList<String>();
    }
    public float getHeight(float x, float y) {
      //if (y<mapHeight && x<mapWidth && y+jsManager.loadFloatSetting("terrain detail")/blockSize<mapHeight && x+jsManager.loadFloatSetting("terrain detail")/blockSize<mapHeight && y-jsManager.loadFloatSetting("terrain detail")/blockSize>=0 && x-jsManager.loadFloatSetting("terrain detail")/blockSize>=0 &&
      //terrain[floor(y)][floor(x)] == JSONIndex(gameData.getJSONArray("terrain"), "hills")+1 &&
      //terrain[floor(y+jsManager.loadFloatSetting("terrain detail")/blockSize)][floor(x+jsManager.loadFloatSetting("terrain detail")/blockSize)] == JSONIndex(gameData.getJSONArray("terrain"), "hills")+1 &&
      //terrain[floor(y-jsManager.loadFloatSetting("terrain detail")/blockSize)][floor(x-jsManager.loadFloatSetting("terrain detail")/blockSize)] == JSONIndex(gameData.getJSONArray("terrain"), "hills")+1){
      //  return (max(noise(x*MAPNOISESCALE, y*MAPNOISESCALE), waterLevel)-waterLevel)*blockSize*GROUNDHEIGHT*HILLRAISE;
      //} else {
      return max(getRawHeight(x, y), jsManager.loadFloatSetting("water level"))*blockSize*GROUNDHEIGHT;
      //float h = (max(noise(x*MAPNOISESCALE, y*MAPNOISESCALE), waterLevel)-waterLevel);
      //return (max(h-(0.5+waterLevel/2.0), 0)*(1000)+h)*blockSize*GROUNDHEIGHT;
      //}
    }
    public float groundMinHeightAt(int x1, int y1) {
      int x = floor(x1);
      int y = floor(y1);
      return min(new float[]{getHeight(x, y), getHeight(x+1, y), getHeight(x, y+1), getHeight(x+1, y+1)});
    }
    public float groundMaxHeightAt(int x1, int y1) {
      int x = floor(x1);
      int y = floor(y1);
      return max(new float[]{getHeight(x, y), getHeight(x+1, y), getHeight(x, y+1), getHeight(x+1, y+1)});
    }

    public void applyCameraPerspective() {
      float fov = PI/3.0f;
      float cameraZ = (height/2.0f) / tan(fov/2.0f);
      perspective(fov, PApplet.parseFloat(width)/PApplet.parseFloat(height), cameraZ/100.0f, cameraZ*20.0f);
      applyCamera();
    }


    public void applyCameraPerspective(PGraphics canvas) {
      float fov = PI/3.0f;
      float cameraZ = (height/2.0f) / tan(fov/2.0f);
      canvas.perspective(fov, PApplet.parseFloat(width)/PApplet.parseFloat(height), cameraZ/100.0f, cameraZ*20.0f);
      applyCamera(canvas);
    }


    public void applyCamera() {
      camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
    }


    public void applyCamera(PGraphics canvas) {
      canvas.camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
    }

    public void applyInvCamera(PGraphics canvas) {
      canvas.camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), -zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
    }

    public void keyboardControls() {
    }


    public void draw(PGraphics panelCanvas) {
      try {
        // Update camera position and orientation
        frameTime = millis()-prevT;
        prevT = millis();
        focusedV.x=0;
        focusedV.y=0;
        rotv = 0;
        tiltv = 0;
        zoomv = 0;
        if (keyState.containsKey('w')&&keyState.get('w')) {
          focusedV.y -= PANSPEED;
          panning = false;
        }
        if (keyState.containsKey('s')&&keyState.get('s')) {
          focusedV.y += PANSPEED;
          panning = false;
        }
        if (keyState.containsKey('a')&&keyState.get('a')) {
          focusedV.x -= PANSPEED;
          panning = false;
        }
        if (keyState.containsKey('d')&&keyState.get('d')) {
          focusedV.x += PANSPEED;
          panning = false;
        }
        if (keyState.containsKey('q')&&keyState.get('q')) {
          rotv -= ROTSPEED;
        }
        if (keyState.containsKey('e')&&keyState.get('e')) {
          rotv += ROTSPEED;
        }
        if (keyState.containsKey('x')&&keyState.get('x')) {
          tiltv += ROTSPEED;
        }
        if (keyState.containsKey('c')&&keyState.get('c')) {
          tiltv -= ROTSPEED;
        }
        if (keyState.containsKey('f')&&keyState.get('f')) {
          zoomv += PANSPEED;
        }
        if (keyState.containsKey('r')&&keyState.get('r')) {
          zoomv -= PANSPEED;
        }
        focusedV.x = between(-PANSPEED, focusedV.x, PANSPEED);
        focusedV.y = between(-PANSPEED, focusedV.y, PANSPEED);
        rotv = between(-ROTSPEED, rotv, ROTSPEED);
        tiltv = between(-ROTSPEED, tiltv, ROTSPEED);
        zoomv = between(-PANSPEED, zoomv, PANSPEED);
        PVector p = focusedV.copy().rotate(-rot).mult(frameTime*pow(zoom, 0.5f)/20);
        focusedX += p.x;
        focusedY += p.y;
        rot += rotv*frameTime;
        tilt += tiltv*frameTime;
        zoom += zoomv*frameTime;


        if (panning) {
          focusedX -= (focusedX-targetXOffset)*panningSpeed*frameTime*60/1000;
          focusedY -= (focusedY-targetYOffset)*panningSpeed*frameTime*60/1000;
          // Stop panning when very close
          if (abs(focusedX-targetXOffset) < 1 && abs(focusedY-targetYOffset) < 1) {
            panning = false;
          }
        } else {
          targetXOffset = focusedX;
          targetYOffset = focusedY;
        }

        // Check camera ok
        setZoom(zoom);
        setRot(rot);
        setTilt(tilt);
        setFocused(focusedX, focusedY);

        if (panning || rotv != 0 || zoomv != 0 || tiltv != 0 || updateHoveringScale) { // update hovering scale
          updateHoveringScale();
          updateHoveringScale = false;
        }

        // update highlight grid if hovering over diffent pos
        if (!(hoveringX == oldHoveringX && hoveringY == oldHoveringY)) {
          updateHighlightingGrid(hoveringX, hoveringY, 8, 8);
          if (showingBombard && !(PApplet.parseInt(hoveringX) == PApplet.parseInt(oldHoveringX) && PApplet.parseInt(hoveringY) == PApplet.parseInt(oldHoveringY))) {
            updateBombard();
          }
          oldHoveringX = hoveringX;
          oldHoveringY = hoveringY;
        }


        if (drawPath == null && cellSelected && parties[selectedCellY][selectedCellX] != null) {
          ArrayList<int[]> path = parties[selectedCellY][selectedCellX].path;
          updatePath(path, parties[selectedCellY][selectedCellX].target);
        }


        pushStyle();
        hint(ENABLE_DEPTH_TEST);

        // Render 3D stuff from normal camera view onto refraction canvas for refraction effect in water
        //refractionCanvas.beginDraw();
        //refractionCanvas.background(#7ED7FF);
        //float fov = PI/3.0;
        //float cameraZ = (height/2.0) / tan(fov/2.0);
        //applyCamera(refractionCanvas);
        ////refractionCanvas.perspective(fov, float(width)/float(height), 1, 100);
        //refractionCanvas.shader(toon);
        //renderScene(refractionCanvas);
        //refractionCanvas.resetShader();
        //refractionCanvas.camera();
        //refractionCanvas.endDraw();

        //water.setTexture(refractionCanvas);

        // Render 3D stuff from normal camera view
        canvas.beginDraw();
        canvas.background(0);
        applyCameraPerspective(canvas);
        renderWater(canvas);
        renderScene(canvas);
        //canvas.box(0, 0, getObjectWidth(), getObjectHeight(), 1, 100);
        canvas.camera();
        canvas.endDraw();


        //Remove all 3D effects for GUI rendering
        hint(DISABLE_DEPTH_TEST);
        camera();
        noLights();
        resetShader();
        popStyle();

        //draw the scene to the screen
        panelCanvas.image(canvas, 0, 0);
        //image(refractionCanvas, 0, 0);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error drawing 3D map", e);
        throw e;
      }
    }

    public void renderWater(PGraphics canvas) {
      //Draw water
      try {
        canvas.pushMatrix();
        canvas.shape(water);
        canvas.popMatrix();
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error rendering water", e);
        throw e;
      }
    }

    public void drawPath(PGraphics canvas) {
      try {
        if (drawPath != null) {
          canvas.shape(pathLine);
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error drawing path", e);
        throw e;
      }
    }

    public void renderScene(PGraphics canvas) {

      try {
        canvas.directionalLight(240, 255, 255, 0, -0.1f, -1);
        //canvas.directionalLight(100, 100, 100, 0.1, 1, -1);
        //canvas.lightSpecular(102, 102, 102);
        canvas.shape(tiles);
        canvas.ambientLight(100, 100, 100);
        canvas.shape(trees);

        canvas.pushMatrix();
        //noLights();
        if (cellSelected&&!cinematicMode) {
          canvas.pushMatrix();
          canvas.stroke(0);
          canvas.strokeWeight(3);
          canvas.noFill();
          canvas.translate(selectedCellX*blockSize, (selectedCellY)*blockSize, 0);
          updateSelectionRect(selectedCellX, selectedCellY);
          canvas.shape(tileRect);
          canvas.translate(0, 0, groundMinHeightAt(selectedCellX, selectedCellY));
          canvas.strokeWeight(1);
          if (parties[selectedCellY][selectedCellX] != null) {
            canvas.translate(blockSize/2, blockSize/2, 32);
            canvas.box(blockSize, blockSize, 64);
          } else {
            canvas.translate(blockSize/2, blockSize/2, 16);
            canvas.box(blockSize, blockSize, 32);
          }
          canvas.popMatrix();
        }

        if (0<hoveringX&&hoveringX<mapWidth&&0<hoveringY&&hoveringY<mapHeight && !cinematicMode) {
          canvas.pushMatrix();
          drawPath(canvas);
          canvas.shape(highlightingGrid);
          canvas.popMatrix();
        }

        float verySmallSize = VERYSMALLSIZE*(400+(zoom-height/3)*(cos(tilt)+1))/10;
        if (moveNodes != null) {
          canvas.pushMatrix();
          canvas.translate(0, 0, verySmallSize);
          canvas.shape(drawPossibleMoves);
          canvas.translate(0, 0, verySmallSize);
          canvas.shape(dangerousCellsOverlay);
          canvas.popMatrix();
        }

        if (jsManager.loadBooleanSetting("fog of war")) {
          canvas.pushMatrix();
          canvas.translate(0, 0, verySmallSize);
          canvas.shape(obscuredCellsOverlay);
          canvas.shape(unseenCellsOverlay);
          canvas.popMatrix();
        }

        if (showingBombard) {
          drawBombard(canvas);
          canvas.pushMatrix();
          canvas.translate(0, 0, verySmallSize);
          canvas.shape(drawPossibleBombards);
          canvas.popMatrix();
        }

        for (int x=0; x<mapWidth; x++) {
          for (int y=0; y<mapHeight; y++) {
            if (visibleCells[y][x] != null) {
              if (visibleCells[y][x].building != null) {
                if (buildingObjs.get(buildingString(visibleCells[y][x].getBuilding().type)) != null) {
                  canvas.lights();
                  canvas.pushMatrix();
                  if (visibleCells[y][x].building.type==buildingIndex("Mine")) {
                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, 16+groundMinHeightAt(x, y));
                    canvas.rotateZ(getDownwardAngle(x, y));
                  } else if (visibleCells[y][x].building.type==buildingIndex("Quarry")) {
                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, groundMinHeightAt(x, y));
                  } else {
                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, 16+groundMaxHeightAt(x, y));
                  }
                  canvas.shape(buildingObjs.get(buildingString(visibleCells[y][x].building.type))[visibleCells[y][x].building.image_id]);
                  canvas.popMatrix();
                }
              }
              if (visibleCells[y][x].party != null) {
                canvas.noLights();
                if (visibleCells[y][x].party instanceof Battle) {
                  // Swords
                  canvas.pushMatrix();
                  canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, 12+groundMaxHeightAt(x, y));
                  canvas.shape(battle);
                  canvas.popMatrix();

                  // Defender
                  canvas.pushMatrix();
                  canvas.translate((x+0.5f+0.1f)*blockSize, (y+0.5f)*blockSize, 30.5f+groundMinHeightAt(x, y));
                  canvas.scale(0.95f, 0.8f, 0.8f);
                  if (((Battle)visibleCells[y][x].party).defender.player == flags.length) {
                    //Bandit
                    canvas.translate(blockSize*0.25f, 0, 0);
                    canvas.shape(bandit);
                  } else {
                    canvas.shape(flags[((Battle)visibleCells[y][x].party).defender.player]);
                    canvas.scale(5.0f/9.5f, 5.0f/8.0f, 1);
                    canvas.shape(flagPole);
                  }
                  canvas.popMatrix();

                  // Attacker
                  canvas.pushMatrix();
                  canvas.translate((x+0.5f-0.1f)*blockSize, (y+0.5f)*blockSize, 30.5f+groundMinHeightAt(x, y));
                  canvas.scale(-0.95f, 0.8f, 0.8f);
                  if (((Battle)visibleCells[y][x].party).attacker.player == flags.length) {
                    //Bandit
                    canvas.translate(-blockSize*0.25f, 0, 0);
                    canvas.shape(bandit);
                  } else {
                    canvas.shape(flags[((Battle)visibleCells[y][x].party).attacker.player]);
                    canvas.scale(5.0f/9.5f, 5.0f/8.0f, 1);
                    canvas.shape(flagPole);
                  }
                  canvas.popMatrix();
                } else {
                  if (visibleCells[y][x].party.player == playerColours.length-1) {
                    canvas.pushMatrix();
                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, 23+groundMinHeightAt(x, y));
                    canvas.shape(bandit);
                    canvas.popMatrix();
                  } else {
                    canvas.pushMatrix();
                    canvas.translate((x+0.5f-0.4f)*blockSize, (y+0.5f)*blockSize, 23+groundMinHeightAt(x, y));
                    canvas.shape(flagPole);
                    canvas.shape(flags[visibleCells[y][x].party.player]);
                    canvas.popMatrix();
                  }
                }

                if (drawingUnitBars&&!cinematicMode) {
                  drawUnitBar(x, y, canvas);
                }

                JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(visibleCells[y][x].party.task);
                if (drawingTaskIcons && jo != null && !jo.isNull("img") && !cinematicMode) {
                  canvas.noLights();
                  canvas.pushMatrix();
                  canvas.translate((x+0.5f+sin(rot)*0.125f)*blockSize, (y+0.5f+cos(rot)*0.125f)*blockSize, blockSize*1.7f+groundMinHeightAt(x, y));
                  canvas.rotateZ(-this.rot);
                  canvas.translate(-0.125f*blockSize, -0.25f*blockSize);
                  canvas.rotateX(PI/2-this.tilt);
                  canvas.translate(0, 0, blockSize*0.35f);
                  canvas.shape(taskObjs.get(jo.getString("id")));
                  canvas.popMatrix();
                }
              }
            }
          }
        }
        canvas.popMatrix();

        if (drawRocket) {
          drawRocket(canvas);
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error rendering scene", e);
        throw e;
      }
    }

    //void renderTexturedEntities(PGraphics canvas) {

    //}

    public void drawUnitBar(int x, int y, PGraphics canvas) {
      try {
        if (visibleCells[y][x].party instanceof Battle) {
          Battle battle = (Battle) visibleCells[y][x].party;
          unitNumberObjects[battle.attacker.player].setVertex(0, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
          unitNumberObjects[battle.attacker.player].setVertex(1, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
          unitNumberObjects[battle.attacker.player].setVertex(2, blockSize, blockSize*0.0625f, 0);
          unitNumberObjects[battle.attacker.player].setVertex(3, blockSize, 0, 0);
          unitNumberObjects[battle.attacker.player].setVertex(4, 0, 0, 0);
          unitNumberObjects[battle.attacker.player].setVertex(5, 0, blockSize*0.0625f, 0);
          unitNumberObjects[battle.attacker.player].setVertex(6, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
          unitNumberObjects[battle.attacker.player].setVertex(7, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
          unitNumberObjects[battle.defender.player].setVertex(0, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
          unitNumberObjects[battle.defender.player].setVertex(1, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125f, 0);
          unitNumberObjects[battle.defender.player].setVertex(2, blockSize, blockSize*0.125f, 0);
          unitNumberObjects[battle.defender.player].setVertex(3, blockSize, blockSize*0.0625f, 0);
          unitNumberObjects[battle.defender.player].setVertex(4, 0, blockSize*0.0625f, 0);
          unitNumberObjects[battle.defender.player].setVertex(5, 0, blockSize*0.125f, 0);
          unitNumberObjects[battle.defender.player].setVertex(6, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125f, 0);
          unitNumberObjects[battle.defender.player].setVertex(7, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
          canvas.noLights();
          canvas.pushMatrix();
          canvas.translate((x+0.5f+sin(rot)*0.5f)*blockSize, (y+0.5f+cos(rot)*0.5f)*blockSize, blockSize*1.6f+groundMinHeightAt(x, y));
          canvas.rotateZ(-this.rot);
          canvas.translate(-0.5f*blockSize, -0.5f*blockSize);
          canvas.rotateX(PI/2-this.tilt);
          canvas.shape(unitNumberObjects[battle.attacker.player]);
          canvas.shape(unitNumberObjects[battle.defender.player]);
          canvas.popMatrix();
        } else {
          canvas.noLights();
          canvas.pushMatrix();
          canvas.translate((x+0.5f+sin(rot)*0.5f)*blockSize, (y+0.5f+cos(rot)*0.5f)*blockSize, blockSize*1.6f+groundMinHeightAt(x, y));
          canvas.rotateZ(-this.rot);
          canvas.translate(-0.5f*blockSize, -0.5f*blockSize);
          canvas.rotateX(PI/2-this.tilt);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(0, blockSize*visibleCells[y][x].party.getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(1, blockSize*visibleCells[y][x].party.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125f, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(2, blockSize, blockSize*0.125f, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(3, blockSize, 0, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(4, 0, 0, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(5, 0, blockSize*0.125f, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(6, blockSize*visibleCells[y][x].party.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125f, 0);
          unitNumberObjects[visibleCells[y][x].party.player].setVertex(7, blockSize*visibleCells[y][x].party.getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
          canvas.shape(unitNumberObjects[visibleCells[y][x].party.player]);
          canvas.popMatrix();
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error drawing unit bar", e);
        throw e;
      }
    }

    public String buildingString(int buildingI) {
      if (gameData.getJSONArray("buildings").isNull(buildingI)) {
        LOGGER_MAIN.warning("invalid building string: "+(buildingI-1));
        return null;
      }
      return gameData.getJSONArray("buildings").getJSONObject(buildingI).getString("id");
    }

    public boolean mouseOver() {
      return mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h;
    }

    public float getRayHeightAt(PVector r, PVector s, float targetX) {
      PVector start = s.copy();
      PVector ray = r.copy();
      float dz_dx = ray.z/ray.x;
      return start.z + (targetX - start.x) * dz_dx;
    }

    public boolean rayPassesThrough(PVector r, PVector s, PVector targetV) {
      PVector start = s.copy();
      PVector ray = r.copy();
      start.add(ray);
      return start.dist(targetV) < blockSize/jsManager.loadFloatSetting("terrain detail");
    }



    // Ray Tracing Code Below is an example by Bontempos, modified for height map intersection by Jack Parsons
    // https://forum.processing.org/two/discussion/21644/picking-in-3d-through-ray-tracing-method

    // Function that calculates the coordinates on the floor surface corresponding to the screen coordinates
    public PVector getUnProjectedPointOnFloor(float screen_x, float screen_y, PVector floorPosition, PVector floorDirection) {

      try {
        PVector f = floorPosition.copy(); // Position of the floor
        PVector n = floorDirection.copy(); // The direction of the floor ( normal vector )
        PVector w = unProject(screen_x, screen_y, -1.0f); // 3 -dimensional coordinate corresponding to a point on the screen
        PVector e = getEyePosition(); // Viewpoint position

        // Computing the intersection of
        f.sub(e);
        w.sub(e);
        w.mult( n.dot(f)/n.dot(w) );
        PVector ray = w.copy();
        w.add(e);

        double acHeight, curX = e.x, curY = e.y, curZ = e.z, minHeight = getHeight(-1, -1);
        // If ray looking upwards or really far away
        if (ray.z > 0 || ray.mag() > blockSize*mapWidth*mapHeight) {
          return new PVector(-1, -1, -1);
        }
        for (int i = 0; i < ray.mag()*2; i++) {
          curX += ray.x/ray.mag()/2;
          curY += ray.y/ray.mag()/2;
          curZ += ray.z/ray.mag()/2;
          if (0 <= curX/blockSize && curX/blockSize <= mapWidth && 0 <= curY/blockSize && curY/blockSize < mapHeight) {
            acHeight = (double)getHeight((float)curX/blockSize, (float)curY/blockSize);
            if (curZ < acHeight+0.000001f) {
              return new PVector((float)curX, (float)curY, (float)acHeight);
            }
          }
          if (curZ < minHeight) { // if out of bounds and below water
            break;
          }
        }
        return new PVector(-1, -1, -1);
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updaing getting unprojected point on floor", e);
        throw e;
      }
    }

    // Function to get the position of the viewpoint in the current coordinate system
    public PVector getEyePosition() {
      applyCameraPerspective();
      PMatrix3D mat = (PMatrix3D)getMatrix(); //Get the model view matrix
      mat.invert();
      return new PVector( mat.m03, mat.m13, mat.m23 );
    }
    //Function to perform the conversion to the local coordinate system ( reverse projection ) from the window coordinate system
    public PVector unProject(float winX, float winY, float winZ) {
      PMatrix3D mat = getMatrixLocalToWindow();
      mat.invert();

      float[] in = {winX, winY, winZ, 1.0f};
      float[] out = new float[4];
      mat.mult(in, out);  // Do not use PMatrix3D.mult(PVector, PVector)

      if (out[3] == 0 ) {
        return null;
      }

      PVector result = new PVector(out[0]/out[3], out[1]/out[3], out[2]/out[3]);
      return result;
    }

    //Function to compute the transformation matrix to the window coordinate system from the local coordinate system
    public PMatrix3D getMatrixLocalToWindow() {
      try {
        PMatrix3D projection = ((PGraphics3D)g).projection;
        PMatrix3D modelview = ((PGraphics3D)g).modelview;

        // viewport transf matrix
        PMatrix3D viewport = new PMatrix3D();
        viewport.m00 = viewport.m03 = width/2;
        viewport.m11 = -height/2;
        viewport.m13 =  height/2;

        // Calculate the transformation matrix to the window coordinate system from the local coordinate system
        viewport.apply(projection);
        viewport.apply(modelview);
        return viewport;
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error updaing getting local to windows matrix", e);
        throw e;
      }
    }
    public void enableRocket(PVector pos, PVector vel) {
      drawRocket = true;
      rocketPosition = pos;
      rocketVelocity = vel;
    }

    public void disableRocket() {
      drawRocket = false;
    }

    public void drawRocket(PGraphics canvas) {
      try {
        canvas.lights();
        canvas.pushMatrix();
        canvas.translate((rocketPosition.x+0.5f)*blockSize, (rocketPosition.y+0.5f)*blockSize, rocketPosition.z*blockSize+16+groundMaxHeightAt(PApplet.parseInt(rocketPosition.x), PApplet.parseInt(rocketPosition.y)));
        canvas.rotateY(atan2(rocketVelocity.x, rocketVelocity.z));
        canvas.shape(buildingObjs.get("Rocket Factory")[2]);
        canvas.popMatrix();
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error drawing rocket", e);
        throw e;
      }
    }

    public void reset() {
      cinematicMode = false;
      drawRocket = false;
      showingBombard = false;
    }

    public void drawBombard(PGraphics canvas) {
      canvas.shape(bombardArrow);
    }

    public void updateBombard() {
      PVector pos = getMousePosOnObject();
      int x = floor(pos.x/blockSize);
      int y = floor(pos.y/blockSize);
      if (pos.equals(new PVector(-1, -1, -1)) || dist(x, y, selectedCellX, selectedCellY) > bombardRange || (x == selectedCellX && y == selectedCellY)) {
        bombardArrow.setVisible(false);
      } else {
        LOGGER_MAIN.finer("Loading bombard arrow");
        PVector startPos = new PVector((selectedCellX+0.5f)*blockSize, (selectedCellY+0.5f)*blockSize, getHeight(selectedCellX+0.5f, selectedCellY+0.5f));
        PVector endPos = new PVector((x+0.5f)*blockSize, (y+0.5f)*blockSize, getHeight(x+0.5f, y+0.5f));
        float rotation = -atan2(startPos.x-endPos.x, startPos.y - endPos.y)+0.0001f;
        PVector thicknessAdder = new PVector(blockSize*0.1f*cos(rotation), blockSize*0.1f*sin(rotation), 0);
        PVector startPosA = PVector.add(startPos, thicknessAdder);
        PVector startPosB = PVector.sub(startPos, thicknessAdder);
        PVector endPosA = PVector.add(endPos, thicknessAdder);
        PVector endPosB = PVector.sub(endPos, thicknessAdder);
        fill(255, 0, 0);
        bombardArrow = createShape();
        bombardArrow.beginShape(TRIANGLES);
        float INCREMENT = 0.01f;
        PVector currentPosA = startPosA.copy();
        PVector currentPosB = startPosB.copy();
        PVector nextPosA;
        PVector nextPosB;
        for (float i = 0; i <= 1 && currentPosA.dist(endPosA) > blockSize*0.2f; i += INCREMENT) {
          nextPosA = PVector.add(startPosA, PVector.mult(PVector.sub(endPosA, startPosA), i)).add(new PVector(0, 0, blockSize*4*i*(1-i)));
          nextPosB = PVector.add(startPosB, PVector.mult(PVector.sub(endPosB, startPosB), i)).add(new PVector(0, 0, blockSize*4*i*(1-i)));
          bombardArrow.vertex(currentPosA.x, currentPosA.y, currentPosA.z);
          bombardArrow.vertex(currentPosB.x, currentPosB.y, currentPosB.z);
          bombardArrow.vertex(nextPosA.x, nextPosA.y, nextPosA.z);
          bombardArrow.vertex(currentPosB.x, currentPosB.y, currentPosB.z);
          bombardArrow.vertex(nextPosA.x, nextPosA.y, nextPosA.z);
          bombardArrow.vertex(nextPosB.x, nextPosB.y, nextPosB.z);
          currentPosA = nextPosA;
          currentPosB = nextPosB;
        }

        PVector temp = PVector.add(currentPosA, thicknessAdder);
        bombardArrow.vertex(currentPosA.x, currentPosA.y, currentPosA.z);
        bombardArrow.vertex(temp.x, temp.y, temp.z);
        bombardArrow.vertex(endPos.x, endPos.y, endPos.z);

        bombardArrow.vertex(currentPosA.x, currentPosA.y, currentPosA.z);
        bombardArrow.vertex(currentPosB.x, currentPosB.y, currentPosB.z);
        bombardArrow.vertex(endPos.x, endPos.y, endPos.z);

        temp = PVector.sub(currentPosB, thicknessAdder);
        bombardArrow.vertex(currentPosB.x, currentPosB.y, currentPosB.z);
        bombardArrow.vertex(temp.x, temp.y, temp.z);
        bombardArrow.vertex(endPos.x, endPos.y, endPos.z);

        bombardArrow.endShape();
        bombardArrow.setVisible(true);
      }
    }

    public void enableBombard(int range) {
      showingBombard = true;
      bombardRange = range;
      updateBombard();
      updatePossibleBombards();
    }

    public void disableBombard() {
      showingBombard = false;
    }

    public void setPlayerColours(int[] playerColours) {
      this.playerColours = playerColours;
    }
  }



  class Building {
    int type;
    int image_id;
    Building(int type) {
      this(type, 0, -1);
    }

    Building(int type, int image_id) {
      this(type, image_id, -1);
    }

    Building(int type, int image_id, int player_id) {
      this.type = type;
      this.image_id = image_id + (player_id+1)*10000;
    }

    public void setHealth(float health) {
      this.image_id = PApplet.parseInt(this.image_id / 10000) * 10000 + PApplet.parseInt(health * 1000);
    }

    public float getHealth() {
      return this.image_id % 1000;
    }

    public int getDefence() {
      JSONObject o = gameData.getJSONArray("buildings").getJSONObject(type);
      if (o.hasKey("defence")) {
        return o.getInt("defence");
      } else {
        return 0;
      }
    }

    public boolean isDefenceBuilding() {
      return getDefence() > 0;
    }

    public int getPlayerID() {
      return (this.image_id / 10000) - 1;
    }
  }

  class Party {
    private int trainingFocus;
    private int unitNumber;
    private int unitCap;
    private int movementPoints, maxMovementPoints;
    private float[] proficiencies;
    private int task;
    private int[] equipment;
    private int [] equipmentQuantities;
    String id;
    int player;
    float strength;
    ArrayList<Action> actions;
    ArrayList<int[]> path;
    int[] target;
    int pathTurns;
    byte[] byteRep;
    boolean autoStockUp;
    int sightRadiusPerUnit;

    Party(int player, int startingUnits, int startingTask, int movementPoints, String id) {
      unitNumber = startingUnits;
      task = startingTask;
      this.player = player;
      this.movementPoints = movementPoints;
      actions = new ArrayList<Action>();
      strength = 1.5f;
      clearPath();
      target = null;
      pathTurns = 0;
      this.id = id;
      autoStockUp = false;

      this.unitCap = jsManager.loadIntSetting("party size");  // Set unit cap to default

      // Default proficiencies = 1
      resetRawProficiencies();
      for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
        this.setRawProficiency(i, 1);
      }

      setTrainingFocus(jsManager.proficiencyIDToIndex("melee attack"));

      this.equipmentQuantities = new int[jsManager.getNumEquipmentClasses()];

      equipment = new int[jsManager.getNumEquipmentClasses()];
      for (int i=0; i<equipment.length;i ++){
        equipment[i] = -1; // -1 represens no equipment
      }

      updateMaxMovementPoints();
    }

    Party(int player, int startingUnits, int startingTask, int movementPoints, String id, float[] proficiencies, int trainingFocus, int[] equipment, int[] equipmentQuantities, int unitCap, boolean autoStockUp) {
      // For parties that already exist and are being splitted or loaded from save
      unitNumber = startingUnits;
      task = startingTask;
      this.player = player;
      this.movementPoints = movementPoints;
      this.actions = new ArrayList<Action>();
      this.strength = 1.5f;
      this.clearPath();
      this.target = null;
      this.pathTurns = 0;
      this.id = id;
      this.unitCap = unitCap;
      this.autoStockUp = autoStockUp;

      this.equipment = equipment;
      this.equipmentQuantities = equipmentQuantities;

      // Load proficiencies given
      try {
        resetRawProficiencies();
        for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
          this.setRawProficiency(i, proficiencies[i]);
        }
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.severe(String.format("Not enough proficiencies given to party:%d (needs %d)  id:%s", proficiencies.length, jsManager.getNumProficiencies(), id));
      }

      setTrainingFocus(trainingFocus);  // 'trainingFocus' is an id string

      updateMaxMovementPoints();
    }

    public int containsPartyFromPlayer(int p) {
      return PApplet.parseInt(player == p);
    }

    public float getTrainingRateMultiplier(float x){
      // x is the current value of the proficiency
      return 4*exp(x-1)/pow(exp(x-1)+1, 2);  // This function is based on the derivative of the logisitics function. The factor of 4 is to make it start at 1.
    }

    public void trainParty(String proficiencyID, String trainingConstantID){
      // This method trains the party in one proficiency
      // The rate of training depends on the current level, and gets progressive more difficault to train parties
      // trainingConstantID is the ID for the raw gain, which is an arbitrary value that represents how significant an event is, and so how big the proficiency gain should be

      float currentProficiencyValue = getRawProficiency(jsManager.proficiencyIDToIndex(proficiencyID));
      float rawGain = jsManager.getRawProficiencyGain(trainingConstantID);
      float addedProficiencyValue = getTrainingRateMultiplier(currentProficiencyValue) * rawGain;
      if (trainingFocus == jsManager.proficiencyIDToIndex(proficiencyID)){ // If training focus, then 2x gains from training this proficiency
        addedProficiencyValue *= 2;
      }
      LOGGER_GAME.fine(String.format("Training party id:%s. Raw gain:%f. Added proficiency value: %f. New proficiency value:%f", id, rawGain, addedProficiencyValue, currentProficiencyValue+addedProficiencyValue));
      setRawProficiency(jsManager.proficiencyIDToIndex(proficiencyID), currentProficiencyValue+addedProficiencyValue);
    }

    public float getEffectivenessMultiplier(String type){
      // This is the funciton to get the effectiveness of the party as different tasks based on proficiencies
      // 'type' is the ID used to determine which constant to use from data.json
      float proficiency = getTotalProficiency(jsManager.proficiencyIDToIndex(type));
      if (proficiency <= 0){ // proficiencies should never be negative because log(0) is undefined and log(<0) is complex.
        return 0;
      } else {
        return jsManager.getEffectivenessConstant(type) * log(proficiency) + 1;
      }
    }

    public boolean getAutoStockUp(){
      return autoStockUp;
    }

    public void setAutoStockUp(boolean v){
      autoStockUp = v;
    }

    public void updateMaxMovementPoints(){
      // Based on speed proficiency
      this.maxMovementPoints = floor(gameData.getJSONObject("game options").getInt("movement points") * getEffectivenessMultiplier("speed"));
      setMovementPoints(min(maxMovementPoints, getMovementPoints()));
    }

    public void resetMovementPoints(){
      updateMaxMovementPoints();
      setMovementPoints(maxMovementPoints);
    }

    public int getMaxMovementPoints(){
      return this.maxMovementPoints;
    }

    public void setUnitCap(int value){
      this.unitCap = value;
    }

    public int getUnitCap(){
      return unitCap;
    }

    public void setTrainingFocus(int value) {
      // Training focus is the index of the proficiency in data.json
      this.trainingFocus = value;
    }
    public String getID() {
      return id;
    }

    public int getTask() {
      return task;
    }

    public int getTrainingFocus() {
      // Training focus is the index of the proficiency in data.json
      return this.trainingFocus;
    }

    public void setAllEquipment(int[] v) {
      equipment = v;
    }

    public int[] getAllEquipment() {
      return equipment;
    }

    public int getEquipmentQuantity(int classIndex){
      return equipmentQuantities[classIndex];
    }

    public void setEquipmentQuantity(int classIndex, int quantity){
      equipmentQuantities[classIndex] = quantity;
    }

    public void addEquipmentQuantity(int classIndex, int addedQuantity){
      setEquipmentQuantity(classIndex, getEquipmentQuantity(classIndex)+addedQuantity);
    }

    public int[] getEquipmentQuantities(){
      return equipmentQuantities;
    }

    public void setEquipmentQuantities(int[] v){
      equipmentQuantities = v;
    }

    public void setEquipment(int classIndex, int equipmentIndex, int quantity) {
      equipment[classIndex] = equipmentIndex;
      equipmentQuantities[classIndex] = quantity;
      LOGGER_GAME.finer(String.format("changing equipment for party with id:'%s' which now has equipment:%s", id, Arrays.toString(equipment)));
    }

    public int getEquipment(int classIndex) {
      return equipment[classIndex];
    }

    public int[] splittedQuantities(int numUnitsSplitted){
      int[] splittedEquipmentQuantities = new int[equipment.length];
      for (int i=0; i < equipment.length; i ++){
        splittedEquipmentQuantities[i] = ceil(getEquipmentQuantity(i) * numUnitsSplitted / getUnitNumber());
      }
      return splittedEquipmentQuantities;
    }

    public Party splitParty(int numUnitsSplitted, String newID){
      if (numUnitsSplitted <= getUnitNumber()){
        int[] splittedEquipmentQuantities = splittedQuantities(numUnitsSplitted);
        for (int i=0; i < equipment.length; i ++){
          setEquipmentQuantity(i, getEquipmentQuantity(i) - splittedEquipmentQuantities[i]);
        }
        changeUnitNumber(-numUnitsSplitted);
        return new Party(player, numUnitsSplitted, getTask(), getMovementPoints(), newID, Arrays.copyOf(getRawProficiencies(), getRawProficiencies().length), getTrainingFocus(), Arrays.copyOf(getAllEquipment(), getAllEquipment().length), splittedEquipmentQuantities, getUnitCap(), getAutoStockUp());
      }
      else{
        LOGGER_GAME.warning(String.format("Num units splitted more than in party. ID:%s", numUnitsSplitted));
        return null;
      }
    }

    public int getOverflow(int unitsTransfered){
      return max((this.getUnitNumber()+unitsTransfered) - this.getUnitCap(), 0);
    }

    public float[] mergeProficiencies(Party other, int unitsTransfered){
      // Other's proficiecies unaffected by merge
      float[] rawProficiencies = new float[proficiencies.length];
      for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
        rawProficiencies[i] = mergeAttribute(this.getUnitNumber(), this.getRawProficiency(i), unitsTransfered, other.getRawProficiency(i));
      }
      return rawProficiencies;
    }

    public int[][] mergeEquipment(Party other, int unitsTransfered){
      //index 0 is equipment for this party, index 1 is equipment for other party, index 2 is for quantity of this party, index 3 is for quantity of other party
      int[][] equipments = new int[4][equipment.length];
      for (int i = 0; i < getAllEquipment().length; i ++){
        int amountTransfered = ceil((float)other.getEquipmentQuantity(i) * ((float)unitsTransfered / other.getUnitNumber()));
        if (this.getEquipment(i) != -1 && this.getEquipment(i) == other.getEquipment(i)){
          // If both parties have same equipment then add quantities togther
          equipments[0][i] = this.getEquipment(i);
          equipments[1][i] = other.getEquipment(i);
          equipments[2][i] = this.getEquipmentQuantity(i) + amountTransfered;
          equipments[3][i] = other.getEquipmentQuantity(i) - amountTransfered;
        }
        else if (this.getEquipment(i) == -1 && other.getEquipment(i) != -1){
          // If this party has nothing equipped but the other party has something equipped, equip that.
          equipments[0][i] = other.getEquipment(i);
          equipments[1][i] = other.getEquipment(i);
          equipments[2][i] = amountTransfered;
          equipments[3][i] = other.getEquipmentQuantity(i) - amountTransfered;
        }
        else {
          // Else: quantity stays same
          equipments[0][i] = this.getEquipment(i);
          equipments[1][i] = other.getEquipment(i);
          equipments[2][i] = this.getEquipmentQuantity(i);
          equipments[3][i] = other.getEquipmentQuantity(i);
        }
      }
      return equipments;
    }

    public int mergeEntireFrom(Party other, int moveCost, Player player) {
      // Note: will need to remove other division
      LOGGER_GAME.fine(String.format("Merging entire party from id:%s into party with id:%s", other.id, this.id));
      return mergeFrom(other, other.getUnitNumber(), moveCost, player);
    }

    public int mergeFrom(Party other, int unitsTransfered, int moveCost, Player player) {
      // Take units from other party into this party and merge attributes, weighted by unit number
      LOGGER_GAME.fine(String.format("Merging %d units from party with id:%s into party with id:%s", unitsTransfered, other.id, this.id));

      int overflow = getOverflow(unitsTransfered);

      unitsTransfered -= overflow;  // Dont do anything to the overflow units

      // Merge all proficiencies with other party
      this.setRawProficiencies(mergeProficiencies(other, unitsTransfered));

      // Merge equipment
      int[][] equipments = mergeEquipment(other, unitsTransfered);
      for (int i = 0; i < getAllEquipment().length; i ++){
        if (other.getEquipment(i) != equipments[1][i]){
          // Recycle all equipment when it changes type during merge
          player.resources[jsManager.getResIndex(jsManager.getEquipmentTypeID(i, other.getEquipment(i)))] += other.getEquipmentQuantity(i);
        }
      }
      this.setAllEquipment(equipments[0]);
      other.setAllEquipment(equipments[1]);
      this.setEquipmentQuantities(equipments[2]);
      other.setEquipmentQuantities(equipments[3]);

      LOGGER_GAME.finer(String.format("New proficiency values: %s for party with id:%s", Arrays.toString(proficiencies), id));
      // Note: other division attributes unaffected by merge

      int movementPoints = min(this.getMovementPoints(), other.getMovementPoints()-moveCost);
      this.setMovementPoints(movementPoints);

      this.changeUnitNumber(unitsTransfered); // Units left over after merging
      other.changeUnitNumber(-unitsTransfered);

      return overflow; // Return units left in othr party
    }

    public void setID(String value) {
      this.id = value;
    }

    public float mergeAttribute(int units1, float attrib1, int units2, float attrib2) {
      // Calcaulate the attributes for merge weighted by units number
      return (units1 * attrib1 + units2 * attrib2) / (units1 + units2);
    }

    public void changeTask(int task) {
      //LOGGER_GAME.info("Party changing task to:"+gameData.getJSONArray("tasks").getJSONObject(task).getString("id")); Removed as this is called too much for battle estimates
      try {
        this.task = task;
        JSONObject jTask = gameData.getJSONArray("tasks").getJSONObject(this.getTask());
        if (!jTask.isNull("strength")) {
          this.strength = jTask.getInt("strength");
        } else {
          this.strength = 1.5f;
        }
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.WARNING, String.format("Error changing party task, id:%s, task=%s. Likely cause is something wrong in data.json", id, task), e);
      }
    }

    public void setPathTurns(int v) {
      LOGGER_GAME.finer(String.format("Setting path turns to:%s, party id:%s", v, id));
      pathTurns = v;
    }

    public void moved() {
      LOGGER_GAME.finest("Decreasing pathTurns due to party moving id: "+id);
      pathTurns = max(pathTurns-1, 0);
    }

    public int[] nextNode() {
      try {
        return path.get(0);
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.WARNING, "Party run out of nodes id:"+id, e);
        return null;
      }
    }

    public void loadPath(ArrayList<int[]> p) {
      LOGGER_GAME.finer("Loading path into party id:"+id);
      path = p;
    }

    public void clearNode() {
      path.remove(0);
    }

    public void clearPath() {
      //LOGGER_GAME.finer("Clearing party path"); Removed as this is called too much for battle estimates
      path = new ArrayList<int[]>();
      pathTurns=0;
    }

    public void addAction(Action a) {
      actions.add(a);
    }

    public boolean hasActions() {
      return actions.size()>0;
    }

    public int turnsLeft() {
      return calcTurns(actions.get(0).turns);
    }

    public int calcTurns(float turnsCost) {
      //Use this to calculate the number of turns a task will take for this party
      return ceil(turnsCost/(sqrt(unitNumber)/10));
    }

    public Action progressAction() {
      try {
        if (actions.size() == 0) {
          return null;
        }
        LOGGER_GAME.finer(String.format("Party action progressing: '%s', id:%s", actions.get(0).type, id));
        if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0) {
          return actions.get(0);
        } else {
          actions.get(0).turns -= sqrt((float)unitNumber)/10;
          if (gameData.getJSONArray("tasks").getJSONObject(actions.get(0).type).getString("id").contains("Build")) {
            if (actions.get(0).turns-sqrt((float)unitNumber)/10 <= 0) {
              return new Action(JSONIndex(gameData.getJSONArray("tasks"), "Construction End"), "Construction End", 0, null, null);
            } else {
              return new Action(JSONIndex(gameData.getJSONArray("tasks"), "Construction Mid"), "Construction Mid", 0, null, null);
            }
          }
          return null;
        }
      }
      catch(Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Progressing party action failed id:"+id);
        throw e;
      }
    }
    public void clearCurrentAction() {
      if (actions.size() > 0) {
        LOGGER_GAME.finest(String.format("Clearing party current action of type:%s, id:%s", actions.get(0).type, id));
        actions.remove(0);
      }
    }

    public void clearActions() {
      actions = new ArrayList<Action>();
    }

    public int currentAction() {
      return actions.get(0).type;
    }

    public boolean isTurn(int turn) {
      return this.player==turn;
    }

    public int getMovementPoints() {
      return movementPoints;
    }

    public void subMovementPoints(int p) {
      movementPoints -= p;
    }

    public void setMovementPoints(int p) {
      movementPoints = p;
    }

    public int getMovementPoints(int turn) {
      return movementPoints;
    }

    public int getUnitNumber() {
      return unitNumber;
    }

    public int getUnitNumber(int turn) {
      return unitNumber;
    }

    public void setUnitNumber(int newUnitNumber) {
      unitNumber = (int)between(0, newUnitNumber, jsManager.loadIntSetting("party size"));
    }

    public int changeUnitNumber(int changeInUnitNumber) {
      int overflow = max(0, changeInUnitNumber+unitNumber-jsManager.loadIntSetting("party size"));
      this.setUnitNumber(unitNumber+changeInUnitNumber);
      return overflow;
    }

    public int[][] removeExcessEquipment() {
      int[][] excessEquipment = new int[equipment.length][];
      int i = 0;
      for (int quantity: equipmentQuantities) {
        if (equipment[i] != -1) {
          excessEquipment[i] = new int[] {equipment[i], max(0, quantity-unitNumber)};
          LOGGER_GAME.fine(String.format("Removing %d excess %s from party %s", quantity-unitNumber, jsManager.getEquipmentTypeID(i, equipment[i]), id));
          equipmentQuantities[i] = min(quantity, unitNumber);
        }
        i++;
      }
      return excessEquipment;
    }

    public Party clone() {
      Party newParty = new Party(player, unitNumber, task, movementPoints, id);
      newParty.actions = new ArrayList<Action>(actions);
      newParty.strength = strength;
      newParty.equipment = equipment.clone();
      newParty.equipmentQuantities = equipmentQuantities.clone();
      newParty.proficiencies = proficiencies.clone();
      newParty.trainingFocus = trainingFocus;
      newParty.unitCap = unitCap;
      newParty.autoStockUp = autoStockUp;
      return newParty;
    }

    public float getRawProficiency(int index) {
      // index is index of proficiency in data.json
      return proficiencies[index];
    }

    public void setRawProficiency(int index, float value) {
      // index is index of proficiency in data.json
      proficiencies[index] = value;
    }

    public void resetRawProficiencies() {
      proficiencies = new float[jsManager.getNumProficiencies()];
    }

    public float[] getRawProficiencies() {
      return proficiencies;
    }

    public void setRawProficiencies(float[] values) {
      this.proficiencies = values;
    }

    public float getProficiencyBonusMultiplier(int index){
      // index is index of proficiency in data.json
      float bonusMultiplier = 0;
      JSONObject equipmentClassJO;
      JSONObject equipmentTypeJO;
      String proficiencyID = jsManager.indexToProficiencyID(index);
      for (int i = 0 ; i < getAllEquipment().length; i++){
        if (getEquipment(i) != -1){
          try{
            equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(i);
            equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(getEquipment(i));

            // Check each equipment equipped for proficiencies to calculate bonus
            if (!equipmentTypeJO.isNull(proficiencyID)){
              bonusMultiplier += equipmentTypeJO.getFloat(proficiencyID) * ((float)getEquipmentQuantity(i)/getUnitNumber());  // Weight each bonus by the proportion of units that have access to equipment
            }
          }
          catch (Exception e){
            LOGGER_GAME.log(Level.WARNING, "Error loading equipment", e);
          }
        }
      }
      return bonusMultiplier;
    }

    public ArrayList<String> getProficiencyBonusMultiplierBreakdown(int index){
      // index is index of proficiency in data.json
      // This method is for the breakdown used by tooltip of bonus
      JSONObject equipmentClassJO;
      JSONObject equipmentTypeJO;
      ArrayList<String> returnMe = new ArrayList<String>();
      String proficiencyID = jsManager.indexToProficiencyID(index);
      for (int i = 0 ; i < getAllEquipment().length; i++){
        if (getEquipment(i) != -1){
          try{
            equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(i);
            equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(getEquipment(i));

            // Check each equipment equipped for proficiencies to calculate bonus
            if (!equipmentTypeJO.isNull(proficiencyID)){
              if (equipmentTypeJO.getFloat(proficiencyID) > 0){
                returnMe.add(String.format("<g>+%s</g> from %s (%d/%d)", roundDpTrailing("+"+getRawProficiency(index)*equipmentTypeJO.getFloat(proficiencyID) * ((float)getEquipmentQuantity(i)/getUnitNumber()), 2), equipmentTypeJO.getString("display name"), getEquipmentQuantity(i), getUnitNumber()));
              }
              else if (equipmentTypeJO.getFloat(proficiencyID) < 0){
                returnMe.add(String.format("<g>%s</g> from %s (%d/%d)", roundDpTrailing("+"+getRawProficiency(index)*equipmentTypeJO.getFloat(proficiencyID) * ((float)getEquipmentQuantity(i)/getUnitNumber()), 2), equipmentTypeJO.getString("display name"), getEquipmentQuantity(i), getUnitNumber()));
              }
            }
          }
          catch (Exception e){
            LOGGER_GAME.log(Level.WARNING, "Error loading equipment", e);
          }
        }
      }
      return returnMe;
    }

    public float getTotalProficiency(int index){
      // USE THIS METHOD FOR BATTLES
      // index is index of proficiency in data.json
      return getRawProficiency(index) * (1+getProficiencyBonusMultiplier(index));
    }

    public float getRawBonusProficiency(int index){
      // For getting bonus amount
      return getProficiencyBonusMultiplier(index) * getRawProficiency(index);
    }

    public float[] getRawBonusProficiencies(){
      float [] r = new float[getRawProficiencies().length];
      for (int i = 0; i < r.length; i++){
        r[i] = getRawBonusProficiency(i);
      }
      return r;
    }

    public boolean capped() {
      return unitNumber == unitCap;
    }

    public int getSightUnitsRadius() {
      return ceil(gameData.getJSONObject("game options").getInt("sight points") * getEffectivenessMultiplier("sight"));
    }
  }


  class Battle extends Party {
    Party attacker;
    Party defender;

    Battle(Party attacker, Party defender, String id) {
      super(-1, attacker.getUnitNumber()+defender.getUnitNumber(), JSONIndex(gameData.getJSONArray("tasks"), "Battle"), 0, id);
      this.attacker = attacker;
      attacker.strength = 2;
      this.defender = defender;
    }

    public int containsPartyFromPlayer(int p) {
      if (attacker.player == p) {
        return 1;
      } else if (defender.player == p) {
        return 2;
      }
      return 0;
    }

    public boolean isTurn(int turn) {
      return true;
    }

    public int getMovementPoints(int turn) {
      if (turn==attacker.player) {
        return attacker.getMovementPoints();
      } else {
        return defender.getMovementPoints();
      }
    }

    public void setUnitNumber(int turn, int newUnitNumber) {
      if (turn==attacker.player) {
        attacker.setUnitNumber(newUnitNumber);
      } else {
        defender.setUnitNumber(newUnitNumber);
      }
    }

    public int getUnitNumber(int turn) {
      if (turn==attacker.player) {
        return attacker.getUnitNumber();
      } else {
        return defender.getUnitNumber();
      }
    }

    public int changeUnitNumber(int turn, int changeInUnitNumber) {
      if (turn==this.attacker.player) {
        int overflow = max(0, changeInUnitNumber+attacker.getUnitNumber()-jsManager.loadIntSetting("party size"));
        this.attacker.setUnitNumber(attacker.getUnitNumber()+changeInUnitNumber);
        return overflow;
      } else {
        int overflow = max(0, changeInUnitNumber+defender.getUnitNumber()-jsManager.loadIntSetting("party size"));
        this.defender.setUnitNumber(defender.getUnitNumber()+changeInUnitNumber);
        return overflow;
      }
    }

    public Party doBattle() {
      try {
        int changeInParty1 = getBattleUnitChange(attacker, defender);
        int changeInParty2 = getBattleUnitChange(defender, attacker);
        attacker.strength = 1;
        defender.strength = 1;
        int newParty1Size = attacker.getUnitNumber()+changeInParty1;
        int newParty2Size = defender.getUnitNumber()+changeInParty2;
        int endDifference = newParty1Size-newParty2Size;
        attacker.setUnitNumber(newParty1Size);
        defender.setUnitNumber(newParty2Size);
        if (attacker.getUnitNumber()==0) {
          if (defender.getUnitNumber()==0) {
            if (endDifference==0) {
              return null;
            } else if (endDifference>0) {
              attacker.setUnitNumber(endDifference);
              attacker.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
              return attacker;
            } else {
              defender.setUnitNumber(-endDifference);
              defender.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
              return defender;
            }
          } else {
            defender.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            return defender;
          }
        }

        if (defender.getUnitNumber()==0) {
          attacker.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          return attacker;
        } else {
          return this;
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error doing battle", e);
        throw e;
      }
    }

    public Battle clone() {
      Battle newParty = new Battle(this.attacker.clone(), this.defender.clone(), id);
      return newParty;
    }

    public int mergeEntireFrom(Party other, int moveCost, Player player) {
      // Note: will need to remove other party
      LOGGER_GAME.fine(String.format("Merging entire party from id:%s into battle with id:%s", other.id, this.id));
      return mergeFrom(other, other.getUnitNumber(), moveCost, player);
    }

    public int mergeFrom(Party other, int unitsTransfered, int moveCost, Player player) {
      // Take units from other party into this party and merge attributes, weighted by unit number
      LOGGER_GAME.fine(String.format("Merging %d units from party with id:%s into battle with id:%s", unitsTransfered, other.id, this.id));
      // THIS NEEDS TO BE CHANGED FOR DIPLOMACY
      if (attacker.player == other.player) {
        return attacker.mergeFrom(other, unitsTransfered, moveCost, player);
      } else if (defender.player == other.player) {
        return defender.mergeFrom(other, unitsTransfered, moveCost, player);
      } else {
        return unitsTransfered;
      }
      //
    }
  }

  class Siege extends Battle {
    Building defence;
    Siege (Party attacker, Building defence, Party garrison, String id) {
      super(attacker, garrison, id);
      this.defence = defence;
      this.player = -2;
    }

    public Party doBattle() {
      try {
        int changeInParty1 = getSiegeUnitChange(attacker, defender, defence);
        int changeInParty2 = getSiegeUnitChange(defender, attacker, defence);
        attacker.strength = 1;
        defender.strength = 1;
        int newParty1Size = attacker.getUnitNumber()+changeInParty1;
        int newParty2Size = defender.getUnitNumber()+changeInParty2;
        int endDifference = newParty1Size-newParty2Size;
        attacker.setUnitNumber(newParty1Size);
        defender.setUnitNumber(newParty2Size);
        if (attacker.getUnitNumber()==0) {
          if (defender.getUnitNumber()==0) {
            if (endDifference==0) {
              return null;
            } else if (endDifference>0) {
              attacker.setUnitNumber(endDifference);
              attacker.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
              return attacker;
            } else {
              defender.setUnitNumber(-endDifference);
              defender.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
              return defender;
            }
          } else {
            defender.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
            return defender;
          }
        }
        if (defender.getUnitNumber()==0) {
          attacker.changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
          return attacker;
        } else {
          return this;
        }
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error doing Siege", e);
        throw e;
      }
    }
  }

  // THIS NEEDS TO BE CHANGED FOR DIPLOMACY
  public int getBattleUnitChange(Party p1, Party p2) {
    float damageRating = p2.strength * p2.getEffectivenessMultiplier("melee attack") /
    (p1.strength * p1.getEffectivenessMultiplier("defence"));
    return floor(-0.2f * (p2.getUnitNumber() + pow(p2.getUnitNumber(), 2) / p1.getUnitNumber()) * random(0.75f, 1.5f) * damageRating);
  }

  // THIS NEEDS TO BE CHANGED FOR DIPLOMACY
  public int getSiegeUnitChange(Party p1, Party p2, Building defence) {
    float damageRating = p2.strength * p2.getEffectivenessMultiplier("melee attack") * (1 + PApplet.parseInt(defence.getPlayerID() == p2.player) * gameData.getJSONArray("buildings").getJSONObject(defence.type).getFloat("defence"))/
    (p1.strength * p1.getEffectivenessMultiplier("defence") * (1 + PApplet.parseInt(defence.getPlayerID() == p1.player) * defence.getHealth()));
    return floor(-0.2f * (p2.getUnitNumber() + pow(p2.getUnitNumber(), 2) / p1.getUnitNumber()) * random(0.75f, 1.5f) * damageRating);
  }

  public boolean playerExists(Player[] players, String name) {
    for (Player p: players) {
      if (p.name.trim().equals(name.trim())){
        return true;
      }
    }
    return false;
  }

  public Player getPlayer(Player[] players, String name){
    for (Player p: players) {
      if (p.name.trim().equals(name.trim())){
        return p;
      }
    }
    LOGGER_MAIN.severe(String.format("Tried to find player: %s but this player was not found. Returned first player, this will likely cause problems", name));
    return players[0];
  }


  class Player {
    private int id;
    float cameraCellX, cameraCellY, blockSize;
    float[] resources;
    int cellX, cellY, colour;
    boolean cellSelected = false;
    String name;
    boolean isAlive = true;
    PlayerController playerController;
    int controllerType;  // 0 for local, 1 for bandits
    Cell[][] visibleCells;

    // Resources: food wood metal energy concrete cable spaceship_parts ore people
    Player(float x, float y, float blockSize, float[] resources, int colour, String name, int controllerType, int id) {
      this.cameraCellX = x;
      this.cameraCellY = y;
      this.blockSize = blockSize;
      this.resources = resources;
      this.colour = colour;
      this.name = name;
      this.id = id;

      this.visibleCells = new Cell[jsManager.loadIntSetting("map size")][jsManager.loadIntSetting("map size")];
      this.controllerType = controllerType;
      switch(controllerType){
        case 1:
          playerController = new BanditController(id, jsManager.loadIntSetting("map size"), jsManager.loadIntSetting("map size"));
          break;
        default:
          playerController = null;
          break;
      }
    }

    public Node[][] sightDijkstra(int x, int y, Party[][] parties, int[][] terrain) {
      int w = visibleCells[0].length;
      int h = visibleCells.length;
      int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
      Node currentHeadNode;
      Node[][] nodes = new Node[h][w];
      nodes[y][x] = new Node(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("sight bonus"), false, x, y, x, y);
      PriorityQueue<Node> curMinNodes = new PriorityQueue<Node>(new NodeComparator());
      curMinNodes.add(nodes[y][x]);
      while (curMinNodes.size() > 0) {
        currentHeadNode = curMinNodes.poll();
        currentHeadNode.fixed = true;

        for (int[] mv : mvs) {
          int nx = currentHeadNode.x+mv[0];
          int ny = currentHeadNode.y+mv[1];
          if (0 <= nx && nx < w && 0 <= ny && ny < h) {
            int newCost = sightCost(nx, ny, currentHeadNode.x, currentHeadNode.y, terrain);
            int prevCost = currentHeadNode.cost;
            if (newCost != -1){ // Check that the cost is valid
              int totalNewCost = prevCost+newCost;
              if (totalNewCost < parties[y][x].getSightUnitsRadius()) {
                if (nodes[ny][nx] == null) {
                  nodes[ny][nx] = new Node(totalNewCost, false, currentHeadNode.x, currentHeadNode.y, nx, ny);
                  curMinNodes.add(nodes[ny][nx]);
                } else if (!nodes[ny][nx].fixed) {
                  if (totalNewCost < nodes[ny][nx].cost) { // Updating existing node
                    nodes[ny][nx].cost = min(nodes[ny][nx].cost, totalNewCost);
                    nodes[ny][nx].setPrev(currentHeadNode.x, currentHeadNode.y);
                    curMinNodes.remove(nodes[ny][nx]);
                    curMinNodes.add(nodes[ny][nx]);
                  }
                }
              }
            }
          }
        }
      }
      return nodes;
    }

    public boolean[][] generateFogMap(Party[][] parties, int[][] terrain) {
      int w = parties[0].length;
      int h = parties.length;
      boolean[][] fogMap = new boolean[h][w];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          if (parties[y][x] != null && (parties[y][x].player == id || parties[y][x].containsPartyFromPlayer(id) > 0) && parties[y][x].getUnitNumber() > 0) {
            Node[][] nodes = sightDijkstra(x, y, parties, terrain);
            for (int y1 = max(0, y - parties[y][x].getSightUnitsRadius()); y1 < min(h, y + parties[y][x].getSightUnitsRadius()+1); y1++) {
              for (int x1 = max(0, x - parties[y][x].getSightUnitsRadius()); x1 < min(w, x + parties[y][x].getSightUnitsRadius()+1); x1++) {
                if (nodes[y1][x1] != null && nodes[y1][x1].cost <= parties[y][x].getSightUnitsRadius()) {
                  fogMap[y1][x1] = true;
                }
              }
            }
          }
        }
      }

      return fogMap;
    }

    public void updateVisibleCells(int[][] terrain, Building[][] buildings, Party[][] parties, boolean[][] seenCells){
      /*
      Run after every event for this player, and it updates the visibleCells taking into account fog of war.
      Cells that have not been discovered yet will be null, and cells that are in active sight will be updated with the latest infomation.
      */
      LOGGER_MAIN.fine("Updating visible cells for player " + name);
      boolean[][] fogMap = generateFogMap(parties, terrain);

      for (int y = 0; y < visibleCells.length; y++) {
        for (int x = 0; x < visibleCells[0].length; x++) {
          if (visibleCells[y][x] == null && (seenCells == null || !seenCells[y][x])) {
            if (fogMap[y][x]) {
              visibleCells[y][x] = new Cell(terrain[y][x], buildings[y][x], parties[y][x]);
              visibleCells[y][x].setActiveSight(true);
            }
          } else {
            if (visibleCells[y][x] == null) {
              visibleCells[y][x] = new Cell(terrain[y][x], buildings[y][x], null);
            } else {
              visibleCells[y][x].setTerrain(terrain[y][x]);
              visibleCells[y][x].setBuilding(buildings[y][x]);
            }
            visibleCells[y][x].setActiveSight(fogMap[y][x]);
            if (visibleCells[y][x].getActiveSight()) {
              visibleCells[y][x].setParty(parties[y][x]);
            } else {
              visibleCells[y][x].setParty(null);
            }
          }
        }
      }
    }

    public void updateVisibleCells(int[][] terrain, Building[][] buildings, Party[][] parties){
      updateVisibleCells(terrain, buildings, parties, null);
    }

    public void saveSettings(float x, float y, float blockSize, int cellX, int cellY, boolean cellSelected) {
      this.cameraCellX = x;
      this.cameraCellY = y;
      this.blockSize = blockSize;
      this.cellX = cellX;
      this.cellY = cellY;
      this.cellSelected = cellSelected;
    }
    public void loadSettings(Game g, Map m) {
      LOGGER_MAIN.fine("Loading player camera settings");
      m.loadSettings(cameraCellX, cameraCellY, blockSize);
      if (cellSelected) {
        g.selectCell((int)this.cellX, (int)this.cellY, false);
      } else {
        g.deselectCell();
      }
    }

    public GameEvent generateNextEvent(){
      // This method will be run continuously until it returns an end turn event
      return playerController.generateNextEvent(visibleCells, resources);
    }
  }


  interface PlayerController {
    public GameEvent generateNextEvent(Cell[][] visibleCells, float resources[]);
  }


  class Cell {
    private int terrain;
    private Building building;
    private Party party;
    private boolean activeSight;

    Cell(int terrain, Building building, Party party){
      this.terrain = terrain;
      this.building = building;
      this.party = party;
      this.activeSight = false; // Needs to be updated later
    }

    public int getTerrain(){
      return terrain;
    }
    public Building getBuilding(){
      return building;
    }
    public Party getParty(){
      return party;
    }
    public boolean getActiveSight(){
      return activeSight;
    }
    public void setTerrain(int terrain){
      this.terrain = terrain;
    }
    public void setBuilding(Building building){
      this.building = building;
    }
    public void setParty(Party party){
      this.party = party;
    }
    public void setActiveSight(boolean activeSight){
      this.activeSight = activeSight;
    }
  }



  class Node {
    int cost;
    boolean fixed;
    int prevX = -1, prevY = -1;
    int x, y;

    Node(int cost, boolean fixed, int prevX, int prevY) {
      this.fixed = fixed;
      this.cost = cost;
      this.prevX = prevX;
      this.prevY = prevY;
    }

    Node(int cost, boolean fixed, int prevX, int prevY, int x, int y) {
      this.fixed = fixed;
      this.cost = cost;
      this.prevX = prevX;
      this.prevY = prevY;
      this.x = x;
      this.y = y;
    }
    public void setPrev(int prevX, int prevY) {
      this.prevX = prevX;
      this.prevY = prevY;
    }
  }



  class MapSave {
    float[] heightMap;
    int mapWidth, mapHeight;
    int[][] terrain;
    Party[][] parties;
    Building[][] buildings;
    int startTurn;
    int startPlayer;
    Player[] players;
    MapSave(float[] heightMap, int mapWidth, int mapHeight, int[][] terrain, Party[][] parties, Building[][] buildings, int startTurn, int startPlayer, Player[] players) {
      this.heightMap = heightMap;
      this.mapWidth = mapWidth;
      this.mapHeight = mapHeight;
      this.terrain = terrain;
      this.parties = parties;
      this.buildings = buildings;
      this.startTurn = startTurn;
      this.startPlayer = startPlayer;
      this.players = players;
    }
  }

  class BattleEstimateManager {
    int currentWins = 0;
    int currentTrials = 0;
    int attackerX;
    int attackerY;
    int defenderX;
    int defenderY;
    int attackerUnits;
    boolean cached = false;
    Party[][] parties;
    BattleEstimateManager(Party[][] parties) {
      this.parties = parties;
    }
    public BigDecimal getEstimate(int x1, int y1, int x2, int y2, int units) {
      try {
        if (parties[y2][x2] == null) {
          LOGGER_MAIN.warning("Invalid player location");
        }
        Party tempAttacker = parties[y1][x1].clone();
        tempAttacker.setUnitNumber(units);
        if (cached&&attackerX==x1&&attackerY==y1&&defenderX==x2&&defenderY==y2&&attackerUnits==units) {
          int TRIALS = 1000;
          for (int i = 0; i<TRIALS; i++) {
            currentWins+=runTrial(tempAttacker, parties[y2][x2]);
          }
          currentTrials+=TRIALS;
        } else {
          cached = true;
          currentWins = 0;
          currentTrials = 0;
          attackerX = x1;
          attackerY = y1;
          defenderX = x2;
          defenderY = y2;
          attackerUnits = units;
          int TRIALS = 10000;
          for (int i = 0; i<TRIALS; i++) {
            currentWins+=runTrial(tempAttacker, parties[y2][x2]);
          }
          currentTrials = TRIALS;
        }
        BigDecimal chance = new BigDecimal(""+currentWins).multiply(new BigDecimal(100)).divide(new BigDecimal(""+currentTrials), 1, BigDecimal.ROUND_HALF_UP);
        return chance;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting estimate for battle between party at (%s, %s) and (%s, %s)", x1, y1, x2, y2));
        throw e;
      }
    }

    // THIS NEEDS TO BE CHANGED FOR DIPLOMACY
    //
    public void refresh() {
      cached = false;
    }
  }

  public int runTrial(Party attacker, Party defender) {
    try {
      Battle battle;
      Party clone1;
      Party clone2;
      if (defender instanceof Battle) {
        battle = (Battle) defender.clone();
        battle.changeUnitNumber(attacker.player, attacker.getUnitNumber());
        if (battle.attacker.player==attacker.player) {
          clone1 = battle.attacker;
          clone2 = battle.defender;
        } else {
          clone1 = battle.defender;
          clone2 = battle.attacker;
        }
      } else {
        clone1 = attacker.clone();
        clone2 = defender.clone();
        battle = new Battle(clone1, clone2, ".battle");
      }
      while (clone1.getUnitNumber()>0&&clone2.getUnitNumber()>0) {
        battle.doBattle();
      }
      if (clone1.getUnitNumber()>0) {
        return 1;
      } else {
        return 0;
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error running battle trial", e);
      throw e;
    }
  }

  public int terrainIndex(String terrain) {
    try {
      int k = JSONIndex(gameData.getJSONArray("terrain"), terrain);
      if (k>=0) {
        return k;
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
  public int buildingIndex(String building) {
    try {
      int k = JSONIndex(gameData.getJSONArray("buildings"), building);
      if (k>=0) {
        return k;
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
    int partyManagementColour;
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
    int[] playerColours;
    boolean bombarding = false;
    int playerCount;

    Game() {
      try {
        LOGGER_MAIN.fine("initializing game");
        gameUICanvas = createGraphics(width, height, P2D);

        // THIS NEEDS TO BE CHANGED WHEN ADDING PLAYER INPUT SELECTOR
        players = new Player[4];
        //

        initialiseResources();
        initialiseTasks();
        initialiseBuildings();
        totals = new float[resourceNames.length];

        addElement("2dmap", new Map2D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight, players));
        addElement("3dmap", new Map3D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
        addElement("notification manager", new NotificationManager(0, 0, 0, 0, color(100), color(255), 8, turn, players.length));

        notificationManager = (NotificationManager)getElement("notification manager", "default");

        addPanel("land management", 0, 0, width, height, false, true, color(50, 200, 50), color(0));
        addPanel("party management", 0, 0, width, height, false, true, color(110, 110, 255), color(0));
        addPanel("bottom bar", 0, height-70, width, 70, true, true, color(150), color(50));
        addPanel("resource management", width/4, height/4, width/2, height/2, false, true, color(200), color(0));
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
        addElement("stock up button", new Button(bezel, bezel*3, 100, 30, color(150), color(50), color(0), 10, CENTER, "Stock Up"), "party management");
        addElement("auto stock up toggle", new ToggleButton(bezel, bezel*3, 100, 30, color(100), color(0), false, "Auto Stock Up"), "party management");
        addElement("unit cap incrementer", new IncrementElement(bezel, bezel*3, 100, 30, jsManager.loadIntSetting("party size"), 0, jsManager.loadIntSetting("party size"), 1, 5), "party management");

        addElement("proficiency summary", new ProficiencySummary(bezel, bezel*5+30+200, 220, 100), "party management");
        addElement("proficiencies", new Text(0, 0, 10, "Proficiencies", color(0), LEFT), "party management");
        addElement("equipment manager", new EquipmentManager(0, 0, 1), "party management");
        addElement("bombardment button", new BombardButton(bezel+100, bezel*3, 32, color(150)), "party management");

        DropDown partyTrainingFocusDropdown = new DropDown(0, 0, 1, 1, color(150), "Training Focus", "strings", 8);
        partyTrainingFocusDropdown.setOptions(jsManager.getProficiencies());
        addElement("party training focus", partyTrainingFocusDropdown, "party management");

        addElement("end turn", new Button(bezel, bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"), "bottom bar");
        addElement("idle party finder", new Button(bezel*2+buttonW, bezel, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Idle Party"), "bottom bar");
        addElement("resource summary", new ResourceSummary(0, 0, 70, resourceNames, startingResources, totals), "bottom bar");
        int resSummaryX = width-((ResourceSummary)(getElement("resource summary", "bottom bar"))).totalWidth();
        addElement("resource detailed", new Button(resSummaryX-50, bezel, 30, 20, color(150), color(50), color(0), 13, CENTER, "^"), "bottom bar");
        addElement("resource expander", new Button(resSummaryX-50, 2*bezel+20, 30, 20, color(150), color(50), color(0), 10, CENTER, "<"), "bottom bar");

        addElement("turn number", new TextBox(bezel*3+buttonW*2, bezel, -1, buttonH, 14, "Turn 0", 0, 0), "bottom bar");
        addElement("2d 3d toggle", new ToggleButton(bezel*4+buttonW*3, bezel*2, buttonW/2, buttonH-bezel, color(100), color(0), jsManager.loadBooleanSetting("map is 3d"), "3D View"), "bottom bar");
        addElement("task icons toggle", new ToggleButton(round(bezel*5+buttonW*3.5f), bezel*2, buttonW/2, buttonH-bezel, color(100), color(0), true, "Task Icons"), "bottom bar");
        addElement("unit number bars toggle", new ToggleButton(bezel*6+buttonW*4, bezel*2, buttonW/2, buttonH-bezel, color(100), color(0), true, "Unit Bars"), "bottom bar");
        addElement("console", new Console(0, 0, width, height/2, 10), "console");
        addElement("resource management table", new ResourceManagementTable(bezel, bezel*2+30, width/2-bezel*2, height/2), "resource management");
        addElement("resources pages button", new HorizontalOptionsButton(bezel, bezel, 100, 30, color(150), 10, new String[]{"Resources", "Equipment"}), "resource management");


        prevIdle = new ArrayList<Integer[]>();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error initializing game", e);
        throw e;
      }
    }

    public void initialiseBuildings() {
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

    public void initialiseTasks() {
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

    public void initialiseResources() {
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

    public void leaveState() {
      map.clearShape();
    }

    public JSONArray taskInitialCost(int type) {
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

    public int taskTurns(String task) {
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

    public Action taskAction(int task) {
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

    public float[] JSONToCost(JSONArray ja) {
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

    public String terrainString(int terrainI) {
      try {
        return gameData.getJSONArray("terrain").getJSONObject(terrainI).getString("id");
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for terrain string", e);
        return null;
      }
    }

    public String buildingString(int buildingI) {
      try {
        if (gameData.getJSONArray("buildings").isNull(buildingI)) {
          LOGGER_MAIN.warning("invalid building string "+(buildingI));
          return null;
        }
        return gameData.getJSONArray("buildings").getJSONObject(buildingI).getString("id");
      }
      catch (NullPointerException e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for building string", e);
        return null;
      }
    }

    public String taskString(int task) {
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

    public float[] buildingCost(int actionType) {
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

    public int getBombardmentDamage(Party attacker, Party defender) {
      return floor(attacker.getUnitNumber() * attacker.getEffectivenessMultiplier("ranged attack") /
        (defender.getEffectivenessMultiplier("defence") * 3));
    }

    public boolean postEvent(GameEvent event) {
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
          if (players[turn].visibleCells[selectedCellY][selectedCellX].getParty() == null){
            LOGGER_MAIN.warning(String.format("invalid movement no party on cell: (%d, %d)", x, y));
            valid = false;
          }

          //Node[][] nodes = djk(selectedCellX, selectedCellY);
          Node[][] nodes = LimitedKnowledgeDijkstra(selectedCellX, selectedCellY, mapWidth, mapHeight, players[turn].visibleCells, 1000);

          if (canMove(selectedCellX, selectedCellY)) {
            int sliderVal = m.num;
            if (sliderVal > 0 && parties[selectedCellY][selectedCellX].getUnitNumber() >= 1 && parties[selectedCellY][selectedCellX].getTask() != JSONIndex(gameData.getJSONArray("tasks"), "Battle")) {
              if (players[turn].controllerType != 1) {
                map.updateMoveNodes(nodes, players);
              }
              moving = true;
              String newPartyName;
              if (sliderVal >= parties[selectedCellY][selectedCellX].getUnitNumber()) {
                newPartyName = parties[selectedCellY][selectedCellX].id;
              } else {
                newPartyName = nextRollingId(players[turn].name);
              }
              LOGGER_GAME.finer(String.format("Splitting party (id:%s) from party (id:%s) at (%d, %d). Number = %d.", newPartyName, parties[selectedCellY][selectedCellX].id, selectedCellX, selectedCellY, sliderVal));
              splittedParty = parties[selectedCellY][selectedCellX].splitParty(sliderVal, newPartyName);
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
              parties[y][x].mergeEntireFrom(splittedParty, 0, players[parties[y][x].player]);
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
          if (parties[selectedCellY][selectedCellX] != null && parties[selectedCellY][selectedCellX].getUnitNumber() <= 0) {
            parties[selectedCellY][selectedCellX] = null;
          }
        } else if (event instanceof Bombard) {
          Bombard bombardEvent = (Bombard)event;
          int x1 = bombardEvent.fromX;
          int y1 = bombardEvent.fromY;
          int x2 = bombardEvent.toX;
          int y2 = bombardEvent.toY;
          if (parties[y1][x1] != null && parties[y2][x2] != null) {
            Party attacker = parties[y1][x1];
            Party defender = parties[y2][x2];
            if (attacker.equipment[1] != -1) {
              JSONObject weapon = gameData.getJSONArray("equipment").getJSONObject(1).getJSONArray("types").getJSONObject(attacker.equipment[1]);
              if (attacker.player == turn && defender.player != turn && weapon.hasKey("range")) {
                int range = weapon.getInt("range");
                if (dist(x1, y1, x2, y2) <= range) {
                  int damage = getBombardmentDamage(attacker, defender);
                  defender.changeUnitNumber(-damage);
                  handlePartyExcessResources(x2, y2);
                  if (defender.getUnitNumber() == 0) {
                    parties[y2][x2] = null;
                  }
                  attacker.setMovementPoints(0);
                  updateBombardment();

                  // Train both parties as result of bombardment
                  attacker.trainParty("ranged attack", "ranged bombardment attack");
                  defender.trainParty("defence", "ranged bombardment defence");

                  LOGGER_GAME.fine(String.format("Party %s bombarding party %s, eliminating %d units", attacker.id, defender.id, damage));
                } else {
                  LOGGER_GAME.fine(String.format("Party %s attempted and failed to bombard party %s, as it was not in range", attacker.id, defender.id));
                }
              } else {
                LOGGER_GAME.fine(String.format("Party %s attempted and failed to bombard party %s, as it did not have the correct weapon or attacked itself or was of the wrong player", attacker.id, defender.id));
              }
            } else {
              LOGGER_GAME.fine(String.format("Party %s attempted and failed to bombard party %s, as it did not have a weapon", attacker.id, defender.id));
            }
          } else {
            LOGGER_GAME.fine("A party attempted and failed to bombard a party, as at least one didn't exist");
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
            parties[selectedCellY][selectedCellX].setMovementPoints(min(parties[selectedCellY][selectedCellX].getMovementPoints()+jo.getInt("movement points"), parties[selectedCellY][selectedCellX].getMaxMovementPoints()));
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
          if (selectedEquipmentType != -1) {
            updatePartyManagementProficiencies();
          }
        } else if (event instanceof ChangePartyTrainingFocus) {
          int newFocus = ((ChangePartyTrainingFocus)event).newFocus;
          LOGGER_GAME.fine(String.format("Changing party focus for cell (%d, %d) id:%s to '%s'", selectedCellX, selectedCellY, parties[selectedCellY][selectedCellX].getID(), newFocus));
          parties[selectedCellY][selectedCellX].setTrainingFocus(newFocus);
        } else if (event instanceof ChangeEquipment) {
          int newResID=-1, oldResID=-1;

          int equipmentClass = ((ChangeEquipment)event).equipmentClass;
          int newEquipmentType = ((ChangeEquipment)event).newEquipmentType;

          LOGGER_GAME.fine(String.format("Changing equipment type for cell (%d, %d) id:%s class:'%d' new equipment index:'%d'", selectedCellX, selectedCellY, parties[selectedCellY][selectedCellX].getID(), equipmentClass, newEquipmentType));

          if (equipmentClass == -1) {
            LOGGER_GAME.warning("No equipment class selected for change equipment event");
          }

          if (newEquipmentType != -1) {
            newResID = jsManager.getResIndex(jsManager.getEquipmentTypeID(equipmentClass, newEquipmentType));
          }
          if (parties[selectedCellY][selectedCellX].getEquipment(equipmentClass) != -1) {
            oldResID = jsManager.getResIndex(jsManager.getEquipmentTypeID(equipmentClass, parties[selectedCellY][selectedCellX].getEquipment(equipmentClass)));
          }

          try {

            //If new type is 'other class blocking', recycle any equipment in blocked classes
            String[] otherBlocking;
            if (equipmentClass != -1 && newEquipmentType != -1) {
              otherBlocking = jsManager.getOtherClassBlocking(equipmentClass, newEquipmentType);
            } else {
              otherBlocking = null;
            }
            if (otherBlocking != null) {
              for (int i=0; i < otherBlocking.length; i ++) {
                int classIndex = jsManager.getEquipmentClassFromID(otherBlocking[i]);
                int otherResID=-1;
                if (parties[selectedCellY][selectedCellX].getEquipment(classIndex) != -1) {
                  otherResID = jsManager.getResIndex(jsManager.getEquipmentTypeID(classIndex, parties[selectedCellY][selectedCellX].getEquipment(classIndex)));
                }
                if (otherResID != -1 && parties[selectedCellY][selectedCellX].getEquipment(classIndex) != -1 && isEquipmentCollectionAllowed(selectedCellX, selectedCellY, classIndex, parties[selectedCellY][selectedCellX].getEquipment(classIndex))) {
                  players[turn].resources[otherResID] += parties[selectedCellY][selectedCellX].getEquipmentQuantity(classIndex);
                }
                parties[selectedCellY][selectedCellX].setEquipment(classIndex, -1, 0);  // Set it to empty after
              }
            } else {
              // Recycle equipment if unequipping
              if (oldResID != -1 && parties[selectedCellY][selectedCellX].getEquipment(equipmentClass) != -1 && isEquipmentCollectionAllowed(selectedCellX, selectedCellY, equipmentClass, newEquipmentType)) {
                players[turn].resources[oldResID] += parties[selectedCellY][selectedCellX].getEquipmentQuantity(equipmentClass);
              }
            }

            int quantity;
            if (newResID == -1 || !isEquipmentCollectionAllowed(selectedCellX, selectedCellY, equipmentClass, newEquipmentType)) {
              quantity = 0;
            } else {
              quantity = floor(min(parties[selectedCellY][selectedCellX].getUnitNumber(), players[turn].resources[newResID]));
            }
            parties[selectedCellY][selectedCellX].setEquipment(equipmentClass, newEquipmentType, quantity);  // Change party equipment

            LOGGER_GAME.fine("Quantity of equipment = "+quantity);

            // Subtract equipment resource
            if (newResID != -1) {
              players[turn].resources[newResID] -= quantity;
            }

            ((EquipmentManager)getElement("equipment manager", "party management")).setEquipment(parties[selectedCellY][selectedCellX]);  // Update equipment manager with new equipment

            // Update max movement points
            parties[selectedCellY][selectedCellX].resetMovementPoints();
            updateBombardment();
          }
          catch (ArrayIndexOutOfBoundsException e) {
            LOGGER_MAIN.warning("Index problem with equipment change");
            throw e;
          }
          parties[selectedCellY][selectedCellX].updateMaxMovementPoints();
          updatePartyManagementProficiencies();
        } else if (event instanceof DisbandParty) {
          int x = ((DisbandParty)event).x;
          int y = ((DisbandParty)event).y;
          parties[y][x] = null;
          LOGGER_GAME.fine(String.format("Party at cell: (%d, %d) disbanded", x, y));
          selectCell(x, y, false);  // Remove party management stuff
        } else if (event instanceof StockUpEquipment) {
          LOGGER_GAME.fine("Stocking up equipment");
          boolean anyAdded = false;
          int x = ((StockUpEquipment)event).x;
          int y = ((StockUpEquipment)event).y;
          if (parties[y][x].getMovementPoints() > 0) {
            int resID, addedQuantity;
            for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
              if (parties[y][x].getEquipment(i) != -1) {
                resID = jsManager.getResIndex(jsManager.getEquipmentTypeID(i, parties[y][x].getEquipment(i)));
                addedQuantity = min(parties[y][x].getUnitNumber()-parties[y][x].getEquipmentQuantity(i), floor(players[turn].resources[resID]));
                if (addedQuantity > 0) {
                  anyAdded = true;
                }
                parties[y][x].addEquipmentQuantity(i, addedQuantity);
                players[turn].resources[resID] -= addedQuantity;
                LOGGER_GAME.fine(String.format("Adding %d quantity to equipment class:%d", addedQuantity, i));
              }
            }
            // Only set movement points to 0 if some equipment was topped up
            if (anyAdded) {
              LOGGER_GAME.finer("Party movement points set to 0 because some equipment was topped up");
              parties[y][x].setMovementPoints(0);
            }
            ((Button)getElement("stock up button", "party management")).deactivate();
            updatePartyManagementInterface();
          }
        } else if (event instanceof SetAutoStockUp) {
          int x = ((SetAutoStockUp)event).x;
          int y = ((SetAutoStockUp)event).y;
          boolean newSetting = ((SetAutoStockUp)event).enabled;
          LOGGER_GAME.fine("Changing auto stock up to: "+ newSetting);
          parties[y][x].setAutoStockUp(newSetting);
        } else if (event instanceof UnitCapChange) {
          int x = ((UnitCapChange)event).x;
          int y = ((UnitCapChange)event).y;
          int newCap = ((UnitCapChange)event).newCap;
          if (newCap >= parties[y][x].getUnitNumber()) {
            parties[y][x].setUnitCap(newCap);
            LOGGER_GAME.fine("Changing unit cap to: "+ newCap);
          } else { // Unit cap set below number of units in party
            valid = false;
            LOGGER_GAME.warning(String.format("Unit cap:&d set below number of units in party:&d", newCap, parties[y][x].getUnitNumber()));
          }
        } else {
          LOGGER_GAME.warning("Event type not found");
          valid = false;
        }

        if (valid) {
          LOGGER_GAME.finest("Event is valid, so updating things...");
          updateThingsAfterGameStateChange();
        }
        return valid;
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error posting event", e);
        throw e;
      }
    }

    public void updateThingsAfterGameStateChange() {
      players[turn].updateVisibleCells(terrain, buildings, parties);
      if (players[turn].controllerType == 0){
        map.updateVisibleCells(players[turn].visibleCells);
      }
      if (!changeTurn) {
        updateResourcesSummary();
        updatePartyManagementInterface();

        if (anyIdle(turn)) {
          LOGGER_GAME.finest("There are idle units so highlighting button red");
          ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
        } else {
          LOGGER_GAME.finest("There are no idle units so not highlighting button red");
          ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
        }

        // Any about to finish moving
        boolean partyReadyToFinish = false;
        for (int y=0; y<mapWidth; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (parties[y][x] != null && parties[y][x].player == turn && parties[y][x].pathTurns == 1 && parties[y][x].getMovementPoints() == parties[y][x].getMaxMovementPoints()) {
              partyReadyToFinish = true;
            }
          }
        }

        Button b = (Button)getElement("end turn", "bottom bar");
        if (partyReadyToFinish) {
          b.setText("Advance Units");
        } else {
          b.setText("Next Turn");
        }
      }
    }

    public boolean anyIdle(int turn) {
      for (int x=0; x<mapWidth; x++) {
        for (int y=0; y<mapWidth; y++) {
          if (parties[y][x] != null && parties[y][x].player == turn && isIdle(x, y)) {
            return true;
          }
        }
      }
      return false;
    }

    public void updateSidePanelElementsSizes() {
      // Update the size of elements on the party panel and cell management panel
      sidePanelX = round(width-450*jsManager.loadFloatSetting("gui scale"));
      sidePanelY = bezel;
      sidePanelW = width-sidePanelX-bezel;
      sidePanelH = round(mapElementHeight)-70;
      ((NotificationManager)(getElement("notification manager", "default"))).transform(bezel, bezel, sidePanelW, round(sidePanelH*0.2f)-bezel*2);
      ((Button)getElement("move button", "party management")).transform(bezel, round(13*jsManager.loadFloatSetting("text scale")+bezel), 60, 36);
      ((Button)getElement("bombardment button", "party management")).transform(bezel*2+60, round(13*jsManager.loadFloatSetting("text scale")+bezel), 36, 36);
      ((Slider)getElement("split units", "party management")).transform(round(10*jsManager.loadFloatSetting("gui scale")+bezel), round(bezel*3+2*jsManager.loadFloatSetting("text scale")*13), sidePanelW-2*bezel-round(20*jsManager.loadFloatSetting("gui scale")), round(jsManager.loadFloatSetting("text scale")*2*13));
      ((Button)getElement("stock up button", "party management")).transform(bezel, round(bezel*4+4*jsManager.loadFloatSetting("text scale")*13), 100, 30);
      ((ToggleButton)getElement("auto stock up toggle", "party management")).transform(bezel*2+100, round(bezel*4+4*jsManager.loadFloatSetting("text scale")*13+8*jsManager.loadFloatSetting("text scale")), 100, PApplet.parseInt(30-jsManager.loadFloatSetting("text scale")*8));
      ((IncrementElement)getElement("unit cap incrementer", "party management")).transform(bezel*3+200, round(bezel*4+4*jsManager.loadFloatSetting("text scale")*13), 100, 30);
      ((EquipmentManager)getElement("equipment manager", "party management")).transform(bezel, round(bezel*5+4*jsManager.loadFloatSetting("text scale")*13)+30, sidePanelW-bezel*2);
      int equipmentBoxHeight = PApplet.parseInt(((EquipmentManager)getElement("equipment manager", "party management")).getBoxHeight())+(30+bezel);
      ((TaskManager)getElement("tasks", "party management")).transform(bezel, round(bezel*5+5*jsManager.loadFloatSetting("text scale")*13+equipmentBoxHeight), sidePanelW/2-PApplet.parseInt(1.5f*bezel), 0);
      ((Text)getElement("task text", "party management")).translate(bezel, round(bezel*5+4*jsManager.loadFloatSetting("text scale")*13+equipmentBoxHeight));
      ((ProficiencySummary)getElement("proficiency summary", "party management")).transform(sidePanelW/2+PApplet.parseInt(bezel*0.5f), round(bezel*5+5*jsManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight, sidePanelW/2-PApplet.parseInt(1.5f*bezel), PApplet.parseInt(jsManager.getNumProficiencies()*jsManager.loadFloatSetting("text scale")*13));
      ((Text)getElement("proficiencies", "party management")).translate(sidePanelW/2+PApplet.parseInt(bezel*0.5f), round(bezel*5+4*jsManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight);
      ((Text)getElement("turns remaining", "party management")).translate(100+bezel*2, round(13*jsManager.loadFloatSetting("text scale")*2 + bezel*3));
      ((DropDown)getElement("party training focus", "party management")).transform(sidePanelW/2+PApplet.parseInt(bezel*0.5f), round(bezel*6+5*jsManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight+PApplet.parseInt(jsManager.getNumProficiencies()*jsManager.loadFloatSetting("text scale")*13), sidePanelW/2-PApplet.parseInt(bezel*(1.5f)), PApplet.parseInt(jsManager.loadFloatSetting("text scale")*13));

      float taskRowHeight = ((TaskManager)getElement("tasks", "party management")).getH(new PGraphics());

      float partyManagementHeight = round(bezel*7+6*jsManager.loadFloatSetting("text scale")*13+equipmentBoxHeight) + taskRowHeight*10 + jsManager.loadFloatSetting("gui scale")*bezel*10;
      getPanel("land management").transform(sidePanelX, sidePanelY, sidePanelW, round(sidePanelH*0.15f));
      getPanel("party management").transform(sidePanelX, sidePanelY+round(sidePanelH*0.15f)+bezel, sidePanelW, round(partyManagementHeight)-bezel*3);
      ((Button)getElement("disband button", "party management")).transform(sidePanelW-bezel-80, PApplet.parseInt(partyManagementHeight-bezel*4-30), 80, 30);
      ((HorizontalOptionsButton)getElement("resources pages button", "resource management")).transform(bezel, bezel, PApplet.parseInt(100*jsManager.loadFloatSetting("gui scale")), PApplet.parseInt(30*jsManager.loadFloatSetting("gui scale")));
    }

    public void makeTaskAvailable(int task) {
      ((TaskManager)getElement("tasks", "party management")).makeAvailable(gameData.getJSONArray("tasks").getJSONObject(task).getString("id"));
    }

    public void resetAvailableTasks() {
      ((TaskManager)getElement("tasks", "party management")).resetAvailable();
      ((TaskManager)getElement("tasks", "party management")).resetAvailableButOverBudget();
    }

    public void makeAvailableButOverBudget(int task) {
      ((TaskManager)getElement("tasks", "party management")).makeAvailableButOverBudget(gameData.getJSONArray("tasks").getJSONObject(task).getString("id"));
    }

    public void checkTasks() {
      // Check which tasks should be made available
      try {
        LOGGER_GAME.finer("Starting checking available tasks");
        resetAvailableTasks();
        boolean correctTerrain, correctBuilding, enoughResources, enoughMovementPoints;
        JSONObject js;

        if (parties[selectedCellY][selectedCellX].player == -1) {
          makeTaskAvailable(parties[selectedCellY][selectedCellX].task);
        }

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
        ((TaskManager)getElement("tasks", "party management")).select(gameData.getJSONArray("tasks").getJSONObject(parties[selectedCellY][selectedCellX].getTask()).getString("id"));
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error checking tasks", e);
        throw e;
      }
    }

    public boolean UIHovering() {
      //To avoid doing things while hoving over important stuff
      NotificationManager nm = ((NotificationManager)(getElement("notification manager", "default")));
      return !((!getPanel("party management").mouseOver() || !getPanel("party management").visible) && (!getPanel("land management").mouseOver() || !getPanel("land management").visible) &&
        (!nm.moveOver()||nm.empty()));
    }

    public float getResourceRequirementsAtCell(int x, int y, int resource) {
      float resourceRequirements = 0;
      for (int i = 0; i < tasks.length; i++) {
        if (parties[y][x].getTask() == i) {
          if (resource == jsManager.getResIndex("food") && gameData.getJSONArray("tasks").getJSONObject(i).getString("id").equals("Super Rest") && parties[y][x].capped()) {
            resourceRequirements += taskCosts[jsManager.getTaskIndex("Rest")][resource] * parties[y][x].getUnitNumber();
          } else {
            resourceRequirements += taskCosts[i][resource] * parties[y][x].getUnitNumber();
          }
        }
      }
      return resourceRequirements;
    }

    public float[] getTotalResourceRequirements() {
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

    public float[] getResourceProductivities(float[] totalResourceRequirements) {
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

    public float[] getResourceProductivities() {
      return getResourceProductivities(getTotalResourceRequirements());
    }

    public float getProductivityAtCell(int x, int y, float[] resourceProductivities) {
      float productivity = 1;
      for (int task = 0; task<tasks.length; task++) {
        if (parties[y][x].getTask() == task) {
          for (int resource = 0; resource < numResources; resource++) {
            if (getResourceRequirementsAtCell(x, y, resource) > 0) {
              if (resource == 0 && players[turn].resources[resource] == 0) {
                productivity = min(productivity, resourceProductivities[resource] + 0.5f);
              } else {
                productivity = min(productivity, resourceProductivities[resource]);
              }
            }
          }
        }
      }
      return productivity;
    }

    public float getProductivityAtCell(int x, int y) {
      return getProductivityAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
    }

    public float[] resourceProductionAtCell(int x, int y, float[] resourceProductivities) {
      float [] production = new float[numResources];
      if (parties[y][x] != null) {
        if (parties[y][x].player == turn) {
          float productivity = getProductivityAtCell(x, y, resourceProductivities);
          for (int task = 0; task < tasks.length; task++) {
            if (parties[y][x].getTask()==task) {
              for (int resource = 0; resource < numResources; resource++) {
                if (resource == jsManager.getResIndex("units") && resourceProductivities[jsManager.getResIndex(("food"))] < 1) {
                  production[resource] = 0;
                } else if (resource == jsManager.getResIndex("units")) {
                  production[resource] = min(parties[y][x].getUnitCap() - parties[y][x].getUnitNumber(), taskOutcomes[task][resource] * productivity * (float) parties[y][x].getUnitNumber());
                } else {
                  production[resource] = taskOutcomes[task][resource] * productivity * (float) parties[y][x].getUnitNumber();
                }
              }
            }
          }
        }
      }
      return production;
    }

    public float[] resourceProductionAtCell(int x, int y) {
      return resourceProductionAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
    }

    public float[] getTotalResourceProductions(float[] resourceProductivities) {
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

    public float[] getTotalResourceProductions() {
      return getTotalResourceProductions(getResourceProductivities(getTotalResourceRequirements()));
    }

    public float[] getResourceConsumptionAtCell(int x, int y, float[] resourceProductivities) {
      float [] consumption = new float[numResources];
      if (parties[y][x] != null) {
        if (parties[y][x].player == turn) {
          float productivity = getProductivityAtCell(x, y, resourceProductivities);
          for (int task = 0; task <tasks.length; task++) {
            if (parties[y][x].getTask() == task) {
              for (int resource = 0; resource < numResources; resource++) {
                if (resource == jsManager.getResIndex("units") && resourceProductivities[jsManager.getResIndex(("food"))] < 1) {
                  consumption[resource] += (1-resourceProductivities[jsManager.getResIndex(("food"))]) * (0.01f+taskOutcomes[task][resource]) * parties[y][x].getUnitNumber();
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

    public float[] getResourceConsumptionAtCell(int x, int y) {
      return getResourceConsumptionAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
    }

    public float[] getTotalResourceConsumptions(float[] resourceProductivities) {
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

    public float[] getTotalResourceConsumptions() {
      return getTotalResourceConsumptions(getResourceProductivities(getTotalResourceRequirements()));
    }

    public float[] getTotalResourceChanges(float[] grossResources, float[] costsResources) {
      float[] amount = new float[resourceNames.length];
      for (int res = 0; res < numResources; res++) {
        amount[res] = grossResources[res] - costsResources[res];
      }
      return amount;
    }

    public float[] getResourceChangesAtCell(int x, int y, float[] resourceProductivities) {
      float[] amount = new float[resourceNames.length];
      for (int res = 0; res < numResources; res++) {
        amount[res] = resourceProductionAtCell(x, y, resourceProductivities)[res] - getResourceConsumptionAtCell(x, y, resourceProductivities)[res];
      }
      return amount;
    }

    public float[] getResourceChangesAtCell(int x, int y) {
      return getResourceChangesAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()));
    }

    public byte[] getResourceWarnings() {
      return getResourceWarnings(getResourceProductivities(getTotalResourceRequirements()));
    }

    public byte[] getResourceWarnings(float[] productivities) {
      byte[] warnings = new byte[productivities.length];
      for (int i = 0; i < productivities.length; i++) {
        if (productivities[i] == 0) {
          warnings[i] = 2;
        } else if (productivities[i] < 1) {
          warnings[i] = 1;
        }
      }
      return warnings;
    }

    public void updateResourcesSummary() {
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

    public boolean isEquipmentCollectionAllowed(int x, int y) {
      int[] equipmentTypes = parties[y][x].equipment;
      for (int c = 0; c < equipmentTypes.length; c++) {
        if (isEquipmentCollectionAllowed(x, y, c, equipmentTypes[c])) {
          return true;
        }
      }
      return false;
    }

    public boolean isEquipmentCollectionAllowed(int x, int y, int c, int t) {
      if (buildings[y][x] != null && c != -1 && t != -1) {
        JSONArray sites = gameData.getJSONArray("equipment").getJSONObject(c).getJSONArray("types").getJSONObject(t).getJSONArray("valid collection sites");
        if (sites != null) {
          for (int j = 0; j < sites.size(); j++) {
            if (buildingIndex(sites.getString(j)) == buildings[y][x].type) {
              return true;
            }
          }
        } else {
          // If no valid collection sites specified, then stockup can occur anywhere
          return true;
        }
      }
      return false;
    }

    public void handlePartyExcessResources(int x, int y) {
      Party p = parties[y][x];
      int[][] excessResources = p.removeExcessEquipment();
      for (int i = 0; i < excessResources.length; i++) {
        if (excessResources[i] != null) {
          int type = excessResources[i][0];
          int quantity = excessResources[i][1];
          if (type != -1) {
            if (isEquipmentCollectionAllowed(x, y, i, type)) {
              LOGGER_GAME.fine(String.format("Recovering %d %s from party decreasing in size", quantity, jsManager.getEquipmentTypeID(i, type)));
              players[parties[y][x].player].resources[jsManager.getResIndex(jsManager.getEquipmentTypeID(i, type))] += quantity;
            }
          }
        }
      }
    }

    public void updateResources(float[] resourceProductivities) {
      for (int y = 0; y < mapHeight; y++) {
        for (int x = 0; x < mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x].player == turn) {
              for (int task = 0; task < tasks.length; task++) {
                if (parties[y][x].getTask()==task) {
                  for (int resource = 0; resource < numResources; resource++) {
                    if (resource != jsManager.getResIndex(("units"))) {
                      if (tasks[task].equals("Produce Rocket")) {
                        resource = jsManager.getResIndex(("rocket progress"));
                      }

                      players[turn].resources[resource] += max(getResourceChangesAtCell(x, y, resourceProductivities)[resource], -players[turn].resources[resource]);
                      if (tasks[task].equals("Produce Rocket")) {
                        break;
                      }
                    } else if (resourceProductivities[jsManager.getResIndex(("food"))] < 1 && players[parties[y][x].player].controllerType != 1) {
                      float lost = (1 - resourceProductivities[jsManager.getResIndex(("food"))]) * (0.01f+taskOutcomes[task][resource]) * parties[y][x].getUnitNumber();
                      int totalLost = floor(lost);
                      if (random(1) < lost-floor(lost)) {
                        totalLost++;
                      }
                      parties[y][x].changeUnitNumber(-totalLost);
                      handlePartyExcessResources(x, y);
                      if (parties[y][x].getUnitNumber() == 0) {
                        notificationManager.post("Party Starved", x, y, turnNumber, turn);
                        LOGGER_GAME.info(String.format("Party starved at cell:(%d, %d) player:%s", x, y, turn));
                      } else {
                        notificationManager.post(String.format("Party Starving - %d lost", totalLost), x, y, turnNumber, turn);
                        LOGGER_GAME.fine(String.format("Party Starving - %d lost at  cell: (%d, %d) player:%s", totalLost, x, y, turn));
                      }
                    } else {
                      int prev = parties[y][x].getUnitNumber();
                      float gained = getResourceChangesAtCell(x, y, resourceProductivities)[resource];
                      int totalGained = floor(gained);
                      if (random(1) < gained-floor(gained)) {
                        totalGained++;
                      }
                      parties[y][x].changeUnitNumber(totalGained);
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

    public void autoMoveParties() {
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x].player == turn) {
              moveParty(x, y);
            }
          }
        }
      }
      updateThingsAfterGameStateChange();
    }

    public void processParties() {
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x].player == turn) {
              if (parties[y][x].getAutoStockUp()) {
                postEvent(new StockUpEquipment(x, y));
              }
              if (parties[y][x].getTask() == jsManager.getTaskIndex("Train Party")) {
                parties[y][x].trainParty(jsManager.indexToProficiencyID(parties[y][x].getTrainingFocus()), "training");
              }
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

                    if (parties[y][x] != null) {
                      // Train party when completed building
                      parties[y][x].trainParty("building speed", "constructing building");
                    }

                    if (buildings[y][x].type == buildingIndex("Quarry")) {
                      LOGGER_GAME.fine("Quarry type detected so changing terrain...");
                      //map.setHeightsForCell(x, y, jsManager.loadFloatSetting("water level"));
                      if (terrain[y][x] == terrainIndex("grass")) {
                        terrain[y][x] = terrainIndex("quarry site stone");
                      } else if (terrain[y][x] == terrainIndex("sand")) {
                        terrain[y][x] = terrainIndex("quarry site clay");
                      }

                      map.replaceMapStripWithReloadedStrip(y);
                    }
                  }
                }
                if (action.terrain != null) {
                  if (terrain[y][x] == terrainIndex("forest")) { // Cut down forest
                    LOGGER_GAME.info("Cutting down forest");

                    // This should be changed so that it can be changed in data
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
            }
          }
        }
      }
    }

    public void processBattles() {
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null)
            if (parties[y][x].player==-1) {
              Battle b = (Battle) parties[y][x];
              if (b.attacker.player == turn) {
                int player = b.attacker.player;
                int otherPlayer = b.defender.player;
                parties[y][x] = b.doBattle();
                if (parties[y][x] == null) {
                  LOGGER_GAME.fine(String.format("Battle ended at:(%d, %d) both parties died", x, y));
                  notificationManager.post("Battle Ended. Both parties died", x, y, turnNumber, player);
                  notificationManager.post("Battle Ended. Both parties died", x, y, turnNumber, otherPlayer);
                } else if (parties[y][x].player != -1) {
                  LOGGER_GAME.fine(String.format("Battle ended at:(%d, %d) winner=&s", x, y, str(parties[y][x].player+1)));
                  notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, player);
                  notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
                  parties[y][x].trainParty("melee attack", "winning battle melee");
                  parties[y][x].trainParty("defence", "winning battle defence");
                }
              }
            } else if (parties[y][x].player == -2) {
              Siege s = (Siege) parties[y][x];
              if (s.attacker.player == turn) {
                int player = s.attacker.player;
                int otherPlayer = s.defender.player;
                parties[y][x] = s.doBattle();
                if (parties[y][x] == null) {
                  LOGGER_GAME.fine(String.format("Siege ended at:(%d, %d) both parties died", x, y));
                  notificationManager.post("Siege Ended. Both parties died", x, y, turnNumber, player);
                  notificationManager.post("Siege Ended. Both parties died", x, y, turnNumber, otherPlayer);
                } else if (parties[y][x].player != -2) {
                  LOGGER_GAME.fine(String.format("Siege ended at:(%d, %d) winner=&s", x, y, str(parties[y][x].player+1)));
                  notificationManager.post("Siege Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, player);
                  notificationManager.post("Siege Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
                  parties[y][x].trainParty("melee attack", "winning battle melee");
                  parties[y][x].trainParty("defence", "winning battle defence");
                }
              }
            }
        }
      }
    }

    public void turnChange() {
      try {
        LOGGER_GAME.finer(String.format("Turn changing - current player = %s, next player = %s", turn, (turn+1)%players.length));
        notificationManager.dismissAll();
        autoMoveParties();
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

        turn = (turn + 1)%players.length; // TURN CHANGE

        // If local player turn disable cinematic mode otherwise enable (e.g. bandits/AI turn)
        if (players[turn].controllerType == 0) {
          leaveCinematicMode(); // Leave cinematic mode as player turn
        } else {
          enterCinematicMode(); // Leave cinematic mode as player turn
        }

        players[turn].loadSettings(this, map);
        changeTurn = false;
        TextBox t = ((TextBox)(getElement("turn number", "bottom bar")));
        t.setColour(players[turn].colour);
        t.setText("Turn "+turnNumber);
        updateResourcesSummary();
        notificationManager.turnChange(turn);

        if (turn==0) {
          turnNumber++;
          spawnBandits();
        }

        processBattles();

        if (anyIdle(turn)) {
          LOGGER_GAME.finest("Idle party set to red becuase idle parties found");
          ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
        } else {
          LOGGER_GAME.finest("Idle party set to grey becuase no idle parties found");
          ((Button)getElement("idle party finder", "bottom bar")).setColour(color(150));
        }
        if (checkForPlayerWin()) {
          this.getPanel("end screen").visible = true;
        } else if (!players[turn].isAlive) {
          turnChange();
          return;
        }
        updateThingsAfterGameStateChange();
      }
      catch (Exception e) {
        LOGGER_MAIN.log(Level.SEVERE, "Error changing turn", e);
        throw e;
      }
    }

    public void spawnBandits() {
      int banditCount = 0;
      for (int y = 0; y < mapHeight; y++) {
        for (int x = 0; x < mapWidth; x++) {
          if (parties[y][x] != null && parties[y][x].containsPartyFromPlayer(playerCount) > 0) {
            banditCount++;
          }
        }
      }
      if (random(0, (gameData.getJSONObject("game options").getFloat("bandits per tile")*mapWidth*mapHeight)) > banditCount) {
        ArrayList<int[]> possibleTiles = new ArrayList<int[]>();
        for (int y = 0; y < mapHeight; y++) {
          for (int x = 0; x < mapWidth; x++) {
             if (terrain[y][x] == terrainIndex("water")) {
               continue;
             }
             if (parties[y][x] != null) {
               continue;
             }
             if (buildings[y][x] != null) {
               continue;
             }
             for (Player p: players) {
               if (p.controllerType != 1 && p.visibleCells[y][x] != null && p.visibleCells[y][x].activeSight) {
                 continue;
               }
             }
             possibleTiles.add(new int[]{x, y});
          }
        }
        int chosenTile = floor(random(0, possibleTiles.size()));
        int x = possibleTiles.get(chosenTile)[0];
        int y = possibleTiles.get(chosenTile)[1];
        parties[y][x] = new Party(playerCount, PApplet.parseInt(jsManager.loadIntSetting("party size")/2), 0, gameData.getJSONObject("game options").getInt("movement points"), nextRollingId(players[playerCount].name));
      }
    }

    public boolean checkForPlayerWin() {
      if (winner == -1) {
        boolean[] playersAlive = new boolean[players.length];


        for (int y=0; y<mapHeight; y++) {
          for (int x=0; x<mapWidth; x++) {
            if (parties[y][x] != null) {
              if (parties[y][x].player >= 0) {
                playersAlive[parties[y][x].player] = true;
              } else if (parties[y][x] instanceof Battle) {
                playersAlive[((Battle)parties[y][x]).defender.player] = true;
                playersAlive[((Battle)parties[y][x]).attacker.player] = true;
              }
            }
          }
        }
        int numAlive = 0;
        for (int p=0; p < players.length; p++) {
          players[p].isAlive = playersAlive[p];
          if (playersAlive[p] && p < players.length-1) {
            numAlive ++;
          }
        }
        if (numAlive == 1) {
          for (int p=0; p < players.length; p++) {
            if (playersAlive[p]) {
              winner = p;
              LOGGER_GAME.info(players[p].name+" wins!");
              break;
            }
          }
        } else if (numAlive == 0) {
          LOGGER_GAME.info("No players alive");
        } else {
          return false;
        }
      }
      Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
      winnerMessage.setText(winnerMessage.text.replace("/w", str(winner+1)));
      return true;
    }

    public void drawPanels() {
      LOGGER_MAIN.fine("started drawing game panels");
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
        if (0<=x&&x<mapWidth&&0<=y&&y<mapHeight && parties[y][x] != null && parties[selectedCellY][selectedCellX] != null && parties[y][x].player != parties[selectedCellY][selectedCellX].player && map.mouseOver() && !UIHovering()) {
          BigDecimal chance = battleEstimateManager.getEstimate(selectedCellX, selectedCellY, x, y, splitUnitsNum());
          tooltip.setAttacking(chance);
        } else {
          tooltip.attacking=false;
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

      //Tooltips are going to be added here so don't delete this. From here
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
      // to here

      if (checkForPlayerWin()) {
        this.getPanel("end screen").visible = true;
      }

      // Process AI and bandits turns
      if (players[turn].playerController != null) {  // Local players have null for playerController
        postEvent(players[turn].generateNextEvent());
      }
    }

    public void partyMovementPointsReset() {
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x].player != -1) {
              parties[y][x].resetMovementPoints();
            }
          }
        }
      }
    }

    public void changeTurn() {
      changeTurn = true;
    }

    public boolean sufficientResources(float[] available, float[] required) {
      for (int i=0; i<numResources; i++) {
        if (available[i] < required[i]) {
          return false;
        }
      }
      return true;
    }

    public boolean sufficientResources(float[] available, float[] required, boolean flash) {
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

    public void spendRes(Player player, float[] required) {
      for (int i=0; i<numResources; i++) {
        player.resources[i] -= required[i];
        LOGGER_GAME.fine(String.format("Player spending: %f %s", required[i], resourceNames[i]));
      }
    }

    public void reclaimRes(Player player, float[] required) {
      //reclaim half cost of building
      for (int i=0; i<numResources; i++) {
        player.resources[i] += required[i]/2;
        LOGGER_GAME.fine(String.format("Player reclaiming (half of building cost): %d %s", required[i], resourceNames[i]));
      }
    }

    public int[] newPartyLoc() {
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
    public boolean canMove(int x, int y) {
      float points;
      int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
      Party p = splittedParty == null ? parties[y][x] : splittedParty;

      points = p.getMovementPoints();
      for (int[] n : mvs) {
        if (points >= cost(x+n[0], y+n[1], x, y)) {
          return true;
        }
      }

      return false;
    }

    public boolean inPrevIdle(int x, int y) {
      for (int i=0; i<prevIdle.size(); i++) {
        if (prevIdle.get(i)[0] == x && prevIdle.get(i)[1] == y) {
          return true;
        }
      }
      return false;
    }
    public void clearPrevIdle() {
      prevIdle.clear();
    }
    public boolean isIdle(int x, int y) {
      try {
        return (parties[y][x].task == JSONIndex(gameData.getJSONArray("tasks"), "Rest") && canMove(x, y) && (parties[y][x].path==null||parties[y][x].path!=null&&parties[y][x].path.size()==0));
      }
      catch (IndexOutOfBoundsException e) {
        LOGGER_MAIN.log(Level.WARNING, "Error with indices in new party loc", e);
        return false;
      }
    }

    public int[] findIdle(int player) {
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

    public void saveGame() {
      float blockSize;
      float x=0, y=0;
      if (jsManager.loadBooleanSetting("map is 3d")) {
        blockSize = 0.66f*exp(pow(0.9f, 2.8f*log(map.getZoom()/100000)));
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

    public void elementEvent(ArrayList<Event> events) {
      for (Event event : events) {
        if (event.type == "clicked") {
          if (event.id == "idle party finder") {
            int[] t = findIdle(turn);
            if (t[0] != -1) {
              selectCell(t[0], t[1], false);
              map.targetCell(t[0], t[1], 64);
            }
          } else if (event.id == "end turn") {
            if (((Button)getElement("end turn", "bottom bar")).getText().equals("Advance Units")) {
              autoMoveParties();
              ((Button)getElement("end turn", "bottom bar")).setText("Next Turn");
            } else {
              postEvent(new EndTurn());
            }
          } else if (event.id == "move button") {
            bombarding = false;
            map.disableBombard();
            if (parties[selectedCellY][selectedCellX].player == turn) {
              moving = !moving;
              if (moving) {
                map.updateMoveNodes(LimitedKnowledgeDijkstra(selectedCellX, selectedCellY, mapWidth, mapHeight, players[turn].visibleCells, 20), players);
              } else {
                map.cancelMoveNodes();
              }
            }
          } else if (event.id == "resource expander") {
            ResourceSummary r = ((ResourceSummary)(getElement("resource summary", "bottom bar")));
            Button b1 = ((Button)(getElement("resource expander", "bottom bar")));
            Button b2 = ((Button)(getElement("resource detailed", "bottom bar")));
            r.toggleExpand();
            b1.transform(width-r.totalWidth()-50, bezel*2+25, 30, 25);
            b2.transform(width-r.totalWidth()-50, bezel, 30, 25);
            if (b1.getText() == ">")
              b1.setText("<");
            else
              b1.setText(">");
          } else if (event.id == "resource detailed") {
            getPanel("resource management").setVisible(!getPanel("resource management").visible);
            ResourceManagementTable r = ((ResourceManagementTable)(getElement("resource management table", "resource management")));
            Button b = ((Button)(getElement("resource detailed", "bottom bar")));
            ArrayList<ArrayList<String>> names = new ArrayList<ArrayList<String>>();
            names.add(new ArrayList<String>());
            names.add(new ArrayList<String>());
            ArrayList<ArrayList<Float>> production = new ArrayList<ArrayList<Float>>();
            production.add(new ArrayList<Float>());
            production.add(new ArrayList<Float>());
            ArrayList<ArrayList<Float>> consumption = new ArrayList<ArrayList<Float>>();
            consumption.add(new ArrayList<Float>());
            consumption.add(new ArrayList<Float>());
            ArrayList<ArrayList<Float>> net = new ArrayList<ArrayList<Float>>();
            net.add(new ArrayList<Float>());
            net.add(new ArrayList<Float>());
            ArrayList<ArrayList<Float>> storage = new ArrayList<ArrayList<Float>>();
            storage.add(new ArrayList<Float>());
            storage.add(new ArrayList<Float>());
            float[] totalResourceRequirements = getTotalResourceRequirements();
            float[] resourceProductivities = getResourceProductivities(totalResourceRequirements);

            float[] gross = getTotalResourceProductions(resourceProductivities);
            float[] costs = getTotalResourceConsumptions(resourceProductivities);
            float[] totals = getTotalResourceChanges(gross, costs);
            for (int i = 0; i < players[turn].resources.length; i++) {
              if (players[turn].resources[i] > 0) {
                int page;
                if (jsManager.resourceIsEquipment(i)) {
                  page = 1;
                } else {
                  page = 0;
                }
                names.get(page).add(jsManager.getResString(i));
                production.get(page).add(gross[i]);
                consumption.get(page).add(costs[i]);
                net.get(page).add(totals[i]);
                storage.get(page).add(players[turn].resources[i]);
              }
            }
            r.update(new String[][]{{"Resource", "Making", "Use    ", "Net   ", "Storage"}, {"Equipment type", "Class   ", "Making", "Use    ", "Net   ", "Storage"}}, names, production, consumption, net, storage);

            // This should be changed to use a graphic instead of a character
            if (b.getText() == "^")
              b.setText("v");
            else
              b.setText("^");
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
          } else if (event.id.equals("disband button")) {
            postEvent(new DisbandParty(selectedCellX, selectedCellY));
          } else if (event.id.equals("stock up button")) {
            postEvent(new StockUpEquipment(selectedCellX, selectedCellY));
          } else if (event.id.equals("bombardment button")) {
            bombarding = !bombarding;
            moving = false;
            map.cancelMoveNodes();
            if (bombarding) {
              map.enableBombard(gameData.getJSONArray("equipment").getJSONObject(1).getJSONArray("types").getJSONObject(parties[selectedCellY][selectedCellX].equipment[1]).getInt("range"));
            } else {
              map.disableBombard();
            }
          }
        }
        if (event.type.equals("valueChanged")) {
          if (event.id.equals("tasks")) {
            postEvent(new ChangeTask(selectedCellX, selectedCellY, JSONIndex(gameData.getJSONArray("tasks"), ((TaskManager)getElement("tasks", "party management")).getSelected())));
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
            bombarding = false;
            moving = false;
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
          } else if (event.id.equals("auto stock up toggle")) {
            postEvent(new SetAutoStockUp(selectedCellX, selectedCellY, ((ToggleButton)getElement("auto stock up toggle", "party management")).getState()));
          } else if (event.id.equals("equipment manager")) {
            for (int [] equipmentChange : ((EquipmentManager)getElement("equipment manager", "party management")).getEquipmentToChange()) {
              postEvent(new ChangeEquipment(equipmentChange[0], equipmentChange[1]));
            }
            updatePartyManagementProficiencies();
          } else if (event.id.equals("unit cap incrementer")) {
            postEvent(new UnitCapChange(selectedCellX, selectedCellY, ((IncrementElement)getElement("unit cap incrementer", "party management")).getValue()));
          } else if (event.id.equals("resources pages button")) {
            HorizontalOptionsButton b = ((HorizontalOptionsButton)getElement("resources pages button", "resource management"));
            ((ResourceManagementTable)getElement("resource management table", "resource management")).setPage(b.selected);
          }
        }
        if (event.type.equals("dropped")) {
          if (event.id.equals("equipment manager")) {
            elementToTop("equipment manager", "party management");
            int selectedEquipmentType = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
            if (selectedEquipmentType != -1) {
              updatePartyManagementProficiencies();
            }
          }
        } else if (event.type.equals("notification selected")) {
          int x = notificationManager.lastSelected.x, y = notificationManager.lastSelected.y;
          LOGGER_GAME.fine(String.format("Notification '%s', cell selected: (%d, %d)", notificationManager.lastSelected.name, x, y));
          map.targetCell(x, y, 100);
          selectCell(x, y, false);
        }
      }
    }
    public void deselectCell() {
      tooltip.hide();
      cellSelected = false;
      map.unselectCell();
      getPanel("land management").setVisible(false);
      getPanel("party management").setVisible(false);
      map.cancelMoveNodes();
      moving = false;
      bombarding = false;
      //map.setWidth(round(width-bezel*2));
      ((Text)getElement("turns remaining", "party management")).setText("");
    }

    public void moveParty(int px, int py) {
      moveParty(px, py, false);
    }

    public void moveParty(int px, int py, boolean splitting) {
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

        if (p.target == null || p.path == null) {
          return;
        }

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
            // Train party for movement
            p.trainParty("speed", "moving");

            players[turn].updateVisibleCells(terrain, buildings, parties);

            if (players[turn].controllerType == 0){
              map.updateVisibleCells(players[turn].visibleCells);
            }

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
                int overflow = parties[path.get(node)[1]][path.get(node)[0]].mergeEntireFrom(p, cost, players[turn]);

                LOGGER_GAME.fine(String.format("Parties merged at (%d, %d) from party with id: %s to party with id:%s. Overflow:%d",
                  (int)path.get(node)[0], (int)path.get(node)[1], p.getID(), parties[path.get(node)[1]][path.get(node)[0]].getID(), overflow));

                if (cellFollow) {
                  selectCell((int)path.get(node)[0], (int)path.get(node)[1], false);
                  stillThere = false;
                }

                if (overflow == 0) {
                  if (splitting) {
                    splittedParty = null;
                    splitting = false;
                  } else {
                    parties[py][px] = null;
                  }
                } else if (overflow>0) {
                  parties[path.get(node-1)[1]][path.get(node-1)[0]] = p;
                  LOGGER_GAME.finer(String.format("Setting units in party with id:%s to %d as there was overflow", p.getID(), p.getUnitNumber()));
                }

              } else if (parties[path.get(node)[1]][path.get(node)[0]].player == -1) {
                if (parties[path.get(node)[1]][path.get(node)[0]].containsPartyFromPlayer(turn) > 0) {
                  // reinforce battle
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
                  if (splitting) {
                    parties[path.get(node-1)[1]][path.get(node-1)[0]] = splittedParty;
                    splittedParty = null;
                    splitting = false;
                  }
                  break;
                }
              } else {
                // Attacking
                int x, y;
                x = path.get(node)[0];
                y = path.get(node)[1];
                int otherPlayer = parties[y][x].player;
                if (buildings[y][x] != null && buildings[y][x].isDefenceBuilding()) {
                  LOGGER_GAME.fine(String.format("Siege started. Attacker party id:%s, defender party id:%s. Cell: (%d, %d)", p.getID(), parties[y][x].getID(), x, y));
                  notificationManager.post("Siege Started", x, y, turnNumber, turn);
                  notificationManager.post("Siege Started", x, y, turnNumber, otherPlayer);
                  p.subMovementPoints(cost);
                  parties[y][x] = new Siege(p, buildings[y][x], parties[y][x], ".battle");
                  parties[y][x] = ((Siege)parties[y][x]).doBattle();

                  if (parties[y][x].player != -2) {
                    notificationManager.post("Siege Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, turn);
                    notificationManager.post("Siege Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
                    parties[y][x].trainParty("melee attack", "winning battle melee");
                    parties[y][x].trainParty("defence", "winning battle defence");
                    LOGGER_GAME.fine(String.format("Siege ended at cell: (%d, %d). Units remaining:", x, y, parties[y][x].getUnitNumber()));
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
                } else {
                  LOGGER_GAME.fine(String.format("Battle started. Attacker party id:%s, defender party id:%s. Cell: (%d, %d)", p.getID(), parties[y][x].getID(), x, y));
                  notificationManager.post("Battle Started", x, y, turnNumber, turn);
                  notificationManager.post("Battle Started", x, y, turnNumber, otherPlayer);
                  p.subMovementPoints(cost);
                  parties[y][x] = new Battle(p, parties[y][x], ".battle");
                  parties[y][x] = ((Battle)parties[y][x]).doBattle();

                  if (parties[y][x].player != -1) {
                    notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, turn);
                    notificationManager.post("Battle Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
                    parties[y][x].trainParty("melee attack", "winning battle melee");
                    parties[y][x].trainParty("defence", "winning battle defence");
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
              }
              p.clearPath();
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
    }

    public int getMoveTurns(int startX, int startY, int targetX, int targetY, Node[][] nodes) {
      LOGGER_MAIN.fine(String.format("Getting move turns from (%s, %s) to (%s, %s)", startX, startY, targetX, targetY));
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
          movementPoints = parties[startY][startX].getMaxMovementPoints();
        }
        movementPoints -= cost;
      }
      return turns;
    }

    public int splitUnitsNum() {
      return round(((Slider)getElement("split units", "party management")).getValue());
    }

    public void refreshTooltip() {
      if (players[turn].controllerType != 1) {
        LOGGER_MAIN.fine("refreshing tooltip");
        if (!getPanel("pause screen").visible) {
          TaskManager tasks = ((TaskManager)getElement("tasks", "party management"));
          if (((EquipmentManager)getElement("equipment manager", "party management")).mouseOverTypes() && getPanel("party management").visible) {
            int hoveringType = ((EquipmentManager)getElement("equipment manager", "party management")).hoveringOverType();
            int equipmentClass = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
            tooltip.setEquipment(equipmentClass, hoveringType, players[turn].resources, parties[selectedCellY][selectedCellX], isEquipmentCollectionAllowed(selectedCellX, selectedCellY, equipmentClass, parties[selectedCellY][selectedCellX].getEquipment(equipmentClass)));
            tooltip.show();
          } else if (tasks.moveOver() && getPanel("party management").visible && !tasks.scrolling && !tasks.hovingOverScroll() && tasks.active) {
            tooltip.setTask(((TaskManager)getElement("tasks", "party management")).findMouseOver(), players[turn].resources, parties[selectedCellY][selectedCellX].getMovementPoints());
            tooltip.show();
          } else if (((ProficiencySummary)getElement("proficiency summary", "party management")).mouseOver() && getPanel("party management").visible) {
            tooltip.setProficiencies(((ProficiencySummary)getElement("proficiency summary", "party management")).hoveringOption(), parties[selectedCellY][selectedCellX]);
            tooltip.show();
          } else if (((Text)getElement("turns remaining", "party management")).mouseOver()&& getPanel("party management").visible) {
            tooltip.setTurnsRemaining();
            tooltip.show();
          } else if (((Button)getElement("move button", "party management")).mouseOver()&& getPanel("party management").visible) {
            tooltip.setMoveButton();
            tooltip.show();
          } else if (((Button)getElement("stock up button", "party management")).mouseOver() && getPanel("party management").visible) {
            if (((Button)getElement("stock up button", "party management")).active) {
              tooltip.setStockUpAvailable(parties[selectedCellY][selectedCellX], players[turn].resources);
            } else {
              tooltip.setStockUpUnavailable(parties[selectedCellY][selectedCellX]);
            }
            tooltip.show();
          } else if (map.mouseOver()) {
            Cell[][] visibleCells = players[turn].visibleCells;
            map.doUpdateHoveringScale();
            int mapInterceptX = floor(map.scaleXInv());
            int mapInterceptY = floor(map.scaleYInv());
            if (moving && !UIHovering()) {
              Node [][] nodes = map.getMoveNodes();
              if (selectedCellX == mapInterceptX && selectedCellY == mapInterceptY) {
                tooltip.hide();
                map.cancelPath();
              } else if (mapInterceptX < mapWidth && mapInterceptY<mapHeight && mapInterceptX>=0 && mapInterceptY>=0 && nodes[mapInterceptY][mapInterceptX] != null) {
                if (parties[selectedCellY][selectedCellX] != null) {
                  map.updatePath(getPath(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, map.getMoveNodes()));
                }
                if (visibleCells[mapInterceptY][mapInterceptX] == null || visibleCells[mapInterceptY][mapInterceptX].getParty() == null) {
                  //Moving into empty tile
                  int turns = getMoveTurns(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, nodes);
                  int cost = nodes[mapInterceptY][mapInterceptX].cost;
                  boolean splitting = splitUnitsNum()!=parties[selectedCellY][selectedCellX].getUnitNumber();
                  tooltip.setMoving(turns, splitting, parties[selectedCellY][selectedCellX], splitUnitsNum(), cost, jsManager.loadBooleanSetting("map is 3d"));
                  tooltip.show();
                } else {
                  if (visibleCells[mapInterceptY][mapInterceptX].getParty().player == turn) {
                    //merge parties
                    tooltip.setMerging(visibleCells[mapInterceptY][mapInterceptX].getParty(), visibleCells[selectedCellY][selectedCellX].getParty(), splitUnitsNum());
                    tooltip.show();
                  } else if (visibleCells[mapInterceptY][mapInterceptX].getBuilding() != null && visibleCells[mapInterceptY][mapInterceptX].getBuilding().getDefence() > 0) {
                    //Siege
                    tooltip.setSieging();
                    tooltip.show();
                  } else if (!(visibleCells[mapInterceptY][mapInterceptX].getParty() instanceof Battle) || visibleCells[mapInterceptY][mapInterceptX].getParty().containsPartyFromPlayer(turn) > 0) {
                    //Attack
                    BigDecimal chance = battleEstimateManager.getEstimate(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, splitUnitsNum());
                    tooltip.setAttacking(chance);
                    tooltip.show();
                  } else {
                    tooltip.hide();
                    map.cancelPath();
                  }
                }
              }
            } else {
              map.cancelPath();
              tooltip.hide();
            }

            if (bombarding) {
              if (0<=mapInterceptX&&mapInterceptX<mapWidth&&0<=mapInterceptY&&mapInterceptY<mapHeight && visibleCells[mapInterceptY][mapInterceptX].getParty() != null && visibleCells[mapInterceptY][mapInterceptX].getParty().player != turn) {
                tooltip.setBombarding(getBombardmentDamage(parties[selectedCellY][selectedCellX], parties[mapInterceptY][mapInterceptX]));
                tooltip.show();
              }
            } else if (!moving && 0 < mapInterceptY && mapInterceptY < mapHeight && 0 < mapInterceptX && mapInterceptX < mapWidth && !(parties[mapInterceptY][mapInterceptX] instanceof Battle) && parties[mapInterceptY][mapInterceptX] != null) {
              if (!jsManager.loadBooleanSetting("fog of war") || (players[turn].visibleCells[mapInterceptY][mapInterceptX] != null && players[turn].visibleCells[mapInterceptY][mapInterceptX].party != null)) {
                // Hovering over party
                tooltip.setHoveringParty(parties[mapInterceptY][mapInterceptX]);
                tooltip.show();
              }
            }
          } else {
            map.cancelPath();
            tooltip.hide();
          }
          map.setActive(!UIHovering());
        }
      }
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
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
                bombarding = false;
                map.disableBombard();
                map.updateMoveNodes(LimitedKnowledgeDijkstra(selectedCellX, selectedCellY, mapWidth, mapHeight, players[turn].visibleCells, 20), players);
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
                postEvent(new Move(selectedCellX, selectedCellY, x, y, round(((Slider)getElement("split units", "party management")).getValue())));
              }
              map.cancelPath();
              moving = false;
              map.cancelMoveNodes();
            }
          }
        }
      }
      if (button == LEFT) {
        if (eventType == "mousePressed" && !bombarding) {
          mapClickPos = new int[]{mouseX, mouseY, millis()};
        }
        if (eventType == "mouseReleased") {
          if (bombarding) {
            int x = floor(map.scaleXInv());
            int y = floor(map.scaleYInv());
            if (activePanel == "default" && !UIHovering() && 0<=x&&x<mapWidth&&0<=y&&y<mapHeight) {
              postEvent(new Bombard(selectedCellX, selectedCellY, x, y));
              map.disableBombard();
              bombarding = false;
            }
          }
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
                    postEvent(new Move(selectedCellX, selectedCellY, x, y, round(((Slider)getElement("split units", "party management")).getValue())));
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
    public void selectCell() {
      // select cell based on mouse pos
      int x = floor(map.scaleXInv());
      int y = floor(map.scaleYInv());
      selectCell(x, y, false);
    }

    public boolean cellInBounds(int x, int y) {
      return 0<=x&&x<mapWidth&&0<=y&&y<mapHeight;
    }

    public void selectCell(int x, int y, boolean raw) {
      // raw means from mouse position
      deselectCell();
      if (raw) {
        selectCell();
      } else if (cellInBounds(x, y) && !cinematicMode) {
        LOGGER_GAME.finer(String.format("Cell selected at (%d, %d) which is in bounds", x, y));
        tooltip.hide();
        selectedCellX = x;
        selectedCellY = y;
        cellSelected = true;
        map.selectCell(selectedCellX, selectedCellY);
        //map.setWidth(round(width-bezel*2-400));
        if (players[turn].visibleCells[y][x] != null){
          getPanel("land management").setVisible(true);
        } else {
          getPanel("land management").setVisible(false);
        }

        updatePartyManagementInterface();
      }
    }

    public void updatePartyManagementInterface() {
      if (parties[selectedCellY][selectedCellX] != null && (parties[selectedCellY][selectedCellX].isTurn(turn) || jsManager.loadBooleanSetting("show all party managements"))) {
        if (parties[selectedCellY][selectedCellX].getTask() != JSONIndex(gameData.getJSONArray("tasks"), "Battle") && parties[selectedCellY][selectedCellX].isTurn(turn)) {
          ((Slider)getElement("split units", "party management")).show();
          ((TaskManager)getElement("tasks", "party management")).active = true;
          ((TaskManager)getElement("tasks", "party management")).show();
          ((Text)getElement("task text", "party management")).show();
        } else {
          ((Slider)getElement("split units", "party management")).hide();
          ((TaskManager)getElement("tasks", "party management")).active = false;
          ((TaskManager)getElement("tasks", "party management")).hide();
          ((Text)getElement("task text", "party management")).hide();
        }
        getPanel("party management").setVisible(true);
        if (parties[selectedCellY][selectedCellX].getUnitNumber() <= 1) {
          ((Slider)getElement("split units", "party management")).hide();
        } else {
          ((Slider)getElement("split units", "party management")).setScale(1, parties[selectedCellY][selectedCellX].getUnitNumber(), parties[selectedCellY][selectedCellX].getUnitNumber(), 1, parties[selectedCellY][selectedCellX].getUnitNumber()/2);
        }

        partyManagementColour = brighten(playerColours[turn], -80); // Top
        getPanel("party management").setColour(brighten(playerColours[turn], 70)); // Background

        if (isEquipmentCollectionAllowed(selectedCellX, selectedCellY)) {
          ((Button)getElement("stock up button", "party management")).bgColour = color(150);
          ((Button)getElement("stock up button", "party management")).textColour = color(0);
          ((Button)getElement("stock up button", "party management")).activate();
        } else {
          ((Button)getElement("stock up button", "party management")).bgColour = color(210);
          ((Button)getElement("stock up button", "party management")).textColour = color(100);
          ((Button)getElement("stock up button", "party management")).deactivate();
        }
        ((ToggleButton)getElement("auto stock up toggle", "party management")).setState(parties[selectedCellY][selectedCellX].getAutoStockUp());
        checkTasks();
        int selectedEquipmentType = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
        if (selectedEquipmentType != -1) {
          updatePartyManagementProficiencies();
        }
        ((EquipmentManager)getElement("equipment manager", "party management")).setEquipment(parties[selectedCellY][selectedCellX]);
        updatePartyManagementProficiencies();
        updateCurrentPartyTrainingFocus();
        updateUnitCapIncrementer();
        updateBombardment();
      }
    }

    public void updateBombardment() {
      if (parties[selectedCellY][selectedCellX].equipment[1] != -1 &&
        gameData.getJSONArray("equipment").getJSONObject(1).getJSONArray("types").getJSONObject(parties[selectedCellY][selectedCellX].equipment[1]).hasKey("range") &&
        parties[selectedCellY][selectedCellX].equipmentQuantities[1] > 0 && parties[selectedCellY][selectedCellX].getMovementPoints() > 0) {
        getElement("bombardment button", "party management").visible = true;
      } else {
        getElement("bombardment button", "party management").visible = false;
      }
    }

    public void updateUnitCapIncrementer() {
      ((IncrementElement)getElement("unit cap incrementer", "party management")).setUpper(jsManager.loadIntSetting("party size"));
      ((IncrementElement)getElement("unit cap incrementer", "party management")).setLower(parties[selectedCellY][selectedCellX].getUnitNumber());
      ((IncrementElement)getElement("unit cap incrementer", "party management")).setValue(parties[selectedCellY][selectedCellX].getUnitCap());
    }

    public void updatePartyManagementProficiencies() {
      // Update proficiencies with those for current party
      ((ProficiencySummary)getElement("proficiency summary", "party management")).setProficiencies(parties[selectedCellY][selectedCellX].getRawProficiencies());
      ((ProficiencySummary)getElement("proficiency summary", "party management")).setProficiencyBonuses(parties[selectedCellY][selectedCellX].getRawBonusProficiencies());
    }

    public void updateCurrentPartyTrainingFocus() {
      int trainingFocus = parties[selectedCellY][selectedCellX].getTrainingFocus();
      ((DropDown)getElement("party training focus", "party management")).setValue(jsManager.indexToProficiencyDisplayName(trainingFocus));
    }

    public void drawPartyManagement(PGraphics panelCanvas) {
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
      float barY = sidePanelY + 13*jsManager.loadFloatSetting("text scale") + sidePanelH*0.15f + bezel*2;
      if (jsManager.loadBooleanSetting("show party id")) {
        panelCanvas.text("Party id: "+parties[selectedCellY][selectedCellX].id, 120+sidePanelX, barY);
        barY += 13*jsManager.loadFloatSetting("text scale");
      }
      panelCanvas.text("Movement Points Remaining: "+parties[selectedCellY][selectedCellX].getMovementPoints(turn) + "/"+parties[selectedCellY][selectedCellX].getMaxMovementPoints(), 120+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");

      if (jsManager.loadBooleanSetting("show all party managements")&&parties[selectedCellY][selectedCellX].player==-1) {
        String t1 = ((Battle)parties[selectedCellY][selectedCellX]).attacker.id;
        String t2 = "Units: "+((Battle)parties[selectedCellY][selectedCellX]).attacker.getUnitNumber() + "/" + jsManager.loadIntSetting("party size");
        float offset = max(panelCanvas.textWidth(t1+" "), panelCanvas.textWidth(t2+" "));
        panelCanvas.text(t1, 120+sidePanelX, barY);
        panelCanvas.text(((Battle)parties[selectedCellY][selectedCellX]).defender.id, 120+sidePanelX+offset, barY);
        barY += 13*jsManager.loadFloatSetting("text scale");
        panelCanvas.text(t2, 120+sidePanelX, barY);
        panelCanvas.text("Units: "+((Battle)parties[selectedCellY][selectedCellX]).defender.getUnitNumber() + "/" + jsManager.loadIntSetting("party size"), 120+sidePanelX+offset, barY);
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

      if (!((Text)getElement("turns remaining", "party management")).text.equals("")) {
        ((Text)getElement("turns remaining", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        barY += 13*jsManager.loadFloatSetting("text scale");
      }

      ((Slider)getElement("split units", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      barY += ((Slider)getElement("split units", "party management")).h;
      barY += ((Button)getElement("stock up button", "party management")).h;

      ((Button)getElement("stock up button", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      ((ToggleButton)getElement("auto stock up toggle", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      ((IncrementElement)getElement("unit cap incrementer", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      barY += ((Button)getElement("stock up button", "party management")).h + bezel;

      ((EquipmentManager)getElement("equipment manager", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      barY += ((EquipmentManager)getElement("equipment manager", "party management")).getBoxHeight();

      ((Text)getElement("task text", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      ((Text)getElement("proficiencies", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      barY += ((Text)getElement("proficiencies", "party management")).h;

      ((TaskManager)getElement("tasks", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      ((ProficiencySummary)getElement("proficiency summary", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
      barY += ((ProficiencySummary)getElement("proficiency summary", "party management")).h;
      ((DropDown)getElement("party training focus", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
    }

    public String resourcesList(float[] resources) {
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

    public void drawCellManagement(PGraphics panelCanvas) {
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
      panelCanvas.text("Cell Type: "+gameData.getJSONArray("terrain").getJSONObject(terrain[selectedCellY][selectedCellX]).getString("display name"), 5+sidePanelX, barY);
      barY += 13*jsManager.loadFloatSetting("text scale");
      if (buildings[selectedCellY][selectedCellX] != null) {
        if (buildings[selectedCellY][selectedCellX].type != 0)
          panelCanvas.text("Building: "+buildingTypes[buildings[selectedCellY][selectedCellX].type], 5+sidePanelX, barY);
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

    public String getResourceString(float amount) {
      String tempString = roundDp(""+amount, 1);
      if (amount >= 0) {
        fill(0);
        tempString = "+"+tempString;
      } else {
        fill(255, 0, 0);
      }
      return tempString;
    }


    public void drawRocketProgressBar(PGraphics panelCanvas) {
      int x, y=0, w, h;
      String progressMessage;
      int PROGRESSBARWIDTH = round(width*0.25f);
      int PROGRESSBARHEIGHT = round(height*0.03f);
      int[] progresses = new int[players.length];
      int count = 0;
      for (int i = 0; i < players.length; i++) {
        progresses[i] = PApplet.parseInt(players[i].resources[jsManager.getResIndex(("rocket progress"))]);
        if (progresses[i] > 0) {
          count++;
        }
      }

      if (max(progresses) <= 0) return;

      if (progresses[turn] == 0) {
        progressMessage = "";
      } else {
        if (progresses[turn] >= 1000) {
          progressMessage = "Rocket Progress: Completed";
        } else {
          progressMessage = "Rocket Progress: "+str(progresses[turn])+"/1000";
        }
      }
      x = round((width-PROGRESSBARWIDTH)/2);
      y = round(height*0.05f);
      w = PROGRESSBARWIDTH;
      h = round(PROGRESSBARHEIGHT/count);
      for (int i = 0; i < players.length; i++) {
        if (progresses[i] > 0) {
          panelCanvas.fill(200);
          panelCanvas.stroke(100);
          panelCanvas.rect(x, y, w, h);
          panelCanvas.noStroke();
          w = round(min(w, w*progresses[i]/1000));
          panelCanvas.fill(playerColours[i]);
          panelCanvas.rect(x, y, w, h);
          y += h;
          w = PROGRESSBARWIDTH;
        }
      }
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(CENTER, BOTTOM);
      panelCanvas.fill(200);
      int tw = ceil((panelCanvas.textWidth(progressMessage)));
      y = round(height*0.05f);
      panelCanvas.rect(round(width/2) -tw/2, y-10*jsManager.loadFloatSetting("text scale"), tw, 10*jsManager.loadFloatSetting("text scale"));
      panelCanvas.fill(0);
      panelCanvas.text(progressMessage, round(width/2), y);
    }

    public ArrayList<String> keyboardEvent(String eventType, char _key) {
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
            if (((Button)getElement("end turn", "bottom bar")).getText().equals("Advance Units")) {
              autoMoveParties();
              ((Button)getElement("end turn", "bottom bar")).setText("Next Turn");
            } else {
              postEvent(new EndTurn());
            }
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
    public void enterState() {
      initialiseResources();
      reloadGame();
    }

    public void reloadGame() {
      LOGGER_MAIN.fine("Reloading game...");
      mapWidth = jsManager.loadIntSetting("map size");
      mapHeight = jsManager.loadIntSetting("map size");
      playerCount = players.length - 1; // THIS NEEDS TO BE CHANGED WHEN ADDING BANDIT TOGGLE
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
        checkForPlayerWin();
        if (jsManager.loadBooleanSetting("map is 3d")) {
          map.targetCell(PApplet.parseInt(this.players[turn].cameraCellX), PApplet.parseInt(this.players[turn].cameraCellY), this.players[turn].blockSize);
          ((Map3D)map).focusedX = ((Map3D)map).targetXOffset;
          ((Map3D)map).focusedY = ((Map3D)map).targetYOffset;
          ((Map3D)map).zoom = this.players[turn].blockSize;
        } else {
          map.targetCell(PApplet.parseInt(this.players[turn].cameraCellX), PApplet.parseInt(this.players[turn].cameraCellY), this.players[turn].blockSize);
          ((Map2D)map).mapXOffset = ((Map2D)map).targetXOffset;
          ((Map2D)map).mapYOffset = ((Map2D)map).targetYOffset;
          ((Map2D)map).blockSize = this.players[turn].blockSize;
        }
      } else {
        LOGGER_MAIN.finer("Creating new map");
        ((BaseMap)map).generateMap(mapWidth, mapHeight, players.length);
        terrain = ((BaseMap)map).terrain;
        buildings = ((BaseMap)map).buildings;
        parties = ((BaseMap)map).parties;
        PVector[] playerStarts = generateStartingParties();
        // THIS NEEDS TO BE CHANGED WHEN ADDING PLAYER INPUT SELECTOR
        players[2] = new Player((int)playerStarts[2].x, (int)playerStarts[2].y, jsManager.loadIntSetting("starting block size"), startingResources.clone(), color(0, 255, 0), "Player 3  ", 0, 2);
        float[] conditions2 = map.targetCell((int)playerStarts[1].x, (int)playerStarts[1].y, jsManager.loadIntSetting("starting block size"));
        players[1] = new Player((int)playerStarts[1].x, (int)playerStarts[1].y, jsManager.loadIntSetting("starting block size"), startingResources.clone(), color(255, 0, 0), "Player 2  ", 0, 1);
        float[] conditions1 = map.targetCell((int)playerStarts[0].x, (int)playerStarts[0].y, jsManager.loadIntSetting("starting block size"));
        players[0] = new Player((int)playerStarts[0].x, (int)playerStarts[0].y, jsManager.loadIntSetting("starting block size"), startingResources.clone(), color(0, 0, 255), "Player 1  ", 0, 0);

        players[playerCount] = new Player(0, 0, jsManager.loadIntSetting("starting block size"), startingResources.clone(), color(255, 0, 255), "Player 4  ", 1, 3);

        turn = 0;
        turnNumber = 0;
        deselectCell();
      }
      playerColours = new int[players.length];
      partyImages = new PImage[players.length];
      for (int i=0; i < players.length-1; i++) {
        playerColours[i] = players[i].colour;
        partyImages[i] = partyBaseImages[1].copy();
        partyImages[i].loadPixels();
        for (int j = 0; j < partyImages[i].pixels.length; j++) {
          if (partyImages[i].pixels[j] == color(255, 0, 0)) {
            partyImages[i].pixels[j] = playerColours[i];
          }
        }
      }
      playerColours[players.length-1] = players[players.length-1].colour;
      partyImages[players.length-1] = partyBaseImages[2].copy();

      if (players[turn].cellSelected) {
        selectCell(players[turn].cellX, players[turn].cellY, false);
      }
      map.setPlayerColours(playerColours);

      ((Console)getElement("console", "console")).giveObjects(map, players, this);

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
        for (int i = playerCount; i >= 0; i--) {
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
      players[turn].updateVisibleCells(terrain, buildings, parties);
      if (players[turn].controllerType == 0){
        map.updateVisibleCells(players[turn].visibleCells);
      }

      map.setDrawingTaskIcons(true);
      map.setDrawingUnitBars(true);
      LOGGER_MAIN.finer("Finished reloading game");
    }
    public int cost(int x, int y, int prevX, int prevY) {
      float mult = 1;
      if (x!=prevX && y!=prevY) {
        mult = 1.41f;
      }
      if (0<=x && x<jsManager.loadIntSetting("map size") && 0<=y && y<jsManager.loadIntSetting("map size")) {
        return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("movement cost")*mult);
      }
      //Not a valid location
      return -1;
    }

    public ArrayList<int[]> getPath(int startX, int startY, int targetX, int targetY, Node[][] nodes) {
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
    public Node[][] djk(int x, int y) {
      int[][] mvs = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
      int w = mapWidth;
      int h = mapHeight;
      Node[][] nodes = new Node[h][w];
      nodes[y][x] = new Node(0, false, x, y);
      ArrayList<Integer> curMinCosts = new ArrayList<Integer>();
      ArrayList<int[]> curMinNodes = new ArrayList<int[]>();
      curMinNodes.add(new int[]{x, y});
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
            if (totalNewCost < parties[y][x].getMaxMovementPoints()*100) {
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
    public int search(ArrayList<int[]> nodes, int x, int y) {
      for (int i=0; i < nodes.size(); i++) {
        if (nodes.get(i)[0] == x && nodes.get(i)[1] == y) {
          return i;
        }
      }
      return -1;
    }
    public int search(ArrayList<Integer> costs, float target) {
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
    public boolean startInvalid(PVector[] ps) {
      for (int i=0; i < playerCount; i++) {
        if (((BaseMap)map).isWater(PApplet.parseInt(ps[i].x), PApplet.parseInt(ps[i].y))) {
          //Check starting position if on water
          return true;
        }
        for (int j=i+1; j < playerCount; j++) {
          if (ps[i].dist(ps[j])<mapWidth/8) {
            // Check distances between all players
            return true;
          }
        }
      }
      return false;
    }
    public PVector generatePartyPosition() {
      return new PVector(PApplet.parseInt(random(0, mapWidth)), PApplet.parseInt(random(0, mapHeight)));
    }

    public PVector[] generateStartingParties() {
      LOGGER_GAME.fine("Generating starting positions");
      PVector[] playersStartingPositions = new PVector[playerCount];
      int counter = 0;
      for (int i=0; i < playerCount; i++) {
        playersStartingPositions[i] = new PVector();
      }
      while (startInvalid(playersStartingPositions)&&counter<1000) {
        counter++;
        for (int i=0; i < playerCount; i++) {
          playersStartingPositions[i] = generatePartyPosition();
        }
      }
      if (counter == 1000) {
        LOGGER_GAME.warning("Resorted to invalid party starts after "+counter+" attempts");
      }
      for (int i=0; i < playerCount; i++) {
        LOGGER_GAME.fine(String.format("Player %d party positition: (%f, %f)", i+1, playersStartingPositions[i].x, playersStartingPositions[i].y));
      }
      if (loadingName == null) {
        for (int i=0; i < playerCount; i++) {
          parties[(int)playersStartingPositions[i].y][(int)playersStartingPositions[i].x] = new Party(i, jsManager.loadIntSetting("party size")/2, JSONIndex(gameData.getJSONArray("tasks"), "Rest"), gameData.getJSONObject("game options").getInt("movement points"), String.format("Player %d   #0", i));
        }
      }
      return playersStartingPositions;
    }

    public void enterCinematicMode() {
      LOGGER_GAME.finer("Entering cinematic mode...");
      cinematicMode = true;
      getPanel("bottom bar").setVisible(false);
      getPanel("land management").setVisible(false);
      getPanel("party management").setVisible(false);
      ((BaseMap)map).cinematicMode = true;
    }
    public void leaveCinematicMode() {
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

    public void startRocketLaunch() {
      LOGGER_GAME.finer("Starting rocket launch");
      rocketVelocity = new PVector(0, 0, 0);
      rocketBehaviour = PApplet.parseInt(random(10));
      buildings[selectedCellY][selectedCellX].image_id=0;
      rocketLaunching = true;
      rocketPosition = new PVector(selectedCellX, selectedCellY, 0);
      map.enableRocket(rocketPosition, rocketVelocity);
      enterCinematicMode();
      rocketStartTime = millis();
    }
    public void handleRocket() {
      float t = PApplet.parseFloat(millis()-rocketStartTime)/1000;
      if (rocketBehaviour > 6) {
        rocketVelocity.z = 10*(exp(t)-1)/(exp(t)+1);
        if (rocketPosition.z>mapHeight) {
          rocketLaunchEnd();
        }
      } else {
        rocketVelocity.x = 0.5f*t;
        rocketVelocity.z = 3*t-pow(t, 2);
        if (rocketPosition.z<0) {
          rocketLaunchEnd();
        }
      }
      rocketVelocity.div(frameRate);
      rocketPosition.add(rocketVelocity);
    }
    public void rocketLaunchEnd() {
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

    public String nextRollingId(String playerName) {
      int maxN = 0;
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          try {
            if (parties[y][x] != null && parties[y][x].id.length() >= 11 && parties[y][x].id.substring(0, 10).equals(playerName) && parties[y][x].id.charAt(11)=='#') {
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



  class Menu extends State {
    PImage BGimg;
    PShape bg;
    String currentPanel, newPanel;
    HashMap<String, String[]> stateChangers, settingChangers;

    Menu() {
      LOGGER_MAIN.fine("Initialising menu");
      BGimg = loadImage(resourcesRoot+"img/ui/menu_background.jpeg");
      bg = createShape(RECT, 0, 0, width, height);
      bg.setTexture(BGimg);

      currentPanel = "startup";
      loadMenuPanels();
      newPanel = currentPanel;
      activePanel = currentPanel;
    }

    public void loadMenuPanels() {
      LOGGER_MAIN.fine("Loading menu panels");
      resetPanels();
      jsManager.loadMenuElements(this, jsManager.loadFloatSetting("gui scale"));
      hidePanels();
      getPanel(currentPanel).setVisible(true);
      stateChangers = jsManager.getChangeStateButtons();
      settingChangers = jsManager.getChangeSettingButtons();

      addElement("loading manager", new BaseFileManager(width/4, height/4, width/2, height/3, "saves"), "load game");
    }

    public int currentColour() {
      float c = abs(((float)(hour()-12)+(float)minute()/60)/24);
      int day = color(255, 255, 255, 50);
      int night = color(0, 0, 50, 255);
      return lerpColor(day, night, c*2);
    }

    public String update() {
      shape(bg);
      //if(((ToggleButton)getElement("background dimming", "settings")).getState()){
      //  pushStyle();
      //  fill(currentColour());
      //  rect(0, 0, width, height);
      //  popStyle();
      //}
      if (!currentPanel.equals(newPanel)) {
        changeMenuPanel();
      }
      drawPanels();

      drawMenuTitle();
      return getNewState();
    }

    public void drawMenuTitle() {
      // Draw menu state title
      if (jsManager.menuStateTitle(currentPanel) != null) {
        fill(0);
        textFont(getFont(jsManager.loadFloatSetting("text scale")*30));
        textAlign(CENTER, TOP);
        text(jsManager.menuStateTitle(currentPanel), width/2, 100);
      }
    }

    public void changeMenuPanel() {
      LOGGER_MAIN.fine("Changing menu panel to: "+newPanel);
      panelToTop(newPanel);
      getPanel(newPanel).setVisible(true);
      getPanel(currentPanel).setVisible(false);
      currentPanel = new String(newPanel);
      for (Element elem : getPanel(newPanel).elements) {
        elem.mouseEvent("mouseMoved", LEFT);
      }
      activePanel = newPanel;
    }

    public void enterState() {
      loadMenuPanels(); // Refresh menu
      newPanel = "startup";
    }

    public void saveMenuSetting(String id, Event event) {
      if (settingChangers.get(id) != null) {
        LOGGER_MAIN.finer(String.format("Saving setting id:%s, event id:%s", id, event.id));
        String type = jsManager.getElementType(event.panel, id);
        switch (type) {
        case "slider":
          jsManager.saveSetting(settingChangers.get(id)[0], ((Slider)getElement(id, event.panel)).getValue());
          break;
        case "toggle button":
          jsManager.saveSetting(settingChangers.get(id)[0], ((ToggleButton)getElement(id, event.panel)).getState());
          break;
        case "tickbox":
          jsManager.saveSetting(settingChangers.get(id)[0], ((Tickbox)getElement(id, event.panel)).getState());
          break;
        case "dropdown":
          switch (((DropDown)getElement(id, event.panel)).optionTypes) {
          case "floats":
            jsManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getFloatVal());
            break;
          case "strings":
            jsManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getStrVal());
            break;
          case "ints":
            jsManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getIntVal());
            break;
          default:
            LOGGER_MAIN.warning("invalid dropdown type: " + ((DropDown)getElement(id, event.panel)).optionTypes);
            break;
          }
          break;
        default:
          LOGGER_MAIN.warning("Invalid element type: "+type);
          break;
        }
      }
    }

    public void revertChanges(String panel, boolean onlyAutosaving) {
      LOGGER_MAIN.fine("Reverting changes made to settings that are not autosaving");
      for (Element elem : getPanel(panel).elements) {
        if (elem.id.equals("loading manager") && ((onlyAutosaving || !jsManager.hasFlag(panel, elem.id, "autosave")) && settingChangers.get(elem.id) != null)) {
          String type = jsManager.getElementType(panel, elem.id);
          switch (type) {
          case "slider":
            ((Slider)getElement(elem.id, panel)).setValue(jsManager.loadFloatSetting(jsManager.getSettingName(elem.id, panel)));
            break;
          case "toggle button":
            ((ToggleButton)getElement(elem.id, panel)).setState(jsManager.loadBooleanSetting(jsManager.getSettingName(elem.id, panel)));
            break;
          case "tickbox":
            ((Tickbox)getElement(elem.id, panel)).setState(jsManager.loadBooleanSetting(jsManager.getSettingName(elem.id, panel)));
            break;
          case "dropdown":
            switch (((DropDown)getElement(elem.id, panel)).optionTypes) {
            case "floats":
              ((DropDown)getElement(elem.id, panel)).setSelected(""+jsManager.loadFloatSetting(jsManager.getSettingName(elem.id, panel)));
              break;
            case "strings":
              ((DropDown)getElement(elem.id, panel)).setSelected(""+jsManager.loadStringSetting(jsManager.getSettingName(elem.id, panel)));
              break;
            case "ints":
              ((DropDown)getElement(elem.id, panel)).setSelected(""+jsManager.loadIntSetting(jsManager.getSettingName(elem.id, panel)));
              break;
            default:
              LOGGER_MAIN.warning("Invalid dropdown type: "+((DropDown)getElement(elem.id, panel)).optionTypes);
              break;
            }
            break;
          default:
            LOGGER_MAIN.warning("Invalid type for element:"+type);
            break;
          }
        }
      }
    }

    public void elementEvent(ArrayList<Event> events) {
      for (Event event : events) {
        if (event.type.equals("valueChanged") && settingChangers.get(event.id) != null && event.panel != null) {
          if (jsManager.hasFlag(event.panel, event.id, "autosave")) {
            saveMenuSetting(event.id, event);
            jsManager.writeSettings();
            if (event.id.equals("framerate cap")) {
              setFrameRateCap();
            }
          }
          if (event.id.equals("sound on")) {
            loadSounds();
          }
          if (event.id.equals("volume")) {
            setVolume();
          }
        }
        if (event.type.equals("clicked")) {
          if (stateChangers.get(event.id) != null && stateChangers.get(event.id)[0] != null ) {
            newPanel = stateChangers.get(event.id)[0];
            revertChanges(event.panel, false);
            if (newPanel.equals("load game")) {
              ((BaseFileManager)getElement("loading manager", "load game")).loadSaveNames();
            }
          } else if (event.id.equals("apply")) {
            for (Element elem : getPanel(event.panel).elements) {
              if (!jsManager.hasFlag(event.panel, elem.id, "autosave")) {
                saveMenuSetting(elem.id, event);
              }
            }
            jsManager.writeSettings();
            loadMenuPanels();
          } else if (event.id.equals("revert")) {
            revertChanges(event.panel, false);
          } else if (event.id.equals("reset default map settings")) {
            LOGGER_MAIN.info("Resetting default setting for new map");
            jsManager.saveDefault("hills height");
            jsManager.saveDefault("water level");
            jsManager.saveDefault("map size");
            jsManager.saveDefault("starting food");
            jsManager.saveDefault("starting wood");
            jsManager.saveDefault("starting stone");
            jsManager.saveDefault("starting metal");
            for (Integer i=1; i<gameData.getJSONArray("terrain").size()+1; i++) {
              if (!gameData.getJSONArray("terrain").getJSONObject(i-1).isNull("weighting")) {
                jsManager.saveDefault(gameData.getJSONArray("terrain").getJSONObject(i-1).getString("id")+" weighting");
              }
            }
            revertChanges(event.panel, true);
          } else if (event.id.equals("start")) {
            LOGGER_MAIN.info("Starting state change to a new game");
            newState = "map";
            loadingName = null;
          } else if (event.id.equals("load")) {
            loadingName = ((BaseFileManager)getElement("loading manager", "load game")).selectedSaveName();
            LOGGER_MAIN.info("Starting state change to game via loading with file name"+loadingName);
            newState = "map";
          } else if (event.id.equals("exit")) {
            quitGame();
          }
        }
      }
    }
  }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "RocketResourceRace" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
