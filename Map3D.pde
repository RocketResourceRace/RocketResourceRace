
class Map3D extends Element{
  final int thickness = 10;
  int x, y, w, h, mapWidth, mapHeight, focusedX, focusedY;
  Building[][] buildings;
  int[][] terrain;
  Party[][] parties;
  PShape tiles;
  PImage[] tempTileImages;
  float targetZoom, zoom;
  Boolean zooming, panning, mapActive;
  Node[][] moveNodes;
  
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
    tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++){
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
    }
    tiles = createShape(GROUP);
    for(int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        PShape t = createShape();
        t.beginShape();
        t.vertex(0+64*x, 0+64*y, 0);   
        t.vertex(64+64*x, 0+64*y, 0);
        t.vertex(64+64*x, 64+64*y, 0);
        t.vertex(0+64*x, 64+64*y, 0);
        t.endShape(CLOSE);
        t.setTexture(tempTileImages[terrain[y][x]-1]);
        tiles.addChild(t);
      }
    }
  }
  
  void setZoom(int zoom){
    this.zoom = zoom;
  }
  
  ArrayList<String> mouseEvent(String eventType, int button){
    if (eventType.equals("mouseDragged")){
      focusedX -= mouseX-pmouseX;
      focusedY -= mouseY-pmouseY;
    }
    
    
    return new ArrayList<String>();
  }
  
  void draw(){
    println(frameRate);
    camera(focusedX+width/2, focusedY+height/2, (height), focusedX+width/2, focusedY+height/2, 0, 0, 1, 0);
    shape(tiles);
    camera();
  }
  
  boolean mouseOver(){
    return mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h;
  }
}
