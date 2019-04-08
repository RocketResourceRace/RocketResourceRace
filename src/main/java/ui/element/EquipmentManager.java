package ui.element;

import json.JSONManager;
import party.Party;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import ui.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import static util.Font.getFont;
import static util.Image.resourceImages;
import static util.Logging.LOGGER_GAME;
import static util.Logging.LOGGER_MAIN;
import static util.Util.between;
import static util.Util.papplet;

public class EquipmentManager extends Element {
    private final float BIGIMAGESIZE = 0.5f;
    private String[] equipmentClassDisplayNames;
    private int[] currentEquipment, currentEquipmentQuantities;
    private int currentUnitNumber;
    private float boxWidth, boxHeight;
    private int selectedClass;
    private float dropX, dropY, dropW, dropH, oldDropH;
    private ArrayList<int[]> equipmentToChange;
    private HashMap<String, PImage> tempEquipmentImages;
    private HashMap<String, PImage> bigTempEquipmentImages;

    public EquipmentManager(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;

        // Load display names for equipment classes
        equipmentClassDisplayNames = new String[JSONManager.getNumEquipmentClasses()];
        for (int i = 0; i < equipmentClassDisplayNames.length; i ++) {
            equipmentClassDisplayNames[i] = JSONManager.getEquipmentClassDisplayName(i);
        }

        currentEquipment = new int[JSONManager.getNumEquipmentClasses()];
        currentEquipmentQuantities = new int[JSONManager.getNumEquipmentClasses()];
        currentUnitNumber = 0;
        for (int i=0; i<currentEquipment.length;i ++){
            currentEquipment[i] = -1; // -1 represens no equipment
            currentEquipmentQuantities[i] = 0;
        }

        updateSizes();

        selectedClass = -1;  // -1 represents nothing being selected
        equipmentToChange = new ArrayList<>();
        tempEquipmentImages = new HashMap<>();
        bigTempEquipmentImages = new HashMap<>();
        oldDropH = 0;
    }

    private void updateSizes(){

        boxWidth = w/ JSONManager.getNumEquipmentClasses();
        float BOXWIDTHHEIGHTRATIO = 0.75f;
        boxHeight = boxWidth* BOXWIDTHHEIGHTRATIO;
        updateDropDownPositionAndSize();
    }

    private void updateDropDownPositionAndSize(){
        dropY = y+boxHeight;
        float EXTRATYPEWIDTH = 1.5f;
        dropW = boxWidth * EXTRATYPEWIDTH;
        dropH = JSONManager.loadFloatSetting("gui scale") * 32;
        dropX = between(x, x+boxWidth*selectedClass-(dropW-boxWidth)/2, x+w-dropW);
    }


    private void resizeImages(){
        // Resize equipment icons
        // Resize icons
        for (int r=0; r < JSONManager.getNumResourceTypes(); r++) {
            String id = JSONManager.getResString(r);
            try {
                tempEquipmentImages.put(id, resourceImages.get(id).copy());
                tempEquipmentImages.get(id).resize(PApplet.parseInt(dropH / 0.75f), PApplet.parseInt(dropH - 1));
                bigTempEquipmentImages.put(id, resourceImages.get(id).copy());
                bigTempEquipmentImages.get(id).resize(PApplet.parseInt(boxWidth * BIGIMAGESIZE), PApplet.parseInt(boxHeight * BIGIMAGESIZE));
            } catch (NullPointerException e) {
                LOGGER_MAIN.log(Level.SEVERE, String.format("Error resizing image for resource icon id:%s", id), e);
                throw e;
            }
        }
    }

    public void transform(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;

        updateSizes();
    }

    public void setEquipment(Party party) {
        this.currentEquipment = party.getAllEquipment();
        LOGGER_GAME.finer(String.format("changing equipment for manager to :%s", Arrays.toString(party.getAllEquipment())));
        currentEquipmentQuantities = party.getEquipmentQuantities();
        currentUnitNumber = party.getUnitNumber();
    }

    public float getBoxHeight(){
        return boxHeight;
    }

