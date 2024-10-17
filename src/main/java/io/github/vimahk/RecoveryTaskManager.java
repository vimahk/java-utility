package io.github.vimahk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecoveryTaskManager {

    private static final Logger logger = Logger.getLogger(RecoveryTaskManager.class.getName());

    private final List<Future<?>> futures;
    private final ExecutorService executor;
    private final BlockingQueue<Object> errorChannel;
    private final List<Runnable> tasks;
    private final Runnable restartJob;
    private final Future<?> reviver;

    public RecoveryTaskManager(List<Runnable> tasks) {
        this(tasks, () -> {});
    }

    public RecoveryTaskManager(List<Runnable> tasks, Runnable restartJob) {
        this.futures = new ArrayList<>(tasks.size());
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.errorChannel = new SynchronousQueue<>();
        this.tasks = tasks;
        this.restartJob = restartJob;

        this.reviver = this.executor.submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    restart();
                    this.errorChannel.take(); // blocks until an error happens
                } catch (InterruptedException ignore) {
                    // expected when shutting down the task manager
                    break;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "reviver crashed", e);
                }
            }
        });
    }

    private void restart() throws InterruptedException, ExecutionException {
        for (Future<?> fut : futures) {
            fut.cancel(true);
        }

        restartJob.run();

        for (Future<?> fut : futures) {
            fut.get();
        }
        futures.clear();
        for (Runnable runnable : tasks) {
            Future<?> fut = executor.submit(runnable);
            futures.add(fut);
        }
    }

    public void recover() {
        this.errorChannel.offer(new Object());
    }

    public boolean terminate(long timeout, TimeUnit unit) throws InterruptedException {
        reviver.cancel(true);
        for (Future<?> fut : futures) {
            fut.cancel(true);
        }

        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }
}