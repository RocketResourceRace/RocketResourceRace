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
  void generateShape();
  void clearShape();
  boolean isMoving();
}


int getPartySize(Party p){
  int totalSize = 0;
  ByteBuffer[] actions = new ByteBuffer[p.actions.size()];
  int index = 0;
  for (Action a: p.actions){
    int notificationSize;
    int terrainSize;
    int buildingSize;
    byte[] notification = new byte[0];
    byte[] terrain = new byte[0];
    byte[] building = new byte[0];
    if(a.notification == null){
      notificationSize = 0;
    } else {
      notification = a.notification.getBytes();
      notificationSize = notification.length;
    }
    if(a.terrain == null){
      terrainSize = 0;
    } else {
      terrain = a.terrain.getBytes();
      terrainSize = terrain.length;
    }
    if(a.building == null){
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
    if(notificationSize>0){
      actions[index].put(notification);
    }
    if(terrainSize>0){
      actions[index].put(terrain);
    }
    if(buildingSize>0){
      actions[index].put(building);
    }
    index++;
  }
  totalSize+=Integer.BYTES; // For action count
  int pathSize = Integer.BYTES*(2*p.path.size()+1);
  totalSize += pathSize;
  
  ByteBuffer path = ByteBuffer.allocate(pathSize);
  path.putInt(p.path.size());
  for (int[] l: p.path){
    path.putInt(l[0]);
    path.putInt(l[1]);
  }
  totalSize += Integer.BYTES*5+Float.BYTES;
  
  
  ByteBuffer partyBuffer = ByteBuffer.allocate(totalSize);
  partyBuffer.putInt(p.actions.size());
  for (ByteBuffer action: actions){
    partyBuffer.put(action.array());
  }
  partyBuffer.put(path.array());
  partyBuffer.putInt(p.getUnitNumber());
  partyBuffer.putInt(p.getMovementPoints());
  partyBuffer.putInt(p.player);
  partyBuffer.putFloat(p.strength);
  partyBuffer.putInt(p.getTask());
  partyBuffer.putInt(p.pathTurns);
  p.byteRep = partyBuffer.array();
  return totalSize;
}
void saveParty(ByteBuffer b, Party p){
  b.put(p.byteRep);
}
Party loadParty(ByteBuffer b){
  int actionCount = b.getInt();
  ArrayList<Action> actions = new ArrayList<Action>();
  for (int i=0; i<actionCount; i++){
    String notification;
    String terrain;
    String building;
    int notificationTextSize = b.getInt();
    int terrainTextSize = b.getInt();
    int buildingTextSize = b.getInt();
    Float turns = b.getFloat();
    Float initialTurns = b.getFloat();
    int type = b.getInt();
    if(notificationTextSize>0){
      byte[] notificationTemp = new byte[notificationTextSize];
      b.get(notificationTemp);
      notification = new String(notificationTemp);
    } else {
      notification = null;
    }
    if(terrainTextSize>0){
      byte[] terrainTemp = new byte[terrainTextSize];
      b.get(terrainTemp);
      terrain = new String(terrainTemp);
    } else {
      terrain = null;
    }
    if(buildingTextSize>0){
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
  for(int i=0; i<pathSize; i++){
    path.add(new int[]{b.getInt(), b.getInt()});
  }
  int unitNumber = b.getInt();
  int movementPoints = b.getInt();
  int player = b.getInt();
  float strength = b.getFloat();
  int task = b.getInt();
  int pathTurns = b.getInt();
  Party p = new Party(player, unitNumber, task, movementPoints);
  p.strength = strength;
  p.pathTurns = pathTurns;
  p.actions = actions;
  if (path.size() > 0){
    p.target = path.get(path.size()-1);
    p.loadPath(path);
  }
  return p;
}

class BaseMap extends Element{
  float[] heightMap;
  int mapWidth, mapHeight;
  long heightMapSeed;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  void saveMap(String filename, int turnNumber, int turnPlayer, Player[] players){
    int partiesByteCount = 0;
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if(parties[y][x] != null){
          if(parties[y][x].player==2){
            partiesByteCount+=getPartySize(((Battle)parties[y][x]).party1);
            partiesByteCount+=getPartySize(((Battle)parties[y][x]).party2);
          } else {
            partiesByteCount+=getPartySize(parties[y][x]);
          }
        }
        partiesByteCount++;
      }
    }
    int playersByteCount = ((3+players[0].resources.length)*Float.BYTES+3*Integer.BYTES+1)*players.length;
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES*8+Long.BYTES+Integer.BYTES*mapWidth*mapHeight*3+partiesByteCount+playersByteCount);
    buffer.putInt(mapWidth);
    buffer.putInt(mapHeight);
    buffer.putInt(partiesByteCount);
    buffer.putInt(playersByteCount);
    buffer.putInt(players.length);
    buffer.putInt(players[0].resources.length);
    buffer.putInt(turnNumber);
    buffer.putInt(turnPlayer);
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
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        if(parties[y][x]==null){
          buffer.put(byte(0));
        } else if (parties[y][x].player == 2){
          buffer.put(byte(2));
          saveParty(buffer, ((Battle)parties[y][x]).party1);
          saveParty(buffer, ((Battle)parties[y][x]).party2);
        } else {
          buffer.put(byte(1));
          saveParty(buffer, parties[y][x]);
        }
      }
    }
    for (Player p: players){
      buffer.putFloat(p.mapXOffset);
      buffer.putFloat(p.mapYOffset);
      buffer.putFloat(p.blockSize);
      for (float r: p.resources){
        buffer.putFloat(r);
      }
      buffer.putInt(p.cellX);
      buffer.putInt(p.cellY);
      buffer.putInt(p.colour);
      buffer.put(byte(p.cellSelected));
    }
    saveBytes(filename, buffer.array());
  }
  MapSave loadMap(String filename, int resourceCountNew){
    byte tempBuffer[] = loadBytes(filename);
    int headerSize = Integer.BYTES*4;
    ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize);
    headerBuffer.put(Arrays.copyOfRange(tempBuffer, 0, headerSize));
    headerBuffer.flip();//need flip
    mapWidth = headerBuffer.getInt();
    mapHeight = headerBuffer.getInt();
    int partiesByteCount = headerBuffer.getInt();
    int playersByteCount = headerBuffer.getInt();
    int dataSize = Long.BYTES+partiesByteCount+playersByteCount+(4+mapWidth*mapHeight*3)*Integer.BYTES;
    ByteBuffer buffer = ByteBuffer.allocate(dataSize);
    buffer.put(Arrays.copyOfRange(tempBuffer, headerSize, headerSize+dataSize));
    buffer.flip();//need flip
    int playerCount = buffer.getInt();
    int resourceCountOld = buffer.getInt();
    int turnNumber = buffer.getInt();
    int turnPlayer = buffer.getInt();
    heightMapSeed = buffer.getLong();
    terrain = new int[mapHeight][mapWidth];
    parties = new Party[mapHeight][mapWidth];
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
    for (int y=0; y<mapHeight; y++){
      for (int x=0; x<mapWidth; x++){
        Byte partyType = buffer.get();
        if (partyType == 2){
          Party p1 = loadParty(buffer);
          Party p2 = loadParty(buffer);
          float savedStrength = p1.strength;
          Battle b = new Battle(p1, p2);
          b.party1.strength = savedStrength;
          parties[y][x] = b;
        } else if (partyType == 1){
          parties[y][x] = loadParty(buffer);
        }
      }
    }
    Player[] players = new Player[playerCount];
    for (int i=0;i<playerCount;i++){
      float mapXOffset = buffer.getFloat();
      float mapYOffset = buffer.getFloat();
      float blockSize = buffer.getFloat();
      float[] resources = new float[resourceCountNew];
      for (int r=0; r<resourceCountOld;r++){
        resources[r] = buffer.getFloat();
      }
      int cellX = buffer.getInt();
      int cellY = buffer.getInt();
      int colour = buffer.getInt();
      boolean cellSelected = boolean(buffer.get());
      players[i] = new Player(mapXOffset, mapYOffset, blockSize, resources, colour);
      players[i].cellSelected = cellSelected;
    }
    noiseSeed(heightMapSeed);
    generateNoiseMaps();
    
    return new MapSave(heightMap, mapWidth, mapHeight, terrain, parties, buildings, turnNumber, turnPlayer, players);
  }
  
  
  int toMapIndex(int x, int y, int x1, int y1){
    return int(x1+x*jsManager.loadFloatSetting("terrain detail")+y1*jsManager.loadFloatSetting("terrain detail")*(mapWidth+1/jsManager.loadFloatSetting("terrain detail"))+y*pow(jsManager.loadFloatSetting("terrain detail"), 2)*(mapWidth+1/jsManager.loadFloatSetting("terrain detail")));
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
    for(int i=0;i<jsManager.loadIntSetting("ground spawns");i++){
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
    terrain = smoothMap(jsManager.loadIntSetting("smoothing"), 2, terrain);
    terrain = smoothMap(jsManager.loadIntSetting("smoothing")+2, 1, terrain);
    for (int y=0; y<mapHeight; y++){
      for(int x=0; x<mapWidth; x++){
        if(terrain[y][x] != terrainIndex("water") && (groundMaxRawHeightAt(x, y) > 0.5+jsManager.loadFloatSetting("water level")/2.0) || getMaxSteepness(x, y)>HILLSTEEPNESS){
          terrain[y][x] = terrainIndex("hills");
        }
      }
    }
  }
  void generateMap(int mapWidth, int mapHeight){
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
  void generateNoiseMaps(){
    heightMap = new float[int((mapWidth+1/jsManager.loadFloatSetting("terrain detail"))*(mapHeight+1/jsManager.loadFloatSetting("terrain detail"))*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
    for(int y = 0;y<mapHeight;y++){
      for(int y1 = 0;y1<jsManager.loadFloatSetting("terrain detail");y1++){
        for(int x = 0;x<mapWidth;x++){
          for(int x1 = 0;x1<jsManager.loadFloatSetting("terrain detail");x1++){
            heightMap[toMapIndex(x, y, x1, y1)] = noise((x+x1/jsManager.loadFloatSetting("terrain detail"))*MAPNOISESCALE, (y+y1/jsManager.loadFloatSetting("terrain detail"))*MAPNOISESCALE);
          }
        }
        heightMap[toMapIndex(mapWidth, y, 0, y1)] = noise(((mapWidth+1))*MAPNOISESCALE, (y+y1/jsManager.loadFloatSetting("terrain detail"))*MAPNOISESCALE);
      }
    }
    for(int x = 0;x<mapWidth;x++){
      for(int x1 = 0;x1<jsManager.loadFloatSetting("terrain detail");x1++){
        heightMap[toMapIndex(x, mapHeight, x1, 0)] = noise((x+x1/jsManager.loadFloatSetting("terrain detail"))*MAPNOISESCALE, (mapHeight)*MAPNOISESCALE);
      }
    }
    heightMap[toMapIndex(mapWidth, mapHeight, 0, 0)] = noise(((mapWidth+1))*MAPNOISESCALE, (mapHeight)*MAPNOISESCALE);
  }
  float getRawHeight(int x, int y, int x1, int y1) {
    try{
      return max(heightMap[int(x1+x*jsManager.loadFloatSetting("terrain detail")+y1*jsManager.loadFloatSetting("terrain detail")*(mapWidth+1/jsManager.loadFloatSetting("terrain detail"))+y*pow(jsManager.loadFloatSetting("terrain detail"), 2)*(mapWidth+1/jsManager.loadFloatSetting("terrain detail")))], jsManager.loadFloatSetting("water level"));
    } catch (ArrayIndexOutOfBoundsException e) {
      return 0;
    }
  }
  float getRawHeight(int x, int y) {
    return getRawHeight(x, y, 0, 0);
  }
  float getRawHeight(float x, float y){
    return getRawHeight(int(x), int(y), round((x-int(x))*jsManager.loadFloatSetting("terrain detail")), round((y-int(y))*jsManager.loadFloatSetting("terrain detail")));
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
    for (float y1 = y; y1<=y+1;y1+=1.0/jsManager.loadFloatSetting("terrain detail")){
      for (float x1 = x; x1<=x+1;x1+=1.0/jsManager.loadFloatSetting("terrain detail")){
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
    heightMap = new float[int((mapWidth+1)*(mapHeight+1)*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
  }
  void generateShape(){

  }
  void clearShape(){

  }
  
  boolean isMoving(){
    return mapVelocity[0] != 0 || mapVelocity[1] != 0;
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
  void reset(){
    mapXOffset = 0;
    mapYOffset = 0;
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
        float newBlockSize = max(min(blockSize*zoom, (float)elementWidth/10), (float)elementWidth/(float)jsManager.loadIntSetting("map size"));
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
    PImage[] tempTaskImages = new PImage[taskImages.length];
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
    for(int i=0; i<taskImages.length; i++){
      if(taskImages[i] != null){
        tempTaskImages[i] = taskImages[i];
        tempTaskImages[i].resize(ceil(3*blockSize/16), 0);
      }
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
             JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(parties[y][x].getTask());
             if (jo != null && !jo.isNull("img")){
               drawCroppedImage(floor(c.x+13*blockSize/32), floor(c.y+blockSize/2), ceil(3*blockSize/16), ceil(3*blockSize/16), tempTaskImages[parties[y][x].getTask()], panelCanvas);
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
               if (blockSize > 10*jsManager.loadFloatSetting("text scale") && moveNodes[y1][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()){
                 panelCanvas.fill(50, 150);
                 panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos));
                 panelCanvas.fill(255);
                 panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
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
