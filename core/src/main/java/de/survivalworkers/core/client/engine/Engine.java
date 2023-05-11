package de.survivalworkers.core.client.engine;

import de.survivalworkers.core.Test;
import de.survivalworkers.core.client.engine.io.keys.HIDInput;
import de.survivalworkers.core.client.engine.vk.Render;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFW;

@Slf4j
public class Engine {
    private boolean running;
    @Getter
    private Window window;
    private Render render;
    @Getter
    private final Scene scene;
    private final HIDInput input;

    public Engine(HIDInput input, Test test) {
        this.input = input;
        window = new Window();
        scene = new Scene(window);
        render = new Render(window, scene);
        test.inti(scene,render);
        GLFW.glfwSetKeyCallback(window.window(), input.getKeyboard());
        GLFW.glfwSetMouseButtonCallback(window.window(), input.getMouseKeys());
        GLFW.glfwSetCursorPosCallback(window.window(), input.getMousePos());
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
                input.invokeContinuous();
                /*test.test();*/
            }
            render.render(window);
            i++;
        }
        close();
        System.exit(0);
    }

    public void close() {
        window.delete();
    }
}
