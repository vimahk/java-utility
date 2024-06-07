package io.github.kebritam.ratelimiter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class BlockingLeakyBucketRateLimiterTest {

    @Test
    void shouldAllowOnly100CallsPerSecond() {
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(100, Duration.ofSeconds(1));

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

        Assertions.assertEquals(2 * 100 /* it's two seconds */, callCount);
    }

    @Test
    void shouldHaveSameResultsOverallSameRPS() throws InterruptedException {
        Random random = new Random(System.currentTimeMillis());
        int randomRate = random.nextInt(1, 200);
        int randomPer = random.nextInt(50, 1000); // Millis
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(randomRate, Duration.ofMillis(randomPer));

        AtomicInteger callCount = new AtomicInteger();
        var t = Thread.startVirtualThread(() -> {
            // first two call will be immediate
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 500) {
                limiter.Take();
            }

            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 2_000) {
                limiter.Take();
                callCount.incrementAndGet();
            }
        });
        t.join();

        Assertions.assertEquals(
                2 * (randomRate * 1000. / randomPer) /* it's two seconds */,
                callCount.get(),
                1.);
    }

    @Test
    void shouldAllowOnly10CallsPerSecondConcurrently() throws InterruptedException {
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(100, Duration.ofSeconds(1));

        AtomicInteger total = new AtomicInteger(0);
        var t1 = Thread.startVirtualThread(() -> {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 2_000) {
                limiter.Take();
                total.incrementAndGet();
            }
        });
        var t2 = Thread.startVirtualThread(() -> {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 2_000) {
                limiter.Take();
                total.incrementAndGet();
            }
        });

        t1.join();
        t2.join();

        Assertions.assertTrue(total.get() > 200 && total.get() < 205, "total: " + total);
    }

    @Test
    void shouldBlockForAbout100MillisForEachCall() {
        RateLimiter limiter = new BlockingLeakyBucketRateLimiter(10, Duration.ofSeconds(1));

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