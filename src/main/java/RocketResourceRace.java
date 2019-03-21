import event.Action;
import map.Building;
import party.Party;
import player.Player;
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import java.io.*;
import java.math.BigDecimal;
import processing.sound.*;

import java.util.logging.*;

import java.nio.ByteBuffer;

import state.Element;
import state.Panel;
import state.State;
import states.Game;
import util.Image;
import util.LoggerFormatter;
import states.Menu;
import json.JSONManager;
import util.Util;

import java.util.HashMap;
import java.util.ArrayList;

import static util.Font.getFont;
import static util.Logging.*;
import static util.Util.*;

public class RocketResourceRace extends PApplet {


    JSONObject gameData = JSONManager.gameData;

    String activeState;
    HashMap<String, State> states;
    int lastClickTime = 0;
    final int DOUBLECLICKWAIT = 500;
    final String LETTERSNUMBERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890/\\_ ";
    int prevT;
    HashMap<Integer, PFont> fonts;
    PShader toon;

    // Event-driven methods
    public void mouseClicked() {
        mouseEvent("mouseClicked", mouseButton);
        doubleClick();
    }
    public void mouseDragged() {
        mouseEvent("mouseDragged", mouseButton);
    }
    public void mouseMoved() {
        mouseEvent("mouseMoved", mouseButton);
    }
    public void mousePressed() {
        mouseEvent("mousePressed", mouseButton);
    }
    public void mouseReleased() {
        mouseEvent("mouseReleased", mouseButton);
    }
    public void mouseWheel(MouseEvent event) {
        mouseEvent("mouseWheel", mouseButton, event);
    }
    public void keyPressed() {
        keyboardEvent("keyPressed", key);
    }
    public void keyReleased() {
        keyboardEvent("keyReleased", key);
    }
    public void keyTyped() {
        keyboardEvent("keyTyped", key);
    }


    /*
    Handles double clicks
    */
    public void doubleClick() {
        if (millis() - lastClickTime < DOUBLECLICKWAIT) {
            mouseEvent("mouseDoubleClicked", mouseButton);
            lastClickTime = 0;
        } else {
            lastClickTime = millis();
        }
    }

    public float sigmoid(float x) {
        return 1-2/(exp(x)+1);
    }

    public void mouseEvent(String eventType, int button) {
        getActiveState()._mouseEvent(eventType, button);
    }
    public void mouseEvent(String eventType, int button, MouseEvent event) {
        getActiveState()._mouseEvent(eventType, button, event);
    }
    public void keyboardEvent(String eventType, char _key) {
        if (key==ESC) {
            key = 0;
        }
        getActiveState()._keyboardEvent(eventType, _key);
    }







    public void settings() {
        System.setProperty("jogl.disable.openglcore", "true");
        fullScreen(P3D);
    }


    float halfScreenWidth;
    float halfScreenHeight;
    public void setup() {
        try {
            Util.papplet = this;
            // Set up loggers
            FileHandler mainHandler = new FileHandler(sketchPath("main_log.log"));
            mainHandler.setFormatter(new LoggerFormatter());
            mainHandler.setLevel(FILELOGLEVEL);
            LOGGER_MAIN.addHandler(mainHandler);
            LOGGER_MAIN.setLevel(FILELOGLEVEL);

            FileHandler gameHandler = new FileHandler(sketchPath("game_log.log"));
            gameHandler.setFormatter(new LoggerFormatter());
            gameHandler.setLevel(FILELOGLEVEL);
            LOGGER_GAME.addHandler(gameHandler);
            LOGGER_GAME.setLevel(FILELOGLEVEL);
            LOGGER_GAME.addHandler(mainHandler);
            LOGGER_GAME.setLevel(FILELOGLEVEL);

            //Logger.getLogger("global").setLevel(Level.WARNING);
            //Logger.getLogger("").setLevel(Level.WARNING);
            //LOGGER_MAIN.setUseParentHandlers(false);

            LOGGER_MAIN.fine("Starting setup");

            new JSONManager();
            gameData = JSONManager.gameData;
            loadSounds();
            setFrameRateCap();
            textFont(createFont("GillSans", 32));

            Image.loadImages();

            LOGGER_MAIN.fine("Loading states");

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
            //toon = loadShader("ToonFrag.glsl", "ToonVert.glsl");
            //toon.set("fraction", 1.0);

            LOGGER_MAIN.fine("Setup finished");
        }

        catch(IOException e) {
            LOGGER_MAIN.log(Level.SEVERE, "IO exception occured duing setup", e);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error occured during setup", e);
            throw e;
        }
    }
    boolean smoothed = false;

    public void draw() {
        try {
            background(0);
            prevT = millis();
            String newState = getActiveState().update();
            if (!newState.equals("")) {
                for (Panel panel : states.get(newState).panels) {
                    for (Element elem : panel.elements) {
                        elem.mouseEvent("mouseMoved", LEFT);
                    }
                }
                states.get(activeState).leaveState();
                states.get(newState).enterState();
                activeState = newState;
            }
            if (JSONManager.loadBooleanSetting("show fps")) {
                textFont(getFont(10));
                textAlign(LEFT, TOP);
                fill(255, 0, 0);
                text(frameRate, 0, 0);
            }
            if (JSONManager.loadBooleanSetting("show fps")) {
                textFont(getFont(10));
                textAlign(LEFT, TOP);
                fill(255, 0, 0);
                text(frameRate, 0, 0);
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Uncaught exception occured during draw", e);
            throw e;
        }
    }

    public State getActiveState() {
        State state = states.get(activeState);
        if (state == null) {
            LOGGER_MAIN.severe("State not found "+activeState);
        }
        return state;
    }

    public void addState(String name, State state) {
        states.put(name, state);
    }


    //boolean isWater(int x, int y) {
    //  //return max(new float[]{
    //  //  noise(x*MAPNOISESCALE, y*MAPNOISESCALE),
    //  //  noise((x+1)*MAPNOISESCALE, y*MAPNOISESCALE),
    //  //  noise(x*MAPNOISESCALE, (y+1)*MAPNOISESCALE),
    //  //  noise((x+1)*MAPNOISESCALE, (y+1)*MAPNOISESCALE),
    //  //  })<JSONManager.loadFloatSetting("water level");
    //  for (float y1 = y; y1<=y+1;y1+=1.0/JSONManager.loadFloatSetting("terrain detail")){
    //    for (float x1 = x; x1<=x+1;x1+=1.0/JSONManager.loadFloatSetting("terrain detail")){
    //      if(noise(x1*MAPHEIGHTNOISESCALE, y1*MAPHEIGHTNOISESCALE)>JSONManager.loadFloatSetting("water level")){
    //        return false;
    //      }
    //    }
    //  }
    //  return true;
    //}







    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "RocketResourceRace" };
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }
}
