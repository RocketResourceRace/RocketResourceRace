
class Slider extends Element{
  private int x, y, w, h, cx, cy;
  private float  major, minor, upper, lower, step;
  private color bgColour, strokeColour;
  private boolean horizontal;
  
  Slider(int x, int y, int w, int h, color bgColour, color strokeColour, float lower, float upper, float major, float minor, float step, boolean horizontal){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.major = major;
    this.minor = minor;
    this.upper = upper;
    this.lower = lower;
    this.horizontal = horizontal;
    this.step = step;
  }
  
  void draw(int xOffset, int yOffset){
    float j = lower, range = upper-lower;
    fill(bgColour);
    stroke(strokeColour);
    //rect(x, y, w, h);
    while(j<=upper){
      line(j/range*w+x, y+h/4, j/range*w+x, y+h-h/4);
      j += range/minor;
    }
    j=lower;
    while(j<=upper){
      line(j/range*w+x, y, j/range*w+x, y+h);
      j += range/major;
    }
  }
}