

class PlayerSelector extends Element {
  PlayerSelector(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }
}

class IncrementElement extends Element {
  final int TEXTSIZE = 8;
  final int SIDEBOXESWIDTH = 15;
  final int ARROWOFFSET = 4;
  final float FULLPANPROPORTION = 0.25;  // Adjusts how much mouse dragging movement is needed to change value as propotion of screen width
  private int upper, lower, value, step, bigStep;
  int startingX, startingValue, pressing;
  boolean grabbed;

  IncrementElement(int x, int y, int w, int h, int upper, int lower, int startingValue, int step, int bigStep){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.upper = upper;
    this.lower = lower;
    this.value = startingValue;
    this.step = step;
    this.bigStep = bigStep;
    grabbed = false;
    startingX = 0;
    startingValue = value;
    pressing = -1;
  }

  void setUpper(int upper){
    this.upper = upper;
  }

  int getUpper(){
    return this.upper;
  }

  void setLower(int lower){
    this.lower = lower;
  }

  int getLower(){
    return this.lower;
  }

  void setValue(int value){
    this.value = value;
  }

  int getValue(){
    return this.value;
  }

  void setValueWithinBounds(){
    value = int(between(lower, value, upper));
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")){
      int change = 0;
      if (button == LEFT){
        change = step;
      } else if (button == RIGHT){  // Right clicking increments by more
        change = bigStep;
      }

      if (mouseOverLeftBox()){
        setValue(getValue()-change);
        setValueWithinBounds();
        events.add("valueChanged");
      } else if (mouseOverRightBox()){
        setValue(getValue()+change);
        setValueWithinBounds();
        events.add("valueChanged");
      }
    }
    if (eventType.equals("mousePressed")){
      if (mouseOverRightBox()){
        pressing = 0;
      } else if (mouseOverLeftBox()){
        pressing = 1;
      } else if (mouseOverMiddleBox()){
        grabbed = true;
        startingX = mouseX;
        startingValue = getValue();
        pressing = 2;
      } else{
        pressing = -1;
      }
    }
    if (eventType.equals("mouseReleased")){
      if (grabbed){
        events.add("valueChanged");
      }
      grabbed = false;
      pressing = -1;
    }
    if (eventType.equals("mouseDragged")){
      if (grabbed){
        int change = floor((mouseX-startingX)*(upper-lower)/(width*FULLPANPROPORTION));
        if (change != 0){
          setValue(startingValue+change);
          setValueWithinBounds();
        }
      }
    }
    return events;
  }

  void draw(PGraphics panelCanvas){
    panelCanvas.pushStyle();

    //Draw middle box
    panelCanvas.strokeWeight(2);
    if (grabbed){
      panelCanvas.fill(150);
    } else if (getElemOnTop() && mouseOverMiddleBox()){
      panelCanvas.fill(200);
    } else{
      panelCanvas.fill(170);
    }
    panelCanvas.rect(x, y, w, h);

    //draw left side box
    panelCanvas.strokeWeight(1);
    if (getElemOnTop() && mouseOverLeftBox()){
      if (pressing == 1){
        panelCanvas.fill(100);
      } else{
        panelCanvas.fill(130);
      }
    } else{
      panelCanvas.fill(120);
    }
    panelCanvas.rect(x, y, SIDEBOXESWIDTH, h-1);
    panelCanvas.fill(0);
    panelCanvas.strokeWeight(2);
    panelCanvas.line(x-ARROWOFFSET+SIDEBOXESWIDTH, y+ARROWOFFSET, x+ARROWOFFSET, y+h/2);
    panelCanvas.line(x+ARROWOFFSET, y+h/2, x-ARROWOFFSET+SIDEBOXESWIDTH, y+h-ARROWOFFSET);

    //draw right side box
    if (getElemOnTop() && mouseOverRightBox()){
      if (pressing == 0){
        panelCanvas.fill(100);
      } else{
        panelCanvas.fill(130);
      }
    } else{
      panelCanvas.fill(120);
    }
    panelCanvas.rect(x+w-SIDEBOXESWIDTH, y, SIDEBOXESWIDTH, h-1);
    panelCanvas.fill(0);
    panelCanvas.strokeWeight(2);
    panelCanvas.line(x+w+ARROWOFFSET-SIDEBOXESWIDTH, y+ARROWOFFSET, x+w-ARROWOFFSET, y+h/2);
    panelCanvas.line(x+w-ARROWOFFSET, y+h/2, x+w+ARROWOFFSET-SIDEBOXESWIDTH, y-ARROWOFFSET+h);

    // Draw value
    panelCanvas.fill(0);
    panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(CENTER, CENTER);
    panelCanvas.text(value, x+w/2, y+h/2);

    panelCanvas.popStyle();
  }

  boolean mouseOverMiddleBox(){
    return mouseOver() && !mouseOverRightBox() && !mouseOverLeftBox();
  }

  boolean mouseOverRightBox() {
    return mouseX-xOffset >= x+w-SIDEBOXESWIDTH && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }

  boolean mouseOverLeftBox() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+SIDEBOXESWIDTH && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }

  boolean mouseOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
  }

  boolean pointOver(){
    return mouseOver();
  }
}

class EquipmentManager extends Element {
  final int TEXTSIZE = 8;
  final float BOXWIDTHHEIGHTRATIO = 0.75;
  final int SHADOWSIZE = 20;
  final float EXTRATYPEWIDTH = 1.5;
  final float BIGIMAGESIZE = 0.5;
  String[] equipmentClassDisplayNames;
  int[] currentEquipment, currentEquipmentQuantities;
  int currentUnitNumber;
  float boxWidth, boxHeight;
  int selectedClass;
  float dropX, dropY, dropW, dropH, oldDropH;
  ArrayList<int[]> equipmentToChange;
  HashMap<String, PImage> tempEquipmentImages;
  HashMap<String, PImage> bigTempEquipmentImages;

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
    currentEquipmentQuantities = new int[jsManager.getNumEquipmentClasses()];
    currentUnitNumber = 0;
    for (int i=0; i<currentEquipment.length;i ++){
      currentEquipment[i] = -1; // -1 represens no equipment
      currentEquipmentQuantities[i] = 0;
    }

    updateSizes();

