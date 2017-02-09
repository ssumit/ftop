package co.riva.door;

import co.riva.door.config.IConnectionConfig;
import co.riva.door.config.Protocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import static co.riva.door.FutureUtils.thenOnException;
import static com.sun.tools.javac.util.Assert.checkNull;

//NotThreadSafe
class Transport {
    @NotNull
    private final FrameMaker _framingProtocol;
    @Nullable
    private Socket _socket;
    @Nullable
    private SocketHandlerEventListener _listener;
    private AtomicBoolean _disconnectionLogged = new AtomicBoolean(false);

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
    public IConnectionConfig getConnectionConfig() {
        IConnectionConfig connectionConfig = null;
        if (_socket != null) {
            final String host = _socket.host();
            final int port = _socket.port();
            final Protocol protocol = Protocol.TLS;
            connectionConfig = new IConnectionConfig() {
                @Override
                public String getHost() {
                    return host;
                }

                @Override
                public int getPort() {
                    return port;
                }

                @Override
                public Protocol getProtocol() {
                    return protocol;
                }
            };
        }
        return connectionConfig;
    }

    public CompletionStage<Void> connect(String host, int port) {
        checkNull(_socket);

        _disconnectionLogged.set(false);
        _socket = new TLSSocket(host, port, getSocketListener());
        return _socket.connect()
                .whenComplete(thenOnException(throwable -> _disconnectionLogged.set(true)));
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
                System.out.println("tt");
                logDisconnectionEvent(reason);
                fireOnShutdown(new Exception("Socket closed: " + reason));
            }

            @Override
            public void onError(@NotNull Exception e) {
                System.out.println("tt2");
                logDisconnectionEvent(e.getMessage());
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

    private void logDisconnectionEvent(String reason) {
        if (_disconnectionLogged.getAndSet(true)) {
            return;
        }
    }

    public void send(@NotNull byte[] data) throws IOException {
        assert _socket != null;
        _socket.send(FrameMaker.frame(data));
    }

    public void close(@NotNull String reason) {
        if (_socket != null) {
            System.out.println("tt3");
            _socket.close(reason);
            _socket = null;
            logDisconnectionEvent(reason);
            fireOnShutdown(new Exception("Socket closed: " + reason));
        }
    }

    public boolean isConnected() {
        return _socket != null && _socket.isConnected();
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