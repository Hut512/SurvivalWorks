package de.survivalworkers.core.engine.io.keys;

/**
 * this interface is for every Class that wants to get informed for every mouse move regardless if the mouse is being dragged
 */
public interface MouseMoveListener {
    void mouseMove(double x,double y);
}
