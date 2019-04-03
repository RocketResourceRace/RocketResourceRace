package ui.element;

import json.JSONManager;
import processing.core.PFont;
import processing.core.PGraphics;
import ui.Element;

import static processing.core.PApplet.ceil;
import static processing.core.PConstants.TOP;
import static util.Font.getFont;
import static util.Util.papplet;

public class Text extends Element {
    private int x;
    public int y;
    private int size;
    private int colour;
    private int align;
    private PFont font;
    public String text;

    public Text(int x, int y, int size, String text, int colour, int align) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.text = text;
        this.colour = colour;
        this.align = align;
    }
    public void translate(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public void setText(String text) {
        this.text = text;
    }
    private void calcSize(PGraphics panelCanvas) {
        panelCanvas.textFont(getFont(size* JSONManager.loadFloatSetting("text scale")));
        this.w = ceil(panelCanvas.textWidth(text));
        this.h = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
    }
    public void draw(PGraphics panelCanvas) {
        calcSize(panelCanvas);
        if (font != null) {
            panelCanvas.textFont(font);
        }
        panelCanvas.textAlign(align, TOP);
        panelCanvas.textFont(getFont(size*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.fill(colour);
        panelCanvas.text(text, x, y);
    }
    public boolean mouseOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
    public boolean pointOver() {
        return mouseOver();
    }
}
