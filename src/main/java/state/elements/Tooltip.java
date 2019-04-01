package state.elements;

import json.JSONManager;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;

import static processing.core.PApplet.ceil;
import static processing.core.PApplet.round;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;
import static util.Util.between;
import static util.Util.papplet;

public class Tooltip extends MultiLineTextBox {
    ArrayList<TooltipElement> elements;
    public Tooltip() {
        super(0, 0, -1, -1, papplet.color(200, 240), 0, 0, (int)(8* JSONManager.loadFloatSetting("text scale")), LEFT, TOP, "");
        hide();
        elements = new ArrayList<>();
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
        for (TooltipElement te: elements) {
            if (te.isEnabled() && te.mouseOver()) {
                setText(te.getText());
                show();
                return;
            }
        }
        hide();
    }

    public void addElement(Element e, String tooltip) {
        elements.add(new TooltipElement(e, tooltip));
    }

    public void enableElement(Element e) {
        for (TooltipElement te: elements) {
            if (te.getElement().equals(e)) {
                te.setEnabled(true);
                return;
            }
        }
    }

    public void disableElement(Element e) {
        for (TooltipElement te: elements) {
            if (te.getElement().equals(e)) {
                te.setEnabled(false);
                return;
            }
        }
    }
    private class TooltipElement {
        private Element e;
        private String s;
        private boolean enabled;

        TooltipElement(Element e, String tooltip) {
            this.e = e;
            s = tooltip;
            enabled = true;
        }

        public boolean mouseOver() {
            return e.mouseOver();
        }

        public String getText() {
            return s;
        }

        public void setText(String s) {
            this.s = s;
        }

        boolean isEnabled() {
            return enabled;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Element getElement() {
            return e;
        }
    }
}
