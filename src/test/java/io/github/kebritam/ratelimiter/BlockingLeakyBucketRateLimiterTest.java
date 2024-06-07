package io.github.kebritam.ratelimiter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BlockingLeakyBucketRateLimiterTest {

    @Test
    void shouldAllowOnly30CallsPerSecond() {
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(100);

        // first two call will be immediate
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 500) {
            limiter.Take();
        }

        int callCount = 0;
        start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 2_000) {
            limiter.Take();
            ++callCount;
        }

        Assertions.assertEquals(200, callCount);
    }

    @Test
    void shouldAllowOnly10CallsPerSecondConcurrently() throws InterruptedException, ExecutionException {
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(100);

        int total = 0;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var t1 = executor.submit(() -> {
                long start = System.currentTimeMillis();
                int callCount = 0;
                while (System.currentTimeMillis() - start < 2_000) {
                    limiter.Take();
                    ++callCount;
                }
                return callCount;
            });
            var t2 = executor.submit(() -> {
                long start = System.currentTimeMillis();
                int callCount = 0;
                while (System.currentTimeMillis() - start < 2_000) {
                    limiter.Take();
                    ++callCount;
                }
                return callCount;
            });
            total += t1.get();
            total += t2.get();
        }

        Assertions.assertTrue(total > 200 && total < 205);
    }

    @Test
    void shouldBlockForAbout100MillisForEachCall() {
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(10);

        for (int i = 0 ; i < 50 ; ++i) {
            long start = System.nanoTime();
            limiter.Take();
            long end = System.nanoTime();
            if (i < 2) {
                Assertions.assertEquals(0, (end - start) / 1_000_000., 10., "index: " + i);
            } else if (i > 3) {
                Assertions.assertEquals(100, (end - start) / 1_000_000., 10., "index: " + i);
            }
        }
    }
}