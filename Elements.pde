

class EquipmentManager extends Element {
  final int TEXTSIZE = 7;
  final float BOXWIDTHHEIGHTRATIO = 0.75;
  String[] equipmentClassDisplayNames;
  int[] currentEquipment;
  float boxWidth, boxHeight, dropBoxHeight;
  int selectedClass;

  EquipmentManager(int x, int y, int w) {
    this.x = x;
    this.y = y;
    this.w = w;

    // Load display names for equipment classes
    equipmentClassDisplayNames = new String[jsManager.getNumEquipmentClasses()];
    for (int i = 0; i < equipmentClassDisplayNames.length; i ++) {
      equipmentClassDisplayNames[i] = jsManager.getEquipmentClassDisplayName(i);
    }

    currentEquipment = new int[jsManager.getNumEquipmentClasses()];

    updateSizes();
    
    selectedClass = -1;  // -1 represents nothing being selected
  }
  
  void updateSizes(){

    boxWidth = w/jsManager.getNumEquipmentClasses();
    boxHeight = boxWidth*BOXWIDTHHEIGHTRATIO;
    dropBoxHeight = jsManager.loadFloatSetting("text scale") * TEXTSIZE * 1.2;
  }

  void transform(int x, int y, int w) {
    this.x = x;
    this.y = y;
    this.w = w;
    
    updateSizes();
  }

  void setEquipment(int[] equipment) {
    this.currentEquipment = equipment;
  }
  
  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")) {
      if (mouseOverClasses()) {
        int newSelectedClass = hoveringOverClass();
        if (newSelectedClass == selectedClass){  // If selecting same option
          selectedClass = -1;
        }
        else{
          selectedClass = newSelectedClass;
        }
      }
      else{
        selectedClass = -1;
      }
    }
    return events;
  }

  void draw(PGraphics panelCanvas) {
    panelCanvas.pushStyle();

    panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER, TOP);
    panelCanvas.strokeWeight(2);
    panelCanvas.fill(170);
    panelCanvas.rect(x, y, w, boxHeight);
    for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
      if (selectedClass == i){
        panelCanvas.strokeWeight(3);
        panelCanvas.fill(140);
      }
      else{
        panelCanvas.strokeWeight(1);
        panelCanvas.noFill();
      }
      panelCanvas.rect(x+boxWidth*i, y, boxWidth, boxHeight);
      panelCanvas.fill(0);
      panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5), y);
    }
    
    // Draw dropdown if an equipment class is selected
    if (selectedClass != -1){
      panelCanvas.textAlign(LEFT, TOP);
      String[] equipmentTypes = jsManager.getEquipmentFromClass(selectedClass);
      for (int i = 0; i < jsManager.getNumEquipmentTypesFromClass(selectedClass); i ++){
        panelCanvas.strokeWeight(1);
        panelCanvas.fill(170);
        panelCanvas.rect(x+selectedClass*boxWidth, y+i*dropBoxHeight+boxHeight, boxWidth, dropBoxHeight);
        panelCanvas.fill(0);
        panelCanvas.text(equipmentTypes[i], x+selectedClass*boxWidth, y+i*dropBoxHeight+boxHeight);
      }
    }

    panelCanvas.popStyle();
  }
  
  boolean mouseOverClasses() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+boxHeight;
  }
  
  int hoveringOverClass() {
    for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
      if (mouseX-xOffset >= x+boxWidth*i && mouseX-xOffset <= x+boxWidth*(i+1) && mouseY-yOffset >= y && mouseY-yOffset <= y+boxHeight){
        return i;
      }
    }
    return -1;
  }
}

class ProficiencySummary extends Element {
  final int TEXTSIZE = 8;
  String[] proficiencyDisplayNames;
  float[] proficiencies;
  int rowHeight;

  ProficiencySummary(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    updateProficiencyDisplayNames();
    proficiencies = new float[jsManager.getNumProficiencies()];
  }

  void transform(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    updateProficiencyDisplayNames();
    updateRowHeight();
  }

  void setProficiencies(float[] proficiencies) {
    this.proficiencies = proficiencies;
  }

  void updateProficiencyDisplayNames() {
    proficiencyDisplayNames = new String[jsManager.getNumProficiencies()];
    for (int i = 0; i < jsManager.getNumProficiencies(); i ++) {
      proficiencyDisplayNames[i] = jsManager.indexToProficiencyDisplayName(i);
    }
    LOGGER_MAIN.finer("Updated proficiency display names to: "+Arrays.toString(proficiencyDisplayNames));
  }

  void updateRowHeight() {
    rowHeight = h/proficiencyDisplayNames.length;
  }

  void draw(PGraphics panelCanvas) {
    panelCanvas.pushStyle();

    //Draw background
    panelCanvas.strokeWeight(2);
    panelCanvas.fill(150);
    panelCanvas.rect(x, y, w, h); // Added and subtracted values are for stroke to line up well with other boxes

    //Draw each proficiency box
    panelCanvas.strokeWeight(1);
    for (int i = 0; i < proficiencyDisplayNames.length; i ++) {
      panelCanvas.noFill();
      panelCanvas.line(x, y+rowHeight*i, x+w, y+rowHeight*i);
      panelCanvas.fill(0);
      panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(LEFT, CENTER);
      panelCanvas.text(proficiencyDisplayNames[i], x+5, y+rowHeight*(i+0.5)); // Display name aligned left, middle height within row
      panelCanvas.textAlign(RIGHT, CENTER);
      panelCanvas.text(proficiencies[i], x+w-5, y+rowHeight*(i+0.5)); // Display name aligned right, middle height within row
    }

    panelCanvas.popStyle();
  }
}

class BaseFileManager extends Element {
  // Basic file manager that scans a folder and makes a selectable list for all the files
  final int TEXTSIZE = 14, SCROLLWIDTH = 30;
  String folderString;
  String[] saveNames;
  int selected, rowHeight, numDisplayed, scroll;
  boolean scrolling;
  float fakeHeight;


  BaseFileManager(int x, int y, int w, int h, String folderString) {
    super.x = x;
    super.y = y;
    super.w = w;
    super.h = h;
    this.folderString = folderString;
    saveNames = new String[0];
    selected = 0;
    rowHeight = ceil(TEXTSIZE * jsManager.loadFloatSetting("text scale"))+5;
    scroll = 0;
    scrolling = false;
    updateFakeHeight();
  }

  void updateFakeHeight() {
    fakeHeight = rowHeight * numDisplayed;
  }

