package ui.element;

import event.Notification;
import json.JSONManager;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import ui.Element;

import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.*;
import static util.Font.getFont;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class NotificationManager extends Element {
    private ArrayList<ArrayList<Notification>> notifications;
    private int bgColour, textColour, displayNots, notHeight, topOffset, scroll, turn, numPlayers;
    public Notification lastSelected;
    private boolean scrolling;

    public NotificationManager(int x, int y, int w, int h, int bgColour, int textColour, int displayNots, int turn, int numPlayers) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.turn = turn;
        this.bgColour = bgColour;
        this.textColour = textColour;
        this.displayNots = displayNots;
        this.notHeight = h/displayNots;
        this.notifications = new ArrayList<>();
        this.numPlayers = numPlayers;
        for (int i = 0; i < numPlayers; i ++){
            notifications.add(new ArrayList<>());
        }
        this.scroll = 0;
        lastSelected = null;
        scrolling = false;
    }

    public boolean moveOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+notHeight*(notifications.get(turn).size()+1);
    }
    public boolean pointOver() {
        return moveOver();
    }

    private boolean mouseOver(int i) {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y+notHeight*i+topOffset && papplet.mouseY-yOffset <= y+notHeight*(i+1)+topOffset;
    }

    private int findMouseOver() {
        if (!moveOver()) {
            return -1;
        }
        for (int i=0; i<notifications.get(turn).size(); i++) {
            if (mouseOver(i)) {
                return i;
            }
        }
        return -1;
    }
    private boolean hoveringDismissAll() {
        return x<papplet.mouseX-xOffset&&papplet.mouseX-xOffset<x+notHeight&&y<papplet.mouseY-yOffset&&papplet.mouseY-yOffset<y+topOffset;
    }

    public void turnChange(int turn) {
        this.turn = turn;
        this.scroll = 0;
    }

    private void dismiss(int i) {
        LOGGER_MAIN.fine(String.format("Dismissing notification at index: %d which equates to:%s", i, notifications.get(turn).get(i)));
        try {
            notifications.get(turn).remove(i);
            scroll = round(between(0, scroll, notifications.get(turn).size()-displayNots));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error dismissing notification", e);
            throw e;
        }
    }

    public void dismissAll() {
        // Dismisses all notification for the current player
        LOGGER_MAIN.fine("Dismissing all notifications");
        notifications.get(turn).clear();
    }

    public void reset() {
        // Clears all notificaitions for all players
        LOGGER_MAIN.fine("Dismissing notifications for all players");
        notifications.clear();
        for (int i = 0; i < numPlayers; i ++){
            notifications.add(new ArrayList<>());
        }
    }

    public void post(Notification n, int turn) {
        try {
            LOGGER_MAIN.fine("Posting notification: "+n.name);
            notifications.get(turn).add(0, n);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Failed to post notification", e);
            throw e;
        }
    }

    public void post(String name, int x, int y, int turnNum, int turn) {
        try {
            LOGGER_MAIN.fine(String.format("Posting notification: %s at cell:%s, %s turn:%d player:%d", name, x, y, turnNum, turn));
            notifications.get(turn).add(0, new Notification(name, x, y, turnNum));
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.WARNING, "Failed to post notification", e);
            throw e;
        }
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mouseWheel")) {
            float count = event.getCount();
            if (moveOver()) {
                scroll = round(between(0, scroll+count, notifications.get(turn).size()-displayNots));
            }
        }
        // Lazy fix for bug
        if (moveOver() && visible && active && !empty()) {
            events.add("stop events");
        }
        return events;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mousePressed")) {
            if (moveOver() && papplet.mouseX-xOffset>x+w-20* JSONManager.loadFloatSetting("gui scale") && papplet.mouseY-yOffset > topOffset && notifications.get(turn).size() > displayNots) {
                scrolling = true;
                scroll = round(between(0, (papplet.mouseY-yOffset-y-topOffset)*(notifications.get(turn).size()-displayNots+1)/(h-topOffset), notifications.get(turn).size()-displayNots));
            } else {
                scrolling = false;
            }
        }
        if (eventType.equals("mouseDragged")) {
            if (scrolling && notifications.get(turn).size() > displayNots) {
                scroll = round(between(0, (papplet.mouseY-yOffset-y-topOffset)*(notifications.get(turn).size()-displayNots+1)/(h-topOffset), notifications.get(turn).size()-displayNots));
            }
        }
        if (eventType.equals("mouseClicked")) {
            int hovering = findMouseOver();
            if (hovering >=0) {
                if (papplet.mouseX-xOffset<x+notHeight) {
                    dismiss(hovering+scroll);
                    events.add("notification dismissed");
                } else if (!(notifications.get(turn).size() > displayNots) || !(papplet.mouseX-xOffset>x+w-20*JSONManager.loadFloatSetting("gui scale"))) {
                    lastSelected = notifications.get(turn).get(hovering+scroll);
                    events.add("notification selected");
                }
            } else if (papplet.mouseX-xOffset<x+notHeight && hoveringDismissAll()) {
                dismissAll();
            }
        }
        return events;
    }

    public boolean empty() {
        return notifications.get(turn).size() == 0;
    }

    public void draw(PGraphics panelCanvas) {
        if (empty())return;
        panelCanvas.pushStyle();
        panelCanvas.fill(bgColour);
        this.notHeight = (h-topOffset)/displayNots;
        panelCanvas.rect(x, y, w, notHeight);
        panelCanvas.textFont(getFont(10*JSONManager.loadFloatSetting("text scale")));
        panelCanvas.fill(brighten(bgColour, -50));
        topOffset = ceil(panelCanvas.textAscent()+panelCanvas.textDescent());
        panelCanvas.rect(x, y, w, topOffset);
        panelCanvas.fill(textColour);
        panelCanvas.textAlign(CENTER, TOP);
        panelCanvas.text("Notification Manager", x+w/2, y);

        if (hoveringDismissAll() && getElemOnTop()) {
            panelCanvas.fill(brighten(bgColour, 80));
        } else {
            panelCanvas.fill(brighten(bgColour, -20));
        }
        panelCanvas.rect(x, y, notHeight, topOffset);
        panelCanvas.strokeWeight(3);
        panelCanvas.line(x+5, y+5, x+notHeight-5, y+topOffset-5);
        panelCanvas.line(x+notHeight-5, y+5, x+5, y+topOffset-5);
        panelCanvas.strokeWeight(1);

        int hovering = findMouseOver();
        for (int i=0; i<min(notifications.get(turn).size(), displayNots); i++) {

            if (hovering == i && getElemOnTop()) {
                panelCanvas.fill(brighten(bgColour, 20));
            } else {
                panelCanvas.fill(brighten(bgColour, -10));
            }
            panelCanvas.rect(x, y+i*notHeight+topOffset, w, notHeight);

            panelCanvas.fill(brighten(bgColour, -20));
            if (papplet.mouseX-xOffset<x+notHeight) {
                if (hovering == i) {
                    panelCanvas.fill(brighten(bgColour, 80));
                } else {
                    panelCanvas.fill(brighten(bgColour, -20));
                }
            }
            panelCanvas.rect(x, y+i*notHeight+topOffset, notHeight, notHeight);
            panelCanvas.strokeWeight(3);
            panelCanvas.line(x+5, y+i*notHeight+topOffset+5, x+notHeight-5, y+(i+1)*notHeight+topOffset-5);
            panelCanvas.line(x+notHeight-5, y+i*notHeight+topOffset+5, x+5, y+(i+1)*notHeight+topOffset-5);
            panelCanvas.strokeWeight(1);

            panelCanvas.fill(textColour);
            panelCanvas.textFont(getFont(8*JSONManager.loadFloatSetting("text scale")));
            panelCanvas.textAlign(LEFT, CENTER);
            panelCanvas.text(notifications.get(turn).get(i+scroll).name, x+notHeight+5, y+topOffset+i*notHeight+notHeight/2f);
            panelCanvas.textAlign(RIGHT, CENTER);
            panelCanvas.text("Turn "+notifications.get(turn).get(i+scroll).turn, x-notHeight+w, y+topOffset+i*notHeight+notHeight/2f);
        }

        //draw scroll
        int d = notifications.get(turn).size() - displayNots;
        if (d > 0) {
            panelCanvas.fill(brighten(bgColour, 100));
            panelCanvas.rect(x-20*JSONManager.loadFloatSetting("gui scale")+w, y+topOffset, 20*JSONManager.loadFloatSetting("gui scale"), h-topOffset);
            panelCanvas.fill(brighten(bgColour, -20));
            panelCanvas.rect(x-20*JSONManager.loadFloatSetting("gui scale")+w, y+(h-topOffset-(h-topOffset)/(float)(d+1))*scroll/(float)d+topOffset, 20*JSONManager.loadFloatSetting("gui scale"), (h-topOffset)/(d+1));
        }
        panelCanvas.popStyle();
    }
}
