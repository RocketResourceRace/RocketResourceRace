package states;


import control.ProductionManager;
import control.TaskManager;
import event.*;
import json.JSONManager;
import map.*;
import party.Battle;
import party.Party;
import party.Siege;
import player.Player;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;
import control.ResourceManager;
import ui.Panel;
import ui.State;
import ui.element.*;
import util.BattleEstimateManager;
import util.Cell;
import util.Node;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;

import static com.jogamp.newt.event.KeyEvent.VK_F12;
import static json.JSONManager.*;
import static processing.core.PApplet.*;
import static util.Constants.BEZEL;
import static util.Dijkstra.LimitedKnowledgeDijkstra;
import static util.Font.getFont;
import static util.GameInfo.turnNumber;
import static util.GameInfo.turn;
import static util.Image.partyBaseImages;
import static util.Image.partyImages;
import static util.Logging.LOGGER_GAME;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Game extends State {
    private final int mapElementHeight = round(papplet.height);
    private PGraphics gameUICanvas;
    private String[] buildingTypes;
    public int mapHeight = JSONManager.loadIntSetting("map size");
    public int mapWidth = JSONManager.loadIntSetting("map size");
    public int[][] terrain;
    public Party[][] parties;
    public Building[][] buildings;
    private BattleEstimateManager battleEstimateManager;
    private NotificationManager notificationManager;
    private ResourceManager resourceManager;
    private ProductionManager productionManager;
    private TaskManager taskManager;
    private AdvancedTooltip tooltip;
    private boolean changeTurn = false;
    private int winner = -1;
    private Map map;
    public Player[] players;
    private int selectedCellX, selectedCellY, sidePanelX, sidePanelY, sidePanelW, sidePanelH;
    private boolean cellSelected=false, moving=false;
    private int partyManagementColour;
    private ArrayList<Integer[]> prevIdle;
    private Party splittedParty;
    private int[] mapClickPos = null;
    private boolean cinematicMode;
    private boolean rocketLaunching;
    private int rocketBehaviour;
    private PVector rocketPosition;
    private PVector rocketVelocity;
    private int rocketStartTime;
    private int[] playerColours;
    private boolean bombarding = false;
    private int playerCount;

    public Game() {
        try {
            LOGGER_MAIN.fine("initializing game");
            gameUICanvas = papplet.createGraphics(papplet.width, papplet.height, P2D);

            // THIS NEEDS TO BE CHANGED WHEN ADDING PLAYER INPUT SELECTOR
            players = new Player[4];
            //

            resourceManager = new ResourceManager(players);
            resourceManager.initialiseResources();
            taskManager = new TaskManager();
            taskManager.initialiseTasks(resourceManager.getNumResources());
            initialiseBuildings();

            int mapElementWidth = round(papplet.width);
            addElement("2dmap", new Map2D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight, players));
            addElement("3dmap", new Map3D(0, 0, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
            notificationManager = new NotificationManager(0, 0, 0, 0, papplet.color(100), papplet.color(255), 8, turn, players.length);
            addElement("notification manager", notificationManager);


            addPanel("land management", 0, 0, papplet.width, papplet.height, false, true, papplet.color(50, 200, 50), papplet.color(0));
            addPanel("party management", 0, 0, papplet.width, papplet.height, false, true, papplet.color(110, 110, 255), papplet.color(0));
            addPanel("bottom bar", 0, papplet.height-70, papplet.width, 70, true, true, papplet.color(150), papplet.color(50));
            addPanel("resource management", papplet.width/4, papplet.height/4, papplet.width/2, papplet.height/2, false, true, papplet.color(200), papplet.color(0));
            addPanel("end screen", 0, 0, papplet.width, papplet.height, false, true, papplet.color(50, 50, 50, 50), papplet.color(0));
            addPanel("pause screen", 0, 0, papplet.width, papplet.height, false, true, papplet.color(50, 50, 50, 50), papplet.color(0));
            addPanel("save screen", (int)(papplet.width/2f+JSONManager.loadFloatSetting("gui scale")*150+ (JSONManager.loadFloatSetting("gui scale")*20)), (int)(papplet.height/2-5*JSONManager.loadFloatSetting("gui scale")*40), (int)(JSONManager.loadFloatSetting("gui scale")*500), (int)(JSONManager.loadFloatSetting("gui scale")*500), false, false, papplet.color(50), papplet.color(0));
            addPanel("overlay", 0, 0, papplet.width, papplet.height, true, false, papplet.color(255, 255), papplet.color(255, 255));
            addPanel("console", 0, papplet.height/2, papplet.width, papplet.height/2, false, true, papplet.color(0, 220), papplet.color(255, 0));

            getPanel("save screen").setOverrideBlocking(true);

            tooltip = new AdvancedTooltip();
            addElement("0tooltip", tooltip, "overlay");

            addElement("end game button", new Button((int)(papplet.width/2-JSONManager.loadFloatSetting("gui scale")*papplet.width/16), (papplet.height/2+papplet.height/8), (int)(JSONManager.loadFloatSetting("gui scale")*papplet.width/8), (int)(JSONManager.loadFloatSetting("gui scale")*papplet.height/16), papplet.color(70, 70, 220), papplet.color(50, 50, 200), papplet.color(255), 14, CENTER, CENTER, "End Game"), "end screen");
            addElement("winner", new Text(papplet.width/2, papplet.height/2, (int)(JSONManager.loadFloatSetting("text scale")*10), "", papplet.color(255), CENTER), "end screen");

            addElement("main menu button", new Button((int)(papplet.width/2-JSONManager.loadFloatSetting("gui scale")*150), (int)(papplet.height/2-JSONManager.loadFloatSetting("gui scale")*40), (int)(JSONManager.loadFloatSetting("gui scale")*300), (int)(JSONManager.loadFloatSetting("gui scale")*60), papplet.color(70, 70, 220), papplet.color(50, 50, 200), papplet.color(255), 14, CENTER, CENTER, "Exit to Main Menu"), "pause screen");
            addElement("desktop button", new Button((int)(papplet.width/2-JSONManager.loadFloatSetting("gui scale")*150), (int)(papplet.height/2+JSONManager.loadFloatSetting("gui scale")*40), (int)(JSONManager.loadFloatSetting("gui scale")*300), (int)(JSONManager.loadFloatSetting("gui scale")*60), papplet.color(70, 70, 220), papplet.color(50, 50, 200), papplet.color(255), 14, CENTER, CENTER, "Exit to Desktop"), "pause screen");
            addElement("save as button", new Button((int)(papplet.width/2-JSONManager.loadFloatSetting("gui scale")*150), (int)(papplet.height/2-3*JSONManager.loadFloatSetting("gui scale")*40), (int)(JSONManager.loadFloatSetting("gui scale")*300), (int)(JSONManager.loadFloatSetting("gui scale")*60), papplet.color(70, 70, 220), papplet.color(50, 50, 200), papplet.color(255), 14, CENTER, CENTER, "Save As"), "pause screen");
            addElement("resume button", new Button((int)(papplet.width/2-JSONManager.loadFloatSetting("gui scale")*150), (int)(papplet.height/2-5*JSONManager.loadFloatSetting("gui scale")*40), (int)(JSONManager.loadFloatSetting("gui scale")*300), (int)(JSONManager.loadFloatSetting("gui scale")*60), papplet.color(70, 70, 220), papplet.color(50, 50, 200), papplet.color(255), 14, CENTER, CENTER, "Resume"), "pause screen");

            addElement("save button", new Button(BEZEL, BEZEL, (int)(JSONManager.loadFloatSetting("gui scale")*300)-2*BEZEL, (int)(JSONManager.loadFloatSetting("gui scale")*60), papplet.color(100), papplet.color(0), papplet.color(255), 14, CENTER, CENTER, "Save"), "save screen");
            addElement("saving manager", new BaseFileManager(BEZEL, (int)(4*JSONManager.loadFloatSetting("gui scale")*40), (int)(JSONManager.loadFloatSetting("gui scale")*500)-2*BEZEL, (int)(JSONManager.loadFloatSetting("gui scale")*320), "saves"), "save screen");
            addElement("save namer", new TextEntry(BEZEL, (int)(2*JSONManager.loadFloatSetting("gui scale")*40)+BEZEL*2, (int)(JSONManager.loadFloatSetting("gui scale")*300), (int)(JSONManager.loadFloatSetting("gui scale")*50), LEFT, papplet.color(0), papplet.color(100), papplet.color(0), "", "Save Name"), "save screen");

            addElement("turns remaining", new Text(BEZEL*2+220, BEZEL*4+30+30, 8, "", papplet.color(255), LEFT), "party management");
            addElement("move button", new Button(BEZEL, BEZEL*3, 100, 30, papplet.color(150), papplet.color(50), papplet.color(0), 10, CENTER, CENTER, "Move"), "party management");
            addElement("disband button", new Button(BEZEL, BEZEL*3, 100, 30, papplet.color(150), papplet.color(50), papplet.color(0), 10, CENTER, CENTER,"Dispand"), "party management");
            addElement("split units", new Slider(BEZEL+10, BEZEL*3+30, 220, 30, papplet.color(255), papplet.color(150), papplet.color(0), papplet.color(0), 0, 0, 0, 1, 1, 1, true, ""), "party management");
            addElement("tasks", taskManager.getTaskChooser(), "party management");
            addElement("task text", new Text(0, 0, 10, "Tasks", papplet.color(0), LEFT), "party management");
            addElement("stock up button", new Button(BEZEL, BEZEL*3, 100, 30, papplet.color(150), papplet.color(50), papplet.color(0), 10, CENTER, CENTER,"Stock Up"), "party management");
            addElement("auto stock up toggle", new ToggleButton(BEZEL, BEZEL*3, 100, 30, papplet.color(100), papplet.color(0), false, "Auto Stock Up"), "party management");
            addElement("unit cap incrementer", new IncrementElement(BEZEL, BEZEL*3, 100, 30, JSONManager.loadIntSetting("party size"), 0, JSONManager.loadIntSetting("party size"), 1, 5), "party management");

            addElement("proficiency summary", new ProficiencySummary(BEZEL, BEZEL*5+30+200, 220, 100), "party management");
            addElement("proficiencies", new Text(0, 0, 10, "Proficiencies", papplet.color(0), LEFT), "party management");
            addElement("equipment manager", new EquipmentManager(0, 0, 1), "party management");
            addElement("bombardment button", new BombardButton(BEZEL+100, BEZEL*3, 32, papplet.color(150)), "party management");

            DropDown partyTrainingFocusDropdown = new DropDown(0, 0, 1, 1, papplet.color(150), "Training Focus", "strings", 8);
            partyTrainingFocusDropdown.setOptions(JSONManager.getProficiencies());
            addElement("party training focus", partyTrainingFocusDropdown, "party management");

            int buttonW = 120;
            int buttonH = 50;
            addElement("end turn", new Button(BEZEL, BEZEL, buttonW, buttonH, papplet.color(150), papplet.color(50), papplet.color(0), 10, CENTER, CENTER, "Next Turn"), "bottom bar");
            addElement("idle party finder", new Button(BEZEL*2+ buttonW, BEZEL, buttonW, buttonH, papplet.color(150), papplet.color(50), papplet.color(0), 10, CENTER, CENTER, "Idle Party"), "bottom bar");
            addElement("resource summary", resourceManager.getResourceSummary(), "bottom bar");
            int resSummaryX = papplet.width-((ResourceSummary)(getElement("resource summary", "bottom bar"))).totalWidth();
            addElement("resource detailed", new Button(resSummaryX-50, BEZEL, 30, 20, papplet.color(150), papplet.color(50), papplet.color(0), 13, CENTER, CENTER, "^"), "bottom bar");
            addElement("resource expander", new Button(resSummaryX-50, 2*BEZEL+20, 30, 20, papplet.color(150), papplet.color(50), papplet.color(0), 10, CENTER, TOP,"<"), "bottom bar");

            addElement("turn number", new SingleLineTextBox(BEZEL*3+ buttonW *2, BEZEL, -1, buttonH, 14, "Turn 0", 0, 0), "bottom bar");
            addElement("2d 3d toggle", new ToggleButton(BEZEL*4+ buttonW *3, BEZEL*2, buttonW /2, buttonH -BEZEL, papplet.color(100), papplet.color(0), JSONManager.loadBooleanSetting("map is 3d"), "3D View"), "bottom bar");
            addElement("task icons toggle", new ToggleButton(round(BEZEL*5+ buttonW *3.5f), BEZEL*2, buttonW /2, buttonH -BEZEL, papplet.color(100), papplet.color(0), true, "Task Icons"), "bottom bar");
            addElement("unit number bars toggle", new ToggleButton(BEZEL*6+ buttonW *4, BEZEL*2, buttonW /2, buttonH -BEZEL, papplet.color(100), papplet.color(0), true, "Unit Bars"), "bottom bar");
            addElement("console", new Console(0, 0, papplet.width, papplet.height/2, 10), "console");
            addElement("resource management table", new ResourceManagementTable(BEZEL, BEZEL*2+30, papplet.width/2-BEZEL*2, papplet.height/2), "resource management");
            addElement("resources pages button", new HorizontalOptionsButton(BEZEL, BEZEL, 100, 30, papplet.color(150), 10, new String[]{"Resources", "Equipment"}), "resource management");


            prevIdle = new ArrayList<>();
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error initializing game", e);
            throw e;
        }
    }

    private void initialiseBuildings() {
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

    public void leaveState() {
        map.clearShape();
    }

    private JSONArray taskInitialCost(int type) {
        // Find initial cost for task (such as for buildings, 'Build Farm')
        try {
            return gameData.getJSONArray("tasks").getJSONObject(type).getJSONArray("initial cost");
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

    private Action taskAction(int task) {
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

    private float[] JSONToCost(JSONArray ja) {
        try {
            float[] costs = new float[resourceManager.getNumResources()];
            if (ja == null) {
                return null;
            }
            for (int i=0; i<ja.size(); i++) {
                costs[JSONManager.getResIndex((ja.getJSONObject(i).getString("id")))] = ja.getJSONObject(i).getFloat("quantity");
            }
            return costs;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error converting JSON cost to float cost", e);
            throw e;
        }
    }

    private String terrainString(int terrainI) {
        try {
            return gameData.getJSONArray("terrain").getJSONObject(terrainI).getString("id");
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error due to JSON being incorrectly formatted for terrain string", e);
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

    private float[] buildingCost(int actionType) {
        try {
            float[] a = JSONToCost(taskInitialCost(actionType));
            if (a == null)
                return new float[resourceManager.getNumResources()];
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

    private int getBombardmentDamage(Party attacker, Party defender) {
        return floor(attacker.getUnitNumber() * attacker.getEffectivenessMultiplier("ranged attack") /
                (defender.getEffectivenessMultiplier("defence") * 3));
    }

    private void postEvent(GameEvent event) {
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
                                productionManager.handlePartyExcessResources(x2, y2);
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
                    if (players[turn].resources[JSONManager.getResIndex(("rocket progress"))]==-1) {
                        players[turn].resources[JSONManager.getResIndex(("rocket progress"))] = 0;
                        LOGGER_GAME.fine("Rocket progress set to zero becuase party task is to produce rocket");
                    }
                } else {
                    Action a = taskAction(parties[selectedCellY][selectedCellX].getTask());
                    if (a != null) {
                        LOGGER_GAME.fine("Adding task action"+a.type);
                        float[] co = buildingCost(parties[selectedCellY][selectedCellX].getTask());
                        if (resourceManager.sufficientResources(players[turn].resources, co, true)) {
                            LOGGER_GAME.finer("Party has sufficient resources to change task to:"+parties[selectedCellY][selectedCellX].getTask());
                            parties[selectedCellY][selectedCellX].clearActions();
                            ((Text)getElement("turns remaining", "party management")).setText("");
                            parties[selectedCellY][selectedCellX].addAction(taskAction(parties[selectedCellY][selectedCellX].getTask()));
                            if (sum(co)>0) {
                                resourceManager.spendRes(players[turn], co);
                                buildings[selectedCellY][selectedCellX] = new Building(buildingIndex("Construction"), 0, turn);
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
                    newResID = JSONManager.getResIndex(JSONManager.getEquipmentTypeID(equipmentClass, newEquipmentType));
                }
                if (parties[selectedCellY][selectedCellX].getEquipment(equipmentClass) != -1) {
                    oldResID = JSONManager.getResIndex(JSONManager.getEquipmentTypeID(equipmentClass, parties[selectedCellY][selectedCellX].getEquipment(equipmentClass)));
                }

                try {

                    //If new type is 'other class blocking', recycle any equipment in blocked classes
                    String[] otherBlocking;
                    if (equipmentClass != -1 && newEquipmentType != -1) {
                        otherBlocking = JSONManager.getOtherClassBlocking(equipmentClass, newEquipmentType);
                    } else {
                        otherBlocking = null;
                    }
                    if (otherBlocking != null) {
                        for (String s : otherBlocking) {
                            int classIndex = getEquipmentClassFromID(s);
                            int otherResID = -1;
                            if (classIndex != -1) {
                                if (parties[selectedCellY][selectedCellX].getEquipment(classIndex) != -1) {
                                    otherResID = getResIndex(getEquipmentTypeID(classIndex, parties[selectedCellY][selectedCellX].getEquipment(classIndex)));
                                }
                                if (otherResID != -1 && parties[selectedCellY][selectedCellX].getEquipment(classIndex) != -1 && productionManager.isEquipmentCollectionAllowed(selectedCellX, selectedCellY, classIndex, parties[selectedCellY][selectedCellX].getEquipment(classIndex))) {
                                    players[turn].resources[otherResID] += parties[selectedCellY][selectedCellX].getEquipmentQuantity(classIndex);
                                }
                                parties[selectedCellY][selectedCellX].setEquipment(classIndex, -1, 0);  // Set it to empty after
                            }
                        }
                    } else {
                        // Recycle equipment if unequipping
                        if (oldResID != -1 && parties[selectedCellY][selectedCellX].getEquipment(equipmentClass) != -1 && productionManager.isEquipmentCollectionAllowed(selectedCellX, selectedCellY, equipmentClass, newEquipmentType)) {
                            players[turn].resources[oldResID] += parties[selectedCellY][selectedCellX].getEquipmentQuantity(equipmentClass);
                        }
                    }

                    int quantity;
                    if (newResID == -1 || !productionManager.isEquipmentCollectionAllowed(selectedCellX, selectedCellY, equipmentClass, newEquipmentType)) {
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
                    for (int i = 0; i < JSONManager.getNumEquipmentClasses(); i ++) {
                        if (parties[y][x].getEquipment(i) != -1) {
                            resID = JSONManager.getResIndex(JSONManager.getEquipmentTypeID(i, parties[y][x].getEquipment(i)));
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
                    getElement("stock up button", "party management").deactivate();
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
                    LOGGER_GAME.warning(String.format("Unit cap:%d set below number of units in party:%d", newCap, parties[y][x].getUnitNumber()));
                }
            } else {
                LOGGER_GAME.warning("Event type not found");
                valid = false;
            }

            if (valid) {
                LOGGER_GAME.finest("Event is valid, so updating things...");
                updateThingsAfterGameStateChange();
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error posting event", e);
            throw e;
        }
    }

    private void updateThingsAfterGameStateChange() {
        players[turn].updateVisibleCells(terrain, buildings, parties);
        if (players[turn].controllerType == 0){
            map.updateVisibleCells(players[turn].visibleCells);
        }
        if (!changeTurn) {
            productionManager.updateResourcesSummary(players[turn].resources[getResIndex("food")] == 0);
            updatePartyManagementInterface();

            if (anyIdle(turn)) {
                LOGGER_GAME.finest("There are idle units so highlighting button red");
                ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
            } else {
                LOGGER_GAME.finest("There are no idle units so not highlighting button red");
                ((Button)getElement("idle party finder", "bottom bar")).setColour(papplet.color(150));
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

    private boolean anyIdle(int turn) {
        for (int x=0; x<mapWidth; x++) {
            for (int y=0; y<mapWidth; y++) {
                if (parties[y][x] != null && parties[y][x].player == turn && isIdle(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateSidePanelElementsSizes() {
        // Update the size of state.elements on the party panel and cell management panel
        sidePanelX = round(papplet.width-450*JSONManager.loadFloatSetting("gui scale"));
        sidePanelY = BEZEL;
        sidePanelW = papplet.width-sidePanelX-BEZEL;
        sidePanelH = round(mapElementHeight)-70;
        getElement("notification manager", "default").transform(BEZEL, BEZEL, sidePanelW, round(sidePanelH*0.2f)-BEZEL*2);
        getElement("move button", "party management").transform(BEZEL, round(13*JSONManager.loadFloatSetting("text scale")+BEZEL), 60, 36);
        getElement("bombardment button", "party management").transform(BEZEL*2+60, round(13*JSONManager.loadFloatSetting("text scale")+BEZEL), 36, 36);
        getElement("split units", "party management").transform(round(10*JSONManager.loadFloatSetting("gui scale")+BEZEL), round(BEZEL*3+2*JSONManager.loadFloatSetting("text scale")*13), sidePanelW-2*BEZEL-round(20*JSONManager.loadFloatSetting("gui scale")), round(JSONManager.loadFloatSetting("text scale")*2*13));
        getElement("stock up button", "party management").transform(BEZEL, round(BEZEL*4+4*JSONManager.loadFloatSetting("text scale")*13), 100, 30);
        getElement("auto stock up toggle", "party management").transform(BEZEL*2+100, round(BEZEL*4+4*JSONManager.loadFloatSetting("text scale")*13+8*JSONManager.loadFloatSetting("text scale")), 100, PApplet.parseInt(30-JSONManager.loadFloatSetting("text scale")*8));
        getElement("unit cap incrementer", "party management").transform(BEZEL*3+200, round(BEZEL*4+4*JSONManager.loadFloatSetting("text scale")*13), 100, 30);
        ((EquipmentManager)getElement("equipment manager", "party management")).transform(BEZEL, round(BEZEL*5+4*JSONManager.loadFloatSetting("text scale")*13)+30, sidePanelW-BEZEL*2);
        int equipmentBoxHeight = PApplet.parseInt(((EquipmentManager)getElement("equipment manager", "party management")).getBoxHeight())+(30+BEZEL);
        getElement("tasks", "party management").transform(BEZEL, round(BEZEL*5+5*JSONManager.loadFloatSetting("text scale")*13+equipmentBoxHeight), sidePanelW/2-PApplet.parseInt(1.5f*BEZEL), 0);
        ((Text)getElement("task text", "party management")).translate(BEZEL, round(BEZEL*5+4*JSONManager.loadFloatSetting("text scale")*13+equipmentBoxHeight));
        getElement("proficiency summary", "party management").transform(sidePanelW/2+PApplet.parseInt(BEZEL*0.5f), round(BEZEL*5+5*JSONManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight, sidePanelW/2-PApplet.parseInt(1.5f*BEZEL), PApplet.parseInt(JSONManager.getNumProficiencies()*JSONManager.loadFloatSetting("text scale")*13));
        ((Text)getElement("proficiencies", "party management")).translate(sidePanelW/2+PApplet.parseInt(BEZEL*0.5f), round(BEZEL*5+4*JSONManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight);
        ((Text)getElement("turns remaining", "party management")).translate(100+BEZEL*2, round(13*JSONManager.loadFloatSetting("text scale")*2 + BEZEL*3));
        getElement("party training focus", "party management").transform(sidePanelW/2+PApplet.parseInt(BEZEL*0.5f), round(BEZEL*6+5*JSONManager.loadFloatSetting("text scale")*13)+equipmentBoxHeight+PApplet.parseInt(JSONManager.getNumProficiencies()*JSONManager.loadFloatSetting("text scale")*13), sidePanelW/2-PApplet.parseInt(BEZEL*(1.5f)), PApplet.parseInt(JSONManager.loadFloatSetting("text scale")*13));

        float taskRowHeight = ((TaskChooser)getElement("tasks", "party management")).getH(new PGraphics());

        float partyManagementHeight = round(BEZEL*7+6*JSONManager.loadFloatSetting("text scale")*13+equipmentBoxHeight) + taskRowHeight*10 + JSONManager.loadFloatSetting("gui scale")*BEZEL*10;
        getPanel("land management").transform(sidePanelX, sidePanelY, sidePanelW, round(sidePanelH*0.15f));
        getPanel("party management").transform(sidePanelX, sidePanelY+round(sidePanelH*0.15f)+BEZEL, sidePanelW, round(partyManagementHeight)-BEZEL*3);
        getElement("disband button", "party management").transform(sidePanelW-BEZEL-80, PApplet.parseInt(partyManagementHeight-BEZEL*4-30), 80, 30);
        getElement("resources pages button", "resource management").transform(BEZEL, BEZEL, PApplet.parseInt(100*JSONManager.loadFloatSetting("gui scale")), PApplet.parseInt(30*JSONManager.loadFloatSetting("gui scale")));
    }

    private void makeTaskAvailable(int task) {
        ((TaskChooser)getElement("tasks", "party management")).makeAvailable(gameData.getJSONArray("tasks").getJSONObject(task).getString("id"));
    }

    private void resetAvailableTasks() {
        ((TaskChooser)getElement("tasks", "party management")).resetAvailable();
        ((TaskChooser)getElement("tasks", "party management")).resetAvailableButOverBudget();
    }

    private void makeAvailableButOverBudget(int task) {
        ((TaskChooser)getElement("tasks", "party management")).makeAvailableButOverBudget(gameData.getJSONArray("tasks").getJSONObject(task).getString("id"));
    }

    private void checkTasks() {
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
                        correctTerrain = JSONManager.JSONContainsStr(js.getJSONArray("terrain"), terrainString(terrain[selectedCellY][selectedCellX]));
                    else
                        correctTerrain = true;
                    correctBuilding = false;
                    enoughResources = true;
                    enoughMovementPoints = true;
                    if (!js.isNull("initial cost")) {
                        for (int j=0; j<js.getJSONArray("initial cost").size(); j++) {
                            JSONObject initialCost = js.getJSONArray("initial cost").getJSONObject(j);
                            if (players[turn].resources[JSONManager.getResIndex((initialCost.getString("id")))]<(initialCost.getInt("quantity"))) {
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
                                    if (buildings[selectedCellY][selectedCellX] != null && JSONContainsStr(js.getJSONArray("buildings"), buildingString(buildings[selectedCellY][selectedCellX].getType())))
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
            ((TaskChooser)getElement("tasks", "party management")).select(gameData.getJSONArray("tasks").getJSONObject(parties[selectedCellY][selectedCellX].getTask()).getString("id"));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error checking tasks", e);
            throw e;
        }
    }

    private boolean UINotHovering() {
        //To avoid doing things while hoving over important stuff
        NotificationManager nm = ((NotificationManager)(getElement("notification manager", "default")));
        return (!getPanel("party management").mouseOver() || !getPanel("party management").isVisible()) && (!getPanel("land management").mouseOver() || !getPanel("land management").isVisible()) &&
                (!nm.moveOver() || nm.empty());
    }



    private void autoMoveParties() {
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

    private void processParties() {
        for (int y=0; y<mapHeight; y++) {
            for (int x=0; x<mapWidth; x++) {
                if (parties[y][x] != null) {
                    if (parties[y][x].player == turn) {
                        if (parties[y][x].getAutoStockUp()) {
                            postEvent(new StockUpEquipment(x, y));
                        }
                        if (parties[y][x].getTask() == JSONManager.getTaskIndex("Train Party")) {
                            parties[y][x].trainParty(JSONManager.indexToProficiencyID(parties[y][x].getTrainingFocus()), "training");
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
                                    buildings[y][x] = new Building(buildingIndex(action.building), 0, turn);

                                    if (parties[y][x] != null) {
                                        // Train party when completed building
                                        parties[y][x].trainParty("building speed", "constructing building");
                                    }

                                    if (buildings[y][x].getType() == buildingIndex("Quarry")) {
                                        LOGGER_GAME.fine("Quarry type detected so changing terrain...");
                                        //map.setHeightsForCell(x, y, JSONManager.loadFloatSetting("water level"));
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
                                    players[turn].resources[JSONManager.getResIndex(("wood"))]+=100;
                                    map.removeTreeTile(x, y);
                                }
                                terrain[y][x] = terrainIndex(action.terrain);
                            }
                            if (action.type==JSONIndex(gameData.getJSONArray("tasks"), "Construction Mid")) {
                                LOGGER_GAME.finer("Action reached mid phase so changing building to mid");
                                buildings[y][x].setImageId(1);
                                action = null;
                            } else if (action.type==JSONIndex(gameData.getJSONArray("tasks"), "Construction End")) {
                                LOGGER_GAME.finer("Action reached end phase so changing building to end");
                                buildings[y][x].setImageId(2);
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

    private void processBattles() {
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
                                LOGGER_GAME.fine(String.format("Battle ended at:(%d, %d) winner=%s", x, y, str(parties[y][x].player+1)));
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
                                LOGGER_GAME.fine(String.format("Siege ended at:(%d, %d) winner=%s", x, y, str(parties[y][x].player+1)));
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

    private void turnChange() {
        try {
            LOGGER_GAME.finer(String.format("Turn changing - current player = %s, next player = %s", turn, (turn+1)%players.length));
            notificationManager.dismissAll();
            autoMoveParties();
            processParties();
            boolean starving = players[turn].resources[getResIndex("food")] == 0;
            productionManager.updateResources(productionManager.getResourceProductivities(productionManager.getTotalResourceRequirements()), starving);
            partyMovementPointsReset();
            LOGGER_GAME.finer("Loading other player camera positions");
            float blockSize;
            if (map.isZooming()) {
                blockSize = map.getTargetZoom();
            } else {
                blockSize = map.getZoom();
            }
            float tempX, tempY;
            if (JSONManager.loadBooleanSetting("map is 3d")) {
                tempX = (map.getTargetOffsetX()+papplet.width/2f)/((Map3D)map).blockSize;
                tempY = (map.getTargetOffsetY()+papplet.height/2f)/((Map3D)map).blockSize;
            } else {
                tempX = (papplet.width/2f-map.getTargetOffsetX()-((Map2D)map).xPos)/((Map2D)map).targetBlockSize;
                tempY = (papplet.height/2f-map.getTargetOffsetY()-((Map2D)map).yPos)/((Map2D)map).targetBlockSize;
            }
            players[turn].saveSettings(tempX, tempY, blockSize, selectedCellX, selectedCellY, cellSelected);
            int tempPlayerCount;
            if (loadBooleanSetting("bandits")) {
                tempPlayerCount = playerCount;
            } else {
                tempPlayerCount = playerCount-1;
            }
            turn = (turn + 1)%tempPlayerCount; // TURN CHANGE

            // If local player turn disable cinematic mode otherwise enable (e.g. bandits/AI turn)
            if (players[turn].controllerType == 0) {
                leaveCinematicMode(); // Leave cinematic mode as player turn
            } else {
                enterCinematicMode(); // Leave cinematic mode as player turn
            }

            players[turn].loadSettings(this, map);
            changeTurn = false;
            SingleLineTextBox t = ((SingleLineTextBox)(getElement("turn number", "bottom bar")));
            t.setColour(players[turn].colour);
            t.setText("Turn "+turnNumber);
            productionManager.updateResourcesSummary(players[turn].resources[getResIndex("food")] == 0);
            notificationManager.turnChange(turn);

            if (turn==0) {
                turnNumber++;
                if (JSONManager.loadBooleanSetting("bandits")) {
                    spawnBandits();
                }
            }

            processBattles();

            if (anyIdle(turn)) {
                LOGGER_GAME.finest("Idle party set to red becuase idle parties found");
                ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
            } else {
                LOGGER_GAME.finest("Idle party set to grey becuase no idle parties found");
                ((Button)getElement("idle party finder", "bottom bar")).setColour(papplet.color(150));
            }
            if (checkForPlayerWin()) {
                this.getPanel("end screen").setVisible(true);
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

    private void spawnBandits() {
        int banditCount = 0;
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                if (parties[y][x] != null && parties[y][x].containsPartyFromPlayer(playerCount) > 0) {
                    banditCount++;
                }
            }
        }
        if (random(0, (gameData.getJSONObject("game options").getFloat("bandits per tile")*mapWidth*mapHeight)) > banditCount) {
            ArrayList<int[]> possibleTiles = new ArrayList<>();
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
                    boolean seenTile = false;
                    for (Player p: players) {
                        if (p.controllerType != 1 && p.visibleCells[y][x] != null && p.visibleCells[y][x].activeSight) {
                            seenTile = true;
                        }
                    }
                    if (!seenTile) {
                        possibleTiles.add(new int[]{x, y});
                    }
                }
            }
            if (possibleTiles.size()>0) {
                int chosenTile = floor(random(0, possibleTiles.size()));
                int x = possibleTiles.get(chosenTile)[0];
                int y = possibleTiles.get(chosenTile)[1];
                parties[y][x] = new Party(playerCount, PApplet.parseInt(JSONManager.loadIntSetting("party size") / 2f), 0, gameData.getJSONObject("game options").getInt("movement points"), nextRollingId(players[playerCount].name));
            }
        }
    }

    private boolean checkForPlayerWin() {
        if (winner == -1) {
            boolean[] playersAlive = new boolean[playerCount];


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
            for (int p=0; p < playerCount; p++) {
                players[p].isAlive = playersAlive[p];
                if (playersAlive[p]) {
                    numAlive ++;
                }
            }
            if (numAlive == 1) {
                for (int p=0; p < playerCount; p++) {
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
            if (panels.get(i).isVisible()) {
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

        if (tooltip.isVisible()&&tooltip.attacking) {
            int x = floor(map.scaleXInv());
            int y = floor(map.scaleYInv());
            if (0<=x&&x<mapWidth&&0<=y&&y<mapHeight && parties[y][x] != null && parties[selectedCellY][selectedCellX] != null && parties[y][x].player != parties[selectedCellY][selectedCellX].player && map.mouseOver() && UINotHovering()) {
                BigDecimal chance = battleEstimateManager.getEstimate(selectedCellX, selectedCellY, x, y, splitUnitsNum());
                tooltip.setAttacking(chance);
            } else {
                tooltip.attacking=false;
            }
        }

        if (players[0].resources[JSONManager.getResIndex(("rocket progress"))]!=-1||players[1].resources[JSONManager.getResIndex(("rocket progress"))]!=-1) {
            drawRocketProgressBar(gameUICanvas);
        }
        if (cellSelected) {
            if (getPanel("land management").isVisible()) {
                drawCellManagement(gameUICanvas);
            }
            if (parties[selectedCellY][selectedCellX] != null && getPanel("party management").isVisible())
                drawPartyManagement(gameUICanvas);
        }
        gameUICanvas.endDraw();
        gameUICanvas.popStyle();
        papplet.image(gameUICanvas, 0, 0);

        //Tooltips are going to be added here so don't delete this. From here
        ResourceSummary resSum = ((ResourceSummary)getElement("resource summary", "bottom bar"));
        if (resSum.pointOver()) {
            String resource = resSum.getResourceAt(papplet.mouseX, papplet.mouseY);
            HashMap<String, Float> tasksMap = new HashMap<>();
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
            this.getPanel("end screen").setVisible(true);
        }

        // Process AI and bandits turns
        if (players[turn].playerController != null) {  // Local players have null for playerController
            postEvent(players[turn].generateNextEvent());
        }
    }

    private void partyMovementPointsReset() {
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

    private void changeTurn() {
        changeTurn = true;
    }

    public int[] newPartyLoc() {
        // Unused
        try {
            ArrayList<int[]> locs = new ArrayList<>();
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
    private boolean canMove(int x, int y) {
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

    private boolean inPrevIdle(int x, int y) {
        for (Integer[] integers : prevIdle) {
            if (integers[0] == x && integers[1] == y) {
                return true;
            }
        }
        return false;
    }
    private void clearPrevIdle() {
        prevIdle.clear();
    }
    private boolean isIdle(int x, int y) {
        try {
            return (parties[y][x].task == JSONIndex(gameData.getJSONArray("tasks"), "Rest") && canMove(x, y) && (parties[y][x].path==null||parties[y][x].path!=null&&parties[y][x].path.size()==0));
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error with indices in new party loc", e);
            return false;
        }
    }

    private int[] findIdle(int player) {
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

    private void saveGame() {
        float blockSize;
        float x, y;
        if (JSONManager.loadBooleanSetting("map is 3d")) {
            blockSize = 0.66f*exp(pow(0.9f, 2.8f*log(map.getZoom()/100000)));
            x = (map.getFocusedX()+papplet.width/2f)/((Map3D)map).blockSize;
            y = (map.getFocusedY()+papplet.height/2f)/((Map3D)map).blockSize;
        } else {
            if (map.isZooming()) {
                blockSize = map.getTargetZoom();
            } else {
                blockSize = map.getZoom();
            }
            x = (papplet.width/2f-map.getFocusedX()-((Map2D)map).xPos)/((Map2D)map).blockSize;
            y = (papplet.height/2f-map.getFocusedY()-((Map2D)map).yPos)/((Map2D)map).blockSize;
        }
        players[turn].saveSettings(x, y, blockSize, selectedCellX, selectedCellY, cellSelected);
        ((BaseMap)map).saveMap("saves/"+loadingName, turnNumber, turn, this.players);
    }

    public void elementEvent(ArrayList<Event> events) {
        for (Event event : events) {
            if (event.type.equals("clicked")) {
                switch (event.id) {
                    case "idle party finder":
                        targetIdle();
                        break;
                    case "end turn":
                        if (((Button) getElement("end turn", "bottom bar")).getText().equals("Advance Units")) {
                            autoMoveParties();
                            ((Button) getElement("end turn", "bottom bar")).setText("Next Turn");
                        } else {
                            postEvent(new EndTurn());
                        }
                        break;
                    case "move button":
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
                        break;
                    case "resource expander": {
                        ResourceSummary r = ((ResourceSummary) (getElement("resource summary", "bottom bar")));
                        Button b1 = ((Button) (getElement("resource expander", "bottom bar")));
                        Button b2 = ((Button) (getElement("resource detailed", "bottom bar")));
                        r.toggleExpand();
                        b1.transform(papplet.width - r.totalWidth() - 50, BEZEL * 2 + 25, 30, 25);
                        b2.transform(papplet.width - r.totalWidth() - 50, BEZEL, 30, 25);
                        if (b1.getText().equals(">"))
                            b1.setText("<");
                        else
                            b1.setText(">");
                        break;
                    }
                    case "resource detailed": {
                        getPanel("resource management").setVisible(!getPanel("resource management").isVisible());
                        ResourceManagementTable r = ((ResourceManagementTable) (getElement("resource management table", "resource management")));
                        Button b = ((Button) (getElement("resource detailed", "bottom bar")));
                        ArrayList<ArrayList<String>> names = new ArrayList<>();
                        names.add(new ArrayList<>());
                        names.add(new ArrayList<>());
                        ArrayList<ArrayList<Float>> production = new ArrayList<>();
                        production.add(new ArrayList<>());
                        production.add(new ArrayList<>());
                        ArrayList<ArrayList<Float>> consumption = new ArrayList<>();
                        consumption.add(new ArrayList<>());
                        consumption.add(new ArrayList<>());
                        ArrayList<ArrayList<Float>> net = new ArrayList<>();
                        net.add(new ArrayList<>());
                        net.add(new ArrayList<>());
                        ArrayList<ArrayList<Float>> storage = new ArrayList<>();
                        storage.add(new ArrayList<>());
                        storage.add(new ArrayList<>());
                        float[] totalResourceRequirements = productionManager.getTotalResourceRequirements();
                        float[] resourceProductivities = productionManager.getResourceProductivities(totalResourceRequirements);
                        boolean starving = players[turn].resources[getResIndex("food")] == 0;
                        float[] gross = productionManager.getTotalResourceProductions(resourceProductivities, starving);
                        float[] costs = productionManager.getTotalResourceConsumptions(resourceProductivities, starving);
                        float[] totals = productionManager.getTotalResourceChanges(gross, costs);
                        for (int i = 0; i < players[turn].resources.length; i++) {
                            if (players[turn].resources[i] > 0) {
                                int page;
                                if (JSONManager.resourceIsEquipment(i)) {
                                    page = 1;
                                } else {
                                    page = 0;
                                }
                                names.get(page).add(JSONManager.getResString(i));
                                production.get(page).add(gross[i]);
                                consumption.get(page).add(costs[i]);
                                net.get(page).add(totals[i]);
                                storage.get(page).add(players[turn].resources[i]);
                            }
                        }
                        r.update(new String[][]{{"Resource", "Making", "Use    ", "Net   ", "Storage"}, {"Equipment type", "Class   ", "Making", "Use    ", "Net   ", "Storage"}}, names, production, consumption, net, storage);

                        // This should be changed to use a graphic instead of a character
                        if (b.getText().equals("^"))
                            b.setText("v");
                        else
                            b.setText("^");
                        break;
                    }
                    case "end game button":
                    case "main menu button":
                        // old save place
                        JSONManager.writeSettings();
                        newState = "menu";
                        break;
                    case "desktop button":
                        JSONManager.writeSettings();
                        quitGame();
                        break;
                    case "resume button":
                        getPanel("pause screen").setVisible(false);
                        getPanel("save screen").setVisible(false);
                        // Enable map
                        getElement("2dmap", "default").active = true;
                        getElement("3dmap", "default").active = true;
                        break;
                    case "save as button":
                        // Show the save menu
                        getPanel("save screen").setVisible(!getPanel("save screen").isVisible());
                        if (loadingName == null) {
                            loadingName = ((BaseFileManager) getElement("saving manager", "save screen")).getNextAutoName(); // Autogen name
                        }
                        ((TextEntry) getElement("save namer", "save screen")).setText(loadingName);
                        break;
                    case "save button":
                        loadingName = ((TextEntry) getElement("save namer", "save screen")).getText();
                        saveGame();
                        ((BaseFileManager) getElement("saving manager", "save screen")).loadSaveNames();
                        break;
                    case "disband button":
                        postEvent(new DisbandParty(selectedCellX, selectedCellY));
                        break;
                    case "stock up button":
                        postEvent(new StockUpEquipment(selectedCellX, selectedCellY));
                        break;
                    case "bombardment button":
                        bombarding = !bombarding;
                        moving = false;
                        map.cancelMoveNodes();
                        if (bombarding) {
                            map.enableBombard(gameData.getJSONArray("equipment").getJSONObject(1).getJSONArray("types").getJSONObject(parties[selectedCellY][selectedCellX].equipment[1]).getInt("range"));
                        } else {
                            map.disableBombard();
                        }
                        break;
                }
            }
            if (event.type.equals("valueChanged")) {
                switch (event.id) {
                    case "tasks":
                        postEvent(new ChangeTask(selectedCellX, selectedCellY, JSONIndex(gameData.getJSONArray("tasks"), ((TaskChooser) getElement("tasks", "party management")).getSelected())));
                        break;
                    case "unit number bars toggle":
                        map.setDrawingUnitBars(((ToggleButton) (getElement("unit number bars toggle", "bottom bar"))).getState());
                        LOGGER_MAIN.fine("Unit number bars visibility changed");
                        break;
                    case "task icons toggle":
                        map.setDrawingTaskIcons(((ToggleButton) (getElement("task icons toggle", "bottom bar"))).getState());
                        LOGGER_MAIN.fine("Task icons bars visibility changed");
                        break;
                    case "2d 3d toggle":
                        // Save the game for toggle between 2d and 3d
                        LOGGER_MAIN.info("Toggling between 2d and 3d, so saving now to 'Autosave.dat'");
                        loadingName = "Autosave.dat";
                        bombarding = false;
                        moving = false;
                        saveGame();
                        JSONManager.saveSetting("map is 3d", ((ToggleButton) (getElement("2d 3d toggle", "bottom bar"))).getState());
                        LOGGER_MAIN.info("Reloading map in new dimension");
                        reloadGame();
                        LOGGER_MAIN.fine("Finished reloading map");
                        break;
                    case "saving manager":
                        loadingName = ((BaseFileManager) getElement("saving manager", "save screen")).selectedSaveName();
                        LOGGER_MAIN.fine("Changing selected save name to: " + loadingName);
                        ((TextEntry) getElement("save namer", "save screen")).setText(loadingName);
                        break;
                    case "party training focus":
                        postEvent(new ChangePartyTrainingFocus(selectedCellX, selectedCellY, ((DropDown) getElement("party training focus", "party management")).getOptionIndex()));
                        break;
                    case "auto stock up toggle":
                        postEvent(new SetAutoStockUp(selectedCellX, selectedCellY, ((ToggleButton) getElement("auto stock up toggle", "party management")).getState()));
                        break;
                    case "equipment manager":
                        for (int[] equipmentChange : ((EquipmentManager) getElement("equipment manager", "party management")).getEquipmentToChange()) {
                            if (equipmentChange[0] >= 0)
                                postEvent(new ChangeEquipment(equipmentChange[0], equipmentChange[1]));
                        }
                        updatePartyManagementProficiencies();
                        break;
                    case "unit cap incrementer":
                        postEvent(new UnitCapChange(selectedCellX, selectedCellY, ((IncrementElement) getElement("unit cap incrementer", "party management")).getValue()));
                        break;
                    case "resources pages button":
                        HorizontalOptionsButton b = ((HorizontalOptionsButton) getElement("resources pages button", "resource management"));
                        ((ResourceManagementTable) getElement("resource management table", "resource management")).setPage(b.selected);
                        break;
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

    private void targetIdle() {
        int[] t = findIdle(turn);
        if (t!=null&&t.length>0&&t[0] != -1) {
            selectCell(t[0], t[1], false);
            map.targetCell(t[0], t[1], 64);
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
        //map.setWidth(round(papplet.width-BEZEL*2));
        ((Text)getElement("turns remaining", "party management")).setText("");
    }

    private void moveParty(int px, int py) {
        moveParty(px, py, false);
    }

    private void moveParty(int px, int py, boolean splitting) {
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
                            notificationManager.post("Parties Merged", path.get(node)[0], path.get(node)[1], turnNumber, turn);
                            int overflow = parties[path.get(node)[1]][path.get(node)[0]].mergeEntireFrom(p, cost, players[turn]);

                            LOGGER_GAME.fine(String.format("Parties merged at (%d, %d) from party with id: %s to party with id:%s. Overflow:%d",
                                    path.get(node)[0], path.get(node)[1], p.getID(), parties[path.get(node)[1]][path.get(node)[0]].getID(), overflow));

                            if (cellFollow) {
                                selectCell(path.get(node)[0], path.get(node)[1], false);
                                stillThere = false;
                            }

                            if (overflow == 0) {
                                if (splitting) {
                                    splittedParty = null;
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
                                notificationManager.post("Battle Reinforced", path.get(node)[0], path.get(node)[1], turnNumber, turn);
                                int overflow = ((Battle) parties[path.get(node)[1]][path.get(node)[0]]).changeUnitNumber(turn, p.getUnitNumber());
                                LOGGER_GAME.fine(String.format("Battle reinforced at cell:(%d, %d). Merging party id:%s. Overflow:%d", path.get(node)[0], path.get(node)[1], p.getID(), overflow));

                                if (cellFollow) {
                                    selectCell(path.get(node)[0], path.get(node)[1], false);
                                    stillThere = false;
                                }

                                if (splitting) {
                                    splittedParty = null;
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
                                    if (parties[y][x].player == -1) {
                                        notificationManager.post("Siege Ended. However the battle continues.", x, y, turnNumber, turn);
                                        notificationManager.post("Siege Ended. However the battle continues.", x, y, turnNumber, otherPlayer);
                                        LOGGER_GAME.fine(String.format("Siege ended at cell: (%d, %d). Battle remains", x, y));
                                    } else {
                                        notificationManager.post("Siege Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, turn);
                                        notificationManager.post("Siege Ended. Player "+str(parties[y][x].player+1)+" won", x, y, turnNumber, otherPlayer);
                                        parties[y][x].trainParty("melee attack", "winning battle melee");
                                        parties[y][x].trainParty("defence", "winning battle defence");
                                        LOGGER_GAME.fine(String.format("Siege ended at cell: (%d, %d). Units remaining: %d", x, y, parties[y][x].getUnitNumber()));
                                    }
                                }
                                if (cellFollow) {
                                    selectCell(path.get(node)[0], path.get(node)[1], false);
                                    stillThere = false;
                                }
                                if (splitting) {
                                    splittedParty = null;
                                } else {
                                    parties[py][px] = null;
                                }
                                if (buildings[path.get(node)[1]][path.get(node)[0]]!=null&&buildings[path.get(node)[1]][path.get(node)[0]].getType()==0) {
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
                                    LOGGER_GAME.fine(String.format("Battle ended at cell: (%d, %d). Units remaining: %d", x, y, parties[y][x].getUnitNumber()));
                                }
                                if (cellFollow) {
                                    selectCell(path.get(node)[0], path.get(node)[1], false);
                                    stillThere = false;
                                }
                                if (splitting) {
                                    splittedParty = null;
                                } else {
                                    parties[py][px] = null;
                                }
                                if (buildings[path.get(node)[1]][path.get(node)[0]]!=null&&buildings[path.get(node)[1]][path.get(node)[0]].getType()==0) {
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
                    p.path = new ArrayList<>(path.subList(i, path.size()));
                    break;
                }
                if (tx==px&&ty==py) {
                    p.clearPath();
                }
            }

            if (cellFollow&&stillThere) {
                selectCell(px, py, false);
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

    private int getMoveTurns(int startX, int startY, int targetX, int targetY, Node[][] nodes) {
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

    private int splitUnitsNum() {
        return round(((Slider)getElement("split units", "party management")).getValue());
    }

    private void refreshTooltip() {
        if (players[turn].controllerType != 1) {
            LOGGER_MAIN.fine("refreshing tooltip");
            if (!getPanel("pause screen").isVisible()) {
                TaskChooser tasks = ((TaskChooser)getElement("tasks", "party management"));
                if (((EquipmentManager)getElement("equipment manager", "party management")).mouseOverTypes() && getPanel("party management").isVisible()) {
                    int hoveringType = ((EquipmentManager)getElement("equipment manager", "party management")).hoveringOverType();
                    int equipmentClass = ((EquipmentManager)getElement("equipment manager", "party management")).getSelectedClass();
                    tooltip.setEquipment(equipmentClass, hoveringType, players[turn].resources, parties[selectedCellY][selectedCellX], productionManager.isEquipmentCollectionAllowed(selectedCellX, selectedCellY, equipmentClass, parties[selectedCellY][selectedCellX].getEquipment(equipmentClass)));
                } else if (tasks.moveOver() && getPanel("party management").isVisible() && !tasks.scrolling && !tasks.hovingOverScroll() && tasks.active) {
                    tooltip.setTask(((TaskChooser)getElement("tasks", "party management")).findMouseOver(), players[turn].resources, parties[selectedCellY][selectedCellX].getMovementPoints());
                    tooltip.show();
                } else if (getElement("proficiency summary", "party management").mouseOver() && getPanel("party management").isVisible()) {
                    tooltip.setProficiencies(((ProficiencySummary)getElement("proficiency summary", "party management")).hoveringOption(), parties[selectedCellY][selectedCellX]);
                } else if (getElement("turns remaining", "party management").mouseOver()&& getPanel("party management").isVisible()) {
                    tooltip.setTurnsRemaining();
                } else if (getElement("move button", "party management").mouseOver()&& getPanel("party management").isVisible()) {
                    tooltip.setMoveButton();
                } else if (getElement("stock up button", "party management").mouseOver() && getPanel("party management").isVisible()) {
                    if (((Button) getElement("stock up button", "party management")).active) {
                        tooltip.setStockUpAvailable(parties[selectedCellY][selectedCellX], players[turn].resources);
                    } else {
                        tooltip.setStockUpUnavailable(parties[selectedCellY][selectedCellX]);
                    }
                } else if (getElement("resource summary", "bottom bar").mouseOver()) {
                    String resource = ((ResourceSummary)getElement("resource summary", "bottom bar")).getResourceUnderMouse();
                    tooltip.setResource(new HashMap<>(), resource);
                } else if (map.mouseOver()) {
                    Cell[][] visibleCells = players[turn].visibleCells;
                    map.doUpdateHoveringScale();
                    int mapInterceptX = floor(map.scaleXInv());
                    int mapInterceptY = floor(map.scaleYInv());
                    if (moving && UINotHovering()) {
                        Node [][] nodes = map.getMoveNodes();
                        if (selectedCellX == mapInterceptX && selectedCellY == mapInterceptY) {
                            tooltip.hide();
                            map.cancelPath();
                        } else if (mapInterceptX < mapWidth && mapInterceptY<mapHeight && mapInterceptX>=0 && mapInterceptY>=0 && nodes[mapInterceptY][mapInterceptX] != null) {
                            if (parties[selectedCellY][selectedCellX] != null) {
                                map.updatePath(getPath(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, map.getMoveNodes()));
                                map.updatePath(getPath(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, map.getMoveNodes()));
                            }
                            if (visibleCells[mapInterceptY][mapInterceptX] == null || visibleCells[mapInterceptY][mapInterceptX].getParty() == null) {
                                //Moving into empty tile
                                int turns = getMoveTurns(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, nodes);
                                int cost = nodes[mapInterceptY][mapInterceptX].cost;
                                boolean splitting = splitUnitsNum()!=parties[selectedCellY][selectedCellX].getUnitNumber();
                                tooltip.setMoving(turns, splitting, parties[selectedCellY][selectedCellX], splitUnitsNum(), cost, JSONManager.loadBooleanSetting("map is 3d"));
                            } else {
                                if (visibleCells[mapInterceptY][mapInterceptX].getParty().player == turn) {
                                    //merge parties
                                    tooltip.setMerging(visibleCells[mapInterceptY][mapInterceptX].getParty(), visibleCells[selectedCellY][selectedCellX].getParty(), splitUnitsNum());
                                } else if (visibleCells[mapInterceptY][mapInterceptX].getBuilding() != null && visibleCells[mapInterceptY][mapInterceptX].getBuilding().getDefence() > 0) {
                                    //Siege
                                    tooltip.setSieging();
                                } else if (!(visibleCells[mapInterceptY][mapInterceptX].getParty() instanceof Battle) || visibleCells[mapInterceptY][mapInterceptX].getParty().containsPartyFromPlayer(turn) > 0) {
                                    //Attack
                                    BigDecimal chance = battleEstimateManager.getEstimate(selectedCellX, selectedCellY, mapInterceptX, mapInterceptY, splitUnitsNum());
                                    tooltip.setAttacking(chance);
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
                        }
                    } else if (!moving && 0 < mapInterceptY && mapInterceptY < mapHeight && 0 < mapInterceptX && mapInterceptX < mapWidth && !(parties[mapInterceptY][mapInterceptX] instanceof Battle) && parties[mapInterceptY][mapInterceptX] != null) {
                        if (!JSONManager.loadBooleanSetting("fog of war") || (players[turn].visibleCells[mapInterceptY][mapInterceptX] != null && players[turn].visibleCells[mapInterceptY][mapInterceptX].party != null)) {
                            // Hovering over party
                            tooltip.setHoveringParty(parties[mapInterceptY][mapInterceptX]);
                        }
                    }
                } else {
                    map.cancelPath();
                    tooltip.hide();
                }
                map.setActive(UINotHovering());
            }
        }
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        refreshTooltip();
        if (button == RIGHT) {
            if (eventType.equals("mousePressed")) {
                if (parties[selectedCellY][selectedCellX] != null && parties[selectedCellY][selectedCellX].player == turn && cellSelected && UINotHovering()) {
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
            if (eventType.equals("mouseReleased")) {
                if (parties[selectedCellY][selectedCellX] != null && parties[selectedCellY][selectedCellX].player == turn && UINotHovering()) {
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
            if (eventType.equals("mousePressed") && !bombarding) {
                mapClickPos = new int[]{papplet.mouseX, papplet.mouseY, papplet.millis()};
            }
            if (eventType.equals("mouseReleased")) {
                if (bombarding) {
                    int x = floor(map.scaleXInv());
                    int y = floor(map.scaleYInv());
                    if (activePanel.equals("default") && UINotHovering() && 0<=x&&x<mapWidth&&0<=y&&y<mapHeight) {
                        postEvent(new Bombard(selectedCellX, selectedCellY, x, y));
                        map.disableBombard();
                        bombarding = false;
                    }
                }
            }
            int CLICKHOLD = 500;
            int MOUSEPRESSTOLERANCE = 100;
            if (eventType.equals("mouseReleased") && mapClickPos != null && sqrt(pow(mapClickPos[0] - papplet.mouseX, 2) + pow(mapClickPos[1] - papplet.mouseY, 2))< MOUSEPRESSTOLERANCE && papplet.millis() - mapClickPos[2] < CLICKHOLD) { // Custom mouse click
                mapClickPos = null;
                if (activePanel.equals("default") && UINotHovering()) {
                    if (map.mouseOver()) {
                        if (moving) {
                            //int x = floor(map.scaleXInv(mouseX));
                            //int y = floor(map.scaleYInv(mouseY));
                            //postEvent(new Move(selectedCellX, selectedCellY, x, y));
                            //map.cancelPath();
                            if (papplet.mousePressed) {
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
        return new ArrayList<>();
    }
    private void selectCell() {
        // select cell based on mouse pos
        int x = floor(map.scaleXInv());
        int y = floor(map.scaleYInv());
        selectCell(x, y, false);
    }

    private boolean cellInBounds(int x, int y) {
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
            //map.setWidth(round(papplet.width-BEZEL*2-400));
            if (players[turn].visibleCells[y][x] != null){
                getPanel("land management").setVisible(true);
            } else {
                getPanel("land management").setVisible(false);
            }

            updatePartyManagementInterface();
        }
    }

    private void updatePartyManagementInterface() {
        if (parties[selectedCellY][selectedCellX] != null && (parties[selectedCellY][selectedCellX].isTurn(turn) || JSONManager.loadBooleanSetting("show all party managements"))) {
            if (parties[selectedCellY][selectedCellX].getTask() != JSONIndex(gameData.getJSONArray("tasks"), "Battle") && parties[selectedCellY][selectedCellX].isTurn(turn)) {
                getElement("split units", "party management").show();
                ((TaskChooser)getElement("tasks", "party management")).active = true;
                getElement("tasks", "party management").show();
                getElement("task text", "party management").show();
            } else {
                getElement("split units", "party management").hide();
                ((TaskChooser)getElement("tasks", "party management")).active = false;
                getElement("tasks", "party management").hide();
                getElement("task text", "party management").hide();
            }
            getPanel("party management").setVisible(true);
            if (parties[selectedCellY][selectedCellX].getUnitNumber() <= 1) {
                getElement("split units", "party management").hide();
            } else {
                ((Slider)getElement("split units", "party management")).setScale(1, parties[selectedCellY][selectedCellX].getUnitNumber(), parties[selectedCellY][selectedCellX].getUnitNumber(), 1, parties[selectedCellY][selectedCellX].getUnitNumber()/2);
            }

            partyManagementColour = brighten(playerColours[turn], -80); // Top
            getPanel("party management").setColour(brighten(playerColours[turn], 70)); // Background

            if (productionManager.isEquipmentCollectionAllowed(selectedCellX, selectedCellY)) {
                ((Button)getElement("stock up button", "party management")).bgColour = papplet.color(150);
                ((Button)getElement("stock up button", "party management")).textColour = papplet.color(0);
                getElement("stock up button", "party management").activate();
            } else {
                ((Button)getElement("stock up button", "party management")).bgColour = papplet.color(210);
                ((Button)getElement("stock up button", "party management")).textColour = papplet.color(100);
                getElement("stock up button", "party management").deactivate();
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

    private void updateBombardment() {
        getElement("bombardment button", "party management").setVisible(parties[selectedCellY][selectedCellX].equipment[1] != -1 &&
                gameData.getJSONArray("equipment").getJSONObject(1).getJSONArray("types").getJSONObject(parties[selectedCellY][selectedCellX].equipment[1]).hasKey("range") &&
                parties[selectedCellY][selectedCellX].equipmentQuantities[1] > 0 && parties[selectedCellY][selectedCellX].getMovementPoints() > 0);
    }

    private void updateUnitCapIncrementer() {
        ((IncrementElement)getElement("unit cap incrementer", "party management")).setUpper(JSONManager.loadIntSetting("party size"));
        ((IncrementElement)getElement("unit cap incrementer", "party management")).setLower(parties[selectedCellY][selectedCellX].getUnitNumber());
        ((IncrementElement)getElement("unit cap incrementer", "party management")).setValue(parties[selectedCellY][selectedCellX].getUnitCap());
    }

    private void updatePartyManagementProficiencies() {
        // Update proficiencies with those for current party
        ((ProficiencySummary)getElement("proficiency summary", "party management")).setProficiencies(parties[selectedCellY][selectedCellX].getRawProficiencies());
        ((ProficiencySummary)getElement("proficiency summary", "party management")).setProficiencyBonuses(parties[selectedCellY][selectedCellX].getRawBonusProficiencies());
    }

    private void updateCurrentPartyTrainingFocus() {
        int trainingFocus = parties[selectedCellY][selectedCellX].getTrainingFocus();
        ((DropDown)getElement("party training focus", "party management")).setValue(JSONManager.indexToProficiencyDisplayName(trainingFocus));
    }

    private void drawPartyManagement(PGraphics panelCanvas) {
        Panel pp = getPanel("party management");
        panelCanvas.pushStyle();
        panelCanvas.fill(partyManagementColour);
        panelCanvas.rect(sidePanelX, pp.y, sidePanelW, 13*JSONManager.loadFloatSetting("text scale"));
        panelCanvas.fill(255);
        panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER, TOP);
        panelCanvas.text("Party Management", sidePanelX+sidePanelW/2f, pp.y);

        panelCanvas.fill(0);
        panelCanvas.textAlign(LEFT, CENTER);
        panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
        float barY = sidePanelY + 13*JSONManager.loadFloatSetting("text scale") + sidePanelH*0.15f + BEZEL*2;
        if (JSONManager.loadBooleanSetting("show party id")) {
            panelCanvas.text("Party id: "+parties[selectedCellY][selectedCellX].id, 120+sidePanelX, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
        }
        panelCanvas.text("Movement Points Remaining: "+parties[selectedCellY][selectedCellX].getMovementPoints(turn) + "/"+parties[selectedCellY][selectedCellX].getMaxMovementPoints(), 120+sidePanelX, barY);
        barY += 13*JSONManager.loadFloatSetting("text scale");

        if (JSONManager.loadBooleanSetting("show all party managements")&&parties[selectedCellY][selectedCellX].player==-1) {
            String t1 = ((Battle)parties[selectedCellY][selectedCellX]).attacker.id;
            String t2 = "Units: "+((Battle)parties[selectedCellY][selectedCellX]).attacker.getUnitNumber() + "/" + JSONManager.loadIntSetting("party size");
            float offset = max(panelCanvas.textWidth(t1+" "), panelCanvas.textWidth(t2+" "));
            panelCanvas.text(t1, 120+sidePanelX, barY);
            panelCanvas.text(((Battle)parties[selectedCellY][selectedCellX]).defender.id, 120+sidePanelX+offset, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
            panelCanvas.text(t2, 120+sidePanelX, barY);
            panelCanvas.text("Units: "+((Battle)parties[selectedCellY][selectedCellX]).defender.getUnitNumber() + "/" + JSONManager.loadIntSetting("party size"), 120+sidePanelX+offset, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
        } else {
            panelCanvas.text("Units: "+parties[selectedCellY][selectedCellX].getUnitNumber(turn) + "/" + JSONManager.loadIntSetting("party size"), 120+sidePanelX, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
        }

        if (parties[selectedCellY][selectedCellX].pathTurns > 0) {
            ((Text)getElement("turns remaining", "party management")).setText("Turns Remaining: "+ parties[selectedCellY][selectedCellX].pathTurns);
        } else if (parties[selectedCellY][selectedCellX].actions.size() > 0 && parties[selectedCellY][selectedCellX].actions.get(0).initialTurns > 0) {
            ((Text)getElement("turns remaining", "party management")).setText("Turns Remaining: "+parties[selectedCellY][selectedCellX].turnsLeft() + "/"+round(parties[selectedCellY][selectedCellX].calcTurns(parties[selectedCellY][selectedCellX].actions.get(0).initialTurns)));
        }

        if (!((Text)getElement("turns remaining", "party management")).text.equals("")) {
            ((Text)getElement("turns remaining", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
            barY += 13*JSONManager.loadFloatSetting("text scale");
        }

        ((Slider)getElement("split units", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        barY += ((Slider)getElement("split units", "party management")).h;
        barY += ((Button)getElement("stock up button", "party management")).h;

        ((Button)getElement("stock up button", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        ((ToggleButton)getElement("auto stock up toggle", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        ((IncrementElement)getElement("unit cap incrementer", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        barY += ((Button)getElement("stock up button", "party management")).h + BEZEL;

        ((EquipmentManager)getElement("equipment manager", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        barY += ((EquipmentManager)getElement("equipment manager", "party management")).getBoxHeight();

        ((Text)getElement("task text", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        ((Text)getElement("proficiencies", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        barY += ((Text)getElement("proficiencies", "party management")).h;

        ((TaskChooser)getElement("tasks", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        ((ProficiencySummary)getElement("proficiency summary", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
        barY += ((ProficiencySummary)getElement("proficiency summary", "party management")).h;
        ((DropDown)getElement("party training focus", "party management")).y = PApplet.parseInt(barY) - getPanel("party management").y;
    }

    private void drawCellManagement(PGraphics panelCanvas) {
        panelCanvas.pushStyle();
        panelCanvas.fill(0, 150, 0);
        panelCanvas.rect(sidePanelX, sidePanelY, sidePanelW, 13*JSONManager.loadFloatSetting("text scale"));
        panelCanvas.fill(255);
        panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER, TOP);
        panelCanvas.text("Land Management", sidePanelX+sidePanelW/2f, sidePanelY);

        panelCanvas.fill(0);
        panelCanvas.textAlign(LEFT, TOP);
        float barY = sidePanelY + 13*JSONManager.loadFloatSetting("text scale");
        if (JSONManager.loadBooleanSetting("show cell coords")) {
            panelCanvas.text(String.format("Cell reference: %s, %s", selectedCellX, selectedCellY), 5+sidePanelX, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
        }
        panelCanvas.text("Cell Type: "+gameData.getJSONArray("terrain").getJSONObject(terrain[selectedCellY][selectedCellX]).getString("display name"), 5+sidePanelX, barY);
        barY += 13*JSONManager.loadFloatSetting("text scale");
        if (buildings[selectedCellY][selectedCellX] != null) {
            if (buildings[selectedCellY][selectedCellX].getType()!= 0)
                panelCanvas.text("Building: "+buildingTypes[buildings[selectedCellY][selectedCellX].getType()], 5+sidePanelX, barY);
            else
                panelCanvas.text("Building: Construction Site", 5+sidePanelX, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
        }
        boolean starving = players[turn].resources[getResIndex("food")] == 0;
        float[] resourceProductivities = productionManager.getResourceProductivities(productionManager.getTotalResourceRequirements());
        float[] production = productionManager.resourceProductionAtCell(selectedCellX, selectedCellY, resourceProductivities, starving);
        float[] consumption = productionManager.getResourceConsumptionAtCell(selectedCellX, selectedCellY, resourceProductivities, starving);
        String pl = resourceManager.resourcesList(production);
        String cl = resourceManager.resourcesList(consumption);
        panelCanvas.fill(0);
        if (!pl.equals("Nothing/Unknown")) {
            panelCanvas.text("Producing: "+pl, 5+sidePanelX, barY);
            barY += 13*JSONManager.loadFloatSetting("text scale");
        }
        if (!cl.equals("Nothing/Unknown")) {
            panelCanvas.fill(255, 0, 0);
            panelCanvas.text("Consuming: "+cl, 5+sidePanelX, barY);
            //barY += 13*JSONManager.loadFloatSetting("text scale");
        }
    }

    public String getResourceString(float amount) {
        String tempString = roundDp(""+amount, 1);
        if (amount >= 0) {
            papplet.fill(0);
            tempString = "+"+tempString;
        } else {
            papplet.fill(255, 0, 0);
        }
        return tempString;
    }


    private void drawRocketProgressBar(PGraphics panelCanvas) {
        int x, y, w, h;
        String progressMessage;
        int PROGRESSBARWIDTH = round(papplet.width*0.25f);
        int PROGRESSBARHEIGHT = round(papplet.height*0.03f);
        int[] progresses = new int[playerCount];
        int count = 0;
        for (int i = 0; i < playerCount; i++) {
            progresses[i] = PApplet.parseInt(players[i].resources[JSONManager.getResIndex(("rocket progress"))]);
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
        x = round((papplet.width-PROGRESSBARWIDTH)/2f);
        y = round(papplet.height*0.05f);
        w = PROGRESSBARWIDTH;
        h = round(PROGRESSBARHEIGHT/(float)count);
        for (int i = 0; i < playerCount; i++) {
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
        panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER, BOTTOM);
        panelCanvas.fill(200);
        int tw = ceil((panelCanvas.textWidth(progressMessage)));
        y = round(papplet.height*0.05f);
        panelCanvas.rect(round(papplet.width/2f) -tw/2f, y-10*JSONManager.loadFloatSetting("text scale"), tw, 10*JSONManager.loadFloatSetting("text scale"));
        panelCanvas.fill(0);
        panelCanvas.text(progressMessage, round(papplet.width/2f), y);
    }

    public ArrayList<String> keyboardEvent(String eventType, char _key) {
        if (eventType.equals("keyPressed") && _key==0 && papplet.keyCode == VK_F12) {
            getPanel("console").setVisible(!getPanel("console").isVisible());
            getElement("2dmap", "default").active = !getPanel("console").isVisible();
            getElement("3dmap", "default").active = !getPanel("console").isVisible();
        }
        if (eventType.equals("keyPressed") && _key == ESC) {
            getPanel("pause screen").setVisible(!getPanel("pause screen").isVisible());
            tooltip.hide();
            if (getPanel("pause screen").isVisible()) {
                ((BaseFileManager)getElement("saving manager", "save screen")).loadSaveNames();
                // Disable map
                getElement("2dmap", "default").active = false;
                getElement("3dmap", "default").active = false;
            } else {
                getPanel("save screen").setVisible(false);
                // Enable map
                getElement("2dmap", "default").active = true;
                getElement("3dmap", "default").active = true;
            }
        }
        if (!getPanel("pause screen").isVisible()&&!getPanel("console").isVisible()) {
            refreshTooltip();
            if (eventType.equals("keyTyped")) {
                if (_key == ' '&&!cinematicMode) {
                    if (((Button)getElement("end turn", "bottom bar")).getText().equals("Advance Units")) {
                        autoMoveParties();
                        ((Button)getElement("end turn", "bottom bar")).setText("Next Turn");
                    } else {
                        postEvent(new EndTurn());
                    }
                } else if (_key == 'i'&&!cinematicMode) {
                    LOGGER_GAME.fine("Finding idle party as 'i' key pressed");
                    targetIdle();
                }
            }
        }
        return new ArrayList<>();
    }
    public void enterState() {
        resourceManager.initialiseResources();
        reloadGame();
    }

    private void reloadGame() {
        LOGGER_MAIN.fine("Reloading game...");
        mapWidth = JSONManager.loadIntSetting("map size");
        mapHeight = JSONManager.loadIntSetting("map size");
        playerCount = players.length;

        updateSidePanelElementsSizes();

        // Default on for showing task icons and unit bars

        clearPrevIdle();
        ((Text)getElement("turns remaining", "party management")).setText("");
        getPanel("end screen").setVisible(false);
        getPanel("save screen").setVisible(false);
        // Enable map
        getElement("2dmap", "default").active = true;
        getElement("3dmap", "default").active = true;

        Text winnerMessage = ((Text)this.getElement("winner", "end screen"));
        winnerMessage.setText("Winner: player /w");

        if (JSONManager.loadBooleanSetting("map is 3d")) {
            LOGGER_MAIN.finer("Map is 3d");
            map = (Map3D)getElement("3dmap", "default");
            getElement("3dmap", "default").setVisible(true);
            getElement("2dmap", "default").setVisible(false);
            getElement("unit number bars toggle", "bottom bar").setVisible(true);
            getElement("task icons toggle", "bottom bar").setVisible(true);
            getElement("unit number bars toggle", "bottom bar").active = true;
            getElement("task icons toggle", "bottom bar").active = true;
            ((Map3D)map).reset();
        } else {
            LOGGER_MAIN.finer("Map is 2d");
            map = (Map2D)getElement("2dmap", "default");
            getElement("3dmap", "default").setVisible(false);
            getElement("2dmap", "default").setVisible(true);
            getElement("unit number bars toggle", "bottom bar").setVisible(false);
            getElement("task icons toggle", "bottom bar").setVisible(false);
            getElement("unit number bars toggle", "bottom bar").active = false;
            getElement("task icons toggle", "bottom bar").active = false;
            ((Map2D)map).reset();
        }
        if (loadingName != null) {
            LOGGER_MAIN.finer("Loading save");
            MapSave mapSave = ((BaseMap)map).loadMap("saves/"+loadingName, resourceManager.getNumResources());
            terrain = mapSave.terrain;
            buildings = mapSave.buildings;
            parties = mapSave.parties;
            mapWidth = mapSave.mapWidth;
            mapHeight = mapSave.mapHeight;
            turnNumber = mapSave.startTurn;
            turn = mapSave.startPlayer;
            this.players = mapSave.players;
            checkForPlayerWin();
            if (JSONManager.loadBooleanSetting("map is 3d")) {
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
            players[2] = new Player((int)playerStarts[2].x, (int)playerStarts[2].y, JSONManager.loadIntSetting("starting block size"), resourceManager.getStartingResourcesClone(), papplet.color(0, 255, 0), "Player 3  ", 0, 2);
            map.targetCell((int) playerStarts[1].x, (int) playerStarts[1].y, JSONManager.loadIntSetting("starting block size"));
            players[1] = new Player((int)playerStarts[1].x, (int)playerStarts[1].y, JSONManager.loadIntSetting("starting block size"), resourceManager.getStartingResourcesClone(), papplet.color(255, 0, 0), "Player 2  ", 0, 1);
            map.targetCell((int) playerStarts[0].x, (int) playerStarts[0].y, JSONManager.loadIntSetting("starting block size"));
            players[0] = new Player((int)playerStarts[0].x, (int)playerStarts[0].y, JSONManager.loadIntSetting("starting block size"), resourceManager.getStartingResourcesClone(), papplet.color(0, 0, 255), "Player 1  ", 0, 0);

            players[players.length-1] = new Player(0, 0, JSONManager.loadIntSetting("starting block size"), resourceManager.getStartingResourcesClone(), papplet.color(255, 0, 255), "Player 4  ", 1, 3);
            players[players.length-1].resources[getResIndex("food")] = -1;
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
                if (partyImages[i].pixels[j] == papplet.color(255, 0, 0)) {
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
        SingleLineTextBox t = ((SingleLineTextBox)(getElement("turn number", "bottom bar")));
        t.setColour(players[turn].colour);
        t.setText("Turn "+turnNumber);

        productionManager = new ProductionManager(resourceManager, taskManager, parties, buildings, mapWidth, notificationManager);
        boolean starving = players[turn].resources[getResIndex("food")] == 0;
        productionManager.updateResourcesSummary(starving);
        getPanel("pause screen").setVisible(false);

        notificationManager.reset();

        // If first turn start players looking at right places
        if (turnNumber == 0) {
            for (int i = playerCount-1; i >= 0; i--) {
                int[] t1 = findIdle(i);
                assert t1 != null;
                map.targetCell(t1[0], t1[1], 64);
                players[i].saveSettings(t1[0], t1[1], 64, selectedCellX, selectedCellY, false);
            }
        }

        if (anyIdle(turn)) {
            ((Button)getElement("idle party finder", "bottom bar")).setColour(color(255, 50, 50));
        } else {
            ((Button)getElement("idle party finder", "bottom bar")).setColour(papplet.color(150));
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
        if (0<=x && x<JSONManager.loadIntSetting("map size") && 0<=y && y<JSONManager.loadIntSetting("map size")) {
            return round(gameData.getJSONArray("terrain").getJSONObject(terrain[y][x]).getInt("movement cost")*mult);
        }
        //Not a valid location
        return -1;
    }

    private ArrayList<int[]> getPath(int startX, int startY, int targetX, int targetY, Node[][] nodes) {
        ArrayList<int[]> returnNodes = new ArrayList<>();
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
        ArrayList<Integer> curMinCosts = new ArrayList<>();
        ArrayList<int[]> curMinNodes = new ArrayList<>();
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
    private int search(ArrayList<int[]> nodes, int x, int y) {
        for (int i=0; i < nodes.size(); i++) {
            if (nodes.get(i)[0] == x && nodes.get(i)[1] == y) {
                return i;
            }
        }
        return -1;
    }
    private int search(ArrayList<Integer> costs, float target) {
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
    private boolean startInvalid(PVector[] ps) {
        for (int i=0; i < playerCount; i++) {
            if (((BaseMap)map).isWater(PApplet.parseInt(ps[i].x), PApplet.parseInt(ps[i].y))) {
                //Check starting position if on water
                return true;
            }
            for (int j=i+1; j < playerCount; j++) {
                if (ps[i].dist(ps[j])<mapWidth/8f) {
                    // Check distances between all players
                    return true;
                }
            }
        }
        return false;
    }
    private PVector generatePartyPosition() {
        return new PVector(PApplet.parseInt(random(0, mapWidth)), PApplet.parseInt(random(0, mapHeight)));
    }

    private PVector[] generateStartingParties() {
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
                parties[(int)playersStartingPositions[i].y][(int)playersStartingPositions[i].x] = new Party(i, JSONManager.loadIntSetting("party size")/2, JSONIndex(gameData.getJSONArray("tasks"), "Rest"), gameData.getJSONObject("game options").getInt("movement points"), String.format("Player %d   #0", i));
            }
        }
        return playersStartingPositions;
    }

    private void enterCinematicMode() {
        LOGGER_GAME.finer("Entering cinematic mode...");
        cinematicMode = true;
        getPanel("bottom bar").setVisible(false);
        getPanel("land management").setVisible(false);
        getPanel("party management").setVisible(false);
        ((BaseMap)map).cinematicMode = true;
    }
    private void leaveCinematicMode() {
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

    private void startRocketLaunch() {
        LOGGER_GAME.finer("Starting rocket launch");
        rocketVelocity = new PVector(0, 0, 0);
        rocketBehaviour = PApplet.parseInt(random(10));
        buildings[selectedCellY][selectedCellX].setImageId(0);
        rocketLaunching = true;
        rocketPosition = new PVector(selectedCellX, selectedCellY, 0);
        map.enableRocket(rocketPosition, rocketVelocity);
        enterCinematicMode();
        rocketStartTime = papplet.millis();
    }
    private void handleRocket() {
        float t = PApplet.parseFloat(papplet.millis()-rocketStartTime)/1000;
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
        rocketVelocity.div(papplet.frameRate);
        rocketPosition.add(rocketVelocity);
    }
    private void rocketLaunchEnd() {
        map.disableRocket();
        rocketLaunching = false;
        if (rocketBehaviour > 6) {
            winner = turn;
        } else {
            players[turn].resources[JSONManager.getResIndex(("rocket progress"))] = 0;
            parties[selectedCellY][selectedCellX].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
        }
        leaveCinematicMode();
    }

    private String nextRollingId(String playerName) {
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

    public void updateResourcesSummary() {
        productionManager.updateResourcesSummary(players[turn].resources[getResIndex("food")] == 0);
    }
}
