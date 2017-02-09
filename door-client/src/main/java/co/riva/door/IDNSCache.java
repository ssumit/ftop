package co.riva.door;

import java.util.Optional;

public interface IDNSCache {
    Optional<String> getCachedIPAddress(String hostname);
}
