package de.survivalworkers.core.common.event;

import de.survivalworks.api.common.event.Event;
import de.survivalworks.api.common.event.EventListener;
import de.survivalworks.api.common.event.EventPriority;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Getter
public class EventHandlerMethod {
    private final EventListener listener;
    private final Method method;
    private final EventPriority priority;
    private final boolean ignoreCancelled;

    public EventHandlerMethod(EventListener listener, Method method, EventPriority priority, boolean ignoreCancelled) {
        this.listener = listener;
        this.method = method;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
    }

    public void invoke(Event event) throws InvocationTargetException, IllegalAccessException {
        method.invoke(listener, event);
    }
}
