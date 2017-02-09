package co.riva.door;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class ensures that the heartbeat (ping pong) is maintained.
 * It takes an executor. Ensure all public methods are invoked from the same executor.
 * Note: all the public methods are non-blocking.
 */
//@NotThreadSafe
class Pinger {
    private static final int PINGER_INTERVAL_IN_SEC = 5;
    private static final int PINGER_TIMEOUT_IN_MILLIS = 30 * 1000;
    @NotNull
    private final Sender _sender;
    @NotNull
    private final DoorLogger _doorLogger;
    private final ScheduledExecutorService executor;
    @Nullable
    private ScheduledFuture<?> _pingTask;
    private volatile long _lastInteractionTimeInMillis = System.currentTimeMillis();
    private PingerState _state;

    public Pinger(@NotNull Sender sender, @NotNull DoorLogger doorLogger, ScheduledExecutorService executor) {
        _sender = sender;
        _doorLogger = doorLogger;
        this.executor = executor;
    }

    public void start() {
        _lastInteractionTimeInMillis = System.currentTimeMillis();
        stop();
        _state = PingerState.PONG_RECEIVED;
        _pingTask = getPingTask(executor);
    }

    private ScheduledFuture<?> getPingTask(ScheduledExecutorService executor) {
        return executor.scheduleAtFixedRate((Runnable) () -> {
            try {
                if (isTimedOut()) {
                    _doorLogger.log("ping pong timed out");
                    fireTimeOut();
                    stop();
                } else {
                    _doorLogger.log("sending ping");
                    _sender.sendPing();
                    _state = PingerState.PING_SENT;
                }
            } catch (Exception ignored) {
                _doorLogger.log("Received exception in pinger:" + ignored);
                stop();
            }
        }, 0, PINGER_INTERVAL_IN_SEC, TimeUnit.SECONDS);
    }

    public void stop() {
        _doorLogger.log("Inside ping stop with pingtask:" + _pingTask);
        if (_pingTask != null) {
            _pingTask.cancel(true);
        } else {
            //todo: find bug and remove the following
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void consumePong() {
        _doorLogger.log("received pong");
        _lastInteractionTimeInMillis = System.currentTimeMillis();
        _state = PingerState.PONG_RECEIVED;
    }

    private void fireTimeOut() {
        _sender.onPingPongTimeout();
    }

    private boolean isTimedOut() {
        return (System.currentTimeMillis() - _lastInteractionTimeInMillis) > PINGER_TIMEOUT_IN_MILLIS && _state == PingerState.PING_SENT;
    }

    private enum PingerState {
        PING_SENT, PONG_RECEIVED
    }

    public interface Sender {
        void sendPing();

        void onPingPongTimeout();
    }
}