
class Game extends State{
  final int numOfGroundTypes = 3;
  final int numOfGroundSpawns = 100;
  final int mapElementWidth = 800;
  final int mapElementHeight = 600;
  final int buttonW = 120;
  final int buttonH = 50;
  final int bezle = 20;
  int mapHeight = mapSize;
  int mapWidth = mapSize;
  int waterLevel = 3;
  int initialSmooth = 7;
  int completeSmooth = 5;
  int[][] terrain;
  Party[][] parties;
  Building[][] buildings;
  int turn;
  boolean changeTurn = false;
  Map map;
  Player[] players;
  
  Game(){
    terrain = generateMap();
    parties = new Party[mapHeight][mapWidth];
    buildings = new Building[mapHeight][mapWidth];
    parties[99][99] = new Party(0, 300, 'r');
    addElement("map", new Map(bezle, bezle, mapElementWidth, mapElementHeight, terrain, parties, buildings, mapWidth, mapHeight));
    //int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text
    addElement("end turn", new Button(bezle, height-buttonH-bezle, buttonW, buttonH, color(150), color(50), color(0), 10, CENTER, "Next Turn"));
    map = (Map)getElement("map", "default");
    players = new Player[2];
    // Initial positions will be focused on starting party
    players[0] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize);
    players[1] = new Player(map.mapXOffset, map.mapYOffset, map.blockSize);
  }
  String update(){
    if (changeTurn){  
      players[turn].saveMapSettings(map.mapXOffset, map.mapYOffset, map.blockSize);
      turn = (turn + 1)%2;
      players[turn].loadMapSettings(map);
      changeTurn = false;
    }
    drawBar();
    drawPanels();
    return getNewState();
  }
  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type == "clicked"){
        if (event.id == "end turn"){
          changeTurn = true;
        }
      }
    }
  }
  void drawBar(){
    float barX=buttonW+bezle*2;
    fill(200);
    stroke(170);
    rect(0, height-bezle*2-buttonH, width, buttonH+bezle*2);
    fill(150);
    textSize(10*TextScale);
    String turnString="";
    if (this.turn==0){
      fill(255, 0, 0);
      turnString = "Red Player's Turn";
    }
    else if (this.turn==1){
      fill(0, 0, 255);
      turnString = "Blue Player's Turn";
    }
    stroke(50);
    rect(barX, height-bezle-buttonH, textWidth(turnString)+10, buttonH);
    barX += textWidth(turnString)+10+bezle;
    fill(255);
    textAlign(CENTER, TOP);
    text(turnString, bezle*2+buttonW+(textWidth(turnString)+10)/2, height-bezle-(textDescent()+textAscent())/2-buttonH/2);
  }
  ArrayList<String> keyboardEvent(String eventType, char _key){
    if (_key == ESC){
      newState = "menu";
    }
    return new ArrayList<String>();
  }
  void enterState(){
    terrain = generateMap();
    ((Map)(getElement("map", "default"))).setTerrain(terrain);
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
      int[] counts = new int[numOfGroundTypes+1];
      for (int y1=coord[1]-distance+1;y1<coord[1]+distance;y1++){
       for (int x1 = coord[0]-distance+1; x1<coord[0]+distance;x1++){
         if (y1<mapHeight&&y1>=0&&x1<mapWidth&&x1>=0){
           counts[terrain[y1][x1]]+=1;
         }
       }
      }
      int highest = terrain[coord[1]][coord[0]];
      for (int i=firstType; i<=numOfGroundTypes;i++){
        if (counts[i] > counts[highest]){
          highest = i;
        }
      }
      newMap[coord[1]][coord[0]] = highest;
    }
    return newMap;
  }
  
  
  int[][] generateMap(){
    
    int [][] terrain = new int[mapHeight][mapWidth];
    
    for(int y=0; y<mapHeight; y++){
      terrain[y][0] = 1;
      terrain[y][mapWidth-1] = 1;
    }
    for(int x=1; x<mapWidth-1; x++){
      terrain[0][x] = 1;
      terrain[mapHeight-1][x] = 1;
    }
    for(int i=0;i<numOfGroundSpawns;i++){
      int type = (int)random(numOfGroundTypes)+1;
      int x = (int)random(mapWidth-2)+1;
      int y = (int)random(mapHeight-2)+1;
      terrain[y][x] = type;
      // Water will be type 1
      if (type==1){
        for (int y1=y-waterLevel+1;y1<y+waterLevel;y1++){
         for (int x1 = x-waterLevel+1; x1<x+waterLevel;x1++){
           if (y1 < mapHeight && y1 >= 0 && x1 < mapWidth && x1 >= 0)
             terrain[y1][x1] = type;
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
      while (terrain[y][x] == 0){
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
    return terrain;
  }
}