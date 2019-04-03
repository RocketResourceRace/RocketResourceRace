package ui.element;

import json.JSONManager;
import processing.core.PGraphics;
import ui.Element;

import java.util.ArrayList;

import static processing.core.PConstants.BOTTOM;
import static processing.core.PConstants.LEFT;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class ToggleButton extends Element {
    private int bgColour, strokeColour;
    String name;
    private boolean on;
    public ToggleButton(int x, int y, int w, int h, int bgColour, int strokeColour, boolean value, String name) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bgColour = bgColour;
        this.strokeColour = strokeColour;
        this.name = name;
        this.on = value;
    }
    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mouseClicked") &&mouseOver()) {
            events.add("valueChanged");
            on = !on;
        }
        return events;
    }
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    public boolean getState() {
        return on;
    }
    public void setState(boolean state) {
        LOGGER_MAIN.finest("Setting toggle states to: " + state);
        on = state;
    }
    public void draw(PGraphics panelCanvas) {
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
        panelCanvas.textFont(getFont(8* JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(LEFT, BOTTOM);
        panelCanvas.text(name, x, y);
        panelCanvas.popStyle();
    }
    public boolean mouseOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
        return mouseOver();
    }
}
