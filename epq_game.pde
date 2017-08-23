
int[][] map;
String activeState;
HashMap<String, State> states;

void setup(){
  activeState = "menu";
   fullScreen();
   noStroke();
   map = generateMap(width, height, 5, 50, 20);
   
}

void draw(){
  drawMap(map, 1, 5, width, height);
  drawElements();
}

void addState(String name, State state){
  states.put(name, state);
}


void drawElements(){
  for(Element el : states.get(activeState).elements){
    
  }
}