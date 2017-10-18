
class TextEntry extends Element{
  StringBuilder text;
  int x, y, w, h, textSize, textAlign, cursor, selected;
  color textColour, boxColour, borderColour;
  String allowedChars;
  final int BLINKTIME = 500;
  
  TextEntry(int x, int y, int w, int h, int textSize, int textAlign, color textColour, color boxColour, color borderColour, String allowedChars){
    this.x = x;
    this.y = y;
    this.h = h;
    this.w = w;
    this.textColour = textColour;
    this.textSize = textSize;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    this.borderColour = borderColour;
    this.allowedChars = allowedChars;
    text = new StringBuilder();
  }
  
  void draw(int xOffset, int yOffest){
    boolean showCursor = (millis()/BLINKTIME)%2==0 || keyPressed;
    pushStyle();
    
    // Draw a box behind the text
    fill(boxColour);
    stroke(borderColour);
    rect(x, y, w, h);
    
    // Draw the text
    textSize(textSize);
    textAlign(textAlign);
    fill(textColour);
    text(text.toString(), x, y+textSize);
    
    // Draw cursor
    if (showCursor){
      fill(0);
      rect(x+textWidth(text.toString().substring(0,cursor)), y, 1, textSize);
    }
    
    popStyle();
  }
  
  void resetSelection(){
    selected = cursor;
  }
  
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
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
          text = new StringBuilder(text.substring(min(cursor, selected)) + text.substring(max(cursor, selected)));
        }
      }
      else if (allowedChars.equals("") || allowedChars.contains(""+_key)){
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
}