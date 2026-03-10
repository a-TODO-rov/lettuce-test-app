package io.lettuce.test.metrics.eventbus;

import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Strategy interface for subscribing to Lettuce EventBus.
 * <p>
 * Different implementations handle reactor-optional vs standard Lettuce APIs:
 * <ul>
 * <li>{@code SyncEventBusSubscriber} - uses direct subscribe(Consumer) API</li>
 * <li>{@code ReactiveEventBusSubscriber} - uses Reactor-based get().subscribe() API</li>
 * </ul>
 */
public interface EventBusSubscriber {

    /**
     * Subscribe to events from the EventBus.
     *
     * @param eventBus the Lettuce EventBus
     * @param eventHandler the handler to process events
     * @return a Closeable to unsubscribe
     */
    Closeable subscribe(EventBus eventBus, Consumer<Event> eventHandler);

}
