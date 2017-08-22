int[][] map;

void setup(){
   fullScreen();
   noStroke();
   map = generateMap(width, height, 5, width);
   
}


void draw(){
  drawMap(map, 1, 5, width, height);
  
}