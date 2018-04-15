

class Map3D extends Element{
  final int thickness = 10;
  final float PANSPEED = 0.5, ROTSPEED = 0.002;
  final int FORESTDENSITY = 20;
  final float STUMPR = 0.5, STUMPH = 4, LEAVESR = 6, LEAVESH = 20, TREERANDOMNESS=0.3;
  int x, y, w, h, mapWidth, mapHeight, prevT, frameTime;
  int selectedCellX, selectedCellY;
  Building[][] buildings;
  int[][] terrain;
  float[][] heights;
  Party[][] parties;
  PShape tiles, blueFlag, redFlag, trees, selectTile;
  HashMap<String, PShape[]> buildingObjs;
  PImage[] tempTileImages;
  float targetZoom, zoom, zoomv, tilt, tiltv, rot, rotv, focusedX, focusedY;
  PVector focusedV, heldPos;
  Boolean zooming, panning, mapActive, cellSelected;
  Node[][] moveNodes;
  float blockSize = 16;
  ArrayList<int[]> drawPath;
  HashMap<Integer, Integer> forestTiles;
  
  Map3D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight){
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
    forestTiles = new HashMap<Integer, Integer>();
  }
   
    
  void updateMoveNodes(Node[][] nodes){      
    moveNodes = nodes;
  }
  void updatePath(ArrayList<int[]> nodes){
    drawPath = nodes;
  }
  void cancelMoveNodes(){
    moveNodes = null;
  }
  void cancelPath(){
    drawPath = null;
  }
  void loadSettings(float mapXOffset, float mapYOffset, float blockSize){}
  float[] targetCell(int x, int y, float zoom){return new float[2];
  }
  
  
  void selectCell(int x, int y){
    cellSelected = true;
    selectedCellX = x;
    selectedCellY = y;
  }
  void unselectCell(){
    cellSelected = false;
  }
  
  float scaleXInv(int x){
    PVector mo = MousePosOnObject(mouseX, mouseY);
    return (mo.x)/getObjectWidth()*mapWidth;
  }
  float scaleYInv(int y){
    PVector mo = MousePosOnObject(mouseX, mouseY);
    return (mo.y)/getObjectHeight()*mapHeight;
  }
  void reset(int mapWidth, int mapHeight, int [][] terrain, Party[][] parties, Building[][] buildings) {
    this.terrain = terrain;
    this.parties = parties;
    this.buildings = buildings;
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
  }
  
  void addTreeTile(int cellX, int cellY, int i){
    forestTiles.put(cellX+cellY*mapWidth, i);
  }
  void removeTreeTile(int cellX, int cellY){
    trees.removeChild(forestTiles.get(cellX+cellY*mapWidth));
    for (Integer i: forestTiles.keySet()){
      if (forestTiles.get(i) > forestTiles.get(cellX+cellY*mapWidth)){
        forestTiles.put(i, forestTiles.get(i)-1);
      }
    }
    forestTiles.remove(cellX+cellY*mapWidth);
  }
  
  PShape generateTrees(int num, int vertices){
    PShape shapes = createShape(GROUP);
    colorMode(HSB, 100);
    for (int i=0; i<num; i++){
      float x = random(0, blockSize), y = random(0, blockSize);
      int leafColour = color(random(35, 40), random(90, 100), random(30, 60));
      PShape leaves = createShape();
      leaves.beginShape(TRIANGLES);
      leaves.fill(leafColour);
      
      // create cylinder
      for (int j=0; j<vertices; j++){
        leaves.vertex(x+cos(j*TWO_PI/vertices), y+sin(j*TWO_PI/vertices), STUMPH);
        leaves.vertex(x, y, STUMPH+LEAVESH*random(1-TREERANDOMNESS, 1+TREERANDOMNESS));
        leaves.vertex(x+cos((j+1)*TWO_PI/vertices)*LEAVESR, y+sin((j+1)*TWO_PI/vertices)*LEAVESR, STUMPH);
      }
      leaves.endShape(CLOSE);
      shapes.addChild(leaves);
    }
    colorMode(RGB);
    return shapes;
  }
  void generateShape(){
    pushStyle();
    noFill();
    noStroke();
    tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++){
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
      tempTileImages[i].resize(graphicsRes, graphicsRes);
    }
    heights = new float[mapHeight+1][mapWidth+1];
    PGraphics tempTerrain;
    float noiseScale = 0.15;
    for(int y=0; y<mapHeight+1; y++){
      for (int x=0; x<mapWidth+1; x++){
        heights[y][x] = noise(x*noiseScale, y*noiseScale)*blockSize*2-blockSize;
      }
    }
    
    tiles = createShape(GROUP);
    textureMode(IMAGE);
    trees = createShape(GROUP);
    PShape t;
    int numTreeTiles=0;
    for(int y=0; y<mapHeight; y++){
      tempTerrain = createGraphics(round((1+mapWidth)*graphicsRes), round(graphicsRes));
      tempTerrain.beginDraw();
      for (int x=0; x<mapWidth; x++){
        tempTerrain.image(tempTileImages[terrain[y][x]-1], x*graphicsRes, 0);
      }
      tempTerrain.endDraw();
      
      t = createShape();
      t.setTexture(tempTerrain);
      t.beginShape(TRIANGLE_STRIP);
      resetMatrix();
      for (int x=0; x<mapWidth; x++){
        t.vertex(x*blockSize, y*blockSize, heights[y][x], x*graphicsRes, 0);   
        t.vertex(x*blockSize, (y+1)*blockSize, heights[y+1][x], x*graphicsRes, graphicsRes);
        if (terrain[y][x] == JSONIndex(gameData.getJSONArray("terrain"), "forest")+1){
          PShape cellTree = generateTrees(FORESTDENSITY, 16);
          cellTree.translate((x)*blockSize, (y)*blockSize, groundHeightAt(x, y));
          trees.addChild(cellTree);
          addTreeTile(x, y, numTreeTiles++);
        }
      }
      t.vertex(mapWidth*blockSize, y*blockSize, heights[y][mapWidth], mapWidth*graphicsRes, 0);   
      t.vertex(mapWidth*blockSize, (y+1)*blockSize, heights[y+1][mapWidth], mapWidth*graphicsRes, graphicsRes);
      t.endShape();
      tiles.addChild(t);
      
    }
    
    
    resetMatrix();
    
    blueFlag = loadShape("blueflag.obj");
    blueFlag.rotateX(PI/2);
    blueFlag.scale(3);
    redFlag = loadShape("redflag.obj");
    redFlag.rotateX(PI/2);
    redFlag.scale(3);
    
    for (int i=0; i<gameData.getJSONArray("buildings").size(); i++){
      JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
      if (!buildingType.isNull("obj")){
        buildingObjs.put(buildingType.getString("id"), new PShape[buildingType.getJSONArray("obj").size()]);
        for (int j=0; j<buildingType.getJSONArray("obj").size(); j++){
          buildingObjs.get(buildingType.getString("id"))[j] = loadShape(buildingType.getJSONArray("obj").getString(j));
          buildingObjs.get(buildingType.getString("id"))[j].rotateX(PI/2);
        }
      }
    }
    
    stroke(0);
    strokeWeight(2);
    //fill(0, 100);
    popStyle();
  }
  
  PVector MousePosOnObject(int mx, int my){
    camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
    PVector floorPos = new PVector(focusedX+width/2, focusedY+height/2, 0); 
    PVector floorDir = new PVector(0, 0, -1);
    PVector mousePos = getUnProjectedPointOnFloor( mouseX, mouseY, floorPos, floorDir);
    camera();
    return mousePos;
  }
  
  float getObjectWidth(){
    return mapWidth*blockSize;
  }
  float getObjectHeight(){
    return mapHeight*blockSize;
  }
  void setZoom(float zoom){
    this.zoom =  between(height/4, zoom, min(mapHeight*blockSize, height*4));
  }
  void setTilt(float tilt){
    this.tilt = between(0.01, tilt, 3*PI/8);
  }
  void setRot(float rot){
    this.rot = rot;
  }
  void setFocused(float focusedX, float focusedY){
    this.focusedX = between(-width/2, focusedX, getObjectWidth()-width/2);
    this.focusedY = between(-height/2, focusedY, getObjectHeight()-height/2);
  }
  
  ArrayList<String> mouseEvent(String eventType, int button){
    if (eventType.equals("mouseDragged")){
      if (mouseButton == LEFT){
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
      }
      else if (mouseButton != RIGHT){
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
  
  
  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){
    if (eventType == "mouseWheel"){
      float count = event.getCount();
      setZoom(zoom+zoom*count*0.15);
    }
    return new ArrayList<String>();
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (eventType.equals("keyPressed")){
      if (_key == 'w'){
        focusedV.y -= PANSPEED;
      }
      if (_key == 's'){
        focusedV.y += PANSPEED;
      }
      if (_key == 'a'){
        focusedV.x -= PANSPEED;
      }
      if (_key == 'd'){
        focusedV.x += PANSPEED;
      }
      if (_key == 'q'){
        rotv += ROTSPEED;
      }
      if (_key == 'e'){
        rotv -= ROTSPEED;
      }
      if (_key == 'x'){
        tiltv += ROTSPEED;
      }
      if (_key == 'c'){
        tiltv -= ROTSPEED;
      }
      if (_key == 'f'){
        zoomv += PANSPEED;
      }
      if (_key == 'r'){
        zoomv -= PANSPEED;
      }
    }
    else if (eventType.equals("keyReleased")){
      if (_key == 'q'){
        rotv = 0;
      }
      if (_key == 'e'){
        rotv = 0;
      }
      if (_key == 'x'){
        tiltv = 0;
      }
      if (_key == 'c'){
        tiltv = 0;
      }
      if (_key == 'w'){
        focusedV.y = 0;
      }
      if (_key == 's'){
        focusedV.y = 0;
      }
      if (_key == 'a'){
        focusedV.x = 0;
      }
      if (_key == 'd'){
        focusedV.x = 0;
      }
      if (_key == 'f'){
        zoomv = 0;
      }
      if (_key == 'r'){
        zoomv = 0;
      }
    }
    return new ArrayList<String>();
  }
  
  float groundHeightAt(int x, int y){
    return min(new float[]{heights[y][x], heights[y+1][x], heights[y][x+1], heights[y+1][x+1]});
  }
  
  void draw(){
    background(#7ED7FF);
    
    frameTime = millis()-prevT;
    prevT = millis();
    PVector p = focusedV.copy().rotate(-rot).mult(frameTime*pow(zoom,0.5)/20);
    focusedX += p.x;
    focusedY += p.y;
    rot += rotv*frameTime;
    tilt += tiltv*frameTime;
    zoom += zoomv*frameTime;
    
    // Check camera ok
    setZoom(zoom);
    setRot(rot);
    setTilt(tilt);
    setFocused(focusedX, focusedY);
    pushStyle();
    hint(ENABLE_DEPTH_TEST);
    camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
    
    lights();
    directionalLight(255, 255, 251, 0, -1, -1);
    //ambientLight(100, 100, 100);
    
    shape(tiles);
    shape(trees);
    
    if (cellSelected){
      pushMatrix();
      stroke(0);
      strokeWeight(3);
      noFill();
      selectTile = createShape();
      selectTile.beginShape();
      selectTile.vertex(selectedCellX*blockSize, selectedCellY*blockSize, heights[selectedCellY][selectedCellX]);
      selectTile.vertex((selectedCellX+1)*blockSize, selectedCellY*blockSize, heights[selectedCellY][selectedCellX+1]);
      selectTile.vertex((selectedCellX+1)*blockSize, (selectedCellY+1)*blockSize, heights[selectedCellY+1][selectedCellX+1]);
      selectTile.vertex(selectedCellX*blockSize, (selectedCellY+1)*blockSize, heights[selectedCellY+1][selectedCellX]);
      selectTile.endShape(CLOSE);
      selectTile.setFill(color(0));
      shape(selectTile);
      popMatrix();
    }
    
    for (int x=0; x<mapWidth; x++){
      for (int y=0; y<mapHeight; y++){
        if (parties[y][x] != null && parties[y][x].player == 0){
          pushMatrix();
          translate((x+0.5)*blockSize, (y+0.5)*blockSize, 30+groundHeightAt(x, y));
          shape(blueFlag);
          popMatrix();
        }
        if (parties[y][x] != null && parties[y][x].player == 1){
          pushMatrix();
          translate((x+0.5)*blockSize, (y+0.5)*blockSize, 30+groundHeightAt(x, y));
          shape(redFlag);
          popMatrix();
        }
        if (buildings[y][x] != null){
          if (buildingObjs.get(buildingString(buildings[y][x].type)) != null){
            pushMatrix();
            translate((x+0.5)*blockSize, (y+0.5)*blockSize, 16+groundHeightAt(x, y));
            shape(buildingObjs.get(buildingString(buildings[y][x].type))[buildings[y][x].image_id]);
            popMatrix();
          }
        }
      }
    }
    
    hint(DISABLE_DEPTH_TEST);
    camera();
    noLights();
    popStyle();
  }
  
  String buildingString(int buildingI){
    if (gameData.getJSONArray("buildings").isNull(buildingI-1)){
      println("invalid building string ", buildingI-1);
      return null;
    }
    return gameData.getJSONArray("buildings").getJSONObject(buildingI-1).getString("id");
  }
  
  boolean mouseOver(){
    return mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h;
  }
  
  
  
  // Ray Tracing Code Below is an example by Bontempos
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
    w.add(e);
   
    return w;
  }
   
  // Function to get the position of the viewpoint in the current coordinate system
  PVector getEyePosition() {
    camera(focusedX+width/2+zoom*sin(tilt)*sin(rot), focusedY+height/2+zoom*sin(tilt)*cos(rot), zoom*cos(tilt), focusedX+width/2, focusedY+height/2, 0, 0, 0, -1);
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
