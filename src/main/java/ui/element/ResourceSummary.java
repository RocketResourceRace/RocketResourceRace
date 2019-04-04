package ui.element;

import json.JSONManager;
import processing.core.PGraphics;
import ui.Element;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;

import static json.JSONManager.gameData;
import static processing.core.PApplet.ceil;
import static processing.core.PApplet.max;
import static processing.core.PConstants.*;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.metricPrefix;
import static util.Util.papplet;

public class ResourceSummary extends Element {
    private float[] stockPile, net;
    private String[] resNames;
    private int numRes, scroll;
    private boolean expanded;
    private int[] timings;
    private byte[] warnings;

    private final int GAP = 10;
    private final int FLASHTIMES = 500;

    public ResourceSummary(int x, int y, int h, String[] resNames, float[] stockPile, float[] net) {
        this.x = x;
        this.y = y;
        this.h = h;
        this.resNames = resNames;
        this.numRes = resNames.length;
        this.stockPile = stockPile;
        this.net = net;
        this.expanded = false;
        this.timings = new int[resNames.length];
        this.warnings = new byte[resNames.length];
    }

    public void updateStockpile(float[] v) {
        try {
            stockPile = v;
            LOGGER_MAIN.finest("Stockpile update: " + Arrays.toString(v));
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Error updating stockpile", e);
            throw e;
        }
    }
    public void updateNet(float[] v) {
        try {
            LOGGER_MAIN.finest("Net update: " + Arrays.toString(v));
            net = v;
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Error updating net", e);
            throw e;
        }
    }

    public void updateWarnings(byte[] v) {
        try {
            LOGGER_MAIN.finest("Warnings update: " + Arrays.toString(v));
            warnings = v;
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Error updating warnings", e);
            throw e;
        }
    }

    public void toggleExpand() {
        expanded = !expanded;
        LOGGER_MAIN.finest("Expanded changed to: " + expanded);
    }

    private String getResString(int i) {
      return resNames[i];
    }
    private String getStockString(int i) {
        return metricPrefix(""+stockPile[i]);
    }
    private String getNetString(int i) {
        String tempString = metricPrefix(""+net[i]);
        if (net[i] >= 0) {
            return "+"+tempString;
        }
        return tempString;
    }
    private int columnWidth(int i) {
        int m=0;
        papplet.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
        m = max(m, ceil(papplet.textWidth(getResString(i))));
        papplet.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
        m = max(m, ceil(papplet.textWidth(getStockString(i))));
        papplet.textFont(getFont(8* JSONManager.loadFloatSetting("text scale")));
        m = max(m, ceil(papplet.textWidth(getNetString(i))));
        return m;
    }
    public int totalWidth() {
        int tot = 0;
        for (int i=numRes-1; i>=0; i--) {
            if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0:1)) continue;
            tot += columnWidth(i)+GAP;
        }
        return tot;
    }

    public void flash(int i) {
        timings[i] = papplet.millis()+FLASHTIMES;
    }
    private int getFill(int i) {
        if (timings[i] < papplet.millis()) {
            return papplet.color(100);
        }
        return papplet.color(155*(timings[i]-papplet.millis())/FLASHTIMES+100, 100, 100);
    }

    public String getResourceAt(int x, int y) {
      return "";
    }

    public void draw(PGraphics panelCanvas) {
        int cw = 0;
        int w, yLevel, tw = totalWidth();
        this.w = tw;
        this.x=papplet.width-tw;
        panelCanvas.pushStyle();
        panelCanvas.textAlign(LEFT, TOP);
        panelCanvas.fill(120);
        panelCanvas.rect(x-GAP/2f, y, tw, h);
        panelCanvas.rectMode(CORNERS);
        for (int i=numRes-1; i>=0; i--) {
            if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0:1)) continue;
            w = columnWidth(i);
            panelCanvas.fill(getFill(i));
            panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
            panelCanvas.rect(papplet.width-cw-GAP/2f, y, papplet.width-cw-GAP/2f-(w+GAP), y+panelCanvas.textAscent()+panelCanvas.textDescent());
            cw += w+GAP;
            panelCanvas.line(papplet.width-cw-GAP/2f, y, papplet.width-cw-GAP/2f, y+h);
            panelCanvas.fill(0);

            yLevel=0;
            panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
            panelCanvas.text(getResString(i), papplet.width-cw, y);
            yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();

            panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
            if (warnings[i] == 1) {
                panelCanvas.fill(255, 127, 0);
            } else if (warnings[i] == 2){
                panelCanvas.fill(255, 0, 0);
            }
            panelCanvas.text(getStockString(i), papplet.width-cw, y+yLevel);
            yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();

            if (net[i] < 0)
                panelCanvas.fill(255, 0, 0);
            else
                panelCanvas.fill(0, 255, 0);
            panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
            panelCanvas.text(getNetString(i), papplet.width-cw, y+yLevel);
            //yLevel += panelCanvas.textAscent()+panelCanvas.textDescent();
        }
        panelCanvas.popStyle();
    }

    public String getResourceUnderMouse() {
        int cw = 0;
        int w;
        for (int i=numRes-1; i>=0; i--) {
            if (gameData.getJSONArray("resources").getJSONObject(i).getInt("resource manager") <= ((expanded) ? 0 : 1))
                continue;
            w = columnWidth(i);
            cw += w+GAP;
            if (papplet.width-cw-GAP/2<papplet.mouseX) {
                return getResString(i);
            }
        }
        return getResString(0);
    }
}