    selectedClass = -1;  // -1 represents nothing being selected
    equipmentToChange = new ArrayList<int[]>();
    tempEquipmentImages = new HashMap<String, PImage>();
    bigTempEquipmentImages = new HashMap<String, PImage>();
    oldDropH = 0;
  }

  void updateSizes(){

    boxWidth = w/jsManager.getNumEquipmentClasses();
    boxHeight = boxWidth*BOXWIDTHHEIGHTRATIO;
    updateDropDownPositionAndSize();
  }

  void updateDropDownPositionAndSize(){
    dropY = y+boxHeight;
    dropW = boxWidth * EXTRATYPEWIDTH;
    dropH = jsManager.loadFloatSetting("gui scale") * 32;
    dropX = between(x, x+boxWidth*selectedClass-(dropW-boxWidth)/2, x+w-dropW);
  }


  void resizeImages(){
    // Resize equipment icons
    for (int c=0; c < jsManager.getNumEquipmentClasses(); c++){
      for (int t=0; t < jsManager.getNumEquipmentTypesFromClass(c); t++){
        try{
          String id = jsManager.getEquipmentTypeID(c, t);
          tempEquipmentImages.put(id, equipmentImages.get(id).copy());
          tempEquipmentImages.get(id).resize(int(dropH/0.75), int(dropH-1));
          bigTempEquipmentImages.put(id, equipmentImages.get(id).copy());
          bigTempEquipmentImages.get(id).resize(int(boxWidth*BIGIMAGESIZE), int(boxHeight*BIGIMAGESIZE));
        }
        catch (NullPointerException e){
          LOGGER_MAIN.log(Level.SEVERE, String.format("Error resizing image for equipment icon class:%d, type:%d, id:%s", c, t, jsManager.getEquipmentTypeID(c, t)), e);
          throw e;
        }
      }
    }
  }

  void transform(int x, int y, int w) {
    this.x = x;
    this.y = y;
    this.w = w;

    updateSizes();
  }

  void setEquipment(Party party) {
    this.currentEquipment = party.getAllEquipment();
    LOGGER_GAME.finer(String.format("changing equipment for manager to :%s", Arrays.toString(party.getAllEquipment())));
    currentEquipmentQuantities = party.getEquipmentQuantities();
    currentUnitNumber = party.getUnitNumber();
  }

  float getBoxHeight(){
    return boxHeight;
  }

  ArrayList<int[]> getEquipmentToChange(){
    // Also clears equipmentToChange
    ArrayList<int[]> temp = new ArrayList<int[]>(equipmentToChange);
    equipmentToChange.clear();
    return temp;
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")) {
      boolean[] blockedClasses = getBlockedClasses();
      if (mouseOverClasses()) {
        if (button == LEFT){
          int newSelectedClass = hoveringOverClass();
          if (newSelectedClass == selectedClass){  // If selecting same option
            selectedClass = -1;
          }
          else if (newSelectedClass == -1 || !blockedClasses[newSelectedClass]){
            selectedClass = newSelectedClass;
            events.add("dropped");
            events.add("stop events");
          }
        }
        else if (button == RIGHT){  // Unequip if right clicking on class
          events.add("valueChanged");
          events.add("stop events");
          equipmentToChange.add(new int[] {selectedClass, -1});
        }
      }
      else if (mouseOverTypes()){
        int newSelectedType = hoveringOverType();
        if (newSelectedType == jsManager.getNumEquipmentTypesFromClass(selectedClass)){
          // Unequip (final option)
          events.add("valueChanged");
          events.add("stop events");
          equipmentToChange.add(new int[] {selectedClass, -1});
        }
        else if (newSelectedType != currentEquipment[selectedClass] && !blockedClasses[selectedClass]){
          events.add("valueChanged");
          events.add("stop events");
          equipmentToChange.add(new int[] {selectedClass, newSelectedType});
        }
        selectedClass = -1;
      }
      else{
        selectedClass = -1;
      }
    }
    return events;
  }

  int getSelectedClass(){
    return selectedClass;
  }

  void drawShadow(PGraphics panelCanvas, float shadowX, float shadowY, float shadowW, float shadowH){
    panelCanvas.noStroke();
    for (int i = SHADOWSIZE; i > 0; i --){
      panelCanvas.fill(0, 255-255*pow(((float)i/SHADOWSIZE), 0.1));
      //panelCanvas.rect(shadowX-i, shadowY-i, shadowW+i*2, shadowH+i*2, i);
    }
  }

  boolean[] getBlockedClasses(){
    boolean[] blockedClasses = new boolean[jsManager.getNumEquipmentClasses()];
    for (int i = 0; i < blockedClasses.length; i ++) {
      if (currentEquipment[i] != -1){
        String[] otherBlocking = jsManager.getOtherClassBlocking(i, currentEquipment[i]);
        if (otherBlocking != null){
          for (int j=0; j < otherBlocking.length; j ++){
            int classIndex = jsManager.getEquipmentClassFromID(otherBlocking[j]);
            blockedClasses[classIndex] = true;
          }
        }
      }
    }
    return blockedClasses;
  }

  boolean[] getHoveringBlockedClasses(){
    boolean[] blockedClasses = new boolean[jsManager.getNumEquipmentClasses()];
    if (selectedClass != -1){
      if (hoveringOverType() != -1 && hoveringOverType() < jsManager.getNumEquipmentTypesFromClass(selectedClass)){
        String[] otherBlocking = jsManager.getOtherClassBlocking(selectedClass, hoveringOverType());
        if (otherBlocking != null){
          for (int j=0; j < otherBlocking.length; j ++){
            int classIndex = jsManager.getEquipmentClassFromID(otherBlocking[j]);
            blockedClasses[classIndex] = true;
          }
        }
      }
    }
    return blockedClasses;
  }

  void draw(PGraphics panelCanvas) {

    if (oldDropH != dropH){  // If height of boxes has changed
      resizeImages();
      oldDropH = dropH;
    }

    updateDropDownPositionAndSize();
    panelCanvas.pushStyle();

    panelCanvas.strokeWeight(2);
    panelCanvas.fill(170);
    panelCanvas.rect(x, y, w, boxHeight);

    boolean[] blockedClasses = getBlockedClasses();
    boolean[] potentialBlockedClasses = getHoveringBlockedClasses();

    for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
      if (!blockedClasses[i]){
        if (selectedClass == i){
          panelCanvas.strokeWeight(3);
          panelCanvas.fill(140);
        }
        else{
          if (!potentialBlockedClasses[i]){
            panelCanvas.strokeWeight(1);
            panelCanvas.noFill();
          }
          else{
            //Hovering over equipment type that blocks this class
            panelCanvas.strokeWeight(1);
            panelCanvas.fill(110);
          }
        }
        panelCanvas.rect(x+boxWidth*i, y, boxWidth, boxHeight);
        if (currentEquipment[i] != -1){
          panelCanvas.image(bigTempEquipmentImages.get(jsManager.getEquipmentTypeID(i, currentEquipment[i])), int(x+boxWidth*i)+(1-BIGIMAGESIZE)*boxWidth/2, y+(1-BIGIMAGESIZE)*boxHeight/2);
        }
        panelCanvas.fill(0);
        panelCanvas.textAlign(CENTER, TOP);
        panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
        panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5), y);
        if (currentEquipment[i] != -1){
          panelCanvas.textFont(getFont((TEXTSIZE-1)*jsManager.loadFloatSetting("text scale")));
          panelCanvas.textAlign(CENTER, BOTTOM);
          panelCanvas.text(jsManager.getEquipmentTypeDisplayName(i, currentEquipment[i]), x+boxWidth*(i+0.5), y+boxHeight);
          if (currentEquipmentQuantities[i] < currentUnitNumber){
            panelCanvas.fill(255, 0, 0);
          }
          panelCanvas.text(String.format("%d/%d", currentEquipmentQuantities[i], currentUnitNumber), x+boxWidth*(i+0.5), y+boxHeight-TEXTSIZE*jsManager.loadFloatSetting("text scale")+5);
        }
      }
      else{
        panelCanvas.strokeWeight(2);
        panelCanvas.fill(80);
        panelCanvas.rect(x+boxWidth*i, y, boxWidth, boxHeight);
        panelCanvas.fill(0);
        panelCanvas.textAlign(CENTER, TOP);
        panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
        panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5), y);
      }
    }

    // Draw dropdown if an equipment class is selected
    panelCanvas.stroke(0);
    if (selectedClass != -1){
      panelCanvas.textAlign(LEFT, TOP);
      panelCanvas.textFont(getFont(TEXTSIZE*jsManager.loadFloatSetting("text scale")));
      String[] equipmentTypes = jsManager.getEquipmentFromClass(selectedClass);
      for (int i = 0; i < jsManager.getNumEquipmentTypesFromClass(selectedClass);   i ++){
        panelCanvas.strokeWeight(1);
        if (currentEquipment[selectedClass] != i){
          panelCanvas.fill(170);
          panelCanvas.rect(dropX, dropY+i*dropH, dropW, dropH);
          panelCanvas.fill(0);
          panelCanvas.text(equipmentTypes[i], 3+dropX, dropY+i*dropH);
          try{
            panelCanvas.image(tempEquipmentImages.get(jsManager.getEquipmentTypeID(selectedClass, i)), dropX+dropW-dropH/0.75-2, dropY+dropH*i+2);
          }
          catch (NullPointerException e){
            LOGGER_MAIN.log(Level.WARNING, String.format("Error drawing image for equipment icon class:%d, type:%d, id:%s", selectedClass, i, jsManager.getEquipmentTypeID(selectedClass, i)), e);
          }
        }
        else{
          panelCanvas.fill(220);
          panelCanvas.rect(dropX, dropY+i*dropH, dropW, dropH);
          panelCanvas.fill(150);
          panelCanvas.text(equipmentTypes[i], 3+dropX, dropY+i*dropH);
          try{
            panelCanvas.image(tempEquipmentImages.get(jsManager.getEquipmentTypeID(selectedClass, i)), dropX+dropW-dropH/0.75-2, dropY+dropH*i+2);
          }
          catch (NullPointerException e){
            LOGGER_MAIN.log(Level.WARNING, String.format("Error drawing image for equipment icon class:%d, type:%d, id:%s", selectedClass, i, jsManager.getEquipmentTypeID(selectedClass, i)), e);
          }
        }
      }
      if (currentEquipment[selectedClass] != -1){
        panelCanvas.fill(170);
        panelCanvas.rect(dropX, dropY+jsManager.getNumEquipmentTypesFromClass(selectedClass)*dropH, dropW, dropH);
        panelCanvas.fill(0);
        panelCanvas.text("Unequip", 3+dropX, dropY+jsManager.getNumEquipmentTypesFromClass(selectedClass)*dropH);
      }
    }
    if (selectedClass != -1){
      panelCanvas.strokeWeight(2);
      panelCanvas.stroke(0);
      panelCanvas.noFill();
      if (currentEquipment[selectedClass] == -1){  // If nothing equipped, there is not unequip option at the bottom
        panelCanvas.rect(dropX, dropY, dropW, dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass))+1);
      } else{
        panelCanvas.rect(dropX, dropY, dropW, dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass)+1)+1);
      }
    }

    panelCanvas.popStyle();
  }

  boolean mouseOverClasses() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+boxHeight;
  }

  boolean mouseOverTypes() {
    if (selectedClass == -1){
      return false;
    } else if (currentEquipment[selectedClass] == -1){  // If nothing equipped, there is not unequip option at the bottom
      return mouseX-xOffset >= dropX && mouseX-xOffset <= dropX+dropW && mouseY-yOffset >= dropY && mouseY-yOffset <= dropY+dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass));
    } else{
      return mouseX-xOffset >= dropX && mouseX-xOffset <= dropX+dropW && mouseY-yOffset >= dropY && mouseY-yOffset <= dropY+dropH*(jsManager.getNumEquipmentTypesFromClass(selectedClass)+1);
    }
  }

  int hoveringOverType() {
    int num = jsManager.getNumEquipmentTypesFromClass(selectedClass);
    for (int i = 0; i < num+1; i ++) {
      if (mouseX-xOffset >= dropX && mouseX-xOffset <= dropX+dropW && mouseY-yOffset >= dropY+dropH*i && mouseY-yOffset <= dropY+dropH*(i+1)){
        return i;
      }
    }
    return -1;
  }

  int hoveringOverClass() {
    for (int i = 0; i < jsManager.getNumEquipmentClasses(); i ++) {
      if (mouseX-xOffset >= x+boxWidth*i && mouseX-xOffset <= x+boxWidth*(i+1) && mouseY-yOffset >= y && mouseY-yOffset <= y+boxHeight){
        return i;
      }
    }
    return -1;
  }

  boolean pointOver() {
    return mouseOverTypes() || mouseOverClasses();
  }
}

