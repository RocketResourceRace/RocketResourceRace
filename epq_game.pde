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

void setup(){
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  activeState = "menu";
  fullScreen();
  noStroke();
  map = generateMap(width/2, height/2, 5, 50, 3);
}
boolean smoothed = false;
int SMOOTHING1 = 7;
int SMOOTHING2 = 5;
int COUNTER = 1;
void draw(){
  //TO KEEP
  background(255);
  String newState = getActiveState().update();
  if (!newState.equals("")){
    activeState = newState;
  }
  
  //TEMP
  if (millis() > 2000 && !smoothed){
    save("smoothing3/Smoothing "+SMOOTHING1+" "+SMOOTHING2+": "+COUNTER+"before");
    map = smoothMap(map, width/2, height/2, 5, SMOOTHING1, 2);
    map = smoothMap(map, width/2, height/2, 5, SMOOTHING2, 1);
    smoothed = true;
    drawMap(map, 2, 5, width/2, height/2);
    save("smoothing3/Smoothing "+SMOOTHING1+" "+SMOOTHING2+": "+COUNTER+"after");
  }
  drawMap(map, 2, 5, width/2, height/2);
  
}

State getActiveState(){
  return states.get(activeState);
}

void addState(String name, State state){
  states.put(name, state);
}