package co.riva.door;

import co.riva.door.config.IConnectionConfig;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;

import co.riva.door.config.DoorConfig;

import static co.riva.door.FutureUtils.thenOnException;

//Not ThreadSafe
public class DoorClient implements Pinger.Sender {
    @NotNull
    private final CopyOnWriteArraySet<DoorListener> _listeners = new CopyOnWriteArraySet<>();
    @NotNull
    private final Pinger pinger;
    @NotNull
    private final DoorLogger logger;
    @NotNull
    private final DoorConfig doorConfig;
    @Nullable
    private Transport _transport;
    private State state;
    private CompletableFuture<Void> isConnectionReady;

    public DoorClient(@NotNull DoorConfig doorConfig,
                      @NotNull DoorLogger doorLogger) {
        logger = doorLogger;
        isConnectionReady = new CompletableFuture<>();
        pinger = new Pinger(this, doorLogger, Executors.newSingleThreadScheduledExecutor());
        state = State.DISCONNECTED;
        this.doorConfig = doorConfig;
    }

    /**
     * Asynchronous call to connect to door
     * <p>
     * If already connected then AlreadyConnectedException is set in the future.
     */
    @SuppressWarnings("UnusedDeclaration")
    public CompletionStage<Void> connect(@NotNull final IConnectionConfig connectionConfig) {
        if (isDisconnected()) {
            state = State.CONNECTING;
            _transport = new Transport();
            _transport.setListener(getTransportListener());
            final String host = connectionConfig.getHost();
            final int port = connectionConfig.getPort();
            final int socketTimeout = doorConfig.getSocketTimeout();

            return _transport.connect(host, port)
                    .whenComplete(thenOnException(throwable -> moveToDisconnectedState(throwable, connectionConfig)));
        } else {
            return FutureUtils.getFailedFuture(new AlreadyConnectedException("Door is already connected!"));
        }
    }

    /**
     * @return true if socket was not disconnected
     */
    public SettableFuture<Boolean> disconnect(final String reason) {
        logger.log("Inside disconnect with reason:" + reason);
        final SettableFuture<Boolean> future = SettableFuture.create();
        future.set(moveToDisconnectedState(new Exception(reason), null));
        return future;
    }

    /**
     * This method should only be run in _executor thread
     *
     * @return true if socket was disconnected by this call, false if socket was already disconnected
     */
    private boolean moveToDisconnectedState(Throwable reason, IConnectionConfig connectionConfig) {

        State previousState = state;

        final boolean previouslyDisconnected = isDisconnected();

        if (!previouslyDisconnected) {
            state = State.DISCONNECTED;
            pinger.stop();
            if (_transport != null) {
                if (connectionConfig == null) {
                    connectionConfig = _transport.getConnectionConfig();
                }
                _transport.close(reason.getMessage());
                _transport.clearListener();
                _transport = null;
            }
        }

        if (previousState == State.CONNECTED_AUTHENTICATION_PENDING ||
                previousState == State.CONNECTED_AUTHENTICATED) {
            fireOnDisconnected(reason, connectionConfig);
        }
        return !previouslyDisconnected;
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isConnected() {
        return state == State.CONNECTED_AUTHENTICATION_PENDING ||
                state == State.CONNECTED_AUTHENTICATED;
    }

    public boolean isAuthenticated() {
        return state == State.CONNECTED_AUTHENTICATED;
    }

    public boolean isDisconnected() {
        return state == State.DISCONNECTED;
    }

    public ListenableFuture<Void> sendPacket(@NotNull final String connectionId,
                                             @NotNull final String envelopeBody,
                                             @NotNull final DoorEnvelopeType doorEnvelopeType) {
        return sendPacket(connectionId, envelopeBody, doorEnvelopeType, null);
    }

    public ListenableFuture<Void> sendPacket(@NotNull final String connectionId,
                                             @NotNull final String envelopeBody,
                                             @NotNull final DoorEnvelopeType doorEnvelopeType,
                                             @Nullable final String doorEnvelopeMethod) {
        return sendPacket(connectionId, envelopeBody, doorEnvelopeType, doorEnvelopeMethod, null);
    }

    public ListenableFuture<Void> sendPacket(@NotNull final String connectionId,
                                             @NotNull final String envelopeBody,
                                             @NotNull final DoorEnvelopeType doorEnvelopeType,
                                             @Nullable final String doorEnvelopeMethod,
                                             @Nullable final String flowId) {
        final SettableFuture<Void> future = SettableFuture.create();
        DoorEnvelope envelope;
        DoorEnvelope.Type type = DoorEnvelope.Type.getEnum(doorEnvelopeType);
        envelope = new DoorEnvelope(type, envelopeBody, connectionId, null,
                doorEnvelopeMethod, flowId);
        Futures.addCallback(sendMessage(envelope), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                future.set(result);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                future.setException(t);
            }
        });
        return future;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ListenableFuture<String> sendStart(@NotNull final String entity,
                                              @NotNull final String startPayload,
                                              @Nullable final String flowId) {
        final SettableFuture<String> future = SettableFuture.create();
        final String doorConnectionId = entity + '_' + UUID.randomUUID();
        DoorEnvelope message = new DoorStartEnvelope(DoorEnvelope.Type.OMS_AUTH, startPayload,
                doorConnectionId, entity, doorConfig.getUaInfo(), doorConfig.isTraceEnabled(),
                flowId);
        Futures.addCallback(sendMessage(message), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                future.set(doorConnectionId);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                future.setException(t);
            }
        });
        return future;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ListenableFuture<Void> sendEnd(@NotNull final String connectionId,
                                          @NotNull final String endXml) {
        final SettableFuture<Void> future = SettableFuture.create();
        DoorEnvelope message = new DoorEnvelope(DoorEnvelope.Type.END, endXml, connectionId,
                null, null, null);
        Futures.addCallback(sendMessage(message), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                future.set(result);
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                future.setException(t);
            }
        });
        return future;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void addListener(DoorListener listener) {
        _listeners.add(listener);
    }

    @Override
    public void sendPing() {
        sendMessage(new DoorEnvelope(DoorEnvelope.Type.PING, null, null, null, null, null));
    }

    @Override
    public void onPingPongTimeout() {
        disconnect("ping_pong_timeout");
    }

    /*privates*/
    private ListenableFuture<Void> sendMessage(final DoorEnvelope doorEnvelope) {
        final SettableFuture<Void> future = SettableFuture.create();
        if (_transport != null) {
            try {
                String json = doorEnvelope.toJson();
                logger.log("||>>" + json);
                _transport.send(json.getBytes());
                future.set(null);
            } catch (IOException e) {
                future.setException(e);
            }
        } else {
            future.setException(new Throwable("Transport is null"));
        }
        return future;
    }

    private Transport.SocketHandlerEventListener getTransportListener() {
        return new Transport.SocketHandlerEventListener() {
            @Override
            public void onMessage(byte... msg) {
                final DoorEnvelope doorEnvelope = DoorEnvelope.fromJson(new String(msg));
                if (doorEnvelope == null) {
                    logger.log("Ignoring stanza from door: " + new String(msg));
                    return;
                }
                logger.log("||<<" + doorEnvelope.toJson());
                handleRecievedEnvelope(doorEnvelope);
            }

            private void handleRecievedEnvelope(final DoorEnvelope doorEnvelope) {
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
                        logger.log("Debug data: " + doorEnvelope.toJson());
                        break;
                    case OMS_PACKET:
                    case OMS_AUTH:
                    case OMS_MESSAGE:
                    case UNKNOWN:
                        fireOnBytesReceived(doorEnvelope.getId(), type,
                                doorEnvelope.getBody().getBytes());
                }
            }

            @Override
            public void onReady(final boolean isAuthenticated) {
                if (isAuthenticated) {
                    state = State.CONNECTED_AUTHENTICATED;
                } else {
                    state = State.CONNECTED_AUTHENTICATION_PENDING;
                }
                logger.log("transport connected. started pinger");
                pinger.start();
                isConnectionReady.complete(null);
                fireOnConnected(isAuthenticated);
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
            for (DoorListener l : _listeners) {
                l.onBytesReceived(connectionId, doorEnvelopeType, data);
            }
        } else {
            logger.log("Failed to get door envelope type for type:" + type + " with payload:" +
                    Arrays.toString(data));
        }
    }

    private void fireOnConnected(boolean isAuthenticated) {
        for (DoorListener l : _listeners) {
            l.onConnected(isAuthenticated);
        }
    }

    private void fireOnDisconnected(Throwable reason, IConnectionConfig connectionConfig) {
        for (DoorListener l : _listeners) {
            l.onDisconnected(reason, connectionConfig);
        }
    }

    private void fireOnAlert(String msg) {
        for (DoorListener l : _listeners) {
            l.onAlert(msg);
        }
    }

    private void fireOnEndReceived(String connectionId, String reason) {
        for (DoorListener l : _listeners) {
            l.onEndReceived(connectionId, reason);
        }
    }

    private void fireOnErrorReceived(String connectionId, String reason) {
        for (DoorListener l : _listeners) {
            l.onErrorReceived(connectionId, reason);
        }
    }

    private enum State {
        DISCONNECTED, CONNECTING, CONNECTED_AUTHENTICATION_PENDING, CONNECTED_AUTHENTICATED
    }
}