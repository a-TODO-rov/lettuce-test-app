package io.lettuce.test;

import java.time.Duration;

public interface CommonWorkloadOptions extends WorkloadOptions {

    int valueSize();

    int elementsCount();

    int iterationCount();

    Duration delayAfterIteration();

    Duration delayAfterWorkload();

    double getSetRatio();

    int transactionSize();

}
