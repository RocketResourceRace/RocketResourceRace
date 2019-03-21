package event;

public class ChangeEquipment extends GameEvent{
  public int equipmentClass;
  public int newEquipmentType;
  public ChangeEquipment(int equipmentClass, int newEqupmentType){
    this.equipmentClass = equipmentClass;
    this.newEquipmentType = newEqupmentType;
  }
}
