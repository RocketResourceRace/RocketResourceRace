class Console extends Element{
  private int textSize, cursorX, maxLength=-1;
  private ArrayList<StringBuilder> text;
  private boolean drawCursor = false;
  private String allowedChars;
  private Map map;
  private JSONObject commands; 
  
  Console(int x, int y, int w, int h, int textSize){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.textSize = textSize;
    text = new ArrayList<StringBuilder>();
    text.add(new StringBuilder(" > "));
    cursorX = 3;
    commands = loadJSONObject("commands.json");
  }

  void giveMap(Map map){
    this.map = map;
  }
  StringBuilder toStr(){
    StringBuilder s = new StringBuilder();
    for (StringBuilder s1 : text){
      s.append(s1+"\n");
    }
    s.deleteCharAt(s.length()-1);
    return s;
  }
  
  ArrayList<StringBuilder> strToText(String s){
    ArrayList<StringBuilder> t = new ArrayList<StringBuilder>();
    t.add(new StringBuilder());
    char c;
    for (int i=0; i<s.length(); i++){
      c = s.charAt(i);
      if (c == '\n'){
        t.add(new StringBuilder());
      }
      else{
        getInputString(t).append(c);
      }
    }
    return t;
  }
  
  void draw(PGraphics canvas){
    canvas.pushStyle();
    float ts = textSize*jsManager.loadFloatSetting("text scale");
    canvas.textFont(createFont("Monospaced", ts));
    canvas.textAlign(LEFT, BOTTOM);
    int time = millis();
    drawCursor = (time/500)%2==0 || keyPressed;
    canvas.fill(255);
    for (int i=0; i < text.size(); i++){
      canvas.text(""+text.get(i), x, height/2-((text.size()-i-1)*ts*1.2));
    }
    if (drawCursor){
      canvas.stroke(255);
      canvas.rect(x+canvas.textWidth(getInputString().substring(0, cursorX)), y+height/2-ts*1.2, 1, ts*1.2);
    }
    canvas.popStyle();
  }
  StringBuilder getInputString(){
    return getInputString(text);
  }
  StringBuilder getInputString(ArrayList<StringBuilder> t){
    return t.get(t.size()-1);
  }
  
  int cxyToC(int cx, int cy){
    int a=0;
    for (int i=0; i<cy; i++){
      a += text.get(i).length()+1;
    }
    return a + cx;
  }
  
  int getCurX(int cy){
    int i=0;
    float ts = textSize*jsManager.loadFloatSetting("text scale");
    textSize(ts);
    for(; i<text.get(cy).length(); i++){
      if (textWidth(text.get(cy).substring(0, i)) + x > mouseX)
        break;
    }
    if (0 <= i && i <= text.get(cy).length()){
      return i;
    }
    return cursorX;
  }
  
  ArrayList<String> _mouseEvent(String eventType, int button){
    ArrayList<String> events = new ArrayList<String>();
    if (eventType == "mousePressed" && button == LEFT){
      cursorX = getCurX(text.size()-1);
    }
    return events;
  }
  
  void clearTextAt(){
    StringBuilder s = toStr();
    String s2 = s.substring(0, cxyToC(cursorX-1, text.size()-1)) + (s.substring(cxyToC(cursorX, text.size()-1), s.length()));
    text = strToText(s2);
  }
  
  void sendLine(String line){
    text.add(text.size()-1, new StringBuilder(line));
  }
  
  void invalid(String message){
    sendLine("Invalid command. "+message);
  }
  
  void invalid(){
    invalid("");
  }
  
  String[] getPossibleSubCommands(JSONObject command){
    Iterable keys = command.getJSONObject("sub commands").keys();
    String[] commandsList = new String[command.getJSONObject("sub commands").size()];
    int i=0;
    for (Object subCommand: keys){
      commandsList[i] = subCommand.toString();
      i++;
    }
    return commandsList;
  }
  
  void invalidSubcommand(JSONObject command, String[] args, int position){
    String[] commandsList = getPossibleSubCommands(command);
    invalid(String.format("Sub-command not found: %s. Possible sub-commands for command %s: %s", args[position], join(Arrays.copyOfRange(args, 0, position), " "), join(commandsList, " ")));
  }
  
  void invalidMissingSubCommand(JSONObject command, String[] args, int position){
    String[] commandsList = getPossibleSubCommands(command);
    getPossibleSubCommands(command);
    invalid(String.format("Sub-command required for %s. Possible sub-commands for command %s: %s", args[position], join(Arrays.copyOfRange(args, 0, position), " "), join(commandsList, " ")));
  }
  
  void invalidMissingValue(JSONObject command, String[] args, int position){
    invalid(String.format("Value required for %s. Value type: %s", args[position], command.getString("value type")));
  }
  
  void invalidValue(JSONObject command, String[] args, int position){
    invalid(String.format("Invalid value for %s. Value type: %s", args[position], command.getString("value type")));
  }
  
  void invalidHelp(String command){
    invalid(String.format("help: invalid command '%s'", command)); 
  }
  
  void commandFileError(String error){
    invalid(error);
    LOGGER_GAME.severe(error);
  }
    
  void getHelp(String[] splitCommand){
    try {
      if(splitCommand.length == 1){
        sendLine("Command list:");
        for (Object c: commands.keys()){
          String c1 = c.toString();
          sendLine(String.format("%-22s%-22s", c1, commands.getJSONObject(c1).getString("basic description")));
        }
      } else {
        if (commands.hasKey(splitCommand[1])) {
          JSONObject command = commands.getJSONObject(splitCommand[1]);
          for (int i = 1; i < splitCommand.length; i++){
            if (command.getString("type").equals("container")){
              if (i == splitCommand.length-1){
                sendLine(String.format("%s: %s", splitCommand[i], command.getString("detailed description")));
                sendLine("Command list:");
                for (Object c: command.getJSONObject("sub commands").keys()){
                  String c1 = c.toString();
                  sendLine(String.format("%-22s%-22s", c1, command.getJSONObject("sub commands").getJSONObject(c1).getString("basic description")));
                }
                return;
              } else if (command.getJSONObject("sub commands").hasKey(splitCommand[i+1])) {
                command = command.getJSONObject("sub commands").getJSONObject(splitCommand[i+1]);
              } else {
                invalidHelp(splitCommand[i+1]);
                return;
              }
            } else if (i == splitCommand.length-1) {
              String c1 = splitCommand[i];
              sendLine(c1+":");
              sendLine(command.getString("detailed description"));
              return;
            } else {
              invalidHelp(splitCommand[i+1]);
              return;
            }
          }
        } else {
          invalidHelp(splitCommand[1]);
        }
      }
    } catch (Exception e) {
      LOGGER_MAIN.severe("Error getting help in console");
      throw (e);
    }
  }
  
  void doCommand(String rawCommand){
    String[] splitCommand = rawCommand.split(" ");
    if(splitCommand.length==0){
      invalid();
      return;
    }
    if(commands.hasKey(splitCommand[0])){
      JSONObject command = commands.getJSONObject(splitCommand[0]);
      if (command.getString("type").equals("help")){
        getHelp(splitCommand);
      } else {
        handleCommand(command, splitCommand, 0);
      }
    } else {
      invalid();
    }
  }
  
  void handleCommand(JSONObject command, String[] arguments, int position){
    switch(command.getString("type")){
      case "container":
        if(arguments.length>position+1){
          JSONObject subCommands = command.getJSONObject("sub commands");
          if(subCommands.hasKey(arguments[position+1])){
            handleCommand(subCommands.getJSONObject(arguments[position+1]), arguments, position+1);
          } else {
            invalidSubcommand(command, arguments, position+1);
          }
        } else {
          invalidMissingSubCommand(command, arguments, position);
        }
        break;
      case "setting":
        if(command.hasKey("value type")){
          if(arguments.length>position+1){
            switch(command.getString("value type")){
              case "boolean":
                  String value = arguments[position+1].toLowerCase();
                  Boolean setting;
                  if(value.equals("true") || value.equals("t") || value.equals("1")){
                    setting = true;
                  } else if (value.equals("false") || value.equals("f")|| value.equals("0")){
                    setting = false;
                  } else {
                    invalidValue(command, arguments, position);
                    return;
                  }
                  sendLine(String.format("Changing %s setting", arguments[position]));
                  jsManager.saveSetting(command.getString("setting id"), setting);
                  if(command.hasKey("regenerate map")&&command.getBoolean("regenerate map")&&map != null){
                    sendLine("This requires regenerating the map. This might take a moment and will mean some randomised features will change");
                    map.generateShape();
                  }
                  sendLine(String.format("%s setting changed!", arguments[position]));
                  break;
              default:
                commandFileError("Command defines invalid value type");
                break;
            }
          } else {
            invalidMissingValue(command, arguments, position);
          }
        } else {
          commandFileError("Command doesn't define a value type");
        }
        break;
      default:
        commandFileError("Command has invalid type");
        break;
    }
  }
  
  void doCommandOld(String rawCommand){
    String[] splitCommand = rawCommand.split(" ");
    if(splitCommand.length==0){
      sendLine("invalid command");
      return;
    }
    switch(splitCommand[0]){
      case "display":
        if(splitCommand.length>1){
          switch(splitCommand[1]){
            case "sub_tile_boundaries":
              if(splitCommand.length==3){
                String value = splitCommand[2].toLowerCase();
                Boolean setting;
                if(value.equals("true") || value.equals("t") || value.equals("1")){
                  setting = true;
                } else if (value.equals("false") || value.equals("f")|| value.equals("0")){
                  setting = false;
                } else {
                  sendLine("Invalid argument for display sub_tile_boundaries: give either true or false");
                  return;
                }
                sendLine("Changing sub_tile_boundaries setting");
                jsManager.saveSetting("tile stroke", setting);
                if(map != null){
                  sendLine("This requires regenerating the map. This might take a moment and will mean some randomised features will change");
                  map.generateShape();
                }
                sendLine("sub_tile_boundaries setting changed!");
              } else {
                sendLine("Invalid number of arguments for display sub_tile_boundaries");
              }
              break;
            case "cell_coords":
              if(splitCommand.length==3){
                String value = splitCommand[2].toLowerCase();
                Boolean setting;
                if(value.equals("true") || value.equals("t") || value.equals("1")){
                  setting = true;
                } else if (value.equals("false") || value.equals("f")|| value.equals("0")){
                  setting = false;
                } else {
                  sendLine("Invalid argument for display cell_coords: give either true or false");
                  return;
                }
                sendLine("Changing cell_coords setting");
                jsManager.saveSetting("show cell coords", setting);
                sendLine("cell_coords setting changed!");
              } else {
                sendLine("Invalid number of arguments for display cell_coords");
              }
              break;
            case "all_party_details":
              if(splitCommand.length==3){
                String value = splitCommand[2].toLowerCase();
                Boolean setting;
                if(value.equals("true") || value.equals("t") || value.equals("1")){
                  setting = true;
                } else if (value.equals("false") || value.equals("f")|| value.equals("0")){
                  setting = false;
                } else {
                  sendLine("Invalid argument for display all_party_details: give either true or false");
                  return;
                }
                sendLine("Changing all_party_details setting");
                jsManager.saveSetting("show all party managements", setting);
                sendLine("all_party_details setting changed!");
              } else {
                sendLine("Invalid number of arguments for display all_party_details");
              }
              break;
            case "party_id":
              if(splitCommand.length==3){
                String value = splitCommand[2].toLowerCase();
                Boolean setting;
                if(value.equals("true") || value.equals("t") || value.equals("1")){
                  setting = true;
                } else if (value.equals("false") || value.equals("f")|| value.equals("0")){
                  setting = false;
                } else {
                  sendLine("Invalid argument for display party_id: give either true or false");
                  return;
                }
                sendLine("Changing party_id setting");
                jsManager.saveSetting("show party id", setting);
                sendLine("party_id setting changed!");
              } else {
                sendLine("Invalid number of arguments for display party_id");
              }
              break;
            default:
              sendLine("Invalid argument for display");
              break;
          }
        } else {
          sendLine("Invalid number of arguments for display");
        }
        break;
      case "test":
        if(splitCommand.length>1){
          switch(splitCommand[1]){
            case "fog_of_war":
              if(splitCommand.length==3){
                String value = splitCommand[2].toLowerCase();
                Boolean setting;
                if(value.equals("true") || value.equals("t") || value.equals("1")){
                  setting = true;
                } else if (value.equals("false") || value.equals("f")|| value.equals("0")){
                  setting = false;
                } else {
                  sendLine("Invalid argument for test fog_of_war: give either true or false");
                  return;
                }
                sendLine("Changing fog_of_war setting");
                jsManager.saveSetting("fog of war", setting);
                sendLine("fog_of_war setting changed!");
              } else {
                sendLine("Invalid number of arguments for test fog_of_war");
              }
              break;
            default:
              sendLine("Invalid argument for test");
              break;
          }
        } else {
          sendLine("Invalid number of arguments for test");
        }
        break;
      default:
        sendLine("invalid command");
        break;
    }
  }
  
  ArrayList<String> _keyboardEvent(String eventType, char _key){
    keyboardEvent(eventType, _key);
    if (eventType == "keyTyped"){
      if (maxLength == -1 || this.toStr().length() < maxLength){
        if (_key == '\n'){
          //clearTextAt();
          text.add(text.size(), new StringBuilder(getInputString().substring(cursorX, getInputString().length())));
          getInputString().replace(cursorX, text.get(text.size()-1).length(), "");
          cursorX=3;
        }
        else if (_key == '\t'){
          for (int i=0; i<4-cursorX%4; i++){
            getInputString().insert(cursorX, " ");
          }
          cursorX += 4-cursorX%4;
        }
        else if(_key != 0 && (allowedChars == null || allowedChars.indexOf(_key) != -1)){
          //clearTextAt();
          getInputString().insert(cursorX, _key);
          cursorX++;
        }
      }
    }
    if (eventType == "keyPressed"){
      if (_key == CODED){
        if (keyCode == LEFT){
          cursorX = max(cursorX-1, 3);
        }
        if (keyCode == RIGHT){
          cursorX = min(cursorX+1, getInputString().length());
        }
        //if (keyCode == UP){
        //  cursorY = max(cursorY-1, 0);
        //  cursorX = min(cursorX, text.get(cursorY).length());
        //}
        //if(keyCode == DOWN){
        //  cursorY = min(cursorY+1, text.size()-1);
        //  cursorX = min(cursorX, text.get(cursorY).length());
        //}
        //if (keyCode == SHIFT){
        //  lshed = true;
        //}
      }
      if(_key == ENTER){
        String rawCommand = getInputString().substring(3);
        text.add(text.size(), new StringBuilder(" > "));
        doCommand(rawCommand);
        cursorX=3;
      }
      if(_key == VK_BACK_SPACE&&cursorX>3){
        clearTextAt();
        cursorX--;
      }
      if(keyCode == VK_DELETE&&cursorX<getInputString().length()){
        cursorX++;
        clearTextAt();
        cursorX--;
      }
    }
    //if (eventType == "keyReleased"){
    //  if(key == CODED){
    //    if (keyCode == SHIFT){
    //      lshed = false;
    //    }
    //  }
    //}
    return new ArrayList<String>();
  }
}
