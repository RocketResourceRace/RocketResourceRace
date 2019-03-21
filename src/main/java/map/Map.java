package map;

import player.Player;
import processing.core.PVector;
import util.Cell;
import util.Node;

import java.util.ArrayList;

public interface Map {
    void updateMoveNodes(Node[][] nodes, Player[] players);
    void cancelMoveNodes();
    void removeTreeTile(int cellX, int cellY);
    void setDrawingTaskIcons(boolean v);
    void setDrawingUnitBars(boolean v);
    void setHeightsForCell(int x, int y, float h);
    void replaceMapStripWithReloadedStrip(int y);
    boolean isPanning();
    float getFocusedX();
    float getFocusedY();
    boolean isZooming();
    float getTargetZoom();
    float getZoom();
    float getTargetOffsetX();
    float getTargetOffsetY();
    float getTargetBlockSize();
    float[] targetCell(int x, int y, float zoom);
    void loadSettings(float x, float y, float bs);
    void unselectCell();
    boolean mouseOver();
    Node[][] getMoveNodes();
    float scaleXInv();
    float scaleYInv();
    void updatePath(ArrayList<int[]> nodes);
    void updateHoveringScale();
    void doUpdateHoveringScale();
    void cancelPath();
    void setActive(boolean a);
    void selectCell(int x, int y);
    void generateShape();
    void clearShape();
    boolean isMoving();
    void enableRocket(PVector pos, PVector vel);
    void disableRocket();
    void enableBombard(int range);
    void disableBombard();
    void setPlayerColours(int[] playerColours);
    void updateVisibleCells(Cell[][] visibleCells);
}
