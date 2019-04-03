package ui.element;

import json.JSONManager;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import ui.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

import static processing.core.PApplet.ceil;
import static processing.core.PConstants.BOTTOM;
import static processing.core.PConstants.LEFT;
import static util.Image.equipmentImages;
import static util.Logging.LOGGER_MAIN;

public class ResourceManagementTable extends Element {
    private int page;
    private String[][] headings;
    private ArrayList<ArrayList<String>> names;
    private ArrayList<ArrayList<Float>> production, consumption, net, storage;
    private int rows;
    private int TEXTSIZE = 13;
    private HashMap<String, PImage> tempEquipmentImages;
    private int rowThickness;
    private int rowGap;
    private int columnGap;
    private int headerSize;
    private int imgHeight;

    public ResourceManagementTable(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        int pages = 2;
        names = new ArrayList<>();
        headings = new String[pages][];
        tempEquipmentImages = new HashMap<>();
        rowThickness = ceil(TEXTSIZE*1.6f*JSONManager.loadFloatSetting("text scale"));
        imgHeight = rowThickness;
        rowGap = ceil(TEXTSIZE/4f*JSONManager.loadFloatSetting("text scale"));
        columnGap = ceil(TEXTSIZE* JSONManager.loadFloatSetting("text scale"));
        headerSize = ceil(1.3f*TEXTSIZE*JSONManager.loadFloatSetting("text scale"));
        resizeImages();
    }

    public void update(String[][] headings,
                       ArrayList<ArrayList<String>> names,
                       ArrayList<ArrayList<Float>> production,
                       ArrayList<ArrayList<Float>> consumption,
                       ArrayList<ArrayList<Float>> net,
                       ArrayList<ArrayList<Float>> storage) {
        this.names = names;
        this.production = production;
        this.consumption = consumption;
        this.net = net;
        this.storage = storage;
        this.headings = headings;
        this.rows = names.get(page).size();
        rowThickness = ceil(TEXTSIZE*1.6f*JSONManager.loadFloatSetting("text scale"));
        rowGap = ceil(TEXTSIZE/4f*JSONManager.loadFloatSetting("text scale"));
        columnGap = ceil(TEXTSIZE*JSONManager.loadFloatSetting("text scale"));
        headerSize = ceil(1.3f*TEXTSIZE*JSONManager.loadFloatSetting("text scale"));
        resizeImages();
    }

    public void setPage(int p) {
        page = p;
        this.rows = names.get(page).size();
    }

    public void draw(PGraphics canvas) {
        canvas.fill(0);
        canvas.textAlign(LEFT, BOTTOM);
        canvas.textSize(headerSize);

        float[] cumulativeWidth = new float[headings[page].length+1];
        for (int i = 0; i < headings[page].length; i++) {
            cumulativeWidth[i+1] = canvas.textWidth(headings[page][i]) + columnGap + cumulativeWidth[i];
        }

        float[] headingXs = new float[headings[page].length];
        for (int i = 0; i < headings[page].length; i++) {
            float headingX;
            if (i == 0) {
                headingX = x+cumulativeWidth[i];
            } else {
                headingX = w-x-cumulativeWidth[headings[page].length]+cumulativeWidth[i];
            }
            headingXs[i] = headingX;
            canvas.text(headings[page][i], headingX, y+headerSize);
            canvas.line(headingX, y+headerSize, headingX+canvas.textWidth(headings[page][i]), y+headerSize);
        }


        int yPos = y+headerSize+2*rowGap;
        for (int i = 0; i < rows; i++) {
            canvas.fill(150);
            canvas.rect(x, yPos+i*(rowThickness+rowGap), w, rowThickness);
            canvas.fill(0);
            canvas.textSize(TEXTSIZE*JSONManager.loadFloatSetting("text scale"));
            int offset = 0;
            int startColumn = 0;
            if (page == 1) {
                canvas.image(tempEquipmentImages.get(names.get(page).get(i)), x+2, yPos+i*(rowThickness+rowGap));
                offset = PApplet.parseInt(imgHeight/0.75f);
                canvas.text(
                        JSONManager.getEquipmentClass(Objects.requireNonNull(JSONManager.getEquipmentTypeClassFromID(names.get(page).get(i)))[0]),
                        headingXs[1], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
                startColumn = 1;
            }
            canvas.text(names.get(page).get(i), x+offset+columnGap, yPos+(i+1)*(rowThickness+rowGap) - rowGap);
            canvas.fill(0, 255, 0);
            canvas.text(production.get(page).get(i), headingXs[startColumn+1], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
            canvas.fill(255, 0, 0);
            canvas.text(consumption.get(page).get(i), headingXs[startColumn+2], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
            canvas.fill(0);
            canvas.text(net.get(page).get(i), headingXs[startColumn+3], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
            canvas.text(storage.get(page).get(i), headingXs[startColumn+4], yPos+(i+1)*(rowThickness+rowGap) - rowGap);
        }
    }

    private void resizeImages(){
        // Resize equipment icons
        for (int c=0; c < JSONManager.getNumEquipmentClasses(); c++){
            for (int t=0; t < JSONManager.getNumEquipmentTypesFromClass(c); t++){
                try{
                    String id = JSONManager.getEquipmentTypeID(c, t);
                    tempEquipmentImages.put(id, equipmentImages.get(id).copy());
                    tempEquipmentImages.get(id).resize(ceil(PApplet.parseFloat(imgHeight)/0.75f), imgHeight);
                }
                catch (NullPointerException e){
                    LOGGER_MAIN.log(Level.SEVERE, String.format("Error resizing image for equipment icon class:%d, type:%d, id:%s", c, t, JSONManager.getEquipmentTypeID(c, t)), e);
                    throw e;
                }
            }
        }
    }
}