class ProficiencySummary extends Element {
  final int TEXTSIZE = 8;
  final int DECIMALPLACES = 2;
  String[] proficiencyDisplayNames;
  float[] proficiencies, bonuses;
  int rowHeight;

  ProficiencySummary(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    updateProficiencyDisplayNames();
    proficiencies = new float[jsManager.getNumProficiencies()];
    bonuses = new float[jsManager.getNumProficiencies()];
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

  void setProficiencyBonuses(float[] bonuses){
    this.bonuses = bonuses;
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
      panelCanvas.text(roundDpTrailing(""+proficiencies[i], DECIMALPLACES), x+w-10-panelCanvas.textWidth("0")*(DECIMALPLACES+4), y+rowHeight*(i+0.5));
      if (bonuses[i] > 0){
        panelCanvas.fill(0, 255, 0);
        panelCanvas.text("+"+roundDpTrailing(""+bonuses[i], DECIMALPLACES), x+w-5, y+rowHeight*(i+0.5)); // Display bonus aligned right, middle height within row
      }
      else if (bonuses[i] < 0){
        panelCanvas.fill(255, 0, 0);
        panelCanvas.text(roundDpTrailing(""+bonuses[i], DECIMALPLACES), x+w-5, y+rowHeight*(i+0.5)); // Display bonus aligned right, middle height within row
      }
    }

    panelCanvas.popStyle();
  }

