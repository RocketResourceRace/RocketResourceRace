package state.elements;

import json.JSONManager;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import state.Element;

import java.util.ArrayList;

import static util.Font.getFont;
import static util.Util.between;
import static util.Util.papplet;

public class IncrementElement extends Element {
  final int TEXTSIZE = 8;
  final int SIDEBOXESWIDTH = 15;
  final int ARROWOFFSET = 4;
  final float FULLPANPROPORTION = 0.25f;  // Adjusts how much mouse dragging movement is needed to change value as propotion of screen width
  private int upper, lower, value, step, bigStep;
  int startingX, startingValue, pressing;
  boolean grabbed;

  public IncrementElement(int x, int y, int w, int h, int upper, int lower, int startingValue, int step, int bigStep){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.upper = upper;
    this.lower = lower;
    this.value = startingValue;
    this.step = step;
    this.bigStep = bigStep;
    grabbed = false;
    startingX = 0;
    startingValue = value;
    pressing = -1;
  }

  public void setUpper(int upper){
    this.upper = upper;
  }

  public int getUpper(){
    return this.upper;
  }

  public void setLower(int lower){
    this.lower = lower;
  }

  public int getLower(){
    return this.lower;
  }

  public void setValue(int value){
    this.value = value;
  }

  public int getValue(){
    return this.value;
  }

  public void setValueWithinBounds(){
    value = PApplet.parseInt(between(lower, value, upper));
  }

  public ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")){
      int change = 0;
      if (button == PConstants.LEFT){
        change = step;
      } else if (button == PConstants.RIGHT){  // Right clicking increments by more
        change = bigStep;
      }

      if (mouseOverLeftBox()){
        setValue(getValue()-change);
        setValueWithinBounds();
        events.add("valueChanged");
      } else if (mouseOverRightBox()){
        setValue(getValue()+change);
        setValueWithinBounds();
        events.add("valueChanged");
      }
    }
    if (eventType.equals("mousePressed")){
      if (mouseOverRightBox()){
        pressing = 0;
      } else if (mouseOverLeftBox()){
        pressing = 1;
      } else if (mouseOverMiddleBox()){
        grabbed = true;
        startingX = papplet.mouseX;
        startingValue = getValue();
        pressing = 2;
      } else{
        pressing = -1;
      }
    }
    if (eventType.equals("mouseReleased")){
      if (grabbed){
        events.add("valueChanged");
      }
      grabbed = false;
      pressing = -1;
    }
    if (eventType.equals("mouseDragged")){
      if (grabbed){
        int change = PApplet.floor((papplet.mouseX-startingX)*(upper-lower)/(papplet.width*FULLPANPROPORTION));
        if (change != 0){
          setValue(startingValue+change);
          setValueWithinBounds();
        }
      }
    }
    return events;
  }

  public void draw(PGraphics panelCanvas){
    panelCanvas.pushStyle();

    //Draw middle box
    panelCanvas.strokeWeight(2);
    if (grabbed){
      panelCanvas.fill(150);
    } else if (getElemOnTop() && mouseOverMiddleBox()){
      panelCanvas.fill(200);
    } else{
      panelCanvas.fill(170);
    }
    panelCanvas.rect(x, y, w, h);

    //draw left side box
    panelCanvas.strokeWeight(1);
    if (getElemOnTop() && mouseOverLeftBox()){
      if (pressing == 1){
        panelCanvas.fill(100);
      } else{
        panelCanvas.fill(130);
      }
    } else{
      panelCanvas.fill(120);
    }
    panelCanvas.rect(x, y, SIDEBOXESWIDTH, h-1);
    panelCanvas.fill(0);
    panelCanvas.strokeWeight(2);
    panelCanvas.line(x-ARROWOFFSET+SIDEBOXESWIDTH, y+ARROWOFFSET, x+ARROWOFFSET, y+h/2);
    panelCanvas.line(x+ARROWOFFSET, y+h/2, x-ARROWOFFSET+SIDEBOXESWIDTH, y+h-ARROWOFFSET);

    //draw right side box
    if (getElemOnTop() && mouseOverRightBox()){
      if (pressing == 0){
        panelCanvas.fill(100);
      } else{
        panelCanvas.fill(130);
      }
    } else{
      panelCanvas.fill(120);
    }
    panelCanvas.rect(x+w-SIDEBOXESWIDTH, y, SIDEBOXESWIDTH, h-1);
    panelCanvas.fill(0);
    panelCanvas.strokeWeight(2);
    panelCanvas.line(x+w+ARROWOFFSET-SIDEBOXESWIDTH, y+ARROWOFFSET, x+w-ARROWOFFSET, y+h/2);
    panelCanvas.line(x+w-ARROWOFFSET, y+h/2, x+w+ARROWOFFSET-SIDEBOXESWIDTH, y-ARROWOFFSET+h);

    // Draw value
    panelCanvas.fill(0);
    panelCanvas.textFont(getFont(TEXTSIZE* JSONManager.loadFloatSetting("text scale")));
    panelCanvas.textAlign(PConstants.CENTER, PConstants.CENTER);
    panelCanvas.text(value, x+w/2, y+h/2);

    panelCanvas.popStyle();
  }

  public boolean mouseOverMiddleBox(){
    return mouseOver() && !mouseOverRightBox() && !mouseOverLeftBox();
  }

  public boolean mouseOverRightBox() {
    return papplet.mouseX-xOffset >= x+w-SIDEBOXESWIDTH && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
  }

  public boolean mouseOverLeftBox() {
    return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+SIDEBOXESWIDTH && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
  }

  public boolean mouseOver() {
    return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
  }

  public boolean pointOver(){
    return mouseOver();
  }
}
