package co.riva.auth;

import com.google.common.base.Strings;
import olympus.common.JID;
import org.jetbrains.annotations.NotNull;

public class Credential implements Cloneable {
    private final JID jid;
    private final String authData;

    public Credential(@NotNull JID jid, @NotNull String authData) {
        if (Strings.isNullOrEmpty(authData)) {
            throw new IllegalArgumentException("auth data cannot be empty");
        } else {
            this.jid = jid;
            this.authData = authData;
        }
    }

    public String getAuthString() {
        return this.authData;
    }

    public JID getBareJid() {
        return this.jid.getBareJID();
    }
}