  String getNextAutoName() {
    // Find the next automatic name for save
    loadSaveNames();
    int mx = 1;
    for (int i=0; i<saveNames.length; i++) {
      if (saveNames[i].length() > 8) {// 'Untitled is 8 characters
        if (saveNames[i].substring(0, 8).equals("Untitled")) {
          try {
            mx = max(mx, Integer.parseInt(saveNames[i].substring(8, saveNames[i].length())));
          }
          catch(NumberFormatException e) {
            LOGGER_MAIN.log(Level.WARNING, "Save name confusing becuase in autogen format", e);
          }
          catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "An error occured with finding autogen name", e);
            throw e;
          }
        }
      }
    }
    String name = "Untitled"+(mx+1);
    LOGGER_MAIN.info("Created autogenerated file name: " + name);
    return name;
  }

  void loadSaveNames() {
    try {
      File dir = new File(sketchPath("saves"));
      if (!dir.exists()) {
        LOGGER_MAIN.info("Creating new 'saves' directory");
        dir.mkdir();
      }
      saveNames = dir.list();
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Files scanning failed", e);
    }
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    int d = saveNames.length - numDisplayed;
    if (eventType.equals("mouseClicked")) {
      if (moveOver()) {
        if (d <= 0 || mouseX-xOffset<x+w-SCROLLWIDTH) {
          // If not hovering over scroll bar, then select item
          selected = hoveringOption();
          events.add("valueChanged");
          scrolling = false;
        }
      }
    } else if (eventType.equals("mousePressed")) {
      if (d > 0 && moveOver() && mouseX-xOffset>x+w-SCROLLWIDTH) {  
        // If hovering over scroll bar, set scroll to mouse pos
        scrolling = true;
        scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/h, d));
      } else {
        scrolling = false;
      }
    } else if (eventType.equals("mouseDragged")) {
      if (scrolling && d > 0) {  
        // If scrolling, set scroll to mouse pos
        scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/h, d));
      }
    } else if (eventType.equals("mouseReleased")) {
      scrolling = false;
    }

    return events;
  }

  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseWheel") {
      float count = event.getCount();
      if (moveOver()) { // Check mouse over element
        if (saveNames.length > numDisplayed) {
          scroll = round(between(0, scroll+count, saveNames.length-numDisplayed));
          LOGGER_MAIN.finest("Changing scroll to: "+scroll);
        }
      }
    }
    return events;
  }

  String selectedSaveName() {
    if (saveNames.length == 0) {
      return "Untitled";
    } else if (saveNames.length <= selected) {
      LOGGER_MAIN.severe("Selected name is out of range " + selected);
    }
    LOGGER_MAIN.info("Selected save name is : " + saveNames[selected]);
    return saveNames[selected];
  }

  void draw(PGraphics panelCanvas) {

    rowHeight = ceil(TEXTSIZE * jsManager.loadFloatSetting("text scale"))+5;
    updateFakeHeight();

    numDisplayed = ceil(h/rowHeight);
    panelCanvas.pushStyle();

    panelCanvas.textSize(TEXTSIZE * jsManager.loadFloatSetting("text scale"));
    panelCanvas.textAlign(LEFT, TOP);
    for (int i=scroll; i<min(numDisplayed+scroll, saveNames.length); i++) {
      if (selected == i) {
        panelCanvas.strokeWeight(2);
        panelCanvas.fill(color(100));
      } else {
        panelCanvas.strokeWeight(1);
        panelCanvas.fill(color(150));
      }
      panelCanvas.rect(x, y+rowHeight*(i-scroll), w, rowHeight);
      panelCanvas.fill(0);
      panelCanvas.text(saveNames[i], x, y+rowHeight*(i-scroll));
    }

    // Draw the scroll bar
    panelCanvas.strokeWeight(2);
    int d = saveNames.length - numDisplayed;
    if (d > 0) {
      panelCanvas.fill(120);
      panelCanvas.rect(x+w-SCROLLWIDTH, y, SCROLLWIDTH, fakeHeight);
      if (scrolling) {
        panelCanvas.fill(40);
      } else {
        panelCanvas.fill(70);
      }
      panelCanvas.stroke(0);
      panelCanvas.rect(x+w-SCROLLWIDTH, y+(fakeHeight-fakeHeight/(d+1))*scroll/d, SCROLLWIDTH, fakeHeight/(d+1));
    }

    panelCanvas.popStyle();
  }

  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+fakeHeight;
  }

  int hoveringOption() {
    int s = (mouseY-yOffset-y)/rowHeight;
    if (!(0 <= s && s < numDisplayed)) {
      return selected;
    }
    return s+scroll;
  }
}




class DropDown extends Element {
  String[] options;  // Either strings or floats
  int selected, bgColour, textSize;
  String name, optionTypes;
  boolean expanded, postExpandedEvent;

  DropDown(int x, int y, int w, int h, int bgColour, String name, String optionTypes, int textSize) {
    // h here means the height of one dropper box
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.name = name;
    this.expanded = false;
    this.optionTypes = optionTypes;
    this.textSize = textSize;
  }

  void setOptions(String[] options) {
    this.options = options;
    LOGGER_MAIN.finer("Options changed to: " + Arrays.toString(options));
  }

  void setValue(String value) {
    for (int i=0; i < options.length; i++) {
      if (value.equals(options[i])) {
        selected = i;
        return;
      }
    }
    LOGGER_MAIN.warning("Invalid value, "+ value);
  }

  void draw(PGraphics panelCanvas) {
    int hovering = hoveringOption();
    panelCanvas.pushStyle();

    // draw selected option
    panelCanvas.stroke(color(0));
    if (moveOver() && hovering == -1) {
      panelCanvas.fill(brighten(bgColour, -20));
    } else {
      panelCanvas.fill(brighten(bgColour, -40));
    }
    panelCanvas.rect(x, y, w, h);
    panelCanvas.textAlign(LEFT, TOP);
    panelCanvas.textFont(getFont((min(h*0.8, textSize))*jsManager.loadFloatSetting("text scale")));
    panelCanvas.fill(color(0));
    panelCanvas.text(String.format("%s: %s", name, options[selected]), x+3, y);

    // Draw expand box
    if (expanded) {
      panelCanvas.line(x+w-h, y+1, x+w-h/2, y+h-1);
      panelCanvas.line(x+w-h/2, y+h-1, x+w, y+1);
    } else {
      panelCanvas.line(x+w-h, y+h-1, x+w-h/2, y+1);
      panelCanvas.line(x+w-h/2, y+1, x+w, y+h-1);
    }

    // Draw other options
    if (expanded) {
      for (int i=0; i < options.length; i++) {
        if (i == selected) {
          panelCanvas.fill(brighten(bgColour, 50));
        } else {
          if (moveOver() && i == hovering) {
            panelCanvas.fill(brighten(bgColour, 20));
          } else {
            panelCanvas.fill(bgColour);
          }
        }
        panelCanvas.rect(x, y+(i+1)*h, w, h);
        if (i == selected) {
          panelCanvas.fill(brighten(bgColour, 20));
        } else {
          panelCanvas.fill(0);
        }
        panelCanvas.text(options[i], x+3, y+(i+1)*h);
      }
    }

    panelCanvas.popStyle();
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")) {
      int hovering = hoveringOption();
      if (moveOver()) {
        if (hovering == -1) {
          toggleExpanded();
        } else {
          events.add("valueChanged");
          selected = hovering;
          contract();
          events.add("stop events");
        }
      } else {
        contract();
      }
    }
    if (postExpandedEvent) {
      events.add("element to top");
      postExpandedEvent = false;
    }
    return events;
  }

  void setSelected(String s) {
    for (int i=0; i < options.length; i++) {
      if (options[i].equals(s)) {
        selected = i;
        return;
      }
    }
    LOGGER_MAIN.warning("Invalid selected:"+s);
  }

  void contract() {
    expanded = false;
  }

  void expand() {
    postExpandedEvent = true;
    expanded = true;
  }

  void toggleExpanded() {
    expanded = !expanded;
    if (expanded) {
      postExpandedEvent = true;
    }
  }

  int getIntVal() {
    try {
      int val = Integer.parseInt(options[selected]);
      LOGGER_MAIN.finer("Value of dropdown "+ val);
      return val;
    }
    catch(IndexOutOfBoundsException e) {
      LOGGER_MAIN.severe("Selected option is out of bounds of dropbox " + selected);
      return -1;
    }
  }

  String getStrVal() {
    try {
      String val = options[selected];
      LOGGER_MAIN.finer("Value of dropdown "+ val);
      return val;
    }
    catch(IndexOutOfBoundsException e) {
      LOGGER_MAIN.severe("Selected option is out of bounds of dropbox " + selected);
      return "";
    }
  }

  float getFloatVal() {
    try {
      float val = Float.parseFloat(options[selected]);
      LOGGER_MAIN.finer("Value of dropdown "+ val);
      return val;
    }
    catch(IndexOutOfBoundsException e) {
      LOGGER_MAIN.severe("Selected option is out of bounds of dropbox " + selected);
      return -1;
    }
  }

  int getOptionIndex() {
    return selected;
  }
  
  boolean moveOver() {
    if (expanded) {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset < y+h*(options.length+1);
    } else {
      return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset < y+h;
    }
  }

  int hoveringOption() {
    if (!expanded) {
      return -1;
    }
    return (mouseY-yOffset-y)/h-1;
  }
}


class Tickbox extends Element {
  boolean val;
  String name;

