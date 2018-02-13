import java.util.Collections;


class Map extends Element{
  int[][] terrain;
  int mapWidth;
  int mapHeight;
  float blockSize;
  float mapXOffset;
  float mapYOffset;
  float mapMaxSpeed;
  int elementWidth;
  int elementHeight;
  float[] mapVelocity = {0,0};
  int startX;
  int startY;
  int frameStartTime;
  int xPos, yPos;
  boolean zoomChanged;
  boolean mapFocused;

  Map(int x, int y, int w, int h, int[][] terrain, int mapWidth, int mapHeight){
    xPos = x;
    yPos = y;
    elementWidth = w;
    elementHeight = h;
    mapXOffset = 0;
    mapYOffset = 0;
    mapMaxSpeed = 5;
    frameStartTime = 0;
    this.terrain = terrain;
    this.mapWidth = mapWidth;
    this.mapHeight = mapHeight;
    blockSize = w/(float)mapWidth;
  }
  void limitCoords(){
    mapXOffset = min(max(mapXOffset, -mapWidth*blockSize+elementWidth), 0);
    mapYOffset = min(max(mapYOffset, -mapHeight*blockSize+elementHeight), 0); 
  }
  
  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){
    if (eventType == "mouseWheel"){
      float count = event.getCount();
      if(mouseX > xPos && mouseX < xPos+elementWidth && mouseY > yPos && mouseY < yPos+elementHeight){
        float zoom = pow(0.9, count);
        float newBlockSize = max(min(blockSize*zoom, (float)elementWidth/10), (float)elementWidth/(float)mapSize);
        if (blockSize != newBlockSize){
          mapXOffset = scaleX(round((mouseX-mapXOffset-xPos)/blockSize))-xPos-round((mouseX-mapXOffset-xPos)*newBlockSize/blockSize);
          mapYOffset = scaleY(round((mouseY-mapYOffset-yPos)/blockSize))-yPos-round((mouseY-mapYOffset-yPos)*newBlockSize/blockSize);
          blockSize = newBlockSize;
          limitCoords();
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
  
  
  void draw(){
    PImage[] tempImages = new PImage[3];
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
       image(tempImages[terrain[y][x]-1], x2, y2);
       }
     }
     int x3 = round(scaleX(hx));
     int y3 = round(scaleY(hy));
     int topW = round(scaleY(ly)-yPos);
     int bottomW = round(yPos+elementHeight-scaleY(hy));
     int leftW = round(scaleX(lx)-xPos);
     int rightW = round(xPos+elementWidth-scaleX(hx));
     for(int y=ly;y<hy;y++){
       image(tempImages[terrain[y][hx]-1].get(0, 0, rightW, ceil(blockSize)), x3,round(scaleY(y)));
       image(tempImages[terrain[y][lx-1]-1].get(ceil(blockSize)-leftW, 0, leftW, ceil(blockSize)), xPos, round(scaleY(y)));
     }
     for(int x=lx;x<hx;x++){
       image(tempImages[terrain[hy][x]-1].get(0, 0, ceil(blockSize), bottomW), round(scaleX(x)), y3);
       image(tempImages[terrain[ly-1][x]-1].get(0, ceil(blockSize)-topW, ceil(blockSize), topW), round(scaleX(x)), yPos);
     }
     image(tempImages[terrain[ly-1][lx-1]-1].get(ceil(blockSize)-leftW, 0, leftW, topW), xPos, yPos);
     image(tempImages[terrain[hy][hx]-1].get(0, 0, rightW, bottomW), x3, y3);
     image(tempImages[terrain[hy][lx-1]-1].get(0, 0, leftW, bottomW), xPos, y3);
     image(tempImages[terrain[ly-1][hx]-1].get(0, ceil(blockSize)-topW, rightW, topW), x3, yPos);
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