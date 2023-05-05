package de.survivalworkers.core.engine.io;

import lombok.Getter;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import static org.lwjgl.glfw.GLFW.*;

public class HIDInput {
    @Getter
    private final boolean[] keys = new boolean[GLFW_KEY_LAST];
    @Getter
    private final boolean[] mouse = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private double mouseX, mouseY;
    @Getter
    private final GLFWKeyCallback keyboard;
    @Getter
    private final GLFWCursorPosCallback mousePos;
    @Getter
    private final GLFWMouseButtonCallback mouseKeys;

    public HIDInput(){
        keyboard = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                keys[key] = (action != GLFW_RELEASE);
            }
        };

        mousePos = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xPos, double yPos) {
                mouseX = xPos;
                mouseY = yPos;
            }
        };

        mouseKeys = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                mouse[button] = (action != GLFW_RELEASE);
            }
        };
    }

    public void close() {
        keyboard.free();
        mousePos.free();
        mouseKeys.free();
    }
}