    public ArrayList<int[]> getEquipmentToChange(){
        // Also clears equipmentToChange
        ArrayList<int[]> temp = new ArrayList<>(equipmentToChange);
        equipmentToChange.clear();
        return temp;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mouseClicked")) {
            boolean[] blockedClasses = getBlockedClasses();
            if (mouseOverClasses()) {
                if (button == PConstants.LEFT){
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
                else if (button == PConstants.RIGHT){  // Unequip if right clicking on class
                    events.add("valueChanged");
                    events.add("stop events");
                    equipmentToChange.add(new int[] {selectedClass, -1});
                }
            }
            else if (mouseOverTypes()){
                int newSelectedType = hoveringOverType();
                if (newSelectedType == JSONManager.getNumEquipmentTypesFromClass(selectedClass)){
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

    public int getSelectedClass(){
        return selectedClass;
    }

    public void drawShadow(PGraphics panelCanvas, float shadowX, float shadowY, float shadowW, float shadowH){
        panelCanvas.noStroke();
        int SHADOWSIZE = 20;
        for (int i = SHADOWSIZE; i > 0; i --){
            panelCanvas.fill(0, 255-255* PApplet.pow(((float)i/ SHADOWSIZE), 0.1f));
            //panelCanvas.rect(shadowX-i, shadowY-i, shadowW+i*2, shadowH+i*2, i);
        }
    }

    private boolean[] getBlockedClasses(){
        boolean[] blockedClasses = new boolean[JSONManager.getNumEquipmentClasses()];
        for (int i = 0; i < blockedClasses.length; i ++) {
            if (currentEquipment[i] != -1){
                String[] otherBlocking = JSONManager.getOtherClassBlocking(i, currentEquipment[i]);
                if (otherBlocking != null){
                    for (String s : otherBlocking) {
                        int classIndex = JSONManager.getEquipmentClassFromID(s);
                        blockedClasses[classIndex] = true;
                    }
                }
            }
        }
        return blockedClasses;
    }

    private boolean[] getHoveringBlockedClasses(){
        boolean[] blockedClasses = new boolean[JSONManager.getNumEquipmentClasses()];
        if (selectedClass != -1){
            if (hoveringOverType() != -1 && hoveringOverType() < JSONManager.getNumEquipmentTypesFromClass(selectedClass)){
                String[] otherBlocking = JSONManager.getOtherClassBlocking(selectedClass, hoveringOverType());
                if (otherBlocking != null){
                    for (String s : otherBlocking) {
                        int classIndex = JSONManager.getEquipmentClassFromID(s);
                        blockedClasses[classIndex] = true;
                    }
                }
            }
        }
        return blockedClasses;
    }

    public void draw(PGraphics panelCanvas) {

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

        int TEXTSIZE = 8;
        for (int i = 0; i < JSONManager.getNumEquipmentClasses(); i ++) {
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
                    panelCanvas.image(bigTempEquipmentImages.get(JSONManager.getEquipmentTypeID(i, currentEquipment[i])), PApplet.parseInt(x+boxWidth*i)+(1-BIGIMAGESIZE)*boxWidth/2, y+(1-BIGIMAGESIZE)*boxHeight/2);
                }
                panelCanvas.fill(0);
                panelCanvas.textAlign(PConstants.CENTER, PConstants.TOP);
                panelCanvas.textFont(getFont(TEXTSIZE * JSONManager.loadFloatSetting("text scale")));
                panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5f), y);
                if (currentEquipment[i] != -1){
                    panelCanvas.textFont(getFont((TEXTSIZE -1)* JSONManager.loadFloatSetting("text scale")));
                    panelCanvas.textAlign(PConstants.CENTER, PConstants.BOTTOM);
                    panelCanvas.text(JSONManager.getEquipmentTypeDisplayName(i, currentEquipment[i]), x+boxWidth*(i+0.5f), y+boxHeight);
                    if (currentEquipmentQuantities[i] < currentUnitNumber){
                        panelCanvas.fill(255, 0, 0);
                    }
                    panelCanvas.text(String.format("%d/%d", currentEquipmentQuantities[i], currentUnitNumber), x+boxWidth*(i+0.5f), y+boxHeight- TEXTSIZE * JSONManager.loadFloatSetting("text scale")+5);
                }
            }
            else{
                panelCanvas.strokeWeight(2);
                panelCanvas.fill(80);
                panelCanvas.rect(x+boxWidth*i, y, boxWidth, boxHeight);
                panelCanvas.fill(0);
                panelCanvas.textAlign(PConstants.CENTER, PConstants.TOP);
                panelCanvas.textFont(getFont(TEXTSIZE * JSONManager.loadFloatSetting("text scale")));
                panelCanvas.text(equipmentClassDisplayNames[i], x+boxWidth*(i+0.5f), y);
            }
        }

        // Draw dropdown if an equipment class is selected
        panelCanvas.stroke(0);
        if (selectedClass != -1){
            panelCanvas.textAlign(PConstants.LEFT, PConstants.TOP);
            panelCanvas.textFont(getFont(TEXTSIZE * JSONManager.loadFloatSetting("text scale")));
            String[] equipmentTypes = JSONManager.getEquipmentFromClass(selectedClass);
            for (int i = 0; i < JSONManager.getNumEquipmentTypesFromClass(selectedClass); i ++){
                panelCanvas.strokeWeight(1);
                if (currentEquipment[selectedClass] != i){
                    panelCanvas.fill(170);
                    panelCanvas.rect(dropX, dropY+i*dropH, dropW, dropH);
                    panelCanvas.fill(0);
                    panelCanvas.text(equipmentTypes[i], 3+dropX, dropY+i*dropH);
                    try{
                        panelCanvas.image(tempEquipmentImages.get(JSONManager.getEquipmentTypeID(selectedClass, i)), dropX+dropW-dropH/0.75f-2, dropY+dropH*i+2);
                    }
                    catch (NullPointerException e){
                        LOGGER_MAIN.log(Level.WARNING, String.format("Error drawing image for equipment icon class:%d, type:%d, id:%s", selectedClass, i, JSONManager.getEquipmentTypeID(selectedClass, i)), e);
                    }
                }
                else{
                    panelCanvas.fill(220);
                    panelCanvas.rect(dropX, dropY+i*dropH, dropW, dropH);
                    panelCanvas.fill(150);
                    panelCanvas.text(equipmentTypes[i], 3+dropX, dropY+i*dropH);
                    try{
                        panelCanvas.image(tempEquipmentImages.get(JSONManager.getEquipmentTypeID(selectedClass, i)), dropX+dropW-dropH/0.75f-2, dropY+dropH*i+2);
                    }
                    catch (NullPointerException e){
                        LOGGER_MAIN.log(Level.WARNING, String.format("Error drawing image for equipment icon class:%d, type:%d, id:%s", selectedClass, i, JSONManager.getEquipmentTypeID(selectedClass, i)), e);
                    }
                }
            }
            if (currentEquipment[selectedClass] != -1){
                panelCanvas.fill(170);
                panelCanvas.rect(dropX, dropY+ JSONManager.getNumEquipmentTypesFromClass(selectedClass)*dropH, dropW, dropH);
                panelCanvas.fill(0);
                panelCanvas.text("Unequip", 3+dropX, dropY+ JSONManager.getNumEquipmentTypesFromClass(selectedClass)*dropH);
            }
        }
        if (selectedClass != -1){
            panelCanvas.strokeWeight(2);
            panelCanvas.stroke(0);
            panelCanvas.noFill();
            if (currentEquipment[selectedClass] == -1){  // If nothing equipped, there is not unequip option at the bottom
                panelCanvas.rect(dropX, dropY, dropW, dropH*(JSONManager.getNumEquipmentTypesFromClass(selectedClass))+1);
            } else{
                panelCanvas.rect(dropX, dropY, dropW, dropH*(JSONManager.getNumEquipmentTypesFromClass(selectedClass)+1)+1);
            }
        }

        panelCanvas.popStyle();
    }

