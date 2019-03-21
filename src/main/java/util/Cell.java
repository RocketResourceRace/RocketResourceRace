package util;

import map.Building;
import party.Party;

public class Cell {
    protected int terrain;
    public Building building;
    public Party party;
    public boolean activeSight;

    public Cell(int terrain, Building building, Party party){
        this.terrain = terrain;
        this.building = building;
        this.party = party;
        this.activeSight = false; // Needs to be updated later
    }

    public int getTerrain(){
        return terrain;
    }
    public Building getBuilding(){
        return building;
    }
    public Party getParty(){
        return party;
    }
    public boolean getActiveSight(){
        return activeSight;
    }
    public void setTerrain(int terrain){
        this.terrain = terrain;
    }
    public void setBuilding(Building building){
        this.building = building;
    }
    public void setParty(Party party){
        this.party = party;
    }
    public void setActiveSight(boolean activeSight){
        this.activeSight = activeSight;
    }
}