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
int SMOOTHING1 = 10;
int SMOOTHING2 = 5;
int COUNTER = 1;
void draw(){
  if (millis() > 2000 && !smoothed){
    save("smoothing3/Smoothing "+SMOOTHING1+" "+SMOOTHING2+": "+COUNTER+"before");
    map = smoothMap(map, width/2, height/2, 5, SMOOTHING1, 2);
    map = smoothMap(map, width/2, height/2, 5, SMOOTHING2, 1);
    smoothed = true;
    drawMap(map, 2, 5, width/2, height/2);
    save("smoothing3/Smoothing "+SMOOTHING2+" "+SMOOTHING2+": "+COUNTER+"after");
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