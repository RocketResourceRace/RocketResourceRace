package control;

import json.JSONManager;
import map.Building;
import party.Party;
import processing.data.JSONArray;
import ui.element.NotificationManager;

import static json.JSONManager.*;
import static processing.core.PApplet.*;
import static util.GameInfo.turn;
import static util.GameInfo.turnNumber;
import static util.Logging.LOGGER_GAME;
import static util.Util.random;

public class ProductionManager {
    private ResourceManager resourceManager;
    private TaskManager taskManager;
    private Party[][] parties;
    private Building[][] buildings;
    private NotificationManager notificationManager;
    private int mapSize;

    public ProductionManager(ResourceManager r, TaskManager t, Party[][] parties, Building[][] buildings, int mapSize, NotificationManager n) {
        this.resourceManager = r;
        this.taskManager = t;
        this.parties = parties;
        this.buildings = buildings;
        this.mapSize = mapSize;
        this.notificationManager = n;
    }

    public boolean isEquipmentCollectionAllowed(int x, int y, int c, int t) {
        Building b = buildings[y][x];
        if (b != null && c != -1 && t != -1) {
            JSONArray sites = gameData.getJSONArray("equipment").getJSONObject(c).getJSONArray("types").getJSONObject(t).getJSONArray("valid collection sites");
            if (sites != null) {
                for (int j = 0; j < sites.size(); j++) {
                    if (buildingIndex(sites.getString(j)) == b.getType()) {
                        return true;
                    }
                }
            } else {
                // If no valid collection sites specified, then stockup can occur anywhere
                return true;
            }
        }
        return false;
    }


    public boolean isEquipmentCollectionAllowed(int x, int y) {
        int[] equipmentTypes = parties[y][x].equipment;
        for (int c = 0; c < equipmentTypes.length; c++) {
            if (isEquipmentCollectionAllowed(x, y, c, equipmentTypes[c])) {
                return true;
            }
        }
        return false;
    }

    private byte[] getResourceWarnings(float[] productivities) {
        byte[] warnings = new byte[productivities.length];
        for (int i = 0; i < productivities.length; i++) {
            if (productivities[i] == 0) {
                warnings[i] = 2;
            } else if (productivities[i] < 1) {
                warnings[i] = 1;
            }
        }
        return warnings;
    }

    public byte[] getResourceWarnings() {
        return getResourceWarnings(getResourceProductivities(getTotalResourceRequirements()));
    }

    public void updateResourcesSummary(boolean starving) {
        float[] totalResourceRequirements = getTotalResourceRequirements();
        float[] resourceProductivities = getResourceProductivities(totalResourceRequirements);

        float[] gross = getTotalResourceProductions(resourceProductivities, starving);
        float[] costs = getTotalResourceConsumptions(resourceProductivities, starving);
        float[] totals = getTotalResourceChanges(gross, costs);
        resourceManager.updateResourcesSummary(totals, getResourceWarnings(resourceProductivities), turn);
    }

    public void handlePartyExcessResources(int x, int y) {
        Party p = parties[y][x];
        int[][] excessResources = p.removeExcessEquipment();
        for (int i = 0; i < excessResources.length; i++) {
            if (excessResources[i] != null) {
                int type = excessResources[i][0];
                int quantity = excessResources[i][1];
                if (type != -1) {
                    if (isEquipmentCollectionAllowed(x, y, i, type)) {
                        LOGGER_GAME.fine(String.format("Recovering %d %s from party decreasing in size", quantity, JSONManager.getEquipmentTypeID(i, type)));
                        resourceManager.addResource(parties[y][x].player, JSONManager.getResIndex(JSONManager.getEquipmentTypeID(i, type)), quantity);
                    }
                }
            }
        }
    }

