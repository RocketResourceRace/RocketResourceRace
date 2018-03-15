
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
