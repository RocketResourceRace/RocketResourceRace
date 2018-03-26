
class DropDown extends Element{
  ArrayList<String> options;
  ArrayList<Integer> availableOptions;
  int textSize;
  boolean dropped;
  color bgColour, strokeColour;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  DropDown(int x, int y, int w, int textSize, color bgColour, color strokeColour, String[] options){
    this.x = x;
    this.y = y;
    this.w = w;
    this.textSize = textSize;
    this.h = getH();
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    removeAllOptions();
    for (String option : options){
      this.options.add(option);
    }
    dropped = true;
    resetAvailable();
  }
  void setOptions(ArrayList<String> options){
    this.options = options;
  }
  void addOption(String option){
    this.options.add(option);
  }
  void removeOption(String option){
    for (int i=0; i <options.size(); i++){
      if (option.equals(options.get(i))){
        options.remove(i);
      }
    }
  }
  void removeAllOptions(){
    this.options = new ArrayList<String>();
  }
  void resetAvailable(){
    this.availableOptions = new ArrayList<Integer>();
  }
  String getSelected(){
    return options.get(availableOptions.get(0));
  }
  void makeAvailable(String option){
    for (int i=0; i<availableOptions.size(); i++){
      if (options.get(availableOptions.get(i)).equals(option)){
        return;
      }
    }
    for (int i=0; i<options.size(); i++){
      if (options.get(i).equals(option)){
        this.availableOptions.add(i);
        return;
      }
    }
  }
  void makeUnavailable(String option){
    for (int i=0; i<options.size(); i++){
      if (options.get(i).equals(option)){
        this.availableOptions.remove(i);
        return;
      }
    }
  }
  void select(int j){
    int temp = availableOptions.get(0);
    availableOptions.set(0, availableOptions.get(j));
    availableOptions.set(j, temp);
  }
  void select(String s){
    for (int j=0; j<availableOptions.size(); j++){
      if (options.get(availableOptions.get(j)) == s){
        select(j);
      }
    }
  }
  int getH(){
    textSize(textSize*TextScale);
    return ceil(textAscent() + textDescent());
  }
  boolean optionAvailable(int i){
    for (int option : availableOptions){
      if(option == i){
        return true;
      }
    }
    return false;
  }
  void draw(){
    pushStyle();
    h = getH();
    fill(brighten(bgColour, ONOFFSET));
    stroke(strokeColour);
    rect(x+xOffset, y+yOffset, w, h);
    fill(0);
    textAlign(LEFT, TOP);
    text("Current Task: "+options.get(availableOptions.get(0)), x+xOffset+5, y+yOffset);
    
    if (dropped){
      for (int j=1; j< availableOptions.size(); j++){
        if (active && mouseOver(j)){
          fill(brighten(bgColour, HOVERINGOFFSET));
        }
        else{
          fill(bgColour);
        }
        rect(x+xOffset, y+yOffset+h*j, w, h);
        fill(0);
        text(options.get(availableOptions.get(j)), x+xOffset+5, y+yOffset+h*j);
      }
    }
    popStyle();
  }
  
  ArrayList<String> mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseMoved"){
      active = moveOver();
      
    }
    if (eventType == "mouseClicked" && button == LEFT){
      for (int j=1; j < availableOptions.size();j++){
        if (mouseOver(j)){
          select(j);
          events.add("valueChanged");
        }
      }
    }
    return events;
  }
  
  String findMouseOver(){
    for (int j=0; j<availableOptions.size(); j++){
      if (mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+h*j+yOffset && mouseY <= y+h*(j+1)+yOffset)
        return options.get(availableOptions.get(j));
    }
    return "";
  }
  
  boolean moveOver(){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+yOffset && mouseY <= y+h*availableOptions.size()+yOffset;
  }
  boolean mouseOver(int j){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+h*j+yOffset && mouseY <= y+h*(j+1)+yOffset;
  }
}





class Button extends Element{
  private int x, y, w, h, cx, cy, textSize, textAlign;
  private color bgColour, strokeColour, textColour;
  private String state, text;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  private ArrayList<String> lines;
  