    private boolean mouseOverClasses() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+boxHeight;
    }

    public boolean mouseOverTypes() {
        if (selectedClass == -1){
            return false;
        } else if (currentEquipment[selectedClass] == -1){  // If nothing equipped, there is not unequip option at the bottom
            return papplet.mouseX-xOffset >= dropX && papplet.mouseX-xOffset <= dropX+dropW && papplet.mouseY-yOffset >= dropY && papplet.mouseY-yOffset <= dropY+dropH*(JSONManager.getNumEquipmentTypesFromClass(selectedClass));
        } else{
            return papplet.mouseX-xOffset >= dropX && papplet.mouseX-xOffset <= dropX+dropW && papplet.mouseY-yOffset >= dropY && papplet.mouseY-yOffset <= dropY+dropH*(JSONManager.getNumEquipmentTypesFromClass(selectedClass)+1);
        }
    }

    public int hoveringOverType() {
        int num = JSONManager.getNumEquipmentTypesFromClass(selectedClass);
        for (int i = 0; i < num+1; i ++) {
            if (papplet.mouseX-xOffset >= dropX && papplet.mouseX-xOffset <= dropX+dropW && papplet.mouseY-yOffset >= dropY+dropH*i && papplet.mouseY-yOffset <= dropY+dropH*(i+1)){
                return i;
            }
        }
        return -1;
    }

    private int hoveringOverClass() {
        for (int i = 0; i < JSONManager.getNumEquipmentClasses(); i ++) {
            if (papplet.mouseX-xOffset >= x+boxWidth*i && papplet.mouseX-xOffset <= x+boxWidth*(i+1) && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+boxHeight){
                return i;
            }
        }
        return -1;
    }

    public boolean pointOver() {
        return mouseOverTypes() || mouseOverClasses();
    }
}
