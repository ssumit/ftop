package co.riva.auth;

import co.riva.door.DoorEnvelopeType;
import co.riva.door.DoorListener;
import co.riva.door.config.ConnectionConfig;
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
    private final DoorListener doorListener;
    private final CompletableFuture<Void> isAuthenticated;

    public AuthHelper(JID jid, String token, SimpleDoorClient doorClient) {
        this.jid = jid;
        this.token = token;
        this.doorClient = doorClient;
        this.isAuthenticated = new CompletableFuture<>();
        this.doorListener = getListener();
    }

    public CompletionStage<Void> authenticate(DoorConfig doorConfig, String streamID) {
        doorClient.addListener(doorListener);
        String streamPacket = getStreamPacket(new Credential(jid, token), streamID);
        doorClient.sendStart(jid.toString(), streamPacket, doorConfig)
                .whenComplete(thenOnException(isAuthenticated::completeExceptionally));
        return isAuthenticated;
    }

    private String getStreamPacket(Credential credential, String streamID) {
        AuthPacket authPacket = new AuthPacket(credential, streamID);
        return gson.toJson(authPacket);
    }

    private DoorListener getListener() {
        return new DoorListener() {
            @Override
            public void onBytesReceived(String id, DoorEnvelopeType type, byte[] data) {
                String response = new String(data);
                System.out.println("Data : " + response);
                if (type.equals(DoorEnvelopeType.O_AUTH)) {
                    if (!isAuthenticated.isDone()) {
                        if (isAuthSuccess(response)) {
                            isAuthenticated.complete(null);
                        } else if (isAuthFailure(response)) {
                            isAuthenticated.completeExceptionally(new RuntimeException("Auth failure: " + data));
                        }
                    }
                }
            }

            @Override
            public void onConnected(boolean isConnected) {
                System.out.println("Connected : " + isConnected);
            }

            @Override
            public void onDisconnected(Throwable reason, ConnectionConfig connectionConfig) {
                if (!isAuthenticated.isDone()) {
                    isAuthenticated.completeExceptionally(reason);
                }
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
                if (!isAuthenticated.isDone()) {
                    isAuthenticated.completeExceptionally(new RuntimeException(reason));
                }
            }
        };
    }

    private boolean isAuthFailure(String response) {
        try {
            AuthResponse authResponse = gson.fromJson(response, new TypeToken<AuthResponse>() {
            }.getType());
            return authResponse.getResponseType() != null &&
                    authResponse.getResponseType().equalsIgnoreCase("failure");
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isAuthSuccess(String response) {
        try {
            AuthResponse authResponse = gson.fromJson(response, new TypeToken<AuthResponse>() {
            }.getType());
            return authResponse.getResponseType() != null &&
                    authResponse.getResponseType().equalsIgnoreCase("success");
        } catch (Exception e) {
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