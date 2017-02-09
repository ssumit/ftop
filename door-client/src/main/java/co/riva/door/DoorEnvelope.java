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
    private final String _body;
    @Nullable
    private final String _id;
    @NotNull
    private final Type _type;
    @Nullable
    private final String _method;
    @Nullable
    private final String _info;
    @Nullable
    private final String _flowId;

    public DoorEnvelope(@NotNull Type type, @Nullable String body, @Nullable String id,
                        @Nullable String info, @Nullable String method, @Nullable String flowId) {
        _body = body;
        _id = id;
        _type = type;
        _info = info;
        _method = method;
        _flowId = flowId;
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
            jsonObject.putOpt(BODY, _body);
            jsonObject.putOpt(ID, _id);
            jsonObject.put(TYPE, _type);
            jsonObject.putOpt(INFO, _info);
            jsonObject.putOpt(METHOD, _method);
            jsonObject.putOpt(FLOW_ID, _flowId);
        } catch (JSONException e) {

        }
        return jsonObject;
    }

    @Nullable
    public String getBody() {
        return _body;
    }

    @NotNull
    public Type getType() {
        return _type;
    }

    @Nullable
    public String getId() {
        return _id;
    }

    @Nullable
    public String getInfo() {
        return _info;
    }

    @Nullable
    public String getMethod() {
        return _method;
    }

    @Nullable
    public String getFlowId() {
        return _flowId;
    }

    public enum Type {
        PING("ping"), PONG("pong"), END("s:end"), OMS_PACKET("o:packet"), DEBUG(
                "debugInfo"), ERROR("error"), OMS_AUTH("o:auth"), OMS_MESSAGE("o:message"), UNKNOWN(
                "unknown");

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
                case O_PACKET:
                    type = OMS_PACKET;
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
                    case OMS_PACKET:
                        doorEnvelopeType = DoorEnvelopeType.O_PACKET;
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
