package at.yawk.cricket.template;

import java.time.ZoneId;
import java.util.concurrent.Callable;

/**
 * Holder class to store the current time zone for a thread, defaulting to {@link ZoneId#systemDefault()}.
 *
 * @author yawkat
 */
public class TimeZoneHolder {
    private static final ThreadLocal<ZoneId> ZONE_ID_THREAD_LOCAL = ThreadLocal.withInitial(ZoneId::systemDefault);

    public static ZoneId getZone() {
        return ZONE_ID_THREAD_LOCAL.get();
    }

    /**
     * Set the current context time zone for this thread.
     *
     * @return The old time zone.
     */
    public static ZoneId setZone(ZoneId zone) {
        ZoneId previous = ZONE_ID_THREAD_LOCAL.get();
        ZONE_ID_THREAD_LOCAL.set(zone);
        return previous;
    }

    /**
     * Call a {@link TimeZoneHolder.TypedCallable} with a given timezone. The timezone will be reset after the callable
     * has completed.
     */
    public static <V, E extends Exception> V withZone(ZoneId zone, TypedCallable<V, E> task) throws E {
        ZoneId previous = setZone(zone);
        try {
            return task.call();
        } finally {
            setZone(previous);
        }
    }

    public interface TypedCallable<V, E extends Exception> extends Callable<V> {
        @Override
        V call() throws E;
    }
}
