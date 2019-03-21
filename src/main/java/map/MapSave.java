package map;


import party.Party;
import player.Player;

public class MapSave {
    float[] heightMap;
    public int mapWidth;
    public int mapHeight;
    public int[][] terrain;
    public Party[][] parties;
    public Building[][] buildings;
    public int startTurn;
    public int startPlayer;
    public Player[] players;
    MapSave(float[] heightMap, int mapWidth, int mapHeight, int[][] terrain, Party[][] parties, Building[][] buildings, int startTurn, int startPlayer, Player[] players) {
        this.heightMap = heightMap;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.terrain = terrain;
        this.parties = parties;
        this.buildings = buildings;
        this.startTurn = startTurn;
        this.startPlayer = startPlayer;
        this.players = players;
    }
}