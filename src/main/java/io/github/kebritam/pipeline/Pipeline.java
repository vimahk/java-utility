package io.github.kebritam.pipeline;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface Pipeline<T> {

    void setInitializer(Supplier<T> initializer);

    Pipeline<T> addPipe(UnaryOperator<T> pipe);

    void start();

    void stop();

    boolean isRunning();
}
