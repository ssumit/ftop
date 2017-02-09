package co.riva.door;

import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public interface SocketListener {
    void onDNSResolved(long resolutionTimeInNanos, InetSocketAddress resolvedAddress);

    void onTCPHandshake(long handshakeTimeInNanos, long connectionTimeoutInMilliseconds);

    void onProtocolHandshake(long hanshakeTimeInNanos, long readTimeoutInMilliseconds);

    void onOpen(boolean isAuthenticated);

    void onFirstByteReceived(long timeToFirstByteAfterHandshakeInNanos, long readTimeoutInMilliseconds);

    void onClose(String reason);

    void onError(@NotNull Exception e);

    void onAlert(byte[] message);

    void onData(ByteBuffer data);
}
