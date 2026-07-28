package com.gwlite.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics (fixes drawback #6). Micrometer auto-forwards
 * these to whatever registry is configured - in production that's the
 * Stackdriver (GCP Cloud Monitoring) registry, configured via
 * management.metrics.export.stackdriver.* properties.
 *
 * These counters answer questions Actuator's defaults can't: "how many
 * document edits per minute," "how many rate-limit rejections," etc -
 * the metrics that actually matter for THIS app, not just JVM health.
 */
@Component
public class AppMetrics {

    private final Counter documentEditsCounter;
    private final Counter rateLimitRejectionsCounter;
    private final Counter websocketConnectionsCounter;

    public AppMetrics(MeterRegistry registry) {
        this.documentEditsCounter = Counter.builder("gwlite.document.edits")
                .description("Total number of document edit operations processed")
                .register(registry);

        this.rateLimitRejectionsCounter = Counter.builder("gwlite.ratelimit.rejections")
                .description("Total number of requests rejected for exceeding rate limits")
                .register(registry);

        this.websocketConnectionsCounter = Counter.builder("gwlite.websocket.connections")
                .description("Total WebSocket connections established")
                .register(registry);
    }

    public void recordDocumentEdit() { documentEditsCounter.increment(); }
    public void recordRateLimitRejection() { rateLimitRejectionsCounter.increment(); }
    public void recordWebSocketConnection() { websocketConnectionsCounter.increment(); }
}
