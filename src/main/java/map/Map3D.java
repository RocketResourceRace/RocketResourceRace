package map;

import json.JSONManager;
import party.Battle;
import party.Party;
import player.Player;
import processing.core.*;
import processing.data.JSONObject;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;
import util.Cell;
import util.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import static json.JSONManager.*;
import static processing.core.PApplet.*;
import static processing.core.PConstants.P3D;
import static processing.core.PConstants.PI;
import static util.Dijkstra.LimitedKnowledgeDijkstra;
import static util.Image.*;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Map3D extends BaseMap implements Map {
    final int thickness = 10;
    final float HILLRAISE = 1.05f;
    private final float GROUNDHEIGHT = 5;
    private final float VERYSMALLSIZE = 0.01f;
    private int numTreeTiles;
    private int x;
    private int y;
    private int w;
    private int h;
    private int prevT;
    private float hoveringX, hoveringY, oldHoveringX, oldHoveringY;
    public float targetXOffset;
    public float targetYOffset;
    private int selectedCellX, selectedCellY;
    private PShape tiles, flagPole, battle, trees, selectTile, water, tileRect, pathLine, highlightingGrid, drawPossibleMoves, drawPossibleBombards, obscuredCellsOverlay, unseenCellsOverlay, dangerousCellsOverlay, bombardArrow, bandit;
    private PShape[] flags;
    private HashMap<String, PShape> taskObjs;
    private HashMap<String, PShape[]> buildingObjs;
    private PShape[] unitNumberObjects;
    private PImage[] tempTileImages;
    private float targetZoom;
    public float zoom;
    private float tilt;
    private float rot;
    public float focusedX;
    public float focusedY;
    private PVector focusedV;
    private Boolean panning;
    private Boolean zooming;
    private Boolean cellSelected;
    private Boolean updateHoveringScale;
    private Node[][] moveNodes;
    public float blockSize;
    private ArrayList<int[]> drawPath;
    private HashMap<Integer, Integer> forestTiles;
    private PGraphics canvas;
    private HashMap<Integer, HashMap<Integer, Float>> downwardAngleCache;
    private boolean drawRocket;
    private PVector rocketPosition;
    private PVector rocketVelocity;
    private boolean showingBombard;
    private int bombardRange;
    private int[] playerColours;

    public Map3D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight) {
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
        zoom = papplet.height/2f;
        tilt = PI/3;
        focusedX = round(mapWidth*blockSize/2);
        focusedY = round(mapHeight*blockSize/2);
        focusedV = new PVector(0, 0);
        PVector heldPos = null;
        cellSelected = false;
        panning = false;
        zooming = false;
        buildingObjs = new HashMap<>();
        taskObjs = new HashMap<>();
        forestTiles = new HashMap<>();
        canvas = papplet.createGraphics(papplet.width, papplet.height, P3D);
        PGraphics refractionCanvas = papplet.createGraphics(papplet.width / 4, papplet.height / 4, P3D);
        downwardAngleCache = new HashMap<>();
        heightMap = new float[PApplet.parseInt((mapWidth+1)*(mapHeight+1)*pow(JSONManager.loadFloatSetting("terrain detail"), 2))];
        targetXOffset = mapWidth/2f*blockSize;
        targetYOffset = mapHeight/2f*blockSize;
        updateHoveringScale = false;
        this.keyState = new HashMap<>();
        showingBombard = false;
    }



    private float getDownwardAngle(int x, int y) {
        try {
            if (!downwardAngleCache.containsKey(y)) {
                downwardAngleCache.put(y, new HashMap<>());
            }
            if (downwardAngleCache.get(y).containsKey(x)) {
                return downwardAngleCache.get(y).get(x);
            } else {
                PVector maxZCoord = new PVector();
                PVector minZCoord = new PVector();
                float maxZ = 0;
                float minZ = 1;
                for (float y1 = y; y1<=y+1; y1+=1.0f/JSONManager.loadFloatSetting("terrain detail")) {
                    for (float x1 = x; x1<=x+1; x1+=1.0f/JSONManager.loadFloatSetting("terrain detail")) {
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
        Boolean mapActive = a;
    }
    public void updateMoveNodes(Node[][] nodes, Player[] players) {
        LOGGER_MAIN.finer("Updating move nodes");
        moveNodes = nodes;
        updatePossibleMoves();
        updateDangerousCellsOverlay(visibleCells, players);
    }

    private void updatePath(ArrayList<int[]> path, int[] target) {
        // Use when updaing with a target node
        ArrayList<int[]> tempPath = new ArrayList<>(path);
        tempPath.add(target);
        updatePath(tempPath);
    }

    public void updatePath(ArrayList<int[]> path) {
        //LOGGER_MAIN.finer("Updating path");
        float x0, y0;
        pathLine = papplet.createShape();
        pathLine.beginShape();
        pathLine.noFill();
        if (drawPath == null) {
            pathLine.stroke(150);
        } else {
            pathLine.stroke(255, 0, 0);
        }
        for (int i=0; i<path.size()-1; i++) {
            for (int u=0; u<blockSize/8; u++) {
                x0 = path.get(i)[0]+(path.get(i+1)[0]-path.get(i)[0])*u/8f+0.5f;
                y0 = path.get(i)[1]+(path.get(i+1)[1]-path.get(i)[1])*u/8f+0.5f;
                pathLine.vertex(x0*blockSize, y0*blockSize, 5+getHeight(x0, y0));
            }
        }
        if (drawPath != null) {
            pathLine.vertex((selectedCellX+0.5f)*blockSize, (selectedCellY+0.5f)*blockSize, 5+getHeight(selectedCellX+0.5f, selectedCellY+0.5f));
        }
        pathLine.endShape();
        drawPath = path;
    }

    private void loadUnseenCellsOverlay(Cell[][] visibleCells) {
        // For the shape that indicates cells that have not been seen
        try {
            LOGGER_MAIN.finer("Loading unseen cells overlay");
            float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
            unseenCellsOverlay = papplet.createShape();
            unseenCellsOverlay.beginShape(TRIANGLES);
            unseenCellsOverlay.fill(0);
            for (int y=0; y<mapHeight; y++) {
                for (int x=0; x<mapWidth; x++) {
                    if (visibleCells[y][x] == null) {
                        if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && forestTiles.containsKey(x+y*mapWidth)) {
                            removeTreeTile(x, y);
                        }
                        for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                            for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                                unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));

                                unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                unseenCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                unseenCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                            }
                        }
                    } else {
                        if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && !forestTiles.containsKey(x+y*mapWidth)) {
                            PShape cellTree = generateTrees(JSONManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
                            cellTree.translate((x)*blockSize, (y)*blockSize, 0);
                            trees.addChild(cellTree);
                            addTreeTile(x, y, numTreeTiles++);
                        }
                        for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                            for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
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

    private void updateUnseenCellsOverlay(Cell[][] visibleCells) {
        // For the shape that indicates cells that have not been seen
        try {
            LOGGER_MAIN.finer("Updating unseen cells overlay");
            float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
            if (unseenCellsOverlay == null) {
                loadUnseenCellsOverlay(visibleCells);
            } else {
                for (int y=0; y<mapHeight; y++) {
                    for (int x=0; x<mapWidth; x++) {
                        int c = PApplet.parseInt(pow(JSONManager.loadFloatSetting("terrain detail"), 2) * (y*mapWidth+x) * 6);
                        if (visibleCells[y][x] == null) {
                            if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && forestTiles.containsKey(x+y*mapWidth)) {
                                removeTreeTile(x, y);
                            }
                            if (unseenCellsOverlay.getVertex(c).z == 0) {
                                for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                    for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                                        unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;

                                        unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        unseenCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        unseenCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                    }
                                }
                            }
                        } else {
                            if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest") && !forestTiles.containsKey(x+y*mapWidth)) {
                                PShape cellTree = generateTrees(JSONManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
                                cellTree.translate((x)*blockSize, (y)*blockSize, 0);
                                trees.addChild(cellTree);
                                addTreeTile(x, y, numTreeTiles++);
                            }

                            if (unseenCellsOverlay.getVertex(c).z != 0) {
                                for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                    for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
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

    private void loadObscuredCellsOverlay(Cell[][] visibleCells) {
        // For the shape that indicates cells that are not currently under party sight
        try {
            LOGGER_MAIN.finer("Loading obscured cells overlay");
            float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
            obscuredCellsOverlay = papplet.createShape();
            obscuredCellsOverlay.beginShape(TRIANGLES);
            obscuredCellsOverlay.fill(0, 0, 0, 200);
            for (int y=0; y<mapHeight; y++) {
                for (int x=0; x<mapWidth; x++) {
                    if (visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
                        for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                            for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                                obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));

                                obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                            }
                        }
                    } else {
                        for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                            for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
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

    private void updateObscuredCellsOverlay(Cell[][] visibleCells) {
        // For the shape that indicates cells that are not currently under party sight
        try {
            LOGGER_MAIN.finer("Updating obscured cells overlay");
            float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
            if (obscuredCellsOverlay == null) {
                loadObscuredCellsOverlay(visibleCells);
            } else {
                for (int y=0; y<mapHeight; y++) {
                    for (int x=0; x<mapWidth; x++) {
                        int c = PApplet.parseInt(pow(JSONManager.loadFloatSetting("terrain detail"), 2) * (y*mapWidth+x) * 6);
                        if (visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
                            if (obscuredCellsOverlay.getVertex(c).z == 0) {
                                for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                    for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                                        obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;

                                        obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        obscuredCellsOverlay.setVertex(c, x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                        obscuredCellsOverlay.setVertex(c, x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                        c++;
                                    }
                                }
                            }
                        } else {
                            if (obscuredCellsOverlay.getVertex(c).z != 0) {
                                for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                    for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
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

    private void updateOverlays(Cell[][] visibleCells) {
        if (JSONManager.loadBooleanSetting("fog of war")) {
            updateObscuredCellsOverlay(visibleCells);
            updateUnseenCellsOverlay(visibleCells);
        }
    }

    private void updatePossibleMoves() {
        // For the shape that indicateds where a party can move
        try {
            LOGGER_MAIN.finer("Updating possible move nodes");
            float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
            drawPossibleMoves = papplet.createShape();
            drawPossibleMoves.beginShape(TRIANGLES);
            drawPossibleMoves.fill(0, 0, 0, 100);
            for (int y=0; y<mapHeight; y++) {
                for (int x=0; x<mapWidth; x++) {
                    if (moveNodes[y][x] != null && parties[selectedCellY][selectedCellX] != null && moveNodes[y][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()) {
                        for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                            for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                drawPossibleMoves.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleMoves.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleMoves.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));

                                drawPossibleMoves.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleMoves.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleMoves.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
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

    private void updateDangerousCellsOverlay(Cell[][] visibleCells, Player[] players) {
        // For the shape that indicates cells that are dangerous
        try {
            if (visibleCells[selectedCellY][selectedCellX] != null && visibleCells[selectedCellY][selectedCellX].party != null) {
                LOGGER_MAIN.finer("Updating dangerous cells overlay");
                float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
                dangerousCellsOverlay = papplet.createShape();
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
                                    assert visibleCells[y][x] != null;
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
                            for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                                for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                    dangerousCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                    dangerousCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                    dangerousCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));

                                    dangerousCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                    dangerousCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                    dangerousCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
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

    private void updatePossibleBombards() {
        // For the shape that indicateds where a party can bombard
        try {
            LOGGER_MAIN.finer("Updating possible bombards");
            float smallSize = blockSize / JSONManager.loadFloatSetting("terrain detail");
            drawPossibleBombards = papplet.createShape();
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
                        for (int x1=0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                            for (int y1=0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
                                drawPossibleBombards.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleBombards.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleBombards.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));

                                drawPossibleBombards.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+y1/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleBombards.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
                                drawPossibleBombards.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(y1+1)/JSONManager.loadFloatSetting("terrain detail")));
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
    public void targetCell(int x, int y, float zoom) {
        LOGGER_MAIN.finer(String.format("Targetting cell:%s, %s and zoom:%s", x, y, zoom));
        targetXOffset = (x+0.5f)*blockSize-papplet.width/2f;
        targetYOffset = (y+0.5f)*blockSize-papplet.height/2f;
        panning = true;
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

    private void addTreeTile(int cellX, int cellY, int i) {
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
    //  water = papplet.createShape(GROUP);
    //  PShape w;
    //  float scale = getObjectWidth()/vertices;
    //  w = papplet.createShape();
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

    private PShape generateTrees(int num, int vertices, float x1, float y1) {
        try {
            //LOGGER_MAIN.info(String.format("Generating trees at %s, %s", x1, y1));
            PShape shapes = papplet.createShape(GROUP);
            PShape stump;
            papplet.colorMode(HSB, 100);
            for (int i=0; i<num; i++) {
                float x = random(0, blockSize), y = random(0, blockSize);
                float h = getHeight((x1+x)/blockSize, (y1+y)/blockSize);
                float LEAVESH = 15;
                float TREERANDOMNESS = 0.3f;
                float randHeight = LEAVESH *random(1- TREERANDOMNESS, 1+ TREERANDOMNESS);
                if (h <= JSONManager.loadFloatSetting("water level")*blockSize*GROUNDHEIGHT) continue; // Don't put trees underwater
                int leafColour = color(random(35, 40), random(90, 100), random(30, 60));
                int stumpColour = color(random(100, 125), random(100, 125), random(50, 30));
                PShape leaves = papplet.createShape();
                leaves.setShininess(0.1f);
                leaves.beginShape(TRIANGLE_FAN);
                leaves.fill(leafColour);
                int tempVertices = round(random(4, vertices));
                // create leaves
                float STUMPH = 4;
                leaves.vertex(x, y, STUMPH +randHeight+h);
                for (int j=0; j<tempVertices+1; j++) {
                    float LEAVESR = 5;
                    leaves.vertex(x+cos(j*TWO_PI/tempVertices)* LEAVESR, y+sin(j*TWO_PI/tempVertices)* LEAVESR, STUMPH +h);
                }
                leaves.endShape(CLOSE);
                shapes.addChild(leaves);

                //create trunck
                stump = papplet.createShape();
                stump.beginShape(QUAD_STRIP);
                stump.fill(stumpColour);
                for (int j=0; j<4; j++) {
                    float STUMPR = 1;
                    stump.vertex(x+cos(j*TWO_PI/3)* STUMPR, y+sin(j*TWO_PI/3)* STUMPR, h);
                    stump.vertex(x+cos(j*TWO_PI/3)* STUMPR, y+sin(j*TWO_PI/3)* STUMPR, STUMPH +h);
                }
                stump.endShape();
                shapes.addChild(stump);
            }
            papplet.colorMode(RGB, 255);
            return shapes;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error generating trees", e);
            throw e;
        }
    }

    private void loadMapStrip(int y, PShape tiles, boolean loading) {
        try {
            LOGGER_MAIN.finer("Loading map strip y:"+y+" loading: "+loading);
            PGraphics tempTerrain = papplet.createGraphics(round((1 + mapWidth) * JSONManager.loadIntSetting("terrain texture resolution")), round(JSONManager.loadIntSetting("terrain texture resolution")));
            PShape tempSingleRow;
            PShape tempRow = papplet.createShape(GROUP);
            tempTerrain.beginDraw();
            for (int x=0; x<mapWidth; x++) {
                tempTerrain.image(tempTileImages[terrain[y][x]], x*JSONManager.loadIntSetting("terrain texture resolution"), 0);
            }
            tempTerrain.endDraw();

            for (int y1=0; y1<JSONManager.loadFloatSetting("terrain detail"); y1++) {
                tempSingleRow = papplet.createShape();
                tempSingleRow.setTexture(tempTerrain);
                tempSingleRow.beginShape(TRIANGLE_STRIP);
                papplet.resetMatrix();
                if (JSONManager.loadBooleanSetting("tile stroke")) {
                    tempSingleRow.stroke(0);
                }
                tempSingleRow.vertex(0, (y+y1/JSONManager.loadFloatSetting("terrain detail"))*blockSize, 0, 0, y1*JSONManager.loadIntSetting("terrain texture resolution")/JSONManager.loadFloatSetting("terrain detail"));
                tempSingleRow.vertex(0, (y+(1+y1)/JSONManager.loadFloatSetting("terrain detail"))*blockSize, 0, 0, (y1+1)*JSONManager.loadIntSetting("terrain texture resolution")/JSONManager.loadFloatSetting("terrain detail"));
                for (int x=0; x<mapWidth; x++) {
                    if (terrain[y][x] == terrainIndex("quarry site stone") || terrain[y][x] == terrainIndex("quarry site clay")) {
                        // End strip and start new one, skipping out cell
                        tempSingleRow.vertex(x*blockSize, (y+y1/JSONManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x, (y+y1/JSONManager.loadFloatSetting("terrain detail"))), x*JSONManager.loadIntSetting("terrain texture resolution"), y1*JSONManager.loadIntSetting("terrain texture resolution")/JSONManager.loadFloatSetting("terrain detail"));
                        tempSingleRow.vertex(x*blockSize, (y+(1+y1)/JSONManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x, y+(1+y1)/JSONManager.loadFloatSetting("terrain detail")), x*JSONManager.loadIntSetting("terrain texture resolution"), (y1+1)*JSONManager.loadIntSetting("terrain texture resolution")/JSONManager.loadFloatSetting("terrain detail"));
                        tempSingleRow.endShape();
                        tempRow.addChild(tempSingleRow);
                        tempSingleRow = papplet.createShape();
                        tempSingleRow.setTexture(tempTerrain);
                        tempSingleRow.beginShape(TRIANGLE_STRIP);

                        if (y1 == 0) {
                            // Add replacement cell for quarry site
                            PShape quarrySite = papplet.loadShape(RESOURCES_ROOT+"obj/building/quarry_site.obj");
                            float quarryheight = groundMinHeightAt(x, y);
                            quarrySite.rotateX(PI/2);
                            quarrySite.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, quarryheight);
                            quarrySite.setTexture(loadImage(RESOURCES_ROOT+"img/terrain/hill.png"));
                            tempRow.addChild(quarrySite);

                            // Create sides for quarry site
                            float smallStripSize = blockSize/JSONManager.loadFloatSetting("terrain detail");
                            float terrainDetail = JSONManager.loadFloatSetting("terrain detail");
                            PShape sides = papplet.createShape();
                            sides.setFill(papplet.color(120));
                            sides.beginShape(QUAD_STRIP);
                            sides.fill(papplet.color(120));
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
                        for (int x1=0; x1<JSONManager.loadFloatSetting("terrain detail"); x1++) {
                            tempSingleRow.vertex((x+x1/JSONManager.loadFloatSetting("terrain detail"))*blockSize, (y+y1/JSONManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), (y+y1/JSONManager.loadFloatSetting("terrain detail"))), (x+x1/JSONManager.loadFloatSetting("terrain detail"))*JSONManager.loadIntSetting("terrain texture resolution"), y1*JSONManager.loadIntSetting("terrain texture resolution")/JSONManager.loadFloatSetting("terrain detail"));
                            tempSingleRow.vertex((x+x1/JSONManager.loadFloatSetting("terrain detail"))*blockSize, (y+(1+y1)/JSONManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x+x1/JSONManager.loadFloatSetting("terrain detail"), y+(1+y1)/JSONManager.loadFloatSetting("terrain detail")), (x+x1/JSONManager.loadFloatSetting("terrain detail"))*JSONManager.loadIntSetting("terrain texture resolution"), (y1+1)*JSONManager.loadIntSetting("terrain texture resolution")/JSONManager.loadFloatSetting("terrain detail"));
                        }
                    }
                }
                tempSingleRow.vertex(mapWidth*blockSize, (y+y1/JSONManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, (y+y1/JSONManager.loadFloatSetting("terrain detail"))), mapWidth*JSONManager.loadIntSetting("terrain texture resolution"), (y1/JSONManager.loadFloatSetting("terrain detail"))*JSONManager.loadIntSetting("terrain texture resolution"));
                tempSingleRow.vertex(mapWidth*blockSize, (y+(1+y1)/JSONManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, y+(1.0f+y1)/JSONManager.loadFloatSetting("terrain detail")), mapWidth*JSONManager.loadIntSetting("terrain texture resolution"), ((y1+1.0f)/JSONManager.loadFloatSetting("terrain detail"))*JSONManager.loadIntSetting("terrain texture resolution"));
                tempSingleRow.endShape();
                tempRow.addChild(tempSingleRow);
            }
            if (loading) {
                tiles.addChild(tempRow);
            } else {
                tiles.addChild(tempRow, y);
            }
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
        // Use to clear references to large objects when exiting states
        try {
            LOGGER_MAIN.info("Clearing 3D models");
            water = null;
            trees = null;
            tiles = null;
            buildingObjs = new HashMap<>();
            taskObjs = new HashMap<>();
            forestTiles = new HashMap<>();
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error clearing shape", e);
            throw e;
        }
    }

    public void generateShape() {
        try {
            LOGGER_MAIN.info("Generating 3D models");
            papplet.pushStyle();
            papplet.noFill();
            papplet.noStroke();
            LOGGER_MAIN.fine("Generating terrain textures");
            tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
            for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
                JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
                if (tile3DImages.containsKey(tileType.getString("id"))) {
                    tempTileImages[i] = tile3DImages.get(tileType.getString("id")).copy();
                } else {
                    tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
                }
                tempTileImages[i].resize(JSONManager.loadIntSetting("terrain texture resolution"), JSONManager.loadIntSetting("terrain texture resolution"));
            }

            tiles = papplet.createShape(GROUP);
            papplet.textureMode(IMAGE);
            trees = papplet.createShape(GROUP);

            LOGGER_MAIN.fine("Generating trees and terrain model");
            for (int y=0; y<mapHeight; y++) {
                loadMapStrip(y, tiles, true);

                // Load trees
                //for (int x=0; x<mapWidth; x++) {
                //  if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest")) {
                //    PShape cellTree = generateTrees(JSONManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
                //    cellTree.translate((x)*blockSize, (y)*blockSize, 0);
                //    trees.addChild(cellTree);
                //    addTreeTile(x, y, numTreeTiles++);
                //  }
                //}
            }
            papplet.resetMatrix();

            LOGGER_MAIN.fine("Generating player flags");

            flagPole = papplet.loadShape(RESOURCES_ROOT+"obj/party/flagpole.obj");
            flagPole.rotateX(PI/2);
            flagPole.scale(2, 2.5f, 2.5f);
            flags = new PShape[playerColours.length-1];
            for (int i = 0; i < playerColours.length-1; i++) {
                flags[i] = papplet.createShape(GROUP);
                PShape edge = papplet.loadShape(RESOURCES_ROOT+"obj/party/flagedges.obj");
                edge.setFill(brighten(playerColours[i], 20));
                PShape side = papplet.loadShape(RESOURCES_ROOT+"obj/party/flagsides.obj");
                side.setFill(brighten(playerColours[i], -40));
                flags[i].addChild(edge);
                flags[i].addChild(side);
                flags[i].rotateX(PI/2);
                flags[i].scale(2, 2.5f, 2.5f);
            }
            bandit = papplet.loadShape(RESOURCES_ROOT+"obj/party/bandit.obj");
            bandit.rotateX(PI/2);
            bandit.scale(0.8f);
            battle = papplet.loadShape(RESOURCES_ROOT+"obj/party/battle.obj");
            battle.rotateX(PI/2);
            battle.scale(0.8f);

            LOGGER_MAIN.fine("Generating Water");
            papplet.fill(10, 50, 180);
            water = papplet.createShape(RECT, 0, 0, getObjectWidth(), getObjectHeight());
            water.translate(0, 0, JSONManager.loadFloatSetting("water level")*blockSize*GROUNDHEIGHT+4*VERYSMALLSIZE);
            generateHighlightingGrid(8, 8);

            int players = playerColours.length;
            papplet.fill(255);

            LOGGER_MAIN.fine("Generating units number objects");
            unitNumberObjects = new PShape[players];
            for (int i=0; i < players; i++) {
                unitNumberObjects[i] = papplet.createShape();
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


            tileRect = papplet.createShape();
            tileRect.beginShape();
            tileRect.noFill();
            tileRect.stroke(0);
            tileRect.strokeWeight(3);
            int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            int[] curLoc = {0, 0};
            for (int[] dir : directions) {
                for (int i=0; i<JSONManager.loadFloatSetting("terrain detail"); i++) {
                    tileRect.vertex(curLoc[0]*blockSize/JSONManager.loadFloatSetting("terrain detail"), curLoc[1]*blockSize/JSONManager.loadFloatSetting("terrain detail"), 0);
                    curLoc[0] += dir[0];
                    curLoc[1] += dir[1];
                }
            }
            LOGGER_MAIN.fine("Loading task icon objects");
            tileRect.endShape(CLOSE);
            for (int i=0; i<gameData.getJSONArray("tasks").size(); i++) {
                JSONObject task = gameData.getJSONArray("tasks").getJSONObject(i);
                if (!task.isNull("obj")) {
                    taskObjs.put(task.getString("id"), papplet.loadShape(RESOURCES_ROOT+"obj/task/"+task.getString("obj")));
                    taskObjs.get(task.getString("id")).translate(blockSize*0.125f, -blockSize*0.2f);
                    taskObjs.get(task.getString("id")).rotateX(PI/2);
                } else if (!task.isNull("img")) {
                    PShape object = papplet.createShape(RECT, 0, 0, blockSize/4, blockSize/4);
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
                            buildingObjs.get(buildingType.getString("id"))[j] = papplet.loadShape(RESOURCES_ROOT+"obj/building/quarry.obj");
                            buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
                            //buildingObjs.get(buildingType.getString("id"))[j].setFill(color(86, 47, 14));
                        } else {
                            buildingObjs.get(buildingType.getString("id"))[j] = papplet.loadShape(RESOURCES_ROOT+"obj/building/"+buildingType.getJSONArray("obj").getString(j));
                            buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
                            buildingObjs.get(buildingType.getString("id"))[j].scale(0.625f);
                            buildingObjs.get(buildingType.getString("id"))[j].translate(0, 0, -6);
                        }
                    }
                }
            }

            bombardArrow = papplet.createShape();

            papplet.popStyle();
            cinematicMode = false;
            drawRocket = false;
            this.keyState = new HashMap<>();
            obscuredCellsOverlay = null;
            unseenCellsOverlay = null;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading models", e);
            throw e;
        }
    }

    private void generateHighlightingGrid(int horizontals, int verticles) {
        try {
            LOGGER_MAIN.fine("Generating highlighting grid");
            PShape line;
            // Load horizontal lines first
            highlightingGrid = papplet.createShape(GROUP);
            for (int i=0; i<horizontals; i++) {
                line = papplet.createShape();
                line.beginShape();
                line.noFill();
                for (int x1=0; x1<JSONManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
                    float x2 = -horizontals/2f+0.5f+x1/JSONManager.loadFloatSetting("terrain detail");
                    float y1 = -verticles/2f+i+0.5f;
                    line.stroke(255, 255, 255, 255-sqrt(pow(x2, 2)+pow(y1, 2))/3*255);
                    line.vertex(0, 0, 0);
                }
                line.endShape();
                highlightingGrid.addChild(line);
            }
            // Next do verticle lines
            for (int i=0; i<verticles; i++) {
                line = papplet.createShape();
                line.beginShape();
                line.noFill();
                for (int y1=0; y1<JSONManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
                    float y2 = -verticles/2f+0.5f+y1/JSONManager.loadFloatSetting("terrain detail");
                    float x1 = -horizontals/2f+i+0.5f;
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

    private void updateHighlightingGrid(float x, float y, int horizontals, int verticles) {
        // x, y are cell coordinates
        try {
            //LOGGER_MAIN.finer(String.format("Updating selection rect at:%s, %s", x, y));
            PShape line;
            float alpha;
            if (JSONManager.loadBooleanSetting("active cell highlighting")) {
                for (int i=0; i<horizontals; i++) {
                    line = highlightingGrid.getChild(i);
                    for (int x1=0; x1<JSONManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
                        float x2 = PApplet.parseInt(x)-horizontals/2f+1+x1/JSONManager.loadFloatSetting("terrain detail");
                        float y1 = PApplet.parseInt(y)-verticles/2f+1+i;
                        float x3 = -horizontals/2f+x1/JSONManager.loadFloatSetting("terrain detail");
                        float y3 = -verticles/2f+i;
                        float dist = sqrt(pow(y3-y%1+1, 2)+pow(x3-x%1+1, 2));
                        if (0 < x2 && x2 < mapWidth && 0 < y1 && y1 < mapHeight) {
                            alpha = 255-dist/(verticles/2f-1)*255;
                        } else {
                            alpha = 0;
                        }
                        line.setStroke(x1, papplet.color(255, alpha));
                        line.setVertex(x1, x2*blockSize, y1*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x2, y1));
                    }
                }
                // verticle lines
                for (int i=0; i<verticles; i++) {
                    line = highlightingGrid.getChild(i+horizontals);
                    for (int y1=0; y1<JSONManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
                        float x1 = PApplet.parseInt(x)-horizontals/2f+1+i;
                        float y2 = PApplet.parseInt(y)-verticles/2f+1+y1/JSONManager.loadFloatSetting("terrain detail");
                        float y3 = -verticles/2f+y1/JSONManager.loadFloatSetting("terrain detail");
                        float x3 = -horizontals/2f+i;
                        float dist = sqrt(pow(y3-y%1+1, 2)+pow(x3-x%1+1, 2));
                        if (0 < x1 && x1 < mapWidth && 0 < y2 && y2 < mapHeight) {
                            alpha = 255-dist/(horizontals/2f-1)*255;
                        } else {
                            alpha = 0;
                        }
                        line.setStroke(y1, papplet.color(255, alpha));
                        line.setVertex(y1, x1*blockSize, y2*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x1, y2));
                    }
                }
            } else {
                for (int i=0; i<horizontals; i++) {
                    line = highlightingGrid.getChild(i);
                    for (int x1=0; x1<JSONManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
                        float x2 = PApplet.parseInt(x)-horizontals/2f+1+x1/JSONManager.loadFloatSetting("terrain detail");
                        float y1 = PApplet.parseInt(y)-verticles/2f+1+i;
                        line.setVertex(x1, x2*blockSize, y1*blockSize, 4.5f*VERYSMALLSIZE+getHeight(x2, y1));
                    }
                }
                // verticle lines
                for (int i=0; i<verticles; i++) {
                    line = highlightingGrid.getChild(i+horizontals);
                    for (int y1=0; y1<JSONManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
                        float x1 = PApplet.parseInt(x)-horizontals/2f+1+i;
                        float y2 = PApplet.parseInt(y)-verticles/2f+1+y1/JSONManager.loadFloatSetting("terrain detail");
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

    private void updateSelectionRect(int cellX, int cellY) {
        try {
            //LOGGER_MAIN.finer(String.format("Updating selection rect at:%s, %s", cellX, cellY));
            int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            int[] curLoc = {0, 0};
            int a = 0;
            for (int[] dir : directions) {
                for (int i=0; i<JSONManager.loadFloatSetting("terrain detail"); i++) {
                    tileRect.setVertex(a++, curLoc[0]*blockSize/JSONManager.loadFloatSetting("terrain detail"), curLoc[1]*blockSize/JSONManager.loadFloatSetting("terrain detail"), getHeight(cellX+curLoc[0]/JSONManager.loadFloatSetting("terrain detail"), cellY+curLoc[1]/JSONManager.loadFloatSetting("terrain detail")));
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

    private PVector getMousePosOnObject() {
        try {
            applyCameraPerspective();
            PVector floorPos = new PVector(focusedX+papplet.width/2f, focusedY+papplet.height/2f, 0);
            PVector floorDir = new PVector(0, 0, -1);
            PVector mousePos = getUnProjectedPointOnFloor(papplet.mouseX, papplet.mouseY, floorPos, floorDir);
            papplet.camera();
            return mousePos;
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error finding mouse position on object", e);
            throw e;
        }
    }

    private float getObjectWidth() {
        return mapWidth*blockSize;
    }
    private float getObjectHeight() {
        return mapHeight*blockSize;
    }
    private void setZoom(float zoom) {
        this.zoom =  between(papplet.height/3f, zoom, min(mapHeight*blockSize, papplet.height*4));
    }
    private void setTilt(float tilt) {
        this.tilt = between(0.01f, tilt, 3*PI/8);
    }
    private void setRot(float rot) {
        this.rot = rot;
    }
    private void setFocused(float focusedX, float focusedY) {
        this.focusedX = between(-papplet.width/2f, focusedX, getObjectWidth()-papplet.width/2f);
        this.focusedY = between(-papplet.height/2f, focusedY, getObjectHeight()-papplet.height/2f);
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        if (eventType.equals("mouseDragged")) {
            //if (papplet.mouseButton == LEFT) {
                //if (heldPos != null){

                //camera(prevFx+papplet.width/2+zoom*sin(tilt)*sin(rot), prevFy+papplet.height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), prevFy+papplet.width/2, focusedY+papplet.height/2, 0, 0, 0, -1);
                //PVector newHeldPos = MousePosOnObject(mouseX, mouseY);
                //camera();
                //focusedX = heldX-(newHeldPos.x-heldPos.x);
                //focusedY = heldY-(newHeldPos.y-heldPos.y);
                //prevFx = heldX-(newHeldPos.x-heldPos.x);
                //prevFy = heldY-(newHeldPos.y-heldPos.y);
                //heldPos.x = newHeldPos.x;
                //heldPos.y = newHeldPos.y;
                //}
            //} else
            if (papplet.mouseButton != RIGHT) {
                setTilt(tilt-(papplet.mouseY-papplet.pmouseY)*0.01f);
                setRot(rot-(papplet.mouseX-papplet.pmouseX)*0.01f);
                doUpdateHoveringScale();
            }
        }
        //else if (eventType.equals("mousePressed")){
        //  if (button == LEFT){
        //    camera(focusedX+papplet.width/2+zoom*sin(tilt)*sin(rot), focusedY+papplet.height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+papplet.width/2, focusedY+papplet.height/2, 0, 0, 0, -1);
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
        return new ArrayList<>();
    }


    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
        if (eventType.equals("mouseWheel")) {
            doUpdateHoveringScale();
            float count = event.getCount();
            setZoom(zoom+zoom*count*0.15f);
        }
        return new ArrayList<>();
    }
    public ArrayList<String> keyboardEvent(String eventType, char _key) {
        if (eventType.equals("keyPressed")) {
            keyState.put(_key, true);
        }
        if (eventType.equals("keyReleased")) {
            keyState.put(_key, false);
        }
        return new ArrayList<>();
    }
    private float getHeight(float x, float y) {
        //if (y<mapHeight && x<mapWidth && y+JSONManager.loadFloatSetting("terrain detail")/blockSize<mapHeight && x+JSONManager.loadFloatSetting("terrain detail")/blockSize<mapHeight && y-JSONManager.loadFloatSetting("terrain detail")/blockSize>=0 && x-JSONManager.loadFloatSetting("terrain detail")/blockSize>=0 &&
        //terrain[floor(y)][floor(x)] == JSONIndex(gameData.getJSONArray("terrain"), "hills")+1 &&
        //terrain[floor(y+JSONManager.loadFloatSetting("terrain detail")/blockSize)][floor(x+JSONManager.loadFloatSetting("terrain detail")/blockSize)] == JSONIndex(gameData.getJSONArray("terrain"), "hills")+1 &&
        //terrain[floor(y-JSONManager.loadFloatSetting("terrain detail")/blockSize)][floor(x-JSONManager.loadFloatSetting("terrain detail")/blockSize)] == JSONIndex(gameData.getJSONArray("terrain"), "hills")+1){
        //  return (max(noise(x*MAPNOISESCALE, y*MAPNOISESCALE), waterLevel)-waterLevel)*blockSize*GROUNDHEIGHT*HILLRAISE;
        //} else {
        return max(getRawHeight(x, y), JSONManager.loadFloatSetting("water level"))*blockSize*GROUNDHEIGHT;
        //float h = (max(noise(x*MAPNOISESCALE, y*MAPNOISESCALE), waterLevel)-waterLevel);
        //return (max(h-(0.5+waterLevel/2.0), 0)*(1000)+h)*blockSize*GROUNDHEIGHT;
        //}
    }
    private float groundMinHeightAt(int x1, int y1) {
        int x = floor(x1);
        int y = floor(y1);
        return min(new float[]{getHeight(x, y), getHeight(x+1, y), getHeight(x, y+1), getHeight(x+1, y+1)});
    }
    private float groundMaxHeightAt(int x1, int y1) {
        int x = floor(x1);
        int y = floor(y1);
        return max(new float[]{getHeight(x, y), getHeight(x+1, y), getHeight(x, y+1), getHeight(x+1, y+1)});
    }

    private void applyCameraPerspective() {
        float fov = PI/3.0f;
        float cameraZ = (papplet.height/2.0f) / tan(fov/2.0f);
        papplet.perspective(fov, PApplet.parseFloat(papplet.width)/PApplet.parseFloat(papplet.height), cameraZ/100.0f, cameraZ*20.0f);
        applyCamera();
    }


    private void applyCameraPerspective(PGraphics canvas) {
        float fov = PI/3.0f;
        float cameraZ = (papplet.height/2.0f) / tan(fov/2.0f);
        canvas.perspective(fov, PApplet.parseFloat(papplet.width)/PApplet.parseFloat(papplet.height), cameraZ/100.0f, cameraZ*20.0f);
        applyCamera(canvas);
    }


    private void applyCamera() {
        papplet.camera(focusedX+papplet.width/2f+zoom*sin(tilt)*sin(rot), focusedY+papplet.height/2f+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+papplet.width/2f, focusedY+papplet.height/2f, 0, 0, 0, -1);
    }


    private void applyCamera(PGraphics canvas) {
        canvas.camera(focusedX+papplet.width/2f+zoom*sin(tilt)*sin(rot), focusedY+papplet.height/2f+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+papplet.width/2f, focusedY+papplet.height/2f, 0, 0, 0, -1);
    }

    public void applyInvCamera(PGraphics canvas) {
        canvas.camera(focusedX+papplet.width/2f+zoom*sin(tilt)*sin(rot), focusedY+papplet.height/2f+zoom*sin(tilt)*cos(rot), -zoom*cos(tilt), focusedX+papplet.width/2f, focusedY+papplet.height/2f, 0, 0, 0, -1);
    }

    public void keyboardControls() {
    }


    public void draw(PGraphics panelCanvas) {
        try {
            // Update camera position and orientation
            int frameTime = papplet.millis() - prevT;
            prevT = papplet.millis();
            focusedV.x=0;
            focusedV.y=0;
            float rotv = 0;
            float tiltv = 0;
            float zoomv = 0;
            float PANSPEED = 0.5f;
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
            float ROTSPEED = 0.002f;
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
            PVector p = focusedV.copy().rotate(-rot).mult(frameTime *pow(zoom, 0.5f)/20);
            focusedX += p.x;
            focusedY += p.y;
            rot += rotv * frameTime;
            tilt += tiltv * frameTime;
            zoom += zoomv * frameTime;


            if (panning) {
                float panningSpeed = 0.05f;
                focusedX -= (focusedX-targetXOffset)* panningSpeed * frameTime *60/1000;
                focusedY -= (focusedY-targetYOffset)* panningSpeed * frameTime *60/1000;
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


            papplet.pushStyle();
            papplet.hint(ENABLE_DEPTH_TEST);

            // Render 3D stuff from normal camera view onto refraction canvas for refraction effect in water
            //refractionCanvas.beginDraw();
            //refractionCanvas.background(#7ED7FF);
            //float fov = PI/3.0;
            //float cameraZ = (papplet.height/2.0) / tan(fov/2.0);
            //applyCamera(refractionCanvas);
            ////refractionCanvas.perspective(fov, float(papplet.width)/float(papplet.height), 1, 100);
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
            papplet.hint(DISABLE_DEPTH_TEST);
            papplet.camera();
            papplet.noLights();
            papplet.resetShader();
            papplet.popStyle();

            //draw the scene to the screen
            panelCanvas.image(canvas, 0, 0);
            //image(refractionCanvas, 0, 0);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error drawing 3D map", e);
            throw e;
        }
    }

    private void renderWater(PGraphics canvas) {
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

    private void drawPath(PGraphics canvas) {
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

    private void renderScene(PGraphics canvas) {

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

            float verySmallSize = VERYSMALLSIZE*(400+(zoom-papplet.height/3f)*(cos(tilt)+1))/10;
            if (moveNodes != null) {
                canvas.pushMatrix();
                canvas.translate(0, 0, verySmallSize);
                canvas.shape(drawPossibleMoves);
                canvas.translate(0, 0, verySmallSize);
                canvas.shape(dangerousCellsOverlay);
                canvas.popMatrix();
            }

            if (JSONManager.loadBooleanSetting("fog of war")) {
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
                            if (buildingObjs.get(buildingString(visibleCells[y][x].getBuilding().getType())) != null) {
                                canvas.lights();
                                canvas.pushMatrix();
                                if (visibleCells[y][x].building.getType()==buildingIndex("Mine")) {
                                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, 16+groundMinHeightAt(x, y));
                                    canvas.rotateZ(getDownwardAngle(x, y));
                                } else if (visibleCells[y][x].building.getType()==buildingIndex("Quarry")) {
                                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, groundMinHeightAt(x, y));
                                } else {
                                    canvas.translate((x+0.5f)*blockSize, (y+0.5f)*blockSize, 16+groundMaxHeightAt(x, y));
                                }
                                canvas.shape(buildingObjs.get(buildingString(visibleCells[y][x].building.getType()))[visibleCells[y][x].building.getImageId()]);
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

    private void drawUnitBar(int x, int y, PGraphics canvas) {
        try {
            if (visibleCells[y][x].party instanceof Battle) {
                Battle battle = (Battle) visibleCells[y][x].party;
                unitNumberObjects[battle.attacker.player].setVertex(0, blockSize*battle.attacker.getUnitNumber()/JSONManager.loadIntSetting("party size"), 0, 0);
                unitNumberObjects[battle.attacker.player].setVertex(1, blockSize*battle.attacker.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
                unitNumberObjects[battle.attacker.player].setVertex(2, blockSize, blockSize*0.0625f, 0);
                unitNumberObjects[battle.attacker.player].setVertex(3, blockSize, 0, 0);
                unitNumberObjects[battle.attacker.player].setVertex(4, 0, 0, 0);
                unitNumberObjects[battle.attacker.player].setVertex(5, 0, blockSize*0.0625f, 0);
                unitNumberObjects[battle.attacker.player].setVertex(6, blockSize*battle.attacker.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
                unitNumberObjects[battle.attacker.player].setVertex(7, blockSize*battle.attacker.getUnitNumber()/JSONManager.loadIntSetting("party size"), 0, 0);
                unitNumberObjects[battle.defender.player].setVertex(0, blockSize*battle.defender.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
                unitNumberObjects[battle.defender.player].setVertex(1, blockSize*battle.defender.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.125f, 0);
                unitNumberObjects[battle.defender.player].setVertex(2, blockSize, blockSize*0.125f, 0);
                unitNumberObjects[battle.defender.player].setVertex(3, blockSize, blockSize*0.0625f, 0);
                unitNumberObjects[battle.defender.player].setVertex(4, 0, blockSize*0.0625f, 0);
                unitNumberObjects[battle.defender.player].setVertex(5, 0, blockSize*0.125f, 0);
                unitNumberObjects[battle.defender.player].setVertex(6, blockSize*battle.defender.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.125f, 0);
                unitNumberObjects[battle.defender.player].setVertex(7, blockSize*battle.defender.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.0625f, 0);
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
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(0, blockSize*visibleCells[y][x].party.getUnitNumber()/JSONManager.loadIntSetting("party size"), 0, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(1, blockSize*visibleCells[y][x].party.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.125f, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(2, blockSize, blockSize*0.125f, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(3, blockSize, 0, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(4, 0, 0, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(5, 0, blockSize*0.125f, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(6, blockSize*visibleCells[y][x].party.getUnitNumber()/JSONManager.loadIntSetting("party size"), blockSize*0.125f, 0);
                unitNumberObjects[visibleCells[y][x].party.player].setVertex(7, blockSize*visibleCells[y][x].party.getUnitNumber()/JSONManager.loadIntSetting("party size"), 0, 0);
                canvas.shape(unitNumberObjects[visibleCells[y][x].party.player]);
                canvas.popMatrix();
            }
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error drawing unit bar", e);
            throw e;
        }
    }

    private String buildingString(int buildingI) {
        if (gameData.getJSONArray("buildings").isNull(buildingI)) {
            LOGGER_MAIN.warning("invalid building string: "+(buildingI-1));
            return null;
        }
        return gameData.getJSONArray("buildings").getJSONObject(buildingI).getString("id");
    }

    public boolean mouseOver() {
        return papplet.mouseX > x && papplet.mouseX < x+w && papplet.mouseY > y && papplet.mouseY < y+h;
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
        return start.dist(targetV) < blockSize/JSONManager.loadFloatSetting("terrain detail");
    }



    // Ray Tracing Code Below is an example by Bontempos, modified for papplet.height map intersection by Jack Parsons
    // https://forum.processing.org/two/discussion/21644/picking-in-3d-through-ray-tracing-method

    // Function that calculates the coordinates on the floor surface corresponding to the screen coordinates
    private PVector getUnProjectedPointOnFloor(float screen_x, float screen_y, PVector floorPosition, PVector floorDirection) {

        try {
            PVector f = floorPosition.copy(); // Position of the floor
            PVector n = floorDirection.copy(); // The direction of the floor ( normal vector )
            PVector w = unProject(screen_x, screen_y, -1.0f); // 3 -dimensional coordinate corresponding to a point on the screen
            PVector e = getEyePosition(); // Viewpoint position

            // Computing the intersection of
            f.sub(e);
            assert w != null;
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
    private PVector getEyePosition() {
        applyCameraPerspective();
        PMatrix3D mat = (PMatrix3D)papplet.getMatrix(); //Get the model view matrix
        mat.invert();
        return new PVector( mat.m03, mat.m13, mat.m23 );
    }
    //Function to perform the conversion to the local coordinate system ( reverse projection ) from the window coordinate system
    private PVector unProject(float winX, float winY, float winZ) {
        PMatrix3D mat = getMatrixLocalToWindow();
        mat.invert();

        float[] in = {winX, winY, winZ, 1.0f};
        float[] out = new float[4];
        mat.mult(in, out);  // Do not use PMatrix3D.mult(PVector, PVector)

        if (out[3] == 0 ) {
            return null;
        }

        return new PVector(out[0]/out[3], out[1]/out[3], out[2]/out[3]);
    }

    //Function to compute the transformation matrix to the window coordinate system from the local coordinate system
    private PMatrix3D getMatrixLocalToWindow() {
        try {
            PMatrix3D projection = ((PGraphics3D)papplet.g).projection;
            PMatrix3D modelview = ((PGraphics3D)papplet.g).modelview;

            // viewport transf matrix
            PMatrix3D viewport = new PMatrix3D();
            viewport.m00 = viewport.m03 = papplet.width/2f;
            viewport.m11 = -papplet.height/2f;
            viewport.m13 =  papplet.height/2f;

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

    private void drawRocket(PGraphics canvas) {
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

    private void drawBombard(PGraphics canvas) {
        canvas.shape(bombardArrow);
    }

    private void updateBombard() {
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
            papplet.fill(255, 0, 0);
            bombardArrow = papplet.createShape();
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