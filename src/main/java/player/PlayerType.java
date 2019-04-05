package player;

public enum PlayerType {
    LOCAL("Local"), AI("AI");

    public String id;

    PlayerType(String id) {
        this.id = id;
    }
}
