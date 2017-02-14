package co.riva.door;

import com.google.common.base.Optional;


import com.google.gson.annotations.SerializedName;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Immutable
public class DoorEnvelope {
    @Nullable
    @SerializedName("body")
    private final String body;
    @Nullable
    @SerializedName("id")
    private final String id;
    @NotNull
    @SerializedName("type")
    private final String type;
    @Nullable
    @SerializedName("method")
    private final String method;
    @Nullable
    @SerializedName("info")
    private final String info;
    @Nullable
    @SerializedName("flowId")
    private final String flowid;

    public DoorEnvelope(@NotNull Type type, @Nullable String body, @Nullable String id,
                        @Nullable String info, @Nullable String method) {
        this.body = body;
        this.id = id;
        this.type = type.getValue();
        this.info = info;
        this.method = method;
        this.flowid = UUID.randomUUID().toString();
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @NotNull
    public Type getType() {
        return Type.getEnum(type);
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
        OMS_AUTH("o:auth"), OMS_MESSAGE("o:message"), UNKNOWN("unknown"), O_REQUEST("o:request");

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
                case O_REQUEST:
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