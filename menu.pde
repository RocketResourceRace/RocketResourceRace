


class Menu extends State{
  PImage BGimg;
  PShape bg;
  String currentPanel, newPanel;
  HashMap<String, String[]> stateChangers, settingChangers;
  
  Menu(){
    LOGGER_MAIN.fine("Initialising menu");
    BGimg = loadImage("data/menu_background.jpeg");
    bg = createShape(RECT, 0, 0, width, height);
    bg.setTexture(BGimg);
    
    currentPanel = "startup";
    loadMenuPanels();
    newPanel = currentPanel;
    activePanel = currentPanel;
    
  }
  
  void loadMenuPanels(){
    LOGGER_MAIN.fine("Loading menu panels");
    resetPanels();
    jsManager.loadMenuElements(this, jsManager.loadFloatSetting("gui scale"));
    hidePanels();
    getPanel(currentPanel).setVisible(true);
    stateChangers = jsManager.getChangeStateButtons();
    settingChangers = jsManager.getChangeSettingButtons();
    
    addElement("loading manager", new BaseFileManager(width/4, height/4, width/2, height/2, "saves"), "load game");
  }
  
  color currentColour(){
    float c = abs(((float)(hour()-12)+(float)minute()/60)/24);
    color day = color(255, 255, 255, 50);
    color night = color(0, 0, 50, 255);
    return lerpColor(day, night, c*2);
  }
  
  String update(){
    shape(bg);
    //if(((ToggleButton)getElement("background dimming", "settings")).getState()){
    //  pushStyle();
    //  fill(currentColour());
    //  rect(0, 0, width, height);
    //  popStyle();
    //}
    if (!currentPanel.equals(newPanel)){
      changeMenuPanel();
    }
    drawPanels();
    
    drawMenuTitle();
    return getNewState();
  }
  
  void drawMenuTitle(){
    // Draw menu state title
    if (jsManager.menuStateTitle(currentPanel) != null){
      fill(0);
      textFont(getFont(jsManager.loadFloatSetting("text scale")*30));
      textAlign(CENTER, TOP);
      text(jsManager.menuStateTitle(currentPanel), width/2, 100);
    }
  }
  
  void changeMenuPanel(){
    LOGGER_MAIN.fine("Changing menu panel to: "+newPanel);
    panelToTop(newPanel);
    getPanel(newPanel).setVisible(true);
    getPanel(currentPanel).setVisible(false);
    currentPanel = new String(newPanel);
    for (Element elem : getPanel(newPanel).elements){
      elem.mouseEvent("mouseMoved", LEFT);
    }
    activePanel = newPanel;
  }
  
  void enterState(){
    loadMenuPanels(); // Refresh menu
    newPanel = "startup";
  }
  
