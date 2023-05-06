package de.survivalworks.api.common.event;

import lombok.Getter;

public enum EventPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4);

    @Getter
    private final int slot;

    EventPriority(int slot) {
        this.slot = slot;
    }
}
