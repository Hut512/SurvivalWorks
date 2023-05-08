package de.survivalworkers.core;

import de.survivalworkers.core.client.engine.Engine;
import de.survivalworkers.core.client.engine.io.keys.HIDInput;
import de.survivalworkers.core.client.util.ClientOptions;
import de.survivalworkers.core.common.event.EventManager;
import lombok.Getter;

public class SurvivalWorkers {
    @Getter
    private final EventManager eventManager;
    @Getter
    private final ClientOptions clientOptions;
    @Getter
    private final HIDInput input;
    @Getter
    private final Engine engine;

    public SurvivalWorkers() {
        eventManager = new EventManager();
        clientOptions = new ClientOptions();
        input = new HIDInput();
        engine = new Engine(input);
        engine.start();
    }

    public static void main(String[] args) {
        new SurvivalWorkers();
    }
}
