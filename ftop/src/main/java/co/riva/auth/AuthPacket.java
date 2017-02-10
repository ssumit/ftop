package co.riva.auth;

import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkState;

public class AuthPacket {
    private static final String AUTH_VERSION = "1.0";
    private static final String SERVER_VERSION = "4.0";
    @SerializedName("userJid")
    String _userJid;
    @SerializedName("authVersion")
    String _authVersion = "1.0";
    @SerializedName("serverVersion")
    String _serverVersion = "4.0";
    @SerializedName("streamId")
    String _streamId;
    @SerializedName("authData")
    String _authData;
    @SerializedName("pushToken")
    @Nullable
    String _pushToken;

    public AuthPacket(@NotNull Credential credential, @NotNull String streamId) {
        this._userJid = credential.getBareJid().toString();
        this._authData = credential.getAuthString();
        this._streamId = streamId;
        checkState(validateFields());
    }

    private boolean validateFields() {
        return !Strings.isNullOrEmpty(this._userJid) &&
                !Strings.isNullOrEmpty(this._authData) &&
                !Strings.isNullOrEmpty(this._streamId);
    }
}