  Tickbox(int x, int y, int w, int h, boolean defaultVal, String name) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.val = defaultVal;
    this.name = name;
  }

  void toggle() {
    val = !val;
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")) {
      if (moveOver()) {
        toggle();
        events.add("valueChanged");
      }
    }
    return events;
  }

  boolean getState() {
    return val;
  }
  void setState(boolean state) {
    LOGGER_MAIN.finer("Tickbox state changed to: "+ state);
    val = state;
  }

  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+h && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }

  void draw(PGraphics panelCanvas) {
    panelCanvas.pushStyle();

    panelCanvas.fill(color(255));
    panelCanvas.stroke(color(0));
    panelCanvas.rect(x, y, h*jsManager.loadFloatSetting("gui scale"), h*jsManager.loadFloatSetting("gui scale"));
    if (val) {
      panelCanvas.line(x+1, y+1, x+h*jsManager.loadFloatSetting("gui scale")-1, y+h*jsManager.loadFloatSetting("gui scale")-1);
      panelCanvas.line(x+h*jsManager.loadFloatSetting("gui scale")-1, y+1, x+1, y+h*jsManager.loadFloatSetting("gui scale")-1);
    }
    panelCanvas.fill(0);
    panelCanvas.textAlign(LEFT, CENTER);
    panelCanvas.textSize(8*jsManager.loadFloatSetting("text scale"));
    panelCanvas.text(name, x+h*jsManager.loadFloatSetting("gui scale")+5, y+h*jsManager.loadFloatSetting("gui scale")/2);
    panelCanvas.popStyle();
  }
}

class Tooltip extends Element {
  boolean visible;
  String text;
  boolean attacking;

  Tooltip() {
    hide();
    setText("");
  }

  void show() {
    visible = true;
  }
  void hide() {
    visible = false;
  }

  ArrayList<String> getLines(String s) {
    try {
      int j = 0;
      ArrayList<String> lines = new ArrayList<String>();
      for (int i=0; i<s.length(); i++) {
        if (s.charAt(i) == '\n') {
          lines.add(s.substring(j, i));
          j=i+1;
        }
      }
      lines.add(s.substring(j, s.length()));
      return lines;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error occured getting lines of tooltip in: "+s, e);
      throw e;
    }
  }

