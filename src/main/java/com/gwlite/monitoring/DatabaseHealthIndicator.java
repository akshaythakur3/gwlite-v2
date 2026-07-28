package com.gwlite.monitoring;

import com.gwlite.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health check exposed at /actuator/health (fixes drawback #6).
 * GCP Cloud Monitoring (or any external monitor) can poll this endpoint
 * to detect if the app can actually reach its database, not just whether
 * the JVM process is alive.
 */
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DocumentRepository documentRepository;

    @Override
    public Health health() {
        try {
            documentRepository.count(); // cheap query to confirm DB connectivity
            return Health.up().withDetail("database", "reachable").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("database", "unreachable").build();
        }
    }
}
