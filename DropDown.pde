
class DropDown extends Element{
  ArrayList<String> options;
  ArrayList<Integer> availableOptions;
  int selectedOption, textSize;
  boolean dropped;
  color bgColour, strokeColour;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  DropDown(int x, int y, int w, int textSize, color bgColour, color strokeColour, String[] options){
    this.x = x;
    this.y = y;
    this.w = w;
    this.textSize = textSize;
    this.h = getH();
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    removeAllOptions();
    selectedOption=0;
    for (String option : options){
      this.options.add(option);
    }
    dropped = true;
    resetAvailable();
  }
  void setOptions(ArrayList<String> options){
    this.options = options;
  }
  void addOption(String option){
    this.options.add(option);
  }
  void removeOption(String option){
    for (int i=0; i <options.size(); i++){
      if (option.equals(options.get(i))){
        options.remove(i);
      }
    }
  }
  void removeAllOptions(){
    this.options = new ArrayList<String>();
  }
  void resetAvailable(){
    this.availableOptions = new ArrayList<Integer>();
  }
  void makeAvailable(String option){
    for (int i=0; i<options.size(); i++){
      if (options.get(i).equals(option)){
        this.availableOptions.add(i);
      }
    }
  }
  int getH(){
    textSize(textSize*TextScale);
    return ceil(textAscent() + textDescent());
  }
  boolean optionAvailable(int i){
    for (int option : availableOptions){
      if(option == i){
        return true;
      }
    }
    return false;
  }
  void draw(){
    pushStyle();
    h = getH();
    fill(brighten(bgColour, ONOFFSET));
    stroke(strokeColour);
    rect(x+xOffset, y+yOffset, w, h);
    fill(0);
    textAlign(LEFT, TOP);
    text("Current Task: "+options.get(selectedOption), x+xOffset+5, y+yOffset);
    
    if (dropped){
      int i=0;
      int j=1;
      while (i < options.size()){
        if (i!=selectedOption && optionAvailable(i)){
          if (mouseOver(j)){
            fill(brighten(bgColour, HOVERINGOFFSET));
          }
          else{
            fill(bgColour);
          }
          rect(x+xOffset, y+yOffset+h*j, w, h);
          fill(0);
          text(options.get(i), x+xOffset+5, y+yOffset+h*j);
          j++;
        }
        i++;
      }
    }
    popStyle();
  }
  boolean moveOver(){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+yOffset && mouseY <= y+h*availableOptions.size()+yOffset;
  }
  boolean mouseOver(int j){
    return mouseX >= x+xOffset && mouseX <= x+w+xOffset && mouseY >= y+h*j+yOffset && mouseY <= y+h*(j+1)+yOffset;
  }
}