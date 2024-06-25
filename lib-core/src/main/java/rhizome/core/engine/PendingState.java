package rhizome.core.engine;

import io.activej.promise.Promise;

public class PendingState implements TaskState {
    @Override
    public Promise<Void> handleEvent(Task task, TaskEvent event) {
        // TODO Auto-generated method stub
        return Promise.of(null);
    }
}