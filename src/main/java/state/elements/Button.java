package state.elements;

import json.JSONManager;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.min;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.TOP;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Button extends Element {
    protected int x;
    public int y;
    private int w;
    public int h;
    private int cx;
    private int cy;
    private int textSize;
    private int textAlign;
    public int bgColour;
    private int strokeColour;
    public int textColour;
    private String state, text;
    private final int HOVERINGOFFSET = 80, ONOFFSET = -50;
    private ArrayList<String> lines;

    public Button(int x, int y, int w, int h, int bgColour, int strokeColour, int textColour, int textSize, int textAlign, String text) {
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
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        centerCoords();
    }
    public void centerCoords() {
        cx = x+w/2;
        cy = y+h/2;
    }
    public void setText(String text) {
        LOGGER_MAIN.finer("Setting text to: " + text);
        this.text = text;
        setLines(text);
    }
    public void setColour(int colour) {
        LOGGER_MAIN.finest("Setting colour to: " + colour);
        this.bgColour = colour;
    }
    public String getText() {
        return this.text;
    }
    public void draw(PGraphics panelCanvas) {
        //println(xOffset, yOffset);
        int padding=0;
        float r = red(bgColour), g = green(bgColour), b = blue(bgColour);
        panelCanvas.pushStyle();
        panelCanvas.fill(bgColour);
        if (state.equals("off")) {
            panelCanvas.fill(bgColour);
        } else if (state.equals("hovering") && getElemOnTop()) {
            panelCanvas.fill(min(r+HOVERINGOFFSET, 255), min(g+HOVERINGOFFSET, 255), min(b+HOVERINGOFFSET, 255));
        } else if (state.equals("on")) {
            panelCanvas.fill(min(r+ONOFFSET, 255), min(g+ONOFFSET, 255), min(b+ONOFFSET, 255));
        }
        panelCanvas.stroke(strokeColour);
        panelCanvas.strokeWeight(3);
        panelCanvas.rect(x, y, w, h);
        panelCanvas.noTint();
        panelCanvas.fill(textColour);
        panelCanvas.textAlign(textAlign, TOP);
        panelCanvas.textFont(getFont(textSize* JSONManager.loadFloatSetting("text scale")));
        if (lines.size() == 1) {
            padding = h/10;
        }
        padding = (lines.size()*(int)(textSize*JSONManager.loadFloatSetting("text scale"))-h/2)/2;
        for (int i=0; i<lines.size(); i++) {
            if (textAlign == CENTER) {
                panelCanvas.text(lines.get(i), cx, y+(h*0.9f-textSize*JSONManager.loadFloatSetting("text scale"))/2);
            } else {
                panelCanvas.text(lines.get(i), x, y );
            }
        }
        panelCanvas.popStyle();
    }

    private ArrayList<String> setLines(String s) {
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
            lines.add(s.substring(j));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error setting lines", e);
            throw e;
        }
        return lines;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mouseReleased")) {
            if (state.equals("on")) {
                events.add("clicked");
            }
            state = "off";
        }
        if (mouseOver()) {
            if (!state.equals("on")) {
                state = "hovering";
            }
            if (eventType.equals("mousePressed")) {
                state = "on";
                if (JSONManager.loadBooleanSetting("sound on")) {
                    try {
//            sfx.get("click3").play();
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

    public Boolean mouseOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
        return mouseOver();
    }
}
