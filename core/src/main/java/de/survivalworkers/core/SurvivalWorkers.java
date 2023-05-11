package de.survivalworkers.core;

import de.survivalworkers.core.client.engine.Engine;
import de.survivalworkers.core.client.engine.io.keys.HIDInput;
import de.survivalworkers.core.common.event.EventManager;
import lombok.Getter;

public class SurvivalWorkers {
    @Getter
    public static SurvivalWorkers instance;
    @Getter
    private final Engine engine;
    private final HIDInput input;
    private final EventManager eventManager;

    public SurvivalWorkers(){
        instance = this;
        input = new HIDInput();
        engine = new Engine(input,new Test());
        eventManager = new EventManager();
        engine.start();
    }

    public static void main(String[] args) {
        new SurvivalWorkers();
    }
}