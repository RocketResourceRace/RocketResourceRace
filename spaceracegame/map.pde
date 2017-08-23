import java.util.Collections;


int[][] smoothMap(int[][] map, int mapWidth, int mapHeight, int numOfGroundTypes){
  ArrayList<int[]> order = new ArrayList<int[]>();
  for (int y=0; y<mapHeight;y++){
    for (int x=0; x<mapWidth;x++){
      order.add(new int[] {x, y});
    }
  }
  Collections.shuffle(order);
  int[][] newMap = new int[mapHeight][mapWidth];
  for (int[] coord: order){
    int[] counts = new int[numOfGroundTypes];
    for (int y1=coord[1]-2;y1<coord[1]+3;y1++){
     for (int x1 = coord[0]-2; x1<coord[0]+3;x1++){
       if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
         counts[map[y1][x1]]+=1;
       }
     }
    }
    int highest = map[coord[1]][coord[0]];
    for (int i=0; i<numOfGroundTypes;i++){
      if (counts[i] > counts[highest]){
        highest = i;
      }
    }
    newMap[coord[1]][coord[0]] = highest;
  }
  return newMap;
}


int[][] generateMap(int mapWidth, int mapHeight, int numOfGroundTypes, int numOfGroundSpawns){
  int[][] map = new int[mapHeight][mapWidth];
  for(int i=0;i<numOfGroundSpawns;i++){
    map[(int)random(mapHeight)][(int)random(mapWidth)] = (int)random(numOfGroundTypes-1)+1;
  }
  ArrayList<int[]> order = new ArrayList<int[]>();
  for (int y=0; y<mapHeight;y++){
    for (int x=0; x<mapWidth;x++){
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
  map = smoothMap(map, mapWidth, mapHeight, numOfGroundTypes);
  map = smoothMap(map, mapWidth, mapHeight, numOfGroundTypes);
  return map;
}


void drawMap(int[][] map, int blockSize, int numOfGroundTypes, int mapWidth, int mapHeight){
  for(int y=0;y<mapHeight;y++){
   for (int x=0; x<mapWidth; x++){
     fill((map[y][x]*255)/numOfGroundTypes);
     rect(x*blockSize, y*blockSize, blockSize, blockSize);
   }
  }
}