int[][] map;
String activeState;
HashMap<String, State> states;
int lastClickTime = 0;
final int DOUBLECLICKWAIT = 300;

// Event-driven methods
void mouseClicked(){mouseEvent("mouseClicked", mouseButton);doubleClick();}
void mouseDragged(){mouseEvent("mouseDragged", mouseButton);}
void mouseMoved(){mouseEvent("mouseMoved", mouseButton);}
void mousePressed(){mouseEvent("mousePressed", mouseButton);}
void mouseReleased(){mouseEvent("mouseReleased", mouseButton);}
void mouseWheel(MouseEvent event){mouseEvent("mouseWheel", mouseButton);}
void keyPressed(){keyboardEvent("keyPressed", key);}
void keyReleased(){keyboardEvent("keyReleased", key);}
void keyTyped(){keyboardEvent("keyTyped", key);}

void doubleClick(){
  if (millis() - lastClickTime < DOUBLECLICKWAIT){
    mouseEvent("mouseDoubleClicked", mouseButton);
    lastClickTime = 0;
  }
  else{
    lastClickTime = millis();
  }
}

void mouseEvent(String eventType, int button){
  getActiveState()._mouseEvent(eventType, button);
}
void mouseEvent(String eventType, int button, MouseEvent event){
  getActiveState()._mouseEvent(eventType, button, event);
}
void keyboardEvent(String eventType, char _key){
  getActiveState()._keyboardEvent(eventType, _key);
}

color brighten(color c, int offset){
  float r = red(c), g = green(c), b = blue(c);
  return color(min(r+offset, 255), min(g+offset, 255), min(b+offset, 255));
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
  //drawMap(map, TILESIZE, NUMOFGROUNDTYPES, MAPWIDTH,MAPHEIGHT);
  
}

State getActiveState(){
  return states.get(activeState);
}

void addState(String name, State state){
  states.put(name, state);
}