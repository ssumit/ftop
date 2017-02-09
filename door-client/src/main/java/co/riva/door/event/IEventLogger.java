package co.riva.door.event;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface IEventLogger {
    void logEvent(String eventName, long eventDurationInMillisec, @Nullable Map<String, String> customParams);
}
