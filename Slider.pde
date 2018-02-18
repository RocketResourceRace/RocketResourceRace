
class Slider extends Element{
  private int x, y, w, h, cx, cy, major, minor, lw, lx;
  private int padding = 20;
  private BigDecimal value, step, upper, lower;
  private float knobSize;
  private color KnobColour, bgColour, strokeColour, scaleColour;
  private boolean horizontal, pressed=false;
  final int boxHeight = 20, boxWidth = 10;
  private final int PRESSEDOFFSET = 50;
  private String name;
  boolean visible = true;
  
  Slider(int x, int y, int w, int h, color KnobColour, color bgColour, color strokeColour, color scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name){
    this.lx = x;
    this.x = x;
    this.y = y;
    this.lw = w;
    this.w = w;
    this.h = h;
    this.KnobColour = KnobColour;
    this.bgColour = bgColour;
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
    textSize(15);
    scaleKnob();
  }
  void show(){
    visible = true;
  }
  void hide(){
    visible = false;
  }
  void scaleKnob(){
    this.knobSize = textWidth(""+getInc(new BigDecimal(""+upper)));
  }
  void transform(int x, int y, int w, int h){
    this.lx = x;
    this.x = x;
    this.lw = w;
    this.w = w; 
    this.y = y;
    this.h = h;
  }
  void setScale(float lower, float value, float upper, int major, int minor){
    this.major = major;
    this.minor = minor;
    this.upper = new BigDecimal(""+upper);
    this.lower = new BigDecimal(""+lower);
    this.value = new BigDecimal(""+value);
    scaleKnob();
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
  
  ArrayList<String> mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (button == LEFT){
      if (mouseOver() && eventType == "mousePressed"){
          pressed = true;
          setValue((new BigDecimal(mouseX-x-xOffset)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
          events.add("valueChanged");
      }
      else if (eventType == "mouseReleased"){
        pressed = false;
      }
      if (eventType == "mouseDragged" && pressed){
        setValue((new BigDecimal(mouseX-x-xOffset)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
        events.add("valueChanged");
      }
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+yOffset && mouseY <= y+h+yOffset;
  }
  
  BigDecimal getInc(BigDecimal i){
    return i.stripTrailingZeros();
  }
  
  void draw(){
    if (!visible)return;
    BigDecimal range = upper.subtract(lower);
    float r = red(KnobColour), g = green(KnobColour), b = blue(KnobColour);
    pushStyle();
    fill(255, 100);
    stroke(strokeColour, 50);
    //rect(lx, y, lw, h);
    //rect(xOffset+x, y+yOffset+padding+2, w, h-padding);
    stroke(strokeColour);
    
    
    for(int i=0; i<=minor; i++){
      fill(scaleColour);
      line(xOffset+x+w*i/minor, y+yOffset+padding+(h-padding)/6, xOffset+x+w*i/minor, y+yOffset+5*(h-padding)/6+padding);
    }
    for(int i=0; i<=major; i++){
      fill(scaleColour);
      textSize(10*TextScale);
      textAlign(CENTER);
      text(getInc((new BigDecimal(""+i).multiply(range).divide(new BigDecimal(""+major), 15, BigDecimal.ROUND_HALF_EVEN).add(lower))).toPlainString(), xOffset+x+w*i/major, y+yOffset+padding);
      line(xOffset+x+w*i/major, y+yOffset+padding, xOffset+x+w*i/major, y+yOffset+h);
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
    rect(x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2+yOffset+padding/2, knobSize, boxHeight);
    rectMode(CORNER);
    fill(scaleColour);
    text(getInc(value).toPlainString(), x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2+boxHeight/4+yOffset+padding/2);
    stroke(0);
    textAlign(CENTER);
    stroke(255, 0, 0);
    line(x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight/2+yOffset+padding/2, x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight+yOffset);
    stroke(0);
    fill(0);
    textSize(12*TextScale);
    textAlign(LEFT, BOTTOM);
    text(name, x+xOffset, y+yOffset);
    popStyle();
  }
}