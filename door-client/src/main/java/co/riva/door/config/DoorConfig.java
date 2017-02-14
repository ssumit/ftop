package co.riva.door.config;

public class DoorConfig {
    private final String _uaInfo;
    private final boolean _enableTrace;

    public DoorConfig(boolean enableTrace, String uaInfo) {
        _enableTrace = enableTrace;
        _uaInfo = uaInfo;
    }

    public String getUaInfo() {
        return _uaInfo;
    }

    public boolean isTraceEnabled() {
        return _enableTrace;
    }
}
