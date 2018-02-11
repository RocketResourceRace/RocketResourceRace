import java.math.BigDecimal;
import processing.sound.*;

int[][] map;
int mapSize;
String activeState;
HashMap<String, State> states;
int lastClickTime = 0;
final int DOUBLECLICKWAIT = 500;  
float GUIScale = 1.0;
float TextScale = 1.0;
PrintWriter settingsWriteFile; 
BufferedReader settingsReadFile;
StringDict settings;
final String LETTERSNUMBERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890/\\_ ";
HashMap<String, SoundFile> sfx;
float volume;

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
  if (key==ESC){
    key = 0;
  }
  getActiveState()._keyboardEvent(eventType, _key);
}

color brighten(color c, int offset){
  float r = red(c), g = green(c), b = blue(c);
  return color(min(r+offset, 255), min(g+offset, 255), min(b+offset, 255));
}
void setVolume(float x){
  if (0<=x && x<=1){
    volume = x;
    for (SoundFile fect:sfx.values()){
      fect.amp(volume);
    }
    return;
  }
  print("invalid volume");
}

int NUMOFGROUNDTYPES = 3;
int NUMOFGROUNDSPAWNS = 100;
int WATERLEVEL = 3;
int TILESIZE = 1;
int MAPWIDTH = 100;
int MAPHEIGHT = 100;

int INITIALSMOOTH = 7;
int COMPLETESMOOTH = 5;

PImage[] tileImages;

void changeSetting(String id, String newValue){
  settings.set(id, newValue);
}

void writeSettings(){
  settingsWriteFile = createWriter("settings.txt"); 
  for(String s: settings.keyArray()){
    settingsWriteFile.println(s+" "+settings.get(s));
  }
  settingsWriteFile.flush();  // Writes the remaining data to the file
  settingsWriteFile.close();  // Finishes the file
}

void loadSettings(){
  String line;
  String[] args;
  try{
    while ((line = settingsReadFile.readLine()) != null) {
      args = line.split(" ");
      settings.set(args[0], args[1]);
    }
  }
  catch (IOException e) {
    e.printStackTrace();
  }
}
void loadSounds(){
  sfx = new HashMap<String, SoundFile>();
  sfx.put("click3", new SoundFile(this, "click3.wav"));
}


float halfScreenWidth;
float halfScreenHeight;
void setup(){
  settings = new StringDict();
  settingsReadFile = createReader("settings.txt");
  loadSettings();
  loadSounds();
  textFont(createFont("GillSans", 32));
  GUIScale = float(settings.get("gui_scale"));
  TextScale = float(settings.get("text_scale"));
  mapSize = int(settings.get("mapSize"));
  volume = float(settings.get("volume"));
  tileImages = new PImage[]{
    loadImage("data/water.png"),
    loadImage("data/sand.png"),
    loadImage("data/grass.png")
  };
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  addState("map", new TestMap());
  activeState = "menu";
  fullScreen();
  noStroke();
  halfScreenWidth = width/2;
  halfScreenHeight= height/2;
}
boolean smoothed = false;

void draw(){
  background(255);
  String newState = getActiveState().update();
  if (!newState.equals("")){
    for (Panel panel : states.get(newState).panels){
      for (String id : panel.elements.keySet()){
        panel.elements.get(id).mouseEvent("mouseMoved", LEFT);
      }
    }
    states.get(activeState).leaveState();
    states.get(newState).enterState();
    activeState = newState;
  }
}

State getActiveState(){
  return states.get(activeState);
}

void addState(String name, State state){
  states.put(name, state);
}