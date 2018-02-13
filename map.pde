import java.util.Collections;


class TestMap extends State{
  TestMap(){
    addElement("map", new Map(100, 100, 1000, 700));
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (_key == ESC){
      newState = "menu";
    }
    return new ArrayList<String>();
  }
  void enterState(){
    Map m = (Map)(getElement("map", "default"));
    m.generateMap();
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
  float mapMaxSpeed;
  PImage buffer;
  int elementWidth;
  int elementHeight;
  float[] mapVelocity = {0,0};
  int startX;
  int startY;
  int frameStartTime;
  int xPos, yPos;
  boolean zoomChanged;
  boolean mapFocused;

  Map(int x, int y, int w, int h){
    xPos = x;
    yPos = y;
    elementWidth = w;
    elementHeight = h;
    numOfGroundTypes = 3;
    numOfGroundSpawns = 100;
    waterLevel = 3;
    initialSmooth = 7;
    completeSmooth = 5;
    mapXOffset = 0;
    mapYOffset = 0;
    mapMaxSpeed = 5;
    frameStartTime = 0;
  } //<>//
  void limitCoords(){
    mapXOffset = min(max(mapXOffset, -mapWidth*blockSize+elementWidth), 0);
    mapYOffset = min(max(mapYOffset, -mapHeight*blockSize+elementHeight), 0); 
  }
  
  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){
    if (eventType == "mouseWheel"){
      float count = event.getCount();
      if(mouseX > xPos && mouseX < xPos+elementWidth && mouseY > yPos && mouseY < yPos+elementHeight){
        if (count > 0){
          float zoom = pow(0.9, count);
          float newBlockSize = min(blockSize*zoom, (float)elementWidth/10);
          if (blockSize != newBlockSize){
            mapXOffset = scaleX(round((mouseX-mapXOffset-xPos)/blockSize))-xPos-round((mouseX-mapXOffset-xPos)*newBlockSize/blockSize);
            mapYOffset = scaleY(round((mouseY-mapYOffset-yPos)/blockSize))-yPos-round((mouseY-mapYOffset-yPos)*newBlockSize/blockSize);
            blockSize = newBlockSize;
            limitCoords();
          }
        }
        if (count < 0){
          float zoom = pow(0.9, count);
          float newBlockSize = max(blockSize*zoom, (float)elementWidth/(float)mapSize);
          if (blockSize != newBlockSize){
            mapXOffset = scaleX(round((mouseX-mapXOffset-xPos)/blockSize))-xPos-round((mouseX-mapXOffset-xPos)*newBlockSize/blockSize);
            mapYOffset = scaleY(round((mouseY-mapYOffset-yPos)/blockSize))-yPos-round((mouseY-mapYOffset-yPos)*newBlockSize/blockSize);
            blockSize =newBlockSize;
            limitCoords();
          }
        }
      }
    }
    return new ArrayList<String>();
  }
    
  ArrayList<String> mouseEvent(String eventType, int button){
      if (eventType=="mouseDragged" && mapFocused){
        mapXOffset += (mouseX-startX);
        mapYOffset += (mouseY-startY);
        limitCoords();
        startX = mouseX;
        startY = mouseY;
      }
      if (eventType == "mousePressed" && mouseX > xPos && mouseX < xPos+elementWidth && mouseY > yPos && mouseY < yPos+elementHeight){
        startX = mouseX;
        startY = mouseY;
        mapFocused = true;
      } else if(eventType == "mousePressed"){
        mapFocused = false;
      }
    return new ArrayList<String>();
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (eventType == "keyPressed"){
      if (_key == 'a'&&mapVelocity[0]>-mapMaxSpeed){
        mapVelocity[0] -= mapMaxSpeed;
      }
      if (_key == 's'&&mapVelocity[1]<mapMaxSpeed){
        mapVelocity[1] += mapMaxSpeed;
      }
      if (_key == 'd'&&mapVelocity[0]<mapMaxSpeed){
        mapVelocity[0] += mapMaxSpeed;
      }
      if (_key == 'w'&&mapVelocity[1]>-mapMaxSpeed){
        mapVelocity[1] -= mapMaxSpeed;
      }
    }
    if (eventType == "keyReleased"){
      if (_key == 'a'&&mapVelocity[0]<0){
        mapVelocity[0] += mapMaxSpeed;
      }
      if (_key == 's'&&mapVelocity[1]>0){
        mapVelocity[1] -= mapMaxSpeed;
      }
      if (_key == 'd'&&mapVelocity[0]>0){
        mapVelocity[0] -= mapMaxSpeed;
      }
      if (_key == 'w'&&mapVelocity[1]<0){
        mapVelocity[1] += mapMaxSpeed;
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
    
    mapWidth = mapSize;
    mapHeight = mapSize;
    blockSize = elementWidth/(float)mapSize;
    
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
    int frameTime = millis()-frameStartTime;
    mapXOffset -= mapVelocity[0]*frameTime*60/1000;
    mapYOffset -= mapVelocity[1]*frameTime*60/1000;
    frameStartTime = millis();
    limitCoords();
    for (int i=0; i<3; i++){
      tempImages[i] = tileImages[i].copy();
      tempImages[i].resize(ceil(blockSize), 0);
    }
    int lx = max(0, -ceil((mapXOffset)/blockSize)+1);
    int ly = max(0, -ceil((mapYOffset)/blockSize)+1);
    int hx = min(floor((elementWidth-mapXOffset)/blockSize), mapWidth-1);
    int hy = min(floor((elementHeight-mapYOffset)/blockSize), mapHeight-1);
    for(int y=ly;y<hy;y++){
      for (int x=lx; x<hx; x++){
       float x2 = round(scaleX(x));
       float y2 = round(scaleY(y));
       image(tempImages[map[y][x]-1], x2, y2);
       }
     }
     int x3 = round(scaleX(hx));
     int y3 = round(scaleY(hy));
     int topW = round(scaleY(ly)-yPos);
     int bottomW = round(yPos+elementHeight-scaleY(hy));
     int leftW = round(scaleX(lx)-xPos);
     int rightW = round(xPos+elementWidth-scaleX(hx));
     for(int y=ly;y<hy;y++){
       image(tempImages[map[y][hx]-1].get(0, 0, rightW, ceil(blockSize)), x3,round(scaleY(y)));
       image(tempImages[map[y][lx-1]-1].get(ceil(blockSize)-leftW, 0, leftW, ceil(blockSize)), xPos, round(scaleY(y)));
     }
     for(int x=lx;x<hx;x++){
       image(tempImages[map[hy][x]-1].get(0, 0, ceil(blockSize), bottomW), round(scaleX(x)), y3);
       image(tempImages[map[ly-1][x]-1].get(0, ceil(blockSize)-topW, ceil(blockSize), topW), round(scaleX(x)), yPos);
     }
     image(tempImages[map[ly-1][lx-1]-1].get(ceil(blockSize)-leftW, 0, leftW, topW), xPos, yPos);
     image(tempImages[map[hy][hx]-1].get(0, 0, rightW, bottomW), x3, y3);
     image(tempImages[map[hy][lx-1]-1].get(0, 0, leftW, bottomW), xPos, y3);
     image(tempImages[map[ly-1][hx]-1].get(0, ceil(blockSize)-topW, rightW, topW), x3, yPos);
     stroke(0);
     fill(255, 0);
     rect(xPos, yPos, elementWidth, elementHeight);
  }
  float scaleX(float x){
    return x*blockSize + mapXOffset + xPos;
  }
  float scaleY(float y){
    return y*blockSize + mapYOffset + yPos;
  }
}