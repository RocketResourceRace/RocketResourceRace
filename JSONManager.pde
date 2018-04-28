

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
  
  void loadMenuElements(State state, String panelID, float guiScale){
    // Load in the elements from JSON menu into panel
    int bgColour, strokeColour, textColour, textSize;
    float x, y, w, h, scale;
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
        bgColour = color(150);
      }
      else{
        bgColour = elem.getInt("bg colour");
      }
      
      if (elem.isNull("stroke colour")){
        strokeColour = color(0);
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
      
      switch (type){
        case "button":
          //println((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text, panelID);
          state.addElement(id, new Button((int)x, (int)y, (int)w, (int)h, bgColour, strokeColour, textColour, textSize, CENTER, text), panelID);
          break;
      }
    }
  }
}
