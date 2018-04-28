

class JSONManager{
  JSONObject menu;
  
  JSONManager(){
    try{
      menu = loadJSONObject("menu.json");
      gameData = loadJSONObject("data.json");
    }
    catch(Exception e){
      println("Error loading JSON");
    }
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
  
  void loadMenuElements(State state, float guiScale){
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
        step = elem.getInt("step");
      }
      
      switch (type){
        case "button":
          state.addElement(id, new Button((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text), panelID);
          break;
        case "slider":
        println((int)x, (int)y, (int)w, (int)h, color(50), bgColour, strokeColour, color(0), lower, defaultValue, upper, major, minor, step, true, text, panelID);
        //int x, int y, int w, int h, color KnobColour, color bgColour, color strokeColour, color scaleColour, float lower, float value, float upper, int major, int minor, float step, boolean horizontal, String name
          state.addElement(id, new Slider((int)x, (int)y, (int)w, (int)h, color(50), bgColour, strokeColour, color(0), lower, defaultValue, upper, major, minor, step, true, text), panelID);
          break;
      }
    }
  }
}
