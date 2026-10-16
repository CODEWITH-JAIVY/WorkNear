package com.labourse.job.integration;

import com.labourse.job.service.JobAcceptanceService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// Under the browse-and-choose model, only the CUSTOMER calls select — so the realistic race is
// no longer "20 different labour accepting," it's the same select request firing more than once
// (a double-tap on the button, or a network retry resubmitting the same click). This proves that
// even under a burst of 20 near-simultaneous calls for the same job, exactly one locks it — never
// zero, never two. This is the one test in this scaffold that MUST run against real infra, not
// mocks, since Mockito can't fake Lua's atomicity guarantee.
@Testcontainers
class JobAcceptanceConcurrencyIT {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Test
    void onlyOneSelectWinsWhenTheSameJobIsSelectedConcurrentlyTwentyTimes() throws InterruptedException {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getFirstMappedPort());
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        JobAcceptanceService acceptanceService = new JobAcceptanceService(template);

        long jobId = 12345L;
        long selectedLabourId = 7L; // same labour every time — simulating duplicate submits of one click
        int attemptCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(attemptCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger(0);

        for (int i = 0; i < attemptCount; i++) {
            pool.submit(() -> {
                try {
                    startGate.await(); // all threads fire at once
                    if (acceptanceService.trySelect(jobId, selectedLabourId)) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                }
            });
        }

        startGate.countDown(); // release all 20 threads simultaneously
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(winners.get()).isEqualTo(1); // exactly one — never zero, never a double-lock
    }
}
