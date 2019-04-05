package ui.element;
import player.PlayerType;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import ui.Element;
import util.Util;

import java.util.ArrayList;
import java.util.List;

import static util.Util.papplet;

public class PlayerSelector extends Element {
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
            panelCanvas.fill(papplet.color(200));
            panelCanvas.rect(x, y+rowHeight*i, w, rowHeight);
            panelCanvas.fill(papplet.color(0));
            panelCanvas.textAlign(PConstants.LEFT, PConstants.CENTER);
            panelCanvas.text(playerContainers.get(i + scroll).getName(), x, (float) (y+rowHeight*(i+0.5)));

            panelCanvas.fill(papplet.color(150));
            panelCanvas.rect((float) (x+w/2.0), y+rowHeight*i, (float) (w/2.0), rowHeight);
            panelCanvas.fill(papplet.color(0));
            panelCanvas.textAlign(PConstants.RIGHT, PConstants.CENTER);
            panelCanvas.text(playerContainers.get(i + scroll).getPlayerType().id, x+w, (float) (y+rowHeight*(i+0.5)));
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
            if (papplet.mouseY-yOffset > y+rowHeight*(playersToDisplay-1) && papplet.mouseY-yOffset < y+rowHeight*playersToDisplay &&
            papplet.mouseX-xOffset > x+xOffset && papplet.mouseX-xOffset < x+xOffset+w) {
                playerContainers.add(new PlayerContainer(getNextPlayerName(), PlayerType.LOCAL));
            }
            for (int i = 0; i < Math.min(playerContainers.size(), playersToDisplay-1); i ++) {
                // If clicked on player
                if (papplet.mouseY-yOffset > y+rowHeight*i && papplet.mouseY-yOffset < y+rowHeight*(i+1) &&
                        papplet.mouseX-xOffset > x+xOffset && papplet.mouseX-xOffset < x+xOffset+w) {
                    // If clicked on player type box then toggle player type
                    if (papplet.mouseX-xOffset > x+xOffset+w/2) {
                        playerContainers.get(i + scroll).togglePlayerType();
                    }
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
}

class PlayerContainer {
    private PlayerType playerType;
    private String name;

    PlayerContainer(String name, PlayerType playerType) {
        this.playerType = playerType;
        this.name = name;
    }

    PlayerType getPlayerType() {
        return playerType;
    }

    private void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    void togglePlayerType() {
        switch (playerType) {
            case LOCAL:
                setPlayerType(PlayerType.AI);
                break;
            case AI:
                setPlayerType(PlayerType.LOCAL);
                break;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
