
class State{
  ArrayList<Panel> panels;
  String newState, activePanel;

  State(){
    panels = new ArrayList<Panel>();
    addPanel("default", 0, 0, width, height, true, true, color(255, 255), color(0));
    newState = "";
    activePanel = "default";
  }

  String getNewState(){
    String t = newState;
    newState = "";
    return t;
  }

  String update(){
    drawPanels();
    return getNewState();
  }
  void enterState(){

  }
  void leaveState(){

  }
  void hidePanels(){
    for (Panel panel:panels){
      panel.visible = false;
    }
  }
  
  void resetPanels(){
    panels.clear();
  }
  
  void addPanel(String id, int x, int y, int w, int h, Boolean visible, Boolean blockEvent, color bgColour, color strokeColour){
    // Adds new panel to front
    panels.add(new Panel(id, x, y, w, h, visible, blockEvent, bgColour, strokeColour));
    panelToTop(id);
  }
  void addPanel(String id, int x, int y, int w, int h, Boolean visible, String fileName, color strokeColour){
    // Adds new panel to front
    panels.add(new Panel(id, x, y, w, h, visible, fileName, strokeColour));
    panelToTop(id);
  }
  void addElement(String id, Element elem){
    elem.setID(id);
    getPanel("default").elements.add(elem);
    elem.setOffset(getPanel("default").x, getPanel("default").y);
  }
  void addElement(String id, Element elem, String panel){
    elem.setID(id);
    getPanel(panel).elements.add(elem);
    elem.setOffset(getPanel(panel).x, getPanel(panel).y);
  }

  Element getElement(String id, String panel){
    for (Element elem : getPanel(panel).elements){
      if (elem.id.equals(id)){
        return  elem;
      }
    }
    println(String.format("Element not found %s panel:%s", id, panel));
    return null;
  }

  void removeElement(String elementID, String panelID){
    getPanel(panelID).elements.remove(elementID);
  }
  void removePanel(String id){
    panels.remove(findPanel(id));
  }

  void panelToTop(String id){
    Panel tempPanel = getPanel(id);
    for (int i=findPanel(id); i>0; i--){
      panels.set(i, panels.get(i-1));
    }
    panels.set(0, tempPanel);
  }
  
  void elementToTop(String id, String panelID){
    Element tempElem = getElement(id, panelID);
    boolean found = false;
    for (int i=0; i<getPanel(panelID).elements.size()-1; i++){
      if (getPanel(panelID).elements.get(i).id.equals(id)){
        found = true;
      }
      if (found){
        getPanel(panelID).elements.set(i, getPanel(panelID).elements.get(i+1));
      }
    }
    getPanel(panelID).elements.set(getPanel(panelID).elements.size()-1, tempElem);
  }

  void printPanels(){
    for(Panel panel:panels){
      print(panel.id);
    }
    println();
  }

  int findPanel(String id){
    for (int i=0; i<panels.size(); i++){
      if (panels.get(i).id.equals(id)){
        return i;
      }
    }
    return -1;
  }
  Panel getPanel(String id){
    return panels.get(findPanel(id));
  }

  void drawPanels(){
    // Draw the panels in reverse order (highest in the list are drawn last so appear on top)
    for (int i=panels.size()-1; i>=0; i--){
      if (panels.get(i).visible){
        panels.get(i).draw();
      }
    }
  }
  // Empty method for use by children
  ArrayList<String> mouseEvent(String eventType, int button){return new ArrayList<String>();}
  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){return new ArrayList<String>();}
  ArrayList<String> keyboardEvent(String eventType, char _key){return new ArrayList<String>();}

  void elementEvent(ArrayList<Event> events){
    for (Event event : events){
      println(event.info(), 1);
    }
  }
 
  void _elementEvent(ArrayList<Event> events){
    for (Event event : events){
      if (event.type.equals("element to top")){
        elementToTop(event.id, event.panel);
      }
    }
  }

  void _mouseEvent(String eventType, int button){
    ArrayList<Event> events = new ArrayList<Event>();
    mouseEvent(eventType, button);
    if (eventType == "mousePressed"){
      for (int i=0; i<panels.size(); i++){
        if (panels.get(i).mouseOver()&& panels.get(i).visible&&panels.get(i).blockEvent){
          activePanel = panels.get(i).id;
          break;
        }
      }
    }
    for (Panel panel : panels){
      if(activePanel == panel.id || eventType.equals("mouseMoved") || panel.overrideBlocking){
        // Iterate in reverse order
        for (int i=panel.elements.size()-1; i>=0; i--){
          if (panel.elements.get(i).active && panel.visible){
            for (String eventName : panel.elements.get(i)._mouseEvent(eventType, button)){
              events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
              if (eventName.equals("stop events")){
                return;
              }
            }
          }
        }
        if (!eventType.equals("mouseMoved") && !panel.overrideBlocking)
          break;
      }
    }
    elementEvent(events);
    _elementEvent(events);
  }
  void _mouseEvent(String eventType, int button, MouseEvent event){
    ArrayList<Event> events = new ArrayList<Event>();
    mouseEvent(eventType, button, event);
    for (Panel panel : panels){
      if(panel.mouseOver() && panel.visible){
        // Iterate in reverse order
        for (int i=panel.elements.size()-1; i>=0; i--){
          if (panel.elements.get(i).active){
            for (String eventName : panel.elements.get(i)._mouseEvent(eventType, button, event)){
              events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
              if (eventName.equals("stop events")){
                return;
              }
            }
          }
        }
      }
    }
    elementEvent(events);
    _elementEvent(events);
  }
  void _keyboardEvent(String eventType, char _key){
    ArrayList<Event> events = new ArrayList<Event>();
    keyboardEvent(eventType, _key);
    for (Panel panel : panels){
      for (int i=panel.elements.size()-1; i>=0; i--){
        if (panel.elements.get(i).active && panel.visible){
          for (String eventName : panel.elements.get(i)._keyboardEvent(eventType, _key)){
            events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
          }
        }
      }
    }
    elementEvent(events);
    _elementEvent(events);
  }
}






class Panel{
  ArrayList<Element> elements;
  String id;
  PImage img;
  Boolean visible, blockEvent, overrideBlocking;
  private int x, y, w, h;
  private color bgColour, strokeColour;
  PGraphics panelCanvas, elemGraphics;

  Panel(String id, int x, int y, int w, int h, Boolean visible, Boolean blockEvent, color bgColour, color strokeColour){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.visible = visible;
    this.blockEvent = blockEvent;
    this.id = id;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    elements = new ArrayList<Element>();
    panelCanvas = createGraphics(w, h, P2D);
    overrideBlocking = false;
  }

  Panel(String id, int x, int y, int w, int h, Boolean visible, String fileName, color strokeColour){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.visible = visible;
    this.id = id;
    this.img = loadImage(fileName);
    this.strokeColour = strokeColour;
    elements = new ArrayList<Element>();
    panelCanvas = createGraphics(w, h, P2D);
    overrideBlocking = false;
  }
  
  void setOverrideBlocking(boolean v){
    overrideBlocking = v;
  }

  void setOffset(){
    for (Element elem : elements){
      elem.setOffset(x, y);
    }
  }
  void setColour(color c){
    bgColour = c;
  }

  void setVisible(boolean a){
    visible = a;
    for (Element elem:elements){
      elem.mouseEvent("mouseMoved", mouseButton);
    }
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    setOffset();
  }

  void draw(){
    panelCanvas.beginDraw();
    panelCanvas.clear();
    panelCanvas.pushStyle();
    if (img == null){
      if (bgColour != color(255, 255)){
        panelCanvas.fill(bgColour);
        panelCanvas.stroke(strokeColour);
        panelCanvas.rect(0, 0, w, h);
      }
    }
    else{
      //imageMode(CENTER);
      panelCanvas.image(img, 0, 0, w, h);
    }
    panelCanvas.popStyle();

    for (Element elem : elements){
      if(elem.visible){
        elem.draw(panelCanvas);
      }
    }
    panelCanvas.endDraw();
    image(panelCanvas, x, y);
  }

  int getX(){
    return x;
  }
  int getY(){
    return y;
  }

  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}





class Element{
  boolean active = true;
  boolean visible = true;
  int x, y, w, h, xOffset, yOffset;
  String id;

  void draw(PGraphics panelCanvas){}
  ArrayList<String> mouseEvent(String eventType, int button){return new ArrayList<String>();}
  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event){return new ArrayList<String>();}
  ArrayList<String> keyboardEvent(String eventType, char _key){return new ArrayList<String>();}
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  void setOffset(int xOffset, int yOffset){
    this.xOffset = xOffset;
    this.yOffset = yOffset;
  }
  
  void setID(String id){
    this.id = id;
  }
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    return mouseEvent(eventType, button);
  }
  ArrayList<String> _mouseEvent(String eventType, int button, MouseEvent event){
    return mouseEvent(eventType, button, event);
  }
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    return keyboardEvent(eventType, _key);
  }
  void activate(){
    active = true;
  }
  void deactivate(){
    active = false;
  }
}
