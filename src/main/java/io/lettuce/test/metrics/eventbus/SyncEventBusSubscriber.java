package io.lettuce.test.metrics.eventbus;

import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * EventBus subscriber using the sync API from the reactor-optional fork.
 * <p>
 * Uses reflection to call {@code eventBus.subscribe(Consumer)} so this class compiles with both standard and reactor-optional
 * Lettuce. The factory ensures this is only instantiated when the method exists.
 */
public class SyncEventBusSubscriber implements EventBusSubscriber {

    private static final Logger log = LoggerFactory.getLogger(SyncEventBusSubscriber.class);

    @Override
    public Closeable subscribe(EventBus eventBus, Consumer<Event> eventHandler) {
        try {
            Method subscribeMethod = eventBus.getClass().getMethod("subscribe", Consumer.class);
            log.debug("Using sync EventBus API (direct subscribe)");
            return (Closeable) subscribeMethod.invoke(eventBus, eventHandler);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("EventBus.subscribe(Consumer) not found. Requires reactor-optional Lettuce fork.",
                    e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe to EventBus", e);
        }
    }

}
