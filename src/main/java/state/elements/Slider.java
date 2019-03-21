package state.elements;

import json.JSONManager;
import processing.core.PGraphics;
import state.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static processing.core.PConstants.*;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Slider extends Element {
    private int x;
    public int y;
    private int w;
    public int h;
    private int cx;
    private int cy;
    private int major;
    private int minor;
    private int lw;
    private int lx;
    private int padding = 20;
    private BigDecimal value, step, upper, lower;
    private float knobSize;
    private int KnobColour, bgColour, strokeColour, scaleColour;
    private boolean horizontal, pressed=false;
    final int boxHeight = 20, boxWidth = 10;
    private final int PRESSEDOFFSET = 50;
    private String name;
    boolean visible = true;

    public Slider(int x, int y, int w, int h, int KnobColour, int bgColour, int strokeColour, int scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name) {
      this.lx = x;
      this.x = x;
      this.y = y;
      this.lw = w;
      this.w = w;
      this.h = h;
      this.KnobColour = KnobColour;
      this.bgColour = bgColour;
      this.strokeColour = strokeColour;
      this.scaleColour = scaleColour;
      this.major = major;
      this.minor = minor;
      this.upper = new BigDecimal(""+upper);
      this.lower = new BigDecimal(""+lower);
      this.horizontal = horizontal;
      this.step = new BigDecimal(""+step);
      this.value = new BigDecimal(""+value);
      this.name = name;
    }

    public void scaleKnob(PGraphics panelCanvas, BigDecimal value) {
      panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
      this.knobSize = max(this.knobSize, panelCanvas.textWidth(""+getInc(value)));
    }
    public void transform(int x, int y, int w, int h) {
      this.lx = x;
      this.x = x;
      this.lw = w;
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

    public void setValue(BigDecimal value) {
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
      ArrayList<String> events = new ArrayList<String>();
      if (button == LEFT) {
        if (mouseOver() && eventType == "mousePressed") {
          pressed = true;
          setValue((new BigDecimal(papplet.mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
          events.add("valueChanged");
        } else if (eventType == "mouseReleased") {
          pressed = false;
        }
        if (eventType == "mouseDragged" && pressed) {
          setValue((new BigDecimal(papplet.mouseX-xOffset-x)).divide(new BigDecimal(w), 15, BigDecimal.ROUND_HALF_EVEN).multiply(upper.subtract(lower)).add(lower));
          events.add("valueChanged");
        }
      }
      return events;
    }

    public Boolean mouseOver() {
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

    public BigDecimal getInc(BigDecimal i) {
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


      for (int i=0; i<=minor; i++) {
        panelCanvas.fill(scaleColour);
        panelCanvas.line(x+w*i/minor, y+padding+(h-padding)/6, x+w*i/minor, y+5*(h-padding)/6+padding);
      }
      for (int i=0; i<=major; i++) {
        panelCanvas.fill(scaleColour);
        panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.textAlign(CENTER);
        panelCanvas.text(getInc((new BigDecimal(""+i).multiply(range).divide(new BigDecimal(""+major), 15, BigDecimal.ROUND_HALF_EVEN).add(lower))).toPlainString(), x+w*i/major, y+padding);
        panelCanvas.line(x+w*i/major, y+padding, x+w*i/major, y+h);
      }

      if (pressed) {
        panelCanvas.fill(min(r-PRESSEDOFFSET, 255), min(g-PRESSEDOFFSET, 255), min(b+PRESSEDOFFSET, 255));
      } else {
        panelCanvas.fill(KnobColour);
      }

      panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(CENTER);
      panelCanvas.rectMode(CENTER);
      scaleKnob(panelCanvas, value);
      panelCanvas.rect(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2+padding/2, knobSize, boxHeight);
      panelCanvas.rectMode(CORNER);
      panelCanvas.fill(scaleColour);
      panelCanvas.text(getInc(value).toPlainString(), x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2+boxHeight/4+padding/2);
      panelCanvas.stroke(0);
      panelCanvas.textAlign(CENTER);
      panelCanvas.stroke(255, 0, 0);
      panelCanvas.line(x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight/2+padding/2, x+value.floatValue()/range.floatValue()*w-lower.floatValue()*w/range.floatValue(), y+h/2-boxHeight+padding/2);
      panelCanvas.stroke(0);
      panelCanvas.fill(0);
      panelCanvas.textFont(getFont(10* JSONManager.loadFloatSetting("text scale")));
      panelCanvas.textAlign(LEFT, BOTTOM);
      panelCanvas.text(name, x, y);
      panelCanvas.popStyle();
    }
  }
