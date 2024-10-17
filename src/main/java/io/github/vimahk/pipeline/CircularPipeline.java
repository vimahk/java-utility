package io.github.vimahk.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

public class CircularPipeline<T> implements Pipeline<T> {

    private static final Logger logger = Logger.getLogger(CircularPipeline.class.getName());

    private enum PipelineState {
        RUNNING,
        NOT_RUNNING,
        ASKED_TO_STOP,
        STOPPING,
    }

    private volatile PipelineState currentState;
    private ExecutorService executor;
    private final List<UnaryOperator<T>> pipes;
    private Supplier<T> initializer;
    private Future<?> runLoopFuture;

    public CircularPipeline(Supplier<T> initializer) {
        this.currentState = PipelineState.NOT_RUNNING;
        this.pipes = new ArrayList<>();
        setInitializer(initializer);
    }

    @Override
    public void setInitializer(Supplier<T> initializer) {
        if (this.currentState == PipelineState.RUNNING)
            throw new IllegalStateException("A new initializer cannot be set when the pipeline is running");
        this.initializer = initializer;
    }

    @Override
    public Pipeline<T> addPipe(UnaryOperator<T> pipe) {
        if (this.currentState == PipelineState.RUNNING)
            throw new IllegalStateException("A new pipe cannot be added when the pipeline is running");
        this.pipes.add(pipe);
        return this;
    }

    @Override
    public void start() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.currentState = PipelineState.RUNNING;
        this.runLoopFuture = this.executor.submit(() -> {
            try {
                runLoop();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void stop() {
        if (this.currentState != PipelineState.RUNNING)
            return;
        this.currentState = PipelineState.ASKED_TO_STOP;

        this.executor.shutdown();

        boolean terminated = false;
        try {
            this.currentState = PipelineState.NOT_RUNNING;
            this.runLoopFuture.get();
            terminated = this.executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            logger.severe("Termination got interrupted");
        } catch (ExecutionException e) {
            logger.severe("Exception thrown in running run loop. Message: " + e.getMessage());
            throw new RuntimeException(e);
        }

        if (!terminated)
            logger.warning("Terminating Pipeline timed out");
    }

    @Override
    public boolean isRunning() {
        return this.currentState.equals(PipelineState.RUNNING);
    }

    private void runLoop() throws ExecutionException, InterruptedException {
        List<T> elements = new ArrayList<>(Collections.nCopies(this.pipes.size() + 1, null));
        List<Future<T>> futures = new ArrayList<>(this.pipes.size() + 1);
        while (this.currentState == PipelineState.RUNNING) {
            futures.clear();

            Future<T> initFuture = this.executor.submit(() -> this.initializer.get());
            futures.add(initFuture);

            for (int i = 0; i < pipes.size(); i++) {
                int finalI = i;
                Future<T> pipeFuture = this.executor.submit(
                        () -> pipes.get(finalI).apply(elements.get(finalI))
                );
                futures.add(pipeFuture);
            }

            List<T> tempElements = new ArrayList<>(futures.size());
            for (var future : futures) {
                tempElements.add(future.get());
            }

            for (int i = 0; i < tempElements.size(); i++) {
                elements.set(i, tempElements.get(i));
            }
        }
    }
}

