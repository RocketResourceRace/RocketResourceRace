import java.util.Collections;
import java.nio.ByteBuffer;

interface Map {
  void updateMoveNodes(Node[][] nodes);
  void cancelMoveNodes();
  void removeTreeTile(int cellX, int cellY);
  void setDrawingTaskIcons(boolean v);
  void setDrawingUnitBars(boolean v);
  void setHeightsForCell(int x, int y, float h);
  void replaceMapStripWithReloadedStrip(int y);
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
  float scaleXInv();
  float scaleYInv();
  void updatePath(ArrayList<int[]> nodes);
  void updateHoveringScale();
  void doUpdateHoveringScale();
  void cancelPath();
  void setActive(boolean a);
  void selectCell(int x, int y);
  void generateShape();
  void clearShape();
  boolean isMoving();
  void enableRocket(PVector pos, PVector vel);
  void disableRocket();
  void generateFog(int player);
  void enableBombard(int range);
  void disableBombard();
  void setPlayerColours(color[] playerColours);
}


int getPartySize(Party p) {
  LOGGER_MAIN.finer("Getting party size for save");
  try {
    int totalSize = 0;
    totalSize += Character.BYTES*16;
    ByteBuffer[] actions = new ByteBuffer[p.actions.size()];
    int index = 0;
    for (Action a : p.actions) {
      int notificationSize;
      int terrainSize;
      int buildingSize;
      byte[] notification = new byte[0];
      byte[] terrain = new byte[0];
      byte[] building = new byte[0];
      if (a.notification == null) {
        notificationSize = 0;
      } else {
        notification = a.notification.getBytes();
        notificationSize = notification.length;
      }
      if (a.terrain == null) {
        terrainSize = 0;
      } else {
        terrain = a.terrain.getBytes();
        terrainSize = terrain.length;
      }
      if (a.building == null) {
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
      if (notificationSize>0) {
        actions[index].put(notification);
      }
      if (terrainSize>0) {
        actions[index].put(terrain);
      }
      if (buildingSize>0) {
        actions[index].put(building);
      }
      index++;
    }
    totalSize+=Integer.BYTES; // For action count
    int pathSize = Integer.BYTES*(2*p.path.size()+1);
    totalSize += pathSize;

    ByteBuffer path = ByteBuffer.allocate(pathSize);
    path.putInt(p.path.size());
    for (int[] l : p.path) {
      path.putInt(l[0]);
      path.putInt(l[1]);
    }
    totalSize += Integer.BYTES * (7 + p.equipment.length * 2) + Float.BYTES * (1 + p.proficiencies.length)+1;


    ByteBuffer partyBuffer = ByteBuffer.allocate(totalSize);
    for (int i=0; i<16; i++) {
      if (i<p.id.length()) {
        partyBuffer.putChar(p.id.charAt(i));
      } else {
        partyBuffer.putChar(' ');
      }
    }

    partyBuffer.putInt(p.actions.size());
    for (ByteBuffer action : actions) {
      partyBuffer.put(action.array());
    }
    partyBuffer.put(path.array());
    partyBuffer.putInt(p.getUnitNumber());
    partyBuffer.putInt(p.getMovementPoints());
    partyBuffer.putInt(p.player);
    partyBuffer.putFloat(p.strength);
    partyBuffer.putInt(p.getTask());
    partyBuffer.putInt(p.pathTurns);
    partyBuffer.putInt(p.trainingFocus);
    for (int type: p.equipment) {
      partyBuffer.putInt(type);
    }
    for (int quantity: p.equipmentQuantities) {
      partyBuffer.putInt(quantity);
    }
    for (float prof: p.proficiencies) {
      partyBuffer.putFloat(prof);
    }
    partyBuffer.putInt(p.unitCap);
    partyBuffer.put(byte(p.autoStockUp));
    p.byteRep = partyBuffer.array();
    return totalSize;
  }
  catch (Exception e) {
    LOGGER_MAIN.log(Level.SEVERE, "Error getting party size", e);
    throw e;
  }
}
void saveParty(ByteBuffer b, Party p) {
  try {
    b.put(p.byteRep);
  }
  catch (Exception e) {
    LOGGER_MAIN.log(Level.SEVERE, "Error saving party", e);
    throw e;
  }
}
Party loadParty(ByteBuffer b, String id) {
  LOGGER_MAIN.finer("Loading party from save");
  try {
    int actionCount = b.getInt();
    ArrayList<Action> actions = new ArrayList<Action>();
    for (int i=0; i<actionCount; i++) {
      String notification;
      String terrain;
      String building;
      int notificationTextSize = b.getInt();
      int terrainTextSize = b.getInt();
      int buildingTextSize = b.getInt();
      Float turns = b.getFloat();
      Float initialTurns = b.getFloat();
      int type = b.getInt();
      if (notificationTextSize>0) {
        byte[] notificationTemp = new byte[notificationTextSize];
        b.get(notificationTemp);
        notification = new String(notificationTemp);
      } else {
        notification = null;
      }
      if (terrainTextSize>0) {
        byte[] terrainTemp = new byte[terrainTextSize];
        b.get(terrainTemp);
        terrain = new String(terrainTemp);
      } else {
        terrain = null;
      }
      if (buildingTextSize>0) {
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
    for (int i=0; i<pathSize; i++) {
      path.add(new int[]{b.getInt(), b.getInt()});
    }
    int unitNumber = b.getInt();
    int movementPoints = b.getInt();
    int player = b.getInt();
    float strength = b.getFloat();
    int task = b.getInt();
    int pathTurns = b.getInt();
    int trainingFocus = b.getInt();
    int[] equipment = new int[jsManager.getNumEquipmentClasses()];
    for (int i = 0; i < jsManager.getNumEquipmentClasses(); i++) {
      equipment[i] = b.getInt();
    }
    int[] equipmentQuantities = new int[jsManager.getNumEquipmentClasses()];
    for (int i = 0; i < jsManager.getNumEquipmentClasses(); i++) {
      equipmentQuantities[i] = b.getInt();
    }
    float[] proficiencies = new float[jsManager.getNumProficiencies()];
    for (int i = 0; i < jsManager.getNumProficiencies(); i++) {
      proficiencies[i] = b.getFloat();
    }
    int unitCap = b.getInt();
    boolean autoStockUp = b.get()==byte(1);
    Party p = new Party(player, unitNumber, task, movementPoints, id.trim());
    p.strength = strength;
    p.pathTurns = pathTurns;
    p.actions = actions;
    if (path.size() > 0) {
      p.target = path.get(path.size()-1);
      p.loadPath(path);
    }
    p.trainingFocus = trainingFocus;
    p.equipment = equipment;
    p.equipmentQuantities = equipmentQuantities;
    p.proficiencies = proficiencies;
    p.unitCap = unitCap;
    p.autoStockUp = autoStockUp;
    return p;
  }
  catch (Exception e) {
    LOGGER_MAIN.log(Level.SEVERE, "Error loading party", e);
    throw e;
  }
}

int SAVEVERSION = 2;


class BaseMap extends Element {
  float[] heightMap;
  int mapWidth, mapHeight;
  long heightMapSeed;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  boolean updateHoveringScale, drawingTaskIcons, drawingUnitBars;
  boolean cinematicMode;
  HashMap<Character, Boolean> keyState;
  boolean[][] fogMap;

  void generateFogMap(int player) {
    fogMap = null;
    fogMap = new boolean[mapHeight][mapWidth];
    int SIGHTRADIUS = 3; // Should be an attribute for parties
    for (int y=0; y<mapHeight; y++) {
      for (int x=0; x<mapWidth; x++) {
        if (parties[y][x]!=null&&parties[y][x].player == player&&parties[y][x].getUnitNumber()>0) {
          for (int y1=y-SIGHTRADIUS; y1<=y+SIGHTRADIUS; y1++) {
            for (int x1=x-SIGHTRADIUS; x1<=x+SIGHTRADIUS; x1++) {
              if (dist(x1, y1, x, y)<=SIGHTRADIUS&&0<=x1&&x1<mapWidth&&0<=y1&&y1<mapHeight) {
                fogMap[y1][x1] = true;
              }
            }
          }
        }
      }
    }
  }

  void saveMap(String filename, int turnNumber, int turnPlayer, Player[] players) {
    LOGGER_MAIN.info("Starting saving progress");
    try {
      int partiesByteCount = 0;
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x] != null) {
            if (parties[y][x] instanceof Battle) {
              partiesByteCount+= Character.BYTES*16;
              partiesByteCount+=getPartySize(((Battle)parties[y][x]).attacker);
              partiesByteCount+=getPartySize(((Battle)parties[y][x]).defender);
            } else {
              partiesByteCount+=getPartySize(parties[y][x]);
            }
          }
          partiesByteCount++;
        }
      }
      int playersByteCount = ((3+players[0].resources.length)*Float.BYTES+3*Integer.BYTES+Character.BYTES*10+1)*players.length;
      ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES*10+Long.BYTES+Integer.BYTES*mapWidth*mapHeight*3+partiesByteCount+playersByteCount+Float.BYTES);
      buffer.putInt(-SAVEVERSION);
      LOGGER_MAIN.finer("Saving version: "+(-SAVEVERSION));
      buffer.putInt(mapWidth);
      LOGGER_MAIN.finer("Saving map width: "+mapWidth);
      buffer.putInt(mapHeight);
      LOGGER_MAIN.finer("Saving map height: "+mapHeight);
      buffer.putInt(partiesByteCount);
      LOGGER_MAIN.finer("Saving parties byte count: "+partiesByteCount);
      buffer.putInt(playersByteCount);
      LOGGER_MAIN.finer("Saving players' byte count: "+playersByteCount);
      buffer.putInt(jsManager.loadIntSetting("party size"));
      LOGGER_MAIN.finer("Saving party size: "+jsManager.loadIntSetting("party size"));
      buffer.putInt(players.length);
      LOGGER_MAIN.finer("Saving number of players: "+players.length);
      buffer.putInt(players[0].resources.length);
      LOGGER_MAIN.finer("Saving number of resources: "+players[0].resources.length);
      buffer.putInt(turnNumber);
      LOGGER_MAIN.finer("Saving turn number: "+turnNumber);
      buffer.putInt(turnPlayer);
      LOGGER_MAIN.finer("Saving player turn: "+turnPlayer);
      buffer.putLong(heightMapSeed);
      LOGGER_MAIN.finer("Saving height map seed: "+heightMapSeed);
      buffer.putFloat(jsManager.loadFloatSetting("water level"));
      LOGGER_MAIN.finer("Saving water level: "+jsManager.loadFloatSetting("water level"));

      LOGGER_MAIN.finer("Saving terrain and buildings");
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          buffer.putInt(terrain[y][x]);
        }
      }
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (buildings[y][x]==null) {
            buffer.putInt(-1);
            buffer.putInt(-1);
          } else {
            buffer.putInt(buildings[y][x].type);
            buffer.putInt(buildings[y][x].image_id);
          }
        }
      }
      LOGGER_MAIN.finer("Saving parties");
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (parties[y][x]==null) {
            buffer.put(byte(0));
          } else if (parties[y][x] instanceof Battle) {
            buffer.put(byte(2));
            for (int i=0; i<16; i++) {
              if (i<parties[y][x].id.length()) {
                buffer.putChar(parties[y][x].id.charAt(i));
              } else {
                buffer.putChar(' ');
              }
            }
            saveParty(buffer, ((Battle)parties[y][x]).attacker);
            saveParty(buffer, ((Battle)parties[y][x]).defender);
          } else {
            buffer.put(byte(1));
            saveParty(buffer, parties[y][x]);
          }
        }
      }
      LOGGER_MAIN.finer("Saving players");
      for (Player p : players) {
        buffer.putFloat(p.cameraCellX);
        buffer.putFloat(p.cameraCellY);
        if (p.blockSize==0) {
          p.blockSize = jsManager.loadIntSetting("starting block size");
        }
        buffer.putFloat(p.blockSize);

        for (float r : p.resources) {
          buffer.putFloat(r);
        }
        buffer.putInt(p.cellX);
        buffer.putInt(p.cellY);
        buffer.putInt(p.colour);
        buffer.put(byte(p.cellSelected));
        for (int i=0; i<10; i++) {
          if (i<p.name.length()) {
            buffer.putChar(p.name.charAt(i));
          } else {
            buffer.putChar(' ');
          }
        }
      }
      LOGGER_MAIN.fine("Saving map to file");
      saveBytes(filename, buffer.array());
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error saving map", e);
      throw e;
    }
  }

  MapSave loadMap(String filename, int resourceCountNew) {
    try {
      boolean versionCheckInt = false;
      byte tempBuffer[] = loadBytes(filename);
      int headerSize = Integer.BYTES*5;
      int versionSpecificData = 0;
      ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize);
      headerBuffer.put(Arrays.copyOfRange(tempBuffer, 0, headerSize));
      headerBuffer.flip();//need flip
      int versionCheck = -headerBuffer.getInt();
      if (versionCheck>0) {
        versionCheckInt = true;
        mapWidth = headerBuffer.getInt();
        versionSpecificData += Integer.BYTES;
      } else {
        LOGGER_MAIN.info("Loading old save with party size 1000");
        mapWidth = -versionCheck;
        jsManager.saveSetting("party size", 1000);
      }
      mapHeight = headerBuffer.getInt();
      int partiesByteCount = headerBuffer.getInt();
      int playersByteCount = headerBuffer.getInt();
      int dataSize = Long.BYTES+partiesByteCount+playersByteCount+(4+mapWidth*mapHeight*3)*Integer.BYTES+Float.BYTES+versionSpecificData;
      ByteBuffer buffer = ByteBuffer.allocate(dataSize);
      if (versionCheckInt) {
        buffer.put(Arrays.copyOfRange(tempBuffer, headerSize, headerSize+dataSize));
      } else {
        buffer.put(Arrays.copyOfRange(tempBuffer, headerSize-Integer.BYTES, headerSize-Integer.BYTES+dataSize));
      }
      buffer.flip();//need flip
      if (versionCheckInt) {
        jsManager.saveSetting("party size", buffer.getInt());
      }
      int playerCount = buffer.getInt();
      int resourceCountOld = buffer.getInt();
      int turnNumber = buffer.getInt();
      int turnPlayer = buffer.getInt();
      heightMapSeed = buffer.getLong();
      float newWaterLevel = buffer.getFloat();
      jsManager.saveSetting("water level", newWaterLevel);
      LOGGER_MAIN.finer("Loading water level: "+newWaterLevel);
      terrain = new int[mapHeight][mapWidth];
      parties = new Party[mapHeight][mapWidth];
      buildings = new Building[mapHeight][mapWidth];

      LOGGER_MAIN.finer("Loading terrain");
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          terrain[y][x] = buffer.getInt();
        }
      }
      LOGGER_MAIN.finer("Loading buildings");
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          int type = buffer.getInt();
          int image_id = buffer.getInt();
          if (type!=-1) {
            buildings[y][x] = new Building(type, image_id);
          }
          if (!versionCheckInt) {
            if (type==9) {
              terrain[y][x] = terrainIndex("quarry site stone");
              LOGGER_MAIN.finer("Changing old quarry tiles into quarry sites - only on old maps");
            }
          }
        }
      }
      LOGGER_MAIN.finer("Loading parties");
      int battleCount = 0;
      int partyCount = 0;
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          Byte partyType = buffer.get();
          if (partyType == -1) {
            char[] rawid;
            char[] p1id;
            char[] p2id;
            if (versionCheck>1) {
              rawid = new char[16];
              for (int i=0; i<16; i++) {
                rawid[i] = buffer.getChar();
              }
              p1id = new char[16];
              for (int i=0; i<16; i++) {
                p1id[i] = buffer.getChar();
              }
              p2id = new char[16];
              for (int i=0; i<16; i++) {
                p2id[i] = buffer.getChar();
              }
            } else {
              rawid = String.format("Old Battle #%d", battleCount).toCharArray();
              battleCount++;
              p1id = String.format("Old Party #%s", partyCount).toCharArray();
              partyCount++;
              p2id = String.format("Old Party #%s", partyCount).toCharArray();
              partyCount++;
            }
            Party p1 = loadParty(buffer, new String(p1id));
            Party p2 = loadParty(buffer, new String(p2id));
            float savedStrength = p1.strength;
            Battle b = new Battle(p1, p2, new String(rawid));
            b.attacker.strength = savedStrength;
            parties[y][x] = b;
          } else if (partyType == 1) {
            char[] rawid;
            if (versionCheck>1) {
              rawid = new char[16];
              for (int i=0; i<16; i++) {
                rawid[i] = buffer.getChar();
              }
            } else {
              rawid = String.format("Old Party #%s", partyCount).toCharArray();
              partyCount++;
            }
            parties[y][x] = loadParty(buffer, new String(rawid));
          }
        }
      }
      LOGGER_MAIN.finer("Loading players, count="+playerCount);
      Player[] players = new Player[playerCount];
      for (int i=0; i<playerCount; i++) {
        float cameraCellX = buffer.getFloat();
        float cameraCellY = buffer.getFloat();
        float blockSize = buffer.getFloat();
        float[] resources = new float[resourceCountNew];
        LOGGER_MAIN.finer(String.format("player %d. Camera Position: (%f, %f) blocksize:%f, resources:%s", i, cameraCellX, cameraCellY, blockSize, Arrays.toString(resources)));
        for (int r=0; r<resourceCountOld; r++) {
          resources[r] = buffer.getFloat();
        }
        int selectedCellX = buffer.getInt();
        int selectedCellY = buffer.getInt();
        int colour = buffer.getInt();
        boolean cellSelected = boolean(buffer.get());
        char[] playerName = new char[10];
        if (versionCheck>1) {
          for (int j=0; j<10; j++) {
            playerName[j] = buffer.getChar();
          }
        } else {
          playerName = String.format("Player %d", i).toCharArray();
        }
        players[i] = new Player(cameraCellX, cameraCellY, blockSize, resources, colour, new String(playerName));
        players[i].cellSelected = cellSelected;
        players[i].cellX = selectedCellX;
        players[i].cellY = selectedCellY;
      }
      LOGGER_MAIN.fine("Seeding height map noise with: "+heightMapSeed);
      noiseSeed(heightMapSeed);
      generateNoiseMaps();
      LOGGER_MAIN.fine("Finished loading save");
      return new MapSave(heightMap, mapWidth, mapHeight, terrain, parties, buildings, turnNumber, turnPlayer, players);
    }

    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading map", e);
      throw e;
    }
  }


  int toMapIndex(int x, int y, int x1, int y1) {
    try {
      return int(x1+x*jsManager.loadFloatSetting("terrain detail")+y1*jsManager.loadFloatSetting("terrain detail")*(mapWidth+1/jsManager.loadFloatSetting("terrain detail"))+y*pow(jsManager.loadFloatSetting("terrain detail"), 2)*(mapWidth+1/jsManager.loadFloatSetting("terrain detail")));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error converting coordinates to map index", e);
      throw e;
    }
  }

  void setDrawingUnitBars(boolean v) {
    drawingUnitBars = v;
  }

  void setDrawingTaskIcons(boolean v) {
    drawingTaskIcons = v;
  }

  //int getRandomGroundType(HashMap<Integer, Float> groundWeightings, float total){
  //  float randomNum = random(0, 1);
  //  float min = 0;
  //  int lastType = 1;
  //  for (int type: groundWeightings.keySet()){
  //    if(randomNum>min&&randomNum<min+groundWeightings.get(type)/total){
  //      return type;
  //    }
  //    min += groundWeightings.get(type)/total;
  //    lastType = type;
  //  }
  //  return lastType;
  //}
  //int getRandomGroundTypeAt(int x, int y, ArrayList<Integer> shuffledTypes, HashMap<Integer, Float> groundWeightings, float total){
  //  double randomNum = Math.cbrt(noise(x*MAPTERRAINNOISESCALE, y*MAPTERRAINNOISESCALE, 1)*2-1)*0.5+0.5;
  //  float min = 0;
  //  int lastType = 1;
  //  for (int type: shuffledTypes){
  //    if(randomNum>min&&randomNum<min+groundWeightings.get(type)/total){
  //      return type;
  //    }
  //    min += groundWeightings.get(type)/total;
  //    lastType = type;
  //  }
  //  return lastType;
  //}
  //int[][] smoothMap(int distance, int firstType, int[][] terrain){
  //  ArrayList<int[]> order = new ArrayList<int[]>();
  //  for (int y=0; y<mapHeight;y++){
  //    for (int x=0; x<mapWidth;x++){
  //      order.add(new int[] {x, y});
  //    }
  //  }
  //  Collections.shuffle(order);
  //  int[][] newMap = new int[mapHeight][mapWidth];
  //  for (int[] coord: order){
  //    if(terrain[coord[1]][coord[0]]==terrainIndex("water")){
  //      newMap[coord[1]][coord[0]] = terrain[coord[1]][coord[0]];
  //    } else {
  //      int[] counts = new int[NUMOFGROUNDTYPES+1];
  //      for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
  //        for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
  //          if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
  //            if(terrain[y1][x1]!=terrainIndex("water")){
  //              counts[terrain[y1][x1]]+=1;
  //            }
  //          }
  //        }
  //      }
  //      int highest = terrain[coord[1]][coord[0]];
  //      for (int i=firstType; i<=NUMOFGROUNDTYPES;i++){
  //        if (counts[i] > counts[highest]){
  //          highest = i;
  //        }
  //      }
  //      newMap[coord[1]][coord[0]] = highest;
  //    }
  //  }
  //  return newMap;
  //}
  void generateTerrain() {
    try {
      LOGGER_MAIN.info("Generating terrain");
      noiseDetail(3, 0.25);
      HashMap<Integer, Float> groundWeightings = new HashMap();
      for (Integer i=0; i<gameData.getJSONArray("terrain").size(); i++) {
        if (gameData.getJSONArray("terrain").getJSONObject(i).isNull("weighting")) {
          groundWeightings.put(i, jsManager.loadFloatSetting(gameData.getJSONArray("terrain").getJSONObject(i).getString("id")+" weighting"));
        } else {
          groundWeightings.put(i, gameData.getJSONArray("terrain").getJSONObject(i).getFloat("weighting"));
        }
      }

      float totalWeighting = 0;
      for (float weight : groundWeightings.values()) {
        totalWeighting+=weight;
      }
      //for(int i=0;i<jsManager.loadIntSetting("ground spawns");i++){
      //  int type = getRandomGroundType(groundWeightings, totalWeighting);
      //  int x = (int)random(mapWidth);
      //  int y = (int)random(mapHeight);
      //  if(isWater(x, y)){
      //    i--;
      //  } else {
      //    terrain[y][x] = type;
      //  }
      //}
      class TempTerrainDetail implements Comparable<TempTerrainDetail> {
        int x;
        int y;
        float noiseValue;
        TempTerrainDetail(int x, int y, float noiseValue) {
          super();
          this.x = x;
          this.y = y;
          this.noiseValue = noiseValue;
        }
        public int compareTo(TempTerrainDetail otherDetail) {
          if (this.noiseValue>otherDetail.noiseValue) {
            return 1;
          } else if (this.noiseValue<otherDetail.noiseValue) {
            return -1;
          } else {
            return 0;
          }
        }
      }
      ArrayList<TempTerrainDetail> cells = new ArrayList<TempTerrainDetail>();
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (isWater(x, y)) {
            terrain[y][x] = terrainIndex("water");
          } else {
            cells.add(new TempTerrainDetail(x, y, noise(x*MAPTERRAINNOISESCALE, y*MAPTERRAINNOISESCALE, 1)));
          }
        }
      }
      TempTerrainDetail[] cellsArray = new TempTerrainDetail[cells.size()];
      cells.toArray(cellsArray);
      Arrays.sort(cellsArray);
      int terrainIndex = 0;
      int totalBelow = 0;
      int lastType = 0;
      for (int type : groundWeightings.keySet()) {
        if (groundWeightings.get(type)>0) {
          while (float(terrainIndex-totalBelow)/cells.size()<groundWeightings.get(type)/totalWeighting) {
            terrain[cellsArray[terrainIndex].y][cellsArray[terrainIndex].x] = type;
            cellsArray[terrainIndex].noiseValue = 0;
            terrainIndex++;
          }
          totalBelow = terrainIndex-1;
        }
        lastType = type;
      }
      for (TempTerrainDetail t : cellsArray) {
        if (t.noiseValue!=0) {
          terrain[t.y][t.x] = lastType;
          //println("map generation possible issue here");
        }
      }
      //ArrayList<int[]> order = new ArrayList<int[]>();
      //for (int y=0; y<mapHeight;y++){
      //  for (int x=0; x<mapWidth;x++){
      //    if(isWater(x, y)){
      //      terrain[y][x] = terrainIndex("water");
      //    } else {
      //      order.add(new int[] {x, y});
      //    }
      //  }
      //}
      //Collections.shuffle(order);
      //for (int[] coord: order){
      //  int x = coord[0];
      //  int y = coord[1];
      //  while (terrain[y][x] == 0||terrain[y][x]==terrainIndex("water")){
      //    int direction = (int) random(8);
      //    switch(direction){
      //      case 0:
      //        x= max(x-1, 0);
      //        break;
      //      case 1:
      //        x = min(x+1, mapWidth-1);
      //        break;
      //      case 2:
      //        y= max(y-1, 0);
      //        break;
      //      case 3:
      //        y = min(y+1, mapHeight-1);
      //        break;
      //      case 4:
      //        x = min(x+1, mapWidth-1);
      //        y = min(y+1, mapHeight-1);
      //        break;
      //      case 5:
      //        x = min(x+1, mapWidth-1);
      //        y= max(y-1, 0);
      //        break;
      //      case 6:
      //        y= max(y-1, 0);
      //        x= max(x-1, 0);
      //        break;
      //      case 7:
      //        y = min(y+1, mapHeight-1);
      //        x= max(x-1, 0);
      //        break;
      //    }
      //  }
      //  terrain[coord[1]][coord[0]] = terrain[y][x];
      //}
      //terrain = smoothMap(jsManager.loadIntSetting("smoothing"), 2, terrain);
      //terrain = smoothMap(jsManager.loadIntSetting("smoothing")+2, 1, terrain);
      for (int y=0; y<mapHeight; y++) {
        for (int x=0; x<mapWidth; x++) {
          if (terrain[y][x] != terrainIndex("water") && (groundMaxRawHeightAt(x, y) > jsManager.loadFloatSetting("hills height")) || getMaxSteepness(x, y)>HILLSTEEPNESS) {
            terrain[y][x] = terrainIndex("hills");
          }
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error generating map", e);
      throw e;
    }
  }
  void generateMap(int mapWidth, int mapHeight, int players) {
    try {
      LOGGER_MAIN.fine("Generating map");
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
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error generating map", e);
      throw e;
    }
  }
  void generateNoiseMaps() {
    try {
      LOGGER_MAIN.info("Generating noise map");
      noiseDetail(4, 0.5);
      heightMap = new float[int((mapWidth+1/jsManager.loadFloatSetting("terrain detail"))*(mapHeight+1/jsManager.loadFloatSetting("terrain detail"))*pow(jsManager.loadFloatSetting("terrain detail"), 2))];
      for (int y = 0; y<mapHeight; y++) {
        for (int y1 = 0; y1<jsManager.loadFloatSetting("terrain detail"); y1++) {
          for (int x = 0; x<mapWidth; x++) {
            for (int x1 = 0; x1<jsManager.loadFloatSetting("terrain detail"); x1++) {
              heightMap[toMapIndex(x, y, x1, y1)] = noise((x+x1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE, (y+y1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE);
            }
          }
          heightMap[toMapIndex(mapWidth, y, 0, y1)] = noise(((mapWidth+1))*MAPHEIGHTNOISESCALE, (y+y1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE);
        }
      }
      for (int x = 0; x<mapWidth; x++) {
        for (int x1 = 0; x1<jsManager.loadFloatSetting("terrain detail"); x1++) {
          heightMap[toMapIndex(x, mapHeight, x1, 0)] = noise((x+x1/jsManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE, (mapHeight)*MAPHEIGHTNOISESCALE);
        }
      }
      heightMap[toMapIndex(mapWidth, mapHeight, 0, 0)] = noise(((mapWidth+1))*MAPHEIGHTNOISESCALE, (mapHeight)*MAPHEIGHTNOISESCALE);
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error generating noise map", e);
      throw e;
    }
  }
  void setHeightsForCell(int x, int y, float h) {
    // Set all the heightmap heights in cell to h
    try {
      int cellIndex;
      for (int x1 = 0; x1 < jsManager.loadFloatSetting("terrain detail"); x1++) {
        for (int y1 = 0; y1 < jsManager.loadFloatSetting("terrain detail"); y1++) {
          cellIndex = toMapIndex(x, y, x1, y1);
          heightMap[cellIndex] = h;
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error setting height for cell with height %s and pos: (%s, %s)", x, y, h), e);
      throw e;
    }
  }
  float getRawHeight(int x, int y, int x1, int y1) {
    try {
      if ((x>=0&&y>=0)&&((x<mapWidth||(x==mapWidth&&x1==0))&&(y<mapHeight||(y==mapHeight&&y1==0)))) {
        return max(heightMap[toMapIndex(x, y, x1, y1)], jsManager.loadFloatSetting("water level"));
      } else {
        // println("A request for the height at a tile outside the map has been made. Ideally this should be prevented earlier"); // Uncomment this when we want to fix it
        return jsManager.loadFloatSetting("water level");
      }
    } 
    catch (ArrayIndexOutOfBoundsException e) {
      LOGGER_MAIN.warning(String.format("Uncaught request for height at (%s, %s) (%s, %s)", x, y, x1, y1));
      return jsManager.loadFloatSetting("water level");
    }
  }
  float getRawHeight(int x, int y, float x1, float y1) {
    try {
      if ((x>=0&&y>=0)&&((x<mapWidth||(x==mapWidth&&x1==0))&&(y<mapHeight||(y==mapHeight&&y1==0)))) {
        float x2 = x1*jsManager.loadFloatSetting("terrain detail");
        float y2 = y1*jsManager.loadFloatSetting("terrain detail");
        float xVal1 = lerp(heightMap[toMapIndex(x, y, floor(x2), floor(y2))], heightMap[toMapIndex(x, y, ceil(x2), floor(y2))], x1);
        float xVal2 = lerp(heightMap[toMapIndex(x, y, floor(x2), ceil(y2))], heightMap[toMapIndex(x, y, ceil(x2), ceil(y2))], x1);
        float yVal = lerp(xVal1, xVal2, y1);
        return max(yVal, jsManager.loadFloatSetting("water level"));
      } else {
        // println("A request for the height at a tile outside the map has been made. Ideally this should be prevented earlier"); // Uncomment this when we want to fix it
        return jsManager.loadFloatSetting("water level");
      }
    } 
    catch (ArrayIndexOutOfBoundsException e) {
      println("this message should never appear. Uncaught request for height at ", x, y, x1, y1);
      return jsManager.loadFloatSetting("water level");
    }
  }
  float getRawHeight(int x, int y) {
    return getRawHeight(x, y, 0, 0);
  }
  float getRawHeight(float x, float y) {
    if (x<0||y<0) {
      return jsManager.loadFloatSetting("water level");
    }
    return getRawHeight(int(x), int(y), (x-int(x)), (y-int(y)));
  }
  float groundMinRawHeightAt(int x1, int y1) {
    try {
      int x = floor(x1);
      int y = floor(y1);
      return min(new float[]{getRawHeight(x, y), getRawHeight(x+1, y), getRawHeight(x, y+1), getRawHeight(x+1, y+1)});
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting min raw ground height at: (%s, %s)", x1, y1), e);
      throw e;
    }
  }
  float groundMaxRawHeightAt(int x1, int y1) {
    try {
      int x = floor(x1);
      int y = floor(y1);
      return max(new float[]{getRawHeight(x, y), getRawHeight(x+1, y), getRawHeight(x, y+1), getRawHeight(x+1, y+1)});
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting max raw ground height at: (%s, %s)", x1, y1), e);
      throw e;
    }
  }
  boolean isWater(int x, int y) {
    return groundMaxRawHeightAt(x, y) == jsManager.loadFloatSetting("water level");
  }

  float getMaxSteepness(int x, int y) {
    try {
      float maxZ, minZ;
      maxZ = 0;
      minZ = 1;
      for (float y1 = y; y1<=y+1; y1+=1.0/jsManager.loadFloatSetting("terrain detail")) {
        for (float x1 = x; x1<=x+1; x1+=1.0/jsManager.loadFloatSetting("terrain detail")) {
          float z = getRawHeight(x1, y1);
          if (z>maxZ) {
            maxZ = z;
          } else if (z<minZ) {
            minZ = z;
          }
        }
      }
      return maxZ-minZ;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error getting max steepness at: (%s, %s)", x, y), e);
      throw e;
    }
  }
}


class Map2D extends BaseMap implements Map {
  final int EW, EH, INITIALHOLD=1000;
  float blockSize, targetBlockSize;
  float mapXOffset, mapYOffset, targetXOffset, targetYOffset, panningSpeed, resetTime;
  boolean panning=false, zooming=false;
  float mapMaxSpeed;
  int elementWidth;
  int elementHeight;
  float[] mapVelocity = {0, 0};
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
  boolean drawRocket;
  PVector rocketPosition;
  PVector rocketVelocity;
  boolean showingBombard;
  int bombardRange;
  color[] playerColours;
  Player[] players;

  Map2D(int x, int y, int w, int h, int[][] terrain, Party[][] parties, Building[][] buildings, int mapWidth, int mapHeight, Player[] players) {
    LOGGER_MAIN.fine("Initialsing map");
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
    this.keyState = new HashMap<Character, Boolean>();
    this.players = players;
  }


  void generateFog(int player) {
    generateFogMap(player);
  }

  void generateShape() {
    cinematicMode = false;
    drawRocket = false;
    this.keyState = new HashMap<Character, Boolean>();
  }
  void clearShape() {
  }
  void updateHoveringScale() {
  }
  void doUpdateHoveringScale() {
  }
  void replaceMapStripWithReloadedStrip(int y) {
  }
  boolean isMoving() {
    return mapVelocity[0] != 0 || mapVelocity[1] != 0;
  }
  Node[][] getMoveNodes() {
    return moveNodes;
  }
  float getTargetZoom() {
    return targetBlockSize;
  }
  float getZoom() {
    return blockSize;
  }
  boolean isZooming() {
    return zooming;
  }
  boolean isPanning() {
    return panning;
  }
  float getFocusedX() {
    return mapXOffset;
  }
  float getFocusedY() {
    return mapYOffset;
  }
  void removeTreeTile(int cellX, int cellY) {
    terrain[cellY][cellX] = terrainIndex("grass");
  }
  void setActive(boolean a) {
    this.mapActive = a;
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
  void setPanningSpeed(float s) {
    panningSpeed = s;
  }
  void limitCoords() {
    mapXOffset = min(max(mapXOffset, -mapWidth*blockSize+elementWidth*0.5), elementWidth*0.5);
    mapYOffset = min(max(mapYOffset, -mapHeight*blockSize+elementHeight*0.5), elementHeight*0.5);
  }
  void reset() {
    mapXOffset = 0;
    mapYOffset = 0;
    mapVelocity[0] = 0;
    mapVelocity[1] = 0;
    blockSize = min(elementWidth/(float)mapWidth, elementWidth/10);
    setPanningSpeed(0.02);
    resetTime = millis();
    frameStartTime = 0;
    cancelMoveNodes();
    showingBombard = false;
  }
  void loadSettings(float x, float y, float bs) {
    targetCell(int(x), int(y), bs);
  }
  void targetOffset(float x, float y) {
    targetXOffset = x;
    targetYOffset = y;
    limitCoords();
    panning = true;
  }
  void targetZoom(float bs) {
    zooming = true;
    targetBlockSize = bs;
  }
  float getTargetOffsetX() {
    return targetXOffset;
  }
  float getTargetOffsetY() {
    return targetYOffset;
  }
  float getTargetBlockSize() {
    return targetBlockSize;
  }
  float [] targetCell(int x, int y, float bs) {
    LOGGER_MAIN.finer(String.format("Targetting cell: %s, %s block size: ", x, y, bs));
    targetBlockSize = bs;
    targetXOffset = -(x+0.5)*targetBlockSize+elementWidth/2+xPos;
    targetYOffset = -(y+0.5)*targetBlockSize+elementHeight/2+yPos;
    panning = true;
    zooming = true;
    return new float[]{targetXOffset, targetYOffset, targetBlockSize};
  }
  void focusMapMouse() {
    // based on mouse click
    if (mouseOver()) {
      targetXOffset = -scaleXInv()*blockSize+elementWidth/2+xPos;
      targetYOffset = -scaleYInv()*blockSize+elementHeight/2+yPos;
      limitCoords();
      panning = true;
    }
  }
  void resetTarget() {
    targetXOffset = mapXOffset;
    targetYOffset = mapYOffset;
    panning = false;
  }
  void resetTargetZoom() {
    zooming = false;
    targetBlockSize = blockSize;
    setPanningSpeed(0.05);
  }

  void updateMoveNodes(Node[][] nodes) {
    moveNodes = nodes;
  }
  void updatePath(ArrayList<int[]> nodes) {
    drawPath = nodes;
  }
  void cancelMoveNodes() {
    moveNodes = null;
  }
  void cancelPath() {
    drawPath = null;
  }

  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
    if (eventType == "mouseWheel") {
      float count = event.getCount();
      if (mouseOver() && mapActive) {
        float zoom = pow(0.9, count);
        float newBlockSize = max(min(blockSize*zoom, (float)elementWidth/10), (float)elementWidth/(float)jsManager.loadIntSetting("map size"));
        if (blockSize != newBlockSize) {
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

  ArrayList<String> mouseEvent(String eventType, int button) {
    if (button == LEFT&mapActive) {
      if (eventType=="mouseDragged" && mapFocused) {
        mapXOffset += (mouseX-startX);
        mapYOffset += (mouseY-startY);
        limitCoords();
        startX = mouseX;
        startY = mouseY;
        resetTarget();
        resetTargetZoom();
      }
      if (eventType == "mousePressed") {
        if (mouseOver()) {
          startX = mouseX;
          startY = mouseY;
          mapFocused = true;
        } else {
          mapFocused = false;
        }
      }
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

  void setWidth(int w) {
    this.elementWidth = w;
  }

  void drawSelectedCell(PVector c, PGraphics panelCanvas) {
    //cell selection
    panelCanvas.stroke(0);
    if (cellSelected) {
      if (max(c.x, xPos)+min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)>xPos && max(c.x, xPos) < elementWidth+xPos && max(c.y, yPos)+min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos)>yPos && max(c.y, yPos) < elementHeight+yPos) {
        panelCanvas.fill(50, 100);
        panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize-1, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize-1, yPos+elementHeight-c.y, blockSize+c.y-yPos));
      }
    }
  }

  void draw(PGraphics panelCanvas) {

    // Terrain
    PImage[] tempTileImages = new PImage[gameData.getJSONArray("terrain").size()];
    PImage[][] tempBuildingImages = new PImage[gameData.getJSONArray("buildings").size()][];
    PImage[] tempPartyImages = new PImage[players.length+1]; // Index 0 is battle
    PImage[] tempTaskImages = new PImage[taskImages.length];
    if (frameStartTime == 0) {
      frameStartTime = millis();
    }
    int frameTime = millis()-frameStartTime;
    if (millis()-resetTime < INITIALHOLD) {
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
    if ((zooming || panning) && pow(mapXOffset-targetXOffset, 2) + pow(mapYOffset-targetYOffset, 2) < blockSize*0.02 && abs(blockSize-targetBlockSize) < 1) {
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
    frameStartTime = millis();
    limitCoords();

    if (blockSize <= 0)
      return;

    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      if (blockSize<24&&!tileType.isNull("low img")) {
        tempTileImages[i] = lowImages.get(tileType.getString("id")).copy();
        tempTileImages[i].resize(ceil(blockSize), 0);
      } else {
        tempTileImages[i] = tileImages.get(tileType.getString("id")).copy();
        tempTileImages[i].resize(ceil(blockSize), 0);
      }
    }
    for (int i=0; i<gameData.getJSONArray("buildings").size(); i++) {
      JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
      tempBuildingImages[i] = new PImage[buildingImages.get(buildingType.getString("id")).length];
      for (int j=0; j<buildingImages.get(buildingType.getString("id")).length; j++) {
        tempBuildingImages[i][j] = buildingImages.get(buildingType.getString("id"))[j].copy();
        tempBuildingImages[i][j].resize(ceil(blockSize*48/64), 0);
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
        panelCanvas.image(tempTileImages[terrain[y][x]], x2, y2);

        //Buildings
        if (buildings[y][x] != null) {
          c = new PVector(scaleX(x), scaleY(y));
          int border = round((64-48)*blockSize/(2*64));
          int imgSize = round(blockSize*48/60);
          drawCroppedImage(round(c.x+border), round(c.y+border*2), imgSize, imgSize, tempBuildingImages[buildings[y][x].type][buildings[y][x].image_id], panelCanvas);
        }
        //Parties
        if (parties[y][x]!=null) {
          c = new PVector(scaleX(x), scaleY(y));
          if (c.x<xPos+elementWidth&&c.y+blockSize/8+blockSize>yPos&&c.y<yPos+elementHeight) {
            panelCanvas.noStroke();
            if (parties[y][x] instanceof Battle) {
              Battle battle = (Battle) parties[y][x];
              if (c.x+blockSize>xPos) {
                panelCanvas.fill(120, 120, 120);
                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
              }
              if (c.x+blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")>xPos) {
                panelCanvas.fill(playerColours[battle.attacker.player]);
                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), max(0, min(blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*battle.attacker.getUnitNumber()/jsManager.loadIntSetting("party size")+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y, blockSize/16+c.y-yPos)));
              }
              if (c.x+blockSize>xPos) {
                panelCanvas.fill(120, 120, 120);
                panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/16+c.y-yPos)));
              }
              if (c.x+blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")>xPos) {
                panelCanvas.fill(playerColours[battle.defender.player]);
                panelCanvas.rect(max(c.x, xPos), max(c.y+blockSize/16, yPos), max(0, min(blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*battle.defender.getUnitNumber()/jsManager.loadIntSetting("party size")+c.x-xPos)), max(0, min(ceil(blockSize/16), yPos+elementHeight-c.y-blockSize/16, blockSize/8+c.y-yPos)));
              }
            } else {
              if (c.x+blockSize>xPos) {
                panelCanvas.fill(120, 120, 120);
                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
              }
              if (c.x+blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")>xPos) {
                panelCanvas.fill(playerColours[parties[y][x].player]);
                panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size"), xPos+elementWidth-c.x, blockSize*parties[y][x].getUnitNumber()/jsManager.loadIntSetting("party size")+c.x-xPos), min(blockSize/8, yPos+elementHeight-c.y, blockSize/8+c.y-yPos));
              }
            }
            int imgSize = round(blockSize);
            if (parties[y][x].player == -1) {
              drawCroppedImage(floor(c.x), floor(c.y), imgSize, imgSize, tempPartyImages[0], panelCanvas);
            } else {
              drawCroppedImage(floor(c.x), floor(c.y), imgSize, imgSize, tempPartyImages[parties[y][x].player+1], panelCanvas);
            }
            
            JSONObject jo = gameData.getJSONArray("tasks").getJSONObject(parties[y][x].getTask());
            if (jo != null && !jo.isNull("img")) {
              drawCroppedImage(floor(c.x+13*blockSize/32), floor(c.y+blockSize/2), ceil(3*blockSize/16), ceil(3*blockSize/16), tempTaskImages[parties[y][x].getTask()], panelCanvas);
            }
          }
        }
        if (cellSelected&&y==selectedCellY&&x==selectedCellX&&!cinematicMode) {
          drawSelectedCell(selectedCell, panelCanvas);
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
              if (c.x+textWidth(turnsLeftString) < elementWidth+xPos && c.y+2*(textAscent()+textDescent()) < elementHeight+yPos) {
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

    if (moveNodes != null) {
      for (int y1=0; y1<mapHeight; y1++) {
        for (int x=0; x<mapWidth; x++) {
          if (moveNodes[y1][x] != null) {
            c = new PVector(scaleX(x), scaleY(y1));
            if (max(c.x, xPos)+min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos)>xPos && max(c.x, xPos) < elementWidth+xPos && max(c.y, yPos)+min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos)>yPos && max(c.y, yPos) < elementHeight+yPos) {
              if (blockSize > 10*jsManager.loadFloatSetting("text scale") && moveNodes[y1][x].cost <= parties[selectedCellY][selectedCellX].getMovementPoints()) {
                shadeCell(panelCanvas, c.x, c.y, color(50, 150));
                panelCanvas.fill(255);
                panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
                panelCanvas.textAlign(CENTER, CENTER);
                String s = ""+moveNodes[y1][x].cost;
                s = s.substring(0, min(s.length(), 3));
                BigDecimal cost = new BigDecimal(s);
                String s2 = cost.stripTrailingZeros().toPlainString();
                if (c.x+panelCanvas.textWidth(s2) < elementWidth+xPos && c.y+2*(textAscent()+textDescent()) < elementHeight+yPos) {
                  panelCanvas.text(s2, c.x+blockSize/2, c.y+blockSize/2);
                }
              }
            }
          }
        }
      }
    }

    if (jsManager.loadBooleanSetting("fog of war")) {
      for (int y1=0; y1<mapHeight; y1++) {
        for (int x=0; x<mapWidth; x++) {
          if (!fogMap[y1][x]) {
            c = new PVector(scaleX(x), scaleY(y1));
            panelCanvas.fill(0, 50);
            panelCanvas.noStroke();
            panelCanvas.rect(max(c.x, xPos), max(c.y, yPos), min(blockSize, xPos+elementWidth-c.x, blockSize+c.x-xPos), min(blockSize, yPos+elementHeight-c.y, blockSize+c.y-yPos));
          }
        }
      }
    }

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
            shadeCell(panelCanvas, scaleX(x), scaleY(y), color(255, 0, 0, 100));
          }
        }
      }
      drawBombard(panelCanvas);
    }

    panelCanvas.noFill();
    panelCanvas.stroke(0);
    panelCanvas.rect(xPos, yPos, elementWidth, elementHeight);
  }
  
  void shadeCell(PGraphics canvas, float x, float y, color c) {
    canvas.fill(c);
    canvas.rect(max(x, xPos), max(y, yPos), min(blockSize, xPos+elementWidth-x, blockSize+x-xPos), min(blockSize, yPos+elementHeight-y, blockSize+y-yPos));
  }
  
  int sign(float x) {
    if (x > 0) {
      return 1;
    } else if (x < 0) {
      return -1;
    }
    return 0;
  }
  void drawCroppedImage(int x, int y, int w, int h, PImage img, PGraphics panelCanvas) {
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
  float scaleX(float x) {
    return x*blockSize + mapXOffset + xPos;
  }
  float scaleY(float y) {
    return y*blockSize + mapYOffset + yPos;
  }
  float scaleXInv() {
    return (mouseX-mapXOffset-xPos)/blockSize;
  }
  float scaleYInv() {
    return (mouseY-mapYOffset-yPos)/blockSize;
  }
  boolean mouseOver() {
    return mouseX > xPos && mouseX < xPos+elementWidth && mouseY > yPos && mouseY < yPos+elementHeight;
  }

  void enableRocket(PVector pos, PVector vel) {
    LOGGER_MAIN.fine("Rocket enabled in 2d map");
    drawRocket = true;
    rocketPosition = pos;
    rocketVelocity = vel;
  }

  void disableRocket() {
    LOGGER_MAIN.fine("Rocket disabled in 2d map");
    drawRocket = false;
  }

  void drawRocket(PGraphics canvas, PImage[][] buildingImages) {
    PVector c = new PVector(scaleX(rocketPosition.x+0.5), scaleY(rocketPosition.y+0.5-rocketPosition.z));
    int border = round((64-48)*blockSize/(2*64));
    canvas.pushMatrix();
    canvas.translate(round(c.x+border), round(c.y+border*2));
    canvas.rotate(atan2(rocketVelocity.x, rocketVelocity.z));
    canvas.translate(-blockSize/2, -blockSize/2);
    canvas.image(buildingImages[buildingIndex("Rocket Factory")-1][2], 0, 0);
    canvas.popMatrix();
  }
  
  void drawBombard(PGraphics canvas) {
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
  
  void enableBombard(int range) {
    showingBombard = true;
    bombardRange = range;
  }
  void disableBombard() {
    showingBombard = false;
  }
  
  void setPlayerColours(color[] playerColours) {
    this.playerColours = playerColours;
  }
}
