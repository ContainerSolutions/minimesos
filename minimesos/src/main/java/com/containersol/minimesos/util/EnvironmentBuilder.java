package com.containersol.minimesos.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * Provides convenient API for building a map of environment variables.
 * Produces the String[] format needed by CreateContainerCmd.setEnv()
 */
public class EnvironmentBuilder {

    private Map<String, String> envMap = new TreeMap<>();

    public static EnvironmentBuilder newEnvironment() {
        return new EnvironmentBuilder();
    }

    public EnvironmentBuilder withValue(String key, String value) {
        envMap.put(key, value);
        return this;
    }

    public EnvironmentBuilder withValues(Map<String, String> envMap) {
        this.envMap.putAll(envMap);
        return this;
    }

    public String[] createEnvironment() {
        return envMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }
}
