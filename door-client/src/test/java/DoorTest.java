import co.riva.door.DoorClient;
import co.riva.door.DoorEnvelopeType;
import co.riva.door.config.DoorConfig;
import co.riva.door.config.IConnectionConfig;
import co.riva.door.config.Protocol;

import java.io.IOException;

public class DoorTest {
    private static final String DOOR_HOST = "doormobile.handler.talk.to";
    private static final int DOOR_PORT = 995;
    private static final int SOCKET_TIMEOUT_MILLIS = 60000;

    public static void main(String[] args) throws IOException {

        DoorConfig doorConfig = new DoorConfig(SOCKET_TIMEOUT_MILLIS, true, "test");
        DoorClient client = new DoorClient(doorConfig, System.out::println);

        client.addListener(new DoorClient.DoorListener() {
            @Override
            public void onBytesReceived(String id, DoorEnvelopeType type, byte[] data) {
                System.out.println("Data : " + new String(data));
            }

            @Override
            public void onConnected(boolean isAuthenticated) {
                System.out.println("Connected : " + isAuthenticated);
            }

            @Override
            public void onDisconnected(Throwable reason, IConnectionConfig connectionConfig) {
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

        client.connect(null, new IConnectionConfig() {
            @Override
            public String getHost() {
                return DOOR_HOST;
            }

            @Override
            public int getPort() {
                return DOOR_PORT;
            }

            @Override
            public Protocol getProtocol() {
                return Protocol.TLS;
            }
        });
    }
}