package de.survivalworkers.core.common.event;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.survivalworks.api.common.event.Event;
import de.survivalworks.api.common.event.EventHandler;
import de.survivalworks.api.common.event.EventListener;
import de.survivalworks.api.common.event.EventPriority;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Slf4j
public class EventManager {
    private final Multimap<Class<? extends Event>, EventHandlerMethod> eventHandlerMethods = Multimaps.synchronizedMultimap(
            Multimaps.newMultimap(new HashMap<>(), () -> new PriorityQueue<>((l1, l2) -> l2.getPriority().getSlot() - l1.getPriority().getSlot())));

    public void callEvent(Event event) {
        for (EventHandlerMethod method : eventHandlerMethods.get(event.getClass())) {
            if (event.isCancelled() && !method.isIgnoreCancelled())
                continue;

            long start = System.currentTimeMillis();

            try {
                method.invoke(event);
            } catch (IllegalAccessException e) {
                log.warn("Event handler method became inaccessible", e);
            } catch (InvocationTargetException e) {
                log.warn("Error dispatching event " + event + " to listener " + method.getListener(), e);
            }

            long passed = System.currentTimeMillis() - start;

            if (passed > 100) {
                log.warn("Listener {} took {}ms to process event {}", method.getListener(), passed, event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void registerEventListener(EventListener listener) {
        Arrays.stream(listener.getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(EventHandler.class))
                .forEach(m -> {
                    if (m.getParameterTypes().length != 1 || Event.class.isAssignableFrom(m.getParameterTypes()[0])) {
                        log.info("Method {} in class {} has illegal arguments", m, listener.getClass());
                        return;
                    }

                    EventPriority priority = m.getAnnotation(EventHandler.class).priority();
                    boolean ignoreCancelled = m.getAnnotation(EventHandler.class).ignoreCancelled();

                    eventHandlerMethods.put((Class<? extends Event>) m.getParameterTypes()[0], new EventHandlerMethod(listener, m, priority, ignoreCancelled));
                });
    }

    public void unregisterEventListener(EventListener listener) {
        eventHandlerMethods.values().removeIf(m -> m.getListener() == listener);
    }
}
