package rhizome.core.engine;

import io.activej.promise.Promise;

@FunctionalInterface
interface Handler {
    Promise<Void> handle(Task task, Action action, Handler next);
}