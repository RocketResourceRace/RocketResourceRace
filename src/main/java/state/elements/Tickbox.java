package state.elements;

import json.JSONManager;
import processing.core.PConstants;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;

import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class Tickbox extends Element {
    private boolean val;
    String name;

    public Tickbox(int x, int y, int w, int h, boolean defaultVal, String name) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.val = defaultVal;
        this.name = name;
    }

    private void toggle() {
        val = !val;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mouseClicked")) {
            if (moveOver()) {
                toggle();
                events.add("valueChanged");
            }
        }
        return events;
    }

    public boolean getState() {
        return val;
    }
    public void setState(boolean state) {
        LOGGER_MAIN.finer("Tickbox states changed to: "+ state);
        val = state;
    }

    private boolean moveOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+h && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
        return moveOver();
    }

    public void draw(PGraphics panelCanvas) {
        panelCanvas.pushStyle();

        panelCanvas.fill(papplet.color(255));
        panelCanvas.stroke(papplet.color(0));
        panelCanvas.rect(x, y, h* JSONManager.loadFloatSetting("gui scale"), h* JSONManager.loadFloatSetting("gui scale"));
        if (val) {
            panelCanvas.line(x+1, y+1, x+h* JSONManager.loadFloatSetting("gui scale")-1, y+h* JSONManager.loadFloatSetting("gui scale")-1);
            panelCanvas.line(x+h* JSONManager.loadFloatSetting("gui scale")-1, y+1, x+1, y+h* JSONManager.loadFloatSetting("gui scale")-1);
        }
        panelCanvas.fill(0);
        panelCanvas.textAlign(PConstants.LEFT, PConstants.CENTER);
        panelCanvas.textSize(8* JSONManager.loadFloatSetting("text scale"));
        panelCanvas.text(name, x+h* JSONManager.loadFloatSetting("gui scale")+5, y+h* JSONManager.loadFloatSetting("gui scale")/2);
        panelCanvas.popStyle();
    }
}
