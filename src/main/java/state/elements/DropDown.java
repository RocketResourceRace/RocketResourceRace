package state.elements;

import json.JSONManager;
import processing.core.PConstants;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;
import java.util.Arrays;

import static processing.core.PApplet.min;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.brighten;
import static util.Util.papplet;

public class DropDown extends Element {
    String[] options;  // Either strings or floats
    public int selected;
    int bgColour;
    int textSize;
    String name;
    public String optionTypes;
    protected boolean expanded;
    private boolean postExpandedEvent;

    public DropDown(int x, int y, int w, int h, int bgColour, String name, String optionTypes, int textSize) {
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

    public void setOptions(String[] options) {
        this.options = options;
        LOGGER_MAIN.finer("Options changed to: " + Arrays.toString(options));
    }

    public void setValue(String value) {
        for (int i=0; i < options.length; i++) {
            if (value.equals(options[i])) {
                selected = i;
                return;
            }
        }
        LOGGER_MAIN.warning("Invalid value, "+ value);
    }

    public void draw(PGraphics panelCanvas) {
        int hovering = hoveringOption();
        panelCanvas.pushStyle();

        // draw selected option
        panelCanvas.stroke(papplet.color(0));
        if (moveOver() && hovering == -1 && getElemOnTop()) {  // Hovering over top selected option
            panelCanvas.fill(brighten(bgColour, -20));
        } else {
            panelCanvas.fill(brighten(bgColour, -40));
        }
        panelCanvas.rect(x, y, w, h);
        panelCanvas.textAlign(PConstants.LEFT, PConstants.TOP);
        panelCanvas.textFont(getFont((min(h*0.8f, textSize))* JSONManager.loadFloatSetting("text scale")));
        panelCanvas.fill(papplet.color(0));
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

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
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

    public void setSelected(String s) {
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

    public void expand() {
        postExpandedEvent = true;
        expanded = true;
    }

    private void toggleExpanded() {
        expanded = !expanded;
        if (expanded) {
            postExpandedEvent = true;
        }
    }

    public int getIntVal() {
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

    public String getStrVal() {
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

    public float getFloatVal() {
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

    public int getOptionIndex() {
        return selected;
    }

    public boolean moveOver() {
        if (expanded) {
            return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset < y+h*(options.length+1);
        } else {
            return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset < y+h;
        }
    }
    public boolean pointOver() {
        return moveOver();
    }

    public int hoveringOption() {
        if (!expanded) {
            return -1;
        }
        return (papplet.mouseY-yOffset-y)/h-1;
    }
}
