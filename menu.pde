
class Menu extends State{
  Menu(){
    addElement("button1", new Button(550, 50, 200, 70, color(0, 255, 0), color(150, 150, 150), color(0), 25, CENTER, "button test\n12345"));
    addElement("slider1", new Slider(550, 200, 300, 50, color(0, 255, 0), color(150, 150, 150), color(0), 0, 100, 10, 50, 0.5, true));
    //addElement("textField1", new TextField(550, 500, 500, 400, 0x0, 20, LEFT, 0x0));
    addElement("textentry1", new TextEntry(550, 500, 500, 50, 16, LEFT, color(120,120,120), color(255,255,255), color(150,150,150), ""));
  }
}