  void saveMenuSetting(String id, Event event){
    if (settingChangers.get(id) != null){
      LOGGER_MAIN.finer(String.format("Saving setting id:%s, event id:%s", id, event.id));
      String type = jsManager.getElementType(event.panel, id);
      switch (type){
        case "slider":
          jsManager.saveSetting(settingChangers.get(id)[0], ((Slider)getElement(id, event.panel)).getValue());
          break;
        case "toggle button":
          jsManager.saveSetting(settingChangers.get(id)[0], ((ToggleButton)getElement(id, event.panel)).getState());
          break;
        case "tickbox":
          jsManager.saveSetting(settingChangers.get(id)[0], ((Tickbox)getElement(id, event.panel)).getState());
          break;
        case "dropdown":
          switch (((DropDown)getElement(id, event.panel)).optionTypes){
            case "floats":
              jsManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getFloatVal());
              break;
            case "strings":
              jsManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getStrVal());
              break;
            case "ints":
              jsManager.saveSetting(settingChangers.get(id)[0], ((DropDown)getElement(id, event.panel)).getIntVal());
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
  
  void revertChanges(String panel, boolean onlyAutosaving){
    LOGGER_MAIN.fine("Reverting changes made to settings that are not autosaving");
    for (Element elem : getPanel(panel).elements){
      if (elem.id.equals("loading manager") && ((onlyAutosaving || !jsManager.hasFlag(panel, elem.id, "autosave")) && settingChangers.get(elem.id) != null)){
        String type = jsManager.getElementType(panel, elem.id);
        switch (type){
          case "slider":
            ((Slider)getElement(elem.id, panel)).setValue(jsManager.loadFloatSetting(jsManager.getSettingName(elem.id, panel)));
            break;
          case "toggle button":
            ((ToggleButton)getElement(elem.id, panel)).setState(jsManager.loadBooleanSetting(jsManager.getSettingName(elem.id, panel)));
            break;
          case "tickbox":
            ((Tickbox)getElement(elem.id, panel)).setState(jsManager.loadBooleanSetting(jsManager.getSettingName(elem.id, panel)));
            break;
          case "dropdown":
            switch (((DropDown)getElement(elem.id, panel)).optionTypes){
              case "floats":
                ((DropDown)getElement(elem.id, panel)).setSelected(""+jsManager.loadFloatSetting(jsManager.getSettingName(elem.id, panel)));
                break;
              case "strings":
                ((DropDown)getElement(elem.id, panel)).setSelected(""+jsManager.loadStringSetting(jsManager.getSettingName(elem.id, panel)));
                break;
              case "ints":
                ((DropDown)getElement(elem.id, panel)).setSelected(""+jsManager.loadIntSetting(jsManager.getSettingName(elem.id, panel)));
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
  
  void elementEvent(ArrayList<Event> events){
    for (Event event:events){
      if (event.type.equals("valueChanged") && settingChangers.get(event.id) != null && event.panel != null){
        if (jsManager.hasFlag(event.panel, event.id, "autosave")){
          saveMenuSetting(event.id, event);
          jsManager.writeSettings();
          if (event.id.equals("framerate cap")){
            setFrameRateCap();
          }
        }
        if (event.id.equals("sound on")){
          loadSounds();
        }
        if (event.id.equals("volume")){
          setVolume();
        }
      }
      if (event.type.equals("clicked")){
        if (stateChangers.get(event.id) != null && stateChangers.get(event.id)[0] != null ){
          newPanel = stateChangers.get(event.id)[0];
          revertChanges(event.panel, false);
          if (newPanel.equals("load game")){
            ((BaseFileManager)getElement("loading manager", "load game")).loadSaveNames();
          }
        }
        else if (event.id.equals("apply")){
          for (Element elem : getPanel(event.panel).elements){
            if (!jsManager.hasFlag(event.panel, elem.id, "autosave")){
              saveMenuSetting(elem.id, event);
            }
          }
          jsManager.writeSettings();
          loadMenuPanels();
        }
        else if (event.id.equals("revert")){
          revertChanges(event.panel, false);
        }
        else if (event.id.equals("reset default map settings")){
          LOGGER_MAIN.info("Resetting default setting for new map");
          jsManager.saveDefault("hills height");
          jsManager.saveDefault("water level");
          jsManager.saveDefault("map size");
          jsManager.saveDefault("starting food");
          jsManager.saveDefault("starting wood");
          jsManager.saveDefault("starting stone");
          jsManager.saveDefault("starting metal");
          for (Integer i=1; i<gameData.getJSONArray("terrain").size()+1; i++){
            if (!gameData.getJSONArray("terrain").getJSONObject(i-1).isNull("weighting")){
              jsManager.saveDefault(gameData.getJSONArray("terrain").getJSONObject(i-1).getString("id")+" weighting");
            }
          }
          revertChanges(event.panel, true);
        }
        else if (event.id.equals("start")){
          LOGGER_MAIN.info("Starting state change to a new game");
          newState = "map";
          loadingName = null;
        }
        else if (event.id.equals("load")){
          loadingName = ((BaseFileManager)getElement("loading manager", "load game")).selectedSaveName();
          LOGGER_MAIN.info("Starting state change to game via loading with file name"+loadingName);
          newState = "map";
        }
        else if (event.id.equals("exit")){
          quitGame();
        }
      }
    }
  }
}
