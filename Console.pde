class Console extends Element{
  private int textSize, cursorX, maxLength=-1;
  private ArrayList<StringBuilder> text;
  private boolean drawCursor = false;
  private String allowedChars;
  
  Console(int x, int y, int w, int h, int textSize){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.textSize = textSize;
    text = new ArrayList<StringBuilder>();
    text.add(new StringBuilder(" > "));
    cursorX = 3;
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
        getInputString(t).append(c);
      }
    }
    return t;
  }
  
  void draw(PGraphics canvas){
    canvas.pushStyle();
    float ts = textSize*jsManager.loadFloatSetting("text scale");
    canvas.textSize(ts);
    canvas.textAlign(LEFT, BOTTOM);
    int time = millis();
    drawCursor = (time/500)%2==0 || keyPressed;
    canvas.fill(255);
    for (int i=0; i < text.size(); i++){
      canvas.text(""+text.get(i), x, height/2-((text.size()-i-1)*ts));
    }
    if (drawCursor){
      canvas.stroke(255);
      canvas.rect(x+canvas.textWidth(getInputString().substring(0, cursorX)), y+height/2-ts, 1, ts*1.2);
    }
    canvas.popStyle();
  }
  StringBuilder getInputString(){
    return getInputString(text);
  }
  StringBuilder getInputString(ArrayList<StringBuilder> t){
    return t.get(t.size()-1);
  }
  
  int cxyToC(int cx, int cy){
    int a=0;
    for (int i=0; i<cy; i++){
      a += text.get(i).length()+1;
    }
    return a + cx;
  }
  
  int getCurX(int cy){
    int i=0;
    float ts = textSize*jsManager.loadFloatSetting("text scale");
    textSize(ts);
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
      cursorX = getCurX(text.size()-1);
    }
    return events;
  }
  
  void clearTextAt(){
    StringBuilder s = toStr();
    String s2 = s.substring(0, cxyToC(cursorX-1, text.size()-1)) + (s.substring(cxyToC(cursorX, text.size()-1), s.length()));
    text = strToText(s2);
  }
  
  
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    if (eventType == "keyTyped"){
      if (maxLength == -1 || this.toStr().length() < maxLength){
        if (_key == '\n'){
          //clearTextAt();
          text.add(text.size(), new StringBuilder(getInputString().substring(cursorX, getInputString().length())));
          getInputString().replace(cursorX, text.get(text.size()-1).length(), "");
          cursorX=3;
        }
        else if (_key == '\t'){
          for (int i=0; i<4-cursorX%4; i++){
            getInputString().insert(cursorX, " ");
          }
          cursorX += 4-cursorX%4;
        }
        else if(_key != 0 && (allowedChars == null || allowedChars.indexOf(_key) != -1)){
          //clearTextAt();
          getInputString().insert(cursorX, _key);
          cursorX++;
        }
      }
    }
    if (eventType == "keyPressed"){
      if (_key == CODED){
        if (keyCode == LEFT){
          cursorX = max(cursorX-1, 3);
        }
        if (keyCode == RIGHT){
          cursorX = min(cursorX+1, getInputString().length());
        }
        //if (keyCode == UP){
        //  cursorY = max(cursorY-1, 0);
        //  cursorX = min(cursorX, text.get(cursorY).length());
        //}
        //if(keyCode == DOWN){
        //  cursorY = min(cursorY+1, text.size()-1);
        //  cursorX = min(cursorX, text.get(cursorY).length());
        //}
        //if (keyCode == SHIFT){
        //  lshed = true;
        //}
      }
      if(_key == ENTER){
        text.add(text.size(), new StringBuilder(" > "));
        cursorX=3;
      }
      if(_key == VK_BACK_SPACE&&cursorX>3){
        clearTextAt();
        cursorX--;
      }
      if(keyCode == VK_DELETE&&cursorX<getInputString().length()){
        cursorX++;
        clearTextAt();
        cursorX--;
      }
    }
    //if (eventType == "keyReleased"){
    //  if(key == CODED){
    //    if (keyCode == SHIFT){
    //      lshed = false;
    //    }
    //  }
    //}
    return new ArrayList<String>();
  }
}
