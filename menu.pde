


class Menu extends State{
  PImage BGimg;
  PShape bg;
  String currentPanel, newPanel;
  HashMap<String, String[]> stateChangers, settingChangers;
  
  Menu(){
    BGimg = loadImage("data/menu_background.jpeg");
    bg = createShape(RECT, 0, 0, width, height);
    bg.setTexture(BGimg);
    
    currentPanel = "startup";
    loadMenuPanels();
    newPanel = currentPanel;
    activePanel = currentPanel;
  }
  
  void loadMenuPanels(){
    resetPanels();
    jsManager.loadMenuElements(this, jsManager.loadFloatSetting("gui scale"));
    hidePanels();
    getPanel(currentPanel).setVisible(true);
    stateChangers = jsManager.getChangeStateButtons();
    settingChangers = jsManager.getChangeSettingButtons();
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
    return getNewState();
  }
  
  void changeMenuPanel(){
    panelToTop(newPanel);
    getPanel(newPanel).setVisible(true);
    getPanel(currentPanel).setVisible(false);
    currentPanel = new String(newPanel);
    for (Element elem : getPanel(newPanel).elements){
      elem.mouseEvent("mouseMoved", LEFT);
    }
    activePanel = newPanel;
  }
  
  void saveMenuSetting(String id, Event event){
    if (settingChangers.get(id) != null){
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
          }
          break;
      }
    }
  }
  
  void revertChanges(String panel){
    for (Element elem : getPanel(panel).elements){
      if (!jsManager.hasFlag(panel, elem.id, "autosave") && settingChangers.get(elem.id) != null){
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
            }
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
          revertChanges(event.panel);
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
          revertChanges(event.panel);
        }
        else if (event.id.equals("start")){
          newState = "map";
        }
        else if (event.id.equals("exit")){
          exit();
        }
      }
    }
  }
}