  float maxWidthLine(ArrayList<String> lines) {
    float ml = 0;
    for (int i=0; i<lines.size(); i++) {
      if (textWidth(lines.get(i)) > ml) {
        ml = textWidth(lines.get(i));
      }
    }
    return ml;
  }
  void setText(String text) {
    if (!text.equals(this.text)) {
      LOGGER_MAIN.finest(String.format("Tooltip text changing to: '%s'", text.replace("\n", "\\n")));
    }
    this.text = text;
  }
  //String resourcesList(float[] resources){
  //  String returnString = "";
  //  boolean notNothing = false;
  //  for (int i=0; i<numResources;i++){
  //    if (resources[i]>0){
  //      returnString += roundDp(""+resources[i], 1)+ " " +resourceNames[i]+ ", "; 
  //      notNothing = true;
  //    }
  //  }
  //  if (!notNothing)
  //    returnString += "Nothing/Unknown";
  //  else if(returnString.length()-2 > 0)
  //    returnString = returnString.substring(0, returnString.length()-2);
  //  return returnString;
  //}
  String getResourceList(JSONArray resArray) {
    String returnString = "";
    try {
      for (int i=0; i<resArray.size(); i++) {
        JSONObject jo = resArray.getJSONObject(i);
        returnString += String.format("  %s %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
      throw e;
    }
    return returnString;
  }
  String getResourceList(JSONArray resArray, float[] availableResources) {
    // Colouring for insufficient resources
    String returnString = "";
    try {
      for (int i=0; i<resArray.size(); i++) {
        JSONObject jo = resArray.getJSONObject(i);
        if (availableResources[jsManager.getResIndex(jo.getString("id"))] >= jo.getFloat("quantity")) { // Check if has enough resources
          returnString += String.format("  %s %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
        } else {
          returnString += String.format("  <i>%s</i> %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
      throw e;
    }
    return returnString;
  }

  void setMoving(int turns, boolean splitting, int cost, boolean is3D) {
    attacking = false;
    //Tooltip text if moving. Turns is the number of turns in move
    JSONObject jo = gameData.getJSONObject("tooltips");
    String t = "";
    if (splitting) {
      t = jo.getString("moving splitting");
    } else if (turns == 0) {
      t = jo.getString("moving");
      if (is3D) {
        t += String.format("\nMovement Cost: %d", cost);
      }
    }
    if (turns > 0) {
      t += String.format(jo.getString("moving turns"), turns);
    }
    setText(t);
  }
  void setAttacking(BigDecimal chance) {
    attacking = true;
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(String.format(jo.getString("attacking"), chance.toString()));
  }
  void setTurnsRemaining() {
    attacking = false;
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(jo.getString("turns remaining"));
  }
  void setMoveButton() {
    attacking = false;
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(jo.getString("move button"));
  }
  void setMerging() {
    attacking = false;
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(jo.getString("merging"));
  }
  void setTask(String task, float[] availibleResources, int movementPoints) {
    try {
      attacking = false;
      JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task);
      String t="";
      if (jo == null){
        setText("Problem");
        return;
      }
      if (!jo.isNull("description")) {
        t += jo.getString("description")+"\n\n";
      }
      if (!jo.isNull("initial cost")) {
        t += String.format("Initial Resource Cost:\n%s\n", getResourceList(jo.getJSONArray("initial cost"), availibleResources));
      }
      if (!jo.isNull("movement points")) {
        if (movementPoints >= jo.getInt("movement points")) {
          t += String.format("Movement Points: %d\n", jo.getInt("movement points"));
        } else {
          t += String.format("Movement Points: <i>%d</i>\n", jo.getInt("movement points"));
        }
      }
      if (!jo.isNull("action")) {
        t += String.format("Turns: %d\n", jo.getJSONObject("action").getInt("turns"));
      }
      if (t.length()>2 && (t.charAt(t.length()-1)!='\n' || t.charAt(t.length()-2)!='\n'))
        t += "\n";
      if (!jo.isNull("production")) {
        t += "Production/Turn/Unit:\n"+getResourceList(jo.getJSONArray("production"));
      }
      if (!jo.isNull("consumption")) {
        t += "Consumption/Turn/Unit:\n"+getResourceList(jo.getJSONArray("consumption"));
      }
      //Strip
      setText(t.replaceAll("\\s+$", ""));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Error changing tooltip to task: "+task, e);
      throw e;
    }
  }

  void setResource(HashMap<String, Float> buildings, String resource) {
    attacking = false;
    try {
      String t = "";
      for (String building : buildings.keySet()) {
        if (buildings.get(resource)>0) {
          t += String.format("%s: +%f", building, buildings.get(resource));
        } else {
          t += String.format("%s: %f", building, buildings.get(resource));
        }
      }
    } 
    catch (Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Error changing tooltip to resource: "+resource, e);
      throw e;
    }
  }

  void drawColouredLine(PGraphics canvas, String line, float startX, float startY) {
    int start=0, end=0;
    float tw=0;
    boolean coloured = false;
    try {
      while (end != line.length()) {
        start = end;
        if (coloured) {
          canvas.fill(255, 0, 0);
          end = line.indexOf("</i>", end);
        } else {
          canvas.fill(0);
          end = line.indexOf("<i>", end);
        }
        if (end == -1) { // indexOf returns -1 when not found
          end = line.length();
        }
        canvas.text(line.substring(start, end).replace("<i>", "").replace("</i>", ""), startX+tw, startY);
        tw += canvas.textWidth(line.substring(start, end).replace("<i>", "").replace("</i>", ""));
        coloured = !coloured;
      };
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Invalid index used drawing line", e);
    }
  }

  void draw(PGraphics panelCanvas) {
    if (visible && text.length() > 0) {
      ArrayList<String> lines = getLines(text);
      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      int tw = ceil(maxWidthLine(lines))+4;
      int gap = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
      int th = ceil(panelCanvas.textAscent()+panelCanvas.textDescent())*lines.size();
      int tx = round(between(0, mouseX-xOffset-tw/2, width-tw));
      int ty = round(between(0, mouseY-yOffset+20, height-th-20));
      panelCanvas.fill(200, 230);
      panelCanvas.stroke(0);
      panelCanvas.rectMode(CORNER);
      panelCanvas.rect(tx, ty, tw, th);
      panelCanvas.fill(0);
      panelCanvas.textAlign(LEFT, TOP);
      for (int i=0; i<lines.size(); i++) {
        if (lines.get(i).contains("<i>")) {
          drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap);
        } else {
          panelCanvas.text(lines.get(i), tx+2, ty+i*gap);
        }
      }
    }
  }
}

class NotificationManager extends Element {
  ArrayList<ArrayList<Notification>> notifications;
  int bgColour, textColour, displayNots, notHeight, topOffset, scroll, turn;
  Notification lastSelected;
  boolean scrolling;

  NotificationManager(int x, int y, int w, int h, int bgColour, int textColour, int displayNots, int turn) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.turn = turn;
    this.bgColour = bgColour;
    this.textColour = textColour;
    this.displayNots = displayNots;
    this.notHeight = h/displayNots;
    this.notifications = new ArrayList<ArrayList<Notification>>();
    notifications.add(new ArrayList<Notification>());
    notifications.add(new ArrayList<Notification>());
    this.scroll = 0;
    lastSelected = null;
    scrolling = false;
  }

  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }

  boolean mouseOver(int i) {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+notHeight*i+topOffset && mouseY-yOffset <= y+notHeight*(i+1)+topOffset;
  }

  int findMouseOver() {
    if (!moveOver()) {
      return -1;
    }
    for (int i=0; i<notifications.get(turn).size(); i++) {
      if (mouseOver(i)) {
        return i;
      }
    }
    return -1;
  }
  boolean hoveringDismissAll() {
    return x<mouseX-xOffset&&mouseX-xOffset<x+notHeight&&y<mouseY-yOffset&&mouseY-yOffset<y+topOffset;
  }

  void turnChange(int turn) {
    this.turn = turn;
    this.scroll = 0;
  }

  void dismiss(int i) {
    LOGGER_MAIN.fine(String.format("Dismissing notification at index: %d which equates to:%s", i, notifications.get(turn).get(i)));
    try {
      notifications.get(turn).remove(i);
      scroll = round(between(0, scroll, notifications.get(turn).size()-displayNots));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error dismissing notification", e);
      throw e;
    }
  }

  void dismissAll() {
    // Dismisses all notification for the current player
    LOGGER_MAIN.fine("Dismissing all notifications");
    notifications.get(turn).clear();
  }

  void reset() {
    // Clears all notificaitions for all players
    LOGGER_MAIN.fine("Dismissing notifications for all players");
    notifications.clear();
    notifications.add(new ArrayList<Notification>());
    notifications.add(new ArrayList<Notification>());
  }

  void post(Notification n, int turn) {
    try {
      LOGGER_MAIN.fine("Posting notification: "+n.name);
      notifications.get(turn).add(0, n);
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Failed to post notification", e);
      throw e;
    }
  }

  void post(String name, int x, int y, int turnNum, int turn) {
    try {
      LOGGER_MAIN.fine(String.format("Posting notification: %s at cell:%s, %s turn:%d player:%d", name, x, y, turnNum, turn));
      notifications.get(turn).add(0, new Notification(name, x, y, turnNum));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Failed to post notification", e);
      throw e;
    }
  }

  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseWheel") {
      float count = event.getCount();
      if (moveOver()) {
        scroll = round(between(0, scroll+count, notifications.get(turn).size()-displayNots));
      }
    }
    // Lazy fix for bug
    if (moveOver() && visible && active && !empty()) {
      events.add("stop events");
    }
    return events;
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mousePressed") {
      if (moveOver() && mouseX-xOffset>x+w-20*jsManager.loadFloatSetting("gui scale") && mouseY-yOffset > topOffset && notifications.get(turn).size() > displayNots) {
        scrolling = true;
        scroll = round(between(0, (mouseY-yOffset-y-topOffset)*(notifications.get(turn).size()-displayNots+1)/(h-topOffset), notifications.get(turn).size()-displayNots));
      } else {
        scrolling = false;
      }
    }
    if (eventType == "mouseDragged") {
      if (scrolling && notifications.get(turn).size() > displayNots) {
        scroll = round(between(0, (mouseY-yOffset-y-topOffset)*(notifications.get(turn).size()-displayNots+1)/(h-topOffset), notifications.get(turn).size()-displayNots));
      }
    }
    if (eventType == "mouseClicked") {
      int hovering = findMouseOver();
      if (hovering >=0) {
        if (mouseX-xOffset<x+notHeight) {
          dismiss(hovering+scroll);
          events.add("notification dismissed");
        } else if (!(notifications.get(turn).size() > displayNots) || !(mouseX-xOffset>x+w-20*jsManager.loadFloatSetting("gui scale"))) {
          lastSelected = notifications.get(turn).get(hovering+scroll);
          events.add("notification selected");
        }
      } else if (mouseX-xOffset<x+notHeight && hoveringDismissAll()) {
        dismissAll();
      }
    }
    return events;
  }

  boolean empty() {
    return notifications.get(turn).size() == 0;
  }

  void draw(PGraphics panelCanvas) {
    if (empty())return;
    panelCanvas.pushStyle();
    panelCanvas.fill(bgColour);
    this.notHeight = (h-topOffset)/displayNots;
    panelCanvas.rect(x, y, w, notHeight);
    panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
    panelCanvas.fill(brighten(bgColour, -50));
    topOffset = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
    panelCanvas.rect(x, y, w, topOffset);
    panelCanvas.fill(textColour);
    panelCanvas.textAlign(CENTER, TOP);
    panelCanvas.text("Notification Manager", x+w/2, y);

    if (hoveringDismissAll()) {
      panelCanvas.fill(brighten(bgColour, 80));
    } else {
      panelCanvas.fill(brighten(bgColour, -20));
    }
    panelCanvas.rect(x, y, notHeight, topOffset);
    panelCanvas.strokeWeight(3);
    panelCanvas.line(x+5, y+5, x+notHeight-5, y+topOffset-5);
    panelCanvas.line(x+notHeight-5, y+5, x+5, y+topOffset-5);
    panelCanvas.strokeWeight(1);

    int hovering = findMouseOver();
    for (int i=0; i<min(notifications.get(turn).size(), displayNots); i++) {

      if (hovering == i) {
        panelCanvas.fill(brighten(bgColour, 20));
      } else {
        panelCanvas.fill(brighten(bgColour, -10));
      }
      panelCanvas.rect(x, y+i*notHeight+topOffset, w, notHeight);

      panelCanvas.fill(brighten(bgColour, -20));
      if (mouseX-xOffset<x+notHeight) {
        if (hovering == i) {
          panelCanvas.fill(brighten(bgColour, 80));
        } else {
          panelCanvas.fill(brighten(bgColour, -20));
        }
      }
      panelCanvas.rect(x, y+i*notHeight+topOffset, notHeight, notHeight);
      panelCanvas.strokeWeight(3);
      panelCanvas.line(x+5, y+i*notHeight+topOffset+5, x+notHeight-5, y+(i+1)*notHeight+topOffset-5);
      panelCanvas.line(x+notHeight-5, y+i*notHeight+topOffset+5, x+5, y+(i+1)*notHeight+topOffset-5);
      panelCanvas.strokeWeight(1);

      panelCanvas.fill(textColour);
      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(LEFT, CENTER);
      panelCanvas.text(notifications.get(turn).get(i+scroll).name, x+notHeight+5, y+topOffset+i*notHeight+notHeight/2);
      panelCanvas.textAlign(RIGHT, CENTER);
      panelCanvas.text("Turn "+notifications.get(turn).get(i+scroll).turn, x-notHeight+w, y+topOffset+i*notHeight+notHeight/2);
    }

    //draw scroll
    int d = notifications.get(turn).size() - displayNots;
    if (d > 0) {
      panelCanvas.fill(brighten(bgColour, 100));
      panelCanvas.rect(x-20*jsManager.loadFloatSetting("gui scale")+w, y+topOffset, 20*jsManager.loadFloatSetting("gui scale"), h-topOffset);
      panelCanvas.fill(brighten(bgColour, -20));
      panelCanvas.rect(x-20*jsManager.loadFloatSetting("gui scale")+w, y+(h-topOffset-(h-topOffset)/(d+1))*scroll/d+topOffset, 20*jsManager.loadFloatSetting("gui scale"), (h-topOffset)/(d+1));
    }
    panelCanvas.popStyle();
  }
}



