package state.elements;

import json.JSONManager;
import processing.core.PGraphics;

import java.util.ArrayList;

import static processing.core.PApplet.min;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;
import static util.Font.getFont;
import static util.Util.brighten;
import static util.Util.papplet;

public class HorizontalOptionsButton extends DropDown {
  public HorizontalOptionsButton(int x, int y, int w, int h, int bgColour, int textSize, String[] options) {
    super(x, y, w, h, bgColour, "", "", textSize);
    setOptions(options);
    expanded = true;
  }

  public void draw(PGraphics canvas) {
    int hovering = hoveringOption();
    canvas.pushStyle();

    // draw selected option
    canvas.stroke(papplet.color(0));
    if (moveOver() && hovering == -1 && getElemOnTop()) {  // Hovering over top selected option
      canvas.fill(brighten(bgColour, -20));
    } else {
      canvas.fill(brighten(bgColour, -40));
    }
    canvas.rect(x, y, w, h);
    canvas.textAlign(LEFT, TOP);
    canvas.textFont(getFont((min(h*0.8f, textSize))* JSONManager.loadFloatSetting("text scale")));
    canvas.fill(papplet.color(0));

    // Draw expand box
    canvas.line(x+w-h, y+1, x+w-h/2, y+h-1);
    canvas.line(x+w-h/2, y+h-1, x+w, y+1);

    int boxX = x;
    for (int i=0; i < options.length; i++) {
      if (i == selected) {
        canvas.fill(brighten(bgColour, 50));
      } else {
        if (moveOver() && i == hovering && getElemOnTop()) {
          canvas.fill(brighten(bgColour, 20));
        } else {
          canvas.fill(bgColour);
        }
      }
      canvas.rect(boxX, y, w, h);
      if (i == selected) {
        canvas.fill(brighten(bgColour, 20));
      } else {
        canvas.fill(0);
      }
      canvas.text(options[i], boxX+3, y);
      boxX += w;
    }
    canvas.popStyle();
  }
  public boolean moveOver() {
    return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w*(options.length) && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset < y+h;
  }

  public ArrayList<String> mouseEvent(String eventType, int button) {
    ArrayList<String> events = new ArrayList<String>();
    if (eventType.equals("mouseClicked")) {
      int hovering = hoveringOption();
      if (moveOver()) {
        if (hovering != -1) {
          events.add("valueChanged");
          selected = hovering;
          contract();
          events.add("stop events");
        }
      }
    }
    return events;
  }

  public int hoveringOption() {
    return (papplet.mouseX-xOffset-x)/w;
  }
}
