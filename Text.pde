class Text extends Element{
  int x, y, size;
  PFont font;
  String text;
  Text(int x, int y,  int size, String text, PFont font){
    this.x = x;
    this.y = y;
    this.size = size;
    this.text = text;
  }
  void setText(String text){
    this.text = text;
  }
  void draw(){
    if (font != null){
      textFont(font);
    }
    textSize(size);
    text(text, x, y);
  }
}