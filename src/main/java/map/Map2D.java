package map;

import json.JSONManager;
import party.Battle;
import party.Party;
import player.Player;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONObject;
import processing.event.MouseEvent;
import util.Node;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import static json.JSONManager.gameData;
import static json.JSONManager.terrainIndex;
import static processing.core.PApplet.*;
import static util.Font.getFont;
import static util.Image.*;
import static util.Logging.LOGGER_MAIN;
import static util.Util.brighten;
import static util.Util.papplet;

public class Map2D extends BaseMap implements Map {
    public float blockSize;
    public float targetBlockSize;
    public float mapXOffset;
    public float mapYOffset;
    public float targetXOffset;
    public float targetYOffset;
    private float panningSpeed;
    private float resetTime;
    private boolean panning=false, zooming=false;
    private float mapMaxSpeed;
    private int elementWidth;
    private int elementHeight;
    private float[] mapVelocity = {0, 0};
    private int startX;
    private int startY;
    private int frameStartTime;
    public int xPos;
    public int yPos;
    boolean zoomChanged;
    private boolean mapFocused, mapActive;
    private int selectedCellX, selectedCellY;
    private boolean cellSelected;
    int partyManagementColour;
    private Node[][] moveNodes;
    private ArrayList<int[]> drawPath;
    private boolean drawRocket;
    private PVector rocketPosition;
    private PVector rocketVelocity;
    private boolean showingBombard;
    private int bombardRange;
    private int[] playerColours;
    private Player[] players;

