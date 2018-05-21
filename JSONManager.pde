

class JSONManager{
  JSONObject menu, gameData, settings;
  
  JSONManager(){
    try{
      menu = loadJSONObject("menu.json");
      gameData = loadJSONObject("data.json");
      
      try{
        settings = loadJSONObject("settings.json");
      }
      catch (Exception e){
        println("creating new settings file");
        PrintWriter w = createWriter("data/settings.json");
        w.print("{}\n");
        w.flush();
        w.close();
        println("Finished creating new settings file");
        settings = loadJSONObject("settings.json");
        println("loading settings... ");
        loadDefaultSettings();
      }
      loadInitialSettings();
    }
    catch(Exception e){
      println("Error loading JSON");
      println(e);
    }
  }
  
  void saveSetting(String id, int val){
    // Save the setting to settings and write settings to file
    settings.setInt(id, val);
  }
  
  void saveSetting(String id, float val){
    // Save the setting to settings and write settings to file
    settings.setFloat(id, val);
  }
  
  void saveSetting(String id, String val){
    // Save the setting to settings and write settings to file
    settings.setString(id, val);
  }
  
  void saveSetting(String id, boolean val){
    // Save the setting to settings and write settings to file
    settings.setBoolean(id, val);
  }
  
  void writeSettings(){
    saveJSONObject(settings, "data/settings.json");
  }
  
  boolean hasFlag(String panelID, String elemID, String flag){
    JSONObject panel = findJSONObject(menu.getJSONArray("states"), panelID);
    JSONObject elem = findJSONObject(panel.getJSONArray("elements"), elemID);
    JSONArray flags = elem.getJSONArray("flags");
    if (flags != null){
      for (int i=0; i<flags.size(); i++){
        if (flags.getString(i).equals(flag)){
          return true;
        }
      }
    }
    return false;
  }
  
  void loadDefaultSettings(){
    // Reset all the settings to their default values
    JSONArray defaultSettings = menu.getJSONArray("default settings");
    for (int i=0; i<defaultSettings.size(); i++){
      JSONObject setting = defaultSettings.getJSONObject(i);
      if (setting.getString("type").equals("int")){
        saveSetting(setting.getString("id"), setting.getInt("value"));
      }
      else if (setting.getString("type").equals("float")){
        saveSetting(setting.getString("id"), setting.getFloat("value"));
      }
      else if (setting.getString("type").equals("string")){
        saveSetting(setting.getString("id"), setting.getString("value"));
      }
      else if (setting.getString("type").equals("boolean")){
        saveSetting(setting.getString("id"), setting.getBoolean("value"));
      }
      else{
        print("invalid setting type");
      }
    }
  }
  
  void loadInitialSettings(){
    // Set all the settings to either the default value, or the value already set
    JSONArray defaultSettings = menu.getJSONArray("default settings");
    for (int i=0; i<defaultSettings.size(); i++){
      JSONObject setting = defaultSettings.getJSONObject(i);
      if (settings.get(setting.getString("id")) == null){
        if (setting.getString("type").equals("int")){
          saveSetting(setting.getString("id"), setting.getInt("value"));
        }
        else if (setting.getString("type").equals("float")){
          saveSetting(setting.getString("id"), setting.getFloat("value"));
        }
        else if (setting.getString("type").equals("string")){
          saveSetting(setting.getString("id"), setting.getString("value"));
        }
        else if (setting.getString("type").equals("boolean")){
          saveSetting(setting.getString("id"), setting.getBoolean("value"));
        }
        else{
          print("invalid setting type");
        }
      }
    }
    writeSettings();
  }
  
  int loadIntSetting(String id){
    // Load a setting that is an int
    return  settings.getInt(id);
  }
  
  float loadFloatSetting(String id){
    // Load a setting that is an float
    return  settings.getFloat(id);
  }
  
  String loadStringSetting(String id){
    // Load a setting that is an string
    return  settings.getString(id);
  }
  
  boolean loadBooleanSetting(String id){
    // Load a setting that is an string
    return  settings.getBoolean(id);
  }
  
  JSONObject findJSONObject(JSONArray j, String id){
    // search for a json object in a json array with correct id
    for (int i=0; i<j.size(); i++){
      if (j.getJSONObject(i).getString("id").equals(id)){
        return j.getJSONObject(i);
      }
    }
    return null;
  }
  
  String getElementType(String panel, String element){
    JSONArray elems = findJSONObject(menu.getJSONArray("states"), panel).getJSONArray("elements");
    return findJSONObject(elems, element).getString("type");
  }
  
  HashMap<String, String[]> getChangeStateButtons(){
    // Store all the buttons that when clicked change the state
    HashMap returnHash = new HashMap<String, String[]>();
     JSONArray panels = menu.getJSONArray("states");
     for (int i=0; i<panels.size(); i++){
       JSONObject panel = panels.getJSONObject(i);
       JSONArray panelElems = panel.getJSONArray("elements");
       for (int j=0; j<panelElems.size(); j++){
         if (!panelElems.getJSONObject(j).isNull("new state")){
           returnHash.put(panelElems.getJSONObject(j).getString("id"), new String[]{panelElems.getJSONObject(j).getString("new state"), panel.getString("id")});
         }
       }
     }
    return returnHash;
  }
  
