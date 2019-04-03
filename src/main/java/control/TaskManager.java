package control;

import json.JSONManager;
import processing.data.JSONObject;
import ui.Element;
import ui.element.TaskChooser;

import java.util.logging.Level;

import static json.JSONManager.gameData;
import static util.Constants.BEZEL;
import static util.Logging.LOGGER_MAIN;
import static util.Util.papplet;

public class TaskManager {
    private String[] tasks;
    private float[][] taskCosts;
    private float[][] taskOutcomes;

    public TaskManager() {
    }

    public void initialiseTasks(int numResources) {
        try {
            LOGGER_MAIN.fine("Initializing tasks");
            JSONObject js;
            int numTasks = gameData.getJSONArray("tasks").size();
            taskOutcomes = new float[numTasks][numResources];
            taskCosts = new float[numTasks][numResources];
            tasks = new String[numTasks];
            for (int i=0; i<numTasks; i++) {
                js = gameData.getJSONArray("tasks").getJSONObject(i);
                tasks[i] = js.getString("id");
                if (!js.isNull("production")) {
                    for (int r=0; r<js.getJSONArray("production").size(); r++) {
                        taskOutcomes[i][JSONManager.getResIndex((js.getJSONArray("production").getJSONObject(r).getString("id")))] = js.getJSONArray("production").getJSONObject(r).getFloat("quantity");
                    }
                }
                if (!js.isNull("consumption")) {
                    for (int r=0; r<js.getJSONArray("consumption").size(); r++) {
                        taskCosts[i][JSONManager.getResIndex((js.getJSONArray("consumption").getJSONObject(r).getString("id")))] = js.getJSONArray("consumption").getJSONObject(r).getFloat("quantity");
                    }
                }
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error initializing tasks", e);
            throw e;
        }
    }

    public Element getTaskChooser() {
        return new TaskChooser(BEZEL, BEZEL*4+30+30, 220, 8, papplet.color(150), papplet.color(50), tasks, 10);
    }

    float[][] getTaskCosts() {
        return taskCosts;
    }

    public String[] getTasks() {
        return tasks;
    }

    float[][] getTaskOutcomes() {
        return taskOutcomes;
    }
}
