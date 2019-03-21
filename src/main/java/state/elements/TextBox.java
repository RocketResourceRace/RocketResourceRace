package state.elements;

import json.JSONManager;
import processing.core.PGraphics;
import state.Element;

import static processing.core.PApplet.ceil;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.CORNER;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class TextBox extends Element {
    int textSize, bgColour, textColour;
    String text;
    boolean autoSizing;

    public TextBox(int x, int y, int w, int h, int textSize, String text, int bgColour, int textColour) {
        //w=-1 means get width from text
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        autoSizing = this.w == -1;
        this.textSize = textSize;
        this.bgColour = bgColour;
        this.textColour = textColour;
        setText(text);
    }

    public void setText(String text) {
        this.text = text;
        LOGGER_MAIN.finer("Text set to: " + text);
    }

    public void updateWidth(PGraphics panelCanvas) {
        if (autoSizing) {
            this.w = ceil(panelCanvas.textWidth(text))+10;
        }
    }

    public String getText() {
      return text;
    }

    public void setColour(int c) {
      bgColour = c;
    }

    public void draw(PGraphics panelCanvas) {
        panelCanvas.pushStyle();
        panelCanvas.textFont(getFont(textSize* JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER, CENTER);
        panelCanvas.rectMode(CORNER);
        updateWidth(panelCanvas);
        if (bgColour != papplet.color(255, 255)) {
            panelCanvas.fill(bgColour);
            panelCanvas.rect(x, y, w, h);
        }
        panelCanvas.fill(textColour);
        panelCanvas.text(text, x+w/2, y+h/2);
        panelCanvas.popStyle();
    }
}