class TextBox extends Element {
  int textSize, bgColour, textColour;
  String text;
  boolean autoSizing;

  TextBox(int x, int y, int w, int h, int textSize, String text, int bgColour, int textColour) {
    //w=-1 means get width from text
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    if (this.w == -1)
      autoSizing = true;
    else
      autoSizing = false;
    this.textSize = textSize;
    this.bgColour = bgColour;
    this.textColour = textColour;
    setText(text);
  }

  void setText(String text) {
    this.text = text;
    LOGGER_MAIN.finer("Text set to: " + text);
  }

  void updateWidth(PGraphics panelCanvas) {
    if (autoSizing) {
      this.w = ceil(panelCanvas.textWidth(text))+10;
    }
  }

  String getText() {
    return text;
  }

  void setColour(int c) {
    bgColour = c;
  }

  void draw(PGraphics panelCanvas) {
    panelCanvas.pushStyle();
    panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER, CENTER);
    panelCanvas.rectMode(CORNER);
    updateWidth(panelCanvas);
    if (bgColour != color(255, 255)) {
      panelCanvas.fill(bgColour);
      panelCanvas.rect(x, y, w, h);
    }
    panelCanvas.fill(textColour);
    panelCanvas.text(text, x+w/2, y+h/2);
    panelCanvas.popStyle();
  }
}



class ResourceSummary extends Element {
  float[] stockPile, net;
  String[] resNames;
  int numRes, scroll;
  boolean expanded;
  int[] timings;

  final int GAP = 10;
  final int FLASHTIMES = 500;

  ResourceSummary(int x, int y, int h, String[] resNames, float[] stockPile, float[] net) {
    this.x = x;
    this.y = y;
    this.h = h;
    this.resNames = resNames;
    this.numRes = resNames.length;
    this.stockPile = stockPile;
    this.net = net;
    this.expanded = false;
    this.timings = new int[resNames.length];
  }

