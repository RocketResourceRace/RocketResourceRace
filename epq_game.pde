import java.math.BigDecimal;
import processing.sound.*;

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
int prevT;
boolean soundOn = true;

// Event-driven methods
void mouseClicked(){mouseEvent("mouseClicked", mouseButton);doubleClick();}
void mouseDragged(){mouseEvent("mouseDragged", mouseButton);}
void mouseMoved(){mouseEvent("mouseMoved", mouseButton);}
void mousePressed(){mouseEvent("mousePressed", mouseButton);}
void mouseReleased(){mouseEvent("mouseReleased", mouseButton);}
void mouseWheel(MouseEvent event){mouseEvent("mouseWheel", mouseButton, event);}
void keyPressed(){keyboardEvent("keyPressed", key);}
void keyReleased(){keyboardEvent("keyReleased", key);}
void keyTyped(){keyboardEvent("keyTyped", key);}

float between(float lower, float v, float upper){
  return max(min(upper, v), lower);
}

color brighten(color old, int off){
  return color(between(0, red(old)+off, 255), between(0, green(old)+off, 255), between(0, blue(old)+off, 255));
}

void doubleClick(){
  if (millis() - lastClickTime < DOUBLECLICKWAIT){
    mouseEvent("mouseDoubleClicked", mouseButton);
    lastClickTime = 0;
  }
  else{
    lastClickTime = millis();
  }
}

float sigmoid(float x){
  return 1-2/(exp(x)+1);
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

int NUMOFGROUNDTYPES = 4;
int NUMOFBUILDINGTYPES = 7;
int groundSpawns = 100;
int waterLevel = 3;
int TILESIZE = 1;
int MAPWIDTH = 100;
int MAPHEIGHT = 100;

int initialSmooth = 7;
int completeSmooth = 5;

color[] playerColours = new color[]{color(0, 0, 255), color(255, 0, 0)};

PImage[] tileImages;
PImage[] buildingImages;
PImage[] partyImages;
HashMap<Integer, PImage> lowImages;

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
  soundOn = Integer.parseInt(settings.get("sound"))==1;
  if(soundOn){
    sfx = new HashMap<String, SoundFile>();
    sfx.put("click3", new SoundFile(this, "click3.wav"));
  }
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
    loadImage("data/grass.png"),
    loadImage("data/forest.png"),
  };
  lowImages = new HashMap<Integer, PImage>();
  lowImages.put(3, loadImage("data/forest_low.png"));
  lowImages.put(0, loadImage("data/water_low.png"));
  buildingImages = new PImage[]{
    loadImage("data/construction.png"),
    loadImage("data/house.png"),
    loadImage("data/farm.png"),
    loadImage("data/mine.png"),
    loadImage("data/smelter.png"),
    loadImage("data/factory.png"),
    loadImage("data/sawmill.png"),
    loadImage("data/big_factory.png")
  };
  partyImages = new PImage[]{
    loadImage("data/blue_flag.png"),
    loadImage("data/red_flag.png"),
    loadImage("data/battle.png")
  };
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  addState("map", new Game());
  activeState = "menu";
  fullScreen();
  noStroke();
  halfScreenWidth = width/2;
  halfScreenHeight= height/2;
}
boolean smoothed = false;

void draw(){
  background(255);
  prevT = millis();
  
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