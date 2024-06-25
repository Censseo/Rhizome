package rhizome.core.common;

import io.activej.async.function.AsyncSupplier;
import io.activej.async.service.EventloopTaskScheduler;
import io.activej.eventloop.Eventloop;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static io.activej.async.service.EventloopTaskScheduler.Schedule.ofInterval;

/**
 * Utility class for scheduling tasks.
 */
public class SchedulerUtil {

    private SchedulerUtil() {}

    /**
     * Schedules a task to run every hour.
     * @param eventloop
     * @param task
     * @return
     */
    public static EventloopTaskScheduler scheduleHourly(Eventloop eventloop, AsyncSupplier<Void> task) {
        LocalTime now = LocalTime.now();
        LocalTime nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);

        long initialDelayMinutes = now.until(nextHour, ChronoUnit.MINUTES);

        return EventloopTaskScheduler.create(eventloop, task)
                .withSchedule(ofInterval(Duration.ofHours(1)))
                .withInitialDelay(Duration.ofMinutes(initialDelayMinutes));
    }

    /**
     * Schedules a task to run every minute.
     * @param eventloop
     * @param task
     * @return
     */
    public static EventloopTaskScheduler scheduleEveryMinute(Eventloop eventloop, AsyncSupplier<Void> task) {
        LocalTime now = LocalTime.now();
        LocalTime nextMinute = LocalTime.of(now.getHour(), now.getMinute()).plusMinutes(1);

        long initialDelaySeconds = now.until(nextMinute, ChronoUnit.SECONDS);

        return EventloopTaskScheduler.create(eventloop, task)
                .withSchedule(ofInterval(Duration.ofMinutes(1)))
                .withInitialDelay(Duration.ofSeconds(initialDelaySeconds));
    }

    /**
     * Schedules a task to run every 30 seconds.
     * @param eventloop
     * @param task
     * @return
     */
    public static EventloopTaskScheduler scheduleEvery30Seconds(Eventloop eventloop, AsyncSupplier<Void> task) {
        LocalTime now = LocalTime.now();
        LocalTime next30Seconds = LocalTime.of(now.getHour(), now.getMinute(), now.getSecond()).plusSeconds(30);

        long initialDelayMillis = now.until(next30Seconds, ChronoUnit.MILLIS);

        return EventloopTaskScheduler.create(eventloop, task)
                .withSchedule(ofInterval(Duration.ofSeconds(30)))
                .withInitialDelay(Duration.ofMillis(initialDelayMillis));
    }

    /**
     * Schedules a task to run every 10 seconds.
     * @param eventloop
     * @param task
     * @return
     */
    public static EventloopTaskScheduler scheduleEvery10Seconds(Eventloop eventloop, AsyncSupplier<Void> task) {
        LocalTime now = LocalTime.now();
        LocalTime next10Seconds = LocalTime.of(now.getHour(), now.getMinute(), now.getSecond()).plusSeconds(10);

        long initialDelayMillis = now.until(next10Seconds, ChronoUnit.MILLIS);

        return EventloopTaskScheduler.create(eventloop, task)
                .withSchedule(ofInterval(Duration.ofSeconds(10)))
                .withInitialDelay(Duration.ofMillis(initialDelayMillis));
    }

    /**
     * Schedules a task to run every second.
     * @param eventloop
     * @param task
     * @return
     */
    public static EventloopTaskScheduler scheduleEverySecond(Eventloop eventloop, AsyncSupplier<Void> task) {
        LocalTime now = LocalTime.now();
        LocalTime nextSecond = LocalTime.of(now.getHour(), now.getMinute(), now.getSecond()).plusSeconds(1);

        long initialDelayMillis = now.until(nextSecond, ChronoUnit.MILLIS);

        return EventloopTaskScheduler.create(eventloop, task)
                .withSchedule(ofInterval(Duration.ofSeconds(1)))
                .withInitialDelay(Duration.ofMillis(initialDelayMillis));
    }
}
