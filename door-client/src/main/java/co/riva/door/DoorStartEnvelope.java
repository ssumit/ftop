package co.riva.door;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoorStartEnvelope extends DoorEnvelope {
    @NotNull
    @SerializedName("ua-info")
    private final String uainfo;
    @NotNull
    @SerializedName("entity")
    private final String entity;
    @SerializedName("trace")
    private final boolean enableTrace;

    public DoorStartEnvelope(@NotNull Type type, @Nullable String body, @NotNull String id,
                             @NotNull String entity, @NotNull String uaInfo, boolean enableTrace,
                             @Nullable String flowId) {
        super(type, body, id, null, null, flowId);
        this.entity = entity;
        this.uainfo = uaInfo;
        this.enableTrace = enableTrace;
    }
}