package states;

import event.Event;
import json.JSONManager;
import processing.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import processing.sound.SoundFile;
import state.Element;
import state.State;
import state.elements.*;

import static json.JSONManager.gameData;
import static processing.core.PApplet.*;
import static processing.core.PConstants.RECT;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Menu extends State {
    PImage BGimg;
    PShape bg;
    String currentPanel, newPanel;
    HashMap<String, String[]> stateChangers, settingChangers;

    public Menu() {
        super();
        LOGGER_MAIN.fine("Initialising menu");
        BGimg = loadImage(RESOURCES_ROOT+"img/ui/menu_background.jpeg");
        bg = papplet.createShape(RECT, 0, 0, papplet.width, papplet.height);
        bg.setTexture(BGimg);

        currentPanel = "startup";
        loadMenuPanels();
        newPanel = currentPanel;
        activePanel = currentPanel;
    }

    public void loadMenuPanels() {
        LOGGER_MAIN.fine("Loading menu panels");
        resetPanels();
        JSONManager.loadMenuElements(this, JSONManager.loadFloatSetting("gui scale"));
        hidePanels();
        getPanel(currentPanel).setVisible(true);
        stateChangers = JSONManager.getChangeStateButtons();
        settingChangers = JSONManager.getChangeSettingButtons();

        addElement("loading manager", new BaseFileManager(papplet.width/4, papplet.height/4, papplet.width/2, papplet.height/3, "saves"), "load game");
    }

    public int currentColour() {
        float c = abs(((float)(hour()-12)+(float)minute()/60)/24);
        int day = papplet.color(255, 255, 255, 50);
        int night = papplet.color(0, 0, 50, 255);
        return papplet.lerpColor(day, night, c*2);
    }

    public String update() {
        papplet.shape(bg);
        //if(((ToggleButton)getElement("background dimming", "settings")).getState()){
        //  pushStyle();
        //  fill(currentColour());
        //  rect(0, 0, width, height);
        //  popStyle();
        //}
        if (!currentPanel.equals(newPanel)) {
            changeMenuPanel();
        }
        drawPanels();

        drawMenuTitle();
        return getNewState();
    }

    public void drawMenuTitle() {
        // Draw menu states title
        if (JSONManager.menuStateTitle(currentPanel) != null) {
            papplet.fill(0);
            papplet.textFont(getFont(JSONManager.loadFloatSetting("text scale")*30));
            papplet.textAlign(CENTER, TOP);
            papplet.text(JSONManager.menuStateTitle(currentPanel), papplet.width/2, 100);
        }
    }

    public void changeMenuPanel() {
        LOGGER_MAIN.fine("Changing menu panel to: "+newPanel);
        panelToTop(newPanel);
        getPanel(newPanel).setVisible(true);
        getPanel(currentPanel).setVisible(false);
        currentPanel = newPanel;
        for (Element elem : getPanel(newPanel).elements) {
            elem.mouseEvent("mouseMoved", LEFT);
        }
        activePanel = newPanel;
    }

    public void enterState() {
        loadMenuPanels(); // Refresh menu
        newPanel = "startup";
    }

    public void saveMenuSetting(String id, Event event) {
        if (settingChangers.get(id) != null) {
            LOGGER_MAIN.finer(String.format("Saving setting id:%s, event id:%s", id, event.id));
            String type = JSONManager.getElementType(event.panel, id);
            switch (type) {
                case "slider":
                    JSONManager.saveSetting(settingChangers.get(id)[0], ((Slider)getElement(id, event.panel)).getValue());
                    break;
                case "toggle button":
                    JSONManager.saveSetting(settingChangers.get(id)[0], ((ToggleButton)getElement(id, event.panel)).getState());
                    break;
                case "tickbox":
                    JSONManager.saveSetting(settingChangers.get(id)[0], ((Tickbox)getElement(id, event.panel)).getState());
                    break;
                case "dropdown":
                    switch (((DropDown)getElement(id, event.panel)).optionTypes) {
                        case "floats":
                            JSONManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getFloatVal());
                            break;
                        case "strings":
                            JSONManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getStrVal());
                            break;
                        case "ints":
                            JSONManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getIntVal());
                            break;
                        default:
                            LOGGER_MAIN.warning("invalid dropdown type: " + ((DropDown)getElement(id, event.panel)).optionTypes);
                            break;
                    }
                    break;
                default:
                    LOGGER_MAIN.warning("Invalid element type: "+type);
                    break;
            }
        }
    }

    public void revertChanges(String panel, boolean onlyAutosaving) {
        LOGGER_MAIN.fine("Reverting changes made to settings that are not autosaving");
        for (Element elem : getPanel(panel).elements) {
            if (elem.id.equals("loading manager") && ((onlyAutosaving || !JSONManager.hasFlag(panel, elem.id, "autosave")) && settingChangers.get(elem.id) != null)) {
                String type = JSONManager.getElementType(panel, elem.id);
                switch (type) {
                    case "slider":
                        ((Slider)getElement(elem.id, panel)).setValue(JSONManager.loadFloatSetting(JSONManager.getSettingName(elem.id, panel)));
                        break;
                    case "toggle button":
                        ((ToggleButton)getElement(elem.id, panel)).setState(JSONManager.loadBooleanSetting(JSONManager.getSettingName(elem.id, panel)));
                        break;
                    case "tickbox":
                        ((Tickbox)getElement(elem.id, panel)).setState(JSONManager.loadBooleanSetting(JSONManager.getSettingName(elem.id, panel)));
                        break;
                    case "dropdown":
                        switch (((DropDown)getElement(elem.id, panel)).optionTypes) {
                            case "floats":
                                ((DropDown)getElement(elem.id, panel)).setSelected(""+JSONManager.loadFloatSetting(JSONManager.getSettingName(elem.id, panel)));
                                break;
                            case "strings":
                                ((DropDown)getElement(elem.id, panel)).setSelected(""+JSONManager.loadStringSetting(JSONManager.getSettingName(elem.id, panel)));
                                break;
                            case "ints":
                                ((DropDown)getElement(elem.id, panel)).setSelected(""+JSONManager.loadIntSetting(JSONManager.getSettingName(elem.id, panel)));
                                break;
                            default:
                                LOGGER_MAIN.warning("Invalid dropdown type: "+((DropDown)getElement(elem.id, panel)).optionTypes);
                                break;
                        }
                        break;
                    default:
                        LOGGER_MAIN.warning("Invalid type for element:"+type);
                        break;
                }
            }
        }
    }




    public void elementEvent(ArrayList<Event> events) {
        for (Event event : events) {
            if (event.type.equals("valueChanged") && settingChangers.get(event.id) != null && event.panel != null) {
                if (JSONManager.hasFlag(event.panel, event.id, "autosave")) {
                    saveMenuSetting(event.id, event);
                    JSONManager.writeSettings();
                    if (event.id.equals("framerate cap")) {
                        setFrameRateCap();
                    }
                }
                if (event.id.equals("sound on")) {
                    loadSounds();
                }
                if (event.id.equals("volume")) {
                    setVolume();
                }
            }
            if (event.type.equals("clicked")) {
                if (stateChangers.get(event.id) != null && stateChangers.get(event.id)[0] != null ) {
                    newPanel = stateChangers.get(event.id)[0];
                    revertChanges(event.panel, false);
                    if (newPanel.equals("load game")) {
                        ((BaseFileManager)getElement("loading manager", "load game")).loadSaveNames();
                    }
                } else if (event.id.equals("apply")) {
                    for (Element elem : getPanel(event.panel).elements) {
                        if (!JSONManager.hasFlag(event.panel, elem.id, "autosave")) {
                            saveMenuSetting(elem.id, event);
                        }
                    }
                    JSONManager.writeSettings();
                    loadMenuPanels();
                } else if (event.id.equals("revert")) {
                    revertChanges(event.panel, false);
                } else if (event.id.equals("reset default map settings")) {
                    LOGGER_MAIN.info("Resetting default setting for new map");
                    JSONManager.saveDefault("hills height");
                    JSONManager.saveDefault("water level");
                    JSONManager.saveDefault("map size");
                    JSONManager.saveDefault("starting food");
                    JSONManager.saveDefault("starting wood");
                    JSONManager.saveDefault("starting stone");
                    JSONManager.saveDefault("starting metal");
                    for (Integer i=1; i<gameData.getJSONArray("terrain").size()+1; i++) {
                        if (!gameData.getJSONArray("terrain").getJSONObject(i-1).isNull("weighting")) {
                            JSONManager.saveDefault(gameData.getJSONArray("terrain").getJSONObject(i-1).getString("id")+" weighting");
                        }
                    }
                    revertChanges(event.panel, true);
                } else if (event.id.equals("start")) {
                    LOGGER_MAIN.info("Starting states change to a new game");
                    newState = "map";
                    loadingName = null;
                } else if (event.id.equals("load")) {
                    loadingName = ((BaseFileManager)getElement("loading manager", "load game")).selectedSaveName();
                    LOGGER_MAIN.info("Starting states change to game via loading with file name"+loadingName);
                    newState = "map";
                } else if (event.id.equals("exit")) {
                    quitGame();
                }
            }
        }
    }
}