  Button(int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text){
    state = "off";
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.textColour = textColour;
    this.textSize = textSize;
    this.textAlign = textAlign;
    this.text = text;
    centerCoords();
    
    setLines(text);
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    centerCoords();
  }
  void centerCoords(){
    cx = x+w/2;
    cy = y+h/2;
  }
  void setText(String text){
    this.text = text;
    setLines(text);
  }
  void draw(){
    //println(xOffset, yOffset);
    int padding=0;
    float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
    pushStyle();
    fill(bgColour);
    if (state == "off"){
      fill(bgColour);
    }
    else if (state == "hovering"){
      fill(min(r+HOVERINGOFFSET, 255), min(g+HOVERINGOFFSET, 255), min(b+HOVERINGOFFSET, 255));
    }
    else if (state == "on"){
      fill(min(r+ONOFFSET, 255), min(g+ONOFFSET, 255), min(b+ONOFFSET, 255));
    }
    stroke(strokeColour);
    strokeWeight(3);
    rect(x+xOffset, y+yOffset, w, h);
    noTint();
    fill(textColour);
    textAlign(textAlign, TOP);
    textSize(textSize*TextScale);
    if (lines.size() == 1){
      padding = h/10;
    }
    padding = (lines.size()*(int)(textSize*TextScale)-h/2)/2;
    for (int i=0; i<lines.size(); i++){
      if (textAlign == CENTER){
        text(lines.get(i), cx+xOffset, y+(h*0.9-textSize*TextScale)/2+yOffset);
      }
      else{
        text(lines.get(i), x+xOffset, y + yOffset);
      }
    }
    popStyle();
  }
  
  ArrayList<String> setLines(String s){
    int j = 0;
    lines = new ArrayList<String>();
    for (int i=0; i<s.length(); i++){
      if(s.charAt(i) == '\n'){
        lines.add(s.substring(j, i));
        j=i+1;
      }
    }
    lines.add(s.substring(j, s.length()));
    
    return lines;
  }
  
  ArrayList<String> mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if(eventType == "mouseReleased"){
      if (state == "on"){
        events.add("clicked");
      }
      state = "off";
    }
    if (mouseOver()){
      if (!state.equals("on")){
        state = "hovering";
      }
      if (eventType == "mousePressed"){
        state = "on";
        if(soundOn){
          sfx.get("click3").play();
        }
      }
    }
    else{
      state = "off";
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+yOffset && mouseY <= y+h+yOffset;
  }
}





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
    line(x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight/2+yOffset+padding/2, x+value.floatValue()/range.floatValue()*w+xOffset-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight+yOffset+padding/2);
    stroke(0);
    fill(0);
    textSize(12*TextScale);
    textAlign(LEFT, BOTTOM);
    text(name, x+xOffset, y+yOffset);
    popStyle();
  }
}




class Text extends Element{
  int x, y, size, colour, align;
  PFont font;
  String text;
  
  Text(int x, int y,  int size, String text, color colour, int align){
    this.x = x;
    this.y = y;
    this.size = size;
    this.text = text;
    this.colour = colour;
    this.align = align;
  }
  void setText(String text){
    this.text = text;
  }
  void calcSize(){
    textSize(size*TextScale);
    this.w = ceil(textWidth(text));
    this.h = ceil(textAscent()+textDescent());
  }
  void draw(){
    calcSize();
    if (font != null){
      textFont(font);
    }
    textAlign(align, TOP);
    textSize(size*TextScale);
    fill(colour);
    text(text, x+xOffset, y+yOffset);
  }
  boolean mouseOver(){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+yOffset && mouseY <= y+h+yOffset;
  }
}





class TextEntry extends Element{
  StringBuilder text;
  int x, y, w, h, textSize, textAlign, cursor, selected;
  color textColour, boxColour, borderColour, selectionColour;
  String allowedChars, name;
  final int BLINKTIME = 500;
  
  TextEntry(int x, int y, int w, int h, int textAlign, color textColour, color boxColour, color borderColour, String allowedChars){
    this.x = x;
    this.y = y;
    this.h = h;
    this.w = w;
    this.textColour = textColour;
    this.textSize = 10;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    this.borderColour = borderColour;
    this.allowedChars = allowedChars;
    text = new StringBuilder();
    selectionColour = brighten(selectionColour, 150);
    deactivate();
  }
  TextEntry(int x, int y, int w, int h, int textAlign, color textColour, color boxColour, color borderColour, String allowedChars, String name){
    this.x = x;
    this.y = y;
    this.h = h;
    this.w = w;
    this.textColour = textColour;
    this.textSize = 20;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    this.borderColour = borderColour;
    this.allowedChars = allowedChars;
    this.name = name;
    text = new StringBuilder();
    selectionColour = brighten(selectionColour, 150);
    deactivate();
  }
  
