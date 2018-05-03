


class Menu extends State{
  PImage BGimg;
  PShape bg;
  String currentPanel, newPanel;
  HashMap<String, String[]> stateChangers, settingChangers;
  
  Menu(){
    BGimg = loadImage("data/menu_background.jpeg");
    bg = createShape(RECT, 0, 0, width, height);
    bg.setTexture(BGimg);
    
    loadMenuPanels();
    hidePanels();
    getPanel("startup").visible = true;
    currentPanel = "startup";
    newPanel = currentPanel;
    activePanel = currentPanel;

    //addElement("gui scale", new Slider(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, color(0, 255, 0), bColour, color(150, 150, 150), color(0), 0.5, GUIScale, 1.5, 10, 50, 0.01, true, "GUI Scale"), "settings");
    //addElement("volume", new Slider(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, color(0, 255, 0), bColour, color(150, 150, 150), color(0), 0, volume, 1, 10, 50, 0.05, true, "Volume"), "settings");
    //addElement("text scale", new Slider(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH, color(0, 255, 0), bColour, color(150, 150, 150), color(0), 0.8, TextScale, 2.4, 8, 8*5, 0.05, true, "Text Scale"), "settings");
    //addElement("back", new Button(width-buttonW-buttonP, buttonH*4+buttonP*5, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Back"), "settings");
    //addElement("background dimming", new ToggleButton(width-(buttonW+buttonP)*2, buttonH*0+buttonP*1, buttonW/2, buttonH/2, bColour, sColour, false, "Background Dimming"), "settings");
  
    //addElement("start", new Button(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Start"), "new game");
    //addElement("save name", new TextEntry(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, LEFT, color(0), color(100, 100, 100), color(150, 150, 150), LETTERSNUMBERS, "Save Name"), "new game");
    //addElement("map size", new Slider(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH, color(0, 255, 0), bColour, color(150), color(0), 50, mapSize, 300, 5, 25, 10, true, "Map Size"), "new game");
    //addElement("map 3d", new ToggleButton(width-buttonW*2-buttonP*2, buttonH, buttonW/2, buttonH/2, bColour, color(0), mapIs3D, "Map 3D"), "new game");
    //addElement("smoothing", new Slider(width-buttonW*2-buttonP*2, buttonH*1+buttonP*2, buttonW, buttonH, color(0, 255, 0), bColour, color(150), color(0), 0, 6, 20, 4, 20, 1, true, "Smoothing"), "new game");
    //addElement("water level", new Slider(width-buttonW*2-buttonP*2, buttonH*2+buttonP*3, buttonW, buttonH, color(0, 255, 0), bColour, color(150), color(0), 0.0, 0.5, 0.75, 5, 5, 0.01, true, "Water Level"), "new game");
    //addElement("ground spawns", new Slider(width-buttonW*2-buttonP*2, buttonH*3+buttonP*4, buttonW, buttonH, color(0, 255, 0), bColour, color(150), color(0), 50, 100, 300, 5, 25, 10, true, "Ground Spawns"), "new game");
    //addElement("back", new Button(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Back"), "new game");
    
  }
  
  void loadMenuPanels(){
    jsManager.loadMenuElements(this, GUIScale);
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
    for (String id : getPanel(newPanel).elements.keySet()){
      getPanel(newPanel).elements.get(id).mouseEvent("mouseMoved", LEFT);
    }
    activePanel = newPanel;
  }
  
  void elementEvent(ArrayList<Event> events){
    for (Event event:events){
      if (event.type.equals("valueChanged") && settingChangers.get(event.id) != null && event.panel != null){
        String type = jsManager.getElementType(event.panel, event.id);
        switch (type){
          case "slider":
            jsManager.saveSetting(settingChangers.get(event.id)[0], ((Slider)getElement(event.id, event.panel)).getValue());
            break;
          case "toggle button":
            jsManager.saveSetting(settingChangers.get(event.id)[0], ((ToggleButton)getElement(event.id, event.panel)).getState());
            break;
          case "tickbox":
            jsManager.saveSetting(settingChangers.get(event.id)[0], ((Tickbox)getElement(event.id, event.panel)).getState());
            break;
        }
        if (jsManager.hasFlag(event.panel, event.id, "autosave")){
          jsManager.writeSettings();
        }
      }
      if (event.type.equals("clicked")){
        if (stateChangers.get(event.id) != null && stateChangers.get(event.id)[0] != null ){
          newPanel = stateChangers.get(event.id)[0];
          if (event.id.equals("apply")){
            jsManager.writeSettings();
            loadMenuPanels();
          }
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
