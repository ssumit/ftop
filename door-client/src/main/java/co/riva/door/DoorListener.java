package co.riva.door;

import co.riva.door.config.ConnectionConfig;

public interface DoorListener {
    void onBytesReceived(String connectionId, DoorEnvelopeType type, byte... data);

    void onConnected(boolean isConnected);

    void onDisconnected(Throwable reason, ConnectionConfig connectionConfig);

    void onAlert(String message);

    void onEndReceived(String connectionId, String reason);

    void onErrorReceived(String connectionId, String reason);
}
