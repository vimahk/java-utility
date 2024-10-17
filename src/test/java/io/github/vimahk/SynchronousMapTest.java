package io.github.vimahk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class SynchronousMapTest {

    private SynchronousMap<String, String> synchronousMap;

    @BeforeEach
    void setUp() {
        synchronousMap = new SynchronousMap<>();
    }

    @Test
    void testInsertAndGetSuccess() throws InterruptedException {
        String key = "testKey";
        String value = "testValue";

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> {
                try {
                    synchronousMap.insert(key, value, 1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Assertions.fail("interruption is unexpected");
                }
            });
            String retrievedValue = synchronousMap.get(key, 1, TimeUnit.SECONDS);

            Assertions.assertEquals(value, retrievedValue);
        }
    }

    @Test
    void testGetAndInsertSuccess() throws InterruptedException {
        String key = "testKey";
        String value = "testValue";

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            AtomicReference<String> retrievedValue = new AtomicReference<>();
            executor.execute(() -> {
                try {
                    retrievedValue.set(synchronousMap.get(key, 1, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Assertions.fail("interruption is unexpected");
                }
            });
            synchronousMap.insert(key, value, 1, TimeUnit.SECONDS);

            Assertions.assertEquals(value, retrievedValue.get());
        }
    }

    @Test
    void testInsertTimeout() throws InterruptedException {
        String key = "testKey";
        String value = "testValue";

        Assertions.assertDoesNotThrow(() -> synchronousMap.insert(key, value, 100, TimeUnit.MILLISECONDS));
        Assertions.assertNull(synchronousMap.get(key, 100, TimeUnit.MILLISECONDS));
    }

    @Test
    void testGetTimeout() throws InterruptedException {
        String key = "testKey";

        String retrievedValue = synchronousMap.get(key, 100, TimeUnit.MILLISECONDS);

        Assertions.assertNull(retrievedValue);
    }

    @Test
    void testConcurrentInsertAndGet() {
        String key = "testKey";
        String value = "testValue";

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            for (int i = 0 ; i < 1000 ; i++) {
                int finalI = i;
                executor.submit(() -> {
                    try {
                        synchronousMap.insert(key + finalI, value, 1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        Assertions.fail("Insert failed with exception: " + e.getMessage());
                    }
                });
            }

            for (int i = 0 ; i < 1000 ; i++) {
                int finalI = i;
                executor.submit(() -> {
                    try {
                        String result = synchronousMap.get(key + finalI, 1, TimeUnit.SECONDS);
                        Assertions.assertEquals(value, result);
                    } catch (Exception e) {
                        Assertions.fail("Get failed with exception: " + e.getMessage());
                    }
                });
            }
        }
    }

    @Test
    void testMapIsClearedAfterOperations() throws InterruptedException {
        String key = "testKey";
        String value = "testValue";

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> {
                try {
                    synchronousMap.insert(key, value, 1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Assertions.fail("interruption is unexpected");
                }
            });

            synchronousMap.get(key, 1, TimeUnit.SECONDS);

            Assertions.assertNull(synchronousMap.get(key, 100, TimeUnit.MILLISECONDS));
        }
    }
}