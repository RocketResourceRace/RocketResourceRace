package state;


import processing.core.PGraphics;
import processing.core.PImage;

import java.util.ArrayList;

import static processing.core.PConstants.P2D;
import static util.Logging.LOGGER_MAIN;
import static util.Util.loadImage;
import static util.Util.papplet;

public class Panel {
    public ArrayList<Element> elements;
    String id;
    private PImage img;
    private Boolean visible;
    Boolean blockEvent;
    Boolean overrideBlocking;
    protected int x;
    public int y;
    private int w;
    private int h;
    private int bgColour, strokeColour;
    private PGraphics panelCanvas;

    Panel(String id, int x, int y, int w, int h, Boolean visible, Boolean blockEvent, int bgColour, int strokeColour) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.visible = visible;
        this.blockEvent = blockEvent;
        this.id = id;
        this.bgColour = bgColour;
        this.strokeColour = strokeColour;
        elements = new ArrayList<>();
        panelCanvas = papplet.createGraphics(w, h, P2D);
        overrideBlocking = false;
    }

    Panel(String id, int x, int y, int w, int h, Boolean visible, String fileName, int strokeColour) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.visible = visible;
        this.id = id;
        this.img = loadImage(fileName);
        this.strokeColour = strokeColour;
        elements = new ArrayList<>();
        panelCanvas = papplet.createGraphics(w, h, P2D);
        overrideBlocking = false;
    }

    public void setOverrideBlocking(boolean v) {
        overrideBlocking = v;
    }

    private void setOffset() {
        for (Element elem : elements) {
            elem.setOffset(x, y);
        }
    }
    public void setColour(int c) {
        bgColour = c;
        LOGGER_MAIN.finest("Colour changed");
    }

    public void setVisible(boolean a) {
        // Sets the visibility of all elements within the panel as well (this may be changed)

        visible = a;
        for (Element elem : elements) {
            elem.setVisible(a);
            elem.mouseEvent("mouseMoved", papplet.mouseButton);

        }
        LOGGER_MAIN.finest("Visiblity changed to " + a);
    }
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        setOffset();
        panelCanvas = papplet.createGraphics(w+1, h+1, P2D);
        LOGGER_MAIN.finest("Panel transformed");
    }

    public void draw() {
        panelCanvas.beginDraw();
        panelCanvas.clear();
        panelCanvas.pushStyle();
        if (img == null) {
            if (bgColour != papplet.color(255, 255)) {
                panelCanvas.fill(bgColour);
                panelCanvas.stroke(strokeColour);
                panelCanvas.rect(0, 0, w, h);
            }
        } else {
            //imageMode(CENTER);
            panelCanvas.image(img, 0, 0, w, h);
        }
        panelCanvas.popStyle();

        for (Element elem : elements) {
            if (elem.isVisible()) {
                elem.draw(panelCanvas);
            }
        }
        panelCanvas.endDraw();
        papplet.image(panelCanvas, x, y);
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

    public Boolean mouseOver() {
        return papplet.mouseX >= x && papplet.mouseX <= x+w && papplet.mouseY >= y && papplet.mouseY <= y+h;
    }

    public Boolean isVisible() {
        return visible;
    }
}
