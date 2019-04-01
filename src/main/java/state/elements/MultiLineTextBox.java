package state.elements;

import json.JSONManager;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static processing.core.PConstants.CENTER;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class MultiLineTextBox extends Element {
    protected int x;
    public int y;
    protected int w;
    public int h;
    private int textSize;
    private int textAlignx;
    private int textAligny;
    public int bgColour;
    private int strokeColour;
    public int textColour;
    private String text;
    ArrayList<String> lines;
    private int strokeWeight;

    MultiLineTextBox(int x, int y, int w, int h, int bgColour, int strokeColour, int textColour, int textSize, int textAlignx, int textAligny, String text) {
        //w=-1 means get width from text
        //h=-1 means get height from text
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bgColour = bgColour;
        this.strokeColour = strokeColour;
        this.textColour = textColour;
        this.textSize = textSize;
        this.textAlignx = textAlignx;
        this.textAligny = textAligny;
        this.text = text;

        this.strokeWeight = 3;

        setLines(text);
    }

    MultiLineTextBox(int x, int y, int w, int h, int bgColour, int strokeColour, int textColour, int textSize, int textAlignx, int textAligny, String text, int strokeWeight) {
        //w=-1 means get width from text
        //h=-1 means get height from text
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bgColour = bgColour;
        this.strokeColour = strokeColour;
        this.textColour = textColour;
        this.textSize = textSize;
        this.textAlignx = textAlignx;
        this.textAligny = textAligny;
        this.text = text;
        this.strokeWeight = strokeWeight;

        setLines(text);
    }
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    public void setText(String text) {
        LOGGER_MAIN.finer("Setting text to: " + text);
        this.text = text;
        setLines(text);
    }
    public void setColour(int colour) {
        LOGGER_MAIN.finest("Setting colour to: " + colour);
        this.bgColour = colour;
    }

    public String getText() {
        return this.text;
    }

    float maxWidthLine(PGraphics panelCanvas, ArrayList<String> lines) {
        float ml = 0;
        for (String line : lines) {
            float length = panelCanvas.textWidth(line);
            if (length > ml) {
                ml = length;
            }
        }
        return ml;
    }

    public void draw(PGraphics panelCanvas) {
        if (isVisible()) {
            panelCanvas.pushStyle();
            ArrayList<String> lines = getLines(text);
            panelCanvas.textFont(getFont(textSize* JSONManager.loadFloatSetting("text scale")));
            int tw = w;
            if (w == -1) {
                tw = ceil(maxWidthLine(panelCanvas, lines)) + 4;
            }
            int th = h;
            if (h == -1) {
                th = ((int) (textSize*JSONManager.loadFloatSetting("text scale") + 4))*lines.size();
            }
            int gap = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());

            panelCanvas.fill(bgColour);

            panelCanvas.stroke(strokeColour);
            panelCanvas.strokeWeight(strokeWeight);
            int tx = x;
            int ty = y;
            if (textAlignx==CENTER) {
                tx = x + tw / 2;
            } else if (textAlignx==RIGHT) {
                tx = x + tw;
            }
            if (textAligny==CENTER) {
                ty = y + th / 2;
            } else if (textAligny==BOTTOM) {
                ty = y + th;
            }
            panelCanvas.rect(x, y, tw, th);
            panelCanvas.fill(textColour);
            panelCanvas.textAlign(textAlignx, textAligny);
            for (int i=0; i<lines.size(); i++) {
                if (lines.get(i).contains("<r>")) {
                    drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap, papplet.color(255,0,0), 'r');
                } else if (lines.get(i).contains("<g>")) {
                    drawColouredLine(panelCanvas, lines.get(i), tx+2, ty+i*gap, papplet.color(50,255,50), 'g');
                } else {
                    panelCanvas.text(lines.get(i), tx+2, ty+i*gap);
                }
            }
            panelCanvas.popStyle();
        }
    }

    private void drawColouredLine(PGraphics canvas, String line, float startX, float startY, int colour, char indicatingChar) {
        int start, end=0;
        float tw=0;
        boolean coloured = false;
        try {
            while (end != line.length()) {
                start = end;
                if (coloured) {
                    canvas.fill(colour);
                    end = line.indexOf("</"+indicatingChar+">", end);
                } else {
                    canvas.fill(0);
                    end = line.indexOf("<"+indicatingChar+">", end);
                }
                if (end == -1) { // indexOf returns -1 when not found
                    end = line.length();
                }
                String text = line.substring(start, end).replace("<" + indicatingChar + ">", "").replace("</" + indicatingChar + ">", "");
                canvas.text(text, startX+tw, startY);
                tw += canvas.textWidth(text);
                coloured = !coloured;
            }
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Invalid index used drawing line", e);
        }
    }

    private ArrayList<String> getLines(String s) {
        try {
            int j = 0;
            ArrayList<String> lines = new ArrayList<>();
            for (int i=0; i<s.length(); i++) {
                if (s.charAt(i) == '\n') {
                    lines.add(s.substring(j, i));
                    j=i+1;
                }
            }
            lines.add(s.substring(j));
            return lines;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error occured getting lines of tooltip in: "+s, e);
            throw e;
        }
    }

    private void setLines(String s) {
        LOGGER_MAIN.finer("Setting lines to: " + s);
        lines = getLines(s);
    }
}
