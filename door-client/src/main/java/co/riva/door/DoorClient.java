package co.riva.door;

import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.Protocol;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

import static co.riva.door.FutureUtils.thenOnException;

//Not ThreadSafe
public class DoorClient implements Pinger.Sender {
    @NotNull
    private final CopyOnWriteArraySet<DoorListener> listeners = new CopyOnWriteArraySet<>();
    @NotNull
    private final Pinger pinger;
    @NotNull
    private final DoorLogger logger;
    @Nullable
    private Transport transport;
    private State state;
    private CompletableFuture<Void> isConnectionReady;
    private final Gson gson;

    public DoorClient(@NotNull DoorLogger doorLogger) {
        logger = doorLogger;
        isConnectionReady = new CompletableFuture<>();
        pinger = new Pinger(this, doorLogger, Executors.newSingleThreadScheduledExecutor());
        state = State.DISCONNECTED;
        this.gson = new Gson();
    }

    public CompletionStage<Void> connect(@NotNull ConnectionConfig connectionConfig) {
        if (isDisconnected()) {
            state = State.CONNECTING;
            transport = new Transport();
            transport.setListener(getTransportListener());

            return transport.connect(connectionConfig)
                    .whenComplete(thenOnException(throwable -> moveToDisconnectedState(throwable, connectionConfig)));
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public CompletionStage<Void> connect(@NotNull String host, int port, Protocol protocol) {
        return connect(new ConnectionConfig(host, port, protocol));
    }

    /**
     * @return true if socket was disconnected by this call, false if socket was already disconnected
     */
    public CompletionStage<Boolean> disconnect(final String reason) {
        logger.log("Inside disconnect with reason:" + reason);
        return CompletableFuture.completedFuture(moveToDisconnectedState(new Exception(reason), null));
    }

    private boolean moveToDisconnectedState(Throwable reason, ConnectionConfig connectionConfig) {

        final boolean previouslyDisconnected = isDisconnected();

        if (!previouslyDisconnected) {
            state = State.DISCONNECTED;
            pinger.stop();
            if (transport != null) {
                if (connectionConfig == null) {
                    connectionConfig = transport.getConnectionConfig();
                }
                transport.close(reason.getMessage());
                transport.clearListener();
                transport = null;
            }
            fireOnDisconnected(reason, connectionConfig);
        }

        return !previouslyDisconnected;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public boolean isDisconnected() {
        return state == State.DISCONNECTED;
    }

    public void addListener(DoorListener listener) {
        listeners.add(listener);
    }

    @Override
    public void sendPing() {
        send(new DoorEnvelope(DoorEnvelope.Type.PING, null, null, null, null, null));
    }

    @Override
    public void onPingPongTimeout() {
        disconnect("ping_pong_timeout");
    }

    public CompletionStage<Void> send(DoorEnvelope doorEnvelope) {
        if (transport != null) {
            String json = gson.toJson(doorEnvelope);
            logger.log("||>>" + json);
            return transport.send(json.getBytes());
        } else {
            return FutureUtils.getFailedFuture(new Throwable("Transport is null"));
        }
    }

    private Transport.SocketHandlerEventListener getTransportListener() {
        return new Transport.SocketHandlerEventListener() {
            @Override
            public void onMessage(byte... msg) {
                String json = new String(msg);
                final DoorEnvelope doorEnvelope = gson.fromJson(json, DoorEnvelope.class);
                if (doorEnvelope == null) {
                    logger.log("Ignoring stanza from door: " + json);
                    return;
                }
                logger.log("||<<" + gson.toJson(doorEnvelope));
                handleIncomingEnvelope(doorEnvelope);
            }

            private void handleIncomingEnvelope(final DoorEnvelope doorEnvelope) {
                final DoorEnvelope.Type type = doorEnvelope.getType();
                switch (type) {
                    case PONG:
                        pinger.consumePong();
                        break;
                    case END:
                        fireOnEndReceived(doorEnvelope.getId(), doorEnvelope.getBody());
                        break;
                    case ERROR:
                        String reason = doorEnvelope.getBody();
                        if (!Strings.isNullOrEmpty(doorEnvelope.getInfo())) {
                            reason += ", info: " + doorEnvelope.getInfo();
                        }
                        final String finalReason = reason;
                        fireOnErrorReceived(doorEnvelope.getId(), finalReason);
                        break;
                    case PING://ignore received ping stanza
                        break;
                    case DEBUG:
                        logger.log("Debug data: " + gson.toJson(doorEnvelope));
                        break;
                    case OMS_AUTH:
                    case OMS_MESSAGE:
                    case O_RESPONSE:
                    case UNKNOWN:
                        fireOnBytesReceived(doorEnvelope.getId(), type,
                                doorEnvelope.getBody().getBytes());
                }
            }

            @Override
            public void onReady(final boolean isConnected) {
                state = State.CONNECTED;
                logger.log("transport connected. started pinger");
                pinger.start();
                isConnectionReady.complete(null);
                fireOnConnected(isConnected);
            }

            @Override
            public void onAlert(final byte... msg) {
                fireOnAlert(new String(msg));
            }

            @Override
            public void onShutdown(@NotNull final Exception reason) {
                if (!isConnectionReady.isDone()) {
                    isConnectionReady.completeExceptionally(reason);
                }
                reason.printStackTrace();
                moveToDisconnectedState(reason, null);
            }
        };
    }

    private void fireOnBytesReceived(String connectionId, DoorEnvelope.Type type, byte... data) {
        final Optional<DoorEnvelopeType> envelopeTypeOptional = DoorEnvelope.Type.getDoorEnvelopeTypeEnum(type);
        if (envelopeTypeOptional.isPresent()) {
            final DoorEnvelopeType doorEnvelopeType = envelopeTypeOptional.get();
            for (DoorListener l : listeners) {
                l.onBytesReceived(connectionId, doorEnvelopeType, data);
            }
        } else {
            logger.log("Failed to get door envelope type for type:" + type + " with payload:" +
                    Arrays.toString(data));
        }
    }

    private void fireOnConnected(boolean isConnected) {
        for (DoorListener l : listeners) {
            l.onConnected(isConnected);
        }
    }

    private void fireOnDisconnected(Throwable reason, ConnectionConfig connectionConfig) {
        for (DoorListener l : listeners) {
            l.onDisconnected(reason, connectionConfig);
        }
    }

    private void fireOnAlert(String msg) {
        for (DoorListener l : listeners) {
            l.onAlert(msg);
        }
    }

    private void fireOnEndReceived(String connectionId, String reason) {
        for (DoorListener l : listeners) {
            l.onEndReceived(connectionId, reason);
        }
    }

    private void fireOnErrorReceived(String connectionId, String reason) {
        for (DoorListener l : listeners) {
            l.onErrorReceived(connectionId, reason);
        }
    }

    private enum State {
        DISCONNECTED, CONNECTING, CONNECTED
    }
}