package io.lettuce.test.generator;

import java.util.concurrent.atomic.AtomicInteger;

public class SequentialKeyGenerator implements KeyGenerator {

    private String pattern = "key-%d";

    private int rangeMin = 0;

    private int rangeMax = 10000;

    private AtomicInteger current = new AtomicInteger();

    public SequentialKeyGenerator(String pattern, int rangeMin, int rangeMax) {
        this.pattern = pattern;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
    }

    public String nextKey() {
        int idx = current.getAndIncrement();
        if (idx > rangeMax) {
            current.set(rangeMin);
            idx = rangeMin;
        }
        return String.format(pattern, idx);
    }

}
