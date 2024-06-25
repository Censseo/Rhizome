package rhizome.core.engine;

import io.activej.promise.Promise;

class Task {
    private TaskState currentState;
    private final Handler handlerChain;

    public Task(TaskState initialState, Handler handlerChain) {
        this.currentState = initialState;
        this.handlerChain = handlerChain;
    }

    public Promise<Void> executeCommand(Action action) {
        return handlerChain.handle(this, action, handlerChain);
    }

    public TaskState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(TaskState newState) {
        this.currentState = newState;
    }
}