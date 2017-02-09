package co.riva.door;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static com.google.common.base.Preconditions.checkNotNull;

//NotThreadSafe
class TLSSocket implements Socket {
    private static final ExecutorService _reader = Executors.newSingleThreadExecutor();
    @NotNull
    private final String host;
    private final int port;
    private SSLSocket socket;
    private final SocketListener listener;

    public TLSSocket(@NotNull String host, int port, SocketListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public CompletionStage<Void> connect() {
        try {
            socket = (javax.net.ssl.SSLSocket) SSLSocketFactory.getDefault().createSocket();
            checkNotNull(socket);
            socket.setSoTimeout(socketTimeoutInMillis());
            long startTime = System.nanoTime();
            final InetSocketAddress endpoint = new InetSocketAddress(host(), port());
            fireDNSResolved(System.nanoTime() - startTime, endpoint);

            startTime = System.nanoTime();
            socket.connect(endpoint, SOCKET_TIMEOUT_MILLIS);
            if (!verifyHostname(host, socket.getSession())) {
                System.out.println("host: " + host + " found: " + socket.getSession().getPeerPrincipal());
                throw new SSLHandshakeException(
                        "Hostname verification failed. Expected hostname: " + host + ", found: " +
                                socket.getSession().getPeerPrincipal());
            }
            fireTCPHandshakeDone(System.nanoTime() - startTime, SOCKET_TIMEOUT_MILLIS);

            final long sslStartTime = System.nanoTime();
            socket.addHandshakeCompletedListener(handshakeCompletedEvent -> {
                fireProtocolHandshakeDone(System.nanoTime() - sslStartTime, socketTimeoutInMillis());
                final SSLSession session = socket.getSession();
                session.invalidate();
            });
            socket.startHandshake();

            fireOpenListener(false);
            startReading();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return FutureUtils.getFailedFuture(e);
        }
    }

    private void fireOpenListener(boolean isAuthenticated) {
        if (listener != null) {
            listener.onOpen(isAuthenticated);
        }
    }

    private void fireProtocolHandshakeDone(long hanshakeTimeInNanos, int readTimeoutInMilliseconds) {
        if (listener != null) {
            listener.onProtocolHandshake(hanshakeTimeInNanos, readTimeoutInMilliseconds);
        }
    }

    private void fireTCPHandshakeDone(long handshakeTimeInNanos, int connectionTimeoutInMilliseconds) {
        if (listener != null) {
            listener.onTCPHandshake(handshakeTimeInNanos, connectionTimeoutInMilliseconds);
        }
    }

    private void fireDNSResolved(long resolutionTimeInNanos, InetSocketAddress endpoint) {
        if (listener != null) {
            listener.onDNSResolved(resolutionTimeInNanos, endpoint);
        }
    }

    @Override
    public CompletionStage<Void> send(@NotNull byte[] payload) {
        try {
            socket.getOutputStream().write(payload);
            return CompletableFuture.completedFuture(null);
        } catch (IOException e) {
            return FutureUtils.getFailedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> close(String reason) {
        try {
            System.out.println("on close reason/msg: " + reason);
            socket.close();
            return CompletableFuture.completedFuture(null);
        } catch (IOException e) {
            return FutureUtils.getFailedFuture(e);
        }
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int socketTimeoutInMillis() {
        return SOCKET_TIMEOUT_MILLIS;
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected() && !socket.isClosed();
    }

    private boolean verifyHostname(final String expectedHostname, final SSLSession sslSession) {
        HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        return hostnameVerifier.verify(expectedHostname, sslSession);
    }

    private void startReading() {
        _reader.submit(this::read);
    }

    private void read() {
        try {
            InputStream inputStream = socket.getInputStream();
            final int BUFFER_SIZE = 16192;
            byte[] buffer = new byte[BUFFER_SIZE];
            int size;
            long startTime = System.nanoTime();
            size = inputStream.read(buffer);
            if (size != -1) {
                fireFirstByteRead(System.nanoTime() - startTime, socketTimeoutInMillis());
                do {
                    ByteBuffer byteBuf = ByteBuffer.allocate(size);
                    byteBuf.order(ByteOrder.BIG_ENDIAN);
                    byteBuf.put(buffer, 0, size);
                    byteBuf.rewind();
                    fireDataListener(byteBuf);
                } while ((size = inputStream.read(buffer)) != -1);
            }
        } catch (IOException e) {
            fireErrorListener(e);
        } finally {
            close("socket read finished");
        }
    }

    private void fireErrorListener(IOException e) {
        if (listener != null) {
            listener.onError(e);
        }
    }

    private void fireDataListener(ByteBuffer byteBuf) {
        if (listener != null) {
            listener.onData(byteBuf);
        }
    }

    private void fireFirstByteRead(long timeToFirstByteAfterHandshakeInNanos, int readTimeoutInMilliseconds) {
        if (listener != null) {
            listener.onFirstByteReceived(timeToFirstByteAfterHandshakeInNanos, readTimeoutInMilliseconds);
        }
    }
}