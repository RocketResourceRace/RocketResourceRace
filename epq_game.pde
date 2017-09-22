int[][] map;
String activeState;
HashMap<String, State> states;

// Event-driven methods
void mouseClicked(){mouseEvent("mouseClicked", mouseButton);}
void mouseDragged(){mouseEvent("mouseDragged", mouseButton);}
void mouseMoved(){mouseEvent("mouseMoved", mouseButton);}
void mousePressed(){mouseEvent("mousePressed", mouseButton);}
void mouseReleased(){mouseEvent("mouseReleased", mouseButton);}
void mouseWheel(){mouseEvent("mouseWheel", mouseButton);}
void keyPressed(){keyboardEvent("keyPressed", key);}
void keyReleased(){keyboardEvent("keyReleased", key);}
void keyTyped(){keyboardEvent("keyTyped", key);}

void mouseEvent(String eventType, int button){
  getActiveState()._mouseEvent(eventType, button);
}
void keyboardEvent(String eventType, int _key){
  getActiveState()._keyboardEvent(eventType, _key);
}

int NUMOFGROUNDTYPES = 3;
int NUMOFGROUNDSPAWNS = 100;
int WATERLEVEL = 3;
int TILESIZE = 1;
int MAPWIDTH = 500;
int MAPHEIGHT = 500;

int INITIALSMOOTH = 7;
int COMPLETESMOOTH = 5;

PImage[] tileImages;

void setup(){
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  activeState = "menu";
  fullScreen();
  noStroke();
  map = generateMap(MAPWIDTH, MAPHEIGHT, NUMOFGROUNDTYPES, NUMOFGROUNDSPAWNS, WATERLEVEL);
  tileImages = new PImage[]{
    loadImage("res/water.png"),
    loadImage("res/sand.png"),
    loadImage("res/grass.png")
  };
}
boolean smoothed = false;
int SMOOTHING1 = 7;
int SMOOTHING2 = 5;
int COUNTER = 3;
void draw(){
  //TO KEEP
  background(255);
  String newState = getActiveState().update();
  if (!newState.equals("")){
    activeState = newState;
  }
  drawMap(map, TILESIZE, NUMOFGROUNDTYPES, MAPWIDTH,MAPHEIGHT);
  
}

State getActiveState(){
  return states.get(activeState);
}

void addState(String name, State state){
  states.put(name, state);
}