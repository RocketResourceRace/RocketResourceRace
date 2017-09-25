
class Event{
  String id, type;
  Event(String id, String type){
    this.id = id;
    this.type = type;
  }
  String info(){
    return "id:"+id+", type:"+type;
  }
}