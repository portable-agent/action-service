package dev.portableagent.action.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("temporal")
public record TemporalProperties(String target, String namespace, String taskQueue) {}
