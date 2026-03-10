package io.lettuce.test.metrics.eventbus;

import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * EventBus subscriber for when Reactor IS on the classpath.
 * <p>
 * Handles both:
 * <ul>
 * <li>reactor-optional fork: uses {@code eventBus.subscribe(Consumer)}</li>
 * <li>standard Lettuce: uses {@code eventBus.get().subscribe(Consumer)}</li>
 * </ul>
 */
public class ReactiveEventBusSubscriber implements EventBusSubscriber {

    private static final Logger log = LoggerFactory.getLogger(ReactiveEventBusSubscriber.class);

    @Override
    public Closeable subscribe(EventBus eventBus, Consumer<Event> eventHandler) {
        // Try reactor-optional API first: direct subscribe(Consumer)
        try {
            Method subscribeMethod = eventBus.getClass().getMethod("subscribe", Consumer.class);
            log.debug("Using reactor-optional EventBus API (direct subscribe)");
            return (Closeable) subscribeMethod.invoke(eventBus, eventHandler);
        } catch (NoSuchMethodException e) {
            // Fall back to standard Lettuce: get() returns Flux
            log.debug("Using standard Lettuce EventBus API (Flux subscribe)");
            return subscribeViaFlux(eventBus, eventHandler);
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe to EventBus", e);
        }
    }

    private Closeable subscribeViaFlux(EventBus eventBus, Consumer<Event> eventHandler) {
        try {
            // Standard Lettuce: eventBus.get() returns Flux<Event>
            Method getMethod = eventBus.getClass().getMethod("get");
            Object flux = getMethod.invoke(eventBus);
            Method subscribeMethod = flux.getClass().getMethod("subscribe", Consumer.class);
            Object disposable = subscribeMethod.invoke(flux, eventHandler);
            // Use reflection to call dispose() to avoid compile-time dependency on Reactor
            Class<?> disposableClass = Class.forName("reactor.core.Disposable");
            Method disposeMethod = disposableClass.getMethod("dispose");
            return () -> {
                try {
                    disposeMethod.invoke(disposable);
                } catch (Exception e) {
                    log.warn("Error disposing subscription", e);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe via Flux API", e);
        }
    }

}
