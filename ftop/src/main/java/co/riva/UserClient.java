package co.riva;

import co.riva.auth.SimpleDoorClient;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
import co.riva.group.GroupMessageHelper;
import olympus.common.JID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static co.riva.door.FutureUtils.thenOnException;

public class UserClient {

    private final SimpleDoorClient doorClient;
    private final ScheduledExecutorService executorService;
    private final GroupMessageHelper groupMessageHelper;
    private static final String DOOR_HOST = "doorstaging.handler.talk.to";
    private static final int DOOR_PORT = 995;


    public UserClient(JID userJID, String authToken) {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        doorClient = new SimpleDoorClient(userJID, authToken, executorService);
        groupMessageHelper = new GroupMessageHelper(doorClient);
    }

    public GroupMessageHelper getGroupMessageHelper() {
        return groupMessageHelper;
    }

    public CompletionStage<UserClient> authenticate() {
        CompletableFuture<Void> response = new CompletableFuture<>();
        DoorConfig doorConfig = new DoorConfig(true, "device-type=service;os=Linux;os-version=14.04;app-name=goto;app-version=0.1-SNAPSHOT;timezone=+05:30");
        executorService.submit(() -> doorClient.authenticate(doorConfig)
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
}