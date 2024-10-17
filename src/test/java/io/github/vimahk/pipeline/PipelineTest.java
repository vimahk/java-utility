package io.github.vimahk.pipeline;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PipelineTest {

    @Test
    void precedenceAndOverallFunctionalityCheckTest() throws InterruptedException {
        List<Integer> generatedIntegers = new ArrayList<>();

        AtomicInteger i = new AtomicInteger();
        Pipeline<Integer> pipeline = new CircularPipeline<>(i::getAndIncrement);

        pipeline.addPipe(integer ->
                {
                    if (integer == null) return null;
                    return 2 * integer;
                })
                .addPipe(integer ->
                {
                    if (integer == null) return null;
                    return integer + 10;
                })
                .addPipe(integer ->
                {
                    if (integer == null) return null;
                    generatedIntegers.add(integer);
                    return integer;
                });
        pipeline.start();
        Thread.sleep(Duration.ofSeconds(2));
        pipeline.stop();

        for (int idx = 0 ; idx < generatedIntegers.size() - 1 ; idx++) {
            assertTrue(generatedIntegers.get(idx) < generatedIntegers.get(idx + 1));
            assertEquals(idx * 2 + 10, generatedIntegers.get(idx));
        }
    }

    @Test
    void throwExceptionWhenPipelineIsRunningAndSetInitializerGetsCalledTest() {
        AtomicInteger i = new AtomicInteger();
        Pipeline<Integer> pipeline = new CircularPipeline<>(i::getAndIncrement);
        pipeline.addPipe(integer -> integer);
        pipeline.start();

        assertThrows(IllegalStateException.class, () -> pipeline.setInitializer(() -> 1));
    }

    @Test
    void throwExceptionWhenPipelineIsRunningAndAddPipeGetsCalledTest() {
        AtomicInteger i = new AtomicInteger();
        Pipeline<Integer> pipeline = new CircularPipeline<>(i::getAndIncrement);
        pipeline.addPipe(integer -> integer);
        pipeline.start();

        assertThrows(IllegalStateException.class, () -> pipeline.addPipe((integer) -> 1));
    }

    @Test
    void stopAddPipeAndRunAgainTest() {
        AtomicInteger i = new AtomicInteger();
        Pipeline<Integer> pipeline = new CircularPipeline<>(i::getAndIncrement);
        pipeline.addPipe(integer -> integer);
        pipeline.start();

        pipeline.stop();
        pipeline.addPipe(integer -> integer * 2);
        pipeline.start();

        assertTrue(pipeline.isRunning());
    }
}