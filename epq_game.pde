int[][] map;
String activeState;
HashMap<String, State> states;

void setup(){
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  activeState = "menu";
  
  fullScreen();
  noStroke();
  map = generateMap(width/2, height/2, 5, 50, 3);
}
boolean smoothed = false;
void draw(){
  if (millis() > 2000 && !smoothed){
    map = smoothMap(map, width/2, height/2, 5, 20);
    smoothed = true;
  }
  drawMap(map, 2, 5, width/2, height/2);
  drawElements();
}

void addState(String name, State state){
  states.put(name, state);
}


void drawElements(){
  for(Element el : states.get(activeState).elements){
    el.draw();
  }
}