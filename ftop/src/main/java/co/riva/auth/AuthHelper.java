package co.riva.auth;

import co.riva.door.DoorEnvelopeType;
import co.riva.door.config.DoorConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import olympus.common.JID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static co.riva.door.FutureUtils.thenOnException;

public class AuthHelper {

    private final JID jid;
    private final String token;
    private final SimpleDoorClient doorClient;
    private final Gson gson = new Gson();
    private final CompletableFuture<Void> isAuthenticated;

    public AuthHelper(JID jid, String token, SimpleDoorClient doorClient) {
        this.jid = jid;
        this.token = token;
        this.doorClient = doorClient;
        this.isAuthenticated = new CompletableFuture<>();
    }

    public CompletionStage<Void> authenticate(DoorConfig doorConfig, String streamID) {
        doorClient.addListener(getListener());
        String streamPacket = getStreamPacket(new Credential(jid, token), streamID);
        doorClient.sendStart(jid.toString(), streamPacket, doorConfig)
                .whenComplete(thenOnException(isAuthenticated::completeExceptionally));
        return isAuthenticated;
    }

    private String getStreamPacket(Credential credential, String streamID) {
        AuthPacket authPacket = new AuthPacket(credential, streamID);
        return gson.toJson(authPacket);
    }


    private SimpleDoorClient.MessageListener getListener() {
        return new SimpleDoorClient.MessageListener() {
            @Override
            public void onNewMessage(DoorEnvelopeType type, String message) {
                if (type.equals(DoorEnvelopeType.O_AUTH)) {
                    if (!isAuthenticated.isDone()) {
                        if (isAuthSuccess(message)) {
                            isAuthenticated.complete(null);
                            doorClient.removeListener(this);
                        } else if (isAuthFailure(message)) {
                            isAuthenticated.completeExceptionally(new RuntimeException("Auth failure: " + message));
                            doorClient.removeListener(this);
                        }
                    }
                }

            }

            @Override
            public void onErrorReceived(Throwable throwable) {
                if (!isAuthenticated.isDone()) {
                    isAuthenticated.completeExceptionally(throwable);
                }
                doorClient.removeListener(this);
            }
        };
    }

    private boolean isAuthFailure(String response) {
        try {
            AuthResponse authResponse = gson.fromJson(response, new TypeToken<AuthResponse>() {
            }.getType());
            return authResponse.getResponseType() != null &&
                    authResponse.getResponseType().equalsIgnoreCase("failure");
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean isAuthSuccess(String response) {
        try {
            AuthResponse authResponse = gson.fromJson(response, new TypeToken<AuthResponse>() {
            }.getType());
            return authResponse.getResponseType() != null &&
                    authResponse.getResponseType().equalsIgnoreCase("success");
        } catch (Exception ignored) {
        }
        return false;
    }

    static class AuthResponse {
        private String responseType;

        public String getResponseType() {
            return responseType;
        }
    }
}