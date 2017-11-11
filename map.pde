import java.util.Collections;


class TestMap extends State{
  TestMap(){
    addElement("map", new Map());
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
  int mapXOffset;
  int mapYOffset;
  PImage buffer;
  int elementWidth;
  int elementHeight;

  Map(){
    mapWidth = 500;
    mapHeight = 500;
    blockSize = 1;
    numOfGroundTypes = 3;
    numOfGroundSpawns = 100;
    waterLevel = 3;
    initialSmooth = 7;
    completeSmooth = 5;
    elementWidth = 500;
    elementHeight = 500;
    mapXOffset = 250;
    mapYOffset = 250;
    buffer = createImage(elementWidth, elementHeight, RGB);
    generateMap();
  }
  void mouseEvent(String eventType, int button){
    if (eventType == "mouseClicked"){
      if (button == LEFT){
        blockSize *= 1.25;
      }
      if (button == RIGHT){
        blockSize *= 0.8;
      }
      updateBuffer();
    }
  }
  void keyboardEvent(String eventType, int _key){}
  ArrayList<String> _mouseEvent(String eventType, int button){
    mouseEvent(eventType, button);
    return new ArrayList<String>();
  } 
  void _keyboardEvent(String eventType, int _key){
    mouseEvent(eventType, _key);
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
    updateBuffer();
  }
  
  void updateBuffer(){
    buffer = createImage(elementWidth, elementHeight, RGB);
    PImage[] tempImages = new PImage[numOfGroundTypes];
    for (int i=0; i<3; i++){
      tempImages[i] = tileImages[i].copy();
      tempImages[i].resize(min(ceil(blockSize), tileImages[i].width), 0);
    }
    for(int y=0;y<mapHeight;y++){
      for (int x=0; x<mapWidth; x++){
        float x2 = scaleX(x);
        float y2 = scaleY(y);
        buffer.copy(tempImages[map[y][x]-1], 0, 0, tempImages[map[y][x]-1].width, tempImages[map[y][x]-1].height, ceil(x2), ceil(y2), ceil(blockSize), ceil(blockSize));
      }
    }
  }
  
  
  void draw(int xOffset, int yOffset){
    image(buffer, xOffset, yOffset, elementWidth, elementHeight);
  }
  float scaleX(int x){
    return (x-mapXOffset)*blockSize + elementWidth/2;
  }
  float scaleY(int y){
    return (y-mapYOffset)*blockSize + elementHeight/2;
  }
}