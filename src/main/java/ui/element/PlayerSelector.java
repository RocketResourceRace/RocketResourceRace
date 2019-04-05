package ui.element;
import processing.core.PGraphics;
import ui.Element;

public class PlayerSelector extends Element {
    private int bgColour;

    public PlayerSelector(int x, int y, int w, int h, int bgColour) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bgColour = bgColour;
    }

    public void draw(PGraphics panelCanvas) {
        panelCanvas.fill(bgColour);
        panelCanvas.rect(x, y, w, h);
    }
}
