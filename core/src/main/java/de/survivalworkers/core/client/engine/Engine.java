package de.survivalworkers.core.client.engine;

import de.survivalworkers.core.client.SWWindow;
import de.survivalworkers.core.client.engine.io.keys.HIDInput;
import de.survivalworkers.core.client.engine.vk.Renderer;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFW;

@Slf4j
public class Engine {
    private boolean running;
    private SWWindow window;
    private HIDInput input;
    private Renderer render;
    private final Scene scene;

    public Engine() {
        window = new SWWindow();
        input = new HIDInput();
        scene = new Scene(window);
        render = new Renderer(window, scene);
        GLFW.glfwSetKeyCallback(window.getHandle(), input.getKeyboard());
        GLFW.glfwSetMouseButtonCallback(window.getHandle(), input.getMouseKeys());
        GLFW.glfwSetCursorPosCallback(window.getHandle(), input.getMousePos());
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
