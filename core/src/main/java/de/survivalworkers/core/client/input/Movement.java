package de.survivalworkers.core.client.input;

import de.survivalworkers.core.SurvivalWorkers;
import de.survivalworkers.core.client.engine.io.keys.KeyHandler;
import de.survivalworkers.core.client.engine.io.keys.KeyListener;
import de.survivalworkers.core.client.engine.io.keys.MouseMoveListener;
import org.lwjgl.glfw.GLFW;

public class Movement implements KeyListener, MouseMoveListener {
    @Override
    public void mouseMove(double x, double y) {
        SurvivalWorkers.getInstance().getEngine().getScene().getCamera().addRotation((float) Math.toRadians((y - SurvivalWorkers.getInstance().getEngine().getWindow().getHeight() / 2) * 0.1f),
                (float) Math.toRadians((x - SurvivalWorkers.getInstance().getEngine().getWindow().getWidth() / 2) * 0.1f));
        GLFW.glfwSetInputMode(SurvivalWorkers.getInstance().getEngine().getWindow().window(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(SurvivalWorkers.getInstance().getEngine().getWindow().window(), SurvivalWorkers.getInstance().getEngine().getWindow().getWidth() / 2, SurvivalWorkers.getInstance().getEngine().getWindow().getHeight() / 2);
    }

    @KeyHandler("forward")
    public void forward(boolean pressed){
        if(pressed) SurvivalWorkers.getInstance().getEngine().getScene().getCamera().moveForward(0.5f);
    }

    @KeyHandler("left")
    public void left(boolean pressed){
        if(pressed) SurvivalWorkers.getInstance().getEngine().getScene().getCamera().moveLeft(0.5f);
    }

    @KeyHandler("back")
    public void back(boolean pressed){
        if(pressed) SurvivalWorkers.getInstance().getEngine().getScene().getCamera().moveBack(0.5f);
    }

    @KeyHandler("right")
    public void right(boolean pressed){
        if(pressed) SurvivalWorkers.getInstance().getEngine().getScene().getCamera().moveRight(0.5f);
    }

    @KeyHandler("reset")
    public void reset(boolean pressed){
        if(pressed) SurvivalWorkers.getInstance().getEngine().getScene().getCamera().setPos(0.0f,0.0f,0.0f);
    }
}
