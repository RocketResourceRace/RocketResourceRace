// <Menu Level> <Order> <Name>


class Menu extends State{
  int menuPanelW, menuPanelH, menuPanelX, menuPanelY;
  Menu(){
    //addElement("button1", new Button(550, 50, 200, 70, color(0, 255, 0), color(150, 150, 150), color(0), 25, CENTER, "button test\n12345"));
    //addElement("slider1", new Slider(550, 200, 300, 50, color(0, 255, 0), color(150, 150, 150), color(0), 0, 100, 10, 50, 0.5, true));
    //addElement("textField1", new TextField(550, 500, 500, 400, 0x0, 20, LEFT, 0x0));
    //addElement("textentry1", new TextEntry(550, 500, 500, 30, LEFT, color(0,0,0), color(250,250,250), color(150,150,150), ""));
    menuPanelW = width/2;
    menuPanelH = (int)(height*0.8);
    menuPanelX = width/4;
    menuPanelY = (int)(height * 0.1);
    
    addPanel("startup", menuPanelX, menuPanelY, menuPanelW, menuPanelH, true, color(20, 20, 170), color(0));
    
    addElement("", new Button(0, 0, 200, 70, color(0, 255, 0), color(150, 150, 150), color(0), 25, CENTER, "button test\n12345"), "startup");
  }
}