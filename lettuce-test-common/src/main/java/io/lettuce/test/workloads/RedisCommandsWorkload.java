package io.lettuce.test.workloads;

import io.lettuce.test.CommonWorkloadOptions;
import io.lettuce.test.util.PayloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

public class RedisCommandsWorkload extends BaseWorkload {

    private static final Logger log = LoggerFactory.getLogger(RedisCommandsWorkload.class);

    private final StringRedisTemplate redisTemplate;

    public RedisCommandsWorkload(StringRedisTemplate redisTemplate, CommonWorkloadOptions options) {
        super(options);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run() {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        String payload = PayloadUtils.randomString(options().valueSize());

        log.info("Starting RedisCommandsWorkload with {} iterations", options().iterationCount());

        for (int i = 0; i < options().iterationCount(); i++) {
            String key = keyGenerator().nextKey();

            // Basic CRUD operations
            valueOps.set(key, payload);
            valueOps.get(key);
            redisTemplate.delete(key);
            valueOps.increment("counter");

            // List operations
            if (options().elementsCount() > 0) {
                String listKey = key + "list";
                for (int j = 0; j < options().elementsCount(); j++) {
                    listOps.leftPush(listKey, payload);
                }
                listOps.range(listKey, 0, -1);
                listOps.trim(listKey, 0, options().elementsCount());
            }

            delay(options().delayAfterIteration());
        }

        log.info("RedisCommandsWorkload completed {} iterations", options().iterationCount());
    }

}