  void draw(){
    boolean showCursor = ((millis()/BLINKTIME)%2==0 || keyPressed) && active;
    pushStyle();
    
    // Draw a box behind the text
    fill(boxColour);
    stroke(borderColour);
    rect(x+xOffset, y+yOffset, w, h);
    // Draw selection box
    if (selected != cursor && active && cursor >= 0 ){
      fill(selectionColour);
      rect(x+textWidth(text.substring(0, min(cursor, selected)))+xOffset+5, y+2, textWidth(text.substring(min(cursor, selected)+yOffset, max(cursor, selected))), h-4);
    }
    
    // Draw the text
    textSize(textSize*TextScale);
    textAlign(textAlign);
    fill(textColour);
    text(text.toString(), x+xOffset+5, y+yOffset+(h-textSize*TextScale)/2, w, h);
    
    // Draw cursor
    if (showCursor){
      fill(0);
      noStroke();
      rect(x+textWidth(text.toString().substring(0,cursor))+xOffset+5, y+yOffset+(h-textSize*TextScale)/2, 1, textSize*TextScale);
    }
    if (name != null){
      fill(0);
      textSize(10);
      textAlign(LEFT);
      text(name, x, y-12);
    }
    
    popStyle();
  }
  
  void resetSelection(){
    selected = cursor;
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  
  int getCursorPos(int mx, int my){
    int i=0;
    for(; i<text.length(); i++){
      textSize(textSize*TextScale);
      if (textWidth(text.substring(0, i)) + x > mx)
        break;
    }
    if (0 <= i && i <= text.length() && y+yOffset+(h-textSize*TextScale)/2<= my && my <= y+yOffset+(h-textSize*TextScale)/2+textSize*TextScale){
      return i;
    }
    return cursor;
  }
  
  void doubleSelectWord(){
    if (!(y <= mouseY && mouseY <= y+h)){
      return;
    }
    int c = getCursorPos(mouseX, mouseY);
    int i;
    for (i=min(c, text.length()-1); i>0; i--){
      if (text.charAt(i) == ' '){
        i++;
        break;
      }
    }
    cursor = i;
    for (i=c; i<text.length(); i++){
      if (text.charAt(i) == ' '){
        break;
      }
    }
    selected = i;
  }
  
  ArrayList<String> mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseClicked"){
      if (button == LEFT){
        if (mouseOver()){
          activate();
        }
      }
    }
    else if (eventType == "mousePressed"){
      if (button == LEFT){
        cursor = getCursorPos(mouseX, mouseY);
        selected = getCursorPos(mouseX, mouseY);
      }
      if(!mouseOver()){
        deactivate();
      }
    }
    else if (eventType == "mouseDragged"){
      if (button == LEFT){
        selected = getCursorPos(mouseX, mouseY);
      }
    }
    else if (eventType == "mouseDoubleClicked"){
      doubleSelectWord();
    }
    return events;
  }
  
  ArrayList<String> keyboardEvent(String eventType, char _key){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "keyTyped"){
      if (_key == BACKSPACE){
        if (selected == cursor){
          if (cursor > 0){
            text.deleteCharAt(--cursor);
            resetSelection();
          }
        }
        else{
          text = new StringBuilder(text.substring(0, min(cursor, selected)) + text.substring(max(cursor, selected), text.length()));
          cursor = min(cursor, selected);
          resetSelection();
        }
      }
      else if (_key == '\n'){
        events.add("enterPressed");
        deactivate();
      }
      else if (allowedChars.equals("") || allowedChars.contains(""+_key)){
        if (cursor != selected){
          text = new StringBuilder(text.substring(0, min(cursor, selected)) + text.substring(max(cursor, selected), text.length()));
          cursor = min(cursor, selected);
          resetSelection();
        }
        text.insert(cursor++, _key);
        resetSelection();
      }
    }
    else if (eventType == "keyPressed"){
      if (_key == CODED){
        if (keyCode == LEFT){
          cursor = max(0, cursor-1);
          resetSelection();
        }
        if (keyCode == RIGHT){
          cursor = min(text.length(), cursor+1);
          resetSelection();
        }
      }
    }
    return events;
  }
  
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}





class ToggleButton extends Element{
  color bgColour, strokeColour;
  String name;
  boolean on;
  ToggleButton(int x, int y, int w, int h, color bgColour, color strokeColour, String name){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.name = name;
  }
  ArrayList<String> mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseClicked"&&mouseOver()){
      events.add("toggled");
      on = !on;
    }
    return events;
  }
  void transform(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  boolean getState(){
    return on;
  }
  void draw(){
    pushStyle();
    fill(bgColour);
    stroke(strokeColour);
    rect(x, y, w, h);
    if (on){
      fill(0, 255, 0);
      rect(x, y, w/2, h);
    }
    else{
      fill(255, 0, 0);
      rect(x+w/2, y, w/2, h);
    }
    fill(0);
    textSize(8*TextScale);
    textAlign(LEFT, BOTTOM);
    text(name, x, y);
    popStyle();
  }
  Boolean mouseOver(){
    return mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
  }
}