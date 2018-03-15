
class Event{
  String id, type, panel;
  Event(String id, String panel, String type){
    this.id = id;
    this.type = type;
  }
  String info(){
    return "id:"+id+", type:"+type;
  }
}
