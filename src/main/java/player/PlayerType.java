package player;

public enum PlayerType {
    LOCAL("Local", 0), AI("AI", 2), BANDIT("Bandit", 1);

    public String id;
    public int intID;

    PlayerType(String id, int intID) {
        this.id = id;
        this.intID = intID;
    }
}
