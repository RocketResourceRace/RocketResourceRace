package map;


import controller.BanditController;
import event.Action;
import json.JSONManager;
import party.Battle;
import party.Party;
import party.Siege;
import player.Player;
import processing.core.PApplet;
import state.Element;
import util.Cell;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import static json.JSONManager.gameData;
import static json.JSONManager.terrainIndex;
import static processing.core.PApplet.*;
import static util.Constants.*;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class BaseMap extends Element {
    float[] heightMap;
    int mapWidth, mapHeight;
    private long heightMapSeed;
    public int[][] terrain;
    public Party[][] parties;
    public Building[][] buildings;
    boolean drawingTaskIcons;
    boolean drawingUnitBars;
    public boolean cinematicMode;
    HashMap<Character, Boolean> keyState;
    Cell[][] visibleCells;

    public void updateVisibleCells(Cell[][] visibleCells){
        this.visibleCells = visibleCells;
    }

    public void saveMap(String filename, int turnNumber, int turnPlayer, Player[] players) {
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
            int mapSize = mapWidth*mapHeight;
            int playersByteCount = ((3+players[0].resources.length)*Float.BYTES+4*Integer.BYTES+Character.BYTES*10+1+mapSize)*players.length;
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES*10+Long.BYTES+Integer.BYTES*mapSize*5+partiesByteCount+playersByteCount+Float.BYTES*(1+mapSize));
            int SAVEVERSION = 2;
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
            buffer.putInt(JSONManager.loadIntSetting("party size"));
            LOGGER_MAIN.finer("Saving party size: "+JSONManager.loadIntSetting("party size"));
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
            buffer.putFloat(JSONManager.loadFloatSetting("water level"));
            LOGGER_MAIN.finer("Saving water level: "+ JSONManager.loadFloatSetting("water level"));

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
                        buffer.putFloat(-1);
                        buffer.putInt(-1);
                    } else {
                        LOGGER_MAIN.finer("Saving a building");
                        buffer.putInt(buildings[y][x].getType());
                        buffer.putInt(buildings[y][x].getImageId());
                        buffer.putFloat(buildings[y][x].getHealth());
                        buffer.putInt(buildings[y][x].getPlayerId());
                    }
                }
            }
            LOGGER_MAIN.finer("Saving parties");
            for (int y=0; y<mapHeight; y++) {
                for (int x=0; x<mapWidth; x++) {
                    if (parties[y][x]==null) {
                        buffer.put(PApplet.parseByte(0));
                    } else if (parties[y][x] instanceof Siege) {
                        buffer.put(PApplet.parseByte(3));
                        for (int i=0; i<16; i++) {
                            if (i<parties[y][x].id.length()) {
                                buffer.putChar(parties[y][x].id.charAt(i));
                            } else {
                                buffer.putChar(' ');
                            }
                        }
                        saveParty(buffer, ((Battle)parties[y][x]).attacker);
                        saveParty(buffer, ((Battle)parties[y][x]).defender);
                        Building defence = ((Siege)parties[y][x]).getDefence();
                        buffer.putInt(defence.getType());
                        buffer.putInt(defence.getImageId());
                        buffer.putFloat(defence.getHealth());
                        buffer.putInt(defence.getPlayerId());

                    } else if (parties[y][x] instanceof Battle) {
                        buffer.put(PApplet.parseByte(2));
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
                        LOGGER_MAIN.finer("Saving a party");
                        buffer.put(PApplet.parseByte(1));
                        saveParty(buffer, parties[y][x]);
                    }
                }
            }
            LOGGER_MAIN.finer("Saving players");
            for (Player p : players) {
                buffer.putFloat(p.cameraCellX);
                buffer.putFloat(p.cameraCellY);
                if (p.blockSize==0) {
                    p.blockSize = JSONManager.loadIntSetting("starting block size");
                }
                buffer.putFloat(p.blockSize);

                for (float r : p.resources) {
                    buffer.putFloat(r);
                }
                buffer.putInt(p.cellX);
                buffer.putInt(p.cellY);
                buffer.putInt(p.colour);
                buffer.putInt(p.controllerType);
                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        if (p.visibleCells[y][x] == null) {
                            buffer.put(PApplet.parseByte(0));
                        } else {
                            buffer.put(PApplet.parseByte(1));
                        }
                    }
                }
                buffer.put(PApplet.parseByte(p.cellSelected));
                for (int i=0; i<10; i++) {
                    if (i<p.name.length()) {
                        buffer.putChar(p.name.charAt(i));
                    } else {
                        buffer.putChar(' ');
                    }
                }
            }
            LOGGER_MAIN.finer("Saving bandit memory");
            if (players[players.length-1].playerController instanceof BanditController) {
                BanditController bc = (BanditController)players[players.length-1].playerController;
                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        buffer.putInt(bc.cellsTargetedWeightings[y][x]);
                    }
                }
            } else {
                LOGGER_MAIN.warning("Final player isn't a bandit");
            }
            LOGGER_MAIN.fine("Saving map to file");
            saveBytes(new File(filename), buffer.array());
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving map", e);
            throw e;
        }
    }

    public MapSave loadMap(String filename, int resourceCountNew) {
        try {
            boolean versionCheckInt = false;
            byte[] tempBuffer = loadBytes(new File(filename));
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
                JSONManager.saveSetting("party size", 1000);
            }
            mapHeight = headerBuffer.getInt();
            int partiesByteCount = headerBuffer.getInt();
            int playersByteCount = headerBuffer.getInt();
            int mapSize = mapHeight*mapHeight;
            int dataSize = Integer.BYTES*10+Long.BYTES+Integer.BYTES*mapSize*5+partiesByteCount+playersByteCount+Float.BYTES*(1+mapSize)+versionSpecificData;
            ByteBuffer buffer = ByteBuffer.allocate(dataSize);
            if (versionCheckInt) {
                buffer.put(Arrays.copyOfRange(tempBuffer, headerSize, headerSize+dataSize));
            } else {
                buffer.put(Arrays.copyOfRange(tempBuffer, headerSize-Integer.BYTES, headerSize-Integer.BYTES+dataSize));
            }
            buffer.flip();//need flip
            if (versionCheckInt) {
                JSONManager.saveSetting("party size", buffer.getInt());
            }
            int playerCount = buffer.getInt();
            int resourceCountOld = buffer.getInt();
            int turnNumber = buffer.getInt();
            int turnPlayer = buffer.getInt();
            heightMapSeed = buffer.getLong();
            float newWaterLevel = buffer.getFloat();
            JSONManager.saveSetting("water level", newWaterLevel);
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
                    int imageId = buffer.getInt();
                    float health = buffer.getFloat();
                    int playerId= buffer.getInt();
                    if (type!=-1) {
                        buildings[y][x] = new Building(type, imageId, playerId);
                        buildings[y][x].setHealth(health);
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
                    byte partyType = buffer.get();
                    if (partyType == 2 || partyType == 3) {
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
                        } else {
                            rawid = String.format("Old Battle #%d", battleCount).toCharArray();
                            battleCount++;
                            p1id = String.format("Old Party #%s", partyCount).toCharArray();
                            partyCount++;
                            p2id = String.format("Old Party #%s", partyCount).toCharArray();
                            partyCount++;
                        }
                        Party p1 = loadParty(buffer, new String(p1id));

                        if (versionCheck>1) {
                            for (int i=0; i<16; i++) {
                                p2id[i] = buffer.getChar();
                            }
                        }
                        Party p2 = loadParty(buffer, new String(p2id));
                        float savedStrength = p1.strength;
                        if (partyType == 3) {
                            int type = buffer.getInt();
                            int imageId = buffer.getInt();
                            float health = buffer.getFloat();
                            int playerId= buffer.getInt();
                            Building defence = new Building(type, imageId, playerId);
                            defence.setHealth(health);

                            Siege s = new Siege(p1, defence, p2, new String(rawid));
                            s.attacker.strength = savedStrength;
                            parties[y][x] = s;
                        } else {
                            Battle b = new Battle(p1, p2, new String(rawid));
                            b.attacker.strength = savedStrength;
                            parties[y][x] = b;
                        }
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
                int controllerType = buffer.getInt();
                boolean[][] seenCells = new boolean[mapHeight][];
                for (int y = 0; y < mapHeight; y++) {
                    seenCells[y] = new boolean[mapWidth];
                    for (int x = 0; x < mapWidth; x++) {
                        seenCells[y][x] = PApplet.parseBoolean(buffer.get());
                    }
                }
                boolean cellSelected = PApplet.parseBoolean(buffer.get());
                char[] playerName = new char[10];
                if (versionCheck>1) {
                    for (int j=0; j<10; j++) {
                        playerName[j] = buffer.getChar();
                    }
                } else {
                    playerName = String.format("Player %d", i).toCharArray();
                }
                players[i] = new Player(cameraCellX, cameraCellY, blockSize, resources, colour, new String(playerName), controllerType, i);
                players[i].updateVisibleCells(terrain, buildings, parties, seenCells);
                players[i].cellSelected = cellSelected;
                players[i].cellX = selectedCellX;
                players[i].cellY = selectedCellY;
            }

            LOGGER_MAIN.finer("Loading bandit memory");
            if (players[players.length-1].playerController instanceof BanditController) {
                BanditController bc = (BanditController)players[players.length-1].playerController;
                for (int y = 0; y < mapHeight; y++) {
                    for (int x = 0; x < mapWidth; x++) {
                        bc.cellsTargetedWeightings[y][x] = buffer.getInt();
                    }
                }
            } else {
                LOGGER_MAIN.warning("Final player isn't a bandit");
            }
            LOGGER_MAIN.fine("Seeding height map noise with: "+heightMapSeed);
            noiseSeed(heightMapSeed);
            generateNoiseMaps();
            LOGGER_MAIN.fine("Finished loading save");
            return new MapSave(mapWidth, mapHeight, terrain, parties, buildings, turnNumber, turnPlayer, players);
        }

        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading map", e);
            throw e;
        }
    }


    private int toMapIndex(int x, int y, int x1, int y1) {
        try {
            return PApplet.parseInt(x1+x*JSONManager.loadFloatSetting("terrain detail")+y1*JSONManager.loadFloatSetting("terrain detail")*(mapWidth+1/JSONManager.loadFloatSetting("terrain detail"))+y*pow(JSONManager.loadFloatSetting("terrain detail"), 2)*(mapWidth+1/JSONManager.loadFloatSetting("terrain detail")));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error converting coordinates to map index", e);
            throw e;
        }
    }

    public void setDrawingUnitBars(boolean v) {
        drawingUnitBars = v;
    }

    public void setDrawingTaskIcons(boolean v) {
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
    private void generateTerrain() {
        try {
            LOGGER_MAIN.info("Generating terrain");
            noiseDetail(3, 0.25f);
            HashMap<Integer, Float> groundWeightings = new HashMap<>();
            for (int i = 0; i<gameData.getJSONArray("terrain").size(); i++) {
                if (gameData.getJSONArray("terrain").getJSONObject(i).isNull("weighting")) {
                    groundWeightings.put(i, JSONManager.loadFloatSetting(gameData.getJSONArray("terrain").getJSONObject(i).getString("id")+" weighting"));
                } else {
                    groundWeightings.put(i, gameData.getJSONArray("terrain").getJSONObject(i).getFloat("weighting"));
                }
            }

            float totalWeighting = 0;
            for (float weight : groundWeightings.values()) {
                totalWeighting+=weight;
            }
            //for(int i=0;i<JSONManager.loadIntSetting("ground spawns");i++){
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
                private int x;
                private int y;
                private float noiseValue;
                private TempTerrainDetail(int x, int y, float noiseValue) {
                    super();
                    this.x = x;
                    this.y = y;
                    this.noiseValue = noiseValue;
                }
                public int compareTo(TempTerrainDetail otherDetail) {
                    return Float.compare(this.noiseValue, otherDetail.noiseValue);
                }
            }
            ArrayList<TempTerrainDetail> cells = new ArrayList<>();
            for (int y=0; y<mapHeight; y++) {
                for (int x=0; x<mapWidth; x++) {
                    if (isWater(x, y)) {
                        terrain[y][x] = terrainIndex("water");
                    } else {
                        cells.add(new TempTerrainDetail(x, y, noise(x*MAPTERRAINNOISESCALE, y*MAPTERRAINNOISESCALE, 1.0f)));
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
                    while (PApplet.parseFloat(terrainIndex-totalBelow)/cells.size()<groundWeightings.get(type)/totalWeighting) {
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
            //terrain = smoothMap(JSONManager.loadIntSetting("smoothing"), 2, terrain);
            //terrain = smoothMap(JSONManager.loadIntSetting("smoothing")+2, 1, terrain);
            for (int y=0; y<mapHeight; y++) {
                for (int x=0; x<mapWidth; x++) {
                    if (terrain[y][x] != terrainIndex("water") && (groundMaxRawHeightAt(x, y) > JSONManager.loadFloatSetting("hills height")) || getMaxSteepness(x, y)>HILLSTEEPNESS) {
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
    public void generateMap(int mapWidth, int mapHeight, int players) {
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
    private void generateNoiseMaps() {
        try {
            LOGGER_MAIN.info("Generating noise map");
            noiseDetail(4, 0.5f);
            heightMap = new float[PApplet.parseInt((mapWidth+1/JSONManager.loadFloatSetting("terrain detail"))*(mapHeight+1/JSONManager.loadFloatSetting("terrain detail"))*pow(JSONManager.loadFloatSetting("terrain detail"), 2))];
            for (int y = 0; y<mapHeight; y++) {
                for (int y1 = 0; y1<JSONManager.loadFloatSetting("terrain detail"); y1++) {
                    for (int x = 0; x<mapWidth; x++) {
                        for (int x1 = 0; x1<JSONManager.loadFloatSetting("terrain detail"); x1++) {
                            heightMap[toMapIndex(x, y, x1, y1)] = noise((x+x1/JSONManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE, (y+y1/JSONManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE);
                        }
                    }
                    heightMap[toMapIndex(mapWidth, y, 0, y1)] = noise(((mapWidth+1))*MAPHEIGHTNOISESCALE, (y+y1/JSONManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE);
                }
            }
            for (int x = 0; x<mapWidth; x++) {
                for (int x1 = 0; x1<JSONManager.loadFloatSetting("terrain detail"); x1++) {
                    heightMap[toMapIndex(x, mapHeight, x1, 0)] = noise((x+x1/JSONManager.loadFloatSetting("terrain detail"))*MAPHEIGHTNOISESCALE, (mapHeight)*MAPHEIGHTNOISESCALE);
                }
            }
            heightMap[toMapIndex(mapWidth, mapHeight, 0, 0)] = noise(((mapWidth+1))*MAPHEIGHTNOISESCALE, (mapHeight)*MAPHEIGHTNOISESCALE);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error generating noise map", e);
            throw e;
        }
    }
    public void setHeightsForCell(int x, int y, float h) {
        // Set all the heightmap heights in cell to h
        try {
            int cellIndex;
            for (int x1 = 0; x1 < JSONManager.loadFloatSetting("terrain detail"); x1++) {
                for (int y1 = 0; y1 < JSONManager.loadFloatSetting("terrain detail"); y1++) {
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
    private float getRawHeight(int x, int y, int x1, int y1) {
        try {
            if ((x>=0&&y>=0)&&((x<mapWidth||(x==mapWidth&&x1==0))&&(y<mapHeight||(y==mapHeight&&y1==0)))) {
                return max(heightMap[toMapIndex(x, y, x1, y1)], JSONManager.loadFloatSetting("water level"));
            } else {
                // println("A request for the height at a tile outside the map has been made. Ideally this should be prevented earlier"); // Uncomment this when we want to fix it
                return JSONManager.loadFloatSetting("water level");
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            LOGGER_MAIN.warning(String.format("Uncaught request for height at (%s, %s) (%s, %s)", x, y, x1, y1));
            return JSONManager.loadFloatSetting("water level");
        }
    }
    private float getRawHeight(int x, int y, float x1, float y1) {
        try {
            if ((x>=0&&y>=0)&&((x<mapWidth||(x==mapWidth&&x1==0))&&(y<mapHeight||(y==mapHeight&&y1==0)))) {
                float x2 = x1*JSONManager.loadFloatSetting("terrain detail");
                float y2 = y1*JSONManager.loadFloatSetting("terrain detail");
                float xVal1 = lerp(heightMap[toMapIndex(x, y, floor(x2), floor(y2))], heightMap[toMapIndex(x, y, ceil(x2), floor(y2))], x1);
                float xVal2 = lerp(heightMap[toMapIndex(x, y, floor(x2), ceil(y2))], heightMap[toMapIndex(x, y, ceil(x2), ceil(y2))], x1);
                float yVal = lerp(xVal1, xVal2, y1);
                return max(yVal, JSONManager.loadFloatSetting("water level"));
            } else {
                // println("A request for the height at a tile outside the map has been made. Ideally this should be prevented earlier"); // Uncomment this when we want to fix it
                return JSONManager.loadFloatSetting("water level");
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            println("this message should never appear. Uncaught request for height at ", x, y, x1, y1);
            return JSONManager.loadFloatSetting("water level");
        }
    }
    private float getRawHeight(int x, int y) {
        return getRawHeight(x, y, 0, 0);
    }
    float getRawHeight(float x, float y) {
        if (x<0||y<0) {
            return JSONManager.loadFloatSetting("water level");
        }
        return getRawHeight(PApplet.parseInt(x), PApplet.parseInt(y), (x-PApplet.parseInt(x)), (y-PApplet.parseInt(y)));
    }
    public float groundMinRawHeightAt(int x1, int y1) {
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
    private float groundMaxRawHeightAt(int x1, int y1) {
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
    public boolean isWater(int x, int y) {
        return groundMaxRawHeightAt(x, y) == JSONManager.loadFloatSetting("water level");
    }

    private float getMaxSteepness(int x, int y) {
        try {
            float maxZ, minZ;
            maxZ = 0;
            minZ = 1;
            for (float y1 = y; y1<=y+1; y1+=1.0f/JSONManager.loadFloatSetting("terrain detail")) {
                for (float x1 = x; x1<=x+1; x1+=1.0f/JSONManager.loadFloatSetting("terrain detail")) {
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

    private void saveParty(ByteBuffer b, Party p) {
        try {
            b.put(p.byteRep);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving party", e);
            throw e;
        }
    }
    private Party loadParty(ByteBuffer b, String id) {
        LOGGER_MAIN.finer("Loading party from save: "+id);
        try {
            int actionCount = b.getInt();
            ArrayList<Action> actions = new ArrayList<>();
            for (int i=0; i<actionCount; i++) {
                String notification;
                String terrain;
                String building;
                int notificationTextSize = b.getInt();
                int terrainTextSize = b.getInt();
                int buildingTextSize = b.getInt();
                float turns = b.getFloat();
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
            ArrayList<int[]> path = new ArrayList<>();
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
            int[] equipment = new int[JSONManager.getNumEquipmentClasses()];
            for (int i = 0; i < JSONManager.getNumEquipmentClasses(); i++) {
                equipment[i] = b.getInt();
            }
            int[] equipmentQuantities = new int[JSONManager.getNumEquipmentClasses()];
            for (int i = 0; i < JSONManager.getNumEquipmentClasses(); i++) {
                equipmentQuantities[i] = b.getInt();
            }
            float[] proficiencies = new float[JSONManager.getNumProficiencies()];
            for (int i = 0; i < JSONManager.getNumProficiencies(); i++) {
                proficiencies[i] = b.getFloat();
            }
            int unitCap = b.getInt();
            boolean autoStockUp = b.get()==PApplet.parseByte(1);
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

            LOGGER_MAIN.finer(String.format("Loaded party with id: %s, player: %d, unitNumber: %d pathSize: %d, actionCount: %d", id, player, unitNumber, pathSize, actionCount));

            return p;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading party", e);
            throw e;
        }
    }


    private int getPartySize(Party p) {
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
            partyBuffer.put(PApplet.parseByte(p.autoStockUp));
            p.byteRep = partyBuffer.array();
            return totalSize;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting party size", e);
            throw e;
        }
    }
}