    public Map2D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight, Player[] players) {
        LOGGER_MAIN.fine("Initialsing map");
        xPos = x;
        yPos = y;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        elementWidth = round(w);
        elementHeight = round(h);
        mapXOffset = 0;
        mapYOffset = 0;
        mapMaxSpeed = 15;
        this.terrain = terrain;
        this.parties = parties;
        this.buildings = buildings;
        limitCoords();
        frameStartTime = 0;
        cancelMoveNodes();
        cancelPath();
        heightMap = new float[PApplet.parseInt((mapWidth+1)*(mapHeight+1)*pow(JSONManager.loadFloatSetting("terrain detail"), 2))];
        this.keyState = new HashMap<>();
        this.players = players;
    }


    public void generateShape() {
        cinematicMode = false;
        drawRocket = false;
        this.keyState = new HashMap<>();
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
    private void setPanningSpeed(float s) {
      panningSpeed = s;
    }
    private void limitCoords() {
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
        resetTime = papplet.millis();
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
    public void targetCell(int x, int y, float bs) {
        LOGGER_MAIN.finer(String.format("Targetting cell: %s, %s block size: %f", x, y, bs));
        targetBlockSize = bs;
        targetXOffset = -(x+0.5f)*targetBlockSize+elementWidth/2f+xPos;
        targetYOffset = -(y+0.5f)*targetBlockSize+elementHeight/2f+yPos;
        panning = true;
        zooming = true;
    }
    public void focusMapMouse() {
        // based on mouse click
        if (mouseOver()) {
            targetXOffset = -scaleXInv()*blockSize+elementWidth/2f+xPos;
            targetYOffset = -scaleYInv()*blockSize+elementHeight/2f+yPos;
            limitCoords();
            panning = true;
        }
    }
    private void resetTarget() {
        targetXOffset = mapXOffset;
        targetYOffset = mapYOffset;
        panning = false;
    }
    private void resetTargetZoom() {
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
        if (eventType.equals("mouseWheel")) {
            float count = event.getCount();
            if (mouseOver() && mapActive) {
                float zoom = pow(0.9f, count);
                float newBlockSize = max(min(blockSize*zoom, (float)elementWidth/10), (float)elementWidth/(float)JSONManager.loadIntSetting("map size"));
                if (blockSize != newBlockSize) {
                    mapXOffset = scaleX(((papplet.mouseX-mapXOffset-xPos)/blockSize))-xPos-((papplet.mouseX-mapXOffset-xPos)*newBlockSize/blockSize);
                    mapYOffset = scaleY(((papplet.mouseY-mapYOffset-yPos)/blockSize))-yPos-((papplet.mouseY-mapYOffset-yPos)*newBlockSize/blockSize);
                    blockSize = newBlockSize;
                    limitCoords();
                    resetTarget();
                    resetTargetZoom();
                }
            }
        }
        return new ArrayList<>();
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        if (button == LEFT&mapActive) {
            if (eventType.equals("mouseDragged") && mapFocused) {
                mapXOffset += (papplet.mouseX-startX);
                mapYOffset += (papplet.mouseY-startY);
                limitCoords();
                startX = papplet.mouseX;
                startY = papplet.mouseY;
                resetTarget();
                resetTargetZoom();
            }
            if (eventType.equals("mousePressed")) {
                if (mouseOver()) {
                    startX = papplet.mouseX;
                    startY = papplet.mouseY;
                    mapFocused = true;
                } else {
                    mapFocused = false;
                }
            }
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

    public void setWidth(int w) {
      this.elementWidth = w;
    }

    private void drawSelectedCell(PVector c, PGraphics panelCanvas) {
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
            frameStartTime = papplet.millis();
        }
        int frameTime = papplet.millis()-frameStartTime;
        int INITIALHOLD = 1000;
        if (papplet.millis()-resetTime < INITIALHOLD) {
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
        frameStartTime = papplet.millis();
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
                tempBuildingImagesDark[i][j] = buildingImages.get(buildingType.getString("id"))[j].copy();
                tempBuildingImagesDark[i][j].resize(ceil(blockSize), 0);
                tempBuildingImagesDark[i][j].loadPixels();
                for (int k = 0; k < tempBuildingImagesDark[i][j].pixels.length; k++) {
                    tempBuildingImagesDark[i][j].pixels[k] = brighten(tempBuildingImagesDark[i][j].pixels[k], -100);
                }
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
                if (JSONManager.loadBooleanSetting("fog of war") && visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
                    panelCanvas.image(tempTileImagesDark[terrain[y][x]], x2, y2);
                } else if (!JSONManager.loadBooleanSetting("fog of war") || visibleCells[y][x] != null) {
                    panelCanvas.image(tempTileImages[terrain[y][x]], x2, y2);
                }

                //Buildings
                if (buildings[y][x] != null) {
                    c = new PVector(scaleX(x), scaleY(y));
                    int border = round((64-48)*blockSize/(2*64));
                    int imgSize = round(blockSize*48/60);
                    PImage p;
                    if (JSONManager.loadBooleanSetting("fog of war") && visibleCells[y][x] != null && !visibleCells[y][x].getActiveSight()) {
                        p = tempBuildingImagesDark[buildings[y][x].getType()][buildings[y][x].getImageId()];
                    } else {
                        p = tempBuildingImages[buildings[y][x].getType()][buildings[y][x].getImageId()];
                    }
                    drawCroppedImage(round(c.x+border), round(c.y+border*2), imgSize, imgSize, p, panelCanvas);
                }
                //Parties
                if (!JSONManager.loadBooleanSetting("fog of war") || (visibleCells[y][x] != null && visibleCells[y][x].party != null)) {
                    c = new PVector(scaleX(x), scaleY(y));
                    if (c.x<xPos+elementWidth&&c.y+blockSize/8+blockSize>yPos&&c.y<yPos+elementHeight) {
                        panelCanvas.noStroke();
                        if (parties[y][x] instanceof Battle) {
                            Battle battle = (Battle) parties[y][x];
                            if (c.x+blockSize>xPos) {
                                panelCanvas.fill(120, 120, 120);
                                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
                            }
                            if (c.x+blockSize*parties[y][x].getUnitNumber()/JSONManager.loadIntSetting("party size")>xPos) {
                                panelCanvas.fill(playerColours[battle.attacker.player]);
                                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize*battle.attacker.getUnitNumber()/JSONManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*battle.attacker.getUnitNumber()/JSONManager.loadIntSetting("party size")+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
                            }
                            if (c.x+blockSize>xPos) {
                                panelCanvas.fill(120, 120, 120);
                                panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/16+c.y-yPos)));
                            }
                            if (c.x+blockSize*parties[y][x].getUnitNumber()/JSONManager.loadIntSetting("party size")>xPos) {
                                panelCanvas.fill(playerColours[battle.defender.player]);
                                panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize*battle.defender.getUnitNumber()/JSONManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*battle.defender.getUnitNumber()/JSONManager.loadIntSetting("party size")+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/8+c.y-yPos)));
                            }
                        } else {
                            if (c.x+blockSize>xPos) {
                                panelCanvas.fill(120, 120, 120);
                                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
                            }
                            if (c.x+blockSize*parties[y][x].getUnitNumber()/JSONManager.loadIntSetting("party size")>xPos) {
                                panelCanvas.fill(playerColours[parties[y][x].player]);
                                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize*parties[y][x].getUnitNumber()/JSONManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*parties[y][x].getUnitNumber()/JSONManager.loadIntSetting("party size")+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
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
                                if (c.x+papplet.textWidth(turnsLeftString) < elementWidth+xPos && c.y+2*(papplet.textAscent()+papplet.textDescent()) < elementHeight+yPos) {
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
                            if (blockSize > 10*JSONManager.loadFloatSetting("text scale") && moveNodes[y1][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()) {
                                shadeCell(panelCanvas, c.x, c.y, papplet.color(50, 150));
                                panelCanvas.fill(255);
                                panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
                                panelCanvas.textAlign(CENTER, CENTER);
                                String s = ""+moveNodes[y1][x].cost;
                                s = s.substring(0, min(s.length(), 3));
                                BigDecimal cost = new BigDecimal(s);
                                String s2 = cost.stripTrailingZeros().toPlainString();
                                if (c.x+panelCanvas.textWidth(s2) < elementWidth+xPos && c.y+2*(papplet.textAscent()+papplet.textDescent()) < elementHeight+yPos) {
                                    panelCanvas.text(s2, c.x+blockSize/2, c.y+blockSize/2);
                                }
                            }
                        }
                    }
                }
            }
        }

        //if (JSONManager.loadBooleanSetting("fog of war")) {
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
                        shadeCell(panelCanvas, scaleX(x), scaleY(y), papplet.color(255, 0, 0, 100));
                    }
                }
            }
            drawBombard(panelCanvas);
        }

        panelCanvas.noFill();
        panelCanvas.stroke(0);
        panelCanvas.rect(xPos, yPos, elementWidth, elementHeight);
    }

    private void shadeCell(PGraphics canvas, float x, float y, int c) {
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
    private void drawCroppedImage(int x, int y, int w, int h, PImage img, PGraphics panelCanvas) {
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
    private float scaleX(float x) {
      return x*blockSize + mapXOffset + xPos;
    }
    private float scaleY(float y) {
      return y*blockSize + mapYOffset + yPos;
    }
    public float scaleXInv() {
        return (papplet.mouseX-mapXOffset-xPos)/blockSize;
    }
    public float scaleYInv() {
        return (papplet.mouseY-mapYOffset-yPos)/blockSize;
    }
    public boolean mouseOver() {
        return papplet.mouseX > xPos && papplet.mouseX < xPos+elementWidth && papplet.mouseY > yPos && papplet.mouseY < yPos+elementHeight;
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

    private void drawRocket(PGraphics canvas, PImage[][] buildingImages) {
        PVector c = new PVector(scaleX(rocketPosition.x+0.5f), scaleY(rocketPosition.y+0.5f-rocketPosition.z));
        int border = round((64-48)*blockSize/(2*64));
        canvas.pushMatrix();
        canvas.translate(round(c.x+border), round(c.y+border*2));
        canvas.rotate(atan2(rocketVelocity.x, rocketVelocity.z));
        canvas.translate(-blockSize/2, -blockSize/2);
        canvas.image(buildingImages[JSONManager.buildingIndex("Rocket Factory")][2], 0, 0);
        canvas.popMatrix();
    }

    private void drawBombard(PGraphics canvas) {
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