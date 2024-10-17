package io.github.vimahk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

class RecoveryTaskManagerTest {

    @Test
    void testTasksExecutionOnInit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Runnable task = latch::countDown;
        List<Runnable> tasks = List.of(task, task, task);

        RecoveryTaskManager manager = new RecoveryTaskManager(tasks);

        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
        manager.terminate(1, TimeUnit.SECONDS);
    }

    @Test
    void testRecoveryRestartsTasks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(9);
        BlockingQueue<Object> chan = new SynchronousQueue<>();
        Runnable task = latch::countDown;
        Runnable latestToRun = () -> chan.offer(new Object());
        List<Runnable> tasks = List.of(task, task, task, latestToRun);

        RecoveryTaskManager manager = new RecoveryTaskManager(tasks);

        chan.take();
        manager.recover();

        chan.take();
        manager.recover();

        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
        manager.terminate(1, TimeUnit.SECONDS);
    }

    @Test
    void testTermination() throws InterruptedException {
        Runnable task = () -> {};
        List<Runnable> tasks = List.of(task, task, task);

        RecoveryTaskManager manager = new RecoveryTaskManager(tasks);

        boolean terminated = manager.terminate(1, TimeUnit.SECONDS);

        Assertions.assertTrue(terminated);
    }

    @Test
    void testReviverInterruptionHandling() throws InterruptedException {
        Runnable task = () -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        BlockingQueue<Object> chan = new SynchronousQueue<>();
        Runnable latestToRun = () -> chan.offer(new Object());

        List<Runnable> tasks = List.of(task, task, task, latestToRun);

        RecoveryTaskManager manager = new RecoveryTaskManager(tasks);
        chan.take();

        boolean terminated = manager.terminate(1, TimeUnit.SECONDS);

        Assertions.assertTrue(terminated);
    }

    @Test
    void testHandlingExceptionThrownFromTasks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(6);
        BlockingQueue<Object> chan = new SynchronousQueue<>();
        Runnable task = latch::countDown;
        Runnable latestToRun = () -> chan.offer(new Object());
        Runnable exceptionTask = () -> { throw new RuntimeException(); };
        List<Runnable> tasks = List.of(task, task, task, exceptionTask, latestToRun);

        RecoveryTaskManager manager = new RecoveryTaskManager(tasks);

        chan.take();
        manager.recover();

        Thread.sleep(Duration.ofMillis(100));

        manager.terminate(1, TimeUnit.SECONDS);
    }

    @Test
    void testRestartJobRun() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Runnable task = () -> {};
        Runnable restartJob = latch::countDown;
        List<Runnable> tasks = List.of(task, task);

        RecoveryTaskManager manager = new RecoveryTaskManager(tasks, restartJob);

        manager.recover();

        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
        manager.terminate(1, TimeUnit.SECONDS);
    }
}