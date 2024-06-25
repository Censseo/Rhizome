package rhizome.core.engine;

import io.activej.promise.Promise;

@FunctionalInterface
public interface Action {
    Promise<Void> execute(Task task);
}
