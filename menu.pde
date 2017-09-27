
class Menu extends State{
  Menu(){
    addElement("button1", new Button(550, 50, 200, 70, color(0, 255, 0), color(150, 150, 150), color(0), 15, CENTER, "button\n test\n12345"));
    addElement("slider1", new Slider(550, 200, 300, 60, color(0, 255, 0), color(150, 150, 150), 0, 100, 10, 50, 5, true));
  }
}