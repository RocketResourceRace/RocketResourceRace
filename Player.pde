
class Player{
  float mapXOffset, mapYOffset, blockSize;
  float wood, food, energy, metal;
  float[] resources;
  // Resources: food wood metal energy
  Player(float mapXOffset, float mapYOffset, float blockSize, float[] resources){
    this.mapXOffset = mapXOffset;
    this.mapYOffset = mapYOffset;
    this.blockSize = blockSize;
    this.resources = resources;
  }
  void saveMapSettings(float mapXOffset, float mapYOffset, float blockSize){
    this.mapXOffset = mapXOffset;
    this.mapYOffset = mapYOffset;
    this.blockSize = blockSize;
  }
  void loadMapSettings(Map m){
    m.loadSettings(mapXOffset, mapYOffset, blockSize);
  }
}