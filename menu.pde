// <Menu Level> <Order> <Name>


class Menu extends State{
  PImage BGimg;
  String currentPanel, newPanel;
  
  Menu(){
    BGimg = loadImage("data/menu_background.jpeg");
    BGimg.resize(width, height);
    
    int buttonW = (int)(300.0*GUIScale);
    int buttonH = (int)(70.0*GUIScale);
    int buttonP = (int)(50.0*GUIScale);
    color bColour = color(100, 100, 100);
    color sColour = color(150, 150, 150);
    
    addPanel("settings", 0, 0, width, height, true, color(255, 255, 255, 255), color(0));
    addPanel("startup", 0, 0, width, height, true, color(255, 255, 255, 255), color(0));
    addPanel("new game", 0, 0, width, height, true, color(255, 255, 255, 255), color(0));
    hidePanels();
    getPanel("startup").visible = true;
    currentPanel = "startup";
    newPanel = currentPanel;
    
    addElement("new game", new Button(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "New Game"), "startup");
    addElement("load game", new Button(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Load Game"), "startup");
    addElement("settings", new Button(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Settings"), "startup");
    addElement("exit", new Button(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Exit"), "startup");
    
    addElement("gui scale", new Slider(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, color(0, 255, 0), bColour, color(150, 150, 150), color(0), 0.5, GUIScale, 1.5, 10, 50, 0.01, true, "GUI Scale"), "settings");
    addElement("volume", new Slider(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, color(0, 255, 0), bColour, color(150, 150, 150), color(0), 0, 0.5, 1, 10, 50, 0.05, true, "Volume"), "settings");
    addElement("text scale", new Slider(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH, color(0, 255, 0), bColour, color(150, 150, 150), color(0), 0.8, TextScale, 2.4, 8, 8*5, 0.05, true, "Text Scale"), "settings");
    addElement("back", new Button(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Back"), "settings");
  
    addElement("start", new Button(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Start"), "new game");
    addElement("save name", new TextEntry(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, LEFT, color(0), color(100, 100, 100), color(150, 150, 150), LETTERSNUMBERS, "Save Name"), "new game");
    addElement("map size", new Slider(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH, color(0, 255, 0), bColour, color(255, 255, 255), color(0), 50, mapSize, 150, 10, 20, 5, true, "Map Size"), "new game");
    addElement("back", new Button(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH, bColour, sColour, color(255), 25, CENTER, "Back"), "new game");
  }
  
  color currentColour(){
    float c = abs(((float)(hour()-12)+(float)minute()/60)/24);
    color day = color(255, 255, 255, 50);
    color night = color(0, 0, 50, 255);
    return lerpColor(day, night, c*2);
  }
  
  String update(){
    pushStyle();
    background(BGimg);
    fill(currentColour());
    rect(0, 0, width, height);
    popStyle();
    if (!currentPanel.equals(newPanel)){
      changeMenuPanel();
    }
    drawPanels();
    return getNewState();
  }
  
  void scaleGUI(){
    int buttonW = (int)(300.0*GUIScale);
    int buttonH = (int)(70.0*GUIScale);
    int buttonP = (int)(50.0*GUIScale);
    
    getElement("new game", "startup").transform(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH);
    getElement("load game", "startup").transform(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH);
    getElement("settings", "startup").transform(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH);
    getElement("exit", "startup").transform(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH);
    
    getElement("gui scale", "settings").transform(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH);
    getElement("volume", "settings").transform(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH);
    getElement("text scale", "settings").transform(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH);
    getElement("back", "settings").transform(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH);
    
    getElement("start", "new game").transform(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH);
    getElement("save name", "new game").transform(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH);
    getElement("map size", "new game").transform(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH);
    getElement("back", "new game").transform(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH);
  }
  
  void changeMenuPanel(){
    panelToTop(newPanel);
    getPanel(newPanel).setVisible(true);
    getPanel(currentPanel).setVisible(false);
    currentPanel = new String(newPanel);
    for (String id : getPanel(newPanel).elements.keySet()){
      getPanel(newPanel).elements.get(id)._mouseEvent("mouseMoved", LEFT);
    }
  }
  
  void elementEvent(ArrayList<Event> events){
    for (Event event:events){
      if (event.type.equals("valueChanged")){
        if (event.id.equals("gui scale")){
          GUIScale = ((Slider)getElement("gui scale", "settings")).getValue();
          changeSetting("gui_scale", ""+GUIScale);
          writeSettings();
        }
        if (event.id.equals("text scale")){
          TextScale = ((Slider)getElement("text scale", "settings")).getValue();
          changeSetting("text_scale", ""+TextScale);
          writeSettings();
        }
        if (event.id.equals("volume")){
          setVolume(((Slider)getElement("volume", "settings")).getValue());
          changeSetting("volume", ""+volume);
          writeSettings();
        }
        if (event.id.equals("map size")){
          mapSize = (int)((Slider)getElement("map size", "new game")).getValue();
          changeSetting("mapSize", ""+mapSize);
          writeSettings();
        }
      }
      if (event.type.equals("clicked")){
        switch (currentPanel){
          case "startup":
            switch (event.id){
              case "exit":
                exit();
                break;
              case "settings":
                newPanel = "settings";
                break;
              case "new game":
                newPanel = "new game";
                break;
            }
            break;
          case "settings":
            switch (event.id){
              case "back":
                newPanel = "startup";
                scaleGUI();
                break;
            }
            break;
          case "new game":
            switch (event.id){
              case "back":
                newPanel = "startup";
                break;
              case "start":
                newState = "map";
                break;
            }
            break;
        }
        
      }
    }
  }
}