  void updateStockpile(float[] v) {
    try {
      stockPile = v;
      LOGGER_MAIN.finest("Stockpile update: " + Arrays.toString(v));
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Error updating stockpile", e);
      throw e;
    }
  }
  void updateNet(float[] v) {
    try {
      LOGGER_MAIN.finest("Net update: " + Arrays.toString(v));
      net = v;
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Error updating net", e);
      throw e;
    }
  }
  void toggleExpand() {
    expanded = !expanded;
    LOGGER_MAIN.finest("Expanded changed to: " + expanded);
  }
  String prefix(String v) {
    try {
      float i = Float.parseFloat(v);
      if (i >= 1000000)
        return (new BigDecimal(v).divide(new BigDecimal("1000000"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString()+"M";
      else if (i >= 1000)
        return (new BigDecimal(v).divide(new BigDecimal("1000"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString()+"K";

      return (new BigDecimal(v).divide(new BigDecimal("1"), 1, BigDecimal.ROUND_HALF_EVEN).stripTrailingZeros()).toPlainString();
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error creating prefix", e);
      throw e;
    }
  }

  String getResString(int i) {
    return resNames[i];
  }
  String getStockString(int i) {
    String tempString = prefix(""+stockPile[i]);
    return tempString;
  }
  String getNetString(int i) {
    String tempString = prefix(""+net[i]);
    if (net[i] >= 0) {
      return "+"+tempString;
    }
    return tempString;
  }
  int columnWidth(int i) {
    int m=0;
    textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
    m = max(m, ceil(textWidth(getResString(i))));
    textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
    m = max(m, ceil(textWidth(getStockString(i))));
    textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
    m = max(m, ceil(textWidth(getNetString(i))));
    return m;
  }
  int totalWidth() {
    int tot = 0;
    for (int i=numRes-1; i>=0; i--) {
      if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0:1)) continue;
      tot += columnWidth(i)+GAP;
    }
    return tot;
  }

  void flash(int i) {
    timings[i] = millis()+FLASHTIMES;
  }
  int getFill(int i) {
    if (timings[i] < millis()) {
      return color(100);
    }
    return color(155*(timings[i]-millis())/FLASHTIMES+100, 100, 100);
  }

  String getResourceAt(int x, int y) {
    return "";
  }

  void draw(PGraphics panelCanvas) {
    int cw = 0;
    int w, yLevel, tw = totalWidth();
    panelCanvas.pushStyle();
    panelCanvas.textAlign(LEFT, TOP);
    panelCanvas.fill(120);
    panelCanvas.rect(width-tw-x-GAP/2, y, tw, h);
    panelCanvas.rectMode(CORNERS);
    for (int i=numRes-1; i>=0; i--) {
      if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0:1)) continue;
      w = columnWidth(i);
      panelCanvas.fill(getFill(i));
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.rect(width-cw+x-GAP/2, y, width-cw+x-GAP/2-(w+GAP), y+panelCanvas.textAscent()+panelCanvas.textDescent());
      cw += w+GAP;
      panelCanvas.line(width-cw+x-GAP/2, y, width-cw+x-GAP/2, y+h);
      panelCanvas.fill(0);

      yLevel=0;
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.text(getResString(i), width-cw, y);
      yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();

      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      panelCanvas.text(getStockString(i), width-cw, y+yLevel);
      yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();

      if (net[i] < 0)
        panelCanvas.fill(255, 0, 0);
      else
        panelCanvas.fill(0, 255, 0);
      panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
      panelCanvas.text(getNetString(i), width-cw, y+yLevel);
      yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();
    }
    panelCanvas.popStyle();
  }
}



class TaskManager extends Element {
  ArrayList<String> options;
  ArrayList<Integer> availableOptions;
  ArrayList<Integer> availableButOverBudgetOptions;
  int textSize;
  int scroll;
  int numDisplayed;
  boolean taskMActive;
  boolean scrolling;
  color bgColour, strokeColour;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  final int SCROLLWIDTH = 20;
  
  TaskManager(int x, int y, int w, int textSize, color bgColour, color strokeColour, String[] options, int numDisplayed) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.textSize = textSize;
    this.h = 10;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.options = new ArrayList<String>();
    this.availableOptions = new ArrayList<Integer>();
    this.availableButOverBudgetOptions = new ArrayList<Integer>();
    removeAllOptions();
    for (String option : options) {
      this.options.add(option);
    }
    resetAvailable();
    taskMActive = true;
    resetScroll();
    this.numDisplayed = numDisplayed;
  }
  
  void resetScroll(){
    scroll = 0;
    scrolling = false;
  }
  
  void setOptions(ArrayList<String> options) {
    LOGGER_MAIN.finer("Options changed to:["+String.join(", ", options));
    this.options = options;
    resetScroll();
  }
  void addOption(String option) {
    LOGGER_MAIN.finer("Option added: " + option);
    this.options.add(option);
    resetScroll();
  }
  void removeOption(String option) {
    LOGGER_MAIN.finer("Option removed: " + option);
    for (int i=0; i <options.size(); i++) {
      if (option.equals(options.get(i))) {
        options.remove(i);
      }
    }
    resetScroll();
  }
  void removeAllOptions() {
    LOGGER_MAIN.finer("Options all removed");
    this.options.clear();
    resetScroll();
  }
  void resetAvailable() {
    LOGGER_MAIN.finer("Available Options all removed");
    this.availableOptions.clear();
    resetScroll();
  }
  void resetAvailableButOverBudget() {
    LOGGER_MAIN.finer("Available But Over Budget Options all removed");
    this.availableButOverBudgetOptions.clear();
    resetScroll();
  }
  String getSelected() {
    return options.get(availableOptions.get(0));
  }
  void makeAvailable(String option) {
    try {
      LOGGER_MAIN.finer("Making option availalbe: " + option);
      for (int i=0; i<availableOptions.size(); i++) {
        if (options.get(availableOptions.get(i)).equals(option)) {
          return;
        }
      }
      for (int i=0; i<options.size(); i++) {
        if (options.get(i).equals(option)) {
          this.availableOptions.add(i);
          return;
        }
      }
      resetScroll();
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error making task available", e);
      throw e;
    }
  }
  void makeAvailableButOverBudget(String option) {
    try {
      LOGGER_MAIN.finer("Making option available but over buject: " + option);
      for (int i=0; i<availableButOverBudgetOptions.size(); i++) {
        if (options.get(availableButOverBudgetOptions.get(i)).equals(option)) {
          return;
        }
      }
      for (int i=0; i<options.size(); i++) {
        if (options.get(i).equals(option)) {
          this.availableButOverBudgetOptions.add(i);
          return;
        }
      }
      resetScroll();
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error making task available", e);
      throw e;
    }
  }
  void makeUnavailableButOverBudget(String option) {
    LOGGER_MAIN.finer("Making unavilablae but over over buject option:"+option);
    for (int i=0; i<options.size(); i++) {
      if (options.get(i).equals(option)) {
        this.availableButOverBudgetOptions.remove(i);
        return;
      }
    }
    resetScroll();
  }
  void makeUnavailable(String option) {
    LOGGER_MAIN.finer("Making unavailable:"+option);
    for (int i=0; i<options.size(); i++) {
      if (options.get(i).equals(option)) {
        this.availableOptions.remove(i);
        return;
      }
    }
    resetScroll();
  }
  void selectAt(int j) {
    LOGGER_MAIN.finer("Selecting based on position, " + j);
    if (j < availableOptions.size()) {
      int temp = availableOptions.get(0);
      availableOptions.set(0, availableOptions.get(j));
      availableOptions.set(j, temp);
    }
  }
  void select(String s) {
    LOGGER_MAIN.finer("Selecting based on string: "+s);
    for (int j=0; j<availableOptions.size(); j++) {
      if (options.get(availableOptions.get(j)).equals(s)) {
        selectAt(j);
        return;
      }
    }
    LOGGER_MAIN.warning("String for selection not found: "+s);
  }
  int getH(PGraphics panelCanvas) {
    panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
    return ceil(panelCanvas.textAscent() + panelCanvas.textDescent());
  }
  boolean optionAvailable(int i) {
    for (int option : availableOptions) {
      if (option == i) {
        return true;
      }
    }
    return false;
  }
  void draw(PGraphics panelCanvas) {
    panelCanvas.pushStyle();
    h = getH(panelCanvas); //Also sets the font
    
    //Draw background
    panelCanvas.strokeWeight(2);
    panelCanvas.stroke(0);
    panelCanvas.fill(170);
    panelCanvas.rect(x, y, w+1, h*numDisplayed+1);
    
    // Draw current task box
    panelCanvas.strokeWeight(1);
    panelCanvas.fill(brighten(bgColour, ONOFFSET));
    panelCanvas.stroke(strokeColour);
    panelCanvas.rect(x, y, w, h);
    panelCanvas.fill(0);
    panelCanvas.textAlign(LEFT, TOP);
    panelCanvas.text("Current Task: "+options.get(availableOptions.get(0)), x+5, y);
     
    // Draw other tasks
    int j;
    for (j=1; j < min(availableOptions.size()-scroll, numDisplayed); j++) {
      if (taskMActive && mouseOver(j)) {
        panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET));
      } else {
        panelCanvas.fill(bgColour);
      }
      panelCanvas.rect(x, y+h*j, w, h);
      panelCanvas.fill(0);
      panelCanvas.text(options.get(availableOptions.get(j+scroll)), x+5, y+h*j);
    }
    for (; j < min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed); j++) {
      panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET/2));
      panelCanvas.rect(x, y+h*j, w, h);
      panelCanvas.fill(120);
      panelCanvas.text(options.get(availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))), x+5, y+h*j);
    }
    
    //draw scroll
    int d = availableOptions.size() + availableButOverBudgetOptions.size()-1 - numDisplayed;
    if (d > 0) {
      panelCanvas.strokeWeight(1);
      panelCanvas.fill(brighten(bgColour, 100));
      panelCanvas.rect(x-SCROLLWIDTH*jsManager.loadFloatSetting("gui scale")+w, y, SCROLLWIDTH*jsManager.loadFloatSetting("gui scale"), h*numDisplayed);
      panelCanvas.strokeWeight(2);
      panelCanvas.fill(brighten(bgColour, -20));
      panelCanvas.rect(x-SCROLLWIDTH*jsManager.loadFloatSetting("gui scale")+w, y+(h*numDisplayed-(h*numDisplayed)/(d+1))*scroll/d, SCROLLWIDTH*jsManager.loadFloatSetting("gui scale"), (h*numDisplayed)/(d+1));
    }
    
    panelCanvas.popStyle();
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    int d = availableOptions.size() + availableButOverBudgetOptions.size()-1 - numDisplayed;
    if (eventType == "mouseMoved") {
      taskMActive = moveOver();
    }
    if (eventType == "mouseClicked" && button == LEFT) {
      for (int j=1; j < availableOptions.size(); j++) {
        if (mouseOver(j)) {
          if (d <= 0 || mouseX-xOffset<x+w-SCROLLWIDTH) {
            selectAt(j-scroll);
            events.add("valueChanged");
            scrolling = false;
          }
        }
      }
    } else if (eventType.equals("mousePressed")) {
      if (d > 0 && moveOver() && mouseX-xOffset>x+w-SCROLLWIDTH) {  
        // If hovering over scroll bar, set scroll to mouse pos
        scrolling = true;
        scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/h, d));
      } else {
        scrolling = false;
      }
    } else if (eventType.equals("mouseDragged")) {
      if (scrolling && d > 0) {  
        // If scrolling, set scroll to mouse pos
        scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/h, d));
      }
    } else if (eventType.equals("mouseReleased")) {
      scrolling = false;
    }
    return events;
  }

  ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseWheel") {
      float count = event.getCount();
      if (moveOver()) { // Check mouse over element
        if (availableOptions.size() + availableButOverBudgetOptions.size()-1 > numDisplayed) {
          scroll = round(between(0, scroll+count, availableOptions.size() + availableButOverBudgetOptions.size()-1-numDisplayed));
          LOGGER_MAIN.finest("Changing scroll to: "+scroll);
        }
      }
    }
    return events;
  }

  String findMouseOver() {
    try {
      int j;
      if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h) {
        return options.get(0);
      }
      for (j=scroll; j<min(availableOptions.size()-scroll, numDisplayed); j++) {
        if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+h*j && mouseY-yOffset <= y+h*(j+1)) {
          return options.get(availableOptions.get(j+scroll));
        }
      }
      for (; j<min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed); j++) {
        if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+h*j && mouseY-yOffset <= y+h*(j+1)) {
          return options.get(availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed)));
        }
      }
      return "";
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error finding mouse over option", e);
      throw e;
    }
  }

  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset > y && mouseY-yOffset < y+h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed);
  }
  boolean mouseOver(int j) {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset > y+h*j && mouseY-yOffset <= y+h*(j+1);
  }
}





class Button extends Element {
  private int x, y, w, h, cx, cy, textSize, textAlign;
  private color bgColour, strokeColour, textColour;
  private String state, text;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  private ArrayList<String> lines;

  Button(int x, int y, int w, int h, color bgColour, color strokeColour, color textColour, int textSize, int textAlign, String text) {
    state = "off";
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.textColour = textColour;
    this.textSize = textSize;
    this.textAlign = textAlign;
    this.text = text;
    centerCoords();

    setLines(text);
  }
  void transform(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    centerCoords();
  }
  void centerCoords() {
    cx = x+w/2;
    cy = y+h/2;
  }
  void setText(String text) {
    LOGGER_MAIN.finer("Setting text to: " + text);
    this.text = text;
    setLines(text);
  }
  void setColour(int colour) {
    LOGGER_MAIN.finest("Setting colour to: " + colour);
    this.bgColour = colour;
  }
  String getText() {
    return this.text;
  }
  void draw(PGraphics panelCanvas) {
    //println(xOffset, yOffset);
    int padding=0;
    float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
    panelCanvas.pushStyle();
    panelCanvas.fill(bgColour);
    if (state == "off") {
      panelCanvas.fill(bgColour);
    } else if (state == "hovering") {
      panelCanvas.fill(min(r+HOVERINGOFFSET, 255), min(g+HOVERINGOFFSET, 255), min(b+HOVERINGOFFSET, 255));
    } else if (state == "on") {
      panelCanvas.fill(min(r+ONOFFSET, 255), min(g+ONOFFSET, 255), min(b+ONOFFSET, 255));
    }
    panelCanvas.stroke(strokeColour);
    panelCanvas.strokeWeight(3);
    panelCanvas.rect(x, y, w, h);
    panelCanvas.noTint();
    panelCanvas.fill(textColour);
    panelCanvas.textAlign(textAlign, TOP);
    panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
    if (lines.size() == 1) {
      padding = h/10;
    }
    padding = (lines.size()*(int)(textSize*jsManager.loadFloatSetting("text scale"))-h/2)/2;
    for (int i=0; i<lines.size(); i++) {
      if (textAlign == CENTER) {
        panelCanvas.text(lines.get(i), cx, y+(h*0.9-textSize*jsManager.loadFloatSetting("text scale"))/2);
      } else {
        panelCanvas.text(lines.get(i), x, y );
      }
    }
    panelCanvas.popStyle();
  }

