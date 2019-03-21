package map;

import processing.core.PApplet;
import processing.data.JSONObject;

import static json.JSONManager.gameData;

public class Building {
    public int type;
    public int image_id;
    public Building(int type) {
        this(type, 0, -1);
    }

    Building(int type, int image_id) {
        this(type, image_id, -1);
    }

    Building(int type, int image_id, int player_id) {
        this.type = type;
        this.image_id = image_id + (player_id+1)*10000;
    }

    public void setHealth(float health) {
        this.image_id = PApplet.parseInt(this.image_id / 10000) * 10000 + PApplet.parseInt(health * 1000);
    }

    public float getHealth() {
        return this.image_id % 1000;
    }

    public int getDefence() {
        JSONObject o = gameData.getJSONArray("buildings").getJSONObject(type);
        if (o.hasKey("defence")) {
            return o.getInt("defence");
        } else {
            return 0;
        }
    }

    public boolean isDefenceBuilding() {
        return getDefence() > 0;
    }

    public int getPlayerID() {
        return (this.image_id / 10000) - 1;
    }
}