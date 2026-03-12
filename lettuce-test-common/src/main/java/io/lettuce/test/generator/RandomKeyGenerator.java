package io.lettuce.test.generator;

import java.util.Random;

public class RandomKeyGenerator implements KeyGenerator {

    private String pattern = "key-%d";

    private int rangeMin = 0;

    private int rangeMax = 10000;

    private Random random = new Random();

    public RandomKeyGenerator(String pattern, int rangeMin, int rangeMax) {
        this.pattern = pattern;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
    }

    public String nextKey() {
        int idx = random.nextInt((rangeMax - rangeMin) + 1) + rangeMin;
        return String.format(pattern, idx);
    }

}
