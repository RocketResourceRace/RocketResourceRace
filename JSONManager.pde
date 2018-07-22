class JSONManager {
  JSONObject menu, gameData, settings;

  JSONManager() {
    try {
      LOGGER_MAIN.fine("Initializing JSON Manager");
      menu = loadJSONObject("json/menu.json");
      gameData = loadJSONObject("json/data.json");
      try {
        settings = loadJSONObject("json/settings.json");
      }
      catch (NullPointerException e) {
        // Create new settings.json
        LOGGER_MAIN.info("creating new settings file");
        PrintWriter w = createWriter("json/settings.json");
        w.print("{}\n");
        w.flush();
        w.close();
        LOGGER_MAIN.info("Finished creating new settings file");
        settings = loadJSONObject("json/settings.json");
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

  String[] getProficiencies() {
    String[] returnArray = new String[getNumProficiencies()];
    for (int i = 0; i < returnArray.length; i ++) {
      returnArray[i] = indexToProficiencyDisplayName(i);
    }
    return returnArray;
  }

  int getNumProficiencies() {
    try {
      JSONArray proficienciesJSON = gameData.getJSONArray("proficiencies");
      return proficienciesJSON.size();
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.WARNING, "Error loading proficiencies from data.json", e);
      return 0;
    }
  }

  String indexToProficiencyID(int index) {
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

  String indexToProficiencyDisplayName(int index) {
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

  int proficiencyIDToIndex(String id) {
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
  
  int getNumEquipmentTypesFromClass(int classType){
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
  
  String[] getEquipmentFromClass(int type) {
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

  int getNumEquipmentClasses() {
    try {
      return gameData.getJSONArray("equipment").size();
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
  }

  String getEquipmentClass(int index) {
    try {
      return gameData.getJSONArray("equipment").getJSONObject(index).getString("id");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
  }

  String getEquipmentClassDisplayName(int index) {
    try {
      return gameData.getJSONArray("equipment").getJSONObject(index).getString("display name");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
  }
  
  String getEquipmentTypeID(int equipmentClass, int equipmentType){
    if (equipmentType == -1){
      LOGGER_MAIN.warning("No equipment type selected");
    }
    if (equipmentClass == -1){
      LOGGER_MAIN.warning("No equipment class selected");
    }
    try {
      return gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types").getJSONObject(equipmentType).getString("id");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
  }
  
  int[] getEquipmentTypeClassFromID(String id){
    for (int c=0; c < getNumEquipmentClasses(); c ++){
      for (int t=0; t < getNumEquipmentTypesFromClass(c); t ++){
        if (getEquipmentTypeID(c, t).equals(id)){
          return new int[] {c, t};
        }
      }
    }
    LOGGER_MAIN.warning("Equipment not found id:"+id);
    return null;
  }
  
  String getEquipmentTypeDisplayName(int equipmentClass, int equipmentType){
    try {
      return gameData.getJSONArray("equipment").getJSONObject(equipmentClass).getJSONArray("types").getJSONObject(equipmentType).getString("display name");
    }
    catch (NullPointerException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
    catch (IndexOutOfBoundsException e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading equipment from data.json", e);
      throw e;
    }
  }

  int getResIndex(String s) {
    // Get the index for a resource
    try {
      return JSONIndex(gameData.getJSONArray("resources"), s);
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting resource index for: " + s);
      throw e;
    }
  }
  String getResString(int r) {
    // Get the string for an index
    try {
      return gameData.getJSONArray("resources").getJSONObject(r).getString("id");
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting resource string for: " + r, e);
      throw e;
    }
  }
  
  boolean resourceIsEquipment(int r){
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

  void saveSetting(String id, int val) {
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

  void saveSetting(String id, float val) {
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

  void saveSetting(String id, String val) {
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

  void saveSetting(String id, boolean val) {
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

  void writeSettings() {
    try {
      LOGGER_MAIN.info("Saving settings to file");
      saveJSONObject(settings, "settings.json");
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error saving settings");
      throw e;
    }
  }

  boolean hasFlag(String panelID, String elemID, String flag) {
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

  void loadDefaultSettings() {
    // Reset all the settings to their default values
    LOGGER_MAIN.info("Loading default settings for all settings");
    try {
      JSONArray defaultSettings = menu.getJSONArray("default settings");
      for (int i=0; i<defaultSettings.size(); i++) {
        JSONObject setting = defaultSettings.getJSONObject(i);
        if (setting.getString("type").equals("int")) {
          saveSetting(setting.getString("id"), setting.getInt("value"));
        } else if (setting.getString("type").equals("float")) {
          saveSetting(setting.getString("id"), setting.getFloat("value"));
        } else if (setting.getString("type").equals("string")) {
          saveSetting(setting.getString("id"), setting.getString("value"));
        } else if (setting.getString("type").equals("boolean")) {
          saveSetting(setting.getString("id"), setting.getBoolean("value"));
        } else {
          LOGGER_MAIN.warning("Invalid setting type: " + setting.getString("id"));
        }
      }
    }
    catch (Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading default settings", e);
      throw e;
    }
  }

  void loadInitialSettings() {
    // Set all the settings to either the default value, or the value already set
    LOGGER_MAIN.info("Loading initial settings");
    try {
      JSONArray defaultSettings = menu.getJSONArray("default settings");
      for (int i=0; i<defaultSettings.size(); i++) {
        JSONObject setting = defaultSettings.getJSONObject(i);
        if (settings.get(setting.getString("id")) == null) {
          if (setting.getString("type").equals("int")) {
            saveSetting(setting.getString("id"), setting.getInt("value"));
          } else if (setting.getString("type").equals("float")) {
            saveSetting(setting.getString("id"), setting.getFloat("value"));
          } else if (setting.getString("type").equals("string")) {
            saveSetting(setting.getString("id"), setting.getString("value"));
          } else if (setting.getString("type").equals("boolean")) {
            saveSetting(setting.getString("id"), setting.getBoolean("value"));
          } else {
            LOGGER_MAIN.warning("Invalid setting type: "+setting.getString("type"));
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

  int loadIntSetting(String id) {
    // Load a setting that is an int
    try {
      return  settings.getInt(id);
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading string setting: " + id, e);
      throw e;
    }
  }

  float loadFloatSetting(String id) {
    // Load a setting that is an float
    try {
      return  settings.getFloat(id);
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading float setting: "+ id, e);
      throw e;
    }
  }

  String loadStringSetting(String id) {
    // Load a setting that is an string
    try {
      return  settings.getString(id);
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading string setting " + id, e);
      throw e;
    }
  }

  boolean loadBooleanSetting(String id) {
    // Load a setting that is an string
    try {
      return  settings.getBoolean(id);
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading int setting: "+ id, e);
      throw e;
    }
  }

  void saveDefault(String id) {
    LOGGER_MAIN.info("Saving all default settings");
    JSONArray defaultSettings = menu.getJSONArray("default settings");
    for (int i=0; i<defaultSettings.size(); i++) {
      if (defaultSettings.getJSONObject(i).getString("id").equals(id)) {
        JSONObject setting = defaultSettings.getJSONObject(i);
        if (setting.getString("type").equals("int")) {
          saveSetting(setting.getString("id"), setting.getInt("value"));
        } else if (setting.getString("type").equals("float")) {
          saveSetting(setting.getString("id"), setting.getFloat("value"));
        } else if (setting.getString("type").equals("string")) {
          saveSetting(setting.getString("id"), setting.getString("value"));
        } else if (setting.getString("type").equals("boolean")) {
          saveSetting(setting.getString("id"), setting.getBoolean("value"));
        } else {
          LOGGER_MAIN.warning("Invalid setting type: "+ setting.getString("type"));
        }
      }
    }
    writeSettings();
  }

  JSONObject findJSONObject(JSONArray j, String id) {
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

  String getElementType(String panel, String element) {
    try {
      JSONArray elems = findJSONObject(menu.getJSONArray("states"), panel).getJSONArray("elements");
      return findJSONObject(elems, element).getString("type");
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error finding element type with id: "+ element + " on panel " + panel, e);
      throw e;
    }
  }

  HashMap<String, String[]> getChangeStateButtons() {
    // Store all the buttons that when clicked change the state
    try {
      LOGGER_MAIN.fine("Loading buttons that change the state");
      HashMap returnHash = new HashMap<String, String[]>();
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
      LOGGER_MAIN.log(Level.SEVERE, "Error getting change state buttons", e);
      throw e;
    }
  }

  HashMap<String, String[]> getChangeSettingButtons() {
    // Store all the buttons that when clicked change a setting
    try {
      LOGGER_MAIN.fine("Loading buttons that change a setting");
      HashMap returnHash = new HashMap<String, String[]>();
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

  String getSettingName(String id, String panelID) {
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
  String menuStateTitle(String id) {
    // Gets the titiel for menu state. Reutnrs null if there is no title defined
    try {
      JSONObject panel = findJSONObject(menu.getJSONArray("states"), id);
      return panel.getString("title");
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error getting menu state title with id:"+id, e);
      throw e;
    }
  }

  void loadMenuElements(State state, float guiScale) {
    // Load all the menu panels in to menu state
    LOGGER_MAIN.info("Loading in menu elements using JSON");
    try {
      JSONArray panels = menu.getJSONArray("states");
      for (int i=0; i<panels.size(); i++) {
        JSONObject panel = panels.getJSONObject(i);
        state.addPanel(panel.getString("id"), 0, 0, width, height, true, true, color(255, 255, 255, 255), color(0));
        loadPanelMenuElements(state, panel.getString("id"), guiScale);
      }
    }
    catch(Exception e) {
      LOGGER_MAIN.log(Level.SEVERE, "Error loading menu elements", e);
      throw e;
    }
  }

  void loadPanelMenuElements(State state, String panelID, float guiScale) {
    // Load in the elements from JSON menu into panel
    // NOTE: "default value" in elements object means value is not saved to setting (and if not defined will be saved)
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
        x = elem.getInt("x")*scale+width/2;
        y = elem.getInt("y")*scale+height/2;
        w = elem.getInt("w")*scale;
        h = elem.getInt("h")*scale;

        // Other attributes
        type = elem.getString("type");
        id = elem.getString("id");

        // Optional attributes
        if (elem.isNull("bg colour")) {
          bgColour = color(100);
        } else {
          bgColour = elem.getInt("bg colour");
        }

        if (elem.isNull("setting")) {
          setting = "";
        } else {
          setting = elem.getString("setting");
        }

        if (elem.isNull("stroke colour")) {
          strokeColour = color(150);
        } else {
          strokeColour = elem.getInt("stroke colour");
        }

        if (elem.isNull("text colour")) {
          textColour = color(255);
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
          step = 0.5;
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
            state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(150), bgColour, strokeColour, color(0), lower, loadFloatSetting(setting), upper, major, minor, step, true, text), panelID);
          } else {
            state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(150), bgColour, strokeColour, color(0), lower, elem.getFloat("default value"), upper, major, minor, step, true, text), panelID);
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
          DropDown dd = new DropDown((int)x, (int)y, (int)w, (int)h, color(150), text, elem.getString("options type"), 10);
          dd.setOptions(options);
          if (elem.isNull("default value")) {
            switch (dd.optionTypes) {
            case "floats":
              dd.setSelected(""+jsManager.loadFloatSetting(setting));
              break;
            case "strings":
              dd.setSelected(jsManager.loadStringSetting(setting));
              break;
            case "ints":
              dd.setSelected(""+jsManager.loadIntSetting(setting));
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
      LOGGER_MAIN.log(Level.SEVERE, "Error loading menu elements", e);
      throw e;
    }
  }
  
  boolean resourceExists(String id) {
    for (int i = 0; i < gameData.getJSONArray("resources").size(); i++) {
      if (gameData.getJSONArray("resources").getJSONObject(i).getString("id").equals(id)) {
        return true;
      }
    }
    return false;
  }
}