  ArrayList<String> setLines(String s) {
    LOGGER_MAIN.finer("Setting lines to: " + s);
    lines = new ArrayList<String>();
    try {
      int j = 0;
      for (int i=0; i<s.length(); i++) {
        if (s.charAt(i) == '\n') {
          lines.add(s.substring(j, i));
          j=i+1;
        }
      }
      lines.add(s.substring(j, s.length()));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error setting lines", e);
      throw e;
    }
    return lines;
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseReleased") {
      if (state == "on") {
        events.add("clicked");
      }
      state = "off";
    }
    if (mouseOver()) {
      if (!state.equals("on")) {
        state = "hovering";
      }
      if (eventType == "mousePressed") {
        state = "on";
        if (jsManager.loadBooleanSetting("sound on")) {
          try {
            sfx.get("click3").play();
          }
          catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error playing sound click 3", e);
            throw e;
          }
        }
      }
    } else {
      state = "off";
    }
    return events;
  }

  Boolean mouseOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }
}





class Slider extends Element {
  private int x, y, w, h, cx, cy, major, minor, lw, lx;
  private int padding = 20;
  private BigDecimal value, step, upper, lower;
  private float knobSize;
  private color KnobColour, bgColour, strokeColour, scaleColour;
  private boolean horizontal, pressed=false;
  final int boxHeight = 20, boxWidth = 10;
  private final int PRESSEDOFFSET = 50;
  private String name;
  boolean visible = true;

  Slider(int x, int y, int w, int h, color KnobColour, color bgColour, color strokeColour, color scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name) {
    this.lx = x;
    this.x = x;
    this.y = y;
    this.lw = w;
    this.w = w;
    this.h = h;
    this.KnobColour = KnobColour;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.scaleColour = scaleColour;
    this.major = major;
    this.minor = minor;
    this.upper = new BigDecimal(""+upper);
    this.lower = new BigDecimal(""+lower);
    this.horizontal = horizontal;
    this.step = new BigDecimal(""+step);
    this.value = new BigDecimal(""+value);
    this.name = name;
  }
  void show() {
    visible = true;
  }
  void hide() {
    visible = false;
  }
  void scaleKnob(PGraphics panelCanvas, BigDecimal value) {
    panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
    this.knobSize = max(this.knobSize, panelCanvas.textWidth(""+getInc(value)));
  }
  void transform(int x, int y, int w, int h) {
    this.lx = x;
    this.x = x;
    this.lw = w;
    this.w = w; 
    this.y = y;
    this.h = h;
  }
  void setScale(float lower, float value, float upper, int major, int minor) {
    this.major = major;
    this.minor = minor;
    this.upper = new BigDecimal(""+upper);
    this.lower = new BigDecimal(""+lower);
    this.value = new BigDecimal(""+value);
  }
  void setValue(float value) {
    LOGGER_MAIN.finer("Setting value to: " + value);
    setValue(new BigDecimal(""+value));
  }

  void setValue(BigDecimal value) {
    LOGGER_MAIN.finer("Setting value to: " + value.toString());
    if (value.compareTo(lower) < 0) {
      this.value = lower;
    } else if (value.compareTo(upper)>0) {
      this.value = new BigDecimal(""+upper);
    } else {
      this.value = value.divideToIntegralValue(step).multiply(step);
    }
  }

