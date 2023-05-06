package de.survivalworkers.core;

import de.survivalworkers.core.client.SWWindow;
import de.survivalworkers.core.client.util.ClientOptions;
import de.survivalworkers.core.common.event.EventManager;
import lombok.Getter;

public class SurvivalWorkers {
    @Getter
    private static SurvivalWorkers instance;

    @Getter
    private final EventManager eventManager;
    @Getter
    private final ClientOptions clientOptions;
    @Getter
    private final SWWindow window;

    public SurvivalWorkers() {
        SurvivalWorkers.instance = this;
        eventManager = new EventManager();
        clientOptions = new ClientOptions();
        window = new SWWindow();
    }

    public void start() {
        while (!window.shouldClose()) {
            window.pollEvents();

        }
    }

    public static void main(String[] args) {
        new SurvivalWorkers().start();
    }
}
