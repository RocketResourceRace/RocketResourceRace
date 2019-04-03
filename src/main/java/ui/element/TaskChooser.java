package ui.element;

import json.JSONManager;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;
import ui.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;
import static util.Image.taskImages;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class TaskChooser extends Element {
    private ArrayList<String> options;
    private ArrayList<Integer> availableOptions;
    private ArrayList<Integer> availableButOverBudgetOptions;
    int textSize;
    private int scroll;
    private int numDisplayed;
    private int oldH;
    private boolean taskMActive;
    public boolean scrolling;
    private int bgColour, strokeColour;
    private final int SCROLLWIDTH = 20;
    private PImage[] resizedImages;

    public TaskChooser(int x, int y, int w, int textSize, int bgColour, int strokeColour, String[] options, int numDisplayed) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.textSize = textSize;
        this.h = 10;
        this.bgColour = bgColour;
        this.strokeColour = strokeColour;
        this.options = new ArrayList<>();
        this.availableOptions = new ArrayList<>();
        this.availableButOverBudgetOptions = new ArrayList<>();
        removeAllOptions();
        Collections.addAll(this.options, options);
        resetAvailable();
        taskMActive = true;
        resetScroll();
        this.numDisplayed = numDisplayed;
        oldH = -1;
    }

    private void updateImages(){
        LOGGER_MAIN.finer("Resizing task images, h="+h);
        resizedImages = new PImage[taskImages.length];
        for (int i=0; i < taskImages.length; i ++){
            if (taskImages[i] != null){
                resizedImages[i] = taskImages[i].copy();
                resizedImages[i].resize(h, h);
            }
        }
    }

    private void resetScroll(){
        scroll = 0;
        scrolling = false;
    }

    public void setOptions(ArrayList<String> options) {
        LOGGER_MAIN.finer("Options changed to:["+String.join(", ", options));
        this.options = options;
        resetScroll();
    }
    public void addOption(String option) {
        LOGGER_MAIN.finer("Option added: " + option);
        this.options.add(option);
        resetScroll();
    }
    public void removeOption(String option) {
        LOGGER_MAIN.finer("Option removed: " + option);
        for (String option1: options) {
            if (option.equals(option1)) {
                options.remove(option1);
            }
        }
        resetScroll();
    }
    private void removeAllOptions() {
        LOGGER_MAIN.finer("Options all removed");
        this.options.clear();
        resetScroll();
    }
    public void resetAvailable() {
        LOGGER_MAIN.finer("Available Options all removed");
        this.availableOptions.clear();
        resetScroll();
    }
    public void resetAvailableButOverBudget() {
        LOGGER_MAIN.finer("Available But Over Budget Options all removed");
        this.availableButOverBudgetOptions.clear();
        resetScroll();
    }
    public String getSelected() {
        return options.get(availableOptions.get(0));
    }
    public void makeAvailable(String option) {
        try {
            LOGGER_MAIN.finer("Making option availalbe: " + option);
            for (Integer availableOption : availableOptions) {
                if (options.get(availableOption).equals(option)) {
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
    public void makeAvailableButOverBudget(String option) {
        try {
            LOGGER_MAIN.finer("Making option available but over buject: " + option);
            for (Integer availableButOverBudgetOption : availableButOverBudgetOptions) {
                if (options.get(availableButOverBudgetOption).equals(option)) {
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
    public void makeUnavailableButOverBudget(String option) {
        LOGGER_MAIN.finer("Making unavilablae but over over buject option:"+option);
        for (int i=0; i<options.size(); i++) {
            if (options.get(i).equals(option)) {
                this.availableButOverBudgetOptions.remove(i);
                return;
            }
        }
        resetScroll();
    }
    public void makeUnavailable(String option) {
        LOGGER_MAIN.finer("Making unavailable:"+option);
        for (int i=0; i<options.size(); i++) {
            if (options.get(i).equals(option)) {
                this.availableOptions.remove(i);
                return;
            }
        }
        resetScroll();
    }
    private void selectAt(int j) {
        LOGGER_MAIN.finer("Selecting based on position, " + j);
        if (j < availableOptions.size()) {
            int temp = availableOptions.get(0);
            availableOptions.set(0, availableOptions.get(j));
            availableOptions.set(j, temp);
        }
    }
    public void select(String s) {
        LOGGER_MAIN.finer("Selecting based on string: "+s);
        for (int j=0; j<availableOptions.size(); j++) {
            if (options.get(availableOptions.get(j)).equals(s)) {
                selectAt(j);
                return;
            }
        }
        LOGGER_MAIN.warning("String for selection not found: "+s);
    }
    public int getH(PGraphics panelCanvas) {
        return ceil(textSize* JSONManager.loadFloatSetting("text scale")+5);
    }
    public boolean optionAvailable(int i) {
        for (int option : availableOptions) {
            if (option == i) {
                return true;
            }
        }
        return false;
    }
    public void draw(PGraphics panelCanvas) {
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
        int ONOFFSET = -50;
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
        int HOVERINGOFFSET = 80;
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
            panelCanvas.fill(brighten(bgColour, HOVERINGOFFSET /2));
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
            panelCanvas.rect(x-SCROLLWIDTH*JSONManager.loadFloatSetting("gui scale")+w, y, SCROLLWIDTH*JSONManager.loadFloatSetting("gui scale"), h*numDisplayed);
            panelCanvas.strokeWeight(2);
            panelCanvas.fill(brighten(bgColour, -20));
            panelCanvas.rect(x-SCROLLWIDTH*JSONManager.loadFloatSetting("gui scale")+w, y+(h*numDisplayed-(h*numDisplayed)/(d+1))*scroll/d, SCROLLWIDTH*JSONManager.loadFloatSetting("gui scale"), (h*numDisplayed)/(d+1));
        }

        panelCanvas.popStyle();
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
        if (eventType.equals("mouseMoved")) {
            taskMActive = moveOver();
        }
        if (eventType.equals("mouseClicked") && button == LEFT) {
            for (int j=1; j < availableOptions.size(); j++) {
                if (mouseOver(j)) {
                    if (d <= 0 || papplet.mouseX-xOffset<x+w-SCROLLWIDTH) {
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
                scroll = round(between(0, (papplet.mouseY-y-yOffset)*(d+1)/(h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed)), d));
            } else {
                scrolling = false;
            }
        } else if (eventType.equals("mouseDragged")) {
            if (scrolling && d > 0) {
                // If scrolling, set scroll to mouse pos
                scroll = round(between(0, (papplet.mouseY-y-yOffset)*(d+1)/(h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed)), d));
            }
        } else if (eventType.equals("mouseReleased")) {
            scrolling = false;
        }
        return events;
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mouseWheel")) {
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

    public String findMouseOver() {
        try {
            int j;
            if (papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h) {
                return options.get(availableOptions.get(0));
            }
            for (j=0; j<min(availableOptions.size()-scroll, numDisplayed); j++) {
                if (papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y+h*j && papplet.mouseY-yOffset <= y+h*(j+1)) {
                    return options.get(availableOptions.get(j+scroll));
                }
            }
            for (; j<min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed); j++) {
                if (papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y+h*j && papplet.mouseY-yOffset <= y+h*(j+1)) {
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
    public boolean hovingOverScroll(){
        int d = availableOptions.size() + availableButOverBudgetOptions.size() - numDisplayed;
        return d > 0 && moveOver() && papplet.mouseX-xOffset>x+w-SCROLLWIDTH;
    }

    public boolean moveOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset > y && papplet.mouseY-yOffset < y+h*min(availableButOverBudgetOptions.size()+availableOptions.size()-scroll, numDisplayed);
    }
    public boolean pointOver() {
        return moveOver();
    }
    public boolean mouseOver(int j) {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset > y+h*j && papplet.mouseY-yOffset <= y+h*(j+1);
    }
}
