package rhizome.core.engine;

import io.activej.promise.Promise;

@FunctionalInterface
interface TaskState {
    Promise<Void> handleEvent(Task task, TaskEvent event);
}