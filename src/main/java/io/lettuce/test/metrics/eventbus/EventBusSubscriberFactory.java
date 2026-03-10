package io.lettuce.test.metrics.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory that lazily provides the appropriate EventBusSubscriber based on:
 * <ul>
 * <li>Classpath: whether EventBus has subscribe(Consumer) method (reactor-optional fork)</li>
 * <li>Classpath: whether Reactor is available</li>
 * </ul>
 *
 * Scenarios:
 * <ul>
 * <li>reactor-optional + sync: SyncEventBusSubscriber (direct subscribe API)</li>
 * <li>reactor-optional + reactive: ReactiveEventBusSubscriber</li>
 * <li>standard + sync: ReactiveEventBusSubscriber (standard Lettuce needs Flux API)</li>
 * <li>standard + reactive: ReactiveEventBusSubscriber</li>
 * </ul>
 */
@Component
public class EventBusSubscriberFactory {

    private static final Logger log = LoggerFactory.getLogger(EventBusSubscriberFactory.class);

    private volatile EventBusSubscriber instance;

    public EventBusSubscriber getSubscriber() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = createSubscriber();
                }
            }
        }
        return instance;
    }

    private EventBusSubscriber createSubscriber() {
        boolean hasSyncSubscribeApi = hasSyncSubscribeApi();
        boolean reactorAvailable = isReactorAvailable();

        if (!hasSyncSubscribeApi) {
            // Standard Lettuce: must use Flux-based API
            log.info("Creating ReactiveEventBusSubscriber (standard Lettuce)");
            return new ReactiveEventBusSubscriber();
        }

        // reactor-optional fork has sync subscribe API
        if (reactorAvailable) {
            log.info("Creating ReactiveEventBusSubscriber (reactor-optional + Reactor available)");
            return new ReactiveEventBusSubscriber();
        } else {
            log.info("Creating SyncEventBusSubscriber (reactor-optional, no Reactor)");
            return new SyncEventBusSubscriber();
        }
    }

    private boolean hasSyncSubscribeApi() {
        // Check if EventBus has subscribe(Consumer) method (reactor-optional fork)
        try {
            Class<?> eventBusClass = Class.forName("io.lettuce.core.event.EventBus");
            eventBusClass.getMethod("subscribe", java.util.function.Consumer.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("EventBus class not found", e);
        }
    }

    private boolean isReactorAvailable() {
        try {
            Class.forName("reactor.core.publisher.Flux");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
