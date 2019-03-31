package state.elements;

import json.JSONManager;
import processing.core.PGraphics;

import static processing.core.PApplet.ceil;
import static processing.core.PApplet.round;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;
import static util.Util.between;
import static util.Util.papplet;

public class Tooltip extends MultiLineTextBox {
    public Tooltip() {
        super(0, 0, -1, -1, papplet.color(200, 240), 0, 0, (int)(8* JSONManager.loadFloatSetting("text scale")), LEFT, TOP, "");
        hide();
    }

    public void show() {
        visible = true;
    }
    public void hide() {
        visible = false;
    }

    public void draw(PGraphics panelCanvas) {
        int tw = ceil(maxWidthLine(panelCanvas, lines))+4;
        x = round(between(0, papplet.mouseX-xOffset-tw/2f, papplet.width-tw));
        int th = ceil(panelCanvas.textAscent()+panelCanvas.textDescent())*lines.size();
        y = round(between(0, papplet.mouseY-yOffset+20, papplet.height-th-20));
        super.draw(panelCanvas);
    }

    public void refresh() {

    }

    public void addElement(float x, float y, float w, float h, String tooltip) {
    }
}
