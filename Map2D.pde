import java.util.Collections;
import java.nio.ByteBuffer;

interface Map {
  void updateMoveNodes(Node[][] nodes);
  void cancelMoveNodes();
  void removeTreeTile(int cellX, int cellY);
  boolean isPanning();
  float getFocusedX();
  float getFocusedY();
  boolean isZooming();
  float getTargetZoom();
  float getZoom();
  float getTargetOffsetX();
  float getTargetOffsetY();
  float getTargetBlockSize();
  float[] targetCell(int x, int y, float zoom);
  void loadSettings(float x, float y, float bs);
  void unselectCell();
  boolean mouseOver();
  Node[][] getMoveNodes();
  float scaleXInv(int x);
  float scaleYInv(int y);
  void updatePath(ArrayList<int[]> nodes);
  void cancelPath();
  void setActive(boolean a);
  void selectCell(int x, int y);
  void reset(Party[][] parties);
  void generateShape();
  void clearShape();
}
class BaseMap extends Element{
  float[] heightMap;
  int mapWidth, mapHeight;
  long heightMapSeed;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  void saveMap(String filename){
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES*2+Long.BYTES+Integer.BYTES*mapWidth*mapHeight*3);
    buffer.putInt(mapWidth);
    buffer.putInt(mapHeight);
    buffer.putLong(heightMapSeed);
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        buffer.putInt(terrain[y][x]);
      }
    }
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if(buildings[y][x]==null){
          buffer.putInt(-1);
          buffer.putInt(-1);
        } else {
          buffer.putInt(buildings[y][x].type);
          buffer.putInt(buildings[y][x].image_id);
        }
      }
    }
    saveBytes(filename, buffer.array());
  }
  void loadMap(String filename){
    byte tempBuffer[] = loadBytes(filename);
    int sizeSize = Integer.BYTES*2;
    ByteBuffer sizeBuffer = ByteBuffer.allocate(sizeSize);
    sizeBuffer.put(Arrays.copyOfRange(tempBuffer, 0, sizeSize));
    sizeBuffer.flip();//need flip
    mapWidth = sizeBuffer.getInt();
    mapHeight = sizeBuffer.getInt();
    int dataSize = Long.BYTES+Integer.BYTES*mapWidth*mapHeight*3;
    ByteBuffer buffer = ByteBuffer.allocate(dataSize);
    buffer.put(Arrays.copyOfRange(tempBuffer, sizeSize, sizeSize+dataSize));
    buffer.flip();//need flip
    heightMapSeed = buffer.getLong();
    terrain = new int[mapHeight][mapWidth];
    buildings = new Building[mapHeight][mapWidth];
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        terrain[y][x] = buffer.getInt();
      }
    }
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        int type = buffer.getInt();
        int image_id = buffer.getInt();
        if(type!=-1){
          buildings[y][x] = new Building(type, image_id);
        }
      }
    }
  }
  int toMapIndex(int x, int y, int x1, int y1){
    return int(x1+x*VERTICESPERTILE+y1*VERTICESPERTILE*(mapWidth+1/VERTICESPERTILE)+y*pow(VERTICESPERTILE, 2)*(mapWidth+1/VERTICESPERTILE));
  }
  
  int getRandomGroundType(HashMap<Integer, Float> groundWeightings, float total){
    float randomNum = random(0, 1);
    float min = 0;
    int lastType = 1;
    for (int type: groundWeightings.keySet()){
      if(randomNum>min&&randomNum<min+groundWeightings.get(type)/total){
        return type;
      }
      min += groundWeightings.get(type)/total;
      lastType = type;
    }
    return lastType;
  }
  int[][] smoothMap(int distance, int firstType, int[][] terrain){
    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=0; y<mapHeight;y++){
      for (int x=0; x<mapWidth;x++){
        order.add(new int[] {x, y});
      }
    }
    Collections.shuffle(order);
    int[][] newMap = new int[mapHeight][mapWidth];
    for (int[] coord: order){
      if(terrain[coord[1]][coord[0]]==terrainIndex("water")){
        newMap[coord[1]][coord[0]] = terrain[coord[1]][coord[0]];
      } else {
        int[] counts = new int[NUMOFGROUNDTYPES+1];
        for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
          for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
            if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
              if(terrain[y1][x1]!=terrainIndex("water")){
                counts[terrain[y1][x1]]+=1;
              }
            }
          }
        }
        int highest = terrain[coord[1]][coord[0]];
        for (int i=firstType; i<=NUMOFGROUNDTYPES;i++){
          if (counts[i] > counts[highest]){
            highest = i;
          }
        }
        newMap[coord[1]][coord[0]] = highest;
      }
    }
    return newMap;
  }
  void generateTerrain(){
    HashMap<Integer, Float> groundWeightings = new HashMap();
    for (Integer i=1; i<gameData.getJSONArray("terrain").size()+1; i++){
      groundWeightings.put(i, gameData.getJSONArray("terrain").getJSONObject(i-1).getFloat("weighting"));
    }

    float totalWeighting = 0;
    for (float weight: groundWeightings.values()){
      totalWeighting+=weight;
    }
    for(int i=0;i<groundSpawns;i++){
      int type = getRandomGroundType(groundWeightings, totalWeighting);
      int x = (int)random(mapWidth);
      int y = (int)random(mapHeight);
      if(isWater(x, y)){
        i--;
      } else {
        terrain[y][x] = type;
      }
    }

    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=0; y<mapHeight;y++){
      for (int x=0; x<mapWidth;x++){
        if(isWater(x, y)){
          terrain[y][x] = terrainIndex("water");
        } else {
          order.add(new int[] {x, y});
        }
      }
    }
    Collections.shuffle(order);
    for (int[] coord: order){
      int x = coord[0];
      int y = coord[1];
      while (terrain[y][x] == 0||terrain[y][x]==terrainIndex("water")){
        int direction = (int) random(8);
        switch(direction){
          case 0:
            x= max(x-1, 0);
            break;
          case 1:
            x = min(x+1, mapWidth-1);
            break;
          case 2:
            y= max(y-1, 0);
            break;
          case 3:
            y = min(y+1, mapHeight-1);
            break;
          case 4:
            x = min(x+1, mapWidth-1);
            y = min(y+1, mapHeight-1);
            break;
          case 5:
            x = min(x+1, mapWidth-1);
            y= max(y-1, 0);
            break;
          case 6:
            y= max(y-1, 0);
            x= max(x-1, 0);
            break;
          case 7:
            y = min(y+1, mapHeight-1);
            x= max(x-1, 0);
            break;
        }
      }
      terrain[coord[1]][coord[0]] = terrain[y][x];
    }
    terrain = smoothMap(initialSmooth, 2, terrain);
    terrain = smoothMap(completeSmooth, 1, terrain);
    for (int y=0; y<mapHeight; y++){
      for(int x=0; x<mapWidth; x++){
        if(terrain[y][x] != terrainIndex("water") && (groundMaxRawHeightAt(x, y) > 0.5+WATERLEVEL/2.0) || getMaxSteepness(x, y)>HILLSTEEPNESS){
          terrain[y][x] = terrainIndex("hills");
        }
      }
    }
  }
  void generateMap(int mapWidth, int mapHeight){
    terrain = new int[mapHeight][mapWidth];
    buildings = new Building[mapHeight][mapWidth];
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    if(loading){
      loadMap("saves/test.dat");
      noiseSeed(heightMapSeed);
      generateNoiseMaps();
    } else {
      heightMapSeed = (long)random(Long.MIN_VALUE, Long.MAX_VALUE);
      noiseSeed(heightMapSeed);
      generateNoiseMaps();
      generateTerrain();
    }
  }
  void generateNoiseMaps(){
    heightMap = new float[int((mapWidth+1/VERTICESPERTILE)*(mapHeight+1/VERTICESPERTILE)*pow(VERTICESPERTILE, 2))];
    for(int y = 0;y<mapHeight;y++){
      for(int y1 = 0;y1<VERTICESPERTILE;y1++){
        for(int x = 0;x<mapWidth;x++){
          for(int x1 = 0;x1<VERTICESPERTILE;x1++){
            heightMap[toMapIndex(x, y, x1, y1)] = noise((x+x1/VERTICESPERTILE)*MAPNOISESCALE, (y+y1/VERTICESPERTILE)*MAPNOISESCALE);
          }
        }
        heightMap[toMapIndex(mapWidth, y, 0, y1)] = noise(((mapWidth+1))*MAPNOISESCALE, (y+y1/VERTICESPERTILE)*MAPNOISESCALE);
      }
    }
    for(int x = 0;x<mapWidth;x++){
      for(int x1 = 0;x1<VERTICESPERTILE;x1++){
        heightMap[toMapIndex(x, mapHeight, x1, 0)] = noise((x+x1/VERTICESPERTILE)*MAPNOISESCALE, (mapHeight)*MAPNOISESCALE);
      }
    }
    heightMap[toMapIndex(mapWidth, mapHeight, 0, 0)] = noise(((mapWidth+1))*MAPNOISESCALE, (mapHeight)*MAPNOISESCALE);
  }
  float getRawHeight(int x, int y, int x1, int y1) {
    return max(heightMap[int(x1+x*VERTICESPERTILE+y1*VERTICESPERTILE*(mapWidth+1/VERTICESPERTILE)+y*pow(VERTICESPERTILE, 2)*(mapWidth+1/VERTICESPERTILE))], WATERLEVEL);
  }
  float getRawHeight(int x, int y) {
    return getRawHeight(x, y, 0, 0);
  }
  float getRawHeight(float x, float y){
    return getRawHeight(int(x), int(y), round((x-int(x))*VERTICESPERTILE), round((y-int(y))*VERTICESPERTILE));
  }
  float groundMinRawHeightAt(int x1, int y1) {
    int x = floor(x1);
    int y = floor(y1);
    return min(new float[]{getRawHeight(x, y), getRawHeight(x+1, y), getRawHeight(x, y+1), getRawHeight(x+1, y+1)});
  }
  float groundMaxRawHeightAt(int x1, int y1) {
    int x = floor(x1);
    int y = floor(y1);
    return max(new float[]{getRawHeight(x, y), getRawHeight(x+1, y), getRawHeight(x, y+1), getRawHeight(x+1, y+1)});
  }
  
  float getMaxSteepness(int x, int y){
    float maxZ, minZ;
    maxZ = 0;
    minZ = 1;
    for (float y1 = y; y1<=y+1;y1+=1.0/VERTICESPERTILE){
      for (float x1 = x; x1<=x+1;x1+=1.0/VERTICESPERTILE){
        float z = getRawHeight(x1, y1);
        if(z>maxZ){
          maxZ = z;
        } else if (z<minZ){
          minZ = z;
        }
      }
    }
    return maxZ-minZ;
  }
}

