String activeState;
HashMap<String, State> states;
int lastClickTime = 0;
final int DOUBLECLICKWAIT = 300;
float GUIScale = 1.0;

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