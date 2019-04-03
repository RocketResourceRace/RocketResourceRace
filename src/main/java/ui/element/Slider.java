package ui.element;

import json.JSONManager;
import processing.core.PGraphics;
import ui.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Slider extends Element {
    private int x;
    public int y;
    private int w;
    public int h;
    private int major;
    private int minor;
    private BigDecimal value, step, upper, lower;
    private float knobSize;
    private int KnobColour;
    private int strokeColour;
    private int scaleColour;
    private boolean pressed=false;
    private String name;

    public Slider(int x, int y, int w, int h, int KnobColour, int bgColour, int strokeColour, int scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.KnobColour = KnobColour;
        this.strokeColour = strokeColour;
        this.scaleColour = scaleColour;
        this.major = major;
        this.minor = minor;
        this.upper = new BigDecimal(""+upper);
        this.lower = new BigDecimal(""+lower);
        this.step = new BigDecimal(""+step);
        this.value = new BigDecimal(""+value);
        this.name = name;
    }

    private void scaleKnob(PGraphics panelCanvas, BigDecimal value) {
        panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
        this.knobSize = max(this.knobSize, panelCanvas.textWidth(""+getInc(value)));
    }
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.w = w;
        this.y = y;
        this.h = h;
    }
    public void setScale(float lower, float value, float upper, int major, int minor) {
        this.major = major;
        this.minor = minor;
        this.upper = new BigDecimal(""+upper);
        this.lower = new BigDecimal(""+lower);
        this.value = new BigDecimal(""+value);
    }
    public void setValue(float value) {
        LOGGER_MAIN.finer("Setting value to: " + value);
        setValue(new BigDecimal(""+value));
    }

    private void setValue(BigDecimal value) {
        LOGGER_MAIN.finer("Setting value to: " + value.toString());
        if (value.compareTo(lower) < 0) {
            this.value = lower;
        } else if (value.compareTo(upper)>0) {
            this.value = new BigDecimal(""+upper);
        } else {
            this.value = value.divideToIntegralValue(step).multiply(step);
        }
    }

    public float getValue() {
      return value.floatValue();
    }
    public BigDecimal getPreciseValue() {
      return value;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (button == LEFT) {
            if (mouseOver() && eventType.equals("mousePressed")) {
                pressed = true;
                setValue((new BigDecimal(papplet.mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
                events.add("valueChanged");
            } else if (eventType.equals("mouseReleased")) {
                pressed = false;
            }
            if (eventType.equals("mouseDragged") && pressed) {
                setValue((new BigDecimal(papplet.mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
                events.add("valueChanged");
            }
        }
        return events;
    }

    public boolean mouseOver() {
        try {
            BigDecimal range = upper.subtract(lower);
            int xKnobPos = round(x+(value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue())-knobSize/2);
            return (papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h) ||
                (papplet.mouseX-xOffset >= xKnobPos && papplet.mouseX-xOffset <= xKnobPos+knobSize && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h); // Over slider or knob box
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error finding if mouse over", e);
            throw e;
        }
    }
    public boolean pointOver() {
      return mouseOver();
    }

    private BigDecimal getInc(BigDecimal i) {
      return i.stripTrailingZeros();
    }

    public void draw(PGraphics panelCanvas) {
        if (!visible)return;
        BigDecimal range = upper.subtract(lower);
        float r = red(KnobColour), g = green(KnobColour), b = blue(KnobColour);
        panelCanvas.pushStyle();
        panelCanvas.fill(255, 100);
        panelCanvas.stroke(strokeColour, 50);
        //rect(lx, y, lw, h);
        //rect(xOffset+x, y+yOffset+padding+2, w, h-padding);
        panelCanvas.stroke(strokeColour);


        int padding = 20;
        for (int i = 0; i<=minor; i++) {
            panelCanvas.fill(scaleColour);
            panelCanvas.line(x+w*i/minor, y+ padding +(h- padding)/6f, x+w*i/minor, y+5*(h- padding)/6f+ padding);
        }
        for (int i=0; i<=major; i++) {
            panelCanvas.fill(scaleColour);
            panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
            panelCanvas.textAlign(CENTER);
            panelCanvas.text(getInc((new BigDecimal(""+i).multiply(range).divide(new BigDecimal(""+major), 15, BigDecimal.ROUND_HALF_EVEN).add(lower))).toPlainString(), x+w*i/major, y+ padding);
            panelCanvas.line(x+w*i/major, y+ padding, x+w*i/major, y+h);
        }

        if (pressed) {
            int PRESSEDOFFSET = 50;
            panelCanvas.fill(min(r- PRESSEDOFFSET, 255), min(g- PRESSEDOFFSET, 255), min(b+ PRESSEDOFFSET, 255));
        } else {
            panelCanvas.fill(KnobColour);
        }

        panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER);
        panelCanvas.rectMode(CENTER);
        scaleKnob(panelCanvas, value);
        int boxHeight = 20;
        panelCanvas.rect(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2f+ padding /2f, knobSize, boxHeight);
        panelCanvas.rectMode(CORNER);
        panelCanvas.fill(scaleColour);
        panelCanvas.text(getInc(value).toPlainString(), x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2f+ boxHeight /4f+ padding /2f);
        panelCanvas.stroke(0);
        panelCanvas.textAlign(CENTER);
        panelCanvas.stroke(255, 0, 0);
        panelCanvas.line(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2- boxHeight /2+ padding /2, x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2- boxHeight + padding /2);
        panelCanvas.stroke(0);
        panelCanvas.fill(0);
        panelCanvas.textFont(getFont(10* JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(LEFT, BOTTOM);
        panelCanvas.text(name, x, y);
        panelCanvas.popStyle();
    }
  }
