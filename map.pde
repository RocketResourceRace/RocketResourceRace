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
  int blockSize;
  int numOfGroundTypes;
  int numOfGroundSpawns;
  int waterLevel;
  int initialSmooth;
  int completeSmooth;

  Map(){
    mapWidth = 500;
    mapHeight = 500;
    blockSize = 1;
    numOfGroundTypes = 3;
    numOfGroundSpawns = 100;
    waterLevel = 3;
    initialSmooth = 7;
    completeSmooth = 5;
    generateMap();
  }
  void mouseEvent(String eventType, int button){}
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
  }
  
  
  
  void draw(int xOffset, int yOffset){
    for(int y=0;y<mapHeight;y++){
     for (int x=0; x<mapWidth; x++){
       //if(map[y][x] == 1){
       //  fill(0, 0, 255);
       //} else {
       //  fill((map[y][x]*255)/numOfGroundTypes);
       //}
       image(tileImages[map[y][x]-1], x*blockSize, y*blockSize, blockSize, blockSize);
       //rect(x*blockSize, y*blockSize, blockSize, blockSize);
     }
    }
  }
}