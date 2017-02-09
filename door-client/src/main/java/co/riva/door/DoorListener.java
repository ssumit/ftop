package co.riva.door;

import co.riva.door.config.IConnectionConfig;

public interface DoorListener {
    void onBytesReceived(String connectionId, DoorEnvelopeType type, byte... data);

    void onConnected(boolean isAuthenticated);

    void onDisconnected(Throwable reason, IConnectionConfig connectionConfig);

    void onAlert(String message);

    void onEndReceived(String connectionId, String reason);

    void onErrorReceived(String connectionId, String reason);
}
