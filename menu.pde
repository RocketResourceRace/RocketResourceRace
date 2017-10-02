
class Menu extends State{
  Menu(){
    addElement("button1", new Button(550, 50, 200, 70, color(0, 255, 0), color(150, 150, 150), color(0), 25, CENTER, "button\n test12345"));
    addElement("slider1", new Slider(550, 200, 300, 50, color(0, 255, 0), color(150, 150, 150), color(0), 0, 100, 10, 50, 0.5, true));
  }
}