package io.github.vimahk.ratelimiter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class BlockingLeakyBucketRateLimiter implements RateLimiter {

    long perRequest;
    AtomicLong prevAccessTime;

    public BlockingLeakyBucketRateLimiter(int rate, Duration per) {
        this.perRequest = per.toNanos() / rate;
        this.prevAccessTime = new AtomicLong(0);
    }

    @Override
    public void Take() {
        long nextAccessTime;
        long currentTime;

        while (true) {
            currentTime = System.nanoTime();
            long prevAccessTime = this.prevAccessTime.get();

            if (currentTime - prevAccessTime > perRequest) {
                nextAccessTime = currentTime;
            } else {
                nextAccessTime = prevAccessTime + perRequest;
            }

            if (this.prevAccessTime.compareAndSet(prevAccessTime, nextAccessTime)) {
                break;
            }
        }

        long sleepDuration = nextAccessTime - currentTime;
        if (sleepDuration > 0) {
            try {
                Thread.sleep(Duration.ofNanos(sleepDuration));
            } catch (InterruptedException ignore) {
            }
        }
    }
}
