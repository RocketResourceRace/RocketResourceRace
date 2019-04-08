package player;

import player.PlayerType;

public class PlayerContainer {
    private PlayerType playerType;
    private String name;
    private int colour;

    public PlayerContainer(String name, PlayerType playerType, int colour) {
        this.playerType = playerType;
        this.name = name;
        this.colour = colour;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    private void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public void togglePlayerType() {
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


    public int getColour() {
        return colour;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }
}
