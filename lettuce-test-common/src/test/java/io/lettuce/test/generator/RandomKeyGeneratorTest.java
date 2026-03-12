package io.lettuce.test.generator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RandomKeyGeneratorTest {

    @Test
    void generatesKeyWithinRange() {
        RandomKeyGenerator generator = new RandomKeyGenerator("key-%d", 0, 10000);
        String key = generator.nextKey();
        assertTrue(key.matches("key-\\d+"));
        int value = Integer.parseInt(key.substring(4));
        assertTrue(value >= 0 && value <= 10000);
    }

    @Test
    void generatesKeyWithCustomPattern() {
        RandomKeyGenerator generator = new RandomKeyGenerator("custom-%d", 0, 10000);
        String key = generator.nextKey();
        assertTrue(key.matches("custom-\\d+"));
    }

    @Test
    void generatesKeyWithSingleValueRange() {
        RandomKeyGenerator generator = new RandomKeyGenerator("key-%d", 5, 5);
        String key = generator.nextKey();
        assertEquals("key-5", key);
    }

}
