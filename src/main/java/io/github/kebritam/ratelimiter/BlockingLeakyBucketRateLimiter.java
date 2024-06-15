package io.github.kebritam.ratelimiter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class BlockingLeakyBucketRateLimiter implements RateLimiter {

    long perRequest;
    AtomicLong nextAccessTime;

    public BlockingLeakyBucketRateLimiter(int rate, Duration per) {
        this.perRequest = per.toNanos() / rate;
        this.nextAccessTime = new AtomicLong(0);
    }

    @Override
    public void Take() {
        long newNextAccessTime;
        long currentTime;

        while (true) {
            currentTime = System.nanoTime();
            long currentNextAccessTime = this.nextAccessTime.get();

            if (currentTime - currentNextAccessTime > perRequest) {
                newNextAccessTime = currentTime;
            } else {
                newNextAccessTime = currentNextAccessTime + perRequest;
            }

            if (this.nextAccessTime.compareAndSet(currentNextAccessTime, newNextAccessTime)) {
                break;
            }
        }

        long sleepDuration = newNextAccessTime - currentTime;
        if (sleepDuration > 0) {
            try {
                Thread.sleep(Duration.ofNanos(sleepDuration));
            } catch (InterruptedException ignore) {
            }
        }
    }
}