  float getValue() {
    return value.floatValue();
  }
  BigDecimal getPreciseValue() {
    return value;
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (button == LEFT) {
      if (mouseOver() && eventType == "mousePressed") {
        pressed = true;
        setValue((new BigDecimal(mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
        events.add("valueChanged");
      } else if (eventType == "mouseReleased") {
        pressed = false;
      }
      if (eventType == "mouseDragged" && pressed) {
        setValue((new BigDecimal(mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
        events.add("valueChanged");
      }
    }
    return events;
  }

  Boolean mouseOver() {
    try {
      BigDecimal range = upper.subtract(lower);
      int xKnobPos = round(x+(value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue())-knobSize/2);
      return (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h) ||
        (mouseX-xOffset >= xKnobPos && mouseX-xOffset <= xKnobPos+knobSize && mouseY-yOffset >= y && mouseY-yOffset <= y+h); // Over slider or knob box
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error finding if mouse over", e);
      throw e;
    }
  }

  BigDecimal getInc(BigDecimal i) {
    return i.stripTrailingZeros();
  }

  void draw(PGraphics panelCanvas) {
    if (!visible)return;
    BigDecimal range = upper.subtract(lower);
    float r = red(KnobColour), g = green(KnobColour), b = blue(KnobColour);
    panelCanvas.pushStyle();
    panelCanvas.fill(255, 100);
    panelCanvas.stroke(strokeColour, 50);
    //rect(lx, y, lw, h);
    //rect(xOffset+x, y+yOffset+padding+2, w, h-padding);
    panelCanvas.stroke(strokeColour);


    for (int i=0; i<=minor; i++) {
      panelCanvas.fill(scaleColour);
      panelCanvas.line(x+w*i/minor, y+padding+(h-padding)/6, x+w*i/minor, y+5*(h-padding)/6+padding);
    }
    for (int i=0; i<=major; i++) {
      panelCanvas.fill(scaleColour);
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(CENTER);
      panelCanvas.text(getInc((new BigDecimal(""+i).multiply(range).divide(new BigDecimal(""+major), 15, BigDecimal.ROUND_HALF_EVEN).add(lower))).toPlainString(), x+w*i/major, y+padding);
      panelCanvas.line(x+w*i/major, y+padding, x+w*i/major, y+h);
    }

    if (pressed) {
      panelCanvas.fill(min(r-PRESSEDOFFSET, 255), min(g-PRESSEDOFFSET, 255), min(b+PRESSEDOFFSET, 255));
    } else {
      panelCanvas.fill(KnobColour);
    }

    panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER);
    panelCanvas.rectMode(CENTER);
    scaleKnob(panelCanvas, value);
    panelCanvas.rect(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2+padding/2, knobSize, boxHeight);
    panelCanvas.rectMode(CORNER);
    panelCanvas.fill(scaleColour);
    panelCanvas.text(getInc(value).toPlainString(), x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2+boxHeight/4+padding/2);
    panelCanvas.stroke(0);
    panelCanvas.textAlign(CENTER);
    panelCanvas.stroke(255, 0, 0);
    panelCanvas.line(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight/2+padding/2, x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight+padding/2);
    panelCanvas.stroke(0);
    panelCanvas.fill(0);
    panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(LEFT, BOTTOM);
    panelCanvas.text(name, x, y);
    panelCanvas.popStyle();
  }
}




class Text extends Element {
  int x, y, size, colour, align;
  PFont font;
  String text;

  Text(int x, int y, int size, String text, color colour, int align) {
    this.x = x;
    this.y = y;
    this.size = size;
    this.text = text;
    this.colour = colour;
    this.align = align;
  }
  void translate(int x, int y) {
    this.x = x;
    this.y = y;
  }
  void setText(String text) {
    this.text = text;
  }
  void calcSize(PGraphics panelCanvas) {
    panelCanvas.textFont(getFont(size*jsManager.loadFloatSetting("text scale")));
    this.w = ceil(panelCanvas.textWidth(text));
    this.h = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
  }
  void draw(PGraphics panelCanvas) {
    calcSize(panelCanvas);
    if (font != null) {
      panelCanvas.textFont(font);
    }
    panelCanvas.textAlign(align, TOP);
    panelCanvas.textFont(getFont(size*jsManager.loadFloatSetting("text scale")));
    panelCanvas.fill(colour);
    panelCanvas.text(text, x, y);
  }
  boolean mouseOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }
}





class TextEntry extends Element {
  StringBuilder text;
  int x, y, w, h, textSize, textAlign, cursor, selected;
  color textColour, boxColour, borderColour, selectionColour;
  String allowedChars, name;
  final int BLINKTIME = 500;
  boolean texActive;

  TextEntry(int x, int y, int w, int h, int textAlign, color textColour, color boxColour, color borderColour, String allowedChars) {
    this.x = x;
    this.y = y;
    this.h = h;
    this.w = w;
    this.textColour = textColour;
    this.textSize = 10;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    this.borderColour = borderColour;
    this.allowedChars = allowedChars;
    text = new StringBuilder();
    selectionColour = brighten(selectionColour, 150);
    texActive = false;
  }
  TextEntry(int x, int y, int w, int h, int textAlign, color textColour, color boxColour, color borderColour, String allowedChars, String name) {
    this.x = x;
    this.y = y;
    this.h = h;
    this.w = w;
    this.textColour = textColour;
    this.textSize = 20;
    this.textAlign = textAlign;
    this.boxColour = boxColour;
    this.borderColour = borderColour;
    this.allowedChars = allowedChars;
    this.name = name;
    text = new StringBuilder();
    selectionColour = brighten(selectionColour, 150);
    texActive = false;
  }

  void setText(String t) {
    LOGGER_MAIN.finest("Changing text to: " + t);
    this.text = new StringBuilder(t);
  }
  String getText() {
    return this.text.toString();
  }

  void draw(PGraphics panelCanvas) {
    boolean showCursor = ((millis()/BLINKTIME)%2==0 || keyPressed) && texActive;
    panelCanvas.pushStyle();

    // Draw a box behind the text
    panelCanvas.fill(boxColour);
    panelCanvas.stroke(borderColour);
    panelCanvas.rect(x, y, w, h);
    panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(textAlign);
    // Draw selection box
    if (selected != cursor && texActive && cursor >= 0 ) {
      panelCanvas.fill(selectionColour);
      panelCanvas.rect(x+panelCanvas.textWidth(text.substring(0, min(cursor, selected)))+5, y+2, panelCanvas.textWidth(text.substring(min(cursor, selected), max(cursor, selected))), h-4);
    }

    // Draw the text
    panelCanvas.textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(textAlign);
    panelCanvas.fill(textColour);
    panelCanvas.text(text.toString(), x+5, y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2, w, h);

    // Draw cursor
    if (showCursor) {
      panelCanvas.fill(0);
      panelCanvas.noStroke();
      panelCanvas.rect(x+panelCanvas.textWidth(text.toString().substring(0, cursor))+5, y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2, 1, textSize*jsManager.loadFloatSetting("text scale"));
    }
    if (name != null) {
      panelCanvas.fill(0);
      panelCanvas.textFont(getFont(10*jsManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(LEFT);
      panelCanvas.text(name, x, y-12);
    }

    panelCanvas.popStyle();
  }

  void resetSelection() {
    selected = cursor;
  }
  void transform(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  int getCursorPos(int mx, int my) {
    try {
      int i=0;
      for (; i<text.length(); i++) {
        textFont(getFont(textSize*jsManager.loadFloatSetting("text scale")));
        if ((textWidth(text.substring(0, i)) + textWidth(text.substring(0, i+1)))/2 + x > mx)
          break;
      }
      if (0 <= i && i <= text.length() && y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2<= my && my <= y+(h-textSize*jsManager.loadFloatSetting("text scale"))/2+textSize*jsManager.loadFloatSetting("text scale")) {
        return i;
      }
      return cursor;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting cursor position", e);
      throw e;
    }
  }

  void doubleSelectWord() {
    try {
      if (!(y <= mouseY-yOffset && mouseY-yOffset <= y+h)) {
        return;
      }
      int c = getCursorPos(mouseX-xOffset, mouseY-yOffset);
      int i;
      for (i=min(c, text.length()-1); i>0; i--) {
        if (text.charAt(i) == ' ') {
          i++;
          break;
        }
      }
      cursor = (int)between(0, i, text.length());
      for (i=c; i<text.length(); i++) {
        if (text.charAt(i) == ' ') {
          break;
        }
      }
      LOGGER_MAIN.finer("Setting selected characetr to position: " + i);
      selected = i;
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error double selecting word", e);
      throw e;
    }
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseClicked") {
      if (button == LEFT) {
        if (mouseOver()) {
          texActive = true;
        }
      }
    } else if (eventType == "mousePressed") {
      if (button == LEFT) {
        cursor = round(between(0, getCursorPos(mouseX-xOffset, mouseY-yOffset), text.length()));
        selected = getCursorPos(mouseX-xOffset, mouseY-yOffset);
      }
      if (!mouseOver()) {
        texActive = false;
      }
    } else if (eventType == "mouseDragged") {
      if (button == LEFT) {
        selected = getCursorPos(mouseX-xOffset, mouseY-yOffset);
      }
    } else if (eventType == "mouseDoubleClicked") {
      doubleSelectWord();
    }
    return events;
  }

  ArrayList<String> keyboardEvent(String eventType, char _key) {
    ArrayList<String> events = new ArrayList<String>();
    if (texActive) {
      if (eventType == "keyTyped") {
        if (allowedChars.equals("") || allowedChars.contains(""+_key)) {
          if (cursor != selected) {
            text = new StringBuilder(text.substring(0, min(cursor, selected)) + text.substring(max(cursor, selected), text.length()));
            cursor = min(cursor, selected);
            resetSelection();
          }
          text.insert(cursor++, _key);
          resetSelection();
        }
      } else if (eventType == "keyPressed") {
        if (_key == '\n') {
          events.add("enterPressed");
          texActive = false;
        }
        if (_key == BACKSPACE) {
          if (selected == cursor) {
            if (cursor > 0) {
              text.deleteCharAt(--cursor);
              resetSelection();
            }
          } else {
            text = new StringBuilder(text.substring(0, min(cursor, selected)) + text.substring(max(cursor, selected), text.length()));
            cursor = min(cursor, selected);
            resetSelection();
          }
        }
        if (_key == CODED) {
          if (keyCode == LEFT) {
            cursor = max(0, cursor-1);
            resetSelection();
          }
          if (keyCode == RIGHT) {
            cursor = min(text.length(), cursor+1);
            resetSelection();
          }
        }
      }
    }
    return events;
  }

  Boolean mouseOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }
}





class ToggleButton extends Element {
  color bgColour, strokeColour;
  String name;
  boolean on;
  ToggleButton(int x, int y, int w, int h, color bgColour, color strokeColour, boolean value, String name) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.bgColour = bgColour;
    this.strokeColour = strokeColour;
    this.name = name;
    this.on = value;
  }
  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mouseClicked"&&mouseOver()) {
      events.add("valueChanged");
      on = !on;
    }
    return events;
  }
  void transform(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
  boolean getState() {
    return on;
  }
  void setState(boolean state) {
    LOGGER_MAIN.finest("Setting toggle state to: " + state);
    on = state;
  }
  void draw(PGraphics panelCanvas) {
    panelCanvas.pushStyle();
    panelCanvas.fill(bgColour);
    panelCanvas.stroke(strokeColour);
    panelCanvas.rect(x, y, w, h);
    if (on) {
      panelCanvas.fill(0, 255, 0);
      panelCanvas.rect(x, y, w/2, h);
    } else {
      panelCanvas.fill(255, 0, 0);
      panelCanvas.rect(x+w/2, y, w/2, h);
    }
    panelCanvas.fill(0);
    panelCanvas.textFont(getFont(8*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(LEFT, BOTTOM);
    panelCanvas.text(name, x, y);
    panelCanvas.popStyle();
  }
  Boolean mouseOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }
}
