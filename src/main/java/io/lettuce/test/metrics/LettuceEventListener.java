package io.lettuce.test.metrics;

import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.event.connection.ReconnectAttemptEvent;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

/**
 * Listens to Lettuce connection events via the EventBus and records metrics for connection lifecycle events.
 */
@Component
public class LettuceEventListener {

    private static final Logger log = LoggerFactory.getLogger(LettuceEventListener.class);

    private final ClientResources clientResources;

    private final MeterRegistry meterRegistry;

    private Disposable subscription;

    // Counters for connection events
    private final Counter connectedCounter;

    private final Counter disconnectedCounter;

    private final Counter connectionActivatedCounter;

    private final Counter connectionDeactivatedCounter;

    private final Counter reconnectAttemptCounter;

    private final Counter reconnectFailureCounter;

    public LettuceEventListener(ClientResources clientResources, MeterRegistry meterRegistry) {
        this.clientResources = clientResources;
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.connectedCounter = Counter.builder("redis.connection.events").tag("type", "connected")
                .description("Redis connection events").register(meterRegistry);

        this.disconnectedCounter = Counter.builder("redis.connection.events").tag("type", "disconnected")
                .description("Redis connection events").register(meterRegistry);

        this.connectionActivatedCounter = Counter.builder("redis.connection.events").tag("type", "activated")
                .description("Redis connection events").register(meterRegistry);

        this.connectionDeactivatedCounter = Counter.builder("redis.connection.events").tag("type", "deactivated")
                .description("Redis connection events").register(meterRegistry);

        this.reconnectAttemptCounter = Counter.builder("redis.reconnect.attempts").description("Redis reconnection attempts")
                .register(meterRegistry);

        this.reconnectFailureCounter = Counter.builder("redis.reconnect.failures").description("Redis reconnection failures")
                .register(meterRegistry);

        log.info("LettuceEventListener initialized");
    }

    @PostConstruct
    public void startListening() {
        EventBus eventBus = clientResources.eventBus();

        subscription = eventBus.get().subscribe(this::handleEvent, error -> log.error("Error in event subscription", error),
                () -> log.info("Event subscription completed"));

        log.info("Started listening to Lettuce events");
    }

    private void handleEvent(Event event) {
        if (event instanceof ConnectedEvent connectedEvent) {
            connectedCounter.increment();
            log.debug("Connected: local={}, remote={}", connectedEvent.localAddress(), connectedEvent.remoteAddress());
        } else if (event instanceof DisconnectedEvent disconnectedEvent) {
            disconnectedCounter.increment();
            log.debug("Disconnected: local={}, remote={}", disconnectedEvent.localAddress(), disconnectedEvent.remoteAddress());
        } else if (event instanceof ConnectionActivatedEvent activatedEvent) {
            connectionActivatedCounter.increment();
            log.debug("Connection activated: local={}, remote={}", activatedEvent.localAddress(),
                    activatedEvent.remoteAddress());
        } else if (event instanceof ConnectionDeactivatedEvent deactivatedEvent) {
            connectionDeactivatedCounter.increment();
            log.debug("Connection deactivated: local={}, remote={}", deactivatedEvent.localAddress(),
                    deactivatedEvent.remoteAddress());
        } else if (event instanceof ReconnectAttemptEvent reconnectEvent) {
            reconnectAttemptCounter.increment();
            log.debug("Reconnect attempt #{}: remote={}", reconnectEvent.getAttempt(), reconnectEvent.remoteAddress());
        } else if (event instanceof ReconnectFailedEvent failedEvent) {
            reconnectFailureCounter.increment();
            log.warn("Reconnect failed: remote={}, cause={}", failedEvent.remoteAddress(),
                    failedEvent.getCause() != null ? failedEvent.getCause().getMessage() : "unknown");
        }
    }

    @PreDestroy
    public void stopListening() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Stopped listening to Lettuce events");
        }
    }

}
