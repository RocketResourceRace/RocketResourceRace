package ui.element;

import json.JSONManager;
import processing.core.PGraphics;
import ui.Element;

import java.util.ArrayList;

import static processing.core.PApplet.ceil;
import static processing.core.PApplet.round;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;
import static util.Util.between;
import static util.Util.papplet;

public class Tooltip extends MultiLineTextBox {
    private ArrayList<TooltipElement> elements;
    private boolean flipped = false;
    public Tooltip() {
        super(0, 0, -1, -1, papplet.color(200, 240), 0, 0, (int)(6 * JSONManager.loadFloatSetting("text scale")), LEFT, TOP, "", 2);
        hide();
        elements = new ArrayList<>();
    }

    public void show() {
        visible = true;
        flipped = false;
    }
    void showFlipped() {
        visible = true;
        flipped = true;
    }
    public void hide() {
        visible = false;
    }

    public void draw(PGraphics panelCanvas) {
        int tw = ceil(maxWidthLine(panelCanvas, lines))+4;
        x = round(between(0, papplet.mouseX-xOffset-tw/2f, (float) (papplet.width-tw*1.1)));
        float th = (ceil(textSize*JSONManager.loadFloatSetting("text scale") + 4))*lines.size();
        if (flipped) {
            y = round(between(th, papplet.mouseY - yOffset - th-16, papplet.height));
        } else {
            y = round(between(0, papplet.mouseY - yOffset, papplet.height - th - 25) + 20);
        }
        super.draw(panelCanvas);
    }

    public void refresh() {
        for (TooltipElement te: elements) {
            if (te.isEnabled() && te.mouseOver()) {
                if (te.getElement().isVisible()) {
                    setText(te.getText());
                    show();
                    return;
                } else {
                    te.setEnabled(false);
                }
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

        boolean mouseOver() {
            return e.mouseOver();
        }

        String getText() {
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

        Element getElement() {
            return e;
        }
    }
}
