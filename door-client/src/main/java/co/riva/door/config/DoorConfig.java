package co.riva.door.config;

public class DoorConfig {
    private final String _uaInfo;
    private final boolean _enableTrace;
    private final int _socketTimeout;

    public DoorConfig(int socketTimeout, boolean enableTrace, String uaInfo) {
        _socketTimeout = socketTimeout;
        _enableTrace = enableTrace;
        _uaInfo = uaInfo;
    }

    public String getUaInfo()
    {
        return _uaInfo;
    }

    public boolean isTraceEnabled()
    {
        return _enableTrace;
    }

    public int getSocketTimeout()
    {
        return _socketTimeout;
    }
}
