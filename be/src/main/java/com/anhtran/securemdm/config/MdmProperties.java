package com.anhtran.securemdm.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings bound from {@code mdm.*} (also MDM_* environment variables). */
@ConfigurationProperties("mdm")
public record MdmProperties(
    String accessKey,
    List<String> commandKeys,
    String commandActiveKid,
    String deviceCaKey,
    @DefaultValue("true") boolean secureCookies,
    long commandTtlMs,
    long leaseMs,
    int maxAttempts,
    long offlineMs,
    String demoPassword) {}
