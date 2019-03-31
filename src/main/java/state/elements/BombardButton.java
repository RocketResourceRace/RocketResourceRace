package state.elements;

import processing.core.PGraphics;
import processing.core.PImage;

import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.TOP;
import static util.Image.bombardImage;
import static util.Util.papplet;

public class BombardButton extends Button {
    private PImage img;
    public BombardButton(int x, int y, int w, int bgColour) {
        super(x, y, w, w, bgColour, papplet.color(0), papplet.color(0), 1, CENTER, TOP, "");
        img = bombardImage;
    }

    public void draw(PGraphics panelCanvas) {
        super.draw(panelCanvas);
        panelCanvas.image(img, super.x+2, super.y+2);
    }
}
