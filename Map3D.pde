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
  final float PANSPEED = 0.5, ROTSPEED = 0.002;
  final float STUMPR = 1, STUMPH = 4, LEAVESR = 5, LEAVESH = 15, TREERANDOMNESS=0.3;
  final float HILLRAISE = 1.05;
  final float GROUNDHEIGHT = 5;
  final float VERYSMALLSIZE = 0.01;
  float panningSpeed = 0.05;
  int x, y, w, h, prevT, frameTime;
  float hoveringX, hoveringY, oldHoveringX, oldHoveringY;
  float targetXOffset, targetYOffset;
  int selectedCellX, selectedCellY;
  PShape tiles, flagPole, battle, trees, selectTile, water, tileRect, pathLine, highlightingGrid, drawPossibleMoves, drawPossibleBombards, obscuredCellsOverlay, bombardArrow, fog;
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
  color[] playerColours;

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
    heightMap = new float[int((mapWidth+1)*(mapHeight+1)*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
    targetXOffset = mapWidth/2*blockSize;
    targetYOffset = mapHeight/2*blockSize;
    updateHoveringScale = false;
    this.keyState = new HashMap<Character, Boolean>();
    showingBombard = false;
  }



  float getDownwardAngle(int x, int y) {
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
        for (float y1 = y; y1<=y+1; y1+=1.0/jsManager.loadFloatSetting("terrain detail")) {
          for (float x1 = x; x1<=x+1; x1+=1.0/jsManager.loadFloatSetting("terrain detail")) {
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

  Node[][] getMoveNodes() {
    return moveNodes;
  }
  boolean isPanning() {
    return panning;
  }
  float getTargetZoom() {
    return targetZoom;
  }
  boolean isMoving() {
    return focusedV.x != 0 || focusedV.y != 0;
  }

  float getTargetOffsetX() {
    return targetXOffset;
  }
  float getTargetOffsetY() {
    return targetYOffset;
  }
  float getTargetBlockSize() {
    return zoom;
  }
  float getZoom() {
    return zoom;
  }
  boolean isZooming() {
    return zooming;
  }
  float getFocusedX() {
    return focusedX;
  }
  float getFocusedY() {
    return focusedY;
  }
  void setActive(boolean a) {
    this.mapActive = a;
  }
  void updateMoveNodes(Node[][] nodes) {
    LOGGER_MAIN.finer("Updating move nodes");
    moveNodes = nodes;
    updatePossibleMoves();
  }

  void updatePath (ArrayList<int[]> path, int[] target) {
    // Use when updaing with a target node
    ArrayList<int[]> tempPath = new ArrayList<int[]>(path);
    tempPath.add(target);
    updatePath(tempPath);
  }

  void updatePath(ArrayList<int[]> path) {
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
        x0 = path.get(i)[0]+(path.get(i+1)[0]-path.get(i)[0])*u/8+0.5;
        y0 = path.get(i)[1]+(path.get(i+1)[1]-path.get(i)[1])*u/8+0.5;
        pathLine.vertex(x0*blockSize, y0*blockSize, 5+getHeight(x0, y0));
      }
    }
    if (drawPath != null) {
      pathLine.vertex((selectedCellX+0.5)*blockSize, (selectedCellY+0.5)*blockSize, 5+getHeight(selectedCellX+0.5, selectedCellY+0.5));
    }
    pathLine.endShape();
    drawPath = path;
  }
  
  
  void updateObscuredCellsOverlay(Cell[][] visibleCells) {
    // For the shape that indicates cells that are not currently under party sight. This is a temporary implementation
    try {
      LOGGER_MAIN.finer("Updating obscured cells overlay");
      float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
      obscuredCellsOverlay = createShape();
      obscuredCellsOverlay.beginShape(TRIANGLES);
      obscuredCellsOverlay.fill(0, 0, 0, 200);
      for (int x=0; x<mapWidth; x++) {
        for (int y=0; y<mapHeight; y++) {
          if (visibleCells[y][x] != null && !visibleCells[y][x].activeSight) {
            println("test");
            for (int x1=0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
              for (int y1=0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
                obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+y1*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));

                obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+y1*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+y1/jsManager.loadFloatSetting("terrain detail")));
                obscuredCellsOverlay.vertex(x*blockSize+(x1+1)*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+(x1+1)/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
                obscuredCellsOverlay.vertex(x*blockSize+x1*smallSize, y*blockSize+(y1+1)*smallSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(y1+1)/jsManager.loadFloatSetting("terrain detail")));
              }
            }
          }
        }
      }
      obscuredCellsOverlay.endShape();
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error updating obscured cells overlay", e);
      throw e;
    }
  }
  
  void updatePossibleMoves() {
    // For the shape that indicateds where a party can move
    try {
      LOGGER_MAIN.finer("Updating possible move nodes");
      float smallSize = blockSize / jsManager.loadFloatSetting("terrain detail");
      drawPossibleMoves = createShape();
      drawPossibleMoves.beginShape(TRIANGLES);
      drawPossibleMoves.fill(0, 0, 0, 100);
      for (int x=0; x<mapWidth; x++) {
        for (int y=0; y<mapHeight; y++) {
          if (moveNodes[y][x] != null && moveNodes[y][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()) {
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

  void updatePossibleBombards() {
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

  void cancelMoveNodes() {
    moveNodes = null;
  }
  void cancelPath() {
    drawPath = null;
  }


  void generateFog(int player) {
    generateFogMap(player);
  }

  void loadSettings(float x, float y, float blockSize) {
    LOGGER_MAIN.fine(String.format("Loading camera settings. cellX:%s, cellY:%s, block size: %s", x, y, blockSize));
    targetCell(int(x), int(y), blockSize);

    panning = true;
  }
  float[] targetCell(int x, int y, float zoom) {
    LOGGER_MAIN.finer(String.format("Targetting cell:%s, %s and zoom:%s", x, y, zoom));
    targetXOffset = (x+0.5)*blockSize-width/2;
    targetYOffset = (y+0.5)*blockSize-height/2;
    panning = true;
    return new float[]{targetXOffset, targetYOffset};
  }


  void selectCell(int x, int y) {
    cellSelected = true;
    selectedCellX = x;
    selectedCellY = y;
  }
  void unselectCell() {
    cellSelected = false;
    showingBombard = false;
  }

  float scaleXInv() {
    return hoveringX;
  }
  float scaleYInv() {
    return hoveringY;
  }
  void updateHoveringScale() {
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

  void doUpdateHoveringScale() {
    updateHoveringScale = true;
  }

  void addTreeTile(int cellX, int cellY, int i) {
    forestTiles.put(cellX+cellY*mapWidth, i);
  }
  void removeTreeTile(int cellX, int cellY) {
    try {
      trees.removeChild(forestTiles.get(cellX+cellY*mapWidth));
      for (Integer i : forestTiles.keySet()) {
        if (forestTiles.get(i) > forestTiles.get(cellX+cellY*mapWidth)) {
          forestTiles.put(i, forestTiles.get(i)-1);
        }
      }
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

  float getWaveHeight(float x, float y, float t) {
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

  PShape generateTrees(int num, int vertices, float x1, float y1) {
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
        leaves.setShininess(0.1);
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

  void loadMapStrip(int y, PShape tiles, boolean loading) {
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
              PShape quarrySite = loadShape("obj/building/quarry_site.obj");
              float quarryheight = groundMinHeightAt(x, y);
              quarrySite.rotateX(PI/2);
              quarrySite.translate((x+0.5)*blockSize, (y+0.5)*blockSize, quarryheight);
              quarrySite.setTexture(loadImage("img/terrain/hill.png"));
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
        tempSingleRow.vertex(mapWidth*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, y+(1.0+y1)/jsManager.loadFloatSetting("terrain detail")), mapWidth*jsManager.loadIntSetting("terrain texture resolution"), ((y1+1.0)/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
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

  void replaceMapStripWithReloadedStrip(int y) {
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

  void clearShape() {
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

  void generateShape() {
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
      int numTreeTiles=0;

      LOGGER_MAIN.fine("Generating trees and terrain model");
      for (int y=0; y<mapHeight; y++) {
        loadMapStrip(y, tiles, true);

        // Load trees
        for (int x=0; x<mapWidth; x++) {
          if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest")) {
            PShape cellTree = generateTrees(jsManager.loadIntSetting("forest density"), 8, x*blockSize, y*blockSize);
            cellTree.translate((x)*blockSize, (y)*blockSize, 0);
            trees.addChild(cellTree);
            addTreeTile(x, y, numTreeTiles++);
          }
        }
      }
      resetMatrix();

      LOGGER_MAIN.fine("Generating player flags");
      
      flagPole = loadShape("obj/party/flagpole.obj");
      flagPole.rotateX(PI/2);
      flagPole.scale(2, 2.5, 2.5);
      flags = new PShape[playerColours.length];
      for (int i = 0; i < playerColours.length; i++) {
        flags[i] = createShape(GROUP);
        PShape edge = loadShape("obj/party/flagedges.obj");
        edge.setFill(brighten(playerColours[i], 20));
        PShape side = loadShape("obj/party/flagsides.obj");
        side.setFill(brighten(playerColours[i], -40));
        flags[i].addChild(edge);
        flags[i].addChild(side);
        flags[i].rotateX(PI/2);
        flags[i].scale(2, 2.5, 2.5);
      }
      battle = loadShape("obj/party/battle.obj");
      battle.rotateX(PI/2);
      battle.scale(0.8);

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
        unitNumberObjects[i].vertex(blockSize, blockSize*0.125, 0);
        unitNumberObjects[i].fill(120, 120, 120);
        unitNumberObjects[i].vertex(blockSize, blockSize*0.125, 0);
        unitNumberObjects[i].fill(120, 120, 120);
        unitNumberObjects[i].vertex(blockSize, 0, 0);
        unitNumberObjects[i].fill(playerColours[i]);
        unitNumberObjects[i].vertex(0, 0, 0);
        unitNumberObjects[i].fill(playerColours[i]);
        unitNumberObjects[i].vertex(0, blockSize*0.125, 0);
        unitNumberObjects[i].fill(playerColours[i]);
        unitNumberObjects[i].vertex(blockSize, blockSize*0.125, 0);
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
          taskObjs.put(task.getString("id"), loadShape("obj/task/"+task.getString("obj")));
          taskObjs.get(task.getString("id")).translate(blockSize*0.125, -blockSize*0.2);
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
              buildingObjs.get(buildingType.getString("id"))[j] = loadShape("obj/building/quarry.obj");
              buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
              //buildingObjs.get(buildingType.getString("id"))[j].setFill(color(86, 47, 14));
            } else {
              buildingObjs.get(buildingType.getString("id"))[j] = loadShape("obj/building/"+buildingType.getJSONArray("obj").getString(j));
              buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
              buildingObjs.get(buildingType.getString("id"))[j].scale(0.625);
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
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading models", e);
      throw e;
    }
  }

  void generateHighlightingGrid(int horizontals, int verticles) {
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
          float x2 = -horizontals/2+0.5+x1/jsManager.loadFloatSetting("terrain detail");
          float y1 = -verticles/2+i+0.5;
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
          float y2 = -verticles/2+0.5+y1/jsManager.loadFloatSetting("terrain detail");
          float x1 = -horizontals/2+i+0.5;
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

  void updateHighlightingGrid(float x, float y, int horizontals, int verticles) {
    // x, y are cell coordinates
    try {
      //LOGGER_MAIN.finer(String.format("Updating selection rect at:%s, %s", x, y));
      PShape line;
      float alpha;
      if (jsManager.loadBooleanSetting("active cell highlighting")) {
        for (int i=0; i<horizontals; i++) {
          line = highlightingGrid.getChild(i);
          for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
            float x2 = int(x)-horizontals/2+1+x1/jsManager.loadFloatSetting("terrain detail");
            float y1 = int(y)-verticles/2+1+i;
            float x3 = -horizontals/2+x1/jsManager.loadFloatSetting("terrain detail");
            float y3 = -verticles/2+i;
            float dist = sqrt(pow(y3-y%1+1, 2)+pow(x3-x%1+1, 2));
            if (0 < x2 && x2 < mapWidth && 0 < y1 && y1 < mapHeight) {
              alpha = 255-dist/(verticles/2-1)*255;
            } else {
              alpha = 0;
            }
            line.setStroke(x1, color(255, alpha));
            line.setVertex(x1, x2*blockSize, y1*blockSize, 4.5*VERYSMALLSIZE+getHeight(x2, y1));
          }
        }
        // verticle lines
        for (int i=0; i<verticles; i++) {
          line = highlightingGrid.getChild(i+horizontals);
          for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
            float x1 = int(x)-horizontals/2+1+i;
            float y2 = int(y)-verticles/2+1+y1/jsManager.loadFloatSetting("terrain detail");
            float y3 = -verticles/2+y1/jsManager.loadFloatSetting("terrain detail");
            float x3 = -horizontals/2+i;
            float dist = sqrt(pow(y3-y%1+1, 2)+pow(x3-x%1+1, 2));
            if (0 < x1 && x1 < mapWidth && 0 < y2 && y2 < mapHeight) {
              alpha = 255-dist/(horizontals/2-1)*255;
            } else {
              alpha = 0;
            }
            line.setStroke(y1, color(255, alpha));
            line.setVertex(y1, x1*blockSize, y2*blockSize, 4.5*VERYSMALLSIZE+getHeight(x1, y2));
          }
        }
      } else {
        for (int i=0; i<horizontals; i++) {
          line = highlightingGrid.getChild(i);
          for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++) {
            float x2 = int(x)-horizontals/2+1+x1/jsManager.loadFloatSetting("terrain detail");
            float y1 = int(y)-verticles/2+1+i;
            line.setVertex(x1, x2*blockSize, y1*blockSize, 4.5*VERYSMALLSIZE+getHeight(x2, y1));
          }
        }
        // verticle lines
        for (int i=0; i<verticles; i++) {
          line = highlightingGrid.getChild(i+horizontals);
          for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail")*(horizontals-1)+1; y1++) {
            float x1 = int(x)-horizontals/2+1+i;
            float y2 = int(y)-verticles/2+1+y1/jsManager.loadFloatSetting("terrain detail");
            line.setVertex(y1, x1*blockSize, y2*blockSize, 4.5*VERYSMALLSIZE+getHeight(x1, y2));
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error updating highlighting grid", e);
      throw e;
    }
  }

  void updateSelectionRect(int cellX, int cellY) {
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

  PVector getMousePosOnObject() {
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

  float getObjectWidth() {
    return mapWidth*blockSize;
  }
  float getObjectHeight() {
    return mapHeight*blockSize;
  }
  void setZoom(float zoom) {
    this.zoom =  between(height/3, zoom, min(mapHeight*blockSize, height*4));
  }
  void setTilt(float tilt) {
    this.tilt = between(0.01, tilt, 3*PI/8);
  }
  void setRot(float rot) {
    this.rot = rot;
  }
  void setFocused(float focusedX, float focusedY) {
    this.focusedX = between(-width/2, focusedX, getObjectWidth()-width/2);
    this.focusedY = between(-height/2, focusedY, getObjectHeight()-height/2);
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
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
        setTilt(tilt-(mouseY-pmouseY)*0.01);
        setRot(rot-(mouseX-pmouseX)*0.01);
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


  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
    if (eventType == "mouseWheel") {
      doUpdateHoveringScale();
      float count = event.getCount();
      setZoom(zoom+zoom*count*0.15);
    }
    return new ArrayList<String>();
  }
  ArrayList<String> keyboardEvent(String eventType, char _key) {
    if (eventType == "keyPressed") {
      keyState.put(_key, true);
    }
    if (eventType == "keyReleased") {
      keyState.put(_key, false);
    }
    return new ArrayList<String>();
  }
  float getHeight(float x, float y) {
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
  float groundMinHeightAt(int x1, int y1) {
    int x = floor(x1);
    int y = floor(y1);
    return min(new float[]{getHeight(x, y), getHeight(x+1, y), getHeight(x, y+1), getHeight(x+1, y+1)});
  }
  float groundMaxHeightAt(int x1, int y1) {
    int x = floor(x1);
    int y = floor(y1);
    return max(new float[]{getHeight(x, y), getHeight(x+1, y), getHeight(x, y+1), getHeight(x+1, y+1)});
  }

  void applyCameraPerspective() {
    float fov = PI/3.0;
    float cameraZ = (height/2.0) / tan(fov/2.0);
    perspective(fov, float(width)/float(height), cameraZ/100.0, cameraZ*20.0);
    applyCamera();
  }


  void applyCameraPerspective(PGraphics canvas) {
    float fov = PI/3.0;
    float cameraZ = (height/2.0) / tan(fov/2.0);
    canvas.perspective(fov, float(width)/float(height), cameraZ/100.0, cameraZ*20.0);
    applyCamera(canvas);
  }


  void applyCamera() {
    camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
  }


  void applyCamera(PGraphics canvas) {
    canvas.camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
  }

  void applyInvCamera(PGraphics canvas) {
    canvas.camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), -zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
  }

  void keyboardControls() {
  }


  void draw(PGraphics panelCanvas) {
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
      PVector p = focusedV.copy().rotate(-rot).mult(frameTime*pow(zoom, 0.5)/20);
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
        if (showingBombard && !(int(hoveringX) == int(oldHoveringX) && int(hoveringY) == int(oldHoveringY))) {
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
      canvas.background(#7ED7FF);
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

  void renderWater(PGraphics canvas) {
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

  void drawPath(PGraphics canvas) {
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

  void renderScene(PGraphics canvas) {

    try {
      canvas.directionalLight(240, 255, 255, 0, -0.1, -1);
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
        canvas.popMatrix();
      }
      
      if (jsManager.loadBooleanSetting("fog of war")) {
        canvas.pushMatrix();
        canvas.translate(0, 0, verySmallSize);
        canvas.shape(obscuredCellsOverlay);
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
          if (buildings[y][x] != null) {
            if (buildingObjs.get(buildingString(buildings[y][x].type)) != null) {
              canvas.lights();
              canvas.pushMatrix();
              if (buildings[y][x].type==buildingIndex("Mine")) {
                canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, 16+groundMinHeightAt(x, y));
                canvas.rotateZ(getDownwardAngle(x, y));
              } else if (buildings[y][x].type==buildingIndex("Quarry")) {
                canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, groundMinHeightAt(x, y));
              } else {
                canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, 16+groundMaxHeightAt(x, y));
              }
              canvas.shape(buildingObjs.get(buildingString(buildings[y][x].type))[buildings[y][x].image_id]);
              canvas.popMatrix();
            }
          }
          if (parties[y][x] != null) {
            canvas.noLights();
            if (parties[y][x] instanceof Battle) {
              // Swords
              canvas.pushMatrix();
              canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, 12+groundMaxHeightAt(x, y));
              canvas.shape(battle);
              canvas.popMatrix();
              
              // Defender
              canvas.pushMatrix();
              canvas.translate((x+0.5+0.1)*blockSize, (y+0.5)*blockSize, 30.5+groundMinHeightAt(x, y));
              canvas.scale(0.95, 0.8, 0.8);
              canvas.shape(flags[((Battle)parties[y][x]).defender.player]);
              canvas.scale(5.0/9.5, 5.0/8.0, 1);
              canvas.shape(flagPole);
              canvas.popMatrix();
              
              // Attacker
              canvas.pushMatrix();
              canvas.translate((x+0.5-0.1)*blockSize, (y+0.5)*blockSize, 30.5+groundMinHeightAt(x, y));
              canvas.scale(-0.95, 0.8, 0.8);
              canvas.shape(flags[((Battle)parties[y][x]).attacker.player]);
              canvas.scale(5.0/9.5, 5.0/8.0, 1);
              canvas.shape(flagPole);
              canvas.popMatrix();
            } else {
              canvas.pushMatrix();
              canvas.translate((x+0.5-0.4)*blockSize, (y+0.5)*blockSize, 23+groundMinHeightAt(x, y));
              canvas.shape(flagPole);
              canvas.shape(flags[parties[y][x].player]);
              canvas.popMatrix();
            }

            if (drawingUnitBars&&!cinematicMode) {
              drawUnitBar(x, y, canvas);
            }

            JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(parties[y][x].task);
            if (drawingTaskIcons && jo != null && !jo.isNull("img") && !cinematicMode) {
              canvas.noLights();
              canvas.pushMatrix();
              canvas.translate((x+0.5+sin(rot)*0.125)*blockSize, (y+0.5+cos(rot)*0.125)*blockSize, blockSize*1.7+groundMinHeightAt(x, y));
              canvas.rotateZ(-this.rot);
              canvas.translate(-0.125*blockSize, -0.25*blockSize);
              canvas.rotateX(PI/2-this.tilt);
              canvas.translate(0, 0, blockSize*0.35);
              canvas.shape(taskObjs.get(jo.getString("id")));
              canvas.popMatrix();
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

  void drawUnitBar(int x, int y, PGraphics canvas) {
    try {
      if (parties[y][x] instanceof Battle) {
        Battle battle = (Battle) parties[y][x];
        unitNumberObjects[battle.attacker.player].setVertex(0, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
        unitNumberObjects[battle.attacker.player].setVertex(1, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625, 0);
        unitNumberObjects[battle.attacker.player].setVertex(2, blockSize, blockSize*0.0625, 0);
        unitNumberObjects[battle.attacker.player].setVertex(3, blockSize, 0, 0);
        unitNumberObjects[battle.attacker.player].setVertex(4, 0, 0, 0);
        unitNumberObjects[battle.attacker.player].setVertex(5, 0, blockSize*0.0625, 0);
        unitNumberObjects[battle.attacker.player].setVertex(6, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625, 0);
        unitNumberObjects[battle.attacker.player].setVertex(7, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
        unitNumberObjects[battle.defender.player].setVertex(0, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625, 0);
        unitNumberObjects[battle.defender.player].setVertex(1, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125, 0);
        unitNumberObjects[battle.defender.player].setVertex(2, blockSize, blockSize*0.125, 0);
        unitNumberObjects[battle.defender.player].setVertex(3, blockSize, blockSize*0.0625, 0);
        unitNumberObjects[battle.defender.player].setVertex(4, 0, blockSize*0.0625, 0);
        unitNumberObjects[battle.defender.player].setVertex(5, 0, blockSize*0.125, 0);
        unitNumberObjects[battle.defender.player].setVertex(6, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125, 0);
        unitNumberObjects[battle.defender.player].setVertex(7, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.0625, 0);
        canvas.noLights();
        canvas.pushMatrix();
        canvas.translate((x+0.5+sin(rot)*0.5)*blockSize, (y+0.5+cos(rot)*0.5)*blockSize, blockSize*1.6+groundMinHeightAt(x, y));
        canvas.rotateZ(-this.rot);
        canvas.translate(-0.5*blockSize, -0.5*blockSize);
        canvas.rotateX(PI/2-this.tilt);
        canvas.shape(unitNumberObjects[battle.attacker.player]);
        canvas.shape(unitNumberObjects[battle.defender.player]);
        canvas.popMatrix();
      } else {
        canvas.noLights();
        canvas.pushMatrix();
        canvas.translate((x+0.5+sin(rot)*0.5)*blockSize, (y+0.5+cos(rot)*0.5)*blockSize, blockSize*1.6+groundMinHeightAt(x, y));
        canvas.rotateZ(-this.rot);
        canvas.translate(-0.5*blockSize, -0.5*blockSize);
        canvas.rotateX(PI/2-this.tilt);
        unitNumberObjects[parties[y][x].player].setVertex(0, blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
        unitNumberObjects[parties[y][x].player].setVertex(1, blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125, 0);
        unitNumberObjects[parties[y][x].player].setVertex(2, blockSize, blockSize*0.125, 0);
        unitNumberObjects[parties[y][x].player].setVertex(3, blockSize, 0, 0);
        unitNumberObjects[parties[y][x].player].setVertex(4, 0, 0, 0);
        unitNumberObjects[parties[y][x].player].setVertex(5, 0, blockSize*0.125, 0);
        unitNumberObjects[parties[y][x].player].setVertex(6, blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size"), blockSize*0.125, 0);
        unitNumberObjects[parties[y][x].player].setVertex(7, blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size"), 0, 0);
        canvas.shape(unitNumberObjects[parties[y][x].player]);
        canvas.popMatrix();
      }
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error drawing unit bar", e);
      throw e;
    }
  }

  String buildingString(int buildingI) {
    if (gameData.getJSONArray("buildings").isNull(buildingI)) {
      LOGGER_MAIN.warning("invalid building string: "+(buildingI-1));
      return null;
    }
    return gameData.getJSONArray("buildings").getJSONObject(buildingI).getString("id");
  }

  boolean mouseOver() {
    return mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h;
  }

  float getRayHeightAt(PVector r, PVector s, float targetX) {
    PVector start = s.copy();
    PVector ray = r.copy();
    float dz_dx = ray.z/ray.x;
    return start.z + (targetX - start.x) * dz_dx;
  }

  boolean rayPassesThrough(PVector r, PVector s, PVector targetV) {
    PVector start = s.copy();
    PVector ray = r.copy();
    start.add(ray);
    return start.dist(targetV) < blockSize/jsManager.loadFloatSetting("terrain detail");
  }



  // Ray Tracing Code Below is an example by Bontempos, modified for height map intersection by Jack Parsons
  // https://forum.processing.org/two/discussion/21644/picking-in-3d-through-ray-tracing-method

  // Function that calculates the coordinates on the floor surface corresponding to the screen coordinates
  PVector getUnProjectedPointOnFloor(float screen_x, float screen_y, PVector floorPosition, PVector floorDirection) {

    try {
      PVector f = floorPosition.copy(); // Position of the floor
      PVector n = floorDirection.copy(); // The direction of the floor ( normal vector )
      PVector w = unProject(screen_x, screen_y, -1.0); // 3 -dimensional coordinate corresponding to a point on the screen
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
          if (curZ < acHeight+0.000001) {
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
  PVector getEyePosition() {
    applyCameraPerspective();
    PMatrix3D mat = (PMatrix3D)getMatrix(); //Get the model view matrix
    mat.invert();
    return new PVector( mat.m03, mat.m13, mat.m23 );
  }
  //Function to perform the conversion to the local coordinate system ( reverse projection ) from the window coordinate system
  PVector unProject(float winX, float winY, float winZ) {
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
  PMatrix3D getMatrixLocalToWindow() {
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
  void enableRocket(PVector pos, PVector vel) {
    drawRocket = true;
    rocketPosition = pos;
    rocketVelocity = vel;
  }

  void disableRocket() {
    drawRocket = false;
  }

  void drawRocket(PGraphics canvas) {
    try {
      canvas.lights();
      canvas.pushMatrix();
      canvas.translate((rocketPosition.x+0.5)*blockSize, (rocketPosition.y+0.5)*blockSize, rocketPosition.z*blockSize+16+groundMaxHeightAt(int(rocketPosition.x), int(rocketPosition.y)));
      canvas.rotateY(atan2(rocketVelocity.x, rocketVelocity.z));
      canvas.shape(buildingObjs.get("Rocket Factory")[2]);
      canvas.popMatrix();
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error drawing rocket", e);
      throw e;
    }
  }

  void reset() {
    cinematicMode = false;
    drawRocket = false;
    showingBombard = false;
  }
  
  void drawBombard(PGraphics canvas) {
    canvas.shape(bombardArrow);
  }
  
  void updateBombard() {
    PVector pos = getMousePosOnObject();
    int x = floor(pos.x/blockSize);
    int y = floor(pos.y/blockSize);
    if (pos.equals(new PVector(-1, -1, -1)) || dist(x, y, selectedCellX, selectedCellY) > bombardRange || (x == selectedCellX && y == selectedCellY)) {
      bombardArrow.setVisible(false);
    } else {
      LOGGER_MAIN.finer("Loading bombard arrow");
      PVector startPos = new PVector((selectedCellX+0.5)*blockSize, (selectedCellY+0.5)*blockSize, getHeight(selectedCellX+0.5, selectedCellY+0.5));
      PVector endPos = new PVector((x+0.5)*blockSize, (y+0.5)*blockSize, getHeight(x+0.5, y+0.5));
      float rotation = -atan2(startPos.x-endPos.x, startPos.y - endPos.y)+0.0001;
      PVector thicknessAdder = new PVector(blockSize*0.1*cos(rotation), blockSize*0.1*sin(rotation), 0);
      PVector startPosA = PVector.add(startPos, thicknessAdder);
      PVector startPosB = PVector.sub(startPos, thicknessAdder);
      PVector endPosA = PVector.add(endPos, thicknessAdder);
      PVector endPosB = PVector.sub(endPos, thicknessAdder);
      fill(255, 0, 0);
      bombardArrow = createShape();
      bombardArrow.beginShape(TRIANGLES);
      float INCREMENT = 0.01;
      PVector currentPosA = startPosA.copy();
      PVector currentPosB = startPosB.copy();
      PVector nextPosA;
      PVector nextPosB;
      for (float i = 0; i <= 1 && currentPosA.dist(endPosA) > blockSize*0.2; i += INCREMENT) {
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
  
  void enableBombard(int range) {
    showingBombard = true;
    bombardRange = range;
    updateBombard();
    updatePossibleBombards();
  }
  
  void disableBombard() {
    showingBombard = false;
  }
  
  void setPlayerColours(color[] playerColours) {
    this.playerColours = playerColours;
  }
}
