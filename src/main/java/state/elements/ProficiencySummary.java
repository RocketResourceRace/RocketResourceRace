package state.elements;

import json.JSONManager;
import processing.core.PConstants;
import processing.core.PGraphics;
import state.Element;

import java.util.Arrays;

import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;
import static util.Util.roundDpTrailing;

public class ProficiencySummary extends Element {
    final int TEXTSIZE = 8;
    final int DECIMALPLACES = 2;
    String[] proficiencyDisplayNames;
    float[] proficiencies, bonuses;
    int rowHeight;

    public ProficiencySummary(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        updateProficiencyDisplayNames();
        proficiencies = new float[JSONManager.getNumProficiencies()];
        bonuses = new float[JSONManager.getNumProficiencies()];
    }

    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        updateProficiencyDisplayNames();
        updateRowHeight();
    }

    public void setProficiencies(float[] proficiencies) {
        this.proficiencies = proficiencies;
    }

    public void setProficiencyBonuses(float[] bonuses){
        this.bonuses = bonuses;
    }

    public void updateProficiencyDisplayNames() {
        proficiencyDisplayNames = new String[JSONManager.getNumProficiencies()];
        for (int i = 0; i < JSONManager.getNumProficiencies(); i ++) {
            proficiencyDisplayNames[i] = JSONManager.indexToProficiencyDisplayName(i);
        }
        LOGGER_MAIN.finer("Updated proficiency display names to: "+ Arrays.toString(proficiencyDisplayNames));
    }

    public void updateRowHeight() {
        rowHeight = h/proficiencyDisplayNames.length;
    }

    public void draw(PGraphics panelCanvas) {
        panelCanvas.pushStyle();

        //Draw background
        panelCanvas.strokeWeight(2);
        panelCanvas.fill(150);
        panelCanvas.rect(x, y, w, h); // Added and subtracted values are for stroke to line up well with other boxes

        //Draw each proficiency box
        panelCanvas.strokeWeight(1);
        for (int i = 0; i < proficiencyDisplayNames.length; i ++) {
            panelCanvas.noFill();
            panelCanvas.line(x, y+rowHeight*i, x+w, y+rowHeight*i);
            panelCanvas.fill(0);
            panelCanvas.textFont(getFont(TEXTSIZE* JSONManager.loadFloatSetting("text scale")));
            panelCanvas.textAlign(PConstants.LEFT, PConstants.CENTER);
            panelCanvas.text(proficiencyDisplayNames[i], x+5, y+rowHeight*(i+0.5f)); // Display name aligned left, middle height within row
            panelCanvas.textAlign(PConstants.RIGHT, PConstants.CENTER);
            panelCanvas.text(roundDpTrailing(""+proficiencies[i], DECIMALPLACES), x+w-10-panelCanvas.textWidth("0")*(DECIMALPLACES+4), y+rowHeight*(i+0.5f));
            if (bonuses[i] > 0){
                panelCanvas.fill(0, 255, 0);
                panelCanvas.text("+"+ roundDpTrailing(""+bonuses[i], DECIMALPLACES), x+w-5, y+rowHeight*(i+0.5f)); // Display bonus aligned right, middle height within row
            }
            else if (bonuses[i] < 0){
                panelCanvas.fill(255, 0, 0);
                panelCanvas.text(roundDpTrailing(""+bonuses[i], DECIMALPLACES), x+w-5, y+rowHeight*(i+0.5f)); // Display bonus aligned right, middle height within row
            }
        }

        panelCanvas.popStyle();
    }

    public int hoveringOption(){
        for (int i = 0; i < proficiencyDisplayNames.length; i++){
            if (papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y+rowHeight*i && papplet.mouseY-yOffset <= y+rowHeight*(i+1)) {
                return i;
            }
        }
        return -1;
    }
    public boolean mouseOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+h;
    }
}
