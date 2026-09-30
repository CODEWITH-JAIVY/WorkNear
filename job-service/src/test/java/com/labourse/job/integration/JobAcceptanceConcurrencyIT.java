package com.labourse.job.integration;

import com.labourse.job.service.JobAcceptanceService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

// Proves the exact thing the earlier design conversation was worried about: under real concurrent
// load against a real Redis, exactly one labour wins the "accept" race — never zero, never two.
// This is the one test in this scaffold that MUST run against real infra, not mocks.
@Testcontainers
class JobAcceptanceConcurrencyIT {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Test
    void onlyOneLabourWinsWhenTwentyAcceptTheSameJobSimultaneously() throws InterruptedException {
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
        int labourCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(labourCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger(0);

        for (long labourId = 1; labourId <= labourCount; labourId++) {
            long id = labourId;
            pool.submit(() -> {
                try {
                    startGate.await(); // all threads fire at once
                    if (acceptanceService.tryAccept(jobId, id)) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                }
            });
        }

        startGate.countDown(); // release all 20 threads simultaneously
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(winners.get()).isEqualTo(1); // exactly one — never zero, never a double-accept
    }
}
