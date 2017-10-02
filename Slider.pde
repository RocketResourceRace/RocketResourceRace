
class Slider extends Element{
  private int x, y, w, h, cx, cy;
  private float  major, minor, upper, lower, step, value;
  private color bgColour, strokeColour, scaleColour;
  private boolean horizontal, pressed=false;
  final int boxHeight = 20, boxWidth = 30;
  private final int PRESSEDOFFSET = 50;
  
  Slider(int x, int y, int w, int h, color bgColour, color strokeColour, color scaleColour, float lower, float upper, float major, float minor, float step, boolean horizontal){
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
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    mouseEvent(eventType, button);
    if (button == LEFT){
      if (mouseOver() && eventType == "mousePressed"){
          pressed = true;
          setValue((float)(mouseX-x)/w*(float)(upper-lower)+lower);
      }
      else if (eventType == "mouseReleased"){
        pressed = false;
      }
      if (eventType == "mouseDragged" && pressed){
        setValue((float)(mouseX-x)/w*(float)(upper-lower)+lower);
      }
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
  
  void draw(int xOffset, int yOffset){
    float j = lower, range = upper-lower;
    float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
    pushStyle();
    stroke(strokeColour);
    //rect(x, y, w, h);
    
    while(j<=upper){
      if (j != 0)
        line(j/range*w+x, y+h/4, j/range*w+x, y+h-h/4);
      j += range/minor;
    }
    j=lower;
    while(j<=upper){
      fill(scaleColour);
      textSize(10);
      textAlign(CENTER);
      text(""+j, j/range*w+x, y);
      fill(bgColour);
      line(j/range*w+x, y, j/range*w+x, y+h);
      j += range/major;
    }
    
    if (pressed){
      fill(min(r-PRESSEDOFFSET, 255), min(g-PRESSEDOFFSET, 255), min(b+PRESSEDOFFSET, 255));
    }
    else{
      fill(bgColour);
    }
    
    rect(x+(float)value/(upper-lower)*w-boxWidth/2, y+h/2-boxHeight/2, boxWidth, boxHeight);
    textSize(15);
    textAlign(CENTER);
    fill(scaleColour);
    text(""+Float.parseFloat((""+value).substring(0, min((""+value).length(), 5))), x+(float)value/(upper-lower)*w, y+h/2+boxHeight/4);
    stroke(0);
    line(x+(float)value/(upper-lower)*w, y+h/2-boxHeight/2, x+(float)value/(upper-lower)*w, y+h/2-boxHeight);
    popStyle();
  }
}