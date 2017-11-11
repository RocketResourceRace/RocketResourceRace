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

PImage[] tileImages;

float halfScreenWidth;
float halfScreenHeight;

void setup(){
  tileImages = new PImage[]{
    loadImage("data/water.png"),
    loadImage("data/sand.png"),
    loadImage("data/grass.png")
  };
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  addState("map", new TestMap());
  activeState = "map";
  fullScreen();
  noStroke();
  //map = generateMap(MAPWIDTH, MAPHEIGHT, NUMOFGROUNDTYPES, NUMOFGROUNDSPAWNS, WATERLEVEL);
  halfScreenWidth = width/2;
  halfScreenHeight= height/2;
}
boolean smoothed = false;

void draw(){
  //TO KEEP
  background(255);
  String newState = getActiveState().update();
  if (!newState.equals("")){
    activeState = newState;
  }
  //drawMap(map, TILESIZE, NUMOFGROUNDTYPES, MAPWIDTH,MAPHEIGHT);
  
}

State getActiveState(){
  return states.get(activeState);
}

void addState(String name, State state){
  states.put(name, state);
}