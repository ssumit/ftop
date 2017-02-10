package co.riva.auth;

import co.riva.door.DoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.DoorListener;
import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.Protocol;
import com.google.gson.Gson;
import olympus.common.JID;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static co.riva.door.FutureUtils.thenOnException;

public class AuthClient {

    private final JID jid;
    private final String token;
    private String _streamId;
    private DoorClient doorClient;
    private final Gson gson = new Gson();
    private static final String DOOR_HOST = "doorstaging.handler.talk.to";
    private static final int DOOR_PORT = 995;

    public AuthClient(JID jid, String token) {
        this.jid = jid;
        this.token = token;
        DoorConfig doorConfig = new DoorConfig(60000, true, "device-type=mobile;os=Android;os-version=6.0.1;app-name=goto;app-version=v3.3-ge8b2565-qa-3090;timezone=+05:30");//gson.toJson(new UserAgent("LG", "android", "1", "ftop", "1")));
        doorClient = new DoorClient(doorConfig, System.out::println);

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

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        new AuthClient(new JID("go.to", "apollo", "64vkt615oooyyy1y"), "r4hh5u2hrdvay525y5dharaa2dv55r4a").start()
                .toCompletableFuture().get();

    }

    public CompletionStage<Void> start() {
        String flowId = UUID.randomUUID().toString();
        return doorClient.connect(DOOR_HOST, DOOR_PORT, Protocol.TLS)
                .thenApply(__ -> getStreamPacket(new Credential(jid, token)))
                .thenCompose(streamPacket -> doorClient.sendStart(jid.toString(), streamPacket, flowId))
                .whenComplete(thenOnException(throwable -> throwable.printStackTrace()));
    }

    private String getStreamPacket(@NotNull Credential credential) {
        this._streamId = createStreamId(credential.getBareJid());
        AuthPacket authPacket = new AuthPacket(credential, this._streamId);
        return gson.toJson(authPacket);
    }

    private String createStreamId(JID bareJID) {
        return bareJID + "_" + UUID.randomUUID();
    }
}