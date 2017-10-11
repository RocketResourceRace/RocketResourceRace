
class TextField extends Element{
  private int x, y, w, h, textSize, textAlign, cursorX, cursorY, active;
  private color textColour, boxColour;
  private ArrayList<StringBuilder> text;
  private boolean drawCursor = false;
  
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

  ArrayList<String> getLines(StringBuilder s){
    ArrayList<String> lines = new ArrayList<String>();
    int j=0, i=0;
    for (; i < s.length(); i++){
      if (s.charAt(i) == '\n'){
        lines.add(s.substring(j, i));
        j=i+1;
      }
    }
    lines.add(s.substring(j, i));
    return lines;
  }
  
  void draw(int xOffset, int yOffest){
    pushStyle();
    textSize(textSize);
    int time = millis();
    drawCursor = (time/500)%2==0;
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
    //fill(0);
    //println(cursorToX(cursor), cursorToY(cursor));
    //line(0, 0, 100, 100);
    //line(cursorToX(cursor), cursorToY(cursor), cursorToX(cursor), cursorToY(cursor)+textSize);
    popStyle();
  }
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    if (eventType == "keyTyped"){
      if (_key == BACKSPACE){
        if (cursorX > 0){
          text.get(cursorY).deleteCharAt(--cursorX);
        }
        else if (cursorY > 0){
          cursorX = text.get(cursorY-1).length();
          text.get(cursorY-1).append(text.get(cursorY));
          text.remove(cursorY);
          cursorY--;
        }
      }
      else if (_key == '\n'){
        text.add(cursorY+1, new StringBuilder(text.get(cursorY).substring(cursorX, text.get(cursorY).length())));
        text.get(cursorY).replace(cursorX, text.get(cursorY).length(), "");
        cursorY++;
        cursorX=0;
      }
      else{
        text.get(cursorY).insert(cursorX, _key);
        cursorX++;
      }
    }
    if(eventType == "keyPressed"){
      if (key == CODED){
        if (keyCode == LEFT){
          cursorX = max(cursorX-1, 0);
        }
        if (keyCode == RIGHT){
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
      }
    }
    return new ArrayList<String>();
  }
}