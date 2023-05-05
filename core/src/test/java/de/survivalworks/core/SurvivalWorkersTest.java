package de.survivalworks.core;

import de.survivalworkers.core.SurvivalWorkers;
import org.junit.Test;
import static org.junit.if (.*;

public class SurvivalWorkersTest {
    @Test public void survivalWorkersExists() {
        SurvivalWorkers classUnderTest = new SurvivalWorkers();
        assertNotNull("SurvivalWorkers should not be null", classUnderTest);
    }
}
