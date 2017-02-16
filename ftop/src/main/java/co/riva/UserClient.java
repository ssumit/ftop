package co.riva;

import co.riva.auth.SimpleDoorClient;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
import co.riva.group.GroupClient;
import olympus.common.JID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static co.riva.door.FutureUtils.thenOnException;

public class UserClient {

    private final SimpleDoorClient doorClient;
    private final GroupClient groupClient;
    private final ScheduledExecutorService executorService;
    private static final String DEFAULT_DOOR_HOST = "doorstaging.handler.talk.to";
    private static final int DEFAULT_DOOR_PORT = 995;
    private static final DoorConfig DEFAULT_DOOOR_CONFIG = new DoorConfig(true, "device-type=service;os=Linux;os-version=14.04;app-name=goto;app-version=0.1-SNAPSHOT;timezone=+05:30");
    private final String doorHost;
    private final int doorPort;
    private final DoorConfig doorConfig;

    public UserClient(JID userJID, String authToken, String doorHost, int doorPort, DoorConfig doorConfig) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.doorClient = new SimpleDoorClient(userJID, authToken, executorService);
        this.doorHost = doorHost;
        this.doorPort = doorPort;
        this.doorConfig = doorConfig;
        this.groupClient = new GroupClient(this, doorClient);
    }

    public UserClient(JID userJID, String authToken) {
        this(userJID, authToken, DEFAULT_DOOR_HOST, DEFAULT_DOOR_PORT, DEFAULT_DOOOR_CONFIG);
    }

    public UserClient(JID userJID, String authToken, String doorHost, int doorPort) {
        this(userJID, authToken, doorHost, doorPort, DEFAULT_DOOOR_CONFIG);
    }

    public UserClient(JID userJID, String authToken, DoorConfig doorConfig) {
        this(userJID, authToken, DEFAULT_DOOR_HOST, DEFAULT_DOOR_PORT, doorConfig);
    }

    public GroupClient getGroupClient() {
        return groupClient;
    }

    public CompletionStage<UserClient> authenticate() {
        CompletableFuture<Void> response = new CompletableFuture<>();
        executorService.submit(() -> doorClient.authenticate(doorConfig)
                .thenAccept(response::complete)
                .whenComplete(thenOnException(response::completeExceptionally)));
        return response
                .thenApply(__ -> UserClient.this);
    }

    public CompletionStage<UserClient> connect() {
        CompletableFuture<Void> response = new CompletableFuture<>();
        executorService.submit(() -> doorClient.connect(doorHost, doorPort, Protocol.TLS)
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
}