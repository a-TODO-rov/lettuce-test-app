package io.lettuce.test;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultWorkloadOptionsTest {

    @Test
    void testGetIntegerWithDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "42");
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals(42, options.getInteger("existingKey", 0));
        assertEquals(0, options.getInteger("missingKey", 0));
    }

    @Test
    void testGetDoubleWithDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "3.14");
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals(3.14, options.getDouble("existingKey", 0.0));
        assertEquals(0.0, options.getDouble("missingKey", 0.0));
    }

    @Test
    void testGetStringWithDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "value");
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals("value", options.getString("existingKey", "default"));
        assertEquals("default", options.getString("missingKey", "default"));
    }

    @Test
    void testGetIntegerWithoutDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "42");
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals(42, options.getInteger("existingKey"));
        assertNull(options.getInteger("missingKey"));
    }

    @Test
    void testGetDoubleWithoutDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "3.14");
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals(3.14, options.getDouble("existingKey"));
        assertNull(options.getDouble("missingKey"));
    }

    @Test
    void testGetStringWithoutDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("existingKey", "value");
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals("value", options.getString("existingKey"));
        assertNull(options.getString("missingKey"));
    }

    @Test
    void testGetDurationWithDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals(Duration.ofMinutes(30), options.getDuration("missingKey", Duration.ofMinutes(30)));
    }

    @Test
    void testGetDurationWithExistingKeyAndDefault() {
        Map<String, String> optionsMap = new HashMap<>();
        optionsMap.put("durationKey", "PT1H"); // 1 hour in ISO-8601 duration format
        DefaultWorkloadOptions options = new DefaultWorkloadOptions(optionsMap);

        assertEquals(Duration.ofHours(1), options.getDuration("durationKey", Duration.ofMinutes(30)));
    }

}
