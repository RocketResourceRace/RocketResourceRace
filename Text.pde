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
