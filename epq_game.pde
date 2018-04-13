import java.math.BigDecimal;
import processing.sound.*;
import java.util.Arrays;

int mapSize;
String activeState;
HashMap<String, State> states;
int lastClickTime = 0;
final int DOUBLECLICKWAIT = 500;  
float GUIScale = 1.0;
float TextScale = 1.6;
PrintWriter settingsWriteFile; 
BufferedReader settingsReadFile;
StringDict settings;
final String LETTERSNUMBERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890/\\_ ";
HashMap<String, SoundFile> sfx;
float volume = 0.5;
int prevT;
boolean soundOn = true;
JSONObject gameData;
HashMap<Integer, PFont> fonts;
int graphicsRes = 32;

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

String roundDp(String val, int dps){
  return (new BigDecimal(""+val).divide(new BigDecimal("1"), dps, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString();
}

int JSONIndex(JSONArray j, String id){
  for (int i=0; i<j.size(); i++){
    if (j.getJSONObject(i).getString("id").equals(id)){
      return i;
    }
  }
  println("invalid id,", id);
  return -1;
}

JSONObject findJSONObject(JSONArray j, String id){
  for (int i=0; i<j.size(); i++){
    if (j.getJSONObject(i).getString("id").equals(id)){
      return j.getJSONObject(i);
    }
  }
  return null;
}

boolean JSONContainsStr(JSONArray j, String id){
  if (id == null || j == null)
    return false;
  for (int i=0; i<j.size(); i++){
    if (j.getString(i).equals(id)){
      return true;
    }
  }
  return false;
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

void createFile(){
  changeSetting("gui_scale", "1.0");
  changeSetting("text_scale", "1.6");
  changeSetting("volume", "0.5");
  changeSetting("mapSize", "100");
  changeSetting("sound", "1");
  changeSetting("water level", "");
  changeSetting("smoothing", "8");
  changeSetting("ground spawns", "");
  writeSettings();
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

int NUMOFGROUNDTYPES = 5;
int NUMOFBUILDINGTYPES = 9;
int groundSpawns = 100;
int waterLevel = 3;
int TILESIZE = 1;
int MAPWIDTH = 100;
int MAPHEIGHT = 100;

int initialSmooth = 7;
int completeSmooth = 5;

color[] playerColours = new color[]{color(0, 0, 255), color(255, 0, 0)};

HashMap<String, PImage> tileImages;
HashMap<String, PImage[]> buildingImages;
PImage[] partyImages;
HashMap<String, PImage> taskImages;
HashMap<String, PImage> lowImages;

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
    print("Ignore that message");
  }
  catch (Exception e){
    createFile();
  }
}
void loadSounds(){
  soundOn = !(Integer.parseInt(settings.get("sound"))==0);
  if(soundOn){
    sfx = new HashMap<String, SoundFile>();
    sfx.put("click3", new SoundFile(this, "click3.wav"));
  }
}

void loadImages(){
  try{
    tileImages = new HashMap<String, PImage>();
    lowImages = new HashMap<String, PImage>();
    buildingImages = new HashMap<String, PImage[]>();
    taskImages = new HashMap<String, PImage>();
    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++){
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      tileImages.put(tileType.getString("id"), loadImage(tileType.getString("img")));
      if (!tileType.isNull("low img")){
        lowImages.put(tileType.getString("id"), loadImage(tileType.getString("low img")));
      }
    }
    for (int i=0; i<gameData.getJSONArray("buildings").size(); i++){
      JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
      PImage[] p = new PImage[buildingType.getJSONArray("img").size()];
      for (int i2=0; i2< buildingType.getJSONArray("img").size(); i2++)
        p[i2] = loadImage(buildingType.getJSONArray("img").getString(i2));
      buildingImages.put(buildingType.getString("id"), p);
    }
    for (int i=0; i<gameData.getJSONArray("tasks").size(); i++){
      JSONObject task = gameData.getJSONArray("tasks").getJSONObject(i);
      if (!task.isNull("img")){
        taskImages.put(task.getString("id"), loadImage(task.getString("img")));
      }
    }
  }
  catch (Exception e){
    println("Error loading images");
    println(e);
  }
}

PFont getFont(float size){
  PFont f=fonts.get(round(size));
  if (f == null){
    fonts.put(round(size), createFont("GillSans", size));
    return fonts.get(round(size));
  }
  else{
    return f;
  }
}


float sum(float[] l){
  float c=0;
  for (int i=0; i<l.length; i++)
    c += l[i];
  return c;
}


float halfScreenWidth;
float halfScreenHeight;
void setup(){
  frameRate(1000);
  fonts = new HashMap<Integer, PFont>();
  gameData = loadJSONObject("data.json");
  settings = new StringDict();
  //if
  settingsReadFile = createReader("settings.txt");
  loadSettings();
  loadSounds();
  textFont(createFont("GillSans", 32));
  GUIScale = float(settings.get("gui_scale"));
  TextScale = float(settings.get("text_scale"));
  mapSize = int(settings.get("mapSize"));
  volume = float(settings.get("volume"));
  
  loadImages();
  
  partyImages = new PImage[]{
    loadImage("data/blue_flag.png"),
    loadImage("data/red_flag.png"),
    loadImage("data/battle.png")
  };
  states = new HashMap<String, State>();
  addState("menu", new Menu());
  addState("map", new Game());
  activeState = "menu";
  fullScreen(P3D);
  //noSmooth();
  noStroke();
  //hint(DISABLE_OPTIMIZED_STROKE);
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
  textFont(getFont(10));
  textAlign(LEFT, TOP);
  fill(255,0,0);
  text(frameRate, 0, 0);
  
  
}

State getActiveState(){
  return states.get(activeState);
}

void addState(String name, State state){
  states.put(name, state);
}
