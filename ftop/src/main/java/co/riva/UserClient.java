package co.riva;

import co.riva.auth.AuthHelper;
import co.riva.door.DoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.DoorListener;
import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
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
    private static final String DOOR_HOST = "doorstaging.handler.talk.to";
    private static final int DOOR_PORT = 995;


    public UserClient(JID userJID, String authToken) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        DoorConfig doorConfig = new DoorConfig(60000, true, "device-type=service;os=Linux;os-version=14.04;app-name=goto;app-version=0.1-SNAPSHOT;timezone=+05:30");
        doorClient = new DoorClient(doorConfig, System.out::println);
        authHelper = new AuthHelper(userJID, authToken, doorClient);
        doorClient.addListener(new DoorListener() {
            @Override
            public void onBytesReceived(String id, DoorEnvelopeType type, byte[] data) {
                System.out.println("Data : " + new String(data));
            }

            @Override
            public void onConnected(boolean isConnected) {
                System.out.println("Connected : " + isConnected);
            }

            @Override
            public void onDisconnected(Throwable reason, ConnectionConfig connectionConfig) {
                System.out.println("Disconnected : " + reason);
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
            }
        });
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

    public CompletionStage<String> request(String request) {
        return null;
    }
}