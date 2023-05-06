package de.survivalworkers.core.engine.io.keys;

import lombok.Getter;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This class is responsible for every input that is physically given through a HID<br>
 * Keys that are to be used have to be registered in {@link #registerKeys()}<br>
 * Key listeners to be used have to implement {@link KeyListener} with every method using the {@link KeyHandler} annotation and registered using {@link #registerKeyListener(KeyListener)}<br>
 * Key drags to be used have to implement {@link MouseDragListener} with every method using the {@link DragHandler} annotation and registered using
 * {@link #registerDragListener(MouseDragListener)}<br>
 * Mouse move listeners to be used have to implement {@link MouseMoveListener} and registered using {@link #registerMoveListener(MouseMoveListener)}
 */
public class HIDInput {
    private final Map<String, Map<Method,KeyListener>> listeners;
    private final Map<String, Map<Method,MouseDragListener>> dragListeners;
    private final Map<Integer,String> defaultKeys;
    private final List<MouseMoveListener> mouseMove;
    @Getter
    private final boolean[] keys = new boolean[GLFW_KEY_LAST];
    private double mouseX, mouseY;
    @Getter
    private final GLFWKeyCallback keyboard;
    @Getter
    private final GLFWCursorPosCallback mousePos;
    @Getter
    private final GLFWMouseButtonCallback mouseKeys;
    @Getter
    private final KeyConfig keyConfig;

    /**
     * Adds a {@link KeyListener} to be called when the specific key was pressed or released
     * @param listener The listener class to be used<br> Every method being registered has to be annotated with {@link  KeyHandler} annotation
     * @see KeyHandler
     */
    private void registerKeyListener(KeyListener listener){
        for (Method method : listener.getClass().getMethods()) {
            if(!method.isAnnotationPresent(KeyHandler.class))continue;
            if(method.getParameters().length != 1 || !method.getParameters()[0].getType().equals(boolean.class))
                throw new IllegalArgumentException(method.getName() + " does not have exactly one parameter of type boolean");
            String name = method.getAnnotation(KeyHandler.class).value();
            boolean contains = false;
            for (String value : defaultKeys.values()) {
                if(value.equals(name)){
                    contains = true;
                    break;
                }
            }
            if(!contains)throw new RuntimeException("The key " + name + " has not been registered");
            if(!listeners.containsKey(name)) listeners.put(name , new HashMap<>());
            listeners.get(name).put(method,listener);
        }
    }
    
    /**
     * @param listener the listener that will be notified when the mouse has been moved
     */
    public void registerMoveListener(MouseMoveListener listener){
        mouseMove.add(listener);
    }

    public void registerDragListener(MouseDragListener listener){
        for (Method method : listener.getClass().getMethods()) {
            if(!method.isAnnotationPresent(DragHandler.class))continue;
            if(method.getParameters().length != 4 || !method.getParameters()[0].getType().equals(double.class) || !method.getParameters()[1].getType().equals(double.class) ||
                    !method.getParameters()[2].getType().equals(double.class) || !method.getParameters()[3].getType().equals(double.class))
                throw new IllegalArgumentException(method.getName() + " does not have exactly four parameter of type double");
            String name = method.getAnnotation(DragHandler.class).value();
            boolean contains = false;
            for (String value : defaultKeys.values()) {
                if(value.equals(name)){
                    contains = true;
                    break;
                }
            }
            if(!contains)throw new RuntimeException("The key " + name + " has not been registered");
            if(!dragListeners.containsKey(name)) dragListeners.put(name , new HashMap<>());
            dragListeners.get(name).put(method,listener);
        }
    }

    /**
     * Removes a {@link KeyListener} from the callback
     * @param listener The {@link KeyListener} to be used
     * @see #registerKey(int, String) 
     */
    public void removeKeyListener(KeyListener listener){
        for (Method method : listener.getClass().getMethods()) {
            if(!method.isAnnotationPresent(KeyHandler.class) || method.getParameters().length != 1 || !method.getParameters()[0].getType().equals(boolean.class))continue;
            String name = method.getAnnotation(KeyHandler.class).value();
            if(!listeners.containsKey(name))continue;
            listeners.get(name).remove(method);
        }
    }


    /**
     * This method is used to define a key which can then be used by {@link KeyListener}
     * @param defaultKey the default Key bind
     * @param name The name under which this key is referenced
     */
    private void registerKey(int defaultKey,String name){
        if(defaultKeys.containsKey(defaultKey) && defaultKeys.get(defaultKey).equals(name))
            throw new IllegalArgumentException("The key: " + ((char) defaultKey) + " already is defined as " + defaultKeys.get(defaultKey));
        defaultKeys.put(defaultKey,name);
    }

    public HIDInput(){
        defaultKeys = new HashMap<>();
        listeners = new HashMap<>();
        mouseMove = new ArrayList<>();
        dragListeners = new HashMap<>();
        registerKeys();
        registerListeners();
        keyConfig = new KeyConfig(defaultKeys);
        keyboard = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (keys[key] == (action != GLFW_RELEASE)) return;
                if (keys[key]){
                    if(!listeners.containsKey(keyConfig.get(key)))return;
                    listeners.get(keyConfig.get(key)).keySet().forEach(method -> {
                        try {
                            method.invoke(listeners.get(keyConfig.get(key)).get(method), false);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    });
                }else {
                    if(!listeners.containsKey(keyConfig.get(key)))return;
                    listeners.get(keyConfig.get(key)).keySet().forEach(method -> {
                        try {
                            method.invoke(listeners.get(keyConfig.get(key)).get(method), true);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    });
                }
                keys[key] = (action != GLFW_RELEASE);
            }
        };

        mousePos = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xPos, double yPos) {
                dragListeners.forEach((name,map) -> map.forEach(((method, listener) -> {
                    try {
                        if(keys[keyConfig.get(name)])
                            method.invoke(listener,xPos - mouseX,yPos - mouseY,xPos,yPos);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                })));
                mouseMove.forEach(listeners -> listeners.mouseMove(xPos,yPos));
                mouseX = xPos;
                mouseY = yPos;
            }
        };

        mouseKeys = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int key, int action, int mods) {
                if (keys[key] == (action != GLFW_RELEASE)) return;
                if (keys[key]){
                    if(!listeners.containsKey(keyConfig.get(key)))return;
                    listeners.get(keyConfig.get(key)).keySet().forEach(method -> {
                        try {
                            method.invoke(listeners.get(keyConfig.get(key)).get(method), false);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    });
                }else {
                    if(!listeners.containsKey(keyConfig.get(key)))return;
                    listeners.get(keyConfig.get(key)).keySet().forEach(method -> {
                        try {
                            method.invoke(listeners.get(keyConfig.get(key)).get(method), true);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    });
                }
                keys[key] = (action != GLFW_RELEASE);
            }
        };
    }

    /**
     * called when the {@link HIDInput} is created<br> should be used to register a key for usage by a {@link KeyListener}
     */
    private void registerKeys() {
    }

    /**
     * called when the {@link HIDInput} is created<br> should be used to {@link #registerKeyListener(KeyListener)} all Key Listeners
     */
    private void registerListeners() {
    }

    public void close() {
        keyboard.free();
        mousePos.free();
        mouseKeys.free();
    }
}