class Map2D extends BaseMap implements Map{
  final int EW, EH, INITIALHOLD=1000;
  float blockSize, targetBlockSize;
  float mapXOffset, mapYOffset, targetXOffset, targetYOffset, panningSpeed, resetTime;
  boolean panning=false, zooming=false;
  float mapMaxSpeed;
  int elementWidth;
  int elementHeight;
  float[] mapVelocity = {0,0};
  int startX;
  int startY;
  int frameStartTime;
  int xPos, yPos;
  boolean zoomChanged;
  boolean mapFocused, mapActive;
  int selectedCellX, selectedCellY;
  boolean cellSelected;
  color partyManagementColour;
  Node[][] moveNodes;
  ArrayList<int[]> drawPath;

  Map2D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight){
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
    heightMap = new float[int((mapWidth+1)*(mapHeight+1)*pow(VERTICESPERTILE, 2))];
  }
  void generateShape(){
    
  }
  void clearShape(){
    
  }
  Node[][] getMoveNodes(){
    return moveNodes;
  }
  float getTargetZoom(){
    return targetBlockSize;
  }
  float getZoom(){
    return blockSize;
  }
  boolean isZooming(){
    return zooming;
  }
  boolean isPanning(){
    return panning;
  }
  float getFocusedX(){
    return mapXOffset;
  }
  float getFocusedY(){
    return mapYOffset;
  }
  void removeTreeTile(int cellX, int cellY) {
    terrain[cellY][cellX] = terrainIndex("grass");
  }
  void setActive(boolean a){
    this.mapActive = a;
  }
  void selectCell(int x, int y){
    cellSelected = true;
    selectedCellX = x;
    selectedCellY = y;
  }
  void unselectCell(){
    cellSelected = false;
  }
  void setPanningSpeed(float s){
    panningSpeed = s;
  }
  void limitCoords(){
    mapXOffset = min(max(mapXOffset, -mapWidth*blockSize+elementWidth*0.5), elementWidth*0.5);
    mapYOffset = min(max(mapYOffset, -mapHeight*blockSize+elementHeight*0.5), elementHeight*0.5);
  }
  void reset(Party[][] parties){
    mapXOffset = 0;
    mapYOffset = 0;
    this.parties = parties;
    blockSize = min(elementWidth/(float)mapWidth, elementWidth/10);
    setPanningSpeed(0.02);
    resetTime = millis();
    frameStartTime = 0;
    cancelMoveNodes();
  }
  void loadSettings(float x, float y, float bs){
    targetZoom(bs);
    targetOffset(x, y);
  }
  void targetOffset(float x, float y){
    targetXOffset = x;
    targetYOffset = y;
    limitCoords();
    panning = true;
  }
  void targetZoom(float bs){
    zooming = true;
    targetBlockSize = bs;
  }
  float getTargetOffsetX(){
    return targetXOffset;
  }
  float getTargetOffsetY(){
    return targetYOffset;
  }
  float getTargetBlockSize(){
    return targetBlockSize;
  }
  float [] targetCell(int x, int y, float bs){
    targetBlockSize = bs;
    targetXOffset = -(x+0.5)*targetBlockSize+elementWidth/2+xPos;
    targetYOffset = -(y+0.5)*targetBlockSize+elementHeight/2+yPos;
    panning = true;
    zooming = true;
    return new float[]{targetXOffset, targetYOffset, targetBlockSize};
  }
  void focusMapMouse(int x, int y){
    // based on mouse click
    if(mouseOver()){
      targetXOffset = -scaleXInv(x)*blockSize+elementWidth/2+xPos;
      targetYOffset = -scaleYInv(y)*blockSize+elementHeight/2+yPos;
      limitCoords();
      panning = true;
    }
  }
  void resetTarget(){
    targetXOffset = mapXOffset;
    targetYOffset = mapYOffset;
    panning = false;
  }
  void resetTargetZoom(){
    zooming = false;
    targetBlockSize = blockSize;
    setPanningSpeed(0.05); 
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

  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){
    if (eventType == "mouseWheel"){
      float count = event.getCount();
      if(mouseOver() && mapActive){
        float zoom = pow(0.9, count);
        float newBlockSize = max(min(blockSize*zoom, (float)elementWidth/10), (float)elementWidth/(float)mapSize);
        if (blockSize != newBlockSize){
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

  ArrayList<String> mouseEvent(String eventType, int button){
    if (button == LEFT&mapActive){
      if (eventType=="mouseDragged" && mapFocused){
        mapXOffset += (mouseX-startX);
        mapYOffset += (mouseY-startY);
        limitCoords();
        startX = mouseX;
        startY = mouseY;
        resetTarget();
        resetTargetZoom();
      }
      if (eventType == "mousePressed"){
        if (mouseOver()){
          startX = mouseX;
          startY = mouseY;
          mapFocused = true;
        }
        else{
          mapFocused = false;
        }
      }
    }
    return new ArrayList<String>();
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (eventType == "keyPressed"){
      if (_key == 'a'){
        resetTargetZoom();
        resetTarget();
        mapVelocity[0] -= mapMaxSpeed;
      }
      if (_key == 's'){
        resetTargetZoom();
        resetTarget();
        mapVelocity[1] += mapMaxSpeed;
      }
      if (_key == 'd'){
        resetTargetZoom();
        resetTarget();
        mapVelocity[0] += mapMaxSpeed;
      }
      if (_key == 'w'){
        resetTargetZoom();
        resetTarget();
        mapVelocity[1] -= mapMaxSpeed;
      }
    }
    if (eventType == "keyReleased"){
      if (_key == 'a'){
        mapVelocity[0] += mapMaxSpeed;
      }
      if (_key == 's'){
        mapVelocity[1] -= mapMaxSpeed;
      }
      if (_key == 'd'){
        mapVelocity[0] -= mapMaxSpeed;
      }
      if (_key == 'w'){
        mapVelocity[1] += mapMaxSpeed;
      }
    }
    return new ArrayList<String>();
  }

  void setWidth(int w){
    this.elementWidth = w;
  }
  
  void drawSelectedCell(PVector c, PGraphics panelCanvas){
   //cell selection
   panelCanvas.stroke(0);
   if (cellSelected){
     if (max(c.x, xPos)+min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)>xPos && max(c.x, xPos) < elementWidth+xPos && max(c.y, yPos)+min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos)>yPos && max(c.y, yPos) < elementHeight+yPos){
       panelCanvas.fill(50, 100);
       panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize-1, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize-1, yPos+elementHeight-c.y, blockSize+c.y-yPos));
     }
   }
  }
  
  void draw(PGraphics panelCanvas){

    // Terrain

    PImage[] tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
    PImage[][] tempBuildingImages = new PImage[gameData.getJSONArray("buildings").size()][];
    PImage[] tempPartyImages = new PImage[3];
    HashMap<String, PImage> tempTaskImages = new HashMap<String, PImage>();
    if (frameStartTime == 0){
      frameStartTime = millis();
    }
    int frameTime = millis()-frameStartTime;
    if (millis()-resetTime < INITIALHOLD){
      frameTime = 0;
    }
    if (zooming){
      blockSize += (targetBlockSize-blockSize)*panningSpeed*frameTime*60/1000;
    }

    // Resize map based on scale
    if (panning){
      mapXOffset -= (mapXOffset-targetXOffset)*panningSpeed*frameTime*60/1000;
      mapYOffset -= (mapYOffset-targetYOffset)*panningSpeed*frameTime*60/1000;
    }
    if ((zooming || panning) && pow(mapXOffset-targetXOffset, 2) + pow(mapYOffset-targetYOffset, 2) < pow(blockSize*0.5, 2) && abs(blockSize-targetBlockSize) < 1){
      resetTargetZoom();
      resetTarget();
    }
    mapXOffset -= mapVelocity[0]*frameTime*60/1000;
    mapYOffset -= mapVelocity[1]*frameTime*60/1000;
    frameStartTime = millis();
    limitCoords();
    
    if (blockSize <= 0)
      return;
    
    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++){
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      if(blockSize<24&&!tileType.isNull("low img")){
        tempTileImages[i] = lowImages.get(tileType.getString("id")).copy();
        tempTileImages[i].resize(ceil(blockSize), 0);
      } else {
        tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
        tempTileImages[i].resize(ceil(blockSize), 0);
      }
    }
    for (int i=0; i<gameData.getJSONArray("buildings").size(); i++){
      JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
      tempBuildingImages[i] = new PImage[buildingImages.get(buildingType.getString("id")).length];
      for (int j=0; j<buildingImages.get(buildingType.getString("id")).length; j++){
        tempBuildingImages[i][j] = buildingImages.get(buildingType.getString("id"))[j].copy();
        tempBuildingImages[i][j].resize(ceil(blockSize*48/64), 0);
      }
    }
    for (int i=0; i<3; i++){
      tempPartyImages[i] = partyImages[i].copy();
      tempPartyImages[i].resize(ceil(blockSize), 0);
    }
    for(String taskName: taskImages.keySet()){
      tempTaskImages.put(taskName, taskImages.get(taskName).copy());
      tempTaskImages.get(taskName).resize(ceil(3*blockSize/16), 0);
    }
    int lx = max(0, -ceil((mapXOffset)/blockSize));
    int ly = max(0, -ceil((mapYOffset)/blockSize));
    int hx = min(floor((elementWidth-mapXOffset)/blockSize)+1, mapWidth);
    int hy = min(floor((elementHeight-mapYOffset)/blockSize)+1, mapHeight);
    
   PVector c;
   PVector selectedCell = new PVector(scaleX(selectedCellX), scaleY(selectedCellY));
     
    for(int y=ly;y<hy;y++){
      for (int x=lx; x<hx; x++){
        float x2 = round(scaleX(x));
        float y2 = round(scaleY(y));
        panelCanvas.image(tempTileImages[terrain[y][x]-1], x2, y2);
        
         //Buildings
         if (buildings[y][x] != null){
           c = new PVector(scaleX(x), scaleY(y));
           int border = round((64-48)*blockSize/(2*64));
           int imgSize = round(blockSize*48/60);
           drawCroppedImage(round(c.x+border), round(c.y+border*2), imgSize, imgSize, tempBuildingImages[buildings[y][x].type-1][buildings[y][x].image_id], panelCanvas);
         }
         //Parties
         if(parties[y][x]!=null){
           c = new PVector(scaleX(x), scaleY(y));
           if(c.x<xPos+elementWidth&&c.y+blockSize/8>yPos&&c.y<yPos+elementHeight){
             panelCanvas.noStroke();
             if(parties[y][x].player == 2){
               Battle battle = (Battle) parties[y][x];
               if (c.x+blockSize>xPos){
                 panelCanvas.fill(120, 120, 120);
                 panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
               }
               if (c.x+blockSize*parties[y][x].getUnitNumber()/1000>xPos){
                 panelCanvas.fill(playerColours[battle.party1.player]);
                 panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize*battle.party1.getUnitNumber()/1000, xPos+elementWidth-c.x, blockSize*battle.party1.getUnitNumber()/1000+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
               }
               if (c.x+blockSize>xPos){
                 panelCanvas.fill(120, 120, 120);
                 panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/16+c.y-yPos)));
               }
               if (c.x+blockSize*parties[y][x].getUnitNumber()/1000>xPos){
                 panelCanvas.fill(playerColours[battle.party2.player]);
                 panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize*battle.party2.getUnitNumber()/1000, xPos+elementWidth-c.x, blockSize*battle.party2.getUnitNumber()/1000+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/8+c.y-yPos)));
               }
               
             } else {
               if (c.x+blockSize>xPos){
                 panelCanvas.fill(120, 120, 120);
                 panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
               }
               if (c.x+blockSize*parties[y][x].getUnitNumber()/1000>xPos){
                 panelCanvas.fill(playerColours[parties[y][x].player]);
                 panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize*parties[y][x].getUnitNumber()/1000, xPos+elementWidth-c.x, blockSize*parties[y][x].getUnitNumber()/1000+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
               }
             }
             int imgSize = round(blockSize);
             drawCroppedImage(floor(c.x), floor(c.y), imgSize, imgSize, tempPartyImages[parties[y][x].player], panelCanvas);
             JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), parties[y][x].task);
             if (jo != null && !jo.isNull("img")){
               drawCroppedImage(floor(c.x+13*blockSize/32), floor(c.y+blockSize/2), ceil(3*blockSize/16), ceil(3*blockSize/16), tempTaskImages.get(parties[y][x].task), panelCanvas);
             }
           }
         }
         if (cellSelected&&y==selectedCellY&&x==selectedCellX){
           drawSelectedCell(selectedCell, panelCanvas);
         }
         if(parties[y][x]!=null){
           c = new PVector(scaleX(x), scaleY(y));
           if(c.x<xPos+elementWidth&&c.y+blockSize/8>yPos&&c.y<yPos+elementHeight){
             panelCanvas.textFont(getFont(blockSize/7));
             panelCanvas.fill(255);
             panelCanvas.textAlign(CENTER, CENTER);
             if(parties[y][x].actions.size() > 0 && parties[y][x].actions.get(0).initialTurns>0){
               int totalTurns = parties[y][x].calcTurns(parties[y][x].actions.get(0).initialTurns);
               String turnsLeftString = str(totalTurns-parties[y][x].turnsLeft())+"/"+str(totalTurns);
               if (c.x+textWidth(turnsLeftString) < elementWidth+xPos && c.y+2*(textAscent()+textDescent()) < elementHeight+yPos){
                 panelCanvas.text(turnsLeftString, c.x+blockSize/2, c.y+3*blockSize/4);
               }
             }
           }
         }
         //if (millis()-pt > 10){
         //  println(millis()-pt);
         //}
       }
     }

     if (moveNodes != null){
       for (int y1=0; y1<mapHeight; y1++){
         for (int x=0; x<mapWidth; x++){
           if (moveNodes[y1][x] != null){
             c = new PVector(scaleX(x), scaleY(y1));
             if (max(c.x, xPos)+min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)>xPos && max(c.x, xPos) < elementWidth+xPos && max(c.y, yPos)+min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos)>yPos && max(c.y, yPos) < elementHeight+yPos){
               if (blockSize > 10*TextScale && moveNodes[y1][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()){
                 panelCanvas.fill(50, 150);
                 panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos));
                 panelCanvas.fill(255);
                 panelCanvas.textFont(getFont(8*TextScale));
                 panelCanvas.textAlign(CENTER, CENTER);
                 String s = ""+moveNodes[y1][x].cost;
                 s = s.substring(0, min(s.length(), 3));
                 BigDecimal cost = new BigDecimal(s);
                 String s2 = cost.stripTrailingZeros().toPlainString();
                 if (c.x+panelCanvas.textWidth(s2) < elementWidth+xPos && c.y+2*(textAscent()+textDescent()) < elementHeight+yPos){
                   panelCanvas.text(s2, c.x+blockSize/2, c.y+blockSize/2);
                 }
               }
             }
           }
         }
       }
     }
     
     if (drawPath != null){
       for (int i=0; i<drawPath.size()-1;i++){
         if (lx <= drawPath.get(i)[0] && drawPath.get(i)[0] < hx && ly <= drawPath.get(i)[1] && drawPath.get(i)[1] < hy){
           panelCanvas.pushStyle();
           panelCanvas.stroke(255,0,0); 
           panelCanvas.line(scaleX(drawPath.get(i)[0])+blockSize/2, scaleY(drawPath.get(i)[1])+blockSize/2, scaleX(drawPath.get(i+1)[0])+blockSize/2, scaleY(drawPath.get(i+1)[1])+blockSize/2);
           panelCanvas.popStyle();
         }
       }
     }
     else if (cellSelected && parties[selectedCellY][selectedCellX] != null){
       ArrayList<int[]> path = parties[selectedCellY][selectedCellX].path;
       if (path != null){
         for (int i=0; i<path.size()-1;i++){
           if (lx <= path.get(i)[0] && path.get(i)[0] < hx && ly <= path.get(i)[1] && path.get(i)[1] < hy){
             panelCanvas.pushStyle();
             panelCanvas.stroke(100); 
             panelCanvas.line(scaleX(path.get(i)[0])+blockSize/2, scaleY(path.get(i)[1])+blockSize/2, scaleX(path.get(i+1)[0])+blockSize/2, scaleY(path.get(i+1)[1])+blockSize/2);
             panelCanvas.popStyle();
           }
         }
       }
     }

     panelCanvas.noFill();
     panelCanvas.stroke(0);
     panelCanvas.rect(xPos, yPos, elementWidth, elementHeight);
  }
  int sign(float x){
    if(x > 0){
      return 1;
    }
    else if (x < 0){
      return -1;
    }
    return 0;
  }
  void drawCroppedImage(int x, int y, int w, int h, PImage img, PGraphics panelCanvas){
    if (x+w>xPos && x<elementWidth+xPos && y+h>yPos && y<elementHeight+yPos){
      int newX = max(min(x, xPos+elementWidth), xPos);
      int newY = max(min(y, yPos+elementHeight), yPos);
      int imgX = max(0, newX-x, 0);
      int imgY = max(0, newY-y, 0);
      int imgW = min(max(elementWidth+xPos-x, -sign(elementWidth+xPos-x)*(x+w-newX)), img.width);
      int imgH = min(max(elementHeight+yPos-y, -sign(elementHeight+yPos-y)*(y+h-newY)), img.height);
      panelCanvas.image(img, newX, newY);
    }
  }
  float scaleX(float x){
    return x*blockSize + mapXOffset + xPos;
  }
  float scaleY(float y){
    return y*blockSize + mapYOffset + yPos;
  }
  float scaleXInv(int x){
    return (x-mapXOffset-xPos)/blockSize;
  }
  float scaleYInv(int y){
    return (y-mapYOffset-yPos)/blockSize;
  }
  boolean mouseOver(){
    return mouseX > xPos && mouseX < xPos+elementWidth && mouseY > yPos && mouseY < yPos+elementHeight;
  }
}
