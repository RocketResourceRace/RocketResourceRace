package control;

import json.JSONManager;
import player.Player;
import processing.data.JSONObject;
import ui.element.ResourceSummary;

import java.util.Objects;
import java.util.logging.Level;

import static json.JSONManager.findJSONObject;
import static json.JSONManager.gameData;
import static util.Logging.LOGGER_GAME;
import static util.Logging.LOGGER_MAIN;
import static util.Util.roundDp;

public class ResourceManager {
    private float [] startingResources;
    private String[] resourceNames;
    private int numResources;
    private ResourceSummary resourceSummary;
    private Player[] players;

    public ResourceManager(Player[] players) {
        this.players = players;
        initialiseResources();
        float[] totals = new float[resourceNames.length];
        resourceSummary = new ResourceSummary(0, 0, 70, resourceNames, startingResources, totals);
    }

    public ResourceSummary getResourceSummary() {
        return resourceSummary;
    }


    public void initialiseResources() {
        try {
            JSONObject js;
            numResources = gameData.getJSONArray("resources").size();
            resourceNames = new String[numResources];
            startingResources = new float[numResources];
            for (int i=0; i<numResources; i++) {
                js = gameData.getJSONArray("resources").getJSONObject(i);
                resourceNames[i] = js.getString("id");
                JSONObject sr = findJSONObject(gameData.getJSONObject("game options").getJSONArray("starting resources"), resourceNames[i]);
                if (sr != null) {
                    startingResources[i] = sr.getFloat("quantity");
                }

                // If resource has specified starting resource on menu
                else if (resourceNames[i].equals("food")) {
                    startingResources[i] = JSONManager.loadFloatSetting("starting food");
                } else if (resourceNames[i].equals("wood")) {
                    startingResources[i] = JSONManager.loadFloatSetting("starting wood");
                } else if (resourceNames[i].equals("stone")) {
                    startingResources[i] = JSONManager.loadFloatSetting("starting stone");
                } else if (resourceNames[i].equals("metal")) {
                    startingResources[i] = JSONManager.loadFloatSetting("starting metal");
                } else {
                    startingResources[i] = 0;
                }
                LOGGER_GAME.fine(String.format("Starting resource: %s = %f", resourceNames[i], startingResources[i]));
            }
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error most likely due to modified resources in data.json", e);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error initializing resources", e);
            throw e;
        }
    }

    public int getNumResources() {
        return numResources;
    }

    void addResource(int player, int resIndex, float quantity) {
        players[player].resources[resIndex] += quantity;
    }

    float getResourceOfPlayer(int player, int resource) {
        return players[player].resources[resource];
    }

    void updateResourcesSummary(float[] totals, byte[] resourceWarnings, int player) {
        resourceSummary.updateNet(totals);
        resourceSummary.updateStockpile(players[player].resources);
        resourceSummary.updateWarnings(resourceWarnings);
    }

    public boolean sufficientResources(float[] available, float[] required) {
        for (int i=0; i<numResources; i++) {
            if (available[i] < required[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean sufficientResources(float[] available, float[] required, boolean flash) {
        boolean t = true;
        for (int i=0; i<numResources; i++) {
            if (available[i] < required[i] && !Objects.equals(JSONManager.buildingString(i), "rocket progress")) {
                t = false;
                resourceSummary.flash(i);
            }
        }
        return t;
    }

    public void spendRes(Player player, float[] required) {
        for (int i=0; i<numResources; i++) {
            player.resources[i] -= required[i];
            LOGGER_GAME.fine(String.format("Player spending: %f %s", required[i], resourceNames[i]));
        }
    }

    public void reclaimRes(Player player, float[] required) {
        //reclaim half cost of building
        for (int i=0; i<numResources; i++) {
            player.resources[i] += required[i]/2;
            LOGGER_GAME.fine(String.format("Player reclaiming (half of building cost): %f %s", required[i], resourceNames[i]));
        }
    }

    public String resourcesList(float[] resources) {
        StringBuilder returnString = new StringBuilder();
        boolean notNothing = false;
        for (int i=0; i<numResources; i++) {
            if (resources[i]>0) {
                returnString.append(roundDp("" + resources[i], 1)).append(" ").append(resourceNames[i]).append(", ");
                notNothing = true;
            }
        }
        if (!notNothing)
            returnString.append("Nothing/Unknown");
        else if (returnString.length()-2 > 0)
            returnString = new StringBuilder(returnString.substring(0, returnString.length() - 2));
        return returnString.toString();
    }

    public float[] getStartingResourcesClone() {
        return startingResources.clone();
    }
}
