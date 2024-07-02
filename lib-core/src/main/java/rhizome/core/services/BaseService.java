package rhizome.core.services;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import io.activej.async.function.AsyncRunnable;
import io.activej.async.function.AsyncRunnables;
import io.activej.async.service.ReactiveService;
import io.activej.promise.Promise;
import io.activej.promise.Promises;
import io.activej.reactor.AbstractReactive;
import io.activej.reactor.Reactor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Getter
public abstract class BaseService extends AbstractReactive implements ReactiveService {

    private List<AsyncRunnable> routines = new ArrayList<>();

    protected BaseService(Reactor reactor) {
        super(reactor);
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

    static Promise<Void> asyncRun(List<AsyncRunnable> runnables) {
        return Promises.all(runnables.stream().map(AsyncRunnable::run));
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
