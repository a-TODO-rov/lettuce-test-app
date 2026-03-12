package io.lettuce.test;

import java.time.Duration;

public interface WorkloadOptions {

    <T> T getOption(String key, Class<T> type);

    <T> T getOption(String key, Class<T> type, T defaultValue);

    default Duration getDuration(String key) {
        return getOption(key, Duration.class);
    }

    default Duration getDuration(String key, Duration defaultValue) {
        return getOption(key, Duration.class, defaultValue);
    }

    default Boolean getBoolean(String key) {
        return getOption(key, Boolean.class);
    }

    default Boolean getBoolean(String key, Boolean defaultValue) {
        return getOption(key, Boolean.class, defaultValue);
    }

    default Integer getInteger(String key) {
        return getOption(key, Integer.class);
    }

    default Integer getInteger(String key, Integer defaultValue) {
        return getOption(key, Integer.class, defaultValue);
    }

    default Double getDouble(String key) {
        return getOption(key, Double.class);
    }

    default Double getDouble(String key, Double defaultValue) {
        return getOption(key, Double.class, defaultValue);
    }

    default String getString(String key) {
        return getOption(key, String.class);
    }

    default String getString(String key, String defaultValue) {
        return getOption(key, String.class, defaultValue);
    }

}
