package co.riva.door.config;

public class ConnectionConfig {

    private final String host;
    private final int port;
    private final Protocol protocol;

    public ConnectionConfig(String host, int port, Protocol protocol) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public Protocol protocol() {
        return protocol;
    }
}