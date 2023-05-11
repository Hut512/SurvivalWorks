package de.survivalworks.core;

import de.survivalworkers.core.SurvivalWorkers;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SurvivalWorkersTest {
    @Test public void survivalWorkersExists() {
        SurvivalWorkers classUnderTest = new SurvivalWorkers();
        assertNotNull("SurvivalWorkers should not be null", classUnderTest);
    }
}
