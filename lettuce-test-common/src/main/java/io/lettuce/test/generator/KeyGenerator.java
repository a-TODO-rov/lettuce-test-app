package io.lettuce.test.generator;

public interface KeyGenerator {

    default String nextKey() {
        return "key";
    }

}
