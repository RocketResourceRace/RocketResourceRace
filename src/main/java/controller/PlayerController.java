package controller;


import event.GameEvent;
import util.Cell;

public interface PlayerController {
    GameEvent generateNextEvent(Cell[][] visibleCells, float[] resources);
}