    public void updateResources(float[] resourceProductivities, boolean starving) {
        String[] tasks = taskManager.getTasks();
        float[][] taskOutcomes = taskManager.getTaskOutcomes();
        for (int y = 0; y < parties.length; y++) {
            for (int x = 0; x < parties.length; x++) {
                if (parties[y][x] != null) {
                    if (parties[y][x].player == turn) {
                        for (int task = 0; task < tasks.length; task++) {
                            if (parties[y][x].getTask()==task) {
                                for (int resource = 0; resource < resourceManager.getNumResources(); resource++) {
                                    if (resource != JSONManager.getResIndex(("units"))) {
                                        if (tasks[task].equals("Produce Rocket")) {
                                            resource = JSONManager.getResIndex(("rocket progress"));
                                        }

                                        resourceManager.addResource(turn, resource, max(getResourceChangesAtCell(x, y, resourceProductivities, starving)[resource], -resourceManager.getResourceOfPlayer(turn, resource)));
                                        if (tasks[task].equals("Produce Rocket")) {
                                            break;
                                        }
                                    } else if (resourceProductivities[JSONManager.getResIndex(("food"))] < 1) {
                                        float lost = (1 - resourceProductivities[JSONManager.getResIndex(("food"))]) * (0.01f+taskOutcomes[task][resource]) * parties[y][x].getUnitNumber();
                                        int totalLost = floor(lost);
                                        if (random(1) < lost-floor(lost)) {
                                            totalLost++;
                                        }
                                        parties[y][x].changeUnitNumber(-totalLost);
                                        handlePartyExcessResources(x, y);
                                        if (parties[y][x].getUnitNumber() == 0) {
                                            notificationManager.post("Party Starved", x, y, turnNumber, turn);
                                            LOGGER_GAME.info(String.format("Party starved at cell:(%d, %d) player:%s", x, y, turn));
                                        } else {
                                            notificationManager.post(String.format("Party Starving - %d lost", totalLost), x, y, turnNumber, turn);
                                            LOGGER_GAME.fine(String.format("Party Starving - %d lost at  cell: (%d, %d) player:%s", totalLost, x, y, turn));
                                        }
                                    } else {
                                        int prev = parties[y][x].getUnitNumber();
                                        float gained = getResourceChangesAtCell(x, y, resourceProductivities, starving)[resource];
                                        int totalGained = floor(gained);
                                        if (random(1) < gained-floor(gained)) {
                                            totalGained++;
                                        }
                                        parties[y][x].changeUnitNumber(totalGained);
                                        if (prev != JSONManager.loadIntSetting("party size") && parties[y][x].getUnitNumber() == JSONManager.loadIntSetting("party size") && parties[y][x].task == JSONIndex(gameData.getJSONArray("tasks"), "Super Rest")) {
                                            notificationManager.post("Party Full", x, y, turnNumber, turn);
                                            LOGGER_GAME.fine(String.format("Party full at  cell: (%d, %d) player:%s", x, y, turn));
                                        }
                                    }
                                }
                            }
                        }
                        if (parties[y][x].getUnitNumber() == 0) {
                            parties[y][x] = null;
                            LOGGER_GAME.finest(String.format("Setting party at cell:(%s, %s) to null becuase it has no units left in it", x, y));
                        }
                    }
                }
            }
        }
        if (resourceManager.getResourceOfPlayer(turn, JSONManager.getResIndex(("rocket progress"))) > 1000) {
            //display indicator saying rocket produced
            LOGGER_GAME.info("Rocket produced");
            for (int y = 0; y < parties.length; y++) {
                for (int x = 0; x < parties.length; x++) {
                    if (parties[y][x] != null) {
                        if (parties[y][x].player == turn) {
                            if (parties[y][x].getTask() == JSONIndex(gameData.getJSONArray("tasks"), "Produce Rocket")) {
                                notificationManager.post("Rocket Produced", x, y, turnNumber, turn);
                                parties[y][x].changeTask(JSONIndex(gameData.getJSONArray("tasks"), "Rest"));
                                buildings[y][x].setImageId(1);
                            }
                        }
                    }
                }
            }
        }
    }

