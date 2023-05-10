package de.survivalworks;

import de.survivalworks.engine.Engine;

public class Main {
    public final static String NAME = "Test";
    public static Engine ENGINE;

    public static void main(String[] args) {
        ENGINE = new Engine(NAME);
        ENGINE.start();
    }
}