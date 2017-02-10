package co.riva.door;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public interface Socket {
    int SOCKET_TIMEOUT_MILLIS = 30 * 1000;

    CompletionStage<Void> connect();

    CompletionStage<Void> send(@NotNull byte[] payload);

    CompletionStage<Void> close(@NotNull String reason);

    int port();

    String host();

    int socketTimeoutInMillis();

    boolean isConnected();
}