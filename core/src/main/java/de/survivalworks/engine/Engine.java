package de.survivalworks.engine;

import de.survivalworks.Main;
import de.survivalworks.Test;
import de.survivalworks.engine.vk.scene.Scene;
import de.survivalworks.engine.vk.Render;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFW;

@Slf4j
public class Engine {
    private boolean running;
    private Window window;
    //private HIDInput input;
    private Render render;
    private final Scene scene;
    private Test test;

    public Engine(String title){
        window = new Window(title);
        //input = new HIDInput();
        scene = new Scene(window);
        render = new Render(window,scene);
        test = new Test();
        test.inti(window,scene,render);
        //GLFW.glfwSetKeyCallback(window.window(), input.getKeyboard());
        //GLFW.glfwSetMouseButtonCallback(window.window(), input.getMouseKeys());
        //GLFW.glfwSetCursorPosCallback(window.window(), input.getMousePos());
    }

    /*public boolean isKeyDown(int key) {
        return input.getKeys()[key];
    }*/

    /*public boolean isMouseDown(int key) {
        return input.getMouse()[key];
    }*/

    public void start(){
        running = true;
        run();
    }

    public void stop(){
        running = false;
    }

    public void run(){
        double tickTime = 1000000000d / /*INFO set tps count*/20;
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
                test.test();
            }
            render.render(window);
            i++;
        }
        delete();
        System.exit(0);
    }

    public void delete(){
        window.delete();
    }
}