  int hoveringOption(){
    for (int i = 0; i < proficiencyDisplayNames.length; i++){
      if (mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y+rowHeight*i && mouseY-yOffset <= y+rowHeight*(i+1)) {
        return i;
      }
    }
    return -1;
  }
  boolean mouseOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+h;
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
  boolean pointOver() {
    return moveOver();
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
    if (moveOver() && hovering == -1 && getElemOnTop()) {  // Hovering over top selected option
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
          if (moveOver() && i == hovering && getElemOnTop()) {
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
  boolean pointOver() {
    return moveOver();
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
  boolean pointOver() {
    return moveOver();
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
          returnString += String.format("  <r>%s</r> %s\n", roundDp(""+jo.getFloat("quantity"), 2), jo.getString("id"));
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting resource list", e);
      throw e;
    }
    return returnString;
  }

  void setMoving(int turns, boolean splitting, Party party, int numUnitsSplitting, int cost, boolean is3D) {
    //Tooltip text if moving. Turns is the number of turns in move
    JSONObject jo = gameData.getJSONObject("tooltips");
    String t = "";
    if (splitting) {
      t = String.format("Split %d units from party and move them to cell.\n", numUnitsSplitting);
      int[] splittedQuantities = party.splittedQuantities(numUnitsSplitting);

      boolean anyEquipment = false;
      for (int i = 0; i < party.getAllEquipment().length; i ++){
        if (party.getEquipment(i) != -1){
          anyEquipment = true;
        }
      }
      if (anyEquipment){
        t += "\n\nThe equipment will be divided as follows:\n";
      }
      for (int i = 0; i < party.getAllEquipment().length; i ++){
        if (party.getEquipment(i) != -1){
          // If the party has something equipped for this class
          t += String.format("%s: New party will get %d, existing party will keep %d", jsManager.getEquipmentTypeDisplayName(i, party.getEquipment(i)), splittedQuantities[i], party.getEquipmentQuantity(i) - splittedQuantities[i]);
        }
      }
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
  
  void setSieging() {
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(jo.getString("siege"));
  }
  
  void setAttacking(BigDecimal chance) {
    attacking = true;
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(String.format(jo.getString("attacking"), chance.toString()));
  }

  void setBombarding(int damage) {
    setText(String.format("Perform a ranged attack on the party.\nThis will eliminate %d units of the other party", damage));
  }

  void setBombarding() {
    setText(String.format("Perform a ranged attack on the party."));
  }

  void setTurnsRemaining() {
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(jo.getString("turns remaining"));
  }
  void setMoveButton() {
    JSONObject jo = gameData.getJSONObject("tooltips");
    setText(jo.getString("move button"));
  }
  void setMerging(Party p1, Party p2, int unitsTransfered) {
    // p1 is being merged into
    JSONObject jo = gameData.getJSONObject("tooltips");
    int overflow = p1.getOverflow(unitsTransfered);
    String t = String.format(jo.getString("merging"), p2.id, p1.id, unitsTransfered-overflow, overflow);

    int[][] equipments = p1.mergeEquipment(p2, unitsTransfered-overflow);

    boolean hasEquipment = false;
    // New equipment quantities + types for merged party
    t += "\n\nMerged party equipment:";
    for (int i=0; i<jsManager.getNumEquipmentClasses(); i++){
      if (equipments[0][i] != -1){
        t += String.format("\n%d x %s", equipments[2][i], jsManager.getEquipmentTypeDisplayName(i, equipments[0][i]));
        hasEquipment = true;
      }
    }
    if (!hasEquipment){
      t += " None\n";
    }

    // New equipment quantities + types for overflow party
    if (overflow > 0){
      hasEquipment = false;
      t += "\n\nOverflow party equipment:";
      for (int i=0; i<jsManager.getNumEquipmentClasses(); i++){
        if (equipments[1][i] != -1){
          t += String.format("\n%d x %s", equipments[3][i], jsManager.getEquipmentTypeDisplayName(i, equipments[1][i]));
          hasEquipment = true;
        }
      }
      if (!hasEquipment){
        t += " None\n";
      }
    }

    //Merged party proficiencies
    float[] mergedProficiencies = p1.mergeProficiencies(p2, unitsTransfered-overflow);
    t += "\n\nMerged party proficiencies:\n";
    for (int i=0; i < jsManager.getNumProficiencies(); i ++){
      t += String.format("%s = %s\n", jsManager.indexToProficiencyDisplayName(i), roundDpTrailing(""+mergedProficiencies[i], 2));
    }

    if (overflow > 0){
      t += "\n\n(Proficiencies for overflow party are the same as the original party)";
    }

    setText(t);
  }

  void setStockUpAvailable(Party p, float[] resources) {
    int[] equipment = p.equipment;
    String text = "";
    for (int i = 0; i < equipment.length; i++) {
      if (p.equipment[i] != -1) {
        JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
        int stockUpTo = min(p.getUnitNumber(), p.equipmentQuantities[i]+floor(resources[jsManager.getResIndex(equipmentObject.getString("id"))]));
        if (stockUpTo > p.equipmentQuantities[i]) {
          String equipmentName = equipmentObject.getString("display name");
          text += String.format("\n  Will stock %s up to %d.", equipmentName, stockUpTo);
        }
      }
    }
    setText("Stock up equipment. This will use all of the party's movement points."+text);
  }

  void setStockUpUnavailable(Party p) {
    String text = "Stock up unavailable.";
    boolean hasEquipment = false;
    String list = "";
    for (int i = 0; i < p.equipment.length; i++) {
      if (p.equipment[i] != -1) {
        hasEquipment = true;
        JSONObject equipmentObject = gameData.getJSONArray("equipment").getJSONObject(i).getJSONArray("types").getJSONObject(p.equipment[i]);
        String name = equipmentObject.getString("display name");
        JSONArray locations = equipmentObject.getJSONArray("valid collection sites");
        list += "\n  "+name+": ";
        for (int j = 0; j < locations.size(); j++) {
          list += locations.getString(j)+", ";
        }
        list = list.substring(0, list.length()-2);
      }
    }
    if (hasEquipment) {
      text += "\nThe following is where each currently equiped item can be stocked up at:\n"+list;
    } else {
      text += "\nThis party has no equipment selected so it cannot be stocked up.";
    }
    setText(text);
  }

  void setTask(String task, float[] availibleResources, int movementPoints) {
    try {
      JSONObject jo = findJSONObject(gameData.getJSONArray("tasks"), task);
      String t="";
      if (jo == null){
        setText("Problem");
        LOGGER_MAIN.warning("Could not find task:"+task);
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
          t += String.format("Movement Points: <r>%d</r>\n", jo.getInt("movement points"));
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
      LOGGER_MAIN.log(Level.SEVERE, "Error changing tooltip to task: "+task, e);
      throw e;
    }
  }

  void setEquipment(int equipmentClass, int equipmentType, float availableResources[], Party party, boolean collectionAllowed){
    // Tooltip is hovering over equipment manager, specifically over one of the equipmment types
    String t="";
    try{
      if (equipmentClass >= jsManager.getNumEquipmentClasses()){
        LOGGER_MAIN.warning("equipment class out of bounds");
        return;
      }
      if (equipmentType > jsManager.getNumEquipmentTypesFromClass(equipmentClass)){
        LOGGER_MAIN.warning("equipment class out of bounds:"+equipmentType);
      } else if (equipmentType == jsManager.getNumEquipmentTypesFromClass(equipmentClass)){
        JSONObject jo = gameData.getJSONObject("tooltips");
        t += jo.getString("unequip");
        if (collectionAllowed){
          t += "All equipment will be returned to stockpile";
        }
        else{
          t += "Equipment will be destroyed";
        }
      } else{
        JSONObject equipmentClassJO = gameData.getJSONArray("equipment").getJSONObject(equipmentClass);
        if (equipmentClassJO == null){
          setText("Problem");
          LOGGER_MAIN.warning("Equipment class not found with tooltip:"+equipmentClass);
          return;
        }
        JSONObject equipmentTypeJO = equipmentClassJO.getJSONArray("types").getJSONObject(equipmentType);
        if (equipmentTypeJO == null){
          setText("Problem");
          LOGGER_MAIN.warning("Equipment type not found with tooltip:"+equipmentType);
          return;
        }
        if (!equipmentTypeJO.isNull("display name")) {
          t += equipmentTypeJO.getString("display name")+"\n\n";
        }

        if (!equipmentTypeJO.isNull("description")) {
          t += equipmentTypeJO.getString("description")+"\n\n";
        }

        // Using 'display multipliers' array so ordering is consistant
        if (!equipmentClassJO.isNull("display multipliers")){
          t += "Proficiency Bonus Multipliers:\n";
          for (int i=0; i < equipmentClassJO.getJSONArray("display multipliers").size(); i++){
            String multiplierName = equipmentClassJO.getJSONArray("display multipliers").getString(i);
            if (!equipmentTypeJO.isNull(multiplierName)){
              if (equipmentTypeJO.getFloat(multiplierName) > 0){
                t += String.format("%s: <g>+%s</g>\n", multiplierName, roundDp("+"+equipmentTypeJO.getFloat(multiplierName), 2));
              } else {
                t += String.format("%s: <r>%s</r>\n", multiplierName, roundDp(""+equipmentTypeJO.getFloat(multiplierName), 2));
              }
            }
          }
          t += "\n";
        }

        // Other attributes e.g. range
        if (!equipmentClassJO.isNull("other attributes")){
          t += "Other Attributes:\n";
          for (int i=0; i < equipmentClassJO.getJSONArray("other attributes").size(); i++){
            String attribute = equipmentClassJO.getJSONArray("other attributes").getString(i);
            if (!equipmentTypeJO.isNull(attribute)){
              t += String.format("%s: %s\n", attribute, roundDp("+"+equipmentTypeJO.getFloat(attribute), 0));
            }
          }
          t += "\n";
        }

        // Display other classes that are blocked (if applicable)
        if (!equipmentTypeJO.isNull("other class blocking")){
          t += "Equipment blocks other classes: ";
          for (int i=0; i < equipmentTypeJO.getJSONArray("other class blocking").size(); i++){
            if (i < equipmentTypeJO.getJSONArray("other class blocking").size()-1){
              t += String.format("%s, ", equipmentTypeJO.getJSONArray("other class blocking").getString(i));
            }
            else{
              t += String.format("%s", equipmentTypeJO.getJSONArray("other class blocking").getString(equipmentTypeJO.getJSONArray("other class blocking").size()-1));
            }
          }
          t += "\n\n";
        }

        // Display amount of equipment available vs needed for party
        int resourceIndex = 0;
        try{
          resourceIndex = jsManager.getResIndex(equipmentTypeJO.getString("id"));
        }
        catch (Exception e){
          LOGGER_MAIN.log(Level.WARNING, String.format("Error finding resource for equipment class:%d, type:%d", equipmentClass, equipmentType), e);
          throw e;
        }
        if (floor(availableResources[resourceIndex]) >= party.getUnitNumber()){
          t += String.format("Equipment Available: %d/%d", floor(availableResources[resourceIndex]), party.getUnitNumber());
        } else{
          t += String.format("Equipment Available: <r>%d</r>/%d", floor(availableResources[resourceIndex]), party.getUnitNumber());
        }

        // Show where equipment can be stocked up
        if (!equipmentTypeJO.isNull("valid collection sites")){
          t += String.format("\n\n%s can be stocked up at: ", equipmentTypeJO.getString("display name"));
          for (int i=0; i < equipmentTypeJO.getJSONArray("valid collection sites").size(); i ++){
            t += equipmentTypeJO.getJSONArray("valid collection sites").getString(i);
            if (i+1 < equipmentTypeJO.getJSONArray("valid collection sites").size()){
              t += ", ";
            }
          }
        }
        else{
          t += String.format("\n\n%s can be stocked up anywhere", equipmentTypeJO.getString("display name"));
        }

        if (party.getMovementPoints() != party.getMaxMovementPoints()){
          t += "\n<r>Equipment can only be changed\nif party has full movement points</r>";
        } else{
          t += "\n(Equipment can only be changed\nif party has full movement points)";
        }
      }

      setText(t.replaceAll("\\s+$", ""));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to equipment class:%dk, type:%d", equipmentClass, equipmentType), e);
      throw e;
    }
  }

  void setProficiencies(int proficiencyIndex, Party party){
    String t="";
    JSONObject proficiencyJO;
    if (!(0 <= proficiencyIndex && proficiencyIndex < jsManager.getNumProficiencies())){
      LOGGER_MAIN.warning("Invalid proficiency index given:"+proficiencyIndex);
      return;
    }
    try{
      proficiencyJO = gameData.getJSONArray("proficiencies").getJSONObject(proficiencyIndex);
      if (!proficiencyJO.isNull("display name")){
        t += proficiencyJO.getString("display name") + "\n";
      }
      if (!proficiencyJO.isNull("tooltip")){
        t += proficiencyJO.getString("tooltip") + "\n";
      }
      float bonusMultiplier = party.getProficiencyBonusMultiplier(proficiencyIndex);
      if (bonusMultiplier != 0){
        ArrayList<String> bonusBreakdown = party.getProficiencyBonusMultiplierBreakdown(proficiencyIndex);
        t += "\nBonus breakdown:\n";
        for (int i = 0; i < bonusBreakdown.size(); i ++){
          t += bonusBreakdown.get(i);
        }
      }

      setText(t.replaceAll("\\s+$", ""));
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, String.format("Error changing tooltip to proficiencies index:%d", proficiencyIndex), e);
      throw e;
    }
  }
  
  void setHoveringParty(Party p){
    String t = String.format("Party '%s'\n", p.id);
    for (int i=0; i < p.proficiencies.length; i++){
      t += String.format("\n%s=%s", jsManager.indexToProficiencyDisplayName(i), roundDpTrailing(""+p.getTotalProficiency(i), 2));
    }
    setText(t);
  }

  void setResource(HashMap<String, Float> buildings, String resource) {
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

  void drawColouredLine(PGraphics canvas, String line, float startX, float startY, int colour, char indicatingChar) {
    int start=0, end=0;
    float tw=0;
    boolean coloured = false;
    try {
      while (end != line.length()) {
        start = end;
        if (coloured) {
          canvas.fill(colour);
          end = line.indexOf("</"+indicatingChar+">", end);
        } else {
          canvas.fill(0);
          end = line.indexOf("<"+indicatingChar+">", end);
        }
        if (end == -1) { // indexOf returns -1 when not found
          end = line.length();
        }
        canvas.text(line.substring(start, end).replace("<"+indicatingChar+">", "").replace("</"+indicatingChar+">", ""), startX+tw, startY);
        tw += canvas.textWidth(line.substring(start, end).replace("<"+indicatingChar+">", "").replace("</"+indicatingChar+">", ""));
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
      panelCanvas.fill(200, 240);
      panelCanvas.stroke(0);
      panelCanvas.rectMode(CORNER);
      panelCanvas.rect(tx, ty, tw, th);
      panelCanvas.fill(0);
      panelCanvas.textAlign(LEFT, TOP);
      for (int i=0; i<lines.size(); i++) {
        if (lines.get(i).contains("<r>")) {
          drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap, color(255,0,0), 'r');
        } else if (lines.get(i).contains("<g>")) {
          drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap, color(50,255,50), 'g');
        } else {
          panelCanvas.text(lines.get(i), tx+2, ty+i*gap);
        }
      }
    }
  }
}

class NotificationManager extends Element {
  ArrayList<ArrayList<Notification>> notifications;
  int bgColour, textColour, displayNots, notHeight, topOffset, scroll, turn, numPlayers;
  Notification lastSelected;
  boolean scrolling;

  NotificationManager(int x, int y, int w, int h, int bgColour, int textColour, int displayNots, int turn, int numPlayers) {
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
    this.numPlayers = numPlayers;
    for (int i = 0; i < numPlayers; i ++){
      notifications.add(new ArrayList<Notification>());
    }
    this.scroll = 0;
    lastSelected = null;
    scrolling = false;
  }

  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset >= y && mouseY-yOffset <= y+notHeight*(notifications.get(turn).size()+1);
  }
  boolean pointOver() {
    return moveOver();
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
    for (int i = 0; i < numPlayers; i ++){
      notifications.add(new ArrayList<Notification>());
    }
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

    if (hoveringDismissAll() && getElemOnTop()) {
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

      if (hovering == i && getElemOnTop()) {
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
  byte[] warnings;

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
    this.warnings = new byte[resNames.length];
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

  void updateWarnings(byte[] v) {
    try {
      LOGGER_MAIN.finest("Warnings update: " + Arrays.toString(v));
      warnings = v;
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.WARNING, "Error updating warnings", e);
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
      if (warnings[i] == 1) {
        panelCanvas.fill(255, 127, 0);
      } else if (warnings[i] == 2){
        panelCanvas.fill(255, 0, 0);
      }
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
  int oldH;
  boolean taskMActive;
  boolean scrolling;
  color bgColour, strokeColour;
  private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
  final int SCROLLWIDTH = 20;
  PImage[] resizedImages;

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
    oldH = -1;
  }
  
  void updateImages(){
    LOGGER_MAIN.finer("Resizing task images, h="+h);
    resizedImages = new PImage[taskImages.length];
    for (int i=0; i < taskImages.length; i ++){
      if (taskImages[i] != null){
        resizedImages[i] = taskImages[i].copy();
        resizedImages[i].resize(h, h);
      }
    }
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
      LOGGER_MAIN.warning("Could not find option to make available but over budject:"+option);
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
    return ceil(textSize*jsManager.loadFloatSetting("text scale")+5);
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
    if (h != oldH){
      updateImages();
      oldH = h;
    }
    
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
    panelCanvas.text("Current Task: "+options.get(availableOptions.get(0)), x+5+h, y);
    if (resizedImages[availableOptions.get(0)] != null){
      panelCanvas.image(resizedImages[availableOptions.get(0)], x+3, y);
    }

    // Draw other tasks
    int j;
    for (j=1; j < min(availableOptions.size()-scroll, numDisplayed); j++) {
      if (taskMActive && mouseOver(j) && getElemOnTop()) {
        panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET));
      } else {
        panelCanvas.fill(bgColour);
      }
      panelCanvas.rect(x, y+h*j, w, h);
      panelCanvas.fill(0);
      panelCanvas.text(options.get(availableOptions.get(j+scroll)), x+5+h, y+h*j);
      if (resizedImages[availableOptions.get(j+scroll)] != null){
        panelCanvas.image(resizedImages[availableOptions.get(j+scroll)], x+3, y+h*j);
      }
    }
    for (; j < min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed); j++) {
      panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET/2));
      panelCanvas.rect(x, y+h*j, w, h);
      panelCanvas.fill(120);
      panelCanvas.text(options.get(availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))), x+5+h, y+h*j);
      if (resizedImages[availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))] != null){
        panelCanvas.image(resizedImages[availableButOverBudgetOptions.get(j-min(availableOptions.size()-scroll, numDisplayed))], x+3, y+h*j);
      }
    }

    //draw scroll
    int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
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
    int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
    if (eventType == "mouseMoved") {
      taskMActive = moveOver();
    }
    if (eventType == "mouseClicked" && button == LEFT) {
      for (int j=1; j < availableOptions.size(); j++) {
        if (mouseOver(j)) {
          if (d <= 0 || mouseX-xOffset<x+w-SCROLLWIDTH) {
            selectAt(j+scroll);
            events.add("valueChanged");
            scrolling = false;
          }
        }
      }
    } else if (eventType.equals("mousePressed")) {
      if (hovingOverScroll()) {
        // If hovering over scroll bar, set scroll to mouse pos
        scrolling = true;
        scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/(h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed)), d));
      } else {
        scrolling = false;
      }
    } else if (eventType.equals("mouseDragged")) {
      if (scrolling && d > 0) {
        // If scrolling, set scroll to mouse pos
        scroll = round(between(0, (mouseY-y-yOffset)*(d+1)/(h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed)), d));
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
        if (availableOptions.size() + availableButOverBudgetOptions.size() > numDisplayed) {
          scroll = round(between(0, scroll+count, availableOptions.size() + availableButOverBudgetOptions.size()-numDisplayed));
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
        return options.get(availableOptions.get(0));
      }
      for (j=0; j<min(availableOptions.size()-scroll, numDisplayed); j++) {
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
  boolean hovingOverScroll(){
    int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
    return d > 0 && moveOver() && mouseX-xOffset>x+w-SCROLLWIDTH;
  }

  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w && mouseY-yOffset > y && mouseY-yOffset < y+h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed);
  }
  boolean pointOver() {
    return moveOver();
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
    } else if (state == "hovering" && getElemOnTop()) {
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
  boolean pointOver() {
    return mouseOver();
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
  boolean pointOver() {
    return mouseOver();
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
  boolean pointOver() {
    return mouseOver();
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
  boolean pointOver() {
    return mouseOver();
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
  boolean pointOver() {
    return mouseOver();
  }
}

class BombardButton extends Button {
  PImage img;
  BombardButton (int x, int y, int w, color bgColour) {
    super(x, y, w, w, bgColour, color(0), color(0), 1, 0, "");
    img = bombardImage;
  }

  void draw(PGraphics panelCanvas) {
    super.draw(panelCanvas);
    panelCanvas.image(img, super.x+2, super.y+2);
  }
}

class ResourceManagementTable extends Element {
  private int page;
  String[][] headings;
  ArrayList<ArrayList<String>> names;
  ArrayList<ArrayList<Float>> production, consumption, net, storage;
  int pages;
  int rows;
  int TEXTSIZE = 13;
  HashMap<String, PImage> tempEquipmentImages;
  int rowThickness;
  int rowGap;
  int columnGap;
  int headerSize;
  int imgHeight;

  ResourceManagementTable(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    pages = 2;
    names = new ArrayList<ArrayList<String>>();
    headings = new String[pages][];
    tempEquipmentImages = new HashMap<String, PImage>();
    rowThickness = ceil(TEXTSIZE*1.6*jsManager.loadFloatSetting("text scale"));
    imgHeight = rowThickness;
    rowGap = ceil(TEXTSIZE/4*jsManager.loadFloatSetting("text scale"));
    columnGap = ceil(TEXTSIZE*jsManager.loadFloatSetting("text scale"));
    headerSize = ceil(1.3*TEXTSIZE*jsManager.loadFloatSetting("text scale"));
    resizeImages();
  }

  void update(String[][] headings, 
  ArrayList<ArrayList<String>> names, 
  ArrayList<ArrayList<Float>> production, 
  ArrayList<ArrayList<Float>> consumption,
  ArrayList<ArrayList<Float>> net,
  ArrayList<ArrayList<Float>> storage) {
    this.names = names;
    this.production = production;
    this.consumption = consumption;
    this.net = net;
    this.storage = storage;
    this.headings = headings;
    this.rows = names.get(page).size();
    rowThickness = ceil(TEXTSIZE*1.6*jsManager.loadFloatSetting("text scale"));
    rowGap = ceil(TEXTSIZE/4*jsManager.loadFloatSetting("text scale"));
    columnGap = ceil(TEXTSIZE*jsManager.loadFloatSetting("text scale"));
    headerSize = ceil(1.3*TEXTSIZE*jsManager.loadFloatSetting("text scale"));
    resizeImages();
  }
  
  void setPage(int p) {
    page = p;
    this.rows = names.get(page).size();
  }

  void draw(PGraphics canvas) {
    canvas.fill(0);
    canvas.textAlign(LEFT, BOTTOM);
    canvas.textSize(headerSize);
    
    float[] cumulativeWidth = new float[headings[page].length+1];
    for (int i = 0; i < headings[page].length; i++) {
      cumulativeWidth[i+1] = canvas.textWidth(headings[page][i]) + columnGap + cumulativeWidth[i];
    }
    
    float[] headingXs = new float[headings[page].length];
    for (int i = 0; i < headings[page].length; i++) {
      float headingX;
      if (i == 0) {
        headingX = x+cumulativeWidth[i];
      } else {
        headingX = w-x-cumulativeWidth[headings[page].length]+cumulativeWidth[i];
      }
      headingXs[i] = headingX;
      canvas.text(headings[page][i], headingX, y+headerSize);
      canvas.line(headingX, y+headerSize, headingX+canvas.textWidth(headings[page][i]), y+headerSize);
    }
    
    
    int yPos = y+headerSize+2*rowGap;
    for (int i = 0; i < rows; i++) {
      canvas.fill(150);
      canvas.rect(x, yPos+i*(rowThickness+rowGap), w, rowThickness);
      canvas.fill(0);
      canvas.textSize(TEXTSIZE*jsManager.loadFloatSetting("text scale"));
      int offset = 0;
      int startColumn = 0;
      if (page == 1) {
        canvas.image(tempEquipmentImages.get(names.get(page).get(i)), x+2, yPos+i*(rowThickness+rowGap));
        offset = int(imgHeight/0.75);
        canvas.text(
          jsManager.getEquipmentClass(jsManager.getEquipmentTypeClassFromID(names.get(page).get(i))[0]),
          headingXs[1], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
        startColumn = 1;
      }
      canvas.text(names.get(page).get(i), x+offset+columnGap, yPos+(i+1)*(rowThickness+rowGap) - rowGap);
      canvas.fill(0, 255, 0);
      canvas.text(production.get(page).get(i), headingXs[startColumn+1], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
      canvas.fill(255, 0, 0);
      canvas.text(consumption.get(page).get(i), headingXs[startColumn+2], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
      canvas.fill(0);
      canvas.text(net.get(page).get(i), headingXs[startColumn+3], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
      canvas.text(storage.get(page).get(i), headingXs[startColumn+4], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
    }
  }
  
  void resizeImages(){
    // Resize equipment icons
    for (int c=0; c < jsManager.getNumEquipmentClasses(); c++){
      for (int t=0; t < jsManager.getNumEquipmentTypesFromClass(c); t++){
        try{
          String id = jsManager.getEquipmentTypeID(c, t);
          tempEquipmentImages.put(id, equipmentImages.get(id).copy());
          tempEquipmentImages.get(id).resize(ceil(float(imgHeight)/0.75), imgHeight);
        }
        catch (NullPointerException e){
          LOGGER_MAIN.log(Level.SEVERE, String.format("Error resizing image for equipment icon class:%d, type:%d, id:%s", c, t, jsManager.getEquipmentTypeID(c, t)), e);
          throw e;
        }
      }
    }
  }
}

class HorizontalOptionsButton extends DropDown {
  HorizontalOptionsButton(int x, int y, int w, int h, int bgColour, int textSize, String[] options) {
    super(x, y, w, h, bgColour, "", "", textSize);
    setOptions(options);
    expanded = true;
  }

  void draw(PGraphics canvas) {
    int hovering = hoveringOption();
    canvas.pushStyle();

    // draw selected option
    canvas.stroke(color(0));
    if (moveOver() && hovering == -1 && getElemOnTop()) {  // Hovering over top selected option
      canvas.fill(brighten(bgColour, -20));
    } else {
      canvas.fill(brighten(bgColour, -40));
    }
    canvas.rect(x, y, w, h);
    canvas.textAlign(LEFT, TOP);
    canvas.textFont(getFont((min(h*0.8, textSize))*jsManager.loadFloatSetting("text scale")));
    canvas.fill(color(0));

    // Draw expand box
    canvas.line(x+w-h, y+1, x+w-h/2, y+h-1);
    canvas.line(x+w-h/2, y+h-1, x+w, y+1);

    int boxX = x;
    for (int i=0; i < options.length; i++) {
      if (i == selected) {
        canvas.fill(brighten(bgColour, 50));
      } else {
        if (moveOver() && i == hovering && getElemOnTop()) {
          canvas.fill(brighten(bgColour, 20));
        } else {
          canvas.fill(bgColour);
        }
      }
      canvas.rect(boxX, y, w, h);
      if (i == selected) {
        canvas.fill(brighten(bgColour, 20));
      } else {
        canvas.fill(0);
      }
      canvas.text(options[i], boxX+3, y);
      boxX += w;
    }
    canvas.popStyle();
  }
  boolean moveOver() {
    return mouseX-xOffset >= x && mouseX-xOffset <= x+w*(options.length) && mouseY-yOffset >= y && mouseY-yOffset < y+h;
  }

  ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")) {
      int hovering = hoveringOption();
      if (moveOver()) {
        if (hovering != -1) {
          events.add("valueChanged");
          selected = hovering;
          contract();
          events.add("stop events");
        }
      }
    }
    return events;
  }

  int hoveringOption() {
    return (mouseX-xOffset-x)/w;
  }
}
