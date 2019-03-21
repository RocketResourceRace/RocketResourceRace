package util;

import json.JSONManager;
import processing.core.PImage;
import processing.data.JSONObject;

import java.util.HashMap;
import java.util.logging.Level;

import static json.JSONManager.gameData;
import static processing.core.PConstants.ALPHA;
import static util.Logging.LOGGER_MAIN;
import static util.Util.*;

public class Image {
    public static HashMap<String, PImage> tileImages;
    public static HashMap<String, PImage[]> buildingImages;
    public static PImage[] partyBaseImages;
    public static PImage[] partyImages;
    public static PImage[] taskImages;
    public static HashMap<String, PImage> lowImages;
    public static HashMap<String, PImage> tile3DImages;
    public static HashMap<String, PImage> equipmentImages;
    public static PImage bombardImage;


    public static void loadImages() {
        try {
            LOGGER_MAIN.fine("Loading images");
            tileImages = new HashMap<String, PImage>();
            lowImages = new HashMap<String, PImage>();
            tile3DImages = new HashMap<String, PImage>();
            buildingImages = new HashMap<String, PImage[]>();
            equipmentImages = new HashMap<String, PImage>();

            partyBaseImages = new PImage[]{
                    loadImage(RESOURCES_ROOT +"img/party/battle.png"),
                    loadImage(RESOURCES_ROOT +"img/party/flag.png"),
                    loadImage(RESOURCES_ROOT +"img/party/bandit.png")
            };

            bombardImage = loadImage(RESOURCES_ROOT +"img/ui/bombard.png");
            LOGGER_MAIN.finer("Loading task images");
            taskImages = new PImage[gameData.getJSONArray("tasks").size()];
            for (int i=0; i<gameData.getJSONArray("terrain").size(); i++) {
                JSONObject tileType = gameData.getJSONArray("terrain").getJSONObject(i);
                tileImages.put(tileType.getString("id"), loadImage(RESOURCES_ROOT +"img/terrain/"+tileType.getString("img")));
                if (!tileType.isNull("low img")) {
                    lowImages.put(tileType.getString("id"), loadImage(RESOURCES_ROOT +"img/terrain/"+tileType.getString("low img")));
                }
                if (!tileType.isNull("img3d")) {
                    tile3DImages.put(tileType.getString("id"), loadImage(RESOURCES_ROOT +"img/terrain/"+tileType.getString("img3d")));
                }
            }
            LOGGER_MAIN.finer("Loading building images");
            for (int i=0; i<gameData.getJSONArray("buildings").size(); i++) {
                JSONObject buildingType = gameData.getJSONArray("buildings").getJSONObject(i);
                PImage[] p = new PImage[buildingType.getJSONArray("img").size()];
                for (int i2=0; i2< buildingType.getJSONArray("img").size(); i2++)
                    p[i2] = loadImage(RESOURCES_ROOT +"img/building/"+buildingType.getJSONArray("img").getString(i2));
                buildingImages.put(buildingType.getString("id"), p);
            }
            LOGGER_MAIN.finer("Loading task images");
            for (int i=0; i<gameData.getJSONArray("tasks").size(); i++) {
                JSONObject task = gameData.getJSONArray("tasks").getJSONObject(i);
                if (!task.isNull("img")) {
                    taskImages[i] = loadImage(RESOURCES_ROOT +"img/task/"+task.getString("img"));
                }
            }
            LOGGER_MAIN.finer("Loading equipment images");

            // Load equipment icons
            for (int c = 0; c < JSONManager.getNumEquipmentClasses(); c++){
                for (int t=0; t < JSONManager.getNumEquipmentTypesFromClass(c); t++){
                    String fn = JSONManager.getEquipmentImageFileName(c, t);
                    if (!fn.equals("")){
                        equipmentImages.put(JSONManager.getEquipmentTypeID(c, t), loadImage(fn));
                        LOGGER_MAIN.finest("Loading equipment image id:"+JSONManager.getEquipmentTypeID(c, t));
                    } else {
                        equipmentImages.put(JSONManager.getEquipmentTypeID(c, t), papplet.createImage(1, 1, ALPHA));
                        LOGGER_MAIN.finest("Loading empty image for equipment id:"+JSONManager.getEquipmentTypeID(c, t));
                    }
                }
            }

            LOGGER_MAIN.finer("Finished loading images");
        }

        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Something wrong with loading images", e);
            throw e;
        }
    }
}
