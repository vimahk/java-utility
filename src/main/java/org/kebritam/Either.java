package org.kebritam;

import java.util.function.Consumer;
import java.util.function.Function;

public class Either<Left, Right> {
    private final Left left;
    private final Right right;

    private Either(Left left, Right right) {
        this.left = left;
        this.right = right;
    }

    public static <Left, Right> Either<Left, Right> left(Left left) {
        if (left == null) throw new IllegalArgumentException("The Left can't be null when calling Either.left()");
        return new Either<>(left, null);
    }

    public static <Left, Right> Either<Left, Right> right(Right right) {
        if (right == null) throw new IllegalArgumentException("The Right can't be null when calling Either.right()");
        return new Either<>(null, right);
    }

    public void match(Consumer<Left> leftConsumer, Consumer<Right> rightConsumer) {
        if (left == null)
            rightConsumer.accept(this.right);
        else
            leftConsumer.accept(this.left);
    }

    public <T> T match(Function<Left, T> leftFunction, Function<Right, T> rightFunction) {
        return left == null ?
                rightFunction.apply(this.right) :
                leftFunction.apply(this.left);
    }
}
