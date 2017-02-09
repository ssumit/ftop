package co.riva.door;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class DoorStartEnvelope extends DoorEnvelope {
    private static final String KEY_UA_INFO = "ua-info";
    private static final String KEY_TRACE = "trace";
    private static final String KEY_ENTITY = "entity";
    @NotNull private final String _uaInfo;
    @NotNull private final String _entity;
    private final boolean _enableTrace;

    public DoorStartEnvelope(@NotNull Type type, @Nullable String body, @NotNull String id,
                             @NotNull String entity, @NotNull String uaInfo, boolean enableTrace,
                             @Nullable String flowId) {
        super(type, body, id, null, null, flowId);
        _entity = entity;
        _uaInfo = uaInfo;
        _enableTrace = enableTrace;
    }

    @Override
    protected JSONObject getJsonObject() {
        JSONObject jsonObject = super.getJsonObject();
        try {
            jsonObject.putOpt(KEY_ENTITY, _entity);
            jsonObject.put(KEY_UA_INFO, _uaInfo);
            jsonObject.put(KEY_TRACE, _enableTrace);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
