package co.riva.door;

import co.riva.door.config.ConnectionConfig;
import co.riva.door.config.Protocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import static com.sun.tools.javac.util.Assert.checkNonNull;

//NotThreadSafe
class Transport {
    @NotNull
    private final FrameMaker _framingProtocol;
    @Nullable
    private Socket socket;
    @Nullable
    private SocketHandlerEventListener _listener;

    public Transport() {
        _framingProtocol = new FrameMaker();
        HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
        _framingProtocol.addListener(this::fireOnMessage);
    }

    public void setListener(@Nullable SocketHandlerEventListener listener) {
        _listener = listener;
    }

    public void clearListener() {
        _listener = null;
    }

    @Nullable
    public ConnectionConfig getConnectionConfig() {
        ConnectionConfig connectionConfig = null;
        if (socket != null) {
            final String host = socket.host();
            final int port = socket.port();
            final Protocol protocol = Protocol.TLS;
            connectionConfig = new ConnectionConfig(host, port, protocol);
        }
        return connectionConfig;
    }

    public CompletionStage<Void> connect(@NotNull ConnectionConfig connectionConfig) {
        if (socket != null) {
            return FutureUtils.getFailedFuture(new IllegalStateException("socket is not null before connecting"));
        }
        if (!connectionConfig.protocol().equals(Protocol.TLS)) {
            return FutureUtils.getFailedFuture(new UnsupportedOperationException("unsupported protocol"));
        }
        socket = new TLSSocket(connectionConfig.host(), connectionConfig.port(), getSocketListener());
        return socket.connect();
    }

    @NotNull
    private SocketListener getSocketListener() {
        return new SocketListener() {
            @Override
            public void onDNSResolved(long resolutionTimeInNanos, InetSocketAddress resolvedAddress) {
            }

            @Override
            public void onTCPHandshake(long handshakeTimeInNanos,
                                       long connectionTimeoutInMilliseconds) {
            }

            @Override
            public void onProtocolHandshake(long handshakeTimeInNanos,
                                            long readTimeoutInMilliseconds) {
            }

            @Override
            public void onOpen(boolean isAuthenticated) {
                fireOnReady(isAuthenticated);
            }

            @Override
            public void onFirstByteReceived(long timeToFirstByteAfterHandshakeInNanos,
                                            long readTimeoutInMilliseconds) {
            }

            @Override
            public void onClose(String reason) {
                fireOnShutdown(new Exception("Socket closed: " + reason));
            }

            @Override
            public void onError(@NotNull Exception e) {
                fireOnShutdown(e);
            }

            @Override
            public void onData(ByteBuffer data) {
                _framingProtocol.consume(data);
            }

            @Override
            public void onAlert(byte[] message) {
                fireOnAlert(message);
            }
        };
    }

    public CompletionStage<Void> send(@NotNull byte[] data) {
        checkNonNull(socket);
        return socket.send(FrameMaker.frame(data));
    }

    public void close(@NotNull String reason) {
        if (socket != null) {
            socket.close(reason);
            socket = null;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    private void fireOnMessage(byte[] buffer) {
        if (_listener != null) {
            _listener.onMessage(buffer);
        }
    }

    /*privates*/

    private void fireOnShutdown(@NotNull Exception reason) {
        if (_listener != null) {
            _listener.onShutdown(reason);
        }
    }

    private void fireOnReady(boolean isAuthenticated) {
        if (_listener != null) {
            _listener.onReady(isAuthenticated);
        }
    }

    private void fireOnAlert(byte[] msg) {
        if (_listener != null) {
            _listener.onAlert(msg);
        }
    }

    public interface SocketHandlerEventListener {
        void onMessage(byte[] msg);

        void onReady(boolean isAuthenticated);

        void onAlert(byte[] msg);

        void onShutdown(@NotNull Exception reason);
    }
}