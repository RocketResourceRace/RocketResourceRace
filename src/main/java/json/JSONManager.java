package json;


import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;
import state.State;
import state.elements.Button;
import state.elements.DropDown;
import state.elements.Slider;
import state.elements.Tickbox;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Level;

import static processing.core.PApplet.CENTER;
import static processing.core.PApplet.max;
import static util.Logging.LOGGER_MAIN;
import static util.Util.RESOURCES_ROOT;
import static util.Util.papplet;

public class JSONManager {
    private static JSONObject menu;
    public static JSONObject gameData;
    private static JSONObject settings;
    private static JSONArray defaultSettings;

    public JSONManager() {
        try {
            LOGGER_MAIN.fine("Initializing JSON Manager");
            menu = loadJSONObject(RESOURCES_ROOT+"json/menu.json");
            defaultSettings = loadJSONObject(RESOURCES_ROOT+"json/default_settings.json").getJSONArray("default settings");
            gameData = loadJSONObject(RESOURCES_ROOT+"json/data.json");
            try {
                settings = loadJSONObject("settings.json");
            }
            catch (RuntimeException e) {
                // Create new settings.json
                LOGGER_MAIN.info("creating new settings file");
                PrintWriter w = createWriter("settings.json");
                w.print("{}\n");
                w.flush();
                w.close();
                LOGGER_MAIN.info("Finished creating new settings file");
                settings = loadJSONObject("settings.json");
                LOGGER_MAIN.info("loading settings... ");
                loadDefaultSettings();
            }
            loadInitialSettings();
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading JSON", e);
            throw e;
        }
    }

    private PrintWriter createWriter(String s) {
        return PApplet.createWriter(new File(s));
    }

    public static JSONObject loadJSONObject(String s) {
        return PApplet.loadJSONObject(new File(s));
    }

    private static void saveJSONObject(JSONObject o, String s) {
        (new PApplet()).saveJSONObject(o, s);
    }

    public static float getRawProficiencyGain(String id){
        if (!gameData.getJSONObject("raw training gains").isNull(id)){
            return gameData.getJSONObject("raw training gains").getFloat(id);
        }
        else{
            LOGGER_MAIN.warning("No training gains found for id:"+id);
            return 0;
        }
    }

    public static int getMaxTerrainMovementCost(){
        // Find the maximum possible terrain cost
        int mx = 0;
        for(int i=0; i < gameData.getJSONArray("terrain").size(); i++){
            mx = max(gameData.getJSONArray("terrain").getJSONObject(i).getInt("movement cost"), mx);
        }
        return mx;
    }

    public int getMaxTerrainSightCost(){
        // Find the maximum possible terrain cost
        int mx = 0;
        for(int i=0; i < gameData.getJSONArray("terrain").size(); i++){
            mx = max(gameData.getJSONArray("terrain").getJSONObject(i).getInt("sight cost"), mx);
        }
        return mx;
    }

    public static String[] getProficiencies() {
        String[] returnArray = new String[getNumProficiencies()];
        for (int i = 0; i < returnArray.length; i ++) {
            returnArray[i] = indexToProficiencyDisplayName(i);
        }
        return returnArray;
    }

    public static int getNumProficiencies() {
        try {
            JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
            return proficienciesJSON.size();
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error loading proficiencies from data.json", e);
            return 0;
        }
    }

    public static String indexToProficiencyID(int index) {
        try {
            JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
            String rs = proficienciesJSON.getJSONObject(index).getString("id");
            if (rs == null) {
                LOGGER_MAIN.warning("Could not find proficiency id with index: "+index);
            }
            return rs;
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER_MAIN.warning("Proficiency index out of range: "+index);
            return "";
        }
    }

    public static String indexToProficiencyDisplayName(int index) {
        try {
            if (index < 0) {
                LOGGER_MAIN.warning("Could not find proficiency display name with index: "+index);
                return "";
            }
            JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
            String rs = proficienciesJSON.getJSONObject(index).getString("display name");
            if (rs == null) {
                LOGGER_MAIN.warning("Could not find proficiency display name with index: "+index);
            }
            return rs;
        }
        catch (IndexOutOfBoundsException e) {
            LOGGER_MAIN.warning("Proficiency index out of range: "+index);
            return "";
        }
    }

