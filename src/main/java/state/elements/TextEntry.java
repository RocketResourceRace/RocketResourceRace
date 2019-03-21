package state.elements;

import json.JSONManager;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static processing.core.PConstants.LEFT;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class TextEntry extends Element {
    StringBuilder text;
    int x, y, w, h, textSize, textAlign, cursor, selected;
    int textColour, boxColour, borderColour, selectionColour;
    String allowedChars, name;
    final int BLINKTIME = 500;
    boolean texActive;

    TextEntry(int x, int y, int w, int h, int textAlign, int textColour, int boxColour, int borderColour, String allowedChars) {
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
    public TextEntry(int x, int y, int w, int h, int textAlign, int textColour, int boxColour, int borderColour, String allowedChars, String name) {
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

    public void setText(String t) {
        LOGGER_MAIN.finest("Changing text to: " + t);
        this.text = new StringBuilder(t);
    }
    public String getText() {
        return this.text.toString();
    }

    public void draw(PGraphics panelCanvas) {
        boolean showCursor = ((papplet.millis()/BLINKTIME)%2==0 || papplet.keyPressed) && texActive;
        panelCanvas.pushStyle();

        // Draw a box behind the text
        panelCanvas.fill(boxColour);
        panelCanvas.stroke(borderColour);
        panelCanvas.rect(x, y, w, h);
        panelCanvas.textFont(getFont(textSize* JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(textAlign);
        // Draw selection box
        if (selected != cursor && texActive && cursor >= 0 ) {
            panelCanvas.fill(selectionColour);
            panelCanvas.rect(x+panelCanvas.textWidth(text.substring(0, min(cursor, selected)))+5, y+2, panelCanvas.textWidth(text.substring(min(cursor, selected), max(cursor, selected))), h-4);
        }

        // Draw the text
        panelCanvas.textFont(getFont(textSize*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(textAlign);
        panelCanvas.fill(textColour);
        panelCanvas.text(text.toString(), x+5, y+(h-textSize*JSONManager.loadFloatSetting("text scale"))/2, w, h);

        // Draw cursor
        if (showCursor) {
            panelCanvas.fill(0);
            panelCanvas.noStroke();
            panelCanvas.rect(x+panelCanvas.textWidth(text.toString().substring(0, cursor))+5, y+(h-textSize*JSONManager.loadFloatSetting("text scale"))/2, 1, textSize*JSONManager.loadFloatSetting("text scale"));
        }
        if (name != null) {
            panelCanvas.fill(0);
            panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
            panelCanvas.textAlign(LEFT);
            panelCanvas.text(name, x, y-12);
        }

        panelCanvas.popStyle();
    }

    public void resetSelection() {
        selected = cursor;
    }
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public int getCursorPos(int mx, int my) {
        try {
            int i=0;
            for (; i<text.length(); i++) {
                papplet.textFont(getFont(textSize*JSONManager.loadFloatSetting("text scale")));
                if ((papplet.textWidth(text.substring(0, i)) + papplet.textWidth(text.substring(0, i+1)))/2 + x > mx)
                    break;
            }
            if (0 <= i && i <= text.length() && y+(h-textSize*JSONManager.loadFloatSetting("text scale"))/2<= my && my <= y+(h-textSize*JSONManager.loadFloatSetting("text scale"))/2+textSize*JSONManager.loadFloatSetting("text scale")) {
                return i;
            }
            return cursor;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting cursor position", e);
            throw e;
        }
    }

    public void doubleSelectWord() {
        try {
            if (!(y <= papplet.mouseY-yOffset && papplet.mouseY-yOffset <= y+h)) {
                return;
            }
            int c = getCursorPos(papplet.mouseX-xOffset, papplet.mouseY-yOffset);
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

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<String>();
        if (eventType.equals("mouseClicked")) {
            if (button == LEFT) {
                if (mouseOver()) {
                    texActive = true;
                }
            }
        } else if (eventType.equals("mousePressed")) {
            if (button == LEFT) {
                cursor = round(between(0, getCursorPos(papplet.mouseX-xOffset, papplet.mouseY-yOffset), text.length()));
                selected = getCursorPos(papplet.mouseX-xOffset, papplet.mouseY-yOffset);
            }
            if (!mouseOver()) {
                texActive = false;
            }
        } else if (eventType.equals("mouseDragged")) {
            if (button == LEFT) {
                selected = getCursorPos(papplet.mouseX-xOffset, papplet.mouseY-yOffset);
            }
        } else if (eventType.equals("mouseDoubleClicked")) {
            doubleSelectWord();
        }
        return events;
    }

    public ArrayList<String> keyboardEvent(String eventType, char _key) {
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
            } else if (eventType.equals("keyPressed")) {
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
                    if (papplet.keyCode == LEFT) {
                        cursor = max(0, cursor-1);
                        resetSelection();
                    }
                    if (papplet.keyCode == RIGHT) {
                        cursor = min(text.length(), cursor+1);
                        resetSelection();
                    }
                }
            }
        }
        return events;
    }

    public Boolean mouseOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
        return mouseOver();
    }
}
