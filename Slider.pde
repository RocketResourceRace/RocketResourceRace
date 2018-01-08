
class Slider extends Element{
  private int x, y, w, h, cx, cy, major, minor;
  private BigDecimal value, step, upper, lower;
  private float knobSize;
  private color KnobColour, strokeColour, scaleColour;
  private boolean horizontal, pressed=false;
  final int boxHeight = 20, boxWidth = 10;
  private final int PRESSEDOFFSET = 50;
  private String name;
  
  Slider(int x, int y, int w, int h, color KnobColour, color strokeColour, color scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.KnobColour = KnobColour;
    this.strokeColour = strokeColour;
    this.scaleColour = scaleColour;
    this.major = major;
    this.minor = minor;
    this.upper = new BigDecimal(""+upper);
    this.lower = new BigDecimal(""+lower);
    this.horizontal = horizontal;
    this.step = new BigDecimal(""+step);
    this.value = new BigDecimal(""+value);
    this.name = name;
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  
  void setValue(BigDecimal value){
    if (value.compareTo(lower) < 0){
      this.value = lower;
    }
    else if (value.compareTo(upper)>0){
      this.value = new BigDecimal(""+upper);
    }
    else{
      this.value = value.divideToIntegralValue(step).multiply(step);
    }
  }
  
  float getValue(){
    return value.floatValue();
  }
  BigDecimal getPreciseValue(){
    return value;
  }
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    mouseEvent(eventType, button);
    if (button == LEFT){
      if (mouseOver() && eventType == "mousePressed"){
          pressed = true;
          setValue((new BigDecimal(mouseX-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
          events.add("valueChanged");
      }
      else if (eventType == "mouseReleased"){
        pressed = false;
      }
      if (eventType == "mouseDragged" && pressed){
        setValue((new BigDecimal(mouseX-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
        events.add("valueChanged");
      }
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
  
  BigDecimal getInc(BigDecimal i){
    return i.stripTrailingZeros();
  }
  
  void draw(){
    BigDecimal range = upper.subtract(lower);
    float r = red(KnobColour), g = green(KnobColour), b = blue(KnobColour);
    pushStyle();
    stroke(strokeColour);
    //rect(x, y, w, h);
    
    
    for(int i=0; i<=minor; i++){
      fill(scaleColour);
      line(xOffset+x+w*i/minor, y+yOffset+h/4, xOffset+x+w*i/minor, y+yOffset+3*h/4);
    }
    for(int i=0; i<=major; i++){
      fill(scaleColour);
      textSize(10);
      textAlign(CENTER);
      text(getInc((new BigDecimal(""+i).multiply(range).divide(new BigDecimal(""+major), 15, BigDecimal.ROUND_HALF_EVEN).add(lower))).toPlainString(), xOffset+x+w*i/major, y+yOffset);
      line(xOffset+x+w*i/major, y+yOffset, xOffset+x+w*i/major, y+yOffset+h);
    }
    
    if (pressed){
      fill(min(r-PRESSEDOFFSET, 255), min(g-PRESSEDOFFSET, 255), min(b+PRESSEDOFFSET, 255));
    }
    else{
      fill(KnobColour);
    }
    
    textSize(15);
    textAlign(CENTER);
    rectMode(CENTER);
    this.knobSize = max(this.knobSize, textWidth(""+getInc(value)));
    rect(x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2+yOffset, knobSize, boxHeight);
    rectMode(CORNER);
    fill(scaleColour);
    text(getInc(value).toPlainString(), x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2+boxHeight/4+yOffset);
    stroke(0);
    textAlign(CENTER);
    line(x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight/2+yOffset, x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight+yOffset);
    fill(0);
    textAlign(LEFT);
    textSize(10);
    text(name, x, y-12);
    popStyle();
  }
}