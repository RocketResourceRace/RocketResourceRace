
class Slider extends Element{
  private int x, y, w, h, cx, cy;
  private float  major, minor, upper, lower, step, value, knobSize;
  private color bgColour, strokeColour, scaleColour;
  private boolean horizontal, pressed=false;
  final int boxHeight = 20, boxWidth = 10;
  private final int PRESSEDOFFSET = 50;
  private String name;
  
  Slider(int x, int y, int w, int h, color bgColour, color strokeColour, color scaleColour, float lower, float def, float upper, float major, float minor, float step, boolean horizontal, String name){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.scaleColour = scaleColour;
    this.major = major;
    this.minor = minor;
    this.upper = upper;
    this.lower = lower;
    this.horizontal = horizontal;
    this.step = step;
    this.name = name;
    this.value = def;
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  
  void setValue(float value){
    if (value < lower){
      this.value = lower;
    }
    else if (value > upper){
      this.value = upper;
    }
    else{
      this.value = int(value/step)*step;
    }
  }
  
  float getValue(){
    return value;
  }
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    mouseEvent(eventType, button);
    if (button == LEFT){
      if (mouseOver() && eventType == "mousePressed"){
          pressed = true;
          setValue((float)(mouseX-x)/w*(float)(upper-lower)+lower);
          events.add("valueChanged");
      }
      else if (eventType == "mouseReleased"){
        pressed = false;
      }
      if (eventType == "mouseDragged" && pressed){
        setValue((float)(mouseX-x)/w*(float)(upper-lower)+lower);
        events.add("valueChanged");
      }
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
  
  float getInc(float x){
    float a = (round(x/step))*step;
    String b = ""+a;
    int c = min((""+step).length(), b.length());
    b = b.substring(0, c);
    a = Float.parseFloat(b);
    return a;
  }
  
  void draw(){
    float j = lower, range = upper-lower;
    float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
    pushStyle();
    stroke(strokeColour);
    //rect(x, y, w, h);
    
    while(j<=upper){
      if (j != 0)
        line(j/range*w+x-lower*w/(upper-lower), y+h/4, j/range*w+x-lower*w/(upper-lower), y+h-h/4);
      j += range/minor;
    }
    j=lower;
    while(j<=upper){
      fill(scaleColour);
      textSize(10);
      textAlign(CENTER);
      text(""+getInc(j), j/range*w+x+xOffset-lower*w/(upper-lower), y+yOffset);
      fill(bgColour);
      line(j/range*w+x-lower*w/(upper-lower), y, j/range*w+x-lower*w/(upper-lower), y+h);
      j = getInc(j+((float)range)/((float)major));
    }
    
    if (pressed){
      fill(min(r-PRESSEDOFFSET, 255), min(g-PRESSEDOFFSET, 255), min(b+PRESSEDOFFSET, 255));
    }
    else{
      fill(bgColour);
    }
    
    textSize(15);
    textAlign(CENTER);
    this.knobSize = textWidth(""+getInc(value));
    rectMode(CENTER);
    rect(x+(float)value/(upper-lower)*w+xOffset-lower*w/(upper-lower), y+h/2+yOffset, knobSize, boxHeight);
    rectMode(CORNER);
    fill(scaleColour);
    text(""+getInc(value), x+(float)value/(upper-lower)*w+xOffset-lower*w/(upper-lower), y+h/2+boxHeight/4+yOffset);
    stroke(0);
    textAlign(CENTER);
    line(x+(float)value/(upper-lower)*w+xOffset-lower*w/(upper-lower), y+h/2-boxHeight/2+yOffset, x+(float)value/(upper-lower)*w+xOffset-lower*w/(upper-lower), y+h/2-boxHeight+yOffset);
    fill(0);
    textAlign(LEFT);
    textSize(10);
    text(name, x, y-12);
    popStyle();
  }
}