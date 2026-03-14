package com.yachaerang.batch.configuration;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.PushGateway;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

@Slf4j
@Configuration
@EnableScheduling
public class PrometheusPushGatewayConfig {

    private final PrometheusMeterRegistry meterRegistry;

    @Value("${prometheus.job.name}")
    private String jobName;

    @Value("${prometheus.grouping.key}")
    private String groupingKeyName;

    @Value("${prometheus.pushgateway.url}")
    private String pushGatewayUrl;

    private PushGateway pushGateway;
    private Map<String, String> groupingKey;

    public PrometheusPushGatewayConfig(PrometheusMeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        pushGateway = new PushGateway(pushGatewayUrl);
        groupingKey = Map.of(groupingKeyName, jobName);
    }

    @Scheduled(fixedRateString = "${prometheus.push.rate}")
    public void pushMetrics() {
        try {

            pushGateway.pushAdd(
                    meterRegistry.getPrometheusRegistry(),
                    jobName,
                    groupingKey
            );

        } catch (Exception ex) {
            log.error("Failed to push metrics to PushGateway", ex);
        }
    }
}