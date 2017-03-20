package co.riva.auth;

import co.riva.door.*;
import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import olympus.common.JID;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

public class SimpleDoorClient {

    private final DoorClient doorClient;
    private final JID userJID;
    private final String authToken;
    private final ScheduledExecutorService executorService;
    private String connectionID;
    private final Gson gson;
    private String streamID;
    private List<MessageListener> listeners;

    public SimpleDoorClient(JID userJID, String authToken, ScheduledExecutorService executorService) {
        this.userJID = userJID;
        this.authToken = authToken;
        this.executorService = executorService;
        this.doorClient = new DoorClient(System.out::println);
        this.listeners = new ArrayList<>();
        gson = new Gson();
        attachRequestListener();
    }

    public void addListener(MessageListener listener) {
        listeners.add(listener);
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
        DoorEnvelope envelope = new DoorEnvelope(DoorEnvelope.Type.O_REQUEST,
                gson.toJson(new Req<>(requestBody, doorEnvelopeMethod.to(), requestID)),
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

    public CompletionStage<Void> connect(String doorHost, int doorPort, Protocol protocol) {
        return doorClient.connect(doorHost, doorPort, protocol);
    }

    public CompletionStage<Boolean> disconnect(String reason) {
        return doorClient.disconnect(reason);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private static class Req<T> extends HashMap<String, Object> {
        public Req(T t, String to, String id) {
            for (Field field : t.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    put(field.getName(), field.get(t));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            put("to", to);
            put("id", id);
        }
    }

    private void attachRequestListener() {
        doorClient.addListener(new DoorListener() {
            @Override
            public void onBytesReceived(String connectionId, DoorEnvelopeType type, byte[] data) {
                String jsonString = new String(data);
                executorService.submit(() ->
                        listeners.forEach(listener -> listener.onNewMessage(type, jsonString)));
            }

            @Override
            public void onConnected(boolean isConnected) {
                System.out.println("Connected : " + isConnected);
            }

            @Override
            public void onDisconnected(Throwable throwable, ConnectionConfig connectionConfig) {
                executorService.submit(() ->
                        listeners.forEach(listener -> listener.onErrorReceived(throwable)));
            }

            @Override
            public void onAlert(String message) {
                System.out.println("Alert : " + message);
            }

            @Override
            public void onEndReceived(String id, String reason) {
                executorService.submit(() ->
                        listeners.forEach(listener -> listener.onErrorReceived(new RuntimeException("on End received, reason: " + reason))));
            }

            @Override
            public void onErrorReceived(String id, String reason) {
                System.out.println("Error received");
                executorService.submit(() ->
                        listeners.forEach(listener -> listener.onErrorReceived(new RuntimeException("on Error received, reason: " + reason))));
            }
        });
    }

    public interface MessageListener {
        void onNewMessage(DoorEnvelopeType type, String message);

        void onErrorReceived(Throwable throwable);
    }
}