  HashMap<String, String[]> getChangeSettingButtons(){
    // Store all the buttons that when clicked change a setting
    HashMap returnHash = new HashMap<String, String[]>();
    JSONArray panels = menu.getJSONArray("states");
    for (int i=0; i<panels.size(); i++){
      JSONObject panel = panels.getJSONObject(i);
      JSONArray panelElems = panel.getJSONArray("elements");
      for (int j=0; j<panelElems.size(); j++){
        if (!panelElems.getJSONObject(j).isNull("setting")){
          returnHash.put(panelElems.getJSONObject(j).getString("id"), new String[]{panelElems.getJSONObject(j).getString("setting"), panel.getString("id")});
        }
      }
    }
  return returnHash;
  }
  
  String getSettingName(String id, String panelID){
    // Gets the name of the setting for an element or null if it doesnt have a settting
    JSONObject panel = findJSONObject(menu.getJSONArray("states"), panelID);
    JSONObject element = findJSONObject(panel.getJSONArray("elements"), id);
    return element.getString("setting");
  }
  String menuStateTitle(String id){
    // Gets the titiel for menu state. Reutnrs null if there is no title defined
    JSONObject panel = findJSONObject(menu.getJSONArray("states"), id);
    return panel.getString("title");
  }
  
  void loadMenuElements(State state, float guiScale){
    // Load all the menu panels in to menu state
     JSONArray panels = menu.getJSONArray("states");
     for (int i=0; i<panels.size(); i++){
       JSONObject panel = panels.getJSONObject(i);
       state.addPanel(panel.getString("id"), 0, 0, width, height, true, true, color(255, 255, 255, 255), color(0));
       loadPanelMenuElements(state, panel.getString("id"), guiScale);
     }
  }
  
  void loadPanelMenuElements(State state, String panelID, float guiScale){
    // Load in the elements from JSON menu into panel
    // NOTE: "default value" in elements object means value is not saved to setting (and if not defined will be saved)
    
    int bgColour, strokeColour, textColour, textSize, major, minor;
    float x, y, w, h, scale, lower, defaultValue, upper, step;
    String type, id, text, setting;
    String[] options;
    JSONArray elements = findJSONObject(menu.getJSONArray("states"), panelID).getJSONArray("elements");
    
    
    scale = 20 * guiScale;
    
    for (int i=0; i<elements.size(); i++){
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
      if (elem.isNull("bg colour")){
        bgColour = color(100);
      }
      else{
        bgColour = elem.getInt("bg colour");
      }
      
      if (elem.isNull("setting")){
        setting = "";
      }
      else{
        setting = elem.getString("setting");
      }
      
      if (elem.isNull("stroke colour")){
        strokeColour = color(150);
      }
      else{
        strokeColour = elem.getInt("stroke colour");
      }
      
      if (elem.isNull("text colour")){
        textColour = color(255);
      }
      else{
        textColour = elem.getInt("text colour");
      }
      
      if (elem.isNull("text size")){
        textSize = 16;
      }
      else{
        textSize = elem.getInt("text size");
      }
      
      if (elem.isNull("text")){
        text = "";
      }
      else{
        text = elem.getString("text");
      }
      
      if (elem.isNull("lower")){
        lower = 0;
      }
      else{
        lower = elem.getFloat("lower");
      }
      
      if (elem.isNull("upper")){
        upper = 1;
      }
      else{
        upper = elem.getFloat("upper");
      }
      
      if (elem.isNull("major")){
        major = 2;
      }
      else{
        major = elem.getInt("major");
      }
      
      if (elem.isNull("minor")){
        minor = 1;
      }
      else{
        minor = elem.getInt("minor");
      }
      
      if (elem.isNull("step")){
        step = 0.5;
      }
      else{
        step = elem.getFloat("step");
      }
      
      if (elem.isNull("options")){
        options = new String[0];
      }
      else{
        options = elem.getJSONArray("options").getStringArray();
      }
      
      // Check if there is a defualt value. If not try loading from settings
      switch (type){
        case "button":
          state.addElement(id, new Button((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text), panelID);
          break;
        case "slider":
          if (elem.isNull("default value")){
            state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(150), bgColour, strokeColour, color(0), lower, loadFloatSetting(setting), upper, major, minor, step, true, text), panelID);
          }
          else{
            state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(150), bgColour, strokeColour, color(0), lower, elem.getFloat("default value"), upper, major, minor, step, true, text), panelID);
          }
          break;
        case "tickbox":
          if (elem.isNull("default value")){
            state.addElement(id, new Tickbox((int)x, (int)y, (int)w, (int)h, loadBooleanSetting(setting), text), panelID);
          }
          else{
            state.addElement(id, new Tickbox((int)x, (int)y, (int)w, (int)h, elem.getBoolean("default value"), text), panelID);
          }
          break;
        case "dropdown":
          DropDown dd = new DropDown((int)x, (int)y, (int)w, (int)h, color(150), text, elem.getString("options type"));
          dd.setOptions(options);
          if (elem.isNull("default value")){
            switch (dd.optionTypes){
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
          }
          else{
            dd.setValue(elem.getString("default value"));
          }
          state.addElement(id, dd, panelID);
          break;
        default:
          println("invalid element type:", type);
          break;
      }
    }
  }
}