    public float[] getResourceChangesAtCell(int x, int y, boolean starving) {
        return getResourceChangesAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()), starving);
    }

    public float[] getResourceProductivities() {
        return getResourceProductivities(getTotalResourceRequirements());
    }


    public float[] getTotalResourceRequirements() {
        float[] totalResourceRequirements = new float[resourceManager.getNumResources()];
        for (int y = 0; y < parties.length; y++) {
            for (int x = 0; x < parties.length; x++) {
                if (parties[y][x] != null) {
                    if (parties[y][x].player == turn) {
                        for (int resource = 0; resource < resourceManager.getNumResources(); resource++) {
                            totalResourceRequirements[resource] += getResourceRequirementsAtCell(x, y, resource);
                        }
                    }
                }
            }
        }
        return totalResourceRequirements;
    }

    private float getResourceRequirementsAtCell(int x, int y, int resource) {
        float[][] taskCosts = taskManager.getTaskCosts();
        float resourceRequirements = 0;
        for (int i = 0; i < taskCosts.length; i++) {
            if (parties[y][x].getTask() == i) {
                if (resource == JSONManager.getResIndex("food") && gameData.getJSONArray("tasks").getJSONObject(i).getString("id").equals("Super Rest") && parties[y][x].capped()) {
                    resourceRequirements += taskCosts[JSONManager.getTaskIndex("Rest")][resource] * parties[y][x].getUnitNumber();
                } else {
                    resourceRequirements += taskCosts[i][resource] * parties[y][x].getUnitNumber();
                }
            }
        }
        return resourceRequirements;
    }

    public float[] getResourceProductivities(float[] totalResourceRequirements) {
        float [] resourceProductivities = new float[resourceManager.getNumResources()];
        for (int i=0; i<resourceManager.getNumResources(); i++) {
            if (totalResourceRequirements[i]==0) {
                resourceProductivities[i] = 1;
            } else {
                resourceProductivities[i] = min(1, resourceManager.getResourceOfPlayer(turn, i)/totalResourceRequirements[i]);
            }
        }
        return resourceProductivities;
    }


    private float getProductivityAtCell(int x, int y, float[] resourceProductivities, boolean starving) {
        float productivity = 1;
        float[][] taskCosts = taskManager.getTaskCosts();
        for (int task = 0; task<taskCosts.length; task++) {
            if (parties[y][x].getTask() == task) {
                for (int resource = 0; resource < resourceManager.getNumResources(); resource++) {
                    if (getResourceRequirementsAtCell(x, y, resource) > 0) {
                        if (resource == 0 && starving) {
                            productivity = min(productivity, resourceProductivities[resource] + 0.5f);
                        } else {
                            productivity = min(productivity, resourceProductivities[resource]);
                        }
                    }
                }
            }
        }
        return productivity;
    }

    public float getProductivityAtCell(int x, int y, boolean starving) {
        return getProductivityAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()), starving);
    }

    public float[] resourceProductionAtCell(int x, int y, float[] resourceProductivities, boolean starving) {
        float[][] taskOutcomes = taskManager.getTaskOutcomes();
        float [] production = new float[resourceManager.getNumResources()];
        if (parties[y][x] != null) {
            if (parties[y][x].player == turn) {
                float productivity = getProductivityAtCell(x, y, resourceProductivities, starving);
                for (int task = 0; task < taskOutcomes.length; task++) {
                    if (parties[y][x].getTask()==task) {
                        for (int resource = 0; resource < resourceManager.getNumResources(); resource++) {
                            if (resource == JSONManager.getResIndex("units") && resourceProductivities[JSONManager.getResIndex(("food"))] < 1) {
                                production[resource] = 0;
                            } else if (resource == JSONManager.getResIndex("units")) {
                                production[resource] = min(parties[y][x].getUnitCap() - parties[y][x].getUnitNumber(), taskOutcomes[task][resource] * productivity * (float) parties[y][x].getUnitNumber());
                            } else {
                                production[resource] = taskOutcomes[task][resource] * productivity * (float) parties[y][x].getUnitNumber();
                            }
                        }
                    }
                }
            }
        }
        return production;
    }

    public float[] resourceProductionAtCell(int x, int y, boolean starving) {
        return resourceProductionAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()), starving);
    }

    public float[] getTotalResourceProductions(float[] resourceProductivities, boolean starving) {
        float[] amount = new float[resourceManager.getNumResources()];
        for (int x = 0; x < mapSize; x++) {
            for (int y = 0; y < mapSize; y++) {
                for (int res = 0; res < resourceManager.getNumResources(); res++) {
                    amount[res]+=resourceProductionAtCell(x, y, resourceProductivities, starving)[res];
                }
            }
        }
        return amount;
    }

    public float[] getTotalResourceProductions(boolean starving) {
        return getTotalResourceProductions(getResourceProductivities(getTotalResourceRequirements()), starving);
    }

    public float[] getResourceConsumptionAtCell(int x, int y, float[] resourceProductivities, boolean starving) {
        float[][] taskCosts = taskManager.getTaskCosts();
        float[][] taskOutcomes = taskManager.getTaskOutcomes();
        float [] consumption = new float[resourceManager.getNumResources()];
        if (parties[y][x] != null) {
            if (parties[y][x].player == turn) {
                float productivity = getProductivityAtCell(x, y, resourceProductivities, starving);
                for (int task = 0; task <taskCosts.length; task++) {
                    if (parties[y][x].getTask() == task) {
                        for (int resource = 0; resource < resourceManager.getNumResources(); resource++) {
                            if (resource == JSONManager.getResIndex("units") && resourceProductivities[JSONManager.getResIndex(("food"))] < 1) {
                                consumption[resource] += (1-resourceProductivities[JSONManager.getResIndex(("food"))]) * (0.01f+taskOutcomes[task][resource]) * parties[y][x].getUnitNumber();
                            } else {
                                consumption[resource] += getResourceRequirementsAtCell(x, y, resource) * productivity;
                            }
                        }
                    }
                }
            }
        }
        return consumption;
    }

    public float[] getResourceConsumptionAtCell(int x, int y, boolean starving) {
        return getResourceConsumptionAtCell(x, y, getResourceProductivities(getTotalResourceRequirements()), starving);
    }

    public float[] getTotalResourceConsumptions(float[] resourceProductivities, boolean starving) {
        float[] amount = new float[resourceManager.getNumResources()];
        for (int x = 0; x < mapSize; x++) {
            for (int y = 0; y < mapSize; y++) {
                for (int res = 0; res < resourceManager.getNumResources(); res++) {
                    amount[res] += getResourceConsumptionAtCell(x, y, resourceProductivities, starving)[res];
                }
            }
        }
        return amount;
    }

    public float[] getTotalResourceConsumptions(boolean starving) {
        return getTotalResourceConsumptions(getResourceProductivities(getTotalResourceRequirements()), starving);
    }

    public float[] getTotalResourceChanges(float[] grossResources, float[] costsResources) {
        float[] amount = new float[resourceManager.getNumResources()];
        for (int res = 0; res < resourceManager.getNumResources(); res++) {
            amount[res] = grossResources[res] - costsResources[res];
        }
        return amount;
    }

    private float[] getResourceChangesAtCell(int x, int y, float[] resourceProductivities, boolean starving) {
        float[] amount = new float[resourceManager.getNumResources()];
        for (int res = 0; res < resourceManager.getNumResources(); res++) {
            amount[res] = resourceProductionAtCell(x, y, resourceProductivities, starving)[res] - getResourceConsumptionAtCell(x, y, resourceProductivities, starving)[res];
        }
        return amount;
    }
}
