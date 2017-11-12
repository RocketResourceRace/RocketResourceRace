class Text extends Element{
  int x, y, size;
  String text;
  Text(int x, int y,  int size, String text){
    this.x = x;
    this.y = y;
    this.size = size;
    this.text = text;
  }
  void setText(String text){
    this.text = text;
  }
}