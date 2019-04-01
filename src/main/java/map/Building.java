package map;

import static json.JSONManager.gameData;

public class Building {
    private int type;
    private int imageId;
    private float health;
    private int playerId;

    public Building(int type, int imageId, int playerId) {
        this.type = type;
        this.imageId = imageId;
        this.playerId = playerId;
        if (gameData.getJSONArray("buildings").getJSONObject(type).hasKey("defence")) {
            this.health = 1;
        } else {
            this.health = 0;
        }
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getHealth() {
        return health;
    }

    public int getDefence() {
        if (health>0) {
            return gameData.getJSONArray("buildings").getJSONObject(type).getInt("defence");
        } else {
            return 0;
        }
    }

    public boolean isDefenceBuilding() {
        return getDefence() > 0;
    }

    public int getPlayerId() {
        return playerId;
    }

    int getImageId() {
        return imageId;
    }

    public int getType() {
        return type;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public void setType(int type) {
        this.type = type;
    }
}