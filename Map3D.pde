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





class Map3D extends BaseMap implements Map{
  final int thickness = 10;
  final float PANSPEED = 0.5, ROTSPEED = 0.002;
  final float STUMPR = 0.5, STUMPH = 4, LEAVESR = 5, LEAVESH = 15, TREERANDOMNESS=0.3;
  final float HILLRAISE = 1.05;
  final float GROUNDHEIGHT = 5;
  int x, y, w, h, prevT, frameTime;
  float hoveringX, hoveringY, oldHoveringX, oldHoveringY;
  int selectedCellX, selectedCellY;
  PShape tiles, blueFlag, redFlag, battle, trees, selectTile, water, tileRect, pathLine, highlightingGrid;
  HashMap<String, PShape> taskObjs;
  HashMap<String, PShape[]> buildingObjs;
  PShape[] unitNumberObjects;
  PImage[] tempTileImages;
  float targetZoom, zoom, zoomv, tilt, tiltv, rot, rotv, focusedX, focusedY;
  PVector focusedV, heldPos;
  Boolean zooming, panning, mapActive, cellSelected;
  Node[][] moveNodes;
  float blockSize = 16;
  ArrayList<int[]> drawPath;
  HashMap<Integer, Integer> forestTiles;
  PGraphics canvas, refractionCanvas;
  HashMap<Integer, HashMap<Integer, Float>> downwardAngleCache;

