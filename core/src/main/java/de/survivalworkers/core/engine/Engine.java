package de.survivalworkers.core.engine;

import de.survivalworkers.core.Window;
import de.survivalworkers.core.vk.Renderer;
import de.survivalworkers.core.engine.graphics.scene.Scene;
import de.survivalworkers.core.engine.io.HIDInput;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFW;

@Slf4j
public class Engine {
    private boolean running;
    private Window window;
    private HIDInput input;
    private Renderer render;
    private final Scene scene;

    public Engine(String title) {
        window = new Window(title);
        input = new HIDInput();
        scene = new Scene(window);
        render = new Renderer(window, scene);
        GLFW.glfwSetKeyCallback(window.getHandle(), input.getKeyboard());
        GLFW.glfwSetMouseButtonCallback(window.getHandle(), input.getMouseKeys());
        GLFW.glfwSetCursorPosCallback(window.getHandle(), input.getMousePos());
    }

    public boolean isKeyDown(int key) {
        return input.getKeys()[key];
    }

    public boolean isMouseDown(int key) {
        return input.getMouse()[key];
    }

    public void start(){
        running = true;
        run();
    }

    public void stop(){
        running = false;
    }

    public void run(){
        double tickTime = 1000000000d / /*Properties.getInstance().getTps()*/20;
        double delta = 0;
        long updateTime = System.nanoTime();
        int i = 0;
        long timeS = System.currentTimeMillis();
        while (running && !window.shouldClose()){
            window.pollEvents();
            long time = System.nanoTime();
            delta += (time - updateTime) / tickTime;
            if(timeS + 1000 <= System.currentTimeMillis()){
                log.info("FPS:" + i);
                timeS = System.currentTimeMillis();
                i = 0;
            }
            if(delta >= 1){
                long diffTime = time - updateTime;
                updateTime = time;
                --delta;
                /*test.test();*/
            }
            render.render(window);
            i++;
        }
        close();
        System.exit(0);
    }

    public void close() {
        window.close();
    }

    public HIDInput getInput() {
        return input;
    }
}
