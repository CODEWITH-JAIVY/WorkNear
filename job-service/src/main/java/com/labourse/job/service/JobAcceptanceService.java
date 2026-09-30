package com.labourse.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobAcceptanceService {

    private final RedisTemplate<String, String> redisTemplate;

    // KEYS[1] = job:accept:lock:{jobId}
    // ARGV[1] = labourId
    // Returns 1 if this labour won the acceptance race, 0 if job was already taken
    private static final String ACCEPT_LUA = """
        if redis.call('EXISTS', KEYS[1]) == 1 then
            return 0
        end
        redis.call('SET', KEYS[1], ARGV[1], 'EX', 86400)
        return 1
        """;

    public boolean tryAccept(Long jobId, Long labourId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ACCEPT_LUA, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList("job:accept:lock:" + jobId),
                String.valueOf(labourId));
        return result != null && result == 1L;
    }
}
