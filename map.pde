import java.util.Collections;


class TestMap extends State{
  TestMap(){
    addElement("map", new Map(0, 0, 1000, 700, mapSize));
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (_key == ESC){
      newState = "menu";
    }
    return new ArrayList<String>();
  }
}


class Map extends Element{
  int[][] map;
  int mapWidth;
  int mapHeight;
  float blockSize;
  int numOfGroundTypes;
  int numOfGroundSpawns;
  int waterLevel;
  int initialSmooth;
  int completeSmooth;
  float mapXOffset;
  float mapYOffset;
  PImage buffer;
  int elementWidth;
  int elementHeight;
  int[] mapSpeed = {0,0};
  int startX;
  int startY;
  boolean zoomChanged;

  Map(int x, int y, int w, int h, int mapSize){
    elementWidth = w;
    elementHeight = h;
    mapWidth = mapSize;
    mapHeight = mapSize;
    blockSize = elementHeight/mapSize;
    numOfGroundTypes = 3;
    numOfGroundSpawns = 100;
    waterLevel = 3;
    initialSmooth = 7;
    completeSmooth = 5;
    mapXOffset = -blockSize*mapSize/2;
    mapYOffset = -blockSize*mapSize/2;
    generateMap();
  }
  ArrayList<String> mouseEvent(String eventType, int button){
    if (eventType == "mouseClicked"){
      if (button == LEFT){
        blockSize *= 1.25;
      }
      if (button == RIGHT){
        blockSize *= 0.8;
      }
    }
    else {
      if (eventType=="mouseDragged"){
        mapXOffset += (mouseX-startX);
        mapYOffset += (mouseY-startY);
        mapXOffset = min(max(mapXOffset, -elementWidth/2), -(mapWidth-elementWidth));
        mapYOffset = min(max(mapYOffset, -elementHeight/2), -mapHeight);
        startX = mouseX;
        startY = mouseY;
      }
      if (eventType == "mousePressed"){
        startX = mouseX;
        startY = mouseY;
      }
    }
    return new ArrayList<String>();
  }
  ArrayList<String> keyboardEvent(String eventType, int _key){
    if (eventType == "keyPressed"){
      if (_key == 'a'&&mapSpeed[0]>-1){
        mapSpeed[0] -= 1;
      }
      if (_key == 's'&&mapSpeed[1]<1){
        mapSpeed[1] += 1;
      }
      if (_key == 'd'&&mapSpeed[0]<1){
        mapSpeed[0] += 1;
      }
      if (_key == 'w'&&mapSpeed[1]>-1){
        mapSpeed[1] -= 1;
      }
    }
    if (eventType == "keyReleased"){
      if (_key == 'a'&&mapSpeed[0]<0){
        mapSpeed[0] += 1;
      }
      if (_key == 's'&&mapSpeed[1]>0){
        mapSpeed[1] -= 1;
      }
      if (_key == 'd'&&mapSpeed[0]>0){
        mapSpeed[0] -= 1;
      }
      if (_key == 'w'&&mapSpeed[1]<0){
        mapSpeed[1] += 1;
      }
    }
    return new ArrayList<String>();
  }
  int[][] smoothMap(int distance, int firstType){
    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=0; y<mapHeight;y++){
      for (int x=0; x<mapWidth;x++){
        order.add(new int[] {x, y});
      }
    }
    Collections.shuffle(order);
    int[][] newMap = new int[mapHeight][mapWidth];
    for (int[] coord: order){
      int[] counts = new int[numOfGroundTypes+1];
      for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
       for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
         if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
           counts[map[y1][x1]]+=1;
         }
       }
      }
      int highest = map[coord[1]][coord[0]];
      for (int i=firstType; i<=numOfGroundTypes;i++){
        if (counts[i] > counts[highest]){
          highest = i;
        }
      }
      newMap[coord[1]][coord[0]] = highest;
    }
    return newMap;
  }
  
  
  void generateMap(){
    map = new int[mapHeight][mapWidth];
    for(int y=0; y<mapHeight; y++){
      map[y][0] = 1;
      map[y][mapWidth-1] = 1;
    }
    for(int x=1; x<mapWidth-1; x++){
      map[0][x] = 1;
      map[mapHeight-1][x] = 1;
    }
    for(int i=0;i<numOfGroundSpawns;i++){
      int type = (int)random(numOfGroundTypes)+1;
      int x = (int)random(mapWidth-2)+1;
      int y = (int)random(mapHeight-2)+1;
      map[y][x] = type;
      // Water will be type 1
      if (type==1){
        for (int y1=y-waterLevel+1;y1<y+waterLevel;y1++){
         for (int x1 = x-waterLevel+1; x1<x+waterLevel;x1++){
           if (y1 < mapHeight && y1 >= 0 && x1 < mapWidth && x1 >= 0)
             map[y1][x1] = type;
         }
        }
      }
    }
    ArrayList<int[]> order = new ArrayList<int[]>();
    for (int y=1; y<mapHeight-1;y++){
      for (int x=1; x<mapWidth-1;x++){
        order.add(new int[] {x, y});
      }
    }
    Collections.shuffle(order);
    for (int[] coord: order){
      int x = coord[0];
      int y = coord[1];
      while (map[y][x] == 0){
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
      map[coord[1]][coord[0]] = map[y][x];
    }
    map = smoothMap(initialSmooth, 2);
    map = smoothMap(completeSmooth, 1);
  }
  
  void draw(){
    PImage[] tempImages = new PImage[numOfGroundTypes];
    for (int i=0; i<3; i++){
      tempImages[i] = tileImages[i].copy();
      tempImages[i].resize(ceil(blockSize), 0);
    }
    for(int y=0;y<mapHeight;y++){
     for (int x=0; x<mapWidth; x++){
       float x2 = scaleX(x);
       float y2 = scaleY(y);
       if(x2>-blockSize&&x2<elementWidth&&y2>-blockSize&&y2<elementHeight)
         image(tempImages[map[y][x]-1], x2, y2);
     }
    }
  }
  float scaleX(int x){
    return x*blockSize + mapXOffset + elementWidth/2;
  }
  float scaleY(int y){
    return y*blockSize + mapYOffset + elementHeight/2;
  }
}