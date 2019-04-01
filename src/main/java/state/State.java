package state;


import event.Event;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.logging.Level;

import static processing.core.PApplet.print;
import static processing.core.PApplet.println;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class State {
    public ArrayList<Panel> panels;
    protected String newState;
    protected String activePanel;

    protected State() {
        panels = new ArrayList<>();
        addPanel("default", 0, 0, papplet.width, papplet.height, true, true, papplet.color(255, 255), papplet.color(0));
        newState = "";
        activePanel = "default";
    }

    protected String getNewState() {
        // Once called, newState is cleared, so only for use states management code
        String t = newState;
        newState = "";
        return t;
    }

    public String update() {
        drawPanels();
        return getNewState();
    }
    public void enterState() {
    }
    public void leaveState() {
    }
    protected void hidePanels() {
        for (Panel panel : panels) {
            panel.setVisible(false);
        }
        LOGGER_MAIN.finer("Panels hidden");
    }

    protected void resetPanels() {
        panels.clear();
        LOGGER_MAIN.finer("Panels cleared");
    }

    public void addPanel(String id, int x, int y, int w, int h, Boolean visible, Boolean blockEvent, int bgColour, int strokeColour) {
        // Adds new panel to front
        panels.add(new Panel(id, x, y, w, h, visible, blockEvent, bgColour, strokeColour));
        LOGGER_MAIN.finer("Panel added " + id);
        panelToTop(id);
    }
    public void addPanel(String id, int x, int y, int w, int h, Boolean visible, String fileName, int strokeColour) {
        // Adds new panel to front
        panels.add(new Panel(id, x, y, w, h, visible, fileName, strokeColour));
        LOGGER_MAIN.finer("Panel added " + id);
        panelToTop(id);
    }
    protected void addElement(String id, Element elem) {
        elem.setID(id);
        getPanel("default").elements.add(elem);
        elem.setOffset(getPanel("default").x, getPanel("default").y);
        LOGGER_MAIN.finer("Element added " + id);
    }
    public void addElement(String id, Element elem, String panel) {
        elem.setID(id);
        getPanel(panel).elements.add(elem);
        elem.setOffset(getPanel(panel).x, getPanel(panel).y);
        LOGGER_MAIN.finer("Elements added " + id);
    }

    protected Element getElement(String id, String panel) {
        for (Element elem : getPanel(panel).elements) {
            if (elem.id.equals(id)) {
                return  elem;
            }
        }
        LOGGER_MAIN.warning(String.format("Element not found %s panel:%s", id, panel));
        return null;
    }

    public void panelToTop(String id) {
        Panel tempPanel = getPanel(id);
        for (int i=findPanel(id); i>0; i--) {
            panels.set(i, panels.get(i-1));
        }
        panels.set(0, tempPanel);
        LOGGER_MAIN.finest("Panel sent to top " + id);
    }

    protected void elementToTop(String id, String panelID) {
        Element tempElem = getElement(id, panelID);
        boolean found = false;
        for (int i=0; i<getPanel(panelID).elements.size()-1; i++) {
            if (getPanel(panelID).elements.get(i).id.equals(id)) {
                found = true;
            }
            if (found) {
                getPanel(panelID).elements.set(i, getPanel(panelID).elements.get(i+1));
            }
        }
        getPanel(panelID).elements.set(getPanel(panelID).elements.size()-1, tempElem);
        LOGGER_MAIN.finest("Element sent to top " + id);
    }

    public void printPanels() {
        for (Panel panel : panels) {
            print(panel.id);
        }
        println();
    }

    private int findPanel(String id) {
        for (int i=0; i<panels.size(); i++) {
            if (panels.get(i).id.equals(id)) {
                return i;
            }
        }
        LOGGER_MAIN.warning("Invalid panel " + id);
        return -1;
    }
    protected Panel getPanel(String id) {
        Panel p = panels.get(findPanel(id));
        if (p == null) {
            LOGGER_MAIN.warning("Invalid panel " + id);
        }
        return p;
    }

    public void drawPanels() {
        checkElementOnTop();
        // Draw the panels in reverse order (highest in the list are drawn last so appear on top)
        for (int i=panels.size()-1; i>=0; i--) {
            if (panels.get(i).isVisible()) {
                panels.get(i).draw();
            }
        }
    }
    // Empty method for use by children
    public ArrayList<String> mouseEvent(String eventType, int button) {
        return new ArrayList<>();
    }
    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
        return new ArrayList<>();
    }
    public ArrayList<String> keyboardEvent(String eventType, char _key) {
        return new ArrayList<String>();
    }

    public void elementEvent(ArrayList<Event> events) {
        //for (Event event : events){
        //  println(event.info(), 1);
        //}
    }

    private void _elementEvent(ArrayList<Event> events) {
        for (Event event : events) {
            if (LOGGER_MAIN.isLoggable(Level.FINEST)) {
                LOGGER_MAIN.finest(String.format("Element event id: '%s', Panel:'%s', Type:'%s'", event.id, event.panel, event.type));
            }
            if (event.type.equals("element to top")) {
                elementToTop(event.id, event.panel);
            }
        }
    }

    public void _mouseEvent(String eventType, int button) {
        try {
            ArrayList<Event> events = new ArrayList<>();
            mouseEvent(eventType, button);
            if (eventType.equals("mousePressed")) {
                for (Panel panel : panels) {
                    if (panel.mouseOver() && panel.isVisible() && panel.blockEvent) {
                        activePanel = panel.id;
                        break;
                    }
                }
            }
            for (Panel panel : panels) {
                if (activePanel.equals(panel.id) || eventType.equals("mouseMoved") || panel.overrideBlocking) {
                    // Iterate in reverse order
                    for (int i=panel.elements.size()-1; i>=0; i--) {
                        if (panel.elements.get(i).active && panel.isVisible()) {
                            try {
                                for (String eventName : panel.elements.get(i)._mouseEvent(eventType, button)) {
                                    events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
                                    if (eventName.equals("stop events")) {
                                        elementEvent(events);
                                        _elementEvent(events);
                                        return;
                                    }
                                }
                            }
                            catch(Exception e) {
                                LOGGER_MAIN.log(Level.SEVERE, String.format("Error during mouse event elem id:%s, panel id:%s", panel.elements.get(i).id, panel.id), e);
                                throw e;
                            }
                        }
                    }
                    if (!eventType.equals("mouseMoved") && !panel.overrideBlocking)
                        break;
                }
            }
            elementEvent(events);
            _elementEvent(events);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error during mouse event", e);
            throw e;
        }
    }

    protected void checkElementOnTop(){
        String elemID = null;
        String panelID = null;
        boolean blocked = false;
        //Find panel and elem on top
        for (int i=panels.size()-1; i>=0; i--) {
            Panel panel = panels.get(i);
            if (panel.mouseOver() && panel.isVisible()) {
                if (panel.blockEvent){
                    blocked = true;
                }
                for (Element elem : panel.elements) {
                    if (elem.pointOver() && elem.isVisible()){
                        elemID = elem.id;
                        panelID = panel.id;
                        blocked = false;
                    }
                }
            }
        }
        for (Panel panel : panels) {
            for (Element elem : panel.elements) {
                if (elem.id.equals(elemID) && panel.id.equals(panelID) && !blocked){
                    elem.setElemOnTop(true);
                }
                else{
                    elem.setElemOnTop(false);
                }
            }
        }
    }

    public void _mouseEvent(String eventType, int button, MouseEvent event) {
        try {
            ArrayList<Event> events = new ArrayList<>();
            mouseEvent(eventType, button, event);
            if (eventType.equals("mouseWheel")) {
                for (Panel panel : panels) {
                    if (panel.mouseOver() && panel.isVisible() && panel.blockEvent) {
                        activePanel = panel.id;
                        break;
                    }
                }
            }
            for (Panel panel : panels) {
                if (activePanel.equals(panel.id) && panel.mouseOver() && panel.isVisible() || panel.overrideBlocking) {
                    // Iterate in reverse order
                    for (int i=panel.elements.size()-1; i>=0; i--) {
                        if (panel.elements.get(i).active) {
                            try {
                                for (String eventName : panel.elements.get(i)._mouseEvent(eventType, button, event)) {
                                    events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
                                    if (eventName.equals("stop events")) {
                                        elementEvent(events);
                                        _elementEvent(events);
                                        return;
                                    }
                                }
                            }
                            catch(Exception e) {
                                LOGGER_MAIN.log(Level.SEVERE, String.format("Error during mouse event elem id:%s, panel id:%s", panel.id, panel.elements.get(i)), e);
                                throw e;
                            }
                        }
                    }
                }
            }
            elementEvent(events);
            _elementEvent(events);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error during mouse event", e);
            throw e;
        }
    }
    public void _keyboardEvent(String eventType, char _key) {
        try {
            ArrayList<Event> events = new ArrayList<>();
            keyboardEvent(eventType, _key);
            for (Panel panel : panels) {
                for (int i=panel.elements.size()-1; i>=0; i--) {
                    if (panel.elements.get(i).active && panel.isVisible()) {
                        for (String eventName : panel.elements.get(i)._keyboardEvent(eventType, _key)) {
                            events.add(new Event(panel.elements.get(i).id, panel.id, eventName));
                        }
                    }
                }
            }
            elementEvent(events);
            _elementEvent(events);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error during keyboard event", e);
            throw e;
        }
    }
}