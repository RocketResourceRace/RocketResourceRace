package ui;

import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.ArrayList;

import static util.Util.papplet;

public class Element {
    public boolean active = true;
    protected boolean visible = true;
    private boolean elemOnTop;
    protected int x;
    public int y;
    protected int w;
    public int h;
    protected int xOffset;
    protected int yOffset;
    public String id;

    void setElemOnTop(boolean value){
        elemOnTop = value;
    }
    protected boolean getElemOnTop(){
        // For checking if hover highlighting is needed
        return elemOnTop;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    public void draw(PGraphics panelCanvas) {
    }
    public ArrayList<String> mouseEvent(String eventType, int button) {
        return new ArrayList<>();
    }
    protected ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
        return new ArrayList<>();
    }
    protected ArrayList<String> keyboardEvent(String eventType, char _key) {
        return new ArrayList<>();
    }
    public void transform(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    void setOffset(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    void setID(String id) {
        this.id = id;
    }

    public ArrayList<String> _mouseEvent(String eventType, int button) {
        return mouseEvent(eventType, button);
    }
    ArrayList<String> _mouseEvent(String eventType, int button, MouseEvent event) {
        return mouseEvent(eventType, button, event);
    }
    public ArrayList<String> _keyboardEvent(String eventType, char _key) {
        return keyboardEvent(eventType, _key);
    }
    public void activate() {
        active = true;
    }
    public void deactivate() {
        active = false;
    }

    public boolean pointOver() {
        return papplet.mouseX >= x && papplet.mouseX <= x+w && papplet.mouseY >= y && papplet.mouseY <= y+h;
    }

    public boolean mouseOver() {
        return papplet.mouseX >= x+xOffset && papplet.mouseX <= x+xOffset+w && papplet.mouseY >= y+yOffset && papplet.mouseY <= y+yOffset+h;
    }

    public void setVisible(boolean a) {
        visible = a;
    }

    public boolean isVisible() {
        return visible;
    }
}