package ui.element;
import json.JSONManager;
import player.Player;
import player.PlayerContainer;
import player.PlayerType;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import ui.Element;
import util.Util;

import java.util.ArrayList;
import java.util.List;

import static util.Util.brighten;
import static util.Util.papplet;

public class PlayerSelector extends Element {
    private static final int SCROLL_WIDTH = 20;

    private int rowHeight, playersToDisplay;
    private int bgColour;
    private int scroll = 0;
    private List<PlayerContainer> playerContainers = new ArrayList<>();

    public PlayerSelector(int x, int y, int w, int h, int bgColour, int playersToDisplay) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bgColour = bgColour;

        this.playersToDisplay = playersToDisplay;
        rowHeight = h/playersToDisplay;

        reset();
    }

    public void reset() {
        playerContainers.add(new PlayerContainer(getNextPlayerName(), PlayerType.LOCAL, Util.randColour()));
    }

    public void draw(PGraphics panelCanvas) {
        panelCanvas.pushStyle();

        panelCanvas.fill(bgColour);
        panelCanvas.rect(x, y, w, h);

        // Draw add player button
        panelCanvas.fill(papplet.color(220));
        panelCanvas.rect(x, y+rowHeight*(playersToDisplay-1), w, rowHeight);
        panelCanvas.fill(papplet.color(0));
        panelCanvas.textAlign(PConstants.LEFT, PConstants.TOP);
        panelCanvas.text( "Add Player", x, y+rowHeight*(playersToDisplay-1));

        // Draw players
        for (int i = 0; i < Math.min(playerContainers.size(), playersToDisplay-1); i ++) {
            panelCanvas.fill(playerContainers.get(i).getColour());
            panelCanvas.rect(x, y+rowHeight*i, w, rowHeight);
            panelCanvas.fill(papplet.color(0));
            panelCanvas.textAlign(PConstants.LEFT, PConstants.CENTER);
            panelCanvas.text(playerContainers.get(i + scroll).getName(), x+5, (float) (y+rowHeight*(i+0.5)));

            panelCanvas.fill(papplet.color(150));
            panelCanvas.rect((float) (x+w/2.0), y+rowHeight*i, (float) (w/2.0), rowHeight);
            panelCanvas.fill(papplet.color(0));
            panelCanvas.textAlign(PConstants.RIGHT, PConstants.CENTER);
            panelCanvas.text(playerContainers.get(i + scroll).getPlayerType().id, x+w-SCROLL_WIDTH-5, (float) (y+rowHeight*(i+0.5)));
        }


        //draw scroll
        int d = playerContainers.size() - playersToDisplay+1;
        if (d > 0) {
            panelCanvas.fill(brighten(bgColour, 100));
            panelCanvas.rect(x- SCROLL_WIDTH * JSONManager.loadFloatSetting("gui scale")+w, y, 20*JSONManager.loadFloatSetting("gui scale"), h);
            panelCanvas.fill(brighten(bgColour, -20));
            panelCanvas.rect(x- SCROLL_WIDTH *JSONManager.loadFloatSetting("gui scale")+w, y+(h-h/(float)(d+1))*scroll/(float)d, 20*JSONManager.loadFloatSetting("gui scale"), h/(float)(d+1));
        }

        panelCanvas.popStyle();
    }

    private String getNextPlayerName() {
        String name;
        int i = 1;
        while (true) {
            boolean valid = true;
            name = "Player " + (i++);
            for (PlayerContainer pt : playerContainers) {
                if (pt.getName().equals(name)) {
                    valid = false;
                }
            }
            if (valid)
                break;
        }
        return name;
    }

    public ArrayList<String> mouseEvent(String eventType, int button) {
        ArrayList<String> events = new ArrayList<>();
        if (eventType.equals("mousePressed")) {
            if (moveOver()) {
                // If hovering over scroll bar and scrolling
                if (playersToDisplay-1 < playerContainers.size() && papplet.mouseX - xOffset > x + w - SCROLL_WIDTH) {
                    scroll = (int) ((papplet.mouseY-(y-yOffset)) / (float)(h) * (playerContainers.size() - (playersToDisplay-2)));
                }
                else {
                    // If hovering over add player button add new player
                    if (papplet.mouseY - yOffset > y + rowHeight * (playersToDisplay - 1) && papplet.mouseY - yOffset < y + rowHeight * playersToDisplay &&
                            papplet.mouseX - xOffset > x + xOffset && papplet.mouseX - xOffset < x + xOffset + w) {
                        playerContainers.add(new PlayerContainer(getNextPlayerName(), PlayerType.LOCAL, Util.randColour()));
                    }
                    for (int i = 0; i < Math.min(playerContainers.size(), playersToDisplay - 1); i++) {
                        // If clicked on player
                        if (papplet.mouseY - yOffset > y + rowHeight * i && papplet.mouseY - yOffset < y + rowHeight * (i + 1) &&
                                papplet.mouseX - xOffset > x + xOffset && papplet.mouseX - xOffset < x + xOffset + w) {
                            // If clicked on player type box then toggle player type
                            if (papplet.mouseX - xOffset > x + xOffset + w / 2) {
                                playerContainers.get(i + scroll).togglePlayerType();
                            }
                        }
                    }
                }
            }
        } else if (eventType.equals("mouseDragged")) {
            if (moveOver()) {
                // If hovering over scroll bar and scrolling
                if (playersToDisplay - 1 < playerContainers.size() && papplet.mouseX - xOffset > x + w - SCROLL_WIDTH) {
                    scroll = (int) ((papplet.mouseY - (y - yOffset)) / (float) (h) * (playerContainers.size() - (playersToDisplay - 2)));
                }
            }
        }
        return events;
    }

    public ArrayList<String> mouseEvent(String eventType, int button, MouseEvent event) {
        ArrayList<String> events = new ArrayList<>();

        if (eventType.equals("mouseWheel")) {
            float count = event.getCount();
            scroll = (int)Util.between(0, scroll+count, Math.max(playerContainers.size() - (playersToDisplay-1), 0));
        }

        return events;
    }

    public boolean moveOver() {
        return papplet.mouseX-xOffset >= x && papplet.mouseX-xOffset <= x+w && papplet.mouseY-yOffset >= y && papplet.mouseY-yOffset <= y+rowHeight*(playersToDisplay);
    }

    public List<Player> getPlayers() {
        List<Player> players = new ArrayList<>();
        for (PlayerContainer playerContainer : playerContainers) {
            players.add(new Player(playerContainer.getName(), playerContainer.getPlayerType(), playerContainer.getColour()));
        }
        return players;
    }
}

