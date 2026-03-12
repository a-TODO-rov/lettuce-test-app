package io.lettuce.test.workloads;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Random;

public class GetSetWorkload extends BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(GetSetWorkload.class);

    private final StringRedisTemplate redisTemplate;

    public GetSetWorkload(StringRedisTemplate redisTemplate, CommonWorkloadOptions options) {
        super(options);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run() {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        Random random = new Random();

        String payload = PayloadUtils.randomString(options().valueSize());

        log.info("Starting GetSetWorkload with {} iterations, getSetRatio={}", options().iterationCount(),
                options().getSetRatio());

        for (int i = 0; i < options().iterationCount(); i++) {
            String key = keyGenerator().nextKey();
            if (random.nextDouble() < options().getSetRatio()) {
                ops.set(key, payload);
            } else {
                ops.get(key);
            }

            delay(options().delayAfterIteration());
        }

        log.info("GetSetWorkload completed {} iterations", options().iterationCount());
    }

}
