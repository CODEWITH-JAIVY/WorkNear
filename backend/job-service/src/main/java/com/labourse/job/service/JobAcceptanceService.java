package com.labourse.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

// Repurposed from the original instant-accept model: previously many labour raced to accept a
// job and only the first writer to this Redis key won. Under browse-and-choose, only the
// CUSTOMER calls this — but the same atomic-lock primitive is still worth keeping, because it
// makes a double-tap/double-submit on the "select this labour" button safe: if the select
// request somehow fires twice (flaky network retry, impatient re-tap), only the first one locks
// the job; the second gets a clean "already selected" instead of silently overwriting the pick.
@Service
@RequiredArgsConstructor
public class JobAcceptanceService {

    private final RedisTemplate<String, String> redisTemplate;

    // KEYS[1] = job:select:lock:{jobId}
    // ARGV[1] = labourId being selected
    // Returns 1 if this call won the lock, 0 if the job was already locked (by an earlier select)
    private static final String SELECT_LUA = """
        if redis.call('EXISTS', KEYS[1]) == 1 then
            return 0
        end
        redis.call('SET', KEYS[1], ARGV[1], 'EX', 86400)
        return 1
        """;

    public boolean trySelect(Long jobId, Long labourId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(SELECT_LUA, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList("job:select:lock:" + jobId),
                String.valueOf(labourId));
        return result != null && result == 1L;
    }
}
