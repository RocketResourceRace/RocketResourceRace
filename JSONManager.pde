

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
      }
    }
    catch(Exception e){
      println("Error loading JSON");
    }
  }
  
  void saveSetting(String id, float val){
    // Save the setting to settings and write settings to file
    settings.setFloat(id, val);
    saveJSONObject(settings, "data/settings.json");
  }
  
  void saveSetting(String id, String val){
    // Save the setting to settings and write settings to file
    settings.setString(id, val);
    saveJSONObject(settings, "data/settings.json");
  }
  
  void saveSetting(String id, boolean val){
    // Save the setting to settings and write settings to file
    settings.setBoolean(id, val);
    saveJSONObject(settings, "data/settings.json");
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
    print(panel);
    JSONArray elems = findJSONObject(menu.getJSONArray("states"), panel).getJSONArray("elements");
    print(1);
    return findJSONObject(elems, element).getString("type");
  }
  
  HashMap<String, String> getChangeStateButtons(){
    // Store all the buttons that when clicked change the state
    HashMap returnHash = new HashMap<String, String>();
     JSONArray panels = menu.getJSONArray("states");
     for (int i=0; i<panels.size(); i++){
       JSONArray panelElems = panels.getJSONObject(i).getJSONArray("elements");
       for (int j=0; j<panelElems.size(); j++){
         if (!panelElems.getJSONObject(j).isNull("new state")){
           returnHash.put(panelElems.getJSONObject(j).getString("id"), panelElems.getJSONObject(j).getString("new state"));
         }
       }
     }
    return returnHash;
  }
  
  HashMap<String, String> getChangeSettingButtons(){
    // Store all the buttons that when clicked change a setting
    HashMap returnHash = new HashMap<String, String>();
     JSONArray panels = menu.getJSONArray("states");
     for (int i=0; i<panels.size(); i++){
       JSONArray panelElems = panels.getJSONObject(i).getJSONArray("elements");
       for (int j=0; j<panelElems.size(); j++){
         if (!panelElems.getJSONObject(j).isNull("setting")){
           returnHash.put(panelElems.getJSONObject(j).getString("id"), panelElems.getJSONObject(j).getString("setting"));
         }
       }
     }
    return returnHash;
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
    int bgColour, strokeColour, textColour, textSize, major, minor;
    float x, y, w, h, scale, lower, defaultValue, upper, step;
    String type, id, text;
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
      
      if (elem.isNull("default value")){
        defaultValue = 0.5;
      }
      else{
        defaultValue = elem.getFloat("default value");
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
      
      switch (type){
        case "button":
          state.addElement(id, new Button((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text), panelID);
          break;
        case "slider":
          state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(50), bgColour, strokeColour, color(0), lower, defaultValue, upper, major, minor, step, true, text), panelID);
          break;
      }
    }
  }
}
