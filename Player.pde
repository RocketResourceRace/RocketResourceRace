
class Player{
  float mapXOffset, mapYOffset, blockSize;
  float wood, food, energy, metal;
  float[] resources;
  int cellX, cellY;
  boolean cellSelected = false;
  // Resources: food wood metal energy concrete cable spaceship_parts ore people
  Player(float mapXOffset, float mapYOffset, float blockSize, float[] resources){
    this.mapXOffset = mapXOffset;
    this.mapYOffset = mapYOffset;
    this.blockSize = blockSize;
    this.resources = resources; 
  }
  void saveSettings(float mapXOffset, float mapYOffset, float blockSize, int cellX, int cellY, boolean cellSelected){
    this.mapXOffset = mapXOffset;
    this.mapYOffset = mapYOffset;
    this.blockSize = blockSize;
    this.cellX = cellX;
    this.cellY = cellY;
    this.cellSelected = cellSelected;
  }
  void loadSettings(Game g, Map m){
    m.loadSettings(mapXOffset, mapYOffset, blockSize);
    if(cellSelected){
      g.selectCell((int)m.scaleX(cellX+1), (int)m.scaleY(cellY+1));
    }
  }
}