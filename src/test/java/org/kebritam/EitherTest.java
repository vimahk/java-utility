package org.kebritam;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


class EitherTest {

    @Test
    void throwIllegalArgumentExceptionWhenPassingNull() {
        assertThrows(IllegalArgumentException.class, () -> { var either = Either.left(null); });
        assertThrows(IllegalArgumentException.class, () -> { var either = Either.right(null); });
    }

    @Test
    void matchLeftAndCallFunctionAndReturn() {
        Either<Integer, RuntimeException> either = Either.left(3);
        int result = either.match(
                integer -> integer * 3,
                err -> 2
        );

        assertEquals(9, result);
    }

    @Test
    void matchRightAndCallFunctionAndReturn() {
        Either<Integer, RuntimeException> either = Either.right(new RuntimeException("error"));
        int result = either.match(
                integer -> integer * 3,
                err -> -1
        );

        assertEquals(-1, result);
    }

    @Test
    void matchLeftAndConsumeWithNoReturn() {
        Either<Integer, RuntimeException> either = Either.left(12);

        AtomicInteger result = new AtomicInteger();
        either.match(
                result::set,
                err -> result.set(-1)
        );

        assertEquals(12, result.get());
    }

    @Test
    void matchRightAndConsumeWithNoReturn() {
        Either<Integer, RuntimeException> either = Either.right(new RuntimeException("Error!"));

        AtomicInteger result = new AtomicInteger();
        either.match(
                result::set,
                err -> result.set(-1)
        );

        assertEquals(-1, result.get());
    }
}