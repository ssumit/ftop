package co.riva.door.config;

public interface IConnectionConfig {
    String getHost();

    int getPort();

    Protocol getProtocol();
}