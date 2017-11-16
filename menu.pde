// <Menu Level> <Order> <Name>


class Menu extends State{
  PImage BGimg;
  String currentPanel;
  
  Menu(){
    
    BGimg = loadImage("res/menu_background.png");
    BGimg.resize(width, height);
    
    int buttonW = (int)(300*GUIScale);
    int buttonH = (int)(70*GUIScale);
    int buttonP = (int)(50*GUIScale);
    
    addPanel("settings", 0, 0, width, height, true, color(255, 255, 255, 255), color(0));
    addPanel("startup", 0, 0, width, height, true, color(255, 255, 255, 255), color(0));
    hidePanels();
    getPanel("startup").visible = true;
    currentPanel = "startup";
    
    addElement("new game", new Button(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, color(100, 100, 100), color(150, 150, 150), color(255), 25, CENTER, "New Game"), "startup");
    addElement("load game", new Button(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, color(100, 100, 100), color(150, 150, 150), color(255), 25, CENTER, "Load Game"), "startup");
    addElement("settings", new Button(width-buttonW-buttonP, buttonH*2+buttonP*3, buttonW, buttonH, color(100, 100, 100), color(150, 150, 150), color(255), 25, CENTER, "Settings"), "startup");
    addElement("exit", new Button(width-buttonW-buttonP, buttonH*3+buttonP*4, buttonW, buttonH, color(100, 100, 100), color(150, 150, 150), color(255), 25, CENTER, "Exit"), "startup");
    
    addElement("gui scale", new Slider(width-buttonW-buttonP, buttonH*0+buttonP*1, buttonW, buttonH, color(0, 255, 0), color(150, 150, 150), color(0), 0, 2, 10, 50, 0.05, true, "GUI Scale"), "settings");
    addElement("back", new Button(width-buttonW-buttonP, buttonH*1+buttonP*2, buttonW, buttonH, color(100, 100, 100), color(150, 150, 150), color(255), 25, CENTER, "Back"), "settings");
  }
  
  String update(){
    background(BGimg);
    drawPanels();
    return newState;
  }
  
  void changeMenuPanel(String newPanel){
    panelToTop(newPanel);
    getPanel(newPanel).visible = true;
    getPanel(currentPanel).visible = false;
    currentPanel = new String(newPanel);
  }
  
  void elementEvent(ArrayList<Event> events){
    for (Event event:events){
      if (event.type.equals("clicked")){
        switch (currentPanel){
          case "startup":
            switch (event.id){
              case "exit":
                exit();
              case "settings":
                changeMenuPanel("settings");
            }
          case "settings":
            switch (event.id){
              case "back":
                changeMenuPanel("startup");
            }
        }
        
      }
    }
  }
}