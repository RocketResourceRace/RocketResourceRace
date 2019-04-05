package ui.element;
import player.Player;
import player.PlayerType;
import processing.core.PConstants;
import processing.core.PGraphics;
import ui.Element;

import java.util.ArrayList;
import java.util.List;

import static util.Util.papplet;

public class PlayerSelector extends Element {
    private int rowHeight, playersToDisplay;
    private int bgColour;
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
        panelCanvas.fill(bgColour);
        panelCanvas.rect(x, y, w, h);

        panelCanvas.fill(papplet.color(255));
        panelCanvas.rect(x, y+rowHeight*(playersToDisplay-1), w, rowHeight);
        panelCanvas.fill(papplet.color(0));
        panelCanvas.textAlign(PConstants.LEFT, PConstants.TOP);
        panelCanvas.text( "Add Player", x, y+rowHeight*(playersToDisplay-1));
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
                System.out.println("123");
            }
        }
        return events;
    }
}

class PlayerContainer {
    private PlayerType playerType;
    private String name;

    public PlayerContainer(String name, PlayerType playerType) {
        this.playerType = playerType;
        this.name = name;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
