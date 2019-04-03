package ui.element;

import json.JSONManager;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.min;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Button extends MultiLineTextBox {
    private String state;
    private int defaultBgColour;

    public Button(int x, int y, int w, int h, int bgColour, int strokeColour, int textColour, int textSize, int textAlignx, int textAligny, String text) {
        super(x, y, w, h, bgColour, strokeColour, textColour, textSize, textAlignx, textAligny, text);
        defaultBgColour = bgColour;
        state = "off";
    }

    public void draw(PGraphics panelCanvas) {
        float r = red(defaultBgColour), g = green(defaultBgColour), b = blue(defaultBgColour);
        if (state.equals("hovering") && getElemOnTop()) {
            int HOVERINGOFFSET = 80;
            bgColour = color(min(r+ HOVERINGOFFSET, 255), min(g+ HOVERINGOFFSET, 255), min(b+ HOVERINGOFFSET, 255));
        } else if (state.equals("on")) {
            int ONOFFSET = -50;
            bgColour = color(min(r+ ONOFFSET, 255), min(g+ ONOFFSET, 255), min(b+ ONOFFSET, 255));
        } else {
            bgColour = defaultBgColour;
        }
        super.draw(panelCanvas);
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

    public boolean mouseOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
        return mouseOver();
    }
}
