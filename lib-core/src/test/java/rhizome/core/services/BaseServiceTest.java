package rhizome.core.services;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.activej.async.function.AsyncRunnable;
import io.activej.promise.Promise;
import io.activej.reactor.Reactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

class BaseServiceTest {

    @Mock private Reactor reactor;
    @InjectMocks private CustomService customService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddRoutine() {
        // Prepare test data
        AsyncRunnable routine = mock(AsyncRunnable.class);

        // Call the method under test
        BaseService result = customService.addRoutine(routine);

        // Verify the behavior
        assertEquals(customService, result);
        assertEquals(1, customService.routines().size());
        assertEquals(routine, customService.routines().get(0));
    }

    @Test
    void testBuild() {
        // Prepare test data
        AsyncRunnable routine = mock(AsyncRunnable.class);
        
        var result = customService.addRoutine(routine);

        // Verify the behavior
        assertEquals(customService, result);
        assertEquals(1, customService.routines().size());
        assertNotEquals(routine, customService.routines().get(0));
    }

    @Test
    void testAsyncRun() {
        // Prepare test data
        AsyncRunnable runnable1 = mock(AsyncRunnable.class);
        AsyncRunnable runnable2 = mock(AsyncRunnable.class);
        AtomicBoolean flag1 = new AtomicBoolean(false);
        AtomicBoolean flag2 = new AtomicBoolean(false);

        // Mock behavior
        when(runnable1.run()).thenReturn(Promise.ofCallback(callback -> {
            flag1.set(true);
            callback.accept(null, null);
        }));
        when(runnable2.run()).thenReturn(Promise.ofCallback(callback -> {
            flag2.set(true);
            callback.accept(null, null);
        }));

        // Call the method under test
        Promise<Void> result = BaseService.asyncRun(List.of(runnable1, runnable2));

        // Verify the behavior
        // assertEquals(Promise.ofCallback(callback-> { 
        //     callback.accept(null, null);
        // }), result);
        assertEquals(true, flag1.get());
        assertEquals(true, flag2.get());
        verify(runnable1, times(1)).run();
        verify(runnable2, times(1)).run();
    }

    private static class CustomService extends BaseService {
        public CustomService(Reactor reactor) {
            super(reactor);
        }

        @Override
        public Promise<?> start() {
            return null;
        }

        @Override
        public Promise<?> stop() {
            return null;
        }

        @Override
        public BaseService addRoutine(AsyncRunnable routine) {
            return super.addRoutine(routine);
        }
    }
}