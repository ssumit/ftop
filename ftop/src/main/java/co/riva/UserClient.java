package co.riva;

import co.riva.auth.AuthHelper;
import co.riva.door.DoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.DoorListener;
import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
import co.riva.group.GroupMessageHelper;
import co.riva.message.MessageMethod;
import olympus.common.JID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static co.riva.door.FutureUtils.thenOnException;

public class UserClient {

    private DoorClient doorClient;
    private final AuthHelper authHelper;
    private final ScheduledExecutorService executorService;
    private MessageListener listener;
    private final GroupMessageHelper groupMessageHelper;
    private static final String DOOR_HOST = "doorstaging.handler.talk.to";
    private static final int DOOR_PORT = 995;


    public UserClient(JID userJID, String authToken) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        DoorConfig doorConfig = new DoorConfig(60000, true, "device-type=service;os=Linux;os-version=14.04;app-name=goto;app-version=0.1-SNAPSHOT;timezone=+05:30");
        doorClient = new DoorClient(doorConfig, System.out::println);
        authHelper = new AuthHelper(userJID, authToken, doorClient);
        groupMessageHelper = new GroupMessageHelper(doorClient);
        doorClient.addListener(new DoorListener() {
            @Override
            public void onBytesReceived(String connectionId, DoorEnvelopeType type, byte[] data) {
                String jsonString = new String(data);
                System.out.println("Data : " + jsonString);
                if (type.equals(DoorEnvelopeType.O_MESSAGE)) {
                    if (listener != null) {
                        listener.onNewMessage(jsonString);
                    }
                    groupMessageHelper.onNewMessage(jsonString);
                }
            }

            @Override
            public void onConnected(boolean isConnected) {
                System.out.println("Connected : " + isConnected);
            }

            @Override
            public void onDisconnected(Throwable reason, ConnectionConfig connectionConfig) {
                System.out.println("Disconnected : " + reason);
                groupMessageHelper.onErrorReceived();
            }

            @Override
            public void onAlert(String message) {
                System.out.println("Alert : " + message);
            }

            @Override
            public void onEndReceived(String id, String reason) {
                System.out.println("End received");
            }

            @Override
            public void onErrorReceived(String id, String reason) {
                System.out.println("Error received");
                groupMessageHelper.onErrorReceived();
            }
        });
    }

    public GroupMessageHelper getGroupMessageHelper() {
        return groupMessageHelper;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public CompletionStage<UserClient> authenticate() {
        CompletableFuture<Void> response = new CompletableFuture<>();
        executorService.submit(() -> authHelper.authenticate()
                .thenAccept(response::complete)
                .whenComplete(thenOnException(response::completeExceptionally)));
        return response
                .thenApply(__ -> UserClient.this);
    }

    public CompletionStage<UserClient> connect() {
        CompletableFuture<Void> response = new CompletableFuture<>();
        executorService.submit(() -> doorClient.connect(DOOR_HOST, DOOR_PORT, Protocol.TLS)
                .thenAccept(response::complete)
                .whenComplete(thenOnException(response::completeExceptionally)));
        return response
                .thenApply(__ -> UserClient.this);
    }

    public CompletionStage<UserClient> disconnect() {
        CompletableFuture<Boolean> response = new CompletableFuture<>();
        executorService.submit(() -> doorClient.disconnect("explicitly disconnect called")
                .thenAccept(response::complete)
                .whenComplete(thenOnException(response::completeExceptionally)));
        return response
                .thenApply(__ -> UserClient.this);
    }

    public CompletionStage<Void> message(String message, MessageMethod methodName) {
        return doorClient.sendPacket(message, DoorEnvelopeType.O_MESSAGE, methodName.getMethodName());
    }

    public interface MessageListener {
        void onNewMessage(String message);
    }

    public interface RequestListener extends MessageListener {
        void onErrorReceived();
    }
}