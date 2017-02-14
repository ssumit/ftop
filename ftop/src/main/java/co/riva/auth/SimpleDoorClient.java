package co.riva.auth;

import co.riva.door.*;
import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import olympus.common.JID;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class SimpleDoorClient {

    private final DoorClient doorClient;
    private final JID userJID;
    private final String authToken;
    private String connectionID;
    private final Gson gson;
    private String streamID;
    private Map<String, CompletionStage<String>> requestIDToResponse;

    public SimpleDoorClient(JID userJID, String authToken) {
        this.userJID = userJID;
        this.authToken = authToken;
        this.doorClient = new DoorClient(System.out::println);
        gson = new Gson();
        requestIDToResponse = new HashMap<>();
        attachRequestListener();
    }

    public CompletionStage<Void> authenticate(DoorConfig doorConfig) {
        this.streamID = new JID(userJID).getBareJID() + "_" + UUID.randomUUID();
        return new AuthHelper(userJID, authToken, this).authenticate(doorConfig, streamID);
    }

    public CompletionStage<Void> message(@NotNull String message, @NotNull MessageMethod methodName) {
        checkNotNull(connectionID);
        DoorEnvelope doorEnvelope = new DoorEnvelope(DoorEnvelope.Type.OMS_MESSAGE, message, connectionID, null,
                methodName.getMethodName());
        return doorClient.send(doorEnvelope);
    }

    public CompletionStage<Void> request(@NotNull String requestID,
                                         @NotNull final Object requestBody,
                                         @NotNull final RequestMethod doorEnvelopeMethod) {
        checkNotNull(connectionID);
        requestIDToResponse.put(requestID, new CompletableFuture<>());
        DoorEnvelope envelope = new DoorEnvelope(DoorEnvelope.Type.O_REQUEST,
                gson.toJson(new Req<>(requestBody, doorEnvelopeMethod.to())),
                connectionID, null, doorEnvelopeMethod.methodName());
        return doorClient.send(envelope);
    }

    public CompletionStage<Void> sendEnd(@NotNull final String endXml) {
        checkNotNull(connectionID);
        DoorEnvelope message = new DoorEnvelope(DoorEnvelope.Type.END, endXml, connectionID,
                null, null);
        return doorClient.send(message);
    }

    public CompletionStage<Void> sendStart(@NotNull final String entity,
                                           @NotNull final String startPayload,
                                           @NotNull final DoorConfig doorConfig) {
        connectionID = entity + '_' + UUID.randomUUID();
        DoorEnvelope message = new DoorStartEnvelope(DoorEnvelope.Type.OMS_AUTH, startPayload,
                connectionID, entity, doorConfig.getUaInfo(), doorConfig.isTraceEnabled());

        return doorClient.send(message);
    }

    public void addListener(DoorListener listener) {
        doorClient.addListener(listener);
    }

    public CompletionStage<Void> connect(String doorHost, int doorPort, Protocol protocol) {
        return doorClient.connect(doorHost, doorPort, protocol);
    }

    public CompletionStage<Boolean> disconnect(String reason) {
        return doorClient.disconnect(reason);
    }

    private static class Req<T> {
        @SerializedName("payload")
        final T t;
        @SerializedName("to")
        final String to;

        public Req(T t, String to) {
            this.t = t;
            this.to = to;
        }
    }

    private void attachRequestListener() {
        doorClient.addListener(new DoorListener() {
            @Override
            public void onBytesReceived(String connectionId, DoorEnvelopeType type, byte... data) {
                String response = new String(data);
                ;
            }

            @Override
            public void onConnected(boolean isConnected) {
            }

            @Override
            public void onDisconnected(Throwable reason, ConnectionConfig connectionConfig) {
            }

            @Override
            public void onAlert(String message) {
            }

            @Override
            public void onEndReceived(String connectionId, String reason) {
            }

            @Override
            public void onErrorReceived(String connectionId, String reason) {
            }
        });
    }
}