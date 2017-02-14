import co.riva.door.DoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.DoorListener;
import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.Protocol;

import java.io.IOException;

public class DoorTest {
    private static final String DOOR_HOST = "doormobile.handler.talk.to";
    private static final int DOOR_PORT = 995;
    private static final int SOCKET_TIMEOUT_MILLIS = 60000;

    public static void main(String[] args) throws IOException {

        DoorClient client = new DoorClient(System.out::println);

        client.addListener(new DoorListener() {
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

        client.connect(new ConnectionConfig(DOOR_HOST, DOOR_PORT, Protocol.TLS));
    }
}