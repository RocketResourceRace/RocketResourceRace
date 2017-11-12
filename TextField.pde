
class TextField extends Element{
  private int x, y, w, h, textSize, textAlign, cursorX, cursorY, active, maxLength=-1, selectedX = 0, selectedY = 0;
  private color textColour, boxColour;
  private ArrayList<StringBuilder> text;
  private boolean drawCursor = false, pressed=false, lshed=false;
  private String allowedChars;
  
  TextField(int x, int y, int w, int h, color textColour, int textSize, int textAlign, color boxColour){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.textSize = textSize;
    this.textColour = textColour;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    text = new ArrayList<StringBuilder>();
    text.add(new StringBuilder());
  }
  TextField(int x, int y, int w, int h, color textColour, int textSize, int textAlign, color boxColour, String allowedChars, int maxLength){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.textSize = textSize;
    this.textColour = textColour;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    text = new ArrayList<StringBuilder>();
    text.add(new StringBuilder());
    this.allowedChars = allowedChars;
    this.maxLength = maxLength;
  }

  StringBuilder toStr(){
    StringBuilder s = new StringBuilder();
    for (StringBuilder s1 : text){
      s.append(s1+"\n");
    }
    s.deleteCharAt(s.length()-1);
    return s;
  }
  
  ArrayList<StringBuilder> strToText(String s){
    ArrayList<StringBuilder> t = new ArrayList<StringBuilder>();
    t.add(new StringBuilder());
    char c;
    for (int i=0; i<s.length(); i++){
      c = s.charAt(i);
      if (c == '\n'){
        t.add(new StringBuilder());
      }
      else{
        t.get(t.size()-1).append(c);
      }
    }
    return t;
  }
  
  void draw(int xOffset, int yOffest){
    pushStyle();
    textSize(textSize);
    int time = millis();
    int sx, sy, ex, ey;
    if (selectedY < cursorY){
      sy = selectedY;
      ey = cursorY;
      sx = selectedX;
      ex = cursorX;
    }
    else{
      sy = cursorY;
      ey = selectedY;
      sx = cursorX;
      ex = selectedX;
    }
    drawCursor = (time/500)%2==0 || keyPressed;
    if (selectedX >= 0 && selectedY >= 0){
      fill(80, 50, 230);
      if (sy == ey){
        rect(x+textWidth(text.get(sy).substring(0, min(sx, ex)).toString()), y+textSize*(sy-1), textWidth(text.get(sy).substring(min(sx, ex), max(sx, ex)).toString()), textSize);
      }
      else{
        rect(x+textWidth(text.get(sy).substring(0, sx).toString()), y+textSize*(sy-1), textWidth(text.get(sy).toString())-textWidth(text.get(sy).substring(0, sx).toString()), textSize);
      }
      for (int i=sy+1; i<ey; i++){
        if (i < ey){
          rect(x, y+textSize*(i-1), textWidth(text.get(i).toString()), textSize);
        }
        else if (i == ey){
          rect(x, y+textSize*(i-1), textWidth(text.get(i).substring(0, ex).toString()), textSize);
        }
        else{
          break;
        }
      }
    }
    fill(textColour);
    if (textAlign == LEFT){
      for (int i=0; i < text.size(); i++){
        text(""+text.get(i), x, y+textSize*i);
      }
    }
    if (drawCursor){
      fill(0);
      rect(x+textWidth(text.get(cursorY).substring(0, cursorX)), y+textSize*(cursorY-1), 1, textSize*1.2);
    }
    popStyle();
  }
  
  int cxyToC(int cx, int cy){
    int a=0;
    for (int i=0; i<cy; i++){
      a += text.get(i).length();
    }
    return a + cx;
  }
  
  int getCurY(){
    int a = round((float)(mouseY - y)/textSize);
    if (0 <= a && a < text.size()){
      return a;
    }
    return cursorY;
  }
  
  int getCurX(int cy){
    int i=0;
    for(; i<text.get(cy).length(); i++){
      if (textWidth(text.get(cy).substring(0, i)) + x > mouseX)
        break;
    }
    if (0 <= i && i <= text.get(cy).length()){
      return i;
    }
    return cursorX;
  }
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mousePressed" && button == LEFT){
      cursorY = getCurY();
      cursorX = getCurX(cursorY);
      selectedY = cursorY;
      selectedX = cursorX;
      pressed = true;
    }
    else if (eventType == "mouseReleased" && button == LEFT){
      pressed = false;
    }
    if (eventType == "mouseDragged" && pressed){
      selectedY = getCurY();
      selectedX = getCurX(selectedY);
    }
    return events;
  }
  
  void clearTextAt(){
    if (selectedX == cursorX && selectedY == cursorY){
      return;
    }
    StringBuilder s = toStr();
    int sx, sy, ex, ey;
    if (selectedY < cursorY){
      sy = selectedY;
      ey = cursorY;
      sx = selectedX;
      ex = cursorX;
    }
    else if(selectedY > cursorY){
      sy = cursorY;
      ey = selectedY;
      sx = cursorX;
      ex = selectedX;
    }
    else{
      sy = cursorY;
      ey = selectedY;
      sx = max(selectedX, cursorX);
      ex = min(selectedX, cursorX);
    }
    String s2 = s.substring(0, cxyToC(ex-ey+2, ey)) + (s.substring(cxyToC(sx+1, sy), s.length()));
    text = strToText(s2);
    cursorX = min(cursorX, selectedX);
    cursorY = min(cursorY, selectedY);
  }
  
  void resetSelection(){
    selectedX = cursorX;
    selectedY = cursorY;
  }
  
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    if (eventType == "keyTyped"){
      if (_key == BACKSPACE){
        if (selectedX == cursorX && selectedY == cursorY){
          if (cursorX > 0){
            text.get(cursorY).deleteCharAt(cursorX-1);
            cursorX--;
            selectedX--;
          }
          else if(cursorY > 0){
            cursorX = text.get(cursorY-1).length();
            text.get(cursorY-1).append(text.get(cursorY));
            text.remove(cursorY);
            cursorY--;
          }
        }
        else{
          clearTextAt();
        }
        resetSelection();
      }
      else if (maxLength == -1 || this.toStr().length() < maxLength){
        if (_key == '\n'){
          //clearTextAt();
          text.add(cursorY+1, new StringBuilder(text.get(cursorY).substring(cursorX, text.get(cursorY).length())));
          text.get(cursorY).replace(cursorX, text.get(cursorY).length(), "");
          cursorY++;
          cursorX=0;
        }
        else if (_key == '\t'){
          for (int i=0; i<4-cursorX%4; i++){
            text.get(cursorY).insert(cursorX, " ");
          }
          cursorX += 4-cursorX%4;
        }
        else if(allowedChars == null || allowedChars.indexOf(_key) != -1){
          resetSelection();
          clearTextAt();
          text.get(cursorY).insert(cursorX, _key);
          cursorX++;
        }
        resetSelection();
      }
    }
    if(eventType == "keyPressed"){
      if (key == CODED){
        if (keyCode == LEFT){
          if (cursorX == 0 && cursorY > 0){
            cursorY --;
            cursorX = text.get(cursorY).length();
          }
          else
          cursorX = max(cursorX-1, 0);
        }
        if (keyCode == RIGHT){
          if (cursorY < text.size()-1 && cursorX == text.get(cursorY).length()){
            cursorY ++;
            cursorX = 0;
          }
          else
          cursorX = min(cursorX+1, text.get(cursorY).length());
        }
        if (keyCode == UP){
          cursorY = max(cursorY-1, 0);
          cursorX = min(cursorX, text.get(cursorY).length());
        }
        if(keyCode == DOWN){
          cursorY = min(cursorY+1, text.size()-1);
          cursorX = min(cursorX, text.get(cursorY).length());
        }
        if (keyCode == SHIFT){
          lshed = true;
        }
        if ((keyCode == DOWN || keyCode == UP || keyCode == LEFT || keyCode == RIGHT) && !lshed){
          selectedX = cursorX;
          selectedY = cursorY;
        }
      }
    }
    if (eventType == "keyReleased"){
      if(key == CODED){
        if (keyCode == SHIFT){
          lshed = false;
        }
      }
    }
    return new ArrayList<String>();
  }
}