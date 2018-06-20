import java.math.BigDecimal;
import processing.sound.*;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.logging.*;


import java.text.SimpleDateFormat;
import java.util.Date;

// Create logger for this pde
final Logger LOGGER_MAIN = Logger.getLogger("RocketResourceRace");

String activeState;
HashMap<String, State> states;
int lastClickTime = 0;
final int DOUBLECLICKWAIT = 500;
final String LETTERSNUMBERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890/\\_ ";
HashMap<String, SoundFile> sfx;
int prevT;
JSONObject gameData;
HashMap<Integer, PFont> fonts;
PShader toon;
String loadingName;

JSONManager jsManager;

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
  LOGGER_MAIN.warning(String.format("Invalid JSON index '%s'", id));
  return -1;
}

JSONObject findJSONObject(JSONArray j, String id){
  for (int i=0; i<j.size(); i++){
    if (j.getJSONObject(i).getString("id").equals(id)){
      return j.getJSONObject(i);
    }
  }
  LOGGER_MAIN.warning(String.format("Invalid JSON Object id %s", id));
  return null;
}

boolean JSONContainsStr(JSONArray j, String id){
  try{
    if (id == null || j == null)
      return false;
    for (int i=0; i<j.size(); i++){
      if (j.getString(i).equals(id)){
        return true;
      }
    }
    return false;
  }
  catch(Exception e){
    LOGGER_MAIN.log(Level.SEVERE, String.format("Error finding string in JSON array, '%s'", id), e);
    return false;
  }
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

void setVolume(){
  try{
    for (SoundFile fect:sfx.values()){
      fect.amp(jsManager.loadFloatSetting("volume"));
    }
  }
  catch (Exception e){
    LOGGER_MAIN.log(Level.SEVERE, "Something wrong with setting volume", e);
  }
}

void setFrameRateCap(){
  if (jsManager.loadBooleanSetting("framerate cap")){
    frameRate(60);
  }
  else{
    frameRate(1000);
  }
}

int NUMOFGROUNDTYPES = 5;
int NUMOFBUILDINGTYPES = 9;
int TILESIZE = 1;
int MAPWIDTH = 100;
int MAPHEIGHT = 100;
float MAPHEIGHTNOISESCALE = 0.08;
float MAPTERRAINNOISESCALE = 0.08;
float HILLSTEEPNESS = 0.1;

color[] playerColours = new color[]{color(0, 0, 255), color(255, 0, 0)};

HashMap<String, PImage> tileImages;
HashMap<String, PImage[]> buildingImages;
PImage[] partyImages;
PImage[] taskImages;
HashMap<String, PImage> lowImages;
HashMap<String, PImage> tile3DImages;

void loadSounds(){
  try{
    if(jsManager.loadBooleanSetting("sound on")){
      sfx = new HashMap<String, SoundFile>();
      sfx.put("click3", new SoundFile(this, "click3.wav"));
    }
  }
  catch (Exception e){
    LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading sounds", e);
  }
}

void loadImages(){
  try{
    tileImages = new HashMap<String, PImage>();
    lowImages = new HashMap<String, PImage>();
    tile3DImages = new HashMap<String, PImage>();
    buildingImages = new HashMap<String, PImage[]>();
    taskImages = new PImage[gameData.getJSONArray("tasks").size()];
    for (int i=0; i<gameData.getJSONArray("terrain").size(); i++){
      JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
      tileImages.put(tileType.getString("id"), loadImage(tileType.getString("img")));
      if (!tileType.isNull("low img")){
        lowImages.put(tileType.getString("id"), loadImage(tileType.getString("low img")));
      }
      if(!tileType.isNull("img3d")){
        tile3DImages.put(tileType.getString("id"), loadImage(tileType.getString("img3d")));
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
        taskImages[i] = loadImage(task.getString("img"));
      }
    }
  }
  
  catch (Exception e){
    LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading images", e);
  }
}

PFont getFont(float size){
  try{
    PFont f=fonts.get(round(size));
    if (f == null){
      fonts.put(round(size), createFont("GillSans", size));
      return fonts.get(round(size));
    }
    else{
      return f;
    }
  }
  catch (Exception e){
    LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading font", e);
    return null;
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

  fullScreen(P3D);
  try{
    FileHandler handler = new FileHandler(sketchPath("log.log"));
    handler.setFormatter(new LoggerFormatter());
    handler.setLevel(Level.FINEST);
    LOGGER_MAIN.addHandler(handler);
    LOGGER_MAIN.setLevel(Level.FINEST);
      
    fonts = new HashMap<Integer, PFont>();
    gameData = loadJSONObject("data.json");
    jsManager = new JSONManager();
    loadSounds();
    setFrameRateCap();
    textFont(createFont("GillSans", 32));

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
    //noSmooth();
    smooth();
    noStroke();
    //hint(DISABLE_OPTIMIZED_STROKE);
    halfScreenWidth = width/2;
    halfScreenHeight= height/2;
    toon = loadShader("ToonFrag.glsl", "ToonVert.glsl");
    toon.set("fraction", 1.0);
  }
  
  catch (Exception e){
    LOGGER_MAIN.log(Level.SEVERE, "Uncaught exception occured during setup", e);
    exit();
  }
}
boolean smoothed = false;

void draw(){
  try{
    background(255);
    prevT = millis();
    String newState = getActiveState().update();
    if (!newState.equals("")){
      for (Panel panel : states.get(newState).panels){
        for (Element elem : panel.elements){
          elem.mouseEvent("mouseMoved", LEFT);
        }
      }
      states.get(activeState).leaveState();
      states.get(newState).enterState();
      activeState = newState;
    }
    if(jsManager.loadBooleanSetting("show fps")){
      textFont(getFont(10));
      textAlign(LEFT, TOP);
      fill(255,0,0);
      text(frameRate, 0, 0);
    }
  }
  
  catch (Exception e){
    LOGGER_MAIN.log(Level.SEVERE, "Uncaught exception occured during draw", e);
    exit();
  }
}

State getActiveState(){
  State state = states.get(activeState);
  if (state == null){
    LOGGER_MAIN.severe("State not found "+activeState);
  }
  return state;
}

void addState(String name, State state){
  states.put(name, state);
}