  Map3D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight) {
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
  }
  

  float getDownwardAngle(int x, int y){
    if(!downwardAngleCache.containsKey(y)){
      downwardAngleCache.put(y, new HashMap<Integer, Float>());
    }
    if(downwardAngleCache.get(y).containsKey(x)){
      return downwardAngleCache.get(y).get(x);
    } else {
      PVector maxZCoord = new PVector();
      PVector minZCoord = new PVector();
      float maxZ = 0;
      float minZ = 1;
      for (float y1 = y; y1<=y+1;y1+=1.0/jsManager.loadFloatSetting("terrain detail")){
        for (float x1 = x; x1<=x+1;x1+=1.0/jsManager.loadFloatSetting("terrain detail")){
          float z = getRawHeight(x1, y1);
          if(z > maxZ){
            maxZCoord = new PVector(x1, y1);
            maxZ = z;
          } else if (z < minZ){
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
  
  Node[][] getMoveNodes(){
    return moveNodes;
  }
  boolean isPanning(){
    return panning;
  }
  float getTargetZoom(){
    return targetZoom;
  }
  boolean isMoving(){
    return focusedV.x != 0 || focusedV.y != 0;
  }

  float getTargetOffsetX(){return 0;}
  float getTargetOffsetY(){return 0;}
  float getTargetBlockSize(){return 0;}
  float getZoom(){
    return zoom;
  }
  boolean isZooming(){
    return zooming;
  }
  float getFocusedX(){
    return focusedX;
  }
  float getFocusedY(){
    return focusedY;
  }
  void setActive(boolean a){
    this.mapActive = a;
  }
  void updateMoveNodes(Node[][] nodes) {
    moveNodes = nodes;
  }
  void updatePath(ArrayList<int[]> nodes) {
    float x0, y0;
    drawPath = nodes;
    pathLine = createShape();
    pathLine.beginShape();
    pathLine.noFill();
    pathLine.stroke(255,0,0);
    for (int i=0; i<drawPath.size()-1;i++){
      for (int u=0; u<blockSize/8; u++){
        x0 = drawPath.get(i)[0]+(drawPath.get(i+1)[0]-drawPath.get(i)[0])*u/8+0.5;
        y0 = drawPath.get(i)[1]+(drawPath.get(i+1)[1]-drawPath.get(i)[1])*u/8+0.5;
        pathLine.vertex(x0*blockSize, y0*blockSize, 5+getHeight(x0, y0));
      }
    }
    pathLine.vertex((selectedCellX+0.5)*blockSize, (selectedCellY+0.5)*blockSize, 5+getHeight(selectedCellX+0.5, selectedCellY+0.5));
    pathLine.endShape();
  }
  void cancelMoveNodes() {
    moveNodes = null;
  }
  void cancelPath() {
    drawPath = null;
  }
  void loadSettings(float mapXOffset, float mapYOffset, float blockSize) {
  }
  float[] targetCell(int x, int y, float zoom) {
    return new float[2];
  }


  void selectCell(int x, int y) {
    cellSelected = true;
    selectedCellX = x;
    selectedCellY = y;
  }
  void unselectCell() {
    cellSelected = false;
  }

  float scaleXInv() {
    return hoveringX;
  }
  float scaleYInv() {
    return hoveringY;
  }
  void updateHoveringScale(){
    PVector mo = MousePosOnObject(mouseX, mouseY);
    hoveringX = (mo.x)/getObjectWidth()*mapWidth;
    hoveringY = (mo.y)/getObjectHeight()*mapHeight;
  }

  void addTreeTile(int cellX, int cellY, int i) {
    forestTiles.put(cellX+cellY*mapWidth, i);
  }
  void removeTreeTile(int cellX, int cellY) {
    trees.removeChild(forestTiles.get(cellX+cellY*mapWidth));
    for (Integer i : forestTiles.keySet()) {
      if (forestTiles.get(i) > forestTiles.get(cellX+cellY*mapWidth)) {
        forestTiles.put(i, forestTiles.get(i)-1);
      }
    }
    forestTiles.remove(cellX+cellY*mapWidth);
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

  float getWaveHeight(float x, float y, float t){
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
    PShape shapes = createShape(GROUP);
    colorMode(HSB, 100);
    for (int i=0; i<num; i++) {
      float x = random(0, blockSize), y = random(0, blockSize);
      float h = getHeight((x1+x)/blockSize, (y1+y)/blockSize);
      if (h <= 0) continue;
      int leafColour = color(random(35, 40), random(90, 100), random(30, 60));
      PShape leaves = createShape();
      leaves.setShininess(0.1);
      leaves.beginShape(TRIANGLES);
      leaves.fill(leafColour);

      // create tree
      for (int j=0; j<vertices; j++) {
        leaves.vertex(x+cos(j*TWO_PI/vertices), y+sin(j*TWO_PI/vertices), STUMPH+h);
        leaves.vertex(x, y, STUMPH+LEAVESH*random(1-TREERANDOMNESS, 1+TREERANDOMNESS)+h);
        leaves.vertex(x+cos((j+1)*TWO_PI/vertices)*LEAVESR, y+sin((j+1)*TWO_PI/vertices)*LEAVESR, STUMPH+h);
      }
      leaves.endShape(CLOSE);
      shapes.addChild(leaves);
    }
    colorMode(RGB, 255);
    return shapes;
  }

  void clearShape(){
    // Use to clear references to large objects when exiting state
    water = null;
    trees = null;
    tiles = null;
    buildingObjs = new HashMap<String, PShape[]>();
    taskObjs = new HashMap<String, PShape>();
    forestTiles = new HashMap<Integer, Integer>();
  }

  void generateShape() {
    pushStyle();
    noFill();
    noStroke();
    tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      if(tile3DImages.containsKey(tileType.getString("id"))){
        tempTileImages[i] = tile3DImages.get(tileType.getString("id")).copy();
      } else {
        tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
      }
      tempTileImages[i].resize(jsManager.loadIntSetting("terrain texture resolution"), jsManager.loadIntSetting("terrain texture resolution"));
    }
    PGraphics tempTerrain;

    water = createShape(RECT, 0, 0, getObjectWidth(), getObjectHeight());
    water.translate(0, 0, 0.1);
    generateHighlightingGrid(4, 4);

    tiles = createShape(GROUP);
    textureMode(IMAGE);
    trees = createShape(GROUP);
    PShape t;
    int numTreeTiles=0;
    for (int y=0; y<mapHeight; y++) {
      tempTerrain = createGraphics(round((1+mapWidth)*jsManager.loadIntSetting("terrain texture resolution")), round(jsManager.loadIntSetting("terrain texture resolution")));
      tempTerrain.beginDraw();
      for (int x=0; x<mapWidth; x++) {
        tempTerrain.image(tempTileImages[terrain[y][x]-1], x*jsManager.loadIntSetting("terrain texture resolution"), 0);
      }
      tempTerrain.endDraw();

      for (int y1=0; y1<jsManager.loadFloatSetting("terrain detail"); y1++) {
        t = createShape();
        t.setTexture(tempTerrain);
        //t.setShininess(0.1);
        //t.setEmissive(color(0, 20, 20));
        //t.setSpecular(color(0, 20, 20));
        t.beginShape(TRIANGLE_STRIP);
        resetMatrix();
        t.vertex(0, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, 0, 0, y1*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
        t.vertex(0, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, 0, 0, (y1+1)*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
        for (int x=0; x<mapWidth; x++) {
          //int x1=(int)jsManager.loadFloatSetting("terrain detail");
          for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail"); x1++) {
            t.vertex((x+x1/jsManager.loadFloatSetting("terrain detail"))*blockSize, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), (y+y1/jsManager.loadFloatSetting("terrain detail"))), (x+x1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"), y1*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
            t.vertex((x+x1/jsManager.loadFloatSetting("terrain detail"))*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+(1+y1)/jsManager.loadFloatSetting("terrain detail")), (x+x1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"), (y1+1)*jsManager.loadIntSetting("terrain texture resolution")/jsManager.loadFloatSetting("terrain detail"));
          }
        }
        t.vertex(mapWidth*blockSize, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, (y+y1/jsManager.loadFloatSetting("terrain detail"))), mapWidth*jsManager.loadIntSetting("terrain texture resolution"), (y1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
        t.vertex(mapWidth*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, getHeight(mapWidth, y+(1.0+y1)/jsManager.loadFloatSetting("terrain detail")), mapWidth*jsManager.loadIntSetting("terrain texture resolution"), ((y1+1.0)/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
        //t.vertex(mapWidth*blockSize, (y+y1/jsManager.loadFloatSetting("terrain detail"))*blockSize, 0, mapWidth*jsManager.loadIntSetting("terrain texture resolution"), (y1/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
        //t.vertex(mapWidth*blockSize, (y+(1+y1)/jsManager.loadFloatSetting("terrain detail"))*blockSize, 0, mapWidth*jsManager.loadIntSetting("terrain texture resolution"), ((y1+1.0)/jsManager.loadFloatSetting("terrain detail"))*jsManager.loadIntSetting("terrain texture resolution"));
        t.endShape();
        tiles.addChild(t);
      }
      for (int x=0; x<mapWidth; x++) {
        if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest")+1) {
          PShape cellTree = generateTrees(jsManager.loadIntSetting("forest density"), 16, x*blockSize, y*blockSize);
          cellTree.translate((x)*blockSize, (y)*blockSize, 0);
          trees.addChild(cellTree);
          addTreeTile(x, y, numTreeTiles++);
        }
      }
    }
    resetMatrix();

    blueFlag = loadShape("blueflag.obj");
    blueFlag.rotateX(PI/2);
    blueFlag.scale(2.6, 3, 3);
    redFlag = loadShape("redflag.obj");
    redFlag.rotateX(PI/2);
    redFlag.scale(2.6, 3, 3);
    battle = loadShape("battle.obj");
    battle.rotateX(PI/2);
    
    
    int players = 2;
    fill(255);
    
    unitNumberObjects = new PShape[players+1];
    for (int i=0;i < players; i++){
      unitNumberObjects[i] = createShape();
      unitNumberObjects[i].beginShape(QUADS);
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
      unitNumberObjects[i].setStroke(false);
    }
    unitNumberObjects[2] = createShape();
    unitNumberObjects[2].beginShape(QUADS);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, 0, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, blockSize*0.125, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, blockSize*0.125, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, 0, 0);
    unitNumberObjects[2].fill(playerColours[0]);
    unitNumberObjects[2].vertex(0, 0, 0);
    unitNumberObjects[2].fill(playerColours[0]);
    unitNumberObjects[2].vertex(0, blockSize*0.125, 0);
    unitNumberObjects[2].fill(playerColours[0]);
    unitNumberObjects[2].vertex(blockSize, blockSize*0.125, 0);
    unitNumberObjects[2].fill(playerColours[0]);
    unitNumberObjects[2].vertex(blockSize, 0, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, 0, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, blockSize*0.125, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, blockSize*0.125, 0);
    unitNumberObjects[2].fill(120, 120, 120);
    unitNumberObjects[2].vertex(blockSize, 0, 0);
    unitNumberObjects[2].fill(playerColours[1]);
    unitNumberObjects[2].vertex(0, 0, 0);
    unitNumberObjects[2].fill(playerColours[1]);
    unitNumberObjects[2].vertex(0, blockSize*0.125, 0);
    unitNumberObjects[2].fill(playerColours[1]);
    unitNumberObjects[2].vertex(blockSize, blockSize*0.125, 0);
    unitNumberObjects[2].fill(playerColours[1]);
    unitNumberObjects[2].vertex(blockSize, 0, 0);
    unitNumberObjects[2].endShape();
    unitNumberObjects[2].rotateX(PI/2);
    unitNumberObjects[2].setStroke(false);
    tileRect = createShape();
    tileRect.beginShape();
    tileRect.noFill();
    tileRect.stroke(0);
    tileRect.strokeWeight(3);
    int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    int[] curLoc = {0, 0};
    for (int[] dir : directions){
      for (int i=0; i<jsManager.loadFloatSetting("terrain detail"); i++){
        tileRect.vertex(curLoc[0]*blockSize/jsManager.loadFloatSetting("terrain detail"), curLoc[1]*blockSize/jsManager.loadFloatSetting("terrain detail"), 0);
        curLoc[0] += dir[0];
        curLoc[1] += dir[1];
      }
    }
    tileRect.endShape(CLOSE);
    for (int i=0; i<gameData.getJSONArray("tasks").size(); i++) {
      JSONObject task = gameData.getJSONArray("tasks").getJSONObject(i);
      if (!task.isNull("img")) {
        PShape object = createShape(RECT, 0, 0, blockSize/2, blockSize/2);
        object.setFill(color(255, 255, 255));
        object.setTexture(taskImages[i]);
        taskObjs.put(task.getString("id"), object);
        taskObjs.get(task.getString("id")).rotateX(-PI/2);
      }
    }
    for (int i=0; i<gameData.getJSONArray("buildings").size(); i++) {
      JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
      if (!buildingType.isNull("obj")) {
        buildingObjs.put(buildingType.getString("id"), new PShape[buildingType.getJSONArray("obj").size()]);
        for (int j=0; j<buildingType.getJSONArray("obj").size(); j++) {
          buildingObjs.get(buildingType.getString("id"))[j] = loadShape(buildingType.getJSONArray("obj").getString(j));
          buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
        }
      }
    }

    popStyle();
  }
  
  void generateHighlightingGrid(int horizontals, int verticles){
    PShape line;
    // Load horizontal lines first
    highlightingGrid = createShape(GROUP);
    for (int i=0; i<horizontals; i++){
      line = createShape();
      line.beginShape();
      line.noFill();
      line.stroke(255,255,255);
      for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++){
        line.vertex(x1*blockSize/jsManager.loadFloatSetting("terrain detail")+(x-horizontals/2)*blockSize, i*blockSize+(y-verticles/2)*blockSize, 1+getHeight(x+x1/jsManager.loadFloatSetting("terrain detail"), y+i));
      }
      line.endShape();
      highlightingGrid.addChild(line);
    }
  }
  
  void updateHighlightingGrid(int x, int y, int horizontals, int verticles){
    // x, y are cell coordinates
    PShape line;
    for (int i=0; i<horizontals; i++){
      line = highlightingGrid.getChild(i);
      for (int x1=0; x1<jsManager.loadFloatSetting("terrain detail")*(verticles-1)+1; x1++){
        line.setVertex(x1, x1*blockSize/jsManager.loadFloatSetting("terrain detail")+(x-horizontals/2+1)*blockSize, i*blockSize+(y-verticles/2+1)*blockSize, 1+getHeight(x-horizontals/2+1+x1/jsManager.loadFloatSetting("terrain detail"), y-verticles/2+1+i));
      }
    }
  }

  void updateSelectionRect(int cellX, int cellY){
    int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    int[] curLoc = {0, 0};
    int a = 0;
    for (int[] dir : directions){
      for (int i=0; i<jsManager.loadFloatSetting("terrain detail"); i++){
        tileRect.setVertex(a++, curLoc[0]*blockSize/jsManager.loadFloatSetting("terrain detail"), curLoc[1]*blockSize/jsManager.loadFloatSetting("terrain detail"), getHeight(cellX+curLoc[0]/jsManager.loadFloatSetting("terrain detail"), cellY+curLoc[1]/jsManager.loadFloatSetting("terrain detail")));
        curLoc[0] += dir[0];
        curLoc[1] += dir[1];
      }
    }
  }

  PVector MousePosOnObject(int mx, int my) {
    applyCameraPerspective();
    PVector floorPos = new PVector(focusedX+width/2, focusedY+height/2, 0);
    PVector floorDir = new PVector(0, 0, -1);
    PVector mousePos = getUnProjectedPointOnFloor( mouseX, mouseY, floorPos, floorDir);
    camera();
    return mousePos;
  }

  float getObjectWidth() {
    return mapWidth*blockSize;
  }
  float getObjectHeight() {
    return mapHeight*blockSize;
  }
  void setZoom(float zoom) {
    this.zoom =  between(height/4, zoom, min(mapHeight*blockSize, height*4));
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
      float count = event.getCount();
      setZoom(zoom+zoom*count*0.15);
    }
    return new ArrayList<String>();
  }
  ArrayList<String> keyboardEvent(String eventType, char _key) {
    if (eventType.equals("keyPressed")) {
      if (_key == 'w') {
        focusedV.y -= PANSPEED;
      }
      if (_key == 's') {
        focusedV.y += PANSPEED;
      }
      if (_key == 'a') {
        focusedV.x -= PANSPEED;
      }
      if (_key == 'd') {
        focusedV.x += PANSPEED;
      }
      if (_key == 'q') {
        rotv -= ROTSPEED;
      }
      if (_key == 'e') {
        rotv += ROTSPEED;
      }
      if (_key == 'x') {
        tiltv += ROTSPEED;
      }
      if (_key == 'c') {
        tiltv -= ROTSPEED;
      }
      if (_key == 'f') {
        zoomv += PANSPEED;
      }
      if (_key == 'r') {
        zoomv -= PANSPEED;
      }
    } else if (eventType.equals("keyReleased")) {
      if (_key == 'w') {
        focusedV.y += PANSPEED;
      }
      if (_key == 's') {
        focusedV.y -= PANSPEED;
      }
      if (_key == 'a') {
        focusedV.x += PANSPEED;
      }
      if (_key == 'd') {
        focusedV.x -= PANSPEED;
      }
      if (_key == 'q') {
        rotv += ROTSPEED;
      }
      if (_key == 'e') {
        rotv -= ROTSPEED;
      }
      if (_key == 'x') {
        tiltv -= ROTSPEED;
      }
      if (_key == 'c') {
        tiltv += ROTSPEED;
      }
      if (_key == 'f') {
        zoomv -= PANSPEED;
      }
      if (_key == 'r') {
        zoomv += PANSPEED;
      }
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
      return (max(getRawHeight(x, y), jsManager.loadFloatSetting("water level"))-jsManager.loadIntSetting("water level"))*blockSize*GROUNDHEIGHT;
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

  void applyCameraPerspective(){
    float fov = PI/3.0;
    float cameraZ = (height/2.0) / tan(fov/2.0);
    perspective(fov, float(width)/float(height), cameraZ/100.0, cameraZ*10.0);
    applyCamera();
  }


  void applyCameraPerspective(PGraphics canvas){
    float fov = PI/3.0;
    float cameraZ = (height/2.0) / tan(fov/2.0);
    canvas.perspective(fov, float(width)/float(height), cameraZ/100.0, cameraZ*10.0);
    applyCamera(canvas);
  }


  void applyCamera(){
    camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
  }


  void applyCamera(PGraphics canvas){
    canvas.camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
  }

  void applyInvCamera(PGraphics canvas){
    canvas.camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), -zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
  }


  void draw(PGraphics panelCanvas) {

    // Update camera position and orientation
    frameTime = millis()-prevT;
    prevT = millis();
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
    
    // update highlight grid if hovering over diffent pos
    if (!(hoveringX == oldHoveringX && hoveringY == oldHoveringY) ){
      updateHighlightingGrid(floor(hoveringX), floor(hoveringY), 4, 4);
      oldHoveringX = hoveringX;
      oldHoveringY = hoveringY;
    }

    // Check camera ok
    setZoom(zoom);
    setRot(rot);
    setTilt(tilt);
    setFocused(focusedX, focusedY);

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

  void renderWater(PGraphics canvas){
    //Draw water
    canvas.pushMatrix();
    canvas.shape(water);
    canvas.popMatrix();
  }

  void drawPath(PGraphics canvas){
    float x0, y0;
    if (drawPath != null){
      canvas.shape(pathLine);
    }
  }

  void renderScene(PGraphics canvas){

    canvas.directionalLight(240, 255, 255, 0, -0.1, -1);
    //canvas.directionalLight(100, 100, 100, 0.1, 1, -1);
    //canvas.lightSpecular(102, 102, 102);
    canvas.shape(tiles);
    canvas.ambientLight(100, 100, 100);
    canvas.shape(trees);
    


    canvas.pushMatrix();
    //noLights();
    if (cellSelected) {
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


    for (int x=0; x<mapWidth; x++) {
      for (int y=0; y<mapHeight; y++) {
        if (buildings[y][x] != null) {
          if (buildingObjs.get(buildingString(buildings[y][x].type)) != null) {
            canvas.lights();
            canvas.pushMatrix();
            if(buildings[y][x].type==buildingIndex("Mine")){
              canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, 16+groundMinHeightAt(x, y));
              canvas.rotateZ(getDownwardAngle(x, y));
            } else {
              canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, 16+groundMaxHeightAt(x, y));
            }
            canvas.shape(buildingObjs.get(buildingString(buildings[y][x].type))[buildings[y][x].image_id]);
            canvas.popMatrix();
          }
        }
        if(parties[y][x] != null){
          canvas.noLights();
          if (parties[y][x].player == 0) {
            canvas.pushMatrix();
            canvas.translate((x+0.5-0.4)*blockSize, (y+0.5)*blockSize, 30+groundMinHeightAt(x, y));
            canvas.shape(blueFlag);
            canvas.popMatrix();
          } else if (parties[y][x].player == 1) {
            canvas.pushMatrix();
            canvas.translate((x+0.5-0.4)*blockSize, (y+0.5)*blockSize, 30+groundMinHeightAt(x, y));
            canvas.shape(redFlag);
            canvas.popMatrix();
          } else if (parties[y][x].player == 2) {
            canvas.pushMatrix();
            canvas.translate((x+0.5)*blockSize, (y+0.5)*blockSize, 16+groundMaxHeightAt(x, y));
            canvas.shape(battle);
            canvas.popMatrix();
          }
          if(parties[y][x].player!=2){
            canvas.noLights();
            canvas.pushMatrix();
            canvas.translate((x+0.5+sin(rot)*0.5)*blockSize, (y+0.5+cos(rot)*0.5)*blockSize, blockSize*2.25+groundMinHeightAt(x, y));
            canvas.rotateZ(-this.rot);
            canvas.translate(-0.5*blockSize, -0.5*blockSize);
            unitNumberObjects[parties[y][x].player].setVertex(0, blockSize*parties[y][x].getUnitNumber()/1000, 0, 0);
            unitNumberObjects[parties[y][x].player].setVertex(1, blockSize*parties[y][x].getUnitNumber()/1000, blockSize*0.125, 0);
            unitNumberObjects[parties[y][x].player].setVertex(6, blockSize*parties[y][x].getUnitNumber()/1000, blockSize*0.125, 0);
            unitNumberObjects[parties[y][x].player].setVertex(7, blockSize*parties[y][x].getUnitNumber()/1000, 0, 0);
            canvas.shape(unitNumberObjects[parties[y][x].player]);
            canvas.popMatrix();
          }
          JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(parties[y][x].task);
          if (jo != null && !jo.isNull("img")){
            canvas.noLights();
            canvas.pushMatrix();
            canvas.translate((x+0.5+sin(rot)*0.25)*blockSize, (y+0.5+cos(rot)*0.25)*blockSize, blockSize*3+groundMinHeightAt(x, y));
            canvas.rotateZ(-this.rot);
            canvas.translate(-0.25*blockSize, -0.25*blockSize);
            canvas.shape(taskObjs.get(jo.getString("id")));
            canvas.popMatrix();
          }
        }
      }
    }
    canvas.popMatrix();

    canvas.pushMatrix();
    drawPath(canvas);
    canvas.shape(highlightingGrid);
    canvas.popMatrix();

  }

  void renderTexturedEntities(PGraphics canvas){

  }

  String buildingString(int buildingI) {
    if (gameData.getJSONArray("buildings").isNull(buildingI-1)) {
      println("invalid building string ", buildingI-1);
      return null;
    }
    return gameData.getJSONArray("buildings").getJSONObject(buildingI-1).getString("id");
  }

  boolean mouseOver() {
    return mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h;
  }

  float getRayHeightAt(PVector r, PVector s, float targetX){
    PVector start = s.copy();
    PVector ray = r.copy();
    float dz_dx = ray.z/ray.x;
    return start.z + (targetX - start.x) * dz_dx;
  }

  boolean rayPassesThrough(PVector r, PVector s, PVector targetV){
    PVector start = s.copy();
    PVector ray = r.copy();
    start.add(ray);
    return start.dist(targetV) < blockSize/jsManager.loadFloatSetting("terrain detail");
  }



  // Ray Tracing Code Below is an example by Bontempos, modified for height map intersection by Jack Parsons
  // https://forum.processing.org/two/discussion/21644/picking-in-3d-through-ray-tracing-method

  // Function that calculates the coordinates on the floor surface corresponding to the screen coordinates
  PVector getUnProjectedPointOnFloor(float screen_x, float screen_y, PVector floorPosition, PVector floorDirection) {

    PVector f = floorPosition.get(); // Position of the floor
    PVector n = floorDirection.get(); // The direction of the floor ( normal vector )
    PVector w = unProject(screen_x, screen_y, -1.0); // 3 -dimensional coordinate corresponding to a point on the screen
    PVector e = getEyePosition(); // Viewpoint position

    // Computing the intersection of
    f.sub(e);
    w.sub(e);
    w.mult( n.dot(f)/n.dot(w) );
    PVector ray = w.copy();
    w.add(e);
    
    float acHeight, curX = e.x, curY = e.y;
    for (int i = 0; i < ray.mag(); i++){
      curX += ray.x/ray.mag();
      curY += ray.y/ray.mag();
      if (0 <= curX/blockSize && curX/blockSize <= mapWidth && 0 <= curY/blockSize && curY/blockSize < mapHeight){
        acHeight = getHeight(curX/blockSize, curY/blockSize);
        if (getRayHeightAt(ray, e, curX) < acHeight+0.0001){
          return new PVector(curX, curY,acHeight);
        }
      }
    }

    return w;
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
}