    public static int proficiencyIDToIndex(String id) {
        try {
            JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
            for (int i = 0; i < proficienciesJSON.size(); i++) {
                if (proficienciesJSON.getJSONObject(i).getString("id").equals(id)) {
                    return i;
                }
            }
            LOGGER_MAIN.severe("Could not find proficiency id: "+id);
            return -1;
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error loading proficiencies from data.json", e);
            return -1;
        }
    }

    public JSONObject getEquipmentObject(int classIndex, int typeIndex){
        try{
            return gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex);
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading equipment other class blocking. Class:%d, type:%d id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)), e);
            throw e;
        }
    }

    public static String[] getOtherClassBlocking(int classIndex, int typeIndex){
        try{
            JSONObject typeObject = gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex);
            if (typeObject.isNull("other class blocking")){
                return null;
            }
            else {
                String[] rs = new String[typeObject.getJSONArray("other class blocking").size()];
                for (int i=0; i < typeObject.getJSONArray("other class blocking").size(); i ++){
                    rs[i] = typeObject.getJSONArray("other class blocking").getString(i);
                }
                return rs;
            }
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading equipment other class blocking. Class:%d, type:%d id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)), e);
            throw e;
        }
    }

    public static String getEquipmentImageFileName(int classIndex, int typeIndex){
        try{
            if (!gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex).isNull("img")){
                return "data/img/equipment/"+gameData.getJSONArray("equipment").getJSONObject(classIndex).getJSONArray("types").getJSONObject(typeIndex).getString("img");
            }
            else{
                LOGGER_MAIN.warning(String.format("Could not find img file for equipment class:%d, type:%d, id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)));
                return "";
            }
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error loading equipment file name from data.json. Class:%d, type:%d id:%s", classIndex, typeIndex, getEquipmentTypeID(classIndex, typeIndex)), e);
            throw e;
        }
    }

    public static int getNumEquipmentTypesFromClass(int classType){
        // type is the index of the type in data.json
        if (classType<0){
            LOGGER_MAIN.warning("Class is invalid");
            return 0;
        }
        try {
            return gameData.getJSONArray("equipment").getJSONObject(classType).getJSONArray("types").size();
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static String[] getEquipmentFromClass(int type) {
        // type is the index of the type in data.json
        try {
            String[] rss = new String[getNumEquipmentTypesFromClass(type)];
            JSONArray types = gameData.getJSONArray("equipment").getJSONObject(type).getJSONArray("types");
            for(int i = 0; i < rss.length; i ++) {
                rss[i] = types.getJSONObject(i).getString("display name");
                if (rss[i] == null){
                    LOGGER_MAIN.warning("No value for display name found for equipment type:"+type);
                }
            }
            return rss;
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static int getNumEquipmentClasses() {
        try {
            return gameData.getJSONArray("equipment").size();
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static String getEquipmentClass(int index) {
        try {
            return gameData.getJSONArray("equipment").getJSONObject(index).getString("id");
        }
        catch (NullPointerException | IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static String getEquipmentClassDisplayName(int index) {
        try {
            return gameData.getJSONArray("equipment").getJSONObject(index).getString("display name");
        }
        catch (NullPointerException | IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static String getEquipmentTypeID(int equipmentClass, int equipmentType){
        if (equipmentType == -1){
            LOGGER_MAIN.warning("No equipment type selected");
        }
        if (equipmentClass == -1){
            LOGGER_MAIN.warning("No equipment class selected");
        }
        try {
            return gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types").getJSONObject(equipmentType).getString("id");
        }
        catch (NullPointerException | IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static int getEquipmentClassFromID(String classID){
        for (int c=0; c < getNumEquipmentClasses(); c ++){
            if (getEquipmentClass(c).equals(classID)){
                return c;
            }
        }
        LOGGER_MAIN.warning("Equipment class not found id:"+classID);
        return -1;
    }

    public static int[] getEquipmentTypeClassFromID(String id){
        for (int c=0; c < getNumEquipmentClasses(); c ++){
            for (int t=0; t < getNumEquipmentTypesFromClass(c); t ++){
                if (getEquipmentTypeID(c, t).equals(id)){
                    return new int[] {c, t};
                }
            }
        }
        LOGGER_MAIN.warning("Equipment type not found id:"+id);
        return null;
    }

    public static String getEquipmentTypeDisplayName(int equipmentClass, int equipmentType){
        try {
            return gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types").getJSONObject(equipmentType).getString("display name");
        }
        catch (NullPointerException | IndexOutOfBoundsException e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
            throw e;
        }
    }

    public static int getResIndex(String s) {
        // Get the index for a resource
        try {
            return JSONIndex(gameData.getJSONArray("resources"), s);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource index for: " + s);
            throw e;
        }
    }
    public static String getResString(int r) {
        // Get the string for an index
        try {
            return gameData.getJSONArray("resources").getJSONObject(r).getString("id");
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource string for: " + r, e);
            throw e;
        }
    }

    public static boolean resourceIsEquipment(int r){
        // Check if a resource represents a type of equipment
        try {
            if (!gameData.getJSONArray("resources").getJSONObject(r).isNull("is equipment")){
                return gameData.getJSONArray("resources").getJSONObject(r).getBoolean("is equipment");
            } else{
                return false;
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error checking if resource is equipment index: " + r, e);
            throw e;
        }
    }

    public static float getEffectivenessConstant(String type){
        try{
            if (!gameData.getJSONObject("effectiveness constants").isNull(type)){
                return gameData.getJSONObject("effectiveness constants").getFloat(type);
            }
            else {
                LOGGER_MAIN.warning("Error finding effectiveness type in data.json: "+type);
                return 0;
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting effectiveness constant: " + type, e);
            throw e;
        }
    }

    public static void saveSetting(String id, int val) {
        // Save the setting to settings and write settings to file
        try {
            LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
            settings.setInt(id, val);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
            throw e;
        }
    }

    public static void saveSetting(String id, float val) {
        // Save the setting to settings and write settings to file
        try {
            LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
            settings.setFloat(id, val);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
            throw e;
        }
    }

    public static void saveSetting(String id, String val) {
        // Save the setting to settings and write settings to file
        try {
            LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
            settings.setString(id, val);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
            throw e;
        }
    }

    public static void saveSetting(String id, boolean val) {
        // Save the setting to settings and write settings to file
        try {
            LOGGER_MAIN.fine(String.format("Saving setting id:'%s', value:%s", id, val));
            settings.setBoolean(id, val);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving setting for: " + id + " with value " + val, e);
            throw e;
        }
    }

    public static void writeSettings() {
        try {
            LOGGER_MAIN.info("Saving settings to file");
            saveJSONObject(settings, "settings.json");
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error saving settings");
            throw e;
        }
    }

    public static boolean hasFlag(String panelID, String elemID, String flag) {
        try {
            JSONObject panel = findJSONObject(menu.getJSONArray("states"), panelID);
            JSONObject elem = findJSONObject(panel.getJSONArray("elements"), elemID);
            JSONArray flags = elem.getJSONArray("flags");
            if (flags != null) {
                for (int i=0; i<flags.size(); i++) {
                    if (flags.getString(i).equals(flag)) {
                        return true;
                    }
                }
            }
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, String.format("Could not find flag for panel:'%s', element:'%s', flag:'%s'", panelID, elemID, flag), e);
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error finding flag for panel:'%s', element:'%s', flag:'%s'", panelID, elemID, flag), e);
            throw e;
        }
        return false;
    }

    private void loadDefaultSettings() {
        // Reset all the settings to their default values
        LOGGER_MAIN.info("Loading default settings for all settings");
        try {
            for (int i=0; i<defaultSettings.size(); i++) {
                JSONObject setting = defaultSettings.getJSONObject(i);
                switch (setting.getString("type")) {
                    case "int":
                        saveSetting(setting.getString("id"), setting.getInt("value"));
                        break;
                    case "float":
                        saveSetting(setting.getString("id"), setting.getFloat("value"));
                        break;
                    case "string":
                        saveSetting(setting.getString("id"), setting.getString("value"));
                        break;
                    case "boolean":
                        saveSetting(setting.getString("id"), setting.getBoolean("value"));
                        break;
                    default:
                        LOGGER_MAIN.warning("Invalid setting type: " + setting.getString("id"));
                        break;
                }
            }
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading default settings", e);
            throw e;
        }
    }

    private void loadInitialSettings() {
        // Set all the settings to either the default value, or the value already set
        LOGGER_MAIN.info("Loading initial settings");
        try {
            for (int i=0; i<defaultSettings.size(); i++) {
                JSONObject setting = defaultSettings.getJSONObject(i);
                if (settings.get(setting.getString("id")) == null) {
                    switch (setting.getString("type")) {
                        case "int":
                            saveSetting(setting.getString("id"), setting.getInt("value"));
                            break;
                        case "float":
                            saveSetting(setting.getString("id"), setting.getFloat("value"));
                            break;
                        case "string":
                            saveSetting(setting.getString("id"), setting.getString("value"));
                            break;
                        case "boolean":
                            saveSetting(setting.getString("id"), setting.getBoolean("value"));
                            break;
                        default:
                            LOGGER_MAIN.warning("Invalid setting type: " + setting.getString("type"));
                            break;
                    }
                }
            }
            writeSettings();
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading initial settings", e);
            throw e;
        }
    }

    public static int loadIntSetting(String id) {
        // Load a setting that is an int
        try {
            return  settings.getInt(id);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading string setting: " + id, e);
            throw e;
        }
    }

    public static float loadFloatSetting(String id) {
        // Load a setting that is an float
        try {
            return  settings.getFloat(id);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading float setting: "+ id, e);
            throw e;
        }
    }

    public static String loadStringSetting(String id) {
        // Load a setting that is an string
        try {
            return  settings.getString(id);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading string setting " + id, e);
            throw e;
        }
    }

    public static boolean loadBooleanSetting(String id) {
        // Load a setting that is an string
        try {
            return  settings.getBoolean(id);
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading int setting: "+ id, e);
            throw e;
        }
    }

    public static void saveDefault(String id) {
        LOGGER_MAIN.info("Saving all default settings");
        for (int i=0; i<defaultSettings.size(); i++) {
            if (defaultSettings.getJSONObject(i).getString("id").equals(id)) {
                JSONObject setting = defaultSettings.getJSONObject(i);
                switch (setting.getString("type")) {
                    case "int":
                        saveSetting(setting.getString("id"), setting.getInt("value"));
                        break;
                    case "float":
                        saveSetting(setting.getString("id"), setting.getFloat("value"));
                        break;
                    case "string":
                        saveSetting(setting.getString("id"), setting.getString("value"));
                        break;
                    case "boolean":
                        saveSetting(setting.getString("id"), setting.getBoolean("value"));
                        break;
                    default:
                        LOGGER_MAIN.warning("Invalid setting type: " + setting.getString("type"));
                        break;
                }
            }
        }
        writeSettings();
    }

    public static JSONObject findJSONObject(JSONArray j, String id) {
        // search for a json object in a json array with correct id
        try {
            for (int i=0; i<j.size(); i++) {
                if (j.getJSONObject(i).getString("id").equals(id)) {
                    return j.getJSONObject(i);
                }
            }
            return null;
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error finding JSON object with id likely cause by issue with code in data.json: "+ id, e);
            return null;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error finding JSON object with id: "+ id, e);
            throw e;
        }
    }

    public static int findJSONObjectIndex(JSONArray j, String id) {
        // search for a json object in a json array with correct id
        try {
            for (int i=0; i<j.size(); i++) {
                if (j.getJSONObject(i).getString("id").equals(id)) {
                    return i;
                }
            }
            return -1;
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error finding JSON object with id likely cause by issue with code in data.json: "+ id, e);
            return -1;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error finding JSON object with id: "+ id, e);
            throw e;
        }
    }

    public static String getElementType(String panel, String element) {
        try {
            JSONArray elems = findJSONObject(menu.getJSONArray("states"), panel).getJSONArray("elements");
            return findJSONObject(elems, element).getString("type");
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error finding element type with id: "+ element + " on panel " + panel, e);
            throw e;
        }
    }

    public static HashMap<String, String[]> getChangeStateButtons() {
        // Store all the buttons that when clicked change the states
        try {
            LOGGER_MAIN.fine("Loading buttons that change the states");
            HashMap<String, String[]> returnHash = new HashMap<>();
            JSONArray panels = menu.getJSONArray("states");
            for (int i=0; i<panels.size(); i++) {
                JSONObject panel = panels.getJSONObject(i);
                JSONArray panelElems = panel.getJSONArray("elements");
                for (int j=0; j<panelElems.size(); j++) {
                    if (!panelElems.getJSONObject(j).isNull("new state")) {
                        returnHash.put(panelElems.getJSONObject(j).getString("id"), new String[]{panelElems.getJSONObject(j).getString("new state"), panel.getString("id")});
                    }
                }
            }
            return returnHash;
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting change states buttons", e);
            throw e;
        }
    }

    public static HashMap<String, String[]> getChangeSettingButtons() {
        // Store all the buttons that when clicked change a setting
        try {
            LOGGER_MAIN.fine("Loading buttons that change a setting");
            HashMap<String, String[]> returnHash = new HashMap<>();
            JSONArray panels = menu.getJSONArray("states");
            for (int i=0; i<panels.size(); i++) {
                JSONObject panel = panels.getJSONObject(i);
                JSONArray panelElems = panel.getJSONArray("elements");
                for (int j=0; j<panelElems.size(); j++) {
                    if (!panelElems.getJSONObject(j).isNull("setting")) {
                        returnHash.put(panelElems.getJSONObject(j).getString("id"), new String[]{panelElems.getJSONObject(j).getString("setting"), panel.getString("id")});
                    }
                }
            }
            return returnHash;
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting buttons that chagne settings", e);
            throw e;
        }
    }

    public static String getSettingName(String id, String panelID) {
        // Gets the name of the setting for an element or null if it doesnt have a settting
        try {
            JSONObject panel = findJSONObject(menu.getJSONArray("states"), panelID);
            JSONObject element = findJSONObject(panel.getJSONArray("elements"), id);
            return element.getString("setting");
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting setting name with id:"+id+", panel: "+panelID, e);
            throw e;
        }
    }
    public static String menuStateTitle(String id) {
        // Gets the titiel for menu states. Reutnrs null if there is no title defined
        try {
            JSONObject panel = findJSONObject(menu.getJSONArray("states"), id);
            return panel.getString("title");
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting menu states title with id:"+id, e);
            throw e;
        }
    }

    public static void loadMenuElements(State state, float guiScale) {
        // Load all the menu panels in to menu states
        LOGGER_MAIN.info("Loading in menu state.elements using JSON");
        try {
            JSONArray panels = menu.getJSONArray("states");
            for (int i=0; i<panels.size(); i++) {
                JSONObject panel = panels.getJSONObject(i);
                state.addPanel(panel.getString("id"), 0, 0, papplet.width, papplet.height, true, true, papplet.color(255, 255, 255, 255), papplet.color(0));
                loadPanelMenuElements(state, panel.getString("id"), guiScale);
            }
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading menu state.elements", e);
            throw e;
        }
    }

    private static void loadPanelMenuElements(State state, String panelID, float guiScale) {
        // Load in the state.elements from JSON menu into panel
        // NOTE: "default value" in state.elements object means value is not saved to setting (and if not defined will be saved)
        try {
            int bgColour, strokeColour, textColour, textSize, major, minor;
            float x, y, w, h, scale, lower, upper, step;
            String type, id, text, setting;
            String[] options;
            JSONArray elements = findJSONObject(menu.getJSONArray("states"), panelID).getJSONArray("elements");


            scale = 20 * guiScale;

            for (int i=0; i<elements.size(); i++) {
                JSONObject elem = elements.getJSONObject(i);

                // Transform the normalised coordinates to screen coordinates
                x = elem.getInt("x")*scale+papplet.width/2f;
                y = elem.getInt("y")*scale+papplet.height/2f;
                w = elem.getInt("w")*scale;
                h = elem.getInt("h")*scale;

                // Other attributes
                type = elem.getString("type");
                id = elem.getString("id");

                // Optional attributes
                if (elem.isNull("bg colour")) {
                    bgColour = papplet.color(100);
                } else {
                    bgColour = elem.getInt("bg colour");
                }

                if (elem.isNull("setting")) {
                    setting = "";
                } else {
                    setting = elem.getString("setting");
                }

                if (elem.isNull("stroke colour")) {
                    strokeColour = papplet.color(150);
                } else {
                    strokeColour = elem.getInt("stroke colour");
                }

                if (elem.isNull("text colour")) {
                    textColour = papplet.color(255);
                } else {
                    textColour = elem.getInt("text colour");
                }

                if (elem.isNull("text size")) {
                    textSize = 16;
                } else {
                    textSize = elem.getInt("text size");
                }

                if (elem.isNull("text")) {
                    text = "";
                } else {
                    text = elem.getString("text");
                }

                if (elem.isNull("lower")) {
                    lower = 0;
                } else {
                    lower = elem.getFloat("lower");
                }

                if (elem.isNull("upper")) {
                    upper = 1;
                } else {
                    upper = elem.getFloat("upper");
                }

                if (elem.isNull("major")) {
                    major = 2;
                } else {
                    major = elem.getInt("major");
                }

                if (elem.isNull("minor")) {
                    minor = 1;
                } else {
                    minor = elem.getInt("minor");
                }

                if (elem.isNull("step")) {
                    step = 0.5f;
                } else {
                    step = elem.getFloat("step");
                }

                if (elem.isNull("options")) {
                    options = new String[0];
                } else {
                    options = elem.getJSONArray("options").getStringArray();
                }

                // Check if there is a defualt value. If not try loading from settings
                switch (type) {
                    case "button":
                        state.addElement(id, new Button((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text), panelID);
                        break;
                    case "slider":
                        if (elem.isNull("default value")) {
                            state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, papplet.color(150), bgColour, strokeColour, papplet.color(0), lower, loadFloatSetting(setting), upper, major, minor, step, true, text), panelID);
                        } else {
                            state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, papplet.color(150), bgColour, strokeColour, papplet.color(0), lower, elem.getFloat("default value"), upper, major, minor, step, true, text), panelID);
                        }
                        break;
                    case "tickbox":
                        if (elem.isNull("default value")) {
                            state.addElement(id, new Tickbox((int)x, (int)y, (int)w, (int)h, loadBooleanSetting(setting), text), panelID);
                        } else {
                            state.addElement(id, new Tickbox((int)x, (int)y, (int)w, (int)h, elem.getBoolean("default value"), text), panelID);
                        }
                        break;
                    case "dropdown":
                        DropDown dd = new DropDown((int)x, (int)y, (int)w, (int)h, papplet.color(150), text, elem.getString("options type"), 10);
                        dd.setOptions(options);
                        if (elem.isNull("default value")) {
                            switch (dd.optionTypes) {
                                case "floats":
                                    dd.setSelected(""+JSONManager.loadFloatSetting(setting));
                                    break;
                                case "strings":
                                    dd.setSelected(JSONManager.loadStringSetting(setting));
                                    break;
                                case "ints":
                                    dd.setSelected(""+JSONManager.loadIntSetting(setting));
                                    break;
                            }
                        } else {
                            dd.setValue(elem.getString("default value"));
                        }
                        state.addElement(id, dd, panelID);
                        break;
                    default:
                        LOGGER_MAIN.warning("Invalid element type: "+ type);
                        break;
                }
            }
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error loading menu state.elements", e);
            throw e;
        }
    }

    public static boolean resourceExists(String id) {
        for (int i = 0; i < gameData.getJSONArray("resources").size(); i++) {
            if (gameData.getJSONArray("resources").getJSONObject(i).getString("id").equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static int getTaskIndex(String id) {
        try {
            return JSONIndex(gameData.getJSONArray("tasks"), id);
        } catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting resource index for: " + id);
            throw e;
        }
    }


    /*
    Gets the index of the JSONObject with 'id' set to id in the JSONArray j
    */
    public static int JSONIndex(JSONArray j, String id) {
        for (int i=0; i<j.size(); i++) {
            if (j.getJSONObject(i).getString("id").equals(id)) {
                return i;
            }
        }
        LOGGER_MAIN.warning(String.format("Invalid JSON index '%s'", id));
        return -1;
    }

    public static boolean JSONContainsStr(JSONArray j, String id) {
        try {
            if (id == null || j == null)
                return false;
            for (int i=0; i<j.size(); i++) {
                if (j.getString(i).equals(id)) {
                    return true;
                }
            }
            return false;
        }
        catch(Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, String.format("Error finding string in JSON array, '%s'", id), e);
            throw e;
        }
    }


    public static int terrainIndex(String terrain) {
        try {
            int k = JSONIndex(gameData.getJSONArray("terrain"), terrain);
            if (k>=0) {
                return k;
            }
            LOGGER_MAIN.warning("Invalid terrain type, "+terrain);
            return 0;
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error most likely due to incorrect JSON code. building:"+terrain, e);
            return 0;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting terrain index for:"+terrain, e);
            throw e;
        }
    }
    public static int buildingIndex(String building) {
        try {
            int k = JSONIndex(gameData.getJSONArray("buildings"), building);
            if (k>=0) {
                return k;
            }
            LOGGER_MAIN.warning("Invalid building type, "+building);
            return 0;
        }
        catch (NullPointerException e) {
            LOGGER_MAIN.log(Level.WARNING, "Error most likely due to incorrect JSON code. building:"+building, e);
            return 0;
        }
        catch (Exception e) {
            LOGGER_MAIN.log(Level.SEVERE, "Error getting building index for:"+building, e);
            throw e;
        }
    }
}
