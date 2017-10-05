
class TextField extends Element{
  private int x, y, w, h, textSize, textAlign, cursor, active;
  private color textColour, boxColour;
  private StringBuilder text;
  
  TextField(int x, int y, int w, int h, color textColour, int textSize, int textAlign, color boxColour){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.textSize = textSize;
    this.textColour = textColour;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    text = new StringBuilder();
  }

  ArrayList<String> getLines(StringBuilder s){
    ArrayList<String> lines = new ArrayList<String>();
    int j=0, i=0;
    for (; i < s.length(); i++){
      if (s.charAt(i) == '\n'){
        lines.add(s.substring(j, i));
        j=i;
      }
    }
    lines.add(s.substring(j, i));
    return lines;
  }
  
  void draw(int xOffset, int yOffest){
    int time = millis();
    if ((time/500)%2==0){
      text.insert(cursor, "|");
    }
    ArrayList<String> lines = getLines(text);
    fill(textColour);
    if (textAlign == LEFT){
      for (int i=0; i < lines.size(); i++){
        text(lines.get(i), x, y+textSize*i);
      }
    }
    if ((time/500)%2==0){
      text.deleteCharAt(cursor);
    }
  }
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    if (eventType == "keyTyped"){
      if (_key == BACKSPACE){
        if (text.length() > 0)
          text.deleteCharAt(--cursor);
      }
      else{
        text.insert(cursor, _key);
        cursor++;
      }
    }
    if(eventType == "keyPressed"){
      if (key == CODED){
        if (keyCode == LEFT){
          cursor = max(cursor-1, 0);
        }
        if (keyCode == RIGHT){
          cursor = min(cursor+1, text.length());
        }
      }
    }
    return new ArrayList<String>();
  }
}