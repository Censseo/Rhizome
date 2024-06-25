package rhizome.core.services;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import io.activej.async.function.AsyncRunnable;
import io.activej.async.function.AsyncRunnables;
import io.activej.async.service.EventloopService;
import io.activej.eventloop.Eventloop;
import io.activej.promise.Promise;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Getter
public abstract class BaseService implements EventloopService {

    private Eventloop eventloop;
    private List<AsyncRunnable> routines = new ArrayList<>();

    protected BaseService(Eventloop eventloop) {
        this.eventloop = eventloop;
    }

    @Override
    public @NotNull Promise<?> start() {
        log.info("|SERVICE STARTING|");
        return asyncRun(routines).whenResult(() -> log.info("|SERVICE STARTED|"));
    }

    @Override
    public @NotNull Promise<?> stop() {
        log.info("|SERVICE STOPPING|");
        return Promise.complete().whenResult(() -> log.info("|SERVICE STOPPED|"));
    }

    @Override
    public @NotNull Eventloop getEventloop() {
        return eventloop;
    }

    static Promise<Void> asyncRun(List<AsyncRunnable> runnables) {
        return Promise.ofCallback(callback -> {
            Promise<Void> promise = Promise.complete();
            for (AsyncRunnable runnable : runnables) {
                promise = promise.then(runnable::run);
            }
            promise.run(callback);
        });
    }

    protected BaseService addRoutine(AsyncRunnable routine) {
        routines.add(routine);
        return this;
    }

    public <T extends BaseService> T build() {
        this.routines = Collections.unmodifiableList(routines.stream().map(AsyncRunnables::reuse).toList());
        return (T) this;
    }
}
