package co.riva.door;

import com.google.common.base.Optional;


import jdk.nashorn.internal.ir.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

@Immutable
class DoorEnvelope {
    private static final String BODY = "body";
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String INFO = "info";
    private static final String METHOD = "method";
    private static final String FLOW_ID = "flowId";
    @Nullable
    private final String body;
    @Nullable
    private final String id;
    @NotNull
    private final Type type;
    @Nullable
    private final String method;

    @Nullable
    private final String info;
    @Nullable
    private final String flowid;

    public DoorEnvelope(@NotNull Type type, @Nullable String body, @Nullable String id,
                        @Nullable String info, @Nullable String method, @Nullable String flowId) {
        this.body = body;
        this.id = id;
        this.type = type;
        this.info = info;
        this.method = method;
        flowid = flowId;
    }

    public static DoorEnvelope fromJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return new DoorEnvelope(Type.getEnum(jsonObject.getString(TYPE)),
                    jsonObject.optString(BODY), jsonObject.optString(ID), jsonObject.optString(INFO),
                    jsonObject.optString(METHOD), jsonObject.optString(FLOW_ID));
        } catch (JSONException e) {
            return null;
        }
    }

    public String toJson() {
        JSONObject jsonObject = getJsonObject();
        return jsonObject.toString();
    }

    protected JSONObject getJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt(BODY, body);
            jsonObject.putOpt(ID, id);
            jsonObject.put(TYPE, type);
            jsonObject.putOpt(INFO, info);
            jsonObject.putOpt(METHOD, method);
            jsonObject.putOpt(FLOW_ID, flowid);
        } catch (JSONException e) {

        }
        return jsonObject;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getInfo() {
        return info;
    }

    @Nullable
    public String getMethod() {
        return method;
    }

    @Nullable
    public String getFlowId() {
        return flowid;
    }

    public enum Type {
        PING("ping"), PONG("pong"), END("s:end"), DEBUG("debugInfo"), ERROR("error"),
        OMS_AUTH("o:auth"), OMS_MESSAGE("o:message"), UNKNOWN("unknown");

        private final String _value;

        Type(String value) {
            _value = value;
        }

        public static Type getEnum(String typeString) {
            for (Type type : values()) {
                if (type.getValue().equalsIgnoreCase(typeString)) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        public static Type getEnum(@NotNull DoorEnvelopeType doorEnvelopeType) {
            Type type = null;
            switch (doorEnvelopeType) {
                case O_AUTH:
                    type = OMS_AUTH;
                    break;
                case O_MESSAGE:
                    type = OMS_MESSAGE;
                    break;
                default:
                    throw new RuntimeException("unsupported doorEnvelopeType: " + doorEnvelopeType.name());
            }
            return type;
        }

        public static Optional<DoorEnvelopeType> getDoorEnvelopeTypeEnum(Type type) {
            DoorEnvelopeType doorEnvelopeType = null;
            if (type != null) {
                switch (type) {
                    case OMS_AUTH:
                        doorEnvelopeType = DoorEnvelopeType.O_AUTH;
                        break;
                    case OMS_MESSAGE:
                        doorEnvelopeType = DoorEnvelopeType.O_MESSAGE;
                        break;
                }
            }
            return Optional.fromNullable(doorEnvelopeType);
        }

        public String getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return getValue();
        }
    }
}