
class Map3D extends Element{
  final int thickness = 10;
  final float PANSPEED = 0.5, ROTSPEED = 0.002;
  int x, y, w, h, mapWidth, mapHeight, prevT, frameTime;
  Building[][] buildings;
  int[][] terrain;
  Party[][] parties;
  PShape tiles;
  PImage[] tempTileImages;
  float targetZoom, zoom, zoomv, tilt, tiltv, rot, rotv, focusedX, focusedY;
  PVector focusedV;
  Boolean zooming, panning, mapActive;
  Node[][] moveNodes;
  float blockSize = 16;
  
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
    focusedX = round(mapWidth*blockSize/2);
    focusedY = round(mapHeight*blockSize/2);
    focusedV = new PVector(0, 0);
  }
  
  void updateMoveNodes(Node[][] nodes){}
  void cancelMoveNodes(){}
  void loadSettings(float mapXOffset, float mapYOffset, float blockSize){}
  float[] targetCell(int x, int y, float zoom){return new float[2];}
  void unselectCell(){}
  void selectCell(int x, int y){}
  float scaleXInv(int x){return x;}
  float scaleYInv(int y){return y;}
  void updatePath(ArrayList<int[]> n){};
  void cancelPath(){}
  void reset(int mapWidth, int mapHeight, int [][] terrain, Party[][] parties, Building[][] buildings) {
    this.terrain = terrain;
    this.parties = parties;
    this.buildings = buildings;
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
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
    float[][] heights = new float[mapHeight*2+1][mapWidth*2];
    PGraphics tempTerrain;
    for(int y=0; y<mapHeight*2+1; y++){
      for (int x=0; x<mapWidth*2; x++){
        heights[y][x] = noise(x, y)*blockSize*0.0;
      }
    }
    
    tiles = createShape(GROUP);
    textureMode(IMAGE);
    PShape t;
    for(int y=0; y<mapHeight; y++){
      tempTerrain = createGraphics(round(mapWidth*graphicsRes), round(graphicsRes));
      tempTerrain.beginDraw();
      for (int x=0; x<mapWidth; x++){
        tempTerrain.image(tempTileImages[terrain[y][x]-1], x*graphicsRes, 0);
      }
      tempTerrain.endDraw();
      
      t = createShape();
      t.setTexture(tempTerrain);
      t.beginShape(TRIANGLE_STRIP);
      for (int x=0; x<mapWidth; x++){
        //translate();
        t.vertex(x*blockSize, y*blockSize, heights[y][x], x*graphicsRes, 0);   
        t.vertex(x*blockSize, (y+1)*blockSize, heights[y+1][x], x*graphicsRes, graphicsRes);
      }
      t.endShape();
      tiles.addChild(t);
    }
    popStyle();
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
        focusedX -= mouseX-pmouseX;
        focusedY -= mouseY-pmouseY;
      }
      else if (mouseButton != RIGHT){
        setTilt(tilt-(mouseY-pmouseY)*0.01);
        setRot(rot-(mouseX-pmouseX)*0.01);
      }
    }
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
  
  void draw(){
    frameTime = millis()-prevT;
    prevT = millis();
    PVector p = focusedV.copy().rotate(-rot).mult(frameTime);
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
    shape(tiles);
    camera();
    hint(DISABLE_DEPTH_TEST);
    popStyle();
    
  }
  
  boolean mouseOver(){
    return mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